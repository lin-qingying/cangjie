# Macro fragment 的真实 owner 上下文（2026-10-06，待实现）

字段共享生成已提交 `29178cd7f`，独立 When 求值修复为 `4f1635f7e`。本文是下一 patch 的接口与调用点映射，**尚未实施**；不表示真实 LL 宏构造、默认构造器阶段迁移或完整 CJMP 目标已经完成。

## 1. 现有缺口与已核定依据

当前 `MacroFragmentParserConfiguration.kt` 把 parameter fragment 包进 `func __macro_fragment__(...)`，声明 fragment 按顶层文件解析。两 raw 的 `buildValueParameterInPackage` 只传 containing symbol，没有传真实 primary 参数身份；`buildDeclarationFragmentInPackage` 只设置包上下文。结果是成员/primary/参数文法和符号 owner 仍不完整。已删除的 LT FUNC→primary 按名提升分支不能恢复。

官方参考仓库 `D:/code/intellij/cangjie/external/cangjie_compiler` 必须读取 **`git show v1.1.3:<path>`**，不能使用当前 checkout 的版本：

| 官方位置 | 对下一 patch 的约束 |
|---|---|
| `src/Parse/ParseMacro.cpp:57–121,517–520` | 按原调用 scope 解析输出；原 modifiers/annotations 只交给首个生成声明；捕获 `scope` 和 `outerDeclIdent`，不是只保存 package |
| `src/Macro/MacroExpansion.cpp:345–369`，`ReplaceEachMacro` | 新 parser 设置编译选项、`SetPrimaryDecl(outerDeclIdent)`、`EnableCustomAnno()`，然后 `ParseNodes(scope, originalNode, modifiers, annotations)` |
| `src/Parse/ParserImpl.cpp:42–47,70–73` | parser 的 scope 输入为 `variant<ScopeKind, ExprKind>`；primary 名是文法输入 |
| `src/Parse/ParseDecl.cpp:1044`，`ParsePrimaryConstructor`；同文件约 2558 的 `ParseLetOrVarInParamList` | primary/成员参数身份由 CLASS/STRUCT primary scope 形成，不能在 raw 层按函数名补判 |
| `src/Parse/ParseDecl.cpp:2370–2469`、`src/Parse/ParserImpl.h:569` | 普通函数/secondary 的 `ParseParameterList` 默认 scope 为 `UNKNOWN_SCOPE` 且要求 `: T`；getter/setter 参数只解析名字，不消费 `: type`、`!` 或默认值 |
| `src/Parse/ParseAtom.cpp:1455–1558` | lambda 使用独立 `ParseFuncParam`，不经过 declaration 参数的 annotation/modifier/macro 前缀解析；没有直接 lambda 参数宏入口 |
| `src/Macro/MacroExpansion.cpp:215–290`，声明/参数替换 | 声明零输出允许删除，多个声明允许展开；参数允许多个 FuncParam，但零输出报 `macro_unexpected_empty_parameter` 并保留原 slot；替换后复核整个参数列表的 named 顺序 |

Kotlin 参考根为 `D:/code/intellij/cangjie/external/kotlin`：

- `compiler/psi/parser/.../KotlinParsing.java:2690`：完整参数前缀与 rest 在统一 parser owner 中解析；函数类型匿名参数另有分支。
- `analysis/low-level-api-fir/.../lazy/resolve/RawFirNonLocalDeclarationBuilder.kt:36–94`：从实际 designation/原声明构建上下文，设置 package 与真实 symbol，随后按 designation 路径进入 owner；不按名字重新认领声明。
- `compiler/fir/raw-fir/psi2fir/.../PsiRawFirBuilder.kt:803,877,1257–1260` 与 LT `LightTreeRawFirDeclarationBuilder.kt:621–632`：callable id、dispatch receiver、构造器类型参数从实际 owner 上下文取得。
- LT `fir/ValueParameter.kt:43–61,116`：参数描述保存实际 primary 角色与 containing symbol，成员生成消费这些已知事实。

Kotlin 没有仓颉 token macro，因此下面的 fragment scope DTO 是仓颉语义所需数据；沿用 parser、raw builder、providers construction 各自现有 owner，不新增兼容解析分支。

## 2. 分开两个接口：文法上下文与 CFIR 身份

**PSI 层新增无 CFIR 引用的 `CjMacroFragmentParsingContext`**，放 `psi/.../parsing`。使用互斥的文法角色表达 TopLevel、Member、ParameterList、LocalStatements、Expression、Annotation；不能仅以“是否有括号”或输出名称分流。

- Member 携带实际 class/interface/struct/enum/extend body 种类；class/struct 可携带 parser 所需 primary 声明名。
- ParameterList 按语法角色区分 `RegularTyped`（普通函数/secondary，native `UNKNOWN_SCOPE` 且要求 `: T`）、`ClassPrimary`、`StructPrimary`、`AccessorNameOnly`（getter/setter，仅名字）。不能用 `typeRequired: Boolean` 表达这一差异：name-only 不是“类型可选”，也不接受 `!` 或默认值。
- lambda 参数保留独立语法，不加入这个 declaration 参数宏入口；lambda body 内的 expression macro 是另一种角色，不能据此允许 lambda 参数 annotation/modifier/macro。
- LocalStatements 区分 FUNC_BODY 与 MACRO_BODY，允许声明和表达式混合的语句 slice，不能只叫 LocalDeclaration。
- Expression 携带原始 ExprKind 及逗号分隔环境；解析入口与实际替换目标的数量约束分别保存。不能统一降为 ALL，也不能只解析第一个表达式。
- 继续显式传递既有 `ParsingFileFacts` / declaration-source mode / CJMP 文件角色；普通宏新 token 的语言模块保持现有空字符串语义。

**providers 层新增 `MacroFragmentOwnerContext`**，由 `MacroFragmentInput` 必须携带。它包含上述 parser DTO，以及实际 CFIR owner 身份：

| 角色 | 必须提供的身份/事实 |
|---|---|
| 顶层声明 | 实际 `PreMacroCfirFile` 所属文件及 package；没有 nominal owner |
| nominal/extend 成员 | 实际 nominal 或 extend 声明/symbol；其类型参数与 self/extended type，不能只给 FQName |
| 参数列表 | 实际 callable symbol、已确认的 parameter grammar scope；primary 名义 owner 从真实父关系取得 |
| 表达式 | 实际词法 enclosing callable/nominal、局部上下文事实；不把最近一个变量声明自动当作求值作用域 |

`MacroSurfaceScopeContext.enclosingClassFqName/enclosingFunctionName` 继续用于已有显示/缓存事实，**不作为反查 symbol 的入口**。`MacroSurfaceContainerContext` 的现有 scope 标记可用于一致性校验，但不能替代实际 identity。
metadata writer 继续单独显式传入并共享宿主文件事务，不为 fragment 新建或发布 session registry。

## 3. 从哪里取得实际 owner

优先在两 raw 收集 surface 时保存已经存在的容器 symbol 与 parameter grammar 事实；symbol 可在收集时尚未绑定，construction 消费时必须已绑定。不要在收集阶段为了读取 symbol.cfir 破坏 raw 构建顺序。

| surface/carrier | 可直接取得什么 | 仍需补充什么 |
|---|---|---|
| `MacroSurfaceParam`，carrier 是 `CfirValueParameter` | `containingDeclarationSymbol` 是实际 callable，不必按名查找 | primary class/struct、getter/setter 等文法角色；actual nominal 从父关系或 raw 捕获取得 |
| annotation carrier 的 owner 是参数 | `annotationCarrier.owner` 是实际 slot owner；参数给出 callable symbol | 不能用 `annotation.containingDeclarationSymbol.cfir` 代替 slot owner，它通常指向父 callable |
| declaration/annotation carrier 是函数、构造器、字段、类型声明 | carrier 是待替换声明自身 | 声明自身的 symbol 不是其 enclosing owner；顶层/member 的父关系须显式捕获或从 CFIR 图建立 |
| expression carrier 是 `CfirErrorExpression` | 只有实际表达式对象 identity | 没有通用 parent/symbol；必须捕获词法上下文，或建立正式 CFIR identity parent index |
| builtin non-macro / node surface | 依具体 typed carrier 使用以上规则 | 不能因表面名称是 builtin 就忽略 owner；无 CFIR carrier 的 token-only 子节点继承已知 macro forest 上下文 |

若需要 parent index，在本次 pre-file 图上用正式 CFIR visitor 建立**对象身份的父边/词法 owner 集合**，不以 PSI 后代、offset、名称或 ClassId 查找父亲；不要遍历 `correspondingField` 等 attribute 反向链接。
索引必须记录每条入边，不能用 `IdentityHashMap<node, firstParent>` 隐藏共享节点。

对共享 initializer 的规则：同一 expression identity 若经多个变量/绑定路径可达，先将各路径归一为实际词法 callable/nominal 和参数文法上下文；所有路径结果一致时可使用该共同上下文，不能因为直接父变量不同就误判。结果不一致时必须要求 raw 采集的明确上下文并校验，不能选遍历首项、按同名变量推断或静默选择某个 owner。
这里规定的是 parent-index 的歧义处理，不宣称每个 initializer 都有多个父节点。

## 4. 下一 patch 的解析与 raw API

建议直接改造原有入口，不保留旧的“缺 context 则顶层解析”重载：

```kotlin
// PSI / LightTree facade；DTO 只来自 psi 模块。
parseMacroExpansionFragment(builder, parsingContext, fileFacts, ...)

// providers，示意新增必需字段。
MacroFragmentInput(..., ownerContext: MacroFragmentOwnerContext, annotationMetadata: CfirAnnotationMetadataBuilder)

// 两 raw 同职责入口，不再仅以 InPackage 表示全部环境。
buildDeclarationFragment(parsedFragment, ownerContext)
buildValueParametersFragment(parsedParameters, ownerContext)
buildExpressionFragment(parsedExpression, ownerContext)
```

parser facade 复用 `CangJieParsing` 的 `parseMemberDeclaration` / `parsePrimaryInitFunc` 和对应角色的参数文法。成员 fragment 必须实际进入 member 文法；参数片段按明确角色分派：普通/primary 使用完整参数解析，accessor 仅消费名字，lambda 不进入此入口。不要把 accessor 交给完整参数解析器再设置 `typeRequired=false`。不要构造 dummy class/function 来承载上下文，以免引入虚假作用域、父声明后置检查和 source 偏移。
PSI factory 应在 lazy parse 发生前把 DTO 安装到该次 fragment 文件的解析入口；LightTree 显式传同一个 DTO。二者返回同类角色的直接解析结果；只从该入口定义的直接 slot 收集产物，不再用 `findFirst`/任意后代搜索提取“第一个像参数/声明的节点”。

raw 构建时复用 `withPackageContext`、`withContainerSymbol`、`withDispatchReceiverType` 和真实 local 上下文。member callable id 使用最终接入 owner；self type 经 `owner.symbol.constructType(owner.typeParameters.map { it.symbol.constructType() })`，保留 class/struct 类型种类。extend 使用它自己的真实接收类型，不伪装成 nominal。
参数转换显式接收 grammar role，由 class/struct primary 角色决定 `isPrimaryConstructorParameter`；普通/secondary 的必需类型与 accessor 的 name-only 文法不能合成一个布尔输入。参数是不可变值参数，字段 var/status 仍通过已实现的 `memberParameterFieldInfo` 捕获。
不要仅设置 `forcedContainerSymbol` 就宣称上下文齐全：该属性会替换首次 push，必须与 raw 调用层级配对，不能把新声明自身 symbol 错换成外层 nominal symbol。

## 5. 必须一起迁移的调用点

| 位置 | 下一步动作 |
|---|---|
| 两 raw 的 `macroSurfaceScopeContext` / `macroContainerContext` / expression surface 采集 | 捕获可用的真实 owner symbol、parameter scope；当前 PSI expression surface 的 enclosing 名称为空更说明不能靠名称字段补身份 |
| `MacroSurface` / `CfirReplaceHandle` / `MacroFragmentInput` | 选定唯一 typed owner-context 承载位置，并将它显式传入每次 reparse；不在 session 放可变“当前 owner” |
| `MacroExpandPhase.parseAndDesugarFragment` | 按实际 pre-file/carrier/parent index 构造上下文；macro forest 内子节点使用明确父上下文 |
| `MacroFragmentParserConfiguration.kt` | 删除 parameter dummy 函数包装；替换固定顶层声明入口；PSI/LT helper 全链传 DTO、实际 owner、现有 writer |
| `CangJieLightParser` / `CangJieParser` / `CjPsiFactory` / `CangJieParsing` | 加 scoped fragment 入口，复用已有文法方法并保持精确 parser diagnostics/source |
| 两 raw 的 `buildDeclarationFragmentInPackage`、`buildValueParameterInPackage`、`buildExpressionInPackage` | 迁为显式 owner-context 构建；不恢复 FUNC 按名升 primary |
| `MacroFragmentResult` 与 `CfirExpressionMacroStableSplicer` | 用 typed slice payload 表达声明零/多输出和参数多输出；声明零输出删除原 slot，原 modifiers/annotations 仅应用于首个生成声明；参数零输出报 `macro_unexpected_empty_parameter` 并保留原 slot，成功替换后检查整个列表的 named 顺序。若本 patch 暂不做 vector，必须列为未完成，不能把 singleOrNull 当最终 native 契约 |

