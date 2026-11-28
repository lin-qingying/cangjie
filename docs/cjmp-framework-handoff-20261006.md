# CJMP 续接状态：2026-10-06（更新至2026-10-08）

本文件是最新入口，补充 `cjmp-framework-handoff-20261003.md`（该文件也包含10月4日至6日的历史记录）。
完整目标仍为主计划 P0–P10 / A1–J1；未完成，当前实现完成度以最新审计重算结果为准。
先读末尾2026-10-08续接记录；前面的失败/未验收描述为历史状态，以后续明确验证结果为准。

## 位置与保留边界

- 实施工作树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。
- 分支：`codex/cjmp-framework-implementation`；本轮开始 HEAD：`57bd317ea`。
- 跨会话实现检查点已保存为 `5c2d937c3`（140文件）；它明确记录完整CFIR尚未验收。
- SDK统一选择修复已独立提交为 `61446bc9d`（18文件）；前端131项与真实宏26项通过。
- 原仓 `D:/code/intellij/cangjie` 只读；使用其 queue JAR 和 external 参考源码。
- 不重新创建工作树、恢复、reset/checkout/stash/clean。
- 根文件 `测试结果 -Run_Cfir_Test_All.html` 的已有删除不属于本轮，不能暂存或恢复。
- `.workbuddy/tmp` 的旧恢复文件、CHIR/CJO/DLL/日志不整体暂存。
- 官方参考仍用 `git show v1.1.3:<path>`，不能把 external 当前 v1.0.0 checkout 当1.1.3。

## 接续到的已有进展

其他会话已提交 `1e7c23520`（混合 callable 匹配和 scope 类型选择）、`fcfeede71`（lazy准备边界）、
`57bd317ea`（返回类型语法规则迁入共享 parser）。因此1002交接的checker→resolve导入错误早已修复，不要重做。
随后未提交工作包含 pattern计划/真实binding与LL事务、初始化纯算法、parser文件角色/来源事实、
宏重解析语法通道与事务、构造器签名/嵌套名义类型处理、SDK协程API和整树stub测试。
详细实现及未完成契约见1003交接和三个20261005 parsing审计文档。

## 本轮新增修复

### 1. 名义成员FQName与整树stub

恢复13926验证前先查真实状态：SDK daemon日志明确客户端断开并取消构建，旧queue RUNNING/PID已失效。
重新运行99619首次进入真实整树测试，暴露原始成员stub中的FQName为null。

根因位于共享 `CjNamedDeclarationUtil.getParentFqName`：解包class body后没有名义类型owner分支。
按Kotlin同层 `KtNamedDeclarationUtil.java:61`，在extend专用名字域分支之后递归真实
`CjTypeStatement/CjNamedFunction` owner。所有使用safeFqNameForLazyResolve的producer共同受修复；
getter继续读持久化字段，没有按祖先猜名或强制加载AST。源码stub版本212→213，使旧空名字缓存失效。

`WholeTreeStubRoundTripTest` 先检查producer，再做生产whole-tree serialize/deserialize；
detached与fresh使用独立树，fresh为真实物理文件正常loader attach，保留逐getter无AST断言。
新增 `NamedDeclarationFqNameTest` 五项覆盖四类nominal、非法嵌套恢复、local、foreign、extend。
本批源码框架gate已通过；不代表whole ERROR/dangling或所有stub生产者都独立验收。

### 2. PSI复制保留真实解析事实

完整LL的五个ContextCollector失败发生在 `file.copy()` 后，原文件两种context查询通过。
SDK `PsiFileImpl.clone()` 只复制copyable data并克隆已解析树，不重新调用parser。

改为复制不可变输入/结果：`cjmpParsingContext`、`parserSourceKind`、`parsedFileFacts`、
`parserLanguageModuleName`、`parsedLanguageModuleName`；新增nullable copyable delegate。
完整parser开始清本轮旧结果、结束重新发布。不复制可变FileRoleState，不从原文件回读，
不取消raw的事实断言，不制造NONE。不能在clearCaches里清快照，SDK clone末尾会调用它。

`ParsingFileCopyTest` 三项覆盖24组角色/源种类/模块输入结果、两代副本及相互隔离。
重新解析通过真实LightVirtualFile内容更新与AbstractFileViewProvider.onContentReload，
验证新root、新root.psi、新snapshot、完整正文诊断及文本，不手工装AST。
SDK非physical文件的长度一致性检查会同步创建根，因此不要求重载返回后树仍为空。

限制：这没有完成独立stub文件的ParsingFileFacts持久化，也不代表IDE增量merge/所有编辑失效已闭合。
BlockSupportImpl临时完整解析副本的输入传播仍需单独审计。

### 3. 参数尾随逗号

完整CFIR发现 `foreignFunctionCFuncLegalityPlaceholder` 在生产frontend入口语法失败，导致语义诊断全缺失。
共享 `parseParameterSeparator` 错误拒绝末尾逗号。固定官方 `ParseDecl.cpp:2495`、
`ParserUtils.cpp:303–314`、`ParseType.cpp:191` 均允许该形式。
已移除该错误报告，保留实际结束边界，不修改fixture和四处语义期望。
新增 `ParameterListTrailingCommaTest`：6合法尾逗号、6空列表、15非法逗号场景，比较PSI/light诊断和真实参数节点。

## 实际验证

| 范围 | 最新有效结果 | 证据 |
|---|---|---|
| 新增PSI四类 | 12/12，0失败/跳过 | 50712 exit0，21s；XML 2026-10-06T02:42:46–47Z |
| 完整LL | 168/168，0失败/跳过 | 16699中的LL；XML 02:45:28–02:46:06Z |
| CLI两段式 + 宏执行 | 26+27=53/53，0失败/跳过 | 16699中的frontend；XML 02:50:27–31Z |
| 参数宏raw | 2/2，0失败/跳过 | 21377 exit0，XML 01:39:54Z |
| ContextCollector定向 | 6/6 | 3719中的LL，XML 02:20:59Z |
| 原interop三入口 | 各1/1 | 3719中的CFIR，XML 02:21:45–49Z |
| 完整CFIR | **尚未取得完整通过结果** | 下述系统提交内存失败，不能用部分XML代替 |

有些组合退出1源于新增测试harness问题而非生产：72884为空List类型推断，73804为JUnit3三字符串
assertEquals重载，3719为独立builder根缺PSI owner，35972为不成立的非physical空树过渡假设。
均已修正并由50712验证；不削弱生产语义或修改原interop数据。

## CFIR全量的资源阻碍与作业

1. 91256 / queue187cd859：exit1，7m59s。LL五个真实失败与interop一个真实失败已如上修复；
   随后PID42292在C2 CompilerThread0原生malloc失败，hs_err记录系统物理可用605MB。
2. 16699：exit1，5m19s。最新完整LL和CLI通过；CFIR PID54484在G1提交425721856字节时失败，
   Windows errno1455，见 `hs_err_pid54484.log`。本轮仅以JAVA_TOOL_OPTIONS选C1，仍不足以缓解系统提交内存耗尽。
3. 一次性init script：`.workbuddy/tmp/cjmp-cfir-memory-20261006.init.gradle`，只针对该工作树的
   `:cfir:analysis-tests:test` 设置768m、每20个测试类更换JVM、C1。没有筛选用例或改仓库构建配置。
   第一次脚本在included build找不到目标project，已限定准确root目录；该setup失败不是测试结果。
   后续handle11466确实执行全量；PID8480仍发生无法提交1MB的errno1455，之后Gradle继续其他fork。
   **即使后续fork执行完，也不能把有崩溃的整轮当全量通过。**

