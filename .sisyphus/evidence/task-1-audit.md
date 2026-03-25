# Task 1 Audit — FirInferenceLogger Local Surface

## Confirmed present first-party seams

### Generic logger API
- `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/InferenceLogger.kt`
  - Present.
  - Current API is thin: `logInitial`, `logNewVariable`, `logReadiness`, `withOrigin`, `withOrigins`, `Dummy`.
  - No generic callbacks yet for variable-constraint logging, error logging, or fix-variable logging.

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

## Confirmed missing or not-discoverable first-party seams

### Concrete logger source
- No first-party `CfirInferenceLogger` source file was found by file glob under the repository.
- The only logger source discovered by name is upstream Kotlin:
  - `external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/inference/FirInferenceLogger.kt`

### Session accessor
- No `val CfirSession.inferenceLogger ...` accessor is currently present in first-party `cfir/**` sources.
- Existing session extension file:
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/session/CfirSessionExtensions.kt`
  - This is the strongest local accessor landing point because it already hosts other resolve-session accessors.

## Current mismatch summary
- Runtime and test layers already assume a concrete `CfirInferenceLogger` exists.
- Resolve and generic inference layers already consume `session.inferenceLogger`.
- However, the discoverable first-party concrete logger source and the session accessor are missing.
- Therefore the repository currently looks like a partial port where call sites and test expectations were added before the concrete implementation landed.

## Tentative local landing paths
- Concrete logger source: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirInferenceLogger.kt`
- Session accessor: `cfir/resolve/src/org/cangnova/cangjie/cfir/session/CfirSessionExtensions.kt`

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
- The current local `InferenceLogger` base class is missing the generic callback surface needed by upstream-shaped concrete logging:
  - variable-constraint logging (`log(variable, constraint, context)`)
  - error logging (`logError(error, context)`)
  - fix-variable logging (`logFixVariable(variable, resultType, context)`)
- Local types needed for these generic signatures already exist in `:resolution.common`:
  - `ConstraintSystemError` in `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/model/ConstraintPositionAndErrors.kt`
  - `Constraint` / `InitialConstraint` in `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/model/ConstraintStorage.kt`
  - `TypeVariableMarker` and `CangJieTypeMarker` are already used pervasively in the same inference package.
