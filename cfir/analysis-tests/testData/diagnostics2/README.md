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
- `DEPRECATED_MODIFIER_FOR_TARGET`
- `DEPRECATED_MODIFIER_CONTAINING_DECLARATION`
- `DEPRECATED_MODIFIER_PAIR`
- `MISMATCHING_HANDLE_BLOCK`
- `NEW_INFERENCE_ERROR` (placeholder planning only)
- `BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION` (placeholder planning only)
- `INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION` (placeholder planning only)
- `INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION` (placeholder planning only)

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
STATUS: not yet defined in CfirDiagnosticsList.kt
*/
```

## Batch boundary for the current OpenSpec implementation

### Current executable batch

- Extend currently thin but already-real semantic domains with runnable `.cj` samples.
- Add placeholder `.cj` samples for official semantics that are documented or present in official C++ Sema, but not yet modeled in the current project.

### Explicit backlog / later batch

- `common-specific/`
- `mock/`
- deeper `inout/` runtime-sensitive shapes beyond current placeholder coverage
- semantics that still require producer/checker registration work before a stable inline assertion can exist

## Writing order for future batches

1. Expand directly modeled diagnostics in already existing directories.
2. Expand thin directories into small semantic matrices.
3. Add new semantic-domain directories with placeholder diagnostics only when the official semantics are clear.
4. Move backlog domains into runnable directories only after semantics and internal modeling both stabilize.
