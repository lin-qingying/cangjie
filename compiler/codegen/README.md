# CHIR -> LLVM IR Backend Contract

## Input
- `ChirCodegenInput.chirPackage`: CHIR package model (`compiler:chir`) as the only backend source.
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
