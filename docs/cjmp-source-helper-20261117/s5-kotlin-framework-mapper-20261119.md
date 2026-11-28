# S5 KotlinFrameworkMapper：类型身份优先与 SOURCE 恢复框架

固定 Kotlin：`3c7a5041f238102285764d370665f56f587f9dc4`，参考仓库 `D:/code/intellij/cangjie/external/kotlin`。所有 Kotlin 实现证据通过 `git show <commit>:<path>` 读取；以下行号均属于该提交，不属于当前工作区。先读了 [type-use continuation](../cjmp-source-type-use-continuation-20261119.md) 和 [source-chain-delivery](../cjmp-source-chain-delivery-20261116.md)。随后读取已有 [类型 decoder 合同](source-type-decoder-materialization-map-20261119.md) 作任务上下文，未把该报告对当前生产代码的描述当成自己复验的结果。

按主线程最新指令，首先给出 `TypeKind.Type`、递归 Generic、跨包 canonical symbol、own/outer TP 与全部 GenericParams reserve 后填 bounds 的映射。本文只写映射报告；未读取或修改本库生产/测试实现，未运行 Gradle/cjc，未联系旧会话。CJO 特定字段含义沿用指定交付文档与已有类型报告，本轮只核验 Kotlin 框架，不独立重验 native 语义。

## 1. 可立即实现的类型链与职责

直接对位对象是已有文档所指的 `CfirDeserializationContext`、`CfirDeclDeserializer`、`CfirTypeDeserializer`、`CjoFullIdResolver` 及正式 deserialized provider。应按 **context registry → 声明/TP identity → 类型引用 → bounds/RHS/header completion → 正式使用产品** 接线。以下接口名是建议的合同名，不声称当前仓库已有这些方法。

| 建议接口/所属职责 | 输入与输出合同 | 固定 Kotlin 对位 |
| --- | --- | --- |
| registry：`contextForSnapshot(snapshotIdentity)`、`resolveDeclarationIdentity(fullId, expectedKind)` | 对实际已加载快照和依赖选择结果路由。保留包、表域、代际；找到 target 的真实 owner context，再获取该 context 的 canonical symbol。不能按名字或第一个 parent 新造声明。 | `KotlinIrLinker.createAndRegisterModuleDeserializer` L328；`IrSymbolDeserializer.deserializeSymbolWithOwnerMaybeInOtherFile` L61。 |
| DECL/CTX：`reserveDeclarationIdentity(declId)` | 先确定声明 kind、真实 symbol 和 owner，登记身份；此阶段不展开 Alias、不递归读 bounds/body。返回内部 identity handle。 | `IrDeclarationDeserializer.withDeserializedIrDeclarationBase` L241；`IrSymbolDeserializer.deserializeSymbolToDeclareInCurrentFile` L47。 |
| DECL/CTX：`reserveOwnTypeParameters(ownerId, orderedGenericParams)` | 一次登记 owner 的**全部**实际 TP 身份与有序 own 列表；绑定 TP 的 containing declaration。没有 own 参数的 owner 返回空 own 列表，不能复制 outer 参数作为新的 own 声明。 | `FirTypeDeserializer.init` L62，第一遍 L67–85；`FirDeserializationContext.childContext` L61。 |
| CTX：`childContext(ownerIdentity, ownParameters, outerContext)` | own 与 outer 是不同关系。outer 引用真实上层 TP symbol；可见性、捕获规则取 CJO 所存 owner/语言合同。 | `FirDeserializationContext.allTypeParameters` L58；`FirTypeDeserializer.forChildContext` L101；`ClassDeserialization.deserializeClassToSymbol` L94、L141。 |
| TY：`decodeRequiredType(typeId)`、`decodeOptionalType(slot)` | 必需与可缺省槽分别表达。同一 type memo 域内复用；Generic 类型只引用已登记真实 TP，nominal/Alias 类型先引用真实 classifier，再读有序 arguments。 | `FirTypeDeserializer.rigidType` L212；`IrDeclarationDeserializer.deserializeIrType` L101、`deserializeSimpleType` L144。 |
| DECL/TY：`fillGenericBounds(ownerIdentity)` | 全部 own TP reserve 后再读 final bounds；递归碰到 TP 返回原 identity，不在 TP 类型构造中继续展开 bounds。原约束组和 final semantic bounds 是独立 CJO 产品，不能彼此代替。 | `FirTypeDeserializer.init` 第二遍 L87–95；`rigidType` L214–220。Kotlin metadata 不提供 CJO 原约束组位置/implicit 的完整对位。 |
| DECL/TY：`fillAliasHeader(aliasIdentity)`、`completeAliasApplication(typeId)` | `TypeKind.Type` 先取得实际 Alias identity 与原 arguments，再填实际 own TP/RHS。Alias self、原 RHS、使用位置展开结果分别保存；identity 获取不调用 RHS 展开。展开必须消费已完成 header。 | `AbstractFirDeserializedSymbolProvider.findAndDeserializeTypeAlias` L269–280；`FirMemberDeserializer.loadTypeAlias` L216–245；`FirTypeDeserializer.rigidType` L252–255。 |
| CTX：`completeHeader(declIdentity)` | bounds、signature、Alias RHS、必需 refs 完成后，才把 header 作为正式可消费产品发布；内部 reserve 结果不等于完整 declaration。 | `FirCachesFactory.createCacheWithPostCompute` L50–70。完成门是 CJO 所需扩展，不是 Kotlin 已有同名状态。 |

