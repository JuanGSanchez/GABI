# GABI — Agent Access Layer (MCP + REST)

GABI exposes its library catalogue and RAG capabilities as both an **MCP server** (Streamable-HTTP
and STDIO) and a **REST API**, over ONE shared `LibraryService` core.

---

## Quick-start

### HTTP server mode (MCP Streamable-HTTP + REST)

```bash
# Set DB credentials via environment variables (never hardcode them)
export DB_USER=<your-derby-user>
export DB_PASSWORD=<your-derby-password>

# Optional: point at Ollama if not on localhost:11434
# export OLLAMA_BASE_URL=http://localhost:11434

# Start GABI in server mode
java -jar target/gabi-1.0.0-exec.jar --spring.profiles.active=server
```

The server listens on port 8080 (override with `SERVER_PORT=<port>`).

### STDIO mode (for MCP clients that launch the process)

```bash
java -jar target/gabi-1.0.0-exec.jar \
     --spring.ai.mcp.server.stdio=true \
     --spring.main.web-application-type=none
```

The MCP client drives GABI via stdin/stdout. No HTTP port is needed.

### CLI mode (original interactive menu — default)

```bash
java -jar target/gabi-1.0.0-exec.jar
# Credentials prompted or read from DB_USER / DB_PASSWORD env vars
```

---

## MCP client configuration

### HTTP (Streamable-HTTP) — Claude Desktop / agent config

```json
{
  "mcpServers": {
    "gabi": {
      "url": "http://localhost:8080/mcp",
      "transport": "streamable-http"
    }
  }
}
```

### STDIO — Claude Desktop / agent config

```json
{
  "mcpServers": {
    "gabi": {
      "command": "java",
      "args": [
        "-jar", "/path/to/gabi-1.0.0-exec.jar",
        "--spring.ai.mcp.server.stdio=true",
        "--spring.main.web-application-type=none"
      ],
      "env": {
        "DB_USER": "<your-user>",
        "DB_PASSWORD": "<your-password>"
      }
    }
  }
}
```

---

## MCP tools

| Tool name             | Description                                                        | Maps to core method          |
|-----------------------|--------------------------------------------------------------------|------------------------------|
| `ask`                 | RAG question over the catalogue → grounded answer + sources        | `LibraryService.ask()`       |
| `reindex`             | Rebuild the RAG vector index from the live catalogue               | `LibraryService.ingest()`    |
| `list_books`          | List all books                                                     | `listBooks()`                |
| `search_books`        | Search books by field (`title`/`author`) + text                    | `searchBooks()`              |
| `get_book`            | Get a single book by ID                                            | `getBook()`                  |
| `count_books`         | Count books (count + maxId)                                        | `countBooks()`               |
| `list_members`        | List all members                                                   | `listMembers()`              |
| `search_members`      | Search members by field (`name`/`surname`) + text                  | `searchMembers()`            |
| `get_member`          | Get a single member by ID                                          | `getMember()`                |
| `count_members`       | Count members (count + maxId)                                      | `countMembers()`             |
| `list_loans`          | List all active loans                                              | `listLoans()`                |
| `list_loans_by_member`| List loans for a specific member                                   | `listLoansByMember()`        |
| `count_loans`         | Count loans (count + maxId)                                        | `countLoans()`               |
| `health`              | Health check: status UP + timestamp                                | —                            |

User-admin operations (`addUser`, `deleteUser`, Derby `derby.user.*` / GRANT) are **not** in the
MCP surface. See security notes below.

---

## REST endpoints

All endpoints are under `/api` (server mode only — requires `--spring.profiles.active=server`).

| Method | Path                           | Description                               |
|--------|--------------------------------|-------------------------------------------|
| POST   | `/api/ask`                     | RAG question → `{"answer":"…","sources":[…]}` |
| POST   | `/api/reindex`                 | Rebuild RAG index (204 No Content)        |
| GET    | `/api/books`                   | List all books                            |
| GET    | `/api/books/search?field=&text=` | Search books by field + text            |
| GET    | `/api/books/{id}`              | Get book by ID                            |
| GET    | `/api/books/count`             | `{"count":N,"maxId":M}`                  |
| GET    | `/api/members`                 | List all members                          |
| GET    | `/api/members/search?field=&text=` | Search members by field + text       |
| GET    | `/api/members/{id}`            | Get member by ID                          |
| GET    | `/api/members/count`           | `{"count":N,"maxId":M}`                  |
| GET    | `/api/loans`                   | List all loans                            |
| GET    | `/api/members/{memberId}/loans`| List loans for a member                  |
| GET    | `/api/loans/count`             | `{"count":N,"maxId":M}`                  |
| GET    | `/health`                      | `{"status":"UP","timestamp":"…"}`        |

### Error responses (RFC 9457 ProblemDetail)

| Exception type               | HTTP status                    |
|------------------------------|-------------------------------|
| `NotFoundException`          | 404 Not Found                  |
| `InvalidIdentifierException` | 400 Bad Request                |
| `BusinessRuleException`      | 422 Unprocessable Entity       |
| `DuplicateException`         | 409 Conflict                   |
| `PersistenceException`       | 500 Internal Server Error      |

Stack traces and DB credentials are never included in error responses.

---

## Transport configuration

In `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    mcp:
      server:
        name: gabi-library-mcp
        version: 1.0.0
        protocol: STREAMABLE   # Streamable-HTTP (deprecated SSE replaced)
        stdio: true             # also enable STDIO for CLI/agent launch
        type: SYNC
```

In `src/main/resources/application-server.yml` (server profile):

```yaml
spring:
  main:
    web-application-type: servlet  # enables Tomcat + REST + Streamable-HTTP MCP endpoint
```

---

## Security notes

- **DB credentials** — set via `DB_USER` and `DB_PASSWORD` environment variables. Never committed.
- **AI API keys** — set via `OPENAI_API_KEY` / `ANTHROPIC_API_KEY` env vars for cloud profiles.
  The default Ollama profile requires no API key.
- **User-admin surface** — Derby user provisioning (`addUser`, `deleteUser`, GRANT/REVOKE,
  `derby.user.*`) is excluded from both MCP tools and REST endpoints. These operations can only
  be performed through the CLI (`--spring.profiles.active=` default, authenticated session).
- **Error responses** — `GlobalExceptionHandler` maps all `LibraryException` subtypes to safe
  HTTP responses. No stack traces or DB metadata are forwarded to clients.
- **Identifier validation** — all user-supplied identifiers pass through `IdentifierValidator`
  inside the core, preventing SQL identifier injection (D-4 fix).
