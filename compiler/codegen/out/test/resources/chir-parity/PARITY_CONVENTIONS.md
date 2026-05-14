# CHIR -> LLVM IR Parity Conventions (Task 1.3)

This document defines the pass/fail contract for parity checks against
`external/cangjie_compiler`.

## Parity Verdict Model

A sample is **PASS** only when both layers pass:

1. **Structural parity**
2. **Normalized textual parity**

Any mismatch in either layer is **FAIL**.

## Layer 1: Structural Parity

The checker must compare, at minimum:

- function set and symbol names
- function signatures (return type, parameter list, calling convention if present)
- block graph shape and terminator kinds
- instruction opcode sequence per block
- global declaration set (global vars, imported symbols, runtime declarations)

Structural comparison is used to detect semantic divergence that might be hidden
by textual formatting differences.

## Layer 2: Normalized Textual Parity

Text comparison is performed after normalization:

- line ending normalization (`CRLF` -> `LF`)
- trailing whitespace trimming
- deterministic declaration ordering rules (where lowering allows equivalent order)
- stable identifier rendering rules where nondeterministic temp names may appear

If normalized text still differs from official baseline, the sample is **FAIL**.

## Baseline Source of Truth

- Input fixtures: `baseline/*.chir.json`
- Official outputs: `cpp-baseline/*.llvmir.txt`
- Sample inventory: `manifest.txt`

Only baselines that are traceable to official compiler outputs should be marked
as production parity gates.

## Reporting Rules

Parity failures must report:

- sample name
- structural mismatch summary (function/block/instruction scope)
- first textual diff hunk after normalization
- recommendation whether mismatch is caused by model, lowering, or printing phase

