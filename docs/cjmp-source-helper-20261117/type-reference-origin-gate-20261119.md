# SOURCE Type 来源基础产品定向审查（2026-11-19）

**裁定：基础产品的字段与主链已建立，但创建／复制／原始节点合同尚未闭合；不接受“基础合同完整”的声明。** 以下 G1–G7 是可直接修复的生产 finding，不以尚未实现 TypeUseGraph selector 为阻塞理由。本轮不签发完整 Alias selector 或 S4，也不签发行为 accept。

唯一生产／测试树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`；读取时 HEAD 为 `b4dd742d589e4b5ee6592d454634c44f634df08a`，审查对象包括该树未提交内容。只读生产、测试和已有文档；未运行 Gradle/cjc，未联系旧会话，唯一写入是本文件。`scopeSource4` 按本轮输入仍处于准备运行状态；准备状态、源码断言和此前其他批次的结果均不构成本轮行为通过证据。

上游内容均从 `D:/code/intellij/cangjie/external` 的两个 Git 仓库以固定提交 `git show` 读取：Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`，native `14276740ffec1262b81b6b5236b1d840af270b37`。下列上游链接均核实了固定提交中的文件／行内容，并对无 fragment 的 URL 执行 HEAD，返回 HTTP 200；GitCode 页面可达性不代替本地 Git 对象证据。仓库文件链接均为相对此文档的 RepoRelative 链接；本地行号为读取时位置。

## 已核实的基础合同