构造错误/取消继续向外传播；用户 token 语法/合法数量失败仍走 typed parser/construction 结果。metadata 的原子 annotation-list API 与 shared primary materializer 沿既有顺序调用，不能在 seal 后补 owner 或字段。

验收聚焦：同名普通函数不变 primary；class/struct primary fragment 与参数 let/var facts正确；普通/secondary 要求类型、accessor 仅名字、lambda 无直接参数宏入口；声明零/多输出及仅首声明继承前缀、参数零输出保留原 slot 与整列表 named 顺序；member/extend generic self type 和 callable owner正确；参数 annotation 的 slot owner不同于解析 callable仍正确；共享 initializer 多入边不选首项。然后重跑字段/宏/LL 回归。默认构造器 COMMON/interop/CJMP merge 后门禁与真实 LL macro service 仍是独立待办。

## 6. 2026-10-07 scalar 产物链的实际阻点（待实施）

本轮只完成 raw 回归与运行器，以下是下一步生产改造位置，不能当作已实现能力：

- `TokenBackedMacroFragmentParser.parse` 对所有模式统一拒绝 `source.isEmpty()`，阻断合法的声明删除；
  空参数又需要专用诊断和保留原槽位，不能简单删除判断后把两者归为同一种成功。
- 两 raw 的 `buildDeclarationFragmentInPackage` 使用 `singleOrNull()`，把声明零/多产物一起丢弃。
- `MacroFragmentParserConfiguration` 的参数 PSI/LT 分支取第一个后代参数，且使用虚假普通函数包装；
  必须与真实参数角色和直接参数列表入口同时迁移，不能靠 `findFirst` 支持多产物。
- `MacroFragmentResult.Success.payload` 和 `MacroFragmentReparseResult.Success.payload` 当前是 `Any?`/`Any`；
  应改为互斥的 typed expression、declaration slice、parameter slice、annotation 结果。
  DeclarationSlice、StatementSlice允许空/多；ParameterSlice成功应非空；scalar/vector Expr替换目标的成功结果恰为一个Expr，
  但parser必须读完片段再判数量/种类，不能丢弃第二个节点。custom annotation保持原独立契约。
- `MacroExpandPhase` 的 `replaceDeclarationSlots` 当前以iterator.set替换一个声明；
  `replaceParameterSlots` 当前以map替换一个参数；两处payload读取强制scalar cast。
  需要真正的slice替换、仅首产物继承前缀、删除旧owner时metadata/annotation槽位处理、整个参数列表named顺序复核。
- `MacroConstructionDiagnostic.Kind` 尚无 `MACRO_UNEXPECTED_EMPTY_PARAMETER`，当前通用Failure会变为
  `MACRO_EXPANSION_FAILED`；DEGRADED路径还会改写原参数。必须接入专用kind/报告通道与保留原参数语义，
  不能只新增枚举或将错误降为普通成功。

下一批验收需同时涵盖PSI/LT、声明0/1/多、参数0/1/多、原annotation前缀只交首产物、
空参数诊断后原identity仍在、列表named排序、primary LET/VAR成员字段、真实owner与泛型receiver。
共享writer事务、字段materializer和后续provider发布顺序保持已有硬约束。

## 7. 2026-10-07 错误、替换顺序与普通 parser 前置

固定v1.1.3的新增取证：

- `MacroExpansion.cpp:273–290` 空参数输出仅报告`macro_unexpected_empty_parameter`并返回，不erase/insert；
  原参数identity、索引、左右相邻参数保持。`CompileStrategy.cpp:562–577`与`CompilerInstance.cpp:183–213`
  因新增错误停止宏阶段后的普通编译，不进入Sema。`CheckUnhandledMacroCall:90`已有错误时直接返回，避免额外unresolved。
- `MacroExpansion.cpp:254–290`每次成功替换后扫描整个当前列表：遇到`isNamedParam=true`之后，
  每个后续false参数均报`macro_named_parameter_after_unnamed`，定位该违规参数，不读取默认值、不重排、不撤销slice。
  调用按位置排序后倒序替换（同文件:177、:372）；同一列表不能左到右替换并对尚未替换的placeholder误报。
- `MacroCall.cpp:37`把原annotations交给ParseNodes；声明零输出时前缀没有接收者，不交下一sibling。
  `ReplaceDecls:230–250`保存原macro节点到历史集合后从活动声明列表移除。
- `CollectMacroEachFile:71–86`对macro节点SKIP_CHILDREN，内层由MacroEvaluation子forest求值。
  外层删除时，被其结果吸收的子slot不能作为独立AST替换继续报“carrier找不到”；已产生子诊断不能丢弃。

对应接口应将空参数表示为专门的正常用户错误结果，例如`OutputRejected(EMPTY_PARAMETER_SLICE)`，
由construction报告专用kind且不生成参数replace slot；不得走通用DEGRADED placeholder改写。
splicer需显式结构化诊断通道，对named顺序报告专用kind并保留当前暂存图；失败事务不seal/publish。
当前collector优先宏调用source，必须让named错误采用违规参数source。声明删除时按对象身份和实际词法包含关系
退役吸收的annotation/parameter/expression slot，不能静默清空所有未匹配slot。

PSI/raw入口前置审查时，普通`CangJieParsing.parseClass`没有保存真实类型名。
2026-10-07基础步已捕获原始名字与kind，并隔离成员语境；仍未实施native名字候选/InvalidDecl恢复，
也未将该事实传入scoped fragment。下一批继续让普通源码与fragment共用完整primary名字/作用域输入，
不能只修fragment或恢复raw按函数名猜primary。
`CjPsiFactory.doCreateFile`中的`markGenerated(file)`会触发AST，scoped DTO必须在它之前安装。
表达式fragment若含return等控制流，还需要真实enclosing function target，不能只补包/容器symbol。

实施顺序：PSI无CFIR的DTO与直接文法入口 → actual owner/context → 两raw slice构建 → typed结果/诊断/splice
→ 删除旧scalar与包装入口 → PSI/LT联合验证。以上作为贯通改动验收，不把未接通的新API声明算完成。

## 8. 2026-10-07 数量约束与名字恢复补充（仍待贯通）

`ParseNodes` 能解析逗号分隔节点，不表示所有容器都允许多输出。
v1.1.3 `MacroExpansion.cpp:298–339` 对 scalar Expr 与 vector Expr 两种replaceLoc均要求恰好一个Expr：

| 真实替换位置 | 零输出 | 多输出 |
|---|---|---|
| initializer、FuncArg.expr、条件、返回值、单个下标 | `macro_unexpect_no_expr`，保留原slot | `macro_expect_one_expr`，保留原slot |
| array/tuple children、quote exprs的vector Expr位置 | 同上 | 同上，不能展开成多个元素 |
| Block.body的vector Node位置 | 删除原语句 | 允许声明/表达式混合 |
| 文件/名义/extend的vector Decl位置 | 删除原声明 | 允许声明序列 |
| vector FuncParam位置 | 专用空参数错误，保留原slot | 允许参数序列，并复核named顺序 |

`G_EXPRS_CONNECTED_BY_COMMA_LIST`仅ARRAY_LIT、TUPLE_LIT、FUNC_ARG（ParseMacro.cpp:42）。
实际vector目标见MacroUpdateInfo.h:66–79/140–147；下标列表逐项UpdateExpr。
`macro_unexpect_no_expr`定位宏调用起点；`macro_expect_one_expr`定位新产物末项，单个非Expr也使用后者。
需测试array/tuple/调用实参输出`1, 2`被拒绝、输出单个`(1, 2)`成功，并独立验证Block允许多语句。

primary名字的真实规则也不只是比较相等：ParserUtils.cpp:580与ParserImpl.h:421先限定class/struct，
要求identifier/软关键字后随`<`、`(`、`[`、`{`，原始拼写（含反引号）与真实owner的编辑距离不超过1。
ParsePrimaryConstructor:1062对近似但不相等的名字报告expected declaration与primary-name help，继续解析完整签名/正文；
ParseClassBody:1167、ParseStructBody:1624在收尾把不匹配的PrimaryCtorDecl改为InvalidDecl。
`func Owner`永远走普通函数handler。远距离名字走普通InvalidDecl恢复，不能生产primary或其成员字段。
native LevenshteinDistance按UTF-8字节处理，空target返回0；不能用空字符串伪造宏fragment的未知owner。
普通源码已破损的名字与fragment缺上下文必须分开表达。当前正在做参数文法/作用域基础步，尚未实现该名字恢复链。

另外，raw owner捕获应保存实际容器symbol路径、参数签名owner/grammar、receiver、function target和local身份的不可变快照。
参数先于function target创建，默认值不能制造dummy target。参数annotation的slot owner仍取carrier.owner。
若以CFIR父边索引校验共享initializer，必须保存全部入边并归一词法上下文，不得选首个父亲。
普通源码仍用原有classdetector检查父属性；新的宏parser不能从CFIR宿主复制common/abstract，native只显式传scope/primary名字。

## 9. 2026-10-07 parser 语法事实与 SDK 253 生命周期考证（待实施）

#### 2026-10-07 主构造器名字前置的生产接线（验证待完成）

共享 `seeingPrimaryConstructor` 已对普通成员和无括号宏输入使用真实 class/struct scope、
完整 raw 名字 UTF-8 字节距离及四种后继 token。缺名源码对应原生 invalid identifier，
不是空 target；上下文禁止以空字符串代替缺失名字。自身错名诊断只包真实名字 token，
完整签名/正文仍解析；普通直接成员收尾返回 typed INVALID，宏 input 仍为 PRIMARY。

`CjInvalidDeclaration` 不暴露名字/callable/签名接口，使用现成 placeholder stub；
`CjFileStubBuilder` 保留 invalid stub 本身并跳过全部恢复子树，禁止持久化错误签名和内部成员。
三种 declaration token 集合及 PSI/LT raw 显式 invalid 转换已同步，源码stub218、片段算法18。
PSI raw 名义快照共用已有 rawIdentifierTokenText，保留反引号，不用语义名字补造原始拼写。

远名按真实声明起点或外层右括号恢复，组内成对括号整体消费；不因换行或分号单独停。
普通 body 丢弃 IS_BROKEN 产物，宏 input 保留 typed invalid 并报告专用无括号输入错误。
SeeingDecl 已按固定v1.1.3核全 FUNC/MACRO/LET/VAR/ENUM/TYPE/STRUCT/CLASS/INTERFACE/MAIN/EXTEND，
modifier范围还包括INOUT；unsafe块及annotation lambda/IfAvailable不是这条声明停止条件。
成对组消费与现有 statement recovery 共享机制，但各自外层停止规则保留。

定向验证暴露恢复已停到 MACRO，但普通成员分派没有接收 MacroDecl 的既有缺口。
固定 native `ParseMacroDecl` 在非法成员作用域只标记 HAS_BROKEN，仍完整解析并返回 MacroDecl；
`IsInvalid` 不按 HAS_BROKEN 判定，名义 body 保留该声明。现共享成员分派复用 MacroParser
的 context 设置，parseMacro 对自身关键字报告本地化作用域诊断、保留完整签名和正文；
LOCAL 用 FUNC_KEYWORD，对 scope 名称统一通过既有 parsing.scope 资源渲染。静态 gate 通过，
运行结果待完成；无括号宏 input 的 MacroDecl 分流不包含在这次修复中。

独立静态gate已通过；PSI/LT、raw NORMAL/LAZY和生产整树stub测试正在准备，不能先声明验证完成。
本步不等于下述 parser语法事实/SDK增量协议，也未完成scoped fragment与vector splice。

续接验证已证明生产整树 invalid 往返及 raw56项通过；最终输入集成15551已exit0，
PSI74/providers55/完整LT raw171/完整PSI raw242/frontend141/LL168共851项全通过。
完整CFIR仍在79919运行，不能引用旧8968项为最终输入证据。
局部语句 FIRST 补齐已有通用声明分派的起点，以对应 native SeeingDecl/ParseExprOrDecl 和
Kotlin STATEMENT_FIRST 的声明分派归属。顶层包头探测修正为 native SeeingMacroPackage
的双 token 判定，含访问修饰符前缀；宏定义不因起首 MACRO 被吃进错误包头。
macro package/public 控制用例与裸宏定义 AST 恢复分开；完整 macro 专用规则仍须另行贯通。

本节只记录设计考证，不表示 scoped parser、语法事实传输或 IDE 增量重解析已经实现。
核对对象为实施工作树的现有 parser，以及 Gradle 缓存中实际使用的 IntelliJ SDK **253.29346.379**。
SDK 结论来自对该版本 `core`、`core-impl`、`util-base` 等 jar 执行 `javap -c -p`；
下面使用类名和方法名定位字节码，不冒充该 SDK 未随缓存提供的源码行号。本轮没有运行 Gradle。

### 9.1 parser 记录事实，raw 只读取已记录结果

native 宏调用的 `ScopeKind` / `ExprKind` 必须在真实 parser 入口记录。raw 已捕获的实际 symbol、
receiver、function target 等 owner 身份与这些语法事实分别负责语义构建和文法分派，不能互相替代。

