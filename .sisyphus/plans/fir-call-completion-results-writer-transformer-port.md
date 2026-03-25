# CfirCallCompletionResultsWriterTransformer Faithful Port

## TL;DR
> **Summary**: Faithfully port Kotlin FIR's `FirCallCompletionResultsWriterTransformer` into `:cfir:resolve` using the vendored `external/kotlin` snapshot as the source of truth, while restoring the local collaborator contract that `CfirCallCompleter` and `CfirExpressionsResolveTransformer` already assume exists.
> **Deliverables**:
> - Upstream-aligned `CfirCallCompletionResultsWriterTransformer`
> - Restored body-resolve collaborator ownership (`dataFlowAnalyzer`, integer literal/operator approximation, finalized result API)
> - Reconciled call-completion flow in `CfirCallCompleter` / `CfirExpressionsResolveTransformer`
> - White-box resolver tests + source-driven regression tests
> **Effort**: Large
> **Parallel**: YES - 3 waves
> **Critical Path**: 1 → 2 → 3/4/5 → 6 → 7/8 → F1-F4

## Context
### Original Request
- 完整移植 Kotlin 编译器中的 `FirCallCompletionResultsWriterTransformer` 到本项目。
- 禁止最小实现、禁止本地近似替代、缺失依赖必须补齐。

### Interview Summary
- Authoritative upstream baseline: vendored `external/kotlin` snapshot in this repository.
- Success criteria: upstream architecture alignment + dependency completion + repository compilation + targeted regression tests.
- Naming/package strategy: preserve upstream semantic layering and dependency topology while adapting names only as needed to fit local `Cfir*` conventions.
- Scope boundary: do **not** expand into full upstream call-completion subsystem parity unless a missing piece is on the transformer's direct support chain.

### Metis Review (gaps addressed)
- Treat missing collaborator ownership as explicit implementation work, not incidental cleanup.
- Force an upstream branch-disposition audit so no writer branch is silently dropped.
- Reconcile `CfirCallCompleter.completedResultType(candidate)` as a first-class API because current consumers already depend on it.
- Explicitly audit the fidelity conflict between `CfirAppliedCallReferenceFactory` shortcutting and writer-owned final writeback.
- Add proof-backed handling for delegated-property and postponed callable-reference branches: implement if directly reachable, otherwise document and test exclusion.

## Work Objectives
### Core Objective
Port `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt` into the local CFIR resolve pipeline as `CfirCallCompletionResultsWriterTransformer`, with the same architectural seam as upstream: `CallCompleter` creates the writer, the writer performs final completion-result writeback during body resolve, and missing direct collaborators are restored rather than approximated.

### Deliverables
- New file `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt`
- Restored/declared writer-required collaborators on `BodyResolveComponents` and `CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents`
- Restored finalized-result API on `CfirCallCompleter` used by `CfirExpressionsResolveTransformer`
- Resolved interaction between writer-owned finalization and `CfirAppliedCallReferenceFactory`
- Unit tests in `:cfir:resolve:test`
- Source-driven CFIR regression coverage using `:tests:test-infrastructure`

### Definition of Done (verifiable conditions with commands)
- `:cfir:resolve` compiles with no unresolved references to the missing writer or missing completion API.
  - Command: `./gradlew.bat :cfir:resolve:compileKotlin`
- Direct unit tests for call-completion result writing pass.
  - Command: `./gradlew.bat :cfir:resolve:test --tests "*CfirCallCompletionResultsWriterTransformer*"`
- Existing adjacent resolve tests still pass.
  - Command: `./gradlew.bat :cfir:resolve:test --tests "*CfirInferTypeArgumentsTest" --tests "*CfirExtensionsResolveProcessorTest"`
- Source-driven regression tests covering resolved types / inference logs pass.
  - Command: `./gradlew.bat :cfir:resolve:test --tests "*CallCompletion*"`
- No call site remains dependent on placeholder-only APIs or absent collaborators.
  - Command: `./gradlew.bat :cfir:resolve:test`

