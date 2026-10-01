# diagnostics2 semantic matrix

`diagnostics2` is the semantic-matrix testdata root for CFIR diagnostics. This directory is intentionally organized by **Cangjie semantic domain**, not only by current internal diagnostic names.

## Evidence sources used for this batch

### Official language docs (first source of truth)

- Calls: `manual_source_zh_cn_function_call_desugar`
- Pattern / match: `manual_source_zh_cn_pattern_overview`, `manual_source_zh_cn_pattern_refutability`
- Throw / try: `manual_source_zh_cn_handle`, `manual_source_zh_cn_nothing`
- Const eval: `manual_source_zh_cn_const_func_and_eval`
- Mutability: `manual_source_zh_cn_mut`
- Interop / inout: `manual_source_zh_cn_cangjie_c_仓颉-C_互操作_0`, `manual_source_zh_cn_cangjie_c_inout_参数_3`
- Range: `manual_source_zh_cn_range`
- Loop control: `manual_source_zh_cn_expression_break_与_continue_表达式_12`
- Generic overview / constraints: `manual_source_zh_cn_generic_overview`

### Official C++ semantic anchors (second source of truth)

- Calls: `external/cangjie_compiler/src/Sema/TypeCheckCall.cpp`, `TypeArgumentInference.cpp`
- Match / pattern: `TypeCheckMatchExpr.cpp`, `TypeCheckPattern.cpp`, `PatternUsefulness.cpp`
- Initialization: `LegalityOfUsage/InitializationChecker.cpp`
- Effects / try / throw: `TypeCheckExpr/PerformExpr.cpp`, `ResumeExpr.cpp`, `TryExpr.cpp`, `ThrowExpr.cpp`
- Imports: `external/cangjie_compiler/src/Modules/ImportManager.cpp`, `ModulesDiag.cpp`
- Range: `TypeCheckExpr/RangeExpr.cpp`
- Interop: `FFI/FFICheck.cpp`, `FFI/CFFICheck.cpp`, `NativeFFI/**/*`
- Generic access: `TypeCheckGeneric.cpp`, `TypeCheckReference.cpp`, `TypeArgumentInference.cpp`
- Mutability / assignment legality: `TypeCheckAccess.cpp`, `TypeCheckExpr/AssignExpr.cpp`

### Project-local planning anchors (third source of truth)

- Diagnostics list: `cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/diagnostics/CfirDiagnosticsList.kt`
- Coverage gap audit: `cfir/analysis-tests/diagnostics-coverage-gap-vs-cpp.md`
- Existing generated suite: `cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisDiagnostics2TestGenerated.kt`

## Name-level gap list covered by this batch

This batch directly covers or keeps direct regression files for:

- `NO_CONSTRUCTOR`
- `REDUNDANT_MODIFIER_FOR_TARGET`
- `WRONG_MODIFIER_CONTAINING_DECLARATION`
- `REDUNDANT_MODIFIER`
- `NON_ABSTRACT_CLASS_CANNOT_BE_SEALED`
- `MUT_ONLY_ON_FUNCTION`
- `STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE`
- `COMMON_OPEN_CLASS_NO_INIT`
- `MISMATCHING_HANDLE_BLOCK`
- `NEW_INFERENCE_ERROR`
- `BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION`
- inferred empty / possible-empty intersection scenarios, currently normalized to `NEW_INFERENCE_ERROR` by the project diagnostic surface

## Semantic-domain gap list covered by this batch

- `calls/`
- `const-eval/`
- `constructor/`
- `coverage/match/`
- `declaration-status/`
- `effects/`
- `generic-access/`
- `initialization/`
- `interop/`
- `jump/`
- `match/`
- `mut/`
- `pattern/`
- `range/`
- `throw/`
- `try/`

## Marker-location decisions locked by evidence

