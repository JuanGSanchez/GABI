#!/usr/bin/env python3
"""PreToolUse hook: keep privileged Derby user-admin OFF the exposed MCP/REST surface.

## Principles Applied
P2 Full Determinism | P8 Principles Inheritance | P9 Role Separation | P11 Programmatic Determinism
Role-separation rule: privileged Derby user-admin (addUser/deleteUser/GRANT/REVOKE/derby.user.*)
must stay off the MCP @Tool / REST surface; this hook enforces the admin-vs-operator boundary
at edit time (java-spring-conventions.md rule 3; review D-3/D-4).

Fires on Edit/Write. BLOCKS (exit 2) when new content for a file on the EXPOSED agent
surface introduces a privileged Derby user-admin operation. The exposed surface is the
MCP @Tool facade and the REST controllers:
    access/mcp/LibraryMcpTools.java  (Spring AI @Tool methods)
    access/rest/*Controller.java     (@RestController / @RequestMapping routes)

Ground-truth invariant (LibraryMcpTools Security javadoc + BACKLOG read-only posture,
review D-3/D-4 PASS): user-admin ops — addUser / deleteUser, Derby derby.user.* SET
PROPERTY, GRANT / REVOKE — are intentionally ABSENT from the tool/REST set. A change that
adds any of those to a @Tool method or REST route re-exposes the privileged surface the
campaign closed.

Violation = the candidate file is on the exposed surface AND new content contains a
privileged user-admin token. Non-exposed core/legacy files (sql/users/UserDerby.java,
core/LibraryServiceImpl.java) are out of scope here — they keep user-admin CLI/admin-side.

Non-fatal on its own errors (exit 0). Block protocol: exit 2 + reason on stderr.
"""
from __future__ import annotations

import json
import re
import sys

EXPOSED_RX = re.compile(
    r"access[\\/]mcp[\\/].*\.java$|access[\\/]rest[\\/].*Controller\.java$",
    re.IGNORECASE,
)
PRIVILEGED_RX = re.compile(
    r"derby\.user\.|\bGRANT\b|\bREVOKE\b|fullAccessUsers|"
    r"\badd(?:User|Db)\b|\bdelete(?:User|DB)\b|UserDerby\b|UserDAO\b",
)
# only meaningful if the edit also touches the EXPOSED entrypoint annotations/handlers
SURFACE_HOOK_RX = re.compile(r"@Tool\b|@(?:Get|Post|Put|Delete|Patch|Request)Mapping\b|@RestController\b")


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
    if not EXPOSED_RX.search(path):
        return 0
    text = _candidate_text(tool_input)
    if PRIVILEGED_RX.search(text):
        on_surface = SURFACE_HOOK_RX.search(text) or EXPOSED_RX.search(path)
        if on_surface:
            sys.stderr.write(
                "BLOCKED (privileged-admin-surface-guard): new content for "
                f"'{path}' adds a privileged Derby user-admin op (derby.user.* / GRANT / "
                "REVOKE / addUser / deleteUser) onto the EXPOSED MCP @Tool / REST surface. "
                "User-admin must stay OFF the default agent surface (review D-3/D-4 PASS; "
                "LibraryMcpTools Security invariant). Keep these ops CLI/admin-gated in the "
                "core/legacy path; expose only read-only catalogue/RAG operations."
            )
            return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
