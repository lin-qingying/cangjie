# CJMP 实施续接（2026-10-09）

## 工作位置与范围

- 唯一实施工作树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。
- 分支：`codex/cjmp-framework-implementation`；本轮接手 HEAD 为 `4b446ca26`。
- 目标仍是 `cjmp-framework-review-and-fix-plan-20260928.md` 的完整 P0–P10、70 项发现，不能缩减为 parser 或通过某组测试。
- `D:/code/intellij/cangjie` 仅使用其官方参考源码和 Gradle queue JAR；不另建工作树。
- `.idea/workspace.xml`、原 HTML 删除、`.workbuddy/tmp` 取证产物和四个仅行尾状态的 PSI raw 生成文件保留，不纳入检查点。

## 本次接收的实现

宏 invocation 事实已接入表达式直接根协议、原生声明 scope、高层 ExprKind refinement、直接 comma parent，
PSI 在真实 AT/ATEXCL 叶承载事实，LightTree 返回同根冻结的 `ParsedCangJieLightFile`。
quote 按 token 连续扫描，保留任意括号深度的真实 `$()` 插值；普通宏 input/attrs 保持 token-only。
普通源码 IfAvailable 现在是宏调用，只有 EnableCustomAnno 的原生高层/statement 入口专有化。

其他会话继续完成了反斜杠 escape 的非消费诊断范围载体、默认未知起始字符 token 与 PSI/LT 收集、
独立 NL token 和 semantic builder 的 NL 跳过模式。marker collapse 使用物理 SDK 游标，
避免把逻辑跳过的尾随 NL 纳入 BLOCK。核心解析规则迁回 parser，原 `CfirCjmpParseRulesChecker`
被移除；`CfirCjmpFeatureSupportChecker` 保留 CFIR 的语言版本支持检查，生成诊断 API 已同步。

用户指出的 `Class.forName("com.intellij.psi.impl.source.codeStyle.CodeStyleManagerImpl")`
已在 `9df9bad20` 删除。当前显式声明 code-style/code-style-impl 依赖，直接构造实现；
POM model 使用公开 API，保留 SDK aspect 查询可空合同与事件更新顺序。

## 已核实的验证边界

- 最新 NL/宏事实定向：PSI 3 个 suite、57/57，0 失败/错误/跳过，XML UTC 2026-10-09T02:35:55–56。
  已复制到 `psi/build/verification/20261009-nl-reviewed-57/`。不能代表最新完整 PSI。
- CJMP parser 迁移：13/13 parser 测试通过；七个 fixture 的标准/PSI 两路径 14/14 通过，
  WithoutAliasExpansion 七项假设跳过。上述验证早于最新 NL 改动。
- `commonSpecificGenericFrozen`：标准/PSI 2 通过、WithoutAliasExpansion 1 跳过，XML UTC 2026-10-09T00:45:51–57。
- 两 raw 与 frontend 的前次结果分别为 LT 171/171、PSI 242/242、frontend 141/141，均 0 失败/错误/跳过；
  LT XML UTC 2026-10-08T18:07，另两模块 UTC 17:21–22，同样早于最新 NL 改动。
- 前次真实宏集成：657 项、44 失败，构建于 UTC 2026-10-08T17:31:11 终止 exit1。
  两条入口各 22 项，集中于 IfAvailable 与 APILevelChecker.testIndex。当前 analysis-tests 的 3 份 XML
  是后续定向运行，已经覆盖该失败批次，不能据此声称宏集成恢复。

