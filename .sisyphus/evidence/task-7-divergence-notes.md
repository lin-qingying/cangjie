# Task 7 Divergence Notes

## Intentional divergences from Kotlin upstream

### Single-file text dump retained
- Upstream Kotlin supports multiple inference-log formats (`.inference.md`, `.inference.mmd`, `.fixation.txt`).
- This repository currently retains a single side-file format:
  - `.cfir.inference.txt`
- Reason: existing test infrastructure and side-file conventions are already built around `cfirSideFile("inference.txt")`.

### Plain-text handler retained instead of full dumper hierarchy
- Upstream uses dedicated dumper classes (`FirInferenceLogsDumper`, `MarkdownInferenceLogsDumper`, etc.).
- Local handler still renders directly from `CfirInferenceLogsHandler`.
- Reason: Task 7 scope is format convergence, not importing the full dumper architecture.

### Constraint rendering remains local/simple
- Upstream distinguishes initial constraints, variable constraints, fixation-only logs, and richer error titles.
- Local renderer currently prints a unified plain-text `CONSTRAINT ...` line plus optional `origins[n]: ...` lines.
- Reason: this is sufficient to unlock deterministic goldens while the local concrete logger model is still converging.

## Outcome
- Output is now structurally richer and closer to upstream block/item dumps without changing the repository's single-file test contract.
