# TypeUseGraph 指定修订复审（2026-11-19）

**KotlinParityGatekeeper：T1／T3／T4／T5 的原拒绝点源码层关闭；T2 的普通 BODY／implicitTypes 后续 BODY／lambda 主链与共同 encoder 返回出口已修复，但构造器创建 owner 尚未全覆盖，T2-R1／T2-R2 仍 REJECT。T6 按明确未实施状态保持 OPEN；不签发完整 S4、方案或行为通过。**

本报告接续 [生产 gate](type-use-graph-production-gate-20261119.md)，只复审用户指定修订。唯一实现树 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，HEAD `b4dd742d589e4b5ee6592d454634c44f634df08a`；结论针对最新未提交磁盘。延续已读的 `D:/code/intellij/cangjie/.agents/skills/kotlin-cangjie-frontend-parity/SKILL.md`。仅 apply_patch 新增本文件，不改生产／测试／既有文档，不跑 Gradle／cjc，不联系停止旧会话。

Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`、官方 `14276740ffec1262b81b6b5236b1d840af270b37` 继续通过主树 external 的 git show 固定对象读取，external 只读。本轮特别重新读取 Kotlin body resolver 和官方普通／默认／enum constructor 返回节点创建，未用此前记录或当前上游 HEAD 替代。

## T1／T3／T4／T5 具体修订核验

| finding | 最新 owner／坐标 | 本轮有限裁定 |
| --- | --- | --- |
| T1 当前 declaration 停止 | [Graph.selectDeclaration L166–173](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) | type.containsError 先出 Invalid；当前 precedingFacts.isCompilerAdded 或非 SOURCE 声明直接 Semantic，然后才解析 signature／header。已消除以 SOURCE 准入代替 COMPILER_ADD 的原拒绝点。与固定 TypeManager L2413–2418 的顺序相符；不据此证明每个创建 owner 已正确发布 compiler-added 位。 |
| T3 selected Array 规范化 | [Producer.frozenTypeUseShape L3095–3102](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) | 完成选出的 arguments 后，外层和唯一子槽均为 Array 时取子 Array 的 arguments 作为单叶列表，以 addExact 累加双方 dimsOrSize；其余类型保留选出的完整 arguments。原“旧 d＋嵌套 Array(k,L)”出口已修，当前为 Array(d+k,L)，对应 [native SubstituteTypeArgs L2359–2361](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L2359)→[GetArrayTy L113–126](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L113)。未运行 nested／flat 或 wire 用例。 |
| T4 CFunc 当前 argument 先选择 | [Graph L139–148](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) | L142 先 selectReference(argument)，不再以 syntaxPayload 的 FunctionType 检查拦截 non-SOURCE 输入；Semantic 和 Invalid 原样返回，只有 Function 选择产品应用 C 标记／外层完成 type。旧 Semantic 重建子用途已删除。对应 [native L2377–2382](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L2377) 的 FuncType owner→当前节点选择。 |
| T5 Generic 上界域 | [Producer.prepareGenericTypeBounds L3227–3231](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) | 以 pending.anchor.domain 取得上界 TypeElement，再传固定 context／projection 给 TypeElement overload；没有再走按 context.moduleData.session 查 Cone 的 overload。此处原换域错误已修。完整 Generic 原始约束／语义上界合同仍属 T6，不随这项域修复关闭。 |

Graph 当前 ref origin 停止仍位于 [L104–112](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt)，Alias 资格仍使用 isExported／IMPLICIT_USED，没有改为 EXTERNAL。本轮不重新签发整条 Alias selector 或全部 encoder 合同。

## T2 已修复的正式完成链

新发布器位于 [CfirDeclarationsResolveTransformer L417–485](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt)：

- L423 只在正式 BODY_RESOLVE phase 执行。L424 排除候选分析／非默认 inference session；L425 排除尚未退出的外层函数、initializer／default／fragment owner。
- L427–435 根据实际 root 的类型、CFG／正文完成输入判断能否提交；L439–469 从正式 root 按实际拥有边收集完成函数，跳过 CFG、type／annotation／header 可达性。局部函数、lambda、已解析实参、默认值和 parameter-owned helper 在其父域完成后进入这次遍历。
- L474 对本源码图中当前 SEMANTIC 返回 ref，在同一提交事务创建新的 COMPILER_ADDED resolved ref；保留原 Cone 和 annotations，将原 ref 本身设为 delegation；L484 再发布 BODY phase。没有改全局 known-ref builder 默认，也没有从 source-null／fake-kind 推断生成节点。
- 对原 SOURCE 返回 ref 不执行这个通用包装；对已 COMPILER_ADDED ref 也不再次包装，因此重复 BODY 提交不会仅因这个循环再增加一层来源 ref。不同真实创建 owner 仍须分别发布，见两个 constructor finding。

### BODY／implicitTypes／lambda／helper owner 路径表

| owner／模式 | 实际入口与最终提交点 | 读取结论 |
| --- | --- | --- |
| 普通／局部 named function，main／macro／finalizer | [L1251／L1264／L1278／L1292](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) 共用 L1310 function-like 路径；L1467 在返回类型写回后调用完成发布。 | 正式 BODY 的当前函数可发布；嵌套函数在外层未完成时不独立发布，最终外层 root visitor 会重访其已完成正文。仅有 header／不完整 body 的函数在 L445 停止，不由“可达”授权发布。 |
| IMPLICIT_TYPES phase／ReturnTypeCalculator designated jump | [SimplePhaseProcessors L33–39](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSimplePhaseProcessors.kt)、[DesignatedBodyResolver L23–29](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDesignatedBodyResolveTransformer.kt) 使用 IMPLICIT_TYPES；发布器 L423 因 phase 不同返回。 | 这是阶段提交限制，不能单独算作漏掉正式 BODY owner。[withFullBodyResolve L101–110](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAbstractBodyResolveTransformer.kt) 只临时切 implicitTypeOnly，不把 phase 改为 BODY。 |
| implicitTypes 已算出返回类型，后续正式 BODY | [SimplePhaseProcessors L65–77](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSimplePhaseProcessors.kt) 使用普通 full BODY dispatcher；[function-like L1317](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) 仅在 implicitTypeOnly 时早退。 | resolved 返回类型不会令正常 BODY 在此早退。即便复用 implicit-aware dispatcher，[cache L179–180](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirImplicitAwareBodyResolveTransformer.kt) 在 full 模式＋resolved 返回时直接执行 transformation，绕过旧 Computed 短路；随后 L1467 可完成发布。不能以“已有隐式计算缓存”推测此主链被漏掉。 |
| postponed／call-completion lambda | [CompletionWriter L1417–1439](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt) 完成返回、参数和函数类型，只有 Mode.Normal 调用同一发布器。 | 自身可作为正式 root 时发布；位于未退出 outer/default/initializer 内则延迟，由父 root 的最终遍历提交。候选试跑没有因 writer 调用自动取得 BODY 发布许可。 |
| 普通 constructor 有实际 body | [L582–604](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) 完成 helper、签名、委托、body、CFG，然后调用发布器。 | 若实际 body／类型／CFG 满足 L491–495，且当前返回为 SEMANTIC，可创建 COMPILER_ADDED 返回 ref。固定 [CheckFuncBody L203–213](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp#L203)、[CheckCtorFuncBody L457–477](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp#L457) 对应创建返回节点、填 ctorTy，再检查 body。无 body 的隐式 primary ctor 不受这条结论覆盖。 |
| 默认参数 helper | [L1363–1373](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt)，[BodyResolveContext.withNamedFunction L864–866](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/body/resolve/BodyResolveContext.kt)。 | beforeEnter 在原 owner 函数入栈前解析 helper；完成后调用发布器。若仍有外层函数根，延迟到其参数拥有的 helper 边。L451 要求参数 default 自身完成，不把未完成 default 的 helper 子域提交。 |
| 变量／字段／模式初值、代码片段 | [L843／L975／L1172](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) 与 L202 提交实际 root；参数默认根 L909。 | 对这些输入中已完成的嵌套函数／lambda，root visitor 有实际表达式／实参／默认值拥有边；不是仅按声明列表扫根。需后续行为验证不同组合。 |
| enum payload constructor／无 body 的 implicit primary constructor | 以下 T2-R1／T2-R2。 | 没有进入已核实的全部实际创建协议，不能宣称 constructor owner 覆盖完整。 |

固定 Kotlin [transformNamedFunction L1026–1052](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/body/resolve/FirDeclarationsResolveTransformer.kt#L1026) 同样区分 implicit-only 早退与完整 body；[L1093–1105](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/body/resolve/FirDeclarationsResolveTransformer.kt#L1093) 处理 ResolvedImplicit／implicit 返回原型。当前 native 生成返回节点是仓颉语言产品适配，不声称该额外 phase publication visitor 是 Kotlin 同名同形实现。

## CopyUtils 原型来源判据：当前包装协议下成立

[CopyUtils.hasImplicitOrInferredReturnType L35–39](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt) 保留 implicit 节点判断；对具名函数，当前 SEMANTIC 仍表示推断／known 返回，新增“源码图 function＋COMPILER_ADDED 当前 ref＋直接 delegation origin 为 SEMANTIC”判断。依据正是发布器 L481 将**原已发布 ref 本身**作为新生成节点原型，不是根据“有 delegation”猜测省略。

该协议区分：

| 当前返回节点／原型 | 原省略判断 |
| --- | --- |
| SEMANTIC resolved／implicit | 延续原隐式／推断合同。 |
| 本轮完成发布的 COMPILER_ADDED → 原 SEMANTIC ref | 具名函数仍被视为原省略返回，不重新施加声明显式返回约束。 |
| COMPILER_ADDED → 原 SOURCE ref | 不因当前生成分类而变成省略。 |
| CJO 生成包裹无 SEMANTIC 原型，或非源码图声明 | 不满足新增原省略分支，不能仅按 COMPILER_ADDED／source-null 当作推断。 |

公共 [CopyUtils L23–26](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt) 仍走公共 resolved 替换；[TypeUtils L78／89／100、L82／93／103](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt) 复制当前 origin 和既有 delegation，因此该直接原型证据不会因普通替换再变成一个“COMPILER_ADDED 原型套 COMPILER_ADDED”的误判。没有 source-null／fake-kind 条件。

判据依赖创建 owner 维护上述原型合同；不能把任意 COMPILER_ADDED 节点没有原型的情况自动视为原省略，也不能把发布器改成先穿过原型后重新猜当前来源。新增 BODY publisher 没有制造这种分叉，本轮不对该判据另发 REJECT。它仍维持既有 CfirNamedFunction 范围，不宣称已为所有 function-like 声明新增“省略”布尔语义。

## lambda／普通 FuncBody encoder 返回出口：已统一

[CjoCanonicalSourceEncodingContext L21–22](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoCanonicalSourceEncoder.kt) 增加 `functionReturnTypeReference(CfirFunction)`；[canonical lambda FuncBody L62–65](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoCanonicalSourceEncoder.kt) 通过 owner 使用它，不再直接拿 body.returnType Cone 编码。[Producer L3003–3004](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) 查同一冻结 TypeUseGraph.functionReturn；[普通函数 L2019](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) 同样查询该用途。

Graph [L73–74](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) 按冻结 header 的来源决定实际返回节点；当前发布的 COMPILER_ADDED ref 通过 [actualTypeNode L97–98](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) 保留，随后当前 origin 在 L111 停止。这消除了上一轮 lambda 的 Cone-only 返回旁路；signature 与 FuncBody 返回使用相同已选出 TypeUse，但声明级 COMPILER_ADD 停止仍是独立合同。

此结论限于 CfirFunction 编码入口；以下 enum constructor 仍有另一条旧出口。

## T2-R1 [P1] REJECT：enum payload constructor 的真实返回节点创建未接入

具体坐标：

- [CfirEnumConstructor L23](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirEnumConstructor.kt) 继承 CfirCallableDeclaration，**不是 CfirFunction**。
- [TypeResolveTransformer L467–475](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) 从原 implicit 返回 ref 通过公共 helper 得到 SEMANTIC owner type；[BODY enum L670–680](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) 也只是返回类型解析／同一 helper。
- 新 root visitor [L443–448、L466–467](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) 只收集 CfirFunction，enum constructor 走 else；Graph functionReturns 也只接受 CfirFunction。
- [Producer L2036–2039](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) 对有参数的 source enum constructor 创建 CjoFunctionBodyInfo 时，仍直接 type(typeOf(returnTypeRef), declaration)，没有冻结返回节点用途。

固定官方 [SetEnumEleTyHandleFuncDecl L527–534](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckDecl.cpp#L527) 在 enum FuncDecl constructor owner 创建真实 RefType、填 ctorTy.retTy，并发布 COMPILER_ADD。这不是普通函数体尾部推断，也不能靠 CfirFunction 的 CFG/body 完成条件覆盖。无参数 enum variant 的 native VarDecl 角色则不能据此补造 FuncBody。

**下一修复合同：** 在 enum payload constructor 对位的语义创建 owner 发布 COMPILER_ADDED 返回 ref／原型，冻结它独立的 FuncBody 返回用途；移除该 source enum constructor 的直接 Cone 出口并消费冻结选择。保留无参数 variant 与 imported wire FuncBody 的不同角色，不为复用函数 publisher 伪造 enum body／CFG。此项由实际类继承及 native 创建路径确定，未运行反例。

## T2-R2 [P1] REJECT：ordinary class／struct 的隐式 primary ctor 仍为无 body，正式 BODY 发布器不能覆盖其返回创建

具体坐标：

- 两 Raw 的 [PSI.buildImplicitPrimaryConstructor L1575–1588](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt)、[LT L642–655](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt) 生成 Source constructor、implicit 返回 ref，但 body=null。
- TYPES 的 [transformConstructor L439–445](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) 通过公共原型 helper 写 owner type，默认仍是 SEMANTIC。
- BODY constructor L604 虽调用了 publisher，[hasCompletedBodyForPhasePublication L491–495](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) 要求非 null 完成 body 与该函数 CFG，所以该隐式 ctor 在 root／nested visitor 都不能进入 L474 返回节点生成；本轮检索到的非 CJO COMPILER_ADDED 返回发布仅此一处。
- [macro finalize L37–38、appendPreparedConstructor L47–52](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/ImplicitConstructorDeclarations.kt) 只删除被显式 ctor 取代的早期节点或将保留的 implicit ctor 移到末尾，没有给保留的节点生产 body／返回 ref；未发现这一前端接线补齐上述发布条件。
- 已冻结 SEMANTIC header 被 Graph.actualTypeNode 排除，普通 Cjo FuncBody 返回出口便得到 null／0。调用点存在不等于真实创建 owner 已覆盖。

固定官方 [CreateDefaultCtor L311–338](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Utils.cpp#L311) 为实际 default ctor 创建 FuncBody、空 Block、参数列表，并发布 constructor 的 COMPILER_ADD／IMPLICIT_ADD；[AddDefaultCtor L385–393](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Utils.cpp#L385) 只在对应 ordinary class／struct 条件下加入。[CheckFuncDecl L78–79](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckDecl.cpp#L78)→CheckFuncBody L203–213→CheckCtorFuncBody L476 的真实生命周期创建返回 RefType 并填 ctorTy。

本项只针对 native 条件下真实生成的 ordinary 默认 ctor，不能推及 common／specific、mirror、foreign、abstract/header-only 函数；native AddDefaultCtor 本身有明确排除条件。不能以本地 body-null 表示证明 native 返回 Type 节点不存在，也不能因此让“不完整正文”的 phase gate 全局放行。

**下一修复合同：** 以正确 native 默认 ctor owner 和完成生命周期生产实际返回节点事实，并保持相应 canonical body／阶段输入一致；若保留一种独立的语义生成路径，须由创建 owner 明确发布可冻结的完成产品。不要仅对所有 body-null 函数无条件包装返回 ref，不用 fake source kind 推断 COMPILER_ADD，不通过放宽 hasCompletedBodyForPhasePublication 绕过正文完成合同。补 ordinary class／struct implicit ctor、显式 ctor、static init 及 native 排除分支的定向验证。

## T6 与验证余额

T6 的完整 Generic 原始 constraint／catch 类型槽未实施，本轮按用户明确状态保持 OPEN，不重记为这次代码新增回归；上述域修复只关闭 T5。G2／G7 的 concrete copy／缺损 Paren 入口未发现本次修改引入回退，亦不在本轮扩大复审范围。

已读新增 [TypeUseGraphLifecycleTest L101–122](../../cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CfirExportTypeUseGraphLifecycleTest.kt) 的来源／原型／换代用例和 [implicitReturn L388–411](../../cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CfirExportTypeUseGraphLifecycleTest.kt) 的 completed COMPILER_ADDED／signature 与 FuncBody 身份断言。这些是当前测试源码，不是已经执行的成绩；此处没有 enum payload ctor 或 body-null implicit primary ctor 覆盖证明。

本轮没有运行 Gradle，也没有轮询用户刚启动的 generate／compile 或取旧 XML 作为本轮终态。待验证包括 implicitTypes→BODY 正式根过渡、候选延期与父域完成遍历、lambda 正常／失败候选、参数 default/helper、变量/字段/fragment 中嵌套函数、CopyUtils 对 SEMANTIC／COMPILER_ADDED→SEMANTIC／COMPILER_ADDED→SOURCE／CJO 包裹的约束判断、两种 constructor 漏项，以及 T1/T3/T4/T5 的定向行为。T2-R1／R2 和 T6 均未关闭；不计完整 S4、S5/S6 或整体方案完成。

## 关键未提交磁盘版本

以下 16 个关键文件在 `2026-11-19T04:08:55.2886838+08:00` 与 `2026-11-19T04:09:53.7172249+08:00` 两次 SHA-256 读取一致，绑定本轮实际审查内容，不以 HEAD 或此前首轮 aggregate 代替。

| RepoRelative 文件 | SHA-256 |
| --- | --- |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) | `5921EAE63A1D1BA6C4D0B76C3520113010CAB70A2E6F11D209822E90B8733412` |
| [cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) | `D5A171B2A9A20AA18DE51EAEC45BAC452D3A2C7C0D533C1318A9DF6B2FEFF062` |
| [cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoCanonicalSourceEncoder.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoCanonicalSourceEncoder.kt) | `266E71501BE6B161A73F7881CD88734CD7A2356A523E243306860B280307A672` |
| [cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt) | `85510CA064CE8102CE4C4C8A16C0EF888277426D2B1FEC0C5A8F3CE191E995AD` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) | `596E74AC8EEDE716C7D7473C77710E7D571DFB5218CCDAF05DBBA62DC9294342` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirImplicitAwareBodyResolveTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirImplicitAwareBodyResolveTransformer.kt) | `20BF0A68597AD4050668438BF27E3823F469E0854251628760A5556A544507CF` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDesignatedBodyResolveTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDesignatedBodyResolveTransformer.kt) | `BB9B53D5F5F3061DE7A638A599CBBC318D233CDF96A0907D9C11E3930FC01626` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAbstractBodyResolveTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirAbstractBodyResolveTransformer.kt) | `1EF0672B37AC6E4624F457183E7EA19F4E40398ED334575ED491573B2FF1B29F` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSimplePhaseProcessors.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSimplePhaseProcessors.kt) | `8F81F01FF96138471EB84FC009B157DED074334B5427051EB5DA34C6A8D6DC6A` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt) | `8F10F5A871A5C6ABF04D900CF272C67C4D3C55132ADF0A1B4E629EAA8C8D57EE` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/body/resolve/BodyResolveContext.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/body/resolve/BodyResolveContext.kt) | `00B9BB3B746AE92FF81B3763A1E91B82FD818A04CAADE1DB42F9B094417A7A84` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt) | `E215EC86FF110616ADBBE7006CDEFB91A330BA33F3FB76AAD4E5694A50E403A2` |
| [cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt) | `7980A2072CBCCC8ABEA1AA9C9C011882E061B6DCD32B650CC28EE1D0909C4A2A` |
| [cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt) | `F836875FC2D4F5CDE01D0446E06197494A909B7453F88844DC2188FF44E2DD01` |
| [cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirEnumConstructor.kt](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirEnumConstructor.kt) | `3C737C1B22B63090A2F22399FB104D9D44C12F599F9986C81DE0EE8991ADB82F` |
| [cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CfirExportTypeUseGraphLifecycleTest.kt](../../cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CfirExportTypeUseGraphLifecycleTest.kt) | `4902CF6DDD1A4038E27510C74222E15293408C377FA93F50A20F25B435D448AB` |