### Must Have
- Upstream-to-local branch mapping for the writer before coding broad behavior changes
- Explicit restoration of direct dependencies instead of substituting unrelated local utilities
- Writeback ownership centered on `CfirCallCompleter` + `CfirCallCompletionResultsWriterTransformer`
- Tests for finalized result type, lambda/PCLA writeback, integer literal/operator approximation, and exclusion proofs for non-reachable upstream branches

### Must NOT Have
- No “minimal viable” transformer that only writes `resultType`
- No use of unrelated local helpers merely because they compile
- No silent omission of upstream branches; every branch must be marked `ported`, `ported-via-local-equivalent`, `blocked by prerequisite`, or `provably excluded`
- No unrelated body-resolve cleanup outside the writer’s direct support chain
- No API drift hidden behind TODO comments without tests or explicit scope justification

## Verification Strategy
> ZERO HUMAN INTERVENTION — all verification is agent-executed.
- Test decision: **tests-after** using existing JUnit 5 + CFIR diagnostic infrastructure
- QA policy: Every task includes agent-executed scenarios
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. <3 per wave (except final) = under-splitting.
> Extract shared dependencies as Wave-1 tasks for max parallelism.

Wave 1: contract restoration and fidelity audit
- Task 1: upstream branch-disposition matrix + direct dependency map
- Task 2: restore `BodyResolveComponents` / `BodyResolveTransformerComponents` collaborator ownership
- Task 3: restore `CfirCallCompleter` finalized-result API and writer factory contract

Wave 2: faithful writer implementation
- Task 4: port core writer skeleton/helpers/mode/traversal guards
- Task 5: port qualified/callable access writeback and candidate-to-reference conversion
- Task 6: port lambda/PCLA/data-flow/integer-approximation writeback branches and reachability exclusions

Wave 3: integration hardening and tests
- Task 7: reconcile `CfirAppliedCallReferenceFactory` with writer-owned completion semantics
- Task 8: add direct unit tests in `:cfir:resolve:test`
- Task 9: add source-driven regression tests using CFIR diagnostic harness

### Dependency Matrix (full, all tasks)
| Task | Depends On | Blocks |
|---|---|---|
| 1 | none | 4,5,6,7,8,9 |
| 2 | none | 3,4,5,6,7,8 |
| 3 | 2 | 4,5,7,8 |
| 4 | 1,2,3 | 5,6,7,8,9 |
| 5 | 1,4 | 7,8,9 |
| 6 | 1,2,4 | 7,8,9 |
| 7 | 3,5,6 | 8,9 |
| 8 | 1,2,3,4,5,6,7 | 9,F1-F4 |
| 9 | 5,6,7,8 | F1-F4 |

