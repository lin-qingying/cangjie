# CandidateCollector and Resolver Skeleton Architecture Port

## TL;DR
> **Summary**: Complete the missing `CandidateCollector` migration as part of a full resolver-skeleton alignment with Kotlin FIR/new-inference architecture, while explicitly preserving Cangjie-specific lookup precedence and removing Kotlin-only semantics that do not belong in the language.
> **Deliverables**:
> - Upstream-aligned candidate collection subsystem in `:cfir:resolve`
> - Completed tower-feeding + candidate factory/state + stage pipeline + overload reduction integration
> - Cangjie-specific precedence rules for `extend` preserved and tested
> - Resolver unit coverage plus file-driven diagnostics regressions
> **Effort**: XL
> **Parallel**: YES - 4 waves
> **Critical Path**: 1 -> 2 -> 3/4/5 -> 6/7/8 -> 9/10/11 -> 12/13 -> F1-F4

## Context
### Original Request
- 完整移植 Kotlin 编译器中的 `CandidateCollector` 和其上游依赖声明到当前仓库。
- 不允许最小实现；需要完整对齐 Kotlin 编译器架构，而不是本地近似替代。
- 对本仓库中可能已存在但实现不正确的相关声明一并审计和修正。
- 删除 Kotlin 专属、且不属于仓颉语言语义的部分；保留通用编译器架构实现。

### Interview Summary
- 规划边界已确认：**扩展到整段解析骨架**，不是只补一个缺失类。
- 测试策略已确认：**tests-after**。
- 允许保留 Kotlin 风格的解析分层与生命周期，但必须显式改写/删除 Kotlin-only 语义。
- 需要把仓颉 `extend` 查找优先级视为硬性语言约束，而不是上游默认行为的附属结果。

### Metis Review (gaps addressed)
- 不把“类名/包结构对齐上游”误判为架构完成；必须以端到端解析行为为验收标准。
- 计划必须先核实 `CfirCandidateCollector` 是否确实缺失/隐藏，再决定“新增文件”还是“替换错误实现”。
- 必须把 `CfirMapArguments` / `CfirCheckArguments` / `CfirInferTypeArguments` 的 TODO 落地写入计划，否则 collector 只是空壳。
- 必须把 `extend` 优先级、`ResolvedWithErrors` 停止条件、enum-constructor fallback、legacy API 兼容边界写成显式验收条件。
- 必须防止范围膨胀到 Kotlin callable references、SAM、context receivers 等未被仓颉采纳的特性。

## Work Objectives
### Core Objective
在 `:cfir:resolve` 中完成一套可工作的、与 Kotlin FIR/new-inference 架构对齐的候选收集与解析骨架：包括 tower feeding、candidate state/factory、collector grouping/early-stop、resolution stage pipeline、overload reduction、以及与 `CfirCallCompleter` / `resolution.common` 的接缝；同时保留仓颉语言自己的 `extend` 层级、enum constructor fallback、synthetic-call expected-type 行为，并剔除 Kotlin-only 语义。

### Deliverables
- `cfir/resolve/.../calls/candidate/CfirCandidateCollector.kt` 或等价 first-party collector 实现落地并接入真实调用链
- `cfir/resolve/.../calls/candidate/` 下的 candidate lifecycle、factory/auxiliary declarations 与 Kotlin source-of-truth 对齐
- `cfir/resolve/.../body/CfirTowerResolver.kt` 的 tower feeding 责任收束完成
- `cfir/resolve/.../calls/stages/CfirMapArguments.kt`、`CfirCheckArguments.kt`、`CfirInferTypeArguments.kt` 不再是 TODO
- `cfir/resolve/.../calls/overloads/` 与 collector 停止条件、best-candidate 结果达成一致
- `cfir/resolve:test` 中的 collector/stage/conflict 单测
- `cfir/analysis-tests/testData/diagnostics/...` 中的 file-driven 回归测试

### Definition of Done (verifiable conditions with commands)
- `:cfir:resolve` 可编译，且 `CfirTowerResolver` 不再依赖缺失/悬空 collector。
  - Command: `./gradlew.bat :cfir:resolve:compileKotlin`
- 候选收集、分组停止、stage pipeline、overload reduction 单元测试通过。
  - Command: `./gradlew.bat :cfir:resolve:test --tests "*CandidateCollector*" --tests "*Tower*" --tests "*Overload*" --tests "*InferTypeArguments*"`
- `extend`、local/import/package/member 竞争的 file-driven diagnostics 回归通过。
  - Command: `./gradlew.bat :cfir:analysis-tests:test --tests "*CfirAnalysisDiagnosticsTestGenerated"`
- `enum constructor fallback`、`ResolvedWithErrors`、ambiguity 路径有明确回归测试并通过。
  - Command: `./gradlew.bat :cfir:resolve:test --tests "*CallResolver*" --tests "*Enum*"`
- 无 Kotlin-only unsupported branches 被静默引入。
  - Command: `./gradlew.bat :cfir:resolve:test`

