# CJMP S4 本轮五路径最终限定 gate（2026-11-18）

**限定裁决：REJECT。F1、F2、F3、F5 的本轮修订路径 ACCEPT；F4 仍有两项实际阻塞。R2/R6 的旧 findings 保持关闭。不得将此裁决作为整个 S4 的签发。**

评审角色：KotlinParityGatekeeper。先读取 [旧拒绝报告](source-frozen-encoder-gate-r1-r6-recheck-20261118.md)，再独立追踪本轮代码及固定上游对象。没有联系旧会话。只读生产、测试与上游；未运行 Gradle、cjc 或测试；唯一写入为本新增报告，未改旧报告。

唯一一方工作树：`本记录所指的唯一实现树`。实际 HEAD：`b4dd742d589e4b5ee6592d454634c44f634df08a`。工作树含未提交修订，**HEAD 不代表全部候选内容**；本报告同时绑定 Producer 实际文件 SHA-256 `17AF24068B674B17A7709B6460408B3BFF8BEE7559D96942D6AE6853ADF01307` 及文末逐文件摘要。写报告前复核全部 26 个摘要未变化，HEAD 也未变化。

Kotlin 权威固定为 `3c7a5041f238102285764d370665f56f587f9dc4`，官方仓颉权威固定为 `14276740ffec1262b81b6b5236b1d840af270b37`。上游实现正文均以对应仓库中的 `git show <固定对象>:<路径>` 读取；`git grep` / `git ls-tree` 仅用于固定对象定位。未以镜像 checkout 内容充当权威。

## 五项裁决

| 项目 | 当前生产路径与旧阻塞复核 | 限定裁决 |
|---|---|---|
| F1 | Producer L3088–3112 确认 Class/Struct/Enum normal self；L3175–3181 先缓存原 shape，只登记 specific 关系。L3198–3209 先查输入槽、再查 specific 已有槽，否则预登记双方，L3210–3247 编码原 shape。This/Interface/Alias 不进入双 memo。 | **ACCEPT**：旧无条件 specific 正文替换已消除。 |
| F2 | L3120–3126 登记 Generic 锚点并排队；外部入口 L3071–3073 在整个结构 shape 返回后独立排空上界队列；L3185–3193 可继续处理新发现的 Generic；fill L3220 只消费完成的上界 shape。 | **ACCEPT**：旧结构 DFS 中立即准备 bounds 的回边阻塞已消除。 |
| F3 | 实际 receiver 规约入口 CfirCallResolver L2734 与 declaration TP 恢复入口 ConeOverloadConflictResolver L1209 均调用共享 withArguments；共享 TypeUtils L458–465 保留 rawArrayDimensions、interface、This、attributes。两入口继续遍历物理实参，与该复制 API 的合同一致。 | **ACCEPT**：这两个真实入口的维度退回 1 已消除。 |
| F4 | 公共接口、Cone context、AbstractTypeChecker 的累计维度/不变叶子已接入；语言声明实参视图是稳定 lazy，class substitution 与 extend assumption 确有调用。但 marker 函数替换、真实 extend 匹配仍使用错误的物理实参域。 | **REJECT**：下述 F4-A、F4-B。 |
| F5 | Producer L626–648 对实际 declarations 队列中的 source 声明读取同一 patternMetadataOwner 的计划，收集 target 并将 args 加入保存表达式集合；L650–686 收集原输入和 canonical 类型、引用实参/父类型、Array/ArrayLiteral initializer 及 payload pattern 的类型/声明/表达式依赖。新目标经 reserveSourceDefinition 保存 owner，新增声明继续由同一队列处理。 | **ACCEPT**：旧“只进入 capture，不进入实际保存”及本轮列明的 canonical 依赖路径已接入。 |

以上 ACCEPT 只关闭所列路径的具体阻塞，属于静态审查结论，不表示测试执行通过。

## F1/F2/F3 的权威对应

