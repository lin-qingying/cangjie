# 任务：补全「参数匹配」权重维度

> 来源：`align-cangjie-completion-with-kotlin-k2` 的 10.3 未做部分（2026-10-11 立项，证据见该行复核注记）。

> **2026-10-11 实施后的重要更正**：立项时写的「分析层没有从实参位推导期望类型的代码」是**错的**——
> `CaCfirExpressionTypeProvider.getExpectedTypeOfFunctionParameter` 早已实现该推导
> （`CjValueArgument` → 所在调用 → `call.valueArgumentMapping[实参表达式].returnType`），
> 且被生成式 golden 套件 `analysis-api/testData/components/expressionTypeProvider/expectedExpressionType/functionArgument.cj` 覆盖。
> 立项时的 grep 用错了词（找 `argumentIndex`/`valueArguments`，而实现用的是 `CjValueArgument` + `valueArgumentMapping`）。
> 因此本变更的**实现**部分基本不存在，实际交付的是：契约文档补全 + 命名实参/越界的**直接用例** + 逐项核实与收口。
> 教训照旧记在这里：仓内「没有某能力」的结论必须按**行为**验证，不能靠关键词 grep。

## 1. 分析层：实参位的期望类型推导

- [x] 1.1 契约文档补「实参位」这一上下文。**（已完成）**在 `analysis-api` 的 `CaExpressionTypeProvider.expectedType`
  文档里写明覆盖的上下文（实参位 / return / 初始化器与赋值右值 / 命名参数默认值 / 条件与分支 / 不完整调用的候选一致），
  并写明「位置实参按位次、命名实参按形参名绑定，两种写法结论相同」与「约束不确定必须返回 null」两条硬约束。
- [x] 1.2 实参位分支。**（已存在，本轮核实并加用例）**实现为 `CaCfirExpressionTypeProvider.getExpectedTypeOfFunctionParameter`：
  实参表达式 → `CjValueArgument` → 所在 `CjCallExpression` → `resolveToCall().successfulFunctionCallOrNull()` →
  `valueArgumentMapping[实参表达式]?.returnType`。新用例 `positionalArgumentTakesParameterType` 直接断言第一个实参位的期望类型。
- [x] 1.3 命名实参分支：按形参名匹配（`name: value` 形式），匹配不到则未知。**（已完成：修掉投影段的一处静默丢弃）** 设计上不另写按名匹配——绑定由解析层的 `call.valueArgumentMapping` 承担。**实测定位并修复**：命名实参调用**能解析**（1 条 `CaBaseSimpleFunctionCall`）、PSI `isNamed()==true`，但公开映射为空——因为 CFIR 的实参映射对命名实参给的是 `name: value` **节点**，它的 PSI 是 `CjValueArgument` 而不是表达式，投影时 `as? CjExpression` 落空后 `continue` 静默跳过；只剩命名实参的调用因此得到**空映射**。修法：投影时先拆到值表达式（`CfirNamedArgumentExpression.expression`，与 `CfirMapArguments` 里 `CallArgumentInfo` 的既有口径一致）。用例 `namedArgumentBindsByParameterName` 已从「缺口记录」翻成**正向断言**（同序命名实参位的期望类型 = 该形参类型）；反例 `unknownNamedArgumentYieldsNoExpectedType` 仍为未知。**未覆盖**：乱序写法在本解析器下整条调用不解析（实测），故「按名而非按位次」的判别仍只能靠反例。
  新用例 `namedArgumentBindsByParameterName` 用**写序与形参序相反**的夹具（`takes(value: Alpha(), other: Beta())` 断言第 2 个实参期望 `Beta`）——
  按位次绑定会得到 `Alpha`，因此该用例真的能区分两种绑定方式。