本轮最初的统一集成验证为 queue `c2fdd3cf`、实际 Java PID 22440，started UTC 2026-10-09T03:08:23。
范围为完整 PSI、providers、两 raw、frontend、LL，以及 CFIR `org.cangnova.cangjie.cfir.analysis.tests.*Macro*`
和 `org.cangnova.cangjie.cfir.analysis.tests.*SourceModule*`；使用固定 1.1.3 SDK 与原 CFIR 内存 init script。
本轮 shell 启动器提前返回而 Java 仍运行，须以真实 Java/queue 状态核验。两个本轮重复等待者
28668/34812 已取消；不要依据残留 WAITING journal 当成活进程，也不要重新提交正在运行的 c2fdd3cf。
主 Gradle daemon 日志是 `C:/Users/lin17/.gradle/daemon/9.4.0/daemon-31252.out.log`。

## 下一阻断与尚未完成项

优先完成 IfAvailable 从真实源码宏到 EnableCustomAnno 重解析、完整调用 token 包络、实际槽位替换的闭环。
现 `MacroExpandPhase` 的 builtin-non-macro 分支仍剥离包络，专用 surface 可绕过 splice；
现有 frontend 构造测试不足以证明真实 source→raw→construction。必须保留两分支，不能在构造阶段选分支。

真实 parser facts 到 raw surface/替换槽位的贯通、token child forest、scoped 直接片段根、typed slices、
参数 MacroExpandParam 的实际 signature owner/有序 construction 参数列表、ATEXCL 原始声明重分类、
CASE_BLOCK 独立入口、LL 真实宏构造以及原始 70 项剩余目标继续开放。
整体完成度未重新计分，不能用本批测试数量提高 2026-10-08 审计的实现百分比。

## 本轮完整验证与 NL 框架修复

queue `c2fdd3cf` 已于 UTC 2026-10-09T03:19:10 终止 exit1（10m46s），不能继续轮询为运行中。
全部 XML 已分别保存到各模块 `build/verification/20261009-integration-c2fdd3cf/`。
PSI 316/32 失败、providers 55/0、LT raw 171/75、PSI raw 242/174、frontend 141/4、
LL 168/80、CFIR 宏及 source-module 669/502；最后一组另有 3 项跳过，其余均无跳过。
这是最新 NL 代码的真实回归，不能混用此前 57 项定向或 297 项旧完整 PSI 成功。

主因已由实际 SDK 253.29346.379 字节码及失败栈证实：semantic builder 在首 marker 之前
跳过前导 NL，导致完整文件起点不为零及 PSI 根长度违约；又把普通语法忽略的 NL 保持为
非 SDK whitespace 类型，造成名称、表达式、import 和诊断范围吸入尾随 NL。
PluginProblemReporter NPE 发生在 SDK 报告 LazyParseableElement.assertTextLengthIntact 时，
不能补注册服务掩盖原始长度错误。

修复方案门禁通过并已修改共享 builder：保留 SDK 原生首 marker 合同，journal 读取实际
marker.startOffset；仅在 skipNL=true 真正消费 NL 时调用标准
remapCurrentTokenAndRestoreOnRollback(WHITE_SPACE)，再 rawAdvanceLexer(1)。
SDK 自身负责 remap rollback 和所有 marker 的空白平衡；skipNL=false 的 opaque NL
仍保持原词元和诊断锚点。不逐个 trim 名称/表达式，不更改旧语义期望。
完整 root/trivia/rollback 回归已补齐；修复后实际结果见下节。

### NL 修复后的完整相关验证

首轮 PID 28512 于 UTC 03:57:46 终止 exit1（1m9s），仅因 Kotlin 要求明确多重继承的
super 类型，没有执行测试。现 remap 和 rawAdvance 均明确调用 `super<PsiBuilderAdapter>`。
最终 queue `262d15c5` / PID 39744 已于 UTC 2026-10-09T04:26:34 终止 exit1（27m16s）。

| 范围 | 实际结果 |
| --- | --- |
| 完整 PSI | 320/320，0 失败/错误/跳过 |
| providers | 55/55，0 失败/错误/跳过 |
| 完整 LT raw | 171/171，0 失败/错误/跳过 |
| 完整 PSI raw | 242/242，0 失败/错误/跳过 |
| frontend | 141/141，0 失败/错误/跳过 |
| low-level-api-cfir | 168/168，0 失败/错误/跳过 |
| CFIR 宏及 source-module | 669 条：622 通过、44 失败、3 跳过，0 error |

