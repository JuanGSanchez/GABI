#!/usr/bin/env python3
"""PreToolUse hook: block raw/concatenated SQL identifiers in *.java (GABI-B01/B02).

Fires on Edit/Write. Reads the Claude Code hook JSON from stdin and BLOCKS (exit 2)
when new Java content builds a SQL string by CONCATENATING a non-literal identifier
expression (e.g. newUser.getName(), rs1.getString(1)) instead of routing it through
core.IdentifierValidator (the [A-Za-z0-9_] whitelist validator).

Ground truth — the legacy sink sql/users/UserDerby.java (addDb / deleteDB) interpolates
user-supplied names into Derby DDL with ZERO IdentifierValidator reference:
    executeUpdate(setProperty + "'derby.user." + newUser.getName() + "', ...")
    executeUpdate("GRANT ALL PRIVILEGES ON TABLE ... TO " + newUser.getName())
    executeUpdate("REVOKE ALL PRIVILEGES ON TABLE ... FROM " + rs1.getString(1))
Any column/table/user identifier must pass IdentifierValidator.validate(...) before
interpolation. Data values use PreparedStatement binding, not this path.

Heuristic (fail-closed on the documented sink shape): a candidate edit is a VIOLATION
when its new content contains a SQL-bearing concatenation of a method-call/field
identifier expression AND does NOT reference IdentifierValidator anywhere in the
candidate text. Plain literal-only SQL and PreparedStatement '?' binding pass.

Non-fatal on its own errors: any parse/IO failure exits 0 so the hook never wedges the
session. Block protocol: exit code 2 + reason on stderr (fed back to the model).
"""
from __future__ import annotations

import json
import re
import sys

JAVA_RX = re.compile(r"\.java$", re.IGNORECASE)
VALIDATOR_RX = re.compile(r"IdentifierValidator")

# A SQL-identifier sink: Derby user-property / GRANT / REVOKE / table-qualifier DDL,
# or a generic SQL keyword, that is CONCATENATED ('+') with a non-literal expression
# ending in a method call or field access (e.g. getName(), getString(1), .name).
SQL_CONTEXT_RX = re.compile(
    r"derby\.user\.|GRANT\s|REVOKE\s|SET\s+PROPERTY|"
    r"\b(?:SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TABLE|FROM|INTO|TO)\b",
    re.IGNORECASE,
)
# "..." + <expr>.something( ... )   or   <expr>.method(...) + "..."
CONCAT_IDENT_RX = re.compile(
    r"""(?x)
    "[^"]*"\s*\+\s*[A-Za-z_]\w*(?:\.\w+)*\s*\(            # "sql" + expr.call(
    | [A-Za-z_]\w*(?:\.\w+)*\s*\([^)]*\)\s*\+\s*"[^"]*"   # expr.call(..) + "sql"
    | "[^"]*"\s*\+\s*[A-Za-z_]\w*\.\w+\b                  # "sql" + expr.field
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
    if not JAVA_RX.search(path):
        return 0
    text = _candidate_text(tool_input)
    if VALIDATOR_RX.search(text):
        return 0  # routed through the whitelist validator — allow
    for line in text.splitlines():
        if SQL_CONTEXT_RX.search(line) and CONCAT_IDENT_RX.search(line):
            sys.stderr.write(
                "BLOCKED (sql-identifier-sink-guard): new content for "
                f"'{path}' concatenates a raw identifier expression into SQL without "
                "IdentifierValidator (GABI-B01/B02; legacy sink sql/users/UserDerby.java "
                "addDb/deleteDB). Route every table/column/user identifier through "
                "core.IdentifierValidator.validate(name, context) BEFORE interpolation; "
                "bind data values with PreparedStatement '?' instead."
            )
            return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
