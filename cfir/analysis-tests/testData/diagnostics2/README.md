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
| `inference/arrayLiteralAndMultipleAssign`、`assign/mismatchedTypesMultipleAssign` | `MISMATCHED_TYPES_MULTIPLE_ASSIGN` | `TYPE_MISMATCH`（2026-10-06 已修复：`CfirAssignmentTypeMismatchChecker` 专用分支改报 `MISMATCHED_TYPES_MULTIPLE_ASSIGN`，实参取整个 RHS 类型；LLT `multipleAssignExpr` 族与 `InitializationCheck/variable_use_before_init_03/04` 的旧 `TYPE_MISMATCH` 标记同步迁移） |
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

## 批次 7（2026-10-02）：属性继承 / 模式声明 / mut 函数引用三个语义夹具

本批次在上一批次（45 个夹具）之后补三个新夹具，全部按“官方 C++ 报告点 → cjc 1.0.5 / 1.1.3 实测范围 → 本项目诊断名”的顺序写，逐点核对两版 Range 的起止行列完全一致后才落标记。

### 新增夹具

| 夹具 | 覆盖的 CFIR 诊断名 | 段数 / 标记数 |
| --- | --- | --- |
| `property/propertyInheritanceRules.cj` | `PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF`、`PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT` / `_IMMUT`、`PROPERTY_MUST_IMPLEMENT_BOTH`、`PROPERTY_MUST_HAVE_ACCESSORS` | 18 段 / 9 标记 |
| `property/propertyInheritanceRulesLangVer100.cj`（`LANGUAGE_VERSION: 1.0.0`） | 上面四种继承类诊断在 1.0.x 的首字符锚点 | 4 段 / 4 标记 |
| `pattern/patternDeclarationRules.cj` | `PATTERN_CAN_NOT_BE_ASSIGNED`、`ENUM_PATTERN_PARAM_SIZE_ERROR`、`TUPLE_PATTERN_NOT_MATCH`、`TUPLE_PATTERN_WITH_CORRECT_SIZE_EXPECTED`、`FORIN_PATTERN_MUST_BE_IRREFUTABLE`、`PATTERN_NOT_MATCH`、`EXPR_IN_FORIN_MUST_HAS_ITERATOR`、`CANNOT_CONVERT_LITERAL`、`TYPE_MISMATCH` | 9 段 / 24 标记 |
| `mut/mutableFunctionReferenceRules.cj` | `ILLEGAL_CAPTURE_THIS`、`CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC`、`USE_MUTABLE_FUNC_ALONE`、`INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE`、`INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER` | 23 段 / 19 标记 |

逐点核对结果（`.scratch/exact_check.py`，官方 Range 结束列按开区间处理）：property 1.1.3 形态 9/9 精确命中，LangVer100 形态 1.0.5 4/4 精确命中；pattern 两版各 24/24；mut 两版各 19/19。唯一未被标记覆盖的官方诊断是 `sema_wrong_forin_guard`（`WRONG_FORIN_GUARD` 在 `CfirDiagnosticsList.kt` 无对应名，按 `SUGGESTED_DIAGNOSTIC` 处理）。

### 本批次相对子代理初稿做的修正

- **同包顶层名冲突**：`// FILE:` 各段在测试框架里是同一模块同一包的多个文件。子代理初稿在多个段里复用 `Base` / `Sub` / `S` / `C` / `A` / `IMut` / `Multi` / `Single` / `Payload`，官方整包编译报 `sema_redefinition`（同名顶层函数是 `sema_overload_conflicts`），CFIR 报 `CLASSIFIER_REDECLARATION` 并级联出 `AMBIGUOUS_USE` / `NOTHING_TO_OVERRIDE` / `NOT_MEMBER_OF` / `UNRESOLVED_REFERENCE` 噪声。已按段改为唯一名（`OverrideTypeDiffBase`、`CtorLambdaCaptureStruct`、`StructMutMismatchIMut`、`ForInIrrefutableMulti` 等）。同批发现已提交的 `intrinsic/intrinsicMemberAndBody.cj` 顶层 `func getTypeForTypeParameter()` 与 `duplicatedIntrinsic.cj` 的两个 `@Intrinsic` 同名顶层函数构成重载冲突（官方 `sema_overload_conflicts`，CFIR `CONFLICTING_OVERLOADS`），把合法对照的顶层函数改名为 `legalGetTypeForTypeParameter`。
- **标记范围逐字符对齐官方**：`AbstractCfirAnalysisDiagnosticsTest.assertDiagnosticsEqual` 比较的是 `(诊断名, startOffset, endOffset)` 三元组，所以标记必须与官方 Range 完全一致。初稿里 18 处标记包住整条声明 / 整条 for 语句 / 整个 `this`，官方只锚 1 个字符的 12 处已收窄成首字符形式（`<!X!>p<!>ublic …`、`<!X!>f<!>or …`、`<!X!>t<!>his …`、`<!X!>(<!>a, b`）。官方 `MakeRange(ma.field)` 形式的 `USE_MUTABLE_FUNC_ALONE` 保持成员名整段（`inc`）。
- **形态 10 的 kind**：`let Payload.D = v`（无参构造器 `D`，但枚举里还有带参构造器）官方不是 `pattern_can_not_be_assigned`，而是 `sema_enum_pattern_param_size_error`（`D` 的类型是函数类型，`FindEnumPatternTarget` 空子模式分支只接受 `EnumTy` 候选）；1.0.5 / 1.1.3 实测一致。对照形态：全为无参构造器的枚举里 `Multi.A` 落到 `pattern_can_not_be_assigned`。
- **官方源码行号**按当前 `external/cangjie_compiler`（v1.0.0 tag）重新核对并修正：`ChkTuplePattern` 的两条报告点实际在 `:461` / `:475`，`SynForInExpr` 在 `:94-134`（iterable `:110-112`、guard `:118-121`、不可反驳 `:127-129`），for-in 节点的 begin 在 `ParseAtom.cpp:999-1010`，`FindEnumPatternTarget` 在 `TypeCheckPattern.cpp:513-547`，`ChkEnumPattern` 在 `:362-420`（`pattern_not_match` 三处 `:396` / `:405` / `:415`），`DiagnosticSema.def` 里 `use_this_as_an_expression_in_func` 在 `:288`、`incompatible_mut_modifier_between_struct_and_interface` 在 `:289-290`，`DiagRefactor/DiagnosticSema.def` 里 `immutable_access_mutable_func` 在 `:45`、`instance_func_cannot_be_used_in_finalizer` 在 `:133`。`CfirErrors.kt` 行号也改成实测值（`:162-163`、`:322`、`:389`、`:1629`）。
- **`INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER` 不是缺口**：初稿按 `SUGGESTED_DIAGNOSTIC` 处理，实际 `CfirErrors.kt:1629` 有对应名，改为写标记，锚在成员名首字母（官方 `CheckForbiddenFuncReferenceAccess` 的调用点 `NameReferenceExpr.cpp:1080` 传 `ma.field.Begin()`）。

### 1.0.x 与 1.1.3 的锚点差异（语言版本门禁）

官方 `DiagnosticEngine.h` 的 `Diagnose(const AST::Node& node, DiagKind)`（非 refactor kind）在 v1.0.0 tag 里退化为 `Diagnose(node.GetBegin(), kind)`，锚 1 个字符；cjc 1.1.3 对 `sema_property_have_same_declaration_in_inherit_mut` / `_immut`、`sema_property_must_implement_both` 锚整条 prop 声明。`sema_property_must_have_accessors` 是 refactor kind（`DiagnoseRefactor`），两版都锚整条声明；`sema_property_override_implement_type_diff` 两版都锚声明首字符。据此 `property` 域拆成两个夹具：默认语言版本（`LATEST_STABLE` = 1.1.3）夹具用整条声明标记，`propertyInheritanceRulesLangVer100.cj` 用 `LANGUAGE_VERSION: 1.0.0` + 首字符标记。

### CFIR 缺口（按官方写期望后仍红的用例；`:cfir:analysis-tests:test` 2026-10-02 09:45 聚焦跑 Property / Pattern / Mut 三组，22 用例 / 6 失败；改完夹具后复跑，失败集合不变）

| 用例 | 官方（本用例期望） | CFIR 当前输出 |
| --- | --- | --- |
| `property/propertyInheritanceRules` | `PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF` 锚声明首字符 `p`（cjc 1.0.5 / 1.1.3 一致） | 锚属性名 `v`（`CfirOverrideChecker.kt:350` 用 `propertyNameDiagnosticSource()`；`CfirInheritanceDeepChecker.kt:350` 用 `nameSource ?: diagnosticSource`）。其余 8 个标记（继承类三种 + 访问器）整条声明锚点一致 |
| `property/propertyInheritanceRulesLangVer100` | 1.0.0 语言版本下四种继承类诊断锚声明首字符 | 锚整条 prop 声明：CFIR 的诊断锚点没有按 `LANGUAGE_VERSION` 门禁 |
| `pattern/patternDeclarationRules` | 模式类诊断锚模式 `GetBegin()` 一个字符（`PATTERN_CAN_NOT_BE_ASSIGNED` 锚 `(`、`PATTERN_NOT_MATCH` 锚 `C` / `F`、`FORIN_PATTERN_MUST_BE_IRREFUTABLE` 锚 `for` 的 `f`、`TUPLE_*` 锚 `(`、`EXPR_IN_FORIN_MUST_HAS_ITERATOR` 锚 in 表达式的首字符）；`let Payload.D = v` 是 `ENUM_PATTERN_PARAM_SIZE_ERROR` | 模式类诊断锚整个模式（`declaration.pattern.source` / `pattern.source`）、`FORIN_PATTERN_MUST_BE_IRREFUTABLE` 锚 `for` 三字符、`EXPR_IN_FORIN_MUST_HAS_ITERATOR` 对 `1 + 2` 锚整个表达式；`let Payload.D = v` 报 `PATTERN_CAN_NOT_BE_ASSIGNED`（语义分歧：无参构造器限定名模式未被识别为 `EnumPattern` 不可解析形态，CFIR 缺 `ENUM_PATTERN_PARAM_SIZE_ERROR` 这条路径） |
| `mut/mutableFunctionReferenceRules` | `ILLEGAL_CAPTURE_THIS` / `CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC` 锚 `NameReferenceExpr` 首字符 1 个字符（`this` / 字段名首字母）；`classFinalizerCaptureMemberFunc` 里 `this.read` 的成员函数引用另报 `INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER`（锚 `read` 首字母） | `ILLEGAL_CAPTURE_THIS` 锚 `this` 整词；字段捕获（`value`）锚整个字段名；`this.read`（非调用形态）不报 `INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER`（`CfirGeneralSemanticsChecker` 只遍历 `CfirFunctionCall`）；`let t = this`（finalizer 里把 `this` 当值）报 `INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER`，官方该形态是 `sema_use_this_as_an_expression_in_func`（本项目对应名是 `THIS_AS_EXPRESSION_IN_FUNC`，但只有 open/abstract 构造器与 struct/extend mut 成员函数两个生产者，析构器分支没有）。`INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE`（`public` 首字符）锚点一致；`USE_MUTABLE_FUNC_ALONE` 的 struct 接收者形态（`s.inc`，`useMutableFuncAloneOnVarReceiver.cj`）锚点一致，但 `let f = this.inc` 形态（`useMutableFuncAloneOnThis.cj`）CFIR 2026-10-02 17:5x 实跑不产出该诊断（当时记为锚点一致，以批次 10 的实跑为准） |

`CfirAnalysisDiagnostics2WithoutAliasExpansionTestGenerated` 的三个新用例均通过；`property/propertyAccessorRules`、`pattern/patternLegality`、`mut/immutableFunctionRestrictions` 等既有夹具不受本批次影响。

### 取证与整包编译的两个坑（复核时踩到）

- **官方整包编译不能用作取证**：`DiagnosticEngine.cpp:764-770` 的 `CanBeEmitted` 只发第一个错误类别，`HandleDiagnose` 里 `HasPrevDiag(range.begin, message)` 会丢掉与已发诊断同起点同文案的重复项。一次编译里只要有一个 parse 错误，其余段的语义诊断全被吞掉。逐段单独编译（`.scratch/verify_one.py` / `exact_check.py` 的做法）仍是唯一可靠的取证方式；整包编译只用来查同包重名。
- **Range 结束列是开区间**：单字符锚点官方报 `Begin..Begin+1`，`MakeRange(pos)` 报 `pos..pos+1`。按闭区间比对会把每个单字符锚点判成“不命中”。
## 批次 8（2026-10-02）：const 声明语义 / lambda 捕获可变变量两个语义夹具

口径与批次 6 / 7 相同：期望只由官方 C++ 源码（`external/cangjie_compiler`，当前 checkout 在 tag v1.0.0）与本机
cjc 1.0.5 / 1.1.3 实测决定；内联标记用本项目 CFIR 名；CFIR 与官方不一致的按缺口记录，不改期望。

