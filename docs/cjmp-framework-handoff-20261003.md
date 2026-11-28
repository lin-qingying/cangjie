# CJMP 实施状态：2026-10-03 pattern / LL 声明事务

> 最新续接入口：[2026-10-06交接](cjmp-framework-handoff-20261006.md)，包含三类新修复、有效验证和系统内存阻碍。

本文补充两份 1002 交接。完整目标仍是主计划 P0–P10 / A1–J1，目标未完成。

当前重新审计见 [70 项源码闭环审计](cjmp-progress-audit-20261003.md)：8/70 = 11.43%，
其余 62 项未闭环；此口径不折算局部实现或测试通过率。P0/P10 门禁尚未全部完成。
主计划 §13.17 与迁移台账记录本轮证据；九步不可变发布迁移要求不变。

## 语法职责迁移：实施中

### 宏重解析语法通道（2026-10-03，局部验证已通过）

- 新增独立 `ParsingDiagnostic` 和类型化 `MacroFragmentReparseResult.SyntaxFailure`；
  PSI/light-tree 在生成文本 raw 构建前收集共享 parser 错误，传入当前 session 的独立 CJMP 选项。
- `MacroExpansionRegistry.parsingFailures` 与语义诊断分开；STRICT/DEGRADED 均拒绝语法错误产物，
  不添加 CFIR 语法工厂，不调用该失败片段的 splicer。parser 算法版本升至 4。
- CLI 原样报告 parser 消息，主位置绑定原始宏调用，生成文本及其原始错误范围保留在结果中；
  构造报告错误后中止前端，禁止继续写 CJO。该主位置尚未证明与 native 文件角色错误锚一致。
- 新增 token-backed 语法结果/独立通道测试，以及 STRICT/DEGRADED 不拼接测试。
- 本轮续接新增生产 parser 实际执行测试：PSI/light-tree 各验证 common/specific 文件角色拒绝，
  以及 common 无体函数缺返回类型；运行使用真实 CoreEnvironment 和默认 fragment parser factory，
  仅宏 executor 提供返回 token。该测试已在 frontend test 编译开始前加入，同一 handle 覆盖。
- 验证 handle **47091** 已终止，exit 0，9m24s：
  `:cfir:providers:test --tests *TokenBackedMacroFragmentParserTest* :psi:test --tests *CjmpFunctionParsingTest*`
  `:compiler:frontend:test --tests *CjmpTwoPhaseCompilationTest* --tests *FrontendMacroConstructionExecutionTest*`。
  新鲜 XML：token-backed 5/5（15:27:09.443Z），共享 parser 8/8（15:27:23.147Z），
  frontend macro 25/25（15:27:46.510Z），CJMP CLI 21/21（15:27:42.949Z），均无失败/跳过。
  每个任务的过滤参数已紧跟该任务。不要再轮询此终止 handle。
- native v1.1.3 `ParseMacro.cpp::ParseNodes` 明确设置 `currentFile = currentMacroCall.curFile`，
  因而宏必须继承并更新宿主的真实 parsing 文件角色；现有新选项注入尚未实现这项。
  `.cj.d` 的 compileCjd 继承和剩余 6 个 CFIR PARSE 定义仍待完成。
  生产片段测试已通过；完整 CLI 宏路径测试已新增，覆盖 PSI/light-tree × STRICT/DEGRADED，
  要求 executor 实际执行、原始 parser 消息绑定宿主调用、零 CFIR 诊断且不写 CJO。
  首次验证 **93395** 因测试使用错误枚举常量 `MACRO` 编译失败，已按实际 schema 修正为 `Macro`。
  验证 **99257** 已终止 exit 1（23s）：22 项中原 21 项通过，新 CLI 宏测试失败，
  首个 PSI/STRICT 场景 executor 实际调用为 0，collector 消息为空。
  随后把调用改成源码限定名 `@macros.GenerateDeclaration`，定向验证 **93787** 亦已终止
  exit 1（27s），仍 executor 0、消息和 CFIR 诊断均为空；因此不能归因于裸名称导入。
  续接取证 **6205**（19s）及 **35279**（16s）均终止 exit 1：完整 PSI AST 的
  MACRO_EXPRESSION/MACRO_INPUT/FUNC 均存在，raw origin registry 也保留
  `(macros.GenerateDeclaration, PLAIN)`，最终文件有 1 个声明。因此 raw 构建没有丢宏。
  真正原因是 `externalDemandPackage` 对 annotation carrier 只接受源宏包/标准宏包，
  完全忽略配置中明确声明的外部 macro artifact；artifact preparation 没有需求而直接返回空，
  最终把已配置宏误判为 custom annotation，executor 未执行。
  已给 classification 增加已知外部宏包输入，并由统一
  `CompilerConfiguration.createMacroDemandClassification` 汇集显式 artifact 和源码宏编译请求，
  CLI 与测试 frontend facade 共用。只改变 demand 依据，实际定义仍须 locator/resolver 验证后冻结。
  临时 AST/registry 探针已移除；测试扩展成 PSI/light-tree × STRICT/DEGRADED × 裸名称/限定名称。
  handle **92479** 已终止 exit 0（2m48s），新 CLI 宏测试通过（15:44:22.318Z），
  8 个真实执行场景均检验 executor 1 次、parser 原消息、宿主调用位置、无 CFIR 诊断、无 CJO。
  后续回归 **59472** 终止 exit 0（14s）：CJMP CLI 22/22（15:45:13.591Z）、
  macro execution 25/25（15:45:16.448Z）、orchestration 29/29（15:45:16.604Z）、
  artifact resolver 9/9（15:45:16.539Z），均无失败/跳过。
  随后扩展同一 CLI 测试覆盖 LF/CRLF，总计 16 个场景。**55248** 终止 exit 1（18s），
  LightTree/STRICT/裸名称的 CRLF 场景 executor 为 0，CJO writer 遇到错误类型文本 `): U`。
  取证发现 `LightTree2Cfir.buildCfirFileWithSurfaces(tree,...)` 丢弃已归一化 parser 文本，
  重新读取磁盘 CRLF 原文并套用归一化节点偏移，污染名称、类型、宏 tokens 的文本切片。
  已改为直接取 `lightTree.toString(lightTree.root)`，确保 raw 构建消费树本身的解析文本。
  验证 **20599** 已终止 exit 0（53s）：CJMP CLI 22/22（15:49:28.859Z）、
  macro execution 25/25（15:49:32.072Z），均无失败/跳过。CLI 新用例中 16 个真实场景通过。

### 宿主文件文法事实：基础层局部验证通过，宏事务接线待完成

- 新增 common `ParsingFileFacts(sourceKind, cjmpRole)`，角色为 NONE/COMMON/SPECIFIC/BOTH。
  `SyntaxOnly` 保留实际角色事实，编译上下文仍按独立选项验证角色；不从 CFIR status 反推。
- `CjmpParsingContext.Compilation.initialFileRole` 支持显式继承原文件角色。
  `CangJieParsing` 的子 parser 共用一次文件角色事务，对外发布不可变事实快照。
  PSI parser 写入 `CjFile.parsedFileFacts`，三种 light-tree 文件入口通过完成回调发布。
- 完整 PSI raw 文件及从源码解析的 light-tree raw 文件承载 `CfirFile.parsingFileFacts`。
  code fragment 没有独立文件角色，尚不制造默认宿主事实。既有 tree-only raw API 还需传入
  显式 parser 事实；不能从 tree/token 扫描推断。metadata 已收敛为只读属性和一次发布入口，
  重复发布会违反不变量；构造期间的角色更新必须存放在宏事务中，不能修改原始文件快照。
- 新增共享 parser 测试，覆盖角色发布、PSI/light-tree 一致性，以及继承 COMMON 文件角色后
  specific 片段的混用错误。尚未接入宏事务：必须继续把宿主 facts 传入 MacroFragmentInput，
  由完成 parser 结果更新构造期每文件角色；并继承 `.cj.d` 实际源种类（接线见下节）。
- **65595** 已终止 exit 1（5m50s）：CLI 22/22（16:02:01.056Z）与 macro execution 25/25 通过，
  PSI 9 项中的新发布测试失败。失败是首个普通函数文件的事实为 null：测试只读取 `file.node`，
  尚未展开 FileElement 的延迟子树，因此 parser 未执行；已改为读取真实子树后断言事实。
- 2026-10-04 续接源码核对发现宏声明输入走独立分派，绕过 `checkCjmpFileRole`，且 secondary ctor
  未传 own detector，primary ctor 分派错误地排除了有修饰符的输入。
  官方 v1.1.3 `ParseMacro.cpp::ParseMacroCallDeclInput` 调用普通 `ParseDecl`，
  `ParseModifiers.cpp::CheckDeclModifiers` 再调用 `CheckCJMPModifiers`；不能让宏输入绕过文件事务。
  已在宏输入声明入口复用角色检查，并传递 primary/secondary ctor 修饰符；新增宏包装角色及
  common 无体构造器的 PSI/light-tree 测试。此判断依据源码，尚未声称新测试已通过。
- **34689** 已终止 exit 1（3m49s）：新 ctor 测试使用无泛型的 `emptyList()`，测试编译类型推断
  失败，已明确元素类型。后续定向 **12718** 已终止 exit 1（13s）：10 项中 9 项通过，
  文件事实发布测试复现 `quote(@M common func...)` 将宿主 NONE 错误变成 COMMON。
  官方 v1.1.3 `ParseQuote.cpp::ParseQuoteTokens` 的普通 token 分支只收集 token，
  本仓 speculative quote macro AST 不是真实文件声明。已在 quote macro 结构解析期间隔离
  文件角色事务（进入/退出计数，finally 恢复），避免引用 token 污染角色及产生文件角色错误。
  新增括号宏 token 输入和 quote 后真实 common 声明用例，检查隔离边界及恢复。
- **56442** 已终止 exit 1（4m51s）：共享 parser 10 项中 1 项失败，括号宏输入内的嵌套
  declaration macro 仍把宿主 NONE 改成 COMMON。已将该 token 输入中的嵌套宏结构解析也纳入
  文件角色隔离；官方 `ParseMacroCallArgsWithParen` 消费任意 tokens，而非宿主声明。
- 后续 **27760** 已终止 exit 0（40s），共享 parser 10/10（16:19:33.546Z），
  CJMP CLI 22/22 与 macro execution 25/25，新鲜 XML 均无失败/错误/跳过。
  本轮基础层局部验证通过；当时宏宿主事实传递、构造事务更新及 `.cj.d` 接线仍未完成。
  当前无运行验证作业，不再轮询上述终止 handles。

### 宏宿主文件事务：2026-10-04 接线待验证

- `MacroFragmentInput.fileFacts` 由 registry 按原始 `CfirFile` 对象身份传入；registry 独立维护
  构造期当前角色，原始 `CfirFile.parsingFileFacts` 仍是一次发布的只读快照。
- 生产 PSI/light-tree parser 拒绝缺失原文件事实的输入；继承 sourceKind 和 initialFileRole，
  返回成功、语法失败及已完成解析的形状失败中的实际 parser facts。Token-backed 结果保留它们。
- `MacroExpandPhase` 在处理 parser 结果前接入角色变化，包括语法失败；官方
  `ParseNodes` 使用 `currentMacroCall.curFile`，修饰符检查先改变角色，后续语法错误不会回滚它。
- PSI 宏文件的临时 file type 仍决定 custom-annotation 入口，`parserSourceKind` 单独继承
  原文件的 `.cj.d` 文法，raw PSI/light builder 也消费同一来源事实。parser 版本升至 5。
