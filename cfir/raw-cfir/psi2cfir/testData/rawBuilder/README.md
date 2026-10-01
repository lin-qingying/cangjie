# rawBuilder TestData

This directory stores golden-file tests for `PsiRawCfirBuilder`.

## Layout

- `declarations/`: file-level and declaration constructs
- `types/`: explicit type-reference constructs
- `expressions/`: valid expression conversion scenarios
- `control-flow/`: valid control-flow conversion scenarios
- `recovery/`: malformed/incomplete syntax recovery scenarios
- `cangjie-features/`: Cangjie-specific syntax (`extend`, `match`, `spawn`, `VArray`)

## Naming

- Input files must use `camelCase` names and `.cj` suffix.
- Each `.cj` file must have a sibling `.txt` golden file.
- `.lazyBodies.txt` is optional but recommended for declarations with bodies.

## Coverage Contract

- Keep [`coverage-matrix.md`](./coverage-matrix.md) updated when adding conversion branches.
- Every matrix entry must point to existing `.cj` files.
- Every `.cj` file in this tree must be referenced by at least one matrix entry.

## Recovery coverage (2026-10-01 批次)

`recovery/` 下的用例必须让 raw builder 走到 `buildErrorExpression(...)` 的具体原因；只有
`doWhileMissingCondition` / `prefixMissingOperand` 之外的新增候选在本批次被确认为不可达，已从测试数据中移除，
原因（PSI 入口的实测形状）记录如下，待 builder 侧修复后可直接按这些形态补回：

| 期望原因 | 试过的输入 | PSI 入口实际产出 |
| --- | --- | --- |
| `Missing is-check operand` | `x is` | `TYPE_OP(IS, <implicit>)`，无错误节点 |
| `Missing subscript receiver` | `[0]` | 解析为数组字面量 `ARRAY_LITERAL` |
| `Missing primitive type conversion argument` | `a as` | `TYPE_OP(AS, <implicit>)`，无错误节点 |
| `Missing constant pattern expression` | `case => 1` | 折叠为 `BRANCH(_)` |
| `Missing let-pattern initializer` | `if (let Some(v))` | 正常生成 `CfirLetPatternExpression` |
| `Missing performed expression` | `perform` | 正常生成 `CfirPerformExpression` |
| `Missing synchronized mutex expression` | `synchronized { }` | 块被当成 monitor 实参，块体同时出现在 monitor 与 body |
| `Missing unsafe block` | `unsafe` | 生成空 `UNSAFE { }` |
| `Missing annotated expression` | `@Deprecated`（单独一行） | 注解被丢弃，函数体为空 |
| `Missing IfAvailable then/else branch` | `@IfAvailable(level: "20")` | 整个函数体变成 `CfirIfAvailableExpression` |

（`@IfAvailable` 的合法形态本身已由 `cangjie-features/ifAvailableExpression.cj` 覆盖：官方 `ParserImpl::ParseIfAvailable`
的 `@IfAvailable(name: literal, { lambda }, { lambda })` 三实参语法，cjc 1.0.5 能解析并给出环境相关的 APILevel 诊断。）

## 已知 PSI / LightTree 差异

本批次新增 `declarations/class-like/constructorDelegation.cj` 后 `TreesCfirCompareTest` 暴露了两处真实分叉，均已修掉，
现在 PSI 与 LightTree 两条路径在全部 rawBuilder 用例上渲染一致：

- `super(1)` / `this()` 委托：LightTree 的 `resolveCalleeReference` 原先把 `THIS_EXPRESSION` / `SUPER_EXPRESSION` 落到
  `else` 分支，产出 `*operator_invoke` + receiver 形状；PSI 路径产出无 receiver 的具名引用（`this` / `super`）。
  已在 LightTree 侧补两个分支。
- `declarations/file-structure/conditionalCompilation.cj` 曾在对比里抛 `CfirAnnotationMetadataRegistry is frozen`：
  `doCompareTest` 让两条路径共用一个 session，PSI 路径的宏 identity service 先 freeze 了注解元数据注册表。
  `doCompareTest` 改为两条路径各用一个 session。

### 词法 / 语法批次新增用例暴露的 PSI / LightTree 分叉（2026-10-01）

