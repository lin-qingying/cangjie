# Task 1 Boundary Audit — Generic vs CFIR-specific Placement

## Generic module observations

### `:resolution.common`
- `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/InferenceLogger.kt`
  - Contains only generic logger API and generic fixation record payloads.
  - Does **not** reference CFIR packages.
- Searches across current logger-related files in `resolution.common/src` show only generic inference model/component usage.

## CFIR-specific expectations already outside `:resolution.common`
- `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/frontend/CfirInferenceLogsHandler.kt`
  - Expects CFIR-specific structured rendering model.
- `cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirAbstractSessionFactory.kt`
  - Registers `CfirInferenceLogger` directly.
- `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/ResolutionStageRunner.kt`
  - Calls logger methods tied to resolve candidate/stage processing.

## Boundary conclusion
- No evidence currently suggests moving structured block/item logger models into `:resolution.common`.
- The clean split remains:
  - Generic callback contract in `:resolution.common`
  - Concrete structured logger model in `:cfir:resolve`
  - Runtime activation in `:cfir:entrypoint` / `compiler:config`
  - Rendering/assertion in `:tests:test-infrastructure`
