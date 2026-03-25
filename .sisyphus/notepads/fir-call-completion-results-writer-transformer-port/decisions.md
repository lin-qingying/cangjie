
## 2026-03-24 Task 1 branch disposition decisions
- Classify upstream substitution helpers, qualified/property/function access writeback, candidate-to-applied-reference conversion, lambda expected-type propagation, and core control-flow result typing as `ported via explicit local equivalent`.
- Classify PCLA callback replay, postponed-PCLA writeback ordering, delegated-property receiver updates, richer argument rewrite traversal, SAM wrapping during writeback, traversal guards, and synthetic-call refined-type postprocessing as `blocked by prerequisite` because they depend on the missing local completion-results writer pass.
- Classify Java flexible-type constructor hacks as `provably excluded from direct local chain` because local CFIR explicitly does not model Kotlin flexible types in this path.
- Classify safe-call, callable-reference access, annotation-call, delegated-constructor-call, and vararg-arguments-expression branches as `provably excluded from direct local chain` because no corresponding local generated CFIR expression nodes / dispatcher branches were found during the audit.
- Treat annotation/collection-literal handling as split: generic array-literal typing exists locally, but upstream annotation-call collection-literal completion remains excluded from the current direct local chain until an annotation-call expression surface exists.

## 2026-03-24 Task 2 collaborator restoration decisions
- Restore `dataFlowAnalyzer` and `integerLiteralAndOperatorApproximationTransformer` on the abstract `BodyResolveComponents` contract instead of hiding them solely in `BodyResolveTransformerComponents`, because `CfirCallCompleter` already consumes them as direct body-resolve collaborators.
- Own both collaborators lazily in `CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents` to stay aligned with the upstream body-resolve seam and avoid eager initialization of services unused by some body-resolve paths.
- Add only seam-preserving direct support implementations required by this task: a minimal `CfirDataFlowAnalyzer` and a no-op `IntegerLiteralAndOperatorApproximationTransformer`, explicitly deferring faithful writer-driven behavior to later plan tasks.

## 2026-03-24 Task 6 lambda/PCLA seam decisions
- Move only the reachable writer-owned PCLA replay order into CfirCallCompletionResultsWriterTransformer: postponed PCLA calls, then onPCLACompletionResultsWritingCallbacks, then local anonymous-function finalization for lambdasAnalyzedWithPCLA.
- Keep ordinary lambda parameter typing in CfirCallCompleter.LambdaAnalyzerImpl for now; the local direct chain already writes those parameter refs there, while this task focuses on final lambda/body writeback and callback replay after completion.
- Adapt the upstream lambda finalization idea to local CFIR by finalizing anonymous-function return type and lambda expression function type from existing body/return-expression information, without broadening into callable-reference, delegated-property proof, or integer-approximation branches.
- Task 6 data-flow/integer slice decision: keep `IntegerLiteralAndOperatorApproximationTransformer` minimal but live by approximating only ideal literal expression/result types reached by the current writer seam, instead of leaving a dead collaborator or widening into excluded receiver/operator branches.
- Task 6 data-flow/integer slice decision: use the writer to re-run approximation over anonymous-function return expressions and lambda body/result types after postponed-atom replacement so return-type finalization stays coherent with the restored data-flow collector.

## 2026-03-24 Task 6 branch disposition decision
- Kept `CfirCallCompletionResultsWriterTransformer.Mode.DelegatedPropertyCompletion` instead of deleting it because the restored writer seam is supposed to stay upstream-shaped, but added a narrow in-code note declaring it intentionally unreachable until a local delegated-property inference session and explicit writer-construction path exist.
- Kept `Candidate.onPCLACompletionResultsWritingCallbacks` as the matching structural seam and documented that it is currently unpopulated in the direct local chain. This avoids falsely signaling supported delegated-property completion while preserving the contract for a future direct-chain implementation.
- Did not implement postponed callable-reference handling because repository evidence still shows no local callable-reference CFIR node or call-info path; treating the shared inference marker alone as reachability proof would be misleading.

## 2026-03-24 Task 7 ownership unification decision
- Chose CfirCallCompletionResultsWriterTransformer as the authoritative producer of finalized applied-callable-reference return types for the direct success path, because it already owns final substitution, supertype approximation, and integer-literal approximation before writing results back to expressions.
- Narrow implementation choice: keep CfirAppliedCallReferenceFactory as a pure reference-construction helper that accepts substitutedReturnType as input, rather than widening the factory into another completion participant or moving more logic into CfirExpressionsResolveTransformer.
