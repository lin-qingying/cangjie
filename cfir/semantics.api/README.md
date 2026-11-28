# CFIR semantics API

[中文](README.zh-CN.md)

This module owns the shared semantic contracts and model operations used by CFIR providers, resolve, checkers and analysis. Its placement follows Kotlin FIR `semantics.api`; Cangjie declaration and type semantics determine the supported operations.

The dependency direction is `providers → semantics.api → cfir-tree → cfir-cones`. Semantic contracts depend on the tree model. Resolver, source-index, binary-loader and checker implementations are owned by their respective modules.

| Package | Ownership |
| --- | --- |
| `cfir.resolve.providers` | Symbol and declaration provider contracts, names and import namespaces, declaration metadata queries, supertype and extend contracts, and callable-owner substitutions. |
| `cfir.scopes.impl` | Shared signature copying and class substitution scopes, inheritance identities and provenance, and lookup completeness contracts. |
| `cfir.resolve.substitution` | Cone substitution interfaces and implementations, including symbol-keyed and constructor-keyed substitutions and substitution chains. |
| `cfir.types` and `cfir.resolve` | Type contexts, recursive type operations, lookup-tag bindings, alias expansion and semantic supertype queries. |
| `cfir.declarations` and `cfir.expressions` | Declaration overloadability contracts and resolved annotation semantic facts. |
| `cfir.session` | Session accessors for semantic services whose contracts remain in this layer. |

`symbolProvider`, `cfirProvider`, `dependenciesSymbolProvider` and the dependency-provider key are declared in `cfir.resolve.providers`. Consumers import those actual declarations; other session accessors retain their own packages.

[CfirFakeOverrideGenerator](src/org/cangnova/cangjie/cfir/scopes/impl/CfirFakeOverrideGenerator.kt) and [CfirClassSubstitutionScope](src/org/cangnova/cangjie/cfir/scopes/impl/CfirClassSubstitutionScope.kt) share signature copying. Copies own fresh callable and parameter symbols, rebind their type parameters and explicit return types, preserve ordinal and provenance information, and require resolved declaration status. Signature copies do not share executable bodies. Constructor default-presence stubs preserve the default contract without producing executable default helpers.

`CfirExtendProvider.modificationCount` is mandatory. Source and lazy CJO providers expose completed index revisions; immutable empty providers explicitly declare zero. Type-aware supertype queries keep extend and graph revisions separate and check them after resolution and before publishing cached descriptors.

The tree module owns generated declaration builders, status values and expression-stub implementations. Source export analysis and its frozen input products remain in [providers](../providers/README.md); CJO encoding and binary package snapshots remain in `:cfir:cfir-serialization`.

Constructor-identity mappings use `createTypeSubstitutorByTypeConstructor` with the actual type context; declaration-symbol mappings use `substitutorByMap` with the actual session. Both factories belong to `cfir.resolve.substitution` and share recursive substitution and attribute handling. The former `cfir.types.CfirTypeSubstitutorByMap` has been removed. The complete SOURCE payload, default-helper lifecycle, and remaining LL/Analysis API validation are tracked in the [CJMP audit](../../docs/cjmp-progress-audit-20261008.md).

On Windows, use the queued Gradle entrypoint:

```powershell
.\gradlew-queue.bat :cfir:semantics.api:assemble
.\gradlew-queue.bat :cfir:providers:test --tests "*CfirFakeOverrideGeneratorTest"
```