最后一次内存实查：OneDrive.Sync.Service PID37248私有提交约34.72GiB，系统可用提交内存约365MiB。
已向用户异步请求释放内存；未终止任何无关应用。下一次完整验证前核对实际资源，不盲目重启。
后续已核实11466停止：handle不存在、queue61bbf639的owner PID11164不存在，本工作树无Java进程。
daemon-55376.out.log记录连接断开/daemon关闭，最后输出6537 tests、35 failed、398 skipped；
没有可用的最终queue exitCode，也没有这轮完整XML（264份XML仍是10:50上一轮残留）。
不能继续轮询11466、不能把这35失败全归为内存；其中一组真实宏测试失败正在从日志/二进制结果取证。
最后内存仍紧张（物理可用约265MiB、可用提交约486MiB），未收到用户已释放内存的明确回复。

已从本轮results-generic.bin取得宏失败的具体证据，不能把它们全归为OOM：
- 默认Macro SDK仍指向不存在的`C:/Users/lin17/.cangjie/sdks/cangjie-1.0.5`；执行器文件不存在，
  对应SameProject/MixMacro的MACRO_EXECUTOR_UNAVAILABLE，以及std.deriving可执行包缺失。
- `Globals/allkinds.cj` 的 `@APILevel[12] S(@APILevel[12] let c!: Int32 = 9) {}`
  主构造参数字段没有进入宿主struct，副构造器的`c = 8`新增UNRESOLVED_REFERENCE。
  这是注解/宏包装后真实primary身份与字段生成链的代码缺口，不能恢复任意后代按名兜底来修。
  后续需查明分类后实际声明身份并在共享owner处补齐PSI/light路径。

## 提交范围与后续顺序

### SDK选择续接（本轮新增）

已删除个人绝对路径DEFAULT_MACRO_SDK_HOME。macroSdkHome现在只是可清除的显式输入；
MacroSdkSelector在调用边界选择配置/CANGJIE_HOME/cjc.home/PATH，保留Selected/Absent/Invalid。
已指定的无效来源不改选其他安装，PATH只在home未指定时使用真实SDK bin/cjc，不扫描用户目录最新版本。
选择快照由artifact准备结果传给compiler context及construction service，不永久缓存于可复用配置。
locator/resolver保持纯输入，无SDK也可处理显式artifact/searchroots。默认compiler不再二次发现或按对象身份分派。
默认executor进入生产service；Windows及非Windows均接收选中SDK的环境。cjc子进程也覆盖CANGJIE_HOME和工具/运行库PATH。
SDK选择身份进入宏cache key；SDK根与组件是否齐全分开验证。

验证60646已exit0（37s）：frontend七类131/131通过，包括SDK9、locator8、resolver9、orchestration31、
architecture21、CLI26、macro execution27。旧guard三个失败已查明：两个扫描测试mock/私有索引helper，
另一个要求过时IfAvailableSurface。守卫现覆盖src/testFixtures语义路径、保留shared test facade，
两raw要求真实IfAvailable IR并禁止重新制造surface；官方展开前/后的阶段差异仍未宣称闭合。

真实宏集成70262已exit0（2m10s）：PSI/LT各8个same-project、4个MixMacro、1个Derive，总26/26，
XML 2026-10-06T05:17:51–05:18:36Z。本轮命令显式设置CANGJIE_HOME为已验证的1.1.3 SDK，未改系统环境。
宿主默认环境CANGJIE_HOME自身仍指旧缺失路径，下一次真实宏验证也须显式提供有效配置。
该组SDK源码最终只读gate通过后已提交61446bc9d。60646/70262均已终止，没有本组后台作业需要轮询。

主构造字段缺口仍未实现，完整设计见 `cjmp-primary-constructor-completion-design-20261006.md`。
关键限制：LL目前不运行compiler macro construction，只记录expression surfaces；唯一field materializer
不能只放frontend，否则普通LL字段消失。必须连同LL cache/finder前共享completion及全部raw API契约接入。

只读审查确认当前first-party变更都属于本计划及相应回归，没有发现需要排除的无关源码。
当前候选manifest为139个first-party文件，另加本文。不能只提交最新copyable几行：
它依赖未跟踪ParsingFileFacts/CjmpParsingContext及parser/raw/macro整套消费者。
SDK协程两个文件必须与整树stub测试同批或更早。PatternStateKeepers依赖公共snapshot、
argument mutable-state、tree-generator定义和8个实际变化的generated receiver API；foreign变量修复依赖真实header owner。
纯初始化求值器仍未接生产，可作为检查点保存，但不得声称实例成员合并已完成。

下一步：

1. 11466已停止，不再轮询；SDK相关真实宏26项已通过。继续修主构造字段等剩余失败，完整CFIR需重新执行并核对XML总账。
2. 补当前实现组的shared/resolve/parser/raw测试，尤其此前广域测试失败不能直接标为基线。
3. 保存依赖完整的实现检查点和验证边界，排除已有HTML删除与临时二进制输出；检查点不代表完整CFIR或全计划已验收。
4. 继续共享声明生命周期：真实scope、raw occurrence与handler有效attrs、实际完成/父成员检查顺序。
   10-06新取证已补入设计文档：projection时点因handler不同；重复modifier仅首次参与投影；
   不是所有声明都调用CheckCJMPDecl。不能把现有CFIR checker的错误顺序原样搬入parser。
5. whole ERROR透明容器、generated primary身份、lazy scope保留/失效仍是五项PARSE迁移前置；
   原计划的声明级不可变发布、生产merge seed、default/doNotExport、P4/P6/P7/P8/P9/P10仍未完成。

## 验证命令

### 2026-10-06 16:13 参数事实基础步续接

已在d4f1b1e40之上完成参数字段事实基础步和其阻塞的共享parser前缀修复，详见
`cjmp-primary-constructor-completion-design-20261006.md`的最新两节。
源码参数的字段isVar与完整19项status独立捕获；两raw只对真实primary LET/VAR发布成员参数事实。
共享parser在所有声明参数中消费完整modifier及LET/VAR，按官方v1.1.3保留诊断与恢复结构；
token级ERROR不会丢掉PSI/LightTree/stub的关键词事实，源码stub版本214。

验证：tree2/2（95914）、两raw字段12/12（16360）、PSI六类34/34（11438），全为exit0、无跳过；
局部架构审查通过。先前57478失败的null-source fixture与const/非法LET/VAR解析问题均已关闭。
这48项不是完整CFIR结果，旧35失败/398跳过的残缺运行仍不能用作验收。

下一实施点仍是唯一字段materializer及LL construction，尚未修复allkinds。
新增明确前置：annotation metadata registry目前按session整体freeze，直接按LL文件调用construction
会导致第二文件/重建写冻结registry。应按文件/构建代次设事务并原子发布metadata，不能解冻或覆盖旧registry。
materializer应置providers现有construction包，避免raw-common→providers的依赖环；
self type使用tree `CfirClassLikeSymbol.constructType`保留struct身份。

### 2026-10-06 16:45 LL回归与参数stub同构修复

参数基础步已提交为`68f9504e5`。随后完整LL61614（exit1，4m47s）暴露4项相同的
binary/source stub形状错误：20383个binary节点对21384个AST stub节点；第5项是mixedDuplicateKind绑定断言。
根因明确的4项来自普通参数新造空MODIFIER_LIST，而CJO普通参数producer没有此节点。

已按Kotlin统一参数规则修复：所有参数无实际modifier时drop空容器，删除CJO primary参数专用的
includeParameterModifierList/includeModifiers及强制造空节点分支。没有恢复primary-only前缀解析。
源码stub版本215，binary/classfile/builtin版本自动随之失效。新增空modifier形状断言，保留原有测试。

最新组合75419 exit0（9m43s）：
- 完整LL168/168，XML08:44:32–08:45:06Z；
- 标准库真实binary→反编译源码整树形状1/1，XML08:42:44Z；
- PSI六类34/34，XML08:43:03–05Z；
- 两raw字段12/12，XML08:43:29Z。

合计215/215，无失败/跳过；先前tree2/2仍是该部分最新有效结果。
mixedDuplicateKind在本轮完整LL中通过，没有改fixture或matcher，也未独立证明此前单项失败的原因。
不能将它归咎于MacroSdkSelector：LL默认库来自仓内cjo-sdk，两个queue命令均未指定stdlib覆盖。
当前完整CFIR仍未验收，allkinds字段缺口仍未修复。