- 每个根解析调用创建私有的 `MacroSyntaxFactsBuilder`；同一次 token stream 的子 parser 共用它。
- 以实际消费的 `AT` token occurrence 的 parser 内 offset 注册宏事实，同时记录该次宏消费结束位置。
  该键只定位已经记录的事实，不查找 owner、不定位 CFIR splice slot，也不跨文件或跨次解析复用。
- 高层表达式入口只有在整个表达式的起止消费位置等于该宏的消费 span 时，才允许更新该宏的 `ExprKind`。
  `@M()`、`@M() + x`、`f(@M())` 不能因为出现同一个宏 token 就共享高层表达式角色。
- statement-start 的 scope 由明确的 statement 入口设置；不能把最近函数、变量或名义声明解释成 native scope。
- 冻结后的节点 payload 只保留不依赖原文件绝对位置的不可变语法事实。offset/end 属于该次解析事务，
  不能随节点复制后继续作为新文件中的绝对坐标。
- 缺少必需事实、活动 occurrence 重复注册、事实与最终语法节点不一致都必须失败，不能补成 `ALL`、
  顶层 scope 或最近 owner。quote/token-only 宏形状的角色也须由明确入口表达，不能与真实宿主声明混用。

LT 的 `ParsedCangJieLightFile` 应携带同一次解析的冻结表；raw 从宏节点的**直接 `AT` 子 token**取键。
PSI 应从真实宏语法节点上的数据读取；若按下述增量方案把权威数据置于直接 `AT` leaf，
则由 `CjMacroExpression` 提供统一属性读取该 leaf，不再在 raw 里按文件 offset 反查全局表。
查询中必须保持 parser 的坐标系，不能把独立 block/fragment 的局部 offset 当作宿主文件绝对 offset。

### 9.2 copyable user data 的真实复制契约

SDK 253 已核对的调用链：

| SDK 类与入口 | 实际行为与设计约束 |
|---|---|
| `com.intellij.lang.ASTNode.getCopyableUserData/putCopyableUserData` | 是公共 API；可直接挂在真实 AST 节点，毋须自造 element type 承载 occurrence 数据 |
| `com.intellij.extapi.psi.ASTDelegatePsiElement.getCopyableUserData/putCopyableUserData` | 委托 `getNode()` 的同名 API；不应改用普通 wrapper userData |
| `com.intellij.openapi.util.UserDataHolderBase.clone` | 清空普通 user map，再调用 `copyCopyableDataTo`；被复制的 payload 对象必须不可变 |
| `com.intellij.psi.impl.source.tree.TreeElement.clone` | 调用父类 clone，清空 parent/sibling 和相对 offset；不会把原 AST identity 变成复制后 identity |
| `com.intellij.psi.impl.source.tree.CompositeElement.clone` | 递归 clone 已有子树，并清空 PSI wrapper；节点 copyable data 随树复制 |
| `com.intellij.psi.impl.source.PsiFileImpl.clone/copy` | 先复制文件 copyable data；已加载 AST 递归 clone，未加载 AST 则保持未加载，之后重新解析 |

因此，已构建 PSI 的 `PsiFile.copy()` 可以保留节点事实；尚未构建 AST 的文件需要在文件上提前安装
不可变 parser seed，复制后的文件首次解析再生成自己的节点事实。`CjPsiFactory.doCreateFile` 的
`markGenerated(file)` 会触发 AST，seed 必须在该调用之前安装。

`psi/.../psi/CjMacroExpression.kt:36` 是 stub-backed PSI。源码 raw 若通过 `node` 读取事实会要求实际 AST；
这是需要明确的入口契约。copyable userData 不会自动进入持久化 stub，不能据此宣称支持 stub-only 消费。

### 9.3 lazy block/lambda 与增量 chameleon 是独立生命周期

实施树中的入口：

- `psi/.../parsing/CangJieParsing.kt:508–540` 的 `parseBlock` 默认可以 `collapse(BLOCK)`，
  此时只消费平衡 token，不解析块内宏。
- `psi/.../parsing/CangJieExpressionParsing.kt:2533–2549` 的函数字面量分支可以 `collapse(LAMBDA_EXPRESSION)`。
- `psi/.../psi/stubs/elements/BlockExpressionElementType.kt:47,112` 分别是 case block 与普通 block 的 `parseContents`。
- `psi/.../psi/stubs/elements/LambdaExpressionElementType.kt:62` 是 lambda 的 `parseContents`。
- `psi/.../parsing/CangJieParser.kt:82,195` 的 lambda/block 入口分别建立新的根 parser。

原 parser 必须在折叠位置记录不可变 lazy seed，保存真实 body scope、来源模式及该入口必需的解析事实。
以后 `parseContents` 从该节点 seed 建立新事务，发布新子树的宏事实；不能继续使用旧文件的可变 builder，
也不能用旧文件的 offset 表覆盖新子树。这样不同 lazy 节点可独立解析，没有 session 级“当前 scope”。
Kotlin 对应的独立入口位于 `external/kotlin/compiler/psi/parser/.../KotlinParser.java:84–96`；
仓颉增加 seed 是 native 宏语义所需信息，不是把 Kotlin 未提供的宏语义凭空归入普通 owner 推断。

SDK 的 `com.intellij.psi.impl.BlockSupportImpl.tryReparseNode` 会先用
`ILazyParseableElementType.createNode(newText)` **创建全新的节点**，并不会自动复制旧节点的 copyable data。
随后它调用公共 `IReparseableElementTypeBase.isValidReparse(oldNode, newNode)`；该方法默认仅返回 `true`。
block/lambda element type 可在此钩子中将旧节点的不可变入口 seed 显式装入新节点，再允许新节点解析。
只传 seed，不传旧 occurrence 表。该平台内部创建的 `DummyHolder` 是 IDE 重解析的既有容器，
不构成允许宏 fragment 人工构造 dummy class/function 的理由。

### 9.4 marker 回滚必须同步撤销语法事实

当前 parser 存在会经过宏或表达式解析的真实回滚路径，不能假设 AT 只解析一次：

| 实施树位置 | 回滚行为 |
|---|---|
| `CangJieExpressionParsing.kt:3513`，`parseQuotedMacroExpression` | 嵌套输入解析失败后整体 `macroExpression.rollbackTo()` |
| `CangJieExpressionParsing.kt:3216,3236`，`parseDeclaration` | 回滚 annotation/modifier/declaration 探测，再走实际分支 |
| `CangJieParsing.kt:4820`，参数解析 | 参数探测失败时回滚已经消费的内容 |
| `CangJieExpressionParsing.kt:3855,3876`，`rollbackOrDrop` / `rollbackOrDropAt` | lambda 参数等探测共享的回滚入口 |
| `AbstractCangJieParsing.kt:995,1669,1730` | token stream 探测及通用 marker/lookahead 包装也会回滚 |

建议在现有 `SemanticWhitespaceAwarePsiBuilderImpl` 统一包装 marker，使所有 `mark()`（包括直接
`builder.mark()`）与 `precede()` 进入同一事实 journal，而不是只在几个已知宏方法手工清理：

1. `mark` 捕获 journal checkpoint。宏注册和后续 `ExprKind` refinement 都记录可撤销操作。
2. `rollbackTo` 与 delegate marker 回滚同步撤销该 checkpoint 之后的事实操作；重新解析同一 AT 才能重新注册。
3. `drop` 只删除当前 marker，不撤销保留的子生产物事实；`done` 同样不是全局事务提交。
4. `precede` 使用被包围 marker 的起点 checkpoint，不能使用调用 `precede` 时的当前 journal 长度。
5. `doneBefore/errorBefore` 传入 delegate 前须解包另一个 marker。SDK 253 的 `Production` 范围 getter
   也必须委托真实 marker，不能继承接口默认实现而丢掉 token 范围。
6. `collapse` 对折叠后不再存在的子生产物事实作明确处理，并保留该 lazy 节点的入口 seed。
7. 根解析结束才冻结；取消、异常或未完成的解析不发布结果。对外不能暴露可变 map、builder 或 marker。

`AbstractCangJieParsing.createTruncatedBuilder` 只是对原 token stream 增加 EOF 视图。
`CangJieParsing.create`、`createForByClause`、expression/quote parser 的 `create` 必须共享同一根 journal，
不能因为新建 parser 对象就新建 facts builder。Kotlin 对应为
`external/kotlin/compiler/psi/parser/.../AbstractKotlinParsing.java:380` 的同形 builder adapter。
同时检查各 `create` 是否保留原 `isLazy`；当前 `CangJieParsing.create` 有直接传入 `true` 的现存实现，
不能在新 facts 路径里再以此假定“子 parser 必然属于另一个 lazy 根”。

### 9.5 Marker/LighterASTNode identity 不能作为长期 occurrence 键

SDK 253 的公共继承链确实是：

```text
PsiBuilder.Marker
  → SyntaxTreeBuilder.Marker
  → SyntaxTreeBuilder.Production
  → LighterASTNode
```

但 `PsiBuilderImpl.StartMarker` 不是稳定语法节点身份：
`MarkerProduction.rollbackTo/dropMarker` 调用 `MarkerPool.freeMarker`，之后会清理并复用该对象。
`PsiBuilderImpl.createMarker` 从同一 pool 分配；AST 构建另行创建 `CompositeElement`。
`PsiBuilderImpl.MyTreeStructure` 的 leaf 节点也使用 `LimitedPool`，不能把 `getChildren` 返回的
AT token 对象保存在 `disposeChildren` 之后作为长期 identity。

所以 marker identity 只用于当前解析事务内的 marker 包装，不能作为跨 parser→AST/LT 的统一身份。
采用根内 token occurrence 键不涉及推断 owner，反而避免了对象归池重用和 AST 另建身份的问题。

### 9.6 已否决的“treeBuilt 返回后 attach”完整性声明

`PsiBuilderImpl.getTreeBuilt → buildTree` 在 `myOriginalTree != null` 且允许增量合并时执行：

```text
prepareLightTree()
merge(originalTree, parsedRoot, lastCommittedText)
throw ReparsedSuccessfullyException(diffLog)
```

实际字节码在进入普通 `createRootAST/bind` 分支之前抛出该异常。因此
`builder.treeBuilt.also { attachFacts(it) }` 只覆盖正常返回 AST 的路径，**不能覆盖 IDE 增量重解析**。
不能捕获此异常后直接修改旧树：此时 diff 尚未作为 PSI 修改提交，取消和并发读取仍需遵守平台契约。
也不能因数据是 copyable 就假定新 chameleon 或新 diff 节点已经携带该数据。

其他已否决做法：保留所有尝试并选择同 AT 最后一条、按最近 owner 补 scope、回滚后保留旧 refinement、
每个 occurrence 新建并全局注册 `IElementType`、在全局/session/ThreadLocal 放“当前 parser 事实”、
缺失事实时补 `ALL`/顶层文法、用 dummy function 包装参数、仅因文本相同复用带旧语境的数据。

### 9.7 完整增量传输的具体候选与下一步落点

若要覆盖首次 AST、lazy、复制及 IDE 增量合并，已找到可用的 SDK 构建钩子：
`PsiBuilderImpl.protected createLeaf(IElementType, start, end)`。
该方法不只创建普通 leaf；`collapseLeaves` 也经它创建 collapsed BLOCK/LAMBDA。
增量 `PsiBuilderImpl.ASTConverter.convert` 对 token 调用其所属 builder 的同一方法，
对 composite 通过 `bind` 构造子节点时也复用该 builder。

可实施的候选为每次解析建立私有 builder 子类，先由 parser 冻结事实，再在 `createLeaf` 中按该次
明确的 token 范围给真实 AT leaf 安装 fact、给真正的 lazy node 安装 seed。
让 `CjMacroExpression` 读取自身直接 AT leaf 的事实作为唯一权威，避免 macro composite 上重复缓存后失效。
这与原先“只在 treeBuilt 返回后 attach 到 macro composite”的方案不同，仍需生产实现与回归验证后才能验收。

`PsiBuilderImpl.CUSTOM_COMPARATOR` 是本次 builder 可设置的公共 key。
SDK `MyComparator.deepEqual` 在普通 token 文本比较前调用 custom comparator；可纯比较旧节点 fact/seed
与本轮新事实，差异时返回 `NO`，交给平台生成替换 diff，禁止在比较时修改旧 AST。
还必须验证文本相同而语法入口变化的节点及其 lazy 祖先不会被整个复用；不能仅新增 comparator 就宣称完成。
按本次冻结事实判等时只比较语法含义，不比较解析事务 identity 或原文件 offset。

