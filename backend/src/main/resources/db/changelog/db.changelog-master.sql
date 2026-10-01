--liquibase formatted sql

--changeset createTableUsers:1
CREATE TABLE users (
    id                 VARCHAR(40)  NOT NULL,
    email              VARCHAR(150) NOT NULL,
    username           VARCHAR(100) NOT NULL,
    first_name         VARCHAR(100) NOT NULL,
    last_name          VARCHAR(100),
    password           VARCHAR(255),
    role               VARCHAR(50)  NOT NULL,
    signup_method      VARCHAR(30)  NOT NULL,
    is_active          BOOLEAN      NOT NULL,
    is_email_verified  BOOLEAN      NOT NULL,
    is_mfa_enabled     BOOLEAN      NOT NULL,
    failed_login_count INTEGER      NOT NULL,
    last_login         BIGINT,
    created            BIGINT       NOT NULL,
    modified           BIGINT       NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);
CREATE UNIQUE INDEX ux_users_email ON users (LOWER(email));
CREATE UNIQUE INDEX ux_users_username ON users (LOWER(username));

--changeset createTableUserSessions:2
CREATE TABLE user_sessions (
    id                 VARCHAR(40) NOT NULL,
    user_id            VARCHAR(40) NOT NULL,
    session_state      VARCHAR(40) NOT NULL,
    session_source     VARCHAR(30) NOT NULL,
    refresh_token_hash VARCHAR(64),
    otp                VARCHAR(10),
    otp_expires_at     BIGINT,
    otp_resend_count   INTEGER     NOT NULL,
    ip_address         VARCHAR(45),
    user_agent         VARCHAR(255),
    expires_at         BIGINT,
    created            BIGINT      NOT NULL,
    modified           BIGINT      NOT NULL,
    CONSTRAINT pk_user_sessions PRIMARY KEY (id),
    CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX ix_user_sessions_user ON user_sessions (user_id);
CREATE UNIQUE INDEX ux_user_sessions_refresh_token ON user_sessions (refresh_token_hash) WHERE refresh_token_hash IS NOT NULL;

--changeset createTableUserAuthIdentities:3
CREATE TABLE user_auth_identities (
    id               VARCHAR(40)  NOT NULL,
    user_id          VARCHAR(40)  NOT NULL,
    provider         VARCHAR(30)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email            VARCHAR(150),
    created          BIGINT       NOT NULL,
    modified         BIGINT       NOT NULL,
    CONSTRAINT pk_user_auth_identities PRIMARY KEY (id),
    CONSTRAINT fk_user_auth_identities_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_user_auth_identities_provider ON user_auth_identities (provider, provider_user_id);
CREATE INDEX ix_user_auth_identities_user ON user_auth_identities (user_id);

--changeset createTablePasskeyCredentials:4
CREATE TABLE passkey_credentials (
    id              VARCHAR(40)  NOT NULL,
    user_id         VARCHAR(40)  NOT NULL,
    credential_id   VARCHAR(255) NOT NULL,
    public_key_cose BYTEA        NOT NULL,
    sign_count      BIGINT       NOT NULL,
    aaguid          VARCHAR(64),
    transports      VARCHAR(100),
    label           VARCHAR(100),
    last_used_at    BIGINT,
    created         BIGINT       NOT NULL,
    modified        BIGINT       NOT NULL,
    CONSTRAINT pk_passkey_credentials PRIMARY KEY (id),
    CONSTRAINT fk_passkey_credentials_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_passkey_credentials_credential_id ON passkey_credentials (credential_id);
CREATE INDEX ix_passkey_credentials_user ON passkey_credentials (user_id);

--changeset createTablePasskeyChallenges:5
CREATE TABLE passkey_challenges (
    id             VARCHAR(40)  NOT NULL,
    user_id        VARCHAR(40),
    challenge      VARCHAR(255) NOT NULL,
    challenge_type VARCHAR(30)  NOT NULL,
    expires_at     BIGINT       NOT NULL,
    created        BIGINT       NOT NULL,
    modified       BIGINT       NOT NULL,
    CONSTRAINT pk_passkey_challenges PRIMARY KEY (id),
    CONSTRAINT fk_passkey_challenges_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_passkey_challenges_challenge ON passkey_challenges (challenge);

--changeset createTableUserMfaConfigurations:6
CREATE TABLE user_mfa_configurations (
    id                    VARCHAR(40) NOT NULL,
    user_id               VARCHAR(40) NOT NULL,
    mfa_type              VARCHAR(30) NOT NULL,
    totp_secret_encrypted VARCHAR(512),
    is_enabled            BOOLEAN     NOT NULL,
    is_preferred          BOOLEAN     NOT NULL,
    verified_at           BIGINT,
    created               BIGINT      NOT NULL,
    modified              BIGINT      NOT NULL,
    CONSTRAINT pk_user_mfa_configurations PRIMARY KEY (id),
    CONSTRAINT fk_user_mfa_configurations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_user_mfa_configurations_user_type ON user_mfa_configurations (user_id, mfa_type);

--changeset createTableMfaRecoveryCodes:7
CREATE TABLE mfa_recovery_codes (
    id        VARCHAR(40)  NOT NULL,
    user_id   VARCHAR(40)  NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    used_at   BIGINT,
    created   BIGINT       NOT NULL,
    modified  BIGINT       NOT NULL,
    CONSTRAINT pk_mfa_recovery_codes PRIMARY KEY (id),
    CONSTRAINT fk_mfa_recovery_codes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX ix_mfa_recovery_codes_user ON mfa_recovery_codes (user_id);

--changeset createTableCategories:8
CREATE TABLE categories (
    id         VARCHAR(40) NOT NULL,
    user_id    VARCHAR(40),
    name       VARCHAR(60) NOT NULL,
    type       VARCHAR(20) NOT NULL,
    icon       VARCHAR(50) NOT NULL,
    color_code VARCHAR(20) NOT NULL,
    is_default BOOLEAN     NOT NULL,
    created    BIGINT      NOT NULL,
    modified   BIGINT      NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX ix_categories_user ON categories (user_id);

--changeset createUniqueIndexCategoriesName:9
-- Two partial indexes rather than one composite: a plain UNIQUE (user_id, name, type) would not
-- constrain the system rows at all, because Postgres treats every NULL user_id as distinct.
CREATE UNIQUE INDEX ux_categories_user_name_type ON categories (user_id, LOWER(name), type) WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX ux_categories_system_name_type ON categories (LOWER(name), type) WHERE user_id IS NULL;

--changeset createTableTransactions:10
CREATE TABLE transactions (
    id             VARCHAR(40)    NOT NULL,
    user_id        VARCHAR(40)    NOT NULL,
    category_id    VARCHAR(40)    NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    amount         NUMERIC(18, 2) NOT NULL,
    date           DATE           NOT NULL,
    description    VARCHAR(150)   NOT NULL,
    payment_method VARCHAR(30)    NOT NULL,
    notes          VARCHAR(500),
    created        BIGINT         NOT NULL,
    modified       BIGINT         NOT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0)
);
CREATE INDEX ix_transactions_user_date ON transactions (user_id, date DESC);
CREATE INDEX ix_transactions_category ON transactions (category_id);

--changeset createTableBudgets:11
CREATE TABLE budgets (
    id           VARCHAR(40)    NOT NULL,
    user_id      VARCHAR(40)    NOT NULL,
    month        INTEGER        NOT NULL,
    year         INTEGER        NOT NULL,
    total_budget NUMERIC(18, 2) NOT NULL,
    notes        VARCHAR(250),
    created      BIGINT         NOT NULL,
    modified     BIGINT         NOT NULL,
    CONSTRAINT pk_budgets PRIMARY KEY (id),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_budgets_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT ck_budgets_year CHECK (year BETWEEN 2000 AND 2100),
    CONSTRAINT ck_budgets_total_non_negative CHECK (total_budget >= 0)
);
CREATE UNIQUE INDEX ux_budgets_user_month_year ON budgets (user_id, month, year);

--changeset createTableCategoryBudgets:12
CREATE TABLE category_budgets (
    id            VARCHAR(40)    NOT NULL,
    budget_id     VARCHAR(40)    NOT NULL,
    category_id   VARCHAR(40)    NOT NULL,
    monthly_limit NUMERIC(18, 2) NOT NULL,
    created       BIGINT         NOT NULL,
    modified      BIGINT         NOT NULL,
    CONSTRAINT pk_category_budgets PRIMARY KEY (id),
    CONSTRAINT fk_category_budgets_budget FOREIGN KEY (budget_id) REFERENCES budgets (id) ON DELETE CASCADE,
    CONSTRAINT fk_category_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
    CONSTRAINT ck_category_budgets_limit_non_negative CHECK (monthly_limit >= 0)
);
CREATE UNIQUE INDEX ux_category_budgets_budget_category ON category_budgets (budget_id, category_id);
CREATE INDEX ix_category_budgets_category ON category_budgets (category_id);

--changeset createTableAccountBalances:13
CREATE TABLE account_balances (
    id              VARCHAR(40)    NOT NULL,
    user_id         VARCHAR(40)    NOT NULL,
    account_name    VARCHAR(100)   NOT NULL,
    current_balance NUMERIC(18, 2) NOT NULL,
    last_updated    BIGINT         NOT NULL,
    notes           VARCHAR(250),
    created         BIGINT         NOT NULL,
    modified        BIGINT         NOT NULL,
    CONSTRAINT pk_account_balances PRIMARY KEY (id),
    CONSTRAINT fk_account_balances_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_account_balances_user ON account_balances (user_id);

--changeset createTableBalanceHistories:14
-- related_transaction_id is intentionally not a foreign key: the audit trail outlives the
-- transaction it describes, and deleting a transaction must not delete or null its history.
CREATE TABLE balance_histories (
    id                     VARCHAR(40)    NOT NULL,
    user_id                VARCHAR(40)    NOT NULL,
    occurred_at            BIGINT         NOT NULL,
    previous_balance       NUMERIC(18, 2) NOT NULL,
    new_balance            NUMERIC(18, 2) NOT NULL,
    change_amount          NUMERIC(18, 2) NOT NULL,
    reason                 VARCHAR(100)   NOT NULL,
    notes                  VARCHAR(250),
    related_transaction_id VARCHAR(40),
    created                BIGINT         NOT NULL,
    modified               BIGINT         NOT NULL,
    CONSTRAINT pk_balance_histories PRIMARY KEY (id),
    CONSTRAINT fk_balance_histories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX ix_balance_histories_user_occurred ON balance_histories (user_id, occurred_at DESC);

--changeset insertDefaultCategories:15
-- System categories: user_id NULL means every user sees them. Icons are Bootstrap Icons class
-- names and colours are hex, both rendered directly by the frontend.
INSERT INTO categories (id, user_id, name, type, icon, color_code, is_default, created, modified)
SELECT 'cat_' || REPLACE(gen_random_uuid()::TEXT, '-', ''),
       NULL,
       seed.name,
       seed.type,
       seed.icon,
       seed.color_code,
       TRUE,
       (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
       (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
FROM (VALUES
    ('Rent & Housing',          'EXPENSE', 'bi-house-door',      '#6366f1'),
    ('Groceries & Supermarket', 'EXPENSE', 'bi-cart3',           '#10b981'),
    ('Dining & Food Delivery',  'EXPENSE', 'bi-cup-hot',         '#f59e0b'),
    ('Utilities & Bills',       'EXPENSE', 'bi-lightning-charge', '#06b6d4'),
    ('Transport & Fuel',        'EXPENSE', 'bi-fuel-pump',       '#8b5cf6'),
    ('Shopping & Fashion',      'EXPENSE', 'bi-bag',             '#ec4899'),
    ('SIP & Investments',       'EXPENSE', 'bi-graph-up-arrow',  '#14b8a6'),
    ('Health & Pharmacy',       'EXPENSE', 'bi-heart-pulse',     '#ef4444'),
    ('Entertainment & OTT',     'EXPENSE', 'bi-film',            '#f97316'),
    ('Education & Books',       'EXPENSE', 'bi-book',            '#3b82f6'),
    ('Personal Care',           'EXPENSE', 'bi-scissors',        '#a855f7'),
    ('Miscellaneous',           'EXPENSE', 'bi-three-dots',      '#64748b'),
    ('Salary',                  'INCOME',  'bi-wallet2',         '#10b981'),
    ('Freelance & Consulting',  'INCOME',  'bi-laptop',          '#3b82f6'),
    ('Dividends & Stocks',      'INCOME',  'bi-piggy-bank',      '#14b8a6'),
    ('Rental Income',           'INCOME',  'bi-building',        '#6366f1'),
    ('Bank Interest',           'INCOME',  'bi-bank',            '#0ea5e9'),
    ('Cashback & Rewards',      'INCOME',  'bi-gift',            '#f59e0b'),
    ('Other Income',            'INCOME',  'bi-cash-coin',       '#84cc16')
) AS seed(name, type, icon, color_code);