- 新增生产链路测试：PSI/light-tree、STRICT/DEGRADED、common→specific 与反序、同文件与
  独立文件、首片段缺返回类型，以及已有宿主角色与 `.cj.d` 无体 raw 状态。
- **50279** 已终止 exit 0（4m35s），新鲜 XML：token-backed 5/5（16:32:15.154Z）、共享 parser
  10/10（16:32:25.813Z）、CJMP CLI 22/22（16:32:42.461Z）、macro execution 27/27
  （16:32:45.521Z），均无失败/错误/跳过。新测试包含 32 个角色事务组合和 12 个源种类/宿主角色组合。
- 在该作业 providers 编译后补充了事务角色不可丢失的不变量。后续 **37409** 已终止 exit 0
  （3m27s），token-backed 5/5（16:36:21.929Z）、CLI 22/22（16:36:35.338Z）、macro execution
  27/27（16:36:38.430Z），均无失败/错误/跳过。本轮最后两条不变量已覆盖。
  该命令的 orchestration/artifact resolver 过滤器误加了 Frontend 前缀，实际未选中这两类测试，
  不能声称已覆盖。已按实际类名另跑完整宏回归组合，命令直接终止 exit 0（7s），
  无存活 handle。新鲜 CLI 22/22、macro execution 27/27、orchestration 29/29、artifact resolver
  9/9，均无失败/错误/跳过。当前无运行作业，不再轮询 50279/37409。
- 本轮宏继承和文件事务局部行为已通过。后续仍须完成原始文件级语法错误的主锚映射、
  tree-only raw API 显式传入 parser facts、语言版本门，以及剩余 6 个 CFIR PARSE 定义迁移。
  完整 P0–P10 / 70 项审计口径未变；本轮不是全量验收，也未解决先前四项广域测试失败。

### 已解析 LightTree 文件边界：定向契约通过，全量 raw 仍有失败

- 新增 `ParsedCangJieLightFile`，仅 psi parser 模块能构造，保存同次完整解析的 tree 与冻结 facts。
  `CangJieLightParser.parseFileWithFacts` 是其生产入口。
- `LightTree2Cfir` 的已解析文件 API 要求该结果；源码入口也委托到它。两条路径都一次发布 facts，
  raw 声明源种类取 parser facts，不再次按物理来源路径决定 `.cj.d` 语义。
  裸节点文件构建方法收窄为 module internal；宏表达式/声明 fragment API 保持各自片段契约。
- raw 测试基类和现有导入/枚举/参数/声明 owner/宏来源测试直接消费新 parser 结果。
  新增发布所有角色的测试，以及物理路径与 parser 源种类不同情况下无体 raw 状态的双向检查。
- 组合 **67239** 已终止 exit 1（5m14s）。完整 raw LightTree 模块 268 项中 17 项失败：
  5 个声明文件测试的 session 缺失 `CfirLanguageSettingsComponent`；另有四个 golden 场景在嵌套
  runner 中重复报错共 12 条（interfaceDeclaration、forWithPatternGuard、featuresDirective、
  specialExpressionsPreserve）。后四类尚未做基线差分，不能声称是已有问题或改测试期望绕过。
  新增角色/源种类两项通过。定向 declaration owner 6/6（16:47:06.740Z）、source-module 5/5
  （16:47:10.852Z）、CLI 22/22（16:47:28.622Z）、macro execution 27/27（16:47:31.722Z）通过。
- 已在共享 raw 测试 session 工厂显式注册语言配置，并允许调用者传入真实语言版本；不在
  生产 LightTree2Cfir 中兜底未配置的 session。修改发生在该作业 raw 测试结束后。
  后续同范围验证 **61714** 已终止 exit 1（2m18s），完整 raw 模块 268 项中 256 passed、12 failed，
  共享 session 语言配置导致的 5 个失败已修复；余下仍是上述四个 golden 场景的重复报告。
  新鲜定向 XML：声明源种类/角色 12/12（16:49:55.153Z）、参数注解 2/2（16:49:58.204Z）、
  枚举注解 8/8（16:49:58.235Z）、导入 7/7（16:49:58.359Z）。随后 owner 6/6、source-module 5/5、
  CLI 22/22、macro execution 27/27，均无失败/错误/跳过。新 public parser 结果和全部文件 raw
  调用点已通过生产编译与这些契约验证；全量 raw 验收仍未通过。
  当前无运行作业，不再轮询 67239/61714。下一步仍需审计四个 golden 差异，不能更新期望掩盖问题；
  主线继续迁移剩余 6 个 CFIR PARSE 规则及原始文件错误主锚、语言版本门。

### 原文件语法错误位置归属

- 官方 v1.1.3 `CheckCJMPModifiers` 的三种文件角色错误均报告 `*currentFile`。
  先前宏 CLI 将这些错误与普通生成文本错误一起定位到宏调用，未保留原文件起点。
- `ParsingDiagnostic.Origin` 明确区分 INPUT / ORIGINAL_FILE；归属在共享 parser 的文件角色
  报告点确定，不用诊断消息或偏移猜测。LightTree listener 改为转交完整结构化诊断，PSI
  根据类型化文件错误节点发布同一归属；宏构造注册表保留原始生成输入偏移与归属。
- 文件角色 element type 实现 IntelliJ `ICompositeElementType`，实体 AST 创建原生
  `PsiErrorElementImpl` 子类，仍为标准 `TokenType.ERROR_ELEMENT` / `PsiErrorElement`。
  这样 IDE 及既有错误访问协议保留，同时 LightTree 中也有明确的文法类型。
- CLI 对 ORIGINAL_FILE 错误报告宿主源文件零偏移；普通生成片段错误继续报告宏调用范围。
  未增加 CFIR 诊断工厂；fragment parser 算法版本递增至 7，旧缓存不能丢失此归属。
- 增加双树诊断归属、原生 AST 错误类型和普通错误不被误分类的验证；既有真实宏 CJO 的
  CLI 文件角色错误覆盖 PSI/light-tree、STRICT/DEGRADED、裸名/全名及 LF/CRLF，主位置改为
  原文件第 1 行第 1 列；宏生产 parser 的混合角色事务同时检查归属。
- 版本 7 验证 **56565** / queue `9a6a8819` 已终止，exit 0，10m29s；已核对 queue DONE
  和新鲜 XML：parser 12/12、MatchPattern 4/4、TokenBackedMacroFragmentParser 5/5、raw
  状态/角色 13/13、迁移 fixture PSI/light-tree 各 1 项通过（WithoutAliasExpansion 1 项按
  配置跳过）、CLI 23/23、生产 macro execution 27/27，均无 failure/error。
  XML 范围 2026-10-05T04:15:35.012Z–04:17:31.265Z。当前无运行验证作业，不再轮询 56565。
  整体 P0–P10 和剩余五项语法迁移仍未完成。
- 只读审查 `cjmp_syntax_origin_gate` 已核验该批源码和当前 IntelliJ 253.29346.379 字节码，
  未发现实质缺陷：原生 AST 错误构造/访问协议、两种 parser 的 origin 传递、宏转交与 CLI
  映射均有直接证据。专用 LightTree 错误仅在文件根，raw 文件声明收集不会误建它。
  Kotlin 提供递归错误监听框架，但没有 CJMP/原文件宏错误协议；这里是已取证的仓颉语义
  扩展，不是 Kotlin 原样实现或全框架对齐验收。
- 后续框架审查确认 IDE 增量比较不将专用 LightTree type 与实体原生 ERROR_ELEMENT 作为
  同一错误节点身份复用，可能替换该错误节点；三个固定类型不增长注册表，且文件起点范围
  无冻结声明绝对偏移问题，但尚未运行 IDE 编辑/增量/stub/cache 验证，不据此闭合整体协议。
  每条声明动态 element type 或 TokenWrapper 没有获准，不能作为后续整声明范围的兼容补丁。

### 剩余五项语法迁移的当前取证

详见 [cjmp-parsing-remaining-rules-evidence-20261005.md](cjmp-parsing-remaining-rules-evidence-20261005.md)。
官方 authority / Kotlin-framework 两项只读考证已完成，不能把该证据当作实现或验收完成。

- 普通原始声明需真实容器签名/成员事实。官方宏替换解析只继承实际 scope、primary 名字、
  carrier 自身 modifiers/annotations、文件及编译选项；不继承父角色/abstract/mirror/成员表，
  不重跑原父检查。当前重解析缺真实 scope 等入口事实，不能扩大为完整父属性补检。
- Native v1.1.3 与 alpha.20 在混合文件、common 外层和参数检查上不同；只有 enabled 布尔
  不足以表达版本。Mirror parser builtin provenance 与当前平台 ClassId 延迟身份也需考证版本。
- 标准 ERROR 整声明包裹会使现有直接 PSI/raw 声明遍历丢成员；声明范围方案尚未获准。
  禁止 token 重扫、CFIR status 重建、伪造 class 模板或动态类型/TokenWrapper 兼容补丁。
- authority 同时确认现有 COMMON_OPEN_CLASS_NO_INIT、abstract sema 两族仍有不完整实现，
  不能随 parser 工厂删掉或把语法限制简单改名为 sema 工厂。
- authority 已完成 builtin/version 与宏生产链考证；test writer 已运行官方 cjc 1.1.3 的
  29 项探针，命令、源码及原始 JSON 在上述探针目录，矩阵文档为
  `docs/cjmp-parsing-remaining-tests-20261005.md`。未修改现有 fixture，也未运行仓库 Gradle。
- 三种 file-role 错误及空首行探针均为 1:1–1:2；已修正 reporter 的 ORIGINAL_FILE 显示范围，
  保留 AST/DTO 0..0 和 INPUT 映射。handle 44021 / queue fc106896 已 exit 0（2m36s），
  新鲜 XML parser 12、CLI 23、宏生产 27，均 0 failure/error/skipped；UTC 时间范围
  2026-10-05T04:40:36.020Z–04:41:06.532Z。独立 gate 审查未发现实质缺陷。
  后续 handle 52955 / queue 05b20046 已 exit 0（1m10s），CLI 新鲜 XML 24/24，全部无失败，
  UTC 2026-10-05T04:47:41.534Z；新增普通生成错误完整宏调用首尾断言，覆盖 PSI/LightTree、
  STRICT/DEGRADED、裸/限定名、LF/CRLF 16 组合，原文件错误另一组 16 组合仍保留。
- ForeignAndAnnotationParsingTest 的裸/限定 JavaMirror 身份期望已依据官方修正为 true/false；
  实际 parser 的 DOT/末段平台名分派仍与官方存在差异，不将期望修正当作实现完成。
  handle 31768 / queue 0d2f9878 已 exit 0（26s）：该类 10/10、CjmpFunctionParsingTest 12/12，
  新鲜 XML UTC 2026-10-05T04:49:03.479Z–04:49:03.480Z，均 0 failure/error/skipped。
  当前无运行 Gradle；不再轮询这些 terminal handles。source 输入冻结已解除。
- authority 正继续只读审计剩余迁移的 version/builtin 签名事实发布及标准 ERROR 包裹的
  PSI/LightTree 声明保留边界；未开始五规则实现，也没有闭合问题或更新总体百分比。

### 2026-10-05 精确版本身份与 alpha.20 文件角色门的新输入

