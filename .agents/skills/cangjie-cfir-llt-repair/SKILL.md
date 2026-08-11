---
name: cangjie-cfir-llt-repair
description: "Repair CFIR LLT failures in D:\\code\\intellij\\cangjie by clustering cfir/analysis-tests failures by semantic problem type, deriving behavior from official cjc and external/cangjie_compiler, checking Kotlin FIR/Analysis/Resolve/Type System counterparts, and fixing CFIR implementation at framework/root-cause level. Use when asked to make :cfir:analysis-tests:test green, repair CfirAnalysisLLTTestGenerated or CfirAnalysisLLTPsiTestGenerated failures, handle testData/llt diagnostics, or align CFIR behavior with official Cangjie semantics and Kotlin compiler design without fixture-driven patches."
---

# 仓颉 CFIR LLT 修复

## Core Contract

Use this skill to repair `cfir/analysis-tests` LLT failures in `D:\code\intellij\cangjie`.

The goal is not to make one fixture pass. The goal is to make each semantic failure family match official Cangjie behavior through CFIR framework-level implementation fixes.

Treat evidence in this order:

1. Official `cjc` behavior.
2. `external/cangjie_compiler` implementation.
3. Kotlin compiler counterpart design: FIR, Analysis API, Resolve, Type System, diagnostics, checker placement.
4. Existing CFIR architecture.

Never treat current CFIR output as semantic authority. Semantics come from what the official compiler actually does; architecture comes from how Kotlin's compiler solves the same structural problem. CFIR's current output is the thing being corrected, never the reference.

Exception: diagnostic *range* — where the underline starts and ends, as opposed to whether a diagnostic fires or what it means — is governed by this project's own Diagnostic Range Policy below, not by `cjc`. That's the one place this skill deliberately doesn't follow the evidence order above.

## Non-Negotiables

A "framework-level fix" changes the shared owner of a problem type, not the one fixture that surfaced it. Each prohibition below exists because it produces output that looks fixed locally but reintroduces the same bug class elsewhere, or papers over a real semantic gap instead of closing it:

- compatibility layers — hide which code path is actually correct
- fallback behavior — silently masks failures instead of surfacing the real diagnostic
- hardcoded fixture logic — fixes the test, not the compiler
- special cases for one file or one test — same failure mode as fixture logic, scoped differently
- bridge-style implementations that bypass the real owner — the fix never reaches the code other call sites use
- test-only behavior — diverges runtime behavior from test behavior, defeating the point of LLT
- semantic downgrades to satisfy current expected output — inverts the evidence order above, making CFIR the authority instead of `cjc`
- importing any Kotlin behavior not licensed by `cjc` or `external/cangjie_compiler` — semantics must come from the official compiler, full stop; Kotlin is consulted to align CFIR's *framework* (how FIR/Analysis API structure a problem) with the Kotlin compiler's design, never to decide what a Cangjie construct actually means or how it behaves. The one exception is diagnostic *range*, covered separately by the Diagnostic Range Policy below — that's a deliberate, scoped carve-out, not license to lean on Kotlin behavior anywhere else

Do not run `build`, `assemble`, or standalone compile tasks before LLT verification — the test task pulls in its real prerequisites itself, and a separate build step costs time without adding signal.

On Windows, read text with UTF-8 and edit files with `apply_patch`.

## Main Workflow

Start from the real LLT surface:

```powershell
.\gradlew.bat :cfir:analysis-tests:test
```

If a full run is too large or hangs, inspect `cfir/analysis-tests/build/test-results/test/*.xml` and then use focused `--tests` filters. Keep PSI and non-PSI LLT suites in separate failure buckets even when they look like the same problem type — they exercise different CFIR entry points (raw-builder path vs PSI-deserialization path), so a fix that closes one does not automatically close the other and each needs independent verification:

- `CfirAnalysisLLTTestGenerated`
- `CfirAnalysisLLTPsiTestGenerated`

Build a failure table by problem type, not by file. Use categories such as:

- Resolve
- Smart Cast
- Type Inference
- Flow Analysis
- Visibility
- Diagnostics
- Override
- Generics
- Constant Evaluation
- Constructor / delegation
- Inheritance

For each problem type:

1. List every failing fixture and generated test class in that type.
2. Identify the shared CFIR owner path before editing.
3. If the failure type is Diagnostics, check whether it's range-only — the diagnostic fires correctly but the underline's position or width is wrong. If so, skip straight to the Diagnostic Range Policy below instead of gathering `cjc`/`external/cangjie_compiler` evidence; the policy is the evidence, not something to derive. Otherwise continue to step 4.
4. Read official evidence: run `cjc` when useful and read `external/cangjie_compiler` for the semantic rule.
5. Read the exact Kotlin counterpart implementation before changing CFIR.
6. Read the current CFIR implementation and nearby architecture.
7. Fix the shared owner, not the fixture that exposed the issue.
8. Validate all fixtures in that problem type, then run the full `:cfir:analysis-tests:test` regression.
9. Append the result to the repair log (below) before moving to the next problem type.

