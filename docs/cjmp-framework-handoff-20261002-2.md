# CJMP 框架实施交接（二）：2026-10-02

> **这是当前接手入口。** 本文件补充并覆盖同日第一份交接的运行状态与下一步。
> 2026-10-03 后续工作区和运行作业状态见 [1003 实施状态](cjmp-framework-handoff-20261003.md)。
> 先读本文，再读 `docs/cjmp-framework-handoff-20261002.md`、主计划和迁移台账。
> 当前有未提交源码；定向构建已失败结束，存在明确的模块边界编译错误。不要把上一份交接中的“无源码改动、无后台任务”用于当前工作区。

## 1. 目标、位置和保护边界

目标始终是完成 `docs/cjmp-framework-review-and-fix-plan-20260928.md` 的完整 P0–P10 / A1–J1，
在隔离工作树实施并及时提交。目标尚未完成；不得缩成 enum 修复或“测试全绿”。

- 唯一实施工作树：`C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie`。
- 分支：`codex/cjmp-framework-implementation`。
- 本文落盘前的实现 HEAD：`f9ac7f2e0`，`feat(cjmp): evaluate enum counterparts through ordered matching plans`。
- 该提交前是 `acbd6f267`（第一份 1002 交接）。本文的文档提交不会代表当前源码已验收。
- 原仓 `D:\code\intellij\cangjie` 保持只读；仅读取参考实现及使用原仓已有 queue JAR。
- 不创建第二个工作树，不重新恢复，不 reset/checkout/stash/clean 当前工作区。
- 旧恢复已经完成，30,609 个文件的恢复记录见旧交接。**当前未提交修改仍在磁盘上，不是丢失。**
- 保留他人已有删除：根目录 `测试结果 -Run_Cfir_Test_All.html`；不得暂存或恢复。
- 保留未跟踪的旧探针目录、原生编译产物；不要用 `git add -A` 混入它们。

规则：PowerShell 7；读文件用 UTF-8；写文件用 apply_patch。框架级修复，禁止假 CfirFunction、
fixture 特判、语义降级、兼容兜底、全模块 BODY 预热。主线程编辑生产及运行 Gradle；子代理取证/审查/按分工写测试。
已应用技能：原仓 `.agents/skills/kotlin-cangjie-frontend-parity/SKILL.md` 和
`.agents/skills/cangjie-cfir-llt-repair/SKILL.md`。

## 2. 已提交、已验证的检查点：f9ac7f2e0

此检查点完成 **enum→enum 专用计划接线**，不等于完整混合候选支持。

1. enum 同名构造项按真实 owner 的声明顺序进入有序计划，使用 Callable/TryBind；
   未匹配（含非穷尽 extra）也记录已处理结果，诊断豁免由 checker 决定。
2. payload 比较完整函数类型，包括 enum 返回类型，按 common→specific 外层泛型映射后严格相等。
   替换了不完整的手工类型递归；现在通过 CfirTypeSubstitutorByMap 和 AbstractTypeChecker.equalTypes。
   ConeEnumType 不是 ConeClassLikeType，旧递归遗漏它曾导致真实泛型失败。
3. LL 去掉 enum 的 CJMP phase 后置条件豁免；enum 的 BODY 确实执行现有 transformer，
   TYPES/BODY StateKeeper 保护其返回类型与 payload 类型，不是空操作。
4. LL TYPES 的 callable header 使用 designation 中真实的 enclosing class/extend/file，
   修正把 callable 自身当 topContainer 导致 enum 返回错误类型的问题。
5. 新增 enum 计划测试、3 个 LL 用例及官方探针；更新主计划 §13.15、迁移台账、REPAIR_LOG。

这些修复已提交，不要再次实现。工作区的后续修改正在替换其中专用 enum 工厂/runner 路径，
以支持官方允许的 enum/function 混合候选。

### 已验收结果（针对 f9ac7f2e0）

| 验证 | 结果 |
|---|---|
| 完整 LL | 150 / 0 失败 |
| 完整 CFIR XML | 8966 = 8569 passed + 397 skipped，0 failed |
| CLI 两段式 | 19 / 19 |
| enum 计划、原函数计划、返回状态定向 | 6 + 4 + 4 全通过 |
| 完整 LL + CFIR 最后一轮 | BUILD SUCCESSFUL，31m24s，禁用 build cache |