前六组共 1097/1097，通过；加宏切片合计 1719 通过。本轮所有 XML 已分别保存到各模块
`build/verification/20261009-nl-sdk-262d15c5/`；完整日志为
`.workbuddy/tmp/cjmp-nl-sdk-regression-20261009-2.log`。本轮没有重新执行全部 8968 条 CFIR。

NL 测试扩为 8 个方法，含 10 种完整输入的真实 PSI/LT 根、叶文本、package carrier 与
诊断检查，以及当前/未来 NL 前瞻、rollback/precede 的 remap 恢复。原有表达式范围、
名称、import 删除、stub、LL ContextCollector/file-structure 失败保持旧断言并全部通过。

在本节所述的 2026-10-09 快照中，仍有 44 项失败，为标准与 PSI 各 22 项：Ifavailable 直接 9 项、ExternalWeak 1 项、
LevelSyscapCheck 11 项、APILevelChecker.testIndex 1 项。所有失败的原始 XML 中已无
`assertTextLengthIntact` 或 full-file 起点异常。3 个跳过来自
`CfirAnalysisDiagnostics2WithoutAliasExpansionTestGenerated$Macro` 的宏重定义用例，未增加 skip。
下一步仍是完整 scoped roots / 实际 slot / token child 构造协议，不能把这 44 项改成跳过。

## 后续宏构造的新增官方证据

本轮官方源码与真实 cjc 1.1.3 探针汇总在
`.workbuddy/tmp/native-ifavailable-boundary-20261009/REPORT.md`，包含 parse/macroexp AST、
真实宏定义包、执行顺序和失败诊断。该目录继续保留。实现不得丢失以下边界：

- 原生 `Declaration(scope)` 的 ParseNodes 逐 root 做声明前瞻，否则调用高层 ParseExpr(ALL)，
  返回混合节点列表；UNKNOWN_SCOPE 不等同于强制声明解析。最后由实际 slot 判断形状。
- comma 环境只影响分隔符，表达式字段和 Expr vector 的单个 slot 仍要求恰好一个 Expr；
  statement/decl vector 可接收完整 slice 或删除空结果，表达式/参数空结果各有专用诊断。
- IfAvailable 的定义解析豁免只针对真实 MacroExpandExpr 且完整名字精确为 IfAvailable。
  `@pkg.IfAvailable`、`@!IfAvailable` 不能按同一 builtin expression 处理。
  另两个未声明判断只按完整名字豁免，因此 Decl/Param 仍可进入 ANNOTATION 状态；
  该状态不等于 Annotation AST。顶层/成员的形状错误发生在最终真实目标替换时。
- IfAvailable 刷新须保留完整调用包络，使用原 invocation scope 开启 EnableCustomAnno 后
  重解析，并替换原真实 AST slot。顶层/成员 decl slot 接收表达式时应报告形状诊断。
- opaque 子宏只参与 token 求值，没有源码 replaceLoc，也不独立执行最终 ParseNodes；
  macro 嵌套 parent 与源码 AST parent 是两个独立关系，不能替用。
- token 子宏通过 source-mode ParseDecl(scope) 取得实际消费终点；初次/reeval 有各自明确的
  scope 计算协议，不能复制父 source facts 或假造 CFIR carrier。
- attrs 不建立普通子宏 forest：解析到真实定义后先展开空参的 sourceLine/sourceFile/sourcePackage，
  再处理 escape，最后拒绝其余未转义 AT。input 与 newTokens 才有各自子宏发现流程。
- 生成新宏的再求值在父执行后发生；实测 Keep(Emit()) 是 Emit→Keep→Final，
  Drop(Emit()) 是 Emit→Drop，不会因为 Emit 生成了新宏而提前执行 Final。
