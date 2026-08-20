# Development conventions

[中文](DEVELOPMENT_CONVENTIONS.zh-CN.md) | [Documentation](docs/README.md)

These conventions apply to every first-party module included by settings.gradle.kts. They define repository-wide engineering expectations; module contracts and language semantics remain governed by their implementation and official Cangjie sources.

## Core principles

- Prefer readability, explicit contracts, and stable domain models over clever or implicit control flow.
- Keep module boundaries and dependency directions explicit; expose independent capabilities through interfaces rather than leaking implementations.
- Prefer immutable public values and read-only collections unless mutation is an essential part of the contract.
- Document non-obvious invariants, ownership, lifecycle, failure semantics, and intentional deviations from Kotlin K2.

## Implementation and module design

- Give each file a single cohesive responsibility and keep extension APIs in dedicated files when that improves discovery.
- Name types, functions, and modules by their domain responsibility; remove obsolete transitional names when changing the relevant boundary.
- Use structured compiler errors and diagnostics for normal invalid-program paths. Reserve exceptions and require/check for failed programmer invariants.
- Keep public contracts explicit about input, output, nullability, failure, threading, and lifecycle semantics.

## Quality and change control

- Cover unit behaviour, module/phase integration, and critical end-to-end paths at the appropriate layer.
- Preserve diagnosability in compiler phases through stable diagnostics, contextual logging, and traceable state.
- Run the narrow affected build and tests before broad verification; do not mix unrelated refactors into a feature or repair.
- Update maintained documentation whenever a module boundary, architecture contract, or test strategy changes.

## Documentation ownership

The module list is maintained in [docs/module-catalog.md](docs/module-catalog.md). Documentation validation is part of the root Gradle check lifecycle.
