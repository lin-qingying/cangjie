# Parity Checklist

## Use This File

Read this file when the task is frontend parity work and you need a compact execution checklist instead of re-deriving the process.

## Identify the Work Item

- Record the exact Cangjie module and file.
- Record the exact failing symbol, stack frame, package, or requested reorganization target.
- Decide whether the task is PSI, raw CFIR, CFIR, analysis API, low-level API, diagnostics, symbol/model, or tests.

## Spawn Plan

- KotlinFrameworkMapper reads Kotlin compiler source and extracts framework mapping.
- KotlinFrameworkMapper must map module, package, file, class, and method one by one.
- CangjieSemanticsAuthority reads the official Cangjie compiler implementation and the Cangjie language docs MCP, then extracts semantic boundaries.
- CangjieTestSemanticsWriter writes or migrates test data according to CangjieSemanticsAuthority's semantic conclusions.
- KotlinParityGatekeeper audits Kotlin-framework parity at module, package, file, class, method, extension, and helper-placement granularity.
- Main thread performs all production code edits and final integration.

## Locate the Kotlin Counterpart

- Start from `external/kotlin/compiler/`.
- Search by symbol name first.
- If the symbol name differs, search by nearby declarations or call chain.
- If the task is package reorganization, compare top-level declarations between Kotlin and Cangjie packages before editing.

## Locate Cangjie Semantic Evidence

- Search the official Cangjie compiler implementation for the relevant syntax or semantic path.
- Query the Cangjie docs MCP for grammar, semantic restrictions, diagnostics, or examples when source alone is insufficient.
- Record which Kotlin semantics do not exist in Cangjie and must therefore be omitted from the implementation.

## Map Before Editing

- Match module path and ownership.
- Match package path.
- Match file split.
- Match public and internal declarations.
- Match class and interface ownership exactly.
- Match method ownership exactly.
- Match extension placement.
- Match helper ownership.
- Match builder or resolver call flow.
- Match tests or test data shape when the Kotlin side has a clear equivalent.
- Apply only Cangjie naming localization; do not change framework role or placement.
- Remove only the parts blocked by missing Cangjie semantics; keep the rest of the framework shape unchanged.
- Scan for local compatibility, fallback, rollback, bridge, adapter, shim, and minimal-implementation layers that Kotlin does not have.
- Plan to delete or refactor those layers unless CangjieSemanticsAuthority proves they are required by real Cangjie semantics.

## Decide the Fix Layer

- If Kotlin fixes the issue in a shared abstraction, fix the Cangjie framework layer.
- If Kotlin only adjusts one call path, keep the Cangjie change at that layer.
- If the Cangjie repo already has the same abstraction under a local name, reuse it instead of inventing a new bridge.
- If Cangjie lacks the underlying language semantic, do not invent a compatibility layer; omit that semantic while preserving Kotlin framework structure elsewhere.
- If the current Cangjie codebase contains compatibility or bridge layers that Kotlin does not have, treat their removal or collapse as part of parity work unless CangjieSemanticsAuthority proves they are semantically required.
- If a counterpart sits in a different local module, prove the repository-level module mapping before editing; otherwise treat it as a parity failure.

## Test Data Rule

- CangjieTestSemanticsWriter writes test data from real Cangjie semantics confirmed by CangjieSemanticsAuthority.
- Do not use Kotlin-only behavior as expected output unless CangjieSemanticsAuthority confirmed the same semantic exists in Cangjie.
- Keep the repository's existing test framework conventions while changing only the semantic content required by the task.

## Parity Review Rule

- KotlinParityGatekeeper rejects any divergence in package, file, class, method, extension, helper ownership, or call flow unless CangjieSemanticsAuthority established a Cangjie semantic reason.
- KotlinParityGatekeeper rejects any divergence in module ownership unless a proven repository-level mapping rule exists.
- KotlinParityGatekeeper rejects any added Kotlin semantic that is absent from Cangjie.
- KotlinParityGatekeeper rejects any introduced or retained compatibility, fallback, rollback, bridge, adapter, shim, or minimal-implementation layer that Kotlin does not have.
- KotlinParityGatekeeper rejects “temporary” branches, widened nullable contracts, compatibility overloads, or legacy escape hatches added to preserve old local behavior.
- KotlinParityGatekeeper rejects any naming change that does more than Cangjie localization of the Kotlin counterpart.
- KotlinParityGatekeeper approves only when module, package, file, class, method, extension, helper ownership, and call flow are fully aligned and the semantic delta is strictly limited to real Cangjie language differences.

## Audit Loop

- Treat every rejection item as the next required patch item.
- Main thread continues editing after review; do not stop at the audit report.
- Re-run KotlinParityGatekeeper review after each patch set.
- Repeat until review passes or the only remaining delta is proven to come from real Cangjie semantics.

## Validate

- Do not mark the task complete before the audit loop finishes.
- Compile or test the narrowest affected module first.
- If successful, continue to the next concrete blocker rather than stopping at the first repaired symbol.
- If a wider Gradle target fails for unrelated reasons, separate that from the source-level result.

## Example Trigger Requests

- “参考 kotlin 编译器实现仓颉前端”
- “先看 Kotlin 怎么写，再整理 analysis-api”
- “照着 Kotlin 的 raw FIR builder 修这个 Cangjie 前端问题”
- “按 Kotlin 的包和声明位置全量整理 analysis-api”