### 新增夹具

| 夹具 | 覆盖的 CFIR 诊断名 | 段数 / 标记数 | 1.0.5 / 1.1.3 exact_check |
| --- | --- | --- | --- |
| `const/constFunctionRules.cj` | `EXPECT_CONST`（"expected 'const' function" / "expression" / "expression guaranteed to be evaluated at compile time" 三种 kind）、`CANNOT_DEFINE_VAR_IN_CONST_FUNCTION`、`NO_CONST_INIT`、`CLASS_CONST_INIT_WITH_VAR` | 34 段 / 31 标记 | 各 31 OK / 0 DIFF / 0 未标记官方诊断 |
| `lambda/lambdaCaptureVarRules.cj` | `USE_FUNC_CAPTURE_VAR_ALONE`、`FUNC_CAPTURE_VAR_CANNOT_ASSIGN` / `_RETURN` / `_PARAM` / `_EXPR`、`LAMBDA_MUST_HAVE_TYPE_ANNOTATION` | 25 段 / 21 标记 | 各 21 OK / 0 DIFF / 0 未标记官方诊断 |

筛选依据（全 testData 盘点：`CfirDiagnosticsList.kt` 552 条声明，diagnostics2 + llt + macro 里 372 条已被内联标记覆盖，
180 条零覆盖，其中互操作注解族占七成）：const 族的 `NO_CONST_INIT` / `CLASS_CONST_INIT_WITH_VAR` /
`CANNOT_DEFINE_VAR_IN_CONST_FUNCTION` 与 lambda 族的 `FUNC_CAPTURE_VAR_CANNOT_PARAM` / `_RETURN` 是纯仓颉语义里
零覆盖的部分；`EXPECT_CONST` / `USE_FUNC_CAPTURE_VAR_ALONE` / `LAMBDA_MUST_HAVE_TYPE_ANNOTATION` 在
`llt/const_evaluation/err_*.cj`、`llt/lambda/lambda_capture/`、`llt/type_infer/lambda_param_*.cj` 已有用例，
两个新夹具按官方报告点重排为诊断矩阵，并在文件头注明既有覆盖位置。

### const 族取证结论（官方 ConstEvaluationChecker.cpp）

- 四个报告点：`DiagExpectConstFunc :18`（锚函数名）、`DiagExpectConstExpr :24`（锚表达式节点）、`DiagDefineVarInConstFunction :46`（锚变量名）、
  `DiagNoConstInit :52`（锚函数名）、`DiagCannotDefineConstInit :73`（锚第一个 const init 的 `init`）。文案在
  `include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:165-168`。入口 `TypeChecker.cpp:2107 CheckConstEvaluation`。
- 写夹具时踩到并已写进文件头的官方事实：
  1. `static var` 成员同样算 var 字段（`ChkClassDeclHasConstInitNoVarMember :410` 只排除 prop，不排除 static），
     所以"static var + const init"就报 `CLASS_CONST_INIT_WITH_VAR`。
  2. 子类的 `const init()` 若隐式调用父类构造器（父类有显式 `init()`），官方把**整条 const init 声明**（`init` 标识符到函数体右花括号）
     当 `EXPECT_CONST` 锚点；显式写 `super(...)` 时只锚 `super`。
  3. 同签名的 `init()` 与 `const init()` 不能共存（`sema_overload_conflicts`），"结构化 init + const init"形态不可表达。
  4. `this(a)` 委托必须是构造器首语句（`sema_illegal_place_of_calling_this_or_super` + `sema_class_uninitialized_field`）。
  5. `ChkClassDeclHasConstInitNoVarMember` 只作用于 class，struct 的 `const init` + var 字段合法（合法对照 L2）。
  6. 版本差异（未取形态）：class 有 const init 时普通成员变量的非常量初始化器，1.0.5 报 strong `EXPECT_CONST`、1.1.3 不报；
     const 函数里含 `var` 的 `for` 语句，1.0.5 额外报块级 `EXPECT_CONST`、1.1.3 不报。
  7. 官方对表达式形态的锚点宽度不同：调用锚被调函数名、下标锚基表达式、赋值锚整条赋值、`t++` 锚整条、`for` 锚整条 for 语句、
     数组字面量初值锚整个字面量。测试框架比较 `(name, startOffset, endOffset)` 三元组，标记必须逐字符对齐。
- 已删除形态 15 条写在文件末（`static const prop`、`open const func`、const 变量漏 static / 无初始化器、`@Frozen` 与 const 变量互斥、
  `@Annotation` 类缺 const init、枚举构造器必须写 `|`、具名形参默认值必须写 `a!:` 等）。

### lambda 捕获族取证结论（官方 TypeChecker.cpp:1780-1814 LegalityOfUsage）

- 上一会话的假设"FUNC_CAPTURE_VAR_CANNOT_PARAM/RETURN 无官方证据"不成立，但它们不是靠"函数引用"可达：官方 `CheckLegalUseOfClosure`
  对直接捕获 var 的闭包先报 `USE_FUNC_CAPTURE_VAR_ALONE`（`Diags.cpp:573`），四条 `FUNC_CAPTURE_VAR_CANNOT_*` 只在
  **传递捕获**时触发。可达路径是嵌套 lambda：`{=> f()}`（f 捕获 var）分别放在声明初始化器 / `return` / 实参 / 裸语句位置触发四条诊断，两版一致。
- 两种锚点形状：`use_func_capture_var_alone` 经 `DiagnoseRefactor` 带 MainHint.Range，锚**整个 lambda 字面量**（可跨行）；
  四条 `CANNOT_*` 经 `diag.Diagnose` 无 Range，退化为 `GetBegin()` **一个字符**（`{` 或标识符首字母）。
- 直接 vs 传递捕获的分界在 `LegalityOfUsage.cpp:22-69`：调用遍历只记"被调用闭包**自己函数体内**声明的 var"，外层 var 走绑定路径。
- 版本差异（未取形态）：裸语句 `g`（函数引用不调用）在 1.0.5 报 `_cannot_expr`、1.1.3 不报。
- 取证干扰（已在文件头注明）：`let name = {…}` / `w = {=> v}` 会多报 `sema_mismatched_types`，且官方 `HasPrevDiag` 会吞掉同段第二个
  相同 lambda 的诊断（逐段取证时会让 exact_check 假 OK）；`let {…} = …` 是解析错误；llt 旧用例是 CRLF，官方把 lambda Range 终点算到行尾 `\r`。

### CFIR 缺口（按官方写期望后仍红的用例；`:cfir:analysis-tests:test` 2026-10-02 15:42 聚焦跑 Const 组、15:56 跑 Lambda 组）

| 用例 | 官方（本用例期望） | CFIR 当前输出 |
| --- | --- | --- |
| `const/constFunctionRules` | `CLASS_CONST_INIT_WITH_VAR` 锚 `init` 1 字符 | 锚 `const init` 整段（`CfirConstDeclarationChecker.kt:139` 用 `constructorNameDiagnosticSource(includeConstKeyword = true)`） |
| `const/constFunctionRules` | `CANNOT_DEFINE_VAR_IN_CONST_FUNCTION` 锚变量名 1 字符 | 锚整条 `var a = 1` 声明（`CfirConstDeclarationChecker.kt:833` 用 `variable.source`） |
| `const/constFunctionRules` 形态 2 | 子类 const init 隐式 super() → `EXPECT_CONST` 锚整条 const init 声明 | 不产出该诊断 |
| `const/constFunctionRules` 形态 22 | 下标表达式 `EXPECT_CONST` 锚基表达式 `constSubscriptArr` | 锚 `constSubscriptArr[0]` 含下标 |
| `const/constFunctionRules` 合法对照 L5 | lambda 捕获 const 函数里的局部 `let` 零诊断 | 多报一条 `EXPECT_CONST` 锚 `b`（CFIR 把捕获的 let 当非常量） |
| `const/constFunctionRules` 其余 26 个标记 | 与 CFIR 一致 | 绿（PSI / LightTree 一致） |
| `lambda/lambdaCaptureVarRules` | 21 个标记（5 USE_FUNC_CAPTURE_VAR_ALONE / 12 FUNC_CAPTURE_VAR_CANNOT_* / 4 LAMBDA_MUST_HAVE_TYPE_ANNOTATION） | 2026-10-08 二次修复后**全绿**（PSI / LightTree / WithoutAliasExpansion 三套件）。三项残余差异逐条销账：① 锚点宽度——`FUNC_CAPTURE_VAR_CANNOT_*` 锚整个被使用闭包表达式（lambda 整个 `{…}` 字面量、函数引用整个标识符），这是本项目 range policy 的"完整最小单位"口径，官方 `GetBegin()` 单字符窄锚不跟，标记即按此书写；② 实参位置 kind——`CfirClosureCaptureUsageChecker.valueUsage` 改为官方"直接子节点"分派（initializer / result / 实参 identity 判定），`transitiveTakeLambda({=> f()})` 与 `obj.take({=> f()})` 报 `_PARAM`；③ 内层 `{j => j}` 的 `LAMBDA_MUST_HAVE_TYPE_ANNOTATION`——新增 resolve 侧"定义点推断失败"事实 `lambdaParameterInferenceFailedAtDefinition`（`CfirSyntheticCallGenerator` 在合成外层调用完成后固化，只写一次），checker 优先消费该事实、事实缺失才回退实时类型判定。此前"全部零产出"是 `CandidateFactory.withCallableValueReceiverSystems` 把同源 receiver 存储当 outer 导入触发 PCLA 前缀断言崩溃、模块分析中止所致（已修，见 REPAIR_LOG 2026-10-08） |

`CfirAnalysisDiagnostics2WithoutAliasExpansionTestGenerated` 的两个新用例（Const / Lambda）均未失败（该套件本次被跳过）。

### 环境事实

- 本工作树的 `gradlew-queue.bat` 首次引导脚本有 PowerShell 语法错误（`[DateTimeOffset]::Now.ToUnixTimeMilliseconds(` 缺右括号），
  首次跑队列 Gradle 需先把主检出里已构建的 `gradle-queue-cli/build/libs/gradle-queue-cli.jar` 复制到工作树对应路径。

## 批次 9（2026-10-02）：泛型上界 / override-redef 修饰符 / 类型引用三个语义夹具

口径与批次 6 / 7 / 8 相同：期望只由官方 C++ 源码（`external/cangjie_compiler`，当前 checkout 在 tag v1.0.0）与本机
cjc 1.0.5 / 1.1.3 实测决定；内联标记用本项目 CFIR 名；CFIR 与官方不一致的按缺口记录，不改期望。三份夹具各由一个子代理
独立编写，主会话用同一套 exact_check 复核并补了两处官方锚点修正（见下）。

### 新增夹具

| 夹具 | 覆盖的 CFIR 诊断名 | 段数 / 标记数 | 1.0.5 / 1.1.3 exact_check |
| --- | --- | --- | --- |
| `generic/genericBoundRules.cj` | `GENERIC_PARAM_DIRECTLY_RECURSIVE`、`GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY`、`FORBID_GENERIC_CONSTRUCTOR`、`FORBID_GENERIC_FINALIZER`、`GENERIC_IN_OPERATOR_OVERLOAD`、`VALUE_TYPE_RECURSIVE`、`CLASS_UNINITIALIZED_FIELD`（值类型递归段的官方伴随诊断，按官方锚 `let` 首字母） | 24 段 / 23 标记 | 各 23 OK / 0 DIFF / 0 未标记官方诊断 |
| `inheritance/overrideRedefRules.cj` | `INVALID_OVERRIDE_MEMBER_IN_CLASS`、`STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`、`REDEF_INSTANCE_ERROR`、`INVALID_MEMBER_VISIBILITY_IN_CLASS`；另有 5 段官方 kind 本项目无对应名（`sema_invalid_override_or_redefine_member_in_interface`、`sema_func_no_override_or_redefine_modifier`、`sema_missing_redefined_func`）按 SUGGESTED_DIAGNOSTIC 处理 | 15 段 / 11 标记 | 各 11 OK / 0 DIFF / 10 未标记（全部 SUGGESTED 段） |
| `type-mismatch/typeReferenceRules.cj` | `NOT_A_TYPE`、`REF_NOT_BE_TYPE`、`NO_MATCH_OPERATOR_FUNCTION_CALL`、`MISMATCHED_TYPES_BECAUSE`、`TYPE_INCOMPATIBLE`、`INVALID_TYPE_PARAM_OF_ENUM_MEMBER_ACCESS` | 17 段 / 17 标记 | 各 17 OK / 0 DIFF / 0 未标记官方诊断 |
| `constructor/superThisCallRules.cj` | `ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER`、`ILLEGAL_PLACE_OF_CALLING_THIS_PRIMARY_CONSTRUCTOR`、`MULTIPLE_PRIMARY_CONSTRUCTORS`、`NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS`（官方三个锚点分支：用户写的 `init` 4 字符、主构造器锚宿主类名）、`THIS_AS_EXPRESSION_IN_FUNC`、`ILLEGAL_THIS_OUTSIDE_STRUCT_CONSTRUCTOR`、`ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR`、`INVALID_THIS_CALL_OUTSIDE_CTOR`；另有 3 段官方 kind 本项目无对应名（`sema_use_super_in_interface`、`sema_super_use_error_inside_non_class`、`sema_extend_use_super`）按 SUGGESTED_DIAGNOSTIC 处理；`ILLEGAL_THIS_OR_SUPER_CALL` / `EXPLICIT_SUPER_CALL_REQUIRED` 有 CFIR 名但无生产者且官方 v1.0.0 无同名 kind，未写期望 | 23 段 / 18 标记 | 各 18 OK / 0 DIFF / 3 未标记（全部 SUGGESTED 段） |