- `NON_EXHAUSTIVE_MATCH`: mark the selector expression, matching official `cjc`.
- `THROW_EXPR_WITH_WRONG_TYPE`: official `cjc` marks the `throw` keyword. `ThrowExpr.cpp` accepts `core.Exception`, `core.Error`, and matching generic bounds, even though the current official diagnostic text still says `core.Exception`. The project now defines this diagnostic name and default message, but checker wiring is still pending, so `throw/` fixtures keep it as `SUGGESTED_DIAGNOSTIC`.
- `throw` currently accepts values whose static type is `Exception`, `Error`, or a generic upper bound `T <: Exception` / `T <: Error` in official current `cjc` and `ThrowExpr.cpp`, even though the emitted message still says `core.Exception`.
- the current manual text under `manual_source_zh_cn_handle` still says manual `throw` requires an `Exception` subtype and excludes `Error`; for this matrix, treat current `cjc` behavior plus `ThrowExpr.cpp` as the semantic authority until that doc/runtime mismatch is resolved.
- do not build positive `throw` cases by declaring custom `Error` subclasses: the official docs forbid inheriting `Error`, and current `cjc` rejects `class X <: Error {}` before the throw semantics are even tested.
- constructor delegation arity is resolved against the actually selected candidate set: `this(1)` inside a class that already has `init()` is an extra-argument error against the zero-arg constructor, while `super(1)` can still be a missing-argument error for a two-parameter superclass constructor.
- default parameter declarations in current Cangjie syntax require named-parameter form such as `a!: Int64 = 0`; `a: Int64 = 0` is not valid syntax.
- unnamed parameters must appear before named parameters in a declaration; a signature like `f(top!: Int64, plain: Int64)` is itself illegal before any call-site diagnostics are considered.
- to test `NAMED_PARAMETER_NOT_FOUND` or `ARGUMENT_PASSED_TWICE`, do not append an extra valid named argument after the offending one, or the official compiler will short-circuit to an extra-argument error first.
- `CATCH_TYPE_MUST_EXTEND_EXCEPTION`: official `cjc` marks the illegal catch type itself. The project now defines this diagnostic name and default message, but checker wiring is still pending, so `try/` fixtures keep it as `SUGGESTED_DIAGNOSTIC`.
- repeated or shadowed `catch` types are an official `USELESS_EXCEPTION_TYPE` warning on the later catch type token. The project now defines this diagnostic name and default message, but checker wiring is still pending, so those `try/` fixtures keep `SUGGESTED_DIAGNOSTIC` blocks and `IGNORE_ERRORS`.
- `finally { false }` in a typed `try` is not a branch-type error in current official `cjc`; it only produces the `unused expression` warning on `false`, and the current project still has no matching diagnostics2 warning surface for it.
- `RANGE_STEP_CANNOT_BE_ZERO`: official `cjc` marks the zero step expression itself. The project now defines this diagnostic name and default message, but checker wiring is still pending, so `range/` fixtures keep it as `SUGGESTED_DIAGNOSTIC`.
- range direction itself is not an error: descending ranges with positive step and ascending ranges with negative step remain legal in the current official `cjc`.
- current official `cjc` rejects the one-sided range forms that look like `0..`, `..10`, `0.. : 1`, or `..10 : 1` at parse stage; do not invent semantic range-step diagnostics for those spellings in `diagnostics2`.
- `INVALID_CFUNC_PARAMETER_TYPE`: official CFFI checking diagnoses the parameter type span. The project now defines this diagnostic name and default message, but checker wiring is still pending, so `interop/` fixtures keep it as `SUGGESTED_DIAGNOSTIC`.
- legal CFFI return types: `CType` and aliases expanded to `CType` stay positive; only non-`CType` returns use `INVALID_CFUNC_RETURN_TYPE`.
- invalid `CFunc<...>` return types also mark the concrete return type node itself, for example the `String` in `CFunc<() -> String>`.
- `@CallingConv` misuse: official CFFI checking splits this into scope misuse and non-foreign-target misuse; the project now defines both diagnostic names and default messages, but checker wiring is still pending, so `interop/` fixtures keep them as `SUGGESTED_DIAGNOSTIC`.
- `@CallingConv` currently accepts only `CDECL` and `STDCALL` in official `CFFICheck.cpp`; do not invent extra positive convention values in `diagnostics2`.
- `inout` requires a mutable variable: for literals and temporary expressions use `INOUT_MUST_BE_VAR_VARIABLE` on the qualified expression, and for member access through an immutable base like `box.value` mark the immutable base variable token (`box`).
- `inout` on an immutable local variable like `let x = 1` also uses `INOUT_MUST_BE_VAR_VARIABLE` on the referenced variable token `x`, not on the `inout` keyword.
- repeated use of the same variable in multiple `inout` arguments currently has no proven official semantic diagnostic in this matrix; do not invent `DUPLICATE_INOUT_ARGUMENT` expectations for it.
- `break` / `continue` inside a `handle` block are ordinary loop-control legality failures when the handle block itself is not a loop body; keep `INVALID_LOOP_CONTROL` on the keyword.
- a `finally` block result is ignored rather than joined into the surrounding `try` expression type; a trailing value like `false` in `finally` is only an official `unused expression` warning, and the current project has no matching diagnostic in this matrix.
- `createMock` / `createSpy` diagnostics in the current project are reported on the callee reference token, so `MOCK_*` markers should wrap `createMock` itself, not the whole call expression.
- for `createMock` / `createSpy`, unsupported target kinds such as primitives, tuples, function types, and enums stay `MOCK_UNSUPPORTED_TYPE` even outside test mode; class and interface targets then fall through to `MOCK_NOT_IN_TEST_MODE` in this matrix.
- import conflicts: mark only the later conflicting imported short name or alias token.
- unresolved import targets: when the package itself is missing, official current `cjc` marks the whole missing package path span such as `ghost.pkg`, `ghostv.pkg.deep`, or `std.void`, not only the last segment token.
- repeated / shadowed catch types use `USELESS_EXCEPTION_TYPE` on the catch type token.
- repeated / shadowed catch types are warnings in official current `cjc`, not hard errors, even though this matrix stores them as undefined-diagnostic placeholders.
- ordinary `try` / `catch` branch result mismatch still reports on the mismatching tail expression inside the offending catch block, for example the `false` branch result in an `Int64`-typed `try` expression.
- `try-with-resources` resource-type mismatch reports on the whole resource specification entry, for example `x = NotResource()`, with the note that the resource specification should implement `Resource`.
- a declared `Unit` return body with a trailing value expression like `func f(): Unit { 1 }` is only an official `unused expression` warning, not a return-type mismatch in this matrix.
- member visibility: current official `cjc` accepts cross-package `protected` member access, but `internal` is only visible inside the defining package and its subpackages, so `visibility/protectedAndInternalMatrix.cj` must mark unrelated-package `internal` access as invisible.
- enum type call vs enum constructor call: when a bare enum type like `A` is called as `A(1)`, use `ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR`, not `NO_MATCHING_OPERATOR_INVOKE`.
- operator invoke with target-type mismatch: once `operator ()` resolution succeeds, a wrong expected result type is `TYPE_MISMATCH`, not `NO_MATCHING_OPERATOR_INVOKE`.
- generic static/member qualification splits by surface: a bare generic type in type position still uses `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT`, but a call like `Box.create()` without class type arguments is an official current `cjc` inference failure, so keep `NEW_INFERENCE_ERROR` on `Box.create`.
- generic inference contradiction such as `chooseGeneric(1, true)` keeps `NEW_INFERENCE_ERROR` on the callee reference, matching official current `cjc`.
- repeated interface upper bounds such as `T <: Named & Named` are accepted by the current official `cjc`; do not invent a duplicate-bound diagnostic for them in `diagnostics2`.
- `static` with `open` / `abstract` / `override` can surface both token-level modifier conflicts and the declaration-level project diagnostic `STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE`; keep the dedicated declaration marker on the member name in addition to modifier markers.
- `mut` on a class member function keeps the parser-style wrong-modifier marker on `mut`, and the project also adds the declaration-level diagnostic `MUT_ONLY_ON_FUNCTION` on the function name.
- `INTERFACE_CANNOT_INHERIT_CLASS` currently points at the interface declaration itself in official `cjc`, not the offending class supertype token.
- `MULTIPLE_CLASS_SUPER_TYPES` currently points at each extra concrete supertype after the first one, not the first class in the supertype list.
- `SUPER_TYPES_DUPLICATE` currently points at the whole class declaration in official `cjc`, not the duplicated interface/type token inside the supertype list.
- a single identity lambda like `applyFunc({ a => a }, true)` is a legal positive case: `T` is inferred from the non-lambda argument, and official current `cjc` emits no diagnostic for the lambda body or call.
- unresolved implicit function return type: when the current project surfaces `UNABLE_TO_INFER_RETURN_TYPE`, mark the function declaration name, not the tail expression that made the body return types disagree.
- `return` inside `try { ... } handle (...) { ... }` uses `RETURN_IN_TRY_HANDLE_BLOCK`; do not degrade this case to generic `INVALID_RETURN`.
- `resume with` wrong payload type currently stays `TYPE_MISMATCH`; official `ResumeExpr.cpp` reuses generic type checking here, and the current project has no stable specialized resumption-value diagnostic on this path.
- `MISMATCHING_HANDLE_BLOCK`: mark the `handle` block itself (`{ ... }`), matching official `TypeCheckExpr/TryExpr.cpp` on `handler.block` and the existing rich fixture.
- `BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION`: official current `cjc` points at the later conflicting lambda expression, so do not mark the whole call.
- non-tuple subject matched by a tuple pattern: use `TUPLE_PATTERN_NOT_MATCH`, not generic `TYPE_MISMATCH`.
- const pattern in `match`: if the literal itself cannot type-check against the selector type, keep it as generic `TYPE_MISMATCH` on the literal token; do not jump straight to `NOT_OVERLOAD_IN_MATCH`.
- selector-less `match { ... }`: `case <bool-expr> => ...` is legal in official current `cjc`; do not invent `MATCH_CASE_HAS_NO_TYPE` merely because the `match` has no selector expression.
- CJMP nominal kind mismatch: when common/specific declarations share the same name but differ by nominal kind, keep `NOT_MATCHED` on both sides and add `SPECIFIC_HAS_DIFFERENT_KIND` on the specific declaration.
- the same CJMP nominal-kind rule also applies when another same-name specific declaration already exists; an extra `specific class` still gets `SPECIFIC_HAS_DIFFERENT_KIND` if the common side for that name is an interface.
- `COMMON_OPEN_CLASS_NO_INIT`: official CJMP only uses it when a common open or abstract class without an explicit constructor is inherited by a general class in the common part; do not use it for a lone common open class.
- interface member `private` / `internal`: official current `cjc` semantics are invalid modifier usage, not deprecated compatibility.
- `open interface`: official current `cjc` semantics are redundant modifier, not deprecated target usage.

