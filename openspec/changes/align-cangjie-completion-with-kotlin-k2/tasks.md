## 1. 前置基线与工作树准备

依据：[设计](design.md)、[Analysis 规格](specs/analysis-completion-support/spec.md)、[插件规格](specs/ide-completion-support/spec.md)。下列复选框全部代表后续实施任务，不是本轮规划进度。每项完成须记录改动、当前源码下的验证命令/结果及失败边界。

- [x] 1.1 对照 source-baseline.json 复核主仓库、宿主和两份 Kotlin 参考，若文件变化则更新受影响结论与任务，不沿用旧断言。
- [x] 1.2 确认实际工作树上游目录配置；增加显式 upstream root 配置，保留普通 checkout 的相邻主仓库默认值，并对不存在的 settings 给出明确错误。禁止写死开发机路径。
- [x] 1.3 建立 Kotlin contributor/API 消费矩阵，区分已有、缺契约、实现受限、待验证及不适用项，固定 D2 的仓颉差异。
- [x] 1.4 核验目标平台的 Completion/Lookup/测试 API 所属制品，确定最小编译依赖及 baseline；属性文件存在不能视为兼容测试通过。
- [x] 1.5 列出现有 scope、candidate decision、shortener、signature、dangling 的调用者及测试基线，记录兼容策略；不得直接把旧 file scope 扩大为全可见声明。
- [x] 1.6 **提交上游工作树中 `Ca` 前缀下线的 27 个重命名**（已完成：提交 `133851357`，`git log --diff-filter=R` 可见 27 条重命名记录；同提交并入 8.8 的上游侧两处漏改修复）。未提交前，`design.md` §4.4 的 42 个文件清单随时可能因 `git stash` 或工作树重建而失效——**本项阻塞其余一切**（G0 前置）。禁止使用任何 Git 回滚命令处理工作树状态。

## 2. Analysis：副本分析与模式控制 A6

依赖：第 1 组。落点：analysis-api 的 analyze/projectStructure，平台 projectStructure provider，low-level-api-cfir session/provider，已有 dangling 测试。

- [x] 2.1 定义作用域化解析模式覆盖的输入、线程隔离、嵌套、取消恢复及非法原件/副本组合契约，复用既有 CaDanglingFileModule。
- [x] 2.2 实现模式覆盖和 analyzeCopy 便捷入口，并让模块创建读取该模式；不只增加一个不被 provider 消费的包装函数。
- [x] 2.3 核对 LL 的 disregardSelfDeclarations、context session 依赖与缓存键，增加不同模式/副本互不污染的验证。
- [x] 2.4 修正或扩展现有声明了 IGNORE_SELF_MODE 却未消费指令的测试准备逻辑；分别验证两种模式、原件不变与副本局部声明可见。**（准备逻辑已完成、两种模式的解析行为对照受缺陷阻塞）**指令已消费：副本准备抽成 `DanglingFileFixturePreparation`（唯一差别是是否把平台 `PsiFile.originalFile` 指向原件），抽象类按指令选模式，并新增 `PREFER_SELF_MODE` 作为仓颉侧显式入口（Kotlin 默认即 PREFER_SELF，仓颉默认仍是 IGNORE_SELF——PREFER_SELF 解析路径受已记录的 LL 缺陷阻塞）。`DanglingFileFixturePreparationTest` 逐个模式验：原件文本不改、副本是原件复制、副本保留原件顶层声明、两种模式的 `originalFile` 形状；用例不跑分析，故不受该缺陷影响。**（2026-10-08 完成）**6.8 修复后补齐了解析行为验证：`AnalysisApiDanglingFileExecutionTest` 断言 PREFER_SELF→**副本**、IGNORE_SELF→**原件**（串行与并行两条路径）、副本局部声明可见、原件文本不变；指令→模式映射由 `DanglingFileFixturePreparationTest` 钉住（未声明落默认 IGNORE_SELF、两条显式声明各归其位、同时声明直接失败），映射消费点收进 `DanglingFileFixturePreparation` 与指令容器同文件；新增数据文件 `danglingUnresolvedReferencePreferSelf.cj`（显式声明 `PREFER_SELF_MODE`）让 PREFER_SELF 夹具路径在 14（cfir）+ 6（standalone）个生成类上端到端跑通（85/0、62/0）。
- [ ] 2.5 增加嵌套、异常/取消恢复、并行模式隔离和文档变化后的 session/pointer 失效测试。

## 3. Analysis：位置、导入与静态/实例作用域 A1/A2

依赖：第 2 组公开契约；正常文件适配可先于副本压力测试实施。落点：CaScopeProvider、CFIR scopes、LL ContextCollector；保留现有 LL 原件/副本测试。

- [x] 3.1 新增 CaScopeContext、CaScopeWithKind、CaScopeKind 和隐式接收者公共视图，明确层级次序、owner、生命周期和声明类别。
- [x] 3.2 实现按文件和位置查询，通过 LL ContextCollector 投影词法 scope 和接收者，不在插件遍历 PSI 重写名称解析。
- [x] 3.3 增加 importing scope context 与组合查询，复用 CfirFileLookupScopes 的同包、显式/星号/默认绑定及别名，保留来源优先级。
- [x] 3.4 增补补全所需的 static/instance 成员查询或筛选契约，区分类限定访问、实例访问、this/super 与 extend 使用点。
- [x] 3.5 修正文档中 file scope 与 importing scope 混淆的表述，并保持既有文件声明查询的兼容结果。
- [x] 3.6 增加公开层的局部/参数/类型参数、嵌套块、同名遮蔽、match 绑定、receiver 和原件/副本测试，断言 scope 次序及候选身份。（已完成，提交 `0bd8901fe`）新增 4 例：`lexicalScopesExposeParametersLocalsAndTypeParameters`、`nestedBlocksKeepInnermostShadowing`（断言**次序**：最先命中的 shadowed 必须来自最内层作用域）、`matchBindingStaysInsideItsBranch`（正例 side 可见 / 反例 radius 不泄漏）、`scopeContextInCopiedFileIsEquivalent`（原件与副本的 LOCAL_SCOPE 名字一致）。写用例时撞出并修复了一个新缺陷：**收集侧没有 match 分支作用域隔离**（编译器侧 `resolveBranch` 有 `withNewLocalScope`，`ContextCollector` 无任何 match 覆写），导致 `case Circle(radius)` 的 radius 泄漏进 `case Square(side)` 分支。receiver 一项由既有 `memberScopeExposesImplicitReceiver` 覆盖。
- [x] 3.7 实现 `CodeFragmentScopeProvider.getExtraScopes`（现为 `emptyList()` 桩）；验收：表达式片段、类型片段各一组正反夹具，且经 cjc 验证为合法源码。**前置：缺陷 D-1 已修**。（**部分完成**，提交 `1cf1ca2d9`）桩已实现。**实现位置与设计预想不同**：本方法在片段 body 解析**之前**被调用（`resolveCodeFragmentContext` 先建上下文再解析片段），此时 `block` 仍是 `CfirLazyBlock` 桩，直接读 `statements` 会 `error("CfirLazyBlock should be resolved before accessing")`；因此片段自身声明实际发布在 `ContextCollector.visitCodeFragment`，`getExtraScopes` 对未解析片段返回空列表（正确结论而非缺失）。已解析片段仍按声明种类汇成成员作用域，`CfirPatternVariable` 显式跳过（`storeVariable` 对它 error）。**遗留**：「类型片段」一组用例未加——`CjTypeCodeFragment` 在本仓 PSI 侧无对应构造入口，实测前需先确认其可用性。宿主文件可见性已由 `e2d6929cb` 补齐（`CaCfirScopeContext.hostFileLevelEntries`），正例断言现已包含「宿主顶层声明必须可见」。**（已完成）**实现早已落地（`CodeFragmentScopeProvider.getExtraScopes` 会收集片段自身的顶层声明，并在 KDoc 里说明「表达式/类型片段返回空是正确结论而非桩」；任务描述里的「现为 emptyList() 桩」已过期）。本轮补齐的是**验收**：类型片段的类与文件类型都在、**只缺 `CjPsiFactory` 工厂入口**，于是「类型片段」这条路在测试里根本走不到——已按表达式片段补上 `createTypeCodeFragment`。`CodeFragmentScopeExecutionTest` 4/0：块片段正例（自身声明可见 + 宿主可见）、表达式片段反例、**类型片段反例+正例**（无片段成员作用域、但能看到宿主类型）、投影同源。另记：`getContentElement()` 是 Kotlin 声明的函数，不合成属性，调用必须写括号。
- [ ] 3.8 增 `CaScopeKind.CodeFragmentMemberScope` 与 `CjCodeFragment.fragmentMemberScope`，并跑生成器产出 `context(session)` 桥接；验收：公共 API 组件测试断言作用域种类、次序与「不泄漏宿主专有符号」。（**部分完成**，提交 `1cf1ca2d9`）`CaScopeKind.CODE_FRAGMENT_MEMBER_SCOPE` 已增（`indexInTower=4`，避开已被 `STATIC_MEMBER_SCOPE` 占用的 3，撞值会让排序等价、掩盖真实次序）；`CfirLocalScope.isCodeFragmentMemberScope` 标记与四个 store 方法的透传已加；`toScopeKind` 在 `isLocal` 之前先判它；只有**块**片段才标记（表达式/类型片段挂空层会与「不泄漏宿主专有符号」冲突）。验收用例 `CodeFragmentScopeExecutionTest` 断言种类与「片段自身声明可见 / 表达式片段无该作用域」。公开入口 `CjCodeFragment.fragmentMemberScope` 已由 `5184aacb0` 补上（复用 `CaCfirScopeContext` 投影，用例 `fragmentMemberScopeMatchesScopeContextProjection` 锁两个入口同源）。**未做**：①`context(session)` 桥接生成器未跑（属任务 7.5，`CaScopeProvider` 底部有「自动生成，请勿手工修改」标记）；②次序断言只覆盖种类，未覆盖 `indexInTower` 的相对次序。
- [x] 3.9 在既有 `CompletionPositionContext` 增代码片段位置分支（**不新建平行文件**）；验收：正反位置用例。（已完成，提交 `1cf1ca2d9`）新增 `CODE_FRAGMENT_EXPRESSION` / `CODE_FRAGMENT_BLOCK`，判定置于块/函数体之前——片段内部也有 `CjBlockExpression`，按块位置处理会让片段退化成普通函数体。3 例正反用例。连带修 `CfirKeywordCompletionSection` 的穷尽 `when`。**未做**：3.8 要求的 `context(session)` 桥接生成器尚未跑（属任务 7.5）。
- [x] 3.10 修复缺陷 D-1（**已完成**，提交 `3b1f2fdcf`）：实际根因不在 `CfirTowerDataContext.addLocalVariable` 的空 `localScopes` 早退（那处确实丢，但丢的是函数参数，随后被 `preloadValueParameters` 补回），而在 `ContextCollector` 把绑定变量的入作用域放在 `visitPatternVariable` 的 `withLocalVariableBodyCompat`（即 `withTowerDataCleanup`）内部，清理退出时把 tower 回滚。存储已移到各自清理之外：`visitPatternVariable` 按 `pattern.bindingVariables()` 逐个存、`visitPatternBindingVariable` 与 `visitFieldVariable` 在 `visitVariableLike` 之后存、`visitVariableLike` 不再负责存储。**两处负例断言均已翻转为正例**：`ScopeContextExecutionTest.lexicalScopeContextAtPosition` 与 `CfirLocalScopeCompletionSectionTest.localScopeCandidates`，两处注释原本都写着「底层修复后此处应断言包含 localValue / local」。补全侧未拼补文件级作用域。golden `contextCollector/simple*.txt` 已重生成。

