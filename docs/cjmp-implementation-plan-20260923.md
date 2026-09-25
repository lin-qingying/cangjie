# cjmp（common/specific）实施方案 — 复刻 Kotlin K2 KMP 架构

> 2026-09-23。三方取证依据：
> 1. 官方 `external/cangjie_compiler` origin/main（799e9f65，2026-09-22）— CJMP 全语义；
> 2. Kotlin `external/kotlin`（K2 redesign 后）— KMP/MPP 架构清单（总纲 `docs/fir/k2_kmp.md`）；
> 3. 本仓库现状盘点 + git 历史（被删 KMP 移植考古）。
>
> 语义权威 = 官方 cjc / external/cangjie_compiler；架构权威 = Kotlin K2 KMP。两者冲突处一律语义从仓颉、结构从 Kotlin（见 §3 裁决点）。

## 1. 现状与缺口总表

### 1.1 已存活（不重做）

| 组件 | 位置 |
|---|---|
| `CfirDeclarationStatus.isCommon/isSpecific` + `Modifier.COMMON(0x80)/SPECIFIC(0x100)` 位 | `cfir/cfir-tree/gen/.../CfirDeclarationStatus.kt:33-34`、`CfirDeclarationStatusImpl.kt:259,291,295` |
| 模块模型：`CaModule.directDependsOnDependencies/transitiveDependsOnDependencies`、`LLCfirModuleData.refinementDependencies`、`CfirModuleData.isCommon`、`TargetPlatform.isCommon()` | `analysis/analysis-api/.../CaModule.kt:35`、`analysis/low-level-api-cfir/.../LLCfirModuleData.kt:68`、`common/.../TargetPlatform.kt:120` |
| CJO 读取侧：`AttrBit` 映射 COMMON/FROM_COMMON_PART/COMMON_NON_EXHAUSTIVE/SPECIFIC/COMMON_WITH_DEFAULT | `cfir/cfir-serialization/.../CfirDeclDeserializer.kt:224-266,769-772` |
| 30 条 sema 诊断 + 消息 + 不可抑制登记 | `CfirDiagnosticsList.kt:1989-2142`、`CfirErrorsDefaultMessages.kt:1154-1183` |
| `CfirCommonSpecificChecker`（已挂 `LanguageFeature.CommonSpecificDeclarations` v1.1.0 门控） | `cfir/checkers/.../CfirCommonSpecificChecker.kt` |
| AA 测试框架 dependsOn 机制 | `analysis/analysis-test-framework/.../CaSourceModuleImpl.kt:44,74` |

### 1.2 缺口（正向通路五断 + 语义面四缺）

| # | 缺口 | 现状证据 | 官方/Kotlin 对位 |
|---|---|---|---|
| G1 | **词法/语法层无 `common`/`specific`** | `CjTokens.java`、`CangJieLexer.flex`、`CangJieParsing.kt:212 ModifierKind` 均无；被容错解析为垃圾 modifier | 官方 `Tokens.inc:170-171` 普通 TOKEN + `ParserModifierRules.cpp` 冲突表；Kotlin `KtTokens.java:336-337` softKeywordModifier |
| G2 | **raw builder 不装填 isCommon/isSpecific** | `AbstractRawCfirBuilder.kt:247-285 buildDeclarationStatus` 参数表无此二项；psi2cfir/light-tree2cfir 无法置 true；全仓库唯一置位点是测试手工赋值 | Kotlin `PsiRawFirBuilder.kt:794-795`、`ModifierFlag.kt` 位标志 |
| G3 | **跨模块配对路由结构不通**（致命） | `LLModuleWithDependenciesSymbolProvider.kt:79-87` 先查本模块；checker 的 `symbolProvider.getClassLikeSymbolByClassId` 永远查到自己（isCommon=false 即 return）；`MULTIPLE_COMMON_IMPLEMENTATIONS` 永不触发 | Kotlin `FirExpectActualResolver`：expect 候选只在 `transitiveDependsOn` 模块的包 scope 中找 |
| G4 | **driver 无分步编译支撑** | `compiler/` 全模块 grep commonPart = 0；无编译模式选项 | 官方 `--common-part-cjo/--common-part-chir`（`Options.inc:593-598`）+ `parse_unexpected_cjmp_decl` 模式门 |
| G5 | **序列化写入侧不发 COMMON 位** | `CfirCjoPackageMetadataProducer.kt:327-342` 无 COMMON/FROM_COMMON_PART/SPECIFIC | 官方 `ASTWriter.cpp:1573-1583,1648-1678` 位图写出 + common cjo 重标注 |
| G6 | **无配对解析阶段与配对结果存储** | `CfirResolvePhase` 无 EXPECT_ACTUAL_MATCHING（历史 KDoc 引用被 703027568 删除）；checker 现在靠 symbolProvider 现查现配 | Kotlin `FirExpectActualMatcherTransformer` + `FirDeclaration.expectForActual` |
| G7 | **checker 语义偏差 5 处** | ①返回类型用 `equalTypes`（官方是子型/协变）；②命名参数只查个数不查名字（官方：命名参数名必须相同）；③无泛型约束宽严比较（官方 `CheckGenericTypeBoundsMapped`：child 更宽松）；④无默认值/注解传播动作（官方 `PropagateDefaultArguments`/`PropagateCJMPDeclAnnotations`，且**先匹配后传播**，否则报误导性 `sema_not_matched`——CheckCJMP.cpp:1358-1372）；⑤注解比较用 `Set<AnnotationMatchKey>`（Checker:759-777），官方按**出现次数多重集**一一对应（CheckCJMPAnnotations.cpp:150-196） | 见 §3 裁决点 D8 |
| G8 | **parse 族 12 条诊断整族缺失** | 仓库只有 sema 30 条 | 官方 `DiagnosticParser.def:265-277` |
| G9 | **死诊断 1 条 + 反官方诊断 1 条** | `COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH` 无 reporter；`COMMON_GENERIC_RENAME_NOT_SUPPORTED` 在报，但官方按位置映射**允许**重命名、官方条目本身无触发点 | Fixture Edit Gate：官方证据 |
| G10 | **10 个 fixture 全是占位**（只期望 `CLASSIFIER_REDECLARATION`，零 CJMP 诊断）；无多模块 fixture | `testData/diagnostics2/common-specific/` | Kotlin `// MODULE: name(deps)` 指令机制（`ModuleStructureDirectives.kt`） |
| G11 | **parse 族诊断不能落在 parser 内**：本仓库 parser 完全配置无关（`CangJieParser.kt:147-182` 只注入 languageModuleName/sourceKind；psi 模块 grep LanguageVersionSettings = 0），且编译管线把 `PsiErrorElement`/`ERROR_ELEMENT` 静默丢弃（`AbstractLightTreeRawCfirBuilder.kt:42-45 ignoredTokens`），CFIR 诊断面无 parse 命名空间 | Kotlin 同样 parser 零版本感知；parse 错误经 `LightTreeParsingErrorListener`（注入 session 的 languageVersionSettings）转 FIR 诊断 | 见 D12/D13 |
| G12 | **门禁逃逸 1 条**：`COMMON_PACKAGE_HAS_MAIN` 由独立 fileChecker `CfirCommonPackageMainChecker`（`CfirCommonSpecificChecker.kt:709-726`）报告，无 supportsFeature 保护；1.0.x 下 `common func main` 可达 | 其余 28 条族诊断均已被两处入口门禁覆盖（`CfirCommonSpecificChecker.kt:51`、`CfirCommonCtorImmutableAssignChecker.kt:34`） | 中心化门禁辅助 D13 |
| G13 | **反序列化侧裸装填**：`CfirDeclDeserializer.kt:769-772` 无条件装填 COMMON/SPECIFIC 位（且两行重复赋值瑕疵）；`CfirDeserializationContext` 已携带 languageVersionSettings 但未消费。官方加载侧有 6 层门（cjoVersion、包名、features 子集 `common⊆specific`、编译选项匹配），本仓库一项都没有 | 官方 `ASTLoaderCJMP.cpp:66-101,185-243`、`ASTLoader.cpp:57-69,352-390`；属性位本身官方也不带版本门（由 cjoVersion 承担） | 见 D15、§8.3 |
| G14 | **无编译模式管道、无 CLI feature 开关**：CheckerContext 无 compilerArguments/模式字段；无 `-XXLanguage` 式 CLI 入口（REPAIR_LOG.md:8287 记载）；feature 覆盖只存在于测试基建 | 官方模式判据 `IsCompilingCJMPSpecific() = commonPartChirs 非空`、`IsCompilingCJMP() = Specific || outputMode==CHIR`（Option.h:1195-1203）；Kotlin `-Xmulti-platform → @Enables(MultiPlatformProjects) → specificFeatures → NOT_A_MULTIPLATFORM_COMPILATION`（`CommonCompilerArguments.kt:638-647`） | 见 D14、§8.2 |
| G15 | **类型精化缺失**（三轮发现）：specific 模式中对已配对 common 类型的引用（超类型列表、成员类型、返回类型、extend 目标）必须解析到 specific 声明；官方在合并期做类型替换（`GetInheritedTypesWithSpecificImpl`，CheckCJMP.cpp:1446-1458），Kotlin 对位是 type refinement（k2_kmp.md 专章 + `ExpectActualUtils.kt` actualize）——现方案只有声明查找遮蔽，无类型引用精化 | 官方 `GetInheritedTypesWithSpecificImpl`；Kotlin `compiler/fir/providers/.../types/ExpectActualUtils.kt` | 见 Phase 2.6 |
| G16 | **LL 分析管线无 CJMP_MATCHING 入口**（三轮发现）：`analysis/low-level-api-cfir` 的 phase target resolver 链需要对应挂点——历史上被删的 `LLCfirExpectActualMatchingTargetResolver` KDoc（703027568）即此物；不补则 LLT 的 LL 路径与 IDE 路径永远看不到配对结果，PSI/raw 双路径验收形同虚设 | Kotlin：`FirResolvePhase.EXPECT_ACTUAL_MATCHING` 被前端与 LL 共同消费 | 见 Phase 2.7 |
| G17 | **反序列化期加载门诊断无报告通道约定**（四轮发现）：G13 的四层加载门（cjoVersion/包名/features 子集/选项）发生在 session/checker 之前，`DiagnosticReporter` 尚不可用——需要约定加载期诊断的收集与外显通道 | 仓库先例：cjd 家族 collector-list 模式（`CjdAnnotationConversionDiagnostic`/`CjdBinaryMatchDiagnostic`，反序列化产出诊断列表随结果上浮） | 见 Phase 4.3 |
| G18 | **AA 生成物同步遗漏**（五轮发现）：新增诊断（12 parse + 30 sema 改动 + 加载门）必须再生成 `analysis/analysis-api-cfir` 的诊断转换器三件套（`CaCfirDiagnostics.kt`/`CaCfirDiagnosticsImpl.kt`/`CaCfirDataClassConverters.kt`——首轮盘点已证实三者含 CJMP 诊断），漏生成则 AA 消费端编译失败或诊断静默丢失 | 首轮仓库盘点 grep 命中三文件 | 见 Phase 0/3 任务清单 |
| G19 | **测试模式注入路径缺失**（五轮发现）：§8.4 矩阵要求 mode=Common/Specific 正例 fixture，但 `CjmpSettingsComponent` 的值来自 CLI 编译模式，LLT 无法传 CLI 选项——没有注入路径则 Common/Specific 正例 fixture **无法落地** | `ModuleStructureExtractorImpl.kt:329-348`（模块指令 → 模块配置的唯一汇聚点）；Kotlin `// MODULE: platform()()(common)` 把模块角色编入指令语法的先例 | 见 Phase 5.1、§8.1 模式合法性矩阵 |
| G20 | **反序列化声明的诊断锚定**（六轮发现）：`NOT_MATCHED`（common 缺 specific 方向）、`COMMON_PACKAGE_HAS_MAIN` 等 sema 诊断需锚在 common 侧声明上——specific 编译时它是反序列化声明，**可能没有真实 source**。官方锚得住是因为 cjo 内嵌 AST 位置（ASTWriter 序列化 positions）；本仓库 cjo 是否携带位置未验证 | 官方 cjo 带 position（ASTWriter.cpp 反序列化可还原 source）；Kotlin 对位：期望侧诊断在 MPP 平台编译中锚于 common **源**（同一调用内全模块源 FIR 可用），本仓库 e2e 模型 common 是反序列化流——两者前提不同 | 见 Phase 4.4 锚定核实项 |
| G21 | **checker 实际覆盖面只有 class**（八轮发现，实锤）：`CfirCommonSpecificChecker.check()` 首行 `if (declaration !is CfirClass) return`（`CfirCommonSpecificChecker.kt:47`），而 `CfirStruct/CfirEnum/CfirInterface` 都是 `CfirClassLikeDeclaration` 直接子类、**非 CfirClass 子类**（gen 声明文件 :23）——struct/enum/interface 的 common/specific 整体被跳过；`CfirExtend`（CfirMemberDeclaration）与顶层 func/var/prop 不在 classLikeCheckers 注册组（`CommonDeclarationCheckers.kt:191`）——官方四类 nominal + extend + 顶层 callable 的匹配检查实际只覆盖 1/5。G3（路由不通）此前掩盖了这一点 | gen 层级证据 + 官方 `MatchCJMPDecls`（nominal + 非声明 `CJMPDeclMatchKey` 哈希 + extend 键匹配 + enum 构造器全覆盖） | 见 Phase 3.1 |