## Directory and naming rules

- Keep every new `.cj` file under `diagnostics2/`.
- Prefer **semantic domain directory + lowerCamelCase file name**.
- One file should normally cover one primary rule family.
- Closely related variants may live in the same file when they share the same semantic boundary.
- Use `// FILE:` sections only when package or multi-file context is required.

## Undefined-diagnostic block comment template

For official semantics that are not yet modeled by `CfirDiagnosticsList.kt`, use this exact block structure near the relevant code:

```text
/*
SUGGESTED_DIAGNOSTIC: <NAME>
SUGGESTED_MESSAGE: <message>
SOURCE: <manual doc id or path>; <official C++ file>
*/
```

## Batch boundary for the current OpenSpec implementation

### Current executable batch

- Extend currently thin but already-real semantic domains with runnable `.cj` samples.
- Add placeholder `.cj` samples for official semantics that are documented or present in official C++ Sema, but not yet modeled in the current project.

### Explicit backlog / later batch

- deeper `inout/` runtime-sensitive shapes beyond current placeholder coverage
- semantics that still require producer/checker registration work before a stable inline assertion can exist

## Writing order for future batches

1. Expand directly modeled diagnostics in already existing directories.
2. Expand thin directories into small semantic matrices.
3. Add new semantic-domain directories with placeholder diagnostics only when the official semantics are clear.
4. Move backlog domains into runnable directories only after semantics and internal modeling both stabilize.

