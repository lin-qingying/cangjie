# Task 1 Audit — FirInferenceLogger Local Surface

## Confirmed present first-party seams

### Generic logger API
- `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/InferenceLogger.kt`
  - Present.
  - Current API now exposes:
    - `logInitial`
    - `logNewVariable`
    - `log(variable, constraint, context)`
    - `logError(error, context)`
    - `logReadiness`
    - `logFixVariable(variable, resultType, context)`
    - `withOrigin`
    - `withOrigins`
    - `Dummy`

### Generic inference pipeline consumers
- `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/ConstraintInjector.kt`
- `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/ConstraintIncorporator.kt`
- `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/model/ConstraintSystemImpl.kt`
- `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/model/MutableConstraintStorage.kt`
- `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/VariableFixationFinder.kt`
  - Present.
  - These already call into `InferenceLogger` for initial constraints, new variables, and readiness/fixation-style records.

### Resolve-phase runtime call sites
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/ResolutionStageRunner.kt`
  - Present.
  - Calls `candidate.callInfo.session.inferenceLogger`, `logCandidate(candidate)`, and `logStage(...)`.
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/InferenceComponents.kt`
  - Present.
  - Passes `session.inferenceLogger` into `ConstraintIncorporator`, `ConstraintInjector`, and readiness/fixation components.
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/ConstraintSystemCompleter.kt`
  - Present.
  - Calls `context.session.inferenceLogger?.logStage("Call Completion", this)`.

### Runtime activation seam
- `compiler/config/src/org/cangnova/cangjie/config/CommonConfigurationKeys.kt`
  - Present.
  - Defines `DUMP_INFERENCE_LOGS` and `CompilerConfiguration.dumpInferenceLogs`.
- `cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirAbstractSessionFactory.kt`
  - Present.
  - Registers `CfirInferenceLogger` when `configuration.dumpInferenceLogs` is enabled.

### Test infrastructure and existing golden contract
- `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/directives/CfirDiagnosticsDirectives.kt`
  - Present.
  - Defines `DUMP_INFERENCE_LOGS` directive.
- `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/services/EnvironmentConfigurator.kt`
  - Present.
  - Maps `DUMP_INFERENCE_LOGS` to `CommonConfigurationKeys.DUMP_INFERENCE_LOGS`.
- `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/frontend/CfirInferenceLogsHandler.kt`
  - Present.
  - Imports `org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceLogger` and `org.cangnova.cangjie.cfir.resolve.inference.inferenceLogger`.
  - Expects the concrete logger to expose:
    - `topLevelElements`
    - `BlockOwner.CandidateOwner` and `BlockOwner.Unknown`
    - `NewVariableElement`
    - `ConstraintElement`
    - `ErrorElement`
    - `FixVariableElement`
- Existing side files found:
  - `cfir/analysis-tests/testData/diagnostics/type-mismatch/genericConstraintCascade.cfir.inference.txt`
  - `cfir/analysis-tests/testData/diagnostics/type-mismatch/genericReturnTypeInference.cfir.inference.txt`
  - `cfir/analysis-tests/testData/diagnostics/type-mismatch/genericArgumentConstraintConflict.cfir.inference.txt`
  - Current contents sampled from representative files are still `<no inference logs>`.

## Confirmed partial / missing first-party seams

### Concrete logger source
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirInferenceLogger.kt`
  - Present and tracked in git, but only as a partial stub.
  - Current contents provide:
    - `Call`
    - `BlockOwner.CandidateOwner` / `BlockOwner.Unknown`
    - `BlockElement(name, owner)`
    - `topLevelBlocks`
    - `logCandidate(candidate)`
    - `logStage(name, system)`
  - Missing relative to the existing handler contract and upstream shape:
    - `topLevelElements`
    - `BlockItemElement`
    - `NewVariableElement`
    - `ConstraintElement`
    - `ErrorElement`
    - `FixVariableElement`
    - origin caching / constraint element tracking / readiness/fixation structure

### Session accessor
- A local accessor currently exists inside the partial logger file:
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirInferenceLogger.kt`
  - `val CfirSession.inferenceLogger: CfirInferenceLogger? by CfirSession.nullableSessionComponentAccessor()`
- Existing session extension file:
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/session/CfirSessionExtensions.kt`
  - This remains the strongest local consolidation target because it already hosts other resolve-session accessors.

## Current mismatch summary
- Runtime and test layers already assume a concrete `CfirInferenceLogger` exists.
- Resolve and generic inference layers already consume `session.inferenceLogger`.
- The concrete first-party logger source does exist, but only as a thin partial implementation that does not satisfy the current test handler contract.
- The session accessor also exists, but is colocated inside the logger file rather than at the usual resolve-session extension seam.
- Therefore the repository currently looks like a partial port where call sites and handler expectations advanced farther than the concrete logger model.

## Tentative local landing paths
- Concrete logger completion target: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirInferenceLogger.kt`
- Session accessor consolidation target: `cfir/resolve/src/org/cangnova/cangjie/cfir/session/CfirSessionExtensions.kt`

## Upstream source of truth
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirInferenceLogger.kt`

## Upstream dependency triage

### Mandatory to mirror/adapt locally
- `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirInferenceLogger.kt`
  - Core concrete logger model and methods to mirror/adapt:
    - `BlockElement`, `BlockOwner`, `BlockItemElement`
    - `NewVariableElement`, `ErrorElement`, `ConstraintElement`, `InitialConstraintElement`, `VariableConstraintElement`
    - `topLevelElements`, system/block ownership tracking, origin caching
    - `logCandidate`, `logStage`, `logInitial`, `log`, `logError`, `logNewVariable`, `logReadiness`, `withOrigin`, `withOrigins`, `logFixVariable`
- `external/kotlin/compiler/fir/entrypoint/src/org/jetbrains/kotlin/fir/session/FirAbstractSessionFactory.kt`
  - Confirms source-session registration pattern already mirrored locally.

### Optional but strongly recommended
- Keep upstream-style `Call` / candidate ownership metadata from `FirInferenceLogger.kt`
  - Not required by the current local handler, but likely useful if renderer richness increases later.
- Keep upstream-style readiness/fixation record behavior
  - Local handler currently expects a simpler `FixVariableElement`, but upstream uses fixation records plus later fix propagation.

### Test-only upstream references
- `external/kotlin/compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/frontend/fir/handlers/FirInferenceLogsHandler.kt`
- `external/kotlin/compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/utils/inferencelogs/FirInferenceLogsDumper.kt`
- `external/kotlin/compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/utils/inferencelogs/MarkdownInferenceLogsDumper.kt`
- `external/kotlin/compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/utils/inferencelogs/MermaidInferenceLogsDumper.kt`
- `external/kotlin/compiler/tests-common-new/testFixtures/org/jetbrains/kotlin/test/utils/inferencelogs/FixationOnlyInferenceDumper.kt`
  - These are renderer/format references only, not runtime dependencies for the first local port.

## Task 2 implication
- The current local `InferenceLogger` base class needed the generic callback surface required by upstream-shaped concrete logging:
  - variable-constraint logging (`log(variable, constraint, context)`)
  - error logging (`logError(error, context)`)
  - fix-variable logging (`logFixVariable(variable, resultType, context)`)
- Local types needed for these generic signatures already exist in `:resolution.common`:
  - `ConstraintSystemError` in `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/model/ConstraintPositionAndErrors.kt`
  - `Constraint` / `InitialConstraint` in `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/model/ConstraintStorage.kt`
  - `TypeVariableMarker` and `CangJieTypeMarker` are already used pervasively in the same inference package.
