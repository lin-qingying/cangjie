# 冻结 encoder R1–R6 复审（2026-11-18）

**本轮门禁：REJECT。R1、R3、R4、R5 仍有具体阻塞；R2、R6 的旧 finding 已关闭。完整 S4 保持 OPEN。**

本报告独立于 [ECD44775 候选的旧报告](source-frozen-encoder-gate-r1-r6-20261118.md)，仅评审当前修订及其新边界，不改写旧结论。当前 Producer 候选 SHA-256 为 `9A918FE0BFACBFA2780181AE9F760C3BF3056035BE6D321F03DF72F987D48359`。下文当前代码行号绑定此候选；其余关键文件的字节身份列在文末。

唯一一方工作树：`本记录所指的唯一实现树`。Kotlin 权威对象固定为 `3c7a5041f238102285764d370665f56f587f9dc4`；native 权威对象固定为 `14276740ffec1262b81b6b5236b1d840af270b37`，均位于 `external`。上游源码及语义 fixture 证据来自这些固定对象的 `git show` / `git grep`，没有把镜像当前 checkout 当作权威；上游链接仅供定位固定对象中的对应文件。

## 逐旧 R1–R6 的裁决

| 旧项 | 原问题的修订情况 | 本轮裁决 |
|---|---|---|
| R1 | 普通 Nominal/Alias 已直接使用冻结声明绑定；This/Interface/Alias 不进入 self 双 memo。原“只换 classifier、留下原 own TP”路径已消除。 | **REJECT：F1**。self 分支仍把 native 的条件复用改成无条件选择 specific 产品。 |
| R2 | `sourceExpressionTarget:2987` 先检查冻结的 `isImported`，再查本地及 raw 索引。 | **旧 finding 关闭**。已有 raw 本地槽不能再覆盖真实 imported FullId。 |
| R3 | GenericTypeAnchor 保存 parameter 身份、捕获 domain、固定 projection；fill 只读取 `genericTypeInputs`，不再查询声明域 payload。 | **REJECT：F2**。Generic bounds 仍在结构 DFS 内立即准备，合法回边可被当作结构环。 |
| R4 | loader 读取并校验 `dimsOrSize`；Cone 保存 `rawArrayDimensions`；TypeUtils、inference.replaceArguments、ideal copy 已透传该标量。 | **REJECT：F3、F4**。两个现存复制入口仍退回一维，类型检查也未接入维度语义。 |
| R5 | semantics.api 准备注解计划；Frozen/TestRegistration 为空参数；SourceGraph 只生产计划实参。S0 的 parser-owned syntax 域保留原语法边并跳过普通表达式完成性要求。 | **旧 parser-owned finding 关闭，但整体 REJECT：F5**。计划的引用仅进入捕获闭包，没有进入实际保存闭包。 |
| R6 | source-shaped CJO origin 声明和 raw CJO 声明统一调用 `importedAnnotations`；原 `arg.expr` 经 imported snapshot 编码进入共享表达式池。 | **旧 finding 关闭**。不再要求 common CJO 注解具有源码 canonical identity。 |

这里的“关闭”仅针对旧 finding 的具体触发路径，不等于模块、S4 或完整 SOURCE 流水线已通过。

## F1 — [P1][R1] self 的条件双 memo 被改成无条件 specific 选择