## 批次 4（2026-10-01 历史记录，期望口径已被批次 6 取代）

批次 4 按"CFIR 现有诊断面"补齐语义矩阵：能对应上的写内联标记，对应不上的写成占位或"不可达"说明。
**该批次的期望取值有相当一部分是照 CFIR 当前行为写的**（例如元组解构赋值用 `TYPE_MISMATCH`、约束名用
`UNDECLARED_TYPE_NAME`、`arch == 1` 归到 `CONDITIONAL_COMPILATION_NOT_SUPPORT_BUILTIN_VALUE`、被条件编译剔除的变量
读取处补 `UNRESOLVED_REFERENCE`），不符合本仓库当前的测试数据口径（见下）。批次 6 已逐条按官方证据改写；
本节仅保留记录，不再作为依据。

## 测试数据口径（2026-10-01 起，适用于 diagnostics2 / llt / macro 全部测试数据）

1. **期望只由官方决定**：官方 C++ 源码（`external/cangjie_compiler` 的 `*.def` 与 `src/Sema/**` 报告点）加上本机
   cjc 0.53.18 / 1.0.0 / 1.0.5 / 1.1.3 的实测（kind、文案、起止行列）。**不因为 CFIR 当前输出不同而改期望**；
   按官方写出来是红的，就是 CFIR 的实现缺口，用例保持红并在本 README 记录。
2. **内联标记写本项目的诊断名**：`<!NAME!>` 里的 `NAME` 是 `CfirDiagnosticsList.kt` 声明的 CFIR 诊断名；官方 kind
   （`sema_xxx` / `parse_xxx` / `conditional_compilation_xxx`）只作为取证依据写在用例头部注释里。官方有 kind 而
   `CfirDiagnosticsList` 没有同名诊断时记为缺口，不用别的名字凑。
