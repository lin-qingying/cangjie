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
- [ ] 2.4 修正或扩展现有声明了 IGNORE_SELF_MODE 却未消费指令的测试准备逻辑；分别验证两种模式、原件不变与副本局部声明可见。
- [ ] 2.5 增加嵌套、异常/取消恢复、并行模式隔离和文档变化后的 session/pointer 失效测试。

## 3. Analysis：位置、导入与静态/实例作用域 A1/A2

依赖：第 2 组公开契约；正常文件适配可先于副本压力测试实施。落点：CaScopeProvider、CFIR scopes、LL ContextCollector；保留现有 LL 原件/副本测试。

- [x] 3.1 新增 CaScopeContext、CaScopeWithKind、CaScopeKind 和隐式接收者公共视图，明确层级次序、owner、生命周期和声明类别。
- [x] 3.2 实现按文件和位置查询，通过 LL ContextCollector 投影词法 scope 和接收者，不在插件遍历 PSI 重写名称解析。
- [x] 3.3 增加 importing scope context 与组合查询，复用 CfirFileLookupScopes 的同包、显式/星号/默认绑定及别名，保留来源优先级。
- [x] 3.4 增补补全所需的 static/instance 成员查询或筛选契约，区分类限定访问、实例访问、this/super 与 extend 使用点。
- [x] 3.5 修正文档中 file scope 与 importing scope 混淆的表述，并保持既有文件声明查询的兼容结果。
- [x] 3.6 增加公开层的局部/参数/类型参数、嵌套块、同名遮蔽、match 绑定、receiver 和原件/副本测试，断言 scope 次序及候选身份。（已完成，提交 `0bd8901fe`）新增 4 例：`lexicalScopesExposeParametersLocalsAndTypeParameters`、`nestedBlocksKeepInnermostShadowing`（断言**次序**：最先命中的 shadowed 必须来自最内层作用域）、`matchBindingStaysInsideItsBranch`（正例 side 可见 / 反例 radius 不泄漏）、`scopeContextInCopiedFileIsEquivalent`（原件与副本的 LOCAL_SCOPE 名字一致）。写用例时撞出并修复了一个新缺陷：**收集侧没有 match 分支作用域隔离**（编译器侧 `resolveBranch` 有 `withNewLocalScope`，`ContextCollector` 无任何 match 覆写），导致 `case Circle(radius)` 的 radius 泄漏进 `case Square(side)` 分支。receiver 一项由既有 `memberScopeExposesImplicitReceiver` 覆盖。
- [ ] 3.7 实现 `CodeFragmentScopeProvider.getExtraScopes`（现为 `emptyList()` 桩）；验收：表达式片段、类型片段各一组正反夹具，且经 cjc 验证为合法源码。**前置：缺陷 D-1 已修**。（**部分完成**，提交 `1cf1ca2d9`）桩已实现。**实现位置与设计预想不同**：本方法在片段 body 解析**之前**被调用（`resolveCodeFragmentContext` 先建上下文再解析片段），此时 `block` 仍是 `CfirLazyBlock` 桩，直接读 `statements` 会 `error("CfirLazyBlock should be resolved before accessing")`；因此片段自身声明实际发布在 `ContextCollector.visitCodeFragment`，`getExtraScopes` 对未解析片段返回空列表（正确结论而非缺失）。已解析片段仍按声明种类汇成成员作用域，`CfirPatternVariable` 显式跳过（`storeVariable` 对它 error）。**遗留**：「类型片段」一组用例未加——`CjTypeCodeFragment` 在本仓 PSI 侧无对应构造入口，实测前需先确认其可用性。宿主文件可见性已由 `e2d6929cb` 补齐（`CaCfirScopeContext.hostFileLevelEntries`），正例断言现已包含「宿主顶层声明必须可见」。
- [ ] 3.8 增 `CaScopeKind.CodeFragmentMemberScope` 与 `CjCodeFragment.fragmentMemberScope`，并跑生成器产出 `context(session)` 桥接；验收：公共 API 组件测试断言作用域种类、次序与「不泄漏宿主专有符号」。（**部分完成**，提交 `1cf1ca2d9`）`CaScopeKind.CODE_FRAGMENT_MEMBER_SCOPE` 已增（`indexInTower=4`，避开已被 `STATIC_MEMBER_SCOPE` 占用的 3，撞值会让排序等价、掩盖真实次序）；`CfirLocalScope.isCodeFragmentMemberScope` 标记与四个 store 方法的透传已加；`toScopeKind` 在 `isLocal` 之前先判它；只有**块**片段才标记（表达式/类型片段挂空层会与「不泄漏宿主专有符号」冲突）。验收用例 `CodeFragmentScopeExecutionTest` 断言种类与「片段自身声明可见 / 表达式片段无该作用域」。公开入口 `CjCodeFragment.fragmentMemberScope` 已由 `5184aacb0` 补上（复用 `CaCfirScopeContext` 投影，���例 `fragmentMemberScopeMatchesScopeContextProjection` 锁两个入口同源）。**未做**：①`context(session)` 桥接生成器未跑（属任务 7.5，`CaScopeProvider` 底部有「自动生成，请勿手工修改」标记）；②次序断言只覆盖种类，未覆盖 `indexInTower` 的相对次序。
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
- [ ] 5.5 增补导入/缩短计划中的目标身份、组织名/alias 与冲突复验信息，区分不可访问、不适用、未知和需要导入；实际写入留给第 10 组。
- [ ] 5.6 增加正反测试：receiver 不匹配、约束失败、权限、private extend、同名导入/局部 shadow、旧三态调用者与缩短回归。
- [x] 5.7 修复缺陷 D-2（**已完成**，提交 `f6b306951`）：`CaCfirCallableSymbolCacheKey` 与 `CaCfirExtendMemberCallableSymbolCacheKey` 增带 `CfirCallableSignature`，`matchesStableCallable` 与 extend 成员恢复都按签名筛选；`callableSnapshot` 的 `deduplicationKey` 改为直接调用 `callableIdentityKey`（同源），该键不再使用带 `!`/`vararg`/`= ...` 展示标记的尾文本。验收用例：AnalysisApiSymbolEquivalenceTest.overloadIdentity（顶层/类成员/extend 三组重载的两两不等价 + 各自恢复到自身）、CfirTopLevelCompletionSectionTest.topLevelOverloadsCarryDistinctSignatures（同名不同参数的候选同时出现且各带实例化签名）。**遗留**：「按声明 PSI 去重」在本仓未找到对应实现——`completionDecisionKey()` 定义在 analysis-api-cfir 但无任何消费方，插件侧也未见按 PSI 去重的代码；若该绕行指的是别处，请指明后补做。