### Agent Dispatch Summary
- Wave 1 → 3 tasks → deep / unspecified-high / quick
- Wave 2 → 3 tasks → deep / unspecified-high / deep
- Wave 3 → 3 tasks → unspecified-high / quick / writing

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [x] 1. Audit upstream writer branches and produce local disposition map

  **What to do**:
  - Use `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt:74-238` and the remainder of that file as the authoritative branch inventory.
  - Create a checked mapping table in code-review notes / implementation notes (not in source comments unless necessary) for every meaningful upstream writer branch:
    - substitution helpers
    - candidate-to-resolved-reference conversion
    - qualified/callable access writeback
    - lambda return-type finalization
    - PCLA postponed calls / callbacks / lambdas
    - integer literal/operator approximation
    - delegated-property mode
    - annotation/collection-literal special handling
    - callable-reference/postponed callable-reference handling
  - For each branch, mark one of: `ported`, `ported via explicit local equivalent`, `blocked by prerequisite task in this plan`, `provably excluded from current local direct chain`.
  - Use this matrix to drive implementation order; do not begin broad branch porting before the matrix exists.

  **Must NOT do**:
  - Do not skip upstream branches because the local code lacks an obvious equivalent.
  - Do not classify a branch as excluded without grep- or compile-backed proof of missing callsites / subsystem reachability.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: requires precise upstream/local architecture comparison and exclusion proofs.
  - Skills: `[]` — no extra skill needed beyond repository and upstream source reading.
  - Omitted: `['test-driven-development']` — this is architecture audit, not coding-first.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 4,5,6,7,8,9 | Blocked By: none

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt:324-336` — local writer construction seam
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt:307-318` — local finalized result type consumer
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAppliedCallReferenceFactory.kt:17-47` — current shortcut applied reference builder to audit against upstream writer ownership
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt` — source of truth
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirCallCompleter.kt:336-350` — upstream construction site

  **Acceptance Criteria**:
  - [ ] Every upstream writer branch has a local disposition recorded before implementation proceeds.
  - [ ] Delegated-property and callable-reference branches are either scheduled for implementation or explicitly excluded with proof.
  - [ ] No remaining “TBD” classifications for direct writer dependencies.

  **QA Scenarios**:
  ```
  Scenario: Branch inventory completed
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:compileKotlin` after audit-related scaffolding changes, ensuring no accidental source breakage while preparing the map.
    Expected: Compilation still succeeds or fails only on known missing writer-chain items already tracked by Tasks 2-6.
    Evidence: .sisyphus/evidence/task-1-branch-audit.txt

  Scenario: Exclusion proof for non-reachable branch
    Tool: Bash
    Steps: Run targeted symbol/content checks used by the implementer (for example repository search or compile references) to prove absence of delegated-property/session callsites before excluding a branch.
    Expected: Evidence file records exact proof for each excluded branch; no exclusion is undocumented.
    Evidence: .sisyphus/evidence/task-1-branch-audit-error.txt
  ```

  **Commit**: YES | Message: `refactor(cfir-resolve): document writer parity matrix` | Files: `cfir/resolve/...`, optional test notes if stored in repo-approved location

