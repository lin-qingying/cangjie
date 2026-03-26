
## 2026-03-24 Task 1 branch audit
- Local direct-chain seam is already split across `CfirCallCompleter`, `CfirExpressionsResolveTransformer`, and `CfirAppliedCallReferenceFactory`.
- Core completed-call outcomes are already partially ported via explicit local equivalents: candidate substitution, applied callable reference creation, and final expression type writeback.
- The largest missing architectural seam is a dedicated `CfirCallCompletionResultsWriterTransformer` pass; many upstream branches are blocked specifically by that missing post-completion rewrite layer rather than by missing inference infrastructure.
- Several upstream families are provably excluded from the current direct local chain because the generated CFIR expression/dispatcher surface lacks corresponding node kinds or handlers, including safe-call, callable-reference access, annotation-call, delegated-constructor-call, and vararg-arguments-expression paths.
- Lambda/PCLA substrate is partially present locally: `Candidate` already stores postponed/PCLA callback state, and `CfirCallCompleter` already tracks `lambdasAnalyzedWithPCLA`, but callback execution/writeback remains blocked without the missing writer pass.

## 2026-03-24 Task 2 collaborator restoration
- `CfirCallCompleter`'s current direct seam truly depends on both `dataFlowAnalyzer` and `integerLiteralAndOperatorApproximationTransformer`; keeping them commented out in `BodyResolveComponents` leaves the abstract contract behind the already-compiled call-completion API.
- Local body resolve had only `CfirDataFlowAnalyzerContext`, not a concrete analyzer class, so restoring ownership required a direct support implementation rather than just uncommenting abstract properties.
- The smallest safe support implementation for this task is a minimal `CfirDataFlowAnalyzer` that only collects anonymous-function return expressions for lambda completion; broader CFG/smart-cast behavior should remain with later writer/data-flow work.
- No local `IntegerLiteralAndOperatorApproximationTransformer` existed either, so a no-op transformer is the correct seam-preserving placeholder until the dedicated completion-results writer can actually consume it.

## 2026-03-24 Task 3 finalized-result API restoration
- `CfirExpressionsResolveTransformer` already depended on `components.callCompleter.completedResultType(candidate)` at function-call, callable-access, and enum-constructor sites, so the narrow restoration point is the completer API rather than more consumer churn.
- The smallest seam-preserving implementation is to route `CfirCallCompleter.completedResultType(candidate)` through the existing `Candidate.substitutedReturnType()` helper, which keeps candidate internals behind the completer boundary while reusing the local substitution contract already used by `CfirAppliedCallReferenceFactory`.
- `createCompletionResultsWriter(...)` should source session-owned collaborators through `BodyResolveComponents` (`components.session`, `components.scopeSession`, `components.session.typeApproximator`) so the constructor matches the restored direct collaborator chain instead of mixing direct session access with component-owned services.

## 2026-03-24 Task 4 writer skeleton
- The local `CfirCallCompletionResultsWriterTransformer` now exists under `cfir/resolve/.../inference` as the dedicated short-lived transformer created by `CfirCallCompleter`, restoring the missing writer seam without preempting downstream writeback branches.
- Matching the restored collaborator chain required `CfirCallCompleter.createCompletionResultsWriter(...)` to pass `components.samResolver`, so the constructor dependency list stays aligned with upstream shape and current local body-resolve ownership.
- The minimal helper layer is adapted to local CFIR types by using `ConeIdealLiteralType` for integer-literal approximation, and the traversal contract is enforced in `transformElement` by returning declarations unchanged so the writer does not descend into nested declarations by default.
## 2026-03-24 Task 5
- `CfirCallCompletionResultsWriterTransformer` can own the qualified/function/property access writeback seam without widening scope: the local direct chain already exposes `createCompletionResultsWriter(...)` from `CfirCallCompleter` and candidate-bearing references via `CfirNamedReferenceWithCandidate`.
- Local CFIR does not mirror upstream FIR's richer contradiction diagnostic set for this seam, so the narrow faithful mapping is: successful candidate -> resolved/applied callable reference, explicit candidate error or inapplicable candidate -> `CfirErrorNamedReference`/`CfirErrorReference` using existing `ConeDiagnostic` values.
- The existing `CfirExpressionsResolveTransformer` successful-call branches were the remaining duplicated ownership point; routing them through the writer keeps enum-expected-type refinement local while moving final reference/type writeback into the dedicated transformer.

## 2026-03-24 Task 5 integration cleanup
- The remaining duplicate ownership in `CfirExpressionsResolveTransformer` was not reference conversion anymore but the post-writer success-path `replaceConeTypeOrNull(...)` in `applySuccessfulCallResult` and `applyResolvedCallableAccessCandidate`.
- Removing those two post-writer assignments leaves successful candidate-bearing function-call and callable-access paths consuming the writer/completer-produced callee reference and completed result type directly, which is the intended finalization boundary for this seam.
- `tryEnumConstructorFallback(...)` still legitimately keeps local enum expected-type refinement because that branch is outside the fully centralized writer-owned success path for this task.

## 2026-03-24 Task 6 lambda/PCLA seam
- The upstream PCLA invariant that matters most for the local seam is replay order inside the writer: postponed PCLA calls first, then completion-result callbacks, then lambda writeback for functions already analyzed with PCLA.
- Local CFIR can port the writer-owned replay/writeback boundary without widening into excluded branches by adding a lambda-expression path in CfirCallCompletionResultsWriterTransformer and invoking the PCLA replay helper from the qualified/function/property access success path.
- The current local tree surface is simpler than upstream FIR: the practical writeback target for this task is anonymous-function return/type finalization driven by CfirDataFlowAnalyzer.returnExpressionsOfAnonymousFunction(...), not the full upstream receiver/function-kind reconstruction matrix.
- Task 6 data-flow/integer slice: the local writer/completer chain already reaches `CfirDataFlowAnalyzer.returnExpressionsOfAnonymousFunction`, so anonymous-function finalization can stay writer-owned without expanding into broader CFG/smart-cast work.
- The local CFIR tree does not expose upstream-style dispatch/extension receiver writeback on qualified accesses; the smallest seam-supported integer approximation is therefore expression-result and explicit-receiver normalization only.

## 2026-03-24 Task 6 delegated-property / callable-reference refresh
- Restoring the writer file changed the delegated-property branch disposition from "blocked because writer missing" to "retained seam but unreachable": `Mode.DelegatedPropertyCompletion` now exists locally, yet there is still no direct-chain creator or delegated-property inference session that can select it.
- `Candidate.onPCLACompletionResultsWritingCallbacks` is not evidence of live delegated-property behavior by itself; without a local producer, it is only a seam retained for future direct-chain work.
- Local postponed callable-reference support is excluded at the CFIR call surface, not at the lowest inference utility layer: `PostponedCallableReferenceMarker` exists in shared inference code, but `CallInfo` intentionally omits callable-reference fields and no local `CallableReferenceAccess` node/transformer exists.

## 2026-03-24 Task 7 callable-reference ownership cleanup
- CfirAppliedCallReferenceFactory.buildAppliedCallableReference(...) is only constructed from CfirCallCompletionResultsWriterTransformer, so the narrow ownership fix is to pass the writer-computed finalized result type into the factory instead of letting the factory call candidate.substitutedReturnType() independently.
- Keeping CfirCallCompleter.completedResultType(candidate) and the writer-local completedResultType(candidate) as the only direct substitutedReturnType()-based seams preserves the intended ownership boundary: consumers ask the completer/writer for finalized types, while the factory only packages already-finalized data into the applied reference.