## 6. Analysis：期望类型与名称发现 A7/A8

依赖：第 2–5 组。索引契约设计可与第 4 组并行，最终结果依赖可见性/签名。

- [x] 6.1 在现有表达式类型组件补 typed initializer、赋值 RHS、普通 Bool 条件、命名参数默认值；保留 return/成功调用既有入口。
- [x] 6.2 按仓颉合法语义补适用分支/块结果场景，并定义没有期望约束时的未知结果，禁止搬入 Kotlin 独有语法。
- [x] 6.3 增加不完整调用的可恢复约束查询，明确重载歧义行为，不以第一个候选的参数类型作为确定答案。
- [x] 6.4 新增明确 RHS 定位的 expected-type fixture；保留 propertyInitializer 的 callee 定位专项，不无脑修改 null golden。未完成调用另放补全恢复 fixture。
- [ ] 6.5 在现有 declaration/package 平台接口与 `CaStubIndexFacade` 之上补**按名称过滤的反向查询契约**（名称 → 声明集合 + analysisScope 限定 + 模块依赖可见性），返回符号身份而非索引键；**不泄漏具体索引名**。保留已有包枚举与 stub 索引。
- [ ] 6.6 落地**持久化符号索引**：①平台接口契约；②IDE 侧平台索引实现（**跨会话持久**，随索引增量更新）；③无平台侧复用 `CaStubSnapshotBuilder` 快照（**仅会话内，不得宣称持久**）；④`.cjo` 侧走已有只读名字视图。实现前缀分级过滤（≤3 字符只做起始匹配）。验收：按前缀查询命中且不漏报；记录查询次数与耗时并设预算；断言 IDE 与无平台实现在同一查询下返回同一符号集合。
- [ ] 6.7 增加多模块同名、未打开源码、SDK/.cjo、typealias、同名重载与 enum 构造器发现测试，记录查询量、取消及失效。
- [ ] 6.8 修复缺陷 D-3（dangling 副本的 PREFER_SELF 非局部解析返回 `null`）；**若判定 LL 修复不可行，必须产出记录在案的决策，不得继续绕行**（已试过的两条绕法会让 21 个生成用例失败）。**（2026-10-05 两轮取证，未修复）** 复现：夹具改成 Kotlin 形态后 7 个生成类 × 3 = **21 个失败**。**根因（与原记录不同）**：`LLCangJieSourceSymbolProvider.kt:177` 的 `require(context == null || context.isPhysical)` 抛 Failed requirement，中断整个文件的诊断收集；**不是** `getNotNullValueForNotNullContext` 的缓存不一致。**已定位调用方**：`LLNameConflictsTracker.getClassifierRedeclarations:55` → `getAllClassLikeSymbolsByClassIdOrSingle` → `getAllClassLikeSymbolsByClassId:166` → `getClassLikeSymbolByPsi:421` → 缓存 `getSymbolByPsi:118` → `getSymbolByClassId:74` 把非物理声明当 CONTEXT 传入。**已否证两条修法**：①删 `require`（失败 21→14，但 Kotlin `:184` 有**完全相同**的断言、其调用点也直接传声明，是刻意的不变量，删它等于掩盖违约）；②只补 `CjFile.contextModule`（真实缺失能力，已由 `1fe72f9cb` 补上，但不减少失败）。**已逐项核对且与 Kotlin 完全一致、不必重走的**：`require`、调用点、provider 是否 multi、`getAllClassLikeSymbolsByClassId`、`...OrSingle`、声明提供者构造、游离模块 `baseContentScope`、`LLNameConflictsTracker`。**下一步**：查 `CfirSession` 为游离模块构造 declaration provider 时 `searchScope` 指向上下文模块还是副本自身；随后修**调用方**（unwrap 到原件或按 ClassId 查），保留 `require`。详见 `implementation-log.md`「缺陷 D-3 取证」三节。
- [ ] 6.9 复核缺陷 D-4（字段成员缺失，已修 + 回归锁定）回归仍绿，并回写 `handover.md` §5 第 4 条「根因未最终定位」的过时表述。
- [ ] 6.10 为无平台索引实现补两项前置：`analysis-api-standalone` 新增 `:analysis:stubs` 模块依赖；`CaStubSnapshotBuilder` / `CaStubSnapshot` 由 `internal` 放开为公开 API。