| 必改归属/调用点 | 下一步具体动作 |
|---|---|
| `psi/.../parsing/CangJieParsing`、`CangJieExpressionParsing`、`SemanticWhitespaceAwarePsiBuilderImpl` | 根内 journal、完整 marker 包装、真实 scope/ExprKind 记录、所有子 parser 共享事务 |
| `CangJieParser` 与 `CangJieLightParser.parseWith` | 同一次解析完成后冻结；PSI 构建与 LT 结果拿到同一语法事实契约；独立 block/lambda/fragment 根不共享可变状态 |
| `ParsedCangJieLightFile` | 与 tree/fileFacts 一起携带本次宏事实；不接受 raw 对裸树补造默认事实 |
| `CjFileElementType.doParseContents`、`CangJieMacroCallParserDefinition` 的文件入口 | 若采用 builder 构建钩子方案，统一创建本次私有 builder，保留 SDK 原有 chameleon/merge 输入 |
| `CjBlockCodeFragmentType`、`CjExpressionCodeFragmentType`、`CjTypeCodeFragmentType` | 与正式文件入口使用相同生命周期；不留下仍能生成无 facts AST 的旧入口 |
| `BlockExpressionElementType`、`CaseBlockExpressionElementType`、`LambdaExpressionElementType` | parseContents 接收节点 seed；isValidReparse 显式传递旧→新 seed；新建 parser 使用新事务 |
| `CjPsiFactory` | 在任何可能触发 AST 的调用前安装 scoped fragment seed，并使文件 copy 的未解析路径保持该 seed |
| `CjMacroExpression` 与两 raw surface 采集入口 | 从真实直接 AT token 读取 parser facts；再与实际 CFIR owner 快照组合，不通过名字/offset 反查 owner |
| parser 与 PSI/LT 回归 | 验证回滚后同 AT 重解析、嵌套宏、whole-expression refinement、truncated parser、PSI copy、lazy block/lambda、同文本不同入口及 IDE 增量重解析 |

上述候选不要求取消平台增量合并，也不要求全局注册带 occurrence 状态的 element type。
若继续采用仅在 macro composite 上存储 facts 的方案，必须先给出在 SDK merge 路径真实安装数据的构建钩子；
当前已取证的“返回后遍历 attach”不能作为完整方案通过。

### 9.8 续接 mapper 确认的构建、坐标及冻结契约（待实施）

SDK253的Heavy私有builder子类必须使用
`PsiBuilderImpl(Project,ParserDefinition,Lexer,ASTNode,CharSequence)`：它保留所属file、
charTable、TREE_TO_BE_REPARSED、oldTree及lastCommittedText。不能用全null的八参数构造器
替代Heavy入口；无宿主LT才按SDK factory使用八参数构造器，LighterLazy节点使用其专用五参数构造器。

`createLeaf(start,end)`使用本builder文本的局部字符坐标；StartMarker offset却包含myOffset，
LighterLazy可非零。根事务必须显式确定坐标转换，不能直接把任意Lighter节点offset当leaf键。
完成parseAction后先取builder.lightTree，使SDK prepareLightTree/balanceWhiteSpaces完成；
验证准备后的direct AT与collapsed节点并冻结事实，再请求treeBuilt或返回同一LT树。
collapsed最终范围不能只按collapse调用前后currentOffset猜测。这个顺序保留SDK正常merge异常流程。

marker包装必须委托全部Production默认getter：tokenType、start/endOffset、start/endIndex、
errorMessage、isCollapsed；doneBefore两签名、errorBefore必须解包且校验同事务marker。
precede继承原marker起点checkpoint；rollback撤销注册/refinement/seed，drop/done/error保留子事实。

SDK MyComparator在通常路径中先调用CUSTOM_COMPARATOR，再做token或collapsed文本判等；
普通composite返回UNSURE并继续下钻。比较AT的fact及collapsed BLOCK/CASE_BLOCK/LAMBDA的seed，
差异返回NO，相同返回UNSURE，禁止用相同事实直接YES或修改旧AST。
seed须包含改变内部解析含义的完整入口上下文，避免未展开lazy祖先被整段复用。
CustomLanguageASTComparator扩展先于该key；当前仓颉源码无注册，实际IDE扩展环境仍需gate。

下一贯通批次为journal/marker包装、真实scope/ExprKind消费点、统一builder、所有文件和
code fragment/lazy入口、LT事实结果、PSI直接AT读取及两raw消费；不能先上线无facts的scoped fragment。
必须真实document commit/BlockSupport diff验收，覆盖eager与未展开/已展开lazy的同文本不同语境、
普通composite祖先、非零LighterLazy offset、带注释collapsed边界和取消不发布。
现有ParsingFileCopyTest的内容更新/onContentReload只证明整根重载，不能替代增量merge gate。

### 9.9 续接审计补充：事务归属和显式坐标原点（待实施）

Kotlin 的 SemanticWhitespaceAwarePsiBuilderImpl/Adapter 没有 macro journal；其 mark 与
truncated builder 都直接委托根 token stream。因此仓颉新增 marker 包装归属根
SemanticWhitespaceAwarePsiBuilderImpl.mark，adapter/truncated/by-clause/quote 继续共享根事务，
不能只拦截 AbstractCangJieParsing.mark，也不能每创建 parser 就新建 journal。
Production 的类型、offset/index 五个默认 getter 会抛 UnsupportedOperationException，
errorMessage/isCollapsed 默认 null/false；包装必须全部委托真实 marker。

SDK253 的局部 currentOffset/rawTokenTypeStart 不含 myOffset，而 Production start/endOffset
含 myOffset。私有构造器只在输入为 LazyParseableToken 时将 myOffset 设为该 token.startOffset；
其他输入恒为零。统一工厂应显式传递事务原点：Heavy chameleon 和无宿主 LT 为零，
LighterLazy 为输入 chameleon.startOffset。prepared Production/Lighter 范围减原点再匹配
createLeaf 局部字符范围；不能用 Heavy 宿主在文件中的 offset，或从 AST 祖先猜原点。

SDK collapseLeaves 先调用 createLeaf，随后附加 LAZY_PARSEABLE_TOKENS；reuseCollapsedTokens
不会跳过 createLeaf。折叠节点的最终范围来自 prepareLightTree/balanceWhiteSpaces 后的
myLexStarts[marker start/end index]。collapse 当场保存的 currentOffset 不是最终边界证明。
折叠删除子事实也要可撤销 journal 操作，以支持外层 rollback；不能截断日志丢失操作历史。
drop/done/error 保留子事实，precede 沿用原起点 checkpoint，before marker 校验同一事务后解包。
真正独立 file/block/lambda/fragment 根在准备树验证后冻结；取消或异常不发布结果。

fixed ParserDiag.cpp:386 将 MACRO_BODY 映射为 macro/macro body。DeclarationParsingMode.LOCAL
并不足以区分普通函数、宏正文等实际body scope；本批 LOCAL宏定义回归只验证普通函数。
完整ScopeKind协议必须保存真实入口并通过lazy seed传输，不能从LOCAL默认补FUNC或根据CFIR后代猜测。

### 9.10 固定 native 的 scope 联合值与表达式根 refinement（待实施）

本次直接读取 v1.1.3 Parser.h:23、ParserImpl.h:43、ParseMacro.cpp:46/503、
ParseExpr.cpp:372、ParseAtom.cpp:1444、Parser.cpp:539 后，必须保留以下区分：

| 真实产生路径 | invocation.scope 的值 |
|---|---|
| 普通表达式 atom，ParseMacroExprOrLambdaExpr → ParseMacroCall 默认参数 | ScopeKind.UNKNOWN_SCOPE |
| ParseExpr(ExprKind ek) 最终返回值本身是 MACRO_EXPAND_EXPR | 改成 ExprKind ek |
| 该宏被 binary/prefix/call/index/ParenExpr 等节点包围 | 不把外层 ek 写到全部后代宏 |
| ParseExprOrDecl(sk) 的语句宏 | 先用真实 ScopeKind sk 构造，再继续 ParseExpr(DOT, expr) 的运算链 |
| 定义/参数宏 | 各自真实 ScopeKind，与 ExprKind 联合类型明确区分 |

ScopeKind.UNKNOWN_SCOPE 是原生 atom 的明确值，不是缺少事实时可补造的默认值。
ExprKind.UNKNOWN_EXPR 则是另一个枚举里的 libast 入口，两者也不能互换。
当前普通局部声明探测先接收 AT/MacroExpressionParser，再直接结束语句，会吞掉语句宏的
后缀/二元运算继续解析；本批 initializer 原子优先级测试不能证明此路径已完成。
新的事实协议须连同这个真实语句入口修正，不以宏节点类名推导其 scope 联合分支。

原生 ScopeKind 共16种：TOPLEVEL、CLASS_BODY、INTERFACE_BODY、STRUCT_BODY、EXTEND_BODY、
FUNC_BODY、MACRO_BODY、PRIMARY_CONSTRUCTOR_BODY_FOR_CLASS、PRIMARY_CONSTRUCTOR_BODY_FOR_STRUCT、
PROP_MEMBER_SETTER_BODY、PROP_MEMBER_GETTER_BODY、ENUM_BODY、ENUM_CONSTRUCTOR、
PRIMARY_CONSTRUCTOR_FUNC_PARAM、MAIN_BODY、UNKNOWN_SCOPE。ExprKind 共14种：ALL、VAR_INIT、
INDEX_EXPR、IF_COND_EXPR、WHILE_COND_EXPR、LET_PATTERN、LEFT_EXPR、EXPR_IN_TUPLE、
EXPR_IN_IF_COND_TUPLE、EXPR_IN_WHILE_COND_TUPLE、EXPR_IN_ARRAY、EXPR_IN_CALLSUFFIX、
EXPR_IN_ANNOTATION、UNKNOWN_EXPR。不能用现有三种 DeclarationParsingMode 压平这些身份。

ParseNodes 的 isConnectedByComma 来源是 invocation 原始 parent ASTKind，仅 ARRAY_LIT、
TUPLE_LIT、FUNC_ARG 为 true；FUNC_PARAM_LIST 又是独立的参数分支。它不来源于 ExprKind，
也不决定最终 replaceLoc 的数量限制。ParenExpr 是真实节点：`f((@M()))` 的宏 parent 是
ParenExpr，不能因为 raw CFIR 消除了括号而将其误判为 FUNC_ARG。
`(@M(), 1)` 在发现逗号后才把第一项放进 TupleLit，先前宏的 parent-comma 事实需随后 refinement；
`(@M() + 1, 2)` 的宏是 binary 子节点，不应作为 tuple 直接孩子 refinement。
这些 refinement 与 scope refinement 都必须参与 marker rollback，不能只撤销 occurrence 注册。

ParseMacroInvocation 还会记录 curPrimaryDeclIdent 为 outerDeclIdent；它与当前
SeeingPrimaryCtorDecl 的 class/struct scope 条件分开。进入普通表达式不得因为清除
active member grammar 就误删仍有效的外层名义名字；原始 backtick 信息与语义名字也须区别处理。

### 9.11 签名 scope 与正文 scope 的原生切换（待实施）

继续读取固定 v1.1.3 `ParseDecl.cpp:2300–2390,2477–2535`、
`ParseMacro.cpp:220–263,404–431` 和 `ParserImpl.h`，不能把传入 ParseFuncBody 的 scope
直接沿用到它的 statement block。原生的真实切换如下：

| 入口 | 参数列表/参数宏的 scope | 直接正文语句宏的 scope |
|---|---|---|
| 普通函数、secondary init、finalizer、main | ParseParameterList 默认 UNKNOWN_SCOPE | FUNC_BODY |
| class primary | PRIMARY_CONSTRUCTOR_BODY_FOR_CLASS | FUNC_BODY |
| struct primary | PRIMARY_CONSTRUCTOR_BODY_FOR_STRUCT | FUNC_BODY |
| getter | PROP_MEMBER_GETTER_BODY | FUNC_BODY |
| setter | PROP_MEMBER_SETTER_BODY | FUNC_BODY |
| macro 定义 | ParseMacroParameterList → ParseParameterList 默认 UNKNOWN_SCOPE | MACRO_BODY |

`ParseFuncParameters` 只对两种 primary 和两种 accessor 显式传入 scope；普通函数与 main
仍走无参 ParseParameterList。`ParseFuncBody:2359` 和 `ParsePropMemberBody:2317` 明确调用
ParseBlock(FUNC_BODY)，不保留签名 scope。macro 定义则使用独立 ParseMacroBody，
在 `ParseMacro.cpp:246` 显式调用 ParseBlock(MACRO_BODY)。枚举中名字带 BODY 的
primary/accessor scope 因此不能按名字认作它们正文内部宏的 scope。

MACRO_BODY 同样不向所有后代 block 自动传播：固定 ParseAtom 中的 if/else、循环、
try/catch/finally、unsafe 和 lambda body 均显式进入 FUNC_BODY。一个 macro 正文的
直接语句 `@M()` 与其 `if (...) { @M() }` 内的宏须分别记录 MACRO_BODY/FUNC_BODY。
lazy seed 保存的是进入这个具体 block 时已经确定的 scope；它不能在 lazy parse 时
按最近 macro/class 祖先重新推导，也不能把普通嵌套 block 的入口继承写成兜底。

PRIMARY_CONSTRUCTOR_FUNC_PARAM 在固定 Parse 文件中只见于
`ParseMacroCallDeclInput:413–417` 的专用无括号 input 分支：在那里将参数解析 scope
明确改成 PRIMARY_CONSTRUCTOR_BODY_FOR_CLASS。它不是当前 primary 参数列表调用
ParseMacroCall 时实际产生的统一 scope，不能用它抹平 class/struct 参数宏的不同事实。
这个入口的显式转换与保留 invocation 原始 scope 是两个动作，输出 ParseNodes 仍消费
invocation.scope；不得为了共享 CjParameterGrammar 而改写所有 occurrence 的 scope。