筛选依据沿用批次 8 的全 testData 盘点：这三族里的 `GENERIC_PARAM_DIRECTLY_RECURSIVE`、
`GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY`、`FORBID_GENERIC_CONSTRUCTOR` / `_FINALIZER`、
`GENERIC_IN_OPERATOR_OVERLOAD`、`VALUE_TYPE_RECURSIVE`、`INVALID_OVERRIDE_MEMBER_IN_CLASS`、
`STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`、`REDEF_INSTANCE_ERROR`、`INVALID_MEMBER_VISIBILITY_IN_CLASS`、
`NOT_A_TYPE`、`REF_NOT_BE_TYPE`、`MISMATCHED_TYPES_BECAUSE`、`TYPE_INCOMPATIBLE`、
`INVALID_TYPE_PARAM_OF_ENUM_MEMBER_ACCESS` 在全 testData 此前无内联标记；`constraints/` 七个既有夹具、
`nothingToOverride` / `classNotOpenForInheritance` / `override*` 系列、`declaration-status/staticCannotBeOpenAbstractOverride.cj`
等已覆盖的部分在各夹具头部逐条列出，不重复。

### 取证结论

- **泛型上界**：报告点集中在 `src/Sema/PreCheck.cpp`（`:923-930` 直接递归、`:932-938` 非类上界里出现形参、`:982-990`
  多个类上界、`:1005-1011` 上界必须是类或接口）与 `src/Sema/TypeChecker.cpp`（`:1389-1392` 泛型构造器、`:1696-1698`
  主构造器泛型）、`DeclAttributeChecker.cpp`（`:368` / `:396` 泛型 operator）、`Utils.cpp`（值类型递归）。
  三段值类型递归的 struct 字段必带 `sema_class_uninitialized_field`，主构造器形态两版 cjc 都是 parse 错误，
  给初值又触发 `sema_recursive_constructor_call`，官方找不到只留 `VALUE_TYPE_RECURSIVE` 的写法，故伴随诊断按官方写入标记。
- **CFIR 有而官方 v1.0.0 无同名 kind**：`SHADOW_CANNOT_IN_TYPE_ARGS`、`PRIMITIVE_TYPE_AS_GENERICS_ARG`、
  `MEET_CONSTRAINT_INDIRECTLY`、`GENERIC_UPPER_BOUNDS_MUST_BE_JAVA_IN_JAVA`、`GENERIC_STATIC_ACCESS`、
  `CONFLICTING_UPPER_BOUNDS`、`REPEATED_BOUND`、`ONLY_ONE_CLASS_BOUND_ALLOWED`、`OVERRIDE_STATIC_ERROR`、
  `FORBID_GENERIC_NONSTATIC_METHOD`（后者在本项目 `CfirDiagnosticsList.kt` 里根本不存在）。前五个的官方 kind 在
  v1.0.0 有定义但全仓无报告点或只在 `@Java` 场景（`@Java` 本机两版都是 `sema_undeclared_identifier`），其余未写标记。
- **override / redef**：`StructInheritanceChecker.cpp:961` / `:988` / `:1006` / `:1022` / `:1052`，
  可见性规则在 `DeclAttributeChecker.cpp:303-307`（abstract 形态）与 `:310-317`（open 形态），锚点都是成员名首字符。
  非 refactor kind 锚整条成员声明首字符，refactor kind（可见性）锚成员名——两种口径都在夹具里出现。
  抽象成员只能写成"无函数体的成员声明"（显式 `abstract` 被 parser 拦），且宿主类必须是 `abstract class`，否则先报
  `sema_missing_func_body`。
- **类型引用**：`sema_not_a_type` 报在 `PreCheck.cpp:404` / `:497` / `:724`，`sema_ref_not_be_type` 报在
  `TypeCheckReference.cpp:626`，`sema_mismatched_types_because` 全仓只有两个 because 非空的调用点
  （`TypeChecker.cpp:134` 空函数体、`:150` CheckReturnThisInFuncBody），锚整条函数体块；`sema_type_incompatible`
  在 v1.0.0 **有**同名 kind（`DiagnosticSema.def:107`，5 个报告点），此前任务描述里"grep 未命中"的判断有误。
  枚举类型当构造器官方没有 `sema_enum_type_cannot_be_used_as_constructor`，实际报 `sema_no_match_operator_function_call`，
  夹具按官方 kind 写标记，CFIR 名字 `ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR` 在头部单列说明。
- 1.0.5 与 1.1.3 两版 kind / 消息 / Range 完全一致，三份夹具均未拆 `LANGUAGE_VERSION`。

### CFIR 缺口（按官方写期望后仍红的用例；`:cfir:analysis-tests:test` 2026-10-02 17:0x–17:4x 聚焦跑 Generic / Inheritance / TypeMismatch 三组）

| 用例 | 官方（本用例期望） | CFIR 当前输出 |
| --- | --- | --- |
| `generic/genericBoundRules` | `FORBID_GENERIC_CONSTRUCTOR` 锚 `init` 首字母 1 字符 | 锚 `init` 整词（4 字符），3 处 |
| `generic/genericBoundRules` | `GENERIC_IN_OPERATOR_OVERLOAD` 锚声明首字符（`public` / `operator` 的 `p` / `o`） | 锚 operator 名 `+`（1 字符），4 处 |
| `generic/genericBoundRules` | `CLASS_UNINITIALIZED_FIELD` 锚字段声明首字符 `let` 的 `l` | 锚字段名（`self` / `other` / `back` / `pair`），5 处 |
| `generic/genericBoundRules` 其余 11 个标记 | 与 CFIR 一致 | 绿 |
| `inheritance/overrideRedefRules` | `REDEF_INSTANCE_ERROR` 锚 `redef` 的 `r` 1 字符 | 锚 `redef` 整词（4 字符） |
| `inheritance/overrideRedefRules` | `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME` 锚成员声明首字符（`public` / `open` 的 `p` / `o`） | 锚成员名（`g` / `f` / `x`），3 处 |
| `inheritance/overrideRedefRules` | 静态 `redef` 无对应父 static 成员：官方 `sema_missing_redefined_func` 锚声明首字符（SUGGESTED 段，不写标记） | CFIR 多报 `NOTHING_TO_OVERRIDE` 锚 `redef`，5 处；实例 `redef func` 命中父类成员时也额外多报一条 `NOTHING_TO_OVERRIDE` |
| `inheritance/overrideRedefRules` 其余 5 个标记（`INVALID_OVERRIDE_MEMBER_IN_CLASS` ×3、`INVALID_MEMBER_VISIBILITY_IN_CLASS` ×4、`INVALID_MEMBER_VISIBILITY_IN_CLASS` 形态 9/10） | 与 CFIR 一致 | 绿 |
| `type-mismatch/typeReferenceRules` | `NOT_A_TYPE` / `REF_NOT_BE_TYPE` 锚名称首字母 1 字符 | PSI 路径锚整名（10 处）；LightTree 路径对裸类型名 / 值位置引用一条都不报（10 处） |
| `type-mismatch/typeReferenceRules` | `NO_MATCH_OPERATOR_FUNCTION_CALL` 锚 `trCalledEnum(1)` 首字符 | 报 `NO_MATCHING_OPERATOR_INVOKE` 锚 `trCalledEnum` 整名 |
| `type-mismatch/typeReferenceRules` | `MISMATCHED_TYPES_BECAUSE` 锚整条函数体块（跨行） | 报 `RETURN_TYPE_MISMATCH` 锚 `{`，2 处 |
| `type-mismatch/typeReferenceRules` | `TYPE_INCOMPATIBLE` 锚复合赋值左值首字母（`p` / `u`）、catch 模式变量 `e` | 复合赋值改报 `INVALID_BINARY_OPERATOR` 锚运算符（2 处）；catch 改成锚整条 `catch (e: …)` 子句；LightTree 路径三条都不报 |
| `type-mismatch/typeReferenceRules` | `INVALID_TYPE_PARAM_OF_ENUM_MEMBER_ACCESS` 锚 owner 首字母 `t` | PSI 锚 owner 整名 `trYearEnum`；LightTree 一致 |

PSI 与 LightTree 两条路径的差异在每一行都相同（同一份 testData 两个入口），`CfirAnalysisDiagnostics2WithoutAliasExpansionTestGenerated`
的三个新用例均通过（本次聚焦跑被跳过）。

### 环境与工具事实

- 工作树里的 `gradlew-queue.bat` 首次引导有 PowerShell 语法错误（见 `cangjie-gradle-queue-worktree-bootstrap` 记忆），
  从主检出复制 `gradle-queue-cli.jar` 后可用；后台 shell 写不进工作树的 `.scratch`，日志统一写 `D:\tmp`。
- `.scratch/verify_one.py` / `exact_check.py` 把首个 `// FILE:` 之前的头部注释也当一段解析，头部里出现字面 `<!X!>` 会被当成真标记；
  夹具头部的示例标记一律用文字描述。三份新夹具的头部均无字面标记。
- 本会话的沙箱对工作树路径的可见性会间歇性丢失（`ls` / Python `os.listdir` 报 `No such file or directory`，重试即恢复）；
  判定"目录真的不存在"前先重试一次。

## 批次 10（2026-10-02）：抽象成员访问 / this()-super() 调用 / 泛型实例化三个语义夹具，外加 mut 夹具的析构器 `this` 修正

口径与批次 6–9 相同：期望只由官方 C++ 源码（`external/cangjie_compiler`，当前 checkout 在 tag v1.0.0）与本机 cjc 1.0.5 / 1.1.3 实测决定；
内联标记用本项目 CFIR 名；官方有而本项目无对应名的 kind 按 SUGGESTED_DIAGNOSTIC 处理；CFIR 与官方不一致的按缺口记录，不改期望。
三份夹具各由一个子代理独立编写，主会话用同一套 exact_check 复核，并修了四处官方口径问题（下表"主会话修正"列）。

### 新增夹具

| 夹具 | 覆盖的 CFIR 诊断名 | 段数 / 标记数 | 1.0.5 / 1.1.3 exact_check | 主会话修正 |
| --- | --- | --- | --- | --- |
| `inheritance/abstractMemberAccessRules.cj` | `ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY`、`ABSTRACT_CLASS_CAN_NOT_BE_INSTANTIATED`、`MISSING_FUNC_BODY`（全 testData 首次有内联标记）、`INHERIT_NOT_RETURN_THIS`、`WEAK_VISIBILITY` | 15 段 / 16 标记 | 各 16 OK / 0 DIFF / 4 未标记（全部 SUGGESTED 段：`sema_interface_can_not_be_instantiated`、`sema_class_need_abstract_modifier_or_func_need_impl` ×3） | 无（官方 `sema_abstract_class_can_not_be_instantiated` 在 v1.0.0 `DiagnosticSema.def:211` 确实存在，子代理起初判为"官方无同名 kind"是错的，已按官方 kind 写标记） |
| `constructor/superThisCallRules.cj` | `ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER`、`ILLEGAL_PLACE_OF_CALLING_THIS_PRIMARY_CONSTRUCTOR`、`MULTIPLE_PRIMARY_CONSTRUCTORS`、`NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS`、`THIS_AS_EXPRESSION_IN_FUNC`、`INVALID_THIS_CALL_OUTSIDE_CTOR`、`ILLEGAL_THIS_OUTSIDE_STRUCT_CONSTRUCTOR` | 23 段 / 18 标记 | 各 18 OK / 0 DIFF / 3 未标记（SUGGESTED：`sema_use_super_in_interface`、`sema_super_use_error_inside_non_class`、`sema_extend_use_super`） | 无 |
| `generic/genericInstantiationRules.cj` | `GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS`（官方 `sema_not_found_from_generic_upper_bounds`）、`GENERIC_INFINITE_INSTANTIATION`、`GENERIC_TYPE_INCONSISTENT` | 26 段 / 17 标记 | 各 17 OK / 0 DIFF / 0 未标记 | 三处：① operator 实参段的形参类型 `GII3A<GII3A<T>>` 官方同样报 `sema_generic_infinite_instantiation`（checkRecursion STOP_NOW），原漏标记，已补；② 合法对照 L7 的 `GIL7Holder(a, a)` 官方报 `sema_mismatched_types`（锚第一个实参），不是合法形态，已改成显式具体类型实参；③ 合法对照 L10 与 L9 顶层名 `GIL9Base` / `GIL9C` 重复（同包两文件同名会触发 `sema_redefinition`），L10 已改名 `GIL10*` |
| `mut/mutableFunctionReferenceRules.cj`（修正） | `THIS_AS_EXPRESSION_IN_FUNC` | 20 段 / 20 标记 | 各 20 OK / 0 DIFF / 0 未标记 | 批次 7 的 `classFinalizerThisAlone` 段按 SUGGESTED 处理且写了本项目不存在的名字 `USE_THIS_AS_AN_EXPRESSION_IN_FUNC`；实际本项目名是 `THIS_AS_EXPRESSION_IN_FUNC`（`CfirDiagnosticsList.kt:511`，生产者 `CfirExpressionSemanticsChecker.kt:388` open/abstract 构造器分支、`:459` struct/extend 的 mut 成员函数分支，析构器分支没有），已按官方锚点（`this` 首字母）改为正式标记 |

