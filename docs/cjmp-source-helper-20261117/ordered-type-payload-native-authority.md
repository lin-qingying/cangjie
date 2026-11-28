# 有序类型输入的固定 native 考证

复用 [source-frozen-types-native-authority-20261118.md](../../build/source-expression-codec-audit-v113/source-frozen-types-native-authority-20261118.md)。本轮只读指定工作树，未改文件、未跑 Gradle、未联系或停止旧会话，使用 PowerShell 7.6.9、UTF-8。

native 行号均来自 `14276740ffec1262b81b6b5236b1d840af270b37`，Kotlin 行号均来自 `3c7a5041f238102285764d370665f56f587f9dc4` 的 `git show`；镜像链接用于定位文件。工作树行号按本轮实际读取校正。

**1. Generic 语义 upperBounds：当前两个接口不能准确提供。**

[native Types.h L989–994](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/AST/Types.h#L989) 的合同确实是：**所有直接上界，加上沿 Generic 上界链可达的非 Generic 上界**。

[native PreCheck.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/PreCheck.cpp#L1106) 的准确过程：

- L1114：以当前集合为起点，因此保留直接 Generic 上界。
- L1124–1128：遇到 Generic 只继续遍历；遇到非 Generic 才补入结果。**不会补入仅传递可达的 Generic，也不会沿 nominal 的继承树展开全部父类型。**
- L1129–1130：同时传播 constraint blame。
- L1134–1135：删除自身，发布集合。
- L1153–1158：约束检查、合法性检查、暴露闭包，然后 sanity；L934–956 的 class bounds reduction 还可能缩减或清空集合。因此冻结对象应是**这些步骤完成后的实际语义集合**。
- L1196–1209：先收集所有候选的 assumptions，再处理 Alias 参数和 extend assumptions，最后检查。不能逐参数刚解析完便宣布最终产品已完成。

直接 assumption 来源是 [Assumption.cpp L99–123](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Assumption.cpp#L99)：实际实例化 sub/upper 后加入关系；L70–81 还会按类型替换递归收集上界声明的约束。这与最后的 Generic 链暴露是两个步骤。

排序也不能简化为名字排序。[ASTWriter.cpp L1015–1030](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L1015) 将最终集合放入 `set<…, CmpTyByName>`，然后写出。比较规则为：

| 顺位 | 固定规则 |
|---|---|
| 第一 | `Ty::String()` 的 `std::string` 字典序 |
| 同 String、双方都有声明 | 比较声明 `begin.fileID`，再比较 `begin.line/column`，同位置比较声明指针 |
| 同 String、任一没有声明 | 比较 Ty 指针 |
| 空指针 | 比较指针 |

证据：[Types.cpp L1038–1050](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/AST/Types.cpp#L1038)、[Node.h L3075–3084](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/AST/Node.h#L3075)、[Position.cpp L21–33](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Basic/Position.cpp#L21)。`String()` 还包含种类前缀：Types.cpp L798–830 的 `Generics-`、`Class-`、`Interface-`、`Struct-`、`TypeAlias-`；不能换成 ClassId 或调试 renderer。指针末级排序是进程内身份规则，不能声称 JVM 任意编号与 native 地址顺序完全一致。

本地 [CfirTypeResolveTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt)（L210）：

| 位置 | 实际能力及缺口 |
|---|---|
| L210–215 `resolveTypeParameterBounds` | 解析 bounds、切断递归上界；没有完成上述 assumption 收集与暴露产品 |
| L302–311 | 仅 extend 入口调用投影 |
| L870–889 `collectAssumptionBounds` | 按声明侧参数收集 bounds，并递归相关声明；没有实现上述 Generic 链结果集合 |
| L897–899 | 预过滤为展开后的 `ConeClassLikeType`；不能代表 native 先收集实际关系、再诊断和 sanity 的合同 |
| L929–936 `appendAssumptionBounds` | 去重后追加无 source 的 TypeRef；没有最终闭包、native 顺序或 constraint 来源标志 |

[explicitUpperBounds L35–60](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/utils/CfirDeclarationBuildingUtils.kt)（L35） 只从当前 bounds 中过滤 `source == null` 的默认 Any。它既不是最终语义闭包，也不是原始 where 序列；assumption 追加项仍会保留。

**本地生产建议：**在 resolve 的共同 owner 流程发布独立的最终语义上界产品，覆盖 eager 与 designated TYPES 路径；前置条件是相关声明 bounds、替换关系和 extend assumptions 已完成。保留来源约束与诊断关系，再执行暴露及 sanity。冻结准备阶段据最终投影后的类型和声明来源确定 native 顺序，writer 消费完成序列。

源码 constraints 必须另存分组、有序 uppers、位置、被约束类型和 `isImplicitlyIntroduced`。native PreCheck L1469–1478 保留原约束，再创建并标记新增 extend constraints；ASTWriter L1145–1156 按实际 constraint 节点写出。当前 [Producer L1760–1768](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L1760） 与 L3373 都读取 `explicitUpperBounds`，不能同时满足这两个产品。

**2. Alias：保留资格是节点路径事实，三个槽必须独立。**

[native TypeManager.cpp L2411–2488](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L2411) 的优先级：

1. 节点缺失或 semantic Ty 不正确：返回 native Invalid。
2. `COMPILER_ADD`：直接返回 `node->ty`。
3. Type 节点已有非 Initial `aliasTy`：只有别名声明满足 `IsExportedDecl() || IMPLICIT_USED` 才返回 `aliasTy`；否则直接返回实际 `node->ty`。
4. 其他节点按实际角色处理参数、返回值、tuple、VArray 等子槽。未写显式类型的变量/参数使用 `node->ty`。

保留资格**不等于仅 public，也不等于 linkage 已变 EXTERNAL**。[Decl::IsExportedDecl L968–985](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/AST/Node.cpp#L968)：public/protected 为真；internal 取决于 `noSubPkg`；private 为假。`IMPLICIT_USED` 是额外条件。不能因真实引用提升 linkage 就改写该判断。

| 独立槽 | native 内容及固定行号 |
|---|---|
| Alias 声明 self type | `TypeAliasTy`＋own TP 实参＋别名声明绑定。PreCheck L638–640、L734–746；ASTWriter L958–963、L995–1012 写 wire `Type＋CompositeTyInfo` |
| 使用槽 | 对具体 Type/声明节点执行 `ObtainsAliasType`；可保留实例化 Alias 身份与实参，也可选择实际类型 |
| RHS 槽 | ASTWriter L1448–1460 单独执行 `ObtainsAliasType(typeAliasDecl.type)`，写 `AliasInfo.aliasedTy`；RHS 若引用符合资格的另一个 Alias，可以继续保留 Alias 链 |

`ObtainsAliasType(&TypeAliasDecl)` 默认分支返回声明自身 `node->ty`；这里的声明节点与 `typeAliasDecl.type` 的 RHS Type 节点不能混淆。

resolve 已有合适的来源入口：native PreCheck L850–863 在展开之前保存实例化 `aliasTy`；本地 [CfirTypeResolver L658–673](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirTypeResolver.kt)（L658） 构造 Alias 实参，并由展开路径保留 abbreviation。

**本地生产建议：**

- resolve 发布每次使用的 semantic type、完整 abbreviated type/实参、真实 Alias 绑定及显式类型/`COMPILER_ADD` 来源；不能只保留 abbreviated declaration。
- [transformTypeAlias L621–628](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt)（L621） 完成后，独立提供 self Alias type 与 RHS 已解析角色图。
- SOURCE 分析冻结 exportability、`IMPLICIT_USED` 后，再确定每个使用槽的选择。该决定依赖节点角色，不能只按 Cone 身份缓存一份全局“Alias 或 expanded”选择。

当前 snapshot L794 把 Alias 声明 type 取成 RHS；Producer L1936 和 L2062 又复用同一 captured reference。三槽仍未分离。另见 [CfirExportAnalysis L360–380、L608–610、L664](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysis.kt)（L360）：导出资格与 EXTERNAL 提升已有不同路径，应继续分别冻结。

固定 Kotlin [FirElementSerializer L792–803](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-serialization/src/org/jetbrains/kotlin/fir/serialization/FirElementSerializer.kt#L792) 区分 underlying/expanded；L995–1019 消费 abbreviation。这支持角色分离，不能替代 native 的三槽及保留资格规则。

**3. readonly capture 可以冻结已有标量；当前实现仍不满足严格只读合同。**

可直接读取并冻结的现有事实包括：

- `ConePrimitiveType.kind`。
- `ConeClassLikeType.isInterface/isThisType`。
- `ConeVArrayType.size`。
- `ConeFunctionType.isCFunc/isClosureType/hasVariableLenArg`。
- Alias 的 ClassId、现有 abbreviated 类型、实参及既有展开关联。

这些标量无需重新解析；有序参数、实参、tuple 元素需要复制为冻结角色列表并保留重复。

[CfirExportTypeElement L49–55](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourceElement.kt)（L49） 当前只有依赖边与部分绑定。[captureType L749–777](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt)（L749） 存在三个具体问题：

- L764–766 对 children 去重，不能据此恢复有序、可重复的 wire 槽。
- L769 只保存 abbreviation 的声明，未冻结完整 abbreviated 类型。
- L770 调用默认 `fullyExpandedType(session)`；[TypeExpansionUtils L48–50、L69–75、L151–160](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/TypeExpansionUtils.kt)（L48） 会查询 provider、推进 Alias 到 SUPER_TYPES 并进行替换。严格捕获应读取已发布产品。

snapshot L249–284 也没有逐项验证类型 payload；仅保留 Cone 对象身份不能证明完整类型内容被冻结。

**不应由 serializer 发明的产品：**Generic assumption/闭包/sanity 结果及排序事实；constraint 分组、位置和 implicit 来源；Alias 使用资格、self type、RHS；RawArray 规范化尺寸；函数已裁决 ABI；真实 classifier/TP 绑定；投影后的完成类型。serializer 负责固定格式映射、引用编号及写表。

**4. RawArray 与函数字段对应。**

| native / 本地事实 | 正确 SOURCE 对应 |
|---|---|
| RawArray 嵌套 | TypeManager L113–126 合并已有 Array 元素层：叶子 element＋累计 dims；ASTWriter L1067–1077 写单个 `typeArgs[0]` 与 `ArrayTyInfo.dimsOrSize` |
| `RawArray<RawArray<T>>` | 在各层已正确解析的前提下，结果为 element=`T`、dims=`2` |
| VArray | `dimsOrSize` 表示实际 size；不能按 RawArray 维度解释 |
| `FuncTy.paramTys` | `SemaTy.typeArgs`，仅参数列表 |
| `FuncTy.retTy` | `FuncTyInfo.retType`，独立返回槽 |
| `FuncTy.isC` ↔ `ConeFunctionType.isCFunc` | `FuncTyInfo.isC` |
| `FuncTy.isClosureTy` ↔ `isClosureType` | 保留来源事实；SOURCE 无此 wire 字段 |
| `hasVariableLenArg` | `FuncTyInfo.hasVariableLenArg` |

固定证据：ASTWriter L1033–1050、[Types.h L684–722](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/AST/Types.h#L684)、[schema L591–612](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/schema/ModuleFormat.fbs#L591)。native 明确声明 closure Ty 不出现于 AST；loader L370–371 以 `{isC, false, hasVariableLenArg}` 恢复函数类型。**不能把“源码 lambda”直接解释成 native `isClosureTy=true`。**

本地 [TypeConstructionUtils L106–110](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/symbols/TypeConstructionUtils.kt)（L106） 的 RawArray 仍是普通 `ConeClassLikeType`，没有 dims 标量；Producer L3377–3381 固定写 1。应在 resolve 的 BuiltIn 类型构造入口发布叶子元素与累计维度，供 capture 冻结。

[现有 ConeFunctionType L14–26](../../cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeFunctionType.kt)（L14） 已具备三个独立字段；Producer L3408–3411 的 wire 字段对应正确，需改为消费冻结值。声明函数的 C 标记应来自已裁决 `resolvedAbi.isCFunction`；native PreCheck L1798–1802 已明确 C、foreign 模式及变长参数来源，无需扩展后端。
