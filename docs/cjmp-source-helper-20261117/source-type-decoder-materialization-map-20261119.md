# S5：正式 SOURCE 新 CJO 的类型解码与共享物化合同

本报告只考证类型消费与身份物化，未修改生产、测试、原 SOURCE、fixture 或期望，未运行 Gradle/cjc，未联系旧代理。唯一输出位于授权工作树。用户提供的 `f614538d`、SOURCE48 `48/48`、同 SHA 全过是已报告的执行证据；本轮不把它扩展为完整 Generic/typed decoder 已通过的证据。

结论：S5 的修复 owner 是 `CfirTypeDeserializer + CfirDeclDeserializer + CfirDeserializationContext`，以及所属 deserialized provider 的包 context 注册和 `FullId` 路由。必须先登记真实声明/TP/Alias 身份，再恢复类型引用、两个 Generic 产品与 Alias RHS，最后发布完成的语义使用视图。当前按名称造 synthetic TP、递归返回 fallback、`TypeKind.Type` unsupported、零类型引用转 Unit，均不能消费合法的正式 SOURCE 类型图。raw CJO 的 reserve/fill 转存已有正确的图表机制，但它不能替代正常 typed reader。

## 1. 证据边界与固定对象

使用已读的 [kotlin-cangjie-frontend-parity/SKILL.md](../../build/cjmp-owned-closure-20261120/documentation-link-repair-map.md#external-skill-provenance)（原主树技能路径：`D:/code/intellij/cangjie/.agents/skills/kotlin-cangjie-frontend-parity/SKILL.md`；历史技能版本未逐字节封存），按用户指定的独立只读 mapper 范围完成 Kotlin/native 两侧考证，不执行主线程的生产实施和测试流程。已有 [T6 框架报告](generic-constraint-capture-framework-map-20261119.md)、[native 类型载荷报告](ordered-type-payload-native-authority.md)、[Kotlin 类型载荷报告](ordered-type-payload-kotlin-authority.md)提供上下文；下面的关键 reader 链重新读取固定 Git 对象。

| 镜像 | 固定提交及实际对象检查 |
| --- | --- |
| `D:/code/intellij/cangjie/external/kotlin` | `3c7a5041f238102285764d370665f56f587f9dc4`；`git show --no-patch` 确认提交主题 `[Serialization,stubs] Add IS_FULL_VALUE_CLASS flag to metadata`、日期 `2026-11-09T20:27:55Z`；逐文件 `git show <SHA>:<path>` |
| `D:/code/intellij/cangjie/external/cangjie_compiler` | `14276740ffec1262b81b6b5236b1d840af270b37`；提交主题 `!1689 merge 14216_revert into release/1.1`、日期 `2026-05-14T19:05:03+08:00`；逐文件 `git show <SHA>:<path>` |
| 生产工作树 | `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，本轮 `HEAD=b4dd742d589e4b5ee6592d454634c44f634df08a`；下文 local 行号来自工作文件，关键 SHA-256 列在末尾 |

任务中 `LoadGenericType / LoadTypeAliasType` 的对应功能，在此 native 对象的 `src/include` 搜索结果中实际名为 `SetGenericTy`、`SetTypeTy<TypeAliasTy, TypeAliasDecl>`、`LoadTypeAliasDecl`、`LoadTypeAliasDeclRef`。固定 Kotlin 全树未找到独立 `IrTypeDeserializer.kt`，compiler 中也没有同名 class/object；实际类型解码 owner 是 `IrDeclarationDeserializer.deserializeIrType*`。下文使用真实名称。

## 2. native：先声明身份、再 refs、最后 Alias 使用视图

| 固定位置/方法 | 已确认的职责和初始化顺序 |
| --- | --- |
| [ASTLoaderImpl.h::CreateAndLoadBasicInfo，L146](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoaderImpl.h#L146) | L149 创建实际 node，L153/L155 立即登记 `allLoadedExprs/allLoadedDecls`，随后才加载位置。登记的是该输入条目的真实节点。 |
| [ASTLoader.cpp::LoadNominalDecl，L902](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L902)；`LoadTypeAliasDecl:924`、`LoadGenericParamDecl:944` | nominal/Alias/TP 都用上述工厂；Alias 在 L926–930 建立节点、basic info、imported/export 身份和 generic，RHS 留到 refs 阶段。TP 只有真实 exportId 时才进入 exported map。 |
| [ASTLoader.cpp::LoadGeneric，L573](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L573) | 按 `Generic.typeParameters` 顺序 `LoadDecl<GenericParamDecl>`，L585 设置真实 `outerDecl=&decl`。名字不是 binder。 |
| [CjoManager.cpp::LoadPackageDeclsOnDemand，L345](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/CjoManager.cpp#L345) | L355/372 对 common 与依赖包先 `LoadPackageDecls`；依赖队列走完才在 L383–386 对各 loader `LoadRefs`。`LoadAllDeclsAndRefs:397` 同样分两遍。 |
| [ASTLoader.cpp::GetDeclFromIndex，L532](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTLoader.cpp#L532) | 源码明确限定在全部 decl 已加载的 LoadRef 阶段使用。CURRENT 查 `allLoadedDecls[fullId.index]`；imported 查目标包 `exportIdDeclMap[fullId.decl]`；PACKAGE_REFERENCE 是 package 本身。返回 canonical 节点指针。 |
| [ReferenceLoader.cpp::LoadType，L269](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L269) | `INVALID_FORMAT_INDEX=0` 返回 `GetInvalidTy()`；非零减一，先读 `allTypes`，再执行 kind loader。`PreLoadImportedPackageNode:423` 先按表大小建立空的类型槽。 |
| [ReferenceLoader.cpp::SetGenericTy，L306](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L306) | `declPtr → GetDeclFromIndex → GenericParamDecl → TypeManager.GetGenericsTy`，L316 **先登记真实 Generic Ty**，L325 才递归 LoadType(upperBounds)。随后在实际 `gpd.outerDecl.GetGeneric().assumptionCollection` 登记语义上界。`isLoadCache` 的跳过分支属于 incremental cache，不适用于正式 SOURCE 新 CJO。 |
| [ReferenceLoader.cpp::SetTypeTy，L346](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L346) | nominal/Alias 从真实 declPtr 与逐槽 typeArgs 构造；Alias 调 `GetTypeAliasTy`，Class 的 `isThisTy` 决定 `GetClassThisTy/GetClassTy`。Func 的 parameters 与 retType、C/variadic 标记独立读取。普通复合 Ty 在子类型读取后登记；不能声称 native 每种 Ty 都先造空壳。 |
| [TypeManager.cpp::GetGenericsTy，L66](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L66)；`GetTypeAliasTy:108` | 真实 TP/TypeAliasDecl 参与类型身份；复用 `allocatedTys`。Generic 身份不来自显示名称；Alias 身份及应用参数不来自 RHS 展开缓存。 |
| [ReferenceLoader.cpp::LoadDeclRefs，L521](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L521)；`LoadGenericConstraintsRef:432`、`LoadTypeAliasDeclRef:501` | `decl.ty`、原约束组 subject/uppers/implicit/source、函数 ret node、Alias RHS 分别恢复。原约束组不会充当 GenericTyInfo 的替代物。 |
| [ReferenceLoader.cpp::LoadExprRefs，L586](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L586)；`LoadSubNodeRefs:657`、`PostLoadReference:172` | expression.ty、target、instTys、matchedParentTy 分槽加载；FuncArg/Pattern/MatchCase 等非 Expr 子节点另补 refs。最后一遍 `PostLoadReference` 才补 Call.resolvedFunction 等依赖已完成 base target 的字段。 |
| [CjoManager.cpp，L389](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/CjoManager.cpp#L389)；`ReplaceTypeAliasInNode:414`、`SubstituteImportedTypeAliasTy:436` | 全部类型引用加载后才对编译使用的 imported/common 节点替换 Alias；L417 保留 Alias 声明自身 ty。L389 的 LSP indexing 例外是特定消费模式，不能作为全局 Alias 开关。 |
| [TypeManager.cpp::GetUnaliasedTypeFromTypeAlias，L2508](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L2508)；`SubstituteTypeAliasInTy:2527` | 从实际 Alias RHS 与实际 typeArgs 生成映射，递归替换完整类型树；ClassThis 与 Func C/variadic 角色保留。语言已判定的 `IN_REFERENCE_CYCLE` 是另一种语义状态，不能套用于所有 loader 重入。 |

合法回环的框架例子是 `Generic(T) → upper I<T> → typeArg Generic(T)`：Generic 的身份边先完成，因此类型参数再次出现时返回同一真实 Ty；其上界填充仍能继续。此图式说明 loader 合同，不宣称任意直接互相约束源码都合法。真实新增测试仍须有 native 成功产物证明合法性。Alias 的身份引用与 Alias RHS 展开也是两条边；把身份构造改成递归展开 RHS，会人为制造 recursion fallback 的需求。

## 3. Kotlin：binder 与类型解码的实际边界

| 固定 Kotlin 文件/方法 | 精确合同 | 本库应对位的 owner |
| --- | --- | --- |
| [FirTypeDeserializer.init，L62](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-deserialization/src/org/jetbrains/kotlin/fir/deserialization/FirTypeDeserializer.kt#L62) | 第一遍 L67–85 建实际 TP symbols/builders，记录 containingSymbol，并将每个 proto.id 登记入 map；第二遍 L87–95 才 decode bounds/build declaration。 | DECL 的 own TP 创建与 CTX 的 canonical binder；TY 只取已登记的真实 symbol。 |
| [FirTypeDeserializer::forChildContext，L101](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-deserialization/src/org/jetbrains/kotlin/fir/deserialization/FirTypeDeserializer.kt#L101)；`typeParameterSymbol:197`、`rigidType:212`、`typeSymbol:336` | 子 context 保存 parent 和实际 containing declaration；TP ID 查当前再查 parent；TP 类型是 symbol lookup-tag 叶子，不在构造此类型时展开该 TP 的 bounds。 | CTX/DECL 的 owner-aware TP 身份查找；不能按当前 decoder 栈或名字重新造 TP。 |
| [FirMemberDeserializer.kt::FirDeserializationContext.childContext，L61](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-deserialization/src/org/jetbrains/kotlin/fir/deserialization/FirMemberDeserializer.kt#L61)；`loadTypeAlias:216`、`loadFunction:649` | symbol 先选定，child context 用它建立自己的 TP；Alias 的 expandedType 和函数签名在该 context 下读取。own TP 与 outer TP 的职责明确。 | DECL 的 `convertTypeAlias/convertFunctionOrEnumConstructor/deserializeTypeParameters` 与 CTX；CJO 用 FullId/owner index，不移植 Kotlin 的 proto-name 分支为跨包兜底。 |
| [AbstractFirDeserializedSymbolProvider::findAndDeserializeTypeAlias，L269](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-deserialization/src/org/jetbrains/kotlin/fir/deserialization/AbstractFirDeserializedSymbolProvider.kt#L269) | 返回真实 `FirTypeAliasSymbol` 与 postProcessor；后者以同一个 preComputedSymbol 调 `loadTypeAlias`。 | ABI/BIN 注册 canonical 包 context 和 Alias identity，再让 DECL 填充同一 identity。 |
| [FirTypeDeserializer::rigidType，L252](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-deserialization/src/org/jetbrains/kotlin/fir/deserialization/FirTypeDeserializer.kt#L252) | expanded class-like type 与 `AbbreviatedTypeAttribute` 各自保存；typeAliasName 有 classifier 路径。 | ALIAS/EXP 与完成产品；self/RHS/use-site 分槽。 |
| [IrDeclarationDeserializer::deserializeIrType，L99](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IrDeclarationDeserializer.kt#L99)；`deserializeSimpleType:144` | `irTypeCache.getOrPut` 缓存类型；classifier symbol 先解析，arguments 再逐项 decode。`deserializeNullableIrType` 对 IR 的 `-1` 表示 null；这不是 CJO 的索引合同。 | TY 的类型表消费与真实 classifier binder。IR 的 type cache 本身不是 reserve-first 证据。 |
| [IrSymbolDeserializer，L36](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IrSymbolDeserializer.kt#L36)；`deserializeSymbolToDeclareInCurrentFile:47`、`deserializeSymbolWithOwnerMaybeInOtherFile:61` | 按 signature 登记/复用 symbol；跨文件 public 引用交 linker 定位所属 file。`referenceDeserializedSymbol:97` 为 TP/Alias 使用类型对应的 symbolTable reference。 | CTX 的 identity registry 与 FullId 到目标包 context 的路由。 |
| [SymbolTableSlice::declare，L36](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/util/SymbolTableSlice.kt#L36)；`referenced:49` | 在实际 owner 创建前登记 symbol；referenced 也登记真实 signature symbol，随后 declare 复用并绑定。暂未 bound 的真实 symbol 不等于 synthetic replacement。 | CTX 的 identity-ready 与 complete 状态分离。 |
| [IrDeclarationDeserializer::deserializeIrTypeParameter，L265](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IrDeclarationDeserializer.kt#L265)；`deserializeIrClass:346`、`deserializeIrTypeAlias:421`、`deserializeTypeParameters:449` | `declareGlobalTypeParameter` 复用真实 symbol；class 创建/绑定后填 TP/supertypes；TP 列表是逐 TP create 后填 superTypes，**没有 FIR 那样的“全列表 builders 两遍”**。后续 TP 的前向引用由 symbol binder 承担。 | DECL 身份登记/填充；不能声称 IR 要先递归建完 bounds 才登记。 |
| [IrFileDeserializer.kt::FileDeserializationStateImpl，L153](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IrFileDeserializer.kt#L153) | file 的 symbol/declaration/body decoder 共享 linker.symbolTable 与该 fileReader；public reference callback 在 L160–163 路由到真实 owner。 | BIN/ABI 共享 context factory；正常 SOURCE type/ref consumer 不另建没有所属包 binder 的 decoder。 |

Kotlin 提供身份登记、child owner、classifier-first、完成视图与原 abbreviation 的框架合同。CJO 的跨包 FullId、GenericTyInfo、TypeAliasTy、ClassThis、RawArray 维度、C/ellipsis 标量来自 native；不能把 Kotlin proto/KLIB 的类型编码直接当 CJO schema。

## 4. 本库当前缺口与生产位置

下表缩写只用于后文方法定位；每个文件链接指向本轮实际工作树。

| owner / 文件 | 当前方法与事实 | 必须修复的合同 |
| --- | --- | --- |
| TY：[CfirTypeDeserializer.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirTypeDeserializer.kt)（原审查坐标 L59） | `deserializeType:71` 在 convert 后 L85 才缓存；L76 重入走 `createRecursiveTypeFallback:284`。`convertGenericType:250` 当前 TP 命中便直接返回，未加载该条 GenericTyInfo.upperBounds；未命中时按 name 建 synthetic TP。 | 类型身份和 bound/RHS 填充拆开；合法重入返回同一实际 binder/type identity。当前包、未先请求的 TP、跨包 TP 使用同一 FullId materializer。 |
| TY 同文件 | `resolveMaterializedTypeParameterSymbol:271` 与 `resolveCurrentPackageTypeParameter:310` 都只查 CURRENT 的 declCache；`createSyntheticTypeParameterSymbol:322` 的 containingDeclarationSymbol 是自己。`convertSemaTy:141` 不支持 Type。 | 删除合法图上的两条 synthetic/fallback 路径；Type 解码为真实 Alias identity＋arguments，RHS 由 Alias 声明 owner 恢复。 |
| TY 同文件 | `deserializeTypeFromField:99` 将零引用转 Unit，L97 注释也作同样错误声明。`convertPointerType:240` 缺 pointee 时补 Unit；`convertVArrayType:231` 缺 info 时补 size=0。 | 依字段角色处理缺省/Invalid/必填；非零指向真实 SemaTy，完整校验 shape。合法 pointee、size、dims 不使用自造默认值。 |
| DECL：[CfirDeclDeserializer.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeclDeserializer.kt)（原审查坐标 L201） | `deserializeDecl:201` 重入只能返回可能尚不存在的 declCache；L217 转换完成、L222 publication、L223 cache。class/function/Alias converters 先读依赖，后构造/bind owner。 | 先登记实际 symbol；真实 TP node 绑定实际 containing owner；依赖可以引用 identity-ready 的 symbol。外部查询仍只得到 metadata/type 合同完整的声明。 |
| DECL 同文件 | `deserializeTypeParameters:1236` 顺次创建 TP，随后 `applyOwnerGenericConstraints:1255` 将各 group 的 uppers 直接拼入 TP.bounds，最后补 default Any。`convertTypeParameter:2394` 本身从空 bounds 开始。 | 原 group、原 group source、最终 GenericTyInfo bounds 三者各自恢复。默认 Any 仅保留 normal type-system 作用，不能冒充 wire final bounds。 |
| DECL 同文件 | `convertTypeAlias:2355` 在绑定 owner 前读 TP/RHS；缺 AliasInfo 时改用 Decl.type。`buildTypeRef:1194` 一律 source=null、COMPILER_ADDED。 | 新正式 CJO 要求 AliasInfo 的真实 RHS；Decl.type 是 self，不能拿它作 RHS。binary source 与 imported wrap origin 分开保存，不能按 source=null 重建原约束位置。 |
| CTX：[CfirDeserializationContext.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeserializationContext.kt)（原审查坐标 L22） | shared typeCache/declCache 在 L50/53；L61/63 是按 index 的锁。`createTypeDeserializer:107/createDeclDeserializer:110` 每次新实例，因此 recursion sets、owner 栈只在单个 decoder 内。跨包 callback L93 仅支持 initialization dependency。 | context 持有 canonical identity/state/完成产品，调用栈保持请求私有。跨包 resolver 必须获得目标 context；不能只共享最终 cache，仍让新 decoder 重复造 binder。 |
| ID：[CjoFullIdResolver.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CjoFullIdResolver.kt)（原审查坐标 L36） | `resolve:36` 返回原始 IndexedDeclaration；`loadImportedPackageIndex:149` 只读 header/pkg。`CjoPackageIndex.buildParentDeclIndex:250/childDeclIndices:269` 只收 Class/Interface/Struct/Enum/Extend body 边，没有 Generic TP 或函数参数的真实 owner 边。 | FullId 解析结果携带 owning snapshot/context 与 index；增补实际 binder owner 关系，区分直接 containing owner 和最近 class owner。 |
| ABI：[AbstractCfirDeserializedSymbolProvider.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/provider/AbstractCfirDeserializedSymbolProvider.kt)（原审查坐标 L177） | `contextCache:57` 按包缓存 List；`getOrCreateDeserializers:177` 注册后即 `initializePackageScope:222`。`PackageDeserializers:425` 每请求创建 decoder。`deserializeCjoInitializationDependency:208` 先选 path/first context，再只接受 class direct member。 | 包 context 登记与 scope 完成发布分开；正常 FullId 身份读取不能借 class-only 初始化依赖 API。每个实际快照仍保有独立 context。 |
| BIN：[CfirDeserializedSymbolProvider.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/provider/CfirDeserializedSymbolProvider.kt)（原审查坐标 L137） | `loadPackageDeserializers:137` 每份 loaded snapshot 创建 context，并只安装 initializationDependencyResolver。 | 在同一 provider/session 的 canonical context registry 中登记 loaded snapshot，安装普通 FullId/owner 身份路由；目标包的 moduleData 来自它自己的 `moduleDataFor:166`。 |
| PKG：[CjoManager.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoManager.kt)（原审查坐标 L69）；[CjoLoadedPackage.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoLoadedPackage.kt)（原审查坐标 L11） | `loadPackageSnapshots:69` 返回同包各实际快照；单数入口 `loadPackageSnapshot:64` 取 first。Snapshot.pkg 每次可返回新的 FlatBuffers 包视图。 | context 必须保留实际 CjoLoadedPackage 对象身份；包 wrapper、路径、包名、ClassId 都不足以代替这个身份。 |
| STUB：[CjoDeclarationLoader.kt](../../analysis/decompiled/decompiler-to-stubs/src/org/cangnova/cangjie/analysis/decompiler/stub/CjoDeclarationLoader.kt)（原审查坐标 L24） | `loadDeclarations` 每次单独创建 context/type decoder，`forCompiledFileStub` 禁用字段 initializer。 | stub 独立消费域也必须有真实 TP/Alias 和跨包 owner；若进入普通 frontend symbol graph，使用该 graph 的 registry，不能拼接两域的同名 symbols。 |

`CfirTypeParameterImpl` 已在 [真实生成类 L46](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/impl/CfirTypeParameterImpl.kt) 的 init 绑定 symbol。生产改造应让这一次创建消费预登记的真实 symbol，保持同一 node；不能构造一个带默认 bound 的替代 TP，再把实际 TP 接到另一份 symbol。

## 5. shared context 的完整初始化流程

下面是可实施的 normal reader 生命周期，不是已有方法已完成的声明。新增的 identity/state/完成产品需放在 CTX/BIN/ABI 的现有职责中；不另造第二套名字 provider 或 backend materializer。

1. **固定输入与 context。** BIN 的 `loadPackageDeserializers` 从 PKG 获取同代实际快照；先在所属 provider/session 注册 `snapshot → CTX`，再递归获取真实依赖快照/context。CTX 直接持有 loaded snapshot、moduleData、raw table view、索引视图与解析域。完整包名仍用于查询入口，最终身份键复用 [CjoSnapshotTableEntry](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoSnapshotTableEntry.kt)（原审查坐标 L10） 的 `(snapshot identity, domain, rawIndex)`，外层加所属 provider/session 域。
2. **先建立 owner 图，再登记 binder。** ID 的 reverse index 收 `Generic.typeParameters`、FuncBody.paramLists.params 以及实际声明 containment；class body、property accessor、保存正文里的 local declaration 边按各字段本来角色记录。TP 的 containing owner 是声明这个 Generic 的函数/class/struct/interface/enum/Alias/extend/builtin，不能直接用最近 class，也不能用正在请求它的外部 decoder 栈。构造器“引用 class 自有 TP”与重新声明 own TP 仍按现有 tree 合同区分。
3. **登记真实声明身份。** DECL 在读取任何 bound/RHS/signature 前使用各 raw Decl kind 创建实际 symbol 并按输入键登记。所有 own TP symbols 先登记，TP node 使用实际 containingDeclarationSymbol；owner symbol 可在同一事务内暂未 bound，身份引用不得读它尚未完成的成员/签名。真实 node 只创建和绑定一次。对有 mandatory RHS 的 Alias，可以先 reserve symbol，待真实 RHS identity graph 建好后构造并绑定 node，不需要临时 error RHS。
4. **建类型身份图。** TY 对 Generic 先找到真实 TP binder，建立/登记其 `ConeTypeParameterTypeImpl(symbol.toLookupTag())`；upperBounds 的填充列入该真实 TP 的待完成产品。nominal/Alias 从真实 FullId target 和有序 arguments 构造；Func/Tuple/Pointer/VArray/RawArray 保留各独立子槽和标量。此阶段不沿 classifier 去展开 owner self/signature/bounds，也不在 Alias identity 构造中展开 RHS。复合类型子槽正常 decode；回边经过已登记的 TP/声明 identity 便终止构造递归。
5. **恢复 references 与产品。** DECL 对实际 owner 读取原约束组、GenericTyInfo final bounds、AliasInfo RHS、继承与函数签名；保存 SOURCE 的 expression/ref type 槽也通过同一 CTX。待所有依赖 RHS 就绪后完成 Alias 参数应用与嵌套展开。Generic 反复遇到同一真实 TP 时复用身份，按 native `SetGenericTy` 的集合登记合同完成语义关系；每条输入 GenericTyInfo 的原有序向量另保留，不能被另一个包的空向量覆盖或改成 default Any。
6. **发布完成 header/type graph。** 现有 `publishDeserializedDeclaration/publishDeclarationMetadata/recordCjoDeclarationPosition` 的 annotation、interop、CJO origin、common/export facts、parameter defaults 等义务仍须在外部 cache 可见前完成。保留现有初始化依赖恢复顺序：不要从 Generic 的内部身份查询调用 public `deserializeDecl`，提前触发全包 `restorePackageInitializationDependencies`。normal refs/header 完成后再执行其完成义务。`markResolved` 与 external declCache/typeCache 发布必须发生在完成门之后，不能用 BODY_RESOLVE 标签掩盖 pending bounds/RHS。
7. **初始化普通 scopes，随后才交 capture。** ABI 的 `initializePackageScope` 与顶层 symbol caches 消费完成的 canonical 节点。正式 library lookup 可以在该正常加载 owner 中物化依赖；export/capture provider 不承担 materialization、不调用阶段推进、不打开另一个 CJO。capture、validate、writer 只读取完成产品。

内部状态至少区分“真实身份已登记”“refs/header/type 产品已完成”“已对外发布”，并记录真实失败。它们是 reader 私有的完成状态，不是新 resolvePhase。`declCache/typeCache` 不能同时充当“外部完整值”和“未填完节点”的无标记 map。内部重入取得真实 identity；另一线程请求完整值时等待同一图事务完成，不能看见半截 Generic/Alias。

并发必须覆盖跨 package/context 的依赖图。当前“各类型/声明一个锁＋各 decoder 私有 recursion set”若用于相互依赖的两个请求，会出现重复构造或持 A 等 B、持 B 等 A 的风险；这是源码级风险，本轮未声称已运行复现。可实施的正常路径是由 ABI/BIN 的同一 materialization 事务串行管理首次 identity/ref 填充（对位 native 包 refs barrier 和 Kotlin symbolTable 的共同锁），完成缓存继续并发读。避免持独立 slot 锁再调用另一 context 的 public 完成等待。请求 owner 栈仍保持私有，canonical state 放 CTX；无需共享有栈 decoder 实例。

## 6. FullId、跨包 TP/Alias 与 provider 接线

| 实际入口 | 接线要求 |
| --- | --- |
| ID.`resolve` CURRENT | 当前快照的 formatted Decl index 减一定位真实身份；即使 TP owner 尚未作为顶层 symbol 被请求，也由预建 Generic owner 图准备其真实 binder。 |
| ID.`resolve` imported / `loadImportedPackageIndex` | 先按当前包 imports 与 FullId.exportId 找实际 target snapshot/index，然后进入其已登记的 CTX。索引视图可用于定位，不能作为 materializer。跨包 TP 读目标 owner；跨包 Alias 读目标 own TP 与 RHS。 |
| ID.`resolveClassId / resolveDeclarationName` | 这些只是显示/查询辅助，不能替代上述 binding 路径。正式 type 图的 target 是 FullId 实际声明。 |
| CTX.`createDeclDeserializer/createTypeDeserializer` | 每请求实例共享同一 identity/state/完成产品 registry；新 decoder 不再拥有 canonical TP 创建权或独立合法回环 fallback。 |
| ABI.`getClassLikeSymbolByClassId/getClassLikeSymbolsByClassId` → `loadDirectTopLevelClassSymbols` | 查询只选择已完成 symbol；当前 plural API 保留各 parent classifier 的真实身份，不能让类型解码把它们按 ClassId 合并。 |
| ABI.`getTopLevelCallableSymbolsTo/getTopLevelFunctionSymbolsTo/getTopLevelPropertySymbolsTo` → `resolveTopLevelCallableSymbols/loadDirectTopLevelCallableSymbols` | 函数 own TP、签名内 TP、Alias/RHS、参数默认值引用同一 context。re-export 最终指向真实 package owner，不能在转出口包再建 symbols。 |
| ABI.`getTopLevelExtendDeclarations` | extend 的 own TP 与其 receiver/继承类型使用真实 Generic owner；其成员不能从请求端 class 栈借 TP。 |
| ABI.`getOrCreateDeserializers / PackageDeserializers`；BIN.`loadPackageDeserializers` | raw context 登记入口不触发 scope 初始化重入；完成后查询路径才初始化 scope。为普通 FullId 提供目标 context/identity 获取能力，保留 initialization-dependency 的独立完成职责。 |
| STUB.`loadDeclarations` → DECL.`forCompiledFileStub` | 明确 header-only 消费 profile，并使用所属消费域的同一 registry。它不要求加载函数体，但不能用 fake TP 或 unsupported Alias 伪造签名。 |

多份同包 parent 快照是本库已存在的输入域。当前 PKG 单数 load 与 ID imported index 只取第一份，不能扩展为任何 imported FullId 都合法取 first。跨包选择必须带回当前实际依赖选择/parent 计划确定的 snapshot/context；若上游没有这个来源合同，该接线本身仍 OPEN，不能在 decoder 按名字、ClassId 或第一次缓存命中另发明选择规则。`sourcePath` 用于来源定位，snapshot 对象与所属 session 才决定同代身份；重新加载同一路径必须是另一代。

## 7. 类型字段的精确角色：零、Invalid、Unit 和有序边

固定 [ModuleFormat.fbs](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/schema/ModuleFormat.fbs#L540) 的 `TypeKind.Invalid` 与 `Unit` 是 enum 值；L608 的 `SemaTy.kind=Unit` 是表内 kind 缺省。`INVALID_FORMAT_INDEX=0` 则是引用域的保留值，定义在 [ASTSerialization.h L30](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/Modules/ASTSerialization.h#L30)。两层不能混为一谈。

native `ASTWriter.SaveType:922` 对 initial/null Ty 输出零；[Ty::IsInitialTy:1027](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/AST/Types.cpp#L1027) 的定义是 `!ty || TYPE_INITIAL`。真实 Invalid Ty 仍可拥有 SemaTy 表项。reader 的 LoadType(0) 返回 InvalidTy；真实 Unit 必须通过非零索引指向 Unit SemaTy。`UInt.MAX_VALUE` 也不是固定 native 的 INVALID_FORMAT_INDEX；本库 raw 转存可保留此原值，但正式 typed reader 不能把它当 Unit/有效表项，非零应先用 Long 验证范围再转换下标。

| 输入槽 | 固定 native reader | 本库具体 read/write owner 与正确合同 |
| --- | --- | --- |
| `SemaTy.typeArgs` | `LoadTypeArgs:334`；Func 分支读取的是 parameter types | TY.`deserializeTypeArgs/convertFuncType/convertTupleType`。逐槽保留长度、顺序、重复引用；同一个 raw TYPE index 重读复用同一 cache 结果。不能拿去重依赖 children 重建输入向量。 |
| `FuncTyInfo.retType/isC/hasVariableLenArg` | `SetTypeTy<FuncTy>:367` | TY.`convertFuncType:200` 独立保存 parameters 与 returnType；C/variadic 来自实际 info，native loader 的 closure 位置固定 false，不移植 Kotlin suspend/vararg 类型语义。 |
| `CompositeTyInfo.declPtr/isThisTy` | `SetTypeTy:373` | TY.`convertClassType/convertStructType/convertEnumType` 与新增 Type kind 分支都绑定真实 FullId；ClassThis 标量必须保留，不能归一成 call 的 nominal result。 |
| `GenericTyInfo.declPtr/upperBounds` | `SetGenericTy:306` | TY.`convertGenericType` 和 CTX 完成产品；declPtr 身份先登记，上界引用随后恢复。空向量就是该载荷的空 final bounds，不能补 Any 写回该槽。 |
| `Decl.type` | `LoadDeclRefs:524` | DECL 各 converter；函数是完整 Func Ty，Alias 是 self Ty，TP 是 Generic Ty，不能统一当 return type 或 RHS。必要的输入 Invalid/initial 状态按真实字段角色保留，不能补 Unit。 |
| `AliasInfo.aliasedTy` | `LoadTypeAliasDeclRef:505` | DECL.`convertTypeAlias` 恢复声明 RHS；必须与 Alias self 独立。 |
| nominal inheritedTypes / extend type | `LoadInheritedTypes:400`；`LoadNominalDeclRef:493` | DECL.`deserializeInheritedTypes/convertExtend`；经相同 type cache 与 TP binder，不能重建自有或外层 TP。 |
| `FuncBody.retType` | `LoadNominalDeclRef:480` | DECL.`buildFunctionReturnTypeRef:1211`；源码 ret node 可不存在，函数语义返回仍由 Decl.type 的 Func.retType 提供。L481–489 明确非 generic、无正文 constructor 不要求 ret node。这种角色提取不允许制造 Unit，也不能抹掉“ret node 不存在”的保存事实。 |
| `Expr.type` | `LoadExprRefs:588` | DECL.`deserializeLiteral/deserializeUnaryExpression/deserializeExpression/deserializeReference` 以及正式 SOURCE type/ref 消费链。每条 expression type 是自身语义槽，不能由 target.returnTypeRef 替代。 |
| `ReferenceInfo.target/instTys/matchedParentTy` | `LoadExprRefs:592–609` | target 经真实 FullId registry；instTys 为该 reference 保存的独立有序实参，parentTy 为独立可缺省槽。零 parentTy 在 CFIR `Absent` 视图中保持缺省，raw wire 仍保留零；不能补 Unit、receiver 或 owner 类型。 |
| FuncArg / catch、for、match Pattern type | `LoadSubNodeRefs:657`、[LoadPatternRefs:745/LoadMatchCaseRef:732](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L745) | 它们不是 allLoadedExprs 的普通 Expr 项，不能因只遍历 expression.ty 而漏读；正式 SOURCE 子槽读取同一 CTX/type cache。本轮只要求类型/引用物化，不重建后端控制流。 |
| block/Lambda 保存正文的类型边 | `LoadRefs` 的 node/refs 两遍及 `LoadExprRefs/LoadSubNodeRefs` | header-only 和正文 type graph 是否已消费必须有独立完成事实；body=null 不证明 type edge 不存在。 |

零引用的 API 必须按角色分为可缺省/真实 Invalid 或 initial/必填非零，而不是返回统一 Unit。可缺省字段使用真实缺省视图并保留 wire presence；required 有效字段必须指向实际类型。已有语言非法类型或损坏数据可走其本来的结构化诊断/装载门；不能把“尚未填完的合法回边”先转成 ConeErrorType，再由外层继续编译。

## 8. Generic 原组与最终 upperBounds 的恢复 owner

原组 schema 是 [ModuleFormat.fbs::Constraint/Generic，L51](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/schema/ModuleFormat.fbs#L51)：group 自己有 begin/end、subject type、uppers、isImplicitlyIntroduced；TP 是 `Generic.typeParameters` 的真实 Decl 列表。最终语义上界是 L600 的 `GenericTyInfo.upperBounds`。native `LoadGenericConstraintsRef` 与 `SetGenericTy` 各自生产对应的恢复产品。

生产应由 DECL/CTX 在 normal materialization 阶段同时完成：

- **原 group 产品**：保留 group ordinal、subject 的真实 Generic type/TP binding、所有 upper 槽与来源 span、implicit 标记。重复 group 与重复 upper 不合并，来源用本 context 的 `sourceLocation`/[CjoSourcePositionResolver](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoSourcePositionResolver.kt)（原审查坐标 L18） 按 position 自己的 pkg/file 解码。
- **final semantic 产品**：直接恢复真实 GenericTyInfo 的完成上界，绑定到实际 TP 及其实际 Generic owner。正常库消费不重跑 Source constraints 的 native reduction/assumption 算法，也不从原 groups 拼出“猜测的 final bounds”。同一 TP 被多个输入条目引用时保持 symbol identity；wire entry provenance 保持独立。
- **normal 类型系统 bounds 视图**：供类型检查/替换读取。无界 TP 的 normal default Any 可以继续由既有 `addDefaultBoundIfNecessary` 生产，并准确维护 hasImplicitDefaultUpperBound；该视图不替代前两个产品。必须先知道 final 是否完成，不能在 binder 创建时用 Any 宣布 final 完成。

现有 [CfirTypeConstraintDiagnosticData](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirTypeConstraintDiagnosticData.kt)（原审查坐标 L40） 仍是 parameterName/source/boundTypeRefs 形状，source 是 mandatory CjSourceElement，没有已解析 subject、CJO span/implicit 的完整载荷。CJO 原组不能靠创建假的 PSI/CjSourceElement 填它。T6 正常树产品需要同时承载 Source 与 CJO 的真实来源角色；reader 在同一产品合同中发布 binary position facts，imported WrapType 的 COMPILER_ADDED origin 仍是另一个事实。

| read/write site | 应读取/写入的完成产品 |
| --- | --- |
| DECL.`deserializeTypeParameters/applyOwnerGenericConstraints/convertTypeParameter` | 前两种 native 载荷分别恢复；normal bounds 的合法默认值独立处理。当前 applyOwnerGenericConstraints 的拼接只能说明原 group 投影，不能关闭 final gate。 |
| TY.`convertGenericType` 两种 symbol 命中路径 | 不论 CURRENT/IMPORTED 或命中缓存，仍须完成/登记该 GenericTyInfo 的 upperBounds 产品；命中 binder 不等于 bounds 已完成。 |
| [CfirTypeParameterSymbol.resolvedBounds L357](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/CfirBasedSymbol.kt)、[ConeInferenceContext.getUpperBounds L851](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeInferenceContext.kt) | 既有 normal 类型系统读 bounds，会调用 TYPES lazy resolve；materializer 内部造身份时不可提前调用，strict capture 也不可借此完成产品。 |
| [CfirDeclarationBuildingUtils.explicitUpperBounds L35 / addDefaultBoundIfNecessary L85](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/utils/CfirDeclarationBuildingUtils.kt) | explicit 与 default 的 normal 投影；不是 native final assumptions 的算法或 authoritative reader。 |
| [CfirExportTypePayload.captureExportTypePayload L73 / captureExportTypeDependencies L104](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypePayload.kt) | 当前 Generic 冻结 parameter.bounds 和 explicitUpperBounds。应读取 normal owner 已发布的独立 final 产品，同时保留原 group/source 产品；依赖闭包按身份访问，不能借遍历生产 bounds。 |
| [CfirCjoPackageMetadataProducer.prepareGenericTypeBounds L3231](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) | 当前仍读 pending.payload.explicitBounds；`type(FrozenTypeShape):3243` 写 GenericTyInfo。Source 完整 Generic final gate 仍 OPEN，不能因 S5 reader 修好而宣称此写端已经是 final。 |
| 同文件 `importedGenericMetadata:1323/importedTypeReference:2503` | raw imported 转存已分别保持原 group span/implicit 与 GenericTyInfo.upperBounds，并先 reserve 再 fill。保留这个真实 raw 图合同，但必须另验证 typed model；不能在 writer 补 synthetic TP 或重算 bounds。 |

冻结、owned copy、session/context 重建必须携带真实 TP binder、原 groups、final 产品与各自来源/完成状态。若创建另一份实际 declaration graph，则以明确的旧 symbol→新实际 symbol 映射迁移所有类型叶子；不改 bound 字面名字，不沿旧 session 的弱 lookup cache 接回旧 TP。旧快照不可被后来 materialization 修改。

## 9. Alias self / RHS / abbreviation / completed expansion

`TypeKind.Type` 是 native 明示的 TypeAliasTy；不是语言非法类型。`CompositeTyInfo.declPtr + SemaTy.typeArgs` 是 Alias 身份及应用，`AliasInfo.aliasedTy` 是声明 RHS。正常 reader 先 reserve Alias symbol 与 own TP，建立 identity 类型，再读 RHS，全部 RHS 就绪后应用真实参数并完成嵌套展开。

| 产品角色 | 完成与发布合同 |
| --- | --- |
| Alias self | 指向 actual Alias declaration，arguments 按该声明 own TP ordinal；保持 Type kind。self 可以没有内联 expandedType，但 reader 必须另有就绪的 RHS/展开产品，不能以 null RHS 宣称完整。 |
| 声明 RHS | 来自 aliasedTy，TP 叶子属于 actual Alias owner；不能读取 Decl.type、自身 self 或其他 use-site 的 expanded 缓存作 RHS。若 normal expandedTypeRef API 使用完成语义展开，还需保留输入 RHS/原 abbreviation，避免覆盖原角色。 |
| 原应用 / abbreviation | 保存 actual Alias binding、实际 arguments 与属性；内部 Tuple/Func/Pointer/VArray 等子槽里的 Alias 也保留。结构 equality 不代替 snapshot/TYPE 输入身份。 |
| 完成声明展开 | 在 normal reader/resolver 中，从本声明 RHS 与它自己的 TP bindings 计算；没有 import/阶段/Supplier 首次副作用留给 capture。 |
| 完成应用展开 | 以该次应用的 arguments 替换声明 TP，再完整展开类型树；不能复用另一个应用的返回值。semantic use 可用 expanded type＋AbbreviatedTypeAttribute，wire/self 角色仍保留原 Alias。 |

当前 [ConeTypeAliasType](../../cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeTypeAliasType.kt)（原审查坐标 L14） 仅持 ClassId、immutable nullable expandedType、arguments、attributes；没有实际 FullId binder。它的 equality 也只比较 ClassId/arguments。新增 Type kind 不能只补 `ConeTypeAliasType(classId)` 再在 capture 按名字找回声明。正常类型模型必须携带 actual Alias binding/lookup identity；ClassId 可继续是显示/查询标识。nominal 的 [ConeClassLikeLookupTagImpl](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/ConeClassLikeLookupTagImpl.kt)（原审查坐标 L16） 也只有按 session 的弱 boundSymbol，不能取代 snapshot registry；正常 reader 至少要用既有 [bindSymbolToLookupTag](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/LookupTagUtils.kt)（原审查坐标 L16） 绑定它已确定的真实 target，不能重新选择同名 parent。

模型模块边界也须保持：cfir-cones 通过已有 [lookup-tag 类型抽象](../../cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeClassLikeType.kt)（原审查坐标 L11） 承载身份合同；具体 Cfir symbols、snapshot/context 与绑定读取放在 tree/semantics.api/serialization 的现有 owner 中。不能让 cones 直接依赖 FlatBuffers Package、provider 或具体 CfirTypeAliasSymbol 来保存这份绑定。

不要为 immutable expandedType 的晚填充换掉已经返回的 type identity。将“原类型身份”与“完成 RHS/应用展开产品”分开保存在 normal owner，公开 semantic 使用视图在完成后发布，按真实应用及所属域读取。input raw TYPE memo 和 normal semantic equality 可以并存，但不能把前者改成 Cone.equals/hashCode 去重。

必须接通这些实际 readsites：

| 文件/方法 | 需要审查的行为 |
| --- | --- |
| [TypeExpansionUtils](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/TypeExpansionUtils.kt)（原审查坐标 L138） 的 `directExpansionType:138/fullyExpandedTypeNoCache:125/fullyExpandedTypeUsingAbbreviation:87/createParametersSubstitutor:198` | 从 actual Alias binder 和正常完成产品取 RHS；typeAlias 参数映射逐 actual symbol。现有 directExpansionType L151 和 abbreviation 路径 L101 仍调用 ClassId provider lookup。完整展开需进入各专用子槽，不把 nominal.typeArguments 为空误判为 RHS 无 TP。 |
| 同文件 `fullyExpandedType():41/expandedConeTypeWithEnsuredPhase:48/fullyExpandedType(session):69` | default Supplier 会 lazyResolve SUPER_TYPES。正常解析可用其 normal 合同；strict capture 必须读明确完成 getter，不能仍走 default，即使 cache 命中或 phase 已高也不能把首次物化藏在这里。 |
| [ToSymbolUtils.toSymbol L60/L110](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/ToSymbolUtils.kt) | 普通 weak ClassId lookup 会进入 provider；当前 ConeCangJieType.toSymbol 只处理 ConeLookupTagBasedType。新的 Alias binding 必须由正常模型识别，不能在各 capture readsite另做名字兜底。 |
| [CfirExportAnalysisInputSnapshot.captureType L798](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt) | 当前 L800/804 provider lookup、L806 default fullyExpandedType。这是仍待退役的 capture 工作；准备应在 normal loader/resolver 完成，capture 只读取并验证其来源、实参、RHS/展开产品。 |
| `CfirExportTypePayload.captureExportTypePayload` 与 `captureExportTypeDependencies` | Alias actual declaration、ordered arguments、完成展开分开冻结。self 不因要编码 Type kind 而强制现场展开或切换全局 Alias 设置。 |
| DECL.`convertTypeAlias`；TY.`convertSemaTy` 的 Type kind；metadata `frozenTypeShape/type` | 声明 RHS、原 Type 身份、semantic use 和 source/cjo domain 显式对应；writer 消费完成产品，不能修复 decoder 缺 binding。 |

若 Alias 模型新增 actual binding，所有实际构造与 copy 点必须同时保持它，不能只修 TY。以下为本轮搜索到的生产构造集合；参数/属性变更后的展开值也必须按新 arguments 重新取得正常完成产品，不能连同旧 cache 机械复制：

| 具体生产 write site | 应传递的事实 |
| --- | --- |
| [CfirTypeResolver.createResolvedClassLikeType L631](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirTypeResolver.kt)，L664/L671 | 已命中的实际 Alias symbol、source arguments、各角色展开。 |
| [ResolveUtils.constructDefaultQualifierType L290 / constructTypeForQualifiedAccess L311](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/ResolveUtils.kt)；[CfirExpressionsResolveTransformer.constructClassLikeType L6596](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt) | actual qualifier Alias binder；self arguments 来自本声明 own TP。 |
| [CfirBuiltInCallResolver.typeAliasVArrayConstructorTarget L246 / typeAliasPointerConstructorTarget L272](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirBuiltInCallResolver.kt) | actual Alias symbol、应用参数及 builtin RHS 的独立布局/子槽。 |
| [CfirCreateFreshTypeVariableSubstitutorStage.expandedTypeAliasQualifierType L1005](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirCreateFreshTypeVariableSubstitutorStage.kt)；[CfirCallCompleter.hasTypeAliasConstructorExpansionUpperBoundViolation L453](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt) | fresh 应用保留 original Alias binder，不把 fresh TP 误当原 GenericParamDecl。 |
| [CfirCallResolver.normalizeFreshReceiverTypeArguments L2725](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt)；[ConeOverloadConflictResolver L1221](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/overloads/ConeOverloadConflictResolver.kt) 的 Alias 重建分支 | 归一/恢复 arguments 时保持真实声明身份、属性、正确展开角色。 |
| [TypeUtils.withAttributes L417 / withArguments L471](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt)；[ConeInferenceContext.replaceArguments L669](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeInferenceContext.kt)；[AbstractConeSubstitutor.substituteTypeAlias L116](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/substitution/AbstractConeSubstitutor.kt) | 统一 copy/substitution 合同；保持 binder，真实替换每个有序槽，失效或重新发布应用展开。 |
| `CfirExportAnalysisInputSnapshot.declarationType:832`；[TypeConstructionUtils.constructType:58/76](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/TypeConstructionUtils.kt)（原审查坐标 L58） | declaration self 与普通应用构造保持 actual Alias 角色；当前通用 class-like constructType 的 else 会造 ConeClassLikeType，不能用它隐去 Alias identity。 |
| [ConeTypePointer 的 Alias createPointer/restore 分支 L72](../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/utils/ConeTypePointer.kt)；[CaCfirTypeCreator.createResolvedClassLikeType L198](../../analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirTypeCreator.kt) | Analysis pointer 必须通过实际 symbol pointer/所属域在新 session 恢复 binder；creator 已持实际 Alias symbol，应直接传递。不能仅复制 ClassId 或旧展开 cache。 |
| [coneDiagnosticToCfirDiagnostic.hasTypeAliasConstructorExpansionUpperBoundViolation L2959](../../cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt)；[CfirUpperBoundViolatedQualifiedAccessExpressionChecker.check L45](../../cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirUpperBoundViolatedQualifiedAccessExpressionChecker.kt) 的 L61/L74 | 诊断中的 Alias 重建也传实际 typeAliasSymbol、arguments 和正确应用展开；保留既有正常诊断职责，不在诊断/capture 阶段完成缺失 binder。 |

这部分列的是修复 binding 模型时必须接通的生命周期，不要求为本次只读报告修改这些生产位置。

## 10. header/body profile 与正式 SOURCE 的 type/ref 读取

DECL 当前 `convertFunctionOrEnumConstructor:1644`、`convertConstructorIfNeeded:1766/1784`、`deserializePropertyAccessor:1874` 都保留 body=null；class/struct/interface/enum 的声明成员列表仍由 `deserializeBody:1286` 加载。这是“函数正文未物化”和“成员声明列表”的不同层次。Kotlin FirMemberDeserializer 的库函数也是 metadata header，但 Kotlin 的 header 行为不证明 CJO 所保存的 SOURCE type/ref 字段可以丢弃。

正式消费需要区分原 CJO 未保存正文、保存但 header consumer 未请求、已物化正文/type-ref graph。该 availability 来自实际 FuncBody.body 等字段和明确的 reader profile；不能由 body=null 猜“源正文不存在”。compiled-file stub 可以继续只消费完整签名。S5 的 type 层应在正常输入图物化中准备保存正文内的 type/ref 槽及真实 context，不要求实现 CHIR、IR codegen、inline 展开或后端执行。

当前 `deserializeExpression:871` 只覆盖 literal/unary/array/ref 的有限路径，没有完整 SOURCE MemberAccess/Call/Lambda/Block 的 type/ref 恢复链。`deserializeReference:896` 更仅针对 enum constructor 按 class scope/name 查 symbol；非 enum ref 建 unresolved named reference，expression type 也没有从 Expr.type 读回，只用 enum constructor.returnTypeRef。这些路径不能冒充正式 SOURCE 的完整 typed reference 消费。

正确 owner 是正常 reader 的 identity→refs pass：所有已保存 target 由 CTX/ID 指向已登记的实际 declaration；每个 type/ref 槽使用同一 TY；不为 enum、Alias 或某个名字另建 symbol/provider 路径。类型层可以恢复所存正文的类型/引用记录而暂不构造后端 body IR，但必须明确它完成的是哪份输入 type/ref graph，不能直接跳过保存正文而宣布完整 SOURCE 消费完成。

### 10.1 This 应用签名与 call result 的角色继续保持

固定 native `LoadExprRefs` 对 Call 和 MemberAccess 分别加载各自 Expr.type。MemberAccess.ty 是 Func 时，其 return 可以是 ClassThisTy；Call.ty 是已计算的 nominal result。`instTys` 又是 ReferenceInfo 的独立向量。decoder 不应重新调用 GetCallTy 或用 call.type 替换 callee signature。

| 解码输入 → 本库读写侧 | 合同 |
| --- | --- |
| MemberAccess/RefExpr.`Expr.type = Func(params,ret)` → [CfirResolvedAppliedCallableReference](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/references/impl/CfirResolvedAppliedCallableReference.kt)（原审查坐标 L26） | 已应用签名的 parameterTypes 和 substitutedReturnType 来自该 Func 的独立槽，ClassThis/Generic 角色保留。 |
| Call.`Expr.type`，或真实函数值表达式的 type → 同模型 `callSiteType:58` | 完成 call/value 的结果类型独立保存。function value 角色由真实 reference/target/operand 使用上下文确定，不用“函数返回函数”猜测。 |
| ReferenceInfo.`instTys/matchedParentTy/target` | 读取原向量、原 parent 状态和实际 target；classT/U 位于返回 ClassTy 的 arguments 不意味着它们同时进入 reference.instTys。 |
| [CfirSourceGraphProducer.Context.appliedSignature L446 / resolvedReference L468](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt) | 当前 callee 签名已经读 substitutedReturnType，reference 产品另读 instantiatedTypes 与 matchedParentType。新 CJO decoder 要接同一完成角色，不能再次混用 call result。 |
| `CfirExportAnalysisInputSnapshot.resolvedReference:456`、SOURCE expression record type 捕获 | target、instantiatedTypes、matchedParentType 和 expression 自身 type 分开进入 closure；owned copy、payload/snapshot 保留两种 return/result 与独立 type arguments。 |

这是对新 reader 的消费合同，并不重新声称 orderedAliasRoles 当前失败在 instTys。用户已撤销该猜测，真实 This 断言和原 fixture/期望保持。

## 11. 此任务不支持移植的 Kotlin 编码/后端语义

| Kotlin 机制 | 固定证据和本轮边界 |
| --- | --- |
| nullable/flexible/dynamic、DNN、STAR/variance projection、suspend functional metadata | FIR 的 `type/rigidType/typeArgument` 和 IR 的 `deserializeIrTypeData` 有这些分支；native ModuleFormat 的 TypeKind/SemaTyInfo/typeArgs 没有同样的编码槽。CJO 按实际 nominal/结构类型与 native 字段恢复，不能造 Kotlin kind、STAR/default projection 或 synthetic type 处理普通 CJO。此结论限定 wire 合同，不用缺字段断言所有仓颉语言特性均不存在。 |
| Kotlin outerType argument 累加、nested typealias metadata context | FIR 在 `collectAllArguments` 与 provider nested context 中有该规则；CJO 只有它实际保存的 FullId 与 typeArgs/owner 关系。不能自行拼 Outer.Inner 或追加外层 TP 到原向量。内部声明身份仍按真实索引处理。 |
| IR partial linkage、错误 symbol kind fallback、allowErrorNodes | IrFileDeserializer L181 与 IrDeclarationDeserializer 的 checkSymbolType/error-type setting 属于该 IR 后端/缺失库合同；不能把合法 CJO TP/Alias 回环转成 wrong-kind/error/unbound 替代对象。 |
| IR deserializeTypeAliases 选项、function body strategy/fake overrides | 是 KLIB/IR 消费配置，不能关闭 native 明示 TypeAliasTy 或省略正式 SOURCE 的所存类型边。Kotlin header-only 对位只用于明确的 header 消费 profile。 |
| native incremental `isLoadCache`、LSP `fromLsp` 例外 | 固定源码有明确模式限制；本任务消费正式 SOURCE 新 CJO，不能当作跳过 final bounds 或全局不展开 Alias 的理由。 |

## 12. 主线程实施后必须使用的真实测试

本轮未新增或运行以下测试。不是给原 SOURCE 添桥、改期望或用手写 FlatBuffers 循环取代官方成功产物。新增合法递归/跨包用例应先由官方编译产物确认，记录 SOURCE SHA、native build/version 与 CJO 来源；typed consumer 再读取本库正式 SAVE_CJO 刚生成的产物。

| 验证面 | 必须走的实际路径与断言 |
| --- | --- |
| 原 orderedAliasRoles / functionAbi，PSI 与 LT | 延续 [CfirSourceFrozenTypesTest/LightTree](../../cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CfirSourceFrozenTypesTest.kt)（原审查坐标 L59） 的原源码、native archive、正式 frontend→SAVE_CJO；追加 **fresh reader/session 的 typed 消费**。Alias self/RHS/链、ClassThis、C/variadic、ordered duplicates 都用 actual target identity 断言，SOURCE SHA 与 bytes 不变。 |
| 为什么原通过不足 | 该 test L179/212 是 `Package.getRootAsPackage → FrozenTypesGraph`，检查正常写出的 raw 图，未调用 CfirTypeDeserializer；L389–400 明确这两个候选 constraints 与 GenericTyInfo.upperBounds 都空。SOURCE48 通过不能覆盖完整 final bounds 和 typed reader。 |
| 请求次序 | 在普通 provider 中分别先请求返回 type、Alias 应用、TP/成员，再请求其 owner；不得手动先塞 declCache。结果必须绑定同一真实 TP/Alias，owner 与所属 moduleData 正确。 |
| 合法递归 bounds 与前向 TP | native 成功 CJO 中的 `T → I<T> → T` 等真实合法图，及 bounds 引用后续 own TP 的图；正常 reader 完成，所有回边 symbol===actual owner TP；没有 synthetic/error/fallback allocation，重复读取同一 raw type index 不产生另一份结果。直接非法循环另走官方负例诊断，不能把两类图合并。 |
| 原组与 final 不同 | 官方 Generic 成功样本含多个原组、上界规范化/派生约束；原 groups 保留数量、顺序、source/implicit，final 与官方 GenericTyInfo 一致。不能只断言原组 upper 与 TP.bounds “看起来一样”。 |
| 无界与 explicit Any | 无界 TP 的 wire final=[] 与 normal default Any 各自正确；显式 Any、其他真实 final 上界不因 source=null/COMPILER_ADDED 被当成 default 或丢弃。 |
| 跨包 TP/Alias/重导出 | 至少两个真实包，各有同名 TP；从实际 GenericTyInfo/CompositeTyInfo.FullId、exportId 走 provider context。签名/arguments/RHS/final bounds 绑定源包实际 declaration，不生成同名 TP，转出口不取得声明所有权。 |
| 同包多快照与重新加载 | 使用实际多 parent 输入和明确依赖选择计划；CURRENT indices 与 imported references 进入被选择的 snapshot context。同路径新代、另一 provider/session、另一个表域的相同 raw index 不共享 memo/旧 weak symbol。 |
| structural 子槽 | Tuple/Func return/Pointer/VArray/RawArray 嵌套 Alias/TP，包含重复 argument/type index；保留有序槽、dims/size/C/variadic/This，完整应用展开不漏专用字段。 |
| 零引用 / Unit / Invalid | 官方保存的 absent matchedParentTy、可缺省 ret node 与真实 Unit/Invalid 表项；typed 缺省和 raw presence 各自正确，0 不变 Unit，合法 type graph 不变 ConeErrorType。constructor header 的 ret-node 省略与 signature return 保持独立。 |
| SOURCE type/ref closure | 保存正文含 MemberAccess/Call/Lambda、catch/for/match Pattern 的真实类型边；native pattern.types[0] 与 Except/Command tail 各槽均读到共享 CTX，不能只覆盖 Expr.type。明确 header-only profile 保留 availability，完整 type 消费另断言完成。 |
| This / instantiation roles | callee Func.return=ClassThis，Call.type=nominal；原 reference 的空 instTys 保持空。另测有 own function TP 和函数返回函数的调用，防止外层 class TP 或 isFunctionValue 猜测替代本来的角色。 |
| normal generic call 与 Analysis 使用 | 新 CJO 作为真实 dependency 经 CfirDeserializedSymbolProvider，SOURCE 对 imported generic 函数/Alias 构造作正常调用，fresh substitutor 命中 actual TP；Analysis type creator/pointer restore 与正常诊断保持同一 actual Alias binder。现有 [CjoSdkDeserializationIntegrationTest](../../cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjo/CjoSdkDeserializationIntegrationTest.kt)（原审查坐标 L20） 可作为真实库入口，但不能只用其手动 context 代替正式 provider 接线测试。 |
| 并发真实 materialization | 两个独立查询交错读取同一/跨包依赖图，首次和重复请求都完成；实际 binder 不重复、没有半完成值或相互等待。完整 cache 的并发复用不能仅靠单线程预热掩盖。 |
| strict capture | 先完成正常 reader/resolver；随后对 lazyResolver、default expansion Supplier、provider materialization/new context、文件装载安装真实计数或拒绝守卫。capture→validate→write 前后 phase/cache/bindings/final bounds/展开产品不变，禁止通过 capture 首次推进或“预热”补产品。 |
| owned copy / restore /失效 | 完成图复制或新 session/context 重建后，声明、TP、Alias、原组、final、abbreviation、signature/result、source/CJO domain 有真实映射；旧冻结图不变，旧 provenance 不混进新 snapshot。 |

## 13. 实施次序与剩余 gate

生产路径的依赖顺序是：BIN/ABI 快照 context registry → ID owner/FullId binding → DECL 真正 symbol/TP 身份登记 → TY 结构类型与 Generic identity-first → DECL/CTX 原组、final bounds、RHS/refs 回填 → Alias 完成使用视图与 header/content 完成门 → 普通 scope/query 与 SOURCE type 消费 → strict capture/metadata 的完成产品只读接线 → 真实新 CJO typed round trip。

当前仍 OPEN 的项有：

1. `TypeKind.Type` 的实际 Alias 解码，以及合法递归/未先请求/跨包 TP 的统一实际 binder。
2. CTX 的 identity/complete 状态、跨包/context 物化事务及真实 owner 反向索引；多个 parent 的 target context 选择不能沿用 first 当合同。
3. 原 Generic groups/source/implicit 与 GenericTyInfo.final bounds 的独立正常恢复/发布；Source 写端仍读 explicitBounds 的 T6 缺口也未因 SOURCE48 而关闭。
4. 零字段、Unit、真实 Invalid 的角色化 API；所有有序结构子槽与保存 SOURCE refs/Pattern type 边。
5. 完成 Alias binding/RHS/应用展开、copy/pointer 生命周期与 capture 中 default Supplier/provider 首次工作的退役。
6. header-only availability 与完整 type 消费的真实测试，以及 This signature/result/instTys 的 independent roles 在新 reader 中保持。

这些是生产实施和真实验证 gate；本轮只完成固定证据、精确 owner 与可实施生命周期映射，没有宣称实现已合并、编译或上述测试通过。

## 14. 本轮工作文件指纹

以下 SHA-256 为本轮读取的工作文件，不把用户的执行 SHA 当成本轮执行记录；TY/DECL 在报告落成前复读指纹一致。

| 文件（路径见上文对应链接） | SHA-256 |
| --- | --- |
| TY：CfirTypeDeserializer.kt | `DD2C69E55196133A473C5F7E52AC02910BD908FE711706FA7E2FAAB6F4D2A39E` |
| DECL：CfirDeclDeserializer.kt | `5010AB80A659786C53EC292F669EE5BBD31D2CCDF5F087D1404D14A02C416A53` |
| CTX：CfirDeserializationContext.kt | `3860C1866751E46112575D1463DB451A163CB16BE87D6CAFD6E8C4098F3D123E` |
| ID：CjoFullIdResolver.kt | `2305609EE4BE3D5193C8EFC25CA007134FEC24469E44ED3E8851D51CD19AE27D` |
| ABI：AbstractCfirDeserializedSymbolProvider.kt | `5DD0363A277523A72A77B3E0270CB692C7256F76A2FF22DE245B49760F7F20CA` |
| BIN：CfirDeserializedSymbolProvider.kt | `F6395701819509837BD91BC64FAA7B9EAA1E3B2F9BEC4236C945921C9E8AE467` |
| TypeExpansionUtils.kt | `80665DDB53CA8587BF86C076F7EA8293240C3EC4A85F2AA2B189394D8EA3A485` |
| CfirExportAnalysisInputSnapshot.kt | `DAF4FCCF1FB44BA762F7CA8C8974621FC2D81064208819FF89C02A2000DE94B7` |
| CfirExportTypePayload.kt | `DDCE9F6021CA1F7F2EFF8DB89E421D81A4C4C6E050E39A822FF03EC468072BE4` |
| CfirCjoPackageMetadataProducer.kt | `3B64C0A9C912151FCC9334A8139841BBD120BE5401FB599D71FE4FAF64AAADEB` |
| CfirSourceGraphProducer.kt | `B07875129A15C40ADC9F59CB9490EEEFEE0A505F033B13789ADB20771FE6836D` |

