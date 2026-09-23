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

## Session-Proven Tooling & Pitfalls (2026-09-19)

1. **回归判定方法**: 只用「同一条命令的全量控制台日志」做 `FAILED` 行（suite > group > test 全名）集合差。
   不同 `--tests` 切片的失败总数不可互相比较——曾被误读为"越修越多"。
2. **取证工具**:
   - 多包 fixture: `python cfir/analysis-tests/tools/cjc_multipkg.py cfir/analysis-tests/testData/<rel>/main.cj`
     （fixture 是目录 + `main.cj`，不是单文件；单文件多包会报 "more than one package declaration"）。
   - 单构造探针: 写 `/tmp` 临时 `.cj` + `cjc --diagnostic-format=json --output-type=staticlib`，
     用 JSON 的 `MainHint.Range` 拿精确锚点（End 列 = 末字符列 + 1）。
3. **定位产生点必须先插桩验证 live 路径**: 凭代码直觉猜 owner 两次都猜错
   （`recordAssignmentRhsLiteralMismatch` 只见已解包类型；真 owner 是
   `CfirTypeSemanticsDiagnostics.literalConversionMismatch`）。`System.err.println` 进
   `build/test-results/test/TEST-*.xml`，用 `grep -a -o` 提取（注意 XML 转义）。
4. **级联抑制守卫的陷阱**: 「实参含错误类型 → 不报外层 UNABLE_TO_INFER」这类守卫必须挂在
   「推断确实失败」的分支内（如 `containsAnyTypeVariable()` 成立时）。挂在之前会把
   推断成功的合法调用降级成错误引用，破坏下游可达性/穷尽性分析（enum14 教训）。
5. **Option 的两种 CFIR 表示**: 显式 `Option<Int64>` 是 `ConeEnumType`，`?T` 语法糖是
   `ConeClassLikeType`——同一条判定对两种表示行为可能不同（回归只出现在其中一种时先查这个）。
6. **`ConeUnreportedDuplicateDiagnostic`**: cone→CfirDiagnostic 映射对它返回 emptyList，
   是「错误继续传播但不重复报告」的既定机制。
7. **语料自相矛盾（同一构造两种期望）→ 先停下问用户**，不要替用户选口径；用户裁决后
   按「保留工程既定约定、修离群 fixture」执行，并在 checker 内留注释登记与官方的差异点。

## Session-Proven Tooling & Pitfalls (2026-09-19 晚)

8. **【必读·会直接导致误判】同一文件在同一消息里发两处 `Edit` 时只有最后一处落盘。**
   症状：插桩（`System.err.println`）"装了但没输出"，`grep` 源码才发现根本没写进去；
   由此得出"真路径在别处"的错误结论，白跑数轮。
   纪律：**单文件单次 Edit**；每次 Edit 后立刻 `grep -n <新符号> <file>` + `git diff --numstat <file>`
   核验落盘，再启动测试。多文件可并行，同一文件不可。
9. **插桩判定 live 路径时，必须同时确认"入口 print 出现了"**。只看到深层 print 命中、
   却看不到入口 print，第一反应应是"插桩没落盘"，而不是"存在另一个调用者"。
10. **级联抑制（poisoning）的权威规则位置**：`external/cangjie_compiler/src/Sema/TypeArgumentInference.cpp:578`
    —— `GenerateTypeMappingByInference` 仅当 `!Utils::In(ce.args, [](const auto& a){ return !Ty::IsTyCorrect(a->ty); })`
    （无任何实参类型为错误类型）时才 `DiagnoseForCallInference` 报 `sema_unable_to_infer_generic_func`。
    CFIR 对应：`payloadEnumConstructorInferenceDiagnostic` 里对 `ConeUnableToInferGenericFuncError`
    用 `Candidate.hasErrorTypedArgument()` 抑制；**`isUninferredParameter` 的 error type 要排除**
    （它对应官方"类型正确但含未定类型变量"，如 `Some(None)`）。
11. **Cangjie 官方对照实验法（强烈推荐）**：判断某诊断是否由某构造"触发/抑制"时，
    写一对只差一个条件的探针（如"有上界约束 / 无上界约束"）跑 cjc，对比诊断集合。
    decl_0 的结论就是这样一次得到的：无约束时 `let v: C<Unit> = 1` 报 cannot_convert_literal，
    有约束（`Unit` 违反 `T <: I`）时**一条都不报** → 证明非法泛型实例化会抑制下游值类型检查。
