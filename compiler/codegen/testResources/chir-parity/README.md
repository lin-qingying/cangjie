# CHIR Parity Baselines

This directory stores parity fixtures that do not depend on any front-end CHIR
producer.

## Layout

- `baseline/`
  - Kotlin-side CHIR fixture payloads
  - source of truth for backend-driven tests in this repository
- `cpp-baseline/`
  - official C++ compiler outputs for the same fixture name
  - currently LLVM IR text baselines

## Naming Rules

For a sample named `simple-return`:

- Kotlin CHIR input:
  - `baseline/simple-return.chir.json`
- Official LLVM IR output:
  - `cpp-baseline/simple-return.llvmir.txt`

Future expansions may add:

- `cpp-baseline/<name>.chir.txt`
- `cpp-baseline/<name>.report.json`
- `baseline/<name>.meta.json`

## Manifest

The canonical sample inventory lives in
[manifest.txt](/D:/code/intellij/cangjie/compiler/codegen/testResources/chir-parity/manifest.txt).

Each line uses the format:

`sample-name | kotlin-input | official-output | category | note`

## Parity Contract

Parity pass/fail rules are defined in
[PARITY_CONVENTIONS.md](/D:/code/intellij/cangjie/compiler/codegen/testResources/chir-parity/PARITY_CONVENTIONS.md).
