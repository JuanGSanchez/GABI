# Instruction: SDD Constitution (GABI)

## Principles Applied
Inherited: P1 (source grounding — phases read predecessor artifacts, not memory or prior context), P2 (determinism — gate conditions are explicit; no ambiguous phase transitions), P3 (systematicity — phase order and gate criteria are enumerated; each phase transition has a named decision point), P4 (consistency — same invariants and gates apply every pipeline run and every session), P6 (self-contained — all gates, invariants, and acceptance criteria stated here), P7 (reference hygiene — citations resolve to CLAUDE.md §Security invariants and `.claude/instructions/java-spring-conventions.md` Rules 1–9; hook names resolve to CLAUDE.md §Hooks), P8 (this block is the P8 expression for this asset), P9 Role Separation (this instruction governs the cross-phase pipeline contract; per-agent instructions govern individual agent execution; no agent owns the constitution), P10 Exit-Status Determinism (Rule 22 requires each agent to report PASS/FAIL for each gate criterion and return EXIT STATUS at phase completion, per CLAUDE.md Operating contract), P11 Programmatic Determinism (harness hooks enforce invariants 1, 3, and 4 deterministically at the tool-use level — plans must not propose workarounds; R18/P11 canonical definition: `repo-enhancer/orchestrator.md` CONVENTIONS, do not restate), P12 Maximal-Effort Completeness (all 9 java-spring-conventions.md Rules and all 6 pre-implement pipeline phase gates are covered; no invariant is partial), P13 Token Economy (rules cite invariant/rule IDs rather than restating them; terse). Engineering Disciplines (R17): canonical definition at `repo-enhancer/orchestrator.md` CONVENTIONS; prompt layer = numbered gated directives with positive/negative examples; context layer = each phase reads only its predecessor artifact, not the full pipeline history; harness layer = gate conditions block phase advancement until the predecessor artifact exists and is approved.

Custom:
- C-SEC — Security-Invariant Authority: the security triad (SQL-identifier validation, credential hygiene, privileged-surface gating — java-spring-conventions.md Rules 1, 4, 3) are elevated to SDD pipeline gates. A change that compiles and passes tests is still WRONG if it breaches one. (The hook trio `sql_identifier_sink_guard.py`, `no_secrets_guard.py`, `privileged_admin_surface_guard.py` enforces the same boundaries mechanically; plans must not propose workarounds.)
- C1 — Pipeline Gate Integrity: every phase must verify its predecessor artifact exists on disk and is approved before proceeding; no phase runs without its input artifact; no phase skips its predecessor regardless of perceived urgency.

Scope: applies to every GABI coding agent (gabi-core-dev, access-dev, rag-dev, test-author, docs-writer, packaging-builder) and the active session when executing SDD pipeline phases (specify / clarify / plan / tasks / analyze / checklist / implement). The per-agent instructions own individual agent execution; this instruction owns the cross-phase contract that binds all of them.

