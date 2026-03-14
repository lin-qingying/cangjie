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
- Use system LLVM (`CANGJIE_USE_OFFICIAL_LLVM=OFF` by default), and pass `LLVM_DIR`.

```bash
cmake -S tools/cangjie-llvm-interop -B tools/cangjie-llvm-interop/build \
  -DCANGJIE_USE_OFFICIAL_LLVM=OFF \
  -DLLVM_DIR=<path-to-llvm-cmake-dir>
cmake --build tools/cangjie-llvm-interop/build --config Release
```

To bootstrap official LLVM from source:

```bash
cmake -S tools/cangjie-llvm-interop -B tools/cangjie-llvm-interop/build \
  -DCANGJIE_USE_OFFICIAL_LLVM=ON
cmake --build tools/cangjie-llvm-interop/build --config Release
```

On success, executable name is:
- `cangjie-llvm-interop_<os>_<arch>`
- example: `cangjie-llvm-interop_linux_amd64`

Useful CMake options:
- `-DCANGJIE_USE_OFFICIAL_LLVM=ON|OFF`
- `-DCANGJIE_LLVM_REPOSITORY=<repo-url>`
- `-DCANGJIE_LLVM_TAG=<branch-or-tag>`
- `-DCANGJIE_LLVM_TARGETS=ARM;AArch64;X86`
- `-DCANGJIE_LLVM_SOURCE_DIR=<local-llvm-project-dir>`
- `-DLLVM_DIR=<existing-llvm-config-dir>`
