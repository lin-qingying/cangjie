# CHIR Parity Gap Register

This register captures task `1.4`: the known gaps between the current Kotlin
implementation and the official C++ CHIR contract.

## Severity Guide

- `P0`: blocks official CHIR or LLVM IR parity immediately
- `P1`: major semantic mismatch that will break serializer, verifier or lowering
- `P2`: important tooling or coverage gap

## Active Gaps

| ID | Severity | Area | Current Kotlin state | Required official parity |
|---|---|---|---|---|
| GAP-CHIR-001 | P0 | Package model | `ChirPackage` only stores `name` and `modules` | Mirror official package globals, imports, custom type defs, init functions and package metadata |
| GAP-CHIR-002 | P0 | Value hierarchy | `ChirValue` only covers locals and constants | Mirror official `ValueKind` families including globals, parameters, funcs, blocks, block groups and imported values |
| GAP-CHIR-003 | P0 | Expression hierarchy | Reduced domain model (`UNARY/BINARY/MEMORY/CALL/OTHERS`) | Mirror official `ExprKind` / `ExprMajorKind` class family |
| GAP-CHIR-004 | P0 | Terminators | Control-flow model only covers a subset of terminators | Mirror official terminator kinds, successors and unwind semantics |
| GAP-CHIR-005 | P0 | Type hierarchy | Primitive, named, tuple and function types only | Mirror official aggregate, generic, ref, box, pointer and runtime-facing types |
| GAP-CHIR-006 | P0 | Ownership and relations | Parent/use-def/CFG links are incomplete | Preserve official parent, user, block-group, top-level function and successor semantics |
| GAP-CHIR-007 | P1 | Serializer payload | Uses official package format but transitional Kotlin-centric field mapping | Expand to official field-by-field semantics |
| GAP-CHIR-008 | P1 | Annotation and debug metadata | No full annotation/debug-location family | Mirror official metadata structures and attachment points |
| GAP-CHIR-009 | P1 | Imported members | Imported funcs/vars/types are not modeled as first-class CHIR entities | Add imported declaration/value parity |
| GAP-CHIR-010 | P1 | Custom type defs | No complete one-to-one `ClassDef/StructDef/EnumDef/ExtendDef` model | Add explicit definition families and their fields |
| GAP-CHIR-011 | P1 | Verifier scope | Verifier validates current reduced model only | Enforce official field, relation and CFG invariants before serializer and codegen |
| GAP-CHIR-012 | P1 | Printer and inspect | Output is stable for Kotlin model only | Print full official node family and parity-critical metadata |
| GAP-CHIR-013 | P2 | Reference baselines | Existing `chir-reference` assets are narrow | Add broader official sample coverage and structured manifests |
| GAP-CHIR-014 | P2 | CLI/backend entry coupling | Current CLI stage is commented out | Replace with active backend pipeline once codegen parity skeleton lands |

## Transitional Implementations To Remove

These are known transitional choices that should not survive parity work:

- String-backed literal payload without official literal kind taxonomy
- Reduced expression domain dispatch instead of official `ExprKind` family
- `ChirPackage` as package -> modules only
- Incomplete value kinds that collapse parameters, globals and imported entities
- Serializer mappings that encode Kotlin-local structure instead of official schema intent
- Commented placeholder codegen stage in
  [ChirToLlvmCodegenStage.kt](/D:/code/intellij/cangjie/compiler/cli/src/org/cangnova/cangjie/cli/pipeline/ChirToLlvmCodegenStage.kt)

## Validation Targets

Every gap above must eventually be closed by at least one of:

- model parity tests
- serializer round-trip tests
- official CHIR diff tests
- LLVM IR parity tests
