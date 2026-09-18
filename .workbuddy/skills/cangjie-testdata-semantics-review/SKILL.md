---
name: cangjie-testdata-semantics-review
description: Review, audit, or directly repair Cangjie compiler test data one file at a time so expected diagnostic names, diagnostic messages, inline marker locations, directives, output text, and negative/positive cases are complete, correct, and based on real Cangjie grammar and language semantics. Use when Codex is asked to 审查测试数据, 检查测试期望, 迁移 testData, 修复 inline diagnostics, 判断测试是否 100% 正确仓颉语义, or validate Cangjie compiler fixtures such as .cj files, diagnostic test data, parser test data, CFIR/LLT tests, analysis tests, and golden outputs; when a problem is found, modify the current file directly according to real Cangjie semantics and grammar unless the user explicitly asked for review-only output; requires reading official Cangjie semantics through docs MCP, official compiler implementation, and cjc validation when applicable, without relying on this repository's possibly incorrect implementation or running this repository's tests.
---

# Cangjie Test Data Semantics Review

## Overview

Use this skill to review Cangjie compiler test data as a grammar and semantic artifact, not as text to make tests pass.
The acceptance bar is: every expected diagnostic name, diagnostic message, directive, marker location, and output must be proven from real Cangjie grammar and real Cangjie semantics.
Review one file at a time. Do not use this repository's current implementation behavior or test result as semantic authority, because it may be wrong.
Default to editing: when the current file has wrong, missing, misplaced, redundant, wrong-message, or unproven expectations, repair the file directly using real Cangjie grammar and semantics.

## Required Inputs

Start from the concrete artifact under review:

- Test data path, failing test, diagnostic name, inline marker, directive, golden output, or migration subtree.
- The exact failure text when the request comes from a failing run.

If the user gives a directory, process files one by one. Do not approve the directory as a batch without per-file semantic verification.

## Evidence Order

Use this order before changing or approving test data:

1. Read the diagnostic definition names and diagnostic messages used by this repository, only to know the available expected diagnostic surface.
2. Read and understand the real Cangjie grammar and semantic rule before deciding any expectation.
3. Read official Cangjie language documentation through the Cangjie docs MCP for grammar, semantic legality, restrictions, examples, and diagnostics.
4. Read the official Cangjie compiler implementation, including C/C++ source when available, to confirm parser, semantic checker, and diagnostic behavior.
5. Compile focused snippets with the official Cangjie SDK at `C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5` when the behavior can be validated by `cjc`; create any temporary `.cj` file only under the repository root `tmp` directory.
6. Decide which diagnostic name and message should appear and exactly where it should appear in the Cangjie source.
7. Directly modify the current file when its expectations conflict with the proven Cangjie grammar or semantic result.
8. Read Kotlin compiler tests only for fixture format parity when needed, not as authority for Cangjie semantics.

Do not approve or write expectations from memory, naming similarity, old generated output, Kotlin-only behavior, or uncompiled guesses.
Do not trace this repository's checker, resolver, builder, CFIR, analysis API, or test implementation to justify semantics. Those paths may be wrong.

## Official Cangjie Sources

Use all applicable sources before claiming the test data is semantically correct:

- Cangjie docs MCP: query grammar, semantic restrictions, type rules, diagnostic meaning, and examples.
- Official Cangjie compiler implementation: read the C/C++ parser, semantic checker, resolver, type checker, diagnostic, or lowering path that owns the construct.
- Official `cjc`: use `C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe` for focused compile checks.

For `cjc` checks, create temporary Cangjie source files only in the current repository root `tmp` directory, for example `D:\code\intellij\cangjie\tmp\<case-name>.cj`.
Do not create `cjc` test files in the SDK directory, system temp directory, testData directory, source tree, or any path outside repository-root `tmp`.

For `cjc` checks on Windows, prefer using the SDK environment setup first:

```powershell
& 'C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\envsetup.ps1'
& 'C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe' 'D:\code\intellij\cangjie\tmp\<case-name>.cj'
```

Use `cjc` as semantic evidence for small, isolated examples. Do not use it to bypass source reading when the task requires understanding the real grammar or semantic rule.

## Review Workflow

1. Pick exactly one file.
   Review and repair that file before moving to the next file.
2. Identify all constructs in the file that can trigger diagnostics.
   Include syntax errors, semantic errors, type errors, declaration errors, modifier errors, visibility errors, overload errors, initialization errors, and control-flow errors.