下一步设计已保存为`cjmp-construction-metadata-lifecycle-design-20261006.md`，**全部待实现**：
每文件/构建代次私有metadata事务、fragment显式共享、seal为实际declaration持有的不可变表、
session仅weak-key/weak-value已发布索引，成功后才交给provider/cache/finder。
参数annotation的resolution owner不等于slot owner，不可通过containingDeclarationSymbol反推。
LL接入时还必须处理MacroExpansionRegistry覆盖和RawMacroSurfaceUsageStore强引用生命周期。
不要仅改session.freeze或先恢复旧字段发射点来绕过这些发布边界。

### 2026-10-06 18:58 metadata 生命周期阶段

在7609a1bbc之上已实现私有文件/代次metadata事务与canonical session弱索引，生产与test fixture均迁移。
实际API与边界见`cjmp-construction-metadata-lifecycle-design-20261006.md`开头。
不再从session取得writer；PreMacroCfirFile同时持有file/surfaces/事务，fragment/replace slot显式传递同一writer。
stable carrier provenance与当前slot分开；支持声明替换、注解迁移、列表删除/重排/合并及历史index复用。
degraded替换的attributes复制、参数原列表快照、@When裁剪及真实可达owner集合均已接通。
内部不变量与取消不再转成可degraded结果，executor在异常时关闭；失败私有事务不向session发布。

验证：58721组合已结束（exit1，9m41s），raw22/22、完整LL168/168通过；
3项失败是新增测试的异常类型断言及未传factory Context，保留全部行为断言修正设置。
12448已结束（exit0，1m5s），providers55/55、frontend141/141，合计386/386，无跳过。
最终静态gate通过。15203已exit0（6m22s），迁移过的CfirMacroAnnotationSourceModuleTest 5/5、
CfirDeclarationOwnerTest 6/6通过。本批总397/397、0失败/跳过，已无本轮活动构建。
这11项不是全量CFIR，本轮完整CFIR尚未验收。

**剩余边界**：LL当前封闭/发布的是raw metadata，仍未运行真实declaration/parameter macro construction；
共享primary field materializer及allkinds字段缺口未解决。MacroExpansionRegistry逐文件发布、
RawMacroSurfaceUsageStore强引用生命周期仍需一并接入，不能将本批弱索引当作完整LL图回收证明。
下一步继续主构造completion设计，不能恢复原始语法的按名/后代搜索。

另有待核实的只读发现：`CfirConditionalCompilationEvaluator.combine`目前只用second决定AND/OR结果，
first只参与Invalid检查；应补官方证据与真假矩阵测试后修复，不作为本轮已完成内容。

### 构建入口（沿用）

### 2026-10-06 21:29 共享主构造字段阶段

metadata阶段已保存为`9a1ed9d89`。随后独立修复When AND/OR丢左值并提交`4f1635f7e`，12项真值/错误传播测试通过。
共享字段helper现位于providers construction包，两raw旧发射/语法回查与LT两个FUNC提升primary分支已删除。
batch在宏splice后生成字段，LL当前raw在cache/finder前同helper；字段状态只表示成员已生成，不代表LL宏完成。
默认构造与真实primary用已有ImplicitConstructor来源身份区分，未改变原默认构造阶段。

94807 exit0（7m）：raw22/22、allkinds PSI/LT2/2，字段定向24/24、0跳过；静态gate通过。
`allkinds` 原c字段缺失已关闭，没有改测试数据。helper用最终owner/参数生成符号、泛型self type、双向关联和派生metadata；
工厂会拒绝字段未生成、生成后primary/参数/声明图改变的recordable产品。
完整LL/frontend/raw/CFIR广域回归尚需继续，不能用这24项代替全计划验收。

下一步：生成fragment的真实primary/参数scope与CFIR owner尚未接入，LL真实宏服务及registry生命周期仍待实现。
默认constructor是另一未闭合阶段：native在CJMP merge后决定，COMMON/interop过滤不能用raw局部判断代替；
本仓两raw与TYPES仍在补implicit-primary，不要仅改isPrimary布尔。详细证据在primary completion设计最新章节。

### 构建参数

构建运行时必须JDK21：JDK17已被实际provisioning插件最低版本拒绝。沿用现有queue入口。

```powershell
& 'C:/Users/lin17/.jdks/corretto-21.0.10/bin/java.exe' -jar 'D:/code/intellij/cangjie/gradle-queue-cli/build/libs/gradle-queue-cli.jar' --project-dir 'C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie' <TASKS> --no-build-cache --continue --console=plain --max-workers=1 '-Dorg.gradle.java.home=C:/Users/lin17/.jdks/corretto-21.0.10' '-Dorg.gradle.jvmargs=-Xms32m -Xmx1g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8' '-Pkotlin.compiler.execution.strategy=in-process'
```

完整CFIR任务为 `:cfir:analysis-tests:test`。如沿用一次性内存配置，在命令中添加：

```text
--init-script C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie/.workbuddy/tmp/cjmp-cfir-memory-20261006.init.gradle
```

完整结果再用 `tools/verification/cfir-test-result-ledger.ps1` 建快照，对比
`cfir/analysis-tests/build/ffi-annotation-verification/20261003-pattern-header-final-full`。
不要追加--rerun-tasks，不以FROM-CACHE当新验证。最新完整快照见下节。

### 2026-10-06：共享字段广域验证完成，继续 raw 回归

`29178cd7f` 后的组合任务 **6964 已结束，exit1，32m58s**，没有该任务的活动构建需要轮询。
本节替代上文所有“完整CFIR尚未验收”的历史状态；不表示整个P0–P10计划完成。

| 模块 | 执行记录 | 失败 | 跳过 |
|---|---:|---:|---:|
| light-tree2cfir | 289 | 12 | 0 |
| psi2cfir | 686 | 180 | 0 |
| providers | 55 | 0 | 0 |
| frontend | 141 | 0 | 0 |
| 完整LL | 168 | 0 | 0 |
| 完整CFIR | 8968 | 0 | 397 |

完整CFIR实际通过8571项，XML1266份，时间2026-10-06T14:04:41.736Z–14:32:49.635Z。
可重算快照：`cfir/analysis-tests/build/ffi-annotation-verification/20261006-primary-fields-full`。
与`20261003-pattern-header-final-full`比较：0新增、0删除、0回归、0变化失败，case身份和状态一致。
397是测试跳过，不能计入通过或框架实现完成；组合exit1来自两个raw模块。

raw失败已定位五类：coverage-matrix漏列已有featuresDirective.cj；FILE/features独立注解行尾空格；
for guard旧golden漏表达式；interface旧golden漏隐式abstract；quote旧golden漏插值引用。
生成类已有featuresDirective，不是生成器遗漏。后三类按官方v1.1.3修订golden，未改.cj；
renderer仅显式控制独立行尾分隔符，不全局trim。上述改动尚待新一轮raw验证。

同时查明JUnit3RunnerWithInners递归执行嵌套类与Gradle逐类发现重复：
LT289条只有141个唯一class#method；PSI686条只有242个唯一class#method。
正按Kotlin Gradle/JPS分派核定运行器修复，必须保留全部唯一用例以及outer/inner/method过滤能力。
不能通过排除嵌套类减少重复，也不能直接以记录数减少判断覆盖下降。

后续宏fragment接口与官方证据已整理到`cjmp-macro-fragment-owner-context-20261006.md`，尚未实施。
真实owner/primary参数文法、声明零/多产物、参数多产物及空产物诊断必须贯通parser/raw/splicer；
LL真实宏构造、registry生命周期和CJMP merge后默认构造器仍为未完成事项。

### 2026-10-07：raw 全量通过与运行器修复

