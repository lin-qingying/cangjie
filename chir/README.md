# CHIR

The `chir/` subtree contains the Cangjie High-level IR boundary. It is a Gradle aggregation namespace rather than a standalone compiler entry point.

| Module | Responsibility |
| --- | --- |
| `:chir` | Aggregates the CHIR tree and frontend IR dependencies for consumers |
| `:chir:chir-tree` | CHIR model, builder, validation, passes, serialization, printing, and inspection |
| `:chir:cfir2chir` | Lowers CFIR into the CHIR model |

CHIR is consumed by the optional backend integrations, notably `:compiler:codegen` and `:compiler:jvm-codegen`. It does not define ordinary CFIR resolve phases; source parsing and semantic resolution remain owned by `:psi`, `:cfir:*`, and `:compiler:frontend`.

## Build and test

```powershell
.\gradlew.bat :chir:assemble
.\gradlew.bat :chir:chir-tree:test
.\gradlew.bat :chir:cfir2chir:test
```

## Related documentation

- [CHIR tree module boundary](chir-tree/docs/module-boundary.md)
- [CHIR debugging guide](chir-tree/docs/chir-debugging-guide.md)
- [CHIR module catalog entries](../docs/module-catalog.md)
- [Compiler stages](../docs/cjfir-compiler-stages.md)
