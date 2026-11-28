# TypeReference 来源基础 G1–G7 复审（2026-11-19）

**KotlinParityGatekeeper 裁定：G1、G4、G5、G6 的原生产缺陷可在源码层关闭；G2 仍 REJECT；G3 的正常 Paren／Qualifier 来源链已修复，但缺损子链随 G7 保持 OPEN；G7 的顶层缺损入口已修复，必需的括号内层仍未闭合。不能签发 G1–G7 全部通过。**

本轮仅复审 [原来源基础 gate](type-reference-origin-gate-20261119.md) 的 G1–G7。先读取该报告与 [冻结 encoder 接续](../cjmp-source-frozen-encoder-continuation-20261118.md)，再读取最新磁盘生产／相关测试源码；接续中的 29／184 窗口不作为本轮候选的行为证据。唯一实现树为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，HEAD 为 `b4dd742d589e4b5ee6592d454634c44f634df08a`；结论针对未提交磁盘内容，不以 HEAD 替代其版本。仅用 apply_patch 新增本文件，没有修改生产／测试／既有文档，没有运行 Gradle、cjc，没有联系旧停止会话。

已读取指定 `kotlin-cangjie-frontend-parity` 技能的实际源文件 `D:/code/intellij/cangjie/.agents/skills/kotlin-cangjie-frontend-parity/SKILL.md`。本轮承担指定 Gatekeeper 角色；没有可用的多代理委派工具，由本角色直接读取两侧参考。用户明确限定复审和唯一文档写入，本轮不执行技能中的生产修订与 Gradle 步骤。

## 固定参考与模块／owner 对位

Kotlin 与官方仓颉均通过主树 external 的 Git 对象 **git show 固定提交**读取，external 只读；已用 rev-parse 核实提交对象。下列坐标来自本轮实际读取，未以当前上游工作区或远端默认分支替换：