上一批 terminal 31768 后新增源码，旧绿色结果不证明本批输入。
LanguageVersion 保留 Kotlin 同层 enum/parse/settings owner，现登记全部本地 46 tag 身份及原有
3 个未在本地 tag 出现的稳定常量，共 49 项。保留前导零、历史 `1.1.0.beta.0`、beta.rc/rc1/rc2；
未知后缀拒绝，不虚构稳定 1.2.0/1.3.0。两段稳定输入仍映射 patch=0 的稳定身份。
39 个预发布身份不再因排在稳定版之前而误判稳定；LATEST_STABLE/DEFAULT 保持 1.1.3。

自然身份顺序不代表全部历史行为的单调演进：固定 v1.1.0-beta.1 仍禁 CJMP 泛型，
alpha.66 已改为 Frozen 判据。早期 keyword/generic/后置检查需要按真实谱系继续迁移；
枚举能解析不证明完整语义支持，P0 仍未完成。Version.cpp 的构建变量也不能以 tag 冒充二进制版本输出。

新增 CjmpMixedCommonSpecificDeclarations（固定提交 7a9258a，2026-05-25；alpha.20 首现），
从统一 settings 投影到 Compilation，再进入 PSI/LightTree/宏共享 parser。任一 CJMP 模式
均接受两种角色及混合文件；两种模式全关时按官方报告原文件 parse_unexpected_cjmp_decl，
使用第四个固定 file-role 类型及已有 ORIGINAL_FILE 范围协议。宏算法版本升为 8，缓存签名
包含完整 settings 身份。未改变 CFIR 的语义工厂来伪装该语法门。

common 增加 7 项身份/版本门测试；parser 新增两入口角色/模式矩阵；CLI 新增精确 alpha
配置到语法诊断的生产测试；宏角色事务矩阵扩为稳定版、alpha.19、alpha.20 三个真实身份。
mapper 已只读检查生产 owner 和自然排序，无阻断项；其指出的 alpha.19 有效关闭集合错误
已修正为 emptySet，没有修改既有 owner 来迁就测试。

首批 handle **38560** / queue **1bd01887** 已 terminal，exit 1（6m26s）；handle 已不存在，
queue DONE 与新鲜 XML 确认终止，没有因失去输出重启。完整 common 50/50（版本类20项）、
config 2/2、PSI 13+10、TokenBacked 5/5、CLI 25/25 均无失败，宏生产 27 项中 1 项失败。
新鲜 XML UTC 2026-10-05T05:02:06.928Z–05:08:06.598Z。
失败为新宏矩阵的预期错误：parsingFactsForSurface 返回原文件当前事务快照，同文件允许
混合后两个 surface 都应看到 BOTH，不能期待第一项仍看到 COMMON/SPECIFIC 片段快照。
已核对 registry 的 IdentityHashMap 文件 owner 和该方法契约，仅修正测试，不修改生产来迁就断言。
定向重验 handle **55515** / queue **f25b62ab** 已 exit 0（2m6s），queue DONE；宏生产类
新鲜 XML 27/27，0 failure/error/skipped，UTC 2026-10-05T08:48:52.022Z。
本批生产输入的 common/config/PSI/providers/CLI 结果沿用前述新鲜成功报告，宏测试修正后
以此新结果替换失败报告；没有再次改动生产源码。当前无运行 Gradle，不再轮询 terminal handles。
源码冻结解除。定向验证不证明完整 CFIR/LL 或全部历史语义完成，不能增加总体闭环百分比。

当前 CFIR DiagnosticsList 仍定义五项 PARSE_*，未宣称语法迁移完成。下一批先解决完整
声明 ERROR 的真实容器消费与 stub/身份协议，再按 native annotation→modifier→signature→body
生命周期接剩余规则；不能直接删除 checker 造成规则丢失。authority/gate/test-writer 正只读
核对实际 nested nominal grammar 及真实整树 stub 往返入口，尚未改相关实现。

另外已完成五规则前置审计：CjBuiltInAnnotation 当前为 registry class/facade，不是缺 JavaMirror
的 enum；真实 raw 发布将 kind/origin/identity 清空，后续 resolver 才设置。shared parseAnnotations
返回 Unit，未向 modifier→class signature→body 发布已解析 mirror kind，这是普通父语法事实缺口。
完整 ERROR 包裹会影响 direct declarations/typed getters、两种 raw 遍历及物理 parent 身份；
IntelliJ stub walker 会遍历 ERROR 内的声明但将 stub 标为 dangling，不能笼统说 stub 必丢。
统一实际容器消费需要仅穿透 ERROR，不吸入嵌套类/函数体/宏输入；现有 primary ctor 宽递归
也需替换，不能拿它补漏。该完整容器迁移尚未实现，不使用动态 element type 或绝对 range 绕过。

### 2026-10-05 nominal scope 定位与实际构造器 owner 的新输入

上一批 55515 terminal 后新增源码，旧绿色快照不能作为本批验收。
authority 已新增 7 份独立 cjc 1.1.3 `authority-*` 探针，原 29 份不变，详情在测试矩阵文档。
native 的 nested nominal / function-local nominal scope 主范围只标关键字；旧 parser 的整
声明 ERROR 包裹本身不对位，因此不能为保留这个错误范围先引入统一透明层。

本批 shared nominal parser 在真实 member scope / LOCAL scope 的关键字处报告，完成的
声明继续作为真实 body 直接子节点；取消旧整声明包裹及 nested shouldReportError=false 抑制。
LOCAL ClassParser 仍拒绝 local nominal 的语言使用，只改为保留其错误声明结构和关键字范围。
宏无括号 LOCAL 输入继承实际 declaration mode；ALL/quote 不猜父属性。算法版本升为 9。

PSI primaryConstructor 及 LightTree primary 查找改为实际 own body 直接 PRIMARY_CONSTRUCTOR，
删除宽 descendants / FUNC fallback。Inner、函数体 Local 及括号宏 Input 的构造器不属于 Outer；
macro init carrier 不计显式构造器已由独立 common 编译实证。宏实际替换后生成字段的完整
语法 scope/primaryName 生产链仍未闭合，不据此宣称全部构造器宏能力完成。

新增 PSI 4 项覆盖 13 种 nested scope、关键字定位、兄弟顺序和三种 ctor owner 隔离；新增 raw
4 项覆盖 PSI/LightTree NORMAL/LAZY 的 Outer/Inner/Following 构造器和成员字段归属。
当前验证 handle **15215** / queue **fad50ed1**（wrapper PID 40168）已启动并观察为 live：
完整 psi:test；raw 的 NestedNominal / PrimaryConstructorMemberField / DeclarationMode / EnumConstructorAnnotation；
CLI/宏生产两类。源码输入冻结，尚无 terminal 结果；后续观察同一作业，不因超时重启。
whole-declaration ERROR 透明、实际 stub 整树恢复及五项 CFIR PARSE 工厂迁移仍未完成。

用户随后指出作用域英文硬编码；已将六种作用域显示名称统一移入中英文 parsing.scope.*
资源，parser 按实际关键字选择资源，不再特判英文 function。同步更新 scope 测试期望。
该新输入发生在 15215 尚 live 时，因此不能将其旧作业结果作为本次输入验收。
上述两个作业均已 terminal exit 1。15215 的 raw NestedNominal 4 项在第 43 行
假定非法 Inner 已 lower 为 CfirClass 处失败（Outer 隐式构造器断言已通过）；
其余 raw 三类新鲜 XML 25/25、CLI 25/25、宏 27/27 通过，
但输入中途变化，不能据此宣称本批全部验收。90887 的 scope 测试失败是继承 JUnit 3 的
三字符串 assertEquals 重载把源码误作 actual；已明确调用 kotlin.test.assertEquals，未改变期望。
随后验证 84726 揭示相同问题的另外两处断言，修正后 **49473** terminal exit 0（24s）。
新鲜 NestedNominalParsingTest XML 4/4，0 failure/error/skip，2026-10-05T09:15:23.303Z。
localized scope、13 种关键字范围及三个 owner 隔离测试已通过；raw 的非法 nested nominal
表示仍待官方语义和 Kotlin 框架独立复核，不能先修改生产或删断言迎合当前结果。

独立复核确认第 43 行要求合法 Inner CfirClass 是错误测试契约。官方 nested nominal 设置
HAS_BROKEN，但保留 parser ClassDecl；不能据此发布合法 ClassId。双 raw 的顶层检查位于
ClassId 创建之前，保留 CfirInvalidDeclaration 的完整源码与 reason，方向正确。测试已改为
检查 invalid Inner 完整范围/名字/兄弟顺序、Outer 无字段或参数提升，以及 Following 的真实
显式构造器与成员字段。1828 terminal 因继承 JUnit assertNotNull 返回 Unit 而测试编译失败；
明确调用 kotlin.test.assertNotNull 后，**85085** terminal exit 0（15s），四类 raw 新鲜 XML
29/29，0 failure/error/skip，2026-10-05T09:19:55.501Z–09:19:59.434Z。

gate 另发现 v9 nominal scope 报告时机未对齐：官方 class/interface 在 signature 后检查，
HAS_BROKEN 则跳过；enum/struct/extend 则在 signature 前检查。当前全部 keyword 处立即报告
会为缺名 class/interface 增报。下一批需保留 keyword 起止 marker 并按实际种类和 signature
文法事实选择报告阶段，不能以所有 parser error 代替 HAS_BROKEN，也不能扩大为整声明范围。
当前尚未修正该缺陷，正由 authority 补固定 1.1.3 malformed-signature 探针。

#### v10：nominal scope 的声明自身破损事实与检查阶段

authority 新增 15 份独立 `authority-broken-*` 官方 1.1.3 探针。fixed 源码与 JSON 证明：
只有 class/interface 缺 identifier 会将 nominal 自身置 HAS_BROKEN 并抑制 scope；generic、
继承和 where 子节点错误不传播为 owner 破损，仍需 scope。enum/struct 则先由 scope 置
HAS_BROKEN，缺名只报 scope；extend 空目标报 scope 和 expected type。

共享 parseIdentifier 现返回实际 identifier 消费成功事实，其他调用可忽略返回值。
parseClass 保留 keyword 起止 marker：class/interface 完整签名后按此事实报告，另外三类
在 signature 前报告；errorBefore 仅闭合关键字范围，两个 marker 所有分支均完成或 drop，
也尊重 shouldReportError。macro 算法版本升至 10。新增测试循环覆盖 15 份输入、完整双树
diagnostic 相等、scope 范围和 following 声明保留。

当前验证 **36726** 已实际观察为 live：上述四类 PSI parser、四类 raw，以及 CLI/宏两类。
最近输出已进入 cfir-serialization 编译，尚无 terminal 结果，源码与测试输入冻结。
gate 已确认检查阶段和 marker 原生 API 行为；另揭示 enum/struct identifier-only context
suppression 在非 recovery token 上仍会消费，而 native 不消费。下一批必须替换为无消息
at(IDENTIFIER) 成功后 advance 的原生检查，不以 context suppression 当作恢复策略；尚未实施，不能声称
任意非法名称已对齐。此外测试还缺官方 diagnostic 总数和延迟后真实签名节点 getter 验收。
gate 后续读到无消息 expect(IDENTIFIER) 的孤立反引号分支会消费后返回 false；因此不能使用
该重载声称完全无失败消费。必须直接使用 at 的 soft-keyword 识别，仅成功时 advance。

