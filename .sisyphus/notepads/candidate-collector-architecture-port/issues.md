## 2026-03-24 Task 1 — inventory issues to carry forward

- Package drift is real across the resolver skeleton: candidate-related contracts live under `calls/`, while `CfirTowerResolver`, `CfirCallResolver`, and `ResolutionStageRunner` remain under `body/`. Task 4 must honor this drifted but live split instead of performing an opportunistic architecture migration.
- The local tower path currently constructs `Candidate` directly in `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirTowerResolver.kt:61-73`, so there is no standalone `CandidateFactory` seam yet. Any collector restoration must avoid silently bundling Task 5 ownership changes into Task 4.
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirMapArguments.kt`, `CfirCheckArguments.kt`, `CfirInferTypeArguments.kt`, `CfirCreateFreshTypeVariableSubstitutorStage.kt`, and `CfirCheckVisibility.kt` still contain `TODO("Not yet implemented")`. These are pre-existing stage-pipeline gaps that will affect compile/runtime behavior, but they do not change the inventory conclusion that the collector seam itself is missing.
- Upstream `CandidateCollector` exposes `currentApplicability`, `bestGroup`, `forwardedDiagnostics`, and supports `AllCandidatesCollector`; the local code has no equivalent hooks. This means later tasks must decide whether analysis-facing all-candidates support is needed, but Task 1 freezes it as currently absent.

## 2026-03-24 Task 2 — semantic drift to watch during implementation

- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/candidate/Candidate.kt` still contains residual context-argument and SAM-related fields copied from Kotlin-shaped architecture, while `CallInfo.kt` has already narrowed the live Cangjie call contract. Later implementation tasks must avoid treating those residual fields as automatic requirements; they are mixed-purpose state, not a blanket feature-approval signal.
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/ConeResolutionAtom.kt` intentionally preserves postponed-atom shells while also declaring the file “thinner, more Cangjie-specific”. That mixed signal is a likely source of accidental Kotlin-feature leakage if Tasks 4-11 port upstream collector logic mechanically instead of following the matrix above.

## 2026-03-24 Task 3 — compatibility-sensitive issues to carry forward

- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt:47-53` currently exposes the primary `resolveCallAndSelectCandidate(...)` entrypoint but leaves it as `TODO()`, while `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt:281-302`, `:359`, `:926-964` already treats it as the live architecture. Task 10 must align implementation to this already-published consumer contract rather than redefining the consumer side.
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt:413-435` still calls `callResolver.resolveCall(...)`, but the current `CfirCallResolver.kt` snapshot in this workspace does not show that legacy method. This is a strong sign that legacy API compatibility is a drifted seam and must be handled carefully instead of being removed casually during collector restoration.
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt:327-340` and `:380-388` own Cangjie-specific `NoCandidate` fallbacks today. If Task 10 moves `NoCandidate` handling entirely into `CfirCallResolver`, it risks regressing enum-constructor fallback and callable-variable invoke fallback that currently happen only after consumer-side dispatch.
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt:80-82`, `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt:83-100`, and `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/ResolveUtils.kt:32-34` all depend on the temporary `CfirNamedReferenceWithCandidate` shape. That makes candidate-backed references a compatibility contract, not an internal convenience.
- Legacy-success branches (`LegacySuccess` / `LegacyAmbiguity`) in `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt:295-300`, `:390-394`, `:417-425` bypass the candidate-backed completion pipeline and bind resolved symbols directly. Task 10 must account for this dual path explicitly when changing result reduction or final-reference writing.

## 2026-03-24 Task 4 — implementation issues observed during seam restoration

- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt` still carries stale upstream-shaped scaffolding (`AllCandidatesCollector`, TODO bodies, and mixed old/new resolver contracts). Task 4 only retargeted the collector-related type names needed to keep the restored seam coherent; it deliberately did not absorb Task 10 result-reduction redesign.
- The LSP diagnostics tool could not produce fresh file diagnostics in this session because the language server repeatedly timed out during initialization/cancelled requests. Verification for this task therefore relied on Gradle compile/test evidence plus direct review of the touched files.

## 2026-03-24 Task 4 — scope-correction note

- Review confirmed that `README.md` and the extra `AllCandidatesCollector` / broad TODO-shaped API drift in `CfirCallResolver.kt` were scope creep for Task 4. Those changes were removed or narrowed so the task no longer pretends to redesign call resolution ownership.

## 2026-03-24 Task 5 — implementation issues observed during factory extraction

- The LSP diagnostics tool again failed in this environment (`initialize` timeout / cancellation), so Task 5 verification had to rely on Gradle compile/test commands plus direct inspection of the touched files instead of file-level language-server diagnostics.
- Focused `:cfir:resolve` verification is still noisy because unrelated compile failures remain widespread across `cfir/resolve` and `cfir/checkers`, which means task-local regressions cannot currently be isolated with a green module build.

## 2026-03-24 Task 6 — implementation issues observed during tower grouping refactor

- The required `lsp_diagnostics` check could not return results for either touched Kotlin file because the language server again timed out during initialization in this environment.
- Focused Gradle verification for `CfirTowerGroupTest` is still blocked before test execution because pre-existing compile failures in `:cfir:checkers` and broader `:cfir:resolve` sources abort the build graph before the new tower tests can run.

## 2026-03-24 Task 7 — implementation issues observed during argument mapping work

- The required lsp_diagnostics check could not produce file diagnostics for either touched Kotlin file because the language server timed out during initialization in this environment. Verification again had to rely on direct source review plus Gradle command evidence.