当前 [frozenTypeShape 的 self 分支](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L3056） 在确认 self 和 specific binding 后，直接递归消费 `specificType`，并把原 self 的 shape 缓存成该结果。此处没有检查 specific 的类型槽是否已经存在。

固定 native [TryGetSpecificImplementationTy / SaveType](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L893) 的行为不同：

- L935–940：只有 specific Ty 已在 `savedTypeMap` 时，才将当前 self 复用为那个已有槽。
- L947–954：双方均未保存时，为输入 `pType` 预留一个槽，同时登记输入和 specific 两个身份。
- L956–963：该槽的正文继续按输入 `pType` 编码；`SaveNominalTy:995` 读取它自己的声明绑定和有序实参。

因此在“先请求 common self，specific 尚无槽”的具体顺序下，native 登记双身份但写原 self，当前候选则写 specific self。classifier、own TP 以及 Generic 捕获域均可能随这个无条件切换改变。修订已解决旧的 classifier/TP 混搭，但这仍不是固定 native 的双 memo 调用流。

**改法：**将 self 身份确认与类型槽复用分开。按 native 的已有 specific 槽检查、双方身份预登记、输入正文编码顺序实现共享表。若必须在某一语义模式下始终选择 specific，应由 resolve/family 的共同 owner 先发布该投影后的真实输入，并给出该模式的固定语义证据；不能在 encoder 内替换输入后称其为 native 条件双 memo。

## F2 — [P1][R3] 从结构上界进入时，Generic 回边仍被拒绝

当前 [Generic 准备分支](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L3074） 先 memo Generic shape，但随即在当前 DFS 中执行 `payload.explicitBounds.map { frozenTypeShape(...) }`。外层 Nominal shape 直到子槽结束后才缓存，`visiting` 中仍保留它。

一个具体的完成输入是：函数 `f<T>(): Unit` 的签名不含 T，而 T 有上界输入 `I<T>`。当前 [genericMetadata](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L1776） 先编码该上界，之后才编码被约束参数自身的类型，准备顺序为：

`I<T> → T（登记 GenericAnchor）→ T.bounds 中同一个 I<T> → visiting.add(I<T>) 失败`。

这是穿过 Generic binding 的合法回边，仍会触发 L3085 的 `A completed type has a structural cycle outside its Generic binding`。Generic 先作为根进入时可能成功，不能证明不同入口都安全。

固定 native [SaveType:944–954](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L944) 在递归类型细节之前登记实际 Ty 槽，L1015–1021 的 Generic 上界沿此 memo 回返。固定 [assumption1.cj](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/unittests/Sema/SemaCangjieFiles/AssumptionTest/assumption1.cj#L17) 也保留 `T <: E<T>` 及相互 nominal 约束；这种 nominal/Generic 回边不等于非法纯结构环。Kotlin [typeParameterProto:938–963](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-serialization/src/org/jetbrains/kotlin/fir/serialization/FirElementSerializer.kt#L938) 同样将 TP binder 与 bound 类型编码分开。

**改法：**Generic DFS 只登记锚点并排队上界输入。当前结构 shape 完成后，再独立排空同域上界准备队列；所有 `genericTypeInputs` 完成后才进入类型表 fill。继续保留 Generic 作为 shape 身份叶子，不把 bounds 递归纳入驻留键，也不把穿过 Generic 的回边归为非法结构环。

本项只审查回环准备顺序，不以尚未发布最终语义 upperBounds 替代该 finding。

## F3 — [P1][R4] 两个真实复制入口仍把已装载的维度退回 1

新 loader 的 [convertArrayType](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirTypeDeserializer.kt)（L219） 已读取 `ArrayTyInfo.dimsOrSize`，旧的直接装载丢维度问题已消除。但以下两处仍逐字段构造 ConeClassLikeType，遗漏新标量：

- [CfirCallResolver.normalizeFreshReceiverTypeArguments](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt)（L2734）：保留 lookupTag、实参、attributes、isInterface、isThisType，未传 `rawArrayDimensions`。
- [ConeOverloadConflictResolver.restoreDeclarationTypeParameters](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/overloads/ConeOverloadConflictResolver.kt)（L1209）：同样遗漏该字段。

两者对所有 ConeClassLikeType 都执行重建；即使实参本身无需替换，输入 `RawArray(Int64, dims=2)` 也会变成默认的一维。它们分别处于真实 receiver 规约和 callable reference most-specific 比较路径，不能仅验证 TypeUtils/inference.replaceArguments/ideal copy 就宣布复制闭包完整。

固定 native [TyInstantiator::GetInstantiatedArrayTy](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L279) 及 [SubstituteTypeArgs](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L2359) 在替换元素时继续传递原 dims。

**改法：**这些恢复、归一化路径复用已经保留全部类型标量的共享重建 API；统一核对所有以已有 ConeClassLikeType 为模板的复制入口。不能仅在 encoder 上补维度，因为维度已经在候选选择之前丢失。

## F4 — [P1][R4 新边界] 类型检查仍把不同维度视为同一 RawArray 类型

当前 [ConeInferenceContext.typeConstructor](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeInferenceContext.kt)（L134） 对 RawArray 仍只返回 lookupTag；[ConeTypeContext.identicalArguments](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeTypeContext.kt)（L83） 只比较实参列表。[AbstractTypeChecker](../../common/src/org/cangnova/cangjie/type/AbstractTypeChecker.kt)（L389） 的结构类型分派没有 RawArray 维度规则，同构造器分支最终在 L571 以 identicalArguments 返回 true。

因此同一 RawArray classifier、同一 Int64 叶子，分别携带 dims=1 与 dims=2 的两个完成 Cone，会经过同构造器、同实参路径，被双向 subtype/equalTypes 当成相等。新增 Cone.equals/hashCode 无法修复这条路径：`AbstractTypeChecker.equalTypes:189–195` 使用双向 subtype。

反向也存在表示不一致：源码逐层 `RawArray<RawArray<Int64>>` 和 wire 装载的单叶子 `RawArray(Int64, dims=2)` 拥有相同 native 布局，但当前类型实参查询仍直接返回物理实参列表，无法统一这两种完成表示。

固定 native [IsArraySubtype](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L915) 明确要求累计 dims 相同、叶子类型 IsTyEqual；[GetTypeArgs](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L1876) 在 dims>1 时返回降一维的数组参数，区分原生布局与语言层泛型实参视图。

**改法：**在共享类型系统接口和类型检查流程中接入已完成 RawArray 布局，统一累计维度、叶子比较以及语言层参数视图。必须同时做到不同维度不相等、同布局的源码/装载表示相等，不能让字段仅用于序列化和普通 equals/hashCode。

## F5 — [P1][R5 新边界] 注解计划未接入实际保存的引用闭包

新计划的目标和实参已经进入 [captureCanonicalSource](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt)（L476）：target 被捕获，参数被确认存在于图中。但这只是输入闭包。

当前 [collectNeededSourceDeclarations](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L607） 只遍历声明 header 和已选正文：

- header 在 L623 跳过所有 CfirExpression，包括 CfirAnnotationCall；
- L629–638 的正文列表没有加入 `annotationPlans` 的实参或目标；
- CfirExportAnalysis 也没有处理新 `annotationPlans`，已有 `annotationSourceExpressions` 仅覆盖 class 的 @Annotation 目标参数。

因此，对一个已经选中写出的声明，只要其可写 custom plan.target 是未被 root/body 选择收录的本包构造器，target 虽已被 S0 捕获，却没有输出声明槽。[annotations:2922](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L2922） 在最终写 target FullId 时仍会进入 `Resolved CJO expression target is outside the emitted declaration graph`。非 const/frozen、未选择 SOURCE 正文的普通导出函数尤其不会借正文遍历偶然补齐这条边。

固定 native [SaveAnnotations:1727–1736](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L1727) 对 custom target 调用 GetFullDeclIndex；[GetFullDeclIndex:791–807](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L791) 对本包目标继续 PreSaveDecl，保证真实注解引用进入保存图，不依赖宿主声明是否写正文。

**改法：**在 metadata 生成前，对实际写出声明的已冻结注解计划遍历 target 和参数图中的类型/声明引用，接入同一保存闭包及 owner 登记。真实 imported 目标仍按 R2 保留外部身份；只消费实际写出声明的计划，不能用全部捕获计划无条件扩大导出域。

## 未作为本轮拒绝替代项的余额

Generic 最终语义 upperBounds、Alias 具体使用槽选择及严格 capture 仍 OPEN；完整 S4 未交付。本报告没有把这些既知余额重复编号为 R1–R6 的修订失败，也未声称完成 S5/S6 或正式 frontend 全余额。

## 字节绑定与验证范围

末次复读以下十个关键文件均与首次审查读取内容一致。SHA-256 为随后同一候选的实际文件字节摘要：

| 文件（相对唯一工作树） | SHA-256 |
| `cfir\cfir-serialization\src\org\cangnova\cangjie\cfir\serialization\cjo\CfirCjoPackageMetadataProducer.kt` | `9A918FE0BFACBFA2780181AE9F760C3BF3056035BE6D321F03DF72F987D48359` |
| `cfir\providers\src\org\cangnova\cangjie\cfir\resolve\providers\CfirExportAnalysisInputSnapshot.kt` | `07D2BBE3732C5D84A0454FB8746C62D28B94F2F318DB1436BC4856FA3670F0A0` |
| `cfir\cfir-cones\src\org\cangnova\cangjie\cfir\types\ConeClassLikeType.kt` | `F889AEE29BC495B66357523AB72233CA1609B6087FEA6553F643815B05729202` |
| `cfir\semantics.api\src\org\cangnova\cangjie\cfir\declarations\CfirAnnotationExportPlan.kt` | `778F8003701DAD23812BACB173FE7B30ED5EDBE2B9FFE7ABE48AF3044AD1BAFE` |
| `cfir\resolve\src\org\cangnova\cangjie\cfir\resolve\source\CfirSourceGraphProducer.kt` | `59353ED8E6A22192B4CA12AE9DB5A91C7F62FD841AC0E5C1978D61A6BFE6CB08` |
| `cfir\semantics.api\src\org\cangnova\cangjie\cfir\types\ConeInferenceContext.kt` | `67F66EE772B1AC7CB90FD8618EAC145A41B2501EE60BB05B0275741E1367E1F0` |
| `cfir\resolve\src\org\cangnova\cangjie\cfir\resolve\body\CfirCallResolver.kt` | `2130AB88FE645EC49C178656F684B4DE1CA37A76B890695CD424F675588E7A29` |
| `cfir\resolve\src\org\cangnova\cangjie\cfir\resolve\calls\overloads\ConeOverloadConflictResolver.kt` | `DAFEFDB0AF95F38497FB3760825B43B5C1EC0C776AD801B9D96077D2A6F66689` |
| `common\src\org\cangnova\cangjie\type\AbstractTypeChecker.kt` | `E64DDD64C814F1FEF14D5985BDCA7E2F2F20CC12EDF8F2F53C392A96E9BE91C6` |
| `cfir\cfir-serialization\src\org\cangnova\cangjie\cfir\serialization\deserialize\CfirTypeDeserializer.kt` | `0943B371A3FF06162896CC6B90AC1BAA56BC52933D1F714A573A8129336D79D9` |

验证方式为静态源码考证、固定 Git 对象证据及入口顺序推演；没有运行 Gradle、cjc 或任何测试，因此上述例子是源码触发路径说明，不是运行结果。本轮没有修改生产或测试文件，唯一写入是本报告；没有联系或停止旧会话。

