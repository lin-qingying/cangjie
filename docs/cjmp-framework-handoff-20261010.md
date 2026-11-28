# CJMP 实施续接（2026-10-10）

## 工作树与检查点

- 唯一实施工作树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。
- 分支：codex/cjmp-framework-implementation；接手时 HEAD 为 69cef759b。
- 完整目标仍为 cjmp-framework-review-and-fix-plan-20260928.md 的 P0–P10 / 70 项，不缩减为宏或定向测试。
- 本次接收并保存前一会话的 scoped parser、宏真实源码构造链、声明 root facts、LL 锁外输入准备和纯计划接线。
- 不纳入检查点：.idea/workspace.xml、原测试 HTML 删除、.workbuddy/tmp、四个仅行尾状态的 PSI raw 生成文件。

## 已核实的验证状态

当前磁盘 XML 为各自最后一次定向运行，不能合并成某次完整全仓验证：

| 模块 | 当前 XML | UTC 时间 |
| --- | --- | --- |
| PSI | ScopedMacroFragmentParsingTest 42 + WholeTreeStubRoundTripTest 2，44/44 | 2026-10-09 23:07:44–46 |
| resolution.common | evaluator 25/25 | 2026-10-10 04:32:37 |
| CFIR resolve | CJMP 52/52 | 2026-10-10 04:40:36–37 |
| low-level-api-cfir | CJMP 45/45 | 2026-10-10 04:42:34–52 |
| CFIR analysis | 默认值读穿两种入口共 4/4 | 2026-10-10 03:56:38–43 |
| frontend | IfAvailable 真实源码链 3/3 | 2026-10-10 00:33:14 |

以上当前记录均无 failure/error/skipped。宏切片 queue 25076203 的实际 journal 为 DONE / exit 0，
参数为两套 CfirAnalysisMacro*TestGenerated 和 CfirMacroAnnotationSourceModuleTest；前一会话记录 657/657，
0 failure/error/skipped。当前 analysis XML 已被后续四项覆盖，不能再当作那次完整宏 XML。
宏切片通过的记录覆盖了先前 44 项 IfAvailable 失败；P1 随后的修改仍需按本批范围重新验证。

旧 err_not_lambda.cj 同时断言未到达的语义诊断。现 parser 的 nonLiteralIfAvailableBranches 测试
保留两条分支的 literal-lambda 语法诊断；新的 err_unknown_arg_name.cj 使用合法 lambda 单独验证语义名字。
这是诊断阶段迁移，不能删去这两个层级的覆盖。

前一会话还记录过一次未解释的 uncalledImplementationCallerFirst 间歇失败；后续单测/整组通过
不代表根因已修复。接手时未发现正在运行的 Java/Gradle 进程；不要轮询旧 handles 或重启已结束任务。

## P1 不可变事实源：本工作树状态（2026-10-11）

完整契约与九步见 cjmp-immutable-publication-migration-20261002.md §§2–6；原始 Kotlin 对应在
.workbuddy/tmp/cjmp_probe113/review-20260928/P1-FRAMEWORK-MAPPING.md。接续提交 `37d523564`
已完成本轮大批接线并删除 `CfirCjmpMappingStorage`；不得再把其下方 10 月 10 日记录中的旧状态当作当前状态。

当前已经接线：root facts 单根发布；CLI/LL 共用有序 family snapshot 和纯 merge/match 求值；诊断、
默认参数、checker 与实际化 scope 读取统一结果；LL callable 与 non-callable 输入在目标锁外准备，
失败/取消不发布完成状态；raw 与 actualized scope 使用独立缓存身份。细节与定向证据见下方按日期记录。

本轮新鲜定向验证：CJMP diagnostics2 69/69、LL CJMP 48/48；`testCjmpDuplicateExtensions` 的顺序回归
已修复。它们是过滤集结果，不等于完整模块或最终 P1 验收。P1 最终 gate 仍需完整 CFIR、LL 与 CLI
两段式验证，并按迁移协议核对取消/失败重试与声明根边界。

实例字段初始化输入、common CJO FullId 依赖恢复以及 CFIR 初始化消费者已在 2026-10-11 检查点接线，
具体改动与定向验证见文末。该项不等于 P1 完成；仍需执行完整模块 gate 并继续处理其他 P1 生命周期边界。
完整 P0–P10 的 70 项仍开放，本轮不重算总体比例。

## 2026-10-10 输入冻结检查点

- `CjmpMatchFacts.create` 直接冻结计划和 seed，不先执行 evaluator；缺少实例 seed 等执行不变量由 evaluator 验证。
- root 属性移入 `CjmpAttributes.kt`，runner 从当前声明的独立计划创建事实；本检查点仍保留旧 storage，不能作为 P1 完成验收。
- 定向队列日志：`.workbuddy/tmp/cjmp-p1-fact-baseline-20261010.log`，实际 Java/queue exit 0。
- 当前 XML：evaluator 26/26、resolve 的 cjmp 包 43/43、LL CJMP 46/46，均 0 failure/error/skipped；这是本批过滤范围，不是完整模块验证。
- 后续必须继续九步集成；原 generic 节点存在性、原 doNotExport 与独立初始化依赖生命周期不能从类型参数数量、可见性或空列表推断。

## 2026-10-10 不可变组快照接线续接

- 当前工作树的未提交切换已删除生产 `CfirCjmpMappingStorage` 源码及 session 注册；root `CfirCjmpDeclarationMatchFacts` 保存冻结候选输入，`CfirCjmpBatchFamilyAggregate` 负责组级合并和有序求值。
- CFIR matching checker、泛型实例化 checker、反序列化 common 诊断入口和 runtime defaults 已改读家族快照；LL session 用共享聚合器并保留 LL 取消感知的包级完成单元。它不再恢复旧配对表或 common 侧 shadow Boolean。
- 定向 Kotlin 主源码编译通过：`:cfir:checkers:compileKotlin`、`:compiler:frontend:compileKotlin`、`:analysis:low-level-api-cfir:compileKotlin`。本轮未运行测试。
- 多个现存测试/fixture 仍引用已删除的 storage 和旧 runner API；测试源码编译尚未核实，因此不能把 storage 删除或 P1 记为完成。下一步需迁移这些现存断言到 root facts/家族快照契约，再继续核对失败重试、默认值和 actualized scope 的消费时序。
- 完整 P0–P10 仍开放；本续接未重算整体完成比例。

## 2026-10-10 不可变组快照测试迁移检查点

- 已按 `matchingGroupFor` 的组边界迁移 LL 断言：class、extend、enum 各自读取其组快照；不再把旧 storage 的包级读面当作单组快照。扩展归并断言保留两个 common owner，并分别核对其类型参数映射。first-fit 测试保留早期参数失配事件。
- caller-first 断言区分默认参数消费与普通调用：前者经过 owning callable 的 CJMP 阶段，后者只验证真实 specific 符号与不可变组结果，不要求无关目标阶段发布。已删除的 `CfirCjmpMappingStorage`、session accessor 与旧写 API 在一方 Kotlin 源码和测试中均无引用。
- 当前目标测试通过：`resolution.common` 71/71、`cfir:resolve` CJMP 52/52、`analysis:low-level-api-cfir` CJMP 47/47、`compiler:frontend` CJMP 两段编译 26/26；failure/error/skipped 均为 0。测试 XML 是各模块各自最后一次定向运行；未运行完整模块或全仓库测试。
- P1 仍未闭合：LL 家族完成缓存已有失败、取消、重入和并发等待重试测试，但 CJMP root 属性连同阶段 updater 的失败事务还缺端到端注入验证；默认值仍经 `cjmpHasDefaultValue` 适配器进入 family defaults，需继续审查 owning callable 与所有调用消费者的时序；实际化 scope 已有成员图和 view key 接线，但旧兼容构造器、RAW 默认视图及 typealias/cache 消费路径仍需逐条审计并补真实 scope 用例。
- 完整 P0–P10 仍开放，本检查点不更新总体比例。

## 2026-10-10 root 事务与成员 scope 回归

- 将 LL CJMP 的事实/签名状态 keeper 提取为 `withCjmpDeclarationState`，生产 resolver 与失败重试 fixture 共用同一事务入口。
- `rootFactsFailureRetry.cj` 在真实 specific 函数上使用 LL jumping lock 执行 root facts/签名子节点阶段事务，注入失败后断言 root facts 和每个参数阶段均恢复；随后通过真实 lazy resolver 重试并确认 facts 与阶段提交。故障注入位于事务 action，覆盖实际锁释放和恢复，不伪称 transformer 内部抛错路径已单独注入。
- `specificMemberMatchesCommon.cj` 增加有函数体的 common-only 成员；测试以同一 `ScopeSession` 查询 STATUS raw 视图和 CJMP 实际化视图，确认 common-only 成员只出现在实际化视图，且其符号回到 family 冻结成员图中的原声明。
- `:analysis:low-level-api-cfir:test --tests "*CfirCjmpMatchingTestGenerated"` 10/10 通过。此前文档将这两项列为缺口，现已由上述回归覆盖；不据此宣称整个 P1 或 P0–P10 完成。
- 默认值读取审计确认唯一适配器 `cjmpHasDefaultValue` 被 `CfirMapArguments` 的缺参判定、默认计数、尾随命名参数选择，以及 `CfirCallResolver` 的候选 arity 预筛选共用；其先查原始参数默认值，再在 specific 候选缺省时推进 owning callable 的 CJMP 阶段并读 family `defaults`。LL caller-first 的显式/推断返回类型用例覆盖参数映射消费，evaluator 单测覆盖有序默认值转移。仍需对参数前缀转移在完整调用映射中的失败候选路径作组合验证。
- 继续补齐 typealias constructor scope：provider 按 CJMP member view 分流，并把 view 放入 `ScopeSession` cache key；替换 scope 从同一 view 构造底层 class member scope。specific class fixture 通过 typealias 断言 common body constructor 与 specific constructor 同时可见，而 STATUS raw class scope 只包含物理 specific constructor。
- 复验：`:cfir:providers:compileKotlin` 和 `:analysis:low-level-api-cfir:test --tests "*CfirCjmpMatchingTestGenerated"` 均通过，生成匹配测试 10/10。该用例覆盖类 raw/actualized scope、session replacement、typealias constructor view 和真实 jumping-lock 下 root publication 事务失败重试；未运行完整 LL 模块或全仓库。
- 新增 `transferredDefaultPrefixCallerFirst.cj`：首个 common 候选在后续命名参数处失败，但此前转移的默认参数前缀由后续候选保留；真实调用省略该前缀，并断言最终 specific 符号及 family defaults。首候选保留 common 函数体，符合 common 默认实现的豁免规则，避免把候选失败误当作无实现诊断。
- `:analysis:low-level-api-cfir:test --tests "*CfirCjmpCallerFirstDiagnosticsTest"` 7/7 通过，含新增 caller-first 组合用例；不代表完整 LL 模块或完整 P1 验收。
- session replacement 审计发现 `CfirClassUseSiteMemberScope.withReplacedSessionOrNull` 重建 scope 时遗漏 `memberScopeView`，会把 CJMP_ACTUALIZED 降级为 RAW。已传递原视图，并将相同 ScopeSession 下的重建 scope 加入成员图用例；`:analysis:low-level-api-cfir:test --tests "*CfirCjmpMatchingTestGenerated"` 10/10 通过。

## 2026-10-10 actualized candidate 顺序与 extend owner 过滤