| 产品／owner | 读取结论与位置 | 本轮可接受的范围 |
| --- | --- | --- |
| 生成式 TypeRef 字段 | [CfirTree.kt](../../cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/CfirTree.kt)（原审查坐标 L1811） 定义 `typeUseOrigin`，带 replace；[BuilderConfigurator.kt](../../cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/BuilderConfigurator.kt)（原审查坐标 L198） 统一默认 `SEMANTIC`。运行字段名是 `typeUseOrigin`，并非 `sourceOrigin`。 | 来源属于 IR 节点产品，不能只在 serializer 或 external map 补标记。默认值是 known-type constructor 合同，不应全局改成 SOURCE。 |
| 所有生成叶子 | [ResolvedTypeRefBuilder](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/types/builder/CfirResolvedTypeRefBuilder.kt)（原审查坐标 L27） 将字段传给 impl，copy 在 L62 复制。Basic/User/Function/Tuple/VArray/Implicit 的生成 copy 同样复制；Option 没有 DSL copy，但 owned-copy 的直接 constructor 携带字段。 | 字段生成与现有生成副本一致。 |
| 两 Raw 的具体类型分派 | [PsiConversionUtils.kt](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt)（原审查坐标 L79） L95、[LightTreeTypeConverter.kt](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt)（原审查坐标 L62） L85 对具体分派结果显式发布 SOURCE；泛型实参、函数参数／返回、tuple、VArray、option 递归进入这些分派。 | 已进入分派的原始类型节点有来源；绕过入口与早返回见 G4/G6/G7。 |
| SpecificTypeResolver 主输出 | [CfirSpecificTypeResolverTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSpecificTypeResolverTransformer.kt)（原审查坐标 L131） 成功 L133、错误 L153 复制 origin，并保存 `delegatedTypeRef = typeRef`。 | 成功／错误主路径均保留原型；不能推及全部补全与恢复输出。 |
| 公共类型替换／原型工具 | [TypeUtils.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt)（原审查坐标 L63） 三个分支复制 origin，并保留既有 delegation；[CopyUtils.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt)（原审查坐标 L19） 对 resolved 走公共替换，对其他原型调用 [toCfirResolvedTypeRef](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt)（原审查坐标 L291），由传入原型复制 origin，无原型时为 SEMANTIC。 | 公共工具合同已建立；私有同名工具不受此结论覆盖，见 G1。 |
| 手写 Error 与 ResolvedImplicit | [CfirErrorTypeRefBuilder.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/builder/CfirErrorTypeRefBuilder.kt)（原审查坐标 L61） 传递 origin，L103 copy；[Error impl](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/impl/CfirErrorTypeRefImpl.kt)（原审查坐标 L53） 换 source 不改 origin；[ResolvedImplicitTypeRef.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/impl/ResolvedImplicitTypeRef.kt)（原审查坐标 L27） 固定 SEMANTIC，拒绝其他 origin，并拒绝非 null source。 | 手写节点本身正确；通用 source-copy 的分派未满足这些节点合同，见 G2。 |
| source 迁移与 owned copy | [LLCfirFileSourceGeneration.kt](../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/file/builder/LLCfirFileSourceGeneration.kt)（原审查坐标 L274） 迁移调用 `replaceSource`；[OwnedGraphCopyPrinter.kt](../../cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/printer/OwnedGraphCopyPrinter.kt)（原审查坐标 L69） 从同一生成模型分配字段；[CfirDeepCopyOwnedGraphVisitor.kt](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/copy/CfirDeepCopyOwnedGraphVisitor.kt)（原审查坐标 L1375） 使用生成 copy，Option L1439 直接复制 origin。手写错误走 [专用 visitor](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/copy/CfirOwnedGraphCopyVisitor.kt)（原审查坐标 L119）。 | 原地 source 迁移与已有 owned-copy 不从 fakeSource 推导 origin。实际生成文件名是 `CfirDeepCopyOwnedGraphVisitor.kt`，未发现名为 `generatedOwnedCopy` 的独立声明。 |
| CJO 已有包裹入口 | [CfirDeclDeserializer.buildTypeRef](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeclDeserializer.kt)（原审查坐标 L1194） L1200 与 [Generic 上界包裹](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirTypeDeserializer.kt)（原审查坐标 L328） L332 显式 COMPILER_ADDED，source 为 null。 | 与 native imported Type 包裹合同相符；null source 仍可有真实生成节点。不能泛化成所有已知／合成 Cone 包裹都应 COMPILER_ADDED。 |
| Snapshot Type／Qualifier 角色 | [CfirExportSourcePayload.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourcePayload.kt)（原审查坐标 L28） TypeReference 冻结 origin；Resolved 原型、User 的 qualifier、每段 arguments、Function params/return、Tuple elements、VArray element/size、Option component 独立有序，L236–244 捕获。 | 列表复制为不可修改列表，重复槽保留。Qualifier payload 自身只有 name/arguments，**没有 origin／节点 flags**；不应宣称它已经保存每个原生 Type 节点的来源。 |
| Declaration 与校验 | [CfirExportAnalysisInputSnapshot.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt)（原审查坐标 L127） 冻结 `headerTypeRef` 和 `superTypeReferences`，L556 捕获；L137–138 对 identity／次序核验；L302 比较重新捕获的 source payload。生成式 [ResolvedTypeRef 来源遍历](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/types/CfirResolvedTypeRef.kt) 包括 delegation，Error 也包括 partially-resolved 引用。 | 对实际已经存在并遍历到的节点，origin 换代可被发现。校验不能发现 Raw 阶段已经抹掉的外层节点，也不会自动纠正创建时就错误的 origin。 |

