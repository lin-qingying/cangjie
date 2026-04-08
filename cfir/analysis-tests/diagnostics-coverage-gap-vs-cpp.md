# CFIR diagnostics 测试覆盖缺口（更新于 2026-04-06）

## 结论摘要

当前 `cfir/analysis-tests/testData/diagnostics` 已经不是“基础语义几乎没测”的状态。
前一轮补齐后，下列语义已经有了比较明确的回归保护：

- `generic-access/*`：裸泛型类型名、上界成员/方法访问失败
- `initialization/*`：局部变量先用后初始化、类字段未初始化
- `constructor/*`：构造器 delegation 位置、递归、显式 `super` 要求、构造器歧义
- `super/*`：`struct` / `enum` / `interface` 中非法 `super`
- `coverage/inheritance/*`：`class not open`、不可见 override、返回类型不匹配
- `pattern/*` 与 `coverage/match/*`：基础 pattern 合法性 + 穷尽性
- `mut/*`：immutable 函数修改字段、调用 `mut` 成员
- `visibility/*`：`private/protected/internal` 访问矩阵

现在剩下的缺口，主要不再是“这些基础负例完全没有”，而是四类问题：

1. `checker` 框架里已经有诊断名，但 `testData/diagnostics` 还没有直接断言到。
2. 某些 checker 已经实现，但没有接入 `Common*Checkers` 注册链路。
3. 一些目录已经有 smoke test，但语义面仍然偏薄，只覆盖了主路径，没覆盖边角分支。
4. 官方 C++ 里稳定存在的语义域，当前 CFIR 仍未形成对应 producer 或测试目录。

---

## 对照依据

本次结论基于以下仓库内证据：

- 测试数据：`cfir/analysis-tests/testData/diagnostics/**/*`
- 诊断定义：`cfir/checkers/gen/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrors.kt`
- 声明/表达式 checker 注册：
  - `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CommonDeclarationCheckers.kt`
  - `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CommonExpressionCheckers.kt`
  - `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CommonTypeCheckers.kt`
- 默认 session 注册入口：
  - `cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/checkers/CheckersContainers.kt`
  - `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/frontend/CfirFrontendFacade.kt`
- 官方语义基线：
  - `external/cangjie_compiler/include/cangjie/Basic/DiagnosticSema.def`
  - `external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticSema.def`

---

## 一、各种检查视角：总体覆盖现状与全局缺口

### 1. 当前目录分布明显不均衡

截至本次盘点，`testData/diagnostics` 下共有 `135` 个 `.cj` 文件。
其中最集中的目录是：

- `coverage/*`：42
- `operator/*`：26
- `type-mismatch/*`：17

而明显偏薄的目录是：

- `call/*`：1
- `constructor/*`：2
- `pattern/*`：1
- `mut/*`：1
- `generic-access/*`：2
- `interop/*`：1
- `effects/*`：2
- `coverage/invalid/*`：0

这说明当前 suite 的重心仍然偏在“基础解析 + operator + extend + 基础 type mismatch”上，真正偏薄的是：

- 调用绑定与歧义
- 声明状态分支
- effect handler 边角语义
- interop 全域语义
- range / jump / throw / try / catch 语义
- invalid declaration 语义

### 2. 从 `CfirErrors` 总表看，仍有 13 个诊断没有被直接 inline 断言

当前 `CfirErrors.kt` 共定义 `103` 个诊断，`testData/diagnostics` 里直接断言到了 `90` 个，尚未直接断言到的有：

- `BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION`
- `DEPRECATED_MODIFIER_CONTAINING_DECLARATION`
- `DEPRECATED_MODIFIER_FOR_TARGET`
- `DEPRECATED_MODIFIER_PAIR`
- `EXTEND_IMMUTABLE_INDEX_ASSIGNMENT`
- `INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION`
- `INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION`
- `MISMATCHING_HANDLE_BLOCK`
- `MUT_ONLY_ON_FUNCTION`
- `NEW_INFERENCE_ERROR`
- `NO_CONSTRUCTOR`
- `STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE`

这 13 个里，真正值得优先关注的是三组：

- 声明状态与构造器：`DEPRECATED_MODIFIER_*`、`DEPRECATED_MODIFIER_PAIR`、`NO_CONSTRUCTOR`、`MUT_ONLY_ON_FUNCTION`、`STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE`
- effect 语义：`MISMATCHING_HANDLE_BLOCK`
- 推断语义：`NEW_INFERENCE_ERROR`、`BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION`、`INFERRED_TYPE_VARIABLE_INTO_*`

说明：
`TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR` 目前保留在框架模型里，但已明确不作为当前仓颉 first-party 语义检查的一部分推进。
原因是官方仓颉实现与公开文档都没有提供类型参数注解入口的证据，因此当前不应继续为它补 producer 或回归。