3. **锚点按官方范围**：官方常锚 1 个字符（关键字首字母、条件名、右值），不是整条声明或整个标识符；嵌套标记按框架的
   栈语义使用（`AbstractCfirAnalysisDiagnosticsTest.parseInlineDiagnostics`）。
4. **多版本**：1.0.5 与 1.1.3 逐点对照（两版对本文涉及的 fixture 结论一致）；差异用 `LANGUAGE_VERSION` 语言版本门禁表达，
   不以单一版本为基线。
5. **指令写法**：指令行必须以 `//` 开头（框架 `RegisteredDirectivesParser.DIRECTIVE_PATTERN` 用 `matchEntire`），
   所以标记不能放在指令行；`NO_PRELUDE`、`CHECK_PROGRAM_ENTRY` 是模块级指令，必须写在第一个 `// FILE:` 段之前；
   同一 fixture 里若某个 `// FILE:` 段带 parse 错误，官方 cjc 会按"首个错误类别"过滤掉其它段的语义诊断
   （`DiagnosticEngine.cpp` `CanBeEmitted`），需要独立取证的段要分到不同段或不同 fixture。

## 批次 6（2026-10-01 复核）：全部未提交测试数据按官方重核

### 取证方式

- 每个 fixture 按 `// FILE:` 拆段、剥离标记后**逐段单独**用 cjc 1.0.5 / 1.1.3 `--diagnostic-format json` 编译，
  逐点比对"内联标记范围 ↔ 官方诊断 `MainHint.Range`"（起止行列都要求一致）。
- 多包 fixture 按包分目录顺序编译：先编依赖包（`--output-type=staticlib --output-dir <out> -o <pkg>.a`，产物含
  `.cjo`），再用 `--import-path <out>` 编译引用方；包有层级时用 `cjc -p <父包目录> -o <out>`（子包目录嵌在父包目录内）。
- 官方报告点与条件以 `external/cangjie_compiler`（git tag `v1.0.0`，另有 v1.1.x / v1.2 / v1.3 标签）源码核对。
- 结果：diagnostics2 45 个未提交 fixture 共 **82 个内联标记，cjc 1.0.5 全部命中**；cjc 1.1.3 仅 3 个 MISS，都是
  1.1.3 SDK 的 `opt.exe` 路径指向 1.0.5 安装导致依赖包静态库构建崩溃的**环境问题**（手工用 1.1.3 单独编译同样输入，
  `sema_cannot_inherit_sealed` / `parse_redundant_modifier` 结论与 1.0.5 一致）。唯一"官方范围与标记不同"的一处是
  `extend/extendMutInterfaceOnPrimitive.cj` 的 `mut`（官方 `sema_invalid_mut_modifier_extend_of_struct` 锚 `mut`
  3 个字符，`sema_property_must_have_accessors` 锚其首字母 1 个字符，两条同起点不同范围，栈式嵌套表达不了后者，
  用例头部已注明；对应本项目 `PROPERTY_MUST_HAVE_ACCESSORS` 本批次不覆盖）。

### 复核中改正的用例（相对批次 4/5 的写法）

