# CHIR Module Boundary

This file defines package boundaries inside `compiler:chir`.

## Core domains

- `core.model`: package/module graph and structural invariants.
- `core.declaration`, `core.type`, `core.value`, `core.expression`, `core.controlflow`: CHIR semantic nodes.
- `core.identity`, `core.symbol`: stable ids and symbol/reference contracts.
- `core.context`: package registration and lookup context.

## Construction and validation

- `core.builder`: controlled node registration and build-time checks.
- `core.checker`: validation rules and report formatting.

## Execution framework

- `core.pipeline`: pass metadata, scheduler, execution records, analysis invalidation.
- `core.analysis`: baseline analyses and result provider.
- `core.transformation`: rewrite session and baseline transformations.

## IO and tooling

- `core.serializer`: CHIR serialization/deserialization gate and round-trip assertions.
- `core.printer`: canonical textual output and structured inspect output.

## Boundary rules

- Public APIs should depend on interfaces/contracts, not concrete implementation details from unrelated subdomains.
- Serialization and pipeline entry points must run validation gates before emitting externally consumed artifacts.
- Test-only fixtures and diff-report helpers are kept under `compiler/chir/tests/.../testkit` and are not production API.