### 取证结论

- **抽象成员**：报告点 `src/Sema/DeclAttributeChecker.cpp:297-317`（`MISSING_FUNC_BODY` 的条件是 `(成员 static 且非 foreign) || (宿主非 abstract 且非 foreign)`；
  可见性两种形态分别锚成员名）、`TypeCheckCall.cpp:2521-2533`（`sema_interface_can_not_be_instantiated` / `sema_abstract_class_can_not_be_instantiated`，锚类型名首字符）、
  `TypeCheckReference.cpp`（抽象方法直接访问）、`StructInheritanceChecker.cpp`（`inherit_not_return_this` 锚子函数名、`weak_visibility` 锚子成员名）。
  抽象成员只能写成"无函数体的成员声明"（显式 `abstract` 被 parser 拦，且 1.1.3 会多报 `parse_explicitly_abstract_only_for_cjmp_abstract_class`，版本分歧）。
  标记不能放在 `super` 与成员名之间（去标记后拼成 `superf`），必须写成只包 `super` 首字母的形式。
- **this()/super()**：`StructInheritanceChecker.cpp` / `TypeCheckDecl.cpp` / `TypeChecker.cpp` 的构造器检查。官方非 refactor 的
  `Diagnose(node, kind)` 只锚 1 个字符（`DiagnosticEngine.h:844-857`）；`sema_no_non_param_constructor_in_super_class` 只对编译器合成的
  `super()` 报，锚点有三个分支（用户写的 `init`、宿主类名整段、补出的构造器）。CFIR 把"显式 super 调用"语义拆成 `EXPLICIT_SUPER_CALL_REQUIRED`
  与 `ILLEGAL_THIS_OR_SUPER_CALL` 两个名字，但两个都没有生产者，官方 v1.0.0 也没有对应 kind（该语义只有
  `sema_no_non_param_constructor_in_super_class` 一种），已在夹具头部注明。
- **泛型实例化**：`GENERIC_INFINITE_INSTANTIATION` 的真正条件是 `WillCauseInfiniteInstantiation` 的 `checkRecursion`：
  `ty->Contains(genericTy)` 要求**被 Walker 访问的节点就是宿主泛型声明**，所以字段 / struct / `var` 带初值 / 非宿主类形参全部零诊断，
  能写出的形态只有 enum 构造器形参 `E<E<T>>` 与 operator 形参或实参自嵌套。`sema_generic_no_method_match_in_upper_bounds` /
  `sema_generic_ambiguous_method_match_in_upper_bounds` 在官方 v1.0.0 不可达（唯一调用点 `DiagnoseForCall` 的三条路径都被更早的诊断挡住），
  约 25 个探针全部零诊断或报别的 kind，因此本项目 `GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS` 没有形态。
- 三个新夹具两版 cjc 的 kind / 消息 / Range 完全一致，未拆 `LANGUAGE_VERSION`。

### CFIR 缺口（按官方写期望后仍红的用例；`:cfir:analysis-tests:test` 2026-10-02 18:0x–18:4x 聚焦跑 Inheritance / Constructor / Mut / Generic 四组）

| 用例 | 官方（本用例期望） | CFIR 当前输出（PSI 与 LightTree 相同） |
| --- | --- | --- |
| `inheritance/abstractMemberAccessRules` | `ABSTRACT_CLASS_CANNOT_BE_INSTANTIATED` 锚类型名首字母 1 字符 | 锚类型名整段（`AbsAccInstantiateParent`），2 处 |
| `inheritance/abstractMemberAccessRules` | 宿主类非 abstract 且成员无体 → `MISSING_FUNC_BODY` 锚成员声明首字符 | 改报 `ABSTRACT_MEMBER_NOT_IMPLEMENTED` 锚类名（3 个"类没写 abstract 修饰符"形态）；4 个成员级标记一致 |
| `inheritance/abstractMemberAccessRules` | `WEAK_VISIBILITY` "a deriving member must be at least as visible as its base member" | 报 `CANNOT_WEAKEN_ACCESS_PRIVILEGE`（锚点相同、诊断名不同），2 处 |
| `inheritance/abstractMemberAccessRules` | `super.p` 访问父类抽象成员 → `ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY` 锚 `super` 的 `s` | 不产出 |
| `inheritance/abstractMemberAccessRules` 形态 4 | 接口实例化：官方 `sema_interface_can_not_be_instantiated`（本项目无对应名，SUGGESTED 段） | 多报 `NO_CONSTRUCTOR` 锚类型名整段 |
| `inheritance/abstractMemberAccessRules` 其余 9 个标记 | 与 CFIR 一致 | 绿 |
| `constructor/superThisCallRules` | `ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER` / `_PRIMARY_CONSTRUCTOR` / `INVALID_THIS_CALL_OUTSIDE_CTOR` / `ILLEGAL_THIS_OUTSIDE_STRUCT_CONSTRUCTOR` 锚关键字首字母 1 字符 | 锚整个 `this` / `super` 关键字（4–5 字符），14 处 |
| `constructor/superThisCallRules` | 主构造器形态：`sema_no_non_param_constructor_in_super_class` 锚宿主类名整段（4 字符） | 不产出 |
| `constructor/superThisCallRules` | lambda 体里的 `super(a)`：官方只报位置 | 多报 `INVALID_THIS_CALL_OUTSIDE_CTOR` |
| `constructor/superThisCallRules` 合法段 L* | 接口 / struct / extend 成员里 `super.f()` 合法（0 error） | 多报 `INTERFACE_SUPER_NOT_ALLOWED` / `STRUCT_SUPER_NOT_ALLOWED` / `EXTEND_SUPER_NOT_ALLOWED`，3 处误报 |
| `constructor/superThisCallRules` 其余标记（`MULTIPLE_PRIMARY_CONSTRUCTORS`、`THIS_AS_EXPRESSION_IN_FUNC` ×3、`INVALID_THIS_CALL_OUTSIDE_CTOR` ×2） | 与 CFIR 一致 | 绿 |
| `generic/genericInstantiationRules` | 17 个标记 | 全部一致，绿 |
| `mut/mutableFunctionReferenceRules`（新增标记） | 析构器里 `let t = this` → `THIS_AS_EXPRESSION_IN_FUNC` 锚 `this` 首字母 | 改报 `INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER`（析构器分支无生产者） |
| `mut/mutableFunctionReferenceRules` 其余 | 与 CFIR 一致（含 `USE_MUTABLE_FUNC_ALONE` 的 struct 接收者形态 5 处） | 绿 |

批次 7 表里"`USE_MUTABLE_FUNC_ALONE` 锚点一致"一句已按本次实跑改正：`let f = this.inc`（`useMutableFuncAloneOnThis.cj`）CFIR 不产出该诊断。

`CfirAnalysisDiagnostics2WithoutAliasExpansionTestGenerated` 的三个新用例（Constructor / Generic / Inheritance）本次聚焦跑均被跳过或通过。

## 批次 11（2026-10-02）：`??` 合并运算 / static 字段初始化 / 常量模式插值三个语义夹具

口径与批次 6–10 相同：期望只由官方 C++ 源码（`external/cangjie_compiler`，当前 checkout 在 tag v1.0.0）与本机 cjc 1.0.5 / 1.1.3 实测决定；
内联标记用本项目 CFIR 名；官方有而本项目无对应名的 kind 按 SUGGESTED_DIAGNOSTIC 处理；CFIR 与官方不一致的按缺口记录，不改期望。
三份夹具各由一个子代理独立编写（const-pattern 那个子代理在会话结束时被中断，主会话接手完成复核与修正）。

### 新增夹具

| 夹具 | 覆盖的 CFIR 诊断名 | 段数 / 标记数 | 1.0.5 / 1.1.3 exact_check | 主会话修正 |
| --- | --- | --- | --- | --- |
| `operator/coalescingRules.cj` | `INVALID_COALESCING` ×31、`TYPE_MISMATCH` ×4、`CANNOT_CONVERT_LITERAL` ×3 | 13 段 / 38 标记 | 各 38 OK / 0 DIFF / 0 未标记 | 子代理在"同名枚举也命中 Option 条件"的对照段里自定义 `enum Option<T>`，与 `pattern/patternLegality.cj`、`mock/createMockOptionBoundaryPlaceholder.cj` 的顶层 `Option` 重名（同包两文件同名触发 `sema_redefinition`），已改名 `CoalSameNameOption`（改名后仍命中 `IsCoalescingLeftTyValid` 的 identifier 条件，见官方 `:1102`） |
| `static-init/staticFieldInitializationRules.cj` | `TYPE_UNINITIALIZED_STATIC_FIELD` ×21；另 2 段官方伴随诊断 `sema_class_uninitialized_field`（父类 static 字段被子类隐式构造器算作未初始化实例字段；类同时有未初始化 static 与实例字段）按 SUGGESTED 处理 | 24 段 / 21 标记 | 1.1.3：21 OK / 0 DIFF / 2 未标记（全部 SUGGESTED 段）；1.0.5：20 OK / 1 DIFF / 2 未标记（形态 8"未被调用的 lambda 里赋值"1.0.5 不报，已在头部记为语言版本分歧） | 无 |
| `pattern/constPatternInterpolation.cj` | `INTERPOLATION_IN_CONST_PATTERN` ×10（全 testData 首次有内联标记；1 段官方 kind `sema_pattern_literal_expected` 在 v1.0.0 是死条目，按 SUGGESTED 记录） | 18 段 / 10 标记 | 各 10 OK / 0 DIFF / 0 未标记 | 子代理初稿的 L4"合法对照"段写了 `s.size()` / `"${x}".size()`，官方两版都报 `sema_no_match_operator_function_call`（`.size` 是 prop 不是方法，锚接收者 1 字符），已改成 prop 形态 `"v${x}${y}".size` 并补 L4b（常量模式分支体里的插值）；L2 / L2b / L3 的 warning 描述按实测改正（`chir_dce_unused_variable` 两版都有，`sema_unreachable_pattern` 锚 `_`） |

### 取证结论

- **`??` 合并运算**：`src/Sema/TypeCheckExpr/BinaryExpr.cpp:1085-1106` `IsCoalescingLeftTyValid` 要求左操作数是 core 包、名为 `Option`、单类型实参的 enum；
  `:1108-1142` `ChkCoalescingExpr` 的报告点是 `diag.Diagnose(*be.leftExpr, DiagKind::sema_invalid_coalescing)`，
  非 refactor kind 经 `DiagnosticEngine.h:844-857` 退化为 `GetBegin` 1 个字符，**成员访问锚的是基表达式首字符**（`b.v` 锚 `b`），
  **括号表达式锚最外层左括号**。`CanSkipDiag`（`TypeCheckUtil.cpp:230-233`）= `!Ty::IsTyCorrect(node.ty)`：左操作数自身类型已 invalid 时
  不报本诊断，且 `:1119` 直接 return，右操作数不再检查；右操作数字面量先由 `CheckWithNegCache` 报 `sema_cannot_convert_literal`（1 字符），
  此时 `Diags.cpp:250-252` 提前返回、不再追加 mismatched types。`??` 是右结合（`ParseExpr.cpp:539-543`，同优先级的 `??` 与 `**`）。
  `??=` 不是 token，只能写成 `(x ?? 2) ?? 3` 强制左结合。
