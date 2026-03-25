# Port Kotlin FirInferenceLogger into CFIR

## TL;DR
> **Summary**: Complete and align the repository’s partially wired inference-logging stack with Kotlin upstream `FirInferenceLogger`, expanding the shared generic logger contract, adding a concrete `CfirInferenceLogger`, wiring compiler/runtime activation, and converging CFIR golden outputs toward upstream structure.
> **Deliverables**:
> - Expanded generic inference logger API in `:resolution.common`
> - Concrete `CfirInferenceLogger` + session accessor in `:cfir:resolve`
> - Source-session activation path via compiler configuration / runtime wiring
> - Updated test renderer and converged `*.cfir.inference.txt` golden files
> - Targeted resolve-unit and frontend-golden verification coverage
> **Effort**: Large
> **Parallel**: YES - 3 waves
> **Critical Path**: 1 → 2 → 3 → 4 → 7 → 8 → 10

## Context
### Original Request
Port Kotlin compiler `FirInferenceLogger` to this project, not as a minimal implementation, including required upstream dependencies and respecting existing module/file boundaries.

### Interview Summary
- User wants **full debug capability first**, not a test-only shim.
- User wants an **initial runtime/compiler-facing activation path** included in the first port.
- User prefers **converging output toward Kotlin upstream structure**, even if this causes broad golden-file churn.

### Metis Review (gaps addressed)
- Treat this as **alignment/completion of an existing partial stack**, not a greenfield feature.
- Keep `:resolution.common` generic only; all CFIR-specific structured log elements stay in `:cfir:resolve`.
- Isolate API expansion, CFIR collection, activation wiring, renderer changes, and golden churn into separate reviewable tasks/commits.
- Add explicit enabled/disabled activation verification and degraded-path logging cases.

## Work Objectives
### Core Objective
Make CFIR inference logging structurally and behaviorally align with Kotlin upstream `FirInferenceLogger` while preserving repository module boundaries and producing agent-verifiable runtime and test outputs.

### Deliverables
- A generic inference logger contract in `:resolution.common` that can express upstream-required events without depending on CFIR types.
- A concrete `CfirInferenceLogger` implementation and `CfirSession.inferenceLogger` accessor in `:cfir:resolve`.
- Source-session registration through the existing compiler configuration seam when `CompilerConfiguration.dumpInferenceLogs` is enabled.
- Test infrastructure and golden output updated to the upstream-oriented block/event structure.
- Narrow resolve tests and representative frontend inference-log goldens proving enabled, disabled, happy-path, and degraded-path behavior.

### Definition of Done (verifiable conditions with commands)
- `./gradlew.bat :resolution.common:test` passes, covering the generic logger callback contract.
- `./gradlew.bat :cfir:resolve:test --tests "*Inference*"` passes, covering CFIR logger collection and activation-sensitive behavior.
- `./gradlew.bat :cfir:analysis-tests:test --tests "*Diagnostics*"` passes with updated `*.cfir.inference.txt` outputs.
- `./gradlew.bat :tests:test-infrastructure:test` passes if test-infrastructure-level assertions/rendering tests are added.
- With logging disabled, no `CfirInferenceLogger` session component is observable in the tested source-session path.
- With logging enabled, representative tests produce deterministic `.cfir.inference.txt` files in the converged upstream-oriented structure.

### Must Have
- Upstream-shaped event model: candidate/stage, initial constraints, variable constraints, errors, readiness/fixation records, fix-variable logging, and origin tracing.
- Clean module split:
  - generic API in `:resolution.common`
  - CFIR-specific logger model + collection in `:cfir:resolve`
  - activation in `:cfir:entrypoint` + compiler config / runtime seams
  - rendering/assertion in `:tests:test-infrastructure`
  - golden verification in `:cfir:analysis-tests`
- No-op behavior when inference logging is disabled.
- Explicit documentation of any intentional divergence from upstream rendering where Cangjie semantics differ.

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- Must NOT move CFIR-specific block/item classes into `:resolution.common`.
- Must NOT change inference solver behavior unless a failing logger-driven test proves an API gap that cannot be solved by instrumentation alone.
- Must NOT mix unrelated diagnostics cleanup, formatting cleanup, or broad resolve refactors into this port.
- Must NOT add test-only bypass wiring when the existing configuration/session seam can be used.
- Must NOT update `external/` sources.

