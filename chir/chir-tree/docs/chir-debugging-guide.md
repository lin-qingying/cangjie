# CHIR Debugging Guide

This guide covers the current CHIR debugging workflow inside `:chir:chir-tree`.

## Core tools

- `ChirValidator`: validates graph integrity before serialization, pass scheduling, and public API usage.
- `ChirPrinter`: produces stable text output sorted by semantic id for diff-friendly snapshots.
- `ChirInspector`: produces a deterministic structured summary with package/module/function/block counts.
- `ChirPackageCodec`: serializes and deserializes CHIR with `PackageFormat.CHIRPackage` (`CHIR` identifier).

## Recommended debugging flow

1. Validate first.

```kotlin
ChirSerializationGate.requireValidForSerialization(chirPackage)
```

2. Print canonical text when comparing behavior.

```kotlin
val printed = ChirPrinter.print(chirPackage)
```

3. Inspect structural counters when triaging large graphs.

```kotlin
val summary = ChirInspector.inspect(chirPackage)
```

4. Round-trip and compare semantics.

```kotlin
val bytes = ChirPackageCodec.serialize(chirPackage)
val restored = ChirPackageCodec.deserialize(bytes)
ChirRoundTripAssert.assertSemanticallyEquivalent(chirPackage, restored)
```

## Common failures and quick checks

- `serialization blocked by CHIR validation`: run validator report and fix missing terminators, broken ids, or unresolved references.
- `invalid flatbuffer identifier, expected CHIR`: input file is not CHIR package bytes or payload is corrupted.
- `unsupported schema version`: payload version (`phase` field) does not match `ChirSerializationSchema.CURRENT_VERSION`.
- `invalid ... line` or `unknown ... kind`: payload body is malformed or produced by an incompatible internal codec revision.

## Regression checklist

- Keep `:chir:chir-tree:test` green after model/serializer/printer changes.
- Ensure printer output remains stable across repeated runs.
- Keep inspector keys stable to avoid breaking tooling that consumes snapshots.