- [x] 2. Restore body-resolve collaborator ownership required by call completion

  **What to do**:
  - Update `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/BodyResolveComponents.kt:17-39` so the abstract contract explicitly declares the direct collaborators already required by `CfirCallCompleter`: at minimum `dataFlowAnalyzer` and `integerLiteralAndOperatorApproximationTransformer`; include `samResolver` only if final writer constructor needs it locally.
  - Update `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAbstractBodyResolveTransformer.kt:51-146` to own and lazily construct the restored collaborators in the same architectural seam as upstream `BodyResolveTransformerComponents`.
  - If a concrete `CfirDataFlowAnalyzer` implementation does not yet exist, add the direct support implementation now instead of leaving `CfirDataFlowAnalyzerContext` (`.../CfirDataFlowAnalyzerContext.kt:3-20`) as the only data-flow artifact.
  - Add any missing imports / Gradle dependencies in `cfir/resolve/build.gradle.kts` only when genuinely required by the restored direct chain.

  **Must NOT do**:
  - Do not inject unrelated utilities as substitutes for the missing collaborators.
  - Do not leave abstract contract holes while wiring only concrete implementation fields.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: cross-file API restoration with architectural invariants.
  - Skills: `[]`
  - Omitted: `['writing-plans']` — execution task, not planning.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 3,4,6,8 | Blocked By: none

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/BodyResolveComponents.kt:30-38` — commented drift points to restore
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAbstractBodyResolveTransformer.kt:70-109` — local collaborator ownership seam
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt:328-333` — current collaborator consumers
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/body/resolve/FirAbstractBodyResolveTransformer.kt` — ownership model to mirror
  - Test: `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/calls/CallResolutionTestFixtures.kt:38-55` — existing stub context patterns that may need extension

  **Acceptance Criteria**:
  - [ ] `BodyResolveComponents` declares all collaborators consumed by the local writer/completer path.
  - [ ] `BodyResolveTransformerComponents` instantiates those collaborators without unresolved references.
  - [ ] `CfirCallCompleter` no longer depends on undeclared abstract members.

  **QA Scenarios**:
  ```
  Scenario: Collaborator ownership compiles
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:compileKotlin`
    Expected: No unresolved-reference errors for `dataFlowAnalyzer`, `integerLiteralAndOperatorApproximationTransformer`, or related body-resolve properties.
    Evidence: .sisyphus/evidence/task-2-collaborators.txt

  Scenario: Contract restoration catches partial wiring
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CfirExtensionsResolveProcessorTest"`
    Expected: Existing processor/phase tests continue to pass; failures indicate body-resolve contract changes leaked into unrelated resolve wiring.
    Evidence: .sisyphus/evidence/task-2-collaborators-error.txt
  ```

  **Commit**: YES | Message: `refactor(cfir-resolve): restore call-completion collaborators` | Files: `cfir/resolve/src/.../BodyResolveComponents.kt`, `cfir/resolve/src/.../CfirAbstractBodyResolveTransformer.kt`, direct collaborator files, `cfir/resolve/build.gradle.kts` if needed

- [x] 3. Restore CfirCallCompleter finalized-result API and writer construction contract

  **What to do**:
  - Update `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt` so it exposes a finalized-result API consumed by existing call sites: restore `completedResultType(candidate)` or an exact replacement **and** update all consumers in the same commit.
  - Ensure `createCompletionResultsWriter(...)` matches the final local direct dependency chain and constructor shape of the ported writer.
  - Preserve current completion entry points (`completeCall`, partial completion path, PCLA-top-level-lambda synthetic call path) while routing all final writeback through the writer rather than ad hoc local shortcuts.

  **Must NOT do**:
  - Do not remove `completedResultType` call sites without replacing them with an equally explicit finalized-result contract.
  - Do not couple `CfirExpressionsResolveTransformer` directly to candidate internals that should remain encapsulated in the completer/writer layer.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: localized API restoration once Task 2 lands.
  - Skills: `[]`
  - Omitted: `['systematic-debugging']` — use only if compile/test failures reveal deeper drift.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 4,5,7,8 | Blocked By: 2

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt:70-133` — current completion flow
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt:324-336` — writer factory site
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt:313-318,405-408,935-937` — existing consumers of missing finalized result API
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirCallCompleter.kt` — contract model

  **Acceptance Criteria**:
  - [ ] All `completedResultType(candidate)` consumer sites compile against the restored API.
  - [ ] `CfirCallCompleter` writer construction uses only declared collaborators.
  - [ ] No direct consumer bypasses the completer/writer layer for final result types.

  **QA Scenarios**:
  ```
  Scenario: Finalized-result API compiles end-to-end
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:compileKotlin`
    Expected: `CfirExpressionsResolveTransformer` and `CfirCallCompleter` compile together with no missing `completedResultType` errors.
    Evidence: .sisyphus/evidence/task-3-completer-api.txt

  Scenario: Generic inference behavior remains intact
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CfirInferTypeArgumentsTest"`
    Expected: Existing inference-stage tests still pass after API restoration.
    Evidence: .sisyphus/evidence/task-3-completer-api-error.txt
  ```

  **Commit**: YES | Message: `refactor(cfir-resolve): restore call completer final result api` | Files: `cfir/resolve/src/.../CfirCallCompleter.kt`, `cfir/resolve/src/.../CfirExpressionsResolveTransformer.kt`

- [x] 4. Port the writer skeleton, constructor, helper primitives, and traversal guard from upstream

  **What to do**:
  - Create `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt`.
  - Port the upstream class structure from `FirCallCompletionResultsWriterTransformer.kt:74+` into local `Cfir*` equivalents:
    - constructor dependency list
    - `Mode` enum
    - substitution helpers (`finallySubstituteOrNull`, `finallySubstituteOrSelf`, local equivalents)
    - writer-local toggles for annotation / array-of special handling if branch audit marks them reachable
    - traversal guard equivalent to upstream `transformElement(...)` declaration skip
  - Make the writer extend the correct local transformer base so it runs at the local body-resolve phase corresponding to upstream `IMPLICIT_TYPES_BODY_RESOLVE`.
  - Preserve the architectural seam: short-lived transformer created by `CfirCallCompleter`, not session-registered singleton.

  **Must NOT do**:
  - Do not start with a stub that only compiles while leaving most branches unimplemented.
  - Do not place the writer in an unrelated package; keep it adjacent to `CfirCallCompleter` unless Task 1 proves another location is required.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: the core faithful port requires systematic upstream-to-local symbol mapping.
  - Skills: `[]`
  - Omitted: `['codeagent']` — avoid opaque bulk generation; this needs targeted architecture work.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 5,6,7,8,9 | Blocked By: 1,2,3

  **References**:
  - Pattern: `cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/visitors/CfirTransformer.kt` — generated transformer APIs
  - Pattern: `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/visitors/CfirDefaultTransformer.kt` — local default transformer conventions
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt:324-336` — constructor call site
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt:74-133,1426-1442` — class skeleton and traversal guard

  **Acceptance Criteria**:
  - [ ] The new writer class exists in the planned location and compiles.
  - [ ] Constructor dependencies align exactly with the final local collaborator contract.
  - [ ] The writer skips descending into declarations unless a specific audited branch requires otherwise.

  **QA Scenarios**:
  ```
  Scenario: Writer skeleton compiles
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:compileKotlin`
    Expected: New writer file compiles and is reachable from `CfirCallCompleter.createCompletionResultsWriter(...)`.
    Evidence: .sisyphus/evidence/task-4-writer-skeleton.txt

  Scenario: Traversal guard prevents declaration regressions
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CfirCallCompletionResultsWriterTransformer*"` once initial unit tests are added, or a targeted compile/test smoke run if tests are not yet present.
    Expected: No declaration-recursion regression; failures must point to explicit branch handling, not uncontrolled traversal.
    Evidence: .sisyphus/evidence/task-4-writer-skeleton-error.txt
  ```

  **Commit**: YES | Message: `feat(cfir-resolve): add call completion writer transformer skeleton` | Files: `cfir/resolve/src/.../CfirCallCompletionResultsWriterTransformer.kt`, `cfir/resolve/src/.../CfirCallCompleter.kt`

- [x] 5. Port candidate-to-reference conversion and qualified/callable access writeback branches

  **What to do**:
  - Port the upstream branches that transform candidate-bearing references into final resolved or error references.
  - Implement the local equivalents for:
    - successful candidate → resolved named/applied callable reference
    - inapplicable/contradictory candidate → error reference
    - dispatch receiver / extension receiver / explicit receiver replacement where local CFIR supports them
    - type arguments and final result type writeback onto qualified/callable access expressions
  - Update `CfirExpressionsResolveTransformer` integrations only where necessary to consume the writer/completer results rather than duplicating writeback logic.

  **Must NOT do**:
  - Do not leave `CfirAppliedCallReferenceFactory` as the authoritative source of final substituted return types if the writer is meant to own that responsibility.
  - Do not collapse candidate error states into a generic unresolved-name error when upstream keeps stronger diagnostics.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: multiple CFIR expression/reference types and candidate states must stay consistent.
  - Skills: `[]`
  - Omitted: `['subagent-driven-development']` — keep this as a focused single implementation task.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 7,8,9 | Blocked By: 1,4

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt:307-318,399-409` — local call/callable access result application sites
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAppliedCallReferenceFactory.kt:17-47` — current reference builder to reconcile
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt:135-215,1393-1423` — core qualified-access preparation and resolved/error reference conversion

  **Acceptance Criteria**:
  - [ ] Successful and erroneous candidate branches write back the correct local reference shape.
  - [ ] Final result types on qualified/callable access nodes come from writer/completer finalization, not ad hoc duplication.
  - [ ] Existing call-resolution paths compile without fallback to placeholder result types like `ConeErrorType("unresolved return type")` on successful candidates.

  **QA Scenarios**:
  ```
  Scenario: Successful candidate writes final result type
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CfirCallCompletionResultsWriterTransformer*successful*"`
    Expected: Test verifies successful candidate becomes a resolved applied callable reference with final substituted return type.
    Evidence: .sisyphus/evidence/task-5-qualified-writeback.txt

  Scenario: Inapplicable candidate writes error reference
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CfirCallCompletionResultsWriterTransformer*error*"`
    Expected: Test verifies candidate contradiction/inapplicability produces the correct error reference and does not masquerade as success.
    Evidence: .sisyphus/evidence/task-5-qualified-writeback-error.txt
  ```

  **Commit**: YES | Message: `feat(cfir-resolve): port call completion writeback branches` | Files: `cfir/resolve/src/.../CfirCallCompletionResultsWriterTransformer.kt`, related reference/expression files

- [x] 6. Port lambda, PCLA, data-flow, integer-approximation, and reachability-sensitive branches

  **What to do**:
  - Port upstream lambda/body-completion branches that rely on return-expression collection, expected type propagation, and final return type materialization.
  - Restore or add any missing direct support required for the ported branch set:
    - data-flow return-expression collection API
    - integer literal/operator approximation transformer
    - post-PCLA type-variable cleanup or explicit local equivalent
    - SAM-related handling if the local writer constructor includes `samResolver`
  - For delegated-property and postponed callable-reference branches:
    - implement them if Task 1 proves the branch is on the local direct chain,
    - otherwise keep explicit enum/branch structure and add proof-backed exclusion tests so the omission is documented rather than silent.

  **Must NOT do**:
  - Do not degrade PCLA/lambda handling into simple AST tail-expression scanning if the branch requires CFG/data-flow semantics.
  - Do not omit integer literal/operator approximation merely because the local type model is simpler; prove exclusion or port the collaborator.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: this is the highest-risk semantic portion of the port.
  - Skills: `[]`
  - Omitted: `['systematic-debugging']` — only load if regression failures appear during execution.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 7,8,9 | Blocked By: 1,2,4

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt:348-451` — local lambda/PCLA entry points already in use
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDataFlowAnalyzerContext.kt:3-20` — current data-flow skeleton that may require direct support restoration
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt:217-237,1015-1096,165-182` — PCLA tasks, lambda finalization, integer receiver approximation
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirCallCompleter.kt:466-490` — lambda PCLA registration flow

  **Acceptance Criteria**:
  - [ ] Lambda completion writes finalized parameter/return types using the restored direct chain.
  - [ ] PCLA-related callbacks/lambdas/postponed calls are either executed or explicitly excluded with proof and tests.
  - [ ] Integer literal/operator approximation behavior is implemented or excluded with proof.
  - [ ] Delegated-property mode is either supported on the direct chain or explicitly preserved as a non-reachable branch with tests proving no local invocation path.

  **QA Scenarios**:
  ```
  Scenario: Lambda/PCLA writeback succeeds
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CfirCallCompletionResultsWriterTransformer*lambda*"`
    Expected: Tests confirm lambda parameter and return types are finalized after completion and PCLA-related callbacks are executed when reachable.
    Evidence: .sisyphus/evidence/task-6-lambda-pcla.txt

  Scenario: Integer-approximation or branch exclusion is verified
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CfirCallCompletionResultsWriterTransformer*integer*" --tests "*CfirCallCompletionResultsWriterTransformer*delegated*"`
    Expected: Either semantic approximation behavior is asserted, or proof-backed exclusion tests pass and document why the branch is unreachable locally.
    Evidence: .sisyphus/evidence/task-6-lambda-pcla-error.txt
  ```

  **Commit**: YES | Message: `feat(cfir-resolve): port lambda and pcla completion writeback` | Files: writer file plus direct collaborator files/tests

- [x] 7. Reconcile applied call reference factory with writer-owned completion semantics

  **What to do**:
  - Audit `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAppliedCallReferenceFactory.kt:17-47`, especially `candidate.substitutedReturnType()` at lines 37-38.
  - Decide and implement one authoritative rule: either the writer owns final substituted return type publication and the factory becomes a thin wrapper, or the factory remains a wrapper that consumes finalized data already prepared by the writer/completer.
  - Remove duplicate or conflicting final-type calculation so `CfirExpressionsResolveTransformer` sees one authoritative completion result.

  **Must NOT do**:
  - Do not leave two different sources of truth for final substituted return type.
  - Do not preserve the current shortcut if it bypasses newly ported writer branches.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: subtle ownership cleanup that directly affects semantic correctness.
  - Skills: `[]`
  - Omitted: `['receiving-code-review']` — use only after implementation review feedback arrives.

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: 8,9 | Blocked By: 3,5,6

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAppliedCallReferenceFactory.kt:17-47` — current shortcut
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt:314-317,405-408` — downstream consumers
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt:135-215` — authoritative writeback owner

  **Acceptance Criteria**:
  - [ ] Only one authoritative source remains for final substituted return types.
  - [ ] Applied callable reference construction no longer bypasses writer-owned finalization semantics.
  - [ ] Existing expression resolve paths still compile and tests observe consistent result types.

  **QA Scenarios**:
  ```
  Scenario: Reference factory uses one source of truth
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CfirCallCompletionResultsWriterTransformer*appliedReference*"`
    Expected: Test confirms applied callable references expose the same final substituted return type as expression result writeback.
    Evidence: .sisyphus/evidence/task-7-reference-factory.txt

  Scenario: No duplicate finalization path remains
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:compileKotlin`
    Expected: No compile-time or test evidence of duplicate/competing final type sources.
    Evidence: .sisyphus/evidence/task-7-reference-factory-error.txt
  ```

  **Commit**: YES | Message: `refactor(cfir-resolve): unify applied call reference finalization` | Files: `cfir/resolve/src/.../CfirAppliedCallReferenceFactory.kt`, writer/completer consumers