## 7. Analysis：API 组装、生成与准入 A9

依赖：第 2–6 组各任务可分批接入，不等待全部结束才更新生成器。

- [ ] 7.1 联动 CaSession/CaBaseSession/CaCfirSession、组件工厂及接口继承，把每个新契约接到实际实现。
- [ ] 7.2 定位并更新现有桥接/测试生成入口和模型，执行真实生成器；禁止手改生成文件或臆造 Gradle 生成任务名。
- [ ] 7.3 在共享 Analysis component 基座/testData 增加新契约测试，CFIR 专用回归保留于相应实现套件；更新必要 API surface baseline 与模块文档。
- [ ] 7.4 在当前源码下运行受影响 Analysis/LL 窄集并记录结果，覆盖已有 callers。没有新执行记录不得宣布 G1/G2 完成。
- [ ] 7.5 定位并跑真实 context 桥接生成器，消除 `CaScopeProvider` 新增成员的手改桥接（缺陷 D-6）；验收：生成器产出桥接，且手改桥接被禁止。

## 8. 插件侧：模块、入口与调度 B1/B2/B3

依赖：第 1 组可先建骨架；语义接入依赖 G1，完整候选依赖 G2。落点：拟新增 code-insight/completion 的 api、impl-shared、impl-cfir。

