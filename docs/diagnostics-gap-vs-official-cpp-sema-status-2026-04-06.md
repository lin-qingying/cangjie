# 语义诊断补齐主计划完成状态

日期：`2026-04-06`

关联总文档：
`docs/diagnostics-gap-vs-official-cpp-sema-detailed.md`

## 结论

本轮“语义分析诊断补齐主计划”在当前允许范围内已经完成。

这里的“完成”指：

- 已允许推进的语义批次已经补齐到可回归、可验证的状态。
- 用户明确要求跳过到后期再细化的语义域，不再计入本轮未完成项。

## 本轮已完成范围

- `call + constructor`
- `initialization + legality`
- `generic-access + visibility matrix`
- `pattern legality`
- `mut / immutable` 核心规则
- `annotation` 核心规则
- `VArray` 专门语义
- 继承/构造器域中 `enum type constructor` 与 `enum constructor` 的语义分流

## 代表性已完成诊断

- `sema_no_match_constructor`
- `sema_ambiguous_constructor_match`
- `sema_unknown_named_argument`
- `sema_multiple_named_argument`
- `sema_unordered_arguments`
- `sema_recursive_constructor_call`
- `sema_illegal_place_of_calling_this_or_super`
- `sema_used_before_initialization`
- `sema_class_uninitialized_field`
- `sema_generic_type_without_type_argument`
- `sema_generic_no_member_match_in_upper_bounds`
- `sema_generic_no_method_match_in_upper_bounds`
- `sema_tuple_pattern_not_match`
- `sema_pattern_not_match`
- `sema_enum_pattern_param_size_error`
- `sema_cannot_modify_var`
- `sema_immutable_function_cannot_access_mutable_function`
- `sema_annotation_no_const_init`
- `sema_varray_size_match`

## 本轮代表性回归样例

- `cfir/analysis-tests/testData/diagnostics/call/namedArgumentsAndArityRich.cj`
- `cfir/analysis-tests/testData/diagnostics/constructor/delegationAndConstructorsRich.cj`
- `cfir/analysis-tests/testData/diagnostics/constructor/illegalDelegationPlacementRich.cj`
- `cfir/analysis-tests/testData/diagnostics/initialization/usedBeforeInitializationRich.cj`
- `cfir/analysis-tests/testData/diagnostics/initialization/classUninitializedFieldRich.cj`
- `cfir/analysis-tests/testData/diagnostics/generic-access/genericTypeWithoutTypeArgumentRich.cj`
- `cfir/analysis-tests/testData/diagnostics/generic-access/upperBoundsMemberAndMethodRich.cj`
- `cfir/analysis-tests/testData/diagnostics/visibility/protectedAndInternalMatrixRich.cj`
- `cfir/analysis-tests/testData/diagnostics/pattern/patternLegalityRich.cj`
- `cfir/analysis-tests/testData/diagnostics/mut/immutableFunctionRestrictionsRich.cj`
- `cfir/analysis-tests/testData/diagnostics/annotation/annotationNoConstInitRich.cj`
- `cfir/analysis-tests/testData/diagnostics/varray/varraySizeMismatchRich.cj`
- `cfir/analysis-tests/testData/diagnostics/enum/errorSimpleEnum.cj`

## 明确后置项

以下内容按用户指令明确后置，不属于本轮完成范围：

- `inout`
- FFI / interop 全域语义
- mocking
- common/specific
- effects

## 说明

- 后续如果继续推进，应以“新开后置批次”的方式进行，而不是继续视作本轮主计划尾项。
- 总需求源仍以 `docs/diagnostics-gap-vs-official-cpp-sema-detailed.md` 为准，本文件只负责标记本轮完成状态。
