#!/usr/bin/env python3
"""PostToolUse hook: remind to run the JaCoCo core coverage gate (>=0.90) (GABI-B05).

Fires AFTER Edit/Write. Non-blocking: when the edited file is under the core package
(src/main/java/core/) or under the test tree (src/test/java/), it emits a reminder
(exit 0 with JSON additionalContext) to run the JaCoCo gate and READ the result before
claiming done. The check rule (pom.xml) enforces LINE/INSTRUCTION ratio >= 0.90 on the
core package, bound to `verify`; GABI-B05 fixes its BUNDLE->PACKAGE scoping so the gate
truly fails the build. Never blocks, never fails the session — a reminder only.
"""
from __future__ import annotations

import json
import re
import sys

TARGET_RX = re.compile(r"src[\\/]main[\\/]java[\\/]core[\\/]|src[\\/]test[\\/]java[\\/]")
GATE_CMD = "mvn -q verify"


def main() -> int:
    try:
        data = json.load(sys.stdin)
    except Exception:
        return 0
    path = (data.get("tool_input", {}) or {}).get("file_path", "") or ""
    if TARGET_RX.search(path):
        print(json.dumps({
            "hookSpecificOutput": {
                "hookEventName": "PostToolUse",
                "additionalContext": (
                    f"Coverage-gate reminder: you edited '{path}'. Before claiming done, run "
                    f"the JaCoCo gate and READ the result: {GATE_CMD} (the core package must be "
                    ">=0.90 LINE/INSTRUCTION; the bound check rule must FAIL verify on a drop — "
                    "GABI-B05)."
                ),
            }
        }))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
