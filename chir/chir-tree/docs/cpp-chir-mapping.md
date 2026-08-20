# Official C++ CHIR to Kotlin CHIR Mapping Matrix

It answers two questions:

1. Which official C++ CHIR areas must be mirrored in Kotlin?
2. Which Kotlin files currently carry that responsibility, and what is still missing?

The scope here is CHIR only. Who produces CHIR is intentionally out of scope.

## Source Baseline

Primary official sources:

- `external/cangjie_compiler/include/cangjie/CHIR`
- `external/cangjie_compiler/src/CHIR`
- `external/cangjie_compiler/include/cangjie/CHIR/Serializer`
- `external/cangjie_compiler/src/CHIR/Serializer`

Primary Kotlin targets:

- `chir/chir-tree/src/org/cangnova/cangjie/chir/core`
- `chir/chir-tree/tests/org/cangnova/cangjie/chir/core`

## Domain Matrix

| Official C++ domain | Representative C++ files | Current Kotlin files | Status | Notes |
|---|---|---|---|---|
| Context and package ownership | `CHIRContext.h`, `Package.h`, `CHIRBuilder.h` | `context/ChirContext.kt`, `context/DefaultChirContext.kt`, `model/ChirPackageModel.kt`, `builder/ChirBuilder.kt` | Partial | Kotlin has a basic context, package and module model, but package payload is much smaller than official `Package`. |
| Base node and semantic identity | `Base.h`, `Annotation.h`, `DebugLocation.h` | `model/ChirNode.kt`, `identity/ChirSemanticId.kt`, `attribute/ChirAttribute.kt` | Partial | Stable IDs exist, but official annotation, attribute and debug metadata are not fully mirrored. |
| Declarations and function bodies | `Value.h`, `Package.h`, function-related definitions under `Type/*` | `declaration/ChirDeclaration.kt`, `model/ChirPackageModel.kt`, `controlflow/ChirControlFlow.kt` | Partial | Basic declarations exist, but imported/global/custom type declaration families are incomplete. |
| Value hierarchy | `Value.h` | `value/ChirValue.kt` | Partial | Kotlin only models locals and constants; official `ValueKind` is much wider. |
| Type hierarchy | `Type/Type.h`, `Type/*.h` | `type/ChirType.kt` | Partial | Primitive, named, tuple and function types exist, but official aggregate, generic, reference and runtime-facing types are not fully represented. |
| Expression hierarchy | `Expression/Expression.h`, `Expression/ExprKind.inc`, `Expression/*.h` | `expression/ChirExpression.kt`, `expression/ChirExpressionDispatcher.kt` | Partial | Kotlin uses a reduced domain model, not the official full `ExprKind` family. |
| Terminators and CFG | `Expression/Terminator.h`, CFG-related utilities | `controlflow/ChirControlFlow.kt`, `checker/ChirValidator.kt` | Partial | Blocks and terminators exist, but successor, unwind and block-group semantics need expansion. |
| Symbols and reference binding | `Value.h`, `Utils.h`, builder helpers | `symbol/ChirSymbol.kt`, `symbol/ChirSymbolTable.kt`, `symbol/ChirReferenceBinder.kt` | Partial | Symbol table exists, but official identity, ownership and use-def semantics are not yet complete. |
| Verifier and invariant checking | `IRChecker.h`, checker sources under `src/CHIR` | `checker/ChirValidator.kt`, `checker/ChirValidationReport.kt`, `model/ChirInvariants.kt` | Partial | Kotlin has a verifier entry point, but it still validates the reduced model rather than full official semantics. |
| Visitor and traversal | visitor headers and traversal helpers | `visitor/ChirVisitor.kt`, `expression/ChirExpressionDispatcher.kt` | Partial | Visitor framework exists, but node coverage is limited by the reduced hierarchy. |
| Analysis | `Analysis/*` | `analysis/ChirDataFlowEngine.kt`, `analysis/ChirBaselineAnalyses.kt`, `analysis/ChirAnalysisResultProvider.kt` | Partial | Framework exists, but only a baseline subset is implemented. |
| Transformation and rewrite | transformation sources under `src/CHIR` | `transformation/ChirRewriteSession.kt`, `transformation/ChirTransformations.kt` | Partial | Infrastructure exists, but official pass parity is not established yet. |
| Pipeline and cache | optimization driver code in `CHIR.cpp` and analysis wrappers | `pipeline/*.kt` | Partial | Kotlin has scheduler and cache primitives, but pass ordering still follows Kotlin-local assumptions. |
| Serializer and deserializer | `Serializer/CHIRSerializer.h`, `Serializer/CHIRDeserializer.h` | `serializer/ChirPackageCodec.kt`, `serializer/ChirSerializationSchema.kt`, `serializer/ChirSerializationGate.kt` | Partial | FlatBuffers package format is used, but several fields still reflect Kotlin transitional semantics. |
| Printer and inspect tooling | `CHIRPrinter.h`, dump utilities | `printer/ChirPrinter.kt`, `printer/ChirInspector.kt` | Partial | Stable printer exists, but it prints the reduced model, not the full official node family. |
| Tests and reference assets | official tests and sample packages | `tests/...`, `testResources/chir-reference` | Partial | Regression suite exists, but official parity coverage is still narrow. |