### 3. 框架级空白比“单个用例缺失”更值得优先处理

有两类缺口不是补一个 `.cj` 文件就能解决的：

- `CommonDeclarationCheckers.memberDeclarationCheckers` 当前为空。
  - 结果：`CfirStaticModifierCompatibilityChecker`、`CfirMutModifierApplicabilityChecker` 虽然已经实现，但默认 diagnostics 流水线不会执行。
  - 直接受影响诊断：`STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE`、`MUT_ONLY_ON_FUNCTION`
- `coverage/invalid/` 目录为空，`CommonDeclarationCheckers.invalidDeclarationCheckers` 也为空。
  - 这意味着 invalid declaration 这条链路当前既没有 checker 注册，也没有测试目录承接。

除此之外，还有两条“各种检查”层面的结构性空白：

- `CommonTypeCheckers` 当前只有 `CfirTypeProjectionModifierChecker` 一条规则。
  - 但 `testData/diagnostics` 中没有专门承接 type projection / type-ref modifier 语义的目录或回归样例。
  - 这意味着类型引用层的 modifier compatibility 目前没有显式的名称级保护。
- `CommonLanguageVersionSettingsCheckers` 当前为空。
  - 语言版本开关相关的语义当前没有 checker 层建模，也没有独立的 diagnostics 回归目录。

### 4. 官方 C++ 已有但当前 CFIR 仍未进入测试面的整块语义

按官方语义基线对照，当前仍明显缺块级覆盖的包括：

- `inout`
- `common/specific`
- `mock`
- Java / ObjC / 更完整的 FFI interop
- `range`
- `throw/catch`
- `jump`（`break` / `continue` 非法使用）
- 更细粒度的 effect handler 语义

其中前四类在 `docs/diagnostics-gap-vs-official-cpp-sema-status-2026-04-06.md` 中已经被明确标记为后置项；后四类则已经到了可以进入 `analysis-tests` 目录建回归的阶段。

---

## 二、声明检查视角：当前还缺什么

### 1. `CfirModifierChecker` 只测到了“错误/冗余/重复”，没测到“弃用分支”

已有覆盖：

- `WRONG_MODIFIER_TARGET`
- `WRONG_MODIFIER_CONTAINING_DECLARATION`
- `REDUNDANT_MODIFIER`
- `REDUNDANT_MODIFIER_FOR_TARGET`
- `REPEATED_MODIFIER`
- `OVERRIDE_STATIC_ERROR`
- `REDEF_INSTANCE_ERROR`

仍缺的直接断言：

- `DEPRECATED_MODIFIER_FOR_TARGET`
- `DEPRECATED_MODIFIER_CONTAINING_DECLARATION`
- `DEPRECATED_MODIFIER_PAIR`

也就是说，当前 `coverage/declaration-status/modifierCheckerRich.cj` 主要覆盖的是“非法”和“冗余”，还没有把“语义仍允许但已经弃用”的分支拉起来。

### 2. 构造器语义已有主路径，但缺直接 `NO_CONSTRUCTOR` 负例

当前 `constructor/*` 已经覆盖：

- `EXPLICIT_SUPER_CALL_REQUIRED`
- `ILLEGAL_THIS_OR_SUPER_CALL`
- `RECURSIVE_CONSTRUCTOR_CALL`
- `AMBIGUOUS_CONSTRUCTOR_CALL`
- `NO_VALUE_FOR_PARAMETER`

但 `CfirConstructorDelegationChecker` 里真正的 `NO_CONSTRUCTOR` 分支还没有被直接 inline 断言。

这会留下一个回归空窗：

- `this(...)` 找不到同类构造器
- `super(...)` 找不到父类匹配构造器

当前 suite 里构造器错误更偏向“参数数目不对”和“歧义”，缺真正的“无可选构造器”负例。

### 3. `memberDeclarationCheckers` 里两条声明状态规则实现了，但默认根本没跑

受影响 checker：

- `CfirStaticModifierCompatibilityChecker`
- `CfirMutModifierApplicabilityChecker`

原因：

- `CommonDeclarationCheckers.memberDeclarationCheckers` 当前返回 `emptySet()`

结果：

- `STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE` 不会出现在默认 diagnostics 中
- `MUT_ONLY_ON_FUNCTION` 不会出现在默认 diagnostics 中

这不是“缺测试文件”，而是“注册链路没接上”。在这个问题解决之前，补 `.cj` 也不会生效。

### 4. `extend` 不可变分支仍有一条诊断没有闭环

当前 `coverage/extensions/*` 已经把以下 extend 语义测得比较深：

- 目标合法性
- 接口合法性
- duplicate interface
- orphan rule
- generic usage
- immutable `mut prop`
- specialization conflict
- default implementation conflict
- C type 边界