## 4. Analysis：类型 scope 和签名 A3

依赖：第 3 组数据模型。落点：CaScopeProvider、拟新增 CaTypeScope、CFIR receiver scope 接缝、signatures。

- [ ] 4.1 增加保留具体接收者的类型作用域/签名查询契约，明确与旧 CaType.scope 的兼容关系，列出后续迁移点。
- [ ] 4.2 从 compiler typeToScope/collectTypeScopes 抽取必要的内部复用入口，接入真实类型实参、上界、intersection、builtin extend 和继承替换，避免复制实现。
- [x] 4.3 补齐已替换签名再次 substitute 的组合实现，保留原声明身份和类型替换顺序。
- [x] 4.4 修正或适配变量签名对底层符号种类的限制，覆盖 field、prop、局部变量，不将 property 当 CfirVariableSymbol 强转。
- [ ] 4.5 增加泛型本类/父类、连续替换、property、完全实例化 extend 和非 class-like receiver 的签名测试；当前非泛型继承 fixture 继续保留。

## 5. Analysis：适用性、访问、可达性 A4/A5

依赖：第 2–4 组。落点：候选/可见性组件、CaCfirImportPlanning、公共 CFIR accessibility 和 extend substitution 接缝。

- [x] 5.1 增加绑定原文件、副本位置及 receiver 的适用性检查 API，结果表达适用性与 substitutor，保留旧三态入口。
- [x] 5.2 实现仓颉 extend 精确类型匹配和泛型约束适配，复用带约束的 createExtendDeclarationSubstitution；不得使用 constraint-derivation 模式绕过约束。
- [x] 5.3 将公开 use-site 可见性适配到 compiler 公共 accessibility 服务，传入需要的文件、声明链、receiver 类型、lookup 来源和 provenance，避免维护两套访问分支。
- [ ] 5.4 将短名可达性与 A1/A2 的位置 scope、shadow 和 alias 关联，补齐局部/成员候选，不只扫描文件同名符号。
- [x] 5.5 增补导入/缩短计划中的目标身份、组织名/alias 与冲突复验信息，区分不可访问、不适用、未知和需要导入；实际写入留给第 10 组。**（分类已完成；写入属第 10 组）**`CaCompletionCandidateStatus` 拆成四类：`HIDDEN`（确定不可访问）/`NOT_APPLICABLE`（不适用）/`UNKNOWN`（判定不出）+ 原有 `DIRECT`/`REQUIRES_IMPORT`，行为规则写成两个契约属性（`shouldHide` 只有确定不可用才算、`isDeterminedUsable` 只含能落地的两态），生产端按四类给出、引用缩短改按 `isDeterminedUsable` 且**行为不变**（既有三态断言用例保持绿）。契约用例 4/0 含穷尽性互斥（未来加状态忘了归类就会红）。目标身份/组织名与 alias/冲突复验信息**已具备**（`CompletionImportTarget.importPath` + `ImportConflict` + 应用器写事务内复验）。**未做**：A 侧「任意位置局部遮蔽」对三态可达性的替代（设计 §6.6 现状条目，属更大的 A4/A5 面）。
- [ ] 5.6 增加正反测试：receiver 不匹配、约束失败、权限、private extend、同名导入/局部 shadow、旧三态调用者与缩短回归。**（补全层五项已完成，两项有剩余）**已有：receiver 不匹配、权限（private 成员）、同名导入/局部 shadow、**约束失败（新）**、**private extend（新）**——后两项均带正反对照，夹具语法经 cjc 1.1.3 实测（泛型 extend 写作 `extend<T> Box<T> where T <: Shaped`）。**未做**：①「旧三态调用者」回归属 A 侧入口；②「缩短回归」——查证发现 `CompletionImportPlan.QualifyThenShorten` **没有任何生产者**（全仓只有契约、应用器消费分支与一条手喂用例），路径休眠，先要有分析层产出才谈得上回归。
- [x] 5.7 修复缺陷 D-2（**已完成**，提交 `f6b306951`）：`CaCfirCallableSymbolCacheKey` 与 `CaCfirExtendMemberCallableSymbolCacheKey` 增带 `CfirCallableSignature`，`matchesStableCallable` 与 extend 成员恢复都按签名筛选；`callableSnapshot` 的 `deduplicationKey` 改为直接调用 `callableIdentityKey`（同源），该键不再使用带 `!`/`vararg`/`= ...` 展示标记的尾文本。验收用例：AnalysisApiSymbolEquivalenceTest.overloadIdentity（顶层/类成员/extend 三组重载的两两不等价 + 各自恢复到自身）、CfirTopLevelCompletionSectionTest.topLevelOverloadsCarryDistinctSignatures（同名不同参数的候选同时出现且各带实例化签名）。**遗留**：「按声明 PSI 去重」在本仓未找到对应实现——`completionDecisionKey()` 定义在 analysis-api-cfir 但无任何消费方，插件侧也未见按 PSI 去重的代码；若该绕行指的是别处，请指明后补做。

## 6. Analysis：期望类型与名称发现 A7/A8

依赖：第 2–5 组。索引契约设计可与第 4 组并行，最终结果依赖可见性/签名。

- [x] 6.1 在现有表达式类型组件补 typed initializer、赋值 RHS、普通 Bool 条件、命名参数默认值；保留 return/成功调用既有入口。
- [x] 6.2 按仓颉合法语义补适用分支/块结果场景，并定义没有期望约束时的未知结果，禁止搬入 Kotlin 独有语法。
- [x] 6.3 增加不完整调用的可恢复约束查询，明确重载歧义行为，不以第一个候选的参数类型作为确定答案。
- [x] 6.4 新增明确 RHS 定位的 expected-type fixture；保留 propertyInitializer 的 callee 定位专项，不无脑修改 null golden。未完成调用另放补全恢复 fixture。
- [x] 6.5 在现有 declaration/package 平台接口与 `CaStubIndexFacade` 之上补**按名称过滤的反向查询契约**（名称 → 声明集合 + analysisScope 限定 + 模块依赖可见性），返回符号身份而非索引键；**不泄漏具体索引名**。保留已有包枚举与 stub 索引。（已完成）在 `analysis-api-platform-interface` 新增 `CaSymbolIndexQuery`：`createSymbolIndexQuery(analysisScope)` 绑定范围，`findDeclarations(CaSymbolIndexNameFilter)` 按姓名发现，返回 `CaSymbolIndexEntry`（`packageFqName` + `name` + `kind` + 枚举构造器的 `ownerClassId`）。**条目里没有符号**——最初写成「返回 `CaSymbol`」被否证：那会迫使契约持有 `CaSession`，把会话泄漏进可替换的索引实现；恢复符号是 Analysis 层按包作用域查询的事。**不泄漏索引键**：没有 stub `fileKey`、没有 IntelliJ stub index key、没有虚拟文件。前缀分级规则（≤3 字符且非重复只做起始匹配）作为**契约的一部分**由 `CaSymbolIndexNameFilter` 统一实现，三个实现不得各自改变结果集。模块依赖可见性由调用方绑定的 analysisScope 表达，不写进条目。api 基线已重新生成。
- [ ] 6.6 落地**持久化符号索引**：①平台接口契约；②IDE 侧平台索引实现（**跨会话持久**，随索引增量更新）；③无平台侧复用 `CaStubSnapshotBuilder` 快照（**仅会话内，不得宣称持久**）；④`.cjo` 侧走已有只读名字视图。实现前缀分级过滤（≤3 字符只做起始匹配）。验收：按前缀查询命中且不漏报；记录查询次数与耗时并设预算；断言 IDE 与无平台实现在同一查询下返回同一符号集合。**（①③④已落地，②未做，故不勾选）**①见 6.5。③见 `CaStandaloneSymbolIndexQueryService`（会话内快照、按修改计数缓存文件摘要与倒排名称表、`SESSION_ONLY`，已在 standalone 描述符注册为 `projectService`）。④已落地：`CaSymbolProvider.getDeclarationsByNamePrefix` 在源码索引之外并把 CFIR 会话名字提供者（`CjoExportedTopLevelNamesResolver` 已处理 re-export/alias）的顶层名字并入，**两条通道在名称层按「包 + 短名 + 种类 + 所属类型」先合并再解析**——各自解析一遍会让同一声明变成两个候选，而按解析结果去重又会合并合法重载。**未做**：②IDE 侧跨会话持久实现属宿主装配面（按分工不在本工作区），因此**不得断言两端等价性**，本项不勾选。**查询量已有记录**（`queryCount`/`queryNanos`/`fileCollectionCount`/`summaryBuildCount`），**耗时预算未设**——预算须在真实 SDK 规模下测量后才可写死，这里不编造数字。
- [x] 6.7 增加多模块同名、未打开源码、SDK/.cjo、typealias、同名重载与 enum 构造器发现测试，记录查询量、取消及失效。（已完成）`CaStandaloneSymbolIndexQueryTest`（4 例，真实模块图）：①跨包同名保留包身份且同名字段分类保留；②**未打开源码**——standalone 宿主无编辑器，夹具文件本就从不"打开"，因此这条不是模拟而是事实；③typealias 可被按名发现且与类同走分类通道；④同名重载在名称层成一条、由恢复阶段展开（重载身份由 `CfirIndexCompletionSectionTest` 的候选侧锁定）；⑤枚举构造器带所属 `ownerClassId`；⑥无关模块被 analysisScope 排除、范围收窄后不召回；⑦**查询量**：`fileCollectionCount`/`summaryBuildCount`/`queryNanos` 断言同一版本内不重建、计时非零；⑧**失效**：注入版本来源推进后必须重新收集文件（缓存键取错的表现是静默的——不报错，只是候选永远不更新）；⑨**取消**：查询在已取消的 `ProgressIndicator` 下抛 `ProcessCanceledException`，不得吞成空候选。SDK/.cjo 另由 `CaSymbolProviderBinaryNamesTest` 在真实 stdlib `.cjo` 上锁定（`Int64`/`Option` 可见，且恢复出带 `ClassId` 的类型符号与带 `callableId` 的函数符号）。前缀分级由 `CaSymbolIndexNameFilterTest` 3 例锁定。
- [x] 6.8 修复缺陷 D-3（dangling 副本的 PREFER_SELF 非局部解析返回 `null`）；**若判定 LL 修复不可行，必须产出记录在案的决策，不得继续绕行**（已试过的两条绕法会让 21 个生成用例失败）。**（2026-10-05 两轮取证，未修复）** 复现：夹具改成 Kotlin 形态后 7 个生成类 × 3 = **21 个失败**。**根因（与原记录不同）**：`LLCangJieSourceSymbolProvider.kt:177` 的 `require(context == null || context.isPhysical)` 抛 Failed requirement，中断整个文件的诊断收集；**不是** `getNotNullValueForNotNullContext` 的缓存不一致。**已定位调用方**：`LLNameConflictsTracker.getClassifierRedeclarations:55` → `getAllClassLikeSymbolsByClassIdOrSingle` → `getAllClassLikeSymbolsByClassId:166` → `getClassLikeSymbolByPsi:421` → 缓存 `getSymbolByPsi:118` → `getSymbolByClassId:74` 把非物理声明当 CONTEXT 传入。**已否证两条修法**：①删 `require`（失败 21→14，但 Kotlin `:184` 有**完全相同**的断言、其调用点也直接传声明，是刻意的不变量，删它等于掩盖违约）；②只补 `CjFile.contextModule`（真实缺失能力，已由 `1fe72f9cb` 补上，但不减少失败）。**已逐项核对且与 Kotlin 完全一致、不必重走的**：`require`、调用点、provider 是否 multi、`getAllClassLikeSymbolsByClassId`、`...OrSingle`、声明提供者构造、游离模块 `baseContentScope`、`LLNameConflictsTracker`。**下一步**：查 `CfirSession` 为游离模块构造 declaration provider 时 `searchScope` 指向上下文模块还是副本自身；随后修**调用方**（unwrap 到原件或按 ClassId 查），保留 `require`。详见 `implementation-log.md`「缺陷 D-3 取证」三节。**（2026-10-08 已修）**根因与早前记录不同：不是缓存不一致、也不是 `require(context.isPhysical)`，而是**漏移植 Kotlin 的 `ConeEquivalentCallConflictResolver`**——副本自身 provider 与上下文模块依赖 provider 同时给出同一份声明的两种身份，调用解析得到两个等价候选、判重载歧义，`resolveToSymbol()`（内部 `singleOrNull()`）返回 `null`。已补该类（`cfir/resolve/.../calls/overloads/ConeEquivalentCallConflictResolver.kt`）并在 `ConeCallConflictResolverFactory` 启用；实测 PREFER_SELF 解析到**副本**、IGNORE_SELF 解析到原件；`:analysis:analysis-api-cfir:test` 1629/0，`AnalysisApiDanglingFileExecutionTest` 四处缺口断言翻正。**仍未做**：诊断夹具默认模式仍是 IGNORE_SELF（Kotlin 默认 PREFER_SELF），改默认会走另一条冲突检查器路径，另案。详见 `implementation-log.md`「缺陷 D-3 修复」。
- [x] 6.9 复核缺陷 D-4（字段成员缺失，已修 + 回归锁定）回归仍绿，并回写 `handover.md` §5 第 4 条「根因未最终定位」的过时表述。（已完成）重跑锁定用例：`CfirIdeNormalAnalysisSourceModuleMemberScopeTestGenerated` 与 `CfirIdeNormalAnalysisSourceModuleDeclaredMemberScopeTestGenerated` 各 2 例、0 失败（按 §6 纪律核对了 XML 里的实际执行用例数，不只看 `BUILD SUCCESSFUL`）；夹具 `memberScopeQueries.cj` 含 `DECLARED_MEMBER_SCOPE_AVAILABLE_NAME`/`DECLARED_MEMBER_SCOPE_CALLABLE` 的 `field`/`state` 指令。`handover.md` §5 第 4 条已改为已修并写回两处落点（`CfirClassUseSiteMemberScope` 而非四层声明作用域；字段指针缺失）。
- [x] 6.10 为无平台索引实现补两项前置：`analysis-api-standalone` 新增 `:analysis:stubs` 模块依赖；`CaStubSnapshotBuilder` / `CaStubSnapshot` 由 `internal` 放开为公开 API。（已完成）依赖已加。放开时**没有**原样公开 `CaStubSnapshotBuilder(fileCollector: CaStubFileCollector)`：该形参吃 IntelliJ `Project`，提到公开契约上就等于要求无平台实现伪造一个 IntelliJ project。改为新增公开 `fun interface CaStubFileSource`（`collectFiles(): List<CjFile>`），builder 只依赖它，平台侧 `CaStubPlatformState` 用 `CaStubFileSource { CaStubFileCollector(project).collectFiles() }` 适配，行为不变。同时放开 `CaStubFileSummary` / `CaStubSummaryBuilder` / `CaStubTreeSummaryExtractor` / `CaStubSnapshotAssembler`（公开构造器的形参类型必须一并公开）。`analysis:stubs` 测试全绿。