36726 已 terminal exit 0（8m41s）：PSI 四类 33/33、raw 四类 29/29、CLI 25/25、宏 27/27，
新鲜 XML 2026-10-05T09:34:57.393Z–09:35:52.800Z，均无 failure/error/skip。源码冻结解除。
此结果只验证 v10 输入，不证明还未覆盖的任意非法名称或 native 全部诊断数量。
随后已按 gate 核对替换为 at(IDENTIFIER) 成功才 advance 的实际 name 解析，macro 版本升至 11；
该新输入尚未验收。test writer 正增补官方诊断总数、完整签名结构与 suppression marker 平衡。
无泛型 where 当前仍先解析约束再报错，和 native 只报 where 并返回不同；正核对共享四类调用
以及 Kotlin helper 的同层职责，不能先修改测试数量迎合当前 parser。

无泛型 where 的共享 helper 已按 fixed native 核对结果实现：只消费/报告 WHERE 并返回，
不解析约束；有自身泛型的路径仍正常解析。函数、secondary/primary ctor、宏声明输入均有
独立官方 probe；资源消息统一有中英文。新增四种无泛型 where 的双树测试，要求唯一错误、
WHERE 精确范围、真实 body/after/Following 和无虚构约束节点。完整签名结构、shouldReportError
双值 marker 平衡及十五种 broken-signature 的官方总数/范围断言也已写入。
验证 **66347** 已 terminal exit 1（7m4s）：四类 PSI 36 项，两项失败。追加验证 **46918**
也已 terminal exit 1（59s），NestedNominal 八项中相同两项失败。不得继续轮询这些 handle。
实际差异为缺名错误范围断言遗漏 IntelliJ whitespace balancing，以及 primary constructor 缺少
自身泛型和 guarded WHERE 调用。完整签名结构及 shouldReportError marker 测试已通过。

独立 range gate 已核对安装 SDK 253.29346.379 的字节码：两种树构建都先 balanceWhiteSpaces；
ErrorItem 使用 DEFAULT_RIGHT_BINDER，回绑到前一 production 允许的空白左界，终点等于起点。
缺名因此落在关键字末尾，缺 GT 落在 TYPE_PARAMETER 末尾；缺 type 的 TYPE_REFERENCE marker
阻止回绑，仍位于 brace。十五份精确范围已按该机制修正，不能统一减一或接受多种范围。

官方主构造器证据见 `.workbuddy/tmp/cjmp_probe113/review-20261005/remaining-parsing/AUTHORITY-PRIMARY-SIGNATURE.md`。
主构造器自身泛型由 parser 保留，再由 sema 禁止；不能用 owner 泛型或固定 false 代替。
当前 v12 已补 own type parameter list、共享 WHERE guard，以及普通/const/宏输入的 LT 分派；
新增验证尚待运行。native 的其他主构造器恢复起始符、名称识别和 return type 报告仍未完整迁移。
五项 CFIR PARSE_* 诊断尚未迁移完成；本批不改变总体闭环百分比。

v12 focused PSI handle **90601** 已 terminal exit 1（11m56s），日志
`.workbuddy/tmp/primary-signature-v12-psi.log`。新鲜 XML UTC 2026-10-05T14:25:29.865Z–14:25:33.322Z：
NestedNominal 9 项两失败，CjmpFunction 13/13、EnumConstructorAnnotation 5/5。
命令把第四类误写为不存在的 ForeignDirectiveParsingTest，故实际仅执行上述 27 项；
下一次须选择真实 ForeignAndAnnotationParsingTest，不能声称四类通过。
两失败均需按已有结构契约检查：宏输入不是宿主 declarations 直接成员，TYPE_CONSTRAINT_LIST
在 WHERE 消费之后建立，不含 WHERE。缺名/GT 范围修正及 primary 的 guarded WHERE 已实际通过。
生产输入冻结解除，测试契约正依据源码纠正，尚无 v12 全部绿色结果。
LT/LPAR 与 own 泛型 WHERE 静态 gate 通过；未覆盖的缺参数列表无消费恢复、其它 primary
起始符/名称以及 return type 仍需继续修复，不能用本批窄范围结果声称完整迁移。

下一批 return/recovery 取证已完成：
`AUTHORITY-CONSTRUCTOR-RETURN-RECOVERY.md` 与 `CONSTRUCTOR-RETURN-RECOVERY-TEST-MATRIX.md`
均在上述 remaining-parsing 证据目录。官方 primary/secondary 返回类型诊断在 parser 后置报告，
锚点仅实际类型；body/after/own 泛型 WHERE 完整保留。PSI constructed-result getter 与双 raw
implicit return type 不因此改变。缺 LPAREN 不消费 body/WHERE；现有恢复路径仍需改。

修正上述两个测试契约后，**24003** 已 terminal exit 0（2m4s）：真实四类 PSI 新鲜 XML
37/37，UTC 2026-10-05T14:30:16.645Z–14:30:19.979Z，0 failure/error/skip。
NestedNominal 9、CjmpFunction 13、ForeignAndAnnotation 10、EnumConstructorAnnotation 5。
此结果证明 v12 已覆盖的 own generic/WHERE/宏 const 签名与范围，不证明尚未迁移的其他规则。
后续 v12 raw/CLI/macro 定向验证 handle **47547** 已 terminal exit 0（4m56s），日志
`.workbuddy/tmp/primary-signature-v12-raw-cli-macro.log`。四类 raw 新鲜 XML 29/29，
UTC 2026-10-05T14:35:08.290Z–14:35:17.288Z；CLI 25/25（14:36:32.350Z）、
宏 27/27（14:36:53.609Z），均 0 failure/error/skip。当前无 live Gradle，输入冻结解除。
这批只验证所选既有 raw/CLI/宏覆盖，不含未来 return/recovery 新用例与其完整 raw 语义验证。
下一步按上述独立 return/recovery 证据修共享构造器签名，先核对 parseInitFuncRest 的
finalizer 调用边界，避免把其非法返回类型错误误报为 constructor。全范围尚未完成。
官方 finalizer 独立探针补证：同样保留自身泛型、参数、return syntax、WHERE、body，
但非法返回类型消息必须使用 finalizer，泛型禁止由 sema_forbid_generic_finalizer 负责。
finalizer 缺 body 没有 common 豁免。因此共享签名不能仅用 isPrimary Boolean 推导诊断角色；
须保留 primary/secondary/finalizer 的真实声明类型，显示名称或消息仍来自资源，不能硬编码。

v13 生产已实现上述共享签名：主构造器消费真实名称后进入既有 parseInitFuncRest，
finalizer 显式传 FINALIZER，secondary 保留 SECONDARY_CONSTRUCTOR；未知类型为编程错误。
own generic 的恢复集合增加 WHERE，缺 LPAREN 只报告不消费；可选 COLON 类型在 WHERE
之前完整解析，正文后以保存的起止 marker 报告仅实际类型的错误，诊断关闭时两 marker 释放。
缺类型只有原类型解析错误，不新增非法返回类型；构造结果 API 和 raw 类型契约不改变。
parseInitFunctionBody 的 COLON 扫描与无 EOF 终止的 while 恢复已删除，三种消息均来自资源。
macro fragment 版本 13，静态 gate 已通过；PSI 与双 raw 新测试正在按独立证据写入，
尚未启动新验证，v12 绿色结果不能当作 v13 验收。
v13 PSI 新增五个测试方法、十九个独立场景已写入 NestedNominalParsingTest，包含七种
非法返回类型、一种缺类型、七种破损泛型/参数恢复、两种 finalizer 泛型/WHERE，以及
两份注释边界官方 probe。实际 type ERROR、完整 body/after/own bounds/宏输入及两树一致均有
严格断言；注释探针官方范围仅 Unit 四字符，证据 AUTHORITY-CONSTRUCTOR-RETURN-COMMENTS.md。
PSI test writer 已停止编辑；raw test writer 正在新的 ConstructorSignatureRawCfirTest.kt 中
验收 NORMAL/LAZY 的双入口 constructed-result 契约，完成后启动本批定向验证。
raw 新文件已完成并停止编辑：四个入口方法各运行十七个签名场景，验证自身/owner 泛型的
真实符号及 bound 来源、参数来源、结果类型契约、正文恢复与 after/Following。
当前 v13 合并验证 handle **6716** 已实际观察为 live，日志
`.workbuddy/tmp/constructor-signature-v13-psi-raw.log`，选择四类 PSI、五类 raw（含新签名类）。
输入已冻结，尚无 terminal 结果；只续查同一 handle，不因暂时无输出重启。
6716 后续已 terminal exit 1（18m29s）：PSI 42 项一失败，新 raw 4/4 失败、旧四类 raw 29/29。
新鲜 PSI NestedNominal XML UTC 2026-10-05T15:06:55.821Z、raw 15:07:26.098Z–15:07:30.640Z。
五个失败都是破损 own generic 的两个独立错误被合并成一个；不能减少期望数量。
return/missingtype/comment 与完整结构测试已通过，但 broken-signature 及各 raw 循环在第一份
失败处中止，不能声称其后续角色全部验证。输入冻结解除。
独立 insertion gate 已核验 installed PsiBuilderImpl.error：同 lexeme 的连续 ErrorItem 会直接
去重且不比较消息，因此 Missing GT 后的 Missing LPAREN 丢失。既有共享
AbstractCangJieParsing.errorWithoutAdvancing 使用独立空 ERROR marker；正核对其完整两树范围
与 Kotlin API 契约，确认后复用，不消费 WHERE/body、不增加虚假非零范围。
insertion gate 已确认该既有 helper 的空 ERROR StartMarker 使用两端 DEFAULT_RIGHT_BINDER，
保留零宽且 Heavy/Light 均逐节点发布。共享 rest 的两处缺参数列表已复用它，cache 版本 14；
没有更改错误数量或任何 fixture。新验证 handle **84886** 已实际启动，日志
`.workbuddy/tmp/constructor-signature-v14-psi-raw.log`，仍选择同四类 PSI、五类 raw。
输入重新冻结，尚无 terminal 结果；6716 已终止不要再轮询。
2026-10-06 续接实查：84886 handle 已不存在，旧 queue 18a08a9b owner PID 33788 不存在，
系统无 java 进程；旧日志停在 checkers 编译且无 terminal，XML 仍是 v13 失败快照。
因此上次运行已中断，不能将旧 RUNNING journal 或 v13 XML 视为 v14 完成证据。
保持原输入与旧日志，已重新启动同一 v14 定向组合，handle **98843**，新日志
`.workbuddy/tmp/constructor-signature-v14-resumed-20261006.log`。当前输入仍冻结，结果待核验。
98843 已 terminal exit 0（4m27s）：PSI 四类新鲜 XML 42/42，UTC
2026-10-06T00:20:10.053Z–00:20:11.910Z；raw 五类 33/33，00:20:35.950Z–00:20:45.167Z，
均 0 failure/error/skipped。所有 v14 PSI 新场景与 raw NORMAL/LAZY 十七场景循环本次实际完成。
旧 84886 无结果、v13 失败 XML 均不作验收。输入冻结解除；正补 CLI 两入口的非法返回类型
消息位置与终止/无CFIR语法factory/无CJO gate，再进行 frontend 定向验证。
2026-10-06 CLI gate 已完成：handle **6197** terminal exit 0（1m54s），日志
`.workbuddy/tmp/constructor-signature-v14-cli-20261006.log`。新鲜 XML 为
`CjmpTwoPhaseCompilationTest` 26/26（UTC 00:28:52.883Z）、
`FrontendMacroConstructionExecutionTest` 27/27（00:29:01.937Z），均无 failure/error/skipped。
新增方法实际完成 PSI/LightTree × LF/CRLF × primary/secondary/finalizer 的 12 个场景，
逐项验证角色资源消息、完整返回类型起止行列、编译失败、无 CFIR diagnostic factory、无 CJO。
这只闭合本批构造签名的定向 gate；剩余五项 CFIR PARSE 工厂迁移、完整 LL/CFIR 回归及
70 项总计划仍未完成，不改变整体完成百分比。当前无本批运行中的 Gradle handle。
等待期间独立只读审核已补充下一批设计：
`docs/cjmp-whole-declaration-error-container-audit-20261005.md` 给实际容器、严格owner、双raw及
整树stub恢复的完整方法清单；whole ERROR 透明及真正 fresh-file 无 AST gate 尚未实现。
2026-10-06 独立 mapper 已补入该文档的实际参数列表边界：参数 getter/owner、primary 字段
FQName 与 stub create、PSI/LT raw 参数身份、operator/foreign/macro 逗号位置均须纳入。
当前还存在 PSI primary 字段的同名函数后代兜底，以及 LT 参数列表的任意后代兜底；
不得保留这些路径冒充 ERROR 透明或 generated constructor 支持。
随后已按实际源码与独立 Kotlin mapper/gate 删除三处兜底：PSI primary 字段只读实际直接
primaryConstructor；LT 值参数和 foreign `...` 只读声明自身直接 VALUE_PARAMETER_LIST。
`ConstructorSignatureRawCfirTest` 三角色各新增缺 GT/LPAR 后正文保留局部函数的场景，
保持两条 parser 错误、自身零参数；NORMAL 核对 nested 参数自己的 symbol/type/source，
LAZY 不读取正文。每种入口/模式实际 case 从 17 增为 20，方法数仍为 4。
定向 raw 五类验证已启动 handle **92346**，日志
`.workbuddy/tmp/parameter-owner-no-descendant-20261006.log`，尚无 terminal 结果。
92346 已 terminal exit 0（2m3s）：五类 raw 新鲜 XML 33/33，UTC
2026-10-06T00:35:58.618Z–00:36:03.159Z，均 0 failure/error/skipped。
新增三角色正文局部函数隔离场景在 PSI/LT NORMAL/LAZY 四入口均实际完成。
独立 gate 接受本批 owner 修正，但 whole ERROR 透明和 generated primary 明示身份仍未闭合。
新增 `WholeTreeStubRoundTripTest.kt` 已完成真实 whole-tree manager、独立 detached 树及
physical 文件正常 loader attach 的合法源码 baseline；独立 SDK gate 接受进入实际验证，
但该 baseline 尚不包含真实 whole ERROR 的 dangling=true 场景，不能代替完整迁移 gate。
实际服务替换必须用公开 Pico Class.name key unregister + Mock register，不能用实际抛
UnsupportedOperationException 的 replaceRegularServiceInstance/unregisterService/unregisterComponent(Class)。

