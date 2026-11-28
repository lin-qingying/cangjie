# TypeUseGraph 生产定向 gate（2026-11-19）

**KotlinParityGatekeeper：REJECT。G2／G7 本次指定修订可在源码层关闭；新增 TypeUseGraph 的字段、冻结及部分接线已建立，但 T1–T6 的节点停止／存在性／选择后形状／域／入口合同仍未闭合。不给完整 S4 或方案通过。**

唯一实现树：`C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`；HEAD `b4dd742d589e4b5ee6592d454634c44f634df08a`。对象是最新未提交磁盘源码。延续已读的 `D:/code/intellij/cangjie/.agents/skills/kotlin-cangjie-frontend-parity/SKILL.md`，直接承担 Gatekeeper 角色。本轮只新增本文件，不改生产／测试／既有报告，不运行 Gradle／cjc，不联系停止旧会话；主树 external 只读。

## 固定源码证据与审查范围

固定 Kotlin `3c7a5041f238102285764d370665f56f587f9dc4` 与官方 `14276740ffec1262b81b6b5236b1d840af270b37` 均通过主树 external 的 git show 读取。本角色重新读了依据，没有把主线程已读或远端页面当成本轮源码阅读的替代：

- [官方 TypeManager L2375–2487](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L2375)：Ref／CFunc／This，函数声明签名，当前节点有效性和 COMPILER_ADD，Alias isExported／IMPLICIT_USED，Paren／VArray／Func／Tuple／Var／Param／Extend／默认分支。
- [官方 ASTWriter L1268](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L1268)、[L1450](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L1450)、[L1602](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L1602)：FuncBody.retType、Alias RHS 与 Decl.type 分别选择；不能用一个“声明 resolved type”替代全部用途。
- [Kotlin FirElementSerializer L771–803](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-serialization/src/org/jetbrains/kotlin/fir/serialization/FirElementSerializer.kt#L771)：Alias child serializer／own TP／underlying 与 expanded 分离；[L970–1019](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-serialization/src/org/jetbrains/kotlin/fir/serialization/FirElementSerializer.kt#L970)：类型表与 abbreviation；[L1197–1208](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-serialization/src/org/jetbrains/kotlin/fir/serialization/FirElementSerializer.kt#L1197)：实际 classifier 和有序 TP 实参。

本地 `CfirExportTypeUseGraph` 位于 providers／org.cangnova.cangjie.cfir.resolve.providers，完成选择后交 serialization／cjo 消费。它是 native CJO 节点选择的语言／格式适配，不能称 Kotlin 中有同名同形 TypeUseGraph；本轮只核验上述确切合同，不签发全模块／整个 serializer 的 Kotlin 框架 parity。

## G2／G7 指定修订：源码层关闭

| 原 finding | 本次最新坐标 | 修订核验与剩余验证 |
| --- | --- | --- |
| G2 concrete resolved／手写 wrapper | [utils L217–228](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt) | Error 先走专用 copy；普通 resolved 改为具体 CfirResolvedTypeRefImpl；ResolvedImplicit 对非 null source 明确抛 IllegalArgumentException，不再返回原节点接纳非法迁移。对应 [Kotlin Utils L62–67](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/tree/src/org/jetbrains/kotlin/fir/Utils.kt#L62) 的 concrete／手写分派边界。需后续执行 diagnostic／partial／origin 保留与拒绝断言，本轮没有运行。 |
| G7 引用存在但无 element／完全缺内层 | [PSI L110–120](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt)、[LT L82–90](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt) | PSI 先取内部 TypeReference；引用存在但缺 element 仍建立 Paren，内层走 L76–79 SOURCE error。完全没有必需引用时，两 Raw 都发布带外层 source 的 SOURCE error。LT 已不把 null 传给可省略入口。本轮原 G7 两条拒绝点消除，缺损输入实际行为和源范围仍待验证。 |

以上仅关闭 [上一轮报告](type-reference-origin-recheck-20261119.md) 的指定余额，不追认旧候选或未运行测试。

## 新图已核实的部分合同

| 检查项 | 已核实坐标与有限结论 |
| --- | --- |
| 当前 TypeRef 来源先裁决 | [Graph.selectReference L104–113](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) 读取当前冻结 origin，错误走 Invalid，非 SOURCE 走 Semantic；随后才进 source 选择。这条 TypeRef 主入口没有按 source-null／delegation presence 重分类。声明入口和 CFunc 子入口仍见 T1／T4。 |
| Alias 资格 | [Graph L125–129](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) 使用 analysis.isExported 或本代 precedingFacts.isImplicitlyUsed；没有以 EXTERNAL linkage 接纳 Alias。符合资格才保存完整 Alias TypeElement，不递归它的 syntax arguments。这里只确认资格谓词和分支，不宣称全部 aliasTy 来源事实或使用角色已验收。 |
| Paren／Ref／Qualified／Option | [Graph L131–163](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) 正常 Paren 进入 inner，单段 Ref 进入 arguments，This 停止；多限定段按 Qualified 的 native 默认分支保存当前语义 Ty；Option／Basic 停止。与 TypeManager 的 switch 范围一致；不把 Qualified／Option 误做无条件递归。CFunc 单独见 T4。 |
| declaration／self／RHS／signature／ret／super 分表 | [Graph L67–79、L167–181](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) 有独立 maps；Alias self 走本身 type，RHS 走 header ref，函数签名按参数声明和返回用途建 Function，FuncBody return 单独查实际节点，super 列表逐槽选择。分表已建立；停止／存在性仍见 T1／T2。 |
| 图冻结与身份 | [Graph L55–61、L187–189](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) key 使用 IdentityHashMap，循环检测按 ref 身份，maps 与多子槽复制为不可修改容器；Semantic memo 以捕获 TypeElement 身份驻留。没有按 Cone.equals 合并两个来源 ref 的语法选择。 |
| 发布时机 | [Analysis L242–244](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysis.kt) 在 linkage 工作队列和 owner 收敛后 validate 输入，再 freeze TypeUseGraph；[FrozenAnalysis L737](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysis.kt) 持有这份图，L797 返回冻结实例。encoder 没有现场重跑选择。 |
| 已接线的格式槽 | [Producer L2002–2005](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) Decl.type；L2018 普通函数 FuncBody.retType；L2069 Alias RHS；L2071／2081／2085／2091／2173 super；L3080–3100 把 TypeUse 投影为 shape，再进 L3229 共用 reserve／fill t 池。不能推及 canonical lambda／约束／catch，见 T2／T6。 |

## T1 [P1] REJECT：声明当前 COMPILER_ADD 停止条件没有进入选择

具体坐标：[Graph.selectDeclaration L170–179](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt)。

该入口只有 `!input.isSourceDeclaration(declaration)` 的停止条件；源码图内的函数、参数、变量、Extend 会继续进入 header／参数选择，没有先消费当前声明的 `analysis.precedingFactsOf(declaration).isCompilerAdded`。

这是两项不同事实：[Snapshot.isSourceDeclaration L180–181](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt) 检查 origin.fromSource／cjoOrigin／所属文件；[Analysis.publishSourceFacts L274](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysis.kt) 则独立发布 compiler-added。 [DeclarationOrigin L54–58](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirDeclarationOrigin.kt) 的 DefaultParameterHelper／CanonicalSource 明确同时 generated 和 fromSource。已有事实字段 [ExportFacts L25](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirDeclarationExportFacts.kt) 不能被 SOURCE 准入条件代替。

官方 TypeManager L2417–2418 对 **当前 Node** 先返回 node.ty，再考虑 FuncDecl／Var／Param／Extend。当前入口在这些声明属于 SOURCE 图且 COMPILER_ADD 时仍可能按原型显式 Alias 改写 signature／header；即使个别 helper 的孩子恰好都是 SEMANTIC，也不能证明停止合同成立。本轮不声称已经运行某个 helper 反例。

**下一修复：** 在声明级选择应用同代已发布的当前节点 COMPILER_ADD 停止协议，先保存完成语义 Ty，再考虑实际语法分支；不能通过排除 SOURCE helper 或修改其子 ref 来源回避。补 generated＋fromSource、显式 Alias 子槽的声明级停止验证。

## T2 [P1] REJECT：完成阶段真实生成的 FuncBody.retType 没有来源产品，canonical lambda 又绕过该图

具体坐标：[Graph L73–74、L97–99、L175–176](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt)；[Producer L2018](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)；[SourceGraphProducer L183–189](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt)；[CanonicalEncoder L59–63](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoCanonicalSourceEncoder.kt)。

`actualTypeNode` 将 SEMANTIC header 一律视为不存在 FuncBody.retType，普通函数编码便得到 0；lambda canonical 产品则只保存 Cone returnType，encoder 无条件写一个返回 type，完全不经过 functionReturns。

固定官方源码明确区分“raw 省略”与“类型检查之后已经创建节点”：

- [TypeChecker.CheckFuncBody L203–208](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp#L203) 在缺 retType 时执行 AddRetTypeNode；[L228–238](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp#L228) 创建 RefType，发布 COMPILER_ADD。
- [LambdaExpr.SynLamExpr L260–264、L307–310](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckExpr/LambdaExpr.cpp#L260) 创建后填入最终 retTy；[ChkLamExpr L343–349](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeCheckExpr/LambdaExpr.cpp#L343) 同样明确创建 COMPILER_ADD RefType。
- ASTWriter L1269 看的是完成后的真实 retType 指针；TypeManager L2402–2406 另外决定签名返回使用该节点选择还是既有 funcTy.retTy。

本地原型 helper 保留 SEMANTIC 是正确的 known-type 默认，但不能代替上述 **真实创建 owner** 的 COMPILER_ADDED 发布。当前 [Completer L1071–1077](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt) 的 lambda 返回补全只是公共原型替换，没有发布该生成节点；canonical lambda 的非零返回槽不能反向证明 origin 产品已完整。

**下一修复：** 在对应完成／语义 owner 发布真正生成的返回 Type 节点事实与来源，冻结独立 FuncBody.retType 用途；普通函数与 canonical lambda 均消费这个用途。只有官方路径实际没有 retType 的情况才保留 null／0，不从“raw 没标注”推断完成阶段不存在，也不全局把所有 known-type ref 改为 COMPILER_ADDED。构造器、库入口和未检查正文需按各自真实合同处理，不能统一补造。

## T3 [P1] REJECT：选择后 RawArray shape 只覆盖 arguments，没有执行原生重新规范化

具体坐标：[Graph L154](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt)；[Producer.frozenTypeUseShape L3092–3094](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)；[frozenTypeShape L3197–3199](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)；[wire fill L3262–3265](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)。

基础 shape 从 layout 取得规范叶子和累计 dimensions；Arguments 用途随后直接把它的 arguments 换成选出的源码子 shape，保留旧 dimsOrSize。选出的子类型本身为 Array 时，结果是“旧外层维度＋嵌套 Array 子槽”，破坏原生单叶子表示。

固定官方 [SubstituteTypeArgs L2359–2361](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L2359) 对选择后的 typeArgs[0] 调用 GetArrayTy；[GetArrayTy L113–126](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp#L113) 当子类型为 Array，取其规范叶子并加其 dims；[SaveArrayTy L1067–1075](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L1067) 只保存单叶子和累计维度。即原输入维度 d、选出 Array(k, L) 时，native 选择后形状为 Array(d+k, L)；当前为 Array(d, Array(k, L))。这是源码上的形状差异，未执行该输入；不能套用未经运行的期望维度数字。

**下一修复：** 将 RawArray 的选择后规范化放入共享完成产品／shape owner，按固定 SubstituteTypeArgs→GetArrayTy 合同生产完整叶子与 dimensions，再驻留／填表。不能只把原始 arguments 塞回旧 layout，也不能以降维声明视图代替 type-system 规范叶子。补 nested／flat、选择后 Alias 叶子、累计维度及真实 encoder 验证；旧 29 窗口不覆盖这个新增出口。

## T4 [P2] REJECT：CFunc 子节点在当前 origin 停止前先检查 source 原型，Semantic 分支再重建 Function

具体坐标：[Graph L139–150](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt)。

外层 SOURCE CFunc 找到 argument 后，L142 先调用 syntaxPayload(argument)，要求最终 source prototype 必须是 FunctionType，L143 才进入会检查当前 origin 的 selectReference。一个已发布 COMPILER_ADDED／SEMANTIC、已完成 Function type 且无 source prototype 的引用会先在 L142 被拒绝，不能到达其停止合同。若它保留 FunctionType 原型并到达 Semantic，L146–150 又读取其 Function payload，制造新的参数／返回用途并强设 C 标记；该实现没有保留“当前节点停止返回”的产品形态。

官方 L2377–2382 在真实 RefType→FuncType owner 上设置 isC，然后调用 ObtainsAliasType，后者 L2417 仍先执行当前 FuncType 的 COMPILER_ADD 停止条件。这需要真实角色与生成事实；不能把解析 wrapper 有没有 source prototype 当成角色资格。此项是允许的生成节点／已知类型合同反例，不声称该分支已经被本轮 fixture 触发。

**下一修复：** 冻结真实 CFunc→FuncType 角色与 C 标记的生产事实；对 argument 按当前节点合同裁决，再消费已发布选择结果。不得先追 prototype 作为 non-SOURCE 引用准入门槛，也不得通过 Semantic 分支二次造子用途补齐。需覆盖普通 SOURCE、真实 COMPILER_ADDED FuncType、无源码原型的完成输入及 Alias 选择，证明与 native 同一顺序。

## T5 [P1] REJECT：Generic 上界队列保存了捕获域，填 shape 时却改按声明 context 域查找

具体坐标：[Producer.GenericTypeAnchor L180–184](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)；[登记 L3148–3155](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)；[prepareGenericTypeBounds L3220–3222](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)；[Cone overload L3103–3108](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)。

锚点记录 input.domain 和固定 projection，结构 children L3160–3161 也在 input.domain 查；但排队上界调用 `frozenTypeShape(it, pending.context, pending.projection)`，该 overload 回到 exportAnalysis.typeElement(type, context)，即 context.moduleData.session。pending.anchor.domain 没有参与这次查找。

[TypeElement L55–56](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourceElement.kt) 已明确捕获域独立于声明所属 session；[Snapshot.type L199](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt) 实际按 domain＋Cone identity 双键取值。域不同时，当前代码会取另一份 payload 或找不到该 identity；TP 映射正确不能补回被换掉的输入域。本轮未运行跨域反例。

**下一修复：** 从排队产品保存的实际 TypeElement／anchor.domain 读取全部上界，向下传递同一 domain 与 projection，不能重回声明 context 域。选择后的 Arguments／Function shape 也需让这个域协议贯穿 Generic 上界闭包。补同一 Cone 在两个已捕获 domain 绑定不同声明／上界的验证，不能按 ClassId／Cone.equals 归并。

## T6 [P1] REJECT：“全部选择后 shape 已接入”不成立：约束与 catch 的真实 Type 槽仍只剩 Cone

具体坐标：

- [Producer.genericMetadata L1827–1835](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) 的 upperBounds 从 explicitUpperBoundsOf 取得 Cone 后直接 type(Cone, declaration)，没有进入 TypeUseGraph 的原 TypeRef 选择。图 L32–37 的公开产品也没有独立原始 constraint 上界用途。官方 [SaveGeneric L1145–1156](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L1145) 对每个实际 upper Type 节点执行 ObtainsAliasType，并保存各 constraint 原始分组／位置／implicit 属性。
- [SourceGraphProducer.catchPattern L531–534](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt) 将 resolved pattern Ty 与 pattern.typeRefs 全部降成 Cone 列表；[CfirSourcePattern L72–78](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/source/CfirSourceGraph.kt) 没有逐槽 TypeRef／选择身份；[CanonicalEncoder.pattern L180](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoCanonicalSourceEncoder.kt) 全部直接 typeReference(Cone)。官方 [SaveExceptTypePattern L830–833](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ExprWriter.cpp#L830) 第一个 info.ty 是直接语义 Ty，其后每个 catch Type 节点各自 ObtainsAliasType。不能把这两种槽合并处理。

这里并非要求“所有 Cone 调用改为 Alias selector”：官方 ExprWriter L40、L145／166 对表达式 Ty／实例化实参直接 SaveType，TypePattern L790 也使用 PackNodeInfo.ty，这些语义槽应保留直接保存。问题是实际原 Type 槽在产品阶段被删除了身份，encoder 无法执行其当前 origin／Alias 资格选择。

**下一修复：** 在 providers／完成 SOURCE 产品分别冻结 constraint 上界与 catch 后续 Type 槽的身份、次序、来源与选出用途，再统一进入 type(use)／完整 shape／TP domain；第一语义槽继续按其 native 角色保存。原 Generic 分组／隐式来源合同也不能用按 TP 聚合的 Cone 列表冒充。补显式 Alias 与语义 Ty 共享 Cone 但需不同保存选择的用例；不得只接声明／RHS／super 后宣布所有用途完成。

## 首轮行为记录与下一修复顺序

已读 [482e6a09 首轮结果](type-use-graph-first-results-20261119.json)：记录为 42 项、40 passed、2 failed、exit 1；同一批的前后 aggregate SHA 为 `3FEABB729B310B3A4E64F4DA734C5F9200542CDC96C354B93ED7EF2884BB16C4`。本轮没有重新执行或独立逐份核验 XML 窗口，以上是已保存记录，不是本轮运行成绩。

两条 orderedAliasRoles 的记录失败均为 Carrier body 顺序：期望 `[asThis, asNominal, choose, pair, init]`，实际 `[init, asThis, asNominal, choose, pair]`。 [断言 L284–291](../../cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CfirSourceFrozenTypesTest.kt) 位于该失败 [L295](../../cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CfirSourceFrozenTypesTest.kt) 之前，四个 Alias self／RHS 检查越过不能证明 [L297 以后](../../cfir/analysis-tests/tests/org/cangnova/cangjie/cfir/analysis/tests/CfirSourceFrozenTypesTest.kt) 的 signature／use／递归闭包通过。当前 [Producer.bodyMembers L1805–1806](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) 只按已有成员序列过滤；这项行为阻塞保留，不修改正确 fixture，且不把它作为上述 T1–T6 的推测根因。

主线程下一修复可直接按 T1／T2 的当前节点事实，T3／T4 的选择产品，T5 的同域上界闭包，T6 的遗漏用途推进，并保留 Carrier 顺序余额。每项需对应定向行为证据；不能通过删除正确类型断言、限制输入到单一来源／单一 domain、保留旧 Cone-only 出口或扩大测试豁免完成 gate。本轮不修改这些代码，也不另起或联系旧会话。

## 关键磁盘版本

关键文件在 `2026-11-19T03:34:16.6883786+08:00` 与 `2026-11-19T03:37:46.0400697+08:00` 两次读取一致。报告写入后发现 SourceGraphProducer 发生外部更新，已于 `2026-11-19T03:41:45.0252718+08:00` 重读其 functionBody／catchPattern 受审路径，相关坐标及 T2／T6 结论不变；下表该行记录重读后的 SHA，其余行使用前述一致版本。不以首轮输入 aggregate 或 HEAD 代替这些未提交字节。

| RepoRelative 文件 | SHA-256 |
| --- | --- |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/utils.kt) | `E9B8AF6F1FF31DFBE5452FAC9E80635674ED828CD57788EBF7A251EB935ED875` |
| [cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt](../../cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt) | `F97A98BCB00A69498BE838F2A4EA6CBAA6B43BA28DDC22652A12F800F3705E38` |
| [cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt](../../cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt) | `2250DD7C2AB45BF69B2B5E092BC8248B8161DC14AEA66342B2C53CD1D7C8C614` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt) | `AD47B04C93B6F7310D0E255EC23DC087808BDF19F76DECA15C4A8C9BA1D1408B` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysis.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysis.kt) | `F26D9012A798A403DFF99618FB06D77431C881CFFCFBCEE91625219E3FB33527` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt) | `CD57A357CD80659991940D64B4925DA53A891F4A5C58A83C2F012D6EA4E7D674` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypePayload.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypePayload.kt) | `DDCE9F6021CA1F7F2EFF8DB89E421D81A4C4C6E050E39A822FF03EC468072BE4` |
| [cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourceElement.kt](../../cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourceElement.kt) | `E2278E05738E9E6DD483A8154E9AE31D69A8FDE0BAA6B2396C31B951E1D4948F` |
| [cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirDeclarationExportFacts.kt](../../cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirDeclarationExportFacts.kt) | `11A273B54B76D8FC28CA2CF3780F0B730567ACBF5C2082BC33740AC769743BA5` |
| [cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt) | `0D41DE85DF456DB9AEF0447BD149B886B9D7B1ED016E06D73FE26A32DAA15F0E` |
| [cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoCanonicalSourceEncoder.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoCanonicalSourceEncoder.kt) | `CCF0BE8D979ECCCE7621415DEF15A5D74904446575811AB83F2423E581C33330` |
| [cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt) | `3B2CF078AC4A0CE779A7C7B84E7F8902AA552F12AC83B1CC2B40B0198308A58B` |