Kotlin 框架定位已核实：[FirTree.typeRef/resolvedTypeRef L1590–1603](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/tree-generator/src/org/jetbrains/kotlin/fir/tree/generator/FirTree.kt#L1590)、[生成 resolved builder/copy](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/gen/org/jetbrains/kotlin/fir/types/builder/FirResolvedTypeRefBuilder.kt)、[SpecificTypeResolver](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirSpecificTypeResolverTransformer.kt#L141)、[semantics.api CopyUtils L42](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/semantics.api/src/org/jetbrains/kotlin/fir/CopyUtils.kt#L42)、[semantics.api types/TypeUtils L494](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/semantics.api/src/org/jetbrains/kotlin/fir/types/TypeUtils.kt#L494)。本地同角色分别位于 `cfir-tree/tree-generator`、`cfir-tree/gen`、`resolve/transformers`、`semantics.api/CopyUtils.kt`、`semantics.api/types/TypeUtils.kt`。Kotlin 没有这个三值 SOURCE origin 字段；它是 native Type 节点合同的语义适配，不能称为 Kotlin 字段的原样照搬，也不应因此直接否决其生成模型归属。

## G1 [P1] 调用补全重建丢失来源，且私有原型工具绕过公共合同

固定修复路径：

- [CfirCallCompletionResultsWriterTransformer.kt L1540](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt)：`computeTypeArguments` 的成功／错误新 builder（L1549–1560）复制 source、cone、delegation，未复制 `sourceTypeArgument.typeUseOrigin`。
- [同文件 L2012](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt)：私有 `resolvedTypeFromPrototype` 两分支均回到默认 SEMANTIC。
- [CfirCallCompleter.kt L1374](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt)：另一份私有同名工具存在同样问题。

触发：已有 SOURCE 的显式调用类型实参经完成阶段重建，或已有非 SEMANTIC 原型进入上述私有工具。新 ref 会成为 SEMANTIC，即使原始 type syntax 仍由 delegation 持有。这个结果可由源码直接确定，不需要 selector 才成立。私有工具另会把已 resolved 原型本身重新作为 delegation，偏离公共工具保留既有 delegation 的合同；Writer L1686、Completer L1069 已有实际调用点。

Kotlin [computeTypeArguments L1035–1055](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt#L1035) 对显式投影使用原 `typeRef.withReplacedConeType(type)`，省略实参才从 Cone 新建。其 [公共原型 helper](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/semantics.api/src/org/jetbrains/kotlin/fir/CopyUtils.kt#L42) 同样对 resolved 使用公共替换。

修复合同：补全显式实参复用公共 resolved 替换合同，保留 origin／原 delegation／错误附属信息；删除两份私有同名原型实现并使用公共 owner。纯推断实参仍为 SEMANTIC。不要用 delegation 是否存在来覆盖当前节点已经发布的分类；那会再次把 presence 当成推断结果。

## G2 [P1] source-copy 的抽象分派吞掉手写 Error，并退化 ResolvedImplicit

固定修复路径：[cfir-tree/utils.kt L210](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt)。

L214 首先匹配 `is CfirResolvedTypeRef`，而 [CfirErrorTypeRef L21](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/types/CfirErrorTypeRef.kt) 继承该抽象类，故 L218 的 error-copy 分支不可达。换 source 后实际生成普通 resolved impl，diagnostic／partiallyResolvedTypeRef 丢失；若调用方以 `CfirErrorTypeRef` 接收泛型返回，还会在类型转换处失败。origin 虽被 resolved copy 携带，也不足以证明手写 error 合同完整。现有 [PrimaryConstructorParameterFields L88](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/PrimaryConstructorParameterFields.kt) 和公共 TypeUtils 的 partial-ref 迁移实际调用此入口。

固定 Kotlin [Utils.kt L62–67](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/src/org/jetbrains/kotlin/fir/Utils.kt#L62) 首先匹配具体 `FirResolvedTypeRefImpl`，随后匹配 `FirErrorTypeRef`，没有扩大为 resolved 抽象类。修复须恢复该 concrete／手写节点分派，保持专用 error copy 和 partial 迁移。

同入口 L279 对任何 `CfirImplicitTypeRef` 构造 `CfirImplicitTypeRefImpl`，也会把 [ResolvedImplicitTypeRef](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/impl/ResolvedImplicitTypeRef.kt)（原审查坐标 L21） 退化成普通 implicit，丢失被包裹的已知 ref。非 null source 迁移应依该手写节点现有合同拒绝；允许的复制必须保留具体 wrapper 和其内部 ref。不能通过生成另一个普通 implicit 绕过 `replaceSource`／origin 的拒绝合同。owned-copy 的专用 wrapper 分派 L1414 已提供对照。

## G3 [P1] Paren 无条件折叠丢失原节点；仅根 origin 不足以证明原语法完整

固定修复路径：[PsiConversionUtils.kt L100](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt)、[LightTreeTypeConverter.kt L76](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt)、[CfirTree.typeRef 模型](../../cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/CfirTree.kt)（原审查坐标 L1811）、[CfirExportSourcePayload 类型角色](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourcePayload.kt)（原审查坐标 L28）。

两 Raw 返回内层 ref，不保存括号 Type 的 identity、外层 source、独立来源 flags。外层分派的 `.also(SOURCE)` 写在同一个内层对象上，不能恢复一个被删掉的外层节点。当前 payload 也没有 Paren 角色；Qualifier 只保存 name／arguments，没有各层 Type flags。

native [ParseType.cpp L227–243](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseType.cpp#L227) 创建独立 ParenType，拥有 inner Type；[Node.h L769](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/AST/Node.h#L769) 明确该结构。更关键的是 [ObtainsAliasType L2413–2438](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L2413)：先检查**当前节点**有效性、COMPILER_ADD、已有 aliasTy／资格，再在 PAREN_TYPE 分支递归。它不是无条件先去括号。相同语义 Cone 下，外层 flags 可以决定不进入 inner；丢失这些节点事实后无法执行原合同。

修复应在 Raw／生成模型与来源遍历中保留节点级原始类型形状，Snapshot 冻结外层／内层的独立 flags 和有序边。若选择折叠，必须先有对该外层节点的已发布、可校验等价证据；当前实现没有该证据。Expanded Cone 或一份 abbreviation 不能补回 Paren／Qualified 等原 Type 节点语法。这是基础输入缺失，**并非要求本轮实现或验收 Alias selector**。

## G4 [P2] 原始数值转换目标绕过两 Raw 的 SOURCE 发布

固定修复路径：[PsiRawCfirBuilder.kt L2778](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt)、[LightTreeRawCfirExpressionBuilder.kt L969](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirExpressionBuilder.kt)。

`Int64(x)` 等原始数值转换直接构造 `BasicTypeRef`，只设置 source/name，默认 SEMANTIC，未经过具体 Type 分派。native [ParseAtom.cpp L1639–1649](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseAtom.cpp#L1639) 真正创建 PrimitiveType 并放入 `TypeConvExpr.type`，因此这个槽有原始 Type 节点，创建 owner 应显式 SOURCE。

修复两条转换 owner 并补双 Raw 来源断言。不要把 [PrimitiveTypeExpr L2723](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/AST/Node.h#L2723)（如 `Int64.foo()` 的表达式占位）混同为该转换的 Type 节点，也不要全局改变 BasicTypeRef builder 默认值。

## G5 [P2] 显式／省略来源消费者仍用 source null／fake kind 推断

固定修复路径：[semantics.api/CopyUtils.kt L36](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt)、[CfirLambdaParameterData.kt L27](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirLambdaParameterData.kt)，及两 Raw lambda 的原始事实发布：[PSI L3328](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt)、[LightTree L1999](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirExpressionBuilder.kt)。

`hasImplicitOrInferredReturnType` 将具名函数的 `source == null` 当成推断证据；SOURCE ref 可以没有 source，SEMANTIC ref 可因诊断迁移获得非 null source。公共原型 helper 的 fallbackSource 就允许后一种情况。该判定被 [DeclarationsResolve L387](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirDeclarationsResolveTransformer.kt) 和 [ExpressionsResolve L3600](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt) 用于是否施加声明返回类型约束，会重新引入错误的 presence 判断。

lambda 的专用原始布尔事实只在省略时发布 true；显式参数不写 false，读取 null 时继续使用 implicit ref／fake source kind。补全之后，未标记的 SEMANTIC ref 若没有该特定 fake kind，会被当成非省略。修复要让原始节点是否存在通过 creation owner 的已发布事实消费，完整发布 raw true/false，并按明确的合成参数合同处理缺失事实；不得再靠 fake kind 或 source 非 null 恢复原始语法。COMPILER_ADDED 和 SEMANTIC 也不能合并成“没有 source”，前者仍是实际生成 Type 节点。

## G6 [P2] 注解解析辅助 TypeRef 在两 Raw 间分类不一致

固定修复路径：[PsiRawCfirBuilder.kt L1764](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt)、[PSI macro override L1784](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt)、[LightTreeRawCfirDeclarationBuilder.kt L1558](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt)。

普通 PSI 注解用 `convertTypeRef(annotation.typeReference)`，进入 SOURCE 分派；LightTree 名称 helper 和 PSI macro override 直接 buildUserTypeRef，默认为 SEMANTIC。三个入口都是为注解名称解析建立 ref，不能将此差异当成 parser 差异的合理结果。

native [Annotation L571–592](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/AST/Node.h#L571) 保存 identifier／custom baseExpr；[ParseCustomAnnotation L255–257](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseAnnotations.cpp#L255) 保存名称表达式，**不创建原始 Type 节点槽**。所以本项不是“把两个漏标 helper 全改 SOURCE”：解析名称而制造的辅助 TypeRef 应明确为 SEMANTIC，源名称继续由 annotation／expression 的原始角色保存；普通 PSI 路径也需执行同一分类合同。若刻意将 SOURCE 定义扩展到所有源码名字，必须另外发布其不是 native Type 节点的角色事实，否则仍不能满足本轮的 explicit-node-presence 合同。

## G7 [P2] 已出现但损坏的 TypeReference 在具体分派前被当成省略

固定修复路径：[PsiConversionUtils.kt L69–75](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt)、[LightTreeTypeConverter.kt L33–37](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt)。

两入口均把“引用为 null”和“引用存在、内部没有可识别 type element”返回为默认 SEMANTIC implicit，后一分支还丢弃原引用 source，绕过 SOURCE 发布。native [ParseBaseType L52–55](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseType.cpp#L52) 在期望类型但无法解析时创建 InvalidType 并标 broken；[InvalidType L702](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/AST/Node.h#L702) 仍是 Type 节点。

修复须在 Raw owner 区分真正省略与已出现的损坏类型引用，后者保留 source 和 SOURCE error／invalid 角色；没有语法标注的 implicit 继续 SEMANTIC。这是 IDE 不完整源码输入合同，不要求修改任何正确 CJ 或既有正确诊断期望。

## 测试与其余入口的审查边界

已读到的新增来源断言包括 [changingTypeReferenceOriginInvalidatesTheCapturedInput L107](../../cfir/providers/test/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshotTest.kt) 与 [withReplacedConeType 保留来源断言 L36](../../cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/CfirTypeRefExtensionsTest.kt)。前者确实断言旧 payload 保持 SEMANTIC、改当前 origin 后 validate 抛错；但只覆盖手工 resolved 根的换代。它不证明 Raw 创建、错误 source-copy、调用补全、CJO 分类、Paren／Qualifier 角色已经闭合。未运行这些断言。

必须明确的手工 fixture 来源：

- [CfirOwnedGraphCopyTest L52](../../cfir/cfir-tree/test/org/cangnova/cangjie/cfir/copy/CfirOwnedGraphCopyTest.kt) 的原始 `Bound` user ref 表示源码 bound，却没有发布 SOURCE；此测试当前只能证明默认 SEMANTIC 副本。为验证来源合同，应明确 SOURCE，并同时断言副本来源和节点身份／共享关系。
- [Snapshot Fixture.resolvedInt64 L1106](../../cfir/providers/test/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshotTest.kt) 给所有 ref `TestSource`，默认 SEMANTIC；调用点包含 `parameter-type`／`return-type`／`binding-type`。这些标签不能证明它们是显式 Type 节点。origin 换代测试故意以 SEMANTIC 起步，应保留；若其余用例需要显式标注，须由相应构造调用明确 SOURCE，而不是全局改此 helper 或按 TestSource 分类。
- [CfirExportSourcePayloadTest.resolvedType L569](../../cfir/providers/test/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourcePayloadTest.kt) 与 [CjoSourceContentSelectionTest.typeRef L364](../../cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjo/CjoSourceContentSelectionTest.kt) 只从 Cone 建 known-type，SEMANTIC 符合现有 helper 合同。不能因为声明 origin 是 Source 或 Library 就自动改它们的 TypeRef origin。

后续行为验证应覆盖两 Raw 的显式／省略／损坏分支与数值转换，三种 origin 的公共替换／source-copy／owned-copy，显式与推断调用类型实参补全，错误 diagnostic／partial prototype 保留，CJO 无 source 的 COMPILER_ADDED，以及有序重复子槽和 header／superTypeReferences 的换代校验。Paren 要验证外层／内层来源独立，不以 Cone 相等代替节点合同。该列表是修复后的验证要求，不是本轮已经执行或通过的测试清单。

恢复前缀入口 [SpecificTypeResolver L207/L227](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSpecificTypeResolverTransformer.kt) 两个新 ref 仍为默认 SEMANTIC。它们裁剪 qualifier 以进行恢复，不能直接冒充完整原始根；应由恢复 owner 明确其为语义尝试，或发布对应真实前缀节点的来源与 source 范围。主 error 的原始 delegation 已保留，本轮不把恢复副产物默认 SEMANTIC 单独算作 G1 的成功主链丢失。

[StubBasedCfirTypeDeserializer.typeRef L123](../../analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/stubBased/deserialization/StubBasedCfirTypeDeserializer.kt) 也只从 PSI/cone 构造 resolved，带 real source、默认 SEMANTIC、没有原 prototype。它是另一种反序列化入口：若其合同是 known-type 视图，该默认可以成立；若宣称恢复 native imported Type 节点，则需明确 COMPILER_ADDED 与节点形状。不能仅因 real PSI 改为 SOURCE，亦不能用它证明两个 CJO 包裹入口以外的全反序列化来源覆盖。

native [ReferenceLoader.WrapType L111–152](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L111) 显式创建 primitive／function／tuple／reference Type，并赋 imported／COMPILER_ADD；[L481–489](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L481) 同时明确某些 constructor 不加载 retType 节点。由此，COMPILER_ADDED 必须由真实创建 owner 发布，不能从 synthetic 声明、fakeSource、null source 或存在 Cone 的事实推导。当前已核实的两个 CJO 入口满足显式分类要求；其他 owner 应逐槽遵守这一边界。

## 关键读取版本

以下 SHA-256 于 `2026-11-19T01:38:57+08:00` 读取，用于识别未提交审查版本；不以 HEAD 代表这些文件的内容。G1/G2/G3/G5 结论针对这些版本。

| RepoRelative 文件 | SHA-256 |
| --- | --- |
| [cfir-tree/utils.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt) | `4CF3749FFC0637F71B57B1EBD960865B9122BB7D2D3D48B59BCF57D00CA7C779` |
| [semantics.api/CopyUtils.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt) | `A55D64EF6C3099444430679891EBAAD9649841E6F29E030057AF2BD99C1CA048` |
| [CallCompletionResultsWriter](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt) | `2F0DEF7988E916A3DC4240D4DB9C99F08B3FF57072F7BBD9209E9A5B24637F75` |
| [CallCompleter](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt) | `32FAFA38844B35FC494ADEB0F6E609990DDDD0936DBF314F998925060A30BE6D` |
| [SourcePayload](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourcePayload.kt) | `1B040EBF762601A194DAD094374425BE42EDEB087BE97E556B771D0C73453D9A` |
| [PsiConversionUtils](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt) | `943E7A577D8D7FA4589C70EF1DED77EE62F636CD96F5C40CB28A9A68120F86C3` |
| [LightTreeTypeConverter](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt) | `D013523E8ECD0CFE433FD9AD585192A92B6A2F3843FA793B88E4D9CDFFB6DEED` |