F1 [当前 type(shape)](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L3197） 与官方固定 `src/Modules/ASTSerialization/ASTWriter.cpp` 的 SaveType L925–954 一致地执行输入 memo、specific memo、双方预登记；正文继续按输入 pType/shape 编码（官方 L956–1009）。身份确认与槽选择已经分离，不能再沿用旧 F1 拒绝。

F2 [独立 bounds 队列](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L3185） 使旧报告的 `I<T> → T → I<T>` 顺序变为：先完成并缓存 I<T> 的结构 shape，再准备 T.bounds；第二次 I<T> 查询命中已缓存 shape。相互 Generic 发现也可继续排队，pending 集合在上界 map 完成后才移除。官方 SaveType L944–954 在递归细节前登记 Ty，SaveGenericsTy L1015–1021 沿 memo 回返；固定 `unittests/Sema/SemaCangjieFiles/AssumptionTest/assumption1.cj` L17–21 保留自/互相 nominal 约束。Kotlin 固定 `compiler/fir/fir-serialization/src/org/jetbrains/kotlin/fir/serialization/FirElementSerializer.kt` L938–963 将 TP binder 与上界编码分开。本项仅核对回边准备顺序。

F3 [共享复制 API](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt)（L458） 保存原层维度，并随子类型复制重新构建布局；输入平坦二维和逐层二维都不因这两个调用点丢失维度。官方固定 TypeManager.cpp L279–286 的 GetInstantiatedArrayTy 和 L2359–2362 的 SubstituteTypeArgs 保留原 dims。

## F4-A — [P1] marker 两个替换重载使用不同的实参域，恒等替换增加维度

当前 [列表重载](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeInferenceContext.kt)（L669） 对 RawArray 使用累计 rawArrayLayout.dimensions 重建，因此调用者必须传入规范叶子实参；这与 getArgument/getArguments 的新合同相符。但 [函数重载](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeInferenceContext.kt)（L707） 仍在 L710 对物理 typeArguments 调用 replacement，再交给该列表重载。

具体输入就是当前 [测试中的逐层二维值](../../cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/CfirTypeRelationsTest.kt)（L106）：外层 RawArray 的物理实参为一维 RawArray<Int64>，外层累计布局为 Int64、2。对它调用 `with(context) { nested.replaceArguments { it } }`：

1. 函数重载把一维内层数组作为 newArgs 保留下来。
2. 列表重载以原累计维度 2 和这个内层数组构造外层。
3. [ConeClassLikeType 布局构造](../../cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeClassLikeType.kt)（L109） 再加内层维度 1，得到 Int64、3。

同布局的平坦 wire 二维值执行同一操作仍为二维。因此两种已完成表示经过公共替换协议后分离，新增 equalTypes 分派无法挽救已经改变的布局。这是源码可直接确定的结果，未声称运行该表达式。

这不是孤立测试 API：[TypeSystemContext.replaceArgumentsDeeply](../../common/src/org/cangnova/cangjie/type/model/TypeSystemContext.kt)（L356） 调用函数重载，生产 [ResultTypeResolver](../../resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/ResultTypeResolver.kt)（L80） 经 createPlaceholderTypeForSelfType 使用 deep 替换。另一个实际通用入口 [eraseContainingTypeParameters](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeInferenceContext.kt)（L724） 也在物理实参递归后调用累计维度列表重载，存在同样的重计维度问题。上述调用存在性不表示所有递归上界输入都合法。

固定 Kotlin ConeInferenceContext L377–386 的两个 replaceArguments 重载共同遵循同一 withArguments 实参合同；固定公共 TypeSystemContext L256–264 的 deep 算法依赖该一致性。仓颉允许规范叶子域与声明降维域不同，但不能令同一 marker API 的重载分别使用这两个域。官方 TypeManager.cpp L113–126 的 GetArrayTy 会累加数组元素的维度，L279–286 的实例化则先读规范叶子再携带原 dims，印证此处重计的原因。

**具体改法：**保持普通 Cone.withArguments 的物理复制合同；让 marker 的函数重载和擦除入口统一读取 typeSystemArguments/getArguments 的规范叶子，再使用累计维度列表重载。由同一公共替换 owner 保证两个重载与 deep 操作的一致性，不在 encoder 或调用点减维度补偿。补充逐层/平坦输入的函数恒等替换、deep 替换及含 TP 叶子的擦除检查；现有 CfirTypeRelationsTest L110–115 只覆盖列表重载。

## F4-B — [P1] 真实 extend matcher 没有消费降维实参，装载数组实例化出错误接口

新 [rawArrayTypeArguments](../../cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeClassLikeType.kt)（L120） 确实是稳定 lazy 视图。它已被 [class substitution](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/scopes/impl/CfirClassSubstitutionScope.kt)（L791） 和 [extend assumption](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt)（L920） 消费；这两条修订本身成立。但真实 use-site extend 实例化通过 [instantiateExtendSupertypeDescriptors](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirTypeAwareSupertypeProviderImpl.kt)（L411） 进入 createExtendDeclarationSubstitution，而 [matcher 的 lookup-tag 分支](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendSubstitution.kt)（L407） 仍只比较 classifier 并递归物理 typeArguments，不读取维度或降维视图。[extendPredicateTypeArguments](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendSubstitution.kt)（L172） 同样仍使用物理列表。

具体完成输入：一层目标 RawArray<T>、接口 I<T>，实际 receiver 为 wire 装载的 RawArray(Int64, dims=2)。当前 matcher 同 classifier 后比较 T 与物理叶子 Int64，于是 substitutions[T] = Int64，生产 provider L425–426 得到 I<Int64>；使用逐层源码二维 receiver 时，同一个 matcher 则绑定 T 为一维 RawArray<Int64>，得到 I<RawArray<Int64>>。同布局的两个已完成 receiver 因表示来源不同而获得不同的实际父接口及成员签名。这是完成输入的调用流推演，不是 cjc fixture 执行结果。

固定官方 TypeManager.cpp L1876–1887 的 GetTypeArgs 对二维数组返回降一维数组；L1367 的 GetAllExtendInterfaceTy 和 L1595–1598 的 CheckExtendWithConstraint 真正消费该视图，L1284–1328 的 helper 用收到的 typeArgs 实例化接口。只改 declaration assumption 不能代替这些 use-site 消费方。

同一域遗漏还存在于 [ConeInferenceContext.createSubstitutorForSuperTypes](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeInferenceContext.kt)（L927） 的 L933，以及 [type-aware provider 的 declaration substitutor](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirTypeAwareSupertypeProviderImpl.kt)（L508） 的 L511–515：它们把声明 TP 配到物理列表。需与已修订的 class substitution 一并审计，不能认定已有两处消费者就代表消费闭包完整。

**具体改法：**在类型公共 owner 中提供统一的声明实参查询（RawArray 读取已有稳定降维视图，其余类型保持各自实际声明实参），让 class substitution、assumption、真实 extend matcher/predicate、声明父类型替换都使用正确域。RawArray 模式匹配须按实际数组层级建立 TP 映射，保留累计维度语义；一层 RawArray<T> 匹配二维 receiver 时 T 必须得到降一维数组，不能用“所有维度必须立即相等”拒绝这个泛型实例。补充真实 provider 的逐层/平坦 receiver 接口实例化与约束过滤一致性检查；当前复读的 CfirTypeAwareSupertypeProviderTest 没有 RawArray 用例。保持通用 marker 的规范叶子查询独立，不把它改回语言层视图。

## F5 与 R2/R6 的限定关闭

F5 [保存闭包](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L616） 与 [最终 annotations](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L2943） 使用同一 source owner、同一计划。普通导出函数即使没有 SOURCE body，计划 target 仍进入 collectNeededSourceTarget；非 imported 本包目标由 L586 登记定义及 owner，实际声明队列继续处理它的 header/计划。canonical visitor 收录 record.type、Reference/Member 的 target、instantiatedTypes、matchedParentType、数组 initializer 和 payload patterns；expressionChildren 包含 pattern 表达式子槽。官方 ASTWriter.cpp L1727–1736 的 custom target 经 GetFullDeclIndex，后者 L791–807 为本包目标调用 PreSaveDecl，与这里的引用闭包角色对应。

此关闭限于本轮指定的计划及依赖边，未签发完整 canonical SOURCE 图所有角色/回边。正常计划准备仍由 CfirAnnotationExportPlan 的 owner 裁决 literal 参数、Annotation 图及 Frozen/TestRegistration 空实参，不扩大到全部被 capture 的声明计划。

R2：[sourceExpressionTarget](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L3027） 在 L3035 先读冻结 isImported，再查询 L3043 的本地/raw 槽；collectNeededSourceTarget L578 同样保留 imported 身份。旧“raw 本地槽覆盖真实 imported FullId”未回归。

R6：source-shaped CJO 声明的 annotations L2945–2946 与 raw metadata L1286 都调用 importedAnnotations；L1311 通过 importedExpressionReference 编码原 arg.expr，L1314 按原快照 remap target。旧“common CJO 注解被要求提供 source canonical identity”未回归。

## 保持 OPEN 的范围

原 SOURCE14、完整原70/P0–P10、整个 S4 全部保持 **OPEN**。Generic 最终 upperBounds、Alias 具体使用槽、严格 capture 的已知 OPEN 未用于替代本轮五项审查，也未重新编号成拒绝项。本报告不签发 S5/S6 或正式 frontend 完整余额。

## 候选字节身份

路径相对唯一工作树；SHA-256 为实际文件字节。此表同时包含读取的旧报告，确保旧证据未被覆盖。

| 文件 | SHA-256 |
|---|---|
| `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt` | `17AF24068B674B17A7709B6460408B3BFF8BEE7559D96942D6AE6853ADF01307` |
| `cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeClassLikeType.kt` | `9CCC7F5CA06B52B2F21623763F97FADCD19D4CC48A3994770AE894FF89DD92F8` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeTypeContext.kt` | `61B7AF180C748E42DCB129A01F6786EA89C3D26B847948DD3CDFB792A3446D37` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeInferenceContext.kt` | `30D4E146B195A2093C260776394DD2E6AF7F8005E6933A3A284D1F5D75929FAA` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt` | `80FC34204B0F41330E3846032EB7A82ECBA7CFF6ADB0049A872CECA48C48F689` |
| `common/src/org/cangnova/cangjie/type/model/TypeSystemContext.kt` | `3B2AD200EEFDB410F530A0CDA32889A881172592D9061332212FDB9A5E2EB5AA` |
| `common/src/org/cangnova/cangjie/type/AbstractTypeChecker.kt` | `DB299F0A9CA0761ED954441EC4A8C82822D1C2AA72E3A7481B64134857C52620` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/scopes/impl/CfirClassSubstitutionScope.kt` | `84C31104CD6A33BCBA69B47ECDB0675422570E4FECFB5A59ED399560CD96DBF7` |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt` | `3E635CAAF37449E5A87BA5BF6D43A9B5D56F285EADEFF93B549EE0E9861D3CC7` |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt` | `40A03E47A209D13B85A6A04183638E450189066BD2F7BD383353EF68E0314069` |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/overloads/ConeOverloadConflictResolver.kt` | `6DC1D5AAC6B61A6553A731B47082B6BD1BB4814BFB79B95FD97529185086F030` |
| `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt` | `07D2BBE3732C5D84A0454FB8746C62D28B94F2F318DB1436BC4856FA3670F0A0` |
| `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypePayload.kt` | `DDCE9F6021CA1F7F2EFF8DB89E421D81A4C4C6E050E39A822FF03EC468072BE4` |
| `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoCanonicalSourceEncoder.kt` | `CCF0BE8D979ECCCE7621415DEF15A5D74904446575811AB83F2423E581C33330` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirSourceGraph.kt` | `D60405145916E5527CD25567EFAD6B915EE31388BE7DC46CEDBF4448A83AB6C6` |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt` | `59353ED8E6A22192B4CA12AE9DB5A91C7F62FD841AC0E5C1978D61A6BFE6CB08` |
| `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirTypeDeserializer.kt` | `849B1F7CC76C259E7EDF0D3344063B426EF8847FD4282D148F8E54C4D1242016` |
| `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/CfirTypeRelationsTest.kt` | `44464BA61AD12A342945C533281A184287910EBEE96B82DFDC04EB07571A31FD` |
| `cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjo/CjoRawArrayLayoutTest.kt` | `F70BF31345FCF65698F9178A251EF2BD3A54EC0F8A83EDF7DB2FD19C275A8B70` |
| `docs/cjmp-source-helper-20261117/source-frozen-encoder-gate-r1-r6-recheck-20261118.md` | `2CB26E0E8BBE567ED40BAD4A5DFB3BA248E75F058E8B0FF6230C046715949080` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendSubstitution.kt` | `C844AF74D50BEAE963CC3D910D6E9710B33DBC029148580E4B1B6A3C195AEC87` |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirTypeAwareSupertypeProviderImpl.kt` | `2D87C4ABB4DB0BE87B92225E44BC388B305D5A61BC99199FAE924A9418860B7F` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/declarations/CfirAnnotationExportPlan.kt` | `778F8003701DAD23812BACB173FE7B30ED5EDBE2B9FFE7ABE48AF3044AD1BAFE` |
| `resolution.common/src/org/cangnova/cangjie/resolve/calls/inference/components/ResultTypeResolver.kt` | `D2F737B93B88C510FF336C4820C9A355B1DFF3B71F9A204C6F6D10523BDDC673` |
| `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/providers/CfirTypeAwareSupertypeProviderTest.kt` | `49F766815CC7A02CDE61994330F47414BCDD1CFEFEBC7F7EA2BC21AE3C943AF7` |
| `cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CfirSourceExpressionExportTest.kt` | `5A6BEF26F21369FFC9356B3FF11126D0BEECD9430263EAC4F5E3F7D9FDFF75B1` |

验证限制：仅固定对象考证、生产/测试静态读取及调用流推演。未执行构建、编译器或测试；本报告中的具体输入说明可复核的代码行为，不伪装为运行日志。