- `CfirActualizingScope.actualizeCjmpCandidates` 先前按 common/其他候选分区后重排结果；现从原 tower 候选序列正向过滤已匹配 common，保留 specific 与未匹配 common 的相对顺序及 provenance。
- `CfirExtendMemberScope.buildIndex` 以完整 receiver 候选集中的 specific extend owner 为起点，按家族快照取得其所有有序 common counterparts，再过滤 paired common owners；不再逐个 common owner 调用反向 shadow Boolean。成员仍从相同 family snapshot 的 actualized owner member graph 读取。
- 编译与定向测试通过：`:cfir:providers:compileKotlin`，`:cfir:resolve:test --tests "*CfirCjmp*"` 52/52，`:analysis:low-level-api-cfir:test --tests "*CfirCjmp*"` 48/48；失败、错误和跳过均为 0。
- 本次验证覆盖 provider/resolve/LL CJMP 过滤范围；继承和泛型实例化 checker 的消费边界后续单独迁移。

## 2026-10-10 override API 求值顺序

- `AbstractOverriddenDeclarationProviderTest` 改为先消费 `allOverriddenSymbols`，再消费 `directlyOverriddenSymbols`，与 Kotlin 对位测试顺序一致，避免 direct 查询预热后改变完整覆盖链观察结果。
- `:analysis:analysis-api-cfir:test --tests "*OverriddenDeclarationProviderTest*"` 通过：7 个生成测试类共 35/35，失败、错误和跳过均为 0。

## 2026-10-10 家族候选事实与阶段契约回归

- `CfirCjmpPreparedGroup` 保留 resolver 给出的候选次序；成员合并后的遍历布局不能重排 first-fit 语义。原排序会把后合并的 common extend 提到首位，跳过首候选参数不匹配诊断。`cjmpFirstFitCandidateDiagnostic` 现验证首候选诊断仍保留且次候选可绑定。
- 合并 seed 过滤只排除既无绑定 seed、也无资格或签名失败事实的候选。保留结构不兼容候选，使 `SPECIFIC_HAS_DIFFERENT_KIND` 等拒绝事实能到达诊断层；此前类/结构种类不匹配被过滤成普通 `NOT_MATCHED`。
- `CfirCjmpMatchRunner` 不再在 `CJMP_MATCHING` 中把 common function 强行推进到 `IMPLICIT_TYPES`；候选签名必须遵守其前置解析阶段契约。
- `CfirGenericInstantiationChecker` 对 owner-specific extend 签名查询使用 family actualized member scope，避免 paired common/specific owner 的成员重复进入重载比较；多 common extend 与泛型外层成员用例通过。common-side checker 直接读取当前/refinement sessions，不吞掉 provider 失败。
- `:cfir:analysis-tests:test --tests "*CfirAnalysisDiagnostics2PsiTestGenerated.testCjmp*"` 通过，69/69；包括 first-fit 诊断与 class/interface/struct kind mismatch fixtures。`:cfir:resolve:test --tests "*CfirCjmp*"` 52/52，`:analysis:low-level-api-cfir:test --tests "*CfirCjmp*"` 48/48，`:analysis:analysis-api-cfir:test --tests "*OverriddenDeclarationProviderTest*"` 35/35。

## 2026-10-10 泛型与继承 checker 的 family 导出面

- `CfirGenericInstantiationChecker` 先枚举完整 receiver extend 集合，再从 specific owners 的 family snapshots 取得 paired common extend 与 `doNotExport` 成员；随后统一过滤直接成员和 inherited-default 输入。common nominal counterpart 的实例化 scope 也先收集完整 callable、override、constructor 候选及对应的 family 导出事实，再进行签名处理。
- `CfirInheritanceDeepChecker` 对直接 receiver 与父类链约束失败的 extend 均先收集完整 owner 集合，再批量读取 specific family snapshots 的导出事实；删除逐个 common member 反向查询。provider 层 `isCjmpShadowedCommonDeclaration` 旧入口已删除，仓库无残留调用。
- `:cfir:analysis-tests:test` 定向覆盖 CJMP diagnostics、LLT/PSI extend 与 class instantiation 共 1303 项，0 failures、0 errors、0 skipped；`:cfir:checkers:compileKotlin` 通过。
- P1 和 P0–P10 仍开放；下一步继续审查 family facts 的生命周期、默认参数消费和剩余 actualized scope 路径，并用完整相关回归验证取消/失败发布边界。

## 2026-10-10 LL common 隐式签名锁外解析

- `LLCfirCjmpMatchingTargetResolver.doResolveWithoutLock` 现在在 specific 目标短读锁内冻结真实 common 候选引用；退出读锁后，按 IMPLICIT_TYPES 正常推进候选 callable、variable 和 pattern-variable 签名，再把同一候选快照交给匹配事实准备。specific 声明仍由标准 CJMP 写锁处理，specific body 保持惰性。
- 这修复了删掉 matcher 内非法阶段请求后的两个 LL 回归：串行 `implicitCommonChain` 与并行 `parallelImplicitCommonChain`。新增路径通过 `withReadLock` 取候选，避免在 specific 锁下跨模块解析 common body，也没有在计划准备时重复发现不同的候选集。
- 复验结果：`:analysis:low-level-api-cfir:test --tests "*CfirCjmpImplicitCommonSignatureTest*"` 通过；完整 CJMP 定向复验 `:cfir:resolve:test --tests "*CfirCjmp*" :analysis:low-level-api-cfir:test --tests "*CfirCjmp*"` 通过，分别 52/52 与 48/48，失败、错误、跳过均为 0。随后对 common 签名种类过滤的小幅收紧再次运行 LL CJMP 全集，48/48 通过。
- 该项关闭 P1 映射中的 common 隐式签名锁外准备缺口，不代表 P1 完成。root 与阶段 updater 失败事务、family 聚合默认值及全部 scope/checker 读面的统一生命周期仍须按计划审计；P0–P10 整体也仍开放。

## 2026-10-11 extend 候选定向索引

- LL 的 `getExtendsForTarget(key)` 在首次刷新时会推进该模块缓存的全部顶层 extend 到 TYPES。CJMP 改用 provider 的同包目标查询；LL provider 在候选输入准备阶段解析所需的头部类型，只将同包、同目标的 extend 推进到 TYPES，并保留 raw/import/typealias 的真实解析语义。结构性 package/file 查询不再刷新整个 TYPES 索引。
- common extend 的类型准备与 key 比较位于 specific 目标锁外。`specificMemberMatchesCommon.cj` 增加 Other 目标的无关 common extend；触发 Holder 的匹配后断言该无关 extend 仍早于 TYPES 阶段。
- 验证：`:cfir:resolve:test --tests "*CfirCjmp*"` 52/52；`:analysis:low-level-api-cfir:test --tests "*CfirCjmp*"` 48/48，失败、错误、跳过均为 0。未运行完整模块或 P0–P10 全量验证。
- 该改动只关闭 CJMP extend 候选查找造成的无关阶段推进；P1 和完整 P0–P10 仍开放。

## 2026-10-11 extend 索引失败重试

- Gatekeeper 复审发现定向查询与全量查询的完成标记先于 `CfirExtendIndexStore.rebuild`，若重建或阶段不变量检查抛错，同一查询会被误判为已完成并跳过重试。现将 `resolvedTargetQueries` 和 `allExtendsResolved` 的发布移到重建及检查成功之后；失败保持未完成状态。
- 再验证：`testSpecificMemberMatchesCommon` 通过；CFIR resolve CJMP 52/52、LL CJMP 48/48，失败、错误、跳过均为 0。`CfirExtendIndexStoreTest` 覆盖无语义模型时结构 owner 索引可用，测试已通过。
- 复审未发现其他阻断项。该批仅闭合定向索引的阶段隔离与失败重试；P1 和完整 P0–P10 仍开放。

## 2026-10-11 specific extend 重复诊断顺序

- diagnostics2 的 `cjmpDuplicateExtensions.cj` 暴露按第二个 specific extend 查询时重复诊断丢失。根因是 `discoverGroup` 将请求目标前置，导致同一 extend 家族因入口不同而得到不同的有序缓存键；纯 evaluator 因此把每个被查询的目标都视为首个 specific。
- 现保留 extend provider 的规范候选顺序，仅在候选中缺少目标时追加目标，确保从任一家族成员查询都使用相同组顺序与快照。
- 验证：`CfirAnalysisDiagnostics2PsiTestGenerated.testCjmpDuplicateExtensions` 通过；CJMP diagnostics2 过滤集 69/69、LL CJMP 过滤集 48/48，失败、错误、跳过均为 0。
- 该回归修正不改变剩余计划范围；P1 和 P0–P10 仍开放。

## 2026-10-11 P1 完整模块 gate 与 scope 阶段契约

- `:cfir:analysis-tests:test :analysis:low-level-api-cfir:test --continue` 执行 32m49s 后未通过：CFIR analysis 8967 项中 46 项失败、396 项跳过，失败均在宏测试；LL 176 项中 `SourceLazyDeclarationResolveScopeBasedTestGenerated.testFakeOverrideScope` 1 项失败。定向 CJMP diagnostics 与 LL 过滤集此前均通过。
- LL 失败由 `CfirCangJieScopeProvider.unsubstitutedScope` 新增的 eager phase resolve 引起；scope 构造提前把成员推进 STATUS，破坏 lazy-scope 阶段契约。已删除该阶段推进，scope 仅选择 RAW/actualized 成员视图，阶段仍由调用方 resolver 管理。
- 修复后 `testFakeOverrideScope` 单测通过，`:analysis:low-level-api-cfir:test --tests "*CfirCjmp*"` 也通过。完整 LL 与 CFIR 模块尚未重跑；宏失败未归因到 CJMP，本次不改其期望或生产实现。
- 下一项继续处理实例字段初始化依赖采集、common CJO FullId 恢复及初始化消费者接线；P1 和 P0–P10 仍开放。

## 2026-10-11 实例字段初始化依赖接线

- 按官方本地 `InitializationChecker::RecordInstanceVariableUsage` 收集解析后的真实非 static 实例字段符号；声明身份、跨包事实和空集合状态保留，不从名称或 receiver 推断。common CJO producer 依据当前包实际声明索引投影 FullId，并按官方 ASTWriter 规则递归越过未收录声明。
- common CJO deserializer 先物化源/目标 owner，再在各自共享 package context 中恢复 canonical field symbol；共享 `Decl.dependencies` 中的函数、static 与 package-global 边不进入 CFIR 实例顺序事实。
- 初始化 checker 使用家族冻结后的实际化成员图，并重放每次结构合并时的精确依赖替换步骤，再运行初始化顺序分析。
- 验证通过：`:resolution.common:test --tests "*CjmpContainerMergeEvaluatorTest"`；`:cfir:analysis-tests:test --tests "*CfirInitializationDependencyCollectionTest"`（覆盖同包及跨包真实声明身份）；`:cfir:cfir-serialization:test --tests "*CfirInitializationDependencySerializationTest"`（覆盖 common 当前包与导入包 FullId、canonical owner、未收录声明投影及 specific 边界）。这些是定向测试，不是完整模块 gate。
- 本次关闭上述初始化依赖接线缺口；完整 P1 验收、完整模块验证和 P0–P10 / 70 项仍开放。

## 2026-10-12 完整 LL gate 与实例初始化回归

