# Calyra-AI

Minimal Spring Boot 3.x service (Java 17).

## Local run

```bash
./mvnw spring-boot:run
```

## Telegram bot (polling)

Set the required token and run the app:

```bash
export TELEGRAM_BOT_TOKEN=your-token
./mvnw spring-boot:run
```

Then message your bot in Telegram to receive echoes.
You can copy values from `.env.example` if you use an env loader.

## Notion integration

1) Create a Notion integration at https://www.notion.so/my-integrations and copy the secret.
2) Share your target database with the integration (Share -> Invite -> integration name).
3) Copy the database ID from the Notion URL.

Set environment variables and run:

```bash
export NOTION_TOKEN=your-notion-secret
export NOTION_DATABASE_ID=your-database-id
./mvnw spring-boot:run
```

In Telegram, send `/notion_test` to create a test page.

## LLM parsing

Set the OpenAI API key and run:

```bash
export OPENAI_API_KEY=your-openai-key
./mvnw spring-boot:run
```

In Telegram, send `/parse <text>` to parse a schedule draft.

## Working memory

Normal messages now go through a short-term clarification flow with TTL.
Commands:
- `/status` shows active pending draft and expiry.
- `/reset` clears the pending draft for the chat.

## Qdrant vector memory

Start Qdrant locally:

```bash
docker compose up -d qdrant
```

By default the app fails fast if Qdrant is enabled and unreachable.
To disable vector memory, set `QDRANT_ENABLED=false`.

In Telegram, send `/qdrant_test` to write a sample vector.

## Vector retrieval

Use `/find <query>` to search your stored events and choose a match.
For ambiguous matches, the bot will ask you to reply with a number.
Stage 7 only resolves candidates; it does not perform destructive changes.

## Smart suggestions

When your event is missing details, the bot can suggest defaults based on similar events.
Reply `accept` (or `use defaults`) to apply suggestions, or answer questions to override.
Use `/suggest <text>` to preview suggestions without creating anything.

## Preference store

Preferences are stored per chat and override vector-based suggestions.
Commands:
- `/prefs` to view current preferences
- `/prefs set remind 20`
- `/prefs set hours 09:00-18:00`
- `/prefs set type Meeting duration 45`
- `/prefs set type Study location "TUM Library"`
- `/prefs reset YES`

Auto-learning (opt-in) updates preferences from created events. Enable via `PREFERENCE_LEARNING_ENABLED=true`.

## Package

```bash
./mvnw clean package
```

## Docker

```bash
docker build -t calyra-ai .
docker run --rm -p 8080:8080 calyra-ai
```

## Configuration