12. **fixture 期望值与官方不一致 ≠ 可以按 CFIR 现状改写**。`sync_0.cj` 期望 0 诊断，
    官方实测 3 条；正确做法是先把 CFIR 的实现补到与官方一致（字面量→interface 形参应报
    `CANNOT_CONVERT_LITERAL`），再按官方口径写 fixture；不能把"CFIR 现在报什么"当期望。

## Session-Proven Tooling & Pitfalls (2026-09-20)

13. **核对"改动是否还在"必须用 `git grep <符号> HEAD`，不能看 `git status`。**
    并行会话（或在 `.claude/worktrees/<branch>/` 工作的人）会 `git commit` 掉你工作区的改动；
    之后 `git status` 干净、看起来像"我的改动被回滚了"，其实已在 HEAD 里。先 `git log --oneline -3`
    + `git grep -l <符号> HEAD -- <dir>` 确认，再决定要不要重做。
14. **上界判定（upper-bound）一族目前只能在 `CheckerContext` 下调用。**
    `satisfiesGenericArgumentUpperBound` / `satisfiesNonCTypeUpperBound` / `declaredUpperBoundTypes` /
    `isGenericTypeWithInvalidUpperBound` / `satisfiesGenericUpperBounds`（另一文件）全是
    `context(CheckerContext)`，且 `withSession`（`cfir-tree/SessionHolder.kt`）只能提供 `SessionHolder`，
    满足不了它们。想给"只有 session"的消费点（如 `coneDiagnosticToCfirDiagnostic.toCfirDiagnostics`
    / `argumentTypeMismatch`）加失效守卫，只有两条路：
    ①给该入口增加可选 `CheckerContext`（由 `ErrorNodeDiagnosticCollectorComponent.reportCfirDiagnostic` 传入，
    它同时持有 context 与 reporter）；②在收集器里按 cone 诊断的候选形参
    `returnTypeRef.hasInvalidGenericTypeArgument()`（既有使用点谓词）判定。
    **不要**试图把这一族降成 session 级——实测需要改 5 个函数（含跨文件），成本远超收益。
15. **多元素语料的锚点可能有两种约定，取决于错误种类。** `sema_mismatched_types_multiple_assign`
    （元数不匹配）锚 **RHS 元组**；元素类型不匹配则锚**整个赋值**。改锚点前必须先判断是哪种错误，
    不要用一个 fixture 的期望去推定同类构造。
16. **多个 `Edit` 打在同一文件时，只有最后一次落盘**（已在 8 条记录）。补充实操：
    需要改同一文件的"导入段 + 函数体"两处时，**分两条消息**各自 Edit，并在两次之间用
    `grep -n <新符号> <file>` 核验，再启动测试。

## Session-Proven Tooling & Pitfalls (2026-09-21)

17. **【流程教训·代价 10 轮测试】必须先用 cjc 拿到官方行为，再动 CFIR 代码。**
    本轮把 fixture 期望当权威、直接从第 ④ 层（CFIR 现状）插桩，跑了十轮才回头做第 ① 步。
    正确顺序的开销极小：单构造探针一次只花十几秒。**cjc 实测结论**（`option_with_element_01.cj`）：
    `Errors: 0` —— 比任何 CFIR 侧推理都更快地锁定了"错在 CFIR 而非 fixture"。
18. **cjc 是原生 Windows 程序，喂给它的路径必须是 Windows 形式。**
    直接用 `/tmp/xxx/probe.cj` 会报 `error: source file '...' doesn't exist`（不是"文件不存在"，
    是路径没被识别）。用 `cygpath -w /tmp/xxx` 转换后再拼 `"$WD\\probe.cj"`。
    可用的最小单构造探针（含 JSON 诊断与精确锚点）：
    ```
    export PATH="/usr/bin:/bin:$PATH"
    CJC=/c/Users/lin17/.cangjie/sdks/cangjie-1.0.5/bin/cjc.exe
    WD=$(cygpath -w /tmp/cjcprobe)
    "$CJC" --diagnostic-format=json --output-type=staticlib "$WD\\probe.cj" -o "$WD\\out"
    ```
    读 JSON 的 `Num: { Errors, Warnings }` 判结论；`MainHint.Range` 拿锚点（End 列 = 末字符列 + 1）。
    注意 chir 阶段的 `chir_dce_unused_function_main` 之类**不是 sema 诊断**，不要算进"官方报错"。