### Must Have
- 上游基线固定为仓库内 `external/kotlin`，并对每个移植概念标记为 `adopt` / `adapt` / `omit`
- 先验证 first-party 中 collector 的真实现状，再决定新增/替换策略
- `CfirMapArguments`、`CfirCheckArguments`、`CfirInferTypeArguments` 必须纳入同一计划，不允许留 TODO
- `extend` 必须在 tower grouping、collector early-stop、overload tie-break 中被显式验证
- 兼容现有 `LegacySuccess` / `LegacyAmbiguity` 风格 API，除非计划中安排有证据支持的渐进替换

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- 不要只复制 Kotlin 文件名和包结构就宣称“对齐架构”
- 不要引入 Kotlin SAM conversion、context receivers、operator/infix policy、companion-object duality 等仓颉未采纳语义
- 不要把 `resolution.common` 扩张成额外的大规模类型系统迁移项目，除非出现编译阻断证据
- 不要覆盖现有仓颉特化规则（`extend`、enum fallback、synthetic-call expected-type logic）
- 不要把 diagnostics renderers、全局 checker 框架、分析 API 等非直接支持链塞进本计划

## Verification Strategy
> ZERO HUMAN INTERVENTION — all verification is agent-executed.
- Test decision: **tests-after** using existing JUnit 5 + `:tests:test-infrastructure` + `:cfir:analysis-tests`
- QA policy: Every task includes agent-executed scenarios
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. <3 per wave (except final) = under-splitting.
> Extract shared dependencies as Wave-1 tasks for max parallelism.

Wave 1: architecture inventory and semantic boundary freeze
- Task 1: verify first-party collector reality and freeze upstream/local mapping matrix
- Task 2: codify adopt/adapt/omit semantics matrix for Kotlin branches
- Task 3: audit current call chain + legacy API compatibility points

Wave 2: candidate lifecycle and tower feeding restoration
- Task 4: restore/add `CfirCandidateCollector` and direct support declarations
- Task 5: introduce/refactor candidate factory + candidate creation seam
- Task 6: rewrite tower feeding and group-stop behavior around collector contract

Wave 3: resolution pipeline completion
- Task 7: implement `CfirMapArguments`
- Task 8: implement `CfirCheckArguments`
- Task 9: implement `CfirInferTypeArguments`
- Task 10: align `CfirCallResolver` result reduction with collector + conflict resolver semantics

Wave 4: hardening and verification
- Task 11: align overload conflict resolution with Cangjie-specific precedence and diagnostics semantics
- Task 12: add unit tests in `:cfir:resolve:test`
- Task 13: add file-driven diagnostics regressions in `:cfir:analysis-tests`

### Dependency Matrix (full, all tasks)
| Task | Depends On | Blocks |
|---|---|---|
| 1 | none | 4,5,6,7,8,9,10,11,12,13 |
| 2 | none | 4,5,6,7,8,9,10,11,13 |
| 3 | none | 4,5,6,10,11,12 |
| 4 | 1,2,3 | 6,10,12,13 |
| 5 | 1,2 | 6,7,8,9,10,12 |
| 6 | 1,2,4,5 | 10,11,12,13 |
| 7 | 1,2,5 | 10,12,13 |
| 8 | 1,2,5,7 | 9,10,12,13 |
| 9 | 1,2,5,7,8 | 10,11,12,13 |
| 10 | 3,4,6,7,8,9 | 11,12,13 |
| 11 | 2,6,9,10 | 12,13 |
| 12 | 4,5,6,7,8,9,10,11 | 13,F1-F4 |
| 13 | 2,4,6,7,8,9,10,11,12 | F1-F4 |

### Agent Dispatch Summary (wave → task count → categories)
- Wave 1 → 3 tasks → deep / oracle-style architectural analysis / unspecified-high
- Wave 2 → 3 tasks → deep / unspecified-high / deep
- Wave 3 → 4 tasks → unspecified-high / deep / deep / unspecified-high
- Wave 4 → 3 tasks → deep / quick / writing

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [x] 1. Audit first-party collector reality and freeze upstream/local mapping matrix

  **What to do**:
  - Inspect `cfir/resolve/src/.../body/CfirTowerResolver.kt:28-82` and confirm whether `CfirCandidateCollector` is truly absent, nested in another file, generated, or stale-referenced.
  - Produce a checked mapping matrix between local resolver pieces and upstream sources:
    - `external/kotlin/compiler/fir/resolve/src/.../calls/candidate/CandidateCollector.kt`
    - `.../calls/candidate/Candidate.kt`
    - `.../calls/FirCallResolver.kt`
    - `.../calls/tower/FirTowerResolver.kt`
    - `.../calls/tower/TowerLevelHandler.kt`
    - `external/kotlin/compiler/resolution.common/src/.../CandidateApplicability.kt`
  - Mark each local concept as `present-correct`, `present-drifted`, `missing`, or `must-omit`.
  - Record whether collector addition is a new file, a resurrection of hidden code, or a replacement of drifted implementation.

  **Must NOT do**:
  - Do not assume “no standalone file” automatically means “no implementation”.
  - Do not start coding collector behavior before the matrix exists.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: precise upstream/local architectural diff with zero guesswork.
  - Skills: `[]` — no additional skill needed.
  - Omitted: `['test-driven-development']` — this is an inventory/gap task.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 4,5,6,7,8,9,10,11,12,13 | Blocked By: none

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirTowerResolver.kt:28-82` — local collector construction and consumption seam
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt:47-90` — local best-candidate / ambiguity reduction seam
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt:26-260` — existing candidate lifecycle state
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/candidate/CandidateCollector.kt`
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/FirCallResolver.kt`

  **Acceptance Criteria**:
  - [ ] A complete matrix exists for collector/candidate/tower/call resolver/applicability pieces.
  - [ ] The plan executor knows whether to add, replace, or resurrect collector code.
  - [ ] No direct support piece remains unclassified.

  **QA Scenarios**:
  ```
  Scenario: Inventory supports later implementation
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:compileKotlin` after any scaffolding-only edits used to expose missing pieces.
    Expected: Build either succeeds or fails only on known, tracked resolver gaps documented by the matrix.
    Evidence: .sisyphus/evidence/task-1-inventory.txt

  Scenario: Hidden collector implementation disproved or found
    Tool: Bash
    Steps: Run repository searches and compilation to prove whether `CfirCandidateCollector` already exists in a non-obvious location.
    Expected: Evidence explicitly shows either the file path or the absence proof.
    Evidence: .sisyphus/evidence/task-1-inventory-error.txt
  ```

  **Commit**: YES | Message: `refactor(cfir-resolve): freeze candidate collector migration matrix` | Files: resolver inventory notes + direct support files if touched

