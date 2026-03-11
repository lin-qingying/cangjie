# CFIR Resolve Phase Rollback Plan

This document defines rollback checkpoints for the CFIR_RESOLVE migration.

## Rollback Principles

1. Roll back by phase, not by whole subsystem.
2. Keep `CfirResolveComponentsRegistrar` as the single switch point.
3. Preserve diagnostics compatibility for already-migrated phases.
4. Never remove legacy compatibility entrypoints until replacement tests pass.

## Checkpoints

1. `IMPORTS`
- Rollback trigger: import binding regression or unresolved import false positives.
- Rollback action: restore previous imports processor implementation only.

2. `SUPER_TYPES`
- Rollback trigger: inheritance graph breakage or duplicate-interface regressions.
- Rollback action: restore previous super type processor and keep current imports processor.

3. `TYPES`
- Rollback trigger: explicit type resolution breakage or widespread error-type propagation.
- Rollback action: restore type processor while retaining previous phases.

4. `STATUS`
- Rollback trigger: modifier/visibility regressions.
- Rollback action: restore status processor in isolation.

5. `EXTENSIONS`
- Rollback trigger: extend legality/orphan-rule regressions.
- Rollback action: restore extensions processor and keep previous phase outputs.

6. `IMPLICIT_TYPES`
- Rollback trigger: inference loops or unstable inferred boundaries.
- Rollback action: restore implicit-types processor and keep diagnostics markers.

7. `BODY_RESOLVE`
- Rollback trigger: expression binding/type-check regressions.
- Rollback action: restore body-resolve processor while preserving declaration-boundary outputs.

8. `CHECKERS`
- Rollback trigger: diagnostics instability or ordering regressions.
- Rollback action: restore checkers-only logic and keep semantic outputs from prior phases.

## Validation After Any Rollback

1. Run `:cfir:cfir-tree:test` for resolve suites.
2. Run `:analysis:analysis-api-cfir:test` for facade compatibility.
3. Compare diagnostics output against golden data.