19. **【插桩技巧·大幅省轮次】查"某条约束是谁派生的"，打印 `Constraint.position` 而不是 kind/type。**
    `ConstraintPosition` 的 `toString()` 自带完整派生链，例如：
    `Incorporate Int64 <: TypeVariable(T) from Argument <字面量节点>`。
    这一条直接定位了 owner，比只打 `kind`/`type` 再回头猜调用栈高效得多。
    位置：`ConstraintInjector.processGivenConstraints` 里 `constraints.addConstraint(...)` 之前。
20. **`ConstraintSystemImpl` 里做行内探针的两个编译坑**：`typeVariable.typeConstructor()`
    需要 `TypeSystemContext` receiver（写成 `with(c) { ... }`）；`position.from` 在该作用域不可用
    （会报 `Unresolved reference ... receiver type mismatch`）。取变量身份用
    `System.identityHashCode(typeVariable)`。
21. **不要用 `io.open(p,'w')` 脚本改写仓库内的 Kotlin 文件**：Windows 上会把 LF 转成 CRLF，
    结果 `git status` 显示 ` M`，但 `git diff` / `git diff --numstat` 全为空，极易误判成
    "改坏了"或"没改成功"。要脚本改写就传 `newline=''`；能用 Edit 就别用脚本。
    同理：对**非自己本轮新建**的文件不要用 `git checkout --` 清探针（并行会话会改同一棵树），
    应改用 Edit 精确删除自己加的行。
22. **`option_with_element_01` 的已知结论（避免重挖）**：官方 `T = Option<Int64>` 靠 Option auto-boxing
    （`external/cangjie_compiler/src/Sema/TypeManager.cpp:836-839`）；官方先由下界 join 解出 `T`
    再实例化声明上界，见 `LocalTypeArgumentSynthesis.cpp:32-60`（注释里就有本用例的形状）与 `:329`
    （正常推断只写 lbs/ubs，等价约束仅在 `deterministic` 诊断重跑路径）。
    CFIR 偏离点：incorporation 把 `A <: α <: B ⇒ A <: B` 用在"下界 × 声明上界"上，
    检查 `Int64 <: Equatable<T>` 时反手给 `T` 加了 `T <: Int64`，与 `Int64 <: T` 合并成
    `EQUALITY:Int64` 把 `T` 钉死。**剩余未知**：`T <: Int64` 在子类型检查器内的具体发出点。
    详见 `cfir/analysis-tests/REPAIR_LOG.md` 2026-09-21 两节。

## Session-Proven Tooling & Pitfalls (2026-09-23)

23. **【方案评估】"原语已存在" ≠ "该路线可行"。** 评估"用既有快照/回滚/抑制原语绕过某个框架缺陷"时，
    结论必须落在**"回滚或抑制之后，后续判定与渲染所需的信息是否仍然存在"**，而不是"原语齐备度"。
    典型失效形态：原语齐全、且已在同一文件复用多处，但目标信息从未被生产过 → 回滚后仍是空值，
    消费点直接 `return`，**收益为零而回归风险非零**。原语齐备度与方案可行性是两件独立的事，必须分开评估。
24. **【方案评估】"信息在何处被生产、又被何处销毁"决定修复层级。** 若修复所需的事实由阶段 A 产出、
    随即被阶段 B 的写回覆盖，则任何发生在 A 之外的补救（调用外部、消费点、checker 侧）都取不回该事实，
    应直接否决；正确层级是在**该事实被生产之前**建立独立求值（对位官方"解糖前独立综合"那一层）。
    实操：先定位"产生点 + 销毁点"，再选层级——一次即可否掉整族错误方案，远快于逐个试。
