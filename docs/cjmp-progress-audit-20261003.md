# CJMP 当前源码重新审计（2026-10-03）

## 口径与结果

审计对象是 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie` 的当前未提交源码，
分支 `codex/cjmp-framework-implementation`，HEAD `fcfeede719a7349c5c095a6d3b34da27722c18a0`。
本次重新读取主计划 §3、§9 和修正附录，并核对当前生产源码、测试源码、XML 与已有官方探针记录。
主计划中“已提交”的状态描述是问题发现时的状态，不能作为问题已修复的证据。

严格问题闭环率：**8 / 70 = 11.43%**。闭环项为 B1、F1、F2、F3、F6、G2、G7、I6。
其余 **62 项未闭环**；已有局部实现也不折算成半个完成项。这是可验收问题的完成率，
不估算已经投入的工时，也不使用历史总体百分比。P0 取证和 P10 最终收口另列为门禁，均未全部完成；
因此此数字不能解释为 P0–P10 已整体验收。

| 问题组 | 闭环 / 总数 |
|---|---:|
| A | 0 / 3 |
| B | 1 / 1 |
| C | 0 / 5 |
| D | 0 / 9 |
| E | 0 / 14 |
| F | 4 / 8 |
| G | 2 / 9 |
| H | 0 / 14 |
| I | 1 / 6 |
| J | 0 / 1 |

## 逐项审计

以下文件名按本工作树中的同名源文件定位。每个未闭环项至少列出一处当前残留或缺失的验收要求；
不因相邻问题的测试转绿而连带计为完成。

| ID | 当前结论 | 当前源码／验证依据与剩余要求 |
|---|---|---|
| A1 | 未闭环 | 阶段已前移，Quest 适配已有测试；但 MatchRunner 的变量/property/extend 路径仍在配对调用内 `resolveCjmpSignatureTypes()`，后置验证未收口到共享 checker。 |
| A2 | 未闭环 | getter 已推进 CJMP，caller-first 诊断测试通过；`CjmpAttributes.getMatchedCommonForSpecific()` 仍读 session 前向表，LL 家族存在 ThreadLocal 重入和活存储快照，声明属性与完整读取协议未替换。 |
| A3 | 未闭环 | MatchRunner 的 callable/property 入口仍枚举同名 specific 组、重新建立 plans 并向组成员写结果；LL 目标解析仍准备兄弟声明，未实现单目标独立发布。 |
| B1 | 闭环 | `CfirDeclDeserializer.buildStatus()` 仅从 COMMON 恢复角色，来源单独写 `isFromCommonPart`；共享 `isCjmpMatchingCandidate()` 仅排除 ImplicitConstructor。两段式 helper/unmarked-g 测试检查角色、来源、messages、completed，现有 19 项 XML 全绿。 |
| C1 | 未闭环 | scope provider 直接创建 ClassUseSiteMemberScope，未接入统一 merged declared scope；生产没有调用 ContainerMergeEvaluator 建 nominal/extend 图，实例字段仍走直连绑定。 |
| C2 | 未闭环 | GenericInstantiationChecker 仍有检查器内 common 投影和 `isCjmpShadowedCommonDeclaration` 过滤，未改成消费唯一合并视图。 |
| C3 | 未闭环 | CommonSpecificChecker 的 `checkUnmarkedDirectMemberOverloadConflicts()` 仍存在，按字符串签名去重、配对对照并报告；未在合并视图完成组级重定义。 |
| C4 | 未闭环 | TowerLevelHandler 已包装 ActualizingScope，正向过滤已实现；getter 仍消费旧前向表，继承前的 raw/actualized 成员图和缓存边界尚未统一，旧 common-shadow 过滤仍供其他消费者读取。 |
| C5 | 未闭环 | `cjmpHasDefaultValue()` 仍存在，MapArguments 消费它；没有统一新入口覆盖构造委托、递归构造及 CJO，失败候选已转移默认前缀未生产接线。 |
| D1 | 未闭环 | GenericInstantiationChecker:1166 仍从 `ownerExtend.declarations` 补 `ownerDirectSignatures`，未由 owner 声明级 scope 统一提供。 |
| D2 | 未闭环 | 同文件:1148 的 `containsUnfixedTypeParameterOrVariable()` 早退仍在，补签名仍在；部分泛型实例的正确检查尚未闭合。 |
| D3 | 未闭环 | ExtendExtraChecker 的 `isIndependentInterfaceDefault()` 仍以 `(implementsInterface || currentMemberIsPrivate)` 作豁免。 |
| D4 | 未闭环 | CJO 成员仍在实例化成员签名收集中按 Source/SubstitutionOverride 筛掉；官方要求的二进制 owner 歧义与 note 尚不能按统一图验收。 |
| D5 | 未闭环 | ClassUseSiteMemberScope 仍有 `isImplementedByLocalFunction()`；GenericInstantiationChecker 仍传 `memberOwnerExtend`，未统一在替换前建立实现关系。 |
| D6 | 未闭环 | 内建目标仍用 `collectInstantiatedExtendMemberSignatures()` 扁平收集，`memberAccessContext` 来自 use-site，再按 checkCallable 过滤。 |
| D7 | 未闭环 | 跨包 peer 顺序检查仍在 GenericInstantiationChecker 的 owner 实例化路径，未迁到进口 extend 的统一复查点。 |
| D8 | 未闭环 | `instantiatedRelatedMemberExtends()` 在没有 ownerAccessContext 时仍使用 `samePackage || peer.status.visibility.isPublicAPI`。 |
| D9 | 未闭环 | GenericInstantiationChecker 仍传 `includedRootExtends`、`memberOwnerExtend` 并直接构造 scope 实现。 |
| E1 | 未闭环 | `checkCJMPAbstractClassMembers()` 仍只遍历 NamedFunction，依赖 body/status 启发式；未统一函数和属性及全部来源豁免。 |
| E2 | 未闭环 | `checkMatched()` nominal 分支提前 return；其余声明仍只找 unmatched specific annotation，没有双向一一匹配与版本分类表。 |
| E3 | 未闭环 | `checkMatched()` nominal 超类型仍只比较 `superTypeRefs.size`，没有映射后的逐类型比较。 |
| E4 | 未闭环 | 修饰符目标尚未完成官方按位置/容器及主、次构造分表；共享 checker 也未落地。 |
| E5 | 未闭环 | `CfirCommonCtorImmutableAssignChecker` 仍注册于 CommonDeclarationCheckers，未统一到赋值和自增自减的合法性入口。 |
| E6 | 未闭环 | ParseRulesChecker 的 `hasDefaultImplementation()` 仍只看 body/accessor/initializer，没有复用含 SPECIFIC 豁免的 HasDefault。 |
| E7 | 未闭环 | BuiltInAnnotationSemanticsChecker 仍对 common/specific 跳过通用注解诊断；CommonSpecificChecker 保留独立禁止集，版本门未收口。 |
| E8 | 未闭环 | `checkMatched()` 泛型约束调用仍只传 specific/common；没有把 family 外层映射交给共享约束比较。 |
| E9 | 未闭环 | common extend 判重仍用目标类型 `toString()`、private 成员 `groupBy { it.name }`。 |
| E10 | 未闭环 | `checkCommonDeclarationConstraints()`、class 级 OPEN_ABSTRACT 报告及 `checkSpecificExtraConstraints()` 的重复 extend 循环仍存在。 |
| E11 | 未闭环 | property MUT 仍被 `checkModifiers()` 的总 differs 归入 SPECIFIC_HAS_DIFFERENT_MODIFIER，未按官方独立诊断和顺序处理。 |
| E12 | 未闭环 | resolution.common 中没有 AbstractCjmpChecker；common 方向仍由 CfirFileChecker 自行遍历依赖文件，未建立 collector 的 checker-kind 分层。 |
| E13 | 未闭环 | LightTree builder 仍有 `isFunction && ... outer.isAbstract ...` 判据，属性和 PSI/STATUS 尚未共享同一规则。 |
| E14 | 未闭环 | file part、门禁、诊断参数未统一；CfirCjmpCommonSideFacts 仍为具体 object 并含呈现逻辑。 |
| F1 | 闭环 | `CfirCjmpMatcher.typesEquivalent()` 先按真实参数身份替换，再调用 AbstractTypeChecker.equalTypes；enum 的函数类型和 extend 的目标/接口/约束复用该入口，没有原结构比较及 == 兜底。共享及 CFIR CJMP 测试通过。 |
| F2 | 闭环 | 原 binding 进入共享 variable first-fit；tuple 按官方直接元素停止边界，root 保存模式结果，checker 只访问实际执行绑定。eager pattern 8/8、LL pattern 7/7，完整 pattern/header 快照无回归。此项闭环不代表 A2/A3/F8 发布协议完成。 |
| F3 | 闭环 | enum 构造项与普通 callable 进入同一组计划及占用程序，nullary/payload 使用真实 native kind，非穷尽判据覆盖其他 enum 成员；LL 后置检查没有 enum 单独跳过。CFIR enum plan 15/15，LL enum 3/3、mixed enum 9/9。 |
| F4 | 未闭环 | moduleData 身份判等已修且 2 项测试通过；依赖仍按传递集合处理，单 common/multi-parent 的版本门与 first-wave 尚未实现。 |
| F5 | 未闭环 | AbstractCjmpMatcher.matchEligibility 仍将 nominal 参数数量差异直接判 NOMINAL_TYPE_PARAMETER_COUNT；附录要求区分 generic/non-generic 的合并条件与数量差异的后置诊断。 |
| F6 | 闭环 | Resolver:567 的 `getContainingCjmpDeclaration()` 为唯一结构 owner 入口；成员候选只取该 owner 的已配对 counterparts。旧 matching processor 的 ThreadLocal 外围上下文和按同 ClassId 恢复成员候选路径已删除。LL 家族重入 ThreadLocal 属另一项未闭环风险。 |
| F7 | 未闭环 | 占用先于 MissingBody 已进入共享求值器；但 context 的 callableKind/classLikeKind 仍返回 String，Matcher 有 `else -> "class"` 等兜底，种类枚举要求未完成。 |
| F8 | 未闭环 | CfirCjmpMappingStorage 仍是公开可读写的 open class，持有前向/反向/映射多个可变表；未拆为要求的读写接口和声明属性发布。 |
| G1 | 未闭环 | LL 生产 session 工厂没有从 CaModule 自动注册角色；AbstractCfirCjmpMatchingTest 仍按模块名注册模式，并按 Box/Choice/shared 等名字分支，未改成统一指令及 golden。 |
| G2 | 闭环 | LLCfirCjmpMatchingLazyResolver 仅门控 transform，通用解析流程推进相位；没有 bodyResolvePrerequisitePhase/shouldCheckIsResolved 特判。modeNone 测试通过。不可据此把目标锁内准备和兄弟写入计为完成。 |
| G3 | 未闭环 | CaStandaloneProjectStructure.getModule():160 与 AnalysisApiLspProjectStructure.getModule():317 仍先 `useSiteModule?.let { return it }`，没有先按 content scope 找真实 owner。 |
| G4 | 未闭环 | LL session 的 extend composer:776 仍为 `allRefinementDependencies + deserializedProvider`，缺普通源码 dependencies 的统一集合。 |
| G5 | 未闭环 | LLCfirTypeLazyResolver:177 仍有独立 buildConfiguration；主 TypeResolveTransformer 仍保留 constructorOwnerForTypeResolution。 |
| G6 | 未闭环 | 单模块 first-fit 和多 parent 夹具未完成按官方精确版本门分组，生产对应门也尚未实现。 |
| G7 | 闭环 | `CjNamedFunction.isReanalyzableContainer() = typeReference != null`；FunctionBodyModificationTest 的推断→空体、空体→推断、显式返回类型共 3/3，验证签名及 caller 失效。 |
| G8 | 未闭环 | AbstractOverriddenDeclarationProviderTest:71–72 仍先 actualDirect，再 actualAll，没有恢复规定的 all-first 顺序。 |
| G9 | 未闭环 | CangJieDeclarationProvider 仍只有 findFilesForFacadeByPackage，缺语义明确的包内全部文件接口；当前 FileBased provider 改动没有完成此契约迁移。 |
| H1 | 未闭环 | CjoFullIdResolver:128 与 CfirCjoPackageMetadataProducer:726 仍硬编码 `_CNat3AnyE`；默认上界与 FullId owner 未按计划完成。 |
| H2 | 未闭环 | CfirSessionFactoryContextUtils:46 仍将 common CJO 父目录并入 library roots，经通用按包搜索加载，未只装载显式输入文件。 |
| H3 | 未闭环 | AbstractFrontendPipeline.execute():30 仅 compileCjd 分支映射 CJO 输出路径，普通 common 模式未完成 CLI 自动输出。现有两段式测试仍直接配置输出键。 |
| H4 | 未闭环 | CfirFrontendPipelinePhase:145 仍调用 CjmpDeserializedCommonSideReporter 字符串报告旁路。 |
| H5 | 未闭环 | provider 仍按 SPECIFIC + header.isCjmpCommonPart 启用 features/options，未由专用显式 common 输入 owner 决定。 |
| H6 | 未闭环 | CommonPartLoadGate 没有接收语言版本，CjoManager 对所有包无条件格式版本检查；alpha.20/alpha.05 等精确版本门未生产实现。 |
| H7 | 未闭环 | debug/opt/packageFeatures 仍从独立 CJMP 配置读取；生产 CLI debug/opt 映射和两 raw 路径真实 file features 输入尚未闭合。 |
| H8 | 未闭环 | 加载事实虽存入 session，尚未建立 CLI/IDE/LLT 统一 checker 诊断通道；见 H4 的旁路和 H6 通用管理器门。 |
| H9 | 未闭环 | producer 没有消费生产合并图写出未遮蔽 common 声明；nominal/extend merge 与初始化依赖也没有生产接线。 |
| H10 | 未闭环 | producer 的 declarationIdentifierOffset() 仍自行扫描 LightTree token，未改为共享定位策略及共享 OPERATION_NAME。 |
| H11 | 未闭环 | deserializer 仅将 begin/identifier 放入 cjoDeclarationPosition 数据，尚未建立统一二进制 source owner；CLI reporter 仍自行定位。 |
| H12 | 未闭环 | MultipleModuleDataProvider.getModuleData() 每次 partition exact/fallback 并拼接顺序，排序职责没有迁到 builder。 |
| H13 | 未闭环 | recordCjoDeclarationPosition() 仍只按 begin.file 查当前 context.pkg.allFiles，忽略 Position.pkgId。 |
| H14 | 未闭环 | MacroExpandPhase:721 的 System.err.println("PROBE-EXE: ...") 仍在生产代码中。 |
| I1 | 未闭环 | DeclarationsResolveTransformer:1297/1312 仍用根级 `type is ConeErrorType` 过滤及传播，没有 containsErrorType。 |
| I2 | 未闭环 | ExpressionsResolveTransformer:672–675 的歧义限定符 + ErrorNamedReference 回滚分支仍在。 |
| I3 | 未闭环 | TypeResolver:529 的 CjBinarySourceElement 特判仍存在。 |
| I4 | 未闭环 | LLModuleLazyDeclarationResolver:183–198 仍向 targetSession 补录外部文件 import bindings。 |
| I5 | 未闭环 | TypeUtils.coneTypeSafe() 仍分支处理 ResolvedImplicitTypeRef；全源码构造点仅在 ReturnTypeStateTest，生产未构造。 |
| I6 | 闭环 | MacroAnnotationSourceModuleTest.resolveThroughBody():309 按 CfirResolvePhase.entries 筛选并驱动，阶段序列没有另写副本。 |
| J1 | 未闭环 | CfirCjmpFamilyAccessors:37、CommonSideChecker 的包/模块读取仍使用 runCatching，nullable/empty 退化路径尚未删除。 |

## 当前验证边界

本次重新读取实际 XML，未重跑或将较早快照冒充最新输入的全量测试。

| 证据 | 实际结果 | 输入边界 |
|---|---|---|
| resolution.common XML | 65/65，0 failure/error/skip；UTC 10:18:27 | 最新共享初始化排序与外部依赖契约输入 |
| CFIR resolve CJMP XML | 52/52，0 failure/error/skip；UTC 10:30:34–35 | 最新输入；仅 CJMP 选择器，不是整个 resolve 模块 |
| LL XML | 168/168，0 failure/error/skip；UTC 05:38:56–05:40:06 | pattern/header 修复输入；早于后续共享算法改动 |
| 两段式 XML | 19/19，0 failure/error/skip；UTC 05:40:45 | 同上；直接 pipeline 配置测试，不等于 H3 CLI e2e |
| pattern-header-final-full 快照 | 8968 条：8571 passed、397 skipped、0 failed；0 regressions | 同上；不是最新共享算法输入的全量验收 |

最新 queue `5a184292` / handle `20067` 已终止，exit 0，12m47s。当前没有需要轮询的该作业。
初始化排序共享算法及 6 项新测试已完成，但 CFIR collector、canonical FullId 恢复、CJO producer、
实际化 scope、初始化检查消费者未接线，不能据此给 C1/H9 或九步迁移计为完成。

## 阶段门禁

P2 的 B1 已闭环。P1、P3、P4、P5、P6、P7、P8、P9 均有未闭环问题。
P0 已有大量官方探针和版本差异记录，但仍缺所有高风险残留项对应的完整复现／验收闭环。
P10 缺最终输入的整套回归、共享 checker/merged scope 等最终 parity 复审及逐项修复提交标注。
现有完整回归绿色只证明其已覆盖输入，没有消除本表列出的框架缺口。