## 7. Analysis：API 组装、生成与准入 A9

依赖：第 2–6 组各任务可分批接入，不等待全部结束才更新生成器。

- [ ] 7.1 联动 CaSession/CaBaseSession/CaCfirSession、组件工厂及接口继承，把每个新契约接到实际实现。
- [ ] 7.2 定位并更新现有桥接/测试生成入口和模型，执行真实生成器；禁止手改生成文件或臆造 Gradle 生成任务名。
- [ ] 7.3 在共享 Analysis component 基座/testData 增加新契约测试，CFIR 专用回归保留于相应实现套件；更新必要 API surface baseline 与模块文档。
- [x] 7.4 在当前源码下运行受影响 Analysis/LL 窄集并记录结果，覆盖已有 callers。没有新执行记录不得宣布 G1/G2 完成。（已完成，2026-10-07）执行记录见 `implementation-log.md`「7.4 受影响 Analysis/LL 窄集执行记录」节：`analysis-api-cfir` 1623/0、`analysis-api-standalone` 712/0、`stubs` 8/0、`low-level-api-cfir` 136/**4**（全部是 `CfirCjmpCallerFirstDiagnosticsTest` 的四条，**已记录的既有基线**，与本次改动文件无交集），补全侧 `impl-shared` 47/47、`impl-cfir` 46/46。`analysis-api` 与 `analysis-api-platform-interface` 是纯契约模块、无 test source set——不是漏跑，其正确性由消费者套件覆盖。
- [ ] 7.5 定位并跑真实 context 桥接生成器，消除 `CaScopeProvider` 新增成员的手改桥接（缺陷 D-6）；验收：生成器产出桥接，且手改桥接被禁止。**（2026-10-07 前提核查：该生成器在参考实现里不存在，本项按原措辞无法验收，已产出记录在案的决策，不勾选）**证据见 `implementation-log.md`「7.5 前提核查」节：①`external/kotlin/analysis/` 下只有 `analysis-api-fir-generator` 与 `deprecated-k1-frontend-internals-for-ide-generator` 两个生成器模块，按 `bridgesgenerator`/`ContextBridgeGenerator`/`generateBridges` 全树搜索无命中；②Kotlin 的桥接确实带 `// Auto-generated bridge. DO NOT EDIT MANUALLY!`，但**就写在组件接口文件里**（如 `KaScopeProvider.kt`），该注释是约定而非代码生成产物标记；③桥接是**选择性**的——`KaScopeProvider.kt` 16 条桥接远少于其成员数，而「哪些成员需要桥接」本身是人写的设计决定，生成器无从推断。因此按原样验收会要求发明一个参考实现不存在的工具，且会引入真实循环依赖（生成器需已编译的 `analysis-api` 做反射输入，`analysis-api` 又需生成结果才能编译）。**D-6 的真实风险是「加了成员忘了写桥接」，可机械化的部分是校验而非生成**：把「哪些成员需要桥接」写成显式清单，用测试断言清单内每条都有对应桥接。本仓现状：`CaScopeProvider.kt` 9 条、`CaVisibilityChecker.kt` 3 条，形态与参考一致。**该校验留待与 7.3 一并处理**（同为共享 Analysis component 基座的契约测试），不在本项临时造半成品生成器。

## 8. 插件侧：模块、入口与调度 B1/B2/B3

依赖：第 1 组可先建骨架；语义接入依赖 G1，完整候选依赖 G2。落点：拟新增 code-insight/completion 的 api、impl-shared、impl-cfir。

- [x] 8.1 建立三个模块及 src/resources/test/testResources，增加 settings/module-catalog；通过现有 convention 配置最小依赖。（已完成）三模块在 `settings.gradle.kts`（77–79 行）与 `docs/module-catalog.md`（53–55 行）均已注册，依赖最小且显式。目录现状：`contracts`(src)、`impl-shared`(src/test)、`impl-cfir`(src/resources/test/testData)。**没有 `testResources`**：它只在存在测试专用描述符时才有意义，而设计要求「禁止把测试 plugin.xml 混入产品」——测试描述符属 8.7/12.4 范围，在没有该需求时建空目录只会留下 git 无法跟踪的空壳；不为了凑清单而建空目录。
- [x] 8.2 定义跨模块服务和策略接口、内部 contribution/section/runner/sink，不给每个内部贡献者增加公共 EP。（已完成）跨模块服务 `CangJieCompletionService`、策略接口 `CompletionFilter`、内部 `CompletionSection`/`CompletionRunner`/`CompletionResultSink` 齐备。**生产描述符只注册一个 `completion.contributor`**，没有为任何内部贡献者加扩展点（`completion.weigher` 命中数 0；各区段由 `CfirCompletionService.sections` 的显式列表组织）。
- [x] 8.3 实现原件/副本、offset、prefix、invocation count、BASIC/SMART 和替换区间的参数包装。（已完成，一处歧义如实记录）`PlatformCompletionRequest` 承载原件/副本两个位置、offset、prefix、invocationCount、BASIC/SMART（`CompletionKind`）与替换区间；`documentStamp` 取**原始文件**的版本戳（分析副本上的 dummy 修正与用户文档无关）。**一处如实记录的歧义**：`originalPosition` 目前与 `completionPosition` 同值（都在副本里且是**修正后**的位置）；契约说它「触发补全的原始位置」并补充「未修正时等于 completionPosition」，设计 B2 说「原件/副本」——两处措辞指向两种读法，当前实现两者都不是。它目前**没有消费者**（语义复查走自己的 use-site 参数，插入校验用 `documentStamp`），因此不猜着改；第一个真正的消费者出现时按它需要的语义定，并用例锁定「两者必须可区分」。
- [x] 8.4 实现 dummy 与位置修正，覆盖泛型、未完成调用/类型、字符串插值及文档，协调 typed-handler 自动触发。（已完成，typed-handler 协调除外，提交 `c4c6c55af`+`125abc23e`）**修正覆盖**：表达式/函数体/成员访问/命名实参位置换成 `cangjieCompletionPlaceholder()`；类型位置保持裸标识符（替换成调用反而会把候选从类型变成值）；原始字符串内直接排除（不插值）；**泛型限定名后与字符串插值内同样替换**。**该逻辑此前没有任何用例**（唯一入口依赖 `CompletionParameters`），新增 `provideFor(file, offset)` 后补齐 8 条。**过程中挖出一个被静默吞掉的真实失败**：占位替换原先是 `runCatching { }.getOrNull()`，去掉吞异常后立刻现形 `IncorrectOperationException: Must not change PSI outside command or undo-transparent action`——**占位替换是一次 PSI 写，必须在命令内执行**；平台入口本身处于补全命令内所以从未暴露，而这条前置条件此前既不在文档里、也无法被测试发现。现已写进 KDoc 并按契约测试。**未做**：`typed-handler` 自动触发协调（设计 §7.2「避免重复弹窗」）属宿主平台接线，不在本工作区。
- [x] 8.5 实现仓颉 PSI position context 分类器及正反测试，不复制 Kotlin 专属上下文。
- [x] 8.6 实现可取消的串行优先级 runner、按批 sink 和延迟阶段，保持执行优先级与展示权重分离。（已完成，提交 `d78f1f085`）**三件都落地并有用例**：①**可取消的串行优先级 runner**——`SerialCompletionRunner` 按 `executionPriority` 稳定排序（同优先级保持注册顺序），每个区段执行前 `checkCanceled()`，取消以异常传播、不吞成空批次；②**按批 sink**——`CompletionResultSink.consume(batch, isLast)`，sink 只接收已渲染快照，因此 runner 被取消时已发出的批次仍可安全展示；收尾收紧为「全部阶段完成后恰好一次」；③**延迟阶段**——`CompletionSection.isDelayed` 把区段分两阶段，快速阶段先跑并立即送达批次，延迟阶段随后跑且**进入前重新检查取消**（用户继续输入时整段跳过）。`CfirIndexCompletionSection` 归入延迟（要枚举源码倒排表与 `.cjo` 名字视图，是管线里最慢的一步）。**执行优先级与展示权重分离**：`executionPriority` 只决定执行次序，展示权重由候选自身的 `CompletionSortData.toWeight()` 决定，执行器不得重排批内候选（有专门用例锁定）。**阶段划分与延迟时长是两件事**：本层只保证「快速批次先于延迟批次」，真正的延迟时长由宿主决定（它才知道用户是否还在输入），该边界写进了 KDoc。用例 +6（`SerialCompletionRunnerTest`）：快速批次先于延迟批次（与优先级数值无关）、同阶段内按优先级且同优先级保注册顺序、快速阶段后取消则延迟阶段整段跳过、收尾恰好一次且必须最后、空区段列表也收尾、执行器不重排批内候选。
- [ ] 8.7 注册上游原生 dummy/char-filter 等生产资源，测试真实入口确实调用新管线，错误态与安全排除位置不崩溃。**`completion.contributor` 已在上游描述符中注册给 `CangJieCfirCompletionContributor`，不得重复注册**（会使其每次补全执行两遍）。
- [ ] 8.8 **修复缺陷 D-5（`Ca` 前缀下线的机械重命名漏改，两处）**：**上游侧两处已修**（提交 `133851357`）——①`ngJieCfirCompletionContributor.kt` 已 `git mv` 回 `CangJieCfirCompletionContributor.kt`，与类名及 `cangjie-code-insight-completion.xml` 的 `implementationClass` 恢复一致；②`impl-shared/test` 下 5 个测试类名已去前缀（文件名此前已改、类名未同步）。`code-insight` 全树已无 `Ca` 前缀类型声明残留（Analysis API 的 `CaSession`/`CaType` 等跨层类型按设计保留）。**仍缺**：验收项中的 `intellij-ide` 与 `deveco` 两个生产描述符 contributor 唯一性核对——该面按分工由宿主侧会话完成，本工作区不覆盖，故本项暂不勾选。
- [x] 8.9 按设计 §3 复核既有 42 个文件的命名与包名，把「采用 `org.cangnova.cangjie.ide.completion.*`」「code-insight 层不用 `Ca` 前缀」记为显式决策。（已完成，2026-10-07）**范围澄清**：设计的「42 个文件」指 `code-insight/completion` 子树，不是整个 `code-insight/`（后者是多特性树，114 个 `.kt`，包名策略各不相同）。复核结果（现 51 个文件＝主源码 34＋测试 17，增量来自本变更新增）：包名**全部**落在 `org.cangnova.cangjie.ide.completion.*`（12 个包），零例外；`Ca` 前缀类型声明 **0**（顶层与缩进都查了）；文件名 = 主声明名有 5 处不满足，逐条判定后全部合规（`CompletionFiltering.kt` 与 `CompletionRecovery.kt` 属 §3.2 明文允许的同族共居；`CfirSnapshotFactory.kt`/`LookupIcons.kt`/`CompletionSectionTestSupport.kt` 是纯函数文件，该条只约束**有主类型声明**的文件）。**显式决策**：①包名统一采用 `org.cangnova.cangjie.ide.completion.*`，不做迁移；②补全层不用 `Ca` 前缀，`Ca` 保留给 Analysis 公共契约；③纯函数文件优先用**职责名**而非 `*Util(s)` 后缀——§3.2 的该条针对真正泛用的工具集合（PSI 侧 `psiUtil/` 那种），本层三个纯函数文件名描述的是职责领域，这是有意取舍并已登记。详见 `implementation-log.md`「8.9 命名与包名复核」节。

## 9. 插件侧：候选生成 B4

依赖：第 7–8 组相应契约。

- [x] 9.1 实现局部、参数、类型参数与隐式 receiver 候选，按位置/前缀/访问过滤并保留身份。（已完成，提交 `f96fe4a41`）局部与参数由 `CfirLocalScopeCompletionSection` 承担；类型参数此前补不出——根因是 `CfirTypeParameterScopeImpl.processClassifiersByName` 是空实现、`getCallableNames()` 也是空集，而 `getClassifierNames()` 却报告了类型参数名，两条通道互相矛盾且公开层无专用入口。已在 `CaScope` 增 `typeParameters` 通道（符号来源 `CfirTypeParameterScope.processTypeParametersByName`，名字来源 `getPossibleClassifierNames()`），类别独立为 `CompletionItemKind.TYPE_PARAMETER`（归入 LOCAL 会被渲染成带括号的调用形态）。回归 `typeParametersAreLexicalCandidates` 断言 `T` 可见且类别正确、同时 `param`/`local` 仍是 LOCAL。**隐式 receiver 候选由 9.2 的成员区段承担，本项不重复实现。**
- [x] 9.2 实现显式 receiver、继承、extend、super 及静态候选，使用实例化签名和适用性结果。
- [x] 9.3 实现类型、顶层 callable、包和 import 候选，区分已导入、alias、未导入和来源。
- [x] 9.4 实现仓颉命名实参与合法实参组合候选，排除已使用参数，不提供 Kotlin 等号形式。（已完成，提交 `e6b30bc10`）新增 `CfirNamedArgumentCompletionSection`（优先级 25）：只在 `NAMED_ARGUMENT` 位置产出；候选来自被调用方的**命名形参**减去本调用已用过的名字（仓颉不允许同一实参名重复出现）；前缀由 `namedArgumentSnapshot` 固定为 `"$name: "` 并从签名上禁止调用方传分隔符，因此不可能写成 Kotlin 的 `name = `。测试三例：已用参数缺席 / 前缀恰为 `"beta: "` 且不含 `=` / 普通表达式位置无候选。仓颉夹具两条规则（cjc 1.1.3 实测）：无名参数必须排在命名参数之前；`!` 只在声明处，调用处写 `alpha: 2`。
- [x] 9.5 实现关键字、声明名称/类型建议、操作符等适用位置候选；每种类型都有 absence 测试。（已完成，提交 `698b5886f` + `39cfaa3c6`）关键字侧完整，absence 断言覆盖 `this`（无接收者时缺席）、`operator`（函数体缺席）、Kotlin 专属关键字（任何位置缺席）、声明关键字（非声明位置缺席）。**类型位置递归缺陷已修复**：根因是 `CaCfirCompositeScope` 的 `classifiers(nameFilter)` / `callables(nameFilter)` 两个重载把 `(Name) -> Boolean` 直接转发给了自己而不是 `vararg names` 重载——lambda 精确匹配 `nameFilter` 形参，于是无限自递归。改为与 `CaCfirBasedScope` 同形的 `classifiers(getPossibleClassifierNames().filter(nameFilter))` 后消失。修复后 `typeSuggestionsAppearOnlyAtTypePosition` 的**正面侧**已成立（类型位置可见 `Holder`、类别 TOP_LEVEL），此前只断言缺席侧。
- [x] 9.6 实现 enum 值/有参构造器/模式候选，按身份和签名去重，同名不同参数个数必须保留。（已完成，提交 `2379fc38b`）新增 `CfirEnumEntryCompletionSection`（优先级 12，注册在成员区段之后）：候选通道与成员区段**同一条**（类型成员作用域 `INSTANCE_THROUGH_RECEIVER`），只把 `CaEnumConstructorSymbol` 单独取出标为 `ENUM_ENTRY`——它们语义上是枚举值而非普通成员，插入形态与排序权重都不同。**Analysis 层零改动**：`CaSymbolByCfirBuilder` 的 callableSymbol 通道已把 `CfirEnumConstructorSymbol` 分派为 `CaCfirEnumConstructorSymbol`，`callableIdentityKey` 与 `renderSignatureTail` 也已有 `CaEnumConstructorSymbol -> payloadTypes` 分支，去重键天然含 payload。仓颉取证（cjc 1.1.3）：`enum Shape { | Dot | Dot(Int64) }` **合法**，同名不同 payload 个数必须各自保留，按显示名去重会让用户选不到无参那个。测试三例：同名不同参数都保留且尾文本与去重键两两不同 / 类别统一为 ENUM_ENTRY / 非枚举接收者处不产出。
- [x] 9.10 新增**索引召回区段**（`CfirCompletionService.sections` 现为固定的 keyword / localScope / topLevel / member 四段，无索引段）：按 `CaSymbolIndexQuery` 的前缀过滤 + analysisScope 限定召回未导入 / 跨模块候选，与作用域路径候选**共用同一去重器**，被遮蔽的索引候选按 `CaScopeKind` 决定存活。无此段则持久化索引没有消费方。（已完成）新增 `CfirIndexCompletionSection`（优先级 50，注册于 `CfirCompletionService.sections`，现为七段）。入口不直接解析平台服务——补全层只依赖 `analysis-api`，因此新增的会话入口是 `CaSymbolProvider.getDeclarationsByNamePrefix(prefix, isRepeatedInvocation)`，由 `CaCfirSymbolProvider` 委托平台索引并把**名称条目**按包作用域恢复成符号；名称→符号的转换只有这一处，三个索引实现共用。**共用同一去重器**：`CfirCompletionService.complete` 统一走 `distinctByIdentity`；此次顺带修掉一处会破坏该前提的缺陷——`classifierSnapshot` 的身份句柄原先只取短名，同名类型跨包会给出同一去重键，现改为 `classifierIdentityKey`（class-like 取 ClassId 字符串，局部类型/类型参数回退短名），`CfirCompletionRecoveryService` 的类型定位同步改用它。**遮蔽按 CaScopeKind 决定存活**：区段按 Analysis 的位置作用域上下文收集已绑定短名集合，跳过同名不同身份的索引候选。**不退化**：索引不可用时返回空集合，不做全工作区声明扫描。测试 4 例（未导入候选可见且带 `AddImport` 计划 / 无依赖模块不召回 / 索引召回一次但恢复展开两个重载且去重键不同 / 被本文件声明遮蔽者不存活），另加 `IndexSectionTestServiceRegistrar` 按生产实现在测试宿主补注册服务——否则缺服务会表现为「空候选通过」，掩盖未注册。**未做**：`.cjo` 名字未进召回集（见 6.6④），故跨二进制依赖的召回尚无覆盖。

## 10. 插件侧：Lookup、排序与插入 B5/B6/B7

依赖：第 9 组可分候选家族逐步接入，不以只展示名字作为完成。

- [x] 10.1 建立稳定 lookup 数据与各类工厂，在 analyze 内完成语义渲染，UI 不捕获裸 symbol/type/session 或分析 Sequence。（**重开条件已满足**：缺陷 D-2 已由 `f6b306951` 修复，身份键改为与框架同源并带签名；`CfirTopLevelCompletionSectionTest.topLevelOverloadsCarryDistinctSignatures` 锁定「同名不同参数的候选同时出现且各带实例化签名」）
- [x] 10.2 实现 pointer 恢复、文本版本/区间校验，覆盖 analyze 已结束后的展示与选择，失效候选不得写文档。（已完成，宿主安装 Lookup 监听器除外）**pointer 恢复**：`CfirCompletionRecoveryService.recover(useSite, identity)` 重新进入分析、在使用点的作用域里按**身份键**（不是短名）重新定位并复算导入需求；同名重载各自恢复回自己。**文本版本/区间校验**：`InsertionValidator` 校验文档修改戳与替换区间，区间越界一律拒绝写入而不是尽力修正。**失效候选不得写文档**：新增复查结论通道（`RevalidationVerdict` 三态 + `LookupElement.revalidateSelection`），失效 → 契约里早就预留的 `InsertionValidity.DanglingCandidate`，且**先于**文档版本判定；`writable` 为 false 时插入函数一个字符都不写。**复查失败降级为「未复查」而不是「失效」**——EDT 不允许分析、会话过期、用户取消都是环境限制，把环境失败判成候选过期会让用户在正常环境里选什么都插不进去。**分析方案与写事务分离**：复查在选择期（读动作）完成，写事务里只读结论。**宿主侧未做的最后一步**：本模块的 `compileOnly` 只有 `intellij-core`、`analysis`、`indexing`，**不含平台 `LookupManager`**，因此监听器安装必须在宿主完成；仓库已提供接缝 `AbstractCangJieCompletionContributor.recoveryService`（open，未装配为 null）+ `revalidateSelection(item, useSite)`，宿主在 `currentItemChanged` 里调用即可。宿主未调用时行为退化为「不做语义复查」，文档版本与区间校验仍生效。用例 +3（失效优先于文档过期、未复查不改变既有校验、悬空身份/关键字身份等既有 5 例保持）。
- [ ] 10.3 实现分析期权重及平台 sorter 组合，验证来源、import 成本、类型匹配、类别与稳定顺序，不为所有 weigher 注册 XML。**（七维中六维已落地，仅「参数匹配」未做，故不勾选）**已落地：来源 / import 成本 / 期望类型匹配 / **类别** / **弃用** / 同级声明次序，各占一个互不重叠的位段（`1e6 > 1e3 > ±200 > 50 > -10 > 0..9`），且每一维的步长**严格大于其下各维的全幅跨度**——低位维度拿到极值就会越过高位维度，而这种错排是静默的，不报错、只是顺序看起来没道理，因此有专门用例用 `Int.MAX_VALUE`/`MIN_VALUE` 逼出极值验算。**类别**只压低附属条目（`DOCUMENTATION`）：不给各类符号之间定次序——同一位置上的候选已被位置分类限制在同种类里，跨种类次序没有可依据的规范；`CompletionItemKind` 目前也没有「声明生成建议」（该值在 `CompletionIdentityKind` 里），将来引入时其权重必须落在 `DOCUMENTATION` 一档或更低。**弃用**只认**内置** `BuiltInAnnotationKind.DEPRECATED`，不按名字匹配。**不为所有 weigher 注册 XML**：`code-insight` 下 `completion.weigher` 注册数为 0，`priorityOf` 一次性把分析期权重折算成单个平台优先级（`PrioritizedLookupElement`）。**未做：参数匹配**——设计全文只在 §7.3 的覆盖清单里提过一次，未给口径；仓内也没有「参数签名/期望参数」契约，真正的适用性检查属于尚未实现的 A4（`CaCompletionExtensionCandidateChecker` 只覆盖 extend 适用性），而 A4 又明确禁止用三态旧接口替代。因此不臆造一套排序规则：`ExpectedTypeMatch.UNKNOWN` 时不产生任何影响，这就是当前的降级行为。
- [x] 10.4 实现 SMART 期望类型过滤/构造与未知降级，和 BASIC 的集合预期分开验证，不因重复调用放开访问权限。（**重开条件已满足**：同 10.1，缺陷 D-2 已由 `f6b306951` 修复；`CfirExpectedTypeCompletionFilterTest` 4/4 保持 BASIC 不查期望类型、SMART 未知时与 BASIC 同集合、TYPE 位置只留类型身份、触发次数不参与过滤）
- [x] 10.5 实现 no-import/add-import/qualified-then-shorten 策略与使用点目标复验，复用现有 PSI 构造但不依赖未实现 shortening 的 bind 方法。（已完成）三种策略由 `PsiImportPlanApplier` 承担（9 例）：`NoImport` 原样跳过；`AddImport` 追加到 import 区末尾、已有同包分组时**并入分组**而不是写平行行、已存在等价 import 时幂等跳过；短名被别的声明占用时**不硬插**，改报冲突并退回「写限定名 + 后续缩短」，且 `QualifyThenShorten` 本身不写任何 import 行——不依赖尚未实现的 shortening bind 方法，只把 `qualifiedExpressionText` 交给调用方。**使用点目标复验**（本轮补齐）：复查改按**结构**判定可达性，不再逐行比文本。此前用「整行 == 目标包路径」判断，两个方向都错且都用 cjc 1.1.3 实测确认：①**漏判**——仓颉里 `import a.b`（包级）、`import a.b.c`（声明级）、`import a.b.*`（星号）三种写法都能直接引用 `c()`，只比整行文本会漏掉后两种，于是往已经能用的文件里重复写一行 import；②**误判**——`import a.b` 既可指包 `a.b` 也可指包 `a` 中的声明 `b`，语法同形，文本无法区分。现按 PSI import 项判定：星号导入按包名比对，包级与声明级按完整名比对。顺带修掉 `CjImportItem.toImportPath()` 里的 `takeIf { !isAllUnder }`——它恰好丢掉了 `ImportPath.isAllUnder` 存在的意义；该映射由 private 放开为公开，作为「PSI import 项 → ImportPath」的唯一映射复用。用例 +2（声明级/星号导入已存在时复查不得再要求写 import）。
- [x] 10.6 实现 Enter/Tab/字符选择的括号、已填实参、命名冒号、caret 和替换处理。（已完成）**括号与已填实参**：括号形态改以**文档现状**为准——分析期的 `insertion` 只表达意图（要不要按调用形态插入、caret 在哪一侧），写入时从文本判断三种现状：无左括号补一对并把 caret 放进括号内；已闭合且括号内有非空白内容则**一个字符都不写**、caret 停在右括号之前给已填实参让位（`foo( )` 只含空白视同空括号）；已写下左括号但未闭合也不补——原实现会写出 `foo(()`，那是无法通过增量输入恢复的文本。**命名冒号**：命名实参走独立分支写 `name: `（`namedArgumentUsesCangjieColonForm`）。**caret**：`INSIDE_NEW_PARENTHESIS` / `BEFORE_EXISTING_CLOSING_PARENTHESIS` / `AFTER_INSERTED_TEXT` / `AFTER_NAMED_ARGUMENT_COLON` 四种策略各有断言。**替换处理**：替换区间由平台的 `startOffset`/`tailOffset` 给出，`InsertionValidator` 在写入前校验区间仍落在文档内，越界一律拒绝写入而不是尽力修正。**触发方式**：删除契约里的 `InsertionTrigger`——它声明了 Enter/Tab/字符选择三种触发却**从未被任何代码使用**；三者的差别由平台的替换区间与当时的文档状态共同表达，插值阶段不需要也不应再分叉，保留一个没人读的枚举等于对外承诺不存在的分别处理。用例 8 → 11 例（新增：已有实参不被破坏、只含空白的括号算空、未闭合括号不被补成双写；原先「已有括号时 caret 停在插入点」的断言按新行为改为落进空括号内）。
- [x] 10.7 实现组织名/alias、分组 import、冲突处理和幂等导入；分析方案与写事务分离，完整支持单次撤销。（已完成）**组织名/alias**：`PsiImportPlanApplier` 只把它们写进 import 行，表达式侧一律用 `CompletionImportTarget.expressionText`，绝不把 `org::` 前缀抄进表达式访问。**分组 import**：已有同包分组时**并入分组**（`import a.{b, c}`）而不是写平行行；分组判定按**分组前缀**而不是成员自身的 `localFqName`（后者只是组内相对名，拿它算父包必然为空、永远匹配不上）；组织名参与判定——不同组织的同名包路径不是同一命名空间，并组会产生二义；跨包不并组。**冲突处理**：短名被别的声明占用时**不硬插**，报告 `SHORT_NAME_TAKEN_BY_OTHER` 并退回「写限定名 + 后续缩短」；同名同路径是幂等情形，由 `covers` 先处理，不误报冲突。**幂等导入**：已存在等价 import 时不重复写入、不产生空行或重复项；同一批计划内新增的路径也立即对后续计划可见。**分析方案与写事务分离**：应用器只接受算好的 `plans`，返回报告而不直接改 PSI 结构；写入必须发生在写命令内（`insertImport` 走文档文本 + commit，对带 stub 的 `CjImportDirective` 调 `addAfter` 既不保证 reparse 也拿不到插入位置）。**单次撤销**：断言导入写入留在调用方给的命令内——应用器若自己开命令，命令名会被改写，用户按一次 Ctrl+Z 只退回一半。**说明**：撤销子系统在轻量解析夹具里未注册（`UndoManager.getInstance(project)` 返回 `null`），因此无法在仓内真的按一次 Ctrl+Z，断言的是「同一条命令」这一成立条件。用例 9 → 10。
- [x] 10.8 增加各候选家族的 after/caret/目标身份/import 测试，真实触发并选择，覆盖插入前已编辑文档的情况。（已完成，平台级「真实触发」除外）新增 `CfirInsertionIntegrationTest`（4 例）把此前各自验证一半的两部分接起来：**真实区段产出快照 → 模拟平台前缀替换 → 走产品的校验与插入路径 → 断言最终文本/caret/身份/导入计划**。两半之间的假设正是最容易出错的地方——快照里的身份与插入数据必须真的能驱动出预期结果。四维覆盖：**after**（文档出现 `target()`／`return`／`insertionTarget()`）、**caret**（callable 落括号内、关键字停插入文本之后）、**目标身份**（`SYMBOL`/`KEYWORD` 身份类别与身份名）、**import**（未导入候选的计划必须是 `AddImport` 且路径为候选所属包）、**插入前已编辑文档**（改动后校验报 `StaleDocument`，一个字符都不写、文档与编辑后逐字一致）。**未做**：平台级「真实触发」（真正经 `completion.contributor` 由平台拉起补全再选择）——那是 8.7 生产注册与 B12 阶段验收的范围，本节只做到区段入口这一层。两处环境约束已写进用例注释：不能直接调 `CfirCompletionService`（它取 `ProgressManager` 的进度指示器，测试线程上为 null），文档写入必须 EDT + 命令上下文。

## 11. 插件侧：完整高级场景 B8

依赖：第 9–10 组；全部属于最终范围，不得只交付基础闭环后遗漏。

- [x] 11.1 完成构造/实例化候选、适用泛型参数与实参组合的展示、过滤和插入。（已完成，提交 `f6c8319ca` + `0835062b0`）**位置决定形态**：调用位置（表达式、函数体、代码片段）给**构造器候选**（带括号、尾文本渲染实参组合），类型位置给类型候选——跟随左括号会让用户写出 `Widget(` 这种非法源码；判据是位置形态、不靠文本启发式。**适用泛型参数**进尾文本：`<T>(value: T)`，前面是适用的类型参数、后面是实参组合（取自 `CaTypeParameterOwnerSymbol.typeParameters`；无类型参数者不加前缀）。**实参组合**：一个类的多个构造器逐个成候选，靠尾文本区分——它们共享同一 ClassId，去重键因此必须含参数列表。**过滤**：前缀过滤 + 位置过滤。**插入**：候选插入形态即调用形态（`insertParenthesis`）；**不自作主张插 `<`**——类型实参由用户写，补全只负责告诉他有哪些适用。**过程中撞到一个静默返回空的通道**：构造器最初走 `declaredMemberScope.callables { true }`，实测**那里没有构造器**，表现为调用位置永远给不出构造器候选且不报错；改走 PSI → 符号的标准恢复路径（类头 primary + 类体 `init` 两者都收）。另一条语言事实（cjc 1.1.3 实测）：`class Box<T>(value: T)` 是**语法错误**，primary constructor 只能经类体 `init` 或类头无参形式声明。用例 +4。
- [x] 11.2 给现有 override-implement 生成能力提取必要窄接口并由补全复用，验证成员生成结果，禁止跨模块访问 internal 实现。（已完成，提交 `2e1b0877d` + `7747f21c7`）**窄接口**放在共享的 `code-insight/api`（`CangJieMemberGenerationService` + `CangJieGeneratableMember` + 注册表），只暴露「成员名 + 渲染后的声明文本」——chooser 对象、符号指针、analysis 会话都不在契约里，生成器重构不会波及补全。**补全复用**：新增 `CfirDeclarationSuggestionCompletionSection`，`impl-cfir` 只依赖 `:code-insight:api`，**不依赖** `:code-insight:override-implement`，更不触碰其 internal 实现；未注册服务即无候选（明确降级）。**验证成员生成结果**：`override-implement` 补测试源集依赖与真实 Analysis 会话用例（未实现的抽象成员被收集、渲染文本含声明）。**过程中的两处前提冲突**：①最初复用 `collectMembersToGenerateUnderProgress`，在后台线程上直接死锁（`Can't invokeAndWait from WT to EDT`）——那是给 UI 动作用的同步模态进度包装；改调内层 `collectMembersToGenerate`（带 `@RequiresBackgroundThread`），说明「复用既有能力」不等于「复用既有入口」。②新候选类别 `DECLARATION_SUGGESTION` 必须同时补恢复语义：不加进永不失效清单，它会走符号恢复被判失效，表现为「每个声明建议都插不进去且不报错」；理由与关键字/文档项不同——它不引用已有声明，其有效性取决于「该类型此刻是否仍缺这个成员」，那是生成器的判断。**同时核出一条模块级缺口**：模块叫 override-implement，但**只有 implement 半有具体处理器**（`GenerateMembersHandler(toImplement = false)` 无具体子类也无注册），接口因此不提供恒走空分支的开关。
- [x] 11.3 完成 super/声明建议及仓颉 match、enum、extend 相关上下文，按官方语义维护差异表。（已完成，提交 `95e9e0c1c` + `5adbd34a1`）**match**：模式位置给出主语表达式类型的枚举构造器（候选来源与成员访问路径完全同源，区别只在「枚举从哪来」：成员访问从接收者，模式位置从 `CjMatchExpression.subjectExpression` 的类型）；位置判据按**偏移划界**而不是「有没有解析出模式」——一个 case 子句里既有模式也有体，只有模式那一段是模式位置，而用户正打到一半时模式往往还没成型，按结构判会漏掉最常见的时刻。**enum**：见 9.6。**extend**：见 9.2。**super**：`super.` 之后给父类成员——`super` 不是普通接收者、没有 `expressionType`，按普通接收者解析必然拿不到类型；判据用 PSI 类型（`CjSuperExpression`）而**不是文本**（仓颉允许把关键字写成转义标识符形式，按文本判会把普通变量当成 super）；父类取超类型列表的**第一个**，因为仓颉的 super 只指向直接父类、接口不参与。**声明建议**：见 11.2 的 `CfirDeclarationSuggestionCompletionSection`（本项点名的那一半由 11.2 落地）。**差异表**：新增 `completion-differences.md`，11 条差异逐条附证据来源（cjc 实测输出 / 官方手册篇名 / 仓内文件路径），并写明「未列入本表的项」与「维护规则——新增差异必须同时给出证据与受影响的实现点」。**过程中的同一形态缺陷重现一次**：super 分支最初判在普通解析**之后**，而 `resolveAccessTarget` 对 `super` 返回的是**错误类型**而不是 null，导致 super 分支永远不执行、候选恒为空且不报错——与未导入 receiver 那次完全同形。
- [x] 11.4 完成 CDoc 标签/参数/引用补全与源码定位，不原样复制 KDoc 规则。（已完成，提交 `a80820de3`）实现 `CfirCDocCompletionSection`：**标签名**取自 `CDocKnownTag`（仓颉自己的词法解析器定义的枚举，**不抄 KDoc 的标签表**），已出现过的标签不再给，插入文本**不带 `@`**；**参数名**取**所属声明的形参列表**而不是全文件名字（`@param` 的语义就是「这个函数的这个参数」），所属声明按「第一条起点在这条注释之后的 callable」定位；**引用**取文件导入作用域内的分类器。**源码定位**：引用候选是真实符号，携带 `qualifiedName` 与身份键，足以定位回声明。**过程中修通一个一直不可达的位置种类**：`DOCUMENTATION` 从未被分类出来过——文档注释由 CDoc 词法产出 `CDocImpl`，**不是 `CjElement`**，注释内最近的 CjElement 祖先是整个文件，`classify` 只返回 `EXPRESSION`（实测：`CDocImpl isCjElement=false` → `depth=2 CjFile isCj=true kind=EXPRESSION`）。后果是注释内候选此前走普通表达式路径、会给出代码符号，而既有代码里对 DOCUMENTATION 的排除（关键字区段、期望类型过滤器）是**死分支**。修法是加性的：`CompletionRequest` 新增 `positionElement`（默认取 `completionPosition`）承载光标处的**原始叶子元素**，位置形态分类改用它，分析位置不变，所有现有调用点无感。用例 +5。**未做（非本任务点名，留作细化）**：把 CDoc 链接的目标位置回写进候选、以及与本仓 `CaCDocProvider` 结构化模型的显式对齐。
- [ ] 11.5 完成宏 Tokens/普通表达式/可靠展开映射的上下文分流，并覆盖 .cjo 无源码及缺 SDK 降级。**（分流与降级已落地；展开映射缺分析层契约，故不勾选）**①**前置**（`cc4d5f779`）：`MACRO_TOKENS` 此前声明了却从不产出——判据落在 `CjMacroAttr` 上（属性宏的 `@Foo[1+]` 方括号里是 token 序列而非表达式，语法取自官方手册《实现宏》），与 `CjMacroInput` 区分开。②**分流**（`219b0afa5`）：新写的分流用例直接把问题打了出来——token 位置上关键字区段给出 `[let, if, return]`、索引区段给出**整个 SDK 的全部声明名**。根因是分类入口不统一：11.4 新加的 `positionElement` 只接进了 CDoc 区段，**其余 7 个区段与期望类型过滤器仍在分类 `completionPosition`**（分析位置），而文档注释与宏 token 的最近 `CjElement` 祖先是**整个文件**，分类只会得到 `EXPRESSION`。已加请求级入口 `CompletionRequest.positionContext` 并统一 8 处调用点。分流用例手写全部区段清单（服务入口需要进度指示器），断言「**所有**区段都不产出」且**不抛异常**。**未做**：①宏展开后可映射代码的识别与可靠源映射判定——**缺的是分析层契约本身**（全仓 grep `macroExpand|expandedFrom|sourceMapping` 无任何入口，宏展开结果不进入可分析 PSI/CFIR），不是补全层漏接；须先有「展开后可映射代码」的分析层表示，本变更范围内的补全层才谈得上分流。②`.cjo` 无源码降级**已落地**：宿主**无索引实现**时，源码模块的候选必须缺席（`SourceDeclarationsNeedHostIndexTest`、`CfirIndexDegradationTest`），库侧（`.cjo`）名字必须照常给出——为此 `CfirSymbolProvider` 新增 `libraryDeclarationsNamesProvider`（详见 `implementation-log.md` §11.5②）；「缺 SDK」在现有测试框架下**不可构造**（无 SDK-less 宿主开关），其行为由构造保证：库侧视图折叠为空 ⇒ 无候选且不抛异常。**交接**：此改动的代价是宿主必须注册 `CaSymbolIndexQueryService`（LSP 已补，IDE 属 §6.6②）。
- [x] 11.6 实现未解析/未导入 receiver 的精确名称恢复和普通成员重补全，协调导入及最终插入，设置取消和预算；不扩展成任意深度表达式链搜索。（已完成，提交 `13912cc41`）成员区段在接收者解析失败时做一次**有界**的名称恢复，并复用原有的成员与 extend 通道（不另建候选来源）。**精确名称**：索引接口按前缀召回（长前缀退化为子串匹配），因此必须再按「短名完全相等」过滤一次，否则 `Foo` 会把 `FooBar` 带进来。**取消与预算**：`ProgressManager.checkCanceled()` 在恢复前检查；预算是**结构性**而非超时的——三条约束（裸标识符 / 精确同名 / 必须唯一）把工作量钉死在「一次索引查询 + 一次类型解析」，不存在需要额外设阈值的长尾。**不扩展成表达式链搜索**：用类型检查表达（receiver 必须是 `CjSimpleNameExpression`），写成 `a.b.c` 时 receiver 是限定表达式，直接放弃。**协调导入与最终插入**：只插入 `Widget.size` 而 `Widget` 不可解析会得到编译不过的代码，因此恢复出的候选带接收者所属包的 `AddImport` 计划。**歧义不猜**：同名类型来自多个包时给不出确定的接收者类型，不给候选——随便挑一个会把用户带到错误的成员集合，比不给更难发现。**过程中撞到一个会让功能彻底静默失效的判据错误**：触发条件最初写成 `resolved == null`，但未解析引用的 `expressionType` 返回的是 **`CaErrorType`** 而不是 null，恢复路径因此永远不触发、候选恒为空且不报错；改为 `resolved == null || resolved.type is CaErrorType`。用例 +2。

## 12. 插件侧：两种制品与宿主装配 B10

依赖：第 1、8 组可先配制品；最终加载/行为验收依赖第 9–11 组。

- [x] 12.1 创建 completion fat jar 和 module 制品，显式列出 API/shared/impl-cfir 合并输入，复用现有 publishing helper。**（已完成并验证）**两个制品在 `settings.gradle.kts` 已注册（fat jar 与 module 形态各一条）：fat jar 模块显式列出三个合并输入（`:code-insight:completion:{contracts,impl-shared,impl-cfir}`）并复用 `publishCangjieJarsForIde`；module 形态以 `api(project(...))` 依赖三者。实测构建：fat jar 147 个类**全在 `org/cangnova/cangjie/ide/completion` 下**（未混入 common/psi/analysis），module 形态 jar 261 字节——与其它 module 制品一致。
- [ ] 12.2 更新主 settings/目录文档及宿主 Version Catalog/substitution，避免重复打包已有 common/psi/analysis 类。**（在分区内的部分已完成并验证）**主 `settings.gradle.kts` 已注册两个制品，`docs/module-catalog.md` 已登记；「避免重复打包已有 common/psi/analysis 类」按 fat jar 内容实测成立（147 个类全在 `ide/completion` 下）。**未做**：宿主侧 Version Catalog / substitution（`intellij-ide`、`deveco`，不在本工作区）。
- [ ] 12.3 更新 base compileOnly/test、product runtimeOnly/test 和 test-support 测试运行时依赖，禁止将测试 plugin.xml 混入产品。
- [ ] 12.4 从当前 base 模块描述符 include 单职责补全 XML，验证所有 class/resource 可加载且只注册一次，不使用不存在的 cangjie-all.xml。
- [ ] 12.5 加入真实产品 contributor/service 接线测试，仿 quick-fix 生产描述符测试模式，随后真正触发并插入。
- [ ] 12.6 分别验源码桥接与发布制品、最终插件 ZIP 的类/XML 唯一性；确认非开发机绝对路径也可解析依赖。
- [ ] 12.7 将平台 API 差异置于 platform 对应目录并验证承诺的平台矩阵，未测平台不得声明兼容。

## 13. 插件侧：并行、降级及性能 B3/B9

依赖：第 2、8、10 组串行正确性；并行未通过前维持关闭。

- [ ] 13.1 增加 dumb/restricted-analysis/no-SDK/cancelled/failure 分流，无索引时只提供约定安全候选，禁止全项目扫描兜底。**增加 LSP 侧分流：`$/cancelRequest`、会话过期错误、无 SDK 基线**（无头宿主默认无 SDK/无索引构建期，不得退化为全工作区声明遍历）。**（LSP 侧三项进度：`$/cancelRequest` 已完成、会话过期错误已完成、无 SDK 基线未做——前两项见 `implementation-log.md` 15.10 与 15.6/15.7 节；无 SDK 基线在现有测试框架下不可构造，见 11.5 的说明）**
- [ ] 13.2 实现阶段耗时、候选量、索引查询和取消统计，不记录完整源码；建立冷/热、小/大项目可复测基线及预算，**预算须写入具名测试配置文件后才可评估阶段门禁**。**增加 resolve 阶段耗时与会话过期错误计数（不并入弹窗路径）**。**（前半完成）**统计接缝与接线已落地：`CompletionStatisticsSink`（只记数值、不阻塞主路径、宿主可不装）+ OTel 实现 + 项目服务装配点，接线覆盖 `CANDIDATES`/`INDEX_QUERY`/`SETUP`/`RENDER`/`INSERT` 与 `CANCELLED`/`SESSION_EXPIRED`/`VERSION_MISMATCH`/`POSITION_NOT_ALLOWED`/`SERVICE_UNAVAILABLE`；用例 `LspCompletionStatisticsTest` 2/0。**未做**：冷/热与大/小项目可复测基线及预算（须写入具名测试配置文件）、`resolve` 阶段耗时（`resolve` 未实现）。
- [ ] 13.3 实现独立 Analysis 会话与 section sink 的并行执行器，以稳定顺序归并结果，禁止共享裸会话状态。
- [ ] 13.4 比较串并行候选、排序、取消、异常和增量失效结果，达成性能与隔离准入后才允许开启并行选项。

## 14. 插件侧及跨层最终验收 B11/B12

依赖：前述全部对应项。

- [ ] 14.1 移植适用的 EXIST/ABSENT/NUMBER/NOTHING_ELSE/WITH_ORDER/INVOCATION_COUNT 测试 DSL，源码用仓颉语法，不复制 Kotlin fixture 答案。
- [ ] 14.2 用 host light fixture 验真实 BASIC/SMART、自动/手动、增量修改、after/caret/import/undo，覆盖源码/库与正反候选。
- [ ] 14.3 在 LSP 未启动条件下验 native contributor；回归 Analysis 变更所影响的 `:lsp` 与 `:analysis:*` 窄集，用真实语义测试，不用协议假实现替代原生验收。（原「回归 Analysis 变更影响的 LSP 方法」半句已退役：LSP 侧旧实现已整体迁至 `legacyCompletion`。）
- [ ] 14.4 执行受影响模块窄集后再执行跨层回归，记录命令、提交/文件基线、结果及未覆盖项；修复失败后重新运行对应集合。
- [ ] 14.5 授权运行时，在真实 sandbox 与最终分发插件中验加载、触发、排序、插入、撤销、错误态与平台矩阵，保存可复查记录。
- [ ] 14.6 仅在各准入完成后调整 native/SMART 默认开关，保留独立停用路径；更新责任域和用户文档，逐项确认没有以阶段完成代替整体完成。

## 15. 插件侧：LSP 接入 B11

依赖：§7.1–§7.7（插入链路可用）。LSP 与 IDE 共用同一候选生成实现，`:lsp` 只做协议适配与扩展点装配，**不在 `:lsp` 重新实现候选生成、可见性或排序**。规格见 `specs/lsp-completion-support/spec.md`。

- [x] 15.1 **前置最小实验（必须先做）**：判定 ①LSP 生产容器能否提供 `Editor` / `CompletionProcess`；②**分析跑在哪份 `CjFile` 上**——B-1 自行构造 `analyzeCopy` + `CaDanglingFileModule` 补全副本（语义与 IDE 一致，但**受缺陷 D-3 阻塞**），或 B-2 直接分析 `LspAnalysisVirtualFile`（绕开副本，但**与 IDE 分析语义不等价**，须另补等价性对照用例）；③`invocationCount` 与 `completionKind` 的映射规则（IDE 侧靠 `INVOCATION_COUNT_KEY` 用户数据与 `completionType`，LSP 侧须自行定义）。扩展点 prime **不是未知项**（`CangJieRefactoringHeadlessRegistrar` 已用同一机制 prime 8 个扩展点）。已记录的「轻量夹具不注册 `EditorFactory`」只覆盖**测试夹具**，不能外推。**三项结论一并写入 `implementation-log.md`，未记录即视为本组未开始。**（已完成，2026-10-08）**结论＝分支 B**。①**不能提供**：`:lsp` 的 `intellijCore()` 制品里 `com/intellij/openapi/editor` 类数为 0，平台 `core` 制品在该包下 42 个类中也**没有** `Editor`/`EditorFactory`，无头容器亦不注册 `EditorFactory`（`CoreApplicationEnvironment` 常量池无该符号）——判据在依赖与装配层面封闭，故分支 A 不成立，**不注册 `completion.contributor`**。②**已实测判定：自建副本 + 在副本上修正 + 分析副本**（`LspCompletionPipelineProbeTest` 按生产容器跑通）。既有旧路径只处理完整标识符、不改 PSI，故不能证明新路径；新路径的 dummy 修正是 PSI 写，改文档那份会污染 `LspDocumentStore`。实测结论：副本可由 `LspAnalysisPsiFileFactory.createFile` 造，use-site 模块仍由 `projectStructureState.useSiteModuleForOpenDocument(uri)` 给出，**不需要** `CaDanglingFileModule`、**不受 D-3 阻塞**。实验同时打出三条生产容器约束（缺一条即静默零候选）：`CangJieCompletionServiceRegistry` 必须注册、修正必须在 EDT 写命令内、`complete` 需要进度指示器——详见 `implementation-log.md` 15.1 节 ②。③**映射规则**：`kind` 恒为 `BASIC`（LSP 无 SMART 触发，故 `CfirExpectedTypeCompletionFilter` 在 LSP 路径不生效，属有意）；`invocationCount` 取 `1 + 同一 (uri, documentVersion, offset) 的重复请求数`，信号是 `triggerKind == TriggerForIncompleteCompletions`，需 15.7 的会话存储落地。三项结论与全部证据见 `implementation-log.md`「15.1 §7.11.0 前置最小实验」。未做的部分如实记录：**未做运行时探测**（判据已封闭，且当轮共享 Gradle 缓存被占用）。
- [ ] 15.2 （分支 A）**—— 15.1 已判分支 B，本项一半适用**：**三个 completion 模块依赖照加**（分支 B 也要直接调用 `CfirCompletionService`，不加依赖连编译都不过）；**只有「prime `completion.contributor` 扩展点」这一半不适用**（分支 B 不注册 contributor）。故本项在分支 B 下的验收应改为「`lsp/build.gradle.kts` 能引用到候选管线的公开契约与服务入口」，不再要求「真实触发一次补全走平台 contributor 管线」。`lsp/build.gradle.kts` 新增三个 completion 模块依赖；新增 `LspCompletionHeadlessRegistrar` prime 平台扩展点。验收：真实触发一次补全并断言走仓颉管线，**禁止手工 new contributor 塞进扩展点**。
- [ ] 15.3 （分支 A）**—— 15.1 已判分支 B，本项不适用**：分支 B 不装载补全描述符，因此**不需要**扩 `allowedExtensionPointNames`，本变更（在分支 B 下）**不存在**「为消费点改动上游」的范围例外；`docs/architecture-host-plugin.md` 应据此改记「无此例外」，而不是留着一段不再成立的登记。分支 A 的内容照原文保留，供将来若改走 A 时使用。上游 `PluginStructureProvider.allowedExtensionPointNames` 增补**枚举条目**（不得写「等」）；在 `docs/architecture-host-plugin.md` 登记本变更唯一一处「为消费点改动上游」的范围例外；补测试证明 standalone 与测试容器**未因此获得错误的补全行为**。
- [x] 15.4 （分支 B）补齐四项前置，缺一即静默返回空候选：①新增接受 `(CjFile, offset)` 的**无参占位修正入口**（现有唯一入口 `CompletionDummyIdentifierProvider.provide` 依赖 `CompletionParameters`，其辅助均为 private）；②`documentStamp` 接 `LspTextDocument.version`（否则过期判定建立在常量 0 上）；③`CfirCompletionServiceProvider` 在 LSP 容器注册（上游描述符无 `<projectService>` 条目）；④转换对象改读 `CompletionSnapshot` 而非 `LookupElement`（分支 B 不经 `LookupElementFactory`）。**第 5、6 项前置（分析对象与 invocationCount/kind 映射）由 15.1 的实验一并判定。**（2026-10-08 进度）四项目前状态：①**已存在**——`CompletionDummyIdentifierProvider.provideFor(file, offset)` 在 8.4 落地（提交 `c4c6c55af`，含回归），其 KDoc 已写明「无头宿主（LSP）同样拿不到平台补全参数」，本项无需再做；③**已接线**（`AnalysisApiLspServiceRegistrar` 注册 `CfirCompletionServiceProvider`，与 `lsp/build.gradle.kts` 的三模块依赖同批，**尚未编译验证**，见 `implementation-log.md`「15.x 分支 B 起步与待验证项」）；②④**未做**。第 5、6 项已有结论（15.1 ③：分析对象＝B-2 现成路径；`kind` 恒 BASIC、`invocationCount` 由会话存储的重复请求计数给出）。**（2026-10-08 复核：四项全部落地）**①`CompletionDummyIdentifierProvider.provideFor(file, offset)` 已存在（8.4，提交 `c4c6c55af`）；②`documentStamp` 已接 `LspTextDocument.version`（`AnalysisApiCangjieAnalysisFacade` 构造快照处 `document.version.toLong()`）；③`CfirCompletionServiceProvider` 已在 `AnalysisApiLspServiceRegistrar` 注册，`:lsp` 编译与测试通过；④`LspCompletionItemConverter` 只读 `CompletionSnapshot`，不经 `LookupElementFactory`。验收「能引用候选管线的公开契约与服务入口」由 `LspCompletionPipelineIntegrationTest` / `LspCompletionPipelineProbeTest` 承担。
- [ ] 15.5 新增 `LspCompletionItemConverter` + `LspCompletionItemKindProvider` + `LspCompletionSortingUtil`。字段映射对位 kotlin-lsp；**空 `textEdit` + `CompletionItem.command` 走 LSP 标准路径**（客户端接受候选后自动执行命令）。验收：同一候选两端 `label`/`tailText`/`typeText`/`kind` 一致，`sortText` 顺序与上游排序一致。**（部分完成，提交 `9472d1c18`）**三个组件已落地并各有用例（7 + 6）：映射按快照冻结的事实判定（形状 + 身份种类 + 签名首字符），`sortText` 取列表下标零填充；另加 `LspCompletionEditCalculator`（复用共享 `performCangjieInsertion`，把「替换已键入前缀」算成删除编辑，并把插入坐标折算回原文）。**未做**：①「两端一致」的验收要 IDE 侧对照（不在本工作区）。②`resolve` **已做**（提交 `a654d27d1`）：按身份重进分析恢复声明 + 复用门面 CDoc 渲染，`data` 携带 `{会话键, 下标}`；但**本容器里 `documentation` 恒为空**（light PSI 上 `docComment` 为空，仓内 CDoc 用例在真实文件夹具上才是绿的），该限制由 `resolveCannotFillDocumentationInThisContainer` 锁定。③命令路由与 `applyEdit` 已完成（见 15.6/15.7）。**（2026-10-08 复核）**①②③均已落地，`resolve` 见提交 `a654d27d1`。仅剩的「两端一致」对照属 IDE 侧，由负责 `intellij-ide` 的会话执行，本工作区不勾选。
- [ ] 15.6 打通命令与 `applyEdit`：`CangjieServerCapabilitiesFactory` **声明** `capabilities.workspace.applyEdit = true`（当前未声明，服务端无权发送该请求）；`CangjieLanguageServerDescriptor.executeCommands` 加入 `applyCompletion`（当前空列表）；`CangjieWorkspaceService.executeCommand` 由 `completedFuture(Any())` 空桩改为路由。注意方向：`workspace/applyEdit` 是服务端**发送**给客户端的请求，服务端不实现它。**（已完成，提交 `205c660ce`；其中「声明 `capabilities.workspace.applyEdit`」这一条经核对 lsp4j 1.0.0 **在协议上不成立**——`WorkspaceServerCapabilities` 只有 workspaceFolders/fileOperations/textDocumentContent，该能力只在**客户端**侧声明；服务端的正确做法是据客户端能力决定发不发请求，已按此实现：协商器新增 `workspaceApplyEdit`，命令处理器按它 gate，客户端没声明时直接报明确错误。`executeCommands` 默认声明 `applyCompletion`；`executeCommand` 由空桩改为路由。）
- [x] 15.7 新增 `LspCompletionSessionStore`，保留活对象 + `data` 会话键，提供 `applyCompletion` 与 `resolve` 命令路径；受「跨请求持有的平台对象」硬约束约束。验收：会话过期返回明确错误，不抛未捕获异常、不写入过期编辑。**（部分完成，提交 `0d4853b76`、`205c660ce`、`05f4003d6`）**`LspCompletionSessionStore`（容量 8、序号键、按 uri 失效、同位置重复请求计数）与 `applyCompletion` 路径已落地并验证：两条防线各有用例——`didChange` 丢弃会话（源头）、命令侧版本复验（兜底）；失败一律经 `ResponseErrorException` 给出**给用户看的**消息（普通异常只会让客户端看到 `Internal error.`），且**不发** `applyEdit`。**`resolve` 未做**：快照契约里没有 documentation 字段，需先决定「快照携带渲染后的 CDoc」还是「resolve 时按身份重进分析」。**（2026-10-08 复核：完成）**`resolve` 已做（`a654d27d1`：按候选身份重进分析恢复声明，复用门面 CDoc 渲染；本容器 `documentation` 恒空，由 `LspCompletionResolveTest` 的容器限制用例锁定）。验收三条各有用例：会话过期给明确错误、不抛未捕获异常、不写入过期编辑（`LspCompletionSessionStoreTest` / `LspCompletionCommandHandlerTest` / `LspCompletionCancellationTest`）。
- [x] 15.8 把 `AnalysisApiCangjieAnalysisFacade.completion`（185–233 行）旧实现迁至 `legacyCompletion` 并加独立开关；**默认路径只保留新实现**，旧实现不再是并行的第二条默认候选路径。（已完成，提交 `05f4003d6`）新路径＝造带 dummy 标记的副本 → EDT 写命令内修正 → 管线（带进度指示器）→ 存会话 → 转带命令与会话键的候选；`kind` 恒 BASIC、`invocationCount` 由会话存储按「同位置重复请求」计数（15.1③）。旧实现原样保留在 `legacyCompletion`，由 `descriptor.useLegacyCompletion`（默认 false）打开。**端到端验证**：`LspCompletionPipelineIntegrationTest` 2/0 断言「走的是哪条路径」看**只有新路径才有的字段**（命令 + 会话键 + sortText），并确认旧特征（类别一律 `Text`）不出现。**未做**：§7.11.6 的「新集合 ⊇ 旧集合」对比用例（15.9）。
- [x] 15.9 新旧候选集对比用例：固定一批源码位置，断言新集合 ⊇ 旧集合，且旧的全工作区遍历**不出现在默认路径中**。**（已完成）**`LspCompletionNewVsLegacyTest`：同一夹具、同一位置各跑一次（默认路径 / 打开 `useLegacyCompletion`），断言**新集合 ⊇ 旧集合**且默认路径的候选全部带管线标记。**对比位置取空前缀**——新路径按已键入前缀过滤、旧实现不过滤，拿「敲了半截」的位置比是问两个不同的问题（详见 `implementation-log.md` 15.9 节）。
- [ ] 15.10 LSP 侧测试套件：无候选返回合法空列表；`$/cancelRequest` 取消；`isIncomplete` 与实际是否穷尽候选一致；import 编辑正确性；`resolve` 填 documentation；**`MockApplication` 上的会话失效路径**；**服务端只做「计算编辑 + 发 `workspace/applyEdit`」，文本写入由客户端执行**，故断言服务端正确声明 `applyEdit` 能力、正确路由 `executeCommand`、发出的编辑内容正确——**不断言服务端写事务的 EDT 上下文**。**（大部分完成）**已覆盖：无候选空列表、`isIncomplete` 与穷尽性一致、import 编辑正确性、会话失效两条路径、`applyEdit` 能力声明与命令路由、**`$/cancelRequest`**（`LspCompletionCancellationTest` 4 例，确定性构造取消时机）。**仍未做**：`resolve` 填 documentation（快照契约无该字段，须先定「快照携带渲染后的 CDoc」还是「resolve 时按身份重进分析」）。**容器事实**：本容器里 `checkCanceled()` 不读推入的指示器（`ProgressIndicatorProvider` 未接线），故取消由管线前后的闸门承担，工作仍会跑完、结果被丢弃——见 `implementation-log.md` 15.10 节。**（2026-10-08 复核）**除「resolve 填 documentation」外全部覆盖；该项在本容器只能验到两半——身份恢复（`resolverRecoversTheSameDeclarationByCandidateIdentity`，真实文件夹具）与门面 CDoc 渲染（analysis-api-cfir 的 `cdoc` 夹具，属 1629/0 的一部分）；LSP 容器里 `documentation` 恒空（light PSI 无 docComment），端到端组合未验。**不勾选**：端到端验收需要真实工程/宿主侧夹具（与 15.11 同批）。
- [ ] 15.11 宿主 LSP 客户端（`modules/ide/lsp`）注册补全能力，使已声明的 `CompletionCapabilities` 与实现一致；实测该客户端是否执行 `CompletionItem.command`，不执行则本项降级并记录。
- [ ] 15.12 **DevEco 接线（第三个出货宿主）**：`deveco/settings.gradle.kts` 加 completion substitution；`deveco/product/build.gradle.kts` 加三个 completion 模块；`deveco/product/src/main/resources/META-INF/plugin.xml` **手工追加**一条 `completion.contributor language="CangJie"`（该产品 jar 形态下 `xi:include` 解析失败，文件头部有明确记录；参照同文件 533 / 753 行既有条目）。**该侧过期注册是静默失效**，须与 IDE 侧一并纳入唯一性验收。

## 16. 后续验证命令与完成记录

以下命令仅为**实施阶段待运行**清单，本轮没有执行。主仓库命令须从授权的主仓库工作副本运行；宿主命令须先完成 1.2 的上游路径配置。不为运行命令切回原工作树覆盖用户修改。

先运行现有的窄范围分析测试：

```bash
./gradlew :analysis:analysis-api-cfir:test --tests '*AnalysisApiCfirComponentExecutionTest*'
```

```bash
./gradlew :analysis:low-level-api-cfir:test --tests '*ContextCollectorTest*'
```

补全模块在**上游工作树**中已建立（三个模块目录与 `settings.gradle.kts` 注册均已存在，主检出待补），直接执行相应测试任务：

```bash
./gradlew :code-insight:completion:impl-shared:test
```

```bash
./gradlew :code-insight:completion:impl-cfir:test
```

LSP 侧（**必须单独运行**，一次只跑一个测试类，见设计 §10 验证纪律）：

```bash
./gradlew :lsp:test --tests '*LspCompletionTest*'
```

DevEco 侧：

```bash
./gradlew -p deveco :product:verifyPlugin
```

宿主测试和产品装配：

```bash
./gradlew :modules:ide:base:test
```

```bash
./gradlew :product:idea-plugin:test
```

```bash
./gradlew :product:idea-plugin:buildPlugin
```

```bash
./gradlew :product:idea-plugin:verifyPlugin
```

受影响 LSP 回归：

```bash
./gradlew :lsp:test --tests '*CangjieSemanticFeatureIntegrationTest*'
```

生成桥接/生成测试必须先在 7.2 定位真实 launcher/task，再记录对应命令；不能把 diagnostics generator 当成所有 API/test 的生成器。平台矩阵需根据项目实际配置逐版本运行，不以一次默认构建代替。

每项验收记录至少包含：任务编号、源码基线、测试类/fixture、完整命令、结果、失败修复记录及仍未覆盖边界。只有新增执行结果满足正反断言后才能勾选任务。**（已完成）**`LspCompletionNewVsLegacyTest`：**三个固定位置**（函数体起始 / let 初始化 / 二元表达式右侧）各在默认路径与 `useLegacyCompletion` 下跑一次，断言新集合覆盖旧集合的全部标签，且默认路径每一项都带管线标记。**对比位置取空前缀**：新路径按已键入前缀过滤、旧实现不过滤，拿「敲了半截」的位置比是问两个不同的问题（见 `implementation-log.md` 15.9 节）。
