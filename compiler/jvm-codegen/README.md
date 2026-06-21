# CHIR -> JVM Backend Contract

## Input

- `ChirJvmCodegenInput.chirPackage`: CHIR package model (`:chir:chir-tree`) as the backend source.
- `ChirJvmCodegenInput.options`: JVM classfile and output behavior.

## Output

- `ChirJvmCodegenOutput.classes`: JVM `.class` artifacts with stable relative paths.
- `JvmArtifactWriter`: writes generated classes to a directory or a standard JVM `.jar`.

## Safety Contract

- CHIR validation runs before lowering unless explicitly disabled.
- Unsupported CHIR types, expressions, terminators, or ABI cases fail fast through `JvmCodegenException`.
- Generated classes must be loadable by the JVM verifier; tests should load the bytes or inspect them through ASM.
