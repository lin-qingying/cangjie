This folder stores companion Cangjie source files used to generate official
LLVM baselines for parity fixtures.

Path mapping:

- fixture: `compiler/codegen/testData/chirParity/<dir>/<name>.chir.json`
- source : `compiler/codegen/testData/chirParity/tools/official-sources/<dir>/<name>.cj`

The baseline generator script first looks for `<name>.cj` next to the fixture,
then falls back to this folder.

For foreign/imported call fixtures, the generator accepts linker failure as long as
the compiler produced `.opt.bc` in `--save-temps` output.