handle56497 / queue f291897b 已完成：exit0，8m9s。新鲜XML时间为本地00:04:43–00:04:56：
LT141/141、PSI242/242，全部通过、0跳过，每个class#method只有一条记录。
两个模块的唯一用例数分别与修复前141/242一致；生成器及测试源未删除用例。
原始XML已分别保存在各模块`build/verification/20261007-runner-full/`，后续筛选验证须与其身份集合比较。

raw renderer/coverage/四份golden已提交为`27f6e47a5`。FILE/features独立注解行不输出尾随空格；
官方语义证据支持for patternGuard、接口隐式abstract、quote插值引用，未改.cj源或features期望。

共享JUnit3运行器已按Kotlin拆成public delegate/ForGradle/ForJPS三类；Gradle仅运行自身TestSuite，
JPS按requestedRunners收集。移除无实际consumer的LegacyNameBasedRunner；保留TestCase真实生命周期、
Filterable/Sortable委托与零事件空容器。Gradle9.4由共享convention同时扩展CLI/build-script筛选集合，
修正多余反斜杠、混合wildcard阻止精确外类扩展、两渠道相同外类时仅扩展一边三个问题。
不引入旧版Gradle反射字段兼容。最终静态gate通过，运行器contract与筛选smoke已完成：

| 验证 | 有效结果 | 运行 |
|---|---|---|
| 精确外类（LT）/精确内类（PSI） | 78/78、29/29 | 7118 exit0，8m52s |
| 外类加wildcard（LT）/精确方法（PSI） | 94/94、1/1 | 60499的raw任务通过 |
| CLI与build-script同外类/仅build-script筛选 | 6/6、4/4 | 36314 exit0，16s |
| 公共runner九项契约 | 9/9，0失败/跳过 | 49562 exit0，25s；XML00:48:13.794Z |

六组筛选均逐class#method与完整raw快照对比：0遗漏、0额外、0重复、0失败/跳过。
对照报告：`tests/test-infrastructure/build/verification/20261007-runner-filter-results.json`。
60499整体exit1仅因初版新增排序测试误以为JUnit3 TestSuite支持自定义倒序；
实际JUnit4.13.2源代码中JUnit38仅向Sortable Test转发，TestSuite不实现Sortable。
现测试用独立JUnit38(TestSuite)差分核对执行顺序和完整生命周期，生产代码未自造排序；49562九项全部通过。

56497、7118、60499、36314、49562均已终止，无需再轮询。当前raw的`test-results/test`已经是最后筛选结果，
完整raw结果必须读取各模块保存的`build/verification/20261007-runner-full/`。
整个P0–P10目标继续打开。下一实施入口为macro fragment设计文档§6–7，已补普通parser的primary名字/作用域缺口、
真实function target、参数右到左splice、专用空参数/named顺序诊断以及外层删除所吸收子slot的生命周期。

### 2026-10-07 15:10：声明参数文法与成员语境基础步

已在普通源码入口接通`CjParameterGrammar`四种角色：REGULAR_TYPED、CLASS_PRIMARY、STRUCT_PRIMARY、ACCESSOR_NAME_ONLY。
完整annotation/modifier/let-var前缀仍共用；ELLIPSIS在角色分派之前消费；getter/setter只解析名字，
不再用`typeRequired=false`接受`: T`、`!`、默认值。函数类型的推测解析及lambda参数仍是各自文法。
源码stub216、macro fragment parser算法版本16。

`CjMemberParsingContext`只保存kind和原始名字，不存CFIR身份或宿主common/abstract状态。
普通名义body建立scope；表达式、局部语句、LOCAL按scope语句及lambda入口copy清除，返回后原成员语境恢复。
这是普通源码解析基础，尚未增加scoped fragment factory、实际raw owner快照或vector splice；
真实primary名字的native候选/InvalidDecl恢复也没有实施，不能将捕获名字当成完成身份检查。

新增两类14项测试：访问器/参数7项、成员作用域7项（24组class/struct输入）。
19399暴露四组非法局部宏输入的PSI/LT诊断不一致：旧同行恢复停在`{`，把本应丢弃的括号组再解析成lambda。
三种语句入口现共用成对括号恢复，保留既有语句起点边界；未闭合组可像native一样消费到EOF，
不宣称所有错误源码都保留外层括号。另一个失败是测试要求非法默认值保留完整外层constructor范围，
官方ParseParameterList/ParseFuncBody并无此保证；改为检查真实默认参数归属、宏内无primary和后续成员恢复。
原输入文本未改。typed ELLIPSIS测试也不再误断言全部parser诊断为空：native CheckVariableParams另有owner级错误，
本步只验证共同参数token/节点文法，不宣称已迁移该owner检查。

验证记录（全部运行均已终止）：
- 78074 exit0（5m2s）：初版PSI37/37、raw三类28/28。
- 19399 exit1（15m3s）：PSI46中上述5项失败；CJO整树1/1、raw28/28、frontend141/141、LL168/168、allkinds2/2通过。
- **16629 exit0（6m40s）**：最终输入PSI46/46、完整LT raw141/141、完整PSI raw242/242、frontend141/141，
  共570/570，0失败/跳过。XML分别为UTC07:06:45–46、07:07:11–15、07:07:23–29、07:07:50–07:08:06。
  19399的LL/CJO/allkinds验证早于最后的共享语句恢复，不冒充最终输入的全量CFIR验证。
参数角色/作用域和共享语句恢复两次静态gate均通过。

下一步继续宏fragment设计§2–8。尤其补LocalStatements、保留ExprKind/逗号环境、按真实replaceLoc判数量；
array/tuple/FuncArg的表达式宏仍只可产生一个Expr，不得因为ParseNodes解析逗号就放宽为多元素。
旧macro input的UNKNOWN_SCOPE消费和专用两条诊断仍未迁移，不能把本次语句恢复当成完整macro输入修复。

### 2026-10-07：实际 raw owner 快照与片段恢复（验证中）

本批在 `aef419cc8` 上继续实现。`CfirReplaceHandle.ownerSnapshot` 为必填，PSI/LT 六处 surface
采集入口保存实际包、容器 symbol 路径、泛型 dispatch receiver、局部状态、function target、参数签名和名义语法上下文。
路径集合为不可变副本；symbol/target 保留同次 raw 构造的身份，允许采集时尚未绑定。
顶层容器路径为空是正常状态，不能补一个假文件/函数 symbol。

参数 API 统一接收 `MacroParameterSignature`，普通声明、class/struct primary、accessor 与 lambda 的文法身份明确分离。
默认值和参数 annotation 都位于签名帧内；进入内层函数/lambda 容器屏蔽外层签名，退出后恢复。
名义 owner 的语法数据仅在 surface 真正采集时读取，普通 stub raw 构建不为获取名字强制加载 AST。
纯语法模型 `CjParameterGrammar.kt` 移到 `common/.../parsing`，与 `ParsingFileFacts` 同属共享数据层；
`cfir:providers` 不新增对 PSI 的实现依赖。

两 raw builder 删除旧 `...InPackage` 宏入口，由 `withMacroOwner` 在新 builder 安装实际上下文：
先容器/receiver/target，再参数签名；新 token 的 source module 清空；`finally` 恢复。
正常 extend 构造与片段直接成员共用 `bindMemberOwners`，以真实 extend 对象连接 `containingExtend`。
frontend 工厂传递 mandatory snapshot，并校验原包和参数签名 owner 的对象身份。fragment parser version 为 17。

26 项新 raw 回归（捕获14、恢复12）覆盖 PSI/LT、泛型 class/struct、extend、参数默认值、lambda/局部函数边界、
容器集合不可变、return target、primary 参数事实。原16处手写 handle 测试迁移到实际测试 owner。
限定静态 gate 通过；这只验收捕获/恢复边界，不包含全部 parser/slice 功能。