## Verification Strategy
> ZERO HUMAN INTERVENTION — all verification is agent-executed.
- Test decision: **tests-after with targeted RED→GREEN slices**; no implementation task is complete without its paired verification.
- QA policy: Every task includes agent-executed scenarios.
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. <3 per wave (except final) = under-splitting.
> Extract shared dependencies as Wave-1 tasks for max parallelism.

Wave 1: API/placement audit, generic logger contract, missing local logger source creation target, activation seam mapping, baseline test inventory

Wave 2: CFIR logger model + session accessor, resolve event wiring, renderer convergence, activation tests

Wave 3: frontend golden migration, targeted cleanup, full verification sweep

### Dependency Matrix (full, all tasks)
| Task | Depends On | Blocks |
|---|---|---|
| 1 | - | 2,3,4,5,6,7,8,9 |
| 2 | 1 | 4,5,6,7 |
| 3 | 1 | 5,6,7 |
| 4 | 1,2 | 6,7,8 |
| 5 | 1,2,3 | 6,7,8 |
| 6 | 4,5 | 8,9 |
| 7 | 2,5 | 8,9 |
| 8 | 6,7 | 9,10 |
| 9 | 6,7,8 | 10 |
| 10 | 8,9 | F1,F2,F3,F4 |

### Agent Dispatch Summary (wave → task count → categories)
- Wave 1 → 5 tasks → `deep`, `quick`, `unspecified-low`
- Wave 2 → 4 tasks → `deep`, `unspecified-high`
- Wave 3 → 1 task → `unspecified-high`

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [ ] 1. Audit and pin the exact local logger surface before code movement

  **What to do**: Verify every existing first-party reference involved in inference logging and turn the current partial stack into an explicit delta table: current generic API, current call sites, current activation path, current handler expectations, current golden files, and missing concrete source files. Confirm that `CfirInferenceLogger` source/accessor is actually absent or misplaced, and document the exact landing path to create it.
  **Must NOT do**: Do not implement behavior changes in this task; do not touch `external/`; do not broaden into unrelated inference cleanup.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: requires cross-module dependency validation and exact file-boundary decisions.
  - Skills: `[]` — No extra skill is needed beyond repository inspection.
  - Omitted: `['brainstorming']` — Design decisions are already locked in this plan.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 2,3,4,5,6,7,8,9 | Blocked By: none

  **References** (executor has NO interview context — be exhaustive):
  - Pattern: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/InferenceLogger.kt:7-35` — Existing generic logger surface that is too thin relative to upstream.
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/ResolutionStageRunner.kt:30-56` — Existing `logCandidate` / `logStage` call sites prove upstream-style hooks are already expected.
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/InferenceComponents.kt:26-69` — Shared inference machinery already receives `session.inferenceLogger`.
  - Pattern: `cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirAbstractSessionFactory.kt:177-189` — Source-session runtime registration seam.
  - Pattern: `compiler/config/src/org/cangnova/cangjie/config/CommonConfigurationKeys.kt:186-195` — Canonical `dumpInferenceLogs` compiler configuration key and property.
  - Pattern: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/frontend/CfirInferenceLogsHandler.kt:23-98` — Current test-side concrete logger contract.
  - Pattern: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/services/EnvironmentConfigurator.kt:60-63` — Directive-to-config mapping proves test activation already uses the runtime seam.
  - Test: `cfir/analysis-tests/testData/diagnostics/type-mismatch/genericReturnTypeInference.cj` — Representative existing golden-driven inference logging case.
  - External: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirInferenceLogger.kt` — Upstream source of truth.

  **Acceptance Criteria** (agent-executable only):
  - [ ] A checked-in implementation note or task-local evidence file lists every current logger-related file, its role, and whether it is present/missing, with the concrete target path for the local `CfirInferenceLogger` source.
  - [ ] The executor can point to a single chosen first-party target file path for `CfirInferenceLogger` and a single chosen file for `CfirSession.inferenceLogger` accessor placement without ambiguity.

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Verify discoverable logger surface
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:compileKotlin --dry-run` only if needed for task context, then use repository search/LSP to enumerate logger references and capture the delta table into `.sisyphus/evidence/task-1-audit.md`.
    Expected: Evidence file lists current references, missing concrete source/accessor files, and selected landing paths.
    Evidence: .sisyphus/evidence/task-1-audit.md

  Scenario: Catch accidental boundary drift
    Tool: Bash
    Steps: Search for any existing CFIR-type references under `resolution.common/src` related to inference logger models.
    Expected: No CFIR-specific logger model types are found in `resolution.common`; evidence notes zero or exact offending paths.
    Evidence: .sisyphus/evidence/task-1-audit-boundaries.md
  ```

  **Commit**: NO | Message: `n/a` | Files: `n/a`