- v1.1.3 有已复现的实现缺陷：IfAvailable child 再生成新宏时，REEVAL 刷新会丢失原调用头，
  最终报 parser 错误。这是待单独核对版本差异的官方缺陷，不能当成语言禁止规则或已通过正例。

仍须迁移两 raw 的真实 invocation facts、独立 slot 类型、scoped 直接 roots 与 typed slices，
再删除当前 dummy function/descendant-first 提取和人工 IfAvailableSurface 测试路径。

REPORT 第 9 节还核实 source ASTKind：顶层/成员的 AT、AT_EXCL 都是 MacroExpandDecl；
statement 首 AT 是 MacroExpandExpr，AT_EXCL 是 MacroExpandDecl；表达式原子只接受 AT
为 MacroExpandExpr，AT_EXCL 是 InvalidExpr；参数宏两种标记都是 MacroExpandParam。
statement 的 `@M func f(){}` 仍是 MacroExpandExpr，不能按无括号输入或预期输出改写原始种类。

## 检查点及解析缓存版本

- `3edbe2a59` 保存接手时交叠的 84 个 CJMP 文件、测试与说明，保留全部已知验证边界。
- `f3dae83c2` 保存本轮 NL 共享框架修复、8 项 SDK 契约测试及完整相关回归证据。

缓存版本审查确认本批解析结构变化还须递增三个既有标识：SOURCE_STUB_VERSION 219→220，
MacroFragmentParser.VERSION 19→20，MacroPayloadTokenizer.VERSION 1→2。前者通过
CjFileElementType 接入 SDK stub 版本机制；后两者已进入真实 construction 的文件缓存键和
registry 模块签名。没有新增缓存读取实现，也不能声称已实测持久化旧缓存读取被拒绝。
本次未变更 forest/splice/builtin 表，所以 construction 算法与 builtin registry 版本保持原值。

这三个常量已修改；定向验证 queue `a3c1c22b` / PID 11476 于 UTC2026-10-09T04:46:32
终止 exit0（11m30s）。
目标为 WholeTreeStubRoundTripTest 两项，以及 frontend 的真实 context/cache-key 方法和
两个 cache 结构方法；日志 `.workbuddy/tmp/cjmp-parser-cache-versions-20261009.log`。
WholeTreeStubRoundTripTest 2/2、frontend 三方法 3/3，共5/5，0失败/错误/跳过。对应 XML
已保存到两个模块的 `build/verification/20261009-parser-cache-versions-a3c1c22b/`。
此验证没有重跑完整 CFIR；完整相关回归与剩余44项仍以上述262d15c5快照为准。
本线程所有验证进程均已结束。后续从真实 occurrence/raw 传播与 scoped construction 继续，
不要重启已终止的28512、39744或11476。

## 2026-10-10 续接复核

- 当前工作树的完整宏切片已重新验证：队列 `25076203` / id `25076203` 执行 `:cfir:analysis-tests:test`，筛选标准与 PSI 两套 `CfirAnalysisMacro*TestGenerated` 及 `CfirMacroAnnotationSourceModuleTest`，exit 0。对应 XML 为 657 tests、0 failures、0 errors、0 skipped，时间 2026-10-10 08:22（本地）。这覆盖并取代上面 2026-10-09 的 44 项宏失败快照；这些旧失败目前不再复现。
- 真实源码 IfAvailable 的 expression 与 declaration-slot PSI/LightTree 闭环测试在队列 `08559bd7` exit 0，3/3；`CfirCjmpEnumConstructorPlanTest.commonSnapshotKeepsBothEnumAndMixedFunctionOccupationKinds` 在 `f404708d` exit 0。整组 `:cfir:resolve:test --tests '*CfirCjmp*Test'` 在 `fcdb0aae` exit 0；LL 的 `:analysis:low-level-api-cfir:test --tests '*CfirCjmp*'` 在 `4e1e5d06` exit 0。
- `CfirCjmpFamilySnapshot` 现已复制事实并脱离 live storage；这只验证了快照边界。`buildSnapshot(..., storage)`、`CfirCjmpMappingStorage` 的 session 前向表与旧写 API、`CfirCjmpMatchingProcessor.beforePhase()` 的 `clear()`、runner/direct matcher 的可变结果翻译仍在，因此 P1 和完整 P0–P10 仍未完成。
- 下一实施主线回到完整 P1 集成：按主计划先确认 Kotlin matcher/声明结果属性的所有权，再以单一不可变声明事实替换重复 session 前向状态，并联动 LL 单目标发布、common checker 有序事件及 P3/C4 后期 scope 缓存。不能用本次宏与 CJMP 定向绿测验收整个框架。