If a proposed fix cannot explain why it covers the whole problem type, reject it as a local patch.

## Official Evidence

Use `cjc` probes for observable behavior when the source can be minimized without changing semantics. Keep the probe source and diagnostic output as evidence before changing code or fixtures.

Use `external/cangjie_compiler` for implementation-level semantics. Search by official diagnostic names, AST concepts, checker names, and language constructs.

Do not claim official parity unless the behavior was observed through `cjc` or traced in `external/cangjie_compiler`.

## Diagnostic Range Policy

`cjc` anchors diagnostics narrowly — for example, `func abc` positions the diagnostic on the single character `a`, not the whole identifier. That's fine for a CLI compiler printing `line:col` messages, but it's a poor experience for an IDE underline, so this project does not mirror it.

Policy: a diagnostic's range covers the full relevant token. For `func abc`, the range covers the entire `abc`, not just its first character. This applies to identifier/token-anchored diagnostics generally, not only this one example.

This changes how range-only mismatches get read, specifically:

- If CFIR currently matches `cjc`'s narrow position, the narrow position is itself the bug — widen it to the full token. `cjc` parity is not evidence the current range is correct.
- If a fixture expects `cjc`'s narrow range, the fixture is wrong by project policy. This policy is the evidence for that correction — Fixture Edit Gate condition 1 doesn't need separate `cjc` counter-evidence for a range-only fixture edit, since the divergence from `cjc` is the point, not something to be proven wrong.
- This covers *range* only. Never let a range fix slide into a semantic change — if a diagnostic's triggering condition also looks wrong, that still goes through the normal evidence order (`cjc` → `external/cangjie_compiler` → Kotlin counterpart) like any other Diagnostics-type failure.
- For constructs more complex than a single identifier, use the Kotlin counterpart's range-selection logic as the model when deciding how wide the range should be. Don't assume what that logic does — have Architecture Mapper trace the actual Kotlin source the same way it does for any other problem type. Kotlin's IntelliJ-facing diagnostics are expected to highlight full PSI-element ranges for the same usability reason `cjc`'s narrow anchors don't serve here, but confirm it in the code rather than relying on that expectation.
- This is more likely to originate on the non-PSI (raw-builder) path, where positions tend to be narrow offsets by default; the PSI path's elements may already span the full token naturally. Check `CfirAnalysisLLTTestGenerated` and `CfirAnalysisLLTPsiTestGenerated` failures independently rather than assuming a range bug on one implies the same bug on the other — this follows the same PSI/non-PSI split called out in Main Workflow.

## Kotlin Counterpart Check

Before editing CFIR, locate the Kotlin owner that matches the problem type. Examples:

- raw FIR building: `LightTreeRawFirDeclarationBuilder`, PSI raw builders, generated FIR builders
- constructor and delegation checks: FIR constructor builders and common constructor-delegation checkers
- value/class layout checks: FIR declaration checkers with the closest structural role
- resolve and calls: FIR call resolver, candidate selection, type substitutors
- diagnostics: FIR checker placement and source-element selection
- analysis API behavior: Kotlin Analysis API surfaces and FIR-backed implementation

Use Kotlin as architecture guidance, not as permission to import Kotlin-only language semantics into Cangjie.

## Fixture Edit Gate

A fixture edit is allowed only when all are true:

1. Official `cjc` or `external/cangjie_compiler` proves the fixture expectation is wrong — except for diagnostic-range-only edits, where the Diagnostic Range Policy above is itself the evidence.
2. The repo diagnostic surface is still respected, including project diagnostic names where LLT expects them.
3. The edit is explained as a fixture correction, not as adaptation to current CFIR output.
4. The same semantic family has been searched for other affected fixtures.

For ordinary LLT rendering, keep project diagnostic names. Reserve official `sema_*` name normalization for dedicated CJC-comparison tooling when the repository has such a separate checker.

## Repair Log (verification memory)

There is no separate eval harness for this skill — the repair log is what keeps results consistent across runs instead. Maintain `cfir/analysis-tests/REPAIR_LOG.md` (create it if missing) and append one entry per *completed and verified* problem type, using the Repair Report Fields below. Before starting a new problem type, check this log first:

- If the problem type was already logged as closed but the regression suite fails on it again, treat that as evidence the earlier fix was incomplete or got reverted — don't silently redo the analysis from scratch. Start by diffing current behavior against what the log says was fixed and why.
- If a related problem type is already logged, its evidence sources and Kotlin counterpart files are often directly reusable for the new one — check before re-deriving from scratch.

## Repair Report Fields

A complete entry has two groups. Keeping them separate matters: only the main agent runs Gradle, so only the main agent can ever fill in the verification group — requiring it from a subagent would be asking for a field it structurally cannot produce.

