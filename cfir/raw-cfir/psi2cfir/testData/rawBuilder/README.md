# rawBuilder TestData

This directory stores golden-file tests for `PsiRawCfirBuilder`.

## Layout

- `declarations/`: file-level and declaration constructs
- `types/`: explicit type-reference constructs
- `expressions/`: valid expression conversion scenarios
- `control-flow/`: valid control-flow conversion scenarios
- `recovery/`: malformed/incomplete syntax recovery scenarios
- `cangjie-features/`: Cangjie-specific syntax (`extend`, `match`, `spawn`, `VArray`)

## Naming

- Input files must use `camelCase` names and `.cj` suffix.
- Each `.cj` file must have a sibling `.txt` golden file.
- `.lazyBodies.txt` is optional but recommended for declarations with bodies.

## Coverage Contract

- Keep [`coverage-matrix.md`](./coverage-matrix.md) updated when adding conversion branches.
- Every matrix entry must point to existing `.cj` files.
- Every `.cj` file in this tree must be referenced by at least one matrix entry.