本批组合 handle **3126** terminal exit 1：JVM 40900 native malloc 失败，
`hs_err_pid40900.log` 明确 native memory OOM，未完成 psi compileTestKotlin、未执行新测试。
日志 `.workbuddy/tmp/whole-stub-and-parameter-macro-20261006.log`。
低内存 in-process 重试 terminal exit 1，日志
`.workbuddy/tmp/whole-stub-and-parameter-macro-low-memory-20261006.log`：新 JVM 启动时
G1 mmap 255852544 字节失败，Windows errno1455；不是 Kotlin 编译错误或测试失败。
再显式 Xms32m 的启动尝试同样已 terminal exit 1，日志
`.workbuddy/tmp/whole-stub-and-parameter-macro-low-startup-20261006.log`，该日志仍待读取；
随后只读 pwsh 命令本身退出 -1073741819，未返回内容。当前没有上述验证的 live handle。
此前读取系统 FreeVirtualMemory 从约 1041296KB 降至 717760KB；不修改测试断言、
Gradle 文件或全局系统资源来绕过这项环境失败。

源码与 Kotlin mapper 另外已确定 `CjFunctionImpl.isLocal` 的 detached 缺口：file root.psi
为空时直接 parent PSI 判断把顶层函数误判 local。生产已增加实际直接 parentStub 类型判断，
file/CLASS_BODY/INTERFACE_BODY/ENUM_BODY/FOREIGN_BODY 均非 local；不读 parentStub.psi、
不找祖先、不改变 isTopLevel 持久化事实。struct/extend 实际共用 CLASS_BODY，独立 gate
确认种类完整。该生产修正和新 stub harness 尚未获得编译/运行通过证据。
测试 writer 最后依据实际 primary 字段/FQName createStub 协议补入三个独立参数 FQName
查询 gate：immutable/mutable 为 `whole_tree.Owner` 的字段限定名，ordinary 为 null；
detached/fresh 的原有无 AST 守卫保留。文件已冻结，未运行 Gradle；最后 diff check 的
pwsh 启动因 System.OutOfMemoryException 未执行，续接还需补查格式。
下次必须先核实内存和 low-startup 日志，再以保存的严格 gate 运行；不能把旧 XML 当新证据。
续接已实际读取 low-startup 日志：queue wrapper 自身启动 JVM 即 G1 mmap 255852544 字节
失败，errno1455，`hs_err_pid54952.log`，不是任务运行失败。系统内存已恢复，FreeVirtualMemory
实查 5798396KB；本批格式检查通过。使用原 1g 模板重新启动组合 handle **39562**，
日志 `.workbuddy/tmp/whole-stub-and-parameter-macro-resumed-20261006.log`，当前 live、输入冻结。
只读 mapper 核对 CjFile/foreign/body/constructor/parameter getter 均已有实际 stub 分支；
等待真正 XML，不因预期错误而新增 AST 兜底。useScope、正文/defaultValue、token/textOffset
不在本次 no-AST getter gate 内，不能推广通过范围。
39562 已 terminal exit 1；`hs_err_pid54384.log` 再次确认 C2 CompilerThread0 原生
Chunk::new malloc 9193624 字节失败，崩溃时物理可用仅 396MB。未产生新测试 XML，
不得当编译或测试失败；没有继续轮询该已终止 handle。
安装的 Corretto 17.0.17 已实际找到，按仓库 JDK 17 约定，以该 java 运行 queue jar，
并通过本次命令 `-Dorg.gradle.java.home` 指定同一 JDK，其他验证范围保持原样。
新组合 handle **34542** live，日志 `.workbuddy/tmp/whole-stub-and-parameter-macro-jdk17-20261006.log`；
输入继续冻结。未修改 Gradle 文件/全局 Java 设置，未终止无关进程。
独立 native authority 完整 modifier scope/projection 表及 handler 例外已补入共享 parsing
设计文档：EXTEND 不走通用 EMPTY projection，参数/宏 carrier/enum constructor各有专用
路径，raw INTERNAL/OVERRIDE/INOUT 与 occurrence 位置信息仍有缺口。下一批实施必须
按固定官方表处理，不把所有声明提前 file-role clear，也不以 ModifierKind 冒充 Attribute。
34542 也已 terminal exit 1，日志仅到“JVM changed，计算 task graph”，未完成任务执行，
未产生测试 XML。`daemon-47024.out.log` 明确启动运行时是 Corretto 17.0.17，随后无
terminal 原因记录；实查系统无 java 进程。当前没有找到该 PID 的 hs_err，不能直接将此
次归因于 C2/内存，也不能宣称 JDK 17 切换修复了问题。保留日志继续调查 abrupt termination。
当前无上述 live Gradle handle；不要再轮询 39562/34542，也不要使用旧测试 XML 验收新 stub 修正。
后续只读系统取证发现 OneDrive.Sync.Service PID37248 的 private bytes 约 34.4GB，
已请用户释放编译任务之外的系统内存，用户回复“我会释放内存”；未终止该应用。
系统可用提交内存随后回升至约 5.3GB，以 JDK17/in-process 启动窄 stub gate，handle
**52893** terminal exit1（15s），这次有明确 setup 错误：
`:gradle-settings-conventions:jvm-toolchain-provisioning` 依赖最低 JVM21，JVM17不能解析。
因此当前构建运行时不能照旧 AGENTS 的 JDK17 描述设置，不能改低依赖要求冒充兼容。
本机 Corretto21.0.10 已实际找到并用于 queue launcher 和 `org.gradle.java.home`；
单 worker、1g heap/in-process Kotlin，窄任务仅 `:psi:test --tests '*WholeTreeStubRoundTripTest*'`。
新 handle **37088** live，日志 `.workbuddy/tmp/whole-stub-jdk21-in-process-20261006.log`。
JVM21 已通过 setup并进入实际依赖任务；输入冻结，尚无新测试 XML或terminal。
52893 不再轮询。原五类 raw 33/33仅证明兜底移除批，不证明随后 stub isLocal修改；
参数宏/CLI两类本批回归仍需在 stub gate之后执行。
待实施 effective attrs 的测试writer原生8探针已读完但尚未落新测试文件，按编译冻结已暂停。
已有本地 missing-body resource 缺原生 main-constructor/具名 function 消息，不能为测例编造期望。
37088 已 terminal exit1（3m38s），这次完成实际生产依赖编译并在 psi compileTestKotlin
报告新 harness 的 10 项错误，未执行测试。已修正可空 Path supplier、显式
getCoroutineScope() 与 inherited JUnit3 assertNotNull 返回 Unit 的值读取。
独立 SDK gate 通过实际缓存 descriptor/JAR 和官方 POM 查明 CoroutineScope 编译 API 缺口：
SDK 使用 com.intellij.platform:kotlinx-coroutines-core-jvm:1.10.1-intellij-5，POM scope为runtime。
版本目录已登记准确 SDK fork，dependencies:intellij-core 显式 api 暴露其公开签名依赖；
不引入不匹配的 org.jetbrains.kotlinx 版本，不扩大全部 SDK 的 compile 传递。
修正后同一窄 stub gate 已重新启动 handle **13926** live，日志
`.workbuddy/tmp/whole-stub-jdk21-api-fixed-20261006.log`，JDK21/in-process/1g模板，输入冻结。
未得到新 XML或terminal；37088 不再轮询。生产 isLocal 与 fresh/detached断言仍需实际验证。
`docs/cjmp-shared-parsing-declaration-flow-design-20261005.md` 给五项剩余 PARSE 规则的固定
native 生命周期。property abstract 必须在每成员返回后立即检查；raw modifiers 与 effective
attrs 必须分离，不能直接照搬当前checker顺序。
尤其文件角色失败确实返回整个空attrs，但 secondary init 的原始 COMMON 已在handler预置，
后续空attrs不会清该值；primary/function无此例外。当前早期 detector.clear 丢该事实，
下一批需按共享声明头发布顺序修复，不能只改clear成删除CJMP角色或仅新增Boolean兜底。
该 COMMON 例外已补独立实测：`AUTHORITY-EFFECTIVE-COMMON-INVALID-FILE.md` 八份新 probe
在真实 compileCommon=false 上下文中确认 ordinary/宏 init 只有文件角色错误，primary另缺体；
顶层 function另缺体，而普通class function可隐式ABSTRACT，不能用其一条错误误判COMMON保留。

### 2026-10-06 续接：整树 stub 与名义成员 FQName

先前 13926 已查明终止：工具 handle 不存在、queue f8db7b6f 的 PID44588 不存在。
daemon-46860.out.log 明确 client disconnection detected/canceling the build，
resolve 编译取消，BUILD FAILED in 9m10s（09:10:03）；不是新的源码或内存故障。
本轮使用 Corretto21.0.10、1g heap、单 worker、in-process Kotlin 续跑窄 stub gate。