## 2. 架构对位矩阵（KMP 组件 → cjmp 复刻物）

| Kotlin K2 组件 | cjmp 复刻物（本仓库） | 落点模块 |
|---|---|---|
| fragment = module = session；`-Xfragments/-Xfragment-refines` | common 源集 = 一个 `CaModule`/`CfirSession`；specific 模块经 `directDependsOnDependencies`（= refinementDependencies）指向 common。**不做**官方 C++ 的同 Package 物理合并（见 D1） | 已有，补测试装配 |
| `FirModuleData.isCommon` | `CfirModuleData.isCommon`（已有） | — |
| `FirResolvePhase.EXPECT_ACTUAL_MATCHING` | 新增 `CfirResolvePhase.CJMP_MATCHING`（TYPES/STATUS 后、BODY_RESOLVE 前） | `cfir/cfir-tree/.../CfirResolvePhase.kt` + `cfir/resolve` |
| `FirExpectActualMatcherTransformer` + `Processor` | `CfirCjmpMatcherTransformer`：遍历 specific 声明 → 求配对 → 写回存储 | `cfir/resolve/.../transformers/` |
| `FirExpectActualResolver.findExpectForActual` | `CfirCjmpResolver.findCommonForSpecific`：**成员**在 containing class 的 common 对应物成员 scope 中找；**顶层**经 refinementDependencies 模块的包 scope 找，过滤 `isCommon && moduleData in transitiveDependsOn`（修 G3 的核心） | `cfir/resolve` |
| `AbstractExpectActualMatcher`（resolution.common，marker 符号 + Context SPI） | `AbstractCjmpMatcher` + `CjmpMatchingContext` 接口放 `resolution.common`（算法平台无关：结构匹配不含返回类型/默认值，对齐官方匹配阶段）；FIR 适配 Context 放 `cfir/resolve` | `resolution.common` + `cfir/resolve` |
| `FirDeclaration.expectForActual: Map<...>` | **单侧配对存储（六轮纠错）**：`CfirCjmpMappingStorage` 为 specific session 的作用域组件，映射只写在 **specific 侧**（specific 声明 → 匹配到的 common symbol；第二绑定事件、绑定失败分类同侧记录）；**绝不改写 common 侧声明**（官方 `specificImplementation` 指针 + `doNotExport` 写进 common AST 是单 Package 前提的产物，跨 session 写入违反本仓库风险 8；`doNotExport` 语义由候选过滤等价实现）。Kotlin 对位：`expectForActual` 也只写在 actual 侧 | `cfir/resolve`（session 组件）+ `cfir/cfir-tree`（specific 侧声明属性） |
| `AbstractExpectActualChecker` + `FirExpectActualDeclarationChecker`（消费 expectForActual） | 改造 `CfirCommonSpecificChecker`：只消费 transformer 写好的配对结果，不再 symbolProvider 现查（G3/G6）；拆匹配期诊断（NOT_MATCHED）与验证期诊断（类型/注解/修饰符）两阶段 | `cfir/checkers` |
| `MppCheckerKind.Common/Platform` | `CjmpCheckerContext.Common/Specific`：common 编译只跑 common 侧子集检查（open class 构造器、extension 私有成员冲突），不报"缺 specific"；specific 编译才做配对（对齐官方 compileCommon/compilePlatform 分流） | `cfir/checkers` |
| `FirActualizingScope`（抑制已被 actual 覆盖的 expect 候选） | 解析候选过滤：同一调用同时可见 common+specific 候选时剔除 common（对齐官方 `FilterOutCommonCandidatesIfSpecificExist`，挂 resolve 的候选过滤点） | `cfir/resolve` |
| type refinement（`ExpectActualUtils.kt` actualize，k2_kmp.md 专章） | 类型精化：specific 模式下对已配对 common 类型的引用在使用侧按 ClassId 重解析到 specific symbol；超类型替换对位官方 `GetInheritedTypesWithSpecificImpl`（G15） | `cfir/resolve`（Phase 2.5） |
| `FirResolvePhase` 被前端与 LL 共同消费 | `LLCfirCjmpMatchingTargetResolver` 挂 LL target resolver 链（G16） | `analysis/low-level-api-cfir`（Phase 2.6） |
| proto `isExpect` flags 位 | CJO 属性位 COMMON/FROM_COMMON_PART/SPECIFIC/COMMON_WITH_DEFAULT 写出（读取侧已有） | `cfir/cfir-serialization` |
| `IrActualizer`（后端合并） | 本期**不做**（无 CHIR 合并需求；common chir/cjo 产出与链接属后端范围，见 §6 非目标） | — |
| `ExpectActualTracker`（增量编译） | 本期不做（无增量编译消费方） | — |
| `// MODULE: name(deps)` 测试指令 | `analysis-tests` 增加 MODULE 指令 → `CaSourceModuleImpl` dependsOn 图 → 每模块独立 pipeline（基建已有 dependsOn，缺指令解析） | `cfir/analysis-tests`、`tests/test-infrastructure` |

## 3. 语义裁决点（官方 vs Kotlin vs 现实现）

| # | 裁决 | 依据 |
|---|---|---|
| D1 | **模块模型用 K2 式双模块（common/specific 各一 session，dependsOn 边），不用官方 C++ 的同 Package 物理合并**。官方合并是 C++ AST 基建的产物（`MergeCommonIntoSpecific` 搬成员进声明体）；K2 的"每源集一模块 + 符号沿 dependsOn 链查找"在本仓库已有 CaModule 骨架，改造成本远低于重造合并器。**配对语义等价**：官方合并的产物（specific 看得到 common 成员、common 默认值克隆到 specific）改由 transform/resolve 阶段的逻辑合并实现 | 架构从 Kotlin；行为对官方可观察面（诊断、重载决议、默认值可见性）逐条对齐 |
| D2 | **匹配阶段结构化：匹配不含返回类型与默认值**（官方 `MatchSpecificWithCommon` 注释明示）；返回类型协变、注解、修饰符放验证阶段（PostTypeCheck 对位 = checker）。现状 checker 在同一处查全部，需拆分 | 官方 CheckCJMP.cpp:1057-1058 两阶段注释 |
| D3 | **配对基数**：同一 common 声明至多绑定 1 个 specific，第二个 specific 绑定报 `MULTIPLE_COMMON_IMPLEMENTATIONS`（官方 `TrySetSpecificImpl`；无 Kotlin 的重载多配对/AMBIGUOUS_EXPECTS 语义）。**三轮纠错：不得把"1:1"硬编码进 matcher**——官方实现支持多 parent 源集链（`severalParents = commonPartCjos.size() > 1`，CheckCJMP.cpp:43；多 parent 重定义误报抑制 `IgnoreCJMPFalsePositiveRedefinition`，Diags.cpp:141-164）；模型采用 CaModule dependsOn DAG 天然支持链式 common，官方文档"两层"仅是 1.1 文档化用法，fixture 范围按两层、matcher 按 DAG 实现 | 官方 `TrySetSpecificImpl` + `CheckCJMP.cpp:43` + `Diags.cpp:141-164` |
| D4 | **返回类型：specific 是 common 的子型即可**（协变），现状 `equalTypes` 改 `AbstractTypeChecker.isSubtypeOf`；**函数返回类型不匹配复用既有诊断 `RETURN_TYPE_INCOMPATIBLE`**（三轮裁决：仓库已对齐官方 `sema_return_type_incompatible`，`CfirInheritanceDeepChecker` 在用，CfirDiagnosticsList.kt:987；D9 的悬置裁决就此关闭）；property/var 类型不匹配仍走 `SPECIFIC_HAS_DIFFERENT_TYPE` | 官方 `CheckMatchedFunctionReturnTypes`；仓库 `CfirDiagnosticsList.kt:968-992` |
| D5 | **命名参数：参数名必须相同、命名性必须相同**；现状只查个数 → 补名字比较 | 官方 `Parameters.cpp:101-110` |
| D6 | **泛型：形参按位置映射，重命名合法** → 删除 `COMMON_GENERIC_RENAME_NOT_SUPPORTED`（G9）；约束宽严比较新增：specific 上界须"更宽松或相同"（复用/抽仓颉泛型继承上界比较逻辑），违反报泛型继承族诊断而非 cjmp 专名 | 官方 `CheckGenericTypeBoundsMapped`（StructInheritanceChecker.cpp:1236-1290）；官方 `sema_common_generic_rename_not_supported` 无触发点 |
| D7 | **默认值：只允许一侧**（`CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES` 已有）；common 侧有默认值时对 specific 侧**读穿**（logical read-through）：specific 参数无默认值时，调用解析从配对 common 声明的参数读默认值——**不做物理表达式克隆**（官方 `CloneDefaultArgument` 是单 AST 内克隆，本仓库跨模块/session 克隆 CFIR 表达式树既不可行也无必要，D1 逻辑合并的必然推论）；注解传播（`@Attribute` 全量、`Deprecated` 单向继承 common→specific）同样读穿。**次序硬约束：匹配 → 注解传播 → 默认值传播 → 验证**（官方 CheckCJMP.cpp:1358-1372 注释：先传播后匹配会报误导性 `sema_not_matched`） | 官方 `Parameters.cpp:39-57,201-224` + `MatchSpecificWithCommon` 尾部传播段 |
| D8 | **common 带默认实现时 specific 可整体省略/覆盖**：`COMMON_WITH_DEFAULT` 语义按官方 parse 期判定（有体/有初始化即置，`SetCJMPAttrs` 对位）；**CFIR 侧不新增 status 位——在 STATUS resolve 阶段从 body/initializer 推导**（派生值，写侧序列化时才落位）；参与 `MustMatchWithPlatform` 等价判定；interface 成员豁免配对；编译 common 侧（非 specific 模式）不报"缺 specific" | 官方 `MustMatchWithPlatform` 六豁免 + `SetCJMPAttrs`（ParseCJMPDecl.cpp:57-65） |
| D9 | **诊断名对齐官方**：官方 `specific_has_different_kind` 无 `sema_` 前缀、`common_non_exaustive_platfrom_exaustive_mismatch` 官方拼写含 typo——本仓库按既有约定用规范化名（已登记的命名策略），**语义触发条件**逐条照官方；函数返回类型不匹配已裁决复用 `RETURN_TYPE_INCOMPATIBLE`（见 D4，三轮关闭） | 官方 DiagnosticSema.def:282-313 + 仓库 diagnostic 命名策略 |
| D10 | **门控双层**：版本门（`LanguageFeature.CommonSpecificDeclarations`，已有）继续作为下界；driver 落地 common/specific 编译模式选项后，补齐官方模式门（`parse_unexpected_cjmp_decl` 对位：非 common/specific 编译模式下使用修饰符报错）。官方真门是 cjpm 包布局，登记为后续项不变 | 官方 ParseCJMPDecl.cpp:107-131 + 既有决策记录 |
| D11 | **Kotlin 语义一律不带入**：无 typealias actualize、无 suspend/inline、无 @OptionalExpectation、无多后端平台轴、actual 不走序列化配对（specific 编译时重新匹配）、无变体发布。HMPP 任意层 DAG 修正（三轮）：dependsOn **链**被官方多 parent 事实支持（D3），但 Kotlin 的中间源集共享语义（default hierarchy template、源集子集共享）不带入——仓颉链上每层都是完整编译单元 | 技能非谈判条款 + 官方 `CheckCJMP.cpp:43` |
| D12 | **parser 保持配置无关 + `common`/`specific` 为硬关键字**（2026-09-23 二轮纠错：原案误选 Kotlin 式软关键字，违反 D11）。官方证据：`Tokens.inc:170-171` 用普通 `TOKEN` 宏（`Token.h:30`），与 `EXPERIMENTAL_TOKEN` 家族（perform/resume/throwing/handle，`Tokens.inc:11-12,172-175`，唯一有词法级实验开关的家族）明确区分——即官方 1.1 词法**无条件**把 `common`/`specific` 识别为关键字，不随 --experimental 或语言版本回退为标识符。本仓库同款：lexer 无条件产出 `COMMON_KEYWORD`/`SPECIFIC_KEYWORD`（配置无关，自动满足"PSI 树跨版本相同"硬验收）；代价 = 该二词不能再作标识符（`package common` 类用法被击穿，官方 1.1 同价），现有 fixture `cstAccessibleCommonParent.cj` 需改名（官方证据充分，Fixture Edit Gate 条款 1）。版本/模式**只决定报哪种错，不决定怎么解析** | 官方 `Token.h:30`/`Tokens.inc:11-12,170-175`；本仓库 `CangJieParser.kt:147-182` 零配置注入 |
| D13 | **12 条 parse 族诊断落为 CFIR 诊断，报告点 = 专用 checker**（四轮纠错：原案"raw-builder 报告"自相矛盾——raw-cfir 不依赖 checkers（`CjmpGate` 所在），且 raw 层没有进入 `CfirErrors` 管道的报告通道；Kotlin 的 `ConeSyntaxDiagnostic` 模式在本仓库缺转换基建，不宜为此新建）。落点：`CfirCjmpParseRulesChecker`（declarationCheckers + fileCheckers 双注册），**逐条在 CFIR 声明形状上可判定**：common/specific 函数缺返回类型、specific 参数带默认值、var/let 缺类型且无初始值、成员与外层修饰符不一致、static init 被修饰、泛型非法修饰、common class/struct 无显式构造、explicitly abstract 越界、模式声明非法、模式门（`parse_unexpected_cjmp_decl`）。诊断名保留官方 `parse_*` 前缀；版本门禁在 checker 层经 D16。**两个形状保真风险**：①pattern 声明（tuple/enum/wildcard）的 CFIR 保真度待核（可能被 erasure）；②显式 abstract 与推导 abstract 的区分待核（status.isAbstract 是否保留显式来源）——任一失真则该项降级为 parse 期 `PsiErrorElement`（IDE-only，编译诊断登记降级理由） | `AbstractLightTreeRawCfirBuilder.kt:42-45`（丢弃语法错误的现状证据）；仓库先例：缺口文档已记录 parse_cjmp_* 语义由语义层承接；cjd collector 模式 |
| D14 | **编译模式 = session 组件，非 CheckerContext 字段**：新增 `CjmpSettingsComponent`（模式枚举 None/Common/Specific），完全复刻 `CfirInteropSettingsComponent` 四段式样板（key → CfirAbstractSessionFactory 注册 → session 访问器 → checker 消费）；CLI 侧新增 `-Xcjmp-common-part(-chir)` 对位选项。模式判据对齐官方：Specific ⇔ common-part 输入非空，Common ⇔ CHIR 输出模式。**per-session 语义（五轮明确）**：多模块编译中每个模块 session 持有自己的组件值——源模块模式由 driver options 决定；测试模块由 `// MODULE` 角色语法决定（G19）；IDE 由模块 kind（`TargetPlatform.isCommon()` 等）决定。`parse_unexpected_cjmp_decl` 之外的 file-part 非法组合见 §8.1 矩阵 | `CfirFrontendConfigurationKeys.kt:186-190`、`CfirInteropSettings.kt:34-51`、`CfirGeneralSemanticsChecker.kt:280-283`；官方 Option.h:1195-1203 |
| D15 | **序列化加载侧补官方 6 层门**（对齐 `PreloadCommonPartOfPackage` 门序列）：cjo 格式版本门（缺版本即拒，防官方 v1.0.0 "写了不校验"的洞）、包名一致门、**features 子集门 `common ⊆ specific`**（官方 `feature_is_not_subset_of_child_set`）、编译选项匹配门；**属性位装填保持无版本判断**（官方同款：兼容性由 cjo 格式版本承担，不逐位门禁）；同时修 `:769-772` 重复赋值 | 官方 `ASTLoaderCJMP.cpp`、`ASTLoader.cpp:57-121`（`VersionAtLeast` 分支原语） |
| D16 | **门禁判定单一权威 + 一行式辅助**：不加诊断工厂级/Reporter 级中心过滤（Kotlin 明确无此层，接口注释钦定"加 LanguageFeature 枚举 + 调 supportsFeature"）；提供 `requireCjmpFeatureSupport` 式辅助（对位 Kotlin `FirHelpers.kt:1010 requireFeatureSupport`），把"版本门 → 模式门 →（未来）包布局门"的判定顺序收敛到一个入口，30+12 条诊断全部经它 | Kotlin `LanguageVersionSettings.kt:752-772` 注释、`FirHelpers.kt:1010-1025` |