- [ ] 2. Expand the generic `InferenceLogger` contract in `:resolution.common`

  **What to do**: Extend `resolution.common`’s `InferenceLogger` so it can express the upstream-required generic event surface without depending on CFIR types: variable-constraint logging, error logging, fix-variable logging, and origin helpers shaped to support the concrete CFIR logger. Preserve `Dummy` no-op semantics. Add tests in `resolution.common/test` covering callback availability, origin nesting behavior, and no-op behavior.
  **Must NOT do**: Do not introduce renderable block/item models in `resolution.common`; do not import any CFIR symbols; do not change solver algorithms.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: API expansion must be careful and module-pure.
  - Skills: `[]` — Native repo patterns are sufficient.
  - Omitted: `['test-driven-development']` — The plan already embeds TDD structure directly.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 4,5,6,7 | Blocked By: 1

  **References** (executor has NO interview context — be exhaustive):
  - Pattern: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/InferenceLogger.kt:7-35` — Existing base class to expand.
  - Pattern: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirInferenceLogger.kt:153-246` — Upstream overrides define the minimum generic callback surface required from the base class.
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/InferenceComponents.kt:28-60` — Generic callbacks are consumed by `ConstraintIncorporator`, `ConstraintInjector`, and readiness calculators.
  - Pattern: `README.md:80-83` — Notes `ConstraintInjector`, `ConstraintIncorporator`, `ResultTypeResolver`, `VariableReadinessCalculator`, etc. are already migrated into `:resolution.common`.
  - API/Type: `org.cangnova.cangjie.resolve.calls.inference.model.Constraint` — Generic constraint payload type already used here.
  - API/Type: `org.cangnova.cangjie.resolve.calls.inference.model.InitialConstraint` — Existing origin payload type.
  - API/Type: `org.cangnova.cangjie.type.model.TypeVariableMarker` — Generic type-variable identity.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `InferenceLogger` exposes the generic callbacks needed by upstream-shaped CFIR logging, while remaining free of any CFIR imports.
  - [ ] `InferenceLogger.Dummy` remains safe to use when logging is disabled.
  - [ ] New or updated `resolution.common` tests pass and prove origin nesting and no-op callback behavior.

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Generic logger contract passes tests
    Tool: Bash
    Steps: Run `./gradlew.bat :resolution.common:test`
    Expected: PASS; generic logger contract tests succeed with no CFIR dependency violations.
    Evidence: .sisyphus/evidence/task-2-resolution-common-test.txt

  Scenario: Guard against CFIR leakage into generic module
    Tool: Bash
    Steps: Search `resolution.common/src` for `org.cangnova.cangjie.cfir` imports after the change.
    Expected: No matches.
    Evidence: .sisyphus/evidence/task-2-no-cfir-leak.txt
  ```

  **Commit**: YES | Message: `refactor(resolution-common): expand inference logger contract` | Files: `resolution.common/src/**`, `resolution.common/test/**`