| 用例 | 批次 4/5 的写法 | 按官方改正为 |
| --- | --- | --- |
| `assign/mismatchedTypesMultipleAssign.cj`、`inference/arrayLiteralAndMultipleAssign.cj` | `TYPE_MISMATCH` | `MISMATCHED_TYPES_MULTIPLE_ASSIGN`（官方 `sema_mismatched_types_multiple_assign`，锚右值元组整段） |
| `constraints/nameInConstraintVisibleClass.cj` | `UNDECLARED_TYPE_NAME` | `NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER`（官方 `sema_generics_type_variable_not_defined`，约束名首字母；标准库类名与本文件类名两种形态官方 kind 与锚点相同） |
| `call/arityMismatch.cj` | 锚整个调用 | 锚实参列表 `(1)` / `(1, 2)` |
| `entry/mainSignature.cj`、`entry/mainParameterType.cj` | 锚 `main` | 锚 `main` 首字母 |
| `entry/missingProgramEntry.cj` | 标记在指令行上、指令位置非法 | 首行普通注释带标记（官方 1:1），指令移到首个 `FILE:` 段之前 |
| `inheritance/*`（superAlone、superclassMustBePlacedFirst、thisAndSuperOutsideClass、cannotInheritSealedClass、thisOrSuperInStaticInit） | 锚整词 / 父类型引用 | 锚关键字首字母 1 字符 / 子类名 `Sub` 整段 |
| `inout/*` | 锚实参 `s` / `v` | 锚 `inout s` / `inout v` 整段；`inoutNonCType` 用嵌套标记（外层非 CType、内层 mismatched types 各锚自己的范围） |
| `operator/subscriptAssignNoPositional.cj` | 锚 `operator` | 锚 `[]`；另补 `subscriptAssignNamedParameter.cj`（`sema_invalid_subscript_assign_parameter` 锚具名参数 `v!`） |
| `unary/invalidUnaryWithTarget.cj` | `INVALID_UNARY_EXPR` | `INVALID_UNARY_EXPR_WITH_TARGET`（官方 `sema_invalid_unary_expr_with_target`） |
| `inference/builderInferenceMultiLambda.cj` | 三个 `UNABLE_TO_INFER_GENERIC_FUNC` 标记锚 lambda | 改为 `pair(1, "x")` 形态，锚被调用者（官方 `sema_unable_to_infer_generic_func`，`TypeArgumentInference.cpp` 诊断建在 `ce.baseFunc` 上）；多 lambda 冲突在官方是内部断言失败 `sema_invalid_node_after_check`（零宽位置、note "please report this to Cangjie team"），不可表达 |
| `static-init/returnInStaticInit.cj`、`property/propertyAccessorRules.cj`、`intrinsic/*`、`extend/*`、`conditional-compilation/*`、`deprecation/*` | 锚整条声明 / 缺 `open` 基类 / 混在一段 | 按官方 kind 与范围改正；`classInheritance` / `overrideAndWeakening` 的父类补 `open`（否则官方先报 `sema_non_inheritable_super_class`）；`intrinsic` 的 `INVALID_INTRINSIC_DECL` 用无函数体形态；`conditional-compilation` 每个条件独立成段（`@When[1]` 是 parser 错误，会掩盖其它段），`arch == 1` 归 `CONDITIONAL_COMPILATION_INVALID_CONDITION_VALUE`、新增 `arch == "arm"` 归 `_NOT_SUPPORT_BUILTIN_VALUE`，条件名锚 1 字符、右值锚右值；`@When[]`、`@When[1]` 官方只有 parser 诊断，本项目无对应名字，用例不写标记 |
| `deprecation/callAndTypeRef.cj`、`classInheritance.cj`、`overrideAndWeakening.cj` | 同文件混放、类名重复、weakening 标记含注解行 | 拆段（cjc 1.0.5 对有父类且声明 `init()` 的顶层类有 parse 限制）、改名、weakening 标记只覆盖成员声明（官方不含 `@Deprecated` 行） |
| `general/coreObjectNotFoundNoPrelude.cj` | 整文件包 `CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE` + 构造调用 `UNRESOLVED_REFERENCE` | 只标类成员类型位置 `Object` 首字母（官方 `sema_undeclared_type_name`）；另两条官方诊断（`sema_no_core_object` 锚 `class`、无位置的 `sema_core_object_not_found_when_no_prelude`）无可用名字 / 无位置，头部注明 |
| `extend/extendMutInterfaceOnPrimitive.cj` | — | 两条同锚诊断合并为一个标记；补 `needMemberOnly.cj` 对照（只报 `sema_need_member_implementation`） |
| `general/conflictWithSubPackage.cj` | 注释称 `--no-sub-pkg` 双包 | 注释改为包目录 + `cjc -p`；消息补 `possible sub-package` |

### 宏 LLT（`macro/llt/function/defaultParameter_pkg_02`）

- 弃用规则按官方 `CheckUsageOfDeprecated`（`src/Sema/TypeCheckReference.cpp`）核对：使用点位于**同包**某个
  `@Deprecated` 声明内部才豁免（strict 上下文豁免所有同包目标；非 strict 上下文只豁免非 strict 目标）；进入的是
  `@Deprecated` 声明整棵子树。三个文件共 363 个 `DEPRECATED_WARNING` 标记：原 74 个 + 新增 289 个（`Enum_….enumValue_…`
  限定符 17×3、弃用全局变量裸引用 81/87/70），逐条按该规则核对遗漏 0、多报 0；把标记剥掉后源码与 HEAD 逐字节相同。
- 其它标记（`TYPECAST_OVERFLOW`、`UNREACHABLE_PATTERN`、`TYPE_MISMATCH`、`REDUNDANT_MODIFIER`、`EXPECT_CONST`）
  未改动，与 CFIR 当前输出一致。
- **已知与官方不一致（既有数据，非本批次引入）**：`TYPECAST_OVERFLOW` 官方 `chir_typecast_overflow` 只在 ConstAnalysis
  稳定（`ConstAnalysis.h` `RaiseTypeCastOverflowError` 的 `isStable` 分支）时报告，cjc 1.0.5 / 1.1.3 对
  `f2/test3.cj` 的 35 个标记只报 8 个（其余位于 `Range` 构造式、枚举构造实参等不稳定上下文）。本批次未按 cjc 收窄，
  记录待后续与 fixture 作者确认。

