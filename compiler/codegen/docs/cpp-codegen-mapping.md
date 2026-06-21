# Official C++ CodeGen to Kotlin Mapping Matrix

This document covers task `1.3` of `port-cpp-chir-to-cangjie`.

Its job is to map the official C++ backend architecture to the Kotlin modules
that must be created or expanded for parity.

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

## Current Kotlin Reality

The `compiler:codegen` module currently contains:

- [README.md](/D:/code/intellij/cangjie/compiler/codegen/README.md)
- [build.gradle.kts](/D:/code/intellij/cangjie/compiler/codegen/build.gradle.kts)
- minimal parity fixture resources under
  `compiler/codegen/testResources/chir-parity`

It does not currently contain:

- source files under `compiler/codegen/src`
- active lowering pipeline implementation
- LLVM context/module/function abstractions
- dispatcher or type-lowering code

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

## Immediate Build-Out Order

Recommended Kotlin build-out order:

1. `codegen/api`
2. `codegen/context`
3. `codegen/module`
4. `codegen/function`
5. `codegen/ir`
6. `codegen/types`
7. `codegen/dispatcher`
8. `codegen/runtime`
9. parity tests

## Related Baselines

- CHIR mapping: [cpp-chir-mapping.md](/D:/code/intellij/cangjie/chir/chir-tree/docs/cpp-chir-mapping.md)
- Baseline fixtures: [README.md](/D:/code/intellij/cangjie/compiler/codegen/testResources/chir-parity/README.md)