下一批验收必须包括同一 macro 定义中的直接语句与嵌套 if/lambda、class/struct primary
的参数宏与正文宏、main 参数宏与正文宏、getter/setter 的 name-only 签名与普通正文。
先检查 parser 的事实联合分支，再检查 PSI/LT/lazy 传输；不能仅凭 CFIR owner 名称证明。

### 9.12 真实 SDK diff 验收入口与现有 fixture 的限制（待实施）

读取当前 SDK253 的实际 core-impl 字节码后，当前 CjParsingTestCase →
CjPlatformLiteFixture → CangJieCoreEnvironment → CoreProjectEnvironment 会注册
CorePsiDocumentManager 和 MockDocumentCommitProcessor；后者的同步/异步 commit 方法
只校验非空参数随后直接 return。本仓库 headless bootstrap 没有替换这两个服务。
因此仅在现有 fixture 内调用 commitDocument，不能当成执行过真实增量重解析的证据。

可在同一 fixture 直接走 SDK 的真实 `com.intellij.psi.impl.BlockSupportImpl`：
六参数 reparseRange/静态 reparse 接收真实 file、old FileASTNode、修改 TextRange、
新文本、ProgressIndicator、旧 committed text，实际执行选取可重解析节点或完整解析，
随后 mergeTrees；返回 DiffLog，用平台 doActualPsiChange(file) 在写操作内提交。
现有 bootstrap 已注册真实 TreeAspect/PomModelImpl。此路径仍须实际运行验证，不能
用手写 DiffLog、setTreeElement、onContentReload 或 mock commit 替代 SDK 解析/比较。

SDK tryReparseNode 的顺序是 isReparseable → createNode → 将新 lazy node 接到
DummyHolder → isValidReparse(old,new)。在调用 isValidReparse 前没有展开这个新节点，
这提供了安装旧不可变入口 seed 的实际窗口。只能传 seed，不能复制旧 occurrence 结果；
后续 mergeTrees 将 TREE_TO_BE_REPARSED 放到新节点上，再通过 getFirstChildNode 触发
真实 parser。它会正常捕获 ReparsedSuccessfullyException 并提取 DiffLog，而非由本地
parser 捕获异常并修改旧树。

mergeTrees 在新树已经预先展开等情况下还存在普通 ASTShallowComparator/diffTrees
路径；不能把只有 PsiBuilderImpl.CUSTOM_COMPARATOR 的安装当作所有 SDK diff 路径
已经验收。测试应让 SDK 自行创建、seed 和展开新 chameleon，并确认实际进入 builder
merge 路径，同时单独检查预展开新树是否存在生产调用入口；若存在，必须覆盖其事实判等。
需要断言旧 macro AT/lazy ancestor 的真实 identity 和事实变化，以及取消时旧 AST 与事实
均未改动。单看最终源码文本相同、或事后重新构建整树得到正确 facts，不证明节点复用正确。

### 9.13 根 marker 事务基础设施（2026-10-08，基础设施定向验证通过）

本批开始实现完整协议的根事务部分，尚未把生产宏 occurrence 登记及冻结结果贯通到 raw，
不能视为完整 macro context/scoped fragment 已完成。

- common 的 `MacroParsingFacts.kt` 保留 v1.1.3 的16种ScopeKind、14种ExprKind与联合类型；
  occurrence 语义显式包含直接parent逗号属性及外层原始声明名字，不提供缺失scope默认值。
- psi 的内部 `MacroParsingJournal` 按根局部AT offset保存注册、scope/逗号refinement的旧值日志，
  collapse删除也可撤销；不保存marker identity、不暴露可变集合、不发布未准备树结果。
- `SemanticWhitespaceAwarePsiBuilderImpl.mark` 在根包装SDK marker；adapter/truncated继续委托同根。
  precede继承原起点checkpoint，rollback同步撤销，drop/done/error委托并保留子事实。
  两组before overload均校验同事务且解包，七项Production getter完整委托SDK。
- 静态gate通过这批基础设施：collapse即时半开范围只用于事务删除，不能作为lazy最终范围；
  冻结结果需依据prepared token顺序，不能依赖rollback后可能改变的map插入顺序。
- 真实SDK回归 `MacroParsingJournalMarkerTest` 共7项，覆盖rollback/refinement、池化后的重注册、
  precede、drop、collapse删除撤销、truncated共享、两接口二参数doneBefore及范围getter、
  跨根before拒绝、联合UNKNOWN身份。随后补充两个真实SDK回归，遍历两接口的三参数
  doneBefore/errorBefore，检查错误结束保留事实、非空errorMessage与折叠节点true isCollapsed。
  三参数doneBefore的production本身仍没有errorMessage，errorBefore的production则持有消息。
- 验证handle **17486** 已终止exit0（9m13s）：队列JAR以`--project-dir`指定CJMP工作树执行
  `:psi:test --tests '*MacroParsingJournalMarkerTest*'`；7/7，0失败/错误/跳过，
  新鲜XML UTC2026-10-08T02:46:34.080Z（北京时间10:46:34.080）。本批生产输入冻结结束，
  不再轮询或重启旧handle。本批通过不代表所有before overload/lazy/增量协议完成。
- 补充验证handle **39188** 已终止exit0（37s）：同一工作树执行上述marker测试，
  新鲜XML共9/9，0失败/错误/跳过，UTC2026-10-08T03:04:26.080Z。

下一步仍须贯通全部真实parser scope/ExprKind/parent-comma登记、lazy入口seed、prepared tree
校验与不可变发布、统一私有builder/createLeaf、PSI/LT/raw和真实SDK diff；基础设施不替代这些门禁。

### 9.14 语句宏继续表达式运算链（2026-10-08，验证中）

固定v1.1.3 Parser.cpp:539在普通语句宏入口先ParseMacroCall(sk)，再ParseExpr(DOT,expr)。
本地parseStatement先做局部声明探测，提前消费整个宏后结束语句，因而不能解析后续运算。

现有Precedence/parseBinaryExpression的首操作数parser仅沿左侧高优先级递归，到postfix入口
创建完整宏节点后继续共享后缀与二元运算。右操作数仍调用普通入口，宏输入内表达式也不继承
首操作数parser。普通parseStatement及LOCAL parseStatementByScope按真实LOCAL输入文法调用，
backToken=false；内置、平台和custom annotation模式保留各自入口。没有持久pending状态。

静态框架gate已通过；新增PSI/LT显式形状回归包括语句宏的member/index/call/binary、乘法优先级、
幂右结合及物理换行截断。handle **58563** 正在工作树验证MacroExpressionPrecedenceParsingTest、
MacroParsingJournalMarkerTest、PrimaryConstructorIdentityParsingTest；生产与测试输入冻结，
尚无终止结果。不能复用此前全CFIR的通过结果证明本批通过。

此批尚未登记完整scope/ExprKind事实，亦未完成macro fragment的ParseNodes入口。静态gate指出
需要补测函数体内annotation-only分流及直接LOCAL scoped入口。普通macro起始合法名字token
检查仍是已有缺口；共享ALLOW_NEWLINE_OPERATIONS已包含DOT，后续应实际验收跨行DOT延续。

### 9.15 固定 native 正文 scope、词法 primary 名字与恢复契约补充（2026-10-08）

本节复核 `D:/code/intellij/cangjie/external/cangjie_compiler` 的
`git show v1.1.3:<path>`，沿用已有探针证据，未重复运行官方探针或 Gradle。
新 `CjParserSeed` 与 seeded parser 入口已有生产接线，主线程仍在修复和执行动态验证；
本节记录语义约束及未完成事项，不声明 seed、scope 登记或 SDK 增量协议已经验收。
此前章节保留为各阶段历史记录；其中名字含义以本节更正为准。

**更正 `outerDeclIdent` 的“原始拼写”说法。** §9.13 及此前候选模型把 occurrence 的
`outerDeclIdent` 说成外层原始声明名字，这不符合固定原生字段的实际赋值：

- `src/Parse/ParseMacro.cpp:519–520` 保存 `curPrimaryDeclIdent`，即已去反引号的语义名。
- 原始拼写另由 `curPrimaryDeclIdentIsRaw` 与语义名共同表示；
  `src/Parse/Parser.cpp:645–654` 的 `GetPrimaryDeclIdentRawValue()` 在需要时补回反引号，
  `src/Parse/ParserUtils.cpp:580–583` 用这个原始拼写进行 UTF-8 字节 Levenshtein 候选比较。
- `src/Parse/ParseDecl.cpp:1425–1427,1662–1664` 只在 class/struct 的 body 入口设置该词法
  primary 名字，退出后恢复；函数、默认值、lambda、控制体不会清除它。
  interface/enum/extend 本身不设置该名字，不能把每一种名义声明都当成新的 primary 名字来源。
- `src/Macro/MacroExpansion.cpp:355` 重新调用 `SetPrimaryDecl(invocation.outerDeclIdent)`；
  `src/Parse/ParserImpl.cpp:70–73` 转交的 `isRawId` 使用默认 false。这是原生输出重解析
  的明确行为，不能为了保留源码反引号而偷偷改变 invocation 字段的含义。

因此要分开当前成员文法、词法 primary 名字和 macro occurrence 的 `outerDeclIdent`。
当前成员文法决定此处能否识别 class/struct primary；进入普通表达式或正文时应清除。
词法名字按 class/struct body 的进入/退出保留语义名与 raw 标记，seed 必须保持这项状态。
occurrence 对位原生 `outerDeclIdent` 时只记录语义名。不得从清空后的 `memberScope`、
CFIR owner、最近 PSI 祖先或任意名义声明名字反推缺失事实。

**参数文法和正文 scope 分别设置。** 下表是直接正文语句宏的真实入口；
scope 名中含有 `BODY` 不表示它会传播进相应函数的语句体。

| 入口 | 参数列表/参数宏 scope | 正文直接语句宏 scope | 固定 v1.1.3 依据 |
|---|---|---|---|
| 普通函数、secondary init、finalizer、main | `UNKNOWN_SCOPE` | `FUNC_BODY` | `src/Parse/ParseDecl.cpp:2370–2380,2359` |
| class primary | `PRIMARY_CONSTRUCTOR_BODY_FOR_CLASS` | `FUNC_BODY` | `src/Parse/ParseDecl.cpp:1068,2374–2377,2359` |
| struct primary | `PRIMARY_CONSTRUCTOR_BODY_FOR_STRUCT` | `FUNC_BODY` | `src/Parse/ParseDecl.cpp:1073,2374–2377,2359` |
| getter/setter | 对应 `PROP_MEMBER_GETTER_BODY` / `PROP_MEMBER_SETTER_BODY` | `FUNC_BODY` | `src/Parse/ParseDecl.cpp:401–403,2317` |
| macro 定义 | `UNKNOWN_SCOPE` | `MACRO_BODY` | `src/Parse/ParseMacro.cpp:246,259` |
| lambda、尾随闭包 | 独立 lambda 参数文法 | `FUNC_BODY` | `src/Parse/ParseAtom.cpp:1517–1558` |
| if/else | — | `FUNC_BODY` | `src/Parse/ParseAtom.cpp:450,468` |
| while/for/do | — | `FUNC_BODY` | `src/Parse/ParseAtom.cpp:1234,1278,1288` |
| try/catch/finally/handle | — | `FUNC_BODY` | `src/Parse/ParseAtom.cpp:595,618,627–629,644` |
| unsafe、match case、字符串插值 | — | `FUNC_BODY` | `src/Parse/ParseAtom.cpp:161,489,300` |
| synchronized | — | `UNKNOWN_SCOPE` | `src/Parse/ParseAtom.cpp:1420`、`src/Parse/ParserImpl.h:751` |

这些入口显式覆盖父正文 scope。macro 定义内直接 `@M()`、其 if/lambda 内的 `@M()`、
其 synchronized 内的 `@M()` 分别是 `MACRO_BODY`、`FUNC_BODY`、`UNKNOWN_SCOPE`。
`DeclarationParsingMode.LOCAL` 只够做现有声明分派，不能提供这三个原生事实。
getter/setter 可共享 `ACCESSOR_NAME_ONLY` 参数文法，但参数宏的 scope 仍应由各自入口
分别传入。lazy seed 保存的是这个具体入口已决定的状态，不能在展开时按 owner 名字补判。

**仍需按共享框架完成的恢复契约：**

1. `CangJieExpressionParsing.parseDeclaration` 返回 false 时，调用者会重新尝试表达式。
   因而 false 必须表示 token 位置与该次事务事实均回到入口、没有消费；不能消费部分前缀
   后返回 false。对于已确定属于声明的 annotation/modifier 前缀，原生
   `src/Parse/ParseDecl.cpp:77–81,123–129` 保留前缀消费，报告错误并返回 InvalidDecl，
   不回退成表达式。当前 `declType == null` 仍整体 `rollbackTo()` 的路径需要与这种
   确定声明输入分开；孤立 annotation 的恢复不能只靠原子表达式新增 AT 分支处理。