### CFIR 缺口（按官方写期望后仍红的用例；`:cfir:analysis-tests:test` 2026-10-01 结果）

`--tests '…Diagnostics2*' --tests '…LLT*' --tests '…Macro*'`（2026-10-01 16:5x 一次全量）共 8176 个用例、78 失败；
随后只重跑了 Entry / General 两组（指令作用域修正后）30 个用例、6 失败，即下表的锚点缺口（`testMissingProgramEntry`
的指令作用域报错已消除，现为绿）。其中：

- **基线既有失败（14）**：`Diagnostics2{,Psi}$CommonSpecific$E2e` 8（CJMP e2e）、`Macro{,Psi}$APILevelChecker$Level*`
  4、`LLT{,Psi}$Operator$testPowInt64Uint640` 2。与本批次无关。
- **宏 LLT 新增标记（6）**：`Macro{,Psi}$DefaultParameterPkg02` 三个用例，CFIR 只产出原 25/25/24 个弃用诊断，
  缺变量引用与 `E.Ctor` 限定符的弃用检查（无变量引用检查器）。
- **其余 58 个为"官方期望 vs CFIR 输出"的锚点 / 诊断名 / 缺失缺口**：

| 用例 | 官方（本用例期望） | CFIR 当前输出 |
| --- | --- | --- |
| `call/arityMismatch`、`constructor/curriedConstructor`、`generic/upperBounds`、`imports/cannotRefToPackageName`、`inference/builderInferenceMultiLambda`、`inheritance/thisAndSuperOutsideClass`(super 部分)、`extend/extendMutMemberOnImmutable`、`extend/extendIllegalMember`、`property/propertyAccessorRules`、`general/conflictWithSubPackage`、`conditional-compilation/whenNoCondition`、`deprecation/deprecatedConstructor`、`deprecation/propertyAndInterfaceMember`、`thread-context/threadContextOpen` | 与 CFIR 一致 | 绿 |
| `declaration-status/staticOpenAndOverload` | `INCOMPATIBLE_MODIFIERS` 锚 `open` | 额外把 `static` 也标一条 |
| `constraints/nameInConstraintVisibleClassifier` | 锚 `Local` 首字母 | 锚整个 `Local` |
| `deprecation/classInheritance`、`deprecation/overrideAndWeakening` | 父 strict + 子放宽 → `DEPRECATION_WEAKENING` | 报 `DEPRECATION_OVERRIDE_ERROR` |
| `deprecation/callAndTypeRef` | 锚 `ReturnRef` / `CtorRef` | 把 Option 前缀 `?` 一并标入 |
| `entry/mainSignature`、`entry/mainParameterType` | 锚 `main` 首字母 | 锚整个 `main` |
| `extend/extendImmutableIndexAssignment` | 锚 `[]` | 锚整条 operator 声明 |
| `extend/extendMutInterfaceOnPrimitive` | `extend` 首字母；无 `REDUNDANT_MODIFIER`；`mut` 上另有 `PROPERTY_MUST_HAVE_ACCESSORS` | `extend` 整词；接口成员 `public` 不报 redundant；无 `PROPERTY_MUST_HAVE_ACCESSORS` |
| `extend/extendImportedInterfaceOrphan` | 接口成员 `public` 各一条 `REDUNDANT_MODIFIER` | 不产出 |
| `general/coreObjectNotFoundNoPrelude` | 锚 `Object` 首字母；文件首字符无标记 | 锚整个 `Object`；文件首字符多一条 `CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE` |
| `inference/arrayLiteralAndMultipleAssign`、`assign/mismatchedTypesMultipleAssign` | `MISMATCHED_TYPES_MULTIPLE_ASSIGN` | `TYPE_MISMATCH` |
| `inheritance/cannotInheritSealedClass` | 锚子类名 `Sub` | 锚父类型引用 `Sealed` |
| `inheritance/superAlone`、`inheritance/superclassMustBePlacedFirst`、`static-init/thisOrSuperInStaticInit` | 关键字首字母 1 字符 | 整个关键字 |
| `inout/inoutCString`、`inout/inoutNonCFuncCall` | 锚 `inout s` / `inout v` | 锚实参 `s` / `v` |
| `inout/inoutNonCType` | 非 CType 锚 `inout s`，mismatched types 锚 `s` | 两条合并标在 `s` 上 |
| `intrinsic/intrinsicMemberAndBody` | 锚函数声明（不含注解行）、函数体、`func` 首字母 | 锚"注解 + 签名"整段，重复声明只标一处 |
| `operator/subscriptAssignNamedParameter` | 锚具名参数 `v!` | 无诊断（折叠为读取运算符） |
| `operator/subscriptAssignNoPositional` | 锚 `[]` | 前端解析该签名失败（`ABSTRACT_MEMBER_NOT_IMPLEMENTED` / `MISSING_FUNC_BODY` / `UNRESOLVED_REFERENCE`），已用 `DISABLE_WITH_PARSER: Psi` 关闭 PSI 变体 |
| `static-init/returnInStaticInit` | 只报 static init 内 return | 额外报 `RETURN_TYPE_MISMATCH` |
| `unary/invalidUnaryWithTarget` | `INVALID_UNARY_EXPR_WITH_TARGET` | `INVALID_UNARY_EXPR` |
| `conditional-compilation/whenConditionErrors`、`whenConditionValueAndDebugOp` | 条件名 / 右值 1 字符 | 无条件编译诊断产出 |