- `:analysis:low-level-api-cfir:test` 完整模块通过：176/176，失败、错误、跳过均为 0。
- 提交 `c66b601fa` 后的首次完整 `:cfir:analysis-tests:test` 有 8969 项、276 项失败、396 项跳过。失败里 100 项集中于 typealias/extend typealias：它不是 CJMP nominal merge container，实例初始化 checker 却按 class-like 查询其 family member graph；另有 8 项声明模式测试揭示合成 source session 未注册 checker 现在必需的 CJMP family aggregate 与依赖 symbol provider。
- 修复为仅对实际声明实例存储的 nominal owner 执行 family 实例成员顺序分析；typealias 保留原有 static 检查。analysis-tests 基础 session 现在注册正式 `CfirCjmpBatchFamilyAggregate`，并用空依赖 provider 表示无 refinement 依赖。
- 过滤验证：typealias、extend typealias、诊断 typealias 与声明模式测试共 116/116 通过；`:cfir:analysis-tests:test --tests "*CfirInitializationDependencyCollectionTest*"` 通过。完整 CFIR 模块需在本修复后重跑，首次失败中其余用例尚未归因，不能据此宣称 P1 gate 通过。
- 首次修复后重跑完整 CFIR：8969 项中 48 项失败、396 项跳过；46 项为既有宏测试失败，另 2 项 `InterfaceMemberVariableCheck02` 因接口非法字段被新的实例存储分析标成 `CLASS_UNINITIALIZED_FIELD`。实例存储分析已对接口与类型别名统一跳过；PSI 与非 PSI 两个对应测试均通过。
- 后续工具链复验更正：系统 `CANGJIE_HOME` 指向不存在的 `cangjie-1.0.5` 目录，`cjc.exe` 不存在。该环境下完整 CFIR 的 46 项失败中有 40 项涉及 `MACRO_DEPENDENCY_COMPILE_FAILED`，不能作为 CJMP 回归结论。改用本机 1.1.3 SDK 重跑完整 CFIR 后为 8969 项、6 项失败、396 项跳过；6 项均是三个宏 fuzz fixture 的 PSI/非 PSI 对，差异只在 `DEPRECATED_WARNING` 数量（32/32/31）。本机 1.0.0 SDK 复验也出现同一组差异。没有 1.0.5 SDK 可作基线，因此不能把这 6 项定性为既有失败；完整 CFIR gate 仍未通过。完整 LL 为 176/176 通过。
- 随后补验：`:compiler:frontend:test --tests "*CjmpTwoPhaseCompilationTest"` 26/26、`:cfir:resolve:test --tests "*CfirCjmp*"` 52/52、`:cfir:cfir-serialization:test --tests "*CfirInitializationDependencySerializationTest"` 6/6，均无失败、错误或跳过。序列化与 CLI 是定向集，不替代完整模块 gate。
- 初始化依赖接线及 P1 其余生命周期边界仍待完整验收；P0–P10 / 70 项仍开放。

## 2026-10-12 默认参数消费接线

- 删除 `cjmpHasDefaultValue` 单参数适配器，新增候选级 `CandidateDefaultParameterStates`：从 owning callable 读取 ordered family defaults，并在同一次候选参数映射中复用；声明自身默认值仍直接来自参数事实。`CfirMapArguments` 的缺参判定、默认计数、尾随命名参数选择以及 `CfirCallResolver` 的 arity 预筛选统一消费该视图。
- `:cfir:resolve:compileKotlin` 通过；CJMP 默认状态、函数计划、匹配及 resolver outcome 定向测试合计 27/27；`:analysis:low-level-api-cfir:test --tests "*CfirCjmpCallerFirstDiagnosticsTest"` 为 7/7；`:compiler:frontend:test --tests "*CjmpTwoPhaseCompilationTest"` 为 26/26，失败、错误、跳过均为 0。
- 全量 `:cfir:resolve:test` 首次为 206 项、6 项失败。其中默认值计数单测把默认参数构造成 `isNamed=false`，与仓颉 `a!: Int64 = 0` 的命名可选参数形状不符；修正测试 fixture 后 `CfirMapArgumentsTest` 全类通过，完整 resolve 重跑为 206 项、5 项失败：3 项 `CfirMapTypeArgumentsTest`、1 项 `CfirBuiltinSymbolProviderTest`、1 项 `CfirExtendAccessibilityCheckerTest`。这些失败未归因到本次 CJMP 改动，resolve 全模块 gate 仍未通过。变更后的完整 `:analysis:low-level-api-cfir:test` 为 176/176，通过；
- 本次只完成默认值调用消费接线；完整 CFIR gate 仍有上述 6 项工具链相关宏 fixture 差异，P1 与 P0–P10 / 70 项仍未完成。

## 2026-10-12 默认值检查器与 CJO 读面

- providers 新增 owning-callable 默认参数状态入口。它统一暴露声明自身事实，并在 specific 参数缺少初始值时按需推进 owning callable 的 `CJMP_MATCHING` 阶段，从不可变 family defaults 读取同位置状态；支持普通函数与 enum constructor 参数，不吞掉阶段、session 或 family 不变量错误。
- 参数映射复用该入口；CJMP 计划只读取声明自身默认值事实。隐式 `super()` 与递归构造器检查共享 `requiredParameterCount()`，不再直接以 `defaultValue == null` 判定必填参数。CJO 写侧通过同一状态写 `HAS_INITIAL`，并只为当前 source package 实际收录的 source helper 写局部 desugar 引用。
- `cjmpDefaultReadThrough.cj` 增加 common `Base.init(x!: Int64 = 1)`、specific 无初始值构造器及子类隐式 `super()`；修复前精确失败为 `NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS`，接线后 `CfirAnalysisDiagnostics2PsiTestGenerated.testCjmpDefaultReadThrough` 通过（1/1）。
- 验证：providers/resolve/checkers/CJO serialization Kotlin 编译通过；`:cfir:resolve:test` 的 `CfirCjmpFunctionPlanTest` 与 `CfirCjmpDefaultStateTest` 14/14；`:cfir:cfir-serialization:test --tests "*CjoDefaultValueMetadataTest*"` 5/5。未运行完整模块 gate。该项闭合 C5 所列的默认值调用、构造器检查和 CJO 写侧消费缺口；P1 与 P0–P10 / 70 项仍未完成。

## 2026-10-12 泛型 owner 与声明级函数覆盖图（D1/D2/D5 部分）

- 泛型实例化 owner 的泛型视图和具体视图 provenance 现在都以 owner extend 自己的 `extendedTypeRef` 为根，因此 owner `where` 约束和它声明的接口边留在原有类型参数空间。
- 删除 owner 函数的原始声明补签名、约束推导替换和部分泛型实例早退；owner 成员统一从 provider use-site member scope 取得，CJMP 配对过滤继续由共享成员图负责。
- class use-site scope 默认以 class 声明 self type 保存函数 provenance。覆盖、接口 default 和抽象函数过滤都按双方 direct-input 声明及各自的 provenance owner type 建图，再把 owner 类型替换留给后续实例化；CJMP common→specific owner 映射使用声明级参数。
- `privateExtendOwnerGroups.cj` 新增有界 owner 的 public 成员回归。改动前该具体实例缺失 `GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS`，改动后 PSI 与非 PSI diagnostics2 均通过；同 fixture 原有 private owner 断言保留。
- 验证通过：providers/checkers Kotlin 编译；diagnostics2 owner/继承回归与 LLT `interface_default_implemented_func_generic_invalid_2`、`privateFunctionsCauseGenericInstantiationAmbiguity` 的 PSI/非 PSI 共 8 项；`:cfir:resolve:test --tests "*CfirCjmp*"` 52/52；CJMP diagnostics2 PSI E2E 64/64；scope 改动后的完整 `:analysis:low-level-api-cfir:test` 176/176（失败、错误、跳过均为 0）。
- D1/D2 的 owner 组缺口已按上述回归接线；P4 仍开放。D5/D9 的剩余项包括祖先 extend private 成员的统一继承边界，以及移除 checker-only scope 开关并经 provider 获取 scope；未据此宣称 P4 或 P0–P10 完成。

## 2026-10-12 泛型实例 scope 创建移入 provider

- `CfirGenericInstantiationChecker` 不再直接创建 `CfirClassUseSiteMemberScope` 和 `CfirClassSubstitutionScope`；改为由 `CfirCangJieScopeProvider` 提供声明成员图与实例化视图，checker 只消费返回结果。
- provider/checker Kotlin 编译通过；`privateExtendOwnerGroups` diagnostics2 PSI 与非 PSI 回归 2/2 通过。
- 这次只迁移 scope 创建责任；`includedRootExtends`、`memberOwnerExtend` 等 checker 专用 scope 开关及祖先 extend private 成员继承边界仍未清理，D9/P4 与 P0–P10 均未完成。

## 2026-10-12 泛型 owner scope 视角与缓存边界

- 新增 `CfirMemberScopeOwner`，用名义实例化、声明级实例化、extend owner、继承节点和普通 use-site 等明确视角统一原有 checker 专用开关；class scope、extend scope 和泛型实例化 checker 共用此 owner 模型。
- 父 scope 传播 owner 身份并按继承节点规则处理 direct extend 来源和 private 成员，收口祖先 extend private 成员边界；泛型实例化 scope 由 provider 通过 `ScopeSession` 缓存，key 包含 session、符号、实例化类型、继承来源类型、owner 视角和成员图视图。
- 验证通过：providers/checkers Kotlin 编译；diagnostics2 与 LLT 的 PSI/非 PSI owner、继承和默认成员回归共 8 项通过；`git diff --cached --check` 通过。
- 提交：`2c39610fd refactor(cfir): model generic member scope ownership`。该提交闭合上述 scope 开关迁移和 owner 继承边界工作；P4 及 P0–P10 的其他计划项仍未完成。

## 2026-10-12 D4 的 CJO 泛型实例化回归

- 恢复 D4 两段式测试的官方期望：CJO 中 `Int64 <: Bound` 满足时，导入的有界 owner extend 与继承 default 产生 1 条歧义和 2 个 `found candidate` note；不满足时为 0 条。note 的 CJO 源位置断言为 `library.cj` 第 15、19 行。另增加普通 CJO 泛型 class 构造器回归，验证两个构造器冲突及第 4、5 行 note。
- 根因包括三层：类型父边缓存版本只观察源码索引，未观察 CJO extend provider 的惰性索引；跨包 extend 可见性把未显式写出的 `std.core.Any` 默认上界当作不可达；把所有 `Library` 成员直接放进普通签名图会重复消费当前 CJMP family 中已由 common→specific 投影表示的 common 成员。
- 修复方向：extend provider 汇总其索引版本并驱动父边缓存失效；可见性规则将 `StdlibClassIds.Any` 视作隐式导出的、默认可达上界；泛型成员图保留普通 CJO 成员，但对当前 family 已有投影的 common CJO 成员只走 family 对应图。
- 验证：`:compiler:frontend:test --tests "*CjmpTwoPhaseCompilationTest"` 为 27/27，通过；包含 D4 bound satisfied/unsatisfied、构造器来源 note，以及 `unmarked specific nominal member is merged once in an extend owner group` 回归。
- 额外运行 `:cfir:resolve:test --tests "*CfirAccessibilityCheckerExtendTest"` 有 1 项 fixture 失败：`shared scope session does not leak private type accessibility between files` 的 `ExtendTestSession` 缺 `CfirLanguageSettingsComponent`，栈止于 `CfirTypeCandidateCollector`。这不是 D4 变更路径；该次运行其余 14 项通过。未运行完整 resolve 模块。
- D4 的这组 CJO 缺口已修复并由两段式端到端用例覆盖；P4 其他 D 项和 P0–P10 仍开放，不能据此更新总体完成度。

## 2026-10-12 D3 接口 default peer 包关系校正

- 按本地官方 `external/cangjie_compiler/src/Sema/InheritanceChecker/StructInheritanceChecker.cpp` 校正包分支：同包 peer、或消费包可见的两个外部 peer 才应用接口继承顺序；其它跨包 peer 按 owner 文件可见性收集。
- 新增共享 `CfirExtendPeerPackageRelation`，声明期 shadow 与泛型实例成员图共用包归属、owner 可见性和 use-site 可见性判定。private 声明期例外、owner 签名替换及 `UNDECIDABLE` 保留规则不变。
- `default_implement_19.cj` 增加跨包子接口 peer 回归，并保留父子方向、无关 peer、泛型 interface owner 替换用例。
- 验证通过：`:cfir:checkers:compileKotlin`；`:cfir:analysis-tests:test --tests '*testDefaultImplement19*' --tests '*testPrivateFunctionsCauseGenericInstantiationAmbiguity*'`，PSI 与非 PSI 共 4 项通过。
- 提交：`af88181ac fix(cfir): align extend peer package ordering`。D3 已关闭；P4 其余事项和 P0–P10 仍开放。