25. **【缺陷可闭合性】提前返回/提前失效的判据若是"递归取子树内任一错误"的实现，会同时吞掉两类语义相反的事实。**
    此时"实体自身的根错误"（应保留、应单独报）与"下游阶段写回的错误"（应丢弃、应改报外层）
    在该判据处**可观测特征完全相同**；任何"让判据忽略后者"的改动都会同时放走前者 →
    缺陷**不可独立闭合**。判定法：列全该判据在期望行为上必须区分的语义，逐个检查本仓是否可区分；
    全不可区分就登记为"需配套基建"，**不要写局部补丁**，也不要用形态枚举去绕（那属于反面信号）。
26. **【归因纪律】"不可重入"类缺陷的根因要落在不变量的名字上，不要落在"写得不够幂等"上。**
    实测形态：写入 API 是纯赋值、无任何断言（所以"幂等性"这条线本来就是死路），真正被违反的是某个
    **阶段不变量**（如"已解析的表达式才有 resolvedType"），由调用链读取未完成节点触发；修法方向随之不同
    （不是加去重表/守卫，而是让该调用发生在阶段边界允许的位置）。
    另：若某形状的**精确中间栈帧没有留证**，日志必须写成"由抛点 + 已文档化不变量推出"，不能写成已观测——
    归因的证据等级要显式标注。
27. **【提交纪律】在含他人未提交改动的脏树上提交**：`git add <file>` 会把该文件里**他人在途的改动一起带走**。
    先 `git diff --numstat <file>` 确认改动只属于自己；混改时按 hunk 归属分离再 `git apply --cached`。
    提交后立刻 `git diff --cached --name-only` 核验暂存区**恰好等于**预期文件集。
28. **【核验纪律】改动 CRLF 文件后要单独验证行尾。** git 会归一化行尾，`git diff` 对"整文件行尾被翻转"
    可能完全沉默（见第 21 条）。核验式：`total=$(wc -l < f)`、`crlf=$(grep -c $'\r$' f)`，
    要求 `lf_only = total - crlf = 0`。反面用法：`git status` 显示 ` M` 而 `git diff --numstat` 为空时，
    **先怀疑行尾，不要怀疑 Edit 没落盘**（真要查落盘用 `grep -n <新符号> <file>`）。
29. **【回归前置】改完源码要跑的全量测试，前提是工作树能编译。** 若工作树同时存在他人未完成的重构，
    先做一次**仅编译**的全仓扫描再谈测试：`classes testClasses testFixturesClasses --continue`
    （根项目聚合调用会覆盖所有子项目；`--continue` 保证前一个模块失败不影响后续模块继续编译）。
    这样能一次性拿到"哪个模块、哪一行、缺什么"的完整清单，避免逐模块迭代编译。
    注意本仓库的生成式构造器（`cfir-tree/gen/**/*Impl.kt` 由 `CfirTree.kt` 规格生成）**没有默认参数**，
    连可空参数也是必填；所以**给树规格加字段 = 所有手写构造点一起破**，修复一律落在调用点，
    不要去手改 `gen/` 目录。

## Session-Proven Tooling & Pitfalls (2026-09-23 续)

30. **【机制判定】二次求值类失败先查"同一节点是否被解析了两次"，不要去调"完成强度"。**
    实测否证：把某个预综合的求值语境从"强制完整完成"换成"不强制完成 + 不携带期望类型"，
    失败照旧；而且异常**不在**新的预综合里抛出，而在紧随其后的那次调用内部——第二次解析读到
    第一次留下的**中间态**节点才违规。判据顺序：①先看异常抛在"第几次解析"里（读栈帧的行号
    落在哪一步）→ ②再谈语境/模式参数。把 ① 跳过直接调模式参数，会白改一轮并误判机制。
31. **【回滚时机】"事后快照回滚"救不了"解析途中被读的不变量"。**
    快照/回滚原语再齐全也没用：若被违反的不变量是在第二次解析**进行中**被读取的，
    异常会在回滚点**之前**抛出，回滚根本轮不到执行。正确形态是**在解析之前**冻结或克隆节点状态、
    让第二次解析从干净状态起步，而不是解析完成之后再恢复。⇒ 评估回滚方案时，先问
    "不变量是在解析之后被读，还是解析途中被读"，这一问直接决定方案可行性。