### 2026-10-10 LL 并发完备协议续接

- `LLCfirCjmpFamilyAggregate` 现通过按包 key 的 completion cell 让并发 `ensureFamilyComplete` 调用等待同一个最终快照；异常和取消移除未完成 cell，供后续请求重试。等待方定期检查 IDE 取消；等待方线程中断只中止等待者；同线程递归在快照发布前失败，不返回部分结果。
- 新增 `CfirCjmpFamilyCompletionCacheTest` 的并发单算、失败重试、取消重试、中断等待者和重入拒绝五项测试。队列 `fa3928bf` 的定向类测试、队列 `c11fc911` 的 `*CfirCjmp*` 测试均 exit 0。
- 复核结论：这是可保留的 LL 并发协调局部修复，但 P1 未闭环。shadow/common 对位读面仍可绕过 completion cell 直接读 `CfirCjmpMappingStorage`；shadow Boolean 逐项写入，家族失败或取消不回滚已推进的阶段、storage 和属性副作用。因此不能称为不可变发布完成。
- 下一批回到原九步集成迁移：冻结声明事实成为唯一消费源并由目标 root 单次发布，再统一迁移 family、checker、actualized scope/default consumers 与旧写表删除；不能切断绕行读取时，不继续扩大完成缓存范围。

### 2026-10-10 callable 组求值输入边界续接

- `matchCallableGroupWithPlan` 先读取已缓存的组结果；未命中时，候选/provider 查询与签名准备在调用组结果缓存前完成。`CfirCjmpMappingStorage.callableGroupMatchResult` 现在接收按规范声明顺序冻结的计划并校验顺序，缓存计算闭包只执行 `CjmpMatchEvaluator.evaluate`，不再在 `ConcurrentHashMap.computeIfAbsent` 中查 provider 或推进阶段。
- callable/property root facts 直接由完整 aggregate 投影，移除先对单个 root 重跑 evaluator、再丢弃求值输出的重复工作。属性仍只保存 root 输入，组级占用/defaults/final implementation 没有写回 root。
- 定向队列 `b0d3c46f` exit 0：CFIR resolve CJMP 测试 52/52、LL CJMP 测试 45/45，均无失败、错误、跳过。默认读穿队列 `ebacfb66` exit 0：PSI/标准入口 4 项通过；WithoutAliasExpansion 的 2 项按测试定义跳过。`resolution.common` 测试任务为 UP-TO-DATE，不计作本轮执行结果。
- 这只缩小了组缓存临界区并消除了 root 重求值。组计划仍由当前 matcher 调用枚举/准备，旧 `CfirCjmpMappingStorage` 仍拥有派生组缓存与可变配对表；LL 锁外依赖准备、全路径 root facts、family/checker/scope 消费迁移与旧表删除均未完成，P1 和 P0–P10 继续开放。

### 2026-10-10 CJMP 定向复验