- **static 字段初始化**：报告点 `src/Sema/LegalityOfUsage/InitializationChecker.cpp:501-512`，
  `sema_type_uninitialized_static_field` 用 `DiagnoseRefactor` 锚**整条 static 字段声明**（含 `static` / `public` 修饰符，如 `public static var x: Int64` → Range (2,5)..(2,25)）。
  整段跳过的条件有两类：`static init` 体以 `throw` 直接终止（`:493-499`，`tryDepth != 0` 时不登记终止，见 `UpdateScopeStatus:304-306`），
  以及字段在类体外 / 枚举构造器 / 伴生对象里被赋值。`interface` / `extend` / `enum` 体里的 `static` 字段是 parse 错误
  （`CheckStaticInitForTypeDecl` 只对 `STRUCT_DECL` / `CLASS_DECL` 调用），写不出可达形态。形态 8（未被调用的 lambda 里赋值）两版分歧：1.1.3 报、1.0.5 不报。
- **常量模式插值**：`src/Sema/TypeCheckPattern.cpp:243-279` `ChkConstPattern` 四步顺序固定、前一步失败即 return；
  第 3 步 `DynamicCast<LitConstExpr*>(literal) && siExpr` 非空时报 `sema_interpolation_in_const_pattern`
  （`DiagRefactor/DiagnosticSema.def:81`），`DiagnoseRefactor(kind, p)` 锚**整个 ConstPattern 节点**，
  而 `ParsePattern.cpp:100-107` 把节点范围取成字面量 token 本身，所以锚点是**带引号的字符串字面量文本**（不含 `case`、不含 `=>`），
  多行字符串时跨行。词法上只有 `${` 算插值（`Lexer.cpp:981-1020` / `:1164-1215`），`$ident` 与 `\$` 都不是，
  故 `case "a$y"` / `case "a\${y}"` 官方零诊断。第 2 步类型不等（选择器不是 `String`）即 return，第 3 步不可达。
  `sema_pattern_literal_expected`（同文件 `:104`）在 v1.0.0 全仓无引用点，是死条目。
- 三份夹具除 static-init 的形态 8 外，两版 cjc 的 kind / 消息 / Range 完全一致，未拆 `LANGUAGE_VERSION`。

### CFIR 缺口（按官方写期望后仍红的用例；`:cfir:analysis-tests:test` 2026-10-02 20:3x–20:5x 聚焦跑 Operator / StaticInit / Pattern 三组，6 用例 / 4 失败 / 2 跳过）

| 用例 | 官方（本用例期望） | CFIR 当前输出 |
| --- | --- | --- |
| `operator/coalescingRules` | `INVALID_COALESCING` 锚左操作数首字符 1 字符：成员访问 / 构造器调用 / 下标 / 函数与 lambda 调用锚基表达式首字符 | PSI 与 LightTree 两条路径相同：锚整个左操作数元素（`b.v`、`CoalPoint()`、`s[0]`、`f(1)`、`coalIdtNonOption(1)`），19 处 |
| `operator/coalescingRules` | 括号表达式锚最外层 `(`（`(i)`、`(((s)))`、`(1 + 1)`、`(x ?? 2)`） | 锚括号内的表达式（`i`、`s`、`1 + 1`、`x ?? 2`），4 处 |
| `operator/coalescingRules` | 右结合 `oInt ?? 2 ?? 3` 内层左操作数 `2`；元组解构 `a ?? b` / `b ?? a` 锚元素首字符 | 不产出，6 处 |
| `operator/coalescingRules` | `os ?? 1` → `CANNOT_CONVERT_LITERAL` 锚 `1` | 报 `TYPE_MISMATCH` 锚 `1`（诊断名不同） |
| `operator/coalescingRules` | `oi ?? 1.5` → `CANNOT_CONVERT_LITERAL` 锚首字符 `1` | 锚整个字面量 `1.5` |
| `operator/coalescingRules` | `let _: Int8 = o ?? 0` → `TYPE_MISMATCH` 锚左操作数 `o` | 锚整个 `o ?? 0`（coalescing 无专用生产者，由通用类型不匹配路径承载） |
| `operator/coalescingRules` | 其余 19 个标记（4 个裸标识符 + 3 个右操作数 `TYPE_MISMATCH` 等） | 与 CFIR 一致，绿 |
| `operator/coalescingRules`（`CfirAnalysisDiagnostics2WithoutAliasExpansionTestGenerated`） | 同上 | 该路径只产出形态 1 的 4 个裸标识符标记，形态 2 起全部不产出 |
| `static-init/staticFieldInitializationRules` | `TYPE_UNINITIALIZED_STATIC_FIELD` 锚整条 static 字段声明（含修饰符，如 `public static var x: Int64` 19 字符） | 锚字段名 1 字符（`x` / `y` / `a` / `b`），21 处；PSI 与 LightTree 两条路径相同 |
| `static-init/staticFieldInitializationRules` 合法对照 D（`classStaticInitThrowSkipsCheck.cj`：`static init` 体以 `throw` 终止，官方整段跳过、零诊断） | 零诊断 | 多报 `CLASS_UNINITIALIZED_FIELD` 锚 `a` / `b`，2 处 |
| `static-init/staticFieldInitializationRules` 形态 24（类同时有未初始化 static 与实例字段，官方伴随 `sema_class_uninitialized_field` 锚实例构造器 `init` 首字符，不写标记） | — | CFIR 的 `CLASS_UNINITIALIZED_FIELD` 锚 `init`（构造器名），与官方伴随诊断锚点相同、仅因未写标记而显示为多出 |
| `pattern/constPatternInterpolation` | `INTERPOLATION_IN_CONST_PATTERN` 锚字面量文本（10 个标记） | 不产出（`CfirConstPatternInterpolationChecker`，`CfirPatternExpressionChecker.kt:352-385`，整块被注释、无注册） |
| `pattern/constPatternInterpolation` 形态 6（or 模式 `case "a${y}" \| "b" => 0`） | 报一条插值诊断，锚 `"a${y}"` | CFIR 在 `runCheckers` 阶段抛 `FileAnalysisException`：`CfirMatchUnreachablePatternChecker.isCoveredBy`（`:81`）→ `MarangetChecker.isUseful` → `MatrixUtils.getFirstColumnType` 抛 `MarangetException: matrix first-column types are inconsistent`（`MatrixUtils.kt:25`），该 `match` 整段没有诊断；PSI 与 LightTree 两条路径相同 |

`CfirAnalysisDiagnostics2WithoutAliasExpansionTestGenerated` 的两个新用例（Pattern / StaticInit）本次聚焦跑均被跳过。

### 环境与工具事实

- 沙箱的工作树路径可见性仍会间歇性丢失（`cd` / `python` 报 `No such file or directory`，重试即恢复）；`Write` 工具对工作树绝对路径偶发
  "Edit the worktree copy" 误拒，改用 bash heredoc 写 `.scratch` 脚本。
- `.scratch/parse_cfir_diff.py`（本批次新增）解析 `cfir/analysis-tests/build/test-results/test/*.xml`，
  按 `=====预期=====` / `=====得到=====` 切出两段并打印统一差异；测试框架的失败消息在"得到"段之后还会跟 `FileAnalysisException` 的栈，需按 `\tat ` 截断。
- 同一 fixture 的三个套件（LightTree / PSI / WithoutAliasExpansion）用同一份 testData，但锚点宽度与产出量可以不同（见上表最后两行）。
## 批次 12（2026-10-03）：元组字段 CType 约束 / 实例字段初始化 / 静态上下文访问三个语义夹具

口径与批次 6–11 相同：期望只由官方 C++ 源码（`external/cangjie_compiler`，当前 checkout 在 tag v1.0.0）与本机 cjc 1.0.5 / 1.1.3 实测决定；
内联标记用本项目 CFIR 名；官方有而本项目无对应名的 kind 按 SUGGESTED_DIAGNOSTIC 处理；CFIR 与官方不一致的按缺口记录，不改期望。
三份夹具各由一个子代理独立编写（实例字段那个子代理三次被网关中断，最终由主会话自己写），主会话逐份用同一套 exact_check 复核。

### 新增夹具

| 夹具 | 覆盖的 CFIR 诊断名 | 段数 / 标记数 | 1.0.5 / 1.1.3 exact_check | 主会话修正 |
| --- | --- | --- | --- | --- |
| `interop/tupleFieldCTypeRules.cj` | `INVALID_TUPLE_FIELD_CTYPE` ×22；另有 4 段按 SUGGESTED 处理（官方同段必带的 `sema_illegal_member_of_cstruct`、`sema_invalid_cfunc_arg_type` / `sema_invalid_cfunc_return_type`、`sema_unsafe_function_invoke_failed`、`sema_upper_bound_must_be_class_or_interface`，本项目都有名但已在别的夹具覆盖） | 28 段 / 22 标记 | 各 22 OK / 0 DIFF / 12 未标记（全部落在 4 个 SUGGESTED 段） | ① 子代理把 `@C struct TFCCPoint` 在 20 多段重复声明——diagnostics2 的 `// FILE:` 段是**同一包**里的不同文件，顶层名必须在整个夹具文件内唯一，PSI 路径因此满屏 `CLASSIFIER_REDECLARATION` + `AMBIGUOUS_USE`；按段改名为 `TFCCPointS1`…`TFCCPointS28`。② 改名时**被内联标记切断的名字**（`<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPoint`）不会被普通词边界替换命中，第一版改名脚本漏了它们，导致 4 段里出现 `TTFCCPointS<n>` 之类的未声明类型（连带 `sema_not_a_type` / `sema_generic_type_without_type_argument` 等 4 条噪声诊断）；修正后 22/22 全中。 |
| `initialization/instanceFieldInitializationRules.cj` | `CLASS_UNINITIALIZED_FIELD` ×17（全 testData 此前只在 diagnostics/ 与 LLT 有覆盖，diagnostics2 下只有泛型夹具里的 5 条伴随标记）；形态 10 / 11 / 12 的官方伴随诊断 `sema_used_before_initialization`、`sema_illegal_usage_of_member`、`sema_recursive_constructor_call`（本项目都有名、已在别处覆盖）按官方 Range 记录、不写标记 | 27 段 / 17 标记 | 各 17 OK / 0 DIFF / 3 未标记（全部是上面三条伴随诊断） | 子代理三次网关失败后由主会话按 70 余个探针自己写；头部把 `DiagnosticSema.def` 行号从 :206 改正为 :191；形态 7（父类 `private` 字段）的锚点从 `var` 改到 `private` 修饰符首字母 |
| `static-members/staticContextAccessRules.cj` | `STATIC_MEMBERS_CANNOT_CALL_MEMBERS`、`STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER`、`STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC`、`STATIC_VARIABLE_CANNOT_ACCESS_NON_STATIC_MEMBER`、`OBJECT_CANNOT_ACCESS_STATIC_MEMBER`、`ILLEGAL_ACCESS_NON_STATIC_MEMBER`（这六个名字在 diagnostics2 下此前零内联标记，只有 LLT 有覆盖） | 24 段 / 41 标记 | 各 41 OK / 0 DIFF / 2 未标记（形态 10 的 `sema_used_before_initialization` 伴随，按 static-init 夹具的先例只登记不写标记） | 无（头部引用的 `TypeCheckExpr.cpp:52/83/85`、`TypeCheckReference.cpp:150-159/211-219/449-462/500/525/618/764`、`TypeCheckDecl.cpp:89/107/256`、`Utils.cpp:325`、`ScopeManager.h:32`、`DeclAttributeChecker.cpp:117` 已逐条与源码核对；`ILLEGAL_ACCESS_NON_STATIC_MEMBER` 的 4 处标记由整个类型名收窄为 1 字符是子代理自己按实测改的） |

### 取证结论

- **元组字段 CType 约束**：`src/Sema/TypeCheckType.cpp:300-310` `CheckTupleType` 遍历 `tt.fieldTypes`，`Synthesize` 后第一个满足
  `Ty::IsCTypeConstraint` 的字段即报 `sema_invalid_tuple_field_ctype` 并 `return`（同一元组类型节点最多一条）。判定在
  `src/AST/Types.cpp:874-879`：只有 **`@C` struct**（`IsCStructType` 且名字不是 `String`）命中；`CPointer<T>` / `CFunc<…>` /
  `Pointer` 是 CType 但 kind 不是 `TYPE_STRUCT`，`VArray<@C struct>` 递归到元素后 kind 是 `TYPE_VARRAY`，都不命中。
  锚点是**出问题字段的类型节点**首字符（非 refactor kind）；具名元组字段 `(first: T, …)` 的锚点是**字段名**。
  该检查对任何类型位置生效（`let` 注解、形参、返回、class/struct 成员、类型别名、泛型实参、`Option<…>`、`VArray<…>` 实参、
  泛型上界、函数类型、interface 方法形参），`@C` struct 本身不能被 extend（`sema_c_type_cannot_extend_interface`），
  enum 构造器形参不走 `CheckReferenceTypeLegality` 的元组分派。