但 `EXTEND_IMMUTABLE_INDEX_ASSIGNMENT` 还没有测试断言。

这里要特别说明：这条诊断不是单纯“漏写了用例”。
当前 `CfirExtendSemanticsSupport.isImmutableTarget()` 只把 `enum` 视为 immutable，而 `isImmutableNonEnumTarget()` 又显式排除了 `enum`，因此 `CfirExtendImmutableMemberChecker` 的 index-assignment 分支在现有语义下事实上不可达。

结论：

- 这条不是“先补测试”的问题。
- 应先收敛 extend immutable 语义设计，再决定是否保留该诊断和如何构造可触发用例。

### 5. 初始化检查已有核心覆盖，但还缺更完整的生命周期场景

当前已覆盖：

- `USED_BEFORE_INITIALIZATION`
- `CLASS_UNINITIALIZED_FIELD`

还缺的生命周期语义：

- 全局 / static 初始化顺序导致的 used-before-init
- `try/catch/finally`、循环、多分支 merge 下的初始化状态回归
- 构造器中更复杂的字段流分析

当前 `initialization/*` 更偏“核心冒烟”，还不是“生命周期语义矩阵”。

### 6. 声明 checker 扩展点里仍有整排空位

在 `CommonDeclarationCheckers` 中，以下扩展点当前没有任何具体 checker：

- `invalidDeclarationCheckers`
- `callableDeclarationCheckers`
- `memberDeclarationCheckers`
- `propertyCheckers`
- `typeAliasCheckers`
- `valueParameterCheckers`
- `mainFunctionCheckers`
- `anonymousFunctionCheckers`
- `enumConstructorCheckers`

这意味着这些声明种类的语义，要么仍依赖 resolver/cone 诊断兜底，要么根本还没进入 checker 层建模。

---

## 三、表达式检查视角：当前还缺什么

### 1. `call/*` 只有一份文件，调用绑定语义明显偏薄

当前 `call/namedArgumentsAndArityRich.cj` 已覆盖：

- `NO_VALUE_FOR_PARAMETER`
- `TOO_MANY_ARGUMENTS`
- `NAMED_ARGUMENTS_NOT_ALLOWED`
- `NAMED_PARAMETER_NOT_FOUND`
- `ARGUMENT_PASSED_TWICE`
- `MIXING_NAMED_AND_POSITIONAL_ARGUMENTS`
- `NEED_NAMED_ARGUMENT`

但对照官方语义，仍明显缺：

- `sema_ambiguous_match`：普通函数调用歧义
- `sema_ambiguous_func_ref`：函数引用歧义
- `sema_param_named_mismatched`：参数名不匹配

也就是说，当前 `call/*` 还主要是“参数绑定错误”，没有真正覆盖“候选选择/引用绑定”的分支。

### 2. const-eval 只覆盖到 `+ - * / %` 的一部分，shift/range 边界仍空白

当前 `CfirConstEvalArithmeticChecker` 只处理：

- `+`
- `-`
- `*`
- `/`
- `%`

当前 suite 已覆盖：

- `CONST_EVAL_DIVIDE_BY_ZERO`
- `CONST_EVAL_ARITHMETIC_OVERFLOW`

仍缺的官方常量边界语义：

- `sema_mod_zero`
- `sema_shift_count_overflow`
- `sema_negative_shift_count`

这说明这里不只是测试缺失，还是实现面尚未长到 shift 语义。

### 3. pattern legality 已有基础覆盖，但还没进入“高级错误语义”

当前 `pattern/patternLegalityRich.cj` 已覆盖：

- `TUPLE_PATTERN_NOT_MATCH`
- `PATTERN_NOT_MATCH`
- `ENUM_PATTERN_PARAM_SIZE_ERROR`

仍缺的官方 pattern 语义：

- `sema_not_overload_in_match`
- `sema_match_case_has_no_type`

加上 `coverage/match/*` 目前主要还是穷尽性，因此 match/pattern 域整体仍然偏“基础合法性 + 穷尽性”，还没把错误语义的深层分支补齐。

### 4. effect handlers 已有 smoke coverage，但缺完整语义矩阵

当前 `effects/*` 已覆盖：

- `EFFECTS_FEATURE_DISABLED`
- `COMMAND_INCOMPATIBLE_TYPE`
- `COMMAND_HANDLE_TYPE_ERROR`
- `IMPLICIT_RESUME_OUTSIDE_HANDLER`
- `RESUME_NO_WITH`
- `RESUME_THROWING_MISMATCH_TYPE`

仍缺的直接断言或完整语义：