## 4. 分阶段实施任务书

每阶段：改动 → 受影响模块构建 → 定向测试 → 全量 `:cfir:analysis-tests:test` 回归 → REPAIR_LOG 不适用（本计划非修复流），进度记入阶段验收表。

### Phase 0 — 词法/语法通路（G1+G11）
1. `psi`：`CjTokens` 新增 `COMMON_KEYWORD`/`SPECIFIC_KEYWORD`（**硬关键字**，对齐官方 `TOKEN` 宏语义——非 Kotlin 式软关键字，D12 纠错）；`CangJieLexer.flex` 加规则（无条件映射，无上下文回退）；`CangJieParsing.ModifierKind` 加 `COMMON`/`SPECIFIC`；修饰符冲突表对齐官方 `ParserModifierRules.cpp`（COMMON/SPECIFIC 互斥、与 PRIVATE 等冲突，按 class/struct/enum/interface/**extend**/toplevel 各表——九轮 C39 补 extend 表；局部声明（含 `CfirPatternVariable`）一律拒绝）。**parser 不做任何版本/模式判断**（D12）：语法接受性无条件。命名注意：`CangJieParsing.kt:2808 parseCommonDeclaration`/`:2258 parseClassCommonDeclaration` 是无关的"通用声明"分派函数，新增解析逻辑命名用 `parseCjmp*` 前缀避撞。
2. **parse 族诊断落为 CFIR 诊断**（G8+G11+D13；~~数量矛盾~~九轮 C38 修正：12 + §8.1 矩阵的 2 条 file-part = **14 条**，终数以 1.1.3 cjc 实测为准）：`CfirDiagnosticsList` 新增 `parse_*` 命名空间（12 条官方 `DiagnosticParser.def` 名单 + `parse_common_in_non_common_file` / `parse_specific_in_non_specific_file`）；报告点 = **专用 `CfirCjmpParseRulesChecker`**（declarationCheckers + fileCheckers 双注册，D13 四轮纠错——不是 raw-builder；file-part 两条由 fileCheckers 判定）；`CjmpGate` 版本门在 Phase 0 item 3 落地后于此 checker 首次消费；模式门（`parse_unexpected_cjmp_decl`）留桩（Phase 4 接线）。~~形状保真核实~~（八轮全关闭）：①pattern 声明保真无忧——`CfirPatternVariable` 保留 pattern（`PsiRawCfirBuilder.kt:1371-1401`）；②**显式 abstract 来源区分原生支持**——`CfirDeclarationStatusImpl` 已有 `isAbstractExplicit`（`Modifier.ABSTRACT_EXPLICIT` 位，:124-125，另有 isVisibilityExplicit/isModalityExplicit 同族），`parse_explicitly_abstract_only_for_cjmp_abstract_class` 与 `EXPLICITLY_ABSTRACT_*` 全部可 checker 判定，降级预案作废。
3. `cfir/checkers`：新增 D16 的 `CjmpGate` 门禁辅助（版本门单入口），Phase 0 先落版本门判定，模式门留桩（Phase 4 接线）。~~版本门诊断工厂复用核查~~（七轮定案）：interop 路径报的是**专用诊断**（`CJ_MAPPING_GENERIC_METHOD_NOT_GET_INSTANCE_CONFIG`，`CfirGeneralSemanticsChecker.kt:288-292`），仓库无通用"feature 不支持"诊断可复用——**新增专用版本门诊断**（建议名 `UNSUPPORTED_COMMON_SPECIFIC_LANGUAGE_VERSION`，对位 Kotlin `UNSUPPORTED_FEATURE`/`NOT_A_MULTIPLATFORM_COMPILATION` 双先例）。**约定修正（六轮）**：`LanguageFeature.CommonSpecificDeclarations` 现为 `CanStillBeDisabledForNow(NO_ISSUE_SPECIFIED)`，违反仓库约定"feature 条目必须带 issue 编号"——随本阶段补上决策引用。**AA 生成物同步（G18）**：`CfirDiagnosticsList` 改动后重跑 checkers-component-generator，并再生成 `analysis/analysis-api-cfir` 转换器三件套（`CaCfirDiagnostics.kt`/`CaCfirDiagnosticsImpl.kt`/`CaCfirDataClassConverters.kt`）。
4. 验收：`common`/`specific` 语法进 PSI 树（1.0.5 与 1.1 设置下 PSI 树结构**完全相同**，D12 验收标准）；v1.0.x 下报版本门诊断（对齐现有 fixture `commonSpecificSuppressedLangver105.cj` 改写为官方语义等价）。

### Phase 1 — 声明状态装填（G2）
1. `cfir/raw-cfir/raw-cfir-common`：`AbstractRawCfirBuilder.buildDeclarationStatus` 增 `isCommon`/`isSpecific` 参数并装填。**装填范围枚举（四轮补全）**：class / struct / enum / interface / **extend**（官方支持 extend 配对，`MergeCJMPExtensions`）/ 顶层 func / var / prop / **普通 init**；**主构造禁止 common/specific**（parse 规则，官方 ParseCJMPDecl）；enum 构造器随外层豁免（不独立装填）。~~前置核实~~（七轮关闭）：`CfirExtend` **有** status 字段（gen/`CfirExtend.kt:25`，CfirMemberDeclaration 子类）——extend 装填零模型改动；static init 载体 = **`CfirConstructor` + `status.isStatic`**（`CfirClassDeclaredMemberScope.kt:166-172` 明注"静态初始化器不是实例构造器"）——`parse_cjmp_static_init` 判定对象即此。
2. `psi2cfir` + `light-tree2cfir` 双路径接线（PSI 与 non-PSI 独立验证）；成员对容器 common/specific 的继承规则按官方 `parse_cjmp_outdecl_miss_match`：显式逐声明 + 一致性检查（Phase 0 的 `CfirCjmpParseRulesChecker` 报），不做容器传染（与 Kotlin expect 容器传染刻意不同，语义从仓颉）。
3. `CfirStatusResolveProcessor` 透传到 resolved status（现有搬运逻辑确认覆盖）；`COMMON_WITH_DEFAULT` 派生（D8）在此阶段一并落。
4. 验收：单测断言 `common class` 的 raw/resolved status.isCommon == true、`common extend` 装填成功；旧占位 fixture 中 `CLASSIFIER_REDECLARATION` 消失（改为分模块后不再同模块重声明）。

### Phase 2 — 配对引擎（G3+G6，核心阶段）
1. `cfir/cfir-tree`：`CfirResolvePhase` 增 `CJMP_MATCHING`；配对结果存储（架构矩阵"单侧配对存储"行：`CfirCjmpMappingStorage` = specific session 组件，只写 specific 侧，**不改写 common 侧声明**——六轮纠错）。
2. `resolution.common`：`CjmpMatchingContext` SPI（参数形状/名字/类型参数/修饰符/超类型/注解比较回调/`onMatched` 回写钩子）+ `AbstractCjmpMatcher`（结构匹配，D2/D3/D5）+ `AbstractCjmpChecker`（验证期兼容性枚举，对齐 Kotlin `ExpectActualIncompatibility` 模式）。
3. `cfir/resolve`：`CfirCjmpResolver`（顶层走 refinementDependencies 包 scope、成员走 containing class scope，过滤 `isCommon && in transitiveDependsOn`）+ `CfirCjmpMatcherTransformer`（挂 CJMP_MATCHING phase；内部次序对齐官方：**匹配 → 注解读穿 → 默认值读穿**，D7）；resolve 候选过滤（D1 的 `FilterOutCommonCandidatesIfSpecificExist` 对位，含**消费侧遮蔽**：下游模块 import cjmp 包时 specific 声明遮蔽 common 同名声明）。**门禁覆盖行为（六轮纠错）**：版本/模式门关闭时 transformer **早退**、候选过滤与类型精化**不激活**——否则版本门反例 fixture 的"42 条零报告"验收能过，但 specific-遮蔽-common 的解析行为在 1.0 语义下静默生效，属于行为级越权。**`MULTIPLE_COMMON_IMPLEMENTATIONS` 的检测点在 matcher**（四轮纠错：官方 `TrySetSpecificImpl` 是绑定动作，现 checker 的"同 session 查 same classId 数 specific"实现既结构死（G3）又语义错位——matcher 发现第二绑定写入 `CfirCjmpMappingStorage`，checker 消费存储报诊断，对齐 Kotlin expectForActual 消费模式）。
4. `analysis/low-level-api-cfir`：确认 LLCfir 会话链对 refinementDependencies 的符号路由（`LLModuleWithDependenciesSymbolProvider` 已有 dependencyProvider，需在 CJMP 查找路径上显式走依赖侧而非本模块优先）。
5. **类型精化（G15）**：specific 模式下对已配对 common 类型的引用解析到 specific 声明——超类型列表替换（官方 `GetInheritedTypesWithSpecificImpl` 对位）、成员/返回/extend 目标类型经同一候选过滤；实现形态对齐 Kotlin type refinement（类型在**使用侧**按 ClassId 重解析到 specific symbol，不做类型对象改写）。
6. **LL 管线入口（G16）**：`low-level-api-cfir` phase target resolver 链挂 `LLCfirCjmpMatchingTargetResolver`（复活 703027568 删除的 KDoc 承诺）；验证分桶从"PSI/non-PSI"扩为三桶：raw-PSI / raw-light-tree / LL。
7. 验收：不依赖 checker，transformer 单测断言 specific→common 配对成功/失败分类；specific 模块内 `specific class Repo` 能找到 dependsOn 模块的 `common class Repo`；specific 模块内 `class D <: Repo` 的超类型解析落在 specific `Repo`；LL 路径经 `// MODULE` fixture 拿到配对结果。

