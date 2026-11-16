# chir:cfir2chir

`:chir:cfir2chir` lowers CFIR (Cangjie Frontend IR) into the CHIR model: a group of `CfirFile`s from one CFIR package is converted into a single `ChirPackage` that backends can keep lowering.

## Scope

- Includes: file/declaration conversion, function body lowering, CFIR → CHIR type mapping, stable semantic ID generation, shared conversion caches.
- Excludes: the CHIR model itself (`:chir:chir-tree`) and downstream LLVM/JVM codegen consumption.

## Public entry points

- `org.cangnova.cangjie.chir.cfir2chir.Cfir2ChirConverter` — module-level conversion entry point, implemented by `DefaultCfir2ChirConverter`

## Key packages

| Package | Responsibility |
| --- | --- |
| `org.cangnova.cangjie.chir.cfir2chir` | CFIR → CHIR lowering: `Cfir2ChirFileConverter` / `Cfir2ChirFunctionBodyConverter`, `Cfir2ChirTypeMapper`, `Cfir2ChirDeclarationStorage`, `Cfir2ChirIds`, CFIR-specific operation names (`Cfir2ChirOperation`), `Cfir2ChirConversionException` |

## Dependencies

- `:chir:chir-tree`
- `:cfir:cfir-tree`

## Commands

```bash
./gradlew :chir:cfir2chir:assemble
./gradlew :chir:cfir2chir:test
```

## Related documentation

- `../README.md` — CHIR subtree overview
- `../chir-tree/README.md` — CHIR tree module