S5 类型的第一段应完整接到正式 provider，不能只让 `decodeType` 的某个分支通过。provider 首次请求 Alias/type/成员时，也必须经同一 registry 与 owner reservation；否则“先读取 owner 的调用顺序”仍会掩盖身份问题。

## 2. own/outer TP：固定源码精确映射

以下表中模块为固定 Kotlin Gradle project ID，文件均可在固定提交定位。

| module | file / method / 行号 | 已核验行为与适用差异 |
| --- | --- | --- |
| `:compiler:fir:fir-deserialization` | [`FirTypeDeserializer.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-deserialization/src/org/jetbrains/kotlin/fir/deserialization/FirTypeDeserializer.kt#L62)，`init` L62–99 | `LinkedHashMap<Int, FirTypeParameterSymbol>` 保留 own 顺序。第一遍为有 proto ID 的参数建立 symbol/builder，记录 `containingDeclarationSymbol`，登记 `result[proto.id]`；第二遍才 `type(upperBound)`、补 metadata 默认 bound、`build()`。这是**全部 symbol reserve 后 bounds fill**的直接依据。第一遍 builder 尚未 build 成完整 TP 节点，不能宣称此时所有 `symbol.fir` 都能读。 |
| 同上 | 同文件 `typeParameterSymbol` L197；`typeSymbol` L336 | ID 查询是 own map 再 parent 链。`typeParameterName` 是单独 metadata 分支，只查本层 name map；它不是跨包 TP 合同，也不新造 symbol。CJO 应用保存的 FullId/owner 身份，不移植 name 分支为兜底。 |
| 同上 | 同文件 `rigidType` L212–220 | TP constructor 转成 `ConeTypeParameterType`，保留 lookup tag；普通分支不访问 TP bounds。合法 `T → I<T> → T` 从 classifier/TP 身份边断开 loader 无限递归。不是通过“发现正在 decode 就给 Error/Any”解决。 |
| `:compiler:fir:tree` | [`symbols/ConeTypeParameterLookupTag.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/src/org/jetbrains/kotlin/fir/symbols/ConeTypeParameterLookupTag.kt#L13)，`ConeTypeParameterLookupTagImpl` L13 | lookup tag 内持有实际 `FirTypeParameterSymbol`，不是字符串名称。tag 的 `name` 会访问 symbol，因此内部未完成期也不能任意做名称/完整声明查询。 |
| `:compiler:fir:fir-deserialization` | [`FirMemberDeserializer.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-deserialization/src/org/jetbrains/kotlin/fir/deserialization/FirMemberDeserializer.kt#L41)，`FirDeserializationContext` L41–92，`childContext` L61 | own 为 `typeDeserializer.ownTypeParameters`；`allTypeParameters = own + outer`。child 的 type deserializer 总是以原 type deserializer 为 parent；outer 列表由 `capturesTypeParameters` 决定。**parent 查找链与捕获列表不是同一开关**。不能把“parent 存在”解释为自动拥有 outer TP。 |
| 同上 | 同文件 `createForClass` L123；`createRootContext` L153 | 根 class context 用真实 class symbol 作为 containing declaration，type table 与 name resolver 来自该 class 的 metadata。根 context parent=null、outer 列表为空。CJO 也要按实际 owner 表域建 context，不能把某个 decoder 的当前栈当成永久归属。 |
| 同上 | [`ClassDeserialization.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-deserialization/src/org/jetbrains/kotlin/fir/deserialization/ClassDeserialization.kt#L94)，`deserializeClassToSymbol` L94–121、L141–148 | 已传入实际 class symbol；nested class 从 parent context 创建 child，自身 TP 先恢复。`status.isInner` 才向 class `typeParameters` 加 `OuterClassTypeParameterRef`，所引用仍是 parent 实际 symbol；然后读 supertypes。仓颉是否有对应捕获语义由实际合同决定，不照搬 Kotlin inner/static 分类。 |
| 同上 | `FirMemberDeserializer.loadFunction` L649–713 | 先创建 function symbol L668；child context L669 完成自己的 TP；返回类型/receiver 在 local context 读取，function 自身 typeParameters 只加 local own L712。outer class TP 由 context/dispatch receiver 使用，不重新声明为 function own。 |
| 同上 | `FirMemberDeserializer.loadTypeAlias` L216–245 | 先选择 `preComputedSymbol` 或实际新 Alias symbol，再建 own TP context，最后读 expandedType。Alias 的 own TP 归 Alias symbol；不能归当前使用 Alias 的函数。 |
| 同上 | `FirMemberDeserializer.loadConstructor` L758–815 | constructor child context own TP protos 为 empty；返回 self type 使用 classBuilder 的实际 TP symbols；constructor `typeParameters` 为 `ConstructedClassTypeParameterRef`，没有重新创建这些 TP。CJO 必须按自己的 GenericParams 与真实 owner 恢复，不能无条件照搬 Kotlin constructor 返回推导。 |
| `:compiler:ir.serialization.common` | [`IrDeclarationDeserializer.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IrDeclarationDeserializer.kt#L265)，`deserializeIrTypeParameter` L265–319；`deserializeTypeParameters` L449–455 | 先 reference/declare 某个 TP，再为该 TP 填 `superTypes`；整体循环是**逐参数 create+fill**，不是 FIR 的全量两遍。前向参数依赖由 `IrSymbolDeserializer` 的可未绑定 canonical symbol 承担。S5 全部 GenericParams reserve 的直接模板应取 FIR init，不能引用 IR 此方法声称两遍。 |
| `:compiler:ir.tree` | [`util/SymbolTable.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/util/SymbolTable.kt#L366)，`declareGlobalTypeParameter` L366；`declareScopedTypeParameter` L374；`referenceTypeParameterImpl` L392 | global TP 使用 symbol-table slice；`declareScopedTypeParameter` 明确不缓存，private reference 也创建新 symbol。IR 中 file-local 的最终复用靠 `IrSymbolDeserializer.deserializedSymbolsWithOwnersInCurrentFile`。不能单独照搬 `SymbolTable.referenceTypeParameter` 就认为 local TP canonical 已解决。 |

## 3. 跨包 canonical 与 Alias：不能把类型缓存当 binder

| module / file / method | 固定证据 | S5 合同与不适用边界 |
| --- | --- | --- |
| `:compiler:ir.serialization.common` / [`IrSymbolDeserializer.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IrSymbolDeserializer.kt#L20) / L20–95 | file-scoped signature→symbol map L30、signature-index cache L34。声明入口 L47 登记同一 symbol；引用入口 L61 按 signature ID 缓存。非 public L70–76 复用本 file map并排队 top-level owner；public L79 经 linker 路由。 | 类型、声明与正文引用必须共用 binder；CURRENT raw index 的身份不能脱离 snapshot/table domain。跨包 TP 取 target owner 的已 reserve 参数。导入/重导出引用不改变 target 的所有权。 |
| 同模块 / [`IdSignatureDeserializer.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IdSignatureDeserializer.kt#L36) / `deserializeIdSignature` L36、local/composite L64–85 | signature cache 属于 reader 实例；FileLocal signature 包含 container，Composite 包含 container+inner，FileSignature 由调用方 file symbol 给定。 | CJO FullId 不是 Kotlin IdSignature 的 wire 格式，但“身份包含归属域，而不只索引/名称”适用。不能把同 raw index 的不同包/新加载代混在全局 memo 中。 |
| 同模块 / [`KotlinIrLinker.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/KotlinIrLinker.kt#L328) / `createAndRegisterModuleDeserializer` L328–362 | library→deserializer 一次注册，并建 package→module deserializer 索引、记录 module dependency；public symbol 路由 L124–184。 | registry 必须在 refs fill 前可用。Kotlin 的“优先当前模块，再第一个匹配”L137–164 带有固定源码明确说明的重复库歧义/已绑定错误 L145–154，**不适合作为 CJO 多快照依赖选择策略**。CJO 使用实际 provenance/依赖计划。 |
| `:compiler:fir:fir-deserialization` / [`AbstractFirDeserializedSymbolProvider.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-deserialization/src/org/jetbrains/kotlin/fir/deserialization/AbstractFirDeserializedSymbolProvider.kt#L177) / `typeAliasCache` L177–185，`findAndDeserializeTypeAlias` L269–280 | 顶层 Alias 的 createValue 返回真实 symbol + postProcessor；缓存后 postProcessor 用**同一个 preComputedSymbol**执行 `loadTypeAlias`。 | `TypeKind.Type` 应拆 identity reserve 和 header/RHS completion。这里只直接证明顶层 Alias 分支；nested Alias L254–260 是直接 load 后返回，不声称所有 declaration 都已统一两遍。 |
| 同模块 / 同文件 `getClass` L370–387；`getTypeAlias` L398–419 | 需要 parentContext 的 nested class 先查已计算缓存，否则先请求 outer class，再从 classCache 取得 nested；nested Alias 同样先加载 parent。 | 首次从 nested/TP 引用开始也要恢复实际 owner，不能要求用户/测试预热。Kotlin class metadata 分支 L293–313 在 createValue 中已调用 `deserializeClassToSymbol`，不是所有 class 都“先缓存空 symbol 再填”。CJO 必须明确自己的 identity pass。 |
| `:compiler:fir:tree` / [`caches/FirCachesFactory.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/src/org/jetbrains/kotlin/fir/caches/FirCachesFactory.kt#L50) / `createCacheWithPostCompute` L50–70 | createValue→放缓存→postCompute；同线程 postCompute 重入可见同一值。createValue 内禁止 cache.getValue：同 key 递归不允许，跨 key 递归可能死锁。 | reserve 是不递归的身份分配阶段；跨包 refs/bounds/RHS 读取在后续 fill。不能把完整 `decodeDeclaration` 放进 computeIfAbsent 再等待其它 owner，以为并发 canonical 就解决。 |
| 同模块 / [`FirThreadUnsafeCachesFactory.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/src/org/jetbrains/kotlin/fir/caches/FirThreadUnsafeCachesFactory.kt#L74) / `FirThreadUnsafeCacheWithPostCompute.getValue` L80–85 | 明确先 `map[key] = createdValue`，再 `postCompute`。 | 这是身份发布顺序的直接实现证据；不能当作 LL 并发实现。 |
| `:compiler:ir.serialization.common` / `IrDeclarationDeserializer.deserializeIrType` L101–105，`deserializeSimpleType` L144–156 | type cache 的 getOrPut 在子类型 decode 完成后才写入；classifier 先从 binder 取得，再读 arguments。 | type memo 本身没有 in-progress type 壳；**不是递归 t 表 reserve/fill 的证据**。Generic/Alias declaration identity 回环应由 binder 截断。普通结构 type 的 arguments 有序保留；任意纯结构 t→t 循环是否合法须由 CJO 语义单独判定，不能从 Kotlin 推断。 |
| `:compiler:fir:semantics.api` / [`ToSymbolUtils.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/semantics.api/src/org/jetbrains/kotlin/fir/resolve/ToSymbolUtils.kt#L83) / `ConeClassLikeLookupTag.toSymbol` L83–92；`LookupTagUtils.bindSymbolToLookupTag` L39 | fixed-symbol tag 直接返回 symbol；普通 class-like tag 的已绑定缓存仅在 session 相同才复用，否则调用该 use-site session 的正式 symbol provider。绑定使用 `WeakPair(session, symbol)`。TP tag 则直接持有真实 TP symbol。 | Kotlin class-like metadata 读 type 时可只建立 ClassId tag，稍后 provider 查询才物化；不声称 FIR decode 已把跨包 classifier 全绑定。CJO 若要求完成的冻结 graph，应在正式 materialization 阶段完成身份与依赖，strict capture 中不能首次查 provider。不能将 weak tag 当作跨 session/快照的 canonical registry。 |

`TypeKind.Type` 在 Kotlin 中没有同名 schema 对位。FIR `typeSymbol` L338–340 的 `className/typeAliasName/typeParameter` 是 classifier 选择；`rigidType` L252–255 又保存 `AbbreviatedTypeAttribute`。这支持 Alias 身份、RHS/展开、使用位置缩写角色分离，不能用 Kotlin IR 默认不加载 typealias 的配置删除 CJO 实际 Alias。

## 4. 类型 implementation pass 的具体顺序

1. **registry 与索引**：为实际 loaded snapshots 建 context，建立 FullId→target context/decl index 和 decl→真实 owner 关系。只登记来源与索引，refs 读取不重新打开文件。
2. **声明身份**：根据实际 decl kind reserve owner/class/function/Alias/TP 等 symbol；为本轮依赖闭包建立可重入的内部 identity map。先请求 TP/Alias/type 的入口也调用这个步骤。
3. **own/outer TP**：owner 绑定全部实际 own GenericParams，保持存储顺序；每个 own TP 记录相同 owner symbol。outer 参数只引用祖先 context 的同一 symbol；不能因同名、当前查询者或 own 列表为空创建新 TP。
4. **类型 identity/use**：Generic→真实 TP；Class/Interface/Enum/Alias→真实 classifier；按 wire 读实际 typeArgs 和结构子槽。`TypeKind.Type` 不递归展开 RHS 才能建立 identity。重复 type index 在同代同域复用，跨域按实际来源切换 reader。
5. **填 header**：全部 own TP 已 reserve 后才填 final bounds、原约束组、Alias RHS、继承/签名类型等各自产品；合法递归 Generic 回边只引用 symbol，不触发再次填 bounds。原约束组 subject/RHS/来源的确切恢复仍按 CJO 专属合同，Kotlin 的 bounds list 不足以替代。
6. **完成使用产品**：header closure 完成后计算 Alias 应用的正式使用视图，保留 Alias self/RHS/use 角色，发布给正常泛型 substitutor、scope/query 与 Analysis。strict capture 消费已完成产品，不以其首次展开/查询 provider 来补 completion。

建议内部按 identity-reserved / header-filling / header-complete 记录进度；这些是本报告建议的状态，非 Kotlin 同名 API。filling 期间只能返回已登记 identity 给合法非拥有边。公共 header query 应完成闭包或报告真实解码失败，不能返回半填 bounds、默认 Unit、synthetic TP、错误 classifier。失败不能把半填对象当 completed cache 值。

Kotlin 的 `FirTypeParameterBuilder.addDefaultBoundIfNecessary` 位于 `:compiler:fir:tree` 的 `declarations/utils/FirDeclarationBuildingUtils.kt` L12，补的是 **nullable Any**。CJO 无界参数的 wire final bounds 和正常使用默认上界按仓颉合同分别处理，不能照搬此默认值或补写入原 wire 列表。`computeClassifier` L123 对 metadata local class 使用 Any、IR partial linkage 替换 symbol kind、error type 分支也不适合解决合法 SOURCE CJO 的递归身份。

## 5. 表达式恢复与 header/body 生命周期

本节接续类型优先映射。所有 `IrBodyDeserializer`、`IrDeclarationDeserializer`、`IrFileDeserializer`、`BasicIrModuleDeserializer`、`KotlinIrLinker`、`FileEntryDeserializer` 都属于 `:compiler:ir.serialization.common`，目录为 `compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/`。

### 5.1 非拥有 target 回边与拥有内容

| 固定 file / method | 精确行为 | CJO SOURCE 对位与不适用差异 |
| --- | --- | --- |
| [`IrBodyDeserializer.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IrBodyDeserializer.kt#L122)，`fileLoops` L102、`deserializeLoopHeader` L122 | 按 file 内 loop ID `getOrPut` 缓存同一 `IrLoop`。 | 需要节点身份的非符号 control target 必须有自己的节点 registry，不能把 loop target 当拥有正文再次递归读取。CJO registry 域由实际 d/e 表域决定，不能裸全局整数。 |
| 同文件 `deserializeDoWhile` L807、`deserializeWhile` L816、`deserializeLoop` L793 | 先建真实具体 loop 并放缓存，再读 body 与 condition，最后填 label/condition/body。源码 L805 明确说明这是为了 `IrBreak.loop`。 | 这是“先具体 node 壳，后拥有字段”的直接模板。CJO 如果 break 在 e 表顺序上先于 loop，也应按已索引的实际 target 分配 loop 壳；Kotlin 的遇到 loop header 顺序不等于 CJO 表序。 |
| 同文件 `deserializeBreak` L895、`deserializeContinue` L905 | 直接取得原 loop；未存在时报 `break clause before loop header`，没有按 label 搜索或复制 loop。 | 目标节点身份复用；不通过名称、父链猜测重绑。所存 loop target 错误就失败，不制造额外 loop。 |
| 同文件 `deserializeReturn` L667 | 从同一 declaration binder 取得 `IrReturnTargetSymbol`，然后读取拥有的 value。 | Return.target 是非拥有身份边；应绑定真实 function/lambda/returnable target，不因为“当前正在读取函数”覆盖所存 target。Kotlin Return 的 Nothing type L673 是 Kotlin IR 专属构造规则，CJO 按其已存类型产品。 |
| 同文件 `deserializeReturnableBlock` L187、`withDeserializedBlock` L218 | 先取得 returnable-block symbol；内部 statements 递归完毕后才构造 block 并绑定 symbol。 | symbol 壳早于 node 完成即可恢复 Return 的符号回边。**不能说 Kotlin 为所有 block/expression 先造具体 node 壳**；CJO 直接 e→e target 仍需自身 node reserve。 |
| `IrDeclarationDeserializer.withDeserializedIrFunctionBase` L540–575、`usingDeclarationParent` L214 | reference symbol 后 enter symbol scope；先创建 function，再作为 current declaration parent。typeParameters→returnType→parameters→body 按顺序恢复；parent 和 body-policy 均通过 finally 恢复。 | local decl、lambda、参数、泛型、正文必须在实际 owner context 中填充；内部 scope 不得泄漏到另一个请求。node 所有权与引用目标是独立关系。 |
| `IrDeclarationDeserializer.deserializeIrVariable` L633–649 | 先构造 variable 并绑定真实 symbol，再读 initializer；base helper L259 写 parent。 | 局部声明及 pattern binding 使用声明池里的真实 symbol，initializer/read/write refs 复用它，不临时制造 named variable。 |
| `IrBodyDeserializer.deserializeFunctionExpression` L435、`deserializeCatch` L141 | lambda 调同一 declaration deserializer；catch parameter 先通过 `deserializeIrVariable` 恢复，再读 catch result。 | Lambda function/参数和 catch binding 是实际声明；不把仅有表达式 type 当作完整 lambda/catch 恢复。仓颉 Pattern/MatchCase/FuncArg 等专属节点仍按 CJO schema 补全，Kotlin 没有逐槽同名对应。 |
| 同文件 `deserializeGetValue` L567、`deserializeSetValue` L689；`deserializeCall` L368、`deserializeGetField` L555 | value/callee/field/super qualifier 都从 `deserializeTypedSymbol` L1078 的同一 binder 获取；Call 在 symbol 取得后建具体 call，再填有序 arguments/typeArguments。 | target 引用不是新的声明拥有边；Call 的 expression type、callee signature、instantiation arguments 各自读取，不能在 reader 重新跑 candidate/completion 或把 result 覆盖 signature。 |
| 同文件 `deserializeStatement` L157、`deserializeBlockBody` L125、`deserializeOperation` L941、`deserializeNullableExpression` L1032 | Statement 区分 declaration/expression/block body/branch/catch 等；普通 statements 按顺序递归，expression 先 decode type/coordinates 再分派具体 operation。必需表达式入口 L1051 在 missing 时失败。 | 完整 concrete 分派和必需/可缺省槽 API 可借鉴；不能以“遇到未知 node 转普通 expression”“缺 body 则 empty block”代替实际恢复。Kotlin expression protobuf 按拥有树内嵌，并没有全 e 表 memo/统一两遍扫描。 |

Kotlin 的可用模板是 **symbol 先登记、拥有声明随后绑定、需要直接 node target 的 loop 先造壳**。CJO 的统一 d/e/t 表与非拥有边更强，适配必须增加按实际表 kind 的 concrete node reservation；不原样移植递归 protobuf reader，不能把普通 type cache 的 getOrPut 当全图 reserve。

### 5.2 header-only 与 body capability：实际模式及完成边界

[`KotlinIrLinker.kt::DeserializationStrategy`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/KotlinIrLinker.kt#L421) 的策略是在 file/module 建立时选定；`BasicIrModuleDeserializer.deserializeIrFile` L98–120 把它转为 `IrDeserializationSettings.DeserializeFunctionBodies`。

| 固定策略 | top-level 选择与正文政策 | CJO 适用边界 |
| --- | --- | --- |
| `ON_DEMAND` L435 | file 暂时不加入 module fragment，引用时才加载，不请求函数正文。 | 可映射延迟可见/header 消费；不是“没有保存 body”。 |
| `ONLY_DECLARATION_HEADERS` L465 | reachable headers；body policy NONE。file 被使用时仍会调度 explicitly-exported roots。 | 显式 header profile；必须保留实际 SOURCE body availability。不能由 Library origin 自动永久清除所存正文。 |
| `WITH_INLINE_BODIES` L475 | reachable；body policy ONLY_INLINE。 | 能力与选择分开；仓颉 SOURCE 的选择规则由本项目合同决定，不能只恢复 Kotlin 的 inline 子集来关闭 S5。 |
| `EXPLICITLY_EXPORTED` L455 | reachable + explicitly exported；所加载函数正文 ALL。 | 可借鉴 reachable/source closure 队列；不是 common/depends-on 的同名语义。 |
| `ALL` L445 | whole-world enqueue 全部 top-level + exported + 正文 ALL。 | 完整恢复模式；所保存的选中 body 必须真正恢复，不能用 header 查询成功代替。 |

**NONE 并不保证绝对零正文读取。** `IrDeclarationDeserializer.withBodyGuard` L488 因 inline 或返回类型泄露本地匿名 classifier 而提升局部正文 policy；`withInitializerGuard` L503 对 const 或匿名 classifier 也读取 initializer。提升使用 dynamic scope，嵌套 local functions 继承当前 body policy，退出后恢复。header 若引用正文内 local classifier，CJO 要显式保证 identity/header 闭包，不能把完整 SOURCE 默认跳过后发现问题才兜底补节点。

Kotlin `deserializeExpressionBody` L522 与 `deserializeStatementBody` L531 在 policy 不允许时直接返回 null。`deserializeIrValueParameter` L340–342 还以空 Composite 表示未恢复的 default；`deserializeIrAnonymousInit` L673 在无读取正文时造空 block；FIR `FirMemberDeserializer.loadDefaultValue` L842–863 对普通默认值建立 ExpressionStub。它们是各自的 metadata/header 合同，**不适合作为完整 CJO SOURCE 恢复结果**。实际 body absent、body saved but not requested、body decoding、body completed、unsupported/corrupt 必须区分。

`FileDeserializationStateImpl.deserializeAllFileReachableTopLevel` L232–247 仅在 top-level symbol 未绑定时 deserialize；已绑定会从队列移除。`IrDeclarationDeserializer.createIfUnbound` L918 的 `allowAlreadyBoundSymbols` 用于特定 JVM 场景，默认 false；`withDeserializeBodies` L577 只是已有 function 上临时开启政策的 helper。固定提交的 compiler/analysis 搜索只找到其定义，没有发现此 helper 的正式调用点。由此不能声称 header 模式经“再次 getDeclaration”自动升级完整 body。

另有明确的独立正文能力：[ `NonLinkingIrInlineFunctionDeserializer.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/NonLinkingIrInlineFunctionDeserializer.kt#L62) 的 `deserializeInlineFunction` L62–105 从实际 library 的 `inlinableFunctionsIr` 读取，按 library/module/signature 缓存。L99–105 返回 prepared **copy**，并标记 original；FileDeserializer L205–238 使用 detached symbol table、ONLY_INLINE 与可选 symbolResolver。默认允许公共引用未绑定是为了以后 KLIB 链接，**不能移植为 CJO 同一 symbol/node 身份升级的实现**。可借鉴的是能力明确、使用实际 container、请求缓存与独立正文入口；当前任务要求的是同一恢复 context 的真实 SOURCE 身份。

S5 建议在 header-complete 之外明确 `bodyAvailable` 与 `bodyComplete`；`requireBody(rootIdentity)` 消费同一 snapshot、binder、type decoder、node registry。header→body 的请求不新建 owner/own TP，不替换已经公开的 canonical symbol；完成 body closure 后才发布正文产品。再次仅查询 header 不应把 pending body 误标为 absent。

### 5.3 FullId、位置与恢复 reader 的寿命

| 固定位置 | 真实合同 | CJO 对位 |
| --- | --- | --- |
| `BasicIrModuleDeserializer.init` L55–77 | 以既有 `klib.irOrFail` component 为每个 file 建 `IrLibraryFileFromBytes(IrKlibBytesSource(ir, fileIndex))`，reader 保留在 file state。 | 所有 type/body/fill 使用同一个已加载快照；refs 补全不重新开同路径产生新代。Kotlin component 是否内部 lazy 读 bytes 与 CJO 的不可变 snapshot 实现不同，不声称 KLIB 预读了全部字节。 |
| `IrFileDeserializer.kt::IrLibraryFile` L251、`IrLibraryBytesSource` L263、`IrKlibBytesSource` L320 | 声明/type/signature/string/body/fileEntry 统一 reader 抽象；bytes source 保留 component + fileIndex。 | 统一 d/e/t/provenance 读取上下文；不同 annotation consumer 也不能各自建不共享 binder 的 reader。 |
| [`FileEntryDeserializer.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/FileEntryDeserializer.kt#L15)，`ProtoCacheKey` L15、`fileEntry` L19、L26、L37 | cache key 为 libraryFile+protoIndex，不仅 protoIndex；file 或 inlined function 的原 file entry 由实际 proto 选择，失效字段明确拒绝。 | 位置表的身份域必须与声明/正文来源相同；不能仅按文件名合并跨代、跨快照位置。 |
| `IrDeclarationDeserializer.deserializeCoordinates` L116；`FileEntryDeserializer.deserializeFileEntry` L48 | global offset 或相对 parent offset 解码；lineStartOffsetDelta 还原原 line map。读 IR type 中的坐标会设 undefined。 | 原 source 位置由 CJO 保存的 Position/FullId 恢复，不需要重新 parse source。**Kotlin type 中 undefined offset 不适用 CJO SOURCE TypeRef 的原节点位置保留**。 |

## 6. 正式库消费：入口、共享 decoder、能力边界

| 固定 module / file / method | 正式连接 | S5/S6 对位与差异 |
| --- | --- | --- |
| `:compiler:fir:entrypoint` / [`AbstractFirKlibSessionFactory.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/entrypoint/src/org/jetbrains/kotlin/fir/session/AbstractFirKlibSessionFactory.kt#L61) / `createLibrarySession` L61–85 | 把 resolvedLibraries 与 moduleDataProvider 传给正式 `KlibBasedSymbolProvider`。 | ordinary dependency 的 reader 通过 library session/provider 接线，不能靠 test-only context 工厂。 |
| 同模块 / [`FirAbstractSessionFactory.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/entrypoint/src/org/jetbrains/kotlin/fir/session/FirAbstractSessionFactory.kt#L168) / `createLibrarySession` L168–233 | 每个 moduleData 绑定 session；StructuredProviders 的 sourceProviders 为空、dependencyProviders 为实际 library providers，最终注册 `FirSymbolProvider` 与 `FirLibrarySessionProvider`。 | library 类型和 declaration 的实际 provider/context 必须是 source consumer 能查询到的同一对象。header profile 可以不构造 FirFile；不能借此说完整 SOURCE body 已恢复。 |
| 同模块 / [`FirMetadataSessionFactory.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/entrypoint/src/org/jetbrains/kotlin/fir/session/FirMetadataSessionFactory.kt#L101) / `AbstractFirMetadataSessionFactory.createLibrarySession` L101–142 | common metadata 环境使用 MetadataSymbolProvider，并对实际 resolvedKLibs 加 KlibBasedSymbolProvider。 | common 与 ordinary 使用实际 binary provider/shared typed decoder。Kotlin expect/actual 和 metadata JAR/KLIB 分类不等于仓颉 commonPart SOURCE 消费模式，不能照搬模式语义。 |
| 同模块 / `FirAbstractSessionFactory.createSourceSession` L311–318；`computeDependencyProviderList` L386–399 | 源 source set 的 binary dependencies 经 depends-on providers 传播；dependency/friend/allDependsOn 依赖计划先确定，去重 session。源码说明 actual 到 expect 的顺序要求。 | depends-on 的实际依赖选择计划与 source 的 consume path 要统一；CJO 不能按“第一个包 context”替代实际依赖选择。Kotlin actualization 不是本轮 S5 待实现的仓颉后端逻辑。 |
| 同模块 / [`KlibBasedSymbolProvider.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/entrypoint/src/org/jetbrains/kotlin/fir/session/KlibBasedSymbolProvider.kt#L28) / L28、`fragmentNamesInLibraries` L58、`moduleData` L83、`createDeserializedContainerSource` L87 | 正式 metadata provider 使用实际 library collection，package→libraries 索引，moduleData 来自 library.path 的已选 provider；containerSource 保存真实 library/header/config/package/版本不兼容信息。 | 来源、实际 package context 与能力要随 declaration 保留，不能重读文件后以名字找同名 symbol。版本格式与 CJO schema/compiler version 各自独立。 |
| `:compiler:fir:resolve` / `FirLibrarySessionProvider.kt::getFirClassifierByFqName` L22、`getFirFilesByPackage` L32 | 分类查询委托 symbolProvider；library 不提供正常 source FirFile 集合，container file 可空。 | header-only declaration/type 分析有正常入口，但不能证明 SOURCE 任意正文恢复或执行。完整 capability 要额外接同一正式物化服务。 |
| `:analysis:low-level-api-fir` / [`sessions/factory/LLFirSessionFactory.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/sessions/factory/LLFirSessionFactory.kt#L290) / `createBinaryLibrarySession` L290–338 | 给真实 KaLibraryModule/contentScope 创建 library session；注册 dummy lazy declaration resolver 和平台实际 binary providers，再注册 LLFirLibrarySessionProvider/FirSymbolProvider。binary session 自身只连 builtins；依赖查询属于 resolvable session。 | LL header 消费有明确 session 类型。不能只在 compile frontend 修 decoder 而 LL 用另一套 annotation reader。S5 body 请求若在 LL 开放，必须按其明确 capability 走同一恢复服务，不能对 binary header session 强行运行 source resolve。 |
| 同模块 / `sessions/factory/configuration/LLNativeSessionConfiguration.kt::createBinaryLibrarySymbolProviders` L15 | 经 `LLLibrarySymbolProviderFactory.fromSettings(project).createNativeLibrarySymbolProvider(session, scope)`，属于正式平台入口。 | 不需要另建仅测试可用库 loader；本库按真实 LL 平台入口注册 shared CJO typed materializer。Kotlin 具体 stub/metadata 选择受 settings 影响，不是 S5 多 reader 兜底模板。 |
| `:compiler:ir.serialization.js` / [`compiler/ir/serialization.js/src/org/jetbrains/kotlin/ir/backend/js/klib.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.js/src/org/jetbrains/kotlin/ir/backend/js/klib.kt#L55) / `deserializeDependencies` L55–79、`getIrModuleInfoForKlib` L187–226 | 实际 loaded KLIBs 决定 ALL/EXPLICITLY_EXPORTED/WITH_INLINE_BODIES 的策略；调用 linker registry，建真实 module dependencies，然后 external dependency generation/postProcess。 | 这是库正文真正送入生产消费的证据；只作为正式 capability/closure 连接模板。本轮排除后端，不实现 JS lowering、fake overrides、partial-linkage stubs 或执行器。 |

正文的正式 closure 在 `BasicIrModuleDeserializer.tryDeserializeIrSymbol` L84–91 先排队 owner并返回同一未绑定 symbol；`ModuleDeserializationState` L154–211 与 `FileDeserializationStateImpl` L198–247 消耗 module/file/top-level 队列。`KotlinIrLinker.deserializeAllReachableTopLevels` L241–248 反复耗尽新增请求，`getDeclaration` L265–284 触发实际绑定。可复用此“引用排队、拥有内容恢复、直到闭包完成”的分层流程。不能把 Kotlin postProcess 的 fake override / 缺失符号 stub 生成当 CJO reader 的 fallback。

## 7. LL 并发、重入与完成发布

`:analysis:low-level-api-fir` 的 [`caches/FirThreadSafeCacheWithPostCompute.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/caches/FirThreadSafeCacheWithPostCompute.kt#L18) `getValue` L18–25，把 per-key `ValueWithPostCompute` 放进 ConcurrentHashMap，再调用其 getValue。

[`caches/ValueWithPostCompute.kt`](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/caches/ValueWithPostCompute.kt#L55) 的 `getValue` L55–81、`computeValueWithoutLock` L85–118 明确区分未计算、当前线程 postCompute、全部完成；仅计算线程在 postCompute 期间能看到原 value，其它线程等待。`getValueIfComputed` L122–125 不返回 postComputing 对象；异常 L108–110 重置未计算。

适用的是**内部 identity 可重入，完成对象才能对外发布**。CJO reserve/fill 要满足同样的观察合同，但不直接把整个跨包 bounds/RHS fill 塞进 per-key 锁：Kotlin 的 `FirCachesFactory` L59–61 已明示 cross-key 递归死锁风险；其 ValueWithPostCompute 也未证明任意跨包互等图都安全。主线程需要在实际 registry/materialization owner 下统一调度依赖闭包，身份分配与等待完整对象的操作分别定义。至少不能通过一个“正在读取则返回 fallback”的分支假装完成。

同一个 snapshot 的 canonical 身份不能因失败或重复请求被替换；同时 failed/partial 内容不能作为 completed header/body 给外部读者。异常、取消、session 失效和新快照需要明确完成状态与代际边界，不能回收缓存后保留上一代 weak bound tag 作为新声明身份。

## 8. S5 类型先行后的表达式接线次序与未关闭范围

类型先行阶段可完成第 1–4 节并接正式 ordinary/common/depends-on/LL header consumer；这不是完整 S5/S6 关闭。下一段按同一 context 建表达式 capability：

1. 选择实际保存的 SOURCE root，保留 body availability、snapshot provenance 和 selection，取得已完成 header/类型 binder。
2. 先 reserve 全部拥有声明及其 own TP；再按实际 e/node kind reserve需要回边的具体节点身份，local/lambda/control/辅助节点拥有域明确。已有 target identity 不重新分配。
3. 用同一 reader 填有序拥有内容、类型、owner；非拥有 target 仅查 canonical identity，必要的外部 owner 进入依赖队列。Return/loop/value/callee/泛型各自绑定，不按名字、当前栈或 expression type猜目标。
4. 补全保存的非拥有 refs 和依赖内容，完成所有必需 slots，检查 root closure 与位置/FullId 来源；随后发布 body-complete 产品。formal source/frontend/LL 消费按 capability 调同一服务，不读取 raw 表后手工注入结果。

主线程实施后的行为验收至少覆盖：第一次从 Alias use/TP type/nested member 查询开始；合法 recursive bounds与后续 own TP 引用；own/outer 同名参数与跨包同名 TP；重导出来源与新 session/snapshot；TypeKind.Type self/RHS/use 角色；同 raw index 的跨域隔离；header-only 保留 body availability；header 后请求 full body 保持同一 TP/符号/节点；Return/lambda/local/loop 回边；异常与并发不能公开半填结果；真实正式 CJO 输出被 fresh ordinary/common/depends-on/LL consumer 读取。用实际新产物走生产入口，不能以缓存旧报告或直接读 raw FlatBuffers 的结构比较代替 typed decoder 消费。

上述是映射与验收合同，未新增/运行测试。S5/S6、完整 Generic/capture、完整 SOURCE body 以及指定续接文档的其余 OPEN gate 仍保持 OPEN；本报告不对候选生产实现作 ACCEPT/REJECT，也不把既有 SOURCE48 通过扩展成 typed consumer 通过。
