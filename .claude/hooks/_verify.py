#!/usr/bin/env python3
"""NOT A HOOK. Self-contained verification harness for the GABI .claude/ guards.

Principles: P2 Full Determinism | P11 Programmatic Determinism (deterministic harness utility; not a hook entrypoint).

Run manually: `python .claude/hooks/_verify.py`. py_compiles all four hooks, validates
settings.json, and feeds violating + clean stdin payloads to each guard asserting the
documented exit codes (sql/secrets/admin = exit 2 on violation, 0 when clean; coverage
= reminder, never blocks). Not wired in settings.json — never runs as a PreToolUse hook.
"""
import json, subprocess, sys, py_compile, os

HD = os.path.dirname(os.path.abspath(__file__))
hooks = ["sql_identifier_sink_guard.py","no_secrets_guard.py",
         "privileged_admin_surface_guard.py","coverage_gate_reminder.py"]

# 1. py_compile all
for h in hooks:
    py_compile.compile(os.path.join(HD,h), doraise=True)
    print(f"COMPILE OK  {h}")

# 2. settings.json valid JSON
sj = os.path.join(os.path.dirname(HD),"settings.json")
json.load(open(sj)); print("JSON OK     settings.json")

def run(hook, payload):
    p = subprocess.run([sys.executable, os.path.join(HD,hook)],
                       input=json.dumps(payload), capture_output=True, text=True)
    return p.returncode, (p.stderr.strip() or p.stdout.strip())

cases = [
  # (label, hook, payload, expected_rc)
  ("SQL violating: getName() concat into GRANT", "sql_identifier_sink_guard.py",
   {"tool_input":{"file_path":"src/main/java/sql/users/UserDerby.java",
    "new_string":'s2.executeUpdate("GRANT ALL PRIVILEGES ON TABLE T TO " + newUser.getName());'}}, 2),
  ("SQL violating: derby.user + getString(1)", "sql_identifier_sink_guard.py",
   {"tool_input":{"file_path":"src/main/java/sql/users/UserDerby.java",
    "new_string":'s1.executeUpdate(setProperty + "\'derby.user." + rs1.getString(1) + "\', null)");'}}, 2),
  ("SQL clean: validated identifier", "sql_identifier_sink_guard.py",
   {"tool_input":{"file_path":"src/main/java/sql/users/UserDerby.java",
    "new_string":'String v = IdentifierValidator.validate(newUser.getName(), "username");\n'
                 's2.executeUpdate("GRANT ALL PRIVILEGES ON TABLE T TO " + v);'}}, 0),
  ("SQL clean: PreparedStatement binding", "sql_identifier_sink_guard.py",
   {"tool_input":{"file_path":"src/main/java/core/LibraryServiceImpl.java",
    "new_string":'ps.setString(1, name); ps.executeUpdate();'}}, 0),
  ("SECRET violating: literal yml password", "no_secrets_guard.py",
   {"tool_input":{"file_path":"src/main/resources/application.yml",
    "new_string":'    database-password: dev-local-only'}}, 2),
  ("SECRET violating: anthropic key in java", "no_secrets_guard.py",
   {"tool_input":{"file_path":"src/main/java/rag/RagConfig.java",
    "new_string":'String ANTHROPIC_API_KEY = "sk-ant-abc123def456";'}}, 2),
  ("SECRET clean: env placeholder", "no_secrets_guard.py",
   {"tool_input":{"file_path":"src/main/resources/application.yml",
    "new_string":'    password: ${DB_PASSWORD:}'}}, 0),
  ("ADMIN violating: GRANT into @Tool", "privileged_admin_surface_guard.py",
   {"tool_input":{"file_path":"src/main/java/access/mcp/LibraryMcpTools.java",
    "new_string":'@Tool(name="add_user")\npublic void addUser(String n){ db.executeUpdate("GRANT ALL TO "+n); }'}}, 2),
  ("ADMIN clean: read-only @Tool", "privileged_admin_surface_guard.py",
   {"tool_input":{"file_path":"src/main/java/access/mcp/LibraryMcpTools.java",
    "new_string":'@Tool(name="list_books")\npublic List<BookDto> listBooks(){ return service.listBooks(); }'}}, 0),
  ("COVERAGE reminder on core edit", "coverage_gate_reminder.py",
   {"tool_input":{"file_path":"src/main/java/core/LibraryServiceImpl.java","new_string":"x"}}, 0),
]

fails = 0
for label, hook, payload, exp in cases:
    rc, out = run(hook, payload)
    ok = (rc == exp)
    fails += (not ok)
    print(f"{'PASS' if ok else 'FAIL'}  rc={rc} (exp {exp})  {label}")
    if hook == "coverage_gate_reminder.py":
        print(f"      -> reminder emitted: {'additionalContext' in out}")
print("ALL GREEN" if not fails else f"{fails} FAILURES")
sys.exit(1 if fails else 0)