- [ ] 3. Create the concrete `CfirInferenceLogger` source and session accessor in `:cfir:resolve`

  **What to do**: Add the missing first-party `CfirInferenceLogger` implementation in the chosen `cfir/resolve/.../inference` package and add the `CfirSession.inferenceLogger` accessor in the chosen session-extension file. The concrete logger must own all structured, renderable CFIR logging elements: top-level blocks, block owners, new-variable items, constraint items, error items, readiness/fixation records, and fix-variable results. Mirror Kotlin upstream structure where semantically possible, while using Cangjie-specific payload rendering when FIR-specific details do not map exactly.
  **Must NOT do**: Do not put the structured model into `:resolution.common`; do not rely on test fixtures to define runtime behavior; do not change output formatting in this task beyond what the concrete model requires.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: this is the core port and requires careful upstream-to-local adaptation.
  - Skills: `[]` — Repository and upstream reference are enough.
  - Omitted: `['writing-plans']` — The implementation plan already exists.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 5,6,7 | Blocked By: 1

  **References** (executor has NO interview context — be exhaustive):
  - Pattern: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirInferenceLogger.kt:25-288` — Canonical concrete logger structure and behaviors to port/adapt.
  - Pattern: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/frontend/CfirInferenceLogsHandler.kt:70-97` — Current repository-visible expectations for `topLevelElements`, `BlockOwner`, and item element names.
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/ResolutionStageRunner.kt:38-55` — Existing call sites requiring concrete logger methods.
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/ConstraintSystemCompleter.kt:96-99` — Existing call completion stage logging.
  - Pattern: `cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirAbstractSessionFactory.kt:181` — Session component registration expects `CfirInferenceLogger::class`.
  - Test: `cfir/analysis-tests/testData/diagnostics/type-mismatch/*.cfir.inference.txt` — Existing golden outputs to be migrated later.

  **Acceptance Criteria** (agent-executable only):
  - [ ] The repository contains a discoverable first-party `CfirInferenceLogger` source file in the chosen `:cfir:resolve` package.
  - [ ] The repository contains a discoverable `CfirSession.inferenceLogger` accessor resolving to the concrete logger component.
  - [ ] The concrete logger can represent candidate owner, unknown owner, constraints, errors, and fix-variable information without test-fixture shims.

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Resolve module compiles with concrete logger present
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:compileKotlin`
    Expected: PASS; unresolved `CfirInferenceLogger` or `inferenceLogger` references are eliminated.
    Evidence: .sisyphus/evidence/task-3-cfir-resolve-compile.txt

  Scenario: Accessor is discoverable from session usage sites
    Tool: Bash
    Steps: Search for `session.inferenceLogger` and compile the resolve module.
    Expected: Existing call sites resolve cleanly to the new accessor.
    Evidence: .sisyphus/evidence/task-3-session-accessor.txt
  ```

  **Commit**: YES | Message: `feat(cfir-resolve): add cfir inference logger model` | Files: `cfir/resolve/src/**`

- [ ] 4. Wire generic inference-engine callbacks to the expanded logger contract

  **What to do**: Update the existing generic inference pipeline to invoke the newly added generic logger callbacks at the correct points: variable-constraint logging, error logging, readiness/fixation logging, and fix-variable logging. Keep all instrumentation behavior-neutral with respect to inference results. Add or update narrow tests proving callback sequencing for representative inference flows.
  **Must NOT do**: Do not alter solver decision rules; do not embed CFIR rendering logic in generic components; do not skip callback invocation simply because some tests still use `Dummy`.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: subtle event-order instrumentation in shared inference code.
  - Skills: `[]` — No special skill beyond careful testing.
  - Omitted: `['systematic-debugging']` — This is planned instrumentation work, not reactive debugging.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 6,7,8 | Blocked By: 1,2

  **References** (executor has NO interview context — be exhaustive):
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/InferenceComponents.kt:28-60` — Current injection points for logger-aware generic components.
  - Pattern: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/InferenceLogger.kt` — Expanded generic callbacks to consume.
  - Pattern: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirInferenceLogger.kt:162-258` — Upstream event categories and sequencing expectations.
  - Test: `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/inference/CfirConstraintSystemImplTest.kt` — Existing resolve-layer constraint-system test area.
  - Test: `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/CfirConstraintSystemFoundationTest.kt` — Existing foundation tests for inference flows.

  **Acceptance Criteria** (agent-executable only):
  - [ ] Shared inference components emit the newly introduced generic logger callbacks at deterministic points.
  - [ ] Existing and new resolve-layer tests show no behavior change except the presence of logging events.
  - [ ] Disabled-mode operation via `InferenceLogger.Dummy` remains side-effect-free.

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Shared inference instrumentation remains green
    Tool: Bash
    Steps: Run `./gradlew.bat :resolution.common:test :cfir:resolve:test --tests "*ConstraintSystem*"`
    Expected: PASS; shared inference tests and resolve constraint-system tests succeed.
    Evidence: .sisyphus/evidence/task-4-shared-instrumentation.txt

  Scenario: Disabled logger remains a no-op
    Tool: Bash
    Steps: Run targeted tests that exercise inference flows with default session/logger setup where logging is disabled.
    Expected: PASS; no new exceptions, ordering failures, or side effects occur.
    Evidence: .sisyphus/evidence/task-4-dummy-noop.txt
  ```

  **Commit**: YES | Message: `refactor(inference): wire expanded logger callbacks` | Files: `resolution.common/src/**`, `cfir/resolve/src/**`, `cfir/resolve/test/**`

