# CHIR -> LLVM IR Backend Contract

## Input
- `ChirCodegenInput.chirPackage`: CHIR package model (`:chir:chir-tree`) as the only backend source.
- `ChirCodegenInput.options`: backend behavior toggles, partition mode, post-processing order.

## Output
- `ChirCodegenOutput.modules`: generated LLVM module artifacts.
- `LlvmModuleArtifact.module`: in-memory LLVM module representation.
- `LlvmModuleArtifact.bitcode`: bitcode bytes when `emitBitcode=true`; otherwise `null`.

## Safety Contract
- verify always runs in post-processing.
- when `verifyBeforeWrite=true`, verify failures throw and prevent bitcode emission.

## Parity Baselines

- Official mapping matrix:
  [cpp-codegen-mapping.md](/D:/code/intellij/cangjie/compiler/codegen/docs/cpp-codegen-mapping.md)
- CHIR parity fixtures:
  [chir-parity/README.md](/D:/code/intellij/cangjie/compiler/codegen/testResources/chir-parity/README.md)
- Sample manifest:
  [manifest.txt](/D:/code/intellij/cangjie/compiler/codegen/testResources/chir-parity/manifest.txt)

## TestData-Driven Parity Tests

The parity test suite that runs in CI is driven by:

- input fixture: `compiler/codegen/testData/chirParity/**/*.chir.json`
- expected golden: same path + same basename + `.txt`

The test class is auto-generated (psi2cfir-style) and must not be hand-edited:

- generator: `org.cangnova.cangjie.codegen.parity.TestGeneratorForCodegenParity`
- output: `compiler/codegen/tests-gen/org/cangnova/cangjie/codegen/parity/CodegenParityTestGenerated.kt`

Useful commands:

- generate tests only:
  - `./gradlew :compiler:codegen:generateTestGeneratorForCodegenParityTests --no-configuration-cache`
- run parity tests:
  - `./gradlew :compiler:codegen:test --no-configuration-cache --tests "org.cangnova.cangjie.codegen.parity.*"`
- run module gate (includes `parityCheck`):
  - `./gradlew :compiler:codegen:check --no-configuration-cache`