`TreesCfirCompareTest`（`light-tree2cfir:test --rerun`）对本批新增用例报 7 个文件分叉。它们不是测试数据问题：既有 golden（PSI 路径）
与 `AbstractRawCfirBuilder` 共用实现给出的都是左边（PSI）那一侧的文本，分叉点全部在两套 builder 的错误节点 / 恢复形态上：

| 文件 | PSI（= golden） | LightTree | 性质 |
| --- | --- | --- | --- |
| `lexical/stringInterpolation.cj` | `ERROR_EXPR(Unsupported expression: CjPatternVariable)` | `ERROR_EXPR(Unsupported expression: VARIABLE)` | `${let y = x + 1; y * 2}` 里块表达式在两条路径被解析成不同 PSI 种类（pattern variable vs 变量） |
| `syntax/recovery/missingNameInFunc.cj` | `func <no name provided>()` | `func <anonymous>()` | 匿名函数名的占位串两条路径不同 |
| `syntax/recovery/operatorNotOverloadable.cj` | `operator func <no name provided>` | `operator func <anonymous>` | 同上（3 处） |
| `syntax/recovery/enumCtorWithoutPipe.cj` | `init(R|<ERROR:No type for parameter>|)` | `init(<error>: R|<ERROR:No type for parameter>|)` | 缺名形参：PSI 传空名，light tree 传 `<error>` |
| `syntax/recovery/synchronizedWithoutLock.cj` | `monitor:`（空） | `monitor: ERROR_EXPR(Missing synchronized mutex expression)` | light tree 走 `buildErrorExpression` 分支，PSI 直接放空 |
| `syntax/recovery/defaultParamOnUnnamed.cj` | `func f(a: Int64, b: Int64 = INT(2)` | `func f(a: Int64 = INT(1), b: Int64 = INT(2)` | `func f(a: Int64 = 1, b: Int64 = 2)` 的解析恢复形态不同（默认值被挂到第一个形参上） |
| `syntax/recovery/functionTypeParamDefault.cj` | `func f(g: R|x|): <implicit>` | `func f(g: R|x| = INT(1)` | 同上，类型形参列表里的默认值 |

`syntax/declarations/functionParams.cj`（`x!:` / `x!: T = v`）、`classBodyForms.cj`、`structForms.cj` 里的合法具名形参在两条路径渲染一致，
所以这些是恢复形态与占位串的差异，不是 light tree 缺具名形参支持。修法：占位串统一到 `AbstractRawCfirBuilder`（`"No type for parameter"` 旁边），
恢复形态统一到两条 builder 的形参表解析。

第二批新增用例又暴露 3 处分叉：`syntax/expressions/lambdaTypedForms.cj`（无名 lambda 参数：PSI 用安全名辅助函数得到 `<no name provided>`，light tree 写死 `<error>`）、
`syntax/recovery/funcMainKeyword.cj`（`func main` 被解析成 main 声明但无 `func` 关键字的函数：同样是 `<no name provided>` vs `<anonymous>`）、`syntax/recovery/arrayPattern.cj`
（`case [1, ..rest]`：PSI 解析成 CjSliceExpression → 整体 unsupported，light tree 解析成 RANGE 节点并补 `Missing range end` 错误节点）。累计 10 个文件分叉，全部是占位串与恢复形态差异，测试输入未改。

## 官方编译器取证（2026-10-01 批次）

本批次新增的非恢复用例全部用官方 cjc 1.0.5 逐文件编译验证：`recovery/` 下两个用例按设计就是残缺语法（cjc 报解析错误），
其余用例 cjc 零错误（个别文件只有 `unused` 警告，探针目录不引用故无害）。
本机 cjc 1.0.5 / 1.1.3 不支持的语法（`constructor` 关键字、`annotation` 类声明、`@Java` / `@APILevel` / `@ForeignName`、
`try ... handle`、`label@` 循环、`as!`、前缀 `++x`、枚举构造器带体、`where` 约束的类型别名、`extend<T> Interface<T>`、接口成员上的 `public`、`synchronized(Lock())`（`Lock` 是接口不可实例化））未写成用例。两个 PSI 缺口记录在案：`CjCodeFragment` 系列（`{| ... |}`）在本仓库 parser 里只在草稿文件（`CjFile.isCodeFragment`）入口生效，cjc 1.0.5 在普通 `.cj` 顶层也直接解析失败，本批次不为其建用例；`@IfAvailable` 的部分恢复形态同样达不到错误节点（见上一节表格）；合法形态另见 `cangjie-features/ifAvailableExpression.cj`。

