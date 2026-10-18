# orders-service

Places, lists and cancels customer orders for the Shopfront web shop and mobile app.
Prices come from catalog-service at the moment the order is placed.

## Stack

- Java 17, Spring Boot 3.5, Gradle (wrapper included)
- PostgreSQL, schema managed by Flyway (`src/main/resources/db/migration`)
- springdoc-openapi: Swagger UI at `/swagger-ui.html`, spec at `/v3/api-docs`

## Running locally

You need a Postgres with an `orders` database and a running catalog-service. The easiest
way is the `dev-env` repo, which starts everything with Docker Compose.

To run just this service against your own Postgres:

```
export DB_URL=jdbc:postgresql://localhost:5432/orders DB_USER=orders DB_PASSWORD=orders
export CATALOG_BASE_URL=http://localhost:8081
./gradlew bootRun
```

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/orders` |
| `DB_USER` / `DB_PASSWORD` | `orders` / `orders` |
| `CATALOG_BASE_URL` | `http://localhost:8081` |
| `PORT` | `8080` |

## Tests

```
./gradlew test
```

Tests use H2 in PostgreSQL mode (profile `test`), so no database is needed. The catalog
is mocked.

One test is expected to fail locally: see [docs/KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md).
CI skips it with `-PexcludeTags=known-bug`.

`OrderConfirmationIT` fails now and then on slow machines. Re-run it before you go looking.

## OpenAPI spec

```
./gradlew generateOpenApi
```

writes `build/openapi/orders.json` without starting a server (it boots the app against
H2 in a test). CI uploads it as the `orders-openapi` artifact. The web and mobile repos
keep a copy in their `specs/` folder; copy the new file there when the API changes.

## Docker

```
docker build -t shopfront/orders-service .
docker run -p 8080:8080 -e DB_URL=... -e CATALOG_BASE_URL=... shopfront/orders-service
```

## More

- [Architecture](docs/architecture.md)
- [Known issues](docs/KNOWN_ISSUES.md)