- [ ] 8.1 建立三个模块及 src/resources/test/testResources，增加 settings/module-catalog；通过现有 convention 配置最小依赖。
- [ ] 8.2 定义跨模块服务和策略接口、内部 contribution/section/runner/sink，不给每个内部贡献者增加公共 EP。
- [ ] 8.3 实现原件/副本、offset、prefix、invocation count、BASIC/SMART 和替换区间的参数包装。
- [ ] 8.4 实现 dummy 与位置修正，覆盖泛型、未完成调用/类型、字符串插值及文档，协调 typed-handler 自动触发。
- [x] 8.5 实现仓颉 PSI position context 分类器及正反测试，不复制 Kotlin 专属上下文。
- [ ] 8.6 实现可取消的串行优先级 runner、按批 sink 和延迟阶段，保持执行优先级与展示权重分离。
- [ ] 8.7 注册上游原生 dummy/char-filter 等生产资源，测试真实入口确实调用新管线，错误态与安全排除位置不崩溃。**`completion.contributor` 已在上游描述符中注册给 `CangJieCfirCompletionContributor`，不得重复注册**（会使其每次补全执行两遍）。
- [ ] 8.8 **修复缺陷 D-5（`Ca` 前缀下线的机械重命名漏改，两处）**：**上游侧两处已修**（提交 `133851357`）——①`ngJieCfirCompletionContributor.kt` 已 `git mv` 回 `CangJieCfirCompletionContributor.kt`，与类名及 `cangjie-code-insight-completion.xml` 的 `implementationClass` 恢复一致；②`impl-shared/test` 下 5 个测试类名已去前缀（文件名此前已改、类名未同步）。`code-insight` 全树已无 `Ca` 前缀类型声明残留（Analysis API 的 `CaSession`/`CaType` 等跨层类型按设计保留）。**仍缺**：验收项中的 `intellij-ide` 与 `deveco` 两个生产描述符 contributor 唯一性核对——该面按分工由宿主侧会话完成，本工作区不覆盖，故本项暂不勾选。
- [ ] 8.9 按设计 §3 复核既有 42 个文件的命名与包名，把「采用 `org.cangnova.cangjie.ide.completion.*`」「code-insight 层不用 `Ca` 前缀」记为显式决策。

## 9. 插件侧：候选生成 B4

依赖：第 7–8 组相应契约。