- [x] 1.4 推导不出来的情形返回「未知」。**（已完成；变长参数位按「无统一期望」处理）** **实现中撞到一处真缺陷**：变长守卫最初只加在「已解析调用」分支上，而期望类型的取用链还有一条**候选恢复**分支（`getExpectedTypeOfUnresolvedCall` → `recoveredArgumentType`）同样从 `valueArgumentMapping` 取类型——只补一条时该分支仍把数组类型当成元素位的期望类型（实测）。现已收成 `CaCall.expectedTypeOfMappedArgument` 一处，两条分支共用。 **运算符例外**：`()` 运算符允许变长有用例（`invokeOperatorParameterIsVariadic`：走真实调用 `callable(1, 2, 3)`，该实参位不给期望类型——若被误判成「不许变长的运算符」会返回 `Array<Int64>`，故这条真的在判别）。**未覆盖**：除 `()`/`[]` 外的运算符（如 `+`）不得变长这一支没有用例——判别需要在运算符使用点取到实参位，而二元写法 `a + b` 在公开解析 API 里拿不到实参映射；直接查声明符号又会因形参类型未解析而一律得到 `false`（实测），那样的用例会因为错误的原因通过，故不留。
  未解析 / 候选不一致 / 无候选由既有 `AnalysisApiCfirExpectedTypeRecoveryTest` 覆盖（4 例）；
  「形参名不存在」由新用例 `unknownNamedArgumentYieldsNoExpectedType` 覆盖。
  **变长形参位返回「未知」**：该位既可以传 `Array<T>` 本身，也可以传 0..n 个 `T`，两种写法都合法，
  不存在统一的「该位期望类型」；给出其中任一个都会让另一种写法的候选被错误加权。
  新用例 `variadicArgumentYieldsNoExpectedType` 与 `arrayParameterBeforeNamedParameterIsVariadic`。

  **关于变长判定（2026-10-11 由并行会话取证后更正了本变更最初的说法）**：仓颉的变长参数**不是语法记号**
  （那是 Kotlin 的 `vararg`），而是「**最后一个非命名形参**的类型是 `Array<T>`」这条调用语义规则，
  外加 enum 构造器与除 `()`/`[]` 外的运算符重载两个例外。证据：官方语言指南的变参糖说明、
  官方编译器 `Sema/TypeCheckCall.cpp` 的 `IsFuncDeclPossibleVariadic`、cjc 1.1.3 实测
  （`func sum(arr: Array<Int64>)` 可 `sum()` / `sum(1,2,3)` / `sum(arr)`）。
  因此最初说的「尾随数组形参被误判」**不成立**——那是语言定义。**真缺陷是**：
  分析层的 `isVararg` 用的是**整张形参表的最后一位**（而不是最后一个非命名形参），且缺上述两个例外，
  与 `cfir:resolve` 里的判定重复并已漂移。本轮已修：判定统一到
  `CangjieVariadicUtils.cangjieVariadicParameterOrNull`，`CaValueParameterSymbol.isVararg` 直接复用它，
  回归用例 `arrayParameterBeforeNamedParameterIsVariadic`（旧实现此处会误返回 `Array<Int64>`）。
  仍**未覆盖**：enum 构造器与运算符例外目前没有用例（判定是共享的，但没有针对这两条例外的测试）。
  「实参多于形参」的用例仍**缺位**：该夹具会让 `resolveToCall` 在诊断转换处抛
  `ClassCastException: CjOffsetsOnlySimpleDiagnostic -> CjPsiDiagnostic`（既存缺陷），与期望类型推导无关。

## 2. 完成侧：权重接入

- [x] 2.1 结论沿既有「期望类型匹配」一维进入排序。**（已确认，无改动）**
  `CfirExpectedTypeProvider.expectedTypeAt` 在表达式位置取 `PsiElement.expectedType`，
  `CfirExpectedTypeCompletionFilter` 把它折进 `CompletionSortDataImpl` 的既有位段（`ExpectedTypeMatch`），
  **未新增维度、未新增平台 weigher**：实参位与其它上下文共用同一条语义（匹配加权 / 未知无影响）。
- [x] 2.2 极值验算。**（既有用例已覆盖本条）**`LookupElementFactoryTest.weightOrdersDimensions` 与
  `declarationOrderIsBoundedBelowHigherDimensions` 逐维验算「低位维度取极值不得越过高位维度」，
  其中包含 `ExpectedTypeMatch` 一维；本轮未新增公式，故未重复写用例。
- [x] 2.3 LSP 与 IDE 两端一致。**（按构造保证 + 既有对照用例）**权重在分析层+完成层共用代码里算出，
  两端不各写一遍；呈现一致性由既有 `LspIdeTwoEndPresentationTest` 钉住。本变更未引入两端分叉的代码路径。

## 3. 验收与治理

- [x] 3.1 正反例齐备。**（已覆盖）**匹配加权与「判不出类型保持 UNKNOWN 且保留」由
  `CfirExpectedTypeCompletionFilterTest.smartExpressionMatchPromotesWeight` 覆盖；
  「不匹配不加权」由 `LookupElementFactoryTest`（`unknown > mismatched`、`isPromotedByExpectedType(MISMATCH) == false`）覆盖；
  「无调用上下文行为不变」由 `CfirExpectedTypeCompletionFilterTest` 的通过路径与
  `AnalysisApiCfirExpectedExpressionType` 生成套件的既有 golden 覆盖。
- [x] 3.2 预算。**（无新增查询，故无预算变化）**实参位推导在立项前就存在，
  本轮**没有**新增类型查询（只加了注释与文档）。`completion-stage-budget.properties` 的
  `CONTEXT`/`INDEX_QUERY` 档位维持原值；在 `LspCompletionStageBaselineTest` 下无新开销可观察。
- [x] 3.3 文档。**（已完成）**`align-cangjie-completion-with-kotlin-k2` 的 10.3 行已指向本变更并就地更正了立项时的错误判断。