- [ ] 5. Complete CFIR resolve event collection and block grouping

  **What to do**: Ensure CFIR resolve emits a coherent upstream-oriented block structure by wiring candidate logging, stage logging, completion logging, and relevant resolve-path events into the new `CfirInferenceLogger`. Normalize how systems are mapped to blocks, how re-entrancy/continuation blocks are created, and when unknown-owner fallback is used. Add focused resolve tests asserting exact block/event ordering for representative candidate processing and call completion flows.
  **Must NOT do**: Do not collapse all logs into a flat list; do not make grouping decisions implicit; do not hide missing owner information by fabricating candidate owners.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: event grouping and sequencing are the heart of the CFIR adaptation.
  - Skills: `[]` — Direct implementation work using repo patterns and upstream reference.
  - Omitted: `['verification-before-completion']` — Task-local verification steps are already specified below.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 6,7,8 | Blocked By: 1,2,3

  **References** (executor has NO interview context — be exhaustive):
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/ResolutionStageRunner.kt:38-55` — Existing candidate/stage logging hook.
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/ConstraintSystemCompleter.kt:96-99` — Existing call completion block name.
  - Pattern: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirInferenceLogger.kt:80-149` — Upstream system-to-block and candidate ownership logic.
  - Pattern: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirInferenceLogger.kt:153-258` — Upstream block item emission and fixation update logic.
  - Test: `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirInferTypeArgumentsTest.kt:56-193` — Existing narrow inference-stage test suite suitable for adding logger-order assertions.
  - Test: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/frontend/CfirInferenceLogsHandler.kt:70-97` — Existing block/item rendering expectation names.

  **Acceptance Criteria** (agent-executable only):
  - [ ] Candidate processing produces deterministic top-level blocks with explicit owner semantics.
  - [ ] Continuation/re-entry into the same system uses a documented and tested block naming/grouping policy.
  - [ ] Resolve-layer tests assert exact block and item ordering for at least one happy path and one degraded/unknown-owner path.

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Candidate and stage grouping are deterministic
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CfirInferTypeArgumentsTest*"`
    Expected: PASS; new assertions on block names, owners, and event order succeed.
    Evidence: .sisyphus/evidence/task-5-candidate-grouping.txt

  Scenario: Unknown-owner fallback remains safe
    Tool: Bash
    Steps: Run targeted resolve tests covering logging before candidate ownership is fully established.
    Expected: PASS; logs use `Unknown` owner rather than crashing or fabricating ownership.
    Evidence: .sisyphus/evidence/task-5-unknown-owner.txt
  ```

  **Commit**: YES | Message: `feat(cfir-resolve): align inference log event grouping` | Files: `cfir/resolve/src/**`, `cfir/resolve/test/**`

- [ ] 6. Align runtime activation through compiler configuration and source-session registration

  **What to do**: Make the runtime/compiler-facing activation path explicit and verified end-to-end: configuration key, `CompilerConfiguration.dumpInferenceLogs` property, session registration in `CfirAbstractSessionFactory`, and any necessary CLI or session-construction plumbing so source sessions can enable or disable the logger consistently outside tests. Add targeted tests proving enabled-mode registration and disabled-mode absence.
  **Must NOT do**: Do not add hidden test-only flags; do not make logger registration unconditional; do not wire activation in library/shared sessions unless the chosen source-session-only policy explicitly requires it.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: this is cross-cutting activation and configuration plumbing.
  - Skills: `[]` — Repository configuration patterns are sufficient.
  - Omitted: `['using-git-worktrees']` — Execution environment choice is outside this plan task.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 8,9 | Blocked By: 4,5

  **References** (executor has NO interview context — be exhaustive):
  - Pattern: `compiler/config/src/org/cangnova/cangjie/config/CommonConfigurationKeys.kt:186-195` — Existing runtime property and configuration key.
  - Pattern: `cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirAbstractSessionFactory.kt:177-182` — Existing conditional registration point.
  - Pattern: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/services/EnvironmentConfigurator.kt:60-63` — Test path already maps directive to config; use as a runtime activation pattern, not a special-case path.
  - Pattern: `external/kotlin/compiler/fir/entrypoint/src/org/jetbrains/kotlin/fir/session/FirAbstractSessionFactory.kt` — Upstream registration model.

  **Acceptance Criteria** (agent-executable only):
  - [ ] Logging-enabled source sessions receive a `CfirInferenceLogger` component.
  - [ ] Logging-disabled source sessions do not expose the component and continue functioning normally.
  - [ ] Runtime activation is driven by the compiler configuration seam rather than test-only overrides.

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Enabled activation registers logger
    Tool: Bash
    Steps: Run the targeted session/configuration test suite or the narrow module tests that construct a source session with `dumpInferenceLogs=true`.
    Expected: PASS; assertions confirm the session component is present.
    Evidence: .sisyphus/evidence/task-6-enabled-activation.txt

  Scenario: Disabled activation keeps logger absent
    Tool: Bash
    Steps: Run the paired tests with `dumpInferenceLogs=false` or default configuration.
    Expected: PASS; assertions confirm no logger component is registered and source-session flows still work.
    Evidence: .sisyphus/evidence/task-6-disabled-activation.txt
  ```

  **Commit**: YES | Message: `feat(cfir-entrypoint): wire inference logger activation` | Files: `compiler/config/src/**`, `cfir/entrypoint/src/**`, `cfir/resolve/test/**` or relevant session tests

- [ ] 7. Converge `CfirInferenceLogsHandler` rendering toward Kotlin upstream structure

  **What to do**: Update `CfirInferenceLogsHandler` and any adjacent test utilities so side-file rendering matches the new upstream-oriented block/event model. Preserve Cangjie-specific values where required, but intentionally move the output format toward Kotlin’s structure, sectioning, and event categories. Document intentional divergences in comments or task evidence if exact parity is impossible.
  **Must NOT do**: Do not keep the old format solely for compatibility; do not encode runtime behavior in the handler; do not require manual golden inspection to validate output.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: renderer changes must stay deterministic and coordinated with golden files.
  - Skills: `[]` — Existing handler patterns are enough.
  - Omitted: `['requesting-code-review']` — Review happens in the final verification wave.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 8,9 | Blocked By: 3,4,5

  **References** (executor has NO interview context — be exhaustive):
  - Pattern: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/frontend/CfirInferenceLogsHandler.kt:20-98` — Current renderer implementation.
  - Pattern: `external/kotlin/compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/frontend/fir/handlers/FirInferenceLogsHandler.kt` — Upstream test handler contract.
  - Pattern: `external/kotlin/compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/utils/inferencelogs/FirInferenceLogsDumper.kt` — Upstream dumper shape and naming ideas.
  - Pattern: `external/kotlin/compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/utils/inferencelogs/MarkdownInferenceLogsDumper.kt` — Upstream richer renderer reference if needed.
  - Pattern: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/frontend/CfirScopeDumpHandler.kt` — Neighboring side-file handler style.

  **Acceptance Criteria** (agent-executable only):
  - [ ] The handler renders the new structured logger model without reflection or test-only ad hoc parsing.
  - [ ] Rendered output is deterministic across repeated runs for the same test data.
  - [ ] Intentional differences from upstream wording/position formatting are documented in task evidence.

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Handler produces deterministic structured output
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:analysis-tests:test --tests "*Diagnostics*"` on a narrow representative subset after updating the handler.
    Expected: PASS or controlled golden diffs only for intended inference-log files.
    Evidence: .sisyphus/evidence/task-7-handler-structure.txt

  Scenario: Document intentional divergence from upstream
    Tool: Bash
    Steps: Compare rendered output shape for one representative case against Kotlin upstream expectations and record exact divergences.
    Expected: Evidence explains any retained Cangjie-specific rendering differences.
    Evidence: .sisyphus/evidence/task-7-divergence-notes.md
  ```

  **Commit**: YES | Message: `test(inference-logs): align cfir log rendering` | Files: `tests/test-infrastructure/testFixtures/**`

- [ ] 8. Add and update representative frontend golden tests for converged inference logs

  **What to do**: Choose a narrow but representative set of analysis testdata that covers generic return inference, expected-type constraints, lambda inference, and conflicting constraints. Update or add `// DUMP_INFERENCE_LOGS` cases and regenerate the corresponding `*.cfir.inference.txt` side files in the new structure. Ensure at least one degraded/error-heavy case is included.
  **Must NOT do**: Do not mass-update unrelated goldens prematurely; do not broaden into all diagnostics suites before the representative set is stable.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: this task balances semantic coverage against reviewable golden churn.
  - Skills: `[]` — Existing test framework is sufficient.
  - Omitted: `['verification-before-completion']` — Explicit commands are already part of the task.

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: 9,10 | Blocked By: 6,7

  **References** (executor has NO interview context — be exhaustive):
  - Test: `cfir/analysis-tests/testData/diagnostics/type-mismatch/genericReturnTypeInference.cj` — Generic return inference.
  - Test: `cfir/analysis-tests/testData/diagnostics/type-mismatch/genericConstraintFromExpectedType.cj` — Expected-type-driven constraints.
  - Test: `cfir/analysis-tests/testData/diagnostics/type-mismatch/lambdaParameterInference.cj` — Lambda parameter inference.
  - Test: `cfir/analysis-tests/testData/diagnostics/type-mismatch/lambdaReturnConstraintMismatch.cj` — Degraded/error-heavy lambda case.
  - Test: `cfir/analysis-tests/testData/diagnostics/type-mismatch/genericArgumentConstraintConflict.cj` — Conflicting constraints.
  - Pattern: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/directives/CfirDiagnosticsDirectives.kt:38-40` — `DUMP_INFERENCE_LOGS` directive.

  **Acceptance Criteria** (agent-executable only):
  - [ ] Representative inference scenarios each have stable `*.cfir.inference.txt` outputs in the new structure.
  - [ ] At least one degraded/error path is covered.
  - [ ] The updated representative set passes under `:cfir:analysis-tests` without manual intervention.

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Representative inference-log goldens pass
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:analysis-tests:test --tests "*CfirAnalysisDiagnosticsTestGenerated*"`
    Expected: PASS for the updated representative cases with new `*.cfir.inference.txt` files.
    Evidence: .sisyphus/evidence/task-8-analysis-goldens.txt

  Scenario: Error-heavy case renders gracefully
    Tool: Bash
    Steps: Execute the targeted diagnostics test covering `lambdaReturnConstraintMismatch` or `genericArgumentConstraintConflict`.
    Expected: PASS; error events are rendered deterministically without crashing the dumper.
    Evidence: .sisyphus/evidence/task-8-error-case.txt
  ```

  **Commit**: YES | Message: `test(cfir-analysis): update representative inference log goldens` | Files: `cfir/analysis-tests/testData/**`, optionally `cfir/analysis-tests/tests-gen/**` only if generator output changes are required

- [ ] 9. Broaden golden migration and close remaining inference-log gaps

  **What to do**: After the representative set is stable, update remaining affected `*.cfir.inference.txt` files and any narrow follow-up tests that fail due to intentional output convergence. Keep this task logic-free unless a failing test reveals a true missing logger event or formatting bug that must be fixed first; if so, fix narrowly and record the reason.
  **Must NOT do**: Do not sneak unrelated code changes into the golden-churn commit; do not rewrite non-inference side files.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: once structure is stable this is mostly disciplined golden regeneration and narrow fallout cleanup.
  - Skills: `[]` — No extra skill needed.
  - Omitted: `['subagent-driven-development']` — The work is now tightly constrained and sequential.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 10 | Blocked By: 6,7

  **References** (executor has NO interview context — be exhaustive):
  - Test pattern: `cfir/analysis-tests/testData/**/*.cfir.inference.txt` — The only side files intended for broad churn in this task.
  - Pattern: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/frontend/CfirInferenceLogsHandler.kt:44-67` — Confirms side-file assertion behavior.
  - Pattern: `README.md:170` — Confirms `CfirInferenceLogsHandler` is already a golden-file based test pattern in this repository.

  **Acceptance Criteria** (agent-executable only):
  - [ ] Remaining affected `*.cfir.inference.txt` files are updated and pass in the owning test suites.
  - [ ] No unrelated logic changes are mixed into the golden migration commit.

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Full inference-log golden migration passes
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:analysis-tests:test`
    Expected: PASS; all affected inference-log side files now match expected outputs.
    Evidence: .sisyphus/evidence/task-9-full-goldens.txt

  Scenario: Ensure golden-only scope
    Tool: Bash
    Steps: Inspect git diff for this task and capture affected paths.
    Expected: Diff is limited to `*.cfir.inference.txt` and narrowly justified follow-up test files if needed.
    Evidence: .sisyphus/evidence/task-9-diff-scope.txt
  ```

  **Commit**: YES | Message: `test(cfir-analysis): converge remaining inference log goldens` | Files: `cfir/analysis-tests/testData/**/*.cfir.inference.txt`