99619 terminal exit1（3m3s）首次真正执行新整树测试，detached member.fqName 为 null。
共享 CjNamedDeclarationUtil.getParentFqName 解包 class body 后没有 nominal owner 分支，
所以原始 function stub 已写入 null；getter 和整树序列化的读写本身对称。
现按 Kotlin KtNamedDeclarationUtil.java:61 在 extend 专用名字域分支之后递归实际
CjTypeStatement/CjNamedFunction owner；普通 local block 仍不产生 FQName。
SOURCE_STUB_VERSION 由212升至213，淘汰旧缓存中已持久化的空名字。

整树测试新增序列化前原始 member stub 断言，保留 detached/fresh 两棵独立反序列化树、
真实物理文件正常 loader attach、逐 getter 无 AST 守卫。新增 NamedDeclarationFqNameTest
覆盖四类 nominal 成员、非法嵌套恢复结构、局部声明、foreign/extend 名字域。
72884 因新增测试 emptyList 的类型推断失败；73804 的5项名称测试通过，整树测试因新增
三字符串 assertEquals 误选继承的 JUnit3 message-first 重载而失败。已修测试类型和明确
kotlin.test 调用，没有削弱期望或改生产来适应测试。

8336 terminal exit0（22s），新鲜 XML：整树1/1（2026-10-06T01:37:50.838Z）、
名称边界5/5（01:37:52.034Z），均无 failure/error/skip。源码框架 gate 通过此修复，
只证明合法 baseline 所列 getter 的持久化和无 AST 查询；whole ERROR/dangling、
generated primary、lazy scope 与剩余五项 PARSE 迁移仍未完成。
随后参数宏/CLI组合21377已 terminal exit0（1m2s），新鲜 XML 参数宏2/2
（01:39:54.350Z）、CLI两段式26/26（01:40:16.441Z）、宏执行27/27
（01:40:20.967Z），均无 failure/error/skip。
完整 LL + CFIR 的91256 / queue187cd859已终止 exit1（7m59s）。LL 168项有5失败，
全部ContextCollectorSourceTestGenerated：主文件通过，file.copy()副本丢parsedFileFacts，
raw构建的真实事实断言失败。SDK PsiFileImpl.clone只复制copyable data并克隆既有树，不重跑parser。
已将不可变解析输入/结果改为PSI copyable：cjmpParsingContext、parserSourceKind、
parsedFileFacts、parserLanguageModuleName、parsedLanguageModuleName。完整parser开始清旧结果，
完成后重新发布；不在clearCaches清除，因为SDK clone末尾会清缓存。raw断言不删除，不制造NONE事实。
新增ParsingFileCopyTest覆盖24组角色/源种类/模块输入结果及副本隔离；不把它当stub持久化或IDE编辑失效验收。

同轮CFIR在testForeignFunctionCFuncLegalityPlaceholder暴露真实parser错误：合法参数尾逗号
被parseParameterSeparator拒绝，生产frontend在语义检查前退出。官方v1.1.3
ParseDecl.cpp:2495、ParserUtils.cpp:303–314、ParseType.cpp:191均使用允许尾逗号的协议。
已修共享separator并新增PSI/light参数列表回归，保留原interop fixture及四个诊断期望。
随后测试JVM PID42292原生malloc失败（hs_err_pid42292.log，C2 CompilerThread0，
系统物理可用605MB），CFIR只运行到控制台1729项/1失败/395跳过即中止；这不是完整全量结果。
两类真实错误与环境中止分别记录，不能把所有失败统称OOM或既有问题。

修正输入后3719已终止 exit1（9m31s）：LL ContextCollector6/6、原interop三入口各1/1、
trailing-comma3/3通过。剩余2失败来自新copy测试的独立builder树没有真实文件owner，
递归展开lazy body时抛ClassCastException，不是这两类生产修复失败。
测试现使用LightVirtualFile.setContent与AbstractFileViewProvider.onContentReload的真实重载协议。
35972进一步暴露测试对非physical文件过渡空树状态的错误假设：SDK长度一致性检查会同步建根。
现改为验证真实新root、newroot.psi、正文诊断、源码文本和新parser快照，保留原件/副本隔离断言。
50712已terminal exit0（21s），PSI四类12/12通过；没有手工setPsi、安装AST或跳过正文检查。

完整LL/CFIR及CLI重跑已启动handle16699。为避免已证实的C2 CompilerThread0原生内存失败，
仅该命令及其子进程使用JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1；没有修改机器配置/Gradle文件，
没有减少测试范围。仍使用JDK21、1g build heap、单worker、in-process Kotlin、禁用build cache。
16699已终止，完整LL168/168与frontend53/53通过，但CFIR仍因G1系统提交内存失败中止。
其后一次性768m/每20类fork的11466也已停止，最后daemon日志6537项/35失败/398跳过，
没有完整XML；宏测试失败尚待分类，不能全归为OOM。输入冻结解除。
当前状态以20261006交接为准，不得把定向通过当成全量验收。

### common 模式声明语法迁移

- 官方 v1.1.3 `MPParserImpl::CheckCJMPModifiersOf` 对 COMMON 的 VAR_WITH_PATTERN_DECL
  在实际根 pattern 范围报告 tuple/enum/wildcard；普通绑定和 specific 不在此禁止面。
- 共享 `PatternParser` 返回实际完成的根节点种类；变量文法据此报告 common 禁止模式，
  包含完整模式范围，保留模式子树与后续声明。没有按首 token、名字文本或 CFIR 类型猜测。
- quote/token 输入中的推测宏结构不触发该角色规则；禁用语言版本及错位文件角色按已有
  parser 上下文短路。新增 PSI/light-tree 精确范围、限定枚举、普通绑定、specific、quote 和门禁测试。
- 删除 `PARSE_CJMP_PATTERN_DECL` 的 CFIR 源定义、默认消息和 checker 报告点；fixture 保留
  原始源码，移除该 CFIR 语法期望。其余 5 个 CFIR PARSE 规则仍待迁移。
- **22414** 已终止 exit 0（4m31s），两组诊断生成完成，CFIR 与 Analysis 生成目录已无该工厂。
  新鲜共享 parser 11/11（16:59:36.102Z）、CLI 23/23（16:59:53.416Z）、macro execution 27/27
  （16:59:56.661Z）通过。CLI 新测试含 PSI/light-tree × LF/CRLF × 三种模式 12 个拒绝场景；
  宏生产 parser 也新增三种模式拒绝场景。
- 后续 **19805** 已终止 exit 0（1m）：迁移 fixture 的 PSI/light-tree 各 1 项通过，
  WithoutAliasExpansion 1 项按配置跳过；共享 parser 11/11（17:02:29.956Z）、MatchPattern 4/4
  （17:02:30.942Z）通过。Quote 过滤器没有匹配独立类，不能声称存在额外 quote 全类覆盖。
- 源码复核发现完整 pattern 范围错误包装使 PSI 根模式 getter 和 light raw 的直属子节点查找
  看不到内部已完成根模式。旧 light raw 会因此走合成 wildcard，违反用户禁止降级要求。
  已让两个根模式访问入口只穿透 parser ERROR 包装，不遍历任意嵌套 binding；实际模式与范围保留。
  未解析模式的旧 wildcard 分支及独立 error-pattern 模型仍待整体审计，不能声称已消除所有兜底。
- 新增 PSI 模式 getter 断言和双 raw builder 的 tuple/enum/wildcard 形状及源码范围验证。
  组合 **69770** / queue `3fc5e3ed` 已终止 exit 0（5m29s），2026-10-05 续接时其进程 handle
  已消失，已核对队列终止记录及新鲜 XML：共享 parser 11/11、MatchPattern 4/4、raw 状态/角色
  13/13、迁移 fixture PSI/light-tree 各 1 项通过（WithoutAliasExpansion 1 项按配置跳过）、
  CLI 23/23、macro execution 27/27。对应 XML 范围为 17:09:29.905Z–17:11:02.924Z。
  随后补充 fragment parser 算法版本 6，禁止复用迁移前该模式片段的成功缓存；该版本修改晚于
  本作业 providers 编译，需要该作业终止后再验证最后输入。
  2026-10-05 同范围最终输入验证 **54988** / queue `f0ac847c` 已终止，exit 0，
  11m19s；已核对 queue DONE 和新鲜 XML：共享 parser 11/11、MatchPattern 4/4、
  raw 状态/角色 13/13、迁移 fixture PSI/light-tree 各 1 项通过（WithoutAliasExpansion
  1 项按配置跳过）、CLI 23/23、macro execution 27/27，均无 failure/error。
  XML 时间范围 2026-10-05T03:52:25.062Z–03:56:23.157Z；此批覆盖 parser 版本 6 的最后输入。
  当前无运行验证作业，不再轮询 54988 或 69770。
  广域 raw golden 四类差异仍待审计，P0–P10 尚未完成。

文件角色后续迁移必须由共享 parser 发布实际解析事实（含源文件种类），再由 raw 文件承载冻结值；
宏构造顺序事务继承宿主事实并接收实际完成解析后的角色变化，包括后续发现语法错误的结果。
不能从 CFIR status、重新扫描 token、
文件名排序或源码字符串反推角色。PSI 与 light-tree 都应先取得解析结果，再进行 raw payload 构建。
原始宿主文件角色错误应保留文件级锚；不能把生成模板偏移直接当宿主偏移。

用户明确覆盖历史 D13：CFIR 诊断只能承担语义诊断，CJMP `parse_*` 规则必须迁入 parsing。
完整 P0–P10 目标及 70 项验收口径不变，本轮局部迁移尚未计为问题闭环。

- 已将无体 common/specific 函数缺返回类型检查移入 `CangJieParsing.parseFunction`，
  PSI 与 light-tree 共用；在 `func` 前报告零宽错误，保留关键字和声明结构。
- 已删除两个对应的 CFIR 源定义、默认消息及 checker 报告点，迁移两个语义 fixture 的旧标记。
- 新增 `CjmpFunctionParsingTest`，覆盖两种角色、错误位置、返回类型和函数体、
  后续声明边界，以及声明文件的 light-tree 入口。
- 首次验证 session **50314** 已终止，退出 1，耗时 6m47s：生产编译与两组诊断生成完成，
  PSI 测试因新增测试的可空类型写法编译失败。第二次 session **22037** 也已终止，退出 1，
  耗时 1m1s：本仓库 `assertNotNull` 不返回被检查的值，不能赋给函数变量。
  已改为 `requireNotNull`，两个失败均未执行测试，不能记为测试通过。
- 验证 session **31961** 已终止，退出 1，耗时 8m30s：新生产报告链编译通过。
  parser 5 项中 2 passed / 3 failed；两段式 CLI 20 项中 19 passed / 1 failed。
  XML 时间分别为 UTC 11:27:51.285 / 11:28:15.572。
  四处失败均是 `func` 前零宽错误被默认边界绑定到前导空白，偏移和列号小 1；
  合法签名、后续声明边界以及原有 19 个两段式测试通过。此轮不记为完整测试通过。
- 已设置错误 marker 两条边的 GREEDY_RIGHT_BINDER，让零宽节点保持在前导 trivia 后。
  验证 session **38602** 已成功终止，退出 0，耗时 11m59s：parser **5/5**，
  两段式 CLI **20/20**，无 skip/failure/error。
  XML 时间分别为 UTC 11:41:10.990 / 11:41:37.891；零宽位置和 CLI 拒绝输出已验证。