2. 无括号 macro input 中非法的 `Owner() {}` 要按真实 scope 恢复。
   `src/Parse/ParseMacro.cpp:405–435` 调用 ParseDecl 后，对无效声明报告
   `parse_illegal_macro_expand_input_args_without_paren`。在 FUNC_BODY/MACRO_BODY 中，
   `src/Parse/ParserUtils.cpp:542–576` 的恢复看到 expression-first 的 Owner 就停止消费；
   UNKNOWN_SCOPE 的 default 分支不消费。随后由 statement 分隔符恢复或外层文法处理。
   当前 `parseMacroInputExprWithoutParens` 在无 `memberScope` 时仍 advance 标识符，
   `parseMacroExpression` 仍使用 `Macro call has no input`，这两处尚未与原生契约贯通。
   不应为了保持完整外层 range 而吞掉余下 token 或伪造成功 input。
3. `CangJieParsing.parseMemberDeclarationRest` 对名义成员中的孤立 `{...}` 仍报错后
   `parseBlock()` 并返回 `FUNC`。原生 `ParseDecl.cpp:123–129` 与
   `ParserUtils.cpp:555–560` 返回 InvalidDecl，并按成对括号跳过无效输入；不会构造函数。
   此分支应并入无效声明恢复，不能给它补正常 `FUNC_BODY` seed 后当成合法正文入口。

接入应保持 Kotlin 的现有 `parseBlock/collapse → elementType.parseContents → Parser`
职责链；依据为 `D:/code/intellij/cangjie/external/kotlin` 中
`compiler/psi/parser/src/org/jetbrains/kotlin/parsing/KotlinParsing.java:1974–2001`、
`KotlinExpressionParsing.java:1088–1149` 和
`compiler/psi/psi-impl/src/org/jetbrains/kotlin/BlockExpressionElementType.kt:39–45`。
增加 seed 是仓颉 macro 语义需要的数据传输，不新增按文件类型或 owner 推导文法的旁路。
BLOCK、LAMBDA、CASE_BLOCK 还须保留各自语法入口；case body 不含外层花括号，不能统一
交给要求 `{` 的 `parseBlockExpression`。后续仍需以真实 lazy、PSI/LT、copy 和 SDK diff
验证 seed 与 occurrence 的传输及判等，生产接线本身不能替代这些验收。

### 9.16 SDK 文件结果的构建/提交钩子与真实 CodeStyle 接线（2026-10-08）

本节依据本机 IntelliJ SDK **253.29346.379** 的 `javap -p/-c/-v`、
`code-style-impl` 内的 `META-INF/CodeStyle.xml`、现有构建约定及真实 PSI worker
classpath 取证。SDK 以类名/方法名和字节码调用顺序定位，不伪造源码行号。
以下 per-parse ASTFactory/root metadata 方案**尚待生产实施和运行验收**。
CodeStyle 的公开 API 接线已由主线程改入工作树；主线程正在用 handle **49365**
执行编译验证，本节写入时尚未取得该 handle 的终止结果，不能记为编译或测试通过。

**文件结果不能只写进 SDK 临时文件的 userData。** `BlockSupportImpl.makeFullParse`
的普通文件分支依次调用 `viewProvider.createCopy`、`getFileCopy`、
`newFile.setOriginalFile(oldFile)` 和 `newFile.getNode`。`getFileCopy` 不复制文件的
copyable userData；`setOriginalFile` 因此需要保留调用方传入文件的解析输入。
随后完整 parser 发布结果的对象是这个临时 `CjFile`，并不是原宿主文件。
`CompositeElement.applyReplaceFileOnReparse` 只移动新节点的 children 与 CharTable，
不复制新 `FileElement` 或临时 `CjFile` 的 userData；该方法为 final。
只在临时文件写 `ROOT_PARSER_SEED_KEY`、实际模块和 file facts，不能证明提交后原宿主
已经得到新结果，也不能提前修改原宿主来弥补这条传输缺口。

**SDK 存在每次解析私有的 ASTFactory 构建钩子。**

| SDK 入口 | 已核实行为 |
|---|---|
| `PsiBuilderImpl.getASTFactory()` | 检测本 builder 的 `ParserDefinition instanceof ASTFactory`，成立则使用这个对象；不是要求把当前解析状态注册到全局 factory |
| `ASTFactory.createComposite(IElementType): CompositeElement?` | 公共可覆写实例方法；SDK 的普通 `bind` 和增量 `ASTConverter` 都经 `PsiBuilderImpl.createComposite` 调用它 |
| `ASTFactory.createLazy(ILazyParseableElementType, CharSequence?): LazyParseableElement?` | 公共可覆写实例方法；root/lazy 创建同样检查本次 parser definition 的 factory |
| `ASTFactory.composite(IElementType)` | 公共静态方法，继续使用该真实 element type 的正常 composite 构建协议 |

可在现有私有 builder 归属内创建 `ASTFactory(), ParserDefinition by originalDefinition`
的每根实例，保留真实 lexer、file node type、PSI factory 等定义行为；不向全局、session
或 ThreadLocal 安装本次解析状态。文件结果仍须在 prepare/freeze 成功后才能由构建钩子读取。
本次只需要对唯一的文件结果载体附数据；不能因为 factory 能看到 element type，就按类型
向多个 BLOCK/LAMBDA 猜配 occurrence seed。它的 composite 回调本身没有 token 范围参数。

**使用现有的真实 `PACKAGE_DIRECTIVE` 作为完整文件结果载体。** 当前共享 parser 中，
只有完整文件的 `parsePreamble` 调用 `parsePackageDirective`；没有 package 声明时也会
`done(PACKAGE_DIRECTIVE)` 创建空节点，随后还有真实的 `IMPORT_LIST`。
因此普通文件、声明来源、macro expansion 文件，以及空源码都具有这个直接子节点。
在本次 ASTFactory 创建该节点时附不可变结果（input seed、实际语言模块、file facts），
可以让 SDK 的 copy 和 DiffLog 移动真实节点时携带结果。这里没有添加假 token 或新语法节点。

这个载体比“完整文件局部 start == 0 的第一个 token”完整：空源码没有 token，却仍有
零宽 `PACKAGE_DIRECTIVE`。读取方应从已建立的当前 AST 的直接 package 节点取结果，
不能经可能返回 green stub 的 PSI getter、任意后代扫描或源码名字推断来补造结果。
CodeFragment 不具有该完整文件 preamble；它创建时捕获的独立 root seed 仍遵守自己的
content chameleon 协议，不能在通用文件结果刷新时被覆盖。

**两条 comparator 都必须比较该节点的完整结果。** AST→LT 路径由当前 builder 的冻结
数据比较旧 package 节点结果与本次文件结果；AST→AST 路径比较两个实际 package 节点
的 copyable 数据。不同返回 `ThreeState.NO`，相同返回 `UNSURE`，继续由 SDK 比较语法。
不以相同 metadata 直接返回 YES，也不在比较时修改旧节点。

零宽节点能够触发真实替换：`DiffTree` 对文件 root 的子节点调用
`looksEqual → comparator.deepEqual`；NO 映射为 `TYPE_ONLY`，
`matchNext3Children` 随后选择 `REPLACE_START`，最终调用 `nodeReplaced`。
它不会在这个分支进入“两个空节点只比 type/hash”的 `DRILL_DOWN` 分支。
因此即使 package 文本不变或两个节点均为零宽，变化后的编译输入/file facts 也不能让
旧载体整段复用。仍须用实际 SDK diff 验证，不能只以上述静态路径代替运行证据。

**`subtreeChanged` 是实际修改通知，但不是仅一次的事务末尾通知。**

- `DiffLog.performActualPsiChange` 执行所有 entries 后，明确调用 `file.subtreeChanged()`。
- `applyInsertOnReparse`、`applyDeleteOnReparse`、`applyReplaceOnReparse` 在各自实际
  修改 children 后，也会调用父节点的 `subtreeChanged`，后者可传播到 `CjFile`。
- 整文件替换还有明确中间态：`applyReplaceFileOnReparse → rawRemoveAllChildren →
  rawRemoveUpTo` 会在旧 children 全部删除、新 children 尚未挂入时通知一次；
  随后的 `rawAddChildren` 和 DiffLog 末尾还会通知。

若在 `CjFile.subtreeChanged` 刷新文件结果，只能在载体已经存在于当前实际 AST 时发布
这个不可变结果；不能每次通知都无条件要求载体存在，也不能把中间态的“没有子节点”
解释成最终空源码并制造默认结果。真正完成解析的空源码仍有上述零宽 package 载体。
若需要在全部 entries 执行完以后统一发布，可使用 `PomModelListener.modelChanged`：
SDK 在事务 `runInner` 完成、`updateDependentAspects` 之后发送该事件，随后才进行文档
同步的 `commitTransaction`。不得把该时机描述为任意写操作中唯一一次 subtreeChanged。
仅生成或放弃候选 diff 时，不得改变原文件的解析结果、入口 seed 或语法节点。

下一批 root metadata 验收至少覆盖：空→空但解析输入变化、非空→空、空→非空、package
文本相同但完整结果变化、前导空白/注释、未应用及取消的 diff、AST→LT 与 AST→AST，
并验证提交后的新 fragment 使用新宿主结果，已捕获的旧 fragment 仍使用自己的原输入。

**真实 CodeStyle 服务的公开 API 接线。** `DiffLog.doActualPsiChange` 固定调用
`CodeStyleManager.performActionWithFormatterDisabled`；真实 `CodeStyleManagerImpl(Project)`
再通过 `PostprocessReformattingAspect.getInstance(project)` 获取后处理 aspect。
该 getter 访问的是 `PomModel.getModelAspect`，不是独立的 project aspect service。
普通 SDK `PomModelImpl` 只提供 TreeAspect/PsiEventWrapperAspect，不能满足这条调用链。

`META-INF/CodeStyle.xml` 正式将 PomModel 覆盖为包私有的
`PostprocessReformattingAspectImpl$LangPomModel`。该类完整职责只有：调用公开父模型构造器，
创建公开的 `PostprocessReformattingAspectImpl(Project)`，让 `getModelAspect` 同时识别
实际 aspect 类及其父类，并在 `super.updateDependentAspects(event)` 之后调用
`postprocessAspect.update(event)`。

主线程已移除本轮新增的两处 `Class.forName` 注册，当前采用明确依赖与有类型的公开 API：

- `dependencies/intellij-core/build.gradle.kts` 显式增加
  `api("com.jetbrains.intellij.platform:code-style-impl:$intellijVersion") { isTransitive = false }`。
- `CangJieHeadlessPlatformBootstrap` 先注册真实 TreeAspect，再注册
  `CangJieHeadlessPomModel(project)` 和 `CodeStyleManagerImpl(project)`。
- `CangJieHeadlessPomModel` 完整复制上述 SDK 接线职责，使用公开基础类和真实后处理实现，
  不提供 no-op formatter，不改写或绕过 DiffLog 的实际 PSI 修改。
- `getModelAspect` 必须返回 `T?`：SDK 对未知 aspect 明确返回 null，LangPomModel 原样
  透传父模型结果；其 Class 参数非空，但返回没有非空保证。Kotlin 返回 `T` 会改变合同。

此前 unresolved 不能用“SDK 缓存已有 jar”解释为环境已满足。`intellijCore()` 来自
`repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/intellijDependencies.kt`；
聚合依赖原先传递引入的 ide-impl POM 将 code-style-impl 声明为 runtime。
真实 worker 参数 `C:/Users/lin17/.gradle/.tmp/gradle-worker-classpath14517462102818127734txt`
同时包含本工作树 PSI test classes 与该实现 jar，证明它原先进入测试 runtime；
这不等于进入 `compiler:config` 的 compile classpath。当前显式 api 依赖补齐的是后者。
handle 49365 的编译验证仍须取得终止结果，之后还须继续真实 diff 回归。

另更正 §9.2 对未加载文件 clone 的概括：SDK `PsiFileImpl.clone()` 会调用原文件的
`getTreeElement()`，该方法对非 physical 文件会加载内容。92064 的复制用例在创建后的
第 71 行断言已通过，失败是 copy 后的第 76 行；`createAnnotationOnlyFile(markGenerated=false)`
并未主动展开 AST。测试应按 SDK 非物理文件复制合同检查根已加载、collapsed 正文仍独立，
不能为保留“原文件一直未加载”的错误期望修改生产 clone。

### 9.17 根表达式返回协议、真实 invocation 登记与参数槽位缺口（2026-10-08，待实施）

本节基于实施树 `3a0abe8b6` 的只读核对；本地行号对应这个提交。官方依据继续使用
`D:/code/intellij/cangjie/external/cangjie_compiler` 的 `git show v1.1.3:<path>`。
本节给出下一批生产改造输入，不表示完整 occurrence 登记、LT 冻结结果、scoped fragment
或 vector splice 已完成。已有 parser seed/AST 提交与本节待实施项应分别验收。

**表达式结果只返回直接根宏身份，不能返回所有后代宏。**

在现有 `CangJieExpressionParsing` 中增加解析期间专用的结果协议，例如：

```kotlin
internal sealed interface ParsedExpression {
    data object Missing : ParsedExpression
    data object Other : ParsedExpression
    data class MacroRoot(val occurrence: MacroOccurrence) : ParsedExpression
}

// 只引用当前根 journal 中已登记的 token；不跨根发布，也不保存 marker。
internal data class MacroOccurrence(val offset: Int)
```