- [x] 9.1 实现局部、参数、类型参数与隐式 receiver 候选，按位置/前缀/访问过滤并保留身份。（已完成，提交 `f96fe4a41`）局部与参数由 `CfirLocalScopeCompletionSection` 承担；类型参数此前补不出——根因是 `CfirTypeParameterScopeImpl.processClassifiersByName` 是空实现、`getCallableNames()` 也是空集，而 `getClassifierNames()` 却报告了类型参数名，两条通道互相矛盾且公开层无专用入口。已在 `CaScope` 增 `typeParameters` 通道（符号来源 `CfirTypeParameterScope.processTypeParametersByName`，名字来源 `getPossibleClassifierNames()`），类别独立为 `CompletionItemKind.TYPE_PARAMETER`（归入 LOCAL 会被渲染成带括号的调用形态）。回归 `typeParametersAreLexicalCandidates` 断言 `T` 可见且类别正确、同时 `param`/`local` 仍是 LOCAL。**隐式 receiver 候选由 9.2 的成员区段承担，本项不重复实现。**
- [x] 9.2 实现显式 receiver、继承、extend、super 及静态候选，使用实例化签名和适用性结果。
- [x] 9.3 实现类型、顶层 callable、包和 import 候选，区分已导入、alias、未导入和来源。
- [ ] 9.4 实现仓颉命名实参与合法实参组合候选，排除已使用参数，不提供 Kotlin 等号形式。
- [ ] 9.5 实现关键字、声明名称/类型建议、操作符等适用位置候选；每种类型都有 absence 测试。**（部分，提交 `698b5886f`）**关键字侧完整，`operator` 缺席断言已补。**类型建议侧存在阻塞缺陷**：类型位置补全抛 `StackOverflowError`，递归点在 `CaCfirCompositeScope.classifiers`（栈里反复出现同一行）。此前记录的「递归只在声明名位置」**不准确**——类型位置同样崩，而类型位置恰是用户最常补全处，该路径目前完全不可用。**已否证一条修法**：构造时展平嵌套组合 + 展开期 IdentityHashMap 防重入，均无效，说明环不在构造期而在查询路径；该改动已回退。下一步应打印 `delegates` 各元素的类名与 identityHashCode 找出互相持有的一对。用例只断言成立的缺席侧（`typeSuggestionsAppearOnlyAtTypePosition`），正面侧不写成通过的测试。详见 `implementation-log.md`「类型位置补全递归」节。
- [ ] 9.6 实现 enum 值/有参构造器/模式候选，按身份和签名去重，同名不同参数个数必须保留。
- [ ] 9.10 新增**索引召回区段**（`CfirCompletionService.sections` 现为固定的 keyword / localScope / topLevel / member 四段，无索引段）：按 `CaSymbolIndexQuery` 的前缀过滤 + analysisScope 限定召回未导入 / 跨模块候选，与作用域路径候选**共用同一去重器**，被遮蔽的索引候选按 `CaScopeKind` 决定存活。无此段则持久化索引没有消费方。

## 10. 插件侧：Lookup、排序与插入 B5/B6/B7

依赖：第 9 组可分候选家族逐步接入，不以只展示名字作为完成。

- [x] 10.1 建立稳定 lookup 数据与各类工厂，在 analyze 内完成语义渲染，UI 不捕获裸 symbol/type/session 或分析 Sequence。（**重开条件已满足**：缺陷 D-2 已由 `f6b306951` 修复，身份键改为与框架同源并带签名；`CfirTopLevelCompletionSectionTest.topLevelOverloadsCarryDistinctSignatures` 锁定「同名不同参数的候选同时出现且各带实例化签名」）
- [ ] 10.2 实现 pointer 恢复、文本版本/区间校验，覆盖 analyze 已结束后的展示与选择，失效候选不得写文档。
- [ ] 10.3 实现分析期权重及平台 sorter 组合，验证来源、import 成本、类型匹配、类别与稳定顺序，不为所有 weigher 注册 XML。
- [x] 10.4 实现 SMART 期望类型过滤/构造与未知降级，和 BASIC 的集合预期分开验证，不因重复调用放开访问权限。（**重开条件已满足**：同 10.1，缺陷 D-2 已由 `f6b306951` 修复；`CfirExpectedTypeCompletionFilterTest` 4/4 保持 BASIC 不查期望类型、SMART 未知时与 BASIC 同集合、TYPE 位置只留类型身份、触发次数不参与过滤）
- [ ] 10.5 实现 no-import/add-import/qualified-then-shorten 策略与使用点目标复验，复用现有 PSI 构造但不依赖未实现 shortening 的 bind 方法。
- [ ] 10.6 实现 Enter/Tab/字符选择的括号、已填实参、命名冒号、caret 和替换处理。
- [ ] 10.7 实现组织名/alias、分组 import、冲突处理和幂等导入；分析方案与写事务分离，完整支持单次撤销。
- [ ] 10.8 增加各候选家族的 after/caret/目标身份/import 测试，真实触发并选择，覆盖插入前已编辑文档的情况。

## 11. 插件侧：完整高级场景 B8

依赖：第 9–10 组；全部属于最终范围，不得只交付基础闭环后遗漏。