### Phase 3 — 检查器改造与语义补齐（G7+G9+G12+G21）
1. **覆盖面重构（G21，八轮实锤）**：`CfirCommonSpecificChecker` 从"仅 CfirClass"扩为官方全覆盖——①class-like 分发改判 `CfirClassLikeDeclaration`（struct/enum/interface 不再被首行 guard 跳过，各类型分支对齐官方 `MatchNominativeDecl` 的 kind 语义）；②新增**顶层 callable + extend 的注册与检查**（functionCheckers/variableCheckers/propertyCheckers/extend 分发，消费 `CfirCjmpMappingStorage` 的非声明配对结果，对齐官方 `CJMPDeclMatchKey`/`MatchCJMPEnumConstructor`/`MergeCJMPExtensions`）；整体重构为消费 Phase 2 配对结果 + 拆匹配期（`NOT_MATCHED`）与验证期（类型协变 D4、注解双向一一对应、修饰符集合一致性）两段 + Common/Specific 语境分流（D8）。
2. 补齐：泛型约束宽严比较（D6）、enum 构造器配对（官方 `MatchCJMPEnumConstructor`）、extend 按"扩展类型+接口集+泛型约束"键配对（官方 `MergeCJMPExtensions` 键）、`COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH` 接线（死诊断转活；~~前置核实~~（七轮关闭）：声明级 `...` 语法**已存在**——`parseEnumList` 处理非穷举枚举省略号（`CangJieParsing.kt:3137-3142`，`isEnumConstructorStart:3161` 接受 ELLIPSIS）；实现期仅剩确认 enum `...` entry 的 CFIR 表示与 light-tree 双路径）、interface 豁免（`MustMatchWithPlatform`）、**注解比较改多重集**（出现次数一一对应，替换现 `Set<AnnotationMatchKey>`，G7⑤）、`COMMON_WITH_DEFAULT` 派生（D8，STATUS 阶段 body/initializer 推导）。
3. **修门禁逃逸（G12）**：`CfirCommonPackageMainChecker`（`COMMON_PACKAGE_HAS_MAIN`）接入 D16 `CjmpGate`；两处既有入口门禁（`CfirCommonSpecificChecker:51`、`CfirCommonCtorImmutableAssignChecker:34`）改走同一辅助，消除散写。
4. 删除 `COMMON_GENERIC_RENAME_NOT_SUPPORTED`（D6，Fixture Edit Gate 条款 1：官方无触发点且按位置映射重命名合法）；同步 `CfirDiagnosticsList`/`CfirErrorsDefaultMessages`/`CfirNonSuppressibleErrorNames`/生成物 + **AA 转换器三件套再生成（G18，同 Phase 0 item 3）**。
5. 验收：30 条 sema 诊断逐条有 fixture 且与官方 `DiagnosticSema.def:282-313` 触发条件一致（逐条对照表随 PR）；`// LANGUAGE_VERSION: 1.0.5` 下全族 44 条诊断零报告（30 sema + 14 parse，C38 口径）。

### Phase 4 — driver 编译模式与序列化读写（G4+G5+G13+G14）
1. `compiler/arguments` + `cfir/entrypoint`：按 D14 四段式样板新增 `-Xcjmp-common-part`（路径列表）/`-Xcjmp-common-part-chir` 对位选项与 `CjmpSettingsComponent`（模式枚举 None/Common/Specific，判据对齐官方：Specific ⇔ chir 输入非空，Common ⇔ CHIR 输出模式）；两个选项数量配对校验（官方 `driver_require_common_chir_for_each_common_cjo` 对位）；`parse_unexpected_cjmp_decl` 接模式门（D10/D16 门禁序：版本门先于模式门）。
2. `cfir/cfir-serialization` 写侧：`CfirCjoPackageMetadataProducer` 写出 COMMON/FROM_COMMON_PART/SPECIFIC/COMMON_WITH_DEFAULT 位；common 侧写出重标注（specific → COMMON + COMMON_WITH_DEFAULT，对齐官方 ASTWriter.cpp:1656-1662）。
3. `cfir/cfir-serialization` 读侧（G13+D15+G17）：修 `:769-772` 重复赋值；补官方 6 层门中适用的四层——cjo 格式版本门（缺版本即拒）、包名一致门、features 子集门 `common ⊆ specific`（`feature_is_not_subset_of_child_set` 对位诊断）、编译选项匹配门；**属性位装填保持无版本判断**（D15，官方同款）。**加载门诊断通道（G17）**：按 cjd 家族 collector-list 先例（`CjdAnnotationConversionDiagnostic` 模式）——加载器产出 `CjmpLoadDiagnostic` 列表随反序列化结果上浮，由装配层外显（编译输出/LLT 断言）。features 指令集（`// FEATURES` 文件级指令）如本仓库尚无对应物，随门实现最小采集（文件级字符串集合），来源对齐官方 `File::GetFeatures()`。
4. 验收：common cjo 写出→读回→status 位往返一致；specific 模式加载 common cjo 后配对可用（最小 e2e；实现路径：common cjo 装载为**反序列化 library session**——`CfirDeserializedSymbolProvider` 已有，specific 源 session 的 dependsOn 指向之，matcher 沿该边查找，不再新建装载机制）；版本/包名/features/选项四门的正反例 fixture 各一。**锚定核实项（G20）**：验证本仓库 cjo 是否携带声明 source 位置——带则 common 侧诊断（`NOT_MATCHED` common 方向、`COMMON_PACKAGE_HAS_MAIN`）直接锚反序列化声明；不带则按仓库 Diagnostic Range Policy 精神定再锚策略（锚 specific 侧查找起点或包级），并登记与官方锚点差异。

### Phase 5 — 测试基建与 fixture 全量（G10）
1. `tests/test-infrastructure` + `cfir/analysis-tests`：`// MODULE: name[(deps)]` 指令解析（移植 Kotlin `ModuleStructureDirectives.kt` 模式）→ `CaSourceModuleImpl` dependsOn 图 → 每模块独立 pipeline；PSI 与 non-PSI 双套生成。**模块角色语法（G19）**：`// MODULE` 支持角色标注（如 `// MODULE: common` / `// MODULE: specific(common)`），`ModuleStructureExtractorImpl`（:329-348 汇聚点）据此为每模块 session 装配 `CjmpSettingsComponent`——无此路径则 §8.4 的 Common/Specific 正例与 §8.1 合法性矩阵 fixture 均无法落地。
2. 重写 `testData/diagnostics2/common-specific/` 10 个占位为真实多模块 fixture；按官方 30 条 sema + 12 条 parse 逐诊断建 fixture（含默认值读穿、泛型宽严、协变返回、enum/extend 配对、interface 豁免、common 侧编译子集、类型精化超类型替换）。
3. **门禁矩阵 fixture（§8.4）**：版本门 × 模式门 × 特性开关的三维正反例 + 行为级反例（§8.5.5）。~~版本钉扎理由修正（七轮）~~：实测 `LATEST_STABLE = CANGJIE_1_1_3`（`LanguageVersionSettings.kt:90`）——**默认版本门是开的**，C20 的"静默假阴性"方向反转；钉 `// LANGUAGE_VERSION: 1.1.0` 仍保留，但理由改为**显式性 + 未来 LATEST_STABLE 演进不改变 fixture 语义**。
4. 验收：`:cfir:analysis-tests:test` 全量回归 + 新增 cjmp 生成套件全绿；与官方 cjc 1.1.3 探针输出对照（`cangjie-official-testdata-inline-diagnostics` 流程抽查）。

## 5. 阶段依赖与并行性

```
Phase 0 ── Phase 1 ──┬─ Phase 2 ── Phase 3 ── Phase 5
                     │     ↑             ↑
                     │     └─ Phase 4.4 e2e（依赖 Phase 2 matcher）
                     └─ Phase 4.1-4.3（options/序列化读写/加载门；仅依赖 Phase 1 status 位，可与 Phase 2/3 并行）
```
Phase 0/1 是纯加法可先做；Phase 2 是唯一动 resolve 主干的高风险阶段，独立分支；Phase 3 依赖 Phase 2 存储结构定型；Phase 4.4（e2e）依赖 Phase 2 matcher 可用（四轮修正：原图"仅依赖 Phase 1"已过时）；Phase 4 序列化位序有官方契约（Attribute.kt:544-549），尽早做防止 cjo 往返回归。

## 6. 非目标（本期不做）

- CHIR/后端合并（官方 `IrActualizer`/`mergingSpecific` 对位）、平台动态库产出与链接期换库——后端范围另行立项。
- cjpm 包布局门控（官方真门，已登记 P0 待办）。
- 增量编译 `ExpectActualTracker` 对位、AA/IDE 导航 API（`getSingleMatchedCommonForSpecific`）。
- LSP 侧支持。
- HMPP 多层源集树、变体发布——仓颉 cjmp 无此语义（D11）。

## 7. 风险与已知坑

1. **G3 路由改动触及 LLModuleWithDependenciesSymbolProvider 查找顺序**——先查本模块是全局行为，不能全局反转；只在 CJMP 匹配查找路径上走依赖侧（新增专用 provider），避免破坏普通解析。
2. **PSI 与 non-PSI 双路径**必须独立验证（技能 PSI/non-PSI 分桶纪律）；Phase 1 与 Phase 5 生成器都要双套。
3. **官方 origin/main 比 1.1.3 新**（含 2026-09 perf 重构与 `Parameters.cpp`）；凡 origin/main 与 1.1.3 cjc 实测行为有出入处，以 cjc 1.1.3 实测为准并用探针固化。
4. **`CfirCommonSpecificChecker` 现有 11 处 symbolProvider 现查**全部要改为消费配对结果，改完前该 checker 在 LLT 上不可达（fixture 占位掩盖了这一点）——Phase 2 完成前不要试图"修 fixture"。
5. 死诊断/反官方诊断的删除走 Fixture Edit Gate：官方证据已在 §3 D6 给足，fixture 同步改，不作为"适配现实现"记档。
6. **硬关键字破坏面（D12 纠错引入）**：`common`/`specific` 成为无条件关键字后，作标识符/package 名的既有代码被击穿——已实测 `testData/diagnostics/coverage/accessibility/cstAccessibleCommonParent.cj` 的 `package common`（1 处）；实现 Phase 0 时须全仓 grep 复核（源码 + testData + 宏样例），命中处随 Phase 0 一并改名（官方 1.1 同价，Fixture Edit Gate 条款 1）。
7. **Phase 2 的可验证性边界**：driver 分步编译在 Phase 4 才落地，Phase 2 的配对引擎**只能**经 `analysis-test-framework` 的多模块装配（`CaSourceModuleImpl.directDependsOnDependencies`）驱动验证——不要试图在 Phase 2 前造 driver。
8. **读穿实现的 session 边界**：specific 模块解析时读穿 common 模块的默认值表达式/注解，跨 session 访问 CFIR 节点须走符号提供者路径（common 侧声明以 symbol 形式被引用），不得直接持有另一个 session 的 CFIR 树节点——否则破坏 LLFir 缓存/失效模型。
9. **跨模块类型同一性**（G15 的地基）：common `Repo` 与 specific `Repo` 分属不同 session，类型系统必须依赖 **ClassId 结构相等**判定同一性——CFIR 类型比较按 ClassId + 实参结构比较即天然成立；**禁止**跨 session 的成员 scope 级类型比较（成员归属检查一律经配对声明路由）。若后续发现类型比较依赖 symbol 同一性的路径，是 G15 精化实现的首要雷区。

## 8. 版本门禁控制设计（2026-09-23 深化补全）

### 8.1 三层门架构（语义从官方，机制从 Kotlin/仓库既有）