- [ ] 8. Add direct unit tests for completion-result writing in :cfir:resolve

  **What to do**:
  - Add focused JUnit 5 tests under `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/inference/` or a sibling package matching the final implementation location.
  - Reuse and extend `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/calls/CallResolutionTestFixtures.kt:38-168` for candidates, symbols, and simple expressions.
  - Cover at minimum:
    - successful candidate finalized result type
    - contradictory/inapplicable candidate error writeback
    - lambda/PCLA result writeback (or proof-backed exclusion)
    - integer literal/operator approximation branch (or proof-backed exclusion)
    - delegated-property mode reachability decision
  - Prefer explicit assertions over snapshot-only tests for these white-box semantics.

  **Must NOT do**:
  - Do not rely solely on compile success as verification.
  - Do not add tests that assert placeholder behavior the port is supposed to eliminate.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: localized test additions using existing fixtures.
  - Skills: `[]`
  - Omitted: `['test-driven-development']` — project is already in tests-after mode per confirmed strategy.

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: 9 | Blocked By: 1,2,3,4,5,6,7

  **References**:
  - Test: `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/calls/CallResolutionTestFixtures.kt:38-168` — fixture factory
  - Test: `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirInferTypeArgumentsTest.kt:41-168` — direct unit-test style to mimic
  - Test: `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/transformers/CfirExtensionsResolveProcessorTest.kt:19-84` — existing resolve transformer unit-test style

  **Acceptance Criteria**:
  - [ ] New unit tests exist for each reachable branch family.
  - [ ] Excluded branch families have proof-backed tests rather than silent omission.
  - [ ] Targeted writer tests pass under `:cfir:resolve:test`.

  **QA Scenarios**:
  ```
  Scenario: Targeted writer unit tests pass
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CfirCallCompletionResultsWriterTransformer*"`
    Expected: All new direct writer tests pass.
    Evidence: .sisyphus/evidence/task-8-unit-tests.txt

  Scenario: Existing adjacent resolve tests remain green
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CfirInferTypeArgumentsTest" --tests "*CfirExtensionsResolveProcessorTest"`
    Expected: No regressions in adjacent inference/transformer tests.
    Evidence: .sisyphus/evidence/task-8-unit-tests-error.txt
  ```

  **Commit**: YES | Message: `test(cfir-resolve): cover call completion writer semantics` | Files: `cfir/resolve/test/...`

- [ ] 9. Add source-driven CFIR regression tests for externally observable completion behavior

  **What to do**:
  - Add a small source-driven regression suite using the existing diagnostic harness from `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/config/BaseDiagnosticConfiguration.kt:41-108`.
  - Verify observable outcomes via resolved types, inference logs, and diagnostics rather than internal implementation details.
  - Include 2-4 focused scenarios:
    - generic call whose final substituted type is visible in resolved types
    - lambda completion with expected return type propagation
    - integer literal/operator-sensitive completion result (if reachable)
    - proof-backed excluded branch scenario if delegated-property/callable-reference path is intentionally unreachable

  **Must NOT do**:
  - Do not create a giant new generated resolve suite for this task; keep the regression layer minimal and targeted.
  - Do not skip resolved-type or inference-log assertions when they are the only observable proof of correct writer behavior.

  **Recommended Agent Profile**:
  - Category: `writing` — Reason: testdata-driven regression coverage with directives and golden files.
  - Skills: `[]`
  - Omitted: `['dispatching-parallel-agents']` — keep fixtures/testdata coherent in one task.

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: F1-F4 | Blocked By: 5,6,7,8

  **References**:
  - Test infra: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/config/BaseDiagnosticConfiguration.kt:41-108` — CFIR diagnostic harness
  - Test infra: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/directives/CfirDiagnosticsDirectives.kt` — directives for inference logs and resolved types
  - Build: `cfir/resolve/build.gradle.kts:23-25` — existing resolve test dependencies

  **Acceptance Criteria**:
  - [ ] Source-driven tests verify externally visible completion behavior changed by the writer port.
  - [ ] Resolved type / inference log assertions cover the intended regression scenarios.
  - [ ] The regression suite passes under the standard `:cfir:resolve:test` task.

  **QA Scenarios**:
  ```
  Scenario: Source-driven completion regressions pass
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CallCompletion*"`
    Expected: New source-driven tests pass and produce expected resolved-type / inference-log outputs.
    Evidence: .sisyphus/evidence/task-9-source-regressions.txt

  Scenario: Full resolve module test pass after regression additions
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test`
    Expected: Entire resolve module test suite passes without new flaky failures.
    Evidence: .sisyphus/evidence/task-9-source-regressions-error.txt
  ```

  **Commit**: YES | Message: `test(cfir-resolve): add call completion regression coverage` | Files: resolve tests + testdata files

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.
- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check — deep

## Commit Strategy
- Commit 1: restore direct collaborator ownership and abstract contract
- Commit 2: restore `CfirCallCompleter` finalized-result API
- Commit 3: add faithful writer skeleton and core branch port
- Commit 4: complete lambda/PCLA/integer/delegated-path reachability work
- Commit 5: add direct unit tests and source-driven regressions
- Commit messages should use local style, e.g. `feat(cfir-resolve): ...`, `refactor(cfir-resolve): ...`, `test(cfir-resolve): ...`

## Success Criteria
- The repository contains a real `CfirCallCompletionResultsWriterTransformer` at the correct architectural seam.
- `CfirCallCompleter` and `CfirExpressionsResolveTransformer` no longer depend on missing or placeholder completion APIs.
- All direct upstream writer dependencies required by the local direct chain are restored or explicitly proven unreachable.
- No branch was silently dropped; exclusions are documented and covered by tests.
- `:cfir:resolve:compileKotlin` and `:cfir:resolve:test` pass.