32. **【改动前置】给 `sealed` 类加子类型会让穷尽 `when` 立刻编译失败。**
    动手前先 grep 全部穷尽分支（`is X ->` 且该 `when` 无 `else`）；本仓 `ResolutionMode` 在
    `src` 内仅有一处（`BodyResolveContext.kt:2470`）。新增分支必须选**语义正确**的那一组，
    不要图省事加 `else` —— 加 `else` 等于把"新模式该走哪条路"这个判断永久隐藏。
33. **【诚实登记】失败的实现尝试也要进 REPAIR_LOG，且必须回退干净。**
    记录三样东西：①尝试了什么（含被否证的假设）；②观测到的**否证证据**（栈帧/失败集合）；
    ③新得到的结论与下一步正确形态。回退后核验 `git diff --numstat` 为空 + 新符号零残留，
    并在提交信息里写明"无源码残留"。失败尝试的栈帧往往比成功路径更有信息量——
    本仓 flow 缺陷的精确栈帧就是这样第一次拿到的（此前只记录了"未取得"）。

## Session-Proven Tooling & Pitfalls (2026-09-23 晚)

34. **【结论复核·最高优先级】"需要新基建"的结论必须先回到官方的最终决策点复核，再决定动手。**
    错误推理链的形态：观测到"共享求值阶段污染了某个输入" → 断言"官方一定有隔离机制" →
    断言"本仓需要同等的投机求值/回滚基建" → 转入大改造。**该链条的中段前提在实测中为假。**
    实际形态：官方**不隔离**污染，它只是**不信**污染结果——在最终决策点重新按"每个输入**自身**的
    独立结果"判定，两个输入各自都成立才报外层诊断。
    判据：新基建的必要性只能由"官方在同一位置也做了同等操作"证明；
    **"本仓当前实现做不到"不构成证据**。
    实操：读官方源码要读**"决定报不报的那一刻"**（诊断发出点 / 分支选择函数），
    不是综合/求值阶段——两者结论常常完全相反。
35. **【语义等价物】判定顺序即语义。**
    同一组判定条件、不同执行顺序 → 可观测诊断不同。若官方在 A 失败后仍回到 B 独立判定，
    则本仓"先判 A 就提前返回"的结构**本身就偏离官方**；对齐顺序即可，不需要任何新机制。
    不要试图让共享阶段不写回——写回是它服务成功路径的正常工作方式，污染只是副产品；
    正确做法是在**失败路径上重新独立求值**，而不是阻止共享阶段写回。
    回归压缩手段：改动**只落在失败分支**，成功分支保持字节级不变
    （同一缺陷的两次实现尝试，切片差从 7 项新失败降到 0）。
36. **【事实订正】官方 `PData::Reset` 重置的是类型变量约束库（`src/Sema/Utils.cpp`），不是 AST 节点。**
    节点级原语是 `Node::Clear()`（`include/cangjie/AST/Node.h:243-250`，`ty = Ty::GetInitialTy()`）。
    任何以"Reset 能清掉节点上的中间态"为前提的方案都建立在错误事实上。
    这是第 34 条的一个具体实例：**错误的前提会稳定地导出错误的基建需求**——
    评估方案前先把官方原语的真实语义查清，再去谈"缺哪块基建"。
37. **【取证版本】官方行为必须与 `external/cangjie_compiler` 的 pin 版本对齐（现为 `v1.0.0` / `b776b44c`），
    不能只用更新版 SDK 下结论。**
    本机 `~/.cangjie/sdks/` 有多版本（0.53.13 / 0.53.18 / 1.0.0 / 1.0.5 / 1.1.3），
    而 1.0.5 已比镜像新（含镜像里不存在的行为/诊断），拿它当"官方"可能与镜像源码对不上。
    跑非默认版本必须**覆盖 `CANGJIE_HOME`**（各版本 modules 目录名不同，
    如 1.0.0=`windows_x86_64_llvm`、1.0.5=`windows_x86_64_cjnative`），
    并用 `-o <dir>` 定向输出，否则产物会落在仓库根。
    纪律：两版行为一致 → 直接引用；不一致 → 回镜像定位"哪次引入"，**以 pin 版本为准**并在报告里标注差异。
