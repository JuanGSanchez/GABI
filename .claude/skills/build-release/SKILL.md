---
name: build-release
description: >
  Build GABI's jpackage app-image reproducibly from a clean checkout using the checked-in
  packaging config (JDK 17), and confirm the launcher starts — without committing any build
  output. Use this skill to produce the release app-image after GABI-B11's jpackage config
  is in place, or to verify the packaging is reproducible. Pairs with the
  gabi-packaging-builder agent.
principles_applied:
  inherited:
    - P1 — Source-of-Truth Grounding
    - P2 — Full Determinism
    - P3 — Systematicity
    - P4 — Consistency
    - P6 — Self-Containment
    - P7 — Reference Hygiene
  custom:
    - id: C-CLEAN
      name: Clean-Reproducible-Build
      requires: The build runs from checked-in config only (no IDE out/ tree, no absolute jar path), targets JDK 17, and leaves all build output (target/, packaging/ app-image) untracked; success requires the documented command to produce the app-image from a clean package.
      rationale: GABI-B11's acceptance is a from-clean-checkout reproducible build; relying on a machine-local artifact or committing output fails it.
---

# Build Release

Produces the GABI jpackage app-image from checked-in config and verifies reproducibility.

## Workflow

### Step 1: Preconditions
Confirm JDK 17 (`java -version`) and `jpackage` are available. Confirm the checked-in packaging config exists (the GABI-B11 Maven `jpackage` profile / `packaging/` build script + `jpackage.properties`). If no checked-in config exists yet, STOP — request GABI-B11 from gabi-packaging-builder first.

### Step 2: Build the fat-jar
Run `mvn -q -DskipTests package` via Bash. Confirm the repackaged fat-jar `target/gabi-1.0.0-exec.jar` is produced (classifier `exec` per `pom.xml`).

### Step 3: Run the documented jpackage command
Run the single documented command from `packaging/README-packaging.md` (e.g. `mvn -Pjpackage -DskipTests package` or the `packaging/` build script). Use relative paths only; do not reference an IDE `out/` tree or an absolute jar path.

### Step 4: Verify the app-image
Confirm the app-image is produced under `packaging/` and (if testable in-environment) the launcher starts GABI in the intended mode. Capture the produced launcher path.

### Step 5: Confirm no build output committed
Run `git_ops.py --action status` (or `git status` read-only) and confirm `target/` and the `packaging/` app-image are untracked / git-ignored — never staged for commit.

## Output Format
```
BUILD RELEASE — GABI
JDK / jpackage:    <versions>
Checked-in config: present (<files>) | MISSING — request GABI-B11
Fat-jar:           target/gabi-1.0.0-exec.jar — built | FAILED
jpackage command:  <command run>
App-image:         <path> produced | FAILED
Launcher start:    OK | not tested in-env | FAILED
Build output:      untracked (not committed) | RED — staged
VERDICT: OK | RED — <reason>
```

## Examples

### Example 1
**Input:** Build the GABI release app-image.
**Output:**
```
JDK 17 + jpackage present. Checked-in config: packaging/build-app-image.ps1 + jpackage.properties + jpackage Maven profile.
Fat-jar: target/gabi-1.0.0-exec.jar built. Command: mvn -Pjpackage -DskipTests package.
App-image: packaging/bin/GABI/ produced; launcher GABI.exe starts CLI mode.
Build output untracked. VERDICT: OK.
```

## Self-Containment Index
This skill package contains everything needed for its complete usage:
- SKILL.md (this file): workflow, output format, example.

External dependencies (must be available in the repo/environment):
- The GABI-B11 checked-in jpackage config (`pom.xml` jpackage profile / `packaging/` script + `jpackage.properties`).
- `pom.xml` — `spring-boot-maven-plugin` repackage (fat-jar `gabi-1.0.0-exec.jar`).
- `bin/git_ops.py` (system) — status to confirm output is untracked.
- `mvn`, JDK 17 `jpackage` on PATH.

## Sources
- User requirement: a build-release skill (jpackage app-image) (this task).
- Repo ground truth: `docs/BACKLOG.md` GABI-B11; `pom.xml` (repackage, classifier exec, JDK 17, Derby pin); `packaging/` output tree; `docs/agent-operating-doc.md` ("Packaging and build" → `packaging/README-packaging.md`).
