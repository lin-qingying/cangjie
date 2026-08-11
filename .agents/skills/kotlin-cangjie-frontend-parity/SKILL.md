---
name: kotlin-cangjie-frontend-parity
description: Use subagents to read the Kotlin compiler implementation, the official Cangjie compiler implementation and language docs, generate test data from real Cangjie semantics, and review whether the Cangjie frontend is 100 percent aligned to the Kotlin compiler framework before the main thread codes. Use when Codex is asked to implement, organize, migrate, or repair Cangjie frontend work involving PSI, raw CFIR, CFIR, analysis API, low-level API, diagnostics, symbol/model layers, or related tests, especially when the user要求“先看 Kotlin 怎么写”“对齐 Kotlin”“照搬实现”“整理 analysis-api” or similar parity-first frontend tasks.
---

# Kotlin Cangjie Frontend Parity

## Overview

Run a fixed multi-agent workflow before and during implementation.
Treat Kotlin framework parity as the non-negotiable goal: module, package, file, class, interface, method, extension, helper ownership, and call flow must align 100 percent with the Kotlin counterpart.
The only allowed surface adaptation is name localization into Cangjie naming, while preserving the exact Kotlin framework role and placement.

## Agent Topology

Spawn these roles when delegation is available:

1. KotlinFrameworkMapper
   Read the Kotlin compiler implementation and produce the exact counterpart mapping: module, package, file, declarations, methods, extensions, helper ownership, builder or resolver path, and test shape.
2. CangjieSemanticsAuthority
   Read the official Cangjie compiler implementation in C or C plus plus form when available, and consult the Cangjie language docs through the docs MCP when needed. Produce the real language-semantics constraints that are allowed to differ from Kotlin.
3. CangjieTestSemanticsWriter
   Write or migrate this repository's test data from real Cangjie semantics confirmed by CangjieSemanticsAuthority. Do not derive test semantics from Kotlin when the behavior is Cangjie-specific.
4. KotlinParityGatekeeper
   Review whether the planned or completed implementation is 100 percent aligned to the Kotlin compiler framework at module, package, file, class, method, extension, and helper-placement granularity, with only name localization into Cangjie allowed at the surface level.

The main thread owns actual production code edits and final integration.

## Workflow

1. Confirm the exact Cangjie target before editing.
   Inputs can be a failing symbol, stack trace, package, module, test, or a user request such as “整理 analysis-api”.
2. Spawn KotlinFrameworkMapper and CangjieSemanticsAuthority in parallel.
   KotlinFrameworkMapper establishes the Kotlin framework mapping.
   CangjieSemanticsAuthority establishes the real Cangjie semantic boundaries.
3. Compare KotlinFrameworkMapper and CangjieSemanticsAuthority outputs.
   Keep the Kotlin framework shape 100 percent aligned at module, package, file, class, and method granularity.
   Keep only the semantic subset that truly exists in Cangjie; do not invent missing semantics.
4. Let CangjieTestSemanticsWriter prepare or revise test data from CangjieSemanticsAuthority's semantic conclusions.
5. Main thread implements the code change.
   Copy the Kotlin framework shape exactly where the semantic layer permits.
6. Ask KotlinParityGatekeeper to review parity before or after the final patch set.
7. If KotlinParityGatekeeper rejects the change, continue implementation from the review findings immediately.
   Remove every rejected divergence, rerun the review, and repeat until KotlinParityGatekeeper approves or the only remaining delta is proven Cangjie semantics.
8. Validate at the narrowest correct scope first.
   Compile or test the affected module, then continue to the next concrete blocker if needed.

## Required Behavior

- Read the Kotlin compiler implementation before writing code.
- Use KotlinFrameworkMapper for Kotlin reference reading when delegation is available.
- Use CangjieSemanticsAuthority for official Cangjie compiler and language-doc reading when delegation is available.
- Use CangjieTestSemanticsWriter to prepare test data according to real Cangjie semantics, not guessed semantics.
- Use KotlinParityGatekeeper to review 100 percent Kotlin-framework parity before declaring the work aligned.
- Continue coding after review findings until the implementation is aligned; do not stop at the first audit report.
- Keep the analysis anchored on concrete evidence: failing symbol, builder path, stack frame, declaration, or exact package mismatch.
- Align to the Kotlin framework at module, package, file, class, interface, method, extension, and helper granularity.
- Keep module boundaries and ownership aligned to Kotlin; do not move a Kotlin counterpart into a different local module without a proven repository-level mapping rule.
- Allow only name localization into Cangjie while preserving the same declaration role, layer, and location as Kotlin.
- Reuse existing Cangjie abstractions if the repo already has the intended bridge name or wrapper.
- Fix shared framework paths when the problem is structural; do not patch only one symptom site.
- Keep edits inside first-party modules unless the task explicitly requires otherwise.
- Exclude Kotlin-only semantics that do not exist in Cangjie; omit them instead of inventing a local interpretation.
- Remove local compatibility, fallback, bridge, adapter, shim, and minimal-implementation code when the Kotlin framework does not contain that layer and it is not required by real Cangjie semantics.
- Separate source fixes from unrelated Gradle, daemon, or environment failures when reporting validation status.