### 官方无法用内联标记表达的诊断（记录备查）

- 无位置：`sema_core_object_not_found_when_no_prelude`（JSON `Line = -1`）。
- parser 阶段 kind，本项目无对应诊断名：`parse_expected_expression`（`@When[]`）、`parse_unrecognized_expression_in_when`
  （`@When[1]`）、`parse_conflict_modifier`、`parse_intrinsic_function_*`、extend 内的 `parse_unexpected_declaration_in_scope`
  （→ 本项目用 `EXTEND_ILLEGAL_MEMBER` 表达）、`sema_invalid_node_after_check`（多 lambda 泛型推导冲突的内部断言失败，
  零宽位置）。
- 官方有 kind 但 `CfirDiagnosticsList` 无同名诊断：`sema_no_core_object`、`sema_generics_type_variable_not_defined`
  （→ `NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER`）、`sema_invalid_mut_modifier_extend_of_struct`（→ `MUT_ONLY_ON_FUNCTION`
  与 `EXTEND_IMMUTABLE_MUT_PROPERTY`）、`sema_immutable_type_extend_assignment_index_operator`（→ `EXTEND_IMMUTABLE_INDEX_ASSIGNMENT`）、
  `sema_interface_is_not_extendable`（→ `EXTEND_INTERFACE_NOT_EXTENDABLE`）、`sema_cannot_inherit_sealed`（→ `CANNOT_INHERIT_SEALED`）、
  `sema_conflict_with_sub_package`（→ `CONFLICT_WITH_SUB_PACKAGE`）、`sema_cannot_ref_to_pkg_name`、`sema_wrong_number_of_arguments`
  （→ `WRONG_NUMBER_OF_ARGUMENTS`）；`sema_undeclared_type_name` → `UNDECLARED_TYPE_NAME`、`sema_mismatched_types` → `TYPE_MISMATCH`。
- 宏包形态：宏 fixture 的宏定义文件用 `macro package define` 旧语法，cjc 1.0.5 不接受；根包 `test_macro.cj` /
  `test_mutation.cj` 内的 `@Deprecated` 标记由规则核对（见上），未逐点跑 cjc（宏展开产物需要宏包可导入）。`f2/test3.cj`
  可单独编译，111/111 弃用标记、28/28 unreachable、4/4 redundant 全部命中。

### 本机 cjc 环境事实（复核时踩到）

- 可执行目标：`cjc x.cj -o out`（不带 `.a` 后缀）才检查 `main` 入口并报 `sema_missing_entry`；`-o out.a` 或
  `--output-type=staticlib` 零诊断。
- `-p` 包目录模式不接受 `-o` 文件名，只能给输出目录；`--no-sub-pkg` 不能让同目录双包编译通过（先报
  `package_multiple_package_declarations`）。
- 1.1.3 SDK 的 `opt.exe` 路径指向 1.0.5 安装目录，静态库构建在本机会崩（`LLVM ERROR` / exit 3221225501），
  依赖包编不出来时引用方只得到 `package_search_error`；1.1.3 的语义诊断本身可用（单独编译一致）。
- 1.0.5 / 1.0.0 / 0.53.18 的 JSON 诊断可用；`@When` 的 `cjc_version` 在 1.0.x 有补零 bug（`== "1.0.5"` 恒假，
  `>= "1.0.5"` 为真），1.1.3 修好；fixture 不要用 `==` 配当前版本号。
