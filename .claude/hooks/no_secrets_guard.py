#!/usr/bin/env python3
"""PreToolUse hook: block committing DB credentials / API keys (GABI-B08).

## Principles Applied
P1 Source-of-Truth Grounding | P2 Full Determinism | P8 Principles Inheritance | P11 Programmatic Determinism
No-secrets rule: DB credentials and API keys must remain as ${ENV} placeholders in all
application config and source files; this hook blocks literal credential commits at edit time
(java-spring-conventions.md rule 4; GABI-B08).

Fires on Edit/Write. BLOCKS (exit 2) when new content for an application*.yml/properties
config file or a *.java file assigns a real literal value to a credential key instead of
an ${ENV} placeholder / profile-sourced value:
  - Derby DB creds: database-name / database-password / DB_USER / DB_PASSWORD /
    gabi.db.user|password, derby user/password
  - Model provider API keys: Ollama / OpenAI / Anthropic (api-key, OPENAI_API_KEY,
    ANTHROPIC_API_KEY, spring.ai.*.api-key)

Ground truth — application.yml requires `${DB_USER:}` / `${DB_PASSWORD:}` style and
"NEVER set defaults here; read from environment variables only"; GABI-B08 removes the
committed `database-password=dev-local-only` / `admin 1234` default-credential pattern.
A value that is an ${ENV...} placeholder, empty, or an obvious placeholder token passes.

Non-fatal on its own errors (exit 0). Block protocol: exit 2 + reason on stderr.
"""
from __future__ import annotations

import json
import re
import sys

TARGET_RX = re.compile(r"application[^\\/]*\.(?:ya?ml|properties)$|\.java$", re.IGNORECASE)

# credential key  <sep>  value   (yaml ':' , properties/java '=' or ':')
CRED_RX = re.compile(
    r"""(?ix)
    (?:^|[\s."'])
    (?:database-(?:name|password)
       | db[._-]?(?:user|password)
       | (?:derby\.)?user(?:name)?
       | password | passwd
       | (?:openai|anthropic|ollama|spring\.ai[.\w]*)?[._-]?api[._-]?key
       | (?:openai|anthropic)_api_key
       | api[._-]?key | secret | token | access[._-]?key)
    \s*[:=]\s*
    (['"]?)(?P<val>[^\s'"#]+)\1
    """,
)
# values that are fine: ${ENV...}, empty, or self-declared placeholders
SAFE_VAL_RX = re.compile(
    r"""(?ix)
    ^\$\{.*\}$ | ^$ | ^null$ | ^none$ | ^false$ | ^true$ |
    (?:your[_-]?|example|placeholder|changeme|dummy|fake|<.*>|x{3,}|\.\.\.|env|secret-ref)
    """,
)


def _candidate_text(tool_input: dict) -> str:
    parts = [tool_input.get("content", ""), tool_input.get("new_string", "")]
    for e in tool_input.get("edits", []) or []:
        parts.append(e.get("new_string", ""))
    return "\n".join(p for p in parts if p)


def main() -> int:
    try:
        data = json.load(sys.stdin)
    except Exception:
        return 0
    tool_input = data.get("tool_input", {}) or {}
    path = tool_input.get("file_path", "") or ""
    if not TARGET_RX.search(path):
        return 0
    for line in _candidate_text(tool_input).splitlines():
        if line.lstrip().startswith(("#", "//", "*")):
            continue
        m = CRED_RX.search(line)
        if m and not SAFE_VAL_RX.search(m.group("val")):
            sys.stderr.write(
                "BLOCKED (no-secrets-guard): a literal credential / API key appears in "
                f"new content for '{path}' (GABI-B08). Use an ${{ENV}} placeholder / "
                "profile instead: DB creds as ${DB_USER:} / ${DB_PASSWORD:}, provider keys "
                "as ${OPENAI_API_KEY} / ${ANTHROPIC_API_KEY} / spring.ai.*.api-key=${...}. "
                "Never commit Derby user/password or model keys; keep them out of logs."
            )
            return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