71018 已 exit1（48s），未执行测试：providers 无法读取原 PSI 内的两种纯语法模型。
移动到 common 后 80204 已 exit1（12m2s）：providers55/55、frontend141/141、source-module annotation5/5通过；
raw定向48项中16项新测试失败。根因是共享parser的EXPRESSION_FIRST缺AT，`return @M(...)`没有宏操作数。
普通宏现接入既有atom/后缀/前缀/二元链，atom显式清除backToken，FIRST加入AT；不改测试输入或身份断言。
源码stub版本217；IfAvailable/annotation lambda既有特殊入口和statement分派暂保留。修正后验证结果待写入。
不能把此前570/570或完整CFIR8968项当成本批最终验证。

**仍未完成**：frontend 中 dummy function/variable wrapper、后代 `findFirst`、标量 declaration payload
仍存在；尚未实现 scoped fragment root、语法事实的完整 PSI/LT/lazy/增量传输、typed slices 与专用数量诊断。
下一步设计取证见 `cjmp-macro-fragment-owner-context-20261006.md` 第9节；完整P0–P10目标保持打开。

### 2026-10-07 22:06：接续其他会话并闭合本批验证

已读取当前 HEAD `aef419cc8`、未提交 owner snapshot/原子宏改动及最新交接；
旧13926已取消，其后整树 stub/FQName/copy 修复和主构造字段完成记录均以本文后续结果为准，
不重新运行历史作业。当前无本批活动构建。

queue `b6f84bac` 已 DONE/exit1，其实际新鲜结果为 providers55/55、完整LT raw167/167
（含26项新增捕获/恢复）、完整PSI raw242/242、frontend141/141、CFIR宏切片657/657，
全部0失败/错误/跳过。该轮PSI48项中仅一个新测试失败；不属于完整CFIR8968项重新执行。

唯一失败是测试把 `obj.1` 当作 DOT 后的非法整数 selector。
固定v1.1.3 `Lexer.cpp:1950` 及实际cjc探针证实 `.1` 是完整FLOAT token，
原生报告 `parse_expected_character`（期望分号/换行），范围覆盖两字符 `.1`；
`obj. 1` 与 `obj.@M()`/`obj. @M()` 才报告 `parse_expected_name`。
只修测试：保留原 `obj.1`，独立验证精确lexer token、PSI/LT的 `.1` 错误范围、
无DOT_QUALIFIED节点及后续函数恢复；三个真实selector输入保留全部非法selector类型断言。
没有针对该测试改生产或放宽selector文法。

handle25516 / queue `0755b439` 已exit0（1m6s），日志
`.workbuddy/tmp/macro-owner-selector-continuation-20261007.log`。
最终PSI五类49/49、0失败/错误/跳过；XML UTC14:06:31.239–14:06:35.075。
原子宏/backToken/selector与raw owner快照的限定静态gate通过。
selector使用simpleName来自仓颉native规则；Kotlin同层实际使用parseAtomicExpression，
不能把这一文法差异描述成照搬Kotlin。

下一批仍是宏fragment文档§9的完整语法事实协议，再贯通scoped roots、typed slices与vector splice。
`IfAvailable`普通模式的enableCustomAnno/ExprKind门尚未对齐，本批正向annotation测试不证明完成。
journal的回滚、precede、lazy seed、copy及同文本不同语境的真实SDK merge均必须验收，
不能在treeBuilt返回后安装数据就声称覆盖增量重解析。

### 2026-10-07：主构造器名字身份与 InvalidDecl 接续（验证中）

普通 class/struct 成员按固定 v1.1.3 原始 UTF-8 字节编辑距离和四种后继 token
识别 primary 候选，再按去反引号的语义名字确定直接成员身份。近似错名保留 typed
`CjInvalidDeclaration` 的源码签名，placeholder stub 阻止全部内部子 stub，PSI/LT raw
显式生成 `CfirInvalidDeclaration`。宏 input 保留原生 primary carrier 身份，不套用直接成员收尾。
远错名恢复消费平衡括号组，保留下一个真实声明起点；MACRO/EXTEND/INOUT 已按 native 补齐。
源码 stub 版本218，fragment parser版本18；限定生产静态 gate 通过，运行验证尚未结束。

三类测试输入已冻结：primary parser/raw 身份矩阵、far 恢复停点及生产整树 invalid
序列化/独立 fresh physical loader attach/无 AST 查询。raw 阶段字段均为空，只有非宏
结构完整后显式执行字段物化；不能提前对尚未执行的宏 carrier 生成字段。

验证 handle **53763** 已终止 exit1（7m2s），日志
`.workbuddy/tmp/primary-identity-verification-20261007.log`，在 CJMP 工作树运行队列包装器。
PSI 十类71项中70通过、1失败：far recovery 的 MACRO 停点之后尚未保留目标声明节点，
正在核定固定 native 的作用域接收行为。LT raw 六类56项中52通过、4个新 identity 方法失败：
JUnit3 inherited 三 String `assertEquals` 抢占 Kotlin 断言重载，把源码错误当 actual；
明确修正测试 alias 后仍须重新运行矩阵，不据其他通过方法推断新矩阵已通过。
本轮0错误/跳过，生产整树 invalid 两项实际通过。旧结果不作为本批最终通过证据。

该 MACRO 失败已证实是生产分派缺口：固定 v1.1.3 ParseMacroDecl 在错误作用域标记
HAS_BROKEN，但仍完整解析并保留 MacroDecl；IsInvalid 不按该标志判断。普通成员现已补共享
MACRO 分派，关键字自身报告已有本地化 scope 诊断，签名/正文仍保留。LOCAL 同样传真实
FUNC_KEYWORD；不用字符串特殊分支。生产静态 gate 通过，raw 测试全部 equal alias 已修正。
追加 PSI/LT 五非法作用域+顶层，以及四 raw 模式的远错恢复后 MacroDecl 保留检查。

handle **18835** 已终止 exit1（5m1s），日志
`.workbuddy/tmp/primary-identity-final-verification-20261007.log`。PSI72中71通过、局部 macro 1失败：
三种 parseStatements 入口共用的 STATEMENT_FIRST 缺声明起点，预先消费 MACRO，未到既有分派。
LT raw56中55通过，仅 LT LAZY identity 的测试把 null body 错当必须 CfirLazyBlock。
convertConstructor/convertMacroDeclaration 的现有 LT lazy body=null，PSI lazy 用 CfirLazyBlock；
两条显式契约分别验收，不改变生产 body 协议。两轮均0错误/跳过。

### 2026-10-08：共享语句声明入口与完整 raw 回归（验证中）

固定 native Parser.cpp ParseExpressionOrDeclarations/ParseExprOrDecl 先 SeeingDecl 再 ParseDecl；
Kotlin KotlinExpressionParsing 的 STATEMENT_FIRST 同样组合声明起点并进入 local declaration。
现共享 FIRST 补齐通用分派已存在的 MACRO/STRUCT/ENUM/EXTEND/TYPE/MAIN/FOREIGN 起点，
不加入 EXPRESSION_FIRST。对应局部声明保留/诊断/后续恢复新增矩阵，宏定义源场景保持。
raw 测试修正 PSI/LT lazy 双契约，并补 macro 参数 symbol owner、参数/返回 syntax type 各自
source、NORMAL input 访问表达式及其 source；不能把 declaration source 当正文证据。

本批 agent 续接因 workspace credits 错误终止；主线程已直接读取固定 native/Kotlin
对应 FIRST 和语句分派源码，继续执行，不把此错误作为构建或目标阻塞。
handle **54306** 已终止 exit1（5m51s），日志
`.workbuddy/tmp/primary-identity-statement-verification-20261008.log`。目标为 PSI 十类和 LT raw 六类；
PSI73项72通过，剩余顶层 control 以裸 macro 起首，被现有包头探测误吃；局部 MACRO 和其他
声明 FIRST 矩阵已通过。raw56/56通过，含完整新身份矩阵及补充的 symbol/type/body 事实。