Environment variables:
- `SERVER_PORT` (default: 8080)
- `APP_TIMEZONE` (default: Europe/Berlin)
- `TELEGRAM_ENABLED` (default: true)
- `TELEGRAM_BOT_TOKEN` (required when enabled)
- `TELEGRAM_BOT_USERNAME` (optional)
- `TELEGRAM_POLLING_ALLOWED_UPDATES` (optional, comma-separated)
- `NOTION_ENABLED` (default: true)
- `NOTION_TOKEN` (required when enabled)
- `NOTION_DATABASE_ID` (required when enabled)
- `NOTION_VERSION` (default: 2022-06-28)
- `NOTION_API_BASE_URL` (default: https://api.notion.com)
- `NOTION_PROPERTY_NAME_TITLE` (default: Name)
- `NOTION_PROPERTY_NAME_SOURCE` (default: Source)
- `NOTION_PROPERTY_NAME_RAW` (default: Raw)
- `NOTION_PROPERTY_NAME_START` (default: Start)
- `NOTION_PROPERTY_NAME_END` (default: End)
- `NOTION_PROPERTY_NAME_TYPE` (default: Type)
- `NOTION_PROPERTY_NAME_LOCATION` (default: Location)
- `NOTION_PROPERTY_NAME_REMINDER` (default: Reminder)
- `NOTION_PROPERTY_TYPE_SOURCE` (default: SELECT; options: SELECT, RICH_TEXT)
- `LLM_ENABLED` (default: true)
- `LLM_API_BASE_URL` (default: https://api.openai.com)
- `LLM_MODEL` (default: gpt-4o-mini)
- `OPENAI_API_KEY` (required when enabled)
- `LLM_CONNECT_TIMEOUT` (default: 5s)
- `LLM_READ_TIMEOUT` (default: 20s)
- `WORKING_MEMORY_ENABLED` (default: true)
- `WORKING_MEMORY_TTL_MINUTES` (default: 30)
- `WORKING_MEMORY_MAX_PER_CHAT` (default: 1)
- `QDRANT_ENABLED` (default: true)
- `QDRANT_URL` (default: http://localhost:6333)
- `QDRANT_COLLECTION` (default: calyra_memory)
- `QDRANT_VECTOR_SIZE` (default: 1536)
- `QDRANT_DISTANCE` (default: Cosine)
- `QDRANT_CONNECT_TIMEOUT` (default: 5s)
- `QDRANT_READ_TIMEOUT` (default: 10s)
- `EMBEDDING_PROVIDER` (default: fake; options: fake, openai)
- `EMBEDDING_MODEL` (default: text-embedding-3-small)
- `RETRIEVAL_MIN_SCORE` (default: 0.35)
- `RETRIEVAL_MIN_MARGIN` (default: 0.05)
- `RETRIEVAL_LOOKBACK_DAYS` (default: 180)
- `RETRIEVAL_MAX_CANDIDATES` (default: 5)
- `RETRIEVAL_SELECTION_TTL_MINUTES` (default: 30)
- `SUGGESTION_ENABLED` (default: true)
- `SUGGESTION_MIN_CONFIDENCE_AUTO` (default: 0.85)
- `SUGGESTION_MIN_CONFIDENCE_SUGGEST` (default: 0.55)
- `SUGGESTION_MAX_CANDIDATES` (default: 10)
- `SUGGESTION_LOOKBACK_DAYS` (default: 180)
- `SUGGESTION_CONFIRMATION_REQUIRED` (default: true)
- `PREFERENCE_LEARNING_ENABLED` (default: false)
- `PREFERENCE_LEARNING_MIN_SAMPLES` (default: 3)
- `PREFERENCE_LEARNING_LOOKBACK_DAYS` (default: 180)
- `PREFERENCE_LEARNING_UPDATE_STRATEGY` (default: MEDIAN; options: MEDIAN, MEAN, MODE)
- `JDBC_URL` (default: jdbc:h2:mem:calyra;DB_CLOSE_DELAY=-1;MODE=PostgreSQL)
- `JDBC_DRIVER` (default: org.h2.Driver)
- `JDBC_USERNAME` (default: sa)
- `JDBC_PASSWORD` (default: empty)
- `JPA_DDL_AUTO` (default: update)
- `SPRING_PROFILES_ACTIVE` (default: dev)
- `POSTGRES_DB` (default: calyra)
- `POSTGRES_USER` (default: calyra)
- `POSTGRES_PASSWORD` (default: change-me)
- `DOMAIN` (optional: TLS proxy hostname)
- `ACME_EMAIL` (optional: TLS proxy email)

## Deployment (Docker Compose, polling)

1) Copy `.env.example` to `.env` and fill in the secrets.
2) Start the stack:

```bash
docker compose up -d --build
```

3) Verify health:

```bash
curl http://localhost:8080/health
```

Set `SPRING_PROFILES_ACTIVE=prod` in `.env` for Postgres, and set `JDBC_URL`, `JDBC_USERNAME`, `JDBC_PASSWORD`, `JDBC_DRIVER=org.postgresql.Driver` to match the Postgres service. Use `dev` + H2 for local-only runs.

## Optional HTTPS proxy (webhook-ready)

If you later enable a Telegram webhook endpoint, you can add a TLS proxy:

```bash
docker compose -f docker-compose.yml -f compose.webhook.yml up -d --build
```

Set `DOMAIN` and `ACME_EMAIL` in `.env` to obtain certificates.
Polling remains the default and does not require HTTPS.

## Actuator

- `/actuator/health` and `/actuator/info` are enabled.
- Avoid exposing actuator endpoints publicly without additional protections.

## Backups

- Postgres: `docker exec -t <postgres-container> pg_dump -U <user> <db> > backup.sql`
- Qdrant: keep `qdrant_data` volume or use Qdrant snapshots.

## Upgrade procedure

1) Pull/build the new image.
2) Run `docker compose up -d --build`.
3) Flyway runs migrations on startup.

## Smoke test

1) `curl http://localhost:8080/health`
2) In Telegram, send `/qdrant_test`
3) Create a normal event and verify a Notion page is created

