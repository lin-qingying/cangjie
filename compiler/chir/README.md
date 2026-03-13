# compiler:chir

`compiler:chir` provides the standalone CHIR subsystem for Cangjie compiler stages.

## Scope

- Includes: CHIR core model, context, builder, validator, pass pipeline, analyses, rewrites, serializer, printer/inspector.
- Excludes: upstream CHIR generation and downstream LLVM/codegen consumption.

## Public entry points

- `org.cangnova.cangjie.chir.core.api.ChirPublicApi`
- `org.cangnova.cangjie.chir.core.serializer.ChirPackageCodec`
- `org.cangnova.cangjie.chir.core.printer.ChirPrinter`
- `org.cangnova.cangjie.chir.core.printer.ChirInspector`

## Module boundary

See `docs/module-boundary.md` for package-level responsibilities and dependency rules.