- 队列 `9ccd87b9` 的整组复验有 1 项失败：`CfirCjmpCallerFirstDiagnosticsTest.uncalledImplementationCallerFirst` 未观察到未调用声明到达 `CJMP_MATCHING`。随后队列 `8b29cfd3` 单独重跑该方法通过，队列 `64ff5534` 再跑完整组，CFIR resolve 52/52、LL 45/45 全部通过。
- 该失败目前未能复现，原因尚未确定；因此记录为一次未解释的间歇失败，不宣称其根因已修复。当前定向组验证通过，但这不构成 P1 架构迁移验收。

### 2026-10-10 LL callable 组锁外计划准备

- specific callable 组在 LL `doResolveWithoutLock` 中先推进签名依赖、按每个 specific root 收集 common 候选并冻结计划；目标锁内只消费准备好的规范组输入、求值并发布当前目标投影。已有组快照时跳过重复候选准备。每个组成员使用自身外围 owner 的类型参数映射。
- 此变更仅覆盖 function/enum-constructor callable 组；其它声明路径仍在 runner 中查询候选或准备计划，旧 storage 的可变配对表、family/checker/scope 消费迁移与删除均未完成。
- 缓存命中时也传递已定序的组身份快照，runner 不在目标写锁内重新访问 provider 查找 callable 组。
- 队列 `36d47603` exit 0：CFIR resolve CJMP 52/52、LL CJMP 45/45；`git diff --check` 对本批代码文件通过。此前一次未解释的 LL 失败仍未定位，不把本次绿测记为根因修复。
- 队列 `d96d0f66` 对标准与 PSI 的 `testCjmpDefaultReadThrough`、`testCjmpDefaultReadThroughBeforeImplicitTypes` 各两项复验通过（4/4）；`WithoutAliasExpansion` 本次未选入。

### 2026-10-10 非 callable 声明组锁外候选准备

- LL 在目标写锁外为 class-like、extend、property、pattern、变量和 constructor 声明准备 specific roots、逐 root common 候选、所需签名阶段及外围类型参数映射；目标锁内 runner 消费该快照，不再重复 provider 查询或推进 common/specific 签名。Callable 组仍使用既有独立 prepared-plan 路径。
- duplicate specific extend 先按模块与包筛选潜在兄弟，只解析需要比较 key 的候选；命中重复项时在查 common 候选前短路。eager 入口仍走原同步查询路径。
- 队列 `b0e87523` exit 0：CFIR resolve CJMP 52/52，LL CJMP 45/45，0 失败、错误或跳过。
- 这只完成 LL 匹配输入准备的一个边界。`CfirCjmpMappingStorage` 仍保存可变配对表和派生结果；声明级不可变事实的单 root 发布、family/checker/default/actualized-scope 消费迁移及旧写表删除仍未完成，P1 与 P0–P10 继续开放。

### 2026-10-10 extend root facts 接入共享求值计划

- `CjmpBindingRule.ExtendCounterpart` 将同 key common extend 的归属绑定纳入共享纯求值器；`CfirCjmpResolver.buildExtendMatchPlan` 保留候选顺序、各 owner 的类型参数映射及全部有效 counterpart。未解析的 extend 仍按官方 key 分组规则排除。
- `matchExtend` 改为一次构造并求值完整计划，然后只发布当前 specific extend 的冻结 root facts。CFIR 存储适配继续保留官方规则：只要有一个 owner 匹配，就合并所有匹配 owner；无匹配时才记录失配或 unmatched。LL 集成断言验证两个 common owner 与 root 计划的成功候选顺序一致。
- 队列 `resolution.common` evaluator 测试 25/25、CFIR resolve CJMP 测试 52/52、LL CJMP 测试 45/45，均 0 failures/errors/skipped；本批修改文件 `git diff --check` 通过。
- 这只迁移 extend 输入计划和 root 发布的一条路径，并未形成单一事实源闭环：family snapshot、checker、scope/default 消费仍读 storage；class-like merge seeds、实例字段和其它 direct matcher 路径仍需迁入；`CfirCjmpMappingStorage` 的 mutable tables 和旧写 API 尚未删除，P1 与 P0–P10 继续开放。
