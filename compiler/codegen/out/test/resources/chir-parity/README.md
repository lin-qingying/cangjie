# CHIR Parity Baselines

This directory stores parity fixtures that do not depend on any front-end CHIR
producer.

This directory is a baseline artifact store. It is not the direct input for the
auto-generated parity test class in `compiler/codegen/tests-gen`.

## Layout

- `source/`
  - representative `.cj` inputs used to generate official baselines
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

## Baseline Generation

Current official baselines are generated with SDK `cjc 1.0.0 (cjnative)` on
`x86_64-w64-mingw32`, using:

`cjc <sample>.cj --output <sample>.exe --save-temps <tmp-dir> -O0`

Then disassemble:

`llvm-dis <tmp-dir>/<sample>.opt.bc -o cpp-baseline/<sample>.llvmir.txt`

For traceability, each generated sample includes:

- `source/<sample>.cj`
- `baseline/<sample>.chir.json`
- `baseline/<sample>.meta.json`
- `cpp-baseline/<sample>.llvmir.txt`

## Manifest

The canonical sample inventory lives in
[manifest.txt](/D:/code/intellij/cangjie/compiler/codegen/testResources/chir-parity/manifest.txt).

Each line uses the format:

`sample-name | kotlin-input | official-output | category | note`

## Parity Contract

Parity pass/fail rules are defined in
[PARITY_CONVENTIONS.md](/D:/code/intellij/cangjie/compiler/codegen/testResources/chir-parity/PARITY_CONVENTIONS.md).

## Relationship To `testData/chirParity`

- `testResources/chir-parity`:
  - source archive for official baseline artifacts (`source/`, `baseline/`, `cpp-baseline/`)
  - used for traceability and future parity expansion
- `testData/chirParity`:
  - direct test input used by generated JUnit3-style parity tests
  - each `*.chir.json` must have a sibling `*.txt` golden