**Investigation fields** (assembled across the pipeline — Evidence Investigator, Architecture Mapper, and Implementer each contribute their piece; or by the main agent alone when working without subagents):

- problem type — Evidence Investigator
- root cause — Evidence Investigator
- official Cangjie evidence (`cjc` output and/or `external/cangjie_compiler` location) — Evidence Investigator
- Kotlin counterpart files consulted — Architecture Mapper
- CFIR owner files changed — Implementer
- repair principle (one sentence: why this is the shared fix, not a local patch) — Implementer
- fixtures covered (the full list, not just the fixture that triggered the investigation) — Implementer, cross-checked against the fixture list Evidence Investigator was given

**Verification fields** (added only by the main agent, after running the regression):

- verification command(s) and outcome

A pipeline's combined output is the investigation fields only — it is not yet a Repair Log entry. The main agent appends the verification fields after confirming the fix; only then does it become loggable. The Final Report uses both groups for every accepted fix, plus a rollup of "remaining failures grouped by problem type."

## Subagent Roles

If subagents are available, split each problem type into three roles run as a short pipeline, rather than one subagent doing everything end to end. The split exists to take "judge the official semantics" and "make the fixture pass" out of the same hands — that's the exact pressure point where shortcuts happen.

```
Evidence Investigator ─┐
                        ├─→ Implementer ─→ (main agent verifies)
Architecture Mapper ────┘
```

**Evidence Investigator**

- Input: problem type, full fixture list, and any existing Repair Log entries for the same or a related problem type
- Job: run `cjc` probes, search `external/cangjie_compiler`, and write a semantics finding — what's officially true, with the probe source/diagnostic output or compiler citation as proof
- Out of scope: never reads or edits CFIR code, never proposes a fix
- Read-only, so it can always run in parallel with everything else — including other Evidence Investigators for other problem types — there's nothing for it to conflict over

**Architecture Mapper**

- Input: problem type
- Job: locate the Kotlin counterpart files (FIR/Analysis API/Resolve/etc.) for this problem type, locate the current CFIR owner path, and write a short structural note — what Kotlin does architecturally, where the equivalent CFIR seam is, what's missing or diverged
- Out of scope: never determines semantics (that's Evidence Investigator's job), never writes the fix
- Also read-only, same unrestricted parallelism, same reason

Run Evidence Investigator and Architecture Mapper for a given problem type at the same time — they draw from disjoint sources and neither touches the working tree, so there's no reason to serialize them.

**Implementer**

- Input: the Evidence Investigator finding and the Architecture Mapper note for one problem type — never starts without both
- Job: write the CFIR fix. Every semantic claim in the diff must trace back to something in the Evidence Investigator's finding; if the fix needs a semantic fact the finding doesn't cover, send it back to Evidence Investigator rather than deciding it inline
- This is the only role that edits files, so it's the only role the owner-path overlap check applies to: before running two Implementers in parallel, check whether their problem types are likely to share a CFIR owner file (shared resolvers/checkers across problem types are common). If so, assign both to the same Implementer or run them sequentially — don't let two Implementers edit the same file at the same time.

Across all three roles: never run Gradle in a subagent, never let a subagent spawn another subagent, never accept a single-fixture fix.

The main agent still owns Gradle execution and verification:

- if it passes the full-type regression, append the verification fields and log it
- if it fails, that counts as one attempt under Escalation / Exit Conditions below — send the verification failure back to the Implementer as new evidence for one retry, then escalate to the user rather than retrying a third time

## Escalation / Exit Conditions

Don't loop indefinitely on a problem type. Stop and ask the user when either is true:

- two distinct fix attempts for the same problem type have both failed the full-type verification — a rejected Implementer fix counts as an attempt the same as a main-agent fix does; infrastructure retries (daemon loss, stale workers, file locks) don't count as attempts
- the official evidence is ambiguous or contradictory after checking both `cjc` and `external/cangjie_compiler` (they disagree, or the construct can't be minimized into a clean `cjc` probe)

When stuck, report exactly what was tried, what the evidence actually showed, and where the ambiguity is — don't keep proposing new patches against the same fixture to "see if it passes."

## Verification Protocol

After each problem-type fix, verify the complete type slice, not just the triggering fixture. Use focused generated-class filters when possible, for example:

```powershell
.\gradlew.bat :cfir:analysis-tests:test --tests 'org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated$Overload'
```

Use single quotes in PowerShell for nested generated classes so `$Name` is not expanded.

When a targeted run fails before assertions because of Gradle daemon disappearance, stale Java workers, or Windows file locks, treat that as infrastructure evidence first. Retry the same test or clear stale workers before changing source.

When a phase is complete, run:

```powershell
.\gradlew.bat :cfir:analysis-tests:test
```

Report verification from XML/test output, not from stale HTML or truncated console logs.

## Final Report

Report only evidence-backed facts, using both Repair Report Fields groups for each accepted fix, plus the remaining-failures rollup. Keep summaries short unless the user asks for detail.