## Prohibited Moves

- Do not start from a local patch and only later search Kotlin for justification.
- Do not let the main thread code before KotlinFrameworkMapper and CangjieSemanticsAuthority have established framework mapping and semantic boundaries, unless delegation is unavailable and you must perform both reads locally first.
- Do not implement any compatibility path, fallback path, rollback path, bridge layer, adapter layer, shim layer, or minimal implementation when the Kotlin side does not have it.
- Do not keep an existing local compatibility, fallback, rollback, bridge, adapter, shim, or minimal-implementation layer just because it already exists in this project; compare it against Kotlin and delete or refactor it when it breaks parity and is not required by real Cangjie semantics.
- Do not import Kotlin semantics into Cangjie when CangjieSemanticsAuthority shows the language does not support them.
- Do not widen nullability, relax control flow, split one Kotlin abstraction into multiple local escape hatches, or add defensive branches solely to “make it work”.
- Do not change module placement, package placement, file placement, class decomposition, or method ownership relative to Kotlin, except for established Cangjie naming localization.
- Do not use “roughly aligned”, “functionally aligned”, or “conceptually aligned” as acceptance criteria; only 100 percent framework alignment counts.
- Do not claim parity after copying behavior but changing package layout or framework boundaries.
- Do not treat audit output as a final deliverable when rejected items remain; use it as the next work queue.
- Do not stop at a summary when the task clearly asks for direct code organization or implementation.

## Search Strategy

Use fast code search first. Prefer `rg` and narrow the scope by module.

- For Kotlin counterparts under the local mirror, start from `external/kotlin/compiler/`.
- For official Cangjie compiler references, search the configured local or referenced upstream source tree first, then consult docs MCP for language rules that affect semantics or diagnostics.
- For analysis API work, inspect Kotlin `analysis/analysis-api*` and map each declaration to the matching Cangjie `analysis/*` package.
- For raw frontend builder work, inspect Kotlin `compiler/fir/raw-fir/psi2fir/`.
- For PSI and syntax model work, inspect the matching Kotlin PSI or FIR declaration chain before editing Cangjie PSI/CFIR code.
- For diagnostics or IDE behavior, trace the real call chain end-to-end before changing visitor or provider code.

Useful search prompts:

- “Find the Kotlin file that defines the matching symbol”
- “Find the official Cangjie semantic rule or compiler path that confirms whether this Kotlin feature exists in Cangjie”
- “Map this Kotlin module, package, file, class, and method to the exact Cangjie counterpart names and locations”
- “List top-level declarations in the Kotlin package and compare them with the Cangjie package”
- “Trace the Kotlin builder path for this PSI or FIR node before editing the Cangjie builder”
- “Check whether Kotlin solves this in the framework layer or only at the call site”

## Review Standard

KotlinParityGatekeeper or the main thread review must reject the change if any of these are true:

- The module placement differs from Kotlin without a proven repository-level mapping rule.
- The package layout differs from Kotlin without a Cangjie semantic reason.
- The file split differs from Kotlin without a Cangjie semantic reason.
- A class, interface, method, or extension is merged, omitted, renamed, or relocated without a proven reason.
- A name change does more than localize Kotlin naming into the Cangjie naming system.
- Helper ownership or call flow is locally reinvented instead of following Kotlin.
- Test data encodes Kotlin semantics that CangjieSemanticsAuthority did not confirm for Cangjie.
- The implementation adds behavior for a Kotlin semantic that Cangjie does not have.
- The implementation introduces or retains a compatibility layer, fallback path, rollback path, bridge, adapter, shim, or minimal implementation that Kotlin does not have and CangjieSemanticsAuthority did not justify from real Cangjie semantics.
- The implementation preserves a historical local workaround instead of refactoring toward the Kotlin framework shape.
- The implementation adds defensive branches, widened nullable contracts, or alternate entry paths whose only purpose is compatibility with old local code.
- Any claim of alignment stops short of module, package, file, class, and method level verification.

A rejection is a continuation signal, not a stopping point.
Main thread must convert each rejected item into the next patch set, then request review again until approval conditions are met.

## Validation

- Only consider the task complete after review approval confirms 100 percent framework alignment at module, package, file, class, and method granularity, with only Cangjie naming localization and real semantic absence allowed.
- Run the most specific affected Gradle target first.
- If the changed code compiles but a broader task fails because of unrelated environment issues, record that separation clearly.
- For reorganization work, validate both source compilation and any API baseline or IDE branch surface touched by the move.

## Reference File

Read [references/parity-checklist.md](references/parity-checklist.md) when you need a compact checklist for module mapping, Kotlin lookup order, and validation sequence.
