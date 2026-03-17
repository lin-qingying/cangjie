This directory stores offline CHIR comparison baselines.

Expected layout:
- `official-generated-chir/basic/<sample>.txt`

The test `OfficialCompilerChirComparisonTest` reads these files and compares them
against CHIR text printed by this project.

Note:
- The current public SDK command set (`cjc 1.0.0`) exposes `--save-temps`, which
  generates LLVM-level artifacts (`*.bc`, `*.opt.bc`, `*.o`, `*.s`) but not a CHIR
  text dump option in `--help`.
- The 1.0.5 compile options document shows the same behavior: `--save-temps <value>`
  keeps intermediate files such as `.bc` and `.o`, and does not document a CHIR text
  dump flag.
- If your internal official compiler build has CHIR dump flags, generate the files
  into `official-generated-chir/basic`.