进一步读取固定 native ParserImpl.h SeeingMacroPackage：它匹配 MACRO+PACKAGE 双 token，
不是只看 MACRO。现 parsePackageDirective 的无修饰/有访问修饰探测均要求该双 token，
防止裸 macro 或 public macro 定义被当包头。顶层合法 control 使用 macro package 及 public；
另增两种裸定义的实际声明保留/空包头/后续恢复测试，不把此 AST 检查当成完整 macro 定义
public、所属包、参数类型等专用诊断已全部迁移的证明。

handle **15551** 已终止 exit0（11m18s），日志
`.workbuddy/tmp/primary-identity-integration-verification-20261008.log`。范围：PSI十类、providers，
完整LT raw/PSI raw、frontend、low-level-api-cfir；进程使用固定v1.1.3 CANGJIE_HOME。
最终输入 **851/851、0失败/错误/跳过**：PSI74、providers55、完整LT raw171、完整PSI raw242、
frontend141、LL168。XML UTC分别为01:41:29.923–01:41:32.304、01:41:43.595–01:41:44.063、
01:42:11.138–01:42:16.237、01:42:24.375–01:42:31.101、01:42:51.806–01:43:09.990、
01:47:01.549–01:47:36.217。diff --check通过。此结果不包含完整CFIR回归，亦不代表整个P0–P10完成。

完整验证 **79919** 已终止 exit0（33m22s）：完整 `:cfir:analysis-tests:test`，日志
`.workbuddy/tmp/primary-identity-full-cfir-verification-20261008.log`；沿用仅内存/进程寿命调整的
`cjmp-cfir-memory-20261006.init.gradle`，未筛除测试。1266份新鲜XML共8968条记录：
8571通过、397跳过、0失败/错误，时间覆盖2026-10-08本地09:54:00.842–10:26:31.598。
本批验证输入冻结已结束。79919/15551/54306/18835/53763均已终止，不能继续轮询或重启旧 handles。

2026-10-08重新核对原始70项及当前生产源码，整项闭环8/70（11.43%），62项未闭环。
按用户要求再按目标子要求计入部分实现，等权实现完成度19.30%，剩余80.70%；8项整项完成、
14项部分完成、48项目标完成尚未证实。逐项分数及依据见`cjmp-progress-audit-20261008.md`。
11.43%不再用作整体实现完成度；前置研究与测试通过率不代替生产目标。

下一实施仍为完整 parser ScopeKind/ExprKind/journal/lazy/增量事实协议，再贯通scoped roots/vector splice。
尤其 fixed ParserDiag.cpp:386 的 MACRO_BODY 显示为 macro/macro body，不能据本批 LOCAL普通函数
测试就把全部 LOCAL（含宏正文）当 FUNC；当前语法模式尚不保存这个真实body scope，须由完整协议
解决并验收 PSI/LT/lazy 一致性。完整 macro public/所属包/参数规则亦未由本批AST保留测试证明完成。

2026-10-08继续接入：根marker事务补充两接口三参数doneBefore/errorBefore、错误结束保留事实及
折叠getter回归。handle39188终止exit0（37s），MacroParsingJournalMarkerTest9/9，0失败/错误/跳过，
XML UTC2026-10-08T03:04:26.080Z。旧17486不再轮询。

随后修复普通语句宏提前结束运算链：首操作数parser沿共享Precedence左侧递归到postfix，
按LOCAL文法创建完整宏后继续postfix/binary；右操作数保持普通入口，注解分流不变。
静态gate通过，详见macro-fragment-owner-context文档§9.14。handle58563正验证PSI三类回归，
尚未终止；不要改动冻结生产/测试输入，也不能把旧851/8968记录当本批结果。后续先观察该handle，
再补注解体/scoped入口回归并贯通完整事实协议；原始70项范围与剩余工作不变。

### 2026-10-08：接收其他会话的未提交工作与当前断点

本次重新检查同一实施工作树，仍在 `codex/cjmp-framework-implementation`，旧HEAD为 `aef419cc8`。
owner snapshot、primary身份/InvalidDecl、语句宏优先级、marker journal及对应测试均实际存在，尚未提交。
保留工作树原有HTML删除、`.workbuddy/tmp`取证产物和IDE工作区配置，检查点只包含CJMP代码/测试/文档。

已读会话“切换到 cjmp-framework-implementation”的最后工具结果：58563对应的PSI三类定向运行已exit0；
此后新增annotation-only语句体回归，并执行完整 `:psi:test`，该运行已exit1。
当前31份XML共212项：211通过、1失败、0跳过，UTC 2026-10-08 03:15:59–03:16:04。
唯一失败是 `MacroExpressionPrecedenceParsingTest.annotationOnlyStatementPrefixRemainsAnAnnotationOnItsDeclaration`：
annotation-only文件的lazy函数body重新进入DEFAULT，`@Tag[1] func nested`变成宏而非annotation。
因此旧851/851和完整CFIR8968项仅证明此前输入，不证明最新语句体模式完整。
旧77411 handle已不存在；队列owner PID18440也已不存在，当前未发现该工作树的Gradle/测试进程。

先保存带上述已知失败的工作检查点，再从共享parser seed实施继续：为真实lazy入口记录不可变解析上下文，
通过SDK节点构建/复制/增量重解析传输；不从最近owner或文件名推测，也不以强制展开全部body消除失败。
完整scope/ExprKind登记、scoped fragment、typed slices、LL真实宏构造和P0–P10剩余目标仍保持打开。

### 2026-10-08：parser seed 接入与用户要求的类型安全平台注册

检查点 `730929368` 已保存上一批47个CJMP文件。此后的seed实现仍在继续：新增 `CjParserSeed`、
每次解析私有的 `CangJiePsiBuilder`、prepareLightTree后冻结的journal结果，真实BLOCK/LAMBDA的入口
通过collapsed节点或自身开括号传递；file/codefragment/lazy入口已使用新的构建链。
种子在进入正文前捕获，重解析的禁折叠操作不改变lambda原始seed。CodeFragment创建时确定入口，
`createContentLeafElement`只向新节点复制；source stub219、fragment算法19。

验证状态：
- 85772 exit1：比较器误写成有构造器的类型；实际SDK是interface，已修正。
- 92064 exit1（7m47s）：44项中35通过。原annotation-only语句体回归通过；新生命周期8项缺
  CodeStyleManager服务，1项非物理文件copy的未加载假设错误。后者已按SDK实际clone合同修正，
  保留copy后正文独立延迟解析断言。
- 98660 exit1（1m57s）：直接CodeStyleManagerImpl导入缺编译依赖，未执行测试。
- 27278 exit1（8m4s）：完整PSI237项，226通过、11失败。10项真实DiffLog触发Document的EDT约束，
  两新增测试现用真实EventQueue.invokeAndWait加原writeAction；另1项ConstSafe getter错误仍待修复。
  该运行使用修正前的反射注册，不能当作当前类型化接线的运行验证。

用户明确质疑 `Class.forName("com.intellij.psi.impl.source.codeStyle.CodeStyleManagerImpl")`。
该写法已经删除；后续不得用字符串反射掩盖公开平台类型缺少编译依赖。
聚合SDK模块显式声明code-style和code-style-impl API；CodeStyleManagerImpl直接构造。
新增CangJieHeadlessPomModel使用公开PomModelImpl和公开PostprocessReformattingAspectImpl，
按SDK LangPomModel完整保留两种aspect查询及super.update→aspect.update，不反射私有构造器。
未知aspect返回可空T，保持SDK合同。
- 49365 exit1（1m35s）：缺code-style中公开aspect基类的编译依赖；补齐后
  **66793 exit0（1m47s）**，`:compiler:config:compileKotlin`通过。
- **30382 exit0（19s）**：CangJieCoreEnvironmentTest 6/6、0失败/错误/跳过；新增真实后处理事务
  嵌套/异常恢复测试通过。这只证明平台注册，不冒充全部parser seed完成。
上述handles均已结束，当前没有本线程仍在运行的Gradle任务。

下一步直接继续生产接入，避免重做上述考证：
1. CjAnnotation的builtin来源改为共同parser模块来源；CjCodeFragment的子片段沿已冻结入口，
   不能绕回已经变化的originalContext。