<instructions>
  <context>
    GABI uses the SDD pipeline: specify → clarify → plan → tasks →
    analyze → checklist → implement. This instruction is the project
    constitution — the non-negotiable contract every phase, artifact, and
    agent must satisfy. Its purpose is to keep each pipeline artifact a
    trustworthy handoff to the next phase across sessions, agents, and
    context windows.

    Existing reality this constitution reflects:
    - Architecture: "one core, many faces" (CLAUDE.md §Architecture). One
      headless `LibraryService` interface + `LibraryServiceImpl` (core domain
      logic) powers both a dual REST+MCP access layer (`LibraryMcpTools`,
      `LibraryRestController`) and a RAG layer (`RagService`/`RagServiceImpl`
      via `RagConfig`). Every new capability must propagate through this
      layered architecture; logic belongs in the core service, not in the
      access or RAG layers.
    - 9 invariants (java-spring-conventions.md Rules 1–9) are in force
      throughout every phase.
    - The central security triad: (1) SQL-identifier sinks go through
      `IdentifierValidator.validate()` — never raw concatenation; (2) no
      plaintext credentials in committed files; (3) privileged user-admin
      operations (addUser/deleteUser/GRANT/REVOKE/`derby.user.*`) are never
      on the MCP/REST exposed surface.
    - Hooks enforce invariants mechanically (`sql_identifier_sink_guard.py`,
      `no_secrets_guard.py`, `privileged_admin_surface_guard.py`). Plans
      must not propose workarounds.
    - There is NO orchestrator agent. Orchestration and SDD pipeline ordering
      are coordinated through CLAUDE.md (the project operating contract).
  </context>

  <rules>
    <!-- Phase gate rules (C1: predecessor artifact must exist before each phase begins) -->

    1. Mandatory phase order. Execute phases in this order only:
       specify → clarify → plan → tasks → analyze → checklist → implement.
       No phase begins until its predecessor artifact exists on disk and is
       approved. Under no circumstances write code before spec.md, plan.md,
       and tasks.md exist and are cross-artifact-consistent.

    2. Specify gate. spec.md must state user-facing requirements (what and why)
       with explicit acceptance criteria per requirement. All requirements must
       be unambiguous when the clarify phase closes; none may remain open.

    3. Clarify gate. Every underspecified area in spec.md must be resolved
       through structured questioning before plan begins. Record every
       resolution in spec.md. A plan must not proceed while any requirement
       reads as ambiguous.

    4. Plan gate. plan.md must: (a) assign every new or modified module to its
       subsystem owner (gabi-core-dev / access-dev / rag-dev / test-author /
       docs-writer / packaging-builder); (b) state all data-model changes; (c)
       explicitly confirm that each of the 9 java-spring-conventions.md Rules
       holds under the plan. A plan that proposes bypassing IdentifierValidator
       for an identifier sink (violates Rule 1), embedding a literal credential
       (violates Rule 4), exposing privileged user-admin via `@Tool`/`@McpTool`
       or a REST route (violates Rule 3), or splitting `application.yml` with a
       `---` separator creating a second `spring:` root (violates Rule 5) is
       rejected without modification. Instead, redesign to preserve the
       invariant.

    5. Tasks gate. tasks.md must list dependency-ordered, single-owner work
       items. Each item must name: its owning agent, a done criterion
       (feature-level and verifiable), and a test criterion (the specific
       test(s) that must pass before the item is marked done).

    6. Analyze gate. Before implement begins, a cross-artifact consistency
       check must verify: (a) every spec requirement is covered by at least
       one plan component; (b) every plan component appears in at least one
       task; (c) no task introduces a latent invariant violation. All
       conflicts identified here must be resolved before implement begins;
       implement does not begin with open conflicts.

    7. Checklist gate. A project-specific quality checklist covering
       java-spring-conventions.md Rules 1–9 must be generated and run against
       the implementation. All items must pass, or be documented exceptions
       with a risk assessment, before the feature is declared done.

    <!-- Non-negotiable architecture invariants (carry through every phase) -->

    8. SQL-identifier validation (java-spring-conventions.md Rule 1). Every
       SQL-identifier sink — `derby.user.<name>` SET PROPERTY,
       `derby.database.fullAccessUsers`, GRANT, REVOKE, DROP/CREATE TABLE name
       interpolation — must be routed through
       `core/IdentifierValidator.validate(name, context)`. Under no
       circumstances may a plan add a new identifier sink that accepts raw
       user-supplied names without IdentifierValidator. After touching a sink,
       verify no raw `+ name +` / `String.format` identifier interpolation
       remains. The `sql_identifier_sink_guard.py` hook (PreToolUse) enforces
       this mechanically; plans must not propose workarounds.

    9. Password handling (java-spring-conventions.md Rule 2). Passwords
       interpolated into Derby SET PROPERTY are Derby-string-escaped (double
       every single quote) or rejected by explicit policy — never concatenated
       raw. Values that are DATA use PreparedStatement parameters; only true
       identifiers go through IdentifierValidator. A plan that concatenates a
       password string is rejected.

    10. Privileged surface gating (java-spring-conventions.md Rule 3). The
        privileged Derby user-admin (addUser/deleteUser/GRANT/REVOKE/
        `derby.user.*`) must never appear as a `@Tool`/`@McpTool` in
        `LibraryMcpTools.java` or as a route in `LibraryRestController.java`.
        The exposed surface stays read-only + RAG (`ask`/`reindex`) + `health`
        (14 ops maximum). Any new write-semantic feature stays CLI/admin-gated
        unless a human explicitly authorizes a curated exposed write — STOP and
        confirm before exposing any write. The
        `privileged_admin_surface_guard.py` hook (PreToolUse) enforces this
        mechanically; plans must not propose workarounds.

    11. Credential hygiene (java-spring-conventions.md Rule 4). Credentials
        live only as `${DB_USER:}` / `${DB_PASSWORD:}` / `${OPENAI_API_KEY}` /
        `${ANTHROPIC_API_KEY}` placeholders. Under no circumstances do commits
        include a literal credential in configuration files, Javadoc examples,
        fixtures, or checkpoints. The `no_secrets_guard.py` hook (PreToolUse)
        enforces this; plans must not propose workarounds.

    12. application.yml single spring: key (java-spring-conventions.md Rule 5).
        `application.yml` must have exactly one top-level `spring:` key. Do not
        use a `---` separator to split the document — that creates two profile
        documents and guts startup (GABI-B03). A plan that introduces a second
        top-level `spring:` key is rejected.

    13. RAG graceful degradation (java-spring-conventions.md Rule 6). Exactly
        one `RagService` bean must exist in every configuration — typed
        `NoOpRagService` via `@ConditionalOnMissingBean(RagService.class)` when
        no `EmbeddingModel` is present; `RagServiceImpl` (and its `RagConfig`
        vector-store beans) conditional on
        `@ConditionalOnBean(EmbeddingModel.class)`. A plan that can produce a
        `NoUniqueBeanDefinitionException` or startup failure on a missing model
        is rejected (GABI-B04).

    14. JaCoCo gate scoping (java-spring-conventions.md Rule 7). The JaCoCo
        `check` rule must scope the ≥0.90 LINE/INSTRUCTION limit to the core
        package via element `PACKAGE` or `CLASS` (not `BUNDLE` with
        `<include>core/*</include>`, which silently never fails — GABI-B05). A
        plan that lowers, re-scopes to BUNDLE, or removes the gate to hide a
        shortfall is rejected. A real core shortfall must fail `mvn verify`.

    15. Spring idioms and pinned versions (java-spring-conventions.md Rule 8).
        Constructor injection only (no field `@Autowired`). JDK 17 baseline and
        Derby 10.16.1.1 are pinned — do NOT bump either. Confirm every Spring
        AI / Boot / Derby / JaCoCo API from existing repo usage or emit a
        RESEARCH REQUEST; never invent an API.

    16. In-memory Derby tests (java-spring-conventions.md Rule 9). DB-touching
        tests use `core/InMemoryDerbyConfig` (`jdbc:derby:memory:gabiTest;
        create=true`, `@Primary` test DataSource) + `core/TestSchemaHelper`.
        Seed deterministic fixtures; no live network Derby (`:1527`) and no
        real embedding model backend in tests. A `@SpringBootTest` context-load
        test covers configuration wiring.

    17. No secrets or build artifacts (C-SEC / Rule 4). Under no circumstances
        do commits include credentials, tokens, keys, or build output. All
        commits land on the enhancement branch (enhancement/*), never main or
        master.

    <!-- Cross-cutting standards (apply throughout the pipeline) -->

    18. Constructor injection everywhere. Every new Spring-managed component
        uses constructor injection. Any plan component that introduces field
        `@Autowired` is flagged and must be redesigned before tasks are derived.

    19. Type all public APIs. Every new public method in `LibraryService`,
        `LibraryServiceImpl`, and new helpers must carry explicit parameter and
        return type declarations. A task is not done if its public surface is
        untyped.

    20. Deterministic, offline tests (Rule 9). Tests use deterministic
        in-memory Derby and fixed fixtures — no live network, no real embedding
        model. DB-touching tests use `InMemoryDerbyConfig`; context-wiring
        tests use `@SpringBootTest`.

    21. Coverage gate (Rule 7 / CLAUDE.md §Gate). The gate is `mvn verify`
        with JaCoCo `check` at ≥0.90 LINE/INSTRUCTION on the core package
        (element PACKAGE or CLASS). No plan or task may lower the threshold,
        widen the scope to BUNDLE, or defer tests to a later task. Every
        implement task ships its tests in the same work item. The
        `coverage_gate_reminder.py` hook (PostToolUse) surfaces this.

    <!-- Acceptance gates: what "done" means -->

    22. A feature is "done" only when all of the following hold, reported as
        explicit PASS/FAIL per criterion in the agent's phase completion
        output, followed by an EXIT STATUS payload:
        (a) cross-artifact analysis (Phase 5) is complete and all conflicts
            resolved (analyze gate — PASS);
        (b) project checklist (Phase 6) is run and all items pass
            (checklist gate — PASS);
        (c) `mvn verify` passes with JaCoCo ≥0.90 LINE/INSTRUCTION on core
            (coverage gate — PASS); the JaCoCo rule is element PACKAGE/CLASS
            not BUNDLE (scope gate — PASS);
        (d) all 9 java-spring-conventions.md Rules hold — hooks verify Rules
            1, 3, and 4 mechanically; agents verify Rules 2, 5–9 before
            reporting PASS;
        (e) no literal credential appears in any committed file
            (credential gate — PASS);
        (f) all changes committed on the enhancement branch, never main/master
            (branch gate — PASS).
  </rules>

  <conditional_rules>
    - If a plan touches any SQL-identifier sink (derby.user.* SET PROPERTY,
      GRANT, REVOKE, DROP/CREATE TABLE name interpolation, or any new sink),
      then tasks.md must include an explicit "IdentifierValidator coverage
      re-check" task (owner: gabi-core-dev; done criterion: "no raw + name +
      or String.format identifier interpolation remains in any sink;
      IdentifierValidator.validate called at every entry point") as a
      prerequisite for every other task that modifies that sink (Rule 8).
    - If a plan adds or modifies any MCP/REST-exposed operation, then tasks.md
      must include an "exposed-surface review" task (owner: access-dev; done
      criterion: "no privileged admin operation appears as @Tool/@McpTool or
      REST route; new operation is read-only + RAG semantics, or has explicit
      human authorization") before any other access-layer task is marked done
      (Rule 10).
    - If a plan adds or modifies credential handling or references to
      `configuration.properties` / `application*.yml`, then tasks.md must
      include a "credential hygiene check" task (owner: gabi-core-dev; done
      criterion: "no literal credential in any committed file; all references
      use ${ENV_VAR:} placeholders") before any task that touches those files
      is marked done (Rule 11).
    - If a plan modifies the RAG configuration or any `RagService` bean, then
      tasks.md must include a "RAG degradation test" task (owner: rag-dev;
      done criterion: "exactly one RagService bean in every configuration;
      NoOpRagService active when EmbeddingModel absent; no
      NoUniqueBeanDefinitionException on startup") before any other RAG task
      is marked done (Rule 13).
    - If the coverage gate drops below 0.90 at any point during implement,
      then halt the phase, report the shortfall, and escalate to the user; do
      not advance or merge (Rule 21).
    - If a task requires a new dependency, then the `pom.xml` entry is part of
      the same task, not a follow-up task (Rule 15).
  </conditional_rules>

  <output_format>
    At each phase gate: report PASS or FAIL for each gate criterion (Rules
    2–7). At implement completion: report PASS or FAIL for each acceptance
    gate criterion (Rule 22a–f). End with the agent's EXIT STATUS payload.
  </output_format>

  <examples>
    <example>
      <input>Plan: expose an updateBookTitle(isbn, newTitle) operation via
      the MCP and REST access layers.</input>
      <output>
        Write-semantic operation detected — requires explicit human
        authorization before exposure (Rule 10). plan.md assigns the change
        to gabi-core-dev (LibraryService: add updateBookTitle method;
        LibraryServiceImpl: implement using PreparedStatement for both isbn
        and newTitle as data values) and access-dev (LibraryMcpTools + 
        LibraryRestController: add exposure after authorization confirmed).
        Invariant check: (1) no identifier sink — PASS; (2) no raw password
        concatenation — PASS; (3) write-semantic — STOP: "Exposing
        updateBookTitle via MCP/REST has write semantics. Rule 10 requires
        explicit human authorization before any write is exposed. Confirm to
        proceed? (yes / no)"; (4) no literal credential — PASS; (5–9) N/A
        for this change. Tasks include: exposed-surface review task before
        any access-layer task is marked done.
      </output>
    </example>
    <example>
      <input>Plan: add a getUsersByRole query by building a SQL string
      with the role name concatenated inline using String.format.</input>
      <output>
        Rejected (Rule 8 — SQL-identifier validation). Role names are
        identifiers; raw String.format concatenation opens an injection sink.
        Instead: route the role identifier through
        IdentifierValidator.validate(role, "getUsersByRole") before
        constructing any GRANT/SET PROPERTY/REVOKE statement. Use
        PreparedStatement parameters for all data-column filter values. Reuse
        the one IdentifierValidator; never fork a second. Plan must not
        advance until the sink is redesigned.
      </output>
    </example>
  </examples>
</instructions>

<!--
  SOURCES:
  - User requirement: SDD constitution instruction governing pipeline gates
    and invariants for GABI (Group E, step 17).
  - CLAUDE.md §Security invariants, §Architecture, §Gate/verify commands, §Hooks:
    existing repo reality (security triad, headless-core/access/RAG architecture,
    coverage gate, branch policy, hook roster).
  - .claude/instructions/java-spring-conventions.md Rules 1–9: security invariants
    and coding rules carried through every pipeline phase.
  - asset-metaprompting/references/software-development.md §2: SDD phase
    definitions (specify/clarify/plan/tasks/analyze/checklist/implement)
    and the gate-before-proceed property.
  - templates/claude_instruction.md: structural template.
  - repo-enhancer/orchestrator.md CONVENTIONS R17 (Engineering Disciplines)
    and R18/P11 (Programmatic Determinism): canonical definitions (cited,
    not restated).
-->