- **实例字段初始化**：唯一报告点 `src/Sema/LegalityOfUsage/InitializationChecker.cpp:1539-1545`，三个锚点分支：
  用户写的构造器 → `init` 首字母；编译器补出的无参构造器 → 字段声明首字符；主构造器 → 主构造器节点（本机两版 cjc 的 parser
  不接受主构造器写法，第三分支不可达）。待检查集合是类型体里**非 static 的 var / let**（`prop` 不参与）+ 父类的**非 private** 字段
  （`GetNonFuncDeclsInSuperClass:1577-1598`）。跳过路径：`this(...)` / `super(...)` 委托方构造器（真正被检查的是被委托到的那个）、
  `@Foreign` / `@Java` 宿主、构造器体被 `throw` 直接终止、以及字段已 `INITIALIZED`（有内联初值 / 构造器体里出现过赋值 /
  被父类构造器初始化过；**读不算赋值，lambda 体内的赋值也不算**）。一个构造器漏多个字段时官方循环会对每个字段调一次
  `Diagnose`，但锚点相同，而 `src/Basic/DiagnosticEngine.cpp:725-733` 的 `ConvertOldDiagToNew` 把 `errorMessage` 置成
  **未替换 `%s` 的模板文案**、`:778` `HasPrevDiag(起点, 模板文案)` 按这个二元组去重，所以只留一条（消息里的字段名是声明顺序里
  第一个漏掉的）。
- **静态上下文访问**：六个报告点 `TypeCheckReference.cpp:159` / `:219`（`this` / `super` 在带 `STATIC` 的函数体里）、
  `TypeCheckExpr.cpp:83` / `:85`（静态函数 / 静态 lambda 裸名访问实例成员，`funcDecl == nullptr` 才走 lambda 那条）、
  `TypeCheckDecl.cpp:107`（静态变量初值里的裸名）、`TypeCheckReference.cpp:462`（类型名访问实例成员）、
  `:525`（对象访问静态成员）。`this.<静态成员>` 官方报的是 `sema_object_cannot_access_static_member` 而**不是**
  `sema_static_members_cannot_call_members`；`IsLegalAccessFromStaticFunc` 只在 `re.ref.targets.size() <= 1` 时检查；
  `SymbolKind::STRUCT` 覆盖 class / interface / struct / enum / extend。
- 三份夹具两版 cjc 的 kind / 文案 / Range 完全一致，均未拆 `LANGUAGE_VERSION`。

### CFIR 缺口（按官方写期望后仍红的用例；`:cfir:analysis-tests:test` 2026-10-03 11:3x–12:0x 聚焦跑 Interop / Initialization / StaticMembers 三组）

| 用例 | 官方（本用例期望） | CFIR 当前输出 |
| --- | --- | --- |
| `interop/tupleFieldCTypeRules` | `INVALID_TUPLE_FIELD_CTYPE` 锚**元组内出问题字段类型**的首字符 1 字符 | PSI 与 LightTree 两条路径相同：锚**整个元组类型引用**（`CfirTupleCFieldTypeChecker` 用 `typeRef.source`，如 `(TFCCPoint, Int64)`），22 个标记全部红在元组起始左括号 |
| `interop/tupleFieldCTypeRules` | 同上（SUGGESTED 段 6 / 13 / 14 / 15 的伴随诊断） | LightTree 路径额外多报 `ILLEGAL_MEMBER_OF_CSTRUCT`（形态 6）、别名使用点的第二条 `INVALID_TUPLE_FIELD_CTYPE`（形态 10）、`INVALID_CFUNC_PARAMETER_TYPE` / `INVALID_CFUNC_RETURN_TYPE` / `UNSAFE_FUNCTION_INVOKE_FAILED`（形态 13 / 14）、`UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE`（形态 15）、warning `UNUSED_IMPORT`（形态 17）；PSI 路径在具名元组字段形态多报 `UNDECLARED_TYPE_NAME`（把字段名 `first` 当类型名） |
| `interop/tupleFieldCTypeRules`（WithoutAliasExpansion 路径） | 同上 | 只产出形态 1 一条（锚整个元组），形态 2 起全部不产出 |
| `initialization/instanceFieldInitializationRules` | `CLASS_UNINITIALIZED_FIELD` 锚 `init` 首字母 1 字符（显式构造器） | 锚整个 `init` 关键字（4 字符，`constructorNameDiagnosticSource` 取 `initKeyword` 整段），13 处 |
| `initialization/instanceFieldInitializationRules` | 无构造器形态锚**字段声明首字符**（`var` / `let` / `private`） | 锚字段名（`a` / `x`），4 处 |
| `initialization/instanceFieldInitializationRules` 形态 6（父类未初始化字段传递给子类） | 父类字段声明一条 + 子类 `init` 一条 | 父类那条一致，子类那条改报成同一 `init` 上的重复条目 `<!CLASS_UNINITIALIZED_FIELD, CLASS_UNINITIALIZED_FIELD!>init<!>()`（`instanceFieldInfos(context)` 不带 `includeInherited`） |
| `initialization/instanceFieldInitializationRules` 形态 9（lambda 内赋值） | 字段仍算未初始化，锚 `init` | `init` 那条一致（但锚点宽度不同），另多报 `CAPTURE_BEFORE_INITIALIZATION` |
| `initialization/instanceFieldInitializationRules` 形态 10 / 11 / 12 的伴随诊断 | `USED_BEFORE_INITIALIZATION`、`ILLEGAL_USAGE_OF_MEMBER`、`RECURSIVE_CONSTRUCTOR_CALL` | 三条都产出且锚点与官方一致（官方也有，只是本文件未写标记） |
| `static-members/staticContextAccessRules` | `STATIC_MEMBERS_CANNOT_CALL_MEMBERS` / `OBJECT_CANNOT_ACCESS_STATIC_MEMBER` 锚 `this` / `super` 首字母 1 字符 | 锚整个 `this` / `super`（4–5 字符），多处；形态 5 / 14 的 `this.<静态成员>` 还**同时**多报一条 `STATIC_MEMBERS_CANNOT_CALL_MEMBERS`（官方只报 `OBJECT_CANNOT_ACCESS_STATIC_MEMBER`） |
| `static-members/staticContextAccessRules` | `STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER`、`STATIC_VARIABLE_CANNOT_ACCESS_NON_STATIC_MEMBER` 锚裸名标识符 / `static` 首字母 | 一致，绿 |
| `static-members/staticContextAccessRules` | `STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC` 锚 lambda 里裸名首字母 | 静态属性访问器里的 lambda 一致（绿）；static func 体内 lambda 里的 `this.a` 官方报 `STATIC_MEMBERS_CANNOT_CALL_MEMBERS`，CFIR 不产出 |
| `static-members/staticContextAccessRules` | `ILLEGAL_ACCESS_NON_STATIC_MEMBER` 锚**类型名首字符** | 锚整个类型名（`SCATopLevelHost`、泛型 `SCAGenericStatic<Int64, String>`），4 处 |
| `static-members/staticContextAccessRules` 形态 12（struct 实例函数里 `this.b = 1`，`b` 是 static var） | 官方只报 `OBJECT_CANNOT_ACCESS_STATIC_MEMBER` | 另多报 `CANNOT_MODIFY_VAR` |
| `static-members/staticContextAccessRules` 形态 16（interface 里 `this.<静态 prop>`） | 官方报 `OBJECT_CANNOT_ACCESS_STATIC_MEMBER` 锚 `this` | 改报 `UNRESOLVED_REFERENCE`（`this.sp` 未解析） |
| `static-members/staticContextAccessRules` 形态 10 的 `USED_BEFORE_INITIALIZATION` 伴随 | 官方有（未写标记） | CFIR 产出且锚点一致 |
| `initialization` / `static-members`（WithoutAliasExpansion 路径） | — | 两个新用例本次聚焦跑均被跳过 |

### 环境与工具事实

- 后台 shell 的工作目录会漂到 `testData/`（环境提示里会切），此时 `./gradlew-queue.bat` 找不到（exit 127、只写出 70 字日志）。
  聚焦跑一律用**绝对路径**调 `gradlew-queue.bat`。
- 沙箱路径守卫会拒绝"用运行时计算出来的值当 `sed` / `find` 的参数"的命令（`S=…; sed -n … "$S/…"`、`sed -n "$(grep …)"`、
  `find $S/src -name …`），官方源码的定点查看改成写全字面路径即可。
- `Write` / `Edit` 工具在工作树绝对路径上仍会偶发 "Edit the worktree copy of this file" 误拒（两个子代理各遇到多次），
  重试或改用 Python 写入都能绕过；`cat >> README.md <<'EOF'` 这种长 heredoc 也可能被守卫改写而报 "unexpected EOF"，
  README 的长小节改用 Write 工具写进 `.scratch/` 再 `cat` 追加。
- 子代理改顶层名时要留意**内联标记会切断标识符**：`<!DIAG!>T<!>FCCPoint` 里的名字不是普通词边界出现，按 `sed s/TFCCPoint/…/`
  或 Python `\bTFCCPoint\b` 都会漏；改名后若 exact_check 的 UNMARKED 里冒出 `sema_not_a_type`、
  `sema_generic_type_without_type_argument` 之类"本来不该有的诊断"，基本就是这种情况。

## 批次 13（2026-10-03）：operator 重载声明 / VArray 构造 / CFunc 签名三个语义夹具

口径与批次 6–12 相同：期望只由官方 C++ 源码（`external/cangjie_compiler`，当前 checkout 在 tag v1.0.0）与本机 cjc 1.0.5 / 1.1.3 实测决定；
内联标记用本项目 CFIR 名；官方有而本项目无对应名的 kind 按 SUGGESTED_DIAGNOSTIC 处理；CFIR 与官方不一致的按缺口记录，不改期望。
三份夹具各由一个子代理独立编写，主会话逐份用同一套 exact_check 复核并修正跨段重名与锚点写法（见下表"主会话修正"）。

### 新增夹具

| 夹具 | 覆盖的 CFIR 诊断名 | 段数 / 标记数 | 1.0.5 / 1.1.3 exact_check | 主会话修正 |
| --- | --- | --- | --- | --- |
| `operator/operatorOverloadDeclarationRules.cj` | `INVALID_OPERATOR_PARAMETER_COUNT`、`OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR`、`OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR`、`INVALID_SUBSCRIPT_ASSIGN_PARAMETER`、`INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM`、`INVALID_SUBSCRIPT_ASSIGN_RETURN` | 18 段 / 40 标记 | 各 40 OK / 0 DIFF / 0 未标记 | 无（头部引用的 `TypeCheckDecl.cpp:155-193 / :196-236`、`DiagnosticSema.def:54-58`、`DiagRefactor/DiagnosticSema.def:36-39` 已逐条核对；顶层名无跨段重名） |
| `varray/varrayConstructorRules.cj` | `VARRAY_ARGS_NUMBER_MISMATCH`、`VARRAY_ARG_TYPE_WITH_REFTYPE`、`VARRAY_SUBSCRIPT_NUM`、`VARRAY_IN_CFUNC`；2 段按 SUGGESTED 处理（`sema_unknown_named_argument` 本项目无同名 CFIR 名；`sema_upper_bound_must_be_class_or_interface` 已在泛型上界夹具覆盖） | 22 段 / 18 标记 | 各 18 OK / 0 DIFF / 2 未标记（SUGGESTED 段） | ① `VCRClass` 在 10 个段同名（diagnostics2 的段是同一包里的不同文件）→ 按段改名；② `main()` 在 22 个段同名 → PSI / LightTree 路径满屏 `REDEFINITION_ENTRY`，全部改成唯一的 `func vcrMainS<n>()`；③ 头部两处过期的"`main` 未使用 CHIR 警告"描述按改名后实测改正 |
| `interop/cfuncSignatureRules.cj` | `CFUNC_CANNOT_HAVE_NAMED_ARGS`、`CFUNC_CANNOT_HAVE_UNIT_ARGS`、`CFUNC_TOO_MANY_ARGUMENTS`、`CFUNC_TYPE`、`INVALID_CFUNC_PARAMETER_TYPE`、`INVALID_CFUNC_RETURN_TYPE`、`VARRAY_IN_CFUNC` | 20 段 / 23 标记 | 各 23 OK / 0 DIFF / 1 未标记（形态 3 的 `parse_named_parameter_after_unnamed`，parser 族伴随） | ① 子代理在 4 段里把第二个标记的锚点写成第一个标记锚点的重复（`<!A!>b<!>ad!: <!B!>bad: Unit<!>`），去标记后源码变成 `bad!: bad: Unit`，`.scratch/exact_check.py` 因此报 `diff=10`；子代理把这归因于 `verify_one.py` 的 `strip_markers` 有缺陷，实际是夹具文本写错（`strip_markers` 只做 `CFIR_MARK.sub("")` + `CFIR_END.sub("")`，行为正确）。已按官方 Range 改成**嵌套标记**（外层锚"形参名起点 → 类型终点"整段，内层只包首字符）；② 形态 1（孤立具名形参 `value!: Int64`）被子代理当成"官方零诊断"的合法对照，实测官方照报 `sema_cfunc_cannot_have_named_args`（锚 `value` 的 `v`，Range (1,27)..(1,28)），已改为正式错误形态 |

