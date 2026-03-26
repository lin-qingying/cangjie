# Task 6 Activation Path Audit

## Confirmed enabled path

### Compiler configuration key
- `compiler/config/src/org/cangnova/cangjie/config/CommonConfigurationKeys.kt`
  - Defines `CompilerConfiguration.dumpInferenceLogs`
  - Getter reads `CommonConfigurationKeys.DUMP_INFERENCE_LOGS`
  - Setter writes `CommonConfigurationKeys.DUMP_INFERENCE_LOGS`

### Test directive to configuration mapping
- `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/directives/CfirDiagnosticsDirectives.kt`
  - Declares `DUMP_INFERENCE_LOGS`
- `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/services/EnvironmentConfigurator.kt`
  - Maps `CfirDiagnosticsDirectives.DUMP_INFERENCE_LOGS` to `CommonConfigurationKeys.DUMP_INFERENCE_LOGS`

### Source-session registration
- `cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirAbstractSessionFactory.kt`
  - In `createSourceSession(...)`:
    - `if (configuration.dumpInferenceLogs) register(CfirInferenceLogger::class, CfirInferenceLogger())`

### Session access path
- `cfir/resolve/src/org/cangnova/cangjie/cfir/session/CfirSessionExtensions.kt`
  - Exposes `val CfirSession.inferenceLogger: CfirInferenceLogger? by CfirSession.nullableSessionComponentAccessor()`

## Confirmed disabled path
- The registration in `CfirAbstractSessionFactory.createSourceSession(...)` is conditional.
- No alternative unconditional registration of `CfirInferenceLogger` was found in first-party code.
- Therefore when `configuration.dumpInferenceLogs == false`, source sessions should not register the component and the accessor should resolve to `null`.

## Scope conclusion
- Task 6 required no runtime code change: the activation seam was already correct.
- Remaining work to observe the enabled path end-to-end belongs to later handler/golden tasks that exercise existing test infrastructure with `DUMP_INFERENCE_LOGS` enabled.