## 词法 / 语法 / 大文件批次（2026-10-01）

新增目录：`lexical/`（12 个词法用例）、`syntax/declarations/`（19）、`syntax/expressions/`（14）、`syntax/types/`（4）、
`syntax/recovery/`（45 个恢复用例）、`large/`（9 个大文件场景）。所有用例都以官方 cjc 1.0.5 与 1.1.3 逐文件编译取证：
非 recovery 用例两版均零 error（只可能有 unused 警告），recovery 用例两版均产生官方诊断，诊断名写在文件头部注释里。
`syntax/declarations/macroCallForms.cj` 是唯一例外：它把宏定义与宏调用放在同一文件以便覆盖语法，cjc 报
`macro_unexpect_def_and_call_in_same_pkg`，属官方语义限制（宏定义与调用不能同包）。

### 本批取证到的官方语法规则（1.0.5 / 1.1.3 一致）

| 规则 | 官方依据 | 相关用例 |
| --- | --- | --- |
| 具名形参的标记是 `x!:`，且只出现在函数 / 构造器声明的形参表里；函数类型、元组类型的形参名写 `x:`，写 `x!:` 报 unclosed delimiter | `ParseDecl.cpp` ParseParameter / ParseTypeParameterInTupleType | `syntax/types/functionTypeRefs.cj`、`syntax/recovery/functionTypeNamedParamBang.cj` |
| 函数类型 / 元组类型的形参表要么全具名要么全无名，且只能"具名在前、无名在后" | `ParseType.cpp` ParseTypeWithParen（parse_all_parameters_must_be_named） | `syntax/recovery/functionTypeMixedParamNames.cj` |
| 调用函数值不能用具名实参 | `Sema/TypeCheckCall.cpp` CheckCallArgs（sema_unsupport_named_argument） | `syntax/recovery/namedArgumentInFuncValueCall.cj` |
| 多行字符串三个引号后必须紧跟真实换行；多行原始字符串无此要求，空的原始字符串（`##`）合法 | `Lexer.cpp` ScanMultiLineString / ScanMultiLineRawString | `lexical/multilineStrings.cj`、`lexical/rawStrings.cj`、`syntax/recovery/emptyMultilineString.cj` |
| `Array<T>(x)` 单实参构造在两版都报 sema_wrong_number_of_arguments；可用形态是 `Array<T>()`、`Array<T>(size, repeat: v)`、`Array<T>(size, { i => v })`、数组字面量 | `Sema/TypeCheckBuiltinExpr.cpp` ChkArrayExpr / ChkSingeArgArrayExpr | `syntax/expressions/constructionForms.cj`、`syntax/recovery/arraySingleArgConstruction.cj` |
| `VArray<T, $n>(x)` 恰好一个实参：无名 lambda 或 `repeat: v`；类型实参只能是整数字面量，元素类型不能是数组 | `Sema/TypeCheckBuiltinExpr.cpp` ChkVArrayArg | `syntax/recovery/varrayConstructorArgForm.cj`、`varrayConstructorArgName.cj`、`varrayRefTypeArg.cj`、`varrayNestedArray.cj` |
| 泛型枚举的构造器不能裸调，必须 `Enum<T>.Ctor` | sema_generic_type_without_type_argument | `syntax/recovery/varrayRefTypeArg.cj`、`syntax/expressions/constructionForms.cj` |
| `where` 写在返回类型之后、约束用 `T <: Bound`，且只对有类型形参的声明合法 | `ParseDecl.cpp` ParseFuncGenericConstraints / ParseGenericUpperBound | `syntax/declarations/whereConstraints.cj`、`syntax/recovery/whereWithoutTypeParams.cj` |
| `if` 作为表达式必须用花括号块体，不能直接作为 `+` 的右操作数 | `ParseExpr.cpp` ParseIfExpr | `syntax/expressions/stringTemplateForms.cj` |
| `~` 不是前缀操作符；可重载一元操作符只有 `-` 与 `!` | `ParseExpr.cpp` 前缀表达式解析、操作符重载表 | `syntax/recovery/bitwiseNotPrefix.cj` |
| 无类型的二维数组字面量被推断为展平的 `Array<Int64>`；写显式 `Array<Array<Int64>>` 可通过 | sema_invalid_subscript_expr | `syntax/recovery/flatNestedArrayLiteral.cj` |
| `Int64` 移位量 ≥ 64 报 chir_shift_length_overflow（ERROR） | `CHIR/Analysis/ConstAnalysis.h` | `large/longOperatorChain.cj` |
| 一个 `match` 的分支只能匹配同一枚举 | sema_pattern_not_match | `large/manyMatchCases.cj` |
| 同一 enum 不能有同名构造器（带参报 sema_duplicated_item_in_enum，无参按声明重复定义报 sema_redefinition） | `DiagnosticSema.def` | `syntax/recovery/enumDuplicateConstructor.cj` |