2. root结果metadata目前仍只写临时CjFile，SDK full diff后宿主会保留旧值；必须随新AST提交。
   已核准更正规的每次解析私有ASTFactory+ParserDefinition方案：在始终存在（含空文件）的真实
   PACKAGE_DIRECTIVE上附input seed/实际module/fileFacts；两个comparator对其完整结果不同返回NO。
   SDK允许这条零宽节点替换路径，详见设计文档§9.16。
3. CjFile.subtreeChanged会在diff中间多次调用；只从当前已存在的直接package结果载体发布，
   删除全部旧children后的短暂缺失不能解释为空文件。若要求事务末尾一次发布，使用正式POM listener。
4. 两新增测试文件ParserSeedLifecycleTest（14）及ParserSeedCodeFragmentTest（11）保留强断言；
   EDT修正及typed服务之后需重新跑完整PSI，再做raw/frontend回归。
5. 真正scope/ExprKind occurrence登记、LT冻结输出、CASE_BLOCK独立入口、scoped roots与vector仍未接通；
   原生非法annotation声明回滚、无括号非法macro input消费、成员孤立brace制造FUNC也仍在未完成清单。

### 2026-10-08：文件结果通过真实AST提交，完整PSI通过

在 `9df9bad20` 上接通每次解析私有的 `ASTFactory + ParserDefinition`，完整保留原definition的
composite/leaf/lazy工厂协议。完整文件的input seed、实际module、ParsingFileFacts附在真实
PACKAGE_DIRECTIVE上，包含零宽package；AST→LT和AST→AST比较均比较完整结果。
初次treeBuilt成功后发布，实际DiffLog提交后CjFile从当前直接package载体更新缓存；
替换中间暂时没有children时不把它解释为空文件。onContentReload清除旧解析结果。

CjAnnotation的builtin判断使用共同parser模块来源。子CjCodeFragment直接继承父片段完整immutable seed，
保留module、sourceKind、COMMON/SPECIFIC角色、编译输入与quote状态，不绕回已变化宿主。

验证：
- 22849 exit1（19m2s）：完整PSI241项227通过、14失败。12项缺真实command上下文；
  已使用EventQueue.invokeAndWait→CommandProcessor.executeCommand→原writeAction→DiffLog。
  另外2项是publicationSnapshot先读未解析结果的错误顺序。SDK没有跳过空文本解析：file.node仅加载
  lazy根，访问直接package才ensureParsed；已调整测试顺序，全部空文件/隔离断言保留，没有新增空根特判。
- **49510 exit0（9m10s）**：完整PSI33个suite、**243/243，0失败/错误/跳过**。
  XML UTC 2026-10-08T07:02:47.719Z–07:02:56.512Z，已保存至
  `psi/build/verification/20261008-parser-seed-root-results/`。
  新生命周期18项、片段13项覆盖真实局部/整文件diff、空→空配置变化、提交前隔离、非物理copy、
  宿主变更与两种父片段角色。SDK标准clone不需要新hook：rawFirstChild复制不会自行触发语言parser。
- **25998 exit0（21m43s）**：core环境6、providers55、完整LT raw171、完整PSI raw242、frontend141、
  LL168、全部宏CFIR及source-module657，共**1440/1440，0失败/错误/跳过**。
  与49510的完整PSI合计**1683项通过**。各模块XML已保存到各自的
  `build/verification/20261008-parser-seed-integration/`；宏CFIR XML UTC07:25:03.939–07:31:13.723。
  本批没有重跑全CFIR8968项，不能混用旧全量结果。25998已终止，输入冻结结束。

后续继续真实scope/ExprKind occurrence登记、LT结果、CASE_BLOCK、scoped roots与typed slices；
本批没有关闭整个宏构造协议或原始70项计划。

代码提交为 `3a0abe8b6`。下一步生产登记方案已补入设计文档§9.17，包括直接根宏返回协议、
高层ExprKind refinement、真实逗号parent、token-only/quote插值区别、ATEXCL以及参数宏分流。
必须保留两个已证实但尚未实现的边界：普通源码IfAvailable原生仍是MacroExpandExpr，
只有enableCustomAnno的特定入口才专有化；参数宏必须实现原生MacroExpandParam和construction槽位，
不能给当前强制annotation形状伪填scope。所有已验证输入和完整P0–P10范围继续保留。

### 2026-10-08：invocation事实首次完整PSI验证及quote/IfAvailable修复

当前源码接通ParsedExpression直接根协议、scope/高层ExprKind refinement、直接comma parent、
PSI实际AT载体与LT同根冻结结果。完整PSI handle39007终止exit1（1m4s）：274项270通过4失败。
两项是quote的AT形状专用分流进入普通macro input/attr scanner，吞掉内部真实$()插值；
另外两项分别为fixture函数名throwing命中实验keyword、半开slice错期待RANGE_EXPRESSION。

按固定ParseQuote.cpp107–140删除quote的AT形状分流及两个无其他调用的专用方法：AT为普通token，
括号统一计数，$()/FIELD_IDENTIFIER保留真实插值入口。普通macro input/attr的token-only屏蔽保留。
fixture改为合法thrower；半开slice明确期待SLICE_EXPRESSION，完整range仍期待RANGE_EXPRESSION。

同时按固定ParseExpr.cpp372/Parser.cpp539修复IfAvailable阶段差异：只有EnableCustomAnno的
高层非INDEX_EXPR入口及语句入口专有化；普通源码保留MacroExpandExpr及运算链。旧普通源码测试
改为真实宏节点，新增initializer/statement事实、binary继续和annotation-only index/RHS边界。
两生产方案静态gate通过。handle15843已终止exit0（18m33s），完整PSI34个suite、276/276，
0失败/错误/跳过，XML UTC2026-10-08T11:37:35.928Z–11:37:57.992Z。
其中MacroInvocationFactsParsingTest33项均通过，包含真实BlockSupport提交、同根LT冻结、
tuple真实解析回滚、普通与annotation-only IfAvailable的阶段差异。
34份XML已保存到psi/build/verification/20261008-expression-occurrences-15843/。
完整参数宏/construction槽位、ATEXCL原始声明事实、scoped ParseNodes/vector仍未完成。

静态gate随后发现三个token scanner的既有美元消费错误。固定v1.1.3 ParseQuote.cpp要求
quote裸美元符号诊断后单次消费，原实现errorAndAdvance后再advance会吞掉真实右括号；
已改为诊断消费后continue。ParseMacro.cpp的普通macro input/attrs允许裸美元符号为token，
已删除这两个scanner错误的美元诊断分支，保留各自的普通括号计数与token-only隔离。
新增真实quote($)的单条精确诊断、quote终止范围及后续函数身份回归，以及@Input($)、
@Attr[$]()无诊断、后续宏事实与函数身份回归；这批新增输入尚待验证，不混用15843结果。
handle56374已终止exit0（6m55s），完整:psi:test为34个suite、278/278，0失败/错误/跳过。
XML UTC2026-10-08T11:46:05.781Z–11:46:14.118Z，已保存到
psi/build/verification/20261008-expression-occurrences-dollar-56374/。
美元修复的实际patch与真实源码测试静态gate通过；随后handle44560开始完整两种raw、frontend、
宏CFIR回归。44560已终止exit1（1m2s），尚未进入测试：共享Gradle9.4.0 generated-gradle-jars
缓存锁被PID7572持有。未终止其他会话进程，不能将此次环境锁失败计作源码失败。
该调用中短模式*Macro*被Windows java launcher展开成目录macro；重试必须使用完整限定类模式，
保留真实宏测试范围，不使用这个无效短筛选结果。
51245的完整限定模式已正确透传，但同样在进入测试前exit1（1m2s），仍为PID7572共享缓存锁。
接下来重用已成功25998的同一完整集成任务与参数，利用其既存configuration cache继续验证；
不删除共享缓存锁、不终止其他会话daemon。
71608同样exit1（1m2s）：动态依赖bcutil-jdk18on缓存版本过期使旧configuration cache失效，
随后jars-9缓存锁仍被PID7572持有。下一次使用同一任务加--offline，检查本地已有依赖和旧配置
能否直接继续；前述三次均未执行测试，没有新的集成成功结论。
19033的--offline尝试已终止exit1（1m1s），仍在进入测试前等待同一PID7572的jars-9锁超时。
当前没有本会话的活跃构建，输入冻结结束；四次集成调用没有产生新的测试XML。
此次最后已验证源码为56374的完整PSI278/278，其中invocation35/35；raw/frontend/LL/宏CFIR
只能引用25998旧输入1440项结果，不能计为当前occurrence批次的集成验证。
后续优先实现§9.18的真实调用事实与独立替换槽位贯通，或完整token escape协议；不要继续
重复相同缓存锁调用，也不要终止其他会话进程。共享缓存释放后补当前集成回归。