`Missing` 表示没有生成表达式根，`Other` 表示生成了非宏根；是否报告诊断不是这两个
状态的区分条件。已保留错误的真实宏节点仍须有明确的 occurrence，不能因它带错误而
改成无来源的普通表达式。这里只有当前根事务内的 token 坐标；最终 PSI/LT 发布的是
冻结语义事实，不发布这个临时解析结果，也不从 CFIR 后代或名字反查 occurrence。

| 本地函数 | 返回协议与动作 |
|---|---|
| `parseExpression:424` | 改为接收明确的 `CjExprKind` 并返回 `ParsedExpression`；仅在最终结果为 `MacroRoot` 时 refine 为 `MacroParsingScope.Expression(kind)` |
| `Precedence.parseHigherPrecedence:399`，PREFIX/EXPONENTIATION overrides:153/160 | 返回上层操作数结果，同时显式传递 grammar kind 与 `firstOperand` |
| `parseBinaryExpression:515` | 未消费运算符时透传左操作数；实际生成 binary/range/as/is/assignment 根后返回 `Other`，不改写后代 occurrence 已登记的 scope |
| `parsePrefixExpression:546` | 无前缀时透传；生成 PREFIX 根后返回 `Other`；递归保持低层调用，不能改为高层 `parseExpression()` |
| `parsePostfixExpression:586` | atom/首操作数结果作为初值；真正生成 call/index/member/postfix/optional/slice 根后清除返回的根宏身份；未成功的后缀探测不清除 |
| `parseAtomicExpression:871` | 普通宏以明确的 `ScopeKind.UNKNOWN_SCOPE` 登记并返回该 occurrence；其他实际原子返回 `Other` |
| `parseStatementMacro:3114` | 首操作数按 `context.statementScope` 登记，随后继续共享 postfix/binary 链；整个语句首宏入口不调用高层 ExprKind refinement |

`firstOperand` 从 `() -> Boolean` 改为 `() -> ParsedExpression`，仍只沿最左侧优先级
递归传递。右操作数与宏输入内部的表达式使用自己的入口。`parsePostfixExpression:708`
现有错误恢复会丢掉未成立的 receiver，再解析一个 atom；该分支没有创建 member 根，
应使用新 atom 的返回值，不能仅因消费过 `.` 就清除身份。

当前 `parseMacroExpression:3550` 的 `IElementType?` 同时用于 `backToken` 闭合约定与
失败，正常自行闭合 MACRO_EXPRESSION 也返回 null。不能用 null/non-null 判断是否产生
根宏。改造时必须把 occurrence 结果与“外层还需闭合的声明节点类型”分开表达。
宏登记必须处在实际宏 marker 捕获的 journal checkpoint 之后；外层 rollback 同时撤销
该次登记与 scope/comma refinement，不能撤销语法后留下事实。

官方 `src/Parse/ParseExpr.cpp:404–407` 只对最终 MACRO_EXPAND_EXPR 根改写 scope；
`src/Parse/Parser.cpp:539–558` 的语句首宏先 `ParseMacroCall(sk)`，再走低层
`ParseExpr(DOT, expr)`。Kotlin 对应 owner 仍是
`compiler/psi/parser/src/org/jetbrains/kotlin/parsing/KotlinExpressionParsing.java`
的 atom/postfix/prefix/binary 方法链。宏 occurrence 是仓颉语义所需数据，不据此新建
独立表达式 parser、全局状态或 ThreadLocal。

**低层 grammar kind 与 invocation.scope 是两种事实。**

- `ParseExprWithRightExprOrType:587` 将当前 `ek` 原样传给低层右操作数，用于条件等
  语法；这不等于给右侧直接宏写 ExprKind。
- `ParseExpr.cpp:700–728` 的前缀分支只将 IF/WHILE 条件及两个 conditional-tuple
  kind 转为 ALL，其余 grammar kind 保留；前缀包围的宏本身不做高层 scope refinement。
- `ParseExpr.cpp:385–386` 的 VAR_INIT 高层入口向低层传默认 grammar ALL，只有最终
  直接宏才被改写为 VAR_INIT。
- `ParseAtom.cpp:1115–1117` 的括号第一项将外层 IF/WHILE kind 改成对应
  EXPR_IN_IF_COND_TUPLE/EXPR_IN_WHILE_COND_TUPLE，其他一律 EXPR_IN_TUPLE；已有
  conditional-tuple kind 再进一层括号也变成普通 TUPLE。逗号后续项固定走普通
  EXPR_IN_TUPLE（同文件:1077）。

因此 `let x = @M()` 是 ExprKind.VAR_INIT；`let x = @M() + 1`、`1 + @M()`、
`-@M()` 内的宏保留 ScopeKind.UNKNOWN_SCOPE。函数直接语句 `@M() + 1` 的左宏保留
FUNC_BODY。`if ((@M()))` 是 EXPR_IN_IF_COND_TUPLE，而 `if (((@M())))` 与
`if (!(@M()))` 中的宏是 EXPR_IN_TUPLE。

**高层 ExprKind 与直接 parent-comma 的精确调用表：**

| 本地入口 | 高层 kind | 此入口对直接根宏设置 comma |
|---|---|---|
| `CangJieParsing.parseField:2548`、`parseVariable:2605`，仍在使用的赋值 helper:2465 | VAR_INIT | false |
| `CangJieExpressionParsing.parseTry` 的 resource initializer:1611 | VAR_INIT | false |
| `CangJieParsing.parseFunctionParameterRest:5083` | ALL | false |
| `parseValueArgument:2784`，普通调用、named、inout 实参 | EXPR_IN_CALLSUFFIX | true |
| annotation `[]` 内的 argument | EXPR_IN_ANNOTATION | true |
| `CangJieParsing.parseWhenAnnotation:996` | ALL | false |
| `parseParenthesizedExpression:1050` 的第一项 | 按上述条件/普通括号规则 | 发现实际逗号后，才给第一项的直接根宏 refine true |
| 同函数的逗号后续项:1061 | EXPR_IN_TUPLE | true |
| `parseTupleLiteralExpression:1099` | EXPR_IN_TUPLE | true |
| array literal 的 `parseInnerExpressions:1216` | EXPR_IN_ARRAY | true |
| `parseArrayAccess` 内的每个 index | INDEX_EXPR | false |
| `parseIf` 调 `parseCondition` | IF_COND_EXPR | false |
| `parseWhile` 调 `parseCondition` | WHILE_COND_EXPR | false |
| `parseDoWhile`、`parseMatch` 调 `parseCondition` | ALL | false |
| `parseLetExpression:2091` 的 initializer | LET_PATTERN | false |
| return/throw、for iterable/guard、match guard、synchronized mutex | ALL | false |
| `parseQuoteInterpolate:3434` 的 `$()` 内容 | ALL | false |

false 只表示该入口自身不提供 ARRAY_LIT/TUPLE_LIT/FUNC_ARG parent；更深的
array/tuple/call 仍使用各自入口。括号根是 Other，因此 `f((@M()))` 不能把里面的宏
改成 FUNC_ARG 的 comma=true。`(@M() + 1, 2)` 的第一项是 binary，不能 refine
其中的宏；`(@M(), 1)` 才在发现逗号后 refine 第一项的直接根宏。

`parseValueArgumentList(start,end)` 应接收显式 argument kind，由 annotation 调用点
传 EXPR_IN_ANNOTATION，不以 `[` 字符猜测。`parseAsCollectionLiteralExpression` /
`parseInnerExpressions` 应由 array 和 index 入口显式选择解析策略，不能继续共用
一个无参数 `parseExpression()`。官方依据是 `ParseAtom.cpp:369,1155,1194`、
`ParseAnnotations.cpp:107` 与 `ParseExpr.cpp:869`。

`parseRangeExpression:780/783`、`parsePefixSliceExpression:1240` 当前重新调用高层
`parseExpression()`，会把 range 子宏错误改成独立 ALL 根。官方 stop/step 走低层
优先级入口（`ParseExpr.cpp:229–260,317–369`）；普通 range stop 保留当前 grammar
kind，step 使用原生低层默认 grammar ALL，但二者均不因此给宏写 ExprKind.ALL。
index 的 range 分支先完整构成 range 根，再由 index 高层判断最终根是不是宏。

**真实 invocation 与 token 内部形状的登记边界：**

| 原始产生入口 | 是否登记为本次源 parser 的 invocation | 固定 v1.1.3 依据 |
|---|---|---|
| 普通 expression atom 的宏 | 是，初始为 ScopeKind.UNKNOWN_SCOPE | `ParseAtom.cpp:111–112,1444–1453` |
| statement 首宏 | 是，实际 statement scope | `Parser.cpp:551–552` |
| 顶层、名义/extend 成员、enum constructor 宏 | 是，各自明确 ScopeKind | `ParseDecl.cpp:89`，各 body 的 ParseDecl 调用 |
| 无括号宏输入中的声明宏链 | 是，沿真实 ParseDecl(scope) 输入登记 | `ParseMacro.cpp:405–435` |
| `quote(@M())` 的普通 token 内容 | 否，原生是 TokenPart | `ParseQuote.cpp:107–140` |
| `quote($(@M()))` 的插值表达式 | 是，经 ParseExpr(ALL)，外层 `$()` 是实际 ParenExpr | `ParseQuote.cpp:21–46` |
| `$identifier` | 否，RefExpr | `ParseQuote.cpp:52–66` |
| `@Outer(@Inner())` 的括号 token 输入中的 Inner | 不是本次源 parser 产生的 invocation | `ParseMacro.cpp:312–338` |
| 宏自身 `[...]` attrs 内的嵌套 `@` | 同上，只是 token | `ParseMacro.cpp:312–338,467–473` |
| 真正 Annotation 的 `@` 本身 | 否；其参数中的真实表达式宏分别登记 | `ParseAnnotations.cpp:97–149,320–380` |
| 原始声明/参数的 `@!Name`，尚未 EnableCustomAnno | 是，MacroExpandDecl/Param，并记录 compile-time-visible | `ParseAnnotations.cpp:212–223`、`ParseMacro.cpp:541–543` |
| EnableCustomAnno 重解析识别的 custom annotation 本身 | 否，此时已经是 Annotation | `ParseAnnotations.cpp:240–261` |

括号 token 输入的子宏由后续 `MacroEvaluation.CreateMacroCallTree` 独立扫描并构造，
不能提前冒充源码调用。`MacroEvaluation.cpp:89–104,820–876` 中的括号 child
UNKNOWN_SCOPE 是该后续 token forest 的明确规则，不是缺失源码事实时的默认值。
本地 `parseQuoteParameters:3396`、`parseQuotedMacroExpression:3485`、
`parseMacroInputExprWithParens:3738`、`parseMacroAttrExpression:3774` 即使保留用于
编辑器的 token 内部结构，也不能把这些形状登记成宿主 invocation。不能仅用
`quotedSyntaxDepth > 0` 禁止所有登记，quote 的 `$()` 会进入真正的表达式语法。

**更正本轮口头映射中的 IfAvailable 范围：普通源码仍是 MacroExpandExpr。**

固定 `include/cangjie/AST/NodeX.h:20` 明确写道：
`@IfAvailable(name: arg, lambda1, lambda2) after macro expansion (before it is a MacroExpandExpr)`。
普通源码通过 `ParseAtom.cpp:111–112,1444–1453` 的宏 atom 链产生 MacroExpandExpr，
其 root refinement 和后缀/二元延续应遵守普通宏规则；不能因为名字为 IfAvailable
就跳过 invocation 登记。

只有 `ParseExpr.cpp:381–391` 的高层入口在 `enableCustomAnno && SeeingIfAvailable()`
成立时调用独立 `ParseIfAvailable`，且 INDEX_EXPR 分支优先于这项检查。这个专用分支
不会再调用低层 postfix/binary continuation。`Parser.cpp:544–552` 的语句首入口也
是在 `curMacroCall || enableCustomAnno` 分支内识别 IfAvailable 并直接返回。
因此不能把“所有阶段的任意 AT atom 都专有化”或“任何位置都继续后缀”当作原生规则。
专有化后的 IfAvailable 条件实参通过真实 FuncArg 入口使用 EXPR_IN_CALLSUFFIX，
普通源码阶段的括号内容则仍是宏 token input，不能提前登记为那些实参表达式。

当前本仓 `parseExpression:426`、`parseStatement:3065` 的普通源码提前专有化是
**已知未迁移差异**。下一批必须按真实阶段/入口处理；不能根据现状把 IfAvailable
描述成原生普遍的独立表达式，更不能为保留现状而伪造或省略其原始 macro facts。

**参数宏必须接通原生参数语法与 construction 槽位，不能给强制 annotation 伪填 facts。**

`CangJieParsing.parseValueParameter:4877–4884` 当前先把大部分参数 `@` 强制切到
disableMacroParsing 的 annotation 分支，导致普通参数宏分支基本不可达。原生
`ParseDecl.cpp:2525–2532` 的顺序是先判断
`SeeingMacroCallDecl() && !enableCustomAnno`，成立则返回 MacroExpandParam(scope)，
否则才解析 annotation/modifier 和普通参数。