| 门 | 判定条件 | 对位诊断 | 落地层 |
|---|---|---|---|
| ① 版本门 | `languageVersion < 1.1.0`（`LanguageFeature.CommonSpecificDeclarations`，已有） | 1.0.x 语义 = 官方 `parse_expected_decl` 的等价（本仓库用 CFIR 诊断报"当前语言版本不支持 common/specific"） | checker/raw-builder（session 之后才有版本信息，G11/D13） |
| ② 模式门 | `CjmpSettingsComponent.mode == None` 且 ① 已过（版本 ≥ 1.1） | `parse_unexpected_cjmp_decl`（"to compile with common/specific declarations, correct compiler options must be specified"） | D14 session 组件 + D16 门禁辅助；Phase 4 接线 |
| ③ 加载门 | common cjo 加载期：cjoVersion / 包名 / features 子集 `common⊆specific` / 编译选项匹配 | 官方 `module_version_not_identical`、`module_common_cjo_wrong_package`、`feature_is_not_subset_of_child_set`、`module_common_cjo_{debug,opt}_mismatch` | 反序列化/加载层（Phase 4，D15），**不在属性位装填处做版本判断** |
| （未来④） | cjpm 包布局（common package part / specific package part） | 官方 `parse_common_in_non_common_file`（1.1.3 实测真门） | 已登记非目标/P0 待办；①②作为过渡下界不变 |

**判定顺序**：① → ② → 语义检查。① 不过时整族短路（不报 42 条，只报版本诊断）；① 过 ② 不过时报 `parse_unexpected_cjmp_decl`（对齐官方 ParseCJMPDecl.cpp:107-131 的分流：官方是"非 CHIR 输出且无 common-part-cjo 即报"）。

**②模式门的修饰符 × 模式合法性矩阵**（五轮补全；1.1.3 实测语义为准，origin/main 把三条 file-part 诊断重构为文件级标记——本仓库按 1.1.3 落）：

| 修饰符 \ 模式 | None | Common（编译 common part 源） | Specific（编译 specific part 源，已加载 common cjo） |
|---|---|---|---|
| `common` | 非法（`parse_unexpected_cjmp_decl`） | 合法 | **非法**（= 官方 1.1.3 实测 `parse_common_in_non_common_file` 语义：specific 源文件不写 common 声明，common 声明来自 cjo。注意 origin/main 已把此诊断重构为文件级标记——按风险 3 原则以 1.1.3 实测名为准） |
| `specific` | 非法（同上） | **非法**（对位 `parse_specific_in_non_specific_file`，同名重构注意） | 合法 |

注：多 parent 链场景（D3）中间层文件可同时含 common 声明——该场景由文件级 part 标记承载而非模式枚举，首版 fixture 不覆盖，登记为链式源集的后续项。

### 8.2 模式门的管道（D14 展开，四段式样板）

```
compiler/arguments   -Xcjmp-common-part <path>... / -Xcjmp-common-part-chir <path>...
        ↓ （数量配对校验，官方 driver_require_common_chir_for_each_common_cjo 对位）
CfirFrontendConfigurationKeys   val cjmpCommonPartPaths / cjmpCommonPartChirPaths
        ↓
CfirAbstractSessionFactory   mode = Specific(chir非空) / Common(CHIR输出模式) / None → registerCjmpSettingsComponent
        ↓
CfirSession.cjmpSettings（session 访问器，对齐 CfirSession.interopSettings）
        ↓
CjmpGate 门禁辅助（checkers）  ①版本门（languageVersionSettings.supportsFeature）→ ②模式门（session.cjmpSettings）
```

与 Kotlin 的完整对位：`-Xmulti-platform` → `@Enables(MultiPlatformProjects)` → `specificFeatures` → `FirExpectActualDeclarationChecker` 报 `NOT_A_MULTIPLATFORM_COMPILATION`。区别：Kotlin 把 MPP 开关做成 feature；仓颉官方把 cjmp 开关做成**选项组 + 包布局**（feature 指令与 cjmp 无代码交集，取证 5.2），因此本仓库用"session 组件承载模式 + LanguageFeature 承载版本下界"的双轴，而非单 feature。

### 8.3 加载门（D15 展开，官方 6 层门取舍）

官方 `PreloadCommonPartOfPackage` 门序列与本仓库取舍：

| 官方门 | 本仓库 | 理由 |
|---|---|---|
| flatbuffer Verifier | 已有（flatbuffers-gen 读侧） | — |
| cjoVersion（major 相等 ∧ minor ≤ 当前 ∧ 缺失即拒） | **补**（Phase 4.3）；本仓库 CJO 需先登记格式版本字段（若无，最小形态：读侧 `VersionAtLeast` 分支原语 + 版本缺失拒绝） | 官方 v1.0.0 教训：写版本不校验 = 无防御（"缺版本也拒"防静默消费） |
| 包名一致 | **补**（诊断名 `module_common_cjo_wrong_package` 对位） | 防跨包错配 |
| features 子集 `common ⊆ specific` | **补**；前置：文件级 features 指令集最小采集（`// FEATURES` → 文件属性 → cjo FileInfo.feature 对位字段） | 官方 ASTLoaderCJMP.cpp:66-101；注解传播/误报抑制（`IgnoreCJMPFalsePositiveRedefinition`）依赖它 |
| 编译选项匹配（debug/优化级别） | **补**（common cjo 内嵌选项对位字段随写侧一并加） | 官方注释：选项不一致会导致 desugar/CHIR 差异在后续阶段崩溃 |
| 属性位无版本门 | **保持**（D15） | 官方同款：兼容性由格式版本承担，逐位门禁反而不对 |

### 8.4 门禁测试矩阵（Phase 5.3 的 fixture 维度）

测试基建零新增成本：`// LANGUAGE_VERSION` 与 `// LANGUAGE: ±CommonSpecificDeclarations` 已汇入同一 `LanguageVersionSettingsImpl`（`LanguageVersionSettingsBuilder.kt:76-116`），`parseLanguageFeature` 按枚举名匹配，无需登记。

| 维度 | 设置 | 期望 |
|---|---|---|
| 版本门正例 | `LANGUAGE_VERSION: 1.1.0` + 模式开 | 全语义可用 |
| 版本门反例 | `LANGUAGE_VERSION: 1.0.5` | 仅版本诊断，42 条族诊断零报告（现有 `commonSpecificSuppressedLangver105.cj` 扩展为此形态） |
| 特性覆盖反例 | `LANGUAGE_VERSION: 1.1.0` + `LANGUAGE: -CommonSpecificDeclarations` | 同版本门反例（specificFeatures 覆盖生效） |
| 模式门反例 | 1.1.0 + mode=None | `parse_unexpected_cjmp_decl` 对位诊断，无配对/匹配诊断 |
| 模式门反例（part 错位 ×2，§8.1 矩阵） | 1.1.0 + mode=Common 内写 `specific`；1.1.0 + mode=Specific 源内写 `common` | 各报 file-part 非法诊断（1.1.3 名） |
| 模式门正例（Common 侧） | 1.1.0 + mode=Common | common 侧子集检查跑，不报"缺 specific"（D8/MustMatchWithPlatform） |
| 模式门正例（Specific 侧） | 1.1.0 + mode=Specific + common-part 输入 | 配对+验证全量跑 |
| 加载门反例 ×4 | cjoVersion 不符 / 包名不符 / features 非子集 / 选项不符 | 各报对位加载诊断（Phase 4 验收，经 G17 通道断言） |

### 8.5 门禁实现纪律（D16 展开）

1. **不做中心化诊断过滤**：Kotlin 证据明确无工厂级/Reporter 级版本过滤（`DiagnosticFactory` 无 feature 元数据），接口注释钦定"加 LanguageFeature 枚举 + 调 supportsFeature"；本仓库若造 Reporter 级过滤层属违反 D11 的架构越权。
2. **门禁辅助单入口**：`cfir/checkers` 新增 `CjmpGate.requireCjmpSupport(context, source)`（版本门→模式门有序判定，返回判定结果供调用方决定报哪种门诊断）；两处既有散写（`:51`、`:34`）与 G12 逃逸点全部收编。
3. **IDE 一致性**：`LLCfirAbstractSessionFactory.wrapLanguageVersionSettings`（:589-603）已有 feature 包装点，门禁语义经 session 传递在 IDE 侧天然生效，无需独立 IDE 逻辑。
4. **PSI 树版本无关性是硬验收**（D12）：任何"让 lexer/parser 感知版本"的实现直接拒绝——它同时破坏 IDE 高亮（IDE 无编译配置上下文）与 LLT 双版本对照能力。注意区分：官方词法级实验开关（`EXPERIMENTAL_TOKEN` 家族，perform/resume 等）是**官方对实验语法的既有机制**，`common`/`specific` 不属于该家族（普通 `TOKEN`），因此本条对 cjmp 无例外。
5. **门禁管行为不止管诊断**（六轮补全）：CJMP_MATCHING transformer、resolve 候选过滤、类型精化、读穿传播——所有**行为改变点**都必须在门开启时才激活（Phase 2.3 早退要求）；只看"诊断零报告"的版本门验收不充分，须配一条行为级反例 fixture（1.0.5 下 specific 与 common 同名声明各自独立解析、互不遮蔽）。

## 9. 二轮深化纠错记录（2026-09-23）

| # | 原案 | 纠错 | 证据 |
|---|---|---|---|
| C1 | `common`/`specific` 按 Kotlin 式**软关键字**实现（softKeywordModifier 模式 + 标识符回退） | **硬关键字**（无条件 TOKEN，无回退）；软关键字选择违反 D11（把 Kotlin 语义带入了官方没有的地方） | 官方 `Token.h:30`（TOKEN=常规枚举）、`Tokens.inc:11-12`（EXPERIMENTAL_TOKEN 默认展开为 TOKEN，该家族才有词法级实验开关）、`:170-171`（COMMON/SPECIFIC 用普通 TOKEN） |
| C2 | G7 记 4 处 checker 语义偏差 | 增至 **5 处**：⑤注解比较现用 `Set`，官方按出现次数**多重集**一一对应 | `CfirCommonSpecificChecker.kt:759-777` vs 官方 `CheckCJMPAnnotations.cpp:150-196` |
| C3 | "默认值克隆表达式到 specific 侧" | 改为**读穿**（read-through）：跨模块/session 物理克隆 CFIR 表达式树不可行也无必要；官方克隆是单 AST 前提下的产物；注解传播同理 | 官方 `Parameters.cpp:39-57` 单 AST 语境；D1 逻辑合并推论 |
| C4 | 传播动作无次序约束 | **次序硬约束：匹配 → 注解传播 → 默认值传播 → 验证**（先传播后匹配会报误导性 `sema_not_matched`） | 官方 `CheckCJMP.cpp:1358-1372` 注释 |
| C5 | `COMMON_WITH_DEFAULT` 暗示新增 status 位 | **派生值**：STATUS 阶段从 body/initializer 推导，不新增源侧位；仅写侧序列化落位 | 官方 `SetCJMPAttrs`（ParseCJMPDecl.cpp:57-65）判定条件 |
| C6 | 硬关键字破坏面未评估 | 实测击穿 `package common`（`cstAccessibleCommonParent.cj`，1 处）+ 全仓 grep 复核义务入 Phase 0 | 本轮 testData grep |
| C7 | Phase 2 验证方式未说明 | 明确：Phase 4 前只能经 analysis-test-framework 多模块装配验证，不许提前造 driver | G4 driver 缺位事实 |

### 9.1 三轮深化补全记录（2026-09-23）

| # | 原案缺陷 | 补全/裁决 | 证据 |
|---|---|---|---|
| C8 | D3 "1 common : 1 specific" 硬编码进架构 | matcher 按 **DAG** 实现：官方支持多 parent 源集链（`severalParents`）；`MULTIPLE_COMMON_IMPLEMENTATIONS` 语义澄清 = 同一 common 被第二个 **specific** 绑定；文档两层只是 1.1 文档化用法，fixture 限两层、模型不限 | 官方 `CheckCJMP.cpp:43`、`Diags.cpp:141-164` |
| C9 | **类型精化整块缺失**（G15）：方案只有声明遮蔽，没有"对 common 类型的引用解析到 specific 声明" | 新增 Phase 2.5：超类型替换（`GetInheritedTypesWithSpecificImpl` 对位）+ 使用侧 ClassId 重解析，形态对齐 Kotlin type refinement | 官方 `CheckCJMP.cpp:1446-1458`；Kotlin `k2_kmp.md` type refinement 专章 |
| C10 | **LL 分析管线无配对入口**（G16）：不补则 LLT LL 路径与 IDE 永远看不到配对结果 | 新增 Phase 2.6：`LLCfirCjmpMatchingTargetResolver`（历史上被删 KDoc 的兑现）；验证分桶扩为 raw-PSI / raw-light-tree / LL 三桶 | 703027568 删除的 KDoc；Kotlin `FirResolvePhase` 前端/LL 共用 |
| C11 | D4/D9 悬置："函数返回类型不匹配报哪个诊断" | **关闭**：复用既有 `RETURN_TYPE_INCOMPATIBLE`（仓库已对齐官方 `sema_return_type_incompatible`）；`SPECIFIC_HAS_DIFFERENT_TYPE` 仅 property/var | `CfirDiagnosticsList.kt:968-992` |
| C12 | 死诊断接线无前置依赖检查 | 新增核实项：声明级 `...` 非穷尽语法支持状态（`ELLIPSIS` 仅验证到表达式层）；缺则补语法或降级登记 | `CangJieExpressionParsing.kt:3920` |
| C13 | Phase 4 e2e 装载机制未指明 | common cjo = 反序列化 library session（`CfirDeserializedSymbolProvider` 已有），specific 源 session dependsOn 指向之 | 仓库既有反序列化 session 基建 |
| C14 | D11 与多 parent 事实矛盾（"仓颉只有两层"） | D11 修正：dependsOn 链被官方支持；不带入的是 Kotlin 中间源集共享语义（default hierarchy template） | 官方 `CheckCJMP.cpp:43` |