另一个尚未完成的独立边界：真实flex/生成lexer未产生ESCAPE_LPAR/RPAR/LBRACKET/RBRACKET，
反斜杠走BAD_CHARACTER；目前dormant escape分支仍有双消费和generic硬编码诊断。
quote/input/attrs的合法escape集合与官方诊断不同，必须完整贯通词法/语法协议，不能用
人工token流或泛化Illegal Token宣称修复。本次美元修复未修改这些分支。

### 2026-10-08：真实token escape与非消费报告范围（实施中）

本轮按固定native ParseQuote.cpp/ParseMacro.cpp和Lexer::GetSymbolToken实现真实BAD_CHARACTER
反斜杠+独立目标词元协议。quote转义不参与括号计数/插值；普通input/attrs共用原生token scanner，
删除AT递归声明文法分支，AT与裸美元符号都是普通payload token。非法macro escape仅消费连续
'['、'('、'.'恢复起点后结束scanner，失败不补结束符错误；attrs外层仍继续尝试input入口。
AT_EXCL是实际独立词元，\@!不属于macro input/attrs合法escape，已按gate移除误接受分支。

共享errorOnCurrentToken原语使用未注册、不可变的CjTokenRangeErrorElementType，零宽marker
与完整token报告宽度分离；实体仍为PsiErrorElementImpl子类，不覆盖AST textRange、不包裹
目标词元、不提前推进。PSI/LT诊断collector读取结构化报告范围；EOF保留SDK真实零宽错误。
不使用延期pending消费或消息匹配，marker rollback直接撤销错误载体。SDK typed LT与AST ERROR
自然判为不同节点并替换，不能声称customComparator让其判等。默认IDE零宽错误通常仅高亮
单字符，IDE完整token高亮/避免重复尚未集成，不计为已完成。

官方1.1.3 cjc已实测native-quote.cj的\[、\]、\ [、\ (、\ @：共5条unknown token start，
每条仅覆盖反斜杠1字符；\ [没有重复错误。JSON保存于
.workbuddy/tmp/escape-protocol-20261008/native-quote-diagnostics.json。
native-recovery.cj证明非法macro目标宽度不能改成zero：$bad为4、AT_EXCL为2、异类括号为1。
实际共享原语、scanner、真实fixtures及SDK生命周期测试仍在准备，尚未运行本轮新输入测试；
不得混用56374的278项成功。PID7572现已不存在，可在本轮PSI通过后补原集成验证。
本批完整PSI验证handle44488已启动并确认live，当前生产与测试输入冻结。
新增真实SDK低层scanner停点/4、1、2报告宽度/目标直接父身份、marker rollback、关闭报告同消费、
PSI copy与真实BlockSupportdiff回归。共享primitive服从shouldReportError且EOF保留原生point。
静态gate正在核实际代码与SDK零宽节点空白binder；任何修正须待44488终止后写入。

44488已终止exit1（9m6s）：完整PSI34个suite、285项283通过2失败，0错误/跳过。
MacroInvocationFactsParsingTest新增输入42/42通过，XML UTC2026-10-08T12:22:35.769Z；
全批XML保存在psi/build/verification/20261008-escape-range-44488/，不能记录为整批通过。
两失败是旧fixture仍把普通括号payload内的@Inner/class当真实PSI节点：
EnumConstructorAnnotationParsingTest.parenthesizedMacroInputDoesNotExposeAQuotedDeclaration、
NestedNominalParsingTest.parenthesizedMacroInputPrimaryConstructorDoesNotBecomeTheOwnerConstructor。
已按ParseMacroCallTokens的token-only语义改为无嵌套宏/class节点，检查完整input文本、
宿主与后续函数身份，stub序列化亦不生成payload声明；修改后尚待重新测试。

实际SDK253.29346.379静态gate通过共享零宽载体设计：默认左binder不吸前置空白/comment，
done边界受start限制，不应反改成贪前置空白的binder。静态gate未发现生产结构拒绝项，
但真实trivia停点、非法attrs、连续恢复起点及EOF仍需覆盖，不把adjacent input三例算完整验证。
官方cjc补跑--error-count-limit all，native-recovery.cj共14条错误完整JSON已保存到
.workbuddy/tmp/escape-protocol-20261008/native-recovery-diagnostics.json；attrs的目标宽度亦为1/4/2。

新增框架边界正在核对：native宏token scanner设置skipNL=false，COMMENT仍跳过、独立NL保留；
本地lexer将NL合并WHITE_SPACE后SDK跳过。不得用跳过换行后的目标词元冒充原生NL停点。
native-newline.cj的cjc仅输出3条slash词法错误（词法失败门控了parser消息），不能据此否认
原生parser的NL消费协议。后续先完成lexer/builder所有权审查与真实恢复测试，再补当前集成回归。

### 2026-10-09：未知起始字符诊断通道复核

普通代码及长模板插值表达式状态的未知字符现在使用专用 `UNKNOWN_TOKEN_START` 词元，
词法事实留在真实 token stream 中；宏反斜杠仍走既有 `BAD_CHARACTER` 协议。独立 CR 按
官方 v1.1.3 `ScanSymbol/GetSymbolToken` 路径归为未知起始字符，LF/CRLF 仍是 whitespace；
未闭合反引号子状态有专属 BAD_CHARACTER 消费规则，非法输入不会触发无匹配扫描异常。
PSI、LightTree 使用共同的纯消息构造器，按未知叶 token 才读取对应字符，递归 PSI 收集会按
SDK lazy 协议进入尚未解析的 `BLOCK`。共享高亮、raw LightTree 忽略集合和 pattern syntax-error
识别已加入该词元；LSP semantic-token 仅保留原 BAD_CHARACTER 的 token 分类行为，LSP parsing
diagnostics 仍未接入该 collector。

`CangJieLexicalDiagnosticTest` 覆盖 quote、宏 input/attrs、Unicode/non-BMP、控制字符、独立 CR、
注释/字符串隔离、lookahead/rollback 及真实 lazy block；`CangJieLineTerminatorTest` 覆盖独立 CR
和未闭合反引号后的任意字符消费。独立复审会话使用 `gpt-6.1-sol` / high 并接受本批实现。
最新 `:psi:test` **297/297 通过**；`:cfir:raw-cfir:light-tree2cfir:test` 通过，
`:cfir:checkers:compileKotlin`、`:code-insight:highlighting:compileKotlin` 与 `:lsp:compileKotlin`
通过。`:code-insight:highlighting:test` 曾有4项因测试进程的 IntelliJ `JBUIScale` 未预计算失败，
错误栈在 UI scale 初始化，不是新增 lexer 映射断言；本批仅记录其 production compile 成功。

本批只闭合默认未知起始字符 token 与 PSI/LightTree 收集，不代表其他 malformed lexer 状态已完工，
也未实现 cjc 的 LEX/PARSE 类别输出优先级。宏参数 construction 槽位、完整独立 NL 协议、
LSP parsing diagnostics 和整体 CJMP 计划仍是未闭合项；IfAvailable 集成失败也没有被本批改变。
