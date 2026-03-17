# CHIR/Codegen State Inventory (Task 1.1)

## Scope

This inventory captures the current Kotlin implementation status for:

- `compiler:chir` (CHIR model and verifier-facing contracts)
- `compiler:codegen` (`CHIR -> LLVM IR` lowering pipeline)

It focuses on implemented surfaces, missing semantics, and known incorrect
behavior that blocks full parity with `external/cangjie_compiler`.

## Implemented Surfaces

- CHIR core data model exists in `compiler/chir/src/.../core/*`:
  - package/module/declaration/type/value/expression/control-flow families
  - semantic id and traversal/analysis utilities
- CHIR baseline tests exist in `compiler/chir/tests/...`:
  - model/transform/serializer/checker/pipeline coverage
  - codegen parity smoke tests (`ChirToLlvmLoweringParityTest`)
- Kotlin codegen skeleton exists in `compiler/codegen/src/...`:
  - `DefaultChirToLlvmCodeGenerator` entry and module partition mode
  - module/function lowerers (`CGModule`, `CGFunction`)
  - expression dispatcher (`ExpressionLoweringDispatcher`)
  - type lowering and runtime symbol table scaffolding

## Missing or Incomplete Semantics

- CHIR parity with official C++ model is not complete:
  - see `compiler/chir/docs/chir-parity-gap-register.md` (`GAP-CHIR-001` to `GAP-CHIR-014`)
- LLVM lowering coverage is partial:
  - expression family support remains operator-string based and incomplete
  - control-flow/phi/unwind-exception semantics are not fully modeled
  - calling convention/runtime integration is only partial skeleton
- Replaceable LLVM backend abstraction is now introduced, but native interop
  backend capability is still early-stage:
  - current backend policy is JNI-only and fails fast when native interop
    toolchain is unavailable

## Known Incorrect Behavior

- Unsupported expressions/operations previously degraded into IR comments instead
  of failing fast, allowing invalid output continuation.
- Terminator fallback path accepted unknown kinds as comments instead of diagnostics.
- Baseline parity fixtures are minimal (`simple-return` only), insufficient for
  full parity regression confidence.

## Current Verification Baseline

- Existing parity fixture root:
  - `compiler/codegen/testResources/chir-parity`
- Existing baseline sample:
  - `simple-return` (`baseline/simple-return.chir.json`,
    `cpp-baseline/simple-return.llvmir.txt`)
- Existing parity test entry:
  - `compiler/chir/tests/.../ChirToLlvmLoweringParityTest.kt`

## Immediate Next Gap Closures

1. Expand representative baseline corpus from official compiler outputs.
2. Replace string-dispatch lowering with explicit semantic families.
3. Stabilize native-interop backend tool contract and production deployment.
4. Add structural + normalized text parity gates in CI.