### 官方编译器在本机的环境限制（不是语法问题）

- 静态库目标下，任何涉及二维数组下标链（`a[0][0]`，无论数组字面量还是变量）都会让 LLVM `opt --cangjie-pipeline` 崩溃
  （exit 3221225501）。三层嵌套字面量 `[[[...]]]` 的类型检查与代码生成正常，因此 `large/wideArrayLiterals.cj` 用它覆盖多层下标。
- 中文原始标识符（`` `仓颉` ``）放在类型位置（返回值类型）时 1.0.5 报 undeclared type name，故 `lexical/rawIdentifiers.cj` 只在值位置使用。
- 1.1.3 的 `quote($(x))` 展开语义改为调用 `transformTokens`，会报 undeclared identifier；两版都接受的花括号块体写法 `quote({ $(x) })` 见 `syntax/declarations/macroDeclVariants.cj`。

### golden 生成与第二轮结果（2026-10-01）

- `-Dupdate.test.data=true` 首轮生成 103 个新 golden；确认轮 `:cfir:raw-cfir:psi2cfir:test --tests '…RawCfirBuilder*' --rerun` 2290 个用例中 9 个失败，
  全部是 `RawCfirBuilderLazyBodiesByStubTestGenerated` 里上一批的 3 个 fixture（`cangjie-features/extend/extendWithWhereOfficial.cj`、
  `extendGenericWhereChain.cj`、`cangjie-features/match/matchExpressionOfficial.cj`）各重复 3 次：stub 路径渲染枚举时把构造器
  渲染成 `init(T: R|<ERROR:No type for parameter>|)`，而 AST 路径与既有 golden 是 `Year(R|Int32|)`。本批次没有触碰这 3 个 fixture，
  也不按 stub 输出改 golden；这是 stub 侧枚举构造器渲染的 CFIR 缺口（`AbstractRawCfirBuilderLazyBodiesByStubTest`）。

### 第二批（续）：main / 资源 / quote / CFunc / This / 区间步长 / 静态成员（2026-10-01）

在第一批（同日）之后又按官方源码与 cjc 1.0.5 / 1.1.3 取证补了 7 个词法、7 个声明、12 个表达式与 35 个恢复用例，主要规则：