## 2026-10-12 D5 祖先 private extend 与 owner override 归并

- `UseSite` 和 `NominalInstantiation` 的父 scope 现在转换为排除祖先 private extend 成员的继承视角；当前泛型 extend owner 自己的 private 成员仍留在其 owner 组内。
- 泛型实例化签名收集先按声明级 override identity 合并 owner 直接覆盖的父成员。这样 private `extend<T> ExactParent<T>` 与同签名接口 default 在 owner 内只形成一个继承成员；后代仍能继承接口 default，并在 `ExactLeaf<Int64>` 上报告与具体 `Int64` 成员的实例化歧义。
- `privateExtendOwnerGroups.cj` 对齐本机 cjc：private extend 方法标记 `EXTEND_MEMBER_CANNOT_SHADOW` 和 `WEAK_VISIBILITY`；具体 `ExactLeaf<Int64>` 调用保留 `GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS`。
- 验证通过：该 diagnostics2 fixture 的 PSI/非 PSI；`:cfir:analysis-tests:test --tests "*ClassInstantiationCheck*" --tests "*testCjmpGenericCounterpartInheritedDefaultInstantiation*"`。未运行全量 CFIR 测试。
- 提交：`050e7ee99 fix(cfir): keep private extends out of inherited scopes`。D5 核心修复已提交；P4 其余 D 项和 P0–P10 仍开放。

## 2026-10-12 D6 内建泛型成员按 CJMP owner 分组

- 内建目标的实例化成员检查复用 extend owner 分组；owner 自身成员按 owner 视角保留，CJMP 配对 common 签名和继承默认成员进入同一逻辑组，避免 common peer 被当成独立 owner 重复检查。
- `CPointer<Int64>` unmatched 成员与配对 inherited-default 两个 E2E fixture，以及 `inst_dup_functions011–013` 的 Light Tree/PSI 回归通过；`:cfir:checkers:compileKotlin` 通过。
- 提交：`9bf41a2bd fix(cfir): group built-in generic members by CJMP owner`。D6 已提交；P4 其他事项和 P0–P10 仍开放。

## 2026-10-12 D7 跨包 extend 顺序诊断归属

- 复核发现计划中 D7 的“已提交 `d82a4d1b2`”状态不准确：生产代码仍在具体泛型实例化检查中报告跨包顺序诊断，而导入 extend 复查没有该检查。依据本地 `external/cangjie_compiler` 与本机 cjc 1.1.3，三个包的库扩展在消费方未实例化目标时仍应对两个冲突 owner 报告顺序诊断；未读取 GitHub 代码。
- `CfirImportsChecker.reportImportedExtendConflicts` 现对消费方可见的跨包 extend owner/peer 使用共享 `extendMemberPeerDecision`，并把诊断归属到 source owner 或 CJO 对应的 import；具体实例化检查仅保留 peer 纳入行为，不再承担声明级顺序诊断。
- 新增 CJO 多包回归，验证消费方不实例化目标仍在两个 import 上报告 `EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE`。Light Tree/PSI 均通过；同包 `multi_default_impl_01/02` 的 Light Tree/PSI 回归均通过；`:cfir:checkers:compileKotlin` 通过。
- 提交：`f477323f8 fix(cfir): report imported extend sequence conflicts`。D7 已闭合；P4 其他事项和 P0–P10 仍开放。

## 2026-10-12 D8 CJO owner 成员可见性

- 对照主仓本地 `external/cangjie_compiler` 的 `StructInheritanceChecker.cpp` 与 `ModulesUtils.h`，官方继承成员过滤由 `IsInvisibleMember` 按成员包关系和 visibility 决定。原实现只有 CJO owner 缺失 source file 时走 `samePackage || isPublicAPI` 近似，漏掉 internal 子包与 protected 同 module 可见性。
- `CfirAccessibilityChecker` 新增 package-only callable visibility 查询，source 与 CJO owner 都通过该共享规则读取 callable 声明身份、包和 visibility；普通 source access 的 protected 子类规则仍保留在完整 access path。
- 新增无 source file 的库 callable 测试，覆盖 internal 子包、protected 同/跨 module、private 与 public。该单测通过；`:compiler:frontend:test --tests '*CjmpTwoPhaseCompilationTest'` 通过；`privateExtendOwnerGroups` 的 Light Tree/PSI 回归 2/2 通过。
- 初次整类运行发现测试 session 缺少 `CfirLanguageSettingsComponent`，使既有的 `shared scope session does not leak private type accessibility between files` 失败。为测试 session 注册默认语言设置后，`:cfir:resolve:test --tests '*CfirAccessibilityCheckerExtendTest*'` 整类 16/16 通过，失败、错误、跳过均为 0；生产实现未改。提交：`7292ce381 test(cfir): initialize language settings in extend fixture`。D8 实现提交：`a3885ab69 fix(cfir): unify CJO owner member visibility`。

## 2026-10-12 P4 Import00 歧义回归

- 重跑原 P4 验收项 `Import00.testErrAmbiguous00/01`，Light Tree 与 PSI 共 4/4 通过，失败、错误、跳过均为 0。
- 命令：`:cfir:analysis-tests:test --tests '*CfirAnalysisLLTTestGenerated$Function$Import00.testErrAmbiguous00' --tests '*CfirAnalysisLLTTestGenerated$Function$Import00.testErrAmbiguous01' --tests '*CfirAnalysisLLTPsiTestGenerated$Function$Import00.testErrAmbiguous00' --tests '*CfirAnalysisLLTPsiTestGenerated$Function$Import00.testErrAmbiguous01' --console=plain --max-workers=1`。
- 该过滤集用于验收 P4，不代替完整 CFIR、P1 或 P0–P10 gate。

## 2026-10-12 CFIR resolve 全模块复验

- 补齐 `CfirAccessibilityCheckerExtendTest` 测试 session 的默认语言设置后，完整 `:cfir:resolve:test --console=plain --max-workers=1` 为 207 项、4 项失败。此前该类缺组件的失败已消失。
- 余下失败：`CfirMapTypeArgumentsTest` 3 项访问测试桩中故意抛错的 `StubBodyResolveComponents.dataFlowAnalyzer`；`CfirBuiltinSymbolProviderTest` 1 项期望抛出 `IllegalStateException` 但实际未抛。当前尚未证明它们与 CJMP 改动有因果关系，也未将它们归为基线；保留为待审查的全模块 gate 差异。
- 本次完整 resolve 复验不是 P1 或 P0–P10 完成证明。

## 2026-10-12 P5 matcher typed-kind 修复与验证

- `CjmpClassLikeKind` / `CjmpCallableKind` 替代字符串分类。CFIR 分类穷举密封声明类型；type alias、built-in、primitive 分别分类，不被折叠为 class；不支持配对的 callable 返回 `null`，共享资格判据拒绝两侧均无类别的声明。候选准备也跳过无 callable kind 的声明。
- 本机 cjc 1.1.3 对 `apply<T>(f: (T) -> Unit)`、`f(p: (T, Int64))`、enum `Value((T) -> Unit)` 各完成 common→specific 两阶段探针，六个阶段均成功；记录：`.workbuddy/tmp/cjmp_probe113/review-20261012/f1-generic-type-substitution/RESULT.md`。同样三种嵌套类型映射现在由真实 CFIR matcher/evaluator 测试直接覆盖。
- 验证：`:resolution.common:test --tests '*AbstractCjmpMatcherTest*'` 22/22；`:cfir:resolve:test --tests '*CfirCjmp*'` 52/52；最新 `:cfir:resolve:test --tests '*CfirCjmpEnumConstructorPlanTest*'` 19/19（含 3 个 generic function/tuple/enum function-payload 回归及 callable-kind 分类）；`:resolution.common:test --tests '*CjmpMatchEvaluatorTest*'` 28/28。生产源码通过 `:cfir:resolve:compileKotlin`。
- P5 整体仍未完成：F1 generic substitution 回归已提交 `61f1d615f`；F2 已补齐 eager Light Tree、PSI 与 LL 模式变量验收。官方语义依据为本地 cjc 1.1.3 两阶段 pattern 探针；Light Tree/PSI 各 2/2、LL resolver 8/8、LL PSI 7/7 通过。WithoutAliasExpansion 为自动生成的测试入口，因 fixture 不含类型别名而由 `OnlyTestsWithTypeAliasesMetaConfigurator` 跳过。完整 `:cfir:resolve:test` 最近一次复验的 4 项未归因失败仍见上节。typed-kind 补丁已完成本地 Kotlin/官方源码审查并已提交 `1d73a4193`。

## 2026-10-12 LL family 隐式签名准备与 CFIR 调用参数所有权回归

- LL family fallback 现在与单目标 resolver 共用 common 候选筛选及签名准备：在缓存/目标锁外推进 common callable 或 enum-constructor 的隐式签名到 `IMPLICIT_TYPES`，再冻结 declaration facts；fallback 不发布 sibling root facts。
- 新增 family-first 与 concurrent target/family 输入 fixture，分别确认隐式 common 返回类型已解析、family 对应项正确、family-only 不发布 root facts，以及并发两个入口给出同一快照。`CfirCjmpImplicitCommonSignatureTest` 7/7 通过；完整 `:analysis:low-level-api-cfir:test` 178/178 通过（失败、错误、跳过均为 0）。
- 此前一次完整 `:cfir:analysis-tests:test --continue` 为 8,990 项、302 失败、0 错误、401 跳过；其中 140 项来自把 property/variable 函数值调用合成的实参误当作 function/enum-constructor 声明参数。`CandidateDefaultParameterStates` 已将其识别为不参与默认值转移的 synthetic local 参数，`generic_constraint13` 与 `match013` 的 Light Tree/PSI 代表用例 4/4 通过。
- callable-value 参数 owner 修正前的一次完整 `:cfir:analysis-tests:test --continue` 为 8,990 项、120 失败、0 错误、401 跳过；失败中有 24 项触发“mapped value parameters must share one owning declaration”，根因是 synthetic 参数在 builder DSL 中把自身 symbol 误作整组参数的 owner。`Candidate` 现捕获候选声明 symbol 并赋给全部 synthetic 参数；`testDefaultParameter40/42` 的 Light Tree/PSI 回归 4/4 通过。该全量结果早于 owner 修正，完整 CFIR gate 仍待重跑验收。
- P1 和完整 P0–P10 / 70 项仍未完成；此检查点不重算总体完成度。

## 2026-10-12 CFIR 全量复验与多 parent 语义审计

- callable-value 参数 owner 修正后，完整 `:cfir:analysis-tests:test --continue --max-workers=1` 实际完成 8,989 项：84 失败、0 错误、400 跳过。失败 XML 不含 CJMP 测试类或 CJMP 测试名；失败集中在既有 LL 推断和 macro 测试，尚未逐项证明其根因或与本轮变更的因果关系。该结果替代前一节“完整 CFIR gate 仍待重跑”的状态，但不等于整套分析测试通过。
- 新鲜 `testDefaultParameter40/42` Light Tree/PSI 回归仍为 4/4；full-suite 数字以队列任务输出和 `cfir/analysis-tests/build/test-results/test` XML 为准。
- 本地官方源码和版本证据复核发现多 parent 规则必须独立于单 parent first-fit：cjc v1.1.3 的输入仅支持单 common CJO；v1.2.0-alpha.20 的 `CheckCJMP.cpp` 在 `severalParents` 时继续遍历兼容 common，并为不同 parent 建立关系。原有 `firstFitCandidateDiagnostic` 两个 fixture 在 1.1.0 声明两个 common 模块，不能证明 v1.1.x 多 parent 行为。主计划 §12 已记录版本边界；迁移协议 §6 已纠正为按版本区分。
- 当前生产实现仍未接通该版本差异：callable/property plan 使用 `FIRST_SUCCESS`，nominal 合并未保留 common parent 身份，且 checker 的部分类型参数映射仍按单一 counterpart 查询。应作为 P1/P7 未完成项继续实现和验证；不得将该缺口报告为已完成。以上取证仅使用本地工作树、主仓本地源码及本地探针，未读取 GitHub。