- 已删除另外四个无报告点的 CFIR 源定义和默认消息，修正 generator/checker 迁移说明及模式注释。
  现在尚余 **8 个 CFIR PARSE_ 源定义**；不是整族迁移完成。
- 验证作业 unified exec session **63875** 已成功终止，退出 0，耗时 5m42s。
  经 queue JAR 执行 `:cfir:checkers:generateCheckersComponents`
  `:analysis:analysis-api-cfir:generateDiagnostics :cfir:analysis-tests:test`
  `--tests '*CommonSpecific*.testCommonSpecificParseRulesCommon01'`
  `--tests '*CommonSpecific*.testCjmpSpecificFunctionMissingReturnType'`。
  两组诊断生成完成；两个语义 fixture 的 PSI/light-tree 四项全部通过，
  WithoutAliasExpansion 两项跳过，无 failure/error。XML 时间为 UTC 11:49:07.617–11:49:15.836。
  当前没有仍在运行的验证作业，不复用已结束的 session。
- 新核对官方 `v1.1.3` 的 `src/` 和 `include/`：
  `parse_specific_function_parameter_cannot_have_default_value`、`parse_expected_type_with_cjmp_var`、
  `parse_cjmp_generic_decl` 只有 `DiagnosticParser.def:272/274/279` 登记，没有生产报告点；
  `parse_unexpected_cjmp_decl` 连登记也不存在。当前仓库四条均无 checker 报告点、无 fixture 期望。
  已删除这四个 CFIR 源工厂并完成生成 API 更新，不给 parser 发明新的限制。
  已修正 `CfirCjmpSettings` 关于 NONE 模式报告 `parse_unexpected_cjmp_decl` 的错误注释；
  官方实际调用是 `ParseCJMPDecl.cpp:113/127` 的两条文件角色错位消息。
- 已接通 CLI `LightTree2Cfir` 的 parser 错误监听器和 PSI 错误遍历：共享
  `psi/parsing/ParsingErrorReporter.kt` 将语法消息直接送入 MessageCollector，
  frontend 在 raw 构建后发现语法错误即停止宏构造和 CJO 写出；不增加 CFIR 语法工厂。
  Kotlin 对位是 `LightTreeParsingErrorListener.kt` 和 `AnalyzerWithCompilerReport.reportSyntaxErrors`，
  按用户要求将消息所有权保持在 parsing；已由 38602 的 parser/CLI 测试验证。
- 官方 CheckFuncBody / CheckCtorBody / CheckPrimaryCtorBody 允许 common 只有签名。
  已修正 parser 的相应缺体判据，并增加合法签名、CRLF 位置及 CLI 拒绝语法错误的测试。

## 位置和保护边界

### 文件角色规则迁移（57bd317ea 后，未提交）

- 将 common/specific 文件错位规则迁入共享 parser，删除 `CfirCjmpFilePartChecker`
  及两条 CFIR 源诊断、默认消息和四个 fixture 的旧语法标记。尚余六条 PARSE_ 源定义。
- 新增 `CjmpParsingContext`：明确区分 syntax-only 与有编译调用的解析。
  编译调用携带独立的 compileCommon / compileSpecific 和语言 feature 门，不从源码猜测模式。
  官方依据为 v1.1.3 `ParseCJMPDecl.cpp::SetCompileOptions/CheckCJMPModifiers` 与
  `ParseModifiers.cpp::CheckDeclModifiers`；specific 先判断，无效声明不发布有效修饰符。
- 当前文件起点保存零宽错误，仅保留首个角色错误；两个谓词都成立时检查同文件角色混用，
  消息采用官方 `parse_common_and_specific_in_the_same_file`，不增加 CFIR 语法工厂。
- `CfirCjmpSettingsComponent` 新增两个独立谓词；旧 mode 枚举仍保留用于历史消费者，
  所有旧 mode 消费尚未完成审计，不能把本轮当作模式框架全部闭环。
- PSI 编译入口在源文件收集时写入 options，先于 features 的首次 AST 访问；
  light-tree 从 session 的同一组事实转换。测试 facade 按模块写入 PSI options。
  CjPsiFactory 从真实 context 继承，LightParser 三种文件入口接受同一 options；
  子 parser factory 同时保留文件 sourceKind 和 options。
- 新增 parser 三项测试（错位四组合、双谓词两顺序、成员/版本/语法-only）以及
  CLI 一个八组合拒绝测试，已经通过；这不等于宏展开后的所有角色规则已经完成验证。
- 验证作业 unified exec session **32257** 已终止，exit 1，14m28s，通过 queue JAR 执行两组诊断生成、
  `:psi:test :compiler:frontend:test :cfir:analysis-tests:test`，定向选择 parser、两段式 CLI
  及四个文件模式 fixture。本次 `--tests` 放在三个 test task 之后，只约束最后的 CFIR task，
  PSI/frontend 实际执行了完整模块测试，不能宣称该组合全绿。当前无 live 验证作业。
  后续需要逐 task 紧跟相应 `--tests`，不能重复本次过滤位置错误。
- 新鲜 XML：`CjmpFunctionParsingTest` **8/8**（UTC 12:20:47.749），
  `CjmpTwoPhaseCompilationTest` **21/21**（UTC 12:21:29.006），
  CFIR 四个模式 fixture PSI/light-tree **8 passed**、WithoutAliasExpansion **4 skipped**，
  无 failure/error（UTC 12:24:06.583–12:24:37.208）。两组诊断生成已完成，生成 API 中也只剩六条 PARSE_。
- 更宽模块实际结果：PSI 107 项、1 failed；frontend 118 项、3 failed。
  PSI 失败为 `ForeignAndAnnotationParsingTest.platformAnnotationSurfaceIsRetainedWithoutBuiltinKindFacade`
  第 137 行对 JavaMirror 的 `isBuiltInAnnotation` 断言。
  frontend 失败均为 `MacroConstructionArchitectureGuardTest`：rawBuildersCoverAllMacroSurfaceShapes
  未找到显式 IfAvailable/BuiltinNonMacroSurface 分支；semanticMacroPathDoesNotReferenceTextPatchExpansion
  扫到测试中的 expandedText；sourceProviderRegistrationUsesFinalRegistrarOnly 扫到测试中的 recordFile。
  这些失败尚未归因审计，不将它们称作已经证实的既有失败，也不修改测试来制造全绿。
- 续接重点：`MacroFragmentParserConfiguration.kt:248/256/269` 的 light-tree 宏重解析仍未传入编译 options，
  且宏片段的 parser 错误尚未接入完整报告/拒绝协议。原文件 checker 删除后，宏生成的错位角色
  不能继续依赖 CFIR 补报；必须完成共享 parser 的宏语境、文件角色继承和错误通道后才能验收整条规则。
  PSI 宏 factory 已继承真实源文件 options，但同样须核实拒绝与消息来源映射。
  六条遗留 PARSE_、版本门和全部旧 mode 消费继续迁移；70 项验收口径不变。

- 唯一实施目录：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。
- 分支 `codex/cjmp-framework-implementation`，HEAD `57bd317ea`。
  语法迁移检查点已提交：20 个文件，迁移两个返回类型规则、删除四个无报告点定义、
  接通 CLI parser 错误通道和测试；剩余八个 CFIR PARSE_ 定义仍待迁移。
  其余 pattern / LL / 发布迁移源码和文档仍为未提交改动。
- 应用默认目录仍是主仓；attach_worktree 返回无法确认 ownerless worktree 归属。
  不声称应用会话已切换；每条 shell 命令必须显式指定上述 workdir。
- 原仓只读，参考官方 tag `v1.1.3` / Kotlin，或使用已有 queue JAR。
- 保留原有 HTML 删除及旧探针原生产物；不 reset/stash/clean，不 `git add -A`。
- PowerShell 7，Get-Content 使用 UTF-8；作者编辑使用 apply_patch。

## 本轮实际改动

1. pattern 计划使用真实 binding；普通绑定不把外壳当 VAR，tuple 非变量停止并保留已接受前缀。
   acceptedBindings 由纯求值器裁决后发布，VAR 覆写不能再经旧 bind 重判为占用。
2. LL binding 路由到真实 root；phase updater 更新 root-owned 原有绑定。
   TYPES 解析/投影显式头，STATUS 完成原有子状态，IMPLICIT 处理仍为隐式类型的绑定。
3. LL source symbol provider 收集实际 binding；file declaration provider 同时索引其名称和文件。
   这是源码查找链的两层共享 owner 修正，不是 matcher 兜底。
4. pattern TYPES/BODY keeper 保存真实子声明；共享 CfirResolutionSnapshot 提供接口，实现保持私有。
   snapshot 捕获显式/dispatch receiver、原始实参列表、resolved/error 映射、context 独占表达式。
   explicitReceiver 的 replace API 由 tree-generator 定义生成，不能手改 gen。
5. checker 只检查实际访问过的 binding；snapshot 读取投影保留真实 root 与 binding 身份。

这些仍使用旧可变 CJMP storage，**不能作为九步不可变架构验收**。最终仍须删除旧 storage/write/clear、
live snapshot、兄弟发布、common shadow 属性和 ThreadLocal 部分结果，接通真实 merge seed、
默认来源与 actualized 成员图，并迁移全部消费者和测试。

## 已确认验证与失败记录

- resolve 定向末轮 `fff3342f` / handle `22410`：exit 0，56s；XML **63/63**，
  时间 2026-10-03T04:47Z；包含 pattern 8、共享快照 6。
- LL 定向 `ffbb7c2b` / handle `69598`：实际 LL XML **29/29**，
  时间 2026-10-03T04:45–04:46Z；pattern 7、enum 3、mixed 9、pattern keeper 2、既有 keeper 8。
  该组合退出码仍是 1：resolve 测试泛型错误；已补明确类型并由上述独立 resolve 末轮验证。
- 更早 `537e4a0c` / handle `31543` 和后续 handle `94523` 均已 exit 1，不能继续等待或重启它们。
  最初 BOOL 测试常量错误已改 BOOLEAN；六个 LL 空 binding 失败由两层 provider 遗漏修复。
- CLI 19/19 在最初组合轮通过；本批最后输入的全量组合仍需完成，不复用旧 XML 作新验收。
- 官方有效取证在 `.workbuddy/tmp/cjmp_probe113/review-20261003/explicit-tuple-header` 和
  `enum-binding-header`：cjc 1.1.3 exit 0。只提交源码/README，不混入原生输出。
- 最新 gate 与进一步协议 mapper 因代理额度错误未返回。之前拒绝的显式接收者/实参状态已改，
  但最新复审未完成，不能宣称框架审查全部通过。

## 当前作业与下一步

### 2026-10-03 13:13 以后续接结果（覆盖下方旧运行状态）

