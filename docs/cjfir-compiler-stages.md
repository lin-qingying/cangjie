# Cangjie frontend stages

[中文](cjfir-compiler-stages.zh-CN.md) | [Architecture](project-architecture-diagram.md) | [Module catalog](module-catalog.md)

This document describes the current frontend boundaries. It is not a historical implementation plan: source code, `settings.gradle.kts`, and the test suite are the source of truth when this text and the implementation differ.

## Frontend flow

```text
source files
  → parse to PSI or LightTree
  → construct Raw CFIR
  → construct macro-expanded source inputs when macros are present
  → register source providers
  → resolve declarations and bodies
  → collect and render diagnostics
  → expose analysis/serialization/backend integration points
```

Parsing and Raw CFIR construction are supplied by `:psi`, `:cfir:raw-cfir:psi2cfir`, and `:cfir:raw-cfir:light-tree2cfir`. Session and frontend assembly live in `:cfir:entrypoint`; ordinary semantic resolution lives in `:cfir:resolve`; diagnostics are supplied by `:cfir:checkers` and `:cfir:diagnostic-renderers`.

## Macro boundary

Macro construction is a frontend preparation step, not an ordinary resolve phase. Expanded raw files are recorded before the ordinary source-provider registration becomes final. The architecture guard in `:compiler:frontend` enforces that `CfirResolvePhase` does not reintroduce `MACRO_EXPAND`.

## Ordinary CFIR resolve phases

`CfirResolvePhase` represents declaration-level, lazily reachable semantic states:

```text
RAW_CFIR
  → IMPORTS
  → SUPER_TYPES
  → TYPES
  → STATUS
  → EXTENSIONS
  → IMPLICIT_TYPES
  → BODY_RESOLVE
```

- `RAW_CFIR` is a structural state marker after syntax conversion.
- `IMPORTS` binds import names and packages.
- `SUPER_TYPES`, `TYPES`, and `STATUS` establish the declaration header and inheritance contract.
- `EXTENSIONS` resolves Cangjie extension declarations.
- `IMPLICIT_TYPES` stabilizes omitted declaration-level types.
- `BODY_RESOLVE` resolves expressions, calls, overloads, and body-level inference.

`:cfir:checkers` runs the diagnostics pipeline after the required resolve information is available. It is intentionally not an enum member of `CfirResolvePhase`.

## Outputs and consumers

| Output | Main consumers |
| --- | --- |
| PSI / LightTree | Raw CFIR builders, editor services, tests |
| Resolved CFIR | Analysis API, code insight, serialization, optional backends |
| Diagnostics | Compiler and Analysis API diagnostic consumers |
| `.cjo` integration | Cross-module symbol loading and decompilation |
| CHIR | JVM and LLVM backend integrations |

See [the architecture diagram](project-architecture-diagram.md) for subsystem ownership and [the module catalog](module-catalog.md) for all Gradle modules.
