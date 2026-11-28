# 有序类型输入的固定 Kotlin 考证

**S4 拒绝直接进入编码；先完成不可变 typed payload 和发布前验证。** 已按指定 Kotlin/native 固定对象核对。指定映射文档在唯一工作树内未找到，无法声称已复用。

| 职责 | Kotlin 确切方法及 ownership | 本地约束 |
|---|---|---|
| 类型、投影、alias | `fir/fir-serialization`，`org.jetbrains.kotlin.fir.serialization`：[FirElementSerializer](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-serialization/src/org/jetbrains/kotlin/fir/serialization/FirElementSerializer.kt#L984) 的 `typeId`、`typeProto`、`typeOrTypealiasProto`、`typeArgument`、`typeAliasProto` | providers 捕获完整类型事实；serialization 消费冻结产品 |
| 泛型绑定 | 同类的 `typeParameterProto:938`、`createChildSerializer:1286`、`getTypeParameterId:1381`；子 serializer 继承 binder interner、共享 type table | 冻结真实 TP 身份及固定 common→specific 投影，不能随 active owner 重算 |
| 类型 shape 与正文编码 | `ir/serialization.common`，`org.jetbrains.kotlin.backend.common.serialization`：[IrFileSerializer](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IrFileSerializer.kt#L375) 的 `serializeSimpleType`、`serializeTypeArgument`、`serializeIrType`、`toIrTypeDeduplicationKey` | shape/类型表由 encoder 拥有；有序实参逐槽编码 |
| 声明绑定编码 | `IrFileSerializer.serializeIrSymbol:312` → `DeclarationTable.signatureByDeclaration` → `IdSignatureSerializer.protoIdSignature:95` | 类型绑定与声明身份计算分属各自 owner；保留 imported 身份 |
| SOURCE self type | `raw-fir.common`，`org.jetbrains.kotlin.fir.builder`：[AbstractRawFirBuilder](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/raw-fir/raw-fir.common/src/org/jetbrains/kotlin/fir/builder/AbstractRawFirBuilder.kt#L150) 的 `Node.toDelegatedSelfType(typeParameters,symbol)`、`registerSelfType:426`；PSI 调用点 `1939`、LightTree `660` | self type 按声明 TP 顺序构造，再冻结 |
| 已完成声明的默认类型 | `semantics.api`，`org.jetbrains.kotlin.fir.resolve`：[ScopeUtils.defaultType](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/semantics.api/src/org/jetbrains/kotlin/fir/resolve/ScopeUtils.kt#L178) | 区别于 tree 层 `constructType`：后者默认空实参，不自动构造泛型 self |

具体拒绝：

- [captureType](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt)（L749） 将 children 按身份去重；[CfirExportTypeElement](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourceElement.kt)（L49） 有绑定字段，但没有有序语义 payload。**拒绝用 children 重建参数、tuple 或实参。** payload 必须分别保存这些有序列表、return、绑定、C/variadic/closure/This、alias 原始与展开视图、raw dims；Generic 语义上界与 constraint 节点分别保存。
- `validateUnchanged` 当前未遍历类型记录。必须验证列表长度、顺序、重复身份、全部标记和绑定及闭包完整性；不可变列表须复制后封装。
- [declarationType:795](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt)（L795） 使用无参 `constructType()`，会丢泛型 self 参数。**拒绝冻结这个空实参结果作为声明 self type。**
- [SourceGraphProducer:408、412](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt)（L408） 两处四参调用确实错位：第四参是 `isClosureType`，variadic 是第五参。须使用命名参数；native SOURCE 合同要求 closure 为 false，variadic ⇒ C。
- [typeReference:3047](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L3047） 丢弃冻结查询结果后继续读 live 类型；RawArray dims 写死 `1`，SOURCE composite 未传 This，alias 无编码分支。**这些路径均不满足冻结消费。**

shape-key 回环策略：依照 Kotlin，将 classifier/TP 绑定作为身份叶子，不递归展开其声明 self type、owner 签名或 bounds；保留有序子槽及全部语义标记。身份 memo 先登记，native `ASTWriterImpl.SaveType:944` 的 **reserve→递归→fill** 处理回边，全部槽完成才发布。shape 去重只能作第二索引，不能替代 source 同代身份或 imported `(snapshot, TYPE, rawIndex)`，也不能使用当前 Cone `equals/hashCode` 充当完整冻结键。

本次只读，未改文件、未跑 Gradle、未联系或停止旧会话。