| 角色 | Kotlin／官方固定坐标 | 本地 owner 与核验结论 |
| --- | --- | --- |
| TypeRef 生成模型 | [Kotlin FirTree L1590](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/tree-generator/src/org/jetbrains/kotlin/fir/tree/generator/FirTree.kt#L1590) | [CfirTree L1803](../../cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/CfirTree.kt)，同为 tree／tree-generator 的类型节点模型；origin 在 L1811，Qualifier origin 在 L1842，Paren 在 L1849；[BuilderConfigurator L202](../../cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/BuilderConfigurator.kt) 仍统一默认 SEMANTIC。 |
| 公共原型 helper | [Kotlin semantics.api／CopyUtils L42](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/semantics.api/src/org/jetbrains/kotlin/fir/CopyUtils.kt#L42) | [CopyUtils L19](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt)，模块 semantics.api、包 org.cangnova.cangjie.cfir、顶层扩展 owner 没有移动到 inference。 |
| 公共类型替换 | [Kotlin semantics.api／types／TypeUtils L478、L494](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/semantics.api/src/org/jetbrains/kotlin/fir/types/TypeUtils.kt#L478) | [TypeUtils L48、L63](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt)，同模块、types 包、三个 resolved/error 分支和 partial-ref source-copy 流程；origin 由原型复制。 |
| SpecificTypeResolver | [Kotlin resolve／transformers L141、L158](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirSpecificTypeResolverTransformer.kt#L141) | [CfirSpecificTypeResolverTransformer L131、L151](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSpecificTypeResolverTransformer.kt)，成功和错误输出均复制 origin，并以当前 raw ref 为 delegation。仅核验本轮来源合同。 |
| source-copy | [Kotlin tree／Utils L57](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/src/org/jetbrains/kotlin/fir/Utils.kt#L57) | [cfir-tree／utils L212](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt) 位于对应 tree 公共层，但 concrete 分派和无 source wrapper 的拒绝合同仍有 G2 余额。 |
| 原始类型构造 | [Kotlin PsiRawFirBuilder.toFirType L2765](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/raw-fir/psi2fir/src/org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder.kt#L2765)；[官方 ParseType L52](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseType.cpp#L52) | PSI／LT 仍由 raw-cfir 层发布 SOURCE；缺损与省略须由语法 owner 区分。括号与三值 origin 的语义依据是官方 Type 节点，不是 Kotlin 字段照搬。 |

固定 Kotlin `3c7a5041f238102285764d370665f56f587f9dc4` 没有本地三值 `typeUseOrigin`，也没有此 Paren 产品；官方 `14276740ffec1262b81b6b5236b1d840af270b37` 的 [ParenType／QualifiedType L769、L781](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/AST/Node.h#L769) 和 [WrapType L111](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ReferenceLoader.cpp#L111) 支持保留真实原始／编译器生成 Type 节点的适配。不能称这些新增字段／节点为 Kotlin 的原样字段复制；也不能因 Kotlin 无对应语言节点而删除仓颉原始形状。

## G1：原缺陷静态关闭；公共 source-copy 的 G2 余额不豁免

已修复：

- [Writer.computeTypeArguments L1542–1549](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt) 对已有显式错误实参直接保留原 ref；其余已有 ref 调用 `withReplacedConeType(type)`；没有源实参才调用 `type.toCfirResolvedTypeRef()`。原先两份手工重建 builder 已移除。
- [Writer L27](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt) 与 [Completer L3](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt) 导入公共 `resolvedTypeFromPrototype`。实际调用点为 Writer L1444、L1465、L1674；Completer L1071、L1188、L1218。在 cfir 范围搜索该函数声明，只剩 semantics.api／CopyUtils L19，没有两份私有同名实现。
- [CopyUtils L23–26](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt) 已 resolved 原型走公共替换，保留既有 delegation；其他原型整体传给 [toCfirResolvedTypeRef L304](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt)，由原型复制来源，无原型默认 SEMANTIC。
- [TypeUtils L74–104](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt) 三个分支均复制 origin、annotations、delegation，错误路径保留 diagnostic 与 partial ref。框架 owner／调用流对应 [Kotlin Writer L1035](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirCallCompletionResultsWriterTransformer.kt#L1035) 和公共 CopyUtils／TypeUtils；本地没有另造完成阶段替换 owner。

剩余合同：显式三种 origin、错误 diagnostic／partial prototype／原 delegation 和纯推断 SEMANTIC 的完成阶段行为尚未验证；公共替换在 L68 调用 source-copy 时仍受 G2 影响。这里只关闭 G1 的来源丢失与私有 owner 绕行，不据此接受所有复制入口。

## G2：部分修复，仍 REJECT [P1]

已修复：[utils.copyWithNewSource L216](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt) 先匹配 Error，再匹配 resolved，原 error 分支不再被抽象 resolved 吞掉。[专用 error copy L98–105](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/builder/CfirErrorTypeRefBuilder.kt) 携带 diagnostic、partiallyResolvedTypeRef、delegation、origin，L218 继续迁移 partial source。L225 也新增 ResolvedImplicit 分支，避免退化成普通 implicit。

仍需修复的具体坐标与合同：

1. **L225–227 对非法 source 迁移静默成功。** 当前 `ResolvedImplicitTypeRef.copyWithNewSource(nonNullSource)` 返回原节点，source 仍是 null。该 API 的输入是非 null `CjSourceElement`，而 [ResolvedImplicitTypeRef.replaceSource L45–46](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/impl/ResolvedImplicitTypeRef.kt) 明确 `require(newSource == null)`。返回原节点只解决“不要丢 known ref”，绕过了原 gate 要求的“禁止非 null source 迁移”。必须执行无 source wrapper 的拒绝合同；不能以 no-op 或普通 implicit 副本接纳非法输入。
2. **L221 仍匹配抽象 CfirResolvedTypeRef。** [固定 Kotlin L62–67](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/src/org/jetbrains/kotlin/fir/Utils.kt#L62) 使用具体 `FirResolvedTypeRefImpl` 与专用 Error 分派。把 Error 放前面使当前已知 error 不再丢字段，但没有恢复 concrete／手写节点边界。需恢复对应 concrete 分派，不能把“所有 resolved 子类均可重建为普通 impl”作为公共合同。本轮未据此声称存在另一种已触发退化的本地 resolved 子类。

[owned-copy L1424](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/copy/CfirDeepCopyOwnedGraphVisitor.kt) 已保留专用 wrapper 与内部 ref，可作为结构参考；它不授权 source-copy 对非法 source 返回成功。未执行 source-copy 的 error／wrapper 行为用例。

## G3：正常 Paren／Qualifier 来源链修复确认；缺损链与 G7 仍 OPEN

已修复的产品链：

| 层 | 当前坐标与实证 |
| --- | --- |
| 模型／生成 builder | [CfirTree L1849](../../cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/CfirTree.kt) 有独立 ParenthesizedTypeRef 和必需 innerTypeRef；[生成 builder L26、L59](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/types/builder/CfirParenthesizedTypeRefBuilder.kt) 分别默认 SEMANTIC、copy 原 origin／inner；[Qualifier builder L24、L55](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/builder/CfirQualifierPartBuilder.kt) 同样传递和复制独立 origin。 |
| PSI／LT 创建 | [PsiConversionUtils L120–123](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt) 构造外层 SOURCE Paren，并用内部真实 TypeReference 转换内层；[CjParenthesizedType L58、L68](../../psi/src/org/cangnova/cangjie/psi/CjParenthesizedType.kt) 返回内部 element／reference。 [LightTreeTypeConverter L80–86](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt) 同样创建外层对象和内层边，正常引用不再直接返回同一个内层对象。 |
| Qualifier 创建 | [PSI L194–198](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt)、[LT L206–210](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt) 在正常 user-type 每段发布 SOURCE，保留各段 source、name、有序 arguments；annotation 名称段另按 G6 分类。 |
| 类型解析 | [CfirTypeResolver L153](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirTypeResolver.kt) 对 Paren 内层解析 semantic type；SpecificTypeResolver L96 变换内层，L133／L153 复制当前节点 origin，L136／L158 将当前 Paren 原型保留为 delegation。因此求 Cone 可穿括号，来源节点没有在输出原型图中被删除。 |
| owned-copy／source-copy | [owned visitor L1385、L1394](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/copy/CfirDeepCopyOwnedGraphVisitor.kt) 使用各自 generated copy，并复制 qualifier arguments／Paren inner；[source-copy L230、L240](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt) 保留 Paren 和各 qualifier origin。 |
| 来源遍历／冻结 | [Paren.acceptSourceChildren L27](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/types/CfirParenthesizedTypeRef.kt) 访问 inner；[UserType L28](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/types/CfirUserTypeRef.kt) 访问各 qualifier；[Qualifier L26](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/CfirQualifierPart.kt) 访问各 argument；[Resolved L28](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/types/CfirResolvedTypeRef.kt) 访问 delegation。 [payload L30、L38](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourcePayload.kt) 已分别冻结 Paren origin／inner 和 Qualifier origin／arguments，L238／L246 实际捕获。 |
| 换代校验 | [Snapshot L128、L137](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt) 保留 header／super 槽身份与次序；L351 通过来源 visitor 收集节点，L695–699 保存各节点 payload／source span；L298／L303 重新比较子身份与各节点 payload。因此已经遍历到的外层、内层和 qualifier origin 是独立可校验数据。 |

上述产品修复了原 G3 的“外层被删除、Qualifier 没有来源字段”。官方 [ParseParenType L236–242](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseType.cpp#L236) 和 [ObtainsAliasType L2413–2438](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L2413) 要求先有独立当前节点，再按其条件决定是否进入 inner；保留节点是必要基础。

剩余合同：G7 所述缺损 Paren 子链仍不能纳入正常路径结论；需验证外层／内层 origin 互不覆盖、Qualifier 各段 origin 的独立换代、重复有序子槽与 fresh owned 身份。三值 origin 只发布来源分类，不等于已经冻结 native 的有效性、aliasTy、目标资格与全部属性；这些 selector／投影合同属于未验收的 S4，本轮不因未实现 selector 额外产生拒绝，也不签发它们已完整的声明。

## G4：原缺陷静态关闭

[PSI TypeConversion.targetTypeRef L2784–2787](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt) 和 [LT L969–972](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirExpressionBuilder.kt) 均明确发布 SOURCE，保留 primitive callee source 与类型名。对应官方 [ParseTypeConvExpr L1643–1649](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseAtom.cpp#L1643) 的真实 PrimitiveType 槽。

剩余合同：两 Raw 的真实 `Int64(x)` 等转换来源、完成后原型保留尚未行为验证。BasicTypeRef 公共默认仍是 SEMANTIC；没有将 primitive 表达式占位或所有 known primitive 包裹改成 SOURCE。

## G5：原缺陷静态关闭

[CopyUtils.hasImplicitOrInferredReturnType L36–38](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt) 已通过 implicit 节点／具名函数的 SEMANTIC origin 判断，不再用 source-null 分类。SOURCE 无 source 与 SEMANTIC 带诊断 source 不会由该 source presence 条件误判；COMPILER_ADDED 也不被当成 SEMANTIC。

[PSI lambda L3335](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt) 与 [LT lambda L2000](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirExpressionBuilder.kt) 对每个参数直接写入 `typeReference == null`，显式 false 和省略 true 都被发布。[LambdaParameterData L27–30](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirLambdaParameterData.kt) 优先消费已发布布尔事实；对没有 raw 标记的参数使用 implicit／SEMANTIC 的创建合同，没有 fake-kind／source-null 条件。该缺失事实处理是已声明的 known／合成参数合同，不能继续解释为“按 source 兜底”。

另核对了参数 fragment owner：[PSI L370](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt) 与 [LT L361](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt) 明确只允许 Declaration signature，直接根入口也在 PSI L316／LT L318 进行该限定；未把这些入口推测为漏标 raw lambda。

官方 docs MCP 的 `manual_source_zh_cn_lambda`“Lambda 表达式定义”明确参数标注可省略、返回类型不能声明且从上下文推断；这是补充语义证据，不替代固定 native 源码或测试运行。

剩余合同：须实际覆盖显式／省略／SOURCE 缺损 lambda 参数在补全后的事实稳定性，以及无 raw 标记的三种 origin；具名函数还须覆盖 SOURCE 无 source 与 SEMANTIC 带 source 的返回约束行为。两 Raw 既有 [PSI test L70–87](../../cfir/raw-cfir/psi2cfir/test/org/cangnova/cangjie/cfir/builder/RawCfirStatuslessDeclarationsTest.kt)、[LT test L76–93](../../cfir/raw-cfir/light-tree2cfir/test/org/cangnova/cangjie/cfir/lightTree/RawCfirStatuslessDeclarationsTest.kt) 只读到省略参数 true 的断言，不作为 false／origin 或本轮运行通过证据。

## G6：原缺陷静态关闭

[普通 PSI annotation L1765–1769](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt) 对普通转换结果或 override 统一发布辅助 TypeRef 为 SEMANTIC，并将 user-type 名称各段同样分类。macro override [L1793–1799](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt) 构造的默认 SEMANTIC ref 也进入这一出口；custom fragment 的 [L396–397](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt) 同样把 helper 结果作为该出口的 override。

[LT annotation helper L1563–1569](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt) 与 [实际发布 L1388、L1395](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt) 保持 SEMANTIC ref／qualifiers。因此原三入口对名称辅助 ref 的分类差异已消除，没有把另两条 helper 改为 SOURCE。

依据官方 [Annotation L571–592](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/AST/Node.h#L571) 和 [ParseCustomAnnotation L249–257](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseAnnotations.cpp#L249)，该角色来自 identifier／baseExpr，名称解析辅助 ref 不冒充原生 Type 槽；source 名称、annotation identity 和 callee source 仍由 annotation／reference owner 保存。

剩余合同：普通、限定名、macro override、custom fragment 的双 Raw 最终辅助 ref／qualifier origin 尚未运行验证。此结论只覆盖名称辅助 ref，不宣称 annotation 参数表达式保存、decoder 或 macro 全流程完成。

## G7：顶层分支修复；括号内层仍 REJECT [P2]

已修复：[PSI type-ref 入口 L74–79](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt) 与 [LT L33–39](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt) 已区分真正 null 引用（SEMANTIC implicit）与非 null 引用但缺 type element（保留引用 source 的 SOURCE error）。原 G7 在顶层的“存在却变省略”已消除。

仍未闭合的原始缺损子链：

- **LT L82–86 仍把括号必需内层的缺失当成可省略类型。** 找不到内部 TYPE_REFERENCE 时，把 null 传给同文件 L33，得到无 source、SEMANTIC implicit，并由 SOURCE ParenthesizedTypeRef 持有。该位置没有合法的“省略内层”合同。可由 parser 源码确认存在这一输入： [parseTupleType L7021–7035](../../psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt) 对 `()` 的计数为 0、报解析错误、不创建内部 TYPE_REFERENCE；[parseTupleOrFunctionType L7417–7418](../../psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt) 仍将该容器结束为 PARENTHESIZED_TYPE。上述结论是源码路径核对，未执行该输入。
- **PSI L110–118 的缺损输出与新增保留外层合同未统一。** `getType()` 为 null 时直接返回外层 SOURCE error；若内部 TypeReference 已存在但无 element，这个早返回在进入已修复的 type-ref 缺损入口前就删除了 Paren／inner 的独立边。LT 对“内部引用存在但无 element”则保留 Paren 并持有 SOURCE error inner。两个 raw owner 对同一必需子槽没有共同的错误／原型保留合同。

固定官方 [ParseBaseType L52–55](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseType.cpp#L52) 创建带 broken 的 InvalidType；[InvalidType L702](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/AST/Node.h#L702) 仍是 Type 节点。[ParseTypeWithParen L206–224](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseType.cpp#L206) 对零项且无 arrow 走 broken-function 处理，而非“合法 Paren＋省略 inner”。[ParseParenType L237](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseType.cpp#L237) 则在真正 Paren 情况保留实际 inner Type。两者都不能支持 LT 的 SEMANTIC implicit 内层。

剩余修复合同：由 Raw owner 区分合法省略与必需子槽损坏；实际内层引用存在但缺 element，应保留该引用的 SOURCE error／invalid 与已存在外层的原型边；完全没有必需内层时，发布具有外层 source 和明确错误角色的 SOURCE 缺损结果，并对齐官方实际容器角色，不能调用可省略入口制造 implicit。PSI／LT 需执行同一合同。有效 CJ 与正确既有诊断期望不应因本项而被改写。

## 本轮验收边界与待验证合同

本轮没有任何行为 ACCEPT。已读 [TypeRefExtensionsTest L35–39](../../cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/CfirTypeRefExtensionsTest.kt) 的 SOURCE 公共替换断言与 [SnapshotTest L107–115](../../cfir/providers/test/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshotTest.kt) 的根 origin 换代断言；它们不能证明新 Paren／Qualifier、缺损分支、实际调用完成或全部三值来源已经运行通过。

后续限于本组合同的验证应涵盖：两 Raw 的正常／缺损／省略类型与数值转换；Paren 外层和内层独立来源及源范围；有序重复 qualifier arguments 和各段换代；SOURCE／SEMANTIC／COMPILER_ADDED 的公共原型替换、source-copy、owned-copy；Error diagnostic／partial 原型保留；ResolvedImplicit 的非法 source 拒绝；显式／推断调用实参；annotation 三类入口；lambda true／false／合成参数消费者。这里列出的是剩余合同，不是本轮执行清单。

SpecificTypeResolver 的恢复前缀 [L207、L227](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSpecificTypeResolverTransformer.kt) 仍使用默认 SEMANTIC root，复用原 qualifier 对象；它是语义恢复尝试，完整原始根保存在主 error delegation 中。本轮维持原 gate 的边界，不把恢复副产物冒充完整 SOURCE 根，也不将其单独计作 G1 主输出回归。

完整 S4／Alias selector、TypeUseGraph 的有效性与资格／冻结投影、完整 encoder／decoder、S5／S6、70 项／P0–P10 方案状态均不在本轮验收范围，全部不因这次来源基础复审而宣告通过。

## 未提交磁盘版本

关键文件在 `2026-11-19T03:13:43.1977728+08:00` 与 `2026-11-19T03:16:19.0719084+08:00` 两次 SHA-256 读取中一致。文档写入后复核发现 Snapshot 发生外部更新，已于 `2026-11-19T03:21:27.6557966+08:00` 重读其最新来源字段、遍历、捕获和校验路径，来源合同结论不变，相关后段坐标后移一行；下表 Snapshot 行记录重读后的 SHA，其余 22 行记录上述一致版本。它们标识本轮实际核验的候选；不替代执行测试，也不声明其他文件或未来磁盘内容同代。

| RepoRelative 文件 | SHA-256 |
| --- | --- |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt) | `04F7A0FE6E24EF2F78A211F559739C42966C1D9BF982835B5F1D8F2A8107E435` |
| [cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/CopyUtils.kt) | `9193FD33ADB45EDA3BFB2A01BB7EECED50C85C17D27249845268938197874CC4` |
| [cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt) | `2584ACFE54DA068056F4086FB6E80D2BDC2CB196038444DF88CF15194CA3461B` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompletionResultsWriterTransformer.kt) | `60DE2028FFC7D71E39A658865C61B67FB6F083585E710CD51576CF8D81F92EB5` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt) | `F56AA75703CDA7A1863DAF80CE282290C2214ECB8AF78944CCDEC02A79BBA58F` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSpecificTypeResolverTransformer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSpecificTypeResolverTransformer.kt) | `13D9ACE4F68BAE4AA5913D8FF8670D2C69F8DC53C62B49122C2CDEA543614D3D` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirTypeResolver.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirTypeResolver.kt) | `3E235120FAC2A86D9B317F22F05DBAE2E16B3F0D7F736CCA41273FE7643D1991` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourcePayload.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourcePayload.kt) | `CCF3957A56EC7D973D1FE4E63E2A6ABF6DBF92541F11616341EB05109C992AFF` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt) | `CD57A357CD80659991940D64B4925DA53A891F4A5C58A83C2F012D6EA4E7D674` |
| [cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt) | `9DBFAE990C8F58B85575BFF8FEC071BD6D27E3A2C14BF7DAE2FCF2A7C1210EB8` |
| [cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt) | `2B534C9CC642F14FE86495C0C4ABE6BB57C1C4C115ED0C21FBAFD96EA4F6D2C9` |
| [cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt) | `929EFAB6E80DDD5F7739921BAF829BF1E82AD8613699EE483E443E9C09612468` |
| [cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirExpressionBuilder.kt](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirExpressionBuilder.kt) | `029F320A80CAFD066A2BA74F5131D65FD956E5C78C1086F581059146FA80A0FF` |
| [cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt) | `4053F11D17AC9D8A3B7D81DAD35CA9329F1EE4AB5611D8A95171618EDA0394E8` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirLambdaParameterData.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirLambdaParameterData.kt) | `44962E10C6F48A5DD481FD8E89592ED1AB969A4008819029F6A97FF966D46845` |
| [cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/CfirTree.kt](../../cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/CfirTree.kt) | `0A37E9F58DD20B982917ED3D08EEC5CEC008CE9861FD41D830A4B57E728AE97C` |
| [cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/BuilderConfigurator.kt](../../cfir/cfir-tree/tree-generator/src/org/cangnova/cangjie/cfir/tree/generator/BuilderConfigurator.kt) | `03DB3B3184DFE8B2B7A9F4098107DA30057431E6663DDB9346559F2A9C650E5E` |
| [cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/types/builder/CfirParenthesizedTypeRefBuilder.kt](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/types/builder/CfirParenthesizedTypeRefBuilder.kt) | `BBDE70A7E2D757AAF2D5BB6197E95445090CA4FEAE912C83907FF360AF4D547A` |
| [cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/builder/CfirQualifierPartBuilder.kt](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/builder/CfirQualifierPartBuilder.kt) | `8E150E79BE19A8AF1EA3CC1194FF5B2030B2716601CBF37FC1D4443B86963FD0` |
| [cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/copy/CfirDeepCopyOwnedGraphVisitor.kt](../../cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/copy/CfirDeepCopyOwnedGraphVisitor.kt) | `A3B9F36C12AB45208CCD41A43A9514BE0CEE548721832F4D8DB26D48683C7E41` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/impl/ResolvedImplicitTypeRef.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/impl/ResolvedImplicitTypeRef.kt) | `A6BD86FCCC27C2644D103C809BDD6426544C8F9F38E18A3B7EF61564C7D2C2B7` |
| [psi/src/org/cangnova/cangjie/psi/CjParenthesizedType.kt](../../psi/src/org/cangnova/cangjie/psi/CjParenthesizedType.kt) | `BBFD3CFB5A87C214213E46DFDB77D5BB2433C0D103EED6AA16FD0FF85937BD57` |
| [psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt](../../psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt) | `F5701DCCE1B5263848F905EF291F228272DC67F5BF266C1CE692E5FEF48F4FF1` |