- [ ] 11.1 完成构造/实例化候选、适用泛型参数与实参组合的展示、过滤和插入。
- [ ] 11.2 给现有 override-implement 生成能力提取必要窄接口并由补全复用，验证成员生成结果，禁止跨模块访问 internal 实现。
- [ ] 11.3 完成 super/声明建议及仓颉 match、enum、extend 相关上下文，按官方语义维护差异表。
- [ ] 11.4 完成 CDoc 标签/参数/引用补全与源码定位，不原样复制 KDoc 规则。
- [ ] 11.5 完成宏 Tokens/普通表达式/可靠展开映射的上下文分流，并覆盖 .cjo 无源码及缺 SDK 降级。
- [ ] 11.6 实现未解析/未导入 receiver 的精确名称恢复和普通成员重补全，协调导入及最终插入，设置取消和预算；不扩展成任意深度表达式链搜索。

## 12. 插件侧：两种制品与宿主装配 B10

依赖：第 1、8 组可先配制品；最终加载/行为验收依赖第 9–11 组。

- [ ] 12.1 创建 completion fat jar 和 module 制品，显式列出 API/shared/impl-cfir 合并输入，复用现有 publishing helper。
- [ ] 12.2 更新主 settings/目录文档及宿主 Version Catalog/substitution，避免重复打包已有 common/psi/analysis 类。
- [ ] 12.3 更新 base compileOnly/test、product runtimeOnly/test 和 test-support 测试运行时依赖，禁止将测试 plugin.xml 混入产品。
- [ ] 12.4 从当前 base 模块描述符 include 单职责补全 XML，验证所有 class/resource 可加载且只注册一次，不使用不存在的 cangjie-all.xml。
- [ ] 12.5 加入真实产品 contributor/service 接线测试，仿 quick-fix 生产描述符测试模式，随后真正触发并插入。
- [ ] 12.6 分别验源码桥接与发布制品、最终插件 ZIP 的类/XML 唯一性；确认非开发机绝对路径也可解析依赖。
- [ ] 12.7 将平台 API 差异置于 platform 对应目录并验证承诺的平台矩阵，未测平台不得声明兼容。

## 13. 插件侧：并行、降级及性能 B3/B9

依赖：第 2、8、10 组串行正确性；并行未通过前维持关闭。

- [ ] 13.1 增加 dumb/restricted-analysis/no-SDK/cancelled/failure 分流，无索引时只提供约定安全候选，禁止全项目扫描兜底。**增加 LSP 侧分流：`$/cancelRequest`、会话过期错误、无 SDK 基线**（无头宿主默认无 SDK/无索引构建期，不得退化为全工作区声明遍历）。
- [ ] 13.2 实现阶段耗时、候选量、索引查询和取消统计，不记录完整源码；建立冷/热、小/大项目可复测基线及预算，**预算须写入具名测试配置文件后才可评估阶段门禁**。**增加 resolve 阶段耗时与会话过期错误计数（不并入弹窗路径）**。
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

