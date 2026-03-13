# cangjie-llvm-interop

Native interop bridge for `compiler:codegen` to call LLVM without JNI/JNA.

## Commands

### `probe --json`
Prints backend capability metadata:

```json
{"llvmVersion":"18.1.8","symbols":["LLVMGetVersion","LLVMContextCreate","LLVMModuleCreateWithName","LLVMPrintModuleToString"]}
```

### `emit-bitcode --module <name>`
Reads LLVM IR text from `stdin`, parses/verifies it with LLVM APIs, and writes bitcode bytes to `stdout`.

## Build

Default behavior:
- If `LLVM_DIR` is not provided, CMake bootstraps LLVM automatically from:
  - repository: `https://gitcode.com/openharmony/third_party_llvm-project.git`
  - tag/branch: `master`

```bash
cmake -S tools/cangjie-llvm-interop -B tools/cangjie-llvm-interop/build
cmake --build tools/cangjie-llvm-interop/build --config Release
```

On success, executable is produced in `tools/cangjie-llvm-interop/build/` (or `build/Release` on multi-config generators).

Useful CMake options:
- `-DCANGJIE_USE_OFFICIAL_LLVM=ON|OFF`
- `-DCANGJIE_LLVM_REPOSITORY=<repo-url>`
- `-DCANGJIE_LLVM_TAG=<branch-or-tag>`
- `-DCANGJIE_LLVM_TARGETS=ARM;AArch64;X86`
- `-DCANGJIE_LLVM_SOURCE_DIR=<local-llvm-project-dir>`
- `-DLLVM_DIR=<existing-llvm-config-dir>`