接线时参数槽位必须真正承载 MacroExpandParam 对应的输入和替换角色，并将已核实的
parameter grammar、真实 callable owner、parameter surface/replace handle 和
construction 参数列表槽位贯通。不能保留当前强制 annotation 分流，仅给注解补一个
scope 字段后宣称参数宏已完成；也不能从新宏输出的第一个后代参数补认原槽位。
参数多输出、空输出保留原 identity、成功后全列表 named 顺序复核仍遵守 §6–8。

| 真实参数入口 | invocation.scope |
|---|---|
| 普通函数、secondary、main、finalizer、macro 定义参数 | UNKNOWN_SCOPE |
| class primary 参数 | PRIMARY_CONSTRUCTOR_BODY_FOR_CLASS |
| struct primary 参数 | PRIMARY_CONSTRUCTOR_BODY_FOR_STRUCT |
| getter 参数 | PROP_MEMBER_GETTER_BODY |
| setter 参数 | PROP_MEMBER_SETTER_BODY |

`ACCESSOR_NAME_ONLY` 可以共享参数文法，但不能区分 getter/setter 的 invocation
scope；`parsePropertyGet:3109`、`parsePropertySet:3149` 必须分别明确传入。
lambda 参数继续独立文法，没有直接参数宏入口。
PRIMARY_CONSTRUCTOR_FUNC_PARAM 是后续特定 token child 的解析场景，不是所有
class/struct primary 参数 occurrence 的统一 scope。

原始 `@!Name` 在 EnableCustomAnno 之前仍属于声明/参数宏候选；本地暂存为
CjAnnotation 不改变该阶段语义。当前 `CangJiePsiBuilder.createLeaf:111` 与
comparator:125 只识别 AT。完整声明/参数 occurrence 接线必须同时支持 ATEXCL
的事实附着、冻结传输、copy 和两种 diff 比较，不能靠节点类型排除 @!。

**建议下一批的完整边界与验收：**

1. 在共享 parser 内接通根表达式返回协议、全部高层 kind/直接-parent comma 和
   statement/顶层/member/enum/参数 scope；同步处理 token-only、quote interpolation、
   annotation、原始 @! 与重解析 annotation，以及 IfAvailable 的阶段差异。
2. `CangJieLightParser.parseWith:115–127` 真正完成同根 journal freeze，把冻结结果
   与同次生成的树一同放进 `ParsedCangJieLightFile`。PSI 读取实际直接 AT/ATEXCL
   载体，LT 读取该次解析结果；缺少必需事实必须暴露，不得补成 ALL 或最近 owner。
3. 验收直接/嵌套根、语句 scope 与运算继续、两层及三层条件括号、前缀条件清除、
   array/index/range、annotation 与 When、tuple 第一项逗号 refinement 的 rollback、
   token-only 不登记与 quote interpolation 登记、getter/setter 参数 scope 区分，
   并在真实 clone/lazy/AST→LT/AST→AST diff 中检查 facts 的保存与变化。
4. 参数回归必须同时检查语法角色和实际 construction 槽位，不能只检查 DTO 值。
   若当前提交尚未纳入参数/ATEXCL 迁移，就只能注明 expression occurrence 的范围，
   不能宣称全部 MacroInvocation facts 已贯通。

这个阶段完成后仍须继续 scoped fragment 的直接 ParseNodes 入口、typed vector
产物和 splice；上述能力不因普通源码 facts 接线而自动成立，也不得降为最终单产物方案。

### 9.18 真实 occurrence 到 raw/construction 的传播边界（2026-10-08）

表达式 occurrence 的完整 PSI 278 项已验证，但 raw/construction 尚未消费它们。
PSI convertMacroExpression、声明 surface、annotation/parameter surface 未读取真实 AT facts；
LightTree2Cfir 接收 ParsedCangJieLightFile 后仅传 tree/source，丢失同根 macroFacts。
LT annotation arguments 局部重解析和 frontend createParsedLightTreeFragment 也必须保留
整个新 ParsedCangJieLightFile 产品，不能拿新树的 offset 查宿主或上一解析根的表。

后续完整调用上下文必须明确区分 source occurrence、初始 token child、reevaluation 和
annotation 重解析阶段。真实 source invocation 的 MacroParsingFacts 必选且无默认值；
scope 联合类型、isConnectedByComma、outerDeclIdent 原样传播。outerDeclIdent 可以真实缺失，
不能从 ClassId 或 raw owner 重新补写。raw owner snapshot 与实际替换槽位保持独立。
surface 转型、builtin classification、routing snapshot、forest 和 fragment 输入均须保存
唯一权威调用上下文；MacroFragmentInput 可读取所属 surface，避免两份字段互相覆盖。

固定 v1.1.3 ParseMacro.cpp:503、ParseExpr.cpp 与 MacroExpansion.cpp:355–359 给出的生产链是
原 invocation scope/outer name → EnableCustomAnno → ParseNodes。初始 token 子宏的
GetMacroScopeKind 在父宏带括号时明确返回 UNKNOWN_SCOPE，否则先检查真实声明/primary 参数，
再使用父 ScopeKind；父为 ExprKind 时明确返回 TOPLEVEL。GetMacroScopeKindReEval 不再检查
声明/primary 参数。它们是不同阶段的明文规则，不能复制 source facts，也不能用默认值代替。
quote token、普通 payload 内 AT 和尚未成为 source occurrence 的生成 token 子宏不得伪造
source facts。当前未找到生产 MacroSurfaceNode 构造或完整 token 子宏 discovery，仍未贯通。

另一个必须同批修复的阻断点：MacroDemandClassification 当前仅凭 MacroSurfaceExpr 选择
EXPRESSION mode，并用 surface 子类推导 slotType。原生 ParseNodes 按 invocation 的 scope
联合类型选择文法，ReplaceEachMacroHelper 另按 replaceLoc 检查产物类型与数量。
语句宏 Declaration(FUNC_BODY) 与 binary 原子宏 Declaration(UNKNOWN_SCOPE) 仍有各自真实
CFIR 替换槽位。parser scope 和 splice 槽位必须分别传递，不能添加 facts 后继续旧路由。
参数和原始 ATEXCL 尚缺真实登记入口，必须先贯通 parser 与 construction 参数槽位，
不能从现有 annotation DTO、containerContext.NONE/false 或 ownerSnapshot 补造事实。

### 9.19 官方独立 NL token 与真实 builder 消费模式（2026-10-09）

v1.1.3 `Lexer.cpp` 对 LF 与 CRLF 都输出单独的 `TokenKind::NL`。`ParserUtils.cpp::Peek/Next`
始终跳 COMMENT，仅在 `skipNL=true` 时跳 NL。`ParseMacro.cpp::ParseMacroCallTokens` 用
`skipNL=false`，所以反斜杠后的独立 NL 是诊断并停止位置；不能让 IntelliJ builder 跳到下一行
括号后声称与原生停点一致。`ParseQuote.cpp::ParseQuoteExpr` 也保留 NL；quote 插值表达式临时
启用跳 NL，结束后恢复保留模式。cjc 的 slash 词法错误门控parser报告，不能凭编译器只打印词法
错误推断 parser 没有NL行为。

SDK 253.29346.379核验：`PsiBuilderImpl.myWhitespaces` 是从 ParserDefinition 初始化的实例final集合；`eof/getTokenType/currentOffset/
tokenText/mark`均能触发跳过，`rawAdvanceLexer`不允许负步，回调不能阻止skip，建树时会重用
whitespace分类。唯一符合真实消费协议的方向是lexer分开输出水平WHITE_SPACE和实际NL token，
不将NL注册到`ParserDefinition.getWhitespaceTokens()`；NL类型通过`ILeafElementType`创建标准
`PsiWhiteSpaceImpl(text)`，并调用SDK `registerWhitespaceToken(NL)` 只让增量比较器承认新旧
whitespace PSI类型对应，不修改builder skip集合。

模式所有权属于`SemanticWhitespaceAwarePsiBuilderImpl`独立skipNL状态，现有enable/disableNewlines
只表示Kotlin语句边界，不能复用。token/eof/offset/text/advance按当前模式规范化逻辑cursor；
`mark`必须原样委托SDK，首marker保留完整输入起点，journal使用返回marker.startOffset。
实际被skipNL忽略的NL通过SDK `remapCurrentTokenAndRestoreOnRollback(WHITE_SPACE)`再
`rawAdvanceLexer(1)`交给平台空白绑定；显式scanner NL不变，回滚恢复类型由SDK事务负责。
lookAhead只规范化当前位置，再按raw indices只读前瞻，不映射未来NL。rawLookup、rawTokenTypeStart、
rawTokenIndex、rawAdvanceLexer继续遵守SDK物理token契约。Adapter共享模式，Truncated builder
的自有lookAhead使用同一忽略判定后再做真实截断。换行边界查询识别NL token；所有WHITE_SPACE/
comment集合读取一并审计。

模式切换必须逐入口遵循原生顺序，不能一概提前到左定界符之前。官方macro caller在skipNL=true
时消费左定界符，随后token scanner设置false；quote则先关闭skipNL并检查keyword后的换行。
quote插值开闭时精确嵌套restore，rollback沿真实marker及SDK remap/facts事务，不允许virtual token、
延迟消费或rewind。普通语法忽略的NL继续作为标准whitespace保留在AST中；宏opaque内部NL的
范围和token产品仍需按原生scanner独立核验。

Kotlin `SemanticWhitespaceAwarePsiBuilderImpl.java` 只从隐藏WHITE_SPACE检查语句换行，不含宏
token scanner。该lexer/消费模式差异由官方仓颉原生实现证明；实现须覆盖PSI、LT、增量converter、
截断parser、rollback与quote插值，不能单改宏scanner。

gpt-6.1-sol/high独立审查发现当前quote scanner仍用有限`QUOTE_TOKENS`门控。原生
`ParseQuoteTokens`保留所有普通lexer token；本地集合漏ATEXCL、DIVEQ、TILDE、XOR、LTLT、
MULMULEQ、ANDANDEQ、DOUBLE_COLON、CHARACTER_BYTE_LITERAL等，因此`quote(@!Foo())`或
`quote(a /= b)`会提早结束。修复应以真实RPAR且局部paren深度为零作为唯一scanner终止条件，
保留escape、`$(`插值、`FIELD_IDENTIFIER`和bare dollar分支，其余词元按原cursor推进；有限集合仍
被`CjQuoteTokens.tokens`使用，必须保留给PSI查询并审计怎样覆盖所有实际quote词元。

同一独立审查发现native `ParserUtils.cpp::Peek`遇END时把Begin/End设为最后一个非comment token
的End，而SDK在跳过尾随空白后EOF为物理文本End。当前共享`errorOnCurrentToken` EOF零宽范围仅
在无尾部trivia时相等。需使用真实token流计算协议EOF offset并覆盖尾空格/comment测试；不要对
所有parser EOF点作无证据全局偏移。当前`@M(\\`单例不能证明含trivia EOF对齐。

### 9.20 五项 CJMP `PARSE_*` 诊断迁移完成（2026-10-09）

五项语法规则已由共享 PSI/LightTree parser 按实际声明接收事实报告；CFIR 不再注册
`CfirCjmpParseRulesChecker`，相应五个 `PARSE_*` 工厂和默认 renderer 已删除。原 checker 唯一
拥有的版本支持门迁入 `CfirCjmpFeatureSupportChecker`，继续作为语义 feature 诊断报告；无报告点的
旧 `EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS` 工厂一并删除。checkers 与 Analysis API 诊断
转换器已重新生成。七个旧 CFIR fixture 仅移除 parser 诊断标记，保留源码和语义期望。

parser 记录当前声明的角色、来源范围、注解身份、直接成员种类和 body/accessor/initializer 事实；
不扫描后代 PSI/CFIR。Specific interface 的 `HasDefault` 精确处理成员自身 specific、函数体、属性
访问器、变量 initializer 和其他成员种类。Nominal 的 direct-member mismatch 仍短路后续
specific-member 实现及 common constructor 检查；static primary/secondary constructor 先报告
static-init，primary 参数 mismatch 随之短路，但外层仍检查该直接成员。primary 参数有效范围在
最后显著源码 token 结束；缺失类型时按 native `InvalidType` 零宽结束位置保留 trivia。generic
`@Frozen` 只短路该声明自己的 CJMP 检查，父层仍比较该直接成员；`COMMON_GENERIC_FROZEN_NOT_SUPPORTED`
仍由 CFIR 语义 checker 报告。显式 abstract 的 JavaMirror/JavaImpl 豁免使用 builtin annotation 身份。

独立 native 期望分别用于 PSI 与 LightTree 测试。`CjmpDeclarationRulesParsingTest` 覆盖消息和范围、
primary/secondary static-init、broken 参数、泛型 `@Frozen` 短路、modifier/file-role/version gate、
JavaMirror/JavaImpl 与接口默认实现边界。定向 `:psi:test` 通过。七个旧 fixture 在标准与 PSI
生成变体中 14/14 通过；7 个 WithoutAliasExpansion 变体由既有
`OnlyTestsWithTypeAliasesMetaConfigurator` 假设跳过。`cfir:checkers` 与 `analysis:analysis-api-cfir`
编译通过；`commonSpecificGenericFrozen` 的 CFIR 语义诊断用例也通过。