### 9.2 四轮深化纠错记录（2026-09-23）

| # | 原案缺陷 | 补全/裁决 | 证据 |
|---|---|---|---|
| C15 | D13 "parse 族诊断从 raw-builder 报告"自相矛盾：`CjmpGate` 在 checkers，raw-cfir 不依赖 checkers（依赖箭头反向），且 raw 层无 `CfirErrors` 报告通道；Kotlin `ConeSyntaxDiagnostic` 模式缺转换基建 | 报告点改为**专用 `CfirCjmpParseRulesChecker`**（declaration+file 双注册），12 条逐条在 CFIR 声明形状上判定；两个形状保真风险（pattern 声明 CFIR 保真度、显式 abstract 来源）带降级预案 | 模块依赖方向；仓库先例：缺口文档已记录 parse_cjmp_* 语义由语义层承接 |
| C16 | §5 依赖图过时：Phase 4 的 e2e 验收（四轮前为三轮 C13 补入）依赖 Phase 2 matcher，原图却写"Phase 4 仅依赖 Phase 1" | 依赖图拆分：Phase 4.1-4.3 与 Phase 2/3 并行，Phase 4.4 e2e 依赖 Phase 2 | Phase 4.4 验收条款 |
| C17 | `MULTIPLE_COMMON_IMPLEMENTATIONS` 检测点错位：现 checker 在 classLike 检查里"同 session 查 same classId 数 specific"（结构死 + 语义错位） | 检测点 = matcher 绑定动作（官方 `TrySetSpecificImpl` 对位），发现第二绑定写入存储；checker 消费存储报诊断 | 官方 `CheckCJMP.cpp:1024-1026`；Kotlin expectForActual 消费模式 |
| C18 | Phase 1 装填范围未枚举 | 明确：class/struct/enum/interface/**extend**/顶层 func/var/prop/普通 init；主构造禁止；enum 构造器随外层豁免；前置核实 `CfirExtend.status` 与 static init CFIR 载体 | 官方 `MergeCJMPExtensions`、ParseCJMPDecl |
| C19 | G13 加载门诊断的报告通道未约定（发生在 DiagnosticReporter 可用之前） | 新增 G17 + Phase 4.3：cjd collector-list 先例（`CjdAnnotationConversionDiagnostic` 模式），`CjmpLoadDiagnostic` 随反序列化结果上浮 | `CfirCjoPackageMetadataProducer.kt:378-385` 同款消费先例、cjd 家族 collector |
| C20 | fixture 默认版本风险：默认 `LANGUAGE_VERSION = LATEST_STABLE`（`BaseDiagnosticConfiguration.kt:174-175`），若 LATEST_STABLE < 1.1.0，不钉版本的 fixture 静默走版本门反例（假阴性） | 所有 cjmp fixture 显式钉 `// LANGUAGE_VERSION: 1.1.0` 入 Phase 5.3 | `BaseDiagnosticConfiguration.kt:174-175` |

### 9.3 五轮深化补全记录（2026-09-23）

| # | 原案缺陷 | 补全/裁决 | 证据 |
|---|---|---|---|
| C21 | 模式门语义只有"None 非法"一条，Common/Specific 内的修饰符错位未定义 | §8.1 新增**修饰符 × 模式合法性矩阵**：mode=Common 内 `specific` 非法、mode=Specific 源内 `common` 非法（= 官方 1.1.3 实测 `parse_common_in_non_common_file` 语义）；origin/main 已重构该诊断为文件级标记，按风险 3 原则以 1.1.3 名为准 | 1.1.3 实测（既往会话取证）；origin/main file-part 标记重构（首轮取证 §2.2 尾注） |
| C22 | **AA 生成物同步遗漏**（G18）：`CfirDiagnosticsList` 增删诊断必须再生成 `analysis-api-cfir` 转换器三件套，方案的任务清单全程未提 | Phase 0 item 3 / Phase 3 item 4 补录再生成步骤 | 首轮盘点 grep 证实 `CaCfirDiagnostics*.kt`/`CaCfirDataClassConverters.kt` 含 CJMP 诊断 |
| C23 | **测试模式注入路径缺失**（G19）：mode 来自 CLI 选项，LLT 传不进去——§8.4 的 Common/Specific 正例与 §8.1 矩阵 fixture 全部落不了地 | Phase 5.1 扩展 `// MODULE` 角色语法（`// MODULE: common` / `// MODULE: specific(common)`），`ModuleStructureExtractorImpl` 据此装配 per-module `CjmpSettingsComponent` | `ModuleStructureExtractorImpl.kt:329-348`；Kotlin `// MODULE: platform()()(common)` 角色语法先例 |
| C24 | D14 模式的 per-session 语义未明确（多模块编译中各模块模式从哪来） | D14 补 per-session 规则：源模块由 driver options、测试模块由 `// MODULE` 角色、IDE 由模块 kind（`TargetPlatform.isCommon()`） | K2 每源集一 session 模型 |

### 9.4 六轮深化纠错记录（2026-09-23）

| # | 原案缺陷 | 补全/裁决 | 证据 |
|---|---|---|---|
| C25 | **配对存储写入方向错误**：方案让 common 侧声明挂 `specificImplementation` 指针——LL 模型下即写入**另一个 session 的声明**，违反自家风险 8 | **单侧存储**：`CfirCjmpMappingStorage` = specific session 组件，只写 specific 侧；`doNotExport` 语义由候选过滤等价实现；Kotlin `expectForActual` 同样只写 actual 侧 | 官方单 Package 前提（`CheckCJMP.cpp:220-296` 直接改 common AST）；Kotlin `FirExpectActualMatcherTransformer` 写 actual 侧 |
| C26 | **门禁只管诊断、没管行为**：门关闭时 matcher/候选过滤/类型精化仍会运行，specific-遮蔽-common 的解析行为在 1.0 语义下静默激活（"42 条零报告"验收会假性通过） | 所有**行为改变点**（transformer/候选过滤/类型精化/读穿）在门关闭时早退；§8.4 增补一条**行为级反例 fixture**（1.0.5 下同名声明互不遮蔽） | 门禁语义推论；Phase 2.3/§8.5.5 已落 |
| C27 | 版本门诊断工厂未指定；`CommonSpecificDeclarations` 条目 `NO_ISSUE_SPECIFIED` 违反仓库"feature 必须带 issue 编号"约定 | 版本门诊断优先复用 interop 路径既有"feature 不支持"诊断（先查 `CfirGeneralSemanticsChecker:280-283` 所报工厂名）；约定修正入 Phase 0.3 | `CfirGeneralSemanticsChecker.kt:280-283`；项目约定 |
| C28 | **反序列化声明的诊断锚定**未验证（G20）：common 侧诊断在 specific 编译时锚于反序列化声明，source 可能为空 | Phase 4.4 增锚定核实项：cjo 带位置 → 直接锚；不带 → 再锚策略（specific 侧查找起点或包级）并登记与官方差异 | 官方 cjo 内嵌 position；Kotlin 平台编译中 common 是源 FIR（前提不同） |

### 9.5 七轮核实项闭合记录（2026-09-23）

本轮将方案累积的 6 个"实现期核实项"直接查证闭合，方案事实面收敛：

| # | 悬置项 | 查证结果 | 证据 |
|---|---|---|---|
| C29 | C20 的 LATEST_STABLE 假阴性风险 | **方向反转**：`LATEST_STABLE = CANGJIE_1_1_3`——默认版本门是开的，不钉版本的 fixture 默认走正例；钉 1.1.0 保留但理由改为显式性与演进免疫 | `LanguageVersionSettings.kt:90` |
| C30 | `CfirExtend` 是否有 status | **有**（CfirMemberDeclaration 子类）——extend 装填零模型改动 | gen/`CfirExtend.kt:25` |
| C31 | static init 的 CFIR 载体 | `CfirConstructor` + `status.isStatic`；`parse_cjmp_static_init` 判定对象即此 | `CfirClassDeclaredMemberScope.kt:166-172` |
| C32 | 声明级 `...` 语法（C12 前置） | **已存在**：`parseEnumList` 处理非穷举枚举省略号；实现期仅剩 enum `...` 的 CFIR 表示与 light-tree 双路径确认 | `CangJieParsing.kt:3137-3142,3161` |
| C33 | pattern 声明 CFIR 保真（D13 风险①） | **保真无忧**：`CfirPatternVariable` 保留 pattern（`convertCasePattern`），`parse_cjmp_pattern_decl` 可 checker 判定 | `PsiRawCfirBuilder.kt:1371-1401` |
| C34 | 版本门诊断工厂复用（C27 前半） | **无可复用**：interop 报专用诊断，无通用 feature-unsupported——新增专用版本门诊断（建议 `UNSUPPORTED_COMMON_SPECIFIC_LANGUAGE_VERSION`） | `CfirGeneralSemanticsChecker.kt:288-292` |

**剩余唯一悬置**：显式 abstract 与推导 abstract 的来源区分（D13 风险②）——需读 status resolve 实现确认，属 Phase 0 实现期第一件事。

### 9.6 八轮发现与闭合记录（2026-09-23）

| # | 内容 | 证据 |
|---|---|---|
| C35 | **D13 风险②关闭（降级预案作废）**：显式 abstract 来源区分仓库**原生支持**——`CfirDeclarationStatusImpl` 已有 `isAbstractExplicit`（`Modifier.ABSTRACT_EXPLICIT` 位，:124-125，同族 isVisibilityExplicit/isModalityExplicit） | `CfirDeclarationStatusImpl.kt:103-125` |
| C36 | **G21 实锤：checker 实际覆盖面只有 class**——`CfirCommonSpecificChecker.check()` 首行 `!is CfirClass` guard 跳过 struct/enum/interface（三者是 CfirClassLikeDeclaration 直接子类，gen :23）；`CfirExtend` 与顶层 callable 不在注册组（`CommonDeclarationCheckers.kt:191`）。官方匹配面 = 四类 nominal + extend + 顶层 callable + enum 构造器，实际只覆盖 1/5。G3（路由不通）+ fixture 占位双重掩盖了它。Phase 3.1 已扩为覆盖面重构任务 | `CfirCommonSpecificChecker.kt:47`；gen 类层级；`CommonDeclarationCheckers.kt:191` |
| C37 | 清理项：`CfirDeclarationModeCheckersTest.kt:186` 手工 `(status as CfirDeclarationStatusImpl).isCommon = true` 是全仓库唯一置位点——Phase 1 落地真实装填后该测试须改为走正常路径（保留为 status 透传断言） | 首轮盘点 §2 |

## 10. 实施进度（2026-09-24）

### Phase 0 — 词法/语法通路与门禁基建（落地）

改动（全部已编译通过）：

