# Draft: FirInferenceLogger Port

## Requirements (confirmed)
- Port Kotlin compiler `FirInferenceLogger` into this project.
- Do not do a minimal implementation.
- Port necessary upstream dependencies as well.
- Align the port with existing module and file boundaries in this repository.
- Completion standard: full debug capability first; align upstream logger structure, call chain, necessary dependencies, and test log artifacts for inference debugging/regression use.
- Initial port must also include a runtime/compiler-facing activation path, not just test-only wiring.

## Technical Decisions
- Planning target is the CFIR resolve / type inference pipeline, not unrelated compiler stages.
- Upstream Kotlin sources under `external/kotlin/` are reference-only and should inform first-party module placement.
- The implementation plan must include dependency triage so only necessary upstream support classes are ported, but feature parity should favor fidelity over stubbing.
- Prefer production-quality internal debug instrumentation parity over a test-only shim.
- Session registration must cover source-session runtime configuration via compiler configuration, following `CfirAbstractSessionFactory`'s existing `dumpInferenceLogs` hook.
- Primary landing modules are `:resolution.common` for generic logger API deltas, `:cfir:resolve` for concrete CFIR logger/event wiring, `:cfir:entrypoint` for session registration, and existing test modules for verification.

## Research Findings
- `README.md`: `:cfir:resolve` owns type inference, overload resolution, and diagnostics.
- `README.md`: `:resolution.common` already contains migrated constraint-system and type-inference infrastructure.
- `README.md`: `CfirInferenceLogsHandler` already exists in test infrastructure, implying some inference-log test contract is present.
- `cjfir-compiler-stages.md`: CFIR resolve phase includes `IMPLICIT_TYPES`, `BODY_RESOLVE`, and `CHECKERS`, which are the likely insertion points for inference logging.
- `resolution.common/.../InferenceLogger.kt` is already present but much thinner than Kotlin upstream: it only exposes `logInitial`, `logNewVariable`, `logReadiness`, `withOrigin`, and `withOrigins`.
- `cfir/entrypoint/.../CfirAbstractSessionFactory.kt` already conditionally registers `CfirInferenceLogger` when `configuration.dumpInferenceLogs` is enabled, confirming a runtime config seam already exists.
- `tests/test-infrastructure/.../CfirInferenceLogsHandler.kt` expects a concrete `CfirInferenceLogger` model with top-level blocks, candidate ownership, new-variable/constraint/error/fix-variable elements, and golden side-file rendering.
- Upstream source of truth is `external/kotlin/compiler/fir/resolve/.../FirInferenceLogger.kt`, whose API surface is broader than local `InferenceLogger` and includes candidate/stage logging, variable constraint logging, error logging, and fix-variable logging.

## Open Questions
- What counts as “done”: compile-only parity, golden-log parity on representative tests, or broader integration across all inference entry points?
- Should the plan include follow-on updates to existing inference log handlers/testdata if upstream log format differs?
- Should the initial port preserve the current local `.cfir.inference.txt` textual format where possible, or intentionally converge to Kotlin upstream log structure/output even if existing golden files need broad churn?

## Scope Boundaries
- INCLUDE: CFIR resolve/inference logging architecture, required dependency ports, tests, module-boundary alignment.
- EXCLUDE: unrelated frontend pipeline stages, changes under `external/`, and non-planning code edits in source modules.