- `MISMATCHING_HANDLE_BLOCK`
- `sema_resumption_handle_type_error`
- `sema_resumption_incorrect_return_type`
- `sema_command_resumption_mismatch`
- `sema_resume_wrong_resumption_type`
- `sema_return_in_try_handle_block`

其中 `MISMATCHING_HANDLE_BLOCK` 已经映射进 `CfirErrors`，但当前 `effectsSemanticsRich.cj` 里只有示例函数，没有 inline 断言；其余几项则还看不到对应 producer 或稳定用例。

### 5. 推断诊断已经接入映射，但 suite 还没有直接保护

`coneDiagnosticToCfirDiagnostic.kt` 当前已经映射了这些推断诊断：

- `NEW_INFERENCE_ERROR`
- `BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION`
- `INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION`
- `INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION`

但 `testData/diagnostics` 里没有任何直接 inline 断言。
其中 `TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR` 已降为保留模型，不再纳入当前批次的直接覆盖目标。

这会导致一个问题：

- 当前 `type-mismatch/*` 虽然很多，但更偏最终表象诊断
- 一旦后续约束求解器、intersection 归约或 builder inference 行为变化，缺少“内部推断诊断名称级”的回归保护

### 6. 表达式 checker 扩展点仍有大面积空白

`CommonExpressionCheckers` 当前没有具体实现的扩展点包括：

- `namedAccessCheckers`
- `binaryOpCheckers`
- `comparisonExpressionCheckers`
- `typeOperatorCheckers`
- `tryExpressionCheckers`
- `throwExpressionCheckers`
- `jumpExpressionCheckers`
- `rangeExpressionCheckers`
- `subscriptExpressionCheckers`
- `errorExpressionCheckers`

需要注意：

- `operator/*`、`subscript*` 等目录并不是完全没测。
- 但它们目前主要依赖 resolver / cone 诊断，而不是 expression checker 层的稳定回归。

换句话说，当前 suite 对“表达式语义结果”有覆盖，但对“表达式 checker 分层”覆盖仍不均匀。

### 7. 官方已有、当前目录仍明显缺席的表达式语义

按官方 `DiagnosticSema.def` / `DiagRefactor/DiagnosticSema.def` 对照，当前 `testData/diagnostics` 里还没有形成完整覆盖的表达式语义包括：

- `sema_invalid_loop_control`
- `sema_step_non_zero_range`
- `sema_inconsistency_range_elemType`
- `sema_range_step_not_int64`
- `sema_throw_expr_with_wrong_type`
- `sema_except_catch_type_error`

这几项分别对应：

- `jumpExpressionCheckers`
- `rangeExpressionCheckers`
- `throwExpressionCheckers`
- `tryExpressionCheckers`

而这些扩展点当前正好也都还是空的。

---

## 四、建议的补齐顺序

如果按照“框架优先、收益最大”的顺序推进，建议分三批做：

### 第一批：先补注册链路与可直接落地的缺口

- 接上 `memberDeclarationCheckers`，让以下规则真正生效：
  - `CfirStaticModifierCompatibilityChecker`
  - `CfirMutModifierApplicabilityChecker`
- 新增直接断言用例：
  - `NO_CONSTRUCTOR`
  - `DEPRECATED_MODIFIER_FOR_TARGET`
  - `DEPRECATED_MODIFIER_CONTAINING_DECLARATION`
  - `DEPRECATED_MODIFIER_PAIR`
  - `MISMATCHING_HANDLE_BLOCK`

### 第二批：补薄弱但已进入实现面的语义域

- `call/*`：普通调用歧义、函数引用歧义、参数名不匹配
- `pattern/*`：`not overload in match`、`match case has no type`
- `const-eval/*`：`mod zero`、negative shift、shift overflow
- `initialization/*`：更复杂的生命周期流分析场景

### 第三批：新建目录承接还没正式进场的官方语义

- `jump/`
- `range/`
- `throw/`
- `try/`
- `effects/advanced/` 或继续扩展 `effects/*`
- `interop/advanced/`
- `common-specific/`
- `mock/`
- `inout/`

---

## 最终判断

当前 `cfir/analysis-tests/testData/diagnostics` 的主要问题，已经不是“完全没有语义测试”，而是：

- 回归点很多，但分布不均
- checker 层与 resolver 层覆盖不平衡
- 声明状态和 effect/推断边角诊断还没被名称级保护
- 一些官方稳定语义域仍未进入 CFIR 测试目录

因此，后续不建议继续只按“哪里红了补哪里”的方式零散加文件。
更合理的策略是同时维护两条清单：

- `checker` 清单：看哪些诊断名还没被直接断言
- 语义域清单：看哪些官方语义域还没有独立目录和回归矩阵

只有两条线同时推进，`diagnostics` 目录才会从“有很多用例”真正进化成“语义覆盖结构完整”。