| 规则 | 官方依据 | 相关用例 |
| --- | --- | --- |
| `main` 是独立声明（不写 `func`），形参只能是 `Array<String>`，不能有修饰符或多个形参列表 | `ParseDecl.cpp` ParseMain / DiagIllegalFunc | `declarations/top-level/main*.cj`、`recovery/funcMainKeyword.cj`、`mainWithModifier.cj`、`mainBadParameterType.cj`、`mainCurriedParameterList.cj` |
| 区间步长写作 `a..b:step`（不是 `step()` 函数，也不是 `a..b..step`） | `ParseExpr.cpp` ParseRangeExpr（COLON 后接表达式） | `expressions/rangeStepForms.cj`、`recovery/stepFunctionCall.cj`、`rangeChainedOperators.cj`、`rangeMissingOperands.cj`、`rangeStringOperands.cj` |
| `This` 只能作类成员函数或 extend 块成员函数的返回类型 | `ParseType.cpp` parse_this_type_not_allow | `expressions/thisTypeReturns.cj`、`recovery/thisTypeAsParameter.cj`、`thisTypeInStruct.cj` |
| `try (r = expr)` 的资源类型必须实现 `Resource`（`isClosed` + `close`），带资源的 try 类型固定为 Unit | `Sema/TypeCheckExpr/TryExpr.cpp` SynTryWithResourcesExpr | `expressions/tryResourceForms.cj`、`recovery/tryResourceWithoutResourceInterface.cj`、`tryResourceAsValue.cj`、`tryResourceExpressionOnly.cj`、`tryResourceMissingBlock.cj` |
| CFunc 形参必须是 C 类型（`CType` / `CPointer` / `CString` / C 数值），CFunc 形参不能具名 | `Sema/FFI/CFFICheck.cpp` CheckCFuncParam / CheckCFuncParamType | `declarations/cInteropForms.cj`、`recovery/cfuncNamedParam.cj`、`foreignNamedParam.cj`、`cfuncUnitType.cj`、`cfuncValueType.cj`、`foreignCArrayType.cj` |
| 同步用 `std.sync` 的互斥量：`synchronized(lock) { }`（必须有实参）、`lock()` / `unlock()` | Cangjie 文档《同步》 | `expressions/synchronizedForms.cj` |
| 泛型 extend 的类型形参必须出现在被扩展类型里；内置类型不能重载内建 operator | `sema_extend_generic_must_be_used` / `sema_operator_overload_built_in_binary_operator` | `declarations/extendBuiltinForms.cj`、`recovery/extendGenericUnused.cj`、`extendBuiltinOperator.cj` |
| const 初值必须是常量表达式（数组字面量不是） | `sema_expect_const` | `declarations/constExpressions.cj`、`recovery/constArrayInitializer.cj` |
| 宏体的 quote 是 token 序列：字面量 / 标识符 / 复合表达式 / 声明 / `Ident` `Int64` `Semi` `Newline` `Paren` `Str` `Square` `TypeNode` `Dollar` `At` 构造 / `parseIdent` | `ParseQuote.cpp` / `ParseAtom.cpp` ParseQuoteExpr | `declarations/macroQuoteForms.cj`、`recovery/quoteDollarIdentifier.cj`、`quoteOutsideMacroPackage.cj` |
| let 模式用 `<-`（`if (let v <- o)` / `while (let v <- o)`），for 里不支持 let 模式 | `ParseExpr.cpp` ParseLetPattern | `expressions/letPatternForms.cj`、`recovery/forLetPattern.cj` |
| 函数类型不能省略参数列表括号（`Int64 -> Unit` 是 parse 错误） | `ParseType.cpp` DiagParseExpectedParenthis | `recovery/bareArrowType.cj` |
| 枚举：构造器参数列表不能为空、不能以 `|` 结尾；继承接口要实现成员 | `parse_expected_type` / `parse_expected_name` / `sema_need_member_implementation` | `recovery/enumCtorParenEmpty.cj`、`enumTrailingPipe.cj`、`enumInterfaceUnimplemented.cj` |
| 构造器形参必须写类型；构造器之间只能 `this(...)` / `super(...)` 委托，不能 `this.init()` | `ParseDecl.cpp` ParseParameter / `parse_expected_name` | `declarations/constructorForms.cj`、`recovery/constructorMemberParams.cj`、`initCallingInit.cj` |
| 非 `mut` 的 prop 不能有 setter | `sema_immutable_property_with_setter` | `declarations/staticMemberForms.cj`、`recovery/immutablePropSetter.cj` |
| 静态成员：类 / 接口 / 枚举上的 `static const` / `static var` / `static mut prop` / `static prop` | Cangjie 文档《属性》 | `declarations/staticMemberForms.cj` |
| import：`*`、选择式、别名 | `Parse/ParseImports.cpp` | `lexical/importForms.cj` |
| 文档注释与嵌套块注释 | `Lexer.cpp` ScanDocComment / ScanBlockComment | `lexical/commentForms.cj` |
| 十六进制浮点指数带符号、整数字面量最大值与后缀组合 | `Lexer.cpp` 数字扫描 | `lexical/numericEdges.cj` |

目录规模（本批结束后）：`lexical/` 17、`syntax/declarations/` 26、`syntax/expressions/` 26、`syntax/types/` 4、`syntax/recovery/` 80、`large/` 9。

### 占位串与恢复形态统一（2026-10-01）

`TreesCfirCompareTest` 最初报 10 个文件分叉，分三类，逐类统一：