## 2026-10-12 alpha.20 多 common parent 匹配接线

- 依据上述版本审计，新增 `CjmpMultipleCommonParents`，精确从 1.2.0-alpha.20 启用。specific session 的 `moduleData.refinementDependencies` 中存在多个直接 parent 时，callable/member 候选遍历全部匹配项；nominal 合并按同一模块图身份分别纳入同名 common 容器，并保持 parent 输入顺序。1.1.x 与 alpha.19 仍走原单 parent first-fit。
- CJO 读取器现保留同一包的多个显式输入快照，并聚合 classifier、callable、extend 与导出名查询。family 快照提供 plural counterpart/type-parameter mapping API；checker、泛型实例化和成员匹配按每个 counterpart 分别消费映射。单值便捷 API 在多个 counterpart 下明确拒绝歧义。
- 修复同名 nominal 的 owner 定位：依赖侧 `getContainingClassSymbol()` 可能按相同 `ClassId` 返回另一 common parent 的 class；member matcher 改按冻结容器的物理成员身份反查 owner，避免第二个 parent 的 `T` 被错误映射，导致 `identity(T): T` 对 `identity(U): U` 判为 `PARAMETER_TYPES`。
- 两段式前端回归验证两个独立 CJO parent 中的泛型 `Box<T>`、成员 `identity` 与顶层 `select` 均映射到 specific `Box<U>` / callable；LLT fixture 同时验证 class、member、top-level callable 和两个 common extend 的 parent 顺序及参数映射。LLT 共享断言改为读取 plural API，以支持同一测试基类覆盖单 parent 与多 parent。
- refinement 图中的直接 parent 保留各自输入身份；其传递 common ancestor 归入首个可达直接 parent。共享 ancestor LLT 回归验证 nominal counterpart 与类型参数映射。
- 验证通过：`:compiler:frontend:test --tests "*CjmpTwoPhaseCompilationTest"`；`:analysis:low-level-api-cfir:test --tests "*CfirCjmpMatchingTestGenerated.testMultipleCommonParents"`；`:cfir:resolve:test --tests "*CfirCjmp*"`。本轮 targeted 验收不代表 P1/P7 或完整 P0–P10 完成；多 parent 下其它语言构造与完整框架 gate 仍需按原计划继续覆盖。

## 2026-10-12 alpha.20 nominal 合并按包遍历顺序

- 复核本地官方 `v1.2.0-alpha.20` 的 `CheckCJMP.cpp:308-335` 后，撤除按 common parent 分组配对 nominal 的 `PER_COMMON_PARENT` 策略及其 parent 身份模型。官方 nominal 阶段按 identifier 独立遍历，不使用 callable 阶段的 `severalParents` 候选循环；当前 source package 中的 common nominal 也没有 direct-parent 身份。
- 增加两段式回归：两个 common parent CJO 加载时，specific source 同时包含一个带默认实现、无需 specific 对应项的 source common nominal；前端完整结束且无 compiler/semantic error。**该 E2E 没有复现旧 `require` 的红结果**：最初探针语料先被缺构造器诊断拦截，合法语料加入前生产 `require` 已被移除；旧路径会给 source nominal 查得 null parent identity 是静态事实。
- 验证通过：`:resolution.common:test --tests "*CjmpContainerMergeEvaluatorTest"`、`:cfir:resolve:test --tests "*CfirCjmpEnumConstructorPlanTest"`、`:compiler:frontend:test --tests "org.cangnova.cangjie.frontend.pipeline.CjmpTwoPhaseCompilationTest.alpha twenty multiple parents accept a source common default nominal"`；针对改动文件的 `git diff --check` 通过。
- 提交：`872de937b fix(cfir): align nominal merge with alpha.20`。这次移除了无官方依据的 nominal parent 分组并覆盖合法 source common 输入；P1/P7 的输入协议、first-wave DAG、完整多 parent 语言构造和 P0–P10 总体验收仍开放。

## 2026-10-12 A3 声明级计划与共享组快照

- eager matcher 保持每次只为当前 specific 声明准备并发布 immutable plan；LL family/group 聚合统一使用 CFIR single-flight 缓存，避免同名组并发重复准备或重复发布。owner 取消、等待者取消、失败驱逐重试及同键递归读取均有定向覆盖。
- 继承来源到具体 owner 的类型替换现在验证结构身份，并对 union/intersection 做无序一致性匹配；CPointer 与 primitive/builtin classifier 等价形态有双向测试。该修正由 LLT 中真实类型结构差异触发。
- 验证通过：providers `CfirCjmpSingleFlightCacheTest` 4/4、`CfirInheritanceProvenanceSubstitutorTest` 8/8；low-level `CfirCjmpImplicitCommonSignatureTest` 9/9、`CfirCjmpMatchingTestGenerated` 12/12；EnumPattern PSI 与 LightTree LLT 各 1/1。
- A3 当前按两项子要求计 1/2：单声明计划、共享 group/family single-flight 与 eager/LL plan parity 已实现并通过定向测试；全量 LLT 相对 v10 的无回归比较尚未完成。总体完成度据此由 37.36% 重算为 38.08%，详见 `docs/cjmp-progress-audit-20261008.md`。不得把定向 LLT 结果表述为完整 P1 或全量 LLT gate 通过。

## 2026-10-12 A2 声明级匹配结果读面

- CJMP 匹配结果现作为 immutable attribute 随其所属 CFIR 声明发布；scope、actualizing 和 checker 消费声明自身结果，不再从 session family 聚合器反查配对关系。
- LL 懒解析入口先检查候选角色；property accessor 的资格按 owning property 判断，但匹配阶段仍直接推进被查询 accessor。普通未标记声明在进入阶段解析前返回，不会因结果查询推进 resolve phase。
- 移除了未被生产消费者使用的反向查询，以及额外的通用 phase-root provider 协议。Pattern binding 继续通过其自身的候选状态与声明级结果读面工作。
- 验证：LL `testPropertyMemberMatches`、`testMultipleCommonParents`、`testRootFactsFailureRetry` 3/3；CJMP generic-instantiation、pattern 的 Light Tree/PSI 回归及 `:cfir:resolve:test --tests "*CfirCjmpPatternMatchingTest*"` 均通过。Kotlin 框架静态审查结论 ACCEPT；这不是全量 LLT gate 结果。

## 2026-10-12 A3 This owner 映射修正与完整 LL 验收

- 新发现：完整 LL 模块测试中 `ContextCollectorSourceTestGenerated.testClass` 与 `testPropertyAccessor` 因 `Box<T>` 和 `Box<T>.This` 的 owner 形状被判为不同而失败。官方文档与本地 Cone 模型表明 `This` 标记改变类型语义视图，不改变 owner 的泛型参数表；因此只在继承来源参数映射的结构判定中忽略 `isThisType`，仍校验 classId/interface，并保留实际 Cone 类型的 This 相等与子类型语义。
- 新增普通 class 与 This view 双向映射测试；caller-first 诊断测试统一断言结果读取会按需将目标推进至 `CJMP_MATCHING`，同时保留首次请求前未预热、符号 identity、返回类型、无诊断及未调用函数体惰性断言。
- 验证通过：providers `CfirInheritanceProvenanceSubstitutorTest` 10/10；caller-first 定向测试通过；完整 `:analysis:low-level-api-cfir:test` 177/177，失败、错误、跳过均为 0。
- A3 仍计 1/2：最新完整 LL 模块已通过，但与 v10 基线的全量 testcase-key 无回归比较尚未完成。按原 70 项等权及子要求比例，总体维持 38.79%（21/70 项整项闭环）；P0–P10 整体仍未验收。

## 2026-10-13 A3 全量 LLT 基线比较与 extend 可见性诊断

- 全量 `CfirAnalysisLLTTestGenerated` 与 `CfirAnalysisLLTPsiTestGenerated` 通过：6,828 项通过、0 失败、0 跳过。相对 v10 的 6,818 个共同 testcase 无回归，新增 10 项全部通过，无移除项；A3 第二项验收闭环，现为 2/2。
- extend shadow 检查复用继承诊断的可见性关系与“可由可见性实现”的判定；仅对无继承关系的兄弟 extend，弱可见性成员实现可见性契约时抑制重复 shadow 诊断。直接 class/extend 继承路径保持原判定。
- 定向验证通过：`testPrivateExtendOwnerGroups` diagnostics2 PSI/LightTree 与 `testExtendFunctionConflictInvalid2` LLT PSI/LightTree 共 4/4；全量 LLT 与 v10 比较无回归。
- A3 现计 2/2。总体完成度更新为 **39.51%**（**22/70** 项整项闭环），剩余 **60.49%**；48 项未闭环，其中 13 项有可计分部分、35 项尚无可计分子要求。P0–P10 总体验收仍开放。审计记录见 `docs/cjmp-progress-audit-20261008.md`。

## 2026-10-13 J1 当前上下文语言版本设置与组件访问闭环

- `isCommonAndSpecific` 通过当前 `CheckerContext.languageVersionSettings` 检查 `CommonSpecificDeclarations`，不再读取任一声明 session 的设置或通过 `runCatching` 隐藏访问失败。
- 保持 common/specific 配对规则不变；统一 specific 开关及显式组件访问/注册的已有实现一并完成 J1 三项验收。
- 定向测试 `testCommonSpecificSuppressedLangver105`、`testCjmpDuplicateSpecific` 与 `:cfir:checkers:compileKotlin` 通过，框架静态审查 ACCEPT。总体完成度重算为 **40.46%**（**23/70** 项整项闭环），47 项未闭环（12 项有可计分部分、35 项尚无可计分子要求）；P0–P10 总体验收仍开放。详见 `docs/cjmp-progress-audit-20261008.md`。

## 2026-10-13 P1/A1 当前源码重审

- A1 从 2/4 更新为 3/4：阶段前移、specific Quest 预配对和声明级读取路径已有实现与 eager 证据；推断返回类型后置协变校验仍内联在 CFIR checker，需按计划移到 `resolution.common.AbstractCjmpChecker`。
- 测试覆盖审计发现 LL caller-first 缺 specific 自身省略返回类型的惰性匹配/后置诊断用例。P1 因此仍未闭环。
- 重算总体完成度为 **40.82%**（**23/70** 项整项闭环），47 项未闭环（12 项有可计分部分、35 项尚无可计分子要求）。详见 `docs/cjmp-progress-audit-20261008.md`。

## 2026-10-13 H4 CJO common 结构化诊断闭环

- 反序列化器为 CJO 声明附加统一 `CfirCjoDeclarationSourceElement`，在保留原始路径/行/列的同时使用声明起点作为诊断锚点；无有效行列的旧元数据不会伪造源码位置。
- `CfirCjmpCommonSideChecker` 现在对源码和 refinement CJO common 消费同一完备配对快照，并通过 `CfirErrors` 报告 `NOT_MATCHED` 和 `MULTIPLE_COMMON_IMPLEMENTATIONS`。`CfirCjmpCommonSideFacts` 只输出声明/占用事实，文案参数由 checker 的诊断 owner 构造。
- 删除 `CjmpDeserializedCommonSideReporter`。前端管线只投影本轮新增、锚定到 `CjBinarySourceElement` 的 CFIR 结构化诊断到 CLI `MessageCollector`，不重新判定 CJMP。
- 回归验证通过：两阶段前端 41/41、resolve 的 enum/mixed occupation 20/20、LL mixed enum candidate 9/9；三项均无失败、错误或跳过。
- H4 从 1/4 更新为 4/4。总完成度由 47.24% 更新为 **48.32%**（**30/70** 项整项闭环），剩余 **51.68%**；40 项未整项闭环，其中 9 项有可计分部分、31 项尚无可计分子要求。H8 的 IDE/LL/Analysis API 共享诊断通道、H11 全链路锚点一致性及 P0–P10 总体验收仍开放；本次结果不代表这些项已闭环。审计明细见 `docs/cjmp-progress-audit-20261008.md`。

