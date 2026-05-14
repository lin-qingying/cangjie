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
- repeated / shadowed catch types: official current C++/`cjc` semantics are `sema_useless_exception_type` on the catch type token; the project now defines this diagnostic name and default message, but these fixtures still keep it as `SUGGESTED_DIAGNOSTIC` until checker wiring lands.
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
- `return` inside `try { ... } handle (...) { ... }`: official `cjc` uses `sema_return_in_try_handle_block`, and the current project has `RETURN_IN_TRY_HANDLE_BLOCK`; do not degrade this case to generic `INVALID_RETURN`.
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
