# Expense Manager

Personal finance tracker: log income and expenses, categorise them, set monthly budgets per
category, reconcile against a bank balance, and see where the money went.

## Stack

| Layer | Choice |
|---|---|
| Backend | Spring Boot 3.3.0, Java 21, Gradle |
| Data | PostgreSQL 17, JPA + QueryDSL, Liquibase |
| Common layer | Vendored in `common/` — repository vocabulary, DTO envelopes, request context |
| Auth | Self-issued JWT, email+password / Google / passkey, with MFA |
| Frontend | Vite, React 19, TypeScript, Bootstrap 5, Chart.js |

## Prerequisites

Java 21, Docker, Node 20+. All dependencies resolve from Maven Central — there is no private
registry to authenticate against.

## Running locally

```bash
cp .env.example .env        # then fill in the blanks
docker compose up -d        # PostgreSQL on :5432

cd backend && ./gradlew bootRun     # API on :8080, Swagger at /swagger-ui.html
cd frontend && npm install && npm run dev   # UI on :5173
```

## Tests

```bash
cd backend && ./gradlew test
```

## Configuration

No secrets live in `application.yml` — every sensitive value is an `${ENV}` placeholder backed by
`.env`, which is gitignored. See `.env.example` for the full list, including how to generate the
Ed25519 token-signing keypair.

## Layout

```
backend/src/main/java/com/expensemanager/
  common/      shared foundation: base entities, repository vocabulary,
               error envelope, request context, id generation
  config/      domain/      repository/      security/
  service/     controller/  dto/             exceptions/   util/
backend/src/main/resources/db/changelog/   Liquibase changesets
frontend/src/                              React application
```

### The `common/` package

Base classes every entity and repository builds on, kept in-repo rather than taken as a binary
dependency:

| | |
|---|---|
| `IdentityJpaDomain` | String id plus `created` / `modified` epoch-millis stamps |
| `ModulePrefix` | Entity to id-prefix map (`txn_`, `usr_`, ...); fails loudly for an unregistered entity |
| `BaseRepository` | QueryDSL vocabulary: keyset paging, projections, bulk update, predicate delete |
| `ListResponse` | `{"has_more": bool, "object": "list", "data": [...]}` |
| `ApiError` | `{"errors": [{"type", "message", "param", "reason_code"}]}` |
| `RequestContext` | Per-request identity, populated from a verified token |