## 2026-10-13 H5 common CJO features/options 加载门闭环

- 只对显式配置的 common-part 路径且归属 `dependsOn` 模块的 CJO 执行 features/options 门；header 内容不再决定 common 身份。普通 classpath CJMP CJO 有端到端回归覆盖。
- 官方 alpha.20 加载门依据和 Kotlin 模块路径/refinement 原则复核通过。`CjmpTwoPhaseCompilationTest` 42/42、`CjmpCommonPartCjoTest` 9/9，均无失败、错误或跳过。
- H5 两项验收闭环后，整体为 **49.74%**（**31/70** 项整项闭环），剩余 **50.26%**；39 项未整项闭环（9 项有可计分部分、30 项尚无可计分子要求）。完整审计记录见 `docs/cjmp-progress-audit-20261008.md`。

## 2026-10-13 H6 语言版本加载门

- 提交 `a176efc95` 将 CJMP 语言特性判定集中到 `CjmpLanguageFeatureGate`，并让 CJO 格式门按当前编译语言设置选择行为。CJO 格式兼容门从 v1.3.0-alpha.05 启用，且仅检查显式 common-part CJO；普通 classpath CJO 不触发该 common 加载门。反编译读取使用独立策略。
- 提交 `9f734e8d4` 记录审计与重算。H6 为 6/7，总完成度 **50.15%**（**31/70** 项整项闭环），剩余 **49.85%**；细项与计算见 `docs/cjmp-progress-audit-20261008.md`。
- 验证通过：CJO 加载门 10/10、两阶段前端 CJMP 43/43；`analysis:low-level-api-cfir` 与 `analysis:decompiled:decompiler-to-file-stubs` 编译通过。
- H6 唯一未闭合项是 1.1.x positional `inputChirFiles` 的 driver specific 判据和生产 CHIR 消费。当前 `compiler/cli` 仅有抽象前端 CLI，没有生产 CLI 子类；仓内也没有 `.chir` 加载消费通路。不得用 common CJO 或 alpha.20 专用 CHIR 列表代替该旧版输入角色。

## 2026-10-14 H6 旧版输入角色约束

- 提交 `a8cf45447`：v1.1.x positional `.chir` 输入现在要求显式 common-part CJO；路径合法性检查先于角色配对诊断。多个 `.chir` 仍按独立输入保留，数量限制留给与官方一致的消费层处理。
- `:compiler:cli:test --tests "*CangJieCLICompilerTest*"` 28/28 通过。
- 这只闭合 CLI 参数协议的一部分；生产 `.chir` 解码/语义导入、common/specific CHIR 合并和 backend 接线仍未完成，H6 最后一个计分子要求保持开放。总体完成度不变，见 `docs/cjmp-progress-audit-20261008.md`。

## 2026-10-14 parser alpha.20 common 成员嵌套规则

- 对照本地官方源码与 `docs/cjmp-parsing-remaining-rules-evidence-20261005.md`，alpha.20 起 common 直接成员允许位于 common 或 specific 外层；这条嵌套规则独立于同文件混合角色门。
- `CjmpLanguageFeature.CjmpCommonMemberInSpecificOuter` 从 alpha.20 起生效；raw-CFIR 调用配置将它映射到 `CjmpParsingContext`，共享 parser 只在直接成员角色比较时消费该门。主构造器参数仍走原判据，没有借用成员门。
- PSI 与 LightTree 覆盖 class、struct、interface、enum、extend，且验证门关闭时报错、门开启后放行，以及 specific-in-common 和 common-in-unmarked 仍报错。raw-CFIR context 测试覆盖版本默认值及两个 feature override 的独立投影；PSI 文件副本测试验证新配置随副本保留。
- 定向验证通过：`LanguageVersionSettingsTest` 24/24、`CjmpParsingContextTest` 1/1、`CjmpDeclarationRulesParsingTest` 13/13、`ParsingFileCopyTest` 3/3；无失败、错误或跳过。独立 parser gate 与测试复审均通过。
- 本次不计入 70 项完成分：E14 的共享解析迁移与全版本验收仍开放，且 alpha.20 已删除、alpha.66 删除后 alpha.68 恢复的参数匹配规则需独立按版本门控。总完成度保持审计值 63.84%（42/70 项整项闭环）。

## 2026-10-14 E14 主构造器参数角色匹配版本门

- 参数角色匹配新增独立 `LanguageFeature`，以有序半开版本区间表达官方非单调默认值：`[1.1.0-alpha.34, 1.1.0-alpha.66)` 与 `[1.1.0-alpha.68, 1.2.0-alpha.20)`。覆盖全部49个已登记版本；通过 raw-CFIR 配置投影传入共享 parser，未复用直接成员版本门。
- 普通函数及次级构造器参数不进行 CJMP 角色语义比较；主构造器参数只在有效 CJMP 编译模式及专用 feature 开启时生成比较事实。多参数列表仍遵循官方 helper 的跳过条件。
- 定向验证通过：LanguageVersionSettings 49版矩阵、Raw-CFIR context 投影、PSI parser/参数诊断/文件副本共26项；无失败。独立框架模型与 parser 规则复审均 ACCEPT。
- **计分归因更正**：参数角色的非单调版本门不属于原 E14 六项，作为补充实现记录，不单独增加总分。总分仍为 **64.08%**，因为原 E14 中“两处 parse 条件差异”已由共享 parser 修正并通过 PSI/LightTree 定向测试，连同此前已完成的 file-part 统一，E14 为 `2/6`。参数版本门通过49个已登记版本的专用测试，但不改变原70项口径；整项闭环仍为 **42/70**。

## 2026-10-14 E14 CommonSideFacts 接口边界

- `CfirCjmpCommonSideFacts` 从公开具体 `object` 改为接口，算法移入私有默认实现，公开默认实例以接口类型暴露；CFIR checker 及直接测试调用均已迁移，独立接口边界复审 ACCEPT。
- `:cfir:cfir-tree:compileKotlin :cfir:checkers:compileKotlin`、`:cfir:resolve:test --tests "*CfirCjmpEnumConstructorPlanTest*"`、`:analysis:low-level-api-cfir:test --tests "*CfirCjmpMixedEnumCandidateTest*"` 均通过。
- 诊断格式化 helper 仍在 `CfirCommonSpecificChecker`，尚未迁入诊断 renderer；E14 原“接口/渲染分层”合并项未闭环，本次不增加总体完成分。

## 2026-10-15 E14 构造器修饰符目标与诊断顺序

- `ModifierTarget` 新增 typed `ConstructorRole` 并保留 primary/secondary 身份；static initializer 使用 secondary 角色。primary target 分类先于 static 状态，且公开目标工厂约束角色只能出现在声明头。
- `CfirModifierChecker` 删除 primary CJMP 特判，改为逐项执行 compatibility、direct target、parent target；非法 direct target 跳过 pair 检查。此顺序符合 Kotlin FIR 的逐项检查，也符合官方 v1.1.3 `ParseModifiers.cpp` 的“非法目标报告后跳过冲突表”。官方 target 表允许 static initializer 上的 protected，已补目标并覆盖四种 visibility/static 冲突。
- 删除 COMMON 模式下 `specific init()` 的无效 fixture。`CfirConstructorDelegationChecker` 不把 fake/implicit synthetic primary 算作重复源码构造器；保留显式重复主构造诊断。
- 独立 Kotlin/Cangjie v1.1.3 复审 **ACCEPT**。`:cfir:checkers:compileKotlin`、构造器相关诊断测试、`CfirAnalysisDiagnostics2` 的 Modifier/CommonSpecific 切片均通过。
- E14 原六项从 `2/6` 到 `3/6`；总体从 **64.08%** 到 **64.32%**，仍为 **42/70** 项整项闭环。当前提交包含该闭环子项及其回归；E14 的诊断参数、统一版本门、诊断 renderer 分层和全计划验收仍未完成。

## 2026-10-15 E14 CJMP 诊断参数与消息对齐

- 参照本地官方 Cangjie v1.1.3 `ParseCJMPDecl.cpp`、`CheckCJMP.cpp`、`Node.cpp` 与 `DiagnosticSema.def`：generic frozen 诊断传声明标识符；重复 specific extend 诊断传与 `extendedType->ToString()` 对齐的源码类型文本；class-kind mismatch 使用该 mismatch 的真实 common 对端。
- 重复扩展诊断参数改为 `String`，冻结诊断参数命名改为 `declarationName`，并同步重新生成 Cfir/Analysis API 诊断代码。MessageFormat 模板已转义静态角色名引号，避免丢失官方文案中的 `'specific'`、`'common'`。
- 新增三条实参和渲染断言通过；`:cfir:checkers:compileKotlin` 及 `CfirAnalysisDiagnostics2` CommonSpecific E2E LightTree、PSI 切片通过。重复扩展既有 E2E fixture 在两种入口均实际执行通过。最终只读复审 **ACCEPT**。
- E14 原六项从 `3/6` 更新为 `4/6`；总体从 **64.32%** 到 **64.56%**，仍为 **42/70** 项整项闭环。统一版本门、诊断 renderer 分层、P0 取证和 P0–P10 总体验收仍开放。

## 2026-10-15 E14 checker 版本门统一入口

- checker 内四处对 `CommonSpecificDeclarations` 的非上报型版本判断统一委托 `CjmpGate.isVersionSupported`；组合门继续按版本与编译模式短路，上报型版本入口仍通过 `requireVersionSupport`。raw-CFIR、CFIR tree 和 resolve 的低层判断保留在各自模块的语言特性门，避免 checkers 反向依赖。
- `:cfir:checkers:compileKotlin` 及 `CfirAnalysisDiagnostics2` 的 Langver/feature-override PSI 与 LightTree 目标用例通过；WithoutAliasExpansion 两项按其既有 fixture configurator 设计跳过。独立框架复审 ACCEPT。
- E14 原六项从 `4/6` 更新为 `5/6`；总体 **64.80%**，仍为 **42/70** 项整项闭环。诊断 renderer 分层、P0 取证及 P0–P10 总体验收仍开放。

## 2026-10-15 E14 CJMP 诊断 renderer 分层闭环

- CJMP 诊断载荷改为内部结构化参数；checker 提供语义事实，`cfir:diagnostic-renderers` 统一生成消息，Analysis API 边界渲染为公开 `String`。错误 extend 类型读取 `CfirErrorTypeRef` 保留的源码文本。
- CJMP 参数/renderer 测试 9/9；`CfirAnalysisDiagnostics2` 的 PSI 与 LightTree 相关切片 8/8；独立 Kotlin/官方语义复审 ACCEPT。E14 原六项闭环为 6/6。
- 当前总体 **65.04%（43/70）**，包含 H9 已闭环分值；P0 取证、H6 positional driver/CHIR 生产消费及 P0–P10 收口仍开放。

## 2026-10-15 H9 多 HMPP 来源身份与初始化依赖补强

