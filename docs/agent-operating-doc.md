# GABI — Agent Operating Guide

This document describes how an external agent (or any automated client) drives the GABI library
management system via its MCP and REST access layer. It is the operating guide for the in-repo
Claude agent asset.

> **In-repo agent asset:** [`.claude/agents/gabi-operator.md`](../.claude/agents/gabi-operator.md)
> — the `gabi-operator` Claude Code subagent that drives GABI's catalogue and RAG capability
> headlessly through the MCP/REST access layer described below.
> *(This asset is authored by The Metaprompter. The link will be live once that step completes.)*

---

## Table of contents

1. [What GABI does for agents](#what-gabi-does-for-agents)
2. [Starting the access layer](#starting-the-access-layer)
3. [Available tools](#available-tools)
4. [Primary RAG workflow](#primary-rag-workflow)
5. [Catalogue read workflows](#catalogue-read-workflows)
6. [Input/output reference per tool](#inputoutput-reference-per-tool)
7. [Error table](#error-table)
8. [RAG provider and runtime caveats](#rag-provider-and-runtime-caveats)
9. [What the agent does NOT control](#what-the-agent-does-not-control)

---

## What GABI does for agents

GABI exposes a **stateful library catalogue** (books, members, loans) and a **RAG Q&A capability**
over a shared `LibraryService` core. An agent uses it to:

- Ask **natural-language questions** about the book catalogue ("Which authors have more than one
  book available?") and receive grounded answers with source citations.
- **Browse and search** the catalogue: list all books/members/loans, search by field, fetch by ID,
  count records.
- **Rebuild the RAG index** after catalogue changes to keep answers current.
- **Check service health** before issuing other tool calls.

The service has **persistent state** (a live Apache Derby database). Catalogue data survives
restarts. The RAG vector index is in-memory by default and must be rebuilt on each start, or
persisted via `gabi.vectorstore.file` configuration.

---

## Starting the access layer

The access layer requires GABI to be running in one of two access-layer modes. The CLI mode (the
default) does not expose MCP tools or REST endpoints.

### HTTP server mode (Streamable-HTTP MCP + REST)

Preferred for remote agents, multi-client access, and CI pipelines.

```bash
export DB_USER=<your-derby-user>
export DB_PASSWORD=<your-derby-password>
java -jar /path/to/gabi-1.0.0-exec.jar --spring.profiles.active=server
```

- MCP Streamable-HTTP endpoint: `http://localhost:8080/mcp`
- REST API base: `http://localhost:8080/api`
- Health check: `http://localhost:8080/health`
- Override port: add `--server.port=<port>`

To also activate RAG with a specific provider, combine profiles:

```bash
# OpenAI provider
export OPENAI_API_KEY=sk-...
java -jar /path/to/gabi-1.0.0-exec.jar --spring.profiles.active=server,openai

# Anthropic chat + Ollama embeddings
export ANTHROPIC_API_KEY=sk-ant-...
java -jar /path/to/gabi-1.0.0-exec.jar --spring.profiles.active=server,anthropic
```

MCP client configuration for the HTTP transport (Claude Desktop / agent config):

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

### STDIO mode (MCP client launches the process)

Preferred for local single-agent use and Claude Desktop.

```bash
export DB_USER=<your-derby-user>
export DB_PASSWORD=<your-derby-password>
java -jar /path/to/gabi-1.0.0-exec.jar \
     --spring.ai.mcp.server.stdio=true \
     --spring.main.web-application-type=none
```

MCP client configuration (Claude Desktop / agent config):

```json
{
  "mcpServers": {
    "gabi": {
      "command": "java",
      "args": [
        "-jar", "/absolute/path/to/gabi-1.0.0-exec.jar",
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

See `docs/README-access.md` for the full transport configuration reference.

---

## Available tools

All 14 tools are available on both the Streamable-HTTP MCP endpoint (`/mcp`) and the STDIO MCP
server. Each tool maps 1-to-1 to a REST endpoint under `server` mode.

| MCP tool | REST equivalent | Description |
|----------|-----------------|-------------|
| `ask` | `POST /api/ask` | RAG Q&A — natural-language question over the indexed catalogue |
| `reindex` | `POST /api/reindex` | Rebuild the RAG vector index from the live catalogue |
| `list_books` | `GET /api/books` | List all books |
| `search_books` | `GET /api/books/search?field=&text=` | Search books by field + text |
| `get_book` | `GET /api/books/{id}` | Get a single book by ID |
| `count_books` | `GET /api/books/count` | Count books (count + maxId) |
| `list_members` | `GET /api/members` | List all members |
| `search_members` | `GET /api/members/search?field=&text=` | Search members by field + text |
| `get_member` | `GET /api/members/{id}` | Get a single member by ID |
| `count_members` | `GET /api/members/count` | Count members (count + maxId) |
| `list_loans` | `GET /api/loans` | List all active loans |
| `list_loans_by_member` | `GET /api/members/{memberId}/loans` | List loans for a specific member |
| `count_loans` | `GET /api/loans/count` | Count loans (count + maxId) |
| `health` | `GET /health` | Health check: status UP + timestamp |

User-admin operations (`addUser`, `deleteUser`, Derby `derby.user.*` / GRANT / REVOKE) are
intentionally absent from the MCP and REST surface. See
[What the agent does NOT control](#what-the-agent-does-not-control).

---

## Primary RAG workflow

The RAG workflow answers natural-language questions grounded in the live book catalogue.

### Step 1 — Build the index

On first use (or after catalogue changes), call `reindex` to project all Derby book rows into the
`SimpleVectorStore`. Each book row is converted to a document of the form:

```
Book ID: <n> | Title: <title> | Author: <author> | Status: Available|Currently lent out
```

Documents are chunked with `TokenTextSplitter`, embedded via the active `EmbeddingModel`, and
stored in the in-memory vector store.

MCP call:

```json
{ "tool": "reindex" }
```

REST call:

```http
POST /api/reindex
```

Returns HTTP 204 No Content (REST) or the string `"Vector index rebuilt successfully."` (MCP).

The index is in-memory. It is reset on every GABI restart unless `gabi.vectorstore.file` is
configured for persistence.

### Step 2 — Ask a question

Call `ask` with any natural-language question about the catalogue.

MCP call:

```json
{
  "tool": "ask",
  "question": "Which science fiction books are currently available?"
}
```

REST call:

```http
POST /api/ask
Content-Type: application/json

{"question": "Which science fiction books are currently available?"}
```

The RAG pipeline:
1. Embeds the question using the active `EmbeddingModel`.
2. Runs a similarity search over the vector store (top-5, threshold 0.5).
3. Injects the matching catalogue chunks into the chat prompt as context via
   `QuestionAnswerAdvisor`.
4. Returns the grounded answer from the `ChatModel` plus the source chunks used.

Example MCP / REST response:

```json
{
  "answer": "Based on the catalogue, the following science fiction books are currently available: ...",
  "sources": [
    "Book ID: 42 | Title: Dune | Author: Frank Herbert | Status: Available",
    "Book ID: 17 | Title: Foundation | Author: Isaac Asimov | Status: Available"
  ]
}
```

### When to rebuild

Call `reindex` again whenever books are added or removed through the CLI. The agent cannot add or
delete books via MCP/REST (those operations are CLI-only in the current surface); however,
detecting catalogue drift (via `count_books` before and after a CLI session) is a reasonable
trigger for a reindex.

---

## Catalogue read workflows

### List and browse

```
health           → confirm service is up
list_books       → full catalogue
list_members     → all members
list_loans       → all active loans
```

### Search by field

```
search_books  field="title"   text="dune"
search_books  field="author"  text="asimov"
search_members field="name"   text="garcia"
search_members field="surname" text="smith"
```

`field` and `text` are both required. The search is case-insensitive and uses substring (LIKE)
matching.

### Fetch by ID

```
get_book    id=42
get_member  id=7
```

### Count and capacity check

```
count_books    → {"count": 150, "maxId": 162}
count_members  → {"count": 48,  "maxId": 51}
count_loans    → {"count": 12,  "maxId": 14}
```

`maxId` is the highest allocated ID, which may exceed `count` when records have been deleted.
Use `count` + `list_*` to understand current catalogue state.

### Member loan lookup

```
list_loans_by_member  memberId=7
```

Returns all active loans for that member, or throws `NotFoundException` if the member has no
active loans.

---

## Input/output reference per tool

### `ask`

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `question` | string | yes | Natural-language question about the catalogue (non-blank) |

Output: `AnswerWithSources`

| Field | Type | Notes |
|-------|------|-------|
| `answer` | string | Model-generated answer grounded in the vector store results |
| `sources` | `List<string>` | Catalogue chunks used as context; empty if no relevant chunks found |

If the model backend is unavailable, `answer` contains a human-readable degraded message and
`sources` is empty (no exception is thrown — see [RAG provider and runtime caveats](#rag-provider-and-runtime-caveats)).

### `reindex`

No inputs. Returns the string `"Vector index rebuilt successfully."` (MCP) or HTTP 204 (REST).

### `list_books`

No inputs. Returns `List<BookDto>`.

| Field | Type | Notes |
|-------|------|-------|
| `id` | int | Unique book ID |
| `title` | string | Book title |
| `author` | string | Book author |
| `lent` | boolean | `true` if currently lent out |

### `search_books`

| Input field | Type | Required | Valid values |
|-------------|------|----------|--------------|
| `field` | string | yes | `"title"` or `"author"` |
| `text` | string | yes | Substring to search (case-insensitive) |

Returns `List<BookDto>`. Throws `NotFoundException` if no books match.

### `get_book`

| Input field | Type | Required | Notes |
|-------------|------|----------|-------|
| `id` | int | yes | Unique book ID |

Returns `BookDto`. Throws `NotFoundException` if the ID does not exist.

### `count_books`

No inputs. Returns `CountResult`:

| Field | Type | Notes |
|-------|------|-------|
| `count` | int | Total books in the catalogue |
| `maxId` | int | Highest allocated book ID (0 if empty) |

### `list_members`

No inputs. Returns `List<MemberDto>`.

| Field | Type | Notes |
|-------|------|-------|
| `id` | int | Unique member ID |
| `name` | string | First name |
| `surname` | string | Surname |

### `search_members`

| Input field | Type | Required | Valid values |
|-------------|------|----------|--------------|
| `field` | string | yes | `"name"` or `"surname"` |
| `text` | string | yes | Substring to search (case-insensitive) |

Returns `List<MemberDto>`. Throws `NotFoundException` if no members match.

### `get_member`

| Input field | Type | Required | Notes |
|-------------|------|----------|-------|
| `id` | int | yes | Unique member ID |

Returns `MemberDto`. Throws `NotFoundException` if the ID does not exist.

### `count_members`

No inputs. Returns `CountResult` (same structure as `count_books`, for members).

### `list_loans`

No inputs. Returns `List<LoanDto>`.

| Field | Type | Notes |
|-------|------|-------|
| `id` | int | Unique loan ID |
| `memberId` | int | ID of the borrowing member |
| `bookId` | int | ID of the borrowed book |
| `dateLoan` | string | Loan date (ISO-8601: `YYYY-MM-DD`) |

### `list_loans_by_member`

| Input field | Type | Required | Notes |
|-------------|------|----------|-------|
| `memberId` | int | yes | Member's unique ID |

Returns `List<LoanDto>`. Throws `NotFoundException` if the member has no active loans.

### `count_loans`

No inputs. Returns `CountResult` (same structure as `count_books`, for loans).

### `health`

No inputs. Returns:

```json
{
  "status": "UP",
  "timestamp": "2026-06-12T10:30:00.123456Z"
}
```

---

## Error table

All `LibraryException` subtypes map to HTTP status codes (REST) or MCP tool errors. REST responses
use RFC 9457 `ProblemDetail` format. MCP tool errors set `isError: true` in the tool result.

| Exception type | HTTP status | Typical trigger | Recovery |
|----------------|-------------|-----------------|----------|
| `NotFoundException` | 404 Not Found | `get_book`/`get_member`/`list_loans_by_member` with a non-existent ID | Use `list_books`, `list_members`, or `count_*` to discover valid IDs |
| `InvalidIdentifierException` | 400 Bad Request | A supplied identifier contains characters outside `[A-Za-z0-9_]` | Sanitize the identifier; only alphanumeric and underscore are allowed |
| `BusinessRuleException` | 422 Unprocessable Entity | Attempting an operation that violates a business rule (e.g., loan limit exceeded, book already lent, deleting a member with active loans) | Read the detail message; resolve the constraint via the CLI |
| `DuplicateException` | 409 Conflict | Adding a book or member that already exists (same title+author or same name+surname) | Use `search_books` or `search_members` to verify before adding |
| `PersistenceException` | 500 Internal Server Error | Derby database error (connection failure, constraint violation, I/O error) | Check that the Derby network server is running on `:1527`; inspect GABI server logs |
| `LibraryException` (base) | 500 Internal Server Error | Unclassified library error | Inspect GABI server logs |

Stack traces and DB metadata are never included in error responses.

### MCP tool error format

When a tool throws, the MCP result has `isError: true` and the text content contains the exception
class name and message:

```json
{
  "isError": true,
  "content": [
    {
      "type": "text",
      "text": "NotFoundException: Book not found: 99"
    }
  ]
}
```

---

## RAG provider and runtime caveats

### Provider requirements

| Provider | What must be running | Notes |
|----------|---------------------|-------|
| Ollama (default) | Ollama daemon + `llama3.2` + `nomic-embed-text` models pulled | `ollama run llama3.2` starts the daemon and pulls the model if not present |
| OpenAI | Active internet connection + `OPENAI_API_KEY` env var | Embeddings use `text-embedding-ada-002` |
| Anthropic | Active internet connection + `ANTHROPIC_API_KEY` env var + Ollama (`nomic-embed-text`) for embeddings | Anthropic provides chat only; embeddings remain on Ollama |

### Degraded mode

If no model backend is reachable at query time, `ask` degrades gracefully rather than throwing:

- MCP tool result: `isError: false`; `answer` field contains:
  `"RAG service is not available (no model running). Start Ollama with 'ollama run llama3.2' or set an API key via OPENAI_API_KEY."`
- `sources`: empty list

The `reindex` / `ask` tools can still be called; the degraded message simply means the model could
not be contacted. The rest of the catalogue tools (`list_books`, `search_books`, etc.) are
unaffected by model availability — they are pure Derby reads.

### NoOpRagService

If no `EmbeddingModel` bean is registered in the Spring context (e.g., GABI is started with
`-Dspring.ai.ollama.enabled=false` and no cloud profile), the `NoOpRagService` fallback is
activated. It logs a warning and returns the same degraded message on every `ask` call.

### Vector store persistence

By default the `SimpleVectorStore` is in-memory and is empty on startup. An agent that needs the
index to survive restarts should configure `gabi.vectorstore.file=/path/to/store.json` in
`application.yml` or pass it as a JVM property. After each `reindex` call the store is persisted
to that file and reloaded on the next start.

---

## What the agent does NOT control

- **The CLI library manager** — interactive menus (Books, Members, Loans, Users) are only
  available when GABI is run in the default CLI mode (no `server` profile). The agent cannot
  trigger console interactions or add/delete books, members, or loans through the MCP/REST surface.
  Write operations (add/delete book, add/delete member, create/return loan) exist in
  `LibraryService` but are not exposed as MCP tools or REST endpoints in this release.
- **DB user administration** — provisioning Derby users (`addUser`, `deleteUser`, GRANT/REVOKE,
  `derby.user.*` properties) is intentionally excluded from the network surface. These privileged
  operations are available only through the authenticated CLI session.
- **Packaging and build** — the jpackage app-image (`packaging/bin/GABI/GABI.exe`) and the Maven
  build (`mvnw package`) are outside the agent's scope. Refer to `packaging/README-packaging.md`
  for build instructions.
- **Database initialization** — the Derby schema is created by `DatabaseBuilder` the first time
  GABI runs with `database-isbuilt=false` in the configuration. Schema management is a one-time
  setup operation performed through the CLI, not through the access layer.
- **Vector store warm-up** — the agent is responsible for calling `reindex` at the start of a
  session if it wants accurate RAG answers. GABI does not auto-index on startup.