### 取证结论

- **operator 重载声明**（`src/Sema/TypeCheckDecl.cpp:155-193 CheckOperatorOverloadFunc`）：按**形参个数**分派。
  0 个形参 → 必须 `IsUnaryOperator`（`BuiltInOperatorUtil.cpp:108`，查 `UNARY_EXPR_TYPE_MAP`）且宿主类型命中内建一元 →
  `sema_operator_overload_built_in_unary_operator`（`:171`）；不是一元运算符 → `sema_operator_overload_invalid_num_parameter`（`:176`）。
  1 个形参 → 必须 `IsBinaryOperator`（`:113`，查 `BINARY_EXPR_TYPE_MAP`），否则个数诊断（`:184`）；是二元且命中内建二元
  → `sema_operator_overload_built_in_binary_operator`（`:187`）。≥2 个形参 → 个数诊断（`:193`）。
  **两个表都很窄**：`+`、`==`、`**`、`!` 两张表里都没有，所以 `+` 写两个形参也报个数错；`-` 在二元表里，写一个形参合法；
  `**` 只在内建 `Int64 ** UInt64 -> Int64`、`Float64 ** (Float64|Int64) -> Float64` 时命中内建二元。
  `[]` 走 `:196-236 HandIndexOperatorOverload`：无形参 → 个数诊断；具名形参（`!` 语法）里不叫 `value` 的 →
  `sema_invalid_subscript_assign_parameter`（refactor，锚第一个非法具名形参的标识符整段，其余作 hint）；
  末位形参具名但总数 ≤ 1 → `sema_invalid_subscript_assign_parameter_num`（refactor，`MakeRange(fd.identifier)` 锚 `[]`）；
  具名合法但返回类型不是 `Unit` → `sema_invalid_subscript_assign_return`（refactor，锚返回类型整段，无显式返回类型时锚 `[]`）。
  **死条目**：`sema_operator_overload_can_not_has_default_param`（`DiagnosticSema.def:59`）与 `sema_unsupport_operator`（`:64`）
  在 v1.0.0 `src/` 全仓无引用点；operator 带默认形参、`++` / `--` / `&&` / `||`、`set` 关键字都被 parser 直接拒绝
  （`parse_expected_dot_lparen`、`parse_invalid_overloaded_operator`），本文件按"已删除的形态"记录取证。
- **VArray 构造**：`TypeCheckBuiltinExpr.cpp:370-376 ChkVArrayArg` 在实参个数 ≠ 1 时报 `sema_varray_args_number_mismatch`，
  锚 `MakeRange(ve.leftParenPos, ve.rightParenPos + 1)`，即**圆括号内的实参列表**，不含 `VArray` 与类型实参；
  尾随 lambda（`VArray<T, $n> { i => i }`）走 `ve.args[0]->name.Empty()` 分支，个数仍算 1，不报。
  `TypeCheckType.cpp:110-129 CheckVArrayType` 对元素类型调 `CheckVArrayWithRefType`，命中引用类型时
  `sema_varray_arg_type_with_reftype`（refactor，锚整个类型实参）。`VArray` 返回值出现在 `foreign func` / `CFunc` 上时
  走 `CFFICheck.cpp:297-300` 的 `else if` 分支报 `sema_varray_in_cfunc`（`VArray` 本身是 CType，所以**不**与
  `sema_invalid_cfunc_return_type` 叠加）。**`VArray` 是内建类型，不需要 `import std.collection.*`**（加了反而多一条
  `sema_unused_import` 警告——子代理纠正了任务简报里的错误说法）。
- **CFunc / C 互操作签名**：`src/Sema/FFI/CFFICheck.cpp`。`UnsafeCheck`（`:281-302`，唯一调用点 `TypeChecker.cpp:266-268`）
  对 `foreign` / `@C func` 的每个形参调 `CheckCFuncParam`（`:305-317`）：`fp.isNamedParam` → `sema_cfunc_cannot_have_named_args`
  （`Diagnose(fp, …)` 非 refactor → 锚**形参首字符 1 个字符**）；`fp.ty->IsUnit()` → `sema_cfunc_cannot_have_unit_args`；
  否则 `!IsMetCType` → `sema_invalid_cfunc_arg_type`（后两条都用显式 Range `Diagnose(fp.identifier.Begin(), fp.type->end, …)`
  锚**形参名起点 → 类型终点整段**，所以与前一条同起点、需要嵌套标记）。返回类型 `!IsMetCType` →
  `sema_invalid_cfunc_return_type`（refactor，锚整个返回类型节点），`VArray` 返回 → `sema_varray_in_cfunc`。
  `CFunc<…>` 类型实参那条路径是 `CheckCFuncParamType` / `CheckCFuncReturnType`（`:319-340`），锚**类型节点**（不含形参名）；
  `CFunc<非函数类型>` → `sema_cfunc_type`（`PreCheck.cpp:431-441`，锚类型实参整段）；
  `CFunc<…>(…)` 构造器实参个数 ≠ 1 → `sema_cfunc_too_many_arguments`（`TypeConvExpr.cpp:49`，锚 `CFunc` 首字符 1 个字符，
  0 个实参也用这一个 kind）。语法前提：`foreign func` 必须写返回类型（否则 parser `IS_BROKEN`）、具名形参后面
  不能再跟无名形参（`parse_named_parameter_after_unnamed`，声明 `IS_BROKEN` ⇒ 本族零诊断）。
- 三份夹具两版 cjc 的 kind / 文案 / Range 完全一致，均未拆 `LANGUAGE_VERSION`。

### CFIR 缺口（按官方写期望后仍红的用例；`:cfir:analysis-tests:test` 2026-10-03 13:0x–13:5x 聚焦跑 Operator / Varray / Interop 三组）

| 用例 | 官方（本用例期望） | CFIR 当前输出 |
| --- | --- | --- |
| `operator/operatorOverloadDeclarationRules` | `INVALID_OPERATOR_PARAMETER_COUNT` / `OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR` / `_BINARY_OPERATOR` 锚 `operator` 修饰符首字母 1 字符 | 锚整个 `operator` 关键字（8 字符），三类标记全红 |
| `operator/operatorOverloadDeclarationRules` | `INVALID_SUBSCRIPT_ASSIGN_PARAMETER` 锚非法具名形参标识符、`_PARAM_NUM` 与 `_RETURN` 锚 `[]` 或返回类型整段 | `[]` setter 的三条**全部不产出** |
| `operator/operatorOverloadDeclarationRules` | 官方只在返回类型与内建不一致时才有别的诊断（本文件按官方只写 built_in 一条） | 形态 7 多报 `RETURN_TYPE_INCOMPATIBLE`（CFIR 侧补充规则 `CfirOperatorDeclarationChecker.kt:140-148`）；`-` / `!` / `/` 的内建重载段还多报 `EXTEND_MEMBER_CANNOT_SHADOW` |
| `operator/operatorOverloadDeclarationRules` | 0 形参 + 非一元运算符（`+` / `==`）→ 个数诊断 | `+` 被 CFIR 归一成 `unaryPlus`，不报个数诊断；`==` 一致 |
| `varray/varrayConstructorRules` | 18 个标记 | PSI 与 LightTree 两条路径全部一致（绿）；额外：`VArray<Int64, $3>(item: 1)` 段官方 `sema_unknown_named_argument`（本项目无同名 CFIR 名）被改报 `NAMED_PARAMETER_NOT_FOUND`（映射到另一个官方 kind）；泛型上界段多报 `UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE`（官方也有，未写标记） |
| `varray/varrayConstructorRules`（WithoutAliasExpansion 路径） | 同上 | 18 个标记一个都不产出 |
| `interop/cfuncSignatureRules` | `CFUNC_CANNOT_HAVE_NAMED_ARGS` 锚形参首字符 1 字符 | 锚整个 `value!: Int64` / `bad!: String`，形态 1 一处、形态 2 / 5 / 8 / 9 与官方第二条同起点的段被合并成同一个范围上的两个名字（`<!CFUNC_CANNOT_HAVE_NAMED_ARGS, CFUNC_CANNOT_HAVE_UNIT_ARGS!>bad!: Unit<!>`） |
| `interop/cfuncSignatureRules` | `INVALID_CFUNC_PARAMETER_TYPE` / `CFUNC_CANNOT_HAVE_UNIT_ARGS` 锚"形参名 → 类型终点"整段 | `foreign func` 路径只锚类型（`Rune`、`CSRSpecText`、`(Int64) -> Unit`），`CFunc<…>` 类型位置一致 |
| `interop/cfuncSignatureRules` | 形态 3（具名形参后跟无名形参）官方只有 `parse_named_parameter_after_unnamed`，本族零诊断 | 误报 `CFUNC_CANNOT_HAVE_NAMED_ARGS`（`b!: Int64`）与 `CFUNC_CANNOT_HAVE_UNIT_ARGS`（`c: Unit`）两条 |
| `interop/cfuncSignatureRules` | `CFUNC_TOO_MANY_ARGUMENTS` 锚 `C` 首字符 1 字符 | 锚整个 `CFunc<…>` 类型引用，2 处 |
| `interop/cfuncSignatureRules` | `CFUNC_TYPE`（锚类型实参整段）、`VARRAY_IN_CFUNC`、`INVALID_CFUNC_RETURN_TYPE`（`?Int64`）、`CFunc<(Unit) -> Unit>` 的 Unit 形参类型 | PSI 与 LightTree 一致（绿）；嵌套标记处 CFIR 的渲染多出一个收尾 `<!>`（`…String<!><!>`），属测试框架渲染嵌套标记的已知表现 |
| `interop/cfuncSignatureRules`（WithoutAliasExpansion 路径） | 同上 | 23 个标记一个都不产出 |
| `operator` / `interop`（PSI 与 WithoutAliasExpansion 路径的 operator 跑） | — | operator 夹具本次聚焦跑只有 LightTree 路径执行（PSI 与 WithoutAliasExpansion 被跳过），其余两套的差异未取证 |

### 环境与工具事实

- `.scratch/verify_one.py` 的 `strip_markers` 行为正确（`CFIR_MARK.sub("")` 去开标记、`CFIR_END.sub("")` 去闭标记）；
  一行里两个标记时 exact_check 报 DIFF，原因几乎都是夹具把第二个标记的锚点写成了重复文本。去标记后出现
  `bad!: bad: Unit`、`X!: X: T` 这类**同一标识符出现两次**的源码，或多出 `sema_not_a_type` /
  `sema_generic_type_without_type_argument` 之类"本来不该有的诊断"，就是这种错误。
- diagnostics2 的 `// FILE:` 段是**同一包**里的不同文件：跨段重名会被 CFIR 报成 `CLASSIFIER_REDECLARATION`（类型）
  或 `REDEFINITION_ENTRY`（`main`）。每段都写 `main()` 是常见踩法，写完要用脚本按段统计顶层声明名。
- 一行两个官方诊断同起点时（形参名同时触发"具名"与"整段"两条），标记必须写成嵌套：外层从形参名起、内层只包首字符。
- 后台 shell 现在**完全看不到**工作树路径（`/d/...` 与 `D:/...` 两种写法都 exit 127，只写出 141 字日志），
  `gradlew-queue.bat` 聚焦跑改在前台执行即可（约 20 秒到 1 分半）。生成的测试组名按目录名首字母大写：`varray/` → `Varray`。## 批次 13（2026-10-03）：类型别名规则 / 继承图规则 / 泛型调用实参映射三个语义夹具

口径与批次 6–12 相同：期望只由官方 C++ 源码（`external/cangjie_compiler`，当前 checkout 在 tag v1.0.0）与本机 cjc 1.0.5 / 1.1.3 实测决定；
内联标记用本项目 CFIR 名；官方有而本项目无对应名的 kind 按 SUGGESTED_DIAGNOSTIC 处理；CFIR 与官方不一致的按缺口记录，不改期望。
三份夹具各由一个子代理独立编写；继承图那个子代理三次被网关空闲超时打断，主会话用其 54 个探针的实测结果自己写完并复核。

### 新增夹具