## Node and Relation Matrix

| Official C++ concept | Representative source | Current Kotlin counterpart | Current state |
|---|---|---|---|
| `Package` | `Package.h` | `ChirPackage` | Package contains `name` and `modules` only; official package-level globals, imports, type defs and init funcs are missing. |
| `Module` / package partition | package/codegen split context | `ChirModule` | Exists as a simple declaration container. |
| `FuncBase` / `Func` | `Value.h` | `ChirFunctionDeclaration` and CFG declarations | Basic function shape exists; full parent, wrapper, imported and metadata semantics are incomplete. |
| `Block` | `Value.h` | `ChirBlock` | Exists, but official successor and block-group relationships need widening. |
| `BlockGroup` | `Value.h`, expression hierarchy | No full equivalent | Missing as a first-class structure. |
| `Parameter` | `Value.h` | Partial via declarations/control-flow payload | Not modeled as a first-class value kind in Kotlin. |
| `LocalVar` | `Value.h` | `ChirLocalValue` | Present, but without official user/ownership metadata. |
| `Literal` | `Value.h` | `ChirConstantValue` | Present as a string-backed literal; official literal kinds are richer. |
| `GlobalVar` / imported globals | `Value.h`, `Package.h` | No full equivalent | Missing or folded into declarations. |
| `ImportedFunc` / `ImportedVar` | `Value.h` | No full equivalent | Missing. |
| `ExprKind` family | `Expression/ExprKind.inc` | `ChirUnaryExpression`, `ChirBinaryExpression`, `ChirMemoryExpression`, `ChirCallExpression`, `ChirOtherExpression` | Reduced approximation only. |
| `Terminator` family | `Expression/Terminator.h` | Return/branch-related nodes in control-flow model | Partial; official `GoTo`, `Branch`, `MultiBranch`, `Exit`, unwind variants are not all mirrored. |
| Type definitions (`ClassDef`, `StructDef`, `EnumDef`, `ExtendDef`) | `Type/*.h` | No complete one-to-one family yet | Missing full custom type definition parity. |
| Annotation and debug location | `Annotation.h`, `DebugLocation.h` | Partial via `ChirAttribute` and node IDs | Missing as first-class full-fidelity metadata families. |

## Field and Relation Focus Areas

The next implementation stages must explicitly preserve these official semantics:

- Stable semantic identity for every referencable entity.
- Parent links:
  - value -> parent function or container
  - expression -> parent block / block group / function
  - block -> top-level function
- Use-def links:
  - value users
  - expression operands
- CFG links:
  - terminator successors
  - predecessor/successor consistency
  - exceptional edges
- Package membership:
  - globals
  - imported members
  - custom type definitions
  - init functions
- Metadata:
  - attributes
  - annotations
  - debug/source location payload

## Serialization Alignment Focus

Official serialization parity must be evaluated against:

- package header and package membership fields
- type vectors
- value vectors
- expression vectors
- union kinds and payload fields
- cross-reference encoding and ID stability

Current Kotlin implementation status:

- `ChirPackageCodec.kt` already writes `PackageFormat.CHIRPackage`.
- The internal mapping is still shaped around the reduced Kotlin model.
- Full official field semantics remain an active parity task.

## Related documentation

- [CHIR module boundary](module-boundary.md)
- [Codegen mapping](../../../compiler/codegen/docs/cpp-codegen-mapping.md)
- [CHIR subsystem](../../README.md)