- [x] 2. Define adopt/adapt/omit semantics matrix for Kotlin resolver branches

  **What to do**:
  - For every upstream-adjacent branch relevant to candidate collection and stage execution, classify it as:
    - `adopt unchanged`
    - `adapt for Cangjie`
    - `omit as Kotlin-only`
  - Cover at minimum:
    - SAM conversions / sam constructors
    - context receivers / context arguments
    - callable-reference adaptation
    - function-kind conversion / suspend-style conversions
    - operator/infix / companion-object / object-as-value rules
    - collection literals / synthetic helper call branches
    - `extend` participation and enum-constructor fallback
  - Use the matrix to drive code-level TODO removal, not as after-the-fact documentation.

  **Must NOT do**:
  - Do not silently inherit Kotlin policy branches.
  - Do not omit a branch without recording why it does not belong to Cangjie.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: language-semantics boundary setting.
  - Skills: `[]`
  - Omitted: `['brainstorming']` — decision already made; this is codification.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 4,5,6,7,8,9,10,11,13 | Blocked By: none

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/tower/CfirTowerGroup.kt:3-50` — Cangjie-specific `extend` tower priority
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt:169-244` — local synthetic-call expected-type behavior to preserve
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt:100-149` — currently present Kotlin-branded conversion seams to classify
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/candidate/Candidate.kt`

  **Acceptance Criteria**:
  - [ ] Every Kotlin-specific branch in the migration boundary has a disposition.
  - [ ] `extend`, enum fallback, and synthetic-call expected-type handling are explicitly preserved/adapted.
  - [ ] Unsupported Kotlin semantics are explicitly excluded from implementation tasks.

  **QA Scenarios**:
  ```
  Scenario: Omit list prevents unsupported feature leakage
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:compileKotlin` after stubbing or pruning unsupported branches during migration.
    Expected: No unresolved references remain to omitted Kotlin-only subsystems.
    Evidence: .sisyphus/evidence/task-2-semantics-matrix.txt

  Scenario: Cangjie semantics preserved
    Tool: Bash
    Steps: Run targeted resolver tests covering `CfirTowerGroup` and synthetic call completion paths.
    Expected: Existing Cangjie-specific behavior still passes after branch classification edits.
    Evidence: .sisyphus/evidence/task-2-semantics-matrix-error.txt
  ```

  **Commit**: YES | Message: `refactor(cfir-resolve): codify kotlin-to-cangjie resolver semantics` | Files: resolver implementation + tests where classifications are enforced

- [x] 3. Audit call-chain ownership and preserve legacy compatibility boundaries

  **What to do**:
  - Audit `CfirCallResolver.kt`, `CfirExpressionsResolveTransformer.kt`, and any legacy `resolveCall` consumers.
  - Preserve the new path (`resolveCallAndSelectCandidate`) as the primary architecture, while keeping backward-compatible legacy APIs working unless all usages are migrated in the same change.
  - Define ownership boundaries for:
    - collection and best-candidate selection
    - failure/no-candidate/ambiguity reduction
    - enum constructor fallback
    - `ResolvedWithErrors` production
  - Ensure downstream `CfirCallCompleter` and named-reference code still receive the expected result shapes.

  **Must NOT do**:
  - Do not break legacy APIs in the same step without proving no usages remain.
  - Do not move final conflict resolution into the collector unless upstream/local architecture explicitly requires it.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: compatibility-sensitive API ownership audit.
  - Skills: `[]`
  - Omitted: `['receiving-code-review']` — no external review yet.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 4,5,6,10,11,12 | Blocked By: none

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt:47-176`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt` — call-resolution entrypoint and downstream expectations
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt:70-133` — completed-call downstream consumer

  **Acceptance Criteria**:
  - [ ] New and legacy resolver paths have explicit compatibility rules.
  - [ ] `ResolvedWithErrors`, ambiguity, and no-candidate ownership are documented in code structure and tests.
  - [ ] Enum constructor fallback still has a defined execution point.

  **QA Scenarios**:
  ```
  Scenario: Primary and legacy APIs both compile
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:compileKotlin`
    Expected: No API drift errors between call resolver, body resolve transformer, and completer consumers.
    Evidence: .sisyphus/evidence/task-3-call-chain.txt

  Scenario: Compatibility regression is caught
    Tool: Bash
    Steps: Run resolver unit tests that still exercise legacy call-resolution entry points.
    Expected: Tests fail if legacy compatibility is broken by refactoring.
    Evidence: .sisyphus/evidence/task-3-call-chain-error.txt
  ```

  **Commit**: YES | Message: `refactor(cfir-resolve): preserve resolver ownership boundaries` | Files: `cfir/resolve/src/.../body/*`, named-reference consumers, tests

- [x] 4. Restore or add the first-party `CfirCandidateCollector` and direct support declarations

  **What to do**:
  - Add or replace `CfirCandidateCollector` under `cfir/resolve/src/.../calls/candidate/` using upstream `CandidateCollector.kt` as the architectural source of truth.
  - Implement the local collector contract expected by `CfirTowerResolver`:
    - `newDataSet()`
    - `consumeCandidate(group, candidate, context)`
    - `shouldStopAtTheGroup(group)`
    - `bestCandidates()`
  - Ensure collector tracks best applicability and best group correctly, including success vs error-only candidate sets.
  - Preserve Cangjie-specific group precedence from `CfirTowerGroup` instead of copying Kotlin ordering blindly.
  - Add any support declarations needed by the collector (e.g. grouped result containers, best-group state, failed-candidate retention policy).

  **Must NOT do**:
  - Do not implement a collector that merely accumulates a flat list.
  - Do not ignore `ResolvedWithErrors` and best-failed-candidate behavior.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: core architectural port.
  - Skills: `[]`
  - Omitted: `['test-driven-development']` — tests-after strategy already chosen.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 6,10,12,13 | Blocked By: 1,2,3

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirTowerResolver.kt:31-74` — required collector API
  - Pattern: `resolution.common/src/org/cangnova/cangjie/resolve/calls/tower/CandidateApplicability.kt` — applicability ordering
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/tower/CfirTowerGroup.kt:11-50` — local group ordering
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/candidate/CandidateCollector.kt`

  **Acceptance Criteria**:
  - [ ] `CfirTowerResolver` compiles against a real collector implementation.
  - [ ] Collector keeps best-group / best-applicability state, not just all candidates.
  - [ ] Collector exposes enough information for `Success`, `ResolvedWithErrors`, `Ambiguity`, and `NoCandidate` reduction.

  **QA Scenarios**:
  ```
  Scenario: Collector contract compiles end-to-end
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:compileKotlin`
    Expected: No unresolved references for collector APIs or grouped-result support types.
    Evidence: .sisyphus/evidence/task-4-collector.txt

  Scenario: Collector stop behavior is observable
    Tool: Bash
    Steps: Run targeted collector/tower tests after implementing the collector.
    Expected: Tests demonstrate lower-priority groups are skipped after higher-priority viable candidates when intended.
    Evidence: .sisyphus/evidence/task-4-collector-error.txt
  ```

  **Commit**: YES | Message: `feat(cfir-resolve): restore candidate collector architecture` | Files: collector + direct support declarations + adjacent tests

- [x] 5. Introduce or refactor a candidate factory seam instead of inlining candidate construction in tower traversal

  **What to do**:
  - Extract candidate creation from `CfirTowerResolver.runResolver(...)` into a dedicated factory or factory-like helper in `calls/candidate/`.
  - Preserve current local inputs (`originScope`, `bodyResolveContext`, explicit receiver kind, base system) while matching upstream lifecycle expectations more closely.
  - Define how dispatch receiver, extension receiver, explicit receiver kind, and fallback call kinds are constructed.
  - Ensure factory decisions align with current `Candidate.kt` lifecycle and `CfirCreateFreshTypeVariableSubstitutorStage` responsibilities.

  **Must NOT do**:
  - Do not leave candidate creation duplicated in multiple tower branches.
  - Do not bypass the existing `Candidate` lifecycle fields that `CfirCallCompleter` already relies on.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: cross-cutting lifecycle cleanup required for faithful port.
  - Skills: `[]`
  - Omitted: `['writing-plans']` — execution task.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 6,7,8,9,10,12 | Blocked By: 1,2

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirTowerResolver.kt:61-73` — current inline candidate construction
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt:26-120` — required construction inputs
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirCreateFreshTypeVariableSubstitutorStage.kt` — downstream lifecycle contract
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/candidate/CandidateFactory.kt`

  **Acceptance Criteria**:
  - [ ] Candidate creation has a dedicated seam outside tower traversal.
  - [ ] Candidate factory preserves all state required by later stages and completer.
  - [ ] Receiver-related fields are no longer “always null by accident”.

  **QA Scenarios**:
  ```
  Scenario: Factory-built candidates still enter stage runner
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CallResolution*"`
    Expected: Existing call-resolution fixtures still create viable candidates via the new factory seam.
    Evidence: .sisyphus/evidence/task-5-candidate-factory.txt

  Scenario: Receiver-related regressions surface
    Tool: Bash
    Steps: Run targeted tests for receiver-sensitive calls after factory extraction.
    Expected: Tests fail if receiver fields are dropped or miswired during factory refactor.
    Evidence: .sisyphus/evidence/task-5-candidate-factory-error.txt
  ```

  **Commit**: YES | Message: `refactor(cfir-resolve): extract candidate creation seam` | Files: candidate factory + tower resolver + tests

- [x] 6. Rewrite tower feeding and group-stop behavior around the collector contract

  **What to do**:
  - Refactor `CfirTowerResolver.runResolver(...)` so group traversal, stop decisions, and candidate feeding reflect the collector contract rather than ad hoc nested loops.
  - Preserve current group order from `CfirTowerGroup`: `MEMBER > LOCAL > EXTEND > IMPORTED > PACKAGE`.
  - Make local-depth/import-depth handling explicit and test-backed.
  - Ensure enum constructor gathering and callable de-duplication still happen before collector consumption.
  - If needed, introduce a `TowerLevelHandler`-like helper to keep collection logic out of the traversal loops.

  **Must NOT do**:
  - Do not change `extend` precedence to match Kotlin if it conflicts with local design.
  - Do not let `shouldStopAtTheGroup` suppress same-group candidates.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: traversal order and stopping rules are correctness-critical.
  - Skills: `[]`
  - Omitted: `['systematic-debugging']` — this is structured refactor, not bug triage.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 10,11,12,13 | Blocked By: 1,2,4,5

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirTowerResolver.kt:44-97`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/tower/CfirTowerGroup.kt:19-50`
  - Test: `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/calls/tower/CfirTowerGroupTest.kt:12-83`
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/tower/FirTowerResolver.kt`
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/tower/TowerLevelHandler.kt`

  **Acceptance Criteria**:
  - [ ] Tower traversal and collector stop behavior are explicit and testable.
  - [ ] `extend` and import/local depth ordering are preserved by design, not accident.
  - [ ] Same-group candidates are all collected before group stop is applied.

  **QA Scenarios**:
  ```
  Scenario: Group ordering and stop behavior pass unit tests
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*TowerGroup*" --tests "*CandidateCollector*"`
    Expected: Priority and early-stop behaviors match the defined group semantics.
    Evidence: .sisyphus/evidence/task-6-tower-feeding.txt

  Scenario: Lower-priority leakage is caught
    Tool: Bash
    Steps: Run a new collector regression with viable higher-group candidates and lower-group distractors.
    Expected: Test fails if lower-priority groups are still considered when they should be cut off.
    Evidence: .sisyphus/evidence/task-6-tower-feeding-error.txt
  ```

  **Commit**: YES | Message: `refactor(cfir-resolve): align tower feeding with collector semantics` | Files: tower resolver/handler + tests

- [x] 7. Implement `CfirMapArguments` using Cangjie call kinds and diagnostics

  **What to do**:
  - Replace the TODO in `cfir/resolve/src/.../calls/stages/CfirMapArguments.kt:20-24` with a real stage.
  - Support at minimum:
    - function calls
    - variable access argument rejection / empty mapping behavior
    - constructors and enum constructors
    - payload-style enum constructor parameter extraction if that is the current local model
  - Initialize candidate argument list and mapping using the candidate lifecycle APIs already present in `Candidate.kt`.
  - Emit the right diagnostics (`WrongArgumentCount` or equivalent) and lower applicability rather than crashing.

  **Must NOT do**:
  - Do not special-case every call shape in tower traversal; keep argument mapping in the stage.
  - Do not silently accept arguments for `VariableAccess`.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: core stage implementation with language-specific branching.
  - Skills: `[]`
  - Omitted: `['test-driven-development']` — tests-after strategy.

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: 10,12,13 | Blocked By: 1,2,5

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirMapArguments.kt:16-24`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/CallKind.kt:23-53`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt:151-204`
  - Upstream: `external/kotlin/compiler/resolution/src/org/jetbrains/kotlin/resolve/calls/components/ArgumentsToParametersMapper.kt`

  **Acceptance Criteria**:
  - [ ] Function and constructor calls initialize argument mapping correctly.
  - [ ] Variable-access calls reject unexpected arguments.
  - [ ] Wrong-argument-count diagnostics affect applicability instead of throwing.

  **QA Scenarios**:
  ```
  Scenario: Argument mapping stage passes direct unit tests
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*MapArguments*"`
    Expected: Candidate argument mappings are initialized correctly for supported call kinds.
    Evidence: .sisyphus/evidence/task-7-map-arguments.txt

  Scenario: Invalid arity is downgraded, not crashed
    Tool: Bash
    Steps: Run tests with zero/too-many arguments and variable-access misuse.
    Expected: Tests observe diagnostics/applicability degradation, not exceptions.
    Evidence: .sisyphus/evidence/task-7-map-arguments-error.txt
  ```

  **Commit**: YES | Message: `feat(cfir-resolve): implement argument mapping stage` | Files: stage + tests

- [x] 8. Implement `CfirCheckArguments` with real applicability and constraint interaction

  **What to do**:
  - Replace the TODO in `cfir/resolve/src/.../calls/stages/CfirCheckArguments.kt:20-24`.
  - For generic candidates, add constraints transactionally through the local constraint system and report `ArgumentTypeMismatch`/equivalent diagnostics on failure.
  - For non-generic candidates, use local subtype/type-relations checks.
  - Ensure the checker sink updates `candidate.lowestApplicability` in line with diagnostics.
  - Preserve current Cangjie `resolution.common` integration; do not reimplement inference internals locally.

  **Must NOT do**:
  - Do not short-circuit through booleans that bypass diagnostics.
  - Do not fork a second constraint system API separate from `resolution.common`.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: type-checking stage touches applicability and inference.
  - Skills: `[]`
  - Omitted: `['systematic-debugging']` — implementation task.

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: 9,10,12,13 | Blocked By: 1,2,5,7

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirCheckArguments.kt:12-24`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/ResolutionStageRunner.kt:22-36`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CheckerSinkImpl.kt` — applicability mutation path
  - Pattern: `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/model/ConstraintSystemImpl.kt`

  **Acceptance Criteria**:
  - [ ] Type-incompatible arguments produce diagnostics and lower applicability.
  - [ ] Generic candidates add constraints through `resolution.common` rather than ad hoc checks.
  - [ ] Stage runner can stop on first error without losing diagnostic correctness.

  **QA Scenarios**:
  ```
  Scenario: Argument type mismatches are reported deterministically
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CheckArguments*"`
    Expected: Mismatch tests show applicability degradation and expected diagnostics.
    Evidence: .sisyphus/evidence/task-8-check-arguments.txt

  Scenario: Generic argument constraints are exercised
    Tool: Bash
    Steps: Run resolver tests that force generic candidates through constraint-based argument checking.
    Expected: Tests fail if constraints are not added or are added to the wrong system.
    Evidence: .sisyphus/evidence/task-8-check-arguments-error.txt
  ```

  **Commit**: YES | Message: `feat(cfir-resolve): implement argument checking stage` | Files: stage + tests

- [x] 9. Implement `CfirInferTypeArguments` and align completion handoff with current candidate lifecycle

  **What to do**:
  - Replace the TODO in `cfir/resolve/src/.../calls/stages/CfirInferTypeArguments.kt:39-43`.
  - Use the fresh variables and substitutor already introduced by `CfirCreateFreshTypeVariableSubstitutorStage`.
  - Complete explicit + inferred type-argument mapping and write back resolved type information in the shape expected by `CfirCallCompleter` and named references.
  - Preserve local synthetic-call and expected-type completion assumptions; do not duplicate final completion logic that already belongs in `CfirCallCompleter`.
  - Ensure inference failures degrade to diagnostics (`InferenceConstraintError`, cannot-infer diagnostics) rather than hard failure.

  **Must NOT do**:
  - Do not move full call completion into this stage.
  - Do not bypass the candidate substitutor lifecycle.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: inference stage is the seam between resolution and completion.
  - Skills: `[]`
  - Omitted: `['verification-before-completion']` — verification belongs after implementation.

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: 10,11,12,13 | Blocked By: 1,2,5,7,8

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirInferTypeArguments.kt:28-43`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt:85-99, 209-260`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt:70-133`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/CallKind.kt:23-53`

  **Acceptance Criteria**:
  - [ ] Explicit and inferred type arguments are written into the candidate state correctly.
  - [ ] Inference failures appear as diagnostics with degraded applicability.
  - [ ] `CfirCallCompleter` still owns final completion/writeback.

  **QA Scenarios**:
  ```
  Scenario: Type-argument inference stage passes direct tests
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*InferTypeArguments*"`
    Expected: Tests cover explicit type args, successful inference, and cannot-infer failures.
    Evidence: .sisyphus/evidence/task-9-infer-type-arguments.txt

  Scenario: Completer handoff remains intact
    Tool: Bash
    Steps: Run end-to-end resolver tests that continue into `CfirCallCompleter`.
    Expected: Tests fail if inference stage steals or breaks final completion responsibilities.
    Evidence: .sisyphus/evidence/task-9-infer-type-arguments-error.txt
  ```

  **Commit**: YES | Message: `feat(cfir-resolve): implement type argument inference stage` | Files: stage + tests

- [ ] 10. Align `CfirCallResolver` result reduction with collector, conflict resolver, and enum fallback semantics

  **What to do**:
  - Revisit `CfirCallResolver.resolveCallAndSelectCandidate(...)` so best-candidate retrieval matches the new collector contract exactly.
  - Ensure reduction behavior is explicit for:
    - no candidates
    - one best failed candidate -> `ResolvedWithErrors`
    - multiple failed candidates -> no candidate or dedicated failure path according to agreed local semantics
    - one successful candidate -> success
    - multiple successful candidates -> overload conflict resolver / ambiguity
  - Preserve enum constructor fallback and wire it through the primary result path rather than a side API if the architecture now allows it.
  - Keep downstream result shapes stable for body resolve and diagnostics.

  **Must NOT do**:
  - Do not let collector and call resolver each invent separate ambiguity rules.
  - Do not regress the currently documented `ResolvedWithErrors` behavior.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` — Reason: result semantics integration task.
  - Skills: `[]`
  - Omitted: `['finishing-a-development-branch']` — not branch-finalization work.

  **Parallelization**: Can Parallel: YES | Wave 3 | Blocks: 11,12,13 | Blocked By: 3,4,6,7,8,9

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt:47-90`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/overloads/ConeCallConflictResolver.kt`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/overloads/ConeOverloadConflictResolver.kt`
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/FirCallResolver.kt`

  **Acceptance Criteria**:
  - [ ] `NoCandidate`, `ResolvedWithErrors`, `Success`, and `Ambiguity` are produced from a single coherent reduction policy.
  - [ ] Enum constructor fallback remains reachable and tested.
  - [ ] Downstream body resolve/completion consumers still understand the result shapes.

  **QA Scenarios**:
  ```
  Scenario: Resolver result reduction passes unit tests
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CallResolver*"`
    Expected: Tests cover success, ambiguity, no-candidate, and resolved-with-errors paths.
    Evidence: .sisyphus/evidence/task-10-call-resolver.txt

  Scenario: Enum fallback still works
    Tool: Bash
    Steps: Run targeted resolver tests involving enum constructor competition.
    Expected: Tests fail if enum-constructor fallback disappears or outranks normal candidates incorrectly.
    Evidence: .sisyphus/evidence/task-10-call-resolver-error.txt
  ```

  **Commit**: YES | Message: `refactor(cfir-resolve): align call resolver result reduction` | Files: call resolver + tests

- [x] 11. Align overload conflict resolution with collector semantics and Cangjie-specific precedence

  **What to do**:
  - Audit `CfirOverloadConflictResolver` against the new collector output and current Cangjie tie-break markers on `Candidate`.
  - Preserve/verify local rules around:
    - non-generic preference
    - fewer defaults
    - numeric compatibility
    - `quest fallback`
    - `extend participation`
  - Ensure same-group candidates reach overload reduction and cross-group candidates are cut off earlier by collector rules where intended.
  - Add or refine any helper abstractions needed for maximally-specific selection without importing Kotlin-only heuristics.

  **Must NOT do**:
  - Do not overwrite local `extend`-related tie-break behavior with upstream defaults.
  - Do not use overload conflict resolver to compensate for incorrect collector group stopping.

  **Recommended Agent Profile**:
  - Category: `deep` — Reason: semantic correctness task.
  - Skills: `[]`
  - Omitted: `['systematic-debugging']` — planned semantics work.

  **Parallelization**: Can Parallel: YES | Wave 4 | Blocks: 12,13 | Blocked By: 2,6,9,10

  **References**:
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/overloads/ConeOverloadConflictResolver.kt`
  - Pattern: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt:204-207` — Cangjie tie-break markers
  - Test: `cfir/resolve/test/.../calls/overloads/CfirOverloadConflictResolverTest.kt`
  - Upstream: `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/calls/ConeCallConflictResolver.kt`

  **Acceptance Criteria**:
  - [ ] Overload reduction consumes collector output correctly.
  - [ ] Cangjie-specific tie-breakers remain effective and tested.
  - [ ] Ambiguity remains when no valid specificity rule breaks the tie.

  **QA Scenarios**:
  ```
  Scenario: Overload reduction keeps local specificity rules
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*OverloadConflictResolver*"`
    Expected: Existing and new specificity tests pass, including extend-related cases.
    Evidence: .sisyphus/evidence/task-11-overload-resolution.txt

  Scenario: Collector bugs are not masked by conflict resolution
    Tool: Bash
    Steps: Run a regression where lower-priority candidates must never reach overload resolution.
    Expected: Test fails if conflict resolver is forced to choose across wrongly-collected groups.
    Evidence: .sisyphus/evidence/task-11-overload-resolution-error.txt
  ```

  **Commit**: YES | Message: `refactor(cfir-resolve): preserve cangjie overload specificity` | Files: overload resolver + tests

- [ ] 12. Add resolver unit tests for collector, stages, and result reduction

  **What to do**:
  - Extend `cfir/resolve/test` with direct tests for:
    - collector best-group / best-applicability / early-stop behavior
    - same-group ambiguity preservation
    - local vs imported vs package vs extend priority
    - `CfirMapArguments`, `CfirCheckArguments`, `CfirInferTypeArguments`
    - `ResolvedWithErrors` and no-candidate paths
    - enum-constructor fallback
  - Reuse or modernize existing fixtures under `CallResolutionTestFixtures.kt`; fix drift between old fixture names (`CfirCandidate`, `CfirResolutionContext`) and current implementation names if needed.
  - Add receiver-sensitive and generic-inference scenarios where the candidate factory and stage pipeline can regress.

  **Must NOT do**:
  - Do not add tests that only assert compilation.
  - Do not leave fixture drift unresolved if new tests need current API shapes.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: many focused test additions and fixture updates.
  - Skills: `[]`
  - Omitted: `['test-driven-development']` — strategy is tests-after, but tests are still mandatory.

  **Parallelization**: Can Parallel: YES | Wave 4 | Blocks: 13,F1-F4 | Blocked By: 4,5,6,7,8,9,10,11

  **References**:
  - Test: `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/calls/tower/CfirTowerGroupTest.kt:7-83`
  - Test: `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/calls/CallResolutionTestFixtures.kt:34-169`
  - Test: `cfir/resolve/test/.../calls/overloads/CfirOverloadConflictResolverTest.kt`
  - Test: `cfir/resolve/test/.../calls/stages/CfirInferTypeArgumentsTest.kt`

  **Acceptance Criteria**:
  - [ ] Unit tests cover each new/ported architectural seam.
  - [ ] Fixture drift is corrected enough to instantiate current resolver objects.
  - [ ] At least one unit test fails if collector stop behavior regresses.

  **QA Scenarios**:
  ```
  Scenario: Focused resolver test suite passes
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:resolve:test --tests "*CandidateCollector*" --tests "*Tower*" --tests "*CheckArguments*" --tests "*InferTypeArguments*" --tests "*CallResolver*"`
    Expected: All targeted resolver tests pass.
    Evidence: .sisyphus/evidence/task-12-resolver-tests.txt

  Scenario: Fixture drift is exposed
    Tool: Bash
    Steps: Run the same targeted suite before final cleanup.
    Expected: Any stale test helper types or constructors fail fast and are fixed in this task.
    Evidence: .sisyphus/evidence/task-12-resolver-tests-error.txt
  ```

  **Commit**: YES | Message: `test(cfir-resolve): cover candidate collection pipeline` | Files: `cfir/resolve/test/...`

- [ ] 13. Add file-driven diagnostics regressions for observable resolver behavior

  **What to do**:
  - Add `.cj` testdata under `cfir/analysis-tests/testData/diagnostics/...` covering observable call-resolution outcomes:
    - local vs imported overload selection
    - member vs extend competition
    - ambiguity across same-priority candidates
    - unresolved call after candidate filtering
    - generic overload resolution with `DUMP_INFERENCE_LOGS`
    - parser parity on collection-sensitive cases if applicable (`CFIR_PARSER` / `COMPARE_WITH_LIGHT_TREE`)
  - Use existing directives (`MODULE`, `FILE`, `WITH_STDLIB`, `VERIFY_RESOLVED_TYPES`, `DUMP_SCOPE`, `DUMP_INFERENCE_LOGS`) rather than inventing new framework primitives first.
  - Add multi-module import tests where candidate source matters.

  **Must NOT do**:
  - Do not rely solely on unit tests for behavior visible to end users.
  - Do not add framework features unless a test is impossible with current infrastructure.

  **Recommended Agent Profile**:
  - Category: `writing` — Reason: source-driven regression design with diagnostics contracts.
  - Skills: `[]`
  - Omitted: `['writing-skills']` — not skill authoring.

  **Parallelization**: Can Parallel: YES | Wave 4 | Blocks: F1-F4 | Blocked By: 2,4,6,7,8,9,10,11,12

  **References**:
  - Test framework: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/frontend/CfirDiagnosticsHandler.kt`
  - Test framework: `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/directives/CfirDiagnosticsDirectives.kt`
  - Test base: `cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/runners/AbstractCfirDiagnosticTestBase.kt`
  - Generated runner: `cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisDiagnosticsTestGenerated.kt`
  - Existing examples: `cfir/analysis-tests/testData/diagnostics/type-mismatch/argumentTypeMismatch.cj`, `.../genericReturnTypeInference.cj`, `.../unresolved/unresolvedNameCall.cj`

  **Acceptance Criteria**:
  - [ ] File-driven tests demonstrate observable candidate-source precedence and ambiguity behavior.
  - [ ] At least one test covers `extend` vs member/imported competition.
  - [ ] At least one test covers generic overload resolution with inference logs or resolved-type verification.

  **QA Scenarios**:
  ```
  Scenario: File-driven diagnostics regressions pass
    Tool: Bash
    Steps: Run `./gradlew.bat :cfir:analysis-tests:test --tests "*CfirAnalysisDiagnosticsTestGenerated"`
    Expected: New and existing diagnostics tests pass with updated golden data where needed.
    Evidence: .sisyphus/evidence/task-13-analysis-tests.txt

  Scenario: Candidate-source precedence is observable externally
    Tool: Bash
    Steps: Run a new multi-module diagnostics test with local/imported/extend competitors and inference-log output.
    Expected: Test output shows the intended winner and fails if precedence regresses.
    Evidence: .sisyphus/evidence/task-13-analysis-tests-error.txt
  ```

  **Commit**: YES | Message: `test(cfir-analysis): add resolver precedence regressions` | Files: `cfir/analysis-tests/testData/...`, generated baselines as required

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.
- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check — deep

## Commit Strategy
- Prefer one commit per numbered task.
- Commit types should reflect intent precisely:
  - `refactor(cfir-resolve): ...` for architectural seam restoration
  - `feat(cfir-resolve): ...` for newly added collector/stage behavior
  - `test(cfir-resolve): ...` / `test(cfir-analysis): ...` for coverage additions
- Do not mix `:cfir:resolve` architectural work with unrelated repository cleanup.

## Success Criteria
- Candidate collection is no longer a dangling reference but a functioning subsystem.
- Resolver architecture is behaviorally complete for current Cangjie call kinds and scopes.
- `extend` precedence is preserved deliberately across collection and overload reduction.
- Stage TODOs are gone from the core call-resolution path.
- Unit and file-driven tests both prove correctness.
- Kotlin-only unsupported semantics are omitted explicitly rather than accidentally half-ported.