3. Read diagnostic definitions.
   Record the available diagnostic name and message that corresponds to each proven Cangjie error.
4. Confirm Cangjie language semantics.
   Read and understand the real grammar and semantics using docs MCP, official compiler C/C++ source, and focused `cjc` compilation when applicable.
5. Decide exact source locations.
   For every diagnostic, identify the token, expression, declaration, type reference, modifier, or source range where the marker should be placed.
6. Compare actual test expectations with proven semantics.
   Mark each expectation as correct, missing, wrong, redundant, wrong-location, wrong-message, or unproven.
7. Repair only proven issues in the current file.
   Edit that file immediately to match real Cangjie grammar, real Cangjie semantics, known diagnostic names, known diagnostic messages, and exact marker locations.
8. Move to the next file only after the current file has been reviewed or repaired.

Read [references/semantic-review-checklist.md](references/semantic-review-checklist.md) when a compact checklist is enough.

## Semantic Standard

- Treat this repository's diagnostic names and diagnostic messages as the available expected diagnostic vocabulary only.
- Treat official Cangjie docs, official Cangjie compiler implementation, and focused `cjc` behavior as the authority for grammar and language semantics.
- Treat the expected marker location as part of the semantic result: the diagnostic must be attached to the exact Cangjie token, expression, declaration, type reference, or source range that violates the rule.
- Preserve Cangjie semantics even when Kotlin has a similar-looking but different rule.
- Require positive cases for legal constructs and negative cases for illegal constructs when the fixture set is meant to cover both.
- Require boundary cases when the semantic rule has important edges, such as nullable types, visibility, overload ambiguity, generic constraints, flow-sensitive typing, pattern binding, mutability, initialization, or modifier combinations.
- Keep test names, directives, and golden outputs consistent with the semantic point being tested.
- Remove stale expectations when the underlying Cangjie semantic is absent or the diagnostic name/message does not match the real error.
- Prefer explicit evidence over broad inference. If no evidence proves an expected diagnostic, do not invent it.
- Ignore this repository's current implementation behavior when it conflicts with real Cangjie semantics.
- Treat a proven problem as an edit request, not only a report item, unless the user explicitly asks for review-only findings.

## Prohibited Moves

- Do not write, approve, or keep a diagnostic marker only because it makes a failing test pass.
- Do not copy Kotlin test expectations as Cangjie semantics unless Cangjie docs or compiler source proves the same rule exists.
- Do not write semantic expectations before reading and understanding the real Cangjie grammar and semantic rule.
- Do not use this repository's current checker, resolver, builder, analysis, CFIR, or test results as semantic authority.
- Do not run this repository's tests as validation for semantic correctness; they may pass or fail for implementation reasons unrelated to true Cangjie semantics.
- Do not keep legacy cjc output when this repository has a different diagnostic model and the user asked for this project's framework.
- Do not create temporary `cjc` `.cj` files anywhere except the current repository root `tmp` directory.
- Do not invent placeholder diagnostic names, TODO markers, broad error buckets, or compatibility output.
- Do not use fallback, degraded, approximate, or minimal expected data.
- Do not delete hard cases to simplify the suite unless the construct is proven outside Cangjie semantics or outside the target fixture scope.
- Do not claim "100% correct" unless every expectation in the reviewed scope has a source of truth.
- Do not batch-approve multiple files without checking each file independently.
- Do not stop after listing a proven test-data problem when the task allows editing; apply the Cangjie-semantics-based correction in the current file.

## Output Standard

Only use review-only reporting when the user explicitly asks not to edit. In that case, report findings first:

- File and line.
- Current expectation.
- Proven semantic rule.
- Required change or approval.
- Expected diagnostic name.
- Expected diagnostic message.
- Expected marker location in the Cangjie source.
- Evidence source: diagnostic definition, docs section, official compiler source path, or `cjc` compile result.

When editing, make the changes directly and then report:

- Files changed.
- Problems found and corrected.
- Per-file semantic verification result.
- Any remaining unproven cases that were intentionally left unchanged.

## Validation Standard

Do not run this repository's tests to validate semantic correctness.
Validation means the current file has been checked against:

- Real Cangjie grammar and semantic evidence.
- Diagnostic definition name.
- Diagnostic message.
- Exact diagnostic location in source.
- Optional focused `cjc` compile result when the construct can be isolated.

The current file is complete only when every diagnostic expectation in that file has those facts established.