CFIR 权威快照：
`cfir/analysis-tests/build/ffi-annotation-verification/20261002-enum-plan-full/summary.json`。
本文写作时再次读取确认 8966/8569/397/0，比较首轮快照为 2 fixed、0 regressed、0 new、0 removed。
控制台曾显示 8965/396；总账按 XML，不按控制台片段。

`:cfir:resolve:test` 的既有 5 失败已在干净基线复核：`CfirMapArgumentsTest.Defaults`、
`CfirMapTypeArgumentsTest` ×3、`CfirBuiltinSymbolProviderTest`。它们不能直接归为本轮回归。

## 3. 当前未提交生产修改：正在做什么

**以下 11 个文件已经修改，但未完成集成验收，不能称为已完成实施。**
路径均相对唯一工作树；用 `git diff` 读取真实内容。

| 文件 | 已落盘改动 |
|---|---|
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatchingContext.kt` | 真实 callable 参数访问 `cjmpValueParameters`；payload enum 是 function-like；返回类型读取真实 callable；双方 enum 严格相等，混合/普通函数保留 Quest/协变 |
| 同目录 `CfirCjmpMatcher.kt` | 删除重复 `matchEnumConstructors`；enum 按 payload 分类 function/variable；读取 enum 的真实泛型参数 |
| 同目录 `CfirCjmpResolver.kt` | `buildCallableMatchPlan` / `CfirCjmpCallableCandidate` 接受真实 callable；删除 enum 专用工厂/候选类；共享同名组；三个工厂都移除外部 `candidateEligibility`，唯一调用 `prepareCandidate` |
| 同目录 `CfirCjmpMatchRunner.kt` | 函数/enum 共用同名有序组与一次求值；删除 enum 专用 runner；三个 plan 路径不再提前调用完整 matcher；显式记录 false 结果；默认来源只认真实 helper |
| `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonSpecificChecker.kt` | enum 先重放候选诊断，有绑定再 checkMatched；非穷尽只免 NOT_MATCHED；真实 payload 参数定位；按 common 侧决定 post-return 豁免 |
| `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpCommonSideFacts.kt` | enum 的 `declKind` 按 native 类型区分 function/variable；重复实现消息仍需后续补齐，见 §6 |
| `cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirParameterDesugaring.kt` | 新增 `hasInitialDefaultValue`：metadata.hasInitial 优先，禁止 marker 覆盖明确 false |
| `resolution.common/src/org/cangnova/cangjie/resolve/calls/mpp/AbstractCjmpMatcher.kt` | 拆 `matchEligibility`、`prepareCandidate`、私有 `matchEligibleSignature`；资格不再读参数/default；旧完整 matcher仍用于未迁移路径 |
| 同目录 `CjmpMatchingContext.kt` | 新增 `CjmpPreparedCandidate`；保存资格、可空签名、映射、缺实现事实 |
| 同目录 `CjmpMatchPlan.kt` | 签名事实可空，表示结构资格已拒绝、签名未执行 |
| 同目录 `CjmpMatchEvaluator.kt` | 资格通过后断言签名事实存在，随后执行有序参数程序 |

### 为什么必须拆资格

旧 runner 将 `matchSpecificAgainstPotentialCommon` 的完整结果作为 eligibility，
会先发现参数 1 的名字错误，导致参数 0 本应已完成的默认来源转移完全不执行。
正确顺序：结构资格 → 类型签名 → 参数 0（名字/冲突/来源转移）→ 参数 1……→ 占用/实现要求/绑定。
类型签名失败时直接停止；“签名失败仍执行参数名字诊断”的旧注释已删除。

当前实现把名称、角色、依赖、真实 owner 的筛选留在 resolver；共享 eligibility 负责 native kind 与本声明泛型数量。
该职责划分已发给审查代理，但**最后版本尚未收到 gate 通过**。接手者要复核，而非宣称已 100% 对齐。

### 默认值事实：已修提取，但运行时链条仍未完成

- `cfirCjmpDefaultStates` 现在是 internal，输入真实 callable；
  hasInitial 使用共享 `hasInitialDefaultValue`，来源槽只存 `metadata.function?.symbol`。
- CJO `HAS_INITIAL` 与 FuncParamList.desugars 的真实 helper 是独立事实。
  反序列化 defaultValue 可以只是 Unit/null 占位表达式，不能用它/参数 symbol 冒充 helper。
- 源码 helper 生成仍是 P8 欠项，当前不虚构 helper。
- `writePlanAggregate` 尚不发布 `aggregate.defaults`；调用端默认值仍从最终 counterpart 推导，
  会丢失失败候选已转移的前缀。**当前修改不能宣称完整默认值语义已修复。**
- 真正 helper owner 是原函数的外层 class/extend/file，不是参数或函数本身。

## 4. 测试文件与子代理：中断时的真实状态

用户要求交接后没有继续修改生产或启动新构建。子代理当前均 completed/interrupted，无后台代理继续写文件。

### 已落盘、未跟踪的 LL 回归

- `analysis/low-level-api-cfir/test/org/cangnova/cangjie/analysis/low/level/api/cfir/resolve/CfirCjmpMixedEnumCandidateTest.kt`
- `analysis/low-level-api-cfir/testData/cjmpMixedEnumCandidate/variantMatchesCommonFunction.cj`

该测试仅注入模式并从 specific variant 首次请求 CJMP，不预热 body；断言对应普通 common 函数，
随后断言 `E.Value(1)` 调用目标是真实 specific variant，而非会返回 A 的 common 默认函数。
重复查询需稳定，common/caller 的 lazy 行为有断言。手写测试，不需要生成器。

### 尚未落盘的测试迁移（接手优先补）

`baseline_auditor` 被中断前没有提交这部分编辑。本文写作时 rg 仍确认：

- `CfirCjmpFunctionPlanTest.kt` 仍用已删除的 `buildFunctionMatchPlan` / `CfirCjmpFunctionCandidate`。
- `CfirCjmpEnumConstructorPlanTest.kt` 仍用已删除的 `buildEnumConstructorMatchPlan` / `matchEnumConstructors`。
- 因此当前 **resolve 测试源码尚不兼容新 API**；不能把这当成未知的新语义失败。
- 待迁移为 `buildCallableMatchPlan` / `CfirCjmpCallableCandidate`，移除 eligibility 入参；
  enum payload 必须有与参数数目相等的 neutral defaults/parameter facts，不能恢复空列表捷径。
- enum 类型断言改走 `CfirCjmpMatchingContext + AbstractCjmpMatcher.matchSignature`，保留真实 enum 返回类型、
  泛型映射、严格相等、nullary、占用顺序等已有覆盖；增加 payload enum/function 双向混配。
- `CfirCjmpDefaultStateTest.kt` **尚不存在**；待覆盖真实 metadata/helper 四组合、明确 false + marker、
  源码/惰性/真实 Unit 默认值；不要只测手工 plan defaults。
- 另应新增资格阶段不提前执行参数程序的回归：前一参数已转移、后一参数失败、后续候选继续消费，
  以及后续候选成功仍保留早期诊断。

### 新探针只写了源码，未形成可引用实测结论

`.workbuddy/tmp/cjmp_probe113/review-20261002/mixed-boundaries/` 当前 8 组、16 个 .cj：
reverse、reverseStatic、reverseNamedDefault、forwardStatic、nonExhaustiveNamed、nonExhaustiveDefault、
duplicateBare、duplicatePayload。语义代理中断；未看到结果报告，不得把预期当实测。
需要续跑、核对语法/编译结果，再决定正式 fixture；不要猜造结果。

## 5. 最后一轮 Gradle：已失败结束

本文写作过程中持续轮询同一作业，交付前已确认终止：**BUILD FAILED in 10m54s，exitCode=1**。
队列 JSON 状态为 DONE，finishedAt 对应 2026-10-02 16:30:35（Asia/Shanghai）。
没有这轮作业需要继续等待；daemon 进程可能仍存活，不能把 daemon 存活视为作业仍运行。

- 会话工具 handle：`94253`（新会话未必能访问，不能只靠它）。
- 队列 task：`2297f35a`，文件 `.gradle/queue/tasks/2297f35a.json`。
- queue JVM PID `66416`；wrapper PID `59416`；daemon PID `46932`。
- 启动时间：2026-10-02 16:19（Asia/Shanghai）。PID 只用于核对本次身份，接手时须再查存活与命令行。
- daemon 日志：`C:\Users\lin17\.gradle\daemon\9.4.0\daemon-46932.out.log`。
- 原命令如下；这是失败作业的复现命令，接手先修 §9 的编译错误，再运行：

```powershell
java -jar 'D:\code\intellij\cangjie\gradle-queue-cli\build\libs\gradle-queue-cli.jar' --project-dir 'C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie' :resolution.common:test :analysis:low-level-api-cfir:test --tests '*CfirCjmpMixedEnumCandidateTest*' --tests '*CfirCjmpEnumMatchingTest*' --no-build-cache --continue --console=plain --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8'
```

已确认本次 `:resolution.common:test` 执行成功，16:22 更新的 4 个 XML 合计 **57 passed /0 failed /0 skipped**。
resolve 生产编译也执行通过；**checkers 编译失败，因此 LL 测试未执行**。
不能将这些 57 项或 resolve 编译通过当成整个未提交改动验收。
当前命令不含 `:cfir:resolve:test`，也不含完整 LL、完整 CFIR 或 CLI 两段式。
具体错误、模块依赖证据和第一步修复入口见 §9。

可独立查询：

```powershell
Get-Content -LiteralPath '.gradle/queue/tasks/2297f35a.json' -Encoding UTF8
Get-CimInstance Win32_Process | Where-Object { $_.ProcessId -in 66416,59416,46932 } | Select-Object ProcessId,ParentProcessId,CommandLine
Get-Content -LiteralPath 'C:\Users\lin17\.gradle\daemon\9.4.0\daemon-46932.out.log' -Encoding UTF8 -Tail 60
```

其他会话/其他工作树有 Gradle 进程，不要据此杀所有 daemon。生成器可能让 gen 文件显示 M，先看实质 diff，不批量回退。

## 6. 官方证据及仍需修正的边界

官方源码只读 `D:\code\intellij\cangjie\external\cangjie_compiler`，必须用
`git show v1.1.3:<path>`；checkout 是 v1.0.0，不能直接当 1.1.3。
Kotlin 参考在原仓 `external/kotlin`；Kotlin 不提供混合 enum/function 的语义授权，只用于框架职责对照。

### 已实测 mixed_body（已提交 README）

`.workbuddy/tmp/cjmp_probe113/review-20261002/enum-plan-main/README.md` 有完整命令、SDK、结果。
common：

```cj
package cjmp_enum_mixed_body_probe
public common enum E {
    A | ...
    public common func Value(x: Int64): E { A }
}
```

specific：

```cj
package cjmp_enum_mixed_body_probe
public specific enum E { A | Value(Int64) }
```

两段编译 exit 0；specific 阶段只有 unused-parameter warning。**不要加 static**。
另一个 static 探针报告 `sema_specific_has_different_modifier`，未报 NOT_MATCHED，说明匹配确实发生。
`enum E { A() }` 在 1.1.3 非法，payload 形式至少一个类型。

### 方向性和 native kind（已按官方源码核实）

- `src/Parse/ParseDecl.cpp`（已用 v1.1.3 git ls-tree 核实路径）：1241–1293，payload 创建 FuncDecl，nullary 创建 VarDecl；1483–1488 传播 common/specific。
- `src/Sema/CJMP/CheckCJMP.cpp:871–895` 是资格；896–912 是占用优先的 TrySetSpecificImpl。
- 915 起普通函数参数程序；932–935 类型签名失败即返回；938–954 名字/default 按位置检查。
- 1014–1047 比较 enum 完整函数类型并 TryBind。
- 1115–1155 先匹配 ASTKind；**仅双方 enum 才进入 enum 专用语义**，否则走普通 function/variable。
- 604–624 PostTypeCheck 遍历 common。608 的 `IsEnumConstructor(*node)` 豁免的是 common：
  common enum→specific 普通函数跳过普通 post-return；common 普通函数→specific payload enum仍需执行。
- 1237 对已绑定非 nominal 做 MatchCJMPDeclAttrs；648 的注解检查也遍历绑定，不能因 specific 为 enum 跳过。
- 1050–1089 mixed VAR 路径不同于 TryBind：static/global 可直接覆盖实现并置 doNotExport；
  实例字段依赖 merge seed；static 不同仍可绑定并在后置报告修饰符。不能用 STATIC_MISMATCH 代替缺失 seed。

### 尚未闭合的本轮问题

1. common 侧重复实现消息 `implementationKind(common)` 还只拿 common。
   双方 enum 的官方 kind 是 `enum 'E' constructor`，mixed FuncDecl 为 `function`。
   应消费导致占用失败的实际配对/事件，不能在 checker 重新匹配，不能简单跟随新 declKind。
2. nullary enum↔普通变量尚未纳入；它要求真实 VAR 事实、按双方选择绑定规则、merge seed，
   并修复发布器：求值器 VariableAssignment 可覆盖实现，但旧 storage.bind 会拒绝第二实现。
3. 当前 parser/scopes 对 mixed 调用的最终身份仍需 LL 测试验证。
   已只读检查 `cfir/resolve/src/org/cangnova/cangjie/cfir/scopes/impl/CfirActualizingScope.kt`；
   如测试出现“已匹配但仍调用 common”，沿真实调用候选/actualizeCjmpCandidates 追查，未对此文件作改动。
4. 最后一次 Kotlin parity gate 尚未通过。先前阻断是完整 matcher 提前检查参数；已改源码，但需复核最终差异。

## 7. 整体完成与未完成

第一份 1002 的约 51% 是历史加权估计；更早的 1/11 是不同口径。
本轮没有重新逐条审计 P0–P10，**不提供新的整体百分比**。

已具备：原 caller-first 红例修复；complete/checker 读取与 current/in-resolution 读取分离；
家族完备 API 与读取迁移；函数/构造器、property/accessor、static/global 的纯计划运行时接线；
已提交 enum→enum 专用接线；纯容器合并算法 15 个测试；完整 LL/CFIR/CLI 基线验证入口。

仍未完成，必须保留在总目标：

1. 收尾本轮 mixed function/enum、资格/参数顺序与默认事实，补测试、复审、全量验收、提交。
2. pattern 接计划；保留非变量项的停止边界，不能扁平化 bindingVariables 丢语义。
3. 真正 CFIR→CjmpMergeContainer 事实构建及 initialBindings seed，实例字段与 mixed VAR 运行时接线。
4. P3/P4：真实 actualized 成员图在继承合并之前形成，结构 scope/实际化 scope 分离缓存。
5. 声明级不可变最终发布；去掉旧 storage.bind/record*/clear、兄弟写入和重新计算；不能保留双真相。
6. P8：doNotExport 写入实际运行时/CJO、源码默认 helper 生成、失败候选 default 前缀发布与消费、
   非字面量和跨包/common-only 导出。纯 aggregate 中有字段不等于运行时已接线。
7. P6 E 组、P7 生产 LL/IDE provider、P9 关联 resolve 修复、P10 最终逐条验收，以及主计划其余未证实要求。

继续保留旧交接的合并不变量：nominal 不是 first-specific-wins；generic/non-generic 不同跳过，
两侧都有泛型时数量诊断在后；common-only/普通/private 成员留在最终成员图；只有符合资格的真实实例 VarDecl seed；
重复字段保留指针链；property/function/constructor 不提前字段绑定。

## 8. 新会话接手顺序与命令

1. 进入唯一工作树，读 AGENTS.md、CLAUDE.md、本文、第一份 1002、主计划、迁移台账。
   `git -c core.safecrlf=false status --short` 与 diff 核对；源码仍未提交，不做恢复或清理。
2. 队列 2297f35a 已 DONE/exitCode=1，不需要等待或重启旧 handle。先修 §9 的共享参数访问器模块归属。
3. 完成 §4 的旧单测 API 迁移和新 adapter 测试，再运行 LL 定向；旧 LL XML 不能当本轮结果。
4. 重新做语义/框架审查，处理 §6；把只写了源码的探针跑完，不把源文件存在算实测完成。
5. 定向 resolve 计划/default/返回状态，CLI 两段式；最后跑 **完整 LL + 完整 CFIR**，检查 XML 与快照。
6. 验收后更新主计划/台账/REPAIR_LOG，只暂存本次文件或 hunk，及时提交；再推进完整剩余计划。

唯一 Gradle 模板：

```powershell
java -jar 'D:\code\intellij\cangjie\gradle-queue-cli\build\libs\gradle-queue-cli.jar' --project-dir 'C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie' <TASKS> --no-build-cache --continue --console=plain --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8'
```

任务参数：

```text
:cfir:resolve:test --tests '*CfirCjmpEnumConstructorPlanTest*' --tests '*CfirCjmpFunctionPlanTest*' --tests '*CfirCjmpDefaultStateTest*' --tests '*CfirCjmpReturnTypeStateTest*'
:compiler:frontend:test --tests '*CjmpTwoPhaseCompilationTest*'
:analysis:low-level-api-cfir:test :cfir:analysis-tests:test
```

ReturnTypeState 类名已用 rg 核实。不要追加 `--rerun-tasks`（既往 OOM/daemon 消失）；
不把 FROM-CACHE 当实际验证，不在 LLT 前单独 assemble/build。两条完整套件是每次代码完成后的回归闸门。

```powershell
.\tools\verification\cfir-test-result-ledger.ps1 -SnapshotName 20261002-mixed-callable-full -CompareTo 'C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie\cfir\analysis-tests\build\ffi-annotation-verification\20261002-enum-plan-full'
```

如果直接 apply_patch 工具不可用，用已有 wrapper 定位引擎（多行 .bat 调用曾破坏参数）：

```powershell
$shim = (Get-Command apply_patch).Source
$relative = [regex]::Match((Get-Content -LiteralPath $shim -Encoding UTF8 -Raw), '"%~dp0([^"]+codex\.exe)"').Groups[1].Value
if (!$relative) { throw 'Cannot resolve apply_patch engine' }
$engine = [IO.Path]::GetFullPath((Join-Path (Split-Path -Parent $shim) $relative))
# $patch 是完整 apply_patch 文本；大改动分小块，避免 Windows 命令行长度上限。
& $engine --codex-run-as-apply-patch $patch
```

## 9. 交付前最后观察与第一项修复

队列 2297f35a 已结束，handle 94253 返回 exitCode=1；没有本轮后台构建需要接手等待。
57 项共享算法测试通过；resolve 生产编译通过；checkers 编译失败，LL 测试未运行。
这是当前未提交修改引入的编译错误，已定位，不能归入旧基线 5 个失败。

错误位置：

```text
CfirCommonSpecificChecker.kt:14:42   Unresolved reference 'cjmp'
CfirCommonSpecificChecker.kt:790:78  Unresolved reference 'cjmpValueParameters'
CfirCommonSpecificChecker.kt:792:45  Unresolved reference 'source'
```

根因：本轮把共享的真实参数访问器放在 resolve 的 CfirCjmpMatchingContext.kt，checker 导入它。
但 cfir/checkers/build.gradle.kts 明确写着“不能依赖resolve模块，如果需要resolve模块的东西，应该将其声明移动到providers或semantics”，
依赖表也确实没有 resolve。

接手第一步应调整**参数事实访问器的共享归属**，放到两侧可依赖的数据/语义层；不新增 checkers→resolve 依赖，
不伪造函数节点，不复制另一套匹配算法。第三个 source 错误是访问器无法解析后的连带错误。
该修复本会话未执行，用户已要求转为编写交接文档。

当前生产改动仍是 11 个 tracked 文件；新 LL 测试和 mixed-boundaries 探针仍未跟踪。
本次交接只提交文档，未把这些未验收源码或原有 HTML 删除一并提交。
本文不能覆盖主计划全部完成证据；目标继续保持未完成。