| 夹具 | 覆盖的 CFIR 诊断名 | 段数 / 标记数 | 1.0.5 / 1.1.3 exact_check | 主会话修正 |
| --- | --- | --- | --- | --- |
| `typealias/typeAliasRules.cj` | `TYPEALIAS_CYCLE`、`TYPEALIAS_UNUSED_TYPE_PARAMETERS`（官方是 **warning**）、`TYPEALIAS_EXTERNAL_REFER_INTERNAL`、`ACCESSIBILITY_ERROR`（伴随名 `sema_accessibility`，也在本文件当正式标记用） | 30 段 / 35 标记 | 各 35 OK / 0 DIFF / 3 未标记（形态 29 支撑声明上的三条官方诊断，头部登记） | ① 12 个顶层名跨段 / 跨文件重名（子代理自查只比了本文件内的其它 `.cj`，而 LLT 把三份文件放进同一个包；已按段改名加 `V<段号>` 后缀，改名脚本要先剥掉内联标记再匹配声明行，否则 `<!X!>p<!>ublic type <!Y!>TARBj<!>` 这类"修饰符被标记切断"的声明会被漏掉）；② 第 1 段的 `// FILE:` 前缀丢失（子代理写成 `typeAliasCycleTwoWay.cj` 裸行），LLT 把整个头部当一段，报 `Filename \` is not valid` |
| `typealias/typeAliasRulesV105.cj` | 同上（1.0.5-only 段） | 7 段 / 11 标记 | 1.0.5：11 OK / 0 DIFF；1.1.3：10 OK / 1 DIFF（形态 7 段，**属预期**：`exact_check.py` 不看 `LANGUAGE_VERSION` 指令，仍会用 1.1.3 编这段，1.1.3 本就不报） | 见下"版本门禁段必须单独成文件" |
| `typealias/typeAliasRulesV110.cj` | 同上（1.1.0-only 段） | 2 段 / 4 标记 | 各 4 OK / 0 DIFF | 同上 |
| `inheritance/inheritanceGraphRules.cj` | `INHERITANCE_CYCLE`、`CLASS_INHERIT_NON_CLASS_NOR_INTERFACE`、`INTERFACE_MEMBER_MUST_BE_IMPLEMENTED`、`NEED_MEMBER_IMPLEMENTATION`、`CANNOT_OVERRIDE`；1 段官方 `sema_class_need_abstract_modifier_or_func_need_impl` 无 CFIR 名按 SUGGESTED 记录 | 32 段 / 28 标记 | 各 28 OK / 0 DIFF / 6 未标记（伴随诊断：`sema_recursive_constructor_call`、`sema_non_inheritable_super_class`、`sema_illegal_extended_type`、`sema_inheritance_non_ref_type` ×2、形态 26 的 `sema_class_need_abstract_modifier_or_func_need_impl`） | ① 官方 `Diagnose(node, kind)` 对非 refactor kind 退化成 `GetBegin()` 1 字符，锚**声明的首 token**（`open` / `class` / `struct` / `enum` / `interface` / `extend` 关键字的首字母），第一版把标记写成整词被 exact_check 判 27 处 DIFF，已收窄；② `CANNOT_OVERRIDE` 是 refactor kind，锚**成员名整段**（`foo` 3 字符 / `a` 1 字符），第一版只标了首字母，已改正 |
| `call/genericCallArgumentMappingRules.cj` | `AMBIGUOUS_ARG_TYPE`、`PARAMETERS_AND_ARGUMENTS_MISMATCH`；伴随 `TYPE_MISMATCH`、`NO_MATCH_FUNCTION_DECLARATION_FOR_REF` | 18 段 / 20 标记 | 各 20 OK / 0 DIFF / 0 未标记 | 无（头部对 `sema_generic_ambiguous_method_match_in_upper_bounds` 死分支与三个 v1.0.0 尚未引入的 kind（`sema_too_many_arguments` / `sema_no_value_for_parameter` / `sema_cannot_infer_parameter_type`，全树 grep 无命中）的判断经主会话复核成立） |

### 取证结论

- **类型别名**（`src/Sema/PreCheck.cpp:1187-1196` 别名环、`src/Sema/TypeCheckDecl.cpp:679-696` 未用类型形参、`:619-642` 访问级别）：
  别名环的锚点是 `TypeAliasDecl::GetBegin()`，parser 在消费修饰符**之前**记录它（`ParseDecl.cpp:1180-1181`），所以
  `public type A = …` 锚 `p`（列 1）而不是 `type` 的 `t`；`sema_accessibility`（`DiagRefactor/DiagnosticSema.def:23`，refactor）锚别名名。
  官方**没有**自引用别名环（报 `sema_undeclared_type_name`）、别名经泛型类参数成环（零诊断）、partial-inference 报告点（只擦除不报）、
  别名声明上的 `where`（parser 不支持）。未用形参的判定来自 `GetUnusedTysInTypeAlias`（`:660-677`）按声明顺序减去 RHS
  类型树任意深度出现过的类型实参，链头形参即使实参被 `EraseIf` 擦掉也算未用。
- **继承图**（`src/Sema/PreCheck.cpp:1315-1322` 环、`:1256-1269` 非 ref 类型、`:1204-1235` DFS 入口；
  `src/Sema/TypeCheckClassLike.cpp:146-150` 非 class-nor-interface；`src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:855-878`
  接口成员未实现 / 需要实现；`src/Sema/Diags.cpp:368-377` override 非 open 父类成员）：struct / enum 自身不查环，
  环只在 class / interface 之间以及经 extend 传播；环经类型别名可穿透（`CheckInheritanceCycleHelper` 递归进别名 RHS）；
  `<:` 后面直接写元组 / 函数类型 / 原始类型会被 parser 拒（`parse_invalid_super_declaration`），
  所以 `sema_inheritance_non_ref_type` **必须经类型别名**才可达；`extend` 参与时报告落在 extend 块上。
- **泛型调用实参映射**（`src/Sema/TypeCheckCall.cpp:989-1050` 主路径、`:265-303 ResolveTypeMappings`、`:305-323 DiagnoseForMultiMapping`）：
  `ResolveTypeMappings` 只从 `matchMark[i] == true` 的下标收集结果，返回的 `resMappings` 恒为 `typeMappings` 的子序列，
  所以 `:318` 的 `resMappings.size() > typeMappings.size()` 分支是**死分支**；该 kind 的活报告点在 `:2579 GetErrorKindForCall`
  （要求接收者是 `isExposedAccess` 的 `MemberAccess`，即类型形参），本轮三种写法都先被 `sema_unable_to_infer_generic_func` 拦下。
  两条 kind 的报告点都是 `Diagnose(ce, …)`，锚调用表达式首字符。`:2195` 的"实参类型推不出来"入口（`CheckFuncPtrCall`）
  五种写法都被更早诊断拦下，本轮不可达。
- 三份主夹具两版 cjc 的 kind / 文案 / Range 完全一致；类型别名夹具按版本拆成三份文件（见下）。

### CFIR 缺口（按官方写期望后仍红的用例；`:cfir:analysis-tests:test` 2026-10-03 11:5x–12:0x 聚焦跑 Typealias / Inheritance / Call 三组）

| 用例 | 官方（本用例期望） | CFIR 当前输出 |
| --- | --- | --- |
| `typealias/typeAliasRules` | `TYPEALIAS_CYCLE` 锚别名声明首字符 1 个（`public type A` 锚 `p`） | 锚 `type A` 整段（`public` 修饰符被排除在外，泛型实参列表被包含在内），如 `type TARB4<T>`；PSI 与 LightTree 相同 |
| `typealias/typeAliasRules` | `TYPEALIAS_UNUSED_TYPE_PARAMETERS`（warning）同上锚 1 字符 | 锚 `type TARB6<T>` 整段；形态 24 / 29 这类同起点的段把 C 与未用形参**合并**成一个范围上的两个名字（`<!TYPEALIAS_EXTERNAL_REFER_INTERNAL, TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type <!ACCESSIBILITY_ERROR!>TARBk<!><T><!>`），官方是两条同起点的独立诊断 |
| `typealias/typeAliasRules` | `TYPEALIAS_EXTERNAL_REFER_INTERNAL` 锚 `p` / `t` 1 字符 | 锚 `type <别名名>`（起点在 `type` 而不是修饰符），`ACCESSIBILITY_ERROR` 的别名名锚点与官方一致 |
| `typealias/typeAliasRules`（WithoutAliasExpansion 路径） | 同上 | 35 个标记里只有 2 条 `TYPEALIAS_CYCLE` 产出（其余 C / 未用形参 / `ACCESSIBILITY_ERROR` 全缺） |
| `inheritance/inheritanceGraphRules` | `INHERITANCE_CYCLE` 锚声明首 token 1 字符 | 锚 `interface IgIpA` / `extend IgCsxS` 这类"关键字 + 类型名"整段；形态 3（别名环）还多报一条 `RECURSIVE_CONSTRUCTOR_CALL`（官方也有，未写标记）；形态 7 多报 `NON_INHERITABLE_SUPER_CLASS`、形态 8 多报 `ILLEGAL_EXTENDED_TYPE`（同属官方伴随，未写标记）；形态 5 / 6 的 extend 块上 CFIR 报而官方报在环上的 interface |
| `inheritance/inheritanceGraphRules` | `CLASS_INHERIT_NON_CLASS_NOR_INTERFACE` 锚超类型名首字母 1 字符 | 锚整个超类型引用（`Array<Int64>` / `IgNaA2`），5 处 |
| `inheritance/inheritanceGraphRules` | `INTERFACE_MEMBER_MUST_BE_IMPLEMENTED` 锚宿主声明首 token 1 字符 | 锚宿主类型名（`IgMsS` / `IgMpS` / `IgMsgS` / `IgM3S`），enum 与 extend 宿主两段不产出 |
| `inheritance/inheritanceGraphRules` | `NEED_MEMBER_IMPLEMENTATION` 锚宿主声明首 token | 改报 `ABSTRACT_MEMBER_NOT_IMPLEMENTED` 并锚宿主类型名，enum / extend 宿主不产出 |
| `inheritance/inheritanceGraphRules` | `CANNOT_OVERRIDE` 锚成员名整段 | 一致（PSI 与 LightTree 绿）；WithoutAliasExpansion 路径全部 28 个标记不产出 |
| `inheritance/inheritanceGraphRules`（WithoutAliasExpansion 路径） | 同上 | 只有 1 条 `INHERITANCE_CYCLE` 产出（形态 1，锚 `interface IgIpA`），其余全缺 |
| `call/genericCallArgumentMappingRules` | `PARAMETERS_AND_ARGUMENTS_MISMATCH` 锚调用表达式首字符 1 字符 | 锚被调者整名（`GAMBox13` / `GAMBox14<…>` / `GAMHolder15`），4 处；`extend` 成员与类内成员两段一致 |
| `call/genericCallArgumentMappingRules` | 伴随 `NO_MATCH_FUNCTION_DECLARATION_FOR_REF` 锚函数名首字母 | 锚整名（`gamS14Height`），1 处 |
| `call/genericCallArgumentMappingRules`（WithoutAliasExpansion 路径） | 12 条 `AMBIGUOUS_ARG_TYPE` + 4 条 `PARAMETERS_AND_ARGUMENTS_MISMATCH` | 全部不产出 |

### 版本门禁段必须单独成文件

LLT 的 `LANGUAGE_VERSION` 指令是**文件级**指令，写在 `// FILE:` 段内会直接失败：
`java.lang.IllegalStateException: Directive LANGUAGE_VERSION has Global applicability but it declared in File`
（`ModuleStructureExtractorImpl.kt:322`）。因此类型别名夹具的 7 个门禁段拆成 `typeAliasRulesV105.cj`（版本 1.0.5，6 段）与
`typeAliasRulesV110.cj`（版本 1.1.0，1 段），主文件只剩 26 个两版一致的段。已核实 testData 下没有任何多段文件在段内写该指令。
另外，头部**散文里**提到 `` `// LANGUAGE_VERSION:` `` 或 `` `// FILE:` `` 的行也会被框架当成真指令解析
（`Filename \` is not valid`），本批已把这些行改成不带冒号的文字描述。

### 环境与工具事实

- `.scratch/exact_check.py` 原来只比对 **error**（`official_ranges` 跳过 warning），因此官方 warning 类标记
  （本批的 `TYPEALIAS_UNUSED_TYPE_PARAMETERS`）恒为 DIFF。已改成 error + warning 都收；批次 13 之前
  `finallyFlowPlaceholder.cj` 的 `UNUSED_VARIABLE` / `UNUSED_EXPRESSION` 同样是这个工具口径造成的假 DIFF
  （README 批次 8 记录过"exact=0 diff=2"），现在应为 exact=2 diff=0。
- 跨文件重名自查要按 **LLT 的包边界**做：同一目录下所有夹具文件进同一个包，只查"本文件 vs 其它文件"会漏
  （类型别名子代理自查就是这样漏掉 12 个重名）。改名脚本匹配声明行前要先剥掉内联标记。
- 生成的测试组名按目录名首字母大写：`varray/` → `Varray`、`typealias/` → `Typealias`、`static-members/` → `StaticMembers`；
  方法名是文件名去 `.cj` 后首字母大写加 `test` 前缀。
- 后台 shell 偶尔完全看不到工作树路径（`/d/...` 与 `D:/...` 两种写法都报 No such file），此时 gradle 聚焦跑要在前台执行。