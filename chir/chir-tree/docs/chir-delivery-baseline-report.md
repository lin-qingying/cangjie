# CHIR Delivery Baseline Report

## Status

- Change: `port-cpp-chir-to-cangjie`
- Scope baseline: CHIR subsystem only (no upstream generator, no downstream codegen)
- Current OpenSpec progress: 48/51 tasks complete (pending 8.3, 9.5, 10.1)

## Delivered baseline

- Core model/context/type/value/declaration/control-flow hierarchy with stable semantic ids.
- Builder + symbol binding + validation gate integration.
- Visitor/rewrite contracts and pass scheduler/cache invalidation.
- Baseline analyses and transformation support.
- FlatBuffers-based serialization switched to `PackageFormat.CHIRPackage`.
- Canonical printer + structured inspector + debugging guide.
- CHIR-specific regression assets:
  - fixture/assertion testkit
  - upstream C++ reference manifest mapping
  - regression smoke suite
  - diff report format

## Verification executed

- `:chir:chir-tree:assemble`
- `:chir:chir-tree:test`

## Out of scope (intentionally)

- CHIR generation ownership (`CFIR/AST -> CHIR`).
- LLVM/codegen consumption (`CHIR -> LLVM IR`).

## Residual work

- 8.3 CLI-only CHIR entry (`verify-chir`, `dump-chir`).
- 9.5 CI gate wiring for CHIR dedicated regression thresholds.
- 10.1 codegen migration (explicitly excluded by current user directive).