- [ ] 15.1 **前置最小实验（必须先做）**：判定 ①LSP 生产容器能否提供 `Editor` / `CompletionProcess`；②**分析跑在哪份 `CjFile` 上**——B-1 自行构造 `analyzeCopy` + `CaDanglingFileModule` 补全副本（语义与 IDE 一致，但**受缺陷 D-3 阻塞**），或 B-2 直接分析 `LspAnalysisVirtualFile`（绕开副本，但**与 IDE 分析语义不等价**，须另补等价性对照用例）；③`invocationCount` 与 `completionKind` 的映射规则（IDE 侧靠 `INVOCATION_COUNT_KEY` 用户数据与 `completionType`，LSP 侧须自行定义）。扩展点 prime **不是未知项**（`CangJieRefactoringHeadlessRegistrar` 已用同一机制 prime 8 个扩展点）。已记录的「轻量夹具不注册 `EditorFactory`」只覆盖**测试夹具**，不能外推。**三项结论一并写入 `implementation-log.md`，未记录即视为本组未开始。**
- [ ] 15.2 （分支 A）`lsp/build.gradle.kts` 新增三个 completion 模块依赖；新增 `LspCompletionHeadlessRegistrar` prime 平台扩展点。验收：真实触发一次补全并断言走仓颉管线，**禁止手工 new contributor 塞进扩展点**。
- [ ] 15.3 （分支 A）上游 `PluginStructureProvider.allowedExtensionPointNames` 增补**枚举条目**（不得写「等」）；在 `docs/architecture-host-plugin.md` 登记本变更唯一一处「为消费点改动上游」的范围例外；补测试证明 standalone 与测试容器**未因此获得错误的补全行为**。
- [ ] 15.4 （分支 B）补齐四项前置，缺一即静默返回空候选：①新增接受 `(CjFile, offset)` 的**无参占位修正入口**（现有唯一入口 `CompletionDummyIdentifierProvider.provide` 依赖 `CompletionParameters`，其辅助均为 private）；②`documentStamp` 接 `LspTextDocument.version`（否则过期判定建立在常量 0 上）；③`CfirCompletionServiceProvider` 在 LSP 容器注册（上游描述符无 `<projectService>` 条目）；④转换对象改读 `CompletionSnapshot` 而非 `LookupElement`（分支 B 不经 `LookupElementFactory`）。**第 5、6 项前置（分析对象与 invocationCount/kind 映射）由 15.1 的实验一并判定。**
- [ ] 15.5 新增 `LspCompletionItemConverter` + `LspCompletionItemKindProvider` + `LspCompletionSortingUtil`。字段映射对位 kotlin-lsp；**空 `textEdit` + `CompletionItem.command` 走 LSP 标准路径**（客户端接受候选后自动执行命令）。验收：同一候选两端 `label`/`tailText`/`typeText`/`kind` 一致，`sortText` 顺序与上游排序一致。
- [ ] 15.6 打通命令与 `applyEdit`：`CangjieServerCapabilitiesFactory` **声明** `capabilities.workspace.applyEdit = true`（当前未声明，服务端无权发送该请求）；`CangjieLanguageServerDescriptor.executeCommands` 加入 `applyCompletion`（当前空列表）；`CangjieWorkspaceService.executeCommand` 由 `completedFuture(Any())` 空桩改为路由。注意方向：`workspace/applyEdit` 是服务端**发送**给客户端的请求，服务端不实现它。
- [ ] 15.7 新增 `LspCompletionSessionStore`，保留活对象 + `data` 会话键，提供 `applyCompletion` 与 `resolve` 命令路径；受「跨请求持有的平台对象」硬约束约束。验收：会话过期返回明确错误，不抛未捕获异常、不写入过期编辑。
- [ ] 15.8 把 `AnalysisApiCangjieAnalysisFacade.completion`（185–233 行）旧实现迁至 `legacyCompletion` 并加独立开关；**默认路径只保留新实现**，旧实现不再是并行的第二条默认候选路径。
- [ ] 15.9 新旧候选集对比用例：固定一批源码位置，断言新集合 ⊇ 旧集合，且旧的全工作区遍历**不出现在默认路径中**。
- [ ] 15.10 LSP 侧测试套件：无候选返回合法空列表；`$/cancelRequest` 取消；`isIncomplete` 与实际是否穷尽候选一致；import 编辑正确性；`resolve` 填 documentation；**`MockApplication` 上的会话失效路径**；**服务端只做「计算编辑 + 发 `workspace/applyEdit`」，文本写入由客户端执行**，故断言服务端正确声明 `applyEdit` 能力、正确路由 `executeCommand`、发出的编辑内容正确——**不断言服务端写事务的 EDT 上下文**。
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

每项验收记录至少包含：任务编号、源码基线、测试类/fixture、完整命令、结果、失败修复记录及仍未覆盖边界。只有新增执行结果满足正反断言后才能勾选任务。