| 类别 | 处理 | 源码位置 |
| --- | --- | --- |
| 缺名占位串（`<no name provided>` vs `<anonymous>` / `<error>`） | LightTree 侧改用同一个常量 `SpecialNames.NO_NAME_PROVIDED` | `LightTreeRawCfirDeclarationBuilder.extractFunctionName` / `convertValueParameter` |
| 错误节点命名（`Unsupported expression: CjPatternVariable` vs `VARIABLE`；`CjSliceExpression` 未被识别） | PSI 侧改用 PSI 节点类型名（`psi.node.elementType`，与 LT 的 `node.tokenType` 同形），并补 `convertSlice(CjSliceExpression)`：切片统一承接为 `CfirRangeExpression`，缺的一端是 error expression | `PsiRawCfirBuilder.convertExpression` / `convertSlice`；`LightTreeRawCfirExpressionBuilder.convertSlice`（按裸 `RANGE` / `RANGEEQ` token 切分前后） |
| 恢复形态（无名形参上的默认值、前缀切片的方向、`synchronized` 缺 monitor） | 形参默认值以"形参节点内有 `EQ` 才采信"统一（官方 `ParseAssignInParam` 在无名形参上报 `parse_expected_dot_lparen` 后仍解析 `=` 后的表达式，但 PSI 的 `CjParameter.equalsToken` 只看直接子节点，恢复出来的 `=` 落在 `ERROR_ELEMENT` 里）；`synchronized` 缺 monitor 两边都补 `ERROR_EXPR(Missing synchronized mutex expression)` | `LightTreeRawCfirDeclarationBuilder.convertValueParameter`；`PsiRawCfirBuilder.convertSynchronized`（已是该形状） |

统一后 `lexical/stringInterpolation.cj`、`syntax/recovery/{arrayPattern,defaultParamOnUnnamed,functionTypeParamDefault,synchronizedWithoutLock}.cj` 的 golden 按 PSI 路径重新生成（PSI 路径的输出是这两条 raw builder 共同的目标形状）。

两处顺带发现并修掉的 CFIR 缺陷：`CfirRangeExpressionBuilder.isInclusive` 是 `lateinit var … by Delegates.notNull<Boolean>()`，LT 的 `convertSlice` 在裸 `RANGE` token 分支上没赋值，`build()` 时抛 `Property isInclusive should be initialized before get`；`CjBlockExpression` 本身实现 `CjExpression`，`CjSynchronizedExpression.expression`（`findChildByClass(CjExpression)`）在缺 `(` 时把块当成 monitor，PSI 侧改为在直接子节点里取第一个非块的表达式作 monitor。

### 第二批（续）：剩余未覆盖形态（2026-10-01）

按官方 `Tokens.inc` 词法种类、`Parse*` 入口与本仓库两批旧用例逐项对表后补了 21 个用例 + 24 个恢复用例，目录规模：
`lexical/` 18、`syntax/declarations/` 30、`syntax/expressions/` 36、`syntax/types/` 5、`syntax/recovery/` 105、`large/` 9（矩阵覆盖 332 个 .cj）。

新增并取证的官方规则：