续接新批次：已新增 provider `getDeclarationMetadataOwner`，复用原有非局部 binding owner 索引；
CJMP 真实 binding 键不变，注解读取原声明头，普通 VAR 类型/var-let/修饰符诊断读取整声明范围。
foreign checker 排除模式外壳的重复报告，读取 header.interopInfo.abiRequest.hasExplicitC。
queue `f5533245` / handle `19806` 已 exit 0（8m54s），验证 CommonSpecific 完整族、FFI 完整族与 LL pattern；
源码已编译，CFIR 切片新鲜 XML 346 条：261 passed /85 skipped /0 failed；
快照 `20261003-pattern-header-first-slice`。LL pattern 新鲜 XML 7/7。
官方新增 `foreign-variable-explicit-c` 探针 exit 1，
唯一 sema_illegal_use_of_annotation，没有 native_var_error；已新增 foreignVariableExplicitC.cj。
官方 CheckCJMP.cpp 的 DiagNotMatchedDecl 同样 DiagnoseRefactor 在原始声明上；
reportUnmatched 已接通普通 VAR 声明头范围，不改变真正结果键或 tuple 子范围。
最终输入批次 queue `f26aa19b` / handle `40102` 已 exit 0（6m10s）：CommonSpecific + FFI 完整族
XML 348 条，263 passed /85 skipped /0 failed；完整 LL 168/168，CLI 19/19。
新增 foreignVariableExplicitC 在 PSI/light-tree 两入口通过。快照 `20261003-pattern-header-final-slice`。
全量 queue **dfe213ff** / handle **33883** 已终止：exit 0，BUILD SUCCESSFUL（22m15s）。
完整 CFIR 新鲜 XML 8968 条：8571 passed /397 skipped /0 failed，保存快照
`20261003-pattern-header-final-full`。对比 `20261002-mixed-callable-final-full`：0 regressions、
2 new、0 removed；新增 foreignVariableExplicitC 的 PSI/light-tree 两入口通过。
逐 key 对比 `20261003-pattern-full-before-diagnostic-owner`：先前八项失败全部 passed。
LL 与 CLI task 均 UP-TO-DATE，复用同一最终输入的上一批 LL 168/168 和 CLI 19/19；不是本轮重新执行。
本轮运行期间未编辑源码或测试；现已解除输入冻结。当前无运行作业，不再轮询 handle 33883。
本批不是不可变发布迁移完成；所有源码仍未提交。
迁移文档 §8 新增真实依赖准备调用面：runner 锁内仍有多种 TYPES 推进、全 extend 预热，
并非只删除 LL hook 的 common IMPLICIT 或给当前 matchingGroup 包 lazy cell 就能闭合。
§9 已核对默认值与实际化成员 scope 消费边界：CfirScopeProvider 当前缺成员阶段参数，
CfirCangJieScopeProvider 缺归并前 declared scope decorator；nominal/typealias/static/builtin 缓存
与父 scope/session replacement/receiver 创建路径均需传播 raw/actualized 视图身份。
下批需要按 Kotlin 同层边界整体接入，不能把 body 名称过滤或属性机械替换当作迁移。
本轮曾核验 wrapper 85044 与 CFIR worker 36868 命令路径均指向本工作树；现在均已退出。
迁移文档 §10 补充了属性恢复与阶段恢复的实际边界：锁只恢复 root phase，keeper 仅恢复显式登记值；
transformer 内 keeper 不覆盖其后的 internals phase updater。CJMP updater 当前没有 provider/阶段请求
或取消检查，最终新增属性事务仍需明确发布顺序并覆盖真实失败点；未改源码，未声称事务已实现。
迁移文档 §11 补充官方字段依赖来源：InitializationChecker 检查真实实例初始化器后写
Decl.dependencies；容器合并每轮只替换一条 implementation 边。isMemberParameter 已有共享属性，
但 CFIR 生产尚未接入容器 evaluator，CJO dependencies 的恢复/生产事实入口仍需端到端核对。
不得为收集 specific 依赖预热 BODY，不得给已编译 common 的真实依赖无条件填空列表。
有效新探针 `review-20261003/instance-dependencies`：common/specific 两段式均 exit 0，
common CJO 的 y 真实保存 FullId(-2,2) 指向 x；specific CJO 不保存依赖。探针按官方 schema
固定槽位读取，命令与数据在 README；首次 LLVM 环境错误已通过命令进程内 SDK 路径纠正。
新增接线要求：官方 writer 对未序列化中间声明递归投影依赖，但 merge 每轮仍只替换一条实现边；
GetVarsInitializationOrderWithPositions 读取这些替换后的边。因此初始化消费者也必须迁移到
同一成员图，不能只接 scope。未序列化中间节点投影/初始化顺序尚未实测或实现。

### 2026-10-03 14:15 后的新输入：纯求值输出的集合边界

完整 CFIR 绿色快照对应此前声明头修复输入；以下是后续新输入，不能复用该全量快照作新验收。
已修改 resolution.common 的 CjmpMatchEvaluator 和 CjmpContainerMergeEvaluator，返回集合除复制外，
还使用不可写的集合视图；默认值列表、成员/依赖列表、seed/merge 的类型参数映射也全部封闭写入。
新增两项测试覆盖真实 seed/default/member 图的输入分离，以及消费者强转后的改写拒绝。
queue 093f8bce / handle 58243 已 exit 0（55s）：两种 evaluator 新鲜 XML 36/36，0 failed/skipped。
后续 queue 634f0528 / handle 98940 已 exit 0（4m48s），完整 resolution.common:test 的新鲜 XML
59/59，cfir:resolve:test --tests *Cjmp* 的新鲜 XML 52/52，均 0 failed/skipped。
输入在运行期间保持不变；当前无运行作业，不再轮询这些 terminal handles。
这只落实纯输出的集合不变量：声明属性发布、强 cell、真实容器生产 seed、scope 图和旧存储删除
仍未完成；不可将本次防止输出改写当作九步迁移完成。所有源码尚未提交。

### 2026-10-03 18:00 后的新输入：同一容器图的初始化排序

迁移文档 §13 记录本次共享层改动和原始官方证据。新增 CjmpInitializationOrderEvaluator，消费
真实合并后的 members/dependencies；result 新增不可写 memberFacts，保存原成员资格、来源位、
begin 的 line/column 排序名次。返回真实实例字段及原槽位，不重新配对，不求依赖闭包。
容器外依赖角色显式冻结，不能覆盖图内成员，也不能把缺失身份当作普通声明。
6 项新增测试和集合边界测试覆盖这些契约；CFIR 生产接线仍未实现。

官方 1.1.3 新探针 instance-dependency-order 两段编译及 EXE 均 exit 0：before=7、x=8、y=15、after=15。
common CJO 保留 private hidden，依赖 hidden→x、y→hidden；specific CJO 依赖为空。
该例不是“未序列化 private 节点的递归投影”，该假设已排除。首次错误 runtime 路径的加载失败
已通过 SDK envsetup.ps1 的实际 windows_x86_64_cjnative 路径修正，不作为语言失败证据。

验证初批 6448bd62 / handle 39351 已 exit 0（1m9s），容器 21/21；afa8e964 / handle 45577 已 exit 0
（5m28s），完整共享层 64/64、CJMP resolve 52/52。随后补外部依赖输入与第 6 项新测试；最新输入
已由 queue **5a184292** / handle **20067** 验证完毕，exit 0，12m47s。共享层新鲜 XML **65/65**，
2026-10-03T10:18:27.041Z–10:18:27.263Z；CJMP resolve 新鲜 XML **52/52**，
2026-10-03T10:30:34.297Z–10:30:35.935Z，均 0 failed/error/skipped。
该作业已终止，无需再轮询；源码冻结解除。所有更早的完整 CFIR/LL/CLI 快照均早于本批新输入，
不作本批全量验收。九步仍未完成；最新整体问题闭环口径见本文顶部的重新审计链接。

#### 先前失败与修复批次记录（已由上方最新作业状态覆盖）

- 全量 queue `663028a1` / handle `49830` 已终止，exit 1，25m34s。
  LL 168/168；CLI 新鲜 XML 19/19；CFIR XML 台账 8966 条，8561 passed、8 failed、397 skipped。
  控制台为 8965 tests /396 skipped，验收使用 XML testcase 台账。
- 快照 `20261003-pattern-full-before-diagnostic-owner` 相对 `20261002-mixed-callable-final-full`
  恰有 8 regressions，0 new/removed。三类真实差异：CJMP 两个诊断从整声明变成绑定名；
  CPointer 显式错误类型投影重复锚在绑定名；foreign 变量漏 NATIVE_VAR_ERROR。
- 已修正 TYPES 的隐式 binding type ref 投影：保留 currentTypeRef.source，不能用 binding.source
  制造新类型语法。对应 Kotlin CopyUtils / FirTypeResolveTransformer 派生类型归属及既有
  CfirPatternBindingResolution.replaceBindingType；没有改 fixture。
- 验证 queue `7e4438d6` / handle `72972` 已终止，exit 1，5m1s。
  完整 PSI/light-tree FFI 切片 82 项中 80 passed、2 failed（foreign）；CPointer 两入口恢复。
  LL pattern 新鲜 XML 7/7，0 failed/error/skipped。未处理 foreign/CJMP 两类，不能声称全量已修复。
- 下一步先完成该切片验证，再处理普通 native VarDecl 的声明头事实归属和显式 @C 请求。
  官方 CFFICheck v1.1.3 判定 HasAnno(C)，不能用 status.isC（派生 ABI）代替。
- 当前无运行作业，不再轮询 `49830` / `72972`。下方等待指令只属于旧运行记录。
  下一批必须同时处理 root 持有 annotations/interop/header source、binding 持有真实变量身份；
  不能只替换 foreign checker 的布尔条件，否则 root/binding 重复检查及绑定丢失显式注解的问题仍在。

### 原运行记录（已由上方终止结果覆盖）

完整组合已启动：queue `663028a1`，handle **49830**，wrapper owner PID 87144。
最后工具观察仍在运行，已进入完整 CFIR test；尚无整个组合的终止结果。
完整 LL 新鲜 XML **168/168**，2026-10-03T04:48:45–04:50:11Z，0 failure/error/skip。
CFIR 已显示 `cjmpVarNotMatchLet`、`cjmpLetTypeMismatch` 各 PSI/light-tree 两个失败，
尚未得到最终 XML。官方 MatchCJMPVar 确认这两类诊断存在且仍配对；fixture 已用相应诊断名称，
不能直接改期望。下一步核对真实 binding 的诊断范围与 native 普通 VarDecl 声明头归属。
后续控制台还显示 `cpointer_generic_reverse` 与 `foreignVariableNegative` 各 PSI/light-tree 失败，
当前已观察八个失败条目，不代表最终总数。官方新增取证 `variable-diagnostic-anchor` 的两个错误
均覆盖完整普通变量声明；stdout 空，stderr JSON 已保存。原 fixture 无需因此改范围。
续接同时核对 TYPES 派生 binding type ref 是否制造新的错误源码位置，以及 STATUS 后的
显式 `@C` 请求与派生 C ABI 是否被 checker 混淆；这些是待用实际差异证实的调查方向，尚未修复。
续接先轮询同一 handle。若 handle 不可用，核对 queue JSON 与精确进程命令行；
观察超时不代表终止，不因失去输出直接重启作业。

```text
java -jar D:/code/intellij/cangjie/gradle-queue-cli/build/libs/gradle-queue-cli.jar --project-dir C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie :analysis:low-level-api-cfir:test :cfir:analysis-tests:test :compiler:frontend:test --tests *CjmpTwoPhaseCompilationTest* --no-build-cache --continue --console=plain --max-workers=1 "-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8"
```

完成后核对新鲜 LL/CFIR/CLI XML；使用 verification ledger 创建新 pattern 快照并对比
`20261002-mixed-callable-final-full`，修复真实回归。修改输入后需重新验证。
随后闭合不可变迁移文档 §7：单目标独立计划不推进 specific 兄弟，组聚合的依赖准备在消费者/家族
入口锁外完成，强 lazy cell 只读冻结事实纯求值。不能直接缓存当前 mutable runner，不能删除准备后
使用未解析类型，也不能扩大同阶段权限或预热兄弟 BODY。整体百分比未重新审计，不更新历史 51%。