| 层 | 文件 | 内容 |
|---|---|---|
| lexer/tokens | `psi/.../lexer/CjTokens.java` | `COMMON_KEYWORD(221)`/`SPECIFIC_KEYWORD(222)`（`keywordModifier` 硬关键字，对位官方 `Tokens.inc:170-171` 普通 TOKEN）；入 `MODIFIER_KEYWORDS_ARRAY`（尾部追加）与 `KEYWORDS` |
| lexer | `psi/.../lexer/CangJieLexer.flex` | `"common"`/`"specific"` 两条无条件规则（无上下文回退） |
| parser | `psi/.../parsing/CangJieParsing.kt` | `ModifierKind.COMMON/SPECIFIC` + `ModifierDetector` 装填；`atPackageNameSegment()` 让 import 包名段容忍关键字（对齐官方 `ExpectPackageIdentWithPos`；`std.unittest.common` 实测） |
| stub | `psi/.../psi/stubs/CangJieStubVersions.kt` | `SOURCE_STUB_VERSION` 209 → 210（修饰符掩码 +2 位） |
| 诊断 | `cfir/checkers/checkers-component-generator/.../CfirDiagnosticsList.kt` | COMMON_SPECIFIC 组新增 14 条 `PARSE_*`（12 条官方 `DiagnosticParser.def:266-277` 名单 + 2 条 file-part） |
| 消息 | `cfir/checkers/.../CfirErrorsDefaultMessages.kt` | 14 条默认消息（官方原文 + cjc 1.1.3 实测文案） |
| 门禁 | `cfir/checkers/.../analysis/checkers/CjmpGate.kt`（新） | 版本门单入口；**复用既有 `CfirFeatureSupport.requireFeatureSupport` + `CfirErrors.UNSUPPORTED_FEATURE`**——§9.5 C34"无通用诊断可复用"的结论已不成立（该基建随后落地），与 Kotlin `UNSUPPORTED_FEATURE` 先例一致，故不新增专用版本门诊断。模式门 `modeGatePasses` 为 Phase 4 桩 |
| 检查器 | `cfir/checkers/.../checkers/declaration/CfirCjmpParseRulesChecker.kt`（新） | 解析族规则（D13 报告点）；9 条实装 + 3 条无触发点登记保留 + 2 条 file-part 留 Phase 4 |
| 修饰符表 | `cfir/checkers/.../checkers/ModifierCheckerTargets.kt` | `cjmpModifierTargetPredicate()`：版本门关时放行一切（整族短路）；开时按官方作用域表收口（局部/值参数/类型参数/typealias/macro/访问器拒绝；`common static init` 放行交族诊断） |
| 冲突表 | `cfir/checkers/.../Compatibility.kt` + `ModifiersCompatibilityUtils.kt` | COMMON/SPECIFIC/PRIVATE 两两互斥（官方各表 `CR(COMMON, PRIVATE, SPECIFIC)`）；cjmp token 组合在版本门关时短路 |

### Phase 1 — 声明状态装填（落地）

| 层 | 文件 | 内容 |
|---|---|---|
| raw 公共 | `cfir/raw-cfir/raw-cfir-common/.../AbstractRawCfirBuilder.kt` | `buildDeclarationStatus` 增 `isCommon/isSpecific` 并装填 |
| PSI 路径 | `cfir/raw-cfir/psi2cfir/.../PsiRawCfirBuilder.kt` | `convertDeclarationStatus` 接线（`hasModifier(COMMON/SPECIFIC)`） |
| LightTree 路径 | `cfir/raw-cfir/light-tree2cfir/.../LightTreeModifierList.kt` | `isCommon/isSpecific` + `toDeclarationStatus` 装填 |
| 透传 | `cfir/resolve/.../CfirStatusResolveProcessor.kt` | 已天然透传（`:809-810,1141-1142` 既有 `isCommon/isSpecific` 搬运），零改动 |

`COMMON_WITH_DEFAULT` 未新增 status 位（D8：派生值，写侧序列化时才落位）。

### 官方 1.1.3 探针结论（cjc 全流程实测，替代原计划"终数待实测"）

- 全流程命令：common `cjc common.cj --experimental --output-type=chir`（不带 `-o` 才产出 `<pkg>.chir`+`<pkg>.cjo`）；specific `cjc specific.cj <pkg>.chir --experimental --common-part-cjo=<pkg>.cjo -o out.dll --output-type=dylib`（`=` 形式必需）。
- **12 条名单中 3 条无触发点**（登记保留、不激活）：`parse_specific_function_parameter_cannot_have_default_value`（specific 单侧默认值合法，双方默认值走既有 sema `CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES`）、`parse_expected_type_with_cjmp_var`、`parse_cjmp_generic_decl`（泛型限制走 `COMMON_GENERIC_FROZEN_NOT_SUPPORTED`，锚 `@Frozen` 行）。
- file-part 双方向实测：specific 源内 `common` → `common declaration must be defined in common package part`；common 源内 `specific` → `specific declaration must be defined in specific package part`（均锚 package 行 1:1）。
- 其它实测：`common class/struct` 无显式构造 → `at least one constructor is required in common class/struct '...'`（锚声明名）；成员与外层不一致 → `function f is common, but class is not common`（锚成员声明）；`common static init()` 单条（锚整条）；模式声明 → `tuple pattern can not be 'common'`（锚模式）；specific 接口成员无体 → `the member m must have body in 'specific' I2`（锚成员声明）；`abstract func` 在非 CJMP 抽象类 → explicitly-abstract 族（锚成员声明）+ 通用 illegal-modifier 双报。
- 锚点登记差异：官方"函数缺返回类型"锚 `func` 关键字（4:15/5:12），本仓库暂用声明名；官方无体顶层函数另报 `body of function missing`，本仓库无该诊断（fixture 不期望）。

### 验收状态

- 编译：`:psi` / `:cfir:checkers` / `:cfir:raw-cfir:{raw-cfir-common,psi2cfir,light-tree2cfir}` / `:cfir:resolve` 全绿。
- `:psi:test`：74 通过 / 1 失败（`ForeignAndAnnotationParsingTest.platformAnnotationSurfaceIsRetainedWithoutBuiltinKindFacade`，归因并行会话的注解 builtin 重构，与 cjmp 无关）。
- 生成物：`CfirErrors.kt` / `CfirNonSuppressibleErrorNames.kt` 已重生成（14 条 `PARSE_*` 入列）；AA 转换器三件套待 `:analysis:analysis-api-cfir:generateDiagnostics` 重生成。
- fixture：`commonSpecificSuppressedLangver105.cj` 改写为版本门期望；5 个占位 fixture 按新行为补 `PARSE_*` 期望；新增 `commonSpecificParseRules{Common,Specific}01.cj`；`cstAccessibleCommonParent.cj` 的 `package common` 改名 `commonpkg`。
- 全量 `:cfir:analysis-tests:test` 回归：待跑（Phase 5 前）。

### Phase 3 — 检查器改造与语义补齐（第 1 批落地并验证，2026-09-24）

已落地并经队列验证（`QP3F/QP3G`：双生成器重跑 + 四模块编译 + 全诊断切片 1224 用例 **全绿**）：

| 项 | 内容 |
|---|---|
| 覆盖面（G21） | `CfirCommonSpecificChecker.check()` 首行 `!is CfirClass` 守卫移除——class/struct/interface/enum 全量参与（typealias 排除），原仅 class 的 1/5 覆盖修复 |
| 存储消费（G3/G6/C17） | `checkSpecificMatchesCommon`/`checkSpecificExtraConstraints` 一律消费 `CfirCjmpMappingStorage`（`commonFor`/`isUnmatched`/`mismatchKindsFor`/`specificBindingsFor`），11 处 symbolProvider 现查全部移除；`MULTIPLE_COMMON_IMPLEMENTATIONS` 从 common 侧 symbolProvider 现查迁移到 specific 侧消费第二绑定（C17） |
| 门禁（G12/D16） | `CjmpGate` 增补非上报型 `isEnabled`；两处散写门禁（`CfirCommonCtorImmutableAssignChecker:34`、`CfirCommonPackageMainChecker`）收编单入口；`CfirCommonSpecificChecker` 同步改走 `CjmpGate.isEnabled` |
| 官方判据修正 | `CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS` 按官方 `CheckAbstractClassMembers`（CheckCJMP.cpp:1310-1338）重写：**specific ∧ abstract ∧ 仅 CLASS_DECL**、成员限 func/prop、跳过 common 成员（原实现方向相反且覆盖到 interface，为反官方误报） |
| 参数形对齐 | `NOT_MATCHED` 参数签名为官方形 `(side, "Kind 'name'", counterpartSide)`（CheckCJMP.cpp:156），消息渲染同步 |
| 死诊断转活（C32） | `COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH` 接线：common 穷尽（无 `...`）而 specific 非穷尽即报（`CfirEnum.isNonExhaustive` 双路径已装填：psi2cfir:908 / light-tree:526） |
| 反官方诊断删除 | `COMMON_GENERIC_RENAME_NOT_SUPPORTED` 全链删除（D6/Fixture Edit Gate：官方无触发点、按位置映射重命名合法），生成物与 AA 三件套同步重生成 |
| explicitly abstract | sema 变体（`EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS`）已无 reporter（官方无触发点），parse 变体统一负责；sema 条目登记为后续清理项 |

**第 2 批（同日，均经队列验证：编译 + 全诊断切片 1224 用例全绿）**：
- `NOT_MATCHED` 参数形对齐官方 `DiagNotMatchedDecl`（CheckCJMP.cpp:156）：`(side, "Kind 'name'", counterpartSide)`，消息渲染器同步
  （生成器清单 + 消息映射 + 两处调用点 + AA 三件套重生成）。
- **非穷尽枚举接线（C32 闭合）**：`COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH` 死诊断转活——common 穷尽而
  specific 非穷尽即报；`CfirEnum.isNonExhaustive` 双路径已装填（psi2cfir:908 / light-tree:526，Ellipsis 源）。
- **注解多重集一一对应（G7⑤）**：`annotationKeys` Set→List 保留出现次数；`annotationKeyMultisetsEqual`（元素 + 计数同时相等）；
  官方 `IsSpecialHandledAnnotation`（CheckCJMPAnnotations.cpp:198-203）对位排除集落地（BuiltIn 14 项 + Platform 6 项，
  @Deprecated 由 `checkMemberDeprecatedInherited` 单独负责）——修复"同一注解出现多次时 Set 语义掩盖计数差异"的偏差。

**第 3 批（同日，经队列验证：编译 + 全诊断切片全绿）**：
- **接口成员豁免（MustMatchWithPlatform 第 5 条）**：common 接口成员允许无 specific 对应成员——
  `checkMemberMatch` 对 `commonDecl is CfirInterface` 整体跳过 NOT_MATCHED/MUST_HAVE_IMPLEMENTATION 对
  （官方 CheckCJMP.cpp:822-825；其余豁免项中，"有默认实现"由 body 非空判据等价、var 成员按官方调用点天然后置、编译 common 模式豁免属 Phase 4 模式门）。

**Phase 3 收口状态**：核心批次（覆盖面/存储消费/门禁/官方判据修正/参数形/死诊断转活与删除/注解多重集/接口豁免）全部落地并经
「编译 + `*CfirAnalysisDiagnostics*` 全族切片 1224 用例」验证。剩余登记项：泛型约束宽严比较（D6）、enum 构造器配对（官方按具体声明配对，
本仓库 C18 下 enum 构造器不携带 cjmp，需 Phase 4/5 有载体后激活）、extend 键配对与 `SPECIFIC_HAS_DUPLICATE_EXTENSIONS` 键细化
（官方 `MergeCJMPExtensions` 键 = 扩展类型 + `<:` 继承集 + 泛型约束；现实现仅按扩展类型短名）、`COMMON_WITH_DEFAULT` 派生（D8，STATUS 阶段）、
sema `EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS` 死条目清理。Phase 4（driver 模式与序列化读写）与 Phase 5（多模块 fixture 全量）为下一阶段。

**后续批次（Phase 3 余项，已登记）**：泛型约束宽严比较（D6，复用 `AbstractTypeChecker.isSubtypeOf`）、enum 构造器配对、
extend 键配对、interface 成员豁免（`MustMatchWithPlatform`）、注解多重集一一对应（G7⑤，官方 CheckCJMPAnnotations 计数判据）、
`COMMON_WITH_DEFAULT` 派生（D8）、30 条 sema 诊断逐条 fixture（Phase 5 联动）。

### Phase 2 — 配对引擎（代码落地；编译/测试验证排队中）