- [ ] 10. Run the integrated verification sweep and record any intentional divergences

  **What to do**: Execute the full targeted verification matrix for generic API, resolve, test infrastructure, and analysis tests. If any retained differences from Kotlin upstream output remain intentional, capture them in a concise evidence note tied to exact file paths and rationale. Ensure each earlier task’s evidence exists and that the final diff respects the commit-strategy boundaries.
  **Must NOT do**: Do not mark the work done without exact command output; do not leave undocumented intentional divergences; do not skip disabled-mode verification.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: final integration verification across modules.
  - Skills: `[]` — Verification is command-driven.
  - Omitted: `['finishing-a-development-branch']` — Branch integration is outside this planning scope.

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: F1,F2,F3,F4 | Blocked By: 8,9

  **References** (executor has NO interview context — be exhaustive):
  - Command target: `:resolution.common:test` — Generic API verification.
  - Command target: `:cfir:resolve:test --tests "*Inference*"` and/or exact logger-related classes — Resolve-layer verification.
  - Command target: `:tests:test-infrastructure:test` — Test infrastructure verification if handler/service tests were added.
  - Command target: `:cfir:analysis-tests:test` — Golden test verification.
  - Evidence pattern: `.sisyphus/evidence/task-*.{txt,md}` — Prior task outputs must exist.

  **Acceptance Criteria** (agent-executable only):
  - [ ] All targeted module test commands pass.
  - [ ] Evidence exists for enabled-mode, disabled-mode, representative goldens, and full golden migration.
  - [ ] Intentional upstream divergences, if any, are documented with exact rationale and file references.

  **QA Scenarios** (MANDATORY — task incomplete without these):
  ```
  Scenario: Full targeted verification matrix passes
    Tool: Bash
    Steps: Run `./gradlew.bat :resolution.common:test :cfir:resolve:test :tests:test-infrastructure:test :cfir:analysis-tests:test`
    Expected: PASS; all targeted modules succeed.
    Evidence: .sisyphus/evidence/task-10-full-verification.txt

  Scenario: Divergence notes are complete
    Tool: Bash
    Steps: Review generated outputs and record any retained divergence from upstream in a markdown note with exact affected tests/files.
    Expected: Evidence file exists and either lists intentional divergences or explicitly states none remain.
    Evidence: .sisyphus/evidence/task-10-divergences.md
  ```

  **Commit**: NO | Message: `n/a` | Files: `n/a`

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.
- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check — deep

## Commit Strategy
- Commit 1: generic logger contract expansion in `:resolution.common`
- Commit 2: `CfirInferenceLogger` model + session accessor in `:cfir:resolve`
- Commit 3: source-session/runtime activation wiring
- Commit 4: test renderer / dumper alignment
- Commit 5: `*.cfir.inference.txt` golden churn only
- Commit 6: behavior-neutral cleanup if required

## Success Criteria
- Upstream-required inference logging events are representable without violating module boundaries.
- CFIR resolve emits candidate/stage/constraint/error/fixation/fix-variable events deterministically.
- Compiler configuration can enable and disable inference logging through the source-session path.
- Updated `*.cfir.inference.txt` outputs are deterministic and reviewable.
- All targeted tests and final review agents pass.