- specific CJO 的 HMPP roots 和成员只由 P3 family 导出图供给。生产端按规范化 source path + declaration range 或 CJO path + allDecls index 合并多 session 重复节点，并合并各 owner 的已选成员；导入引用闭包优先重映射 common declaration 到 specific 对应项。
- 增强两阶段测试覆盖重复 common CJO、重复 common source root/member，以及 common-only 字段依赖指向 specific 替换字段。当前 `CjmpTwoPhaseCompilationTest` XML 为 52/52，独立 Kotlin/family 导出图增量审查 ACCEPT。重跑命令命中 UP-TO-DATE。
- 2026-10-15 后续查明该失败同时暴露了无效测试签名与 CJD 快照漏报：官方仓颉语法禁止 `x!:` 后接普通 `cb:`；LightTree 丢失自定义 range-error 节点，PSI 快照则未读取 PSI error 的 `reportedRange`。现将有效签名改为 `cb!:`，并让两种快照统一保留通用、自定义 token/source-range 和 CJMP 文件角色错误节点及其报告范围。新增合法/非法参数顺序回归，`:cfir:cfir-serialization:test --tests "org.cangnova.cangjie.cfir.serialization.cjd.CjdSidecarParserTest"` 通过（13 passed、1 skipped、0 failed）；本地官方 v1.1.3 与独立 parity gate 均 ACCEPT。
- H9 早前已按 2/2 计入总体完成度；本次补强不重复计分。总体维持 65.04%（43/70）；P0 取证、H6 positional driver/CHIR 生产消费及 P0–P10 收口仍未完成。

## 2026-10-15 H6 生产 CHIR 消费实现边界

- 当前 `OfficialChirV113WirePackageDecoder` 只保留根表元数据、四个 union 向量的标签/序号和原始字节；它既未验证嵌套表与跨表 ID，也未构造 CFIR/CHIR 语义图，不能作为生产消费入口。仓内 `ChirPackageCodec` 使用不同 schema，禁止直接读取官方 `.chir`。
- 官方 v1.1.3 的旧版 Specific 判据仍是 positional `inputChirFiles`；common CJO 是前端声明来源，`.chir` 是独立的驱动链接输入。官方消费者只接收一个 positional CHIR，并将 RAW common 图完整反序列化后再与 specific AST 翻译结果合并。
- importer 必须按版本固定 schema，并维护独立的 type、value、expression、definition 四个 ID 域；按声明/值/表达式构建和延迟配置阶段恢复引用，完成嵌套结构及所有跨表引用校验后，才发布完整语义包。禁止把官方 union tag、局部 ID 或生成表对象泄漏到 backend API。
- 合并身份不能取自现有 `Cfir2ChirIds` 的 `symbol.debugName`：它不含 package、owner 或 overload signature。common CJO 反序列化声明已有 `CfirCjoDeclarationOrigin(packagePath, declarationIndex)`，可据此读取原始 `mangledName`；source specific 与 common 的对应关系必须来自已完成的 CFIR matcher/family 事实，不得按显示名猜测。官方 CHIR identifier 使用 `@` 加 post-sema mangled name；`rawMangleName` 是另一语义，FOREIGN 函数存在专门索引规则。
- 分层：`chir-tree` 负责 v1.1.3 wire 验证与完整语义导入；common/specific actualization 与跨包 ID 重映射位于 CHIR 语义/链接层；CLI driver 按旧版 positional 输入选择 Specific，并在后端之前调度导入和合并。每层只暴露稳定的仓内语义 API。
- H6 端到端验收至少要证明官方 `cjmpRawCommon` 图的全部引用进入 CHIR validator、两个局部 ID 均从 1 开始的包能按官方声明身份安全合并，以及真实 CLI 输入在 specific 前端后进入 LLVM backend 并实际调用 common 函数；损坏嵌套表、非法引用和合并冲突必须在 backend 前失败且不发布部分图。
- 依据：本地官方 v1.1.3 `Option.h`、`CHIRDeserializer.cpp`、`CHIRDeserializerImpl.h`、`ASTPackage2CHIR.cpp`、`BaseMangler.cpp`；Kotlin `IrModuleDeserializer`、`IrActualizer`；当前工作树 `OfficialChirV113WirePackage.kt`、`Cfir2ChirIds.kt`、`CfirCjoDeclarationPosition.kt`。当前唯一开放项不据此计分；只有生产导入、actualization/merge、driver/backend 接线及上述验收完成后才闭环。

## 2026-10-15 H6 官方 CHIR 图验证与语义导入

- `chir-tree` 新增基于随 v1.1.3 schema 生成的 BFBS 反射描述的完整嵌套图校验，并构造带独立 ID 域及类型化引用的不可变语义包；完整引用域检查通过后才返回，避免将无效或部分图交给下游。CLI RAW phase 用例改为官方 v1.1.3 RAW 样本。
- `:chir:chir-tree:test --tests "*OfficialChirV113*"` 通过，9 项；变更提交 `2dfee3886`。
- CLI 的旧版 positional `.chir` 路径现读取并验证 v1.1.3 semantic package、拒绝非 RAW phase，并在全部输入验证成功后一次性写入 `CompilerConfiguration`；`:compiler:cli:test --tests "*CangJieCLICompilerTest*"` 通过，30 项，提交 `8652b137c`。
- 这闭合 wire 验证和语义导入基础，不改变 70 项整体计分：**65.04%（43/70）**。actualization/跨包 ID 重映射、specific CHIR 合并、CLI 到 LLVM backend 的生产调度和端到端 common 函数调用仍未完成。

### 2026-10-15 H6 CFIR→CHIR 稳定身份注入边界

- `Cfir2ChirSemanticIdentityProvider` 现在从模块级转换器进入共享 components；声明/header、局部与导入 callable、导入变量均通过同一身份查询边界，未覆盖的符号保留现有 CFIR 本地 ID。
- 定向回归用独立 common 与 specific 声明证明它们可共享同一 CHIR semantic ID，并验证 specific 包的导入调用指向该 ID。`:chir:cfir2chir:test --tests "*Cfir2ChirCoverageTest"` 通过。
- `fromCjmpFamilies` 直接消费 family snapshot 的 specific→common matcher 关系，不按声明名推导；多个 common counterpart 的身份冲突或匹配 common 缺少稳定身份会在转换前失败。未配对 CJO 引用仍可由稳定二进制来源回调解析。
- 回归同时验证缺少 common 身份和多个 common 身份冲突时拒绝生成 provider。
- 这只建立 CFIR family/CJO 映射可注入的转换器接口；生产身份映射、specific CHIR actualization/合并、CLI 到 LLVM backend 调度与端到端调用仍未完成，不增加 70 项计分。

## 2026-10-15 H6 CJO 来源到 CHIR 身份

- 新增 `chir:cjmp-linker` 的 `CfirCjmpChirSemanticIdentityProviderFactory`：按声明的规范化 CJO 路径和 `allDecls` 索引读取当前依赖 provider generation 中已加载的快照；普通声明使用官方 post-sema `mangledName` 和 `@` 前缀，FOREIGN 函数使用 `mangledBeforeSema` 且不加前缀。specific→common 关系仍直接取自 family snapshot。
- 生产 session 使用组合 `CfirSymbolProvider`，身份 factory 递归查询其子 provider。同一路径在多 session 重复加载时，只有各快照的同一声明索引解析到相同官方 mangle 才合并身份；来源/索引/表缺失、mangle 缺失或身份冲突时明确拒绝，不按显示名猜测。
- `:chir:cjmp-linker:test` 通过，覆盖精确路径/索引、组合 provider 查找、相同来源的重复快照、冲突身份和缺失官方身份；`:compiler:frontend:test --tests "*CjmpTwoPhaseCompilationTest.frontend pipeline routes argument outputs and round trips common CJO"` 通过。
- 该前端 E2E 的 common CJO 由仓内 writer 生成，其声明没有官方 `mangledName`，因此只验证 CJO 来源 provenance，不能作为官方 CHIR 身份映射证据。身份解析由带明确官方 mangle 字段的 CJO 快照测试覆盖。生产 actualization、CHIR 合并、driver/backend 调度和端到端 common 调用仍未完成，不增加 70 项计分。

## 2026-10-15 H6 独立代码审查与修复

- 两个子代理只读审查 `0a2bb2b8d..b546d1e81`，合计发现四项：CLI 解码的 positional CHIR 没有生产消费方；CJMP 身份工厂没有生产调用点；FOREIGN 函数使用了错误的 identity 字段/前缀；旧版 driver 接受多个 positional CHIR。
- FOREIGN identity 已改为官方 `mangledBeforeSema` 原始键且不加 `@`，并覆盖属性位、raw mangle 缺失与普通函数路径。旧版 driver 现在在进入前端前拒绝多个 positional CHIR，测试改为单文件嵌套损坏图验证原子导入。提交：`b07f37295`。
- `:chir:cjmp-linker:test :compiler:cli:test` 通过。P1 生产消费与生产身份工厂接线仍开放；这些修复不增加 70 项计分。

## 2026-10-15 H6 前端产物交付给下游编译阶段

- `AbstractFrontendPipeline.execute` 保持原有布尔 API；新增 `executeWithArtifact` 返回阶段成功状态和最终 `PipelineArtifact`。前端阶段正常结束却未返回管线产物时显式触发不变量错误，避免静默丢弃下游需要的 CFIR 结果。
- `:compiler:cli:test` 全量通过；新增 CLI 管线回归确认参数、配置及实际阶段产物原样交付。该接口为 linker/backend driver 提供结果边界，但当前没有生产 driver 调用它，positional CHIR 消费、actualization/合并及 LLVM 后端接线仍未完成，不增加 70 项计分。

## 2026-10-15 H6 仓内 CHIR 包 actualization 核心

- `DefaultCjmpChirPackageActualizer` 校验输入包后，按 matcher 提供的稳定 CHIR identity 替换对应 specific 声明；无 matcher 关系的同 ID 碰撞、函数签名变化、重复 ID 和无效结果均失败。common-only 与 specific-only 声明保留，类/结构体/枚举/extend 成员按同一身份规则递归处理。
- 两侧包初始化函数身份不同时，生成调用 common 与 specific 初始化函数的组合入口；相同身份时沿用已被 specific actualize 的函数。声明属性按 key 合并并由 specific 值覆盖。
- `:chir:cjmp-linker:test` 通过，覆盖 actualization 替换与保留、函数调用身份稳定、未经 matcher 授权的 ID 冲突、签名不兼容和初始化入口组合。
- 该实现只覆盖仓内 `ChirPackage` 之间的 actualization。`OfficialChirV113SemanticPackage` 到仓内 CHIR 的完整语义转换、生产 linker/CLI 调用、LLVM 调度和真实 common 函数端到端调用仍未完成；不增加 70 项计分。

### H6 官方 RAW fixture 导入规模核验

- 按官方 v1.1.3 根表的 union tag/value 向量复核 `cjmpRawCommon` fixture，顶层 union 项为 TYPE 1,258、VALUE 2,858、EXPRESSION 1,410、DEFINITION 190，共 5,716 项；此前 1,404/5,710 的统计漏计了 6 个 tag 为 1 的 UnaryExpression。数字是顶层索引项数，不含嵌套表数量。
- 因此只转换目标 common 函数或忽略未识别的表不能满足“完整 common RAW 图进入仓内 CHIR validator”的验收条件。下一步必须保持四个官方 ID 域及每个受支持 schema union 的明确映射，并在完整引用闭包转换成功后统一发布；未知 union、损坏引用或无法表达的 backend 语义必须明确失败。
- 该规模核验是实现范围证据，不代表新增功能或计分；H6 仍需官方图转换、生产消费、actualization/ID 重映射、driver/backend 接线和端到端 common 函数调用。

### 2026-10-16 H6 官方 CHIR→仓内 CHIR 导入复核