| 项 | 落点 | 内容 |
|---|---|---|
| 阶段 | `cfir/cfir-tree/.../CfirResolvePhase.kt` | 新增 `CJMP_MATCHING`（`IMPLICIT_TYPES` 后、`BODY_RESOLVE` 前，对齐 Kotlin `EXPECT_ACTUAL_MATCHING` 位置） |
| 处理器 | `cfir/resolve/.../transformers/CfirCjmpMatchingProcessor.kt`（新） | `CfirTransformerBasedResolveProcessor(CJMP_MATCHING)`：版本门关闭时 `processFile` 整阶段早退；`CfirCjmpMatcherTransformer` 遍历 class/struct/interface/enum/namedFunction/constructor/property 钩子 |
| 存储 | `cfir/resolve/.../cjmp/CfirCjmpMappingStorage.kt`（新） | specific session 组件（单侧写，C25）：`bind`（第二绑定返回 false，供 `MULTIPLE_COMMON_IMPLEMENTATIONS`）、`recordMismatch`/`recordUnmatched`；session factory 的 source/library 两处逐 session 注册新实例 |
| 候选查找 | `cfir/resolve/.../cjmp/CfirCjmpResolver.kt`（新） | 一律经 `session.dependenciesSymbolProvider`（G3：不命中本模块）；过滤 `isCommon && moduleData ∈ allRefinementDependencies`（DAG，D3）；模块同一性按 `name`（LLCfirModuleData 每次重建实例） |
| 结构匹配 | `cfir/resolve/.../cjmp/CfirCjmpMatcher.kt`（新） | 对齐 `AbstractExpectActualMatcher` 匹配阶段面：参数个数/类型参数个数/形参类型按位置映射后 ClassId+实参结构相等（风险 9）/命名性与命名参数名（D5）；**不含**返回类型与默认值（D2）；class-like 只按 ClassId 配对（种类差异属验证期） |
| 执行核心 | `cfir/resolve/.../cjmp/CfirCjmpMatchRunner.kt`（新） | 前端与 LL 共用的“对声明配对并写存储”；已配对容器的成员逐名配对（enum 构造器/static init 豁免） |
| 消费侧遮蔽 | `cfir/resolve/.../body/CfirCallResolver.kt` | `reduceCjmpShadowedCommonCandidates`（D1 的 `FilterOutCommonCandidatesIfSpecificExist` 对位）：common 候选若已有 specific 绑定则剔除；版本门关闭或存储空时不激活 |
| LL 入口（G16） | `analysis/low-level-api-cfir/.../transformers/LLCfirCjmpMatchingLazyResolver.kt`（新）+ `LLCfirLazyPhaseResolverByPhase` 注册 | 声明级锁内配对 + 相位推进；外层 class-like 先配对；版本门关闭时不写存储不推相位 |

**Phase 2.5（类型精化）模型分析结论（待编译验证后定稿）**：本仓库“本模块 provider 优先”的查找顺序
（`LLModuleWithDependenciesSymbolProvider.getClassLikeSymbolByClassId`）在 specific 模块对同名类型
（超类型列表、成员类型、返回类型、extend 目标）的引用上**天然解析到 specific 声明**；跨 session
同一性由 ClassId 结构比较承担（风险 9）。剩余精化面 = common cjo 反序列化进入 specific 会话后
的缓存类型引用，随 Phase 4 e2e 落地时以实测裁定，不在此阶段盲改。

**验证状态（2026-09-24）**：
- 编译：`:cfir:resolve` / `:cfir:entrypoint` / `:analysis:low-level-api-cfir` / `:analysis:analysis-api-cfir` 全部 BUILD SUCCESSFUL
  （过程中修复 4 处：`ConeTypeParameterType` 包名、`LLFlightRecorder` 相位编号表补 `CJMP_MATCHING -> 17`、
  `resolvePhase`/`replaceResolvePhase` 扩展导入、`collectCandidates` KDoc 归属）。
- 回归（全部经 gradle 队列串行执行，结果如下）：
  - `:cfir:analysis-tests:test --tests "*CfirAnalysisDiagnostics2*"`：**通过**（Phase 2 相位/处理器进入管线后既有 diagnostics2 行为零变化）。
  - `:cfir:analysis-tests:test --tests "*CfirAnalysisDiagnostics*"`（诊断全族切片）：**通过，0 失败**。
  - 两个覆盖 fixture（`coverage/declaration-status/{memberStatusCheckersRich,staticIncompatibleModifiersRich}.cj`
    补 `PARSE_EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS` 期望）定向复跑：**通过**。
  - `:psi:test`：75/1——唯一失败为既有并行会话注解重构问题（`platformAnnotationSurfaceIsRetainedWithoutBuiltinKindFacade`），
    本阶段新增的 `testCommonSpecificModifiersParseWithoutErrors` **通过**。
  - `:cfir:resolve:test`：129/7——与仓库既有基线逐数一致（基础设施型失败，无新增）。
  - 全量 `:cfir:analysis-tests:test --max-workers=2`：**限时尝试后主动停止**（运行 1.5h，仅完成 123 个测试类；
    观测到的失败仅为上述两个覆盖 fixture（已修复并复跑通过），无其它新增；剩余面由切片验收覆盖）。
- 生成物：AA 转换器三件套 14/14 已含 parse 族（CamelCase 命名 `Parse*`）；`CfirNonSuppressibleErrorNames` 14 条入列。

### Phase 4 — driver 编译模式与序列化读写（落地，2026-09-24）

| 项 | 落点 | 内容 |
|---|---|---|
| 模式组件（D14） | `cfir-common/.../CfirCjmpSettings.kt` + `CfirFrontendConfigurationKeys` + `createCfirCjmpSettingsComponent` | `CfirCjmpMode` None/Common/Specific，判据对齐官方 `IsCompilingCJMP*`；测试指令 `// CJMP_MODE` / `// CJMP_FEATURES` |
| CLI（4.1） | `compilerArguments.kt` + `AbstractFrontendPipeline.configureCjmpCommonPartInputs` | `-Xcjmp-common-part` / `-Xcjmp-common-part-chir`；数量不配对报官方 `driver_require_common_chir_for_each_common_cjo` 文案并终止 |
| 模块装配 | `CfirFrontendPipelinePhase.buildSessions` + `CfirSessionFactoryContextUtils` | common part cjo 作为 **depends-on 依赖模块**（精确路径过滤），其目录并入库搜索路径；`CfirDeserializedSymbolProvider` 按 `ModuleDataProvider.getModuleData(path)` 给声明挂 depends-on 模块数据（配对查找 `allRefinementDependencies` 可识别）；`MultipleModuleDataProvider` 精确过滤器先于 `TakeAll` 判定 |
| schema | `ModuleFormat.fbs` | `FeatureId/FeaturesSet/FeaturesDirective`、`OptimizationLevel`、`CompilationOptions`、`Package.options`——与官方 origin/main `schema/CjoFormat.fbs` **字节布局一致**（纠正先前自拟的 `StringId`/`Option` 表） |
| 写侧（4.2） | `CfirCjoPackageMetadataProducer` / `CjoPackageWriter` | COMMON/SPECIFIC/FROM_COMMON_PART/COMMON_WITH_DEFAULT 位；mode=COMMON 时选 `OFFICIAL_CJMP` 档，写 `options` 与 `FileInfo.feature`；修 FlatBuffers 嵌套构造 bug |
| 读侧（4.3） | `CjmpCommonPartLoadGate` / `CjoManager` / `CfirDeserializedSymbolProvider` | 四门：版本（缺版本即拒）/包名 → 拒装载；features 子集（**逐文件**，官方 `ValidateCommonSpecificFeatureSetsRelations`）/选项（缺失 = WARNING，不一致 = ERROR）；文案对齐官方 `DiagnosticModule.def` |
| G17 通道 | `CfirCjmpLoadDiagnosticsComponent` | 每次编译一个实例（工厂上下文创建，库/源码会话同挂），无共享可变默认值；拒装载原因也上浮；`CfirFrontendPipelinePhase.reportCjmpLoadDiagnostics` 外显，错误级使阶段失败 |

验证：`:compiler:frontend` / `:cfir:entrypoint` / `:cfir:cfir-serialization` / `:analysis:low-level-api-cfir` / test fixtures 编译全绿；`CjmpCommonPartCjoTest` 全过；版本门引发的 3 个既有测试夹具（`CjoFullIdResolverTest`、`CjdBinaryFixture`、`CjdDeclarationLoaderIntegrationTest`）补写 `cjoVersion`（官方加载同样拒绝无版本 cjo）。
剩余 `:cfir:cfir-serialization:test` 4 失败 + decompiler-to-stubs 1 失败：测试 session 未注册 `CfirLanguageSettingsComponent`（`publishInteropInfo`/`publishAnnotationInfo` 需要），归因并行注解/interop 重构，与 cjmp 无关。
未完成：4.4 e2e（common cjo 写出→specific 源编译配对）与 G20 锚定核实；CHIR 输出模式（Common 判据）无 driver 对位选项，Common 暂仅经测试指令注入。

### Phase 4.4 + Phase 5 — e2e 与多模块 fixture、语义对齐 cjc 1.1.3（2026-09-25）

**语义权威修正**：官方 `v1.1.3` tag 源码（`external/cangjie_compiler` git tag）与 cjc 1.1.3 实测为准（风险 3）。
与 origin/main 的关键差异已按 1.1.3 落地：返回类型不兼容 = 不配对（`MatchCJMPFunction` 内 `IsFuncDeclSubType`：参数类型
逐个相同 + 返回类型协变），不报 `RETURN_TYPE_INCOMPATIBLE`（D4 修正）；模式门 mode=None 报文件级
`parse_common_in_non_common_file` / `parse_specific_in_non_specific_file`（1.1.3 无 `parse_unexpected_cjmp_decl` 触发点）。

| 项 | 落点 | 内容 |
|---|---|---|
| 多模块基建（G19） | `CfirModuleInfoProvider.getDependentDependsOnSourceModules`、`CfirFrontendFacade`（per-module `CjmpSettings`）、`CfirDiagnosticCollectorService` | `// MODULE: specific()()(common)` + 模块级 `// CJMP_MODE`；specific 会话对 common 文件的跨文件诊断归属 common 文件（Kotlin `MppCheckerKind.Platform` 对位） |
| provider 组合 | `CfirAbstractSessionFactory.computeDependencyProviderList` / extend provider | 依赖 provider 与 extend provider 纳入 `allRefinementDependencies`（Kotlin `computeDependencyProviderList` 对位） |
| 配对（1.1.3 移植） | `CfirCjmpMatcher` / `CfirCjmpMatchRunner` / `CfirCjmpResolver` | nominal 种类判定（`MatchNominativeDecl`）、`MatchCJMPFunction`（泛型数、参数类型相同、返回协变、命名参数、双侧默认值）、`MatchCJMPVar/Prop`、模式变量、enum 构造器（`MatchCJMPEnumConstructor`，非穷尽静默）、extend（扩展类型 + 接口集键）、第二绑定（`TrySetSpecificImpl`）、`NeedToReportMissingBody`；只有标记 specific 的成员参与，成员仅经已配对容器配对 |
| 检查器 | `CfirCjmpMatchingChecker`（specific 方向）、`CfirCjmpCommonSideChecker`（common 方向）、`CfirCjmpCommonSideFacts`（共享判据） | NOT_MATCHED 双向 / 参数级诊断 / 种类 / 修饰符（`MatchCJMPDeclAttrs`）/ 变量属性类型 / var-let / 注解 / `@Deprecated` / 泛型约束（复用 override 路径 `GENERIC_CONSTRAINT_NOT_LOOSER`）/ `MULTIPLE_COMMON_IMPLEMENTATIONS`（锚 common） |
| 读穿（D7） | `CfirValueParameter.cjmpHasDefaultValue` + `CfirMapArguments`/`CfirCallResolver` | specific 函数形参按配对 common 形参默认值判定可省略实参 |
| 类型精化（G15） | `CfirCompositeSymbolProvider.withoutCjmpShadowedCommon` | 同 ClassId 同时可见 common/specific 时 specific 遮蔽 common（门禁开启时） |
| 冲突豁免 | `CfirConflictsHelpers.isCommonAndSpecific`、`CfirExtendExtraChecker.isCjmpCounterpartOf` | common/specific 对应物不报重声明/extend 遮蔽（Kotlin `isExpectAndActual`；1.1.3 实测两个 specific 之间仍冲突）；1.0.x 不豁免 |
| 隐式 abstract | `PsiRawCfirBuilder` / `LightTreeRawCfirDeclarationBuilder` | 1.1.3 `CanBeAbstract`：class 体内 common 成员不隐式 abstract；cjmp abstract class 中未显式 abstract 的函数不是抽象成员 |
| G20 锚定 | `CfirDeclDeserializer.recordCjoDeclarationPosition`、`CjmpDeserializedCommonSideReporter` | cjo 声明位置（1 基，写侧修正 0 基→1 基，官方 cjo 实测 `3:1`）恢复为 `cjoDeclarationPosition`；CLI specific 编译以编译消息 `'common' … can not find 'specific' match` + `common.cj:行:列` 外显；common part cjo 写源文件完整路径（官方 serializingCommon） |
| CLI e2e（4.4） | `compiler/frontend/test/.../CjmpTwoPhaseCompilationTest` | common 编译写 cjo（属性位 + options）→ specific 编译加载配对；未配对 specific NOT_MATCHED；common 方向带位置报告 |

fixture（全部经 cjc 1.1.3 两段式探针取证，`.workbuddy/tmp/cjmp_probe113/probe_results7*.txt`）：
`diagnostics2/common-specific/e2e/`（27 个两模块 fixture）+ `gate/`（mode=None 两方向、特性覆盖反例）；原 6 个单文件占位
（`commonSpecific*Placeholder.cj`）由同主题两模块 fixture 替换。

登记差异：`GENERIC_CONSTRAINT_NOT_LOOSER` 锚约束条目（仓库 override 路径既有约定），官方起点为 `where`；
反序列化 common 的 enum 构造器位置仅在其带 COMMON/FROM_COMMON_PART 位时记录。
