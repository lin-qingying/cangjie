# Official C++ CodeGen to Kotlin Mapping Matrix

This document maps the official C++ backend architecture to the Kotlin backend modules and records the current boundary of `:compiler:codegen`.

## Source Baseline

Primary official sources:

- `external/cangjie_compiler/include/cangjie/CodeGen`
- `external/cangjie_compiler/src/CodeGen`

Current Kotlin targets:

- `compiler:codegen`
- CLI entry glue in `compiler/cli`

## Architecture Matrix

| Official C++ area | Representative files | Current Kotlin status | Kotlin target |
|---|---|---|---|
| Package-to-module entry | `include/cangjie/CodeGen/EmitPackageIR.h` | Missing | `compiler:codegen` package-level entry API |
| Global codegen context | `src/CodeGen/CGContext.h`, `CGPkgContext.cpp` | Missing | `codegen/context` |
| Module lowering | `src/CodeGen/CGModule.h`, `CGModule.cpp` | Missing | `codegen/module` |
| Function lowering | `src/CodeGen/CGFunction.h`, `CGFunction.cpp` | Missing | `codegen/function` |
| Block lowering | `src/CodeGen/EmitBasicBlockIR.cpp` | Missing | `codegen/function` or `codegen/block` |
| Expression lowering | `src/CodeGen/EmitExpressionIR.cpp` | Missing | `codegen/expression` |
| IR builder wrapper | `src/CodeGen/IRBuilder.h`, `IRBuilder.cpp` | Missing | `codegen/ir` |
| Type lowering and layout | `src/CodeGen/Base/CGTypes/*` | Missing | `codegen/types` |
| Dispatcher-based lowering | `src/CodeGen/Base/ExprDispatcher/*` | Missing | `codegen/dispatcher` |
| Runtime helpers and utils | `src/CodeGen/Utils/*`, helper calls in lowering code | Missing | `codegen/runtime`, `codegen/utils` |
| Parity tests and baselines | official backend outputs | Minimal test resources only | `compiler/codegen/testResources/chir-parity` and tests |

## Current Kotlin boundary

`:compiler:codegen` receives a `ChirCodegenInput` whose source is the CHIR package model from `:chir:chir-tree`. The module owns LLVM-facing code generation and its parity checks; CHIR construction belongs to `:chir:cfir2chir`, and CFIR construction and resolution remain upstream.

## Entry-Point Mapping

| Official entry | Purpose | Kotlin parity target |
|---|---|---|
| `GenPackageModules(...)` | Convert CHIR package(s) into one or more LLVM modules | `ChirToLlvmCodeGenerator.generate(...)` style entry in `compiler:codegen` |
| `CGModule` | Own module-wide lowering state | Kotlin module lowering class |
| `CGFunction` | Own per-function lowering state | Kotlin function lowering class |
| `IRBuilder2` | Wrap LLVM IRBuilder with runtime-aware helpers | Kotlin IR builder facade |

## Lowering Coverage Matrix

| Official lowering family | Representative implementation | Kotlin status |
|---|---|---|
| Apply / call lowering | `Base/ApplyImpl.cpp`, dispatcher files | Missing |
| Allocation lowering | `Base/AllocateImpl.cpp` | Missing |
| Arithmetic lowering | `Base/ArithmeticOpImpl.cpp` | Missing |
| Array / tuple / varray lowering | `Base/ArrayImpl.cpp`, `TupleExprImpl.cpp`, `VArrayExprImpl.cpp` | Missing |
| Type cast lowering | `Base/TypeCastImpl.cpp` | Missing |
| Terminator lowering | `Base/ExprDispatcher/TerminatorExprDispatcher.cpp` | Missing |
| Runtime / intrinsic bridging | `IRBuilder.h`, `Utils/CGUtils.*` | Missing |
| Type info and layout | `Base/CGTypes/CGType.cpp` and related files | Missing |

## Related Baselines

- [CHIR mapping](../../../chir/chir-tree/docs/cpp-chir-mapping.md)
- [Codegen module](../README.md)
- [CHIR subsystem](../../../chir/README.md)