| 规则 | 官方依据 | 相关用例 |
| --- | --- | --- |
| 幂运算 `**` 右操作数固定 `UInt64`、不可链式（`a ** b ** c`、`a ** (b ** c)` 报 sema_invalid_binary_expr）；`operator func **` 可重载 | `Sema/TypeCheckUtil.cpp` BUILTIN_OPERATORS | `expressions/powerOperatorForms.cj`、`recovery/powerOperatorChain.cj`、`powerOperatorRightOperand.cj` |
| 下标 set 形如 `operator func [](i: T, value!: T)`；赋值 / 复合赋值 / 自增自减都落到 set；无名形参在调用处报 sema_unknown_named_argument | C++ 互操作下标 | `expressions/subscriptOperatorForms.cj`、`recovery/subscriptSetUnnamedParameter.cj` |
| `redef` 只能修饰 static 成员（函数 / prop），顺序 `public redef static`；实例成员用 `override` 且基类成员必须 `open`（否则 sema_cannot_override） | `DiagnosticSema.def` sema_cannot_override / sema_redef_modify_static_func | `declarations/redefMemberForms.cj`、`recovery/redefInstanceMember.cj` |
| extend 可多次、可实现接口、可定义 operator / prop；泛型结构 extend 的方法可再带泛型形参 | 《扩展概述》 | `declarations/extendStructForms.cj` |
| 私有构造器只能在类内调用（类外报 sema_no_match_constructor）；构造器之间 `this(...)` 委托 | 《构造函数》 | `declarations/memberInitForms.cj`、`recovery/privateInitCalledOutside.cj` |
| 类型别名可用在签名位置（标量 / 函数类型 / `?M` 容器） | 《类型别名》 | `declarations/typeAliasSignatureForms.cj` |
| 集合类型引用需 `import std.collection.*`（`HashMap` / `HashSet` / `ArrayList` / `LinkedList` / `Iterator` / `Iterable` / `Option`）；不 import 报 sema_undeclared_type_name | 《集合》 | `types/collectionTypeRefs.cj`、`recovery/collectionTypeNoImport.cj` |
| `J"..."` 是官方 J 字符串字面量（`Lexer.cpp` 只认大写 `J` + `"`，`ParseAtom.cpp` StringKind::JSTRING），但 1.0.5 / 1.1.3 的 SDK 无 `JString` 类型（无 std.json 包），小写 `j` 前缀是解析错误 | `Lexer.cpp ScanStringOrJString` / `ParseAtom.cpp` | `recovery/jstringUpperCase.cj`、`jstringLowerCase.cj` |
| 整数字面量越界（十六进制 / 十进制 / 带后缀）报 sema_exceed_num_value_range | Sema 常量求值 | `lexical/integerLiteralForms.cj`、`recovery/hexOverflow.cj`、`integerOverflow.cj` |
| `inout` 不是普通函数形参类型（只出现在 C 互操作 / 宏形参与调用实参） | `ParseDecl.cpp` ParseParameter | `recovery/inoutParameter.cj` |
| 没有 `with` 表达式 | `Tokens.inc` WITH 词元无表达式入口 | `recovery/withExpression.cj` |
| 泛型形参无 `in` / `out` 变型标注 | `ParseType.cpp` ParseGenericParamDecl | `recovery/genericVarianceMarker.cj` |
| lambda 形参不带默认值、不带 `!` | `ParseExpr.cpp` ParseLambdaExpr | `recovery/lambdaDefaultValue.cj`、`lambdaNamedParameter.cj` |
| 枚举体不接受 `...` 非穷尽写法 | `ParseDecl.cpp` ParseEnumBody | `recovery/enumEllipsisNotSupported.cj` |
| 接口体不接受 `init`；枚举体不接受 `static init` | `ParseDecl.cpp` ParseInterfaceBody / ParseEnumBody | `recovery/interfaceInit.cj`、`staticInitInEnum.cj` |
| `&&` / `||` 不可重载（一元 `!` 可重载） | `Sema/TypeCheckUtil.cpp` | `recovery/logicalOperatorOverload.cj` |
| `Result` / `Ok` / `Map` / `CChar` 在 1.0.5 / 1.1.3 的 std 中不存在 | SDK 模块清单 | `recovery/resultTypeUndeclared.cj`、`collectionTypeNoImport.cj`、`ccharTypeUndeclared.cj` |
| VArray 不能切片出 `Array`（`a[0..2]` 结果不能按 Array 下标 / 迭代）；VArray 第二个类型实参必须 `$​` 整数字面量 | `parse_varray_type_args_mismatch` | `recovery/varraySliceNotSupported.cj`、`varrayTypeArgumentWithoutDollar.cj` |
| `main` 形参不可变参 `Array<String>...`；`@IfAvailable` 只有圆括号形式 | `ParseDecl.cpp` ParseMainDecl | `recovery/mainVariadicArgs.cj`、`ifAvailableBracketForm.cj` |
| do-while 循环体必须带花括号 | `ParseExpr.cpp` ParseDoWhileExpr | `recovery/doWhileWithoutBrace.cj` |
| 其它补齐：if 表达式作实参 / if-let-else-if / while 条件 let 模式 / for 元组解构 / 多级成员访问 / 类静态成员访问 / 枚举静态工厂 / 嵌套元组与 Option 数组类型 / `CType` 形参 / 异常层次与 `throw` 表达式 / `Nothing` / 位运算与全套复合赋值 / 嵌套 lambda 与尾随闭包 | 对应 `Parse*` 入口 | `expressions/*` |