- 独立只读审计确认：`OfficialChirV113SemanticPackageDecoder` 能保留四个顶层 ID 域与完整嵌套表图；后续 `DefaultOfficialChirV113ChirPackageImporter.importFunctions` 仍只导入按稳定 identity 选中的函数及直接 `Apply` 函数依赖闭包，未导入全部 package definitions、globals 和 imported variables。
- 当前 importer 位于 `chir/cjmp-linker`，而既定分层要求 v1.1.3 版本化完整语义导入位于 `chir-tree`。它的 value/expression/type 映射仍是子集：官方 15 种 Value、55 种 Expression、4 种 Definition 并未全部映射；递归类型被拒绝，类父类型也未从官方图导入。现有两条 importer 测试只验证单个选中函数闭包和缺失 identity，不证明 5,716 个顶层项能进入仓内 `ChirPackage` 并通过 validator。
- 所以 H6 的完整图导入、actualization、生产 driver/backend 调度与端到端调用仍未闭环；按原 70 项计分不增加，总体仍为 **65.04%**（43 项整项闭环，另 4 项部分计分、23 项尚无可计分子项）。
- 下一步先在 `chir-tree` 建立覆盖四个 ID 域的两阶段语义转换与完整引用闭包恢复，再原子发布并运行仓内 validator；链接层只消费完整仓内语义包处理 identity/actualization，不继续扩展按函数挑选的 importer。

### 2026-10-16 H6 完整官方语义图转换基础

- 提交 `761b72f25` 在 `chir-tree` 增加官方 v1.1.3 四域身份表、不可变源语义图、完整嵌套字段/union/字节/跨域引用转换，以及根向量和引用闭包校验。转换先注册全包身份，再解析字段引用；校验有错时不返回部分图。测试覆盖 5,716 个顶层表和缺失跨域目标。
- 这一步的输出是完整 `ChirSourceSemanticGraph`，尚未映射为 linker 使用的完整 `ChirPackage` 声明/函数/CFG，也没有调用 `DefaultChirValidator.validatePackage` 或接入生产加载链；所以它是 H6 的转换基础，不是 H6 闭环，不增加 70 项计分，整体仍为 **65.04%**。
- `:chir:chir-tree:test --tests "*OfficialChirV113SemanticPackageDecoderTest*"` 通过，7/7；覆盖 5,716 项导入与跨域引用闭包。首次运行确认官方图中 ID 0 表示可选引用缺失，转换层现规范化为 NullValue，随后全类测试通过。该验证尚未覆盖完整 `ChirPackage` 映射和 production linker 接线。
- 测试依赖编译还发现工作树 JVM backend 的未提交改动引用了不存在的 `declaration` 属性；修正为当前函数 `function` 后 `compiler:jvm-codegen:compileKotlin` 通过。后续先完成源语义图到完整仓内包模型的框架转换和 validator 接线，再验证并接到 linker。
- `:chir:cjmp-linker:test --tests "*OfficialChirV113ChirPackageImporterTest*"` 通过，2/2；按仓内表达式契约修正选中函数投影中的 side-effect store / raw-array literal-init 结果字段。该测试仍只证明单函数切片，不证明全包映射。`:compiler:jvm-codegen:test` 通过；这些局部回归不增加 H6 计分。

### 2026-10-16 H6 仓内类型与结果身份模型

- 提交 `47dcdf35d` 为 nominal class/struct/enum 增加稳定声明身份，补齐函数类型 ABI/vararg、RawArray 维数、VArray 长度、泛型身份、枚举 selector 等官方类型事实；表达式结果 VALUE 身份与 EXPRESSION 身份分离，并更新 validator、codec、actualizer 与 JVM lowering。
- 全量 `:chir:chir-tree:test`、`:chir:cjmp-linker:test` 和 `:compiler:jvm-codegen:test` 均通过。此模型为完整官方包映射提供了仓内表示，但 `OfficialChirV113SemanticPackage` 尚未全量生成 `ChirPackage` 的 definitions、globals、imported values 与所有函数/表达式，因此不增加 H6 分值，整体仍为 **65.04%**。
- 下一步在 `chir-tree` 实现全包导入器并覆盖四域所有顶层项；完成后以 `DefaultChirValidator.validatePackage` 校验，再将完整包交给 linker。

### 2026-10-16 JVM 后端局部 alloca 语义修正

- CHIR 的局部 `ref` alloca 与 C pointer alloca 现在按结果类型分流：局部引用绑定到 alloca 的 semantic ID 并分配 JVM local slot；`cpointer` 仍走 `ByteBuffer` carrier。两者都要求显式结果类型，避免局部地址被误当成堆指针。
- JVM 局部变量回归改用官方 alloca 形状（size operand 为 1、结果为 `ref<T>`）。`:chir:chir-tree:test :chir:cfir2chir:test :chir:cjmp-linker:test :compiler:jvm-codegen:test` 通过；提交 `0df22155c`。
- 这是后端 lowering 正确性修复，不闭合原 70 项中的 H6 项目，整体计分保持 **65.04%**。

### 2026-10-16 H6 全包导入后的 LLVM 联调边界

- 当前 `DefaultOfficialChirV113ChirPackageImporter.importPackage` 已将官方 common 包的函数、变量、类型定义和函数体全部映射到仓内 `ChirPackage`；`imports every official function in the common package` 定向测试通过，检查了官方函数/变量/定义/基本块/表达式的完整身份覆盖，且全包通过 `DefaultChirValidator`。这证明仓内全包导入，不代表 H6 生产链或端到端验收完成。
- 全包 actualize 后交给 `DefaultChirToLlvmCodeGenerator` 的端到端用例失败于 `rawarray.allocate`，位置是官方包初始化可达的 `gv$_EMPTY_UINT8_RAW_ARRAY` 函数。由 package-init/literal-init 调用图得到的可达闭包包含该函数，因此不能通过删掉“无关”函数、只 lower `Value` 或忽略初始化函数解决。
- 官方实现依据：`external/cangjie_compiler/src/CodeGen/Base/ArrayImpl.cpp` 在 RawArray 分配前检查负长度，再调用 `IRBuilder2::AllocateArray`；`CJNativeIRBuilder.cpp` 使用 RawArray `TypeInfo`、长度、元素尺寸及 `llvm.cj.malloc.array` / generic intrinsic。运行时 `external/cangjie_runtime/runtime/src/CompilerCalls.h` 暴露 `MCC_NewArray` 等入口，但均要求 ABI 兼容的 `TypeInfo`。仓内当前 LLVM lowering 尚无 TypeInfo 发射器；用通用 `cangjie.alloc` 或把 RawArray 降为普通指针都不满足该语义。
- 验证：`:chir:cjmp-linker:test --tests "*OfficialChirV113ChirPackageImporterTest.imports every official function in the common package"` 通过；`:chir:cjmp-linker:test --tests "*OfficialChirV113ChirPackageImporterTest.links complete official common package before lowering a specific caller"` 失败，错误为 `unsupported other operation 'rawarray.allocate'`。H6 与 70 项总完成度不加分，仍为 **65.04%**。
- 下一步在 LLVM backend 建立与运行时 ABI 对齐的类型信息与 RawArray lowering，再以官方包初始化图及完整 common/specific 编译验证；不得用测试投影或删减导入图绕过该调用。

### 2026-10-16 H6 交接状态更正

- 上述 RawArray 未支持状态已过期：当前全包 importer 将 official common package 完整映射到 `ChirPackage` 并通过 `DefaultChirValidator`；`links complete official common package before lowering a specific caller` 已通过，覆盖初始化可达闭包中的 `rawarray.allocate`。LLVM lowering 现发射 TypeInfo、primitive/object/generic 分配入口及负长度分支。
- 复核并修复了跨模块 actualizer 在目标模块已存在时遗漏 matcher 合并结果、LLVM `.ll/.bc` 写出失败留下部分产物、CLI `-O` 未传到 LLVM，以及 RawArray TypeInfo 名称/外部符号/负数组异常尺寸 ABI 问题。新增 linker 失败时不得启动 backend 或写 LLVM 文件的 CLI 集成测试。
- `:compiler:codegen:test` 全量通过（94 项、4 项跳过）；CLI 优化映射、CLI linker failure、linker importer/actualizer 定向测试通过。CLI 成功生成 bitcode 的测试仍未获验收：本机 LLVM 头文件为 15.0.4，项目 codegen 版本门要求 LLVM 18，且 `cangjie.llvm.version` 未配置；不得用伪造版本号替代验收。
- H6 最后一项生产 positional CLI `.bc` 成功输出/验证仍开放，70 项方案仍为 **65.04%（43/70 完整、4 项部分、23 项无可计分部分）**。P0/P0–P10 总体验收仍开放。

### 2026-10-16 E1 与 E13 框架级收口

- E1 依官方条件覆盖 common/specific 抽象类函数与属性，保留 `FROM_COMMON_PART`、声明文件和显式 ABSTRACT 属性差异，并使用结构化 CJMP 诊断参数；E13 将隐式 abstract 判定合并到 `raw-cfir-common` resolver，PSI 与 LightTree builder 共用，并按函数/属性分别提取 body 事实。
- common/specific diagnostics 的 PSI/LightTree 定向用例 4 项通过；`DeclarationModeRawCfirStatusTest` 通过。独立 gatekeeper **ACCEPT**，代码提交 `8592f51b6`。
- E1 `3/3`、E13 `2/2` 闭环；原70项总分更新为 **70.76%**（47 项整项闭环、4 项部分计分、19 项尚无可计分子要求），剩余 **29.24%**。P0 取证、H6 positional CLI 实际 bitcode 和 P0–P10 总体验收仍开放。当前未发现 LLVM 18 工具链；H6 的真实 `.bc` 验收仍受版本门阻塞。

### 2026-10-19 CFIR→CHIR 接收者与类型层次续接

- `Cfir2ChirTypeMapper` 将父类、接口和 extend 类型按 CHIR 名义层次类型映射；class 分开保存单一父类与接口，保留声明级及函数级泛型参数身份。`Cfir2ChirFunctionBodyConverter` 登记真实 dispatch receiver ABI 参数，并对继承成员调用和 `super` 调用将接收者适配到目标 owner 类型。
- JVM backend 处理 `CANGJIE_TYPE_CAST_NA` 的引用适配；FlatBuffers schema 与生成 API 一并增加函数 receiver/throws、reference mutability、`this` owner 和 Debug 环境类型字段。
- 验证：`:chir:cfir2chir:test --tests '*Cfir2ChirCoverageTest*' --rerun-tasks` 18/18；`:chir:cfir2chir:test :compiler:jvm-codegen:test` BUILD SUCCESSFUL，最新 XML 分别为 18/18 和 53/53，无失败、错误或跳过。独立只读框架复审 ACCEPT；普通泛型 receiver 风险未能复现，既有虚调用 backend 贯通仍单独开放。
- 本批不改变原 70 项计分；整体按最新复审后的主计划审计值 **70.76%** 记录，H6 为 6/7。后续须继续完整 P0–P10 目标及生产前端泛型继承/上界 receiver 验收。

### 2026-10-19 P1 alpha.66 LL 最终 family 快照

- LL 现在由 `CJMP_FINAL_MATCHING` 阶段在 specific 隐式签名稳定后发布最终配对事实；family/package 快照入口先推进组内签名，且在进入聚合 single-flight 缓存前完成，避免 common-side checker 冻结 Quest 状态下的候选关系。签名准备在 specific 目标写锁外执行。
- 新增 caller-first 包级回归：specific `caller.cj` 不调用 `inferred`，首轮 caller 诊断请求仍触发 common-side 包检查；断言该请求返回 `CFIR_NOT_MATCHED`，specific 返回类型已经推断为 Bool 且结果已由最终阶段发布，再验证 family 不含 common 对应物。
- 验证通过：`:analysis:low-level-api-cfir:test --tests "*CfirCjmpCallerFirstDiagnosticsTest" --tests "*CfirCjmpImplicitCommonSignatureTest"`，caller-first 12/12、implicit common signature 9/9；CLI 的 generic parser、parameter default、inferred return 三个 alpha 版本边界测试 3/3。Kotlin FIR 对位与测试语义的两次独立只读复核均未发现具体缺陷。
- 提交：`6f1ae87ca fix(cjmp): finalize alpha.66 matching before family snapshots`。本批不改变原 70 项计分；总体仍为 **70.76%**（47 项整项闭环、4 项部分计分、19 项尚无可计分子要求），H6 为 6/7，P0 与 P0–P10 总体验收仍开放。
