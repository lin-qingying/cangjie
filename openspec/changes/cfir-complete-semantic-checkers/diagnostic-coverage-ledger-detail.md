# CFIR 全诊断覆盖台账（逐诊断明细）

## 说明

本文件是 `diagnostic-coverage-ledger.md` 的逐诊断展开版。

使用规则：

- 每个诊断必须单独占一行。
- `责任层` 只能填写：`resolve`、`checker`、`existing-checker`、`existing-resolve`。
- `当前状态` 暂使用：`未复核`、`部分覆盖`、`已覆盖`、`未覆盖`。
- `实现入口`、`测试入口`、`C++ 依据` 在后续任务中继续回填，不允许长期留空。

| 诊断组 | 诊断名 | 责任层 | 责任子域 | 当前状态 | 任务号 | Spec | 实现入口 | 测试入口 | C++ 依据 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Resolve | `NO_CONSTRUCTOR` | `resolve` | `Resolve` | `部分覆盖` | `2.4` | `specs/resolve-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConstructorDelegationChecker.kt` | `cfir/analysis-tests/testData/diagnostics/constructor/noConstructorRich.cj`; `cfir/analysis-tests/testData/diagnostics2/invalid-declaration/noConstructorMatrix.cj` | `sema_no_matching_constructor` | 仍需核对 direct construction / this / super 三种路径是否都走 resolve 主链路 |
| Resolve | `ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR` | `resolve` | `Resolve` | `部分覆盖` | `2.4` | `specs/resolve-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/golden/DiagnosticNameMapper.kt` | `sema_enum_type_cannot_be_used_as_constructor` | 尚未定位到专门的 `.cj` 样例，需补直接断言测试 |
| Redeclaration | `CONFLICTING_OVERLOADS` | `existing-checker` | `Redeclaration` | `未复核` | `7.0`, `7.5` | `specs/redeclaration-diagnostics/spec.md` |  |  |  |  |
| Redeclaration | `REDECLARATION` | `existing-checker` | `Redeclaration` | `未复核` | `7.0`, `7.5` | `specs/redeclaration-diagnostics/spec.md` |  |  |  |  |
| Redeclaration | `CLASSIFIER_REDECLARATION` | `existing-checker` | `Redeclaration` | `未复核` | `7.0`, `7.5` | `specs/redeclaration-diagnostics/spec.md` |  |  |  |  |
| Imports | `UNRESOLVED_IMPORT` | `existing-checker` | `Imports` | `未复核` | `7.0`, `7.5` | `specs/imports-diagnostics/spec.md` |  |  |  |  |
| Imports | `IMPORT_CONFLICT` | `existing-checker` | `Imports` | `未复核` | `7.0`, `7.5` | `specs/imports-diagnostics/spec.md` |  |  |  |  |
| Imports | `IMPORT_ALIAS_CONFLICT` | `existing-checker` | `Imports` | `未复核` | `7.0`, `7.5` | `specs/imports-diagnostics/spec.md` |  |  |  |  |
| SuperTypes | `SUPER_TYPES_SELF_REFERENCE` | `existing-checker` | `SuperTypes` | `未复核` | `7.0`, `7.5` | `specs/supertypes-diagnostics/spec.md` |  |  |  |  |
| SuperTypes | `SUPER_TYPES_DUPLICATE` | `existing-checker` | `SuperTypes` | `未复核` | `7.0`, `7.5` | `specs/supertypes-diagnostics/spec.md` |  |  |  |  |
| SuperTypes | `INTERFACE_CANNOT_INHERIT_CLASS` | `existing-checker` | `SuperTypes` | `未复核` | `7.0`, `7.5` | `specs/supertypes-diagnostics/spec.md` |  |  |  |  |
| SuperTypes | `MULTIPLE_CLASS_SUPER_TYPES` | `existing-checker` | `SuperTypes` | `未复核` | `7.0`, `7.5` | `specs/supertypes-diagnostics/spec.md` |  |  |  |  |
| CallResolution | `NO_VALUE_FOR_PARAMETER` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/call/namedArgumentsAndArityRich.cj`; `cfir/analysis-tests/testData/diagnostics2/call/namedArgumentsAndArity.cj`; `cfir/analysis-tests/testData/diagnostics/constructor/delegationAndConstructorsRich.cj` | `sema_no_value_for_parameter` | 由 cone diagnostics 映射，仍需核对 diagnostics2 是否覆盖全部变体 |
| CallResolution | `TOO_MANY_ARGUMENTS` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/call/namedArgumentsAndArityRich.cj`; `cfir/analysis-tests/testData/diagnostics2/call/namedArgumentsAndArity.cj` | `sema_too_many_arguments` |  |
| CallResolution | `NAMED_PARAMETER_NOT_FOUND` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/call/namedArgumentsAndArityRich.cj`; `cfir/analysis-tests/testData/diagnostics2/call/namedArgumentsAndArity.cj` | `sema_named_parameter_not_found` |  |
| CallResolution | `ARGUMENT_PASSED_TWICE` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/call/namedArgumentsAndArityRich.cj`; `cfir/analysis-tests/testData/diagnostics2/call/namedArgumentsAndArity.cj` | `sema_argument_passed_twice` |  |
| CallResolution | `NAMED_ARGUMENTS_NOT_ALLOWED` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/call/namedArgumentsAndArityRich.cj`; `cfir/analysis-tests/testData/diagnostics2/call/namedArgumentsAndArity.cj` | `sema_named_arguments_not_allowed` |  |
| CallResolution | `MIXING_NAMED_AND_POSITIONAL_ARGUMENTS` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/call/namedArgumentsAndArityRich.cj`; `cfir/analysis-tests/testData/diagnostics2/call/namedArgumentsAndArity.cj` | `sema_mixing_named_and_positional_arguments` |  |
| CallResolution | `NEED_NAMED_ARGUMENT` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/call/namedArgumentsAndArityRich.cj`; `cfir/analysis-tests/testData/diagnostics2/call/namedArgumentsAndArity.cj` | `sema_need_named_argument` |  |
| CallResolution | `AMBIGUOUS_CONSTRUCTOR_CALL` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConstructorDelegationChecker.kt` | `cfir/analysis-tests/testData/diagnostics/constructor/delegationAndConstructorsRich.cj` | `sema_ambiguous_constructor_call` | 仍需补 diagnostics2 直断言样例 |
| CallResolution | `AMBIGUOUS_FUNCTION_CALL` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/call/ambiguousFunctionCallRich.cj` | `sema_ambiguous_call` |  |
| CallResolution | `RECURSIVE_CONSTRUCTOR_CALL` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConstructorDelegationChecker.kt` | `cfir/analysis-tests/testData/diagnostics/constructor/delegationAndConstructorsRich.cj` | `sema_recursive_constructor_call` | 需确认是否同时存在 resolve 主链路发射 |
| CallResolution | `ILLEGAL_THIS_OR_SUPER_CALL` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirConstructorDelegationCallChecker.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConstructorDelegationChecker.kt` | `cfir/analysis-tests/testData/diagnostics/constructor/illegalDelegationPlacementRich.cj` | `sema_illegal_this_or_super_call` | 当前由 checker 承担位置合法性，需后续再校正职责边界说明 |
| CallResolution | `EXPLICIT_SUPER_CALL_REQUIRED` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConstructorDelegationChecker.kt` | `cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/golden/DiagnosticNameMapper.kt` | `sema_explicit_super_call_required` | 尚需补直接 `.cj` 断言样例定位 |
| CallResolution | `INVALID_LOOP_CONTROL` | `resolve` | `CallResolution` | `部分覆盖` | `2.1`, `2.2`, `2.3` | `specs/call-resolution-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/golden/DiagnosticNameMapper.kt` | `sema_invalid_loop_control` | 需补或定位现有 inline 测试文件 |
| Initialization | `USED_BEFORE_INITIALIZATION` | `existing-checker` | `Initialization` | `未复核` | `8.0`, `8.6` | `specs/initialization-diagnostics/spec.md` |  |  |  |  |
| Initialization | `CLASS_UNINITIALIZED_FIELD` | `existing-checker` | `Initialization` | `未复核` | `8.0`, `8.6` | `specs/initialization-diagnostics/spec.md` |  |  |  |  |
| GenericAccess | `GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS` | `existing-checker` | `GenericAccess` | `未复核` | `8.0`, `8.6` | `specs/generic-access-diagnostics/spec.md` |  |  |  |  |
| GenericAccess | `GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS` | `existing-checker` | `GenericAccess` | `未复核` | `8.0`, `8.6` | `specs/generic-access-diagnostics/spec.md` |  |  |  |  |
| Mutability | `CANNOT_MODIFY_VAR` | `existing-checker` | `Mutability` | `未复核` | `8.0`, `8.6` | `specs/mutability-diagnostics/spec.md` |  |  |  |  |
| Mutability | `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION` | `existing-checker` | `Mutability` | `未复核` | `8.0`, `8.6` | `specs/mutability-diagnostics/spec.md` |  |  |  |  |
| Annotation | `ANNOTATION_NO_CONST_INIT` | `existing-checker` | `Annotation` | `未复核` | `9.0`, `9.10` | `specs/annotation-semantics/spec.md` |  |  |  |  |
| Interop | `INVALID_CFUNC_RETURN_TYPE` | `existing-checker` | `Interop` | `未复核` | `9.0`, `9.10` | `specs/interop-semantics/spec.md` |  |  |  |  |
| Effects | `EFFECTS_FEATURE_DISABLED` | `existing-checker` | `Effects` | `未复核` | `9.0`, `9.10` | `specs/effects-semantics/spec.md` |  |  |  |  |
| Effects | `COMMAND_INCOMPATIBLE_TYPE` | `existing-checker` | `Effects` | `未复核` | `9.0`, `9.10` | `specs/effects-semantics/spec.md` |  |  |  |  |
| Effects | `COMMAND_HANDLE_TYPE_ERROR` | `existing-checker` | `Effects` | `未复核` | `9.0`, `9.10` | `specs/effects-semantics/spec.md` |  |  |  |  |
| Effects | `IMPLICIT_RESUME_OUTSIDE_HANDLER` | `existing-checker` | `Effects` | `未复核` | `9.0`, `9.10` | `specs/effects-semantics/spec.md` |  |  |  |  |
| Effects | `RESUME_NO_WITH` | `existing-checker` | `Effects` | `未复核` | `9.0`, `9.10` | `specs/effects-semantics/spec.md` |  |  |  |  |
| Effects | `RESUME_THROWING_MISMATCH_TYPE` | `existing-checker` | `Effects` | `未复核` | `9.0`, `9.10` | `specs/effects-semantics/spec.md` |  |  |  |  |
| Effects | `MISMATCHING_HANDLE_BLOCK` | `existing-checker` | `Effects` | `未复核` | `9.0`, `9.10` | `specs/effects-semantics/spec.md` |  |  |  |  |
| Match | `NON_EXHAUSTIVE_MATCH` | `existing-checker` | `Match` | `未复核` | `9.0`, `9.10` | `specs/match-semantics/spec.md` |  |  |  |  |
| Match | `TUPLE_PATTERN_NOT_MATCH` | `existing-checker` | `Match` | `未复核` | `9.0`, `9.10` | `specs/match-semantics/spec.md` |  |  |  |  |
| Match | `PATTERN_NOT_MATCH` | `existing-checker` | `Match` | `未复核` | `9.0`, `9.10` | `specs/match-semantics/spec.md` |  |  |  |  |
| Match | `ENUM_PATTERN_PARAM_SIZE_ERROR` | `existing-checker` | `Match` | `未复核` | `9.0`, `9.10` | `specs/match-semantics/spec.md` |  |  |  |  |
| Match | `NOT_OVERLOAD_IN_MATCH` | `existing-checker` | `Match` | `未复核` | `9.0`, `9.10` | `specs/match-semantics/spec.md` |  |  |  |  |
| Match | `MATCH_CASE_HAS_NO_TYPE` | `existing-checker` | `Match` | `未复核` | `9.0`, `9.10` | `specs/match-semantics/spec.md` |  |  |  |  |
| Constraint | `NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER` | `resolve` | `Constraint` | `部分覆盖` | `3.1`, `3.2`, `3.3` | `specs/constraint-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeConstraintsChecker.kt` | `cfir/analysis-tests/testData/diagnostics/constraints/nameInConstraintIsNotTypeParameter.cj`; `cfir/analysis-tests/testData/diagnostics/constraints/nameInConstraintIsNotTypeParameterExtend.cj` | `sema_name_in_constraint_is_not_a_type_parameter` | 当前实现仍在 checker，后续需判断是否保留该分层 |
| Constraint | `ONLY_ONE_CLASS_BOUND_ALLOWED` | `resolve` | `Constraint` | `部分覆盖` | `3.1`, `3.2`, `3.3` | `specs/constraint-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeParameterBoundsChecker.kt` | `cfir/analysis-tests/testData/diagnostics/constraints/onlyOneClassBoundAllowed.cj` | `sema_only_one_class_bound_allowed` |  |
| Constraint | `REPEATED_BOUND` | `resolve` | `Constraint` | `部分覆盖` | `3.1`, `3.2`, `3.3` | `specs/constraint-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeParameterBoundsChecker.kt` | `cfir/analysis-tests/testData/diagnostics/constraints/repeatedBound.cj`; `cfir/analysis-tests/testData/diagnostics/constraints/repeatedBoundTypeParameter.cj` | `sema_repeated_bound` |  |
| Constraint | `CONFLICTING_UPPER_BOUNDS` | `resolve` | `Constraint` | `部分覆盖` | `3.1`, `3.2`, `3.3` | `specs/constraint-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeParameterBoundsChecker.kt` | `cfir/analysis-tests/testData/diagnostics/constraints/conflictingUpperBounds.cj` | `sema_conflicting_upper_bounds` |  |
| Constraint | `CANNOT_INFER_PARAMETER_TYPE` | `resolve` | `Constraint` | `部分覆盖` | `3.1`, `3.2`, `3.3` | `specs/constraint-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/golden/DiagnosticNameMapper.kt` | `sema_cannot_infer_parameter_type` | 尚未定位到稳定 inline 用例 |
| Constraint | `NEW_INFERENCE_ERROR` | `resolve` | `Constraint` | `部分覆盖` | `3.1`, `3.2`, `3.3` | `specs/constraint-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/golden/DiagnosticNameMapper.kt` | `sema_new_inference_error` | 需补或定位 diagnostics2 样例 |
| Constraint | `TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR` | `resolve` | `Constraint` | `部分覆盖` | `3.1`, `3.2`, `3.3` | `specs/constraint-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt` | `sema_type_inference_only_input_types_error` | 需补直断言测试 |
| Constraint | `BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION` | `resolve` | `Constraint` | `部分覆盖` | `3.1`, `3.2`, `3.3` | `specs/constraint-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/golden/DiagnosticNameMapper.kt` | `sema_builder_inference_multi_lambda_restriction` | 需补直断言测试 |
| Constraint | `INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION` | `resolve` | `Constraint` | `部分覆盖` | `3.1`, `3.2`, `3.3` | `specs/constraint-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/golden/DiagnosticNameMapper.kt` | `sema_inferred_type_variable_into_empty_intersection` | 需补直断言测试 |
| Constraint | `INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION` | `resolve` | `Constraint` | `部分覆盖` | `3.1`, `3.2`, `3.3` | `specs/constraint-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/golden/DiagnosticNameMapper.kt` | `sema_inferred_type_variable_into_possible_empty_intersection` | 需补直断言测试 |
| TypeCheck | `TYPE_MISMATCH` | `resolve` | `TypeCheck` | `部分覆盖` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirIfConditionTypeMismatchChecker.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirLoopConditionTypeMismatchChecker.kt` | `cfir/analysis-tests/testData/diagnostics/type-mismatch/simple.cj`; `cfir/analysis-tests/testData/diagnostics/type-mismatch/conditionMustBeBool.cj`; `cfir/analysis-tests/testData/diagnostics2/type-mismatch/*` | `sema_mismatched_types` | 主路径存在，但仍需逐子场景回填 |
| TypeCheck | `PATTERN_INITIALIZER_TYPE_MISMATCH` | `resolve` | `TypeCheck` | `部分覆盖` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirPatternVariableInitializerTypeMismatchChecker.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/type-mismatch/simple.cj`; `cfir/analysis-tests/testData/diagnostics2/operator/invokeOverloads.cj` | `sema_mismatched_types` |  |
| TypeCheck | `RETURN_TYPE_MISMATCH` | `resolve` | `TypeCheck` | `部分覆盖` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirReturnTypeMismatchChecker.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/type-mismatch/returnTypeMismatch.cj`; `cfir/analysis-tests/testData/diagnostics2/type-mismatch/returnTypeMismatch.cj` | `sema_mismatched_types` |  |
| TypeCheck | `ARGUMENT_TYPE_MISMATCH` | `resolve` | `TypeCheck` | `部分覆盖` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/type-mismatch/argumentTypeMismatch.cj`; `cfir/analysis-tests/testData/diagnostics2/type-mismatch/argumentTypeMismatch.cj`; `cfir/analysis-tests/testData/diagnostics2/operator/invokeOverloads.cj` | `sema_mismatched_types` |  |
| TypeCheck | `ASSIGNMENT_TYPE_MISMATCH` | `resolve` | `TypeCheck` | `部分覆盖` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirAssignmentTypeMismatchChecker.kt` | `cfir/analysis-tests/testData/diagnostics/type-mismatch/assignmentTypeMismatch.cj`; `cfir/analysis-tests/testData/diagnostics2/type-mismatch/assignmentTypeMismatch.cj` | `sema_mismatched_types` |  |
| TypeCheck | `VARRAY_SIZE_MISMATCH` | `resolve` | `TypeCheck` | `未复核` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` |  |  |  |  |
| TypeCheck | `GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT` | `resolve` | `TypeCheck` | `未复核` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` |  |  | 尚未定位用例 |
| TypeCheck | `INVISIBLE_MEMBER` | `resolve` | `TypeCheck` | `未复核` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` |  |  | 尚未定位用例 |
| TypeCheck | `INVISIBLE_REFERENCE` | `resolve` | `TypeCheck` | `未复核` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` |  |  | 尚未定位用例 |
| TypeCheck | `OVERRIDING_RETURN_TYPE_MISMATCH` | `resolve` | `TypeCheck` | `部分覆盖` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOverrideChecker.kt` | `cfir/analysis-tests/testData/diagnostics/coverage/inheritance/overrideReturnTypeMismatchRich.cj`; `cfir/analysis-tests/testData/diagnostics2/inheritance/overrideReturnTypeMismatch.cj` | `sema_mismatched_types` |  |
| TypeCheck | `CANNOT_OVERRIDE_INVISIBLE_MEMBER` | `resolve` | `TypeCheck` | `部分覆盖` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOverrideChecker.kt` | `cfir/analysis-tests/testData/diagnostics/coverage/inheritance/overrideInvisibleMemberRich.cj` |  |  |
| TypeCheck | `CLASS_NOT_OPEN_FOR_INHERITANCE` | `resolve` | `TypeCheck` | `部分覆盖` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirSupertypesChecker.kt` | `cfir/analysis-tests/testData/diagnostics/coverage/inheritance/classNotOpenForInheritanceRich.cj` |  |  |
| TypeCheck | `ABSTRACT_MEMBER_NOT_IMPLEMENTED` | `resolve` | `TypeCheck` | `部分覆盖` | `4.1`, `4.2`, `4.3` | `specs/type-check-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirNotImplementedOverrideChecker.kt` |  |  | 尚未定位稳定 inline 用例 |
| ConstEval | `LITERAL_NUMERIC_OVERFLOW` | `existing-checker` | `ConstEval` | `未复核` | `9.0`, `9.10` | `specs/const-eval-diagnostics/spec.md` |  |  |  |  |
| ConstEval | `CONST_EVAL_DIVIDE_BY_ZERO` | `existing-checker` | `ConstEval` | `未复核` | `9.0`, `9.10` | `specs/const-eval-diagnostics/spec.md` |  |  |  |  |
| ConstEval | `CONST_EVAL_ARITHMETIC_OVERFLOW` | `existing-checker` | `ConstEval` | `未复核` | `9.0`, `9.10` | `specs/const-eval-diagnostics/spec.md` |  |  |  |  |
| ConstEval | `CONST_EVAL_NEGATIVE_SHIFT_COUNT` | `existing-checker` | `ConstEval` | `未复核` | `9.0`, `9.10` | `specs/const-eval-diagnostics/spec.md` |  |  |  |  |
| ConstEval | `CONST_EVAL_SHIFT_COUNT_OVERFLOW` | `existing-checker` | `ConstEval` | `未复核` | `9.0`, `9.10` | `specs/const-eval-diagnostics/spec.md` |  |  |  |  |
| Unresolved | `UNRESOLVED_REFERENCE` | `resolve` | `Unresolved` | `部分覆盖` | `5.1`, `5.2`, `5.3` | `specs/unresolved-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/unresolved/unresolvedReferenceName.cj`; `cfir/analysis-tests/testData/diagnostics/unresolved/unresolvedNameCall.cj`; `cfir/analysis-tests/testData/diagnostics/unresolved/unresolvedSymbolType.cj`; `cfir/analysis-tests/testData/diagnostics2/unresolved/*` | `sema_undeclared_identifier` | 需要继续和 operator/subscript 分流规则对齐 |
| Unresolved | `INVALID_BINARY_OPERATOR` | `resolve` | `Unresolved` | `部分覆盖` | `5.1`, `5.2`, `5.3` | `specs/unresolved-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/operator/*.cj`; `cfir/analysis-tests/testData/diagnostics2/operator/*.cj` | `sema_invalid_binary_operator` |  |
| Unresolved | `NO_MATCHING_OPERATOR_INVOKE` | `resolve` | `Unresolved` | `部分覆盖` | `5.1`, `5.2`, `5.3` | `specs/unresolved-diagnostics/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/golden/DiagnosticNameMapper.kt` | `sema_no_matching_operator_invoke` | 尚需定位或补 dedicated inline 样例 |
| General | `INVALID_NODE_AFTER_CHECK` | `checker` | `General` | `未复核` | `7.1`, `7.5` | `specs/general-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt` |  |  | 尚未定位实现与测试入口 |
| General | `UNABLE_TO_INFER_DECL` | `checker` | `General` | `部分覆盖` | `7.1`, `7.5` | `specs/general-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGeneralSemanticsChecker.kt` |  |  | 需补稳定 inline 样例 |
| General | `MISMATCHED_TYPES_MULTIPLE_ASSIGN` | `checker` | `General` | `未复核` | `7.1`, `7.5` | `specs/general-semantics-checker/spec.md` |  |  |  |  |
| General | `MISMATCHED_TYPES_BECAUSE` | `checker` | `General` | `未复核` | `7.1`, `7.5` | `specs/general-semantics-checker/spec.md` |  |  |  |  |
| General | `AMBIGUOUS_USE` | `checker` | `General` | `部分覆盖` | `7.1`, `7.5` | `specs/general-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGeneralSemanticsChecker.kt` | `cfir/analysis-tests/testData/diagnostics/call/ambiguousFunctionCallRich.cj` |  | 需补非 call-like 场景 |
| General | `CONFLICT_WITH_SUB_PACKAGE` | `checker` | `General` | `未复核` | `7.1`, `7.5` | `specs/general-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGeneralSemanticsChecker.kt` |  |  | 尚未定位样例 |
| General | `CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE` | `checker` | `General` | `未复核` | `7.1`, `7.5` | `specs/general-semantics-checker/spec.md` |  |  |  |  |
| General | `ACCESSIBILITY_WITH_MAIN_HINT` | `checker` | `General` | `未复核` | `7.1`, `7.5` | `specs/general-semantics-checker/spec.md` |  |  |  |  |
| General | `ACCESSIBILITY_ERROR` | `checker` | `General` | `部分覆盖` | `7.1`, `7.5` | `specs/general-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGeneralSemanticsChecker.kt` | `cfir/analysis-tests/testData/diagnostics/coverage/accessibility/*` |  |  |
| General | `PARAM_COUNT_MISMATCH` | `checker` | `General` | `未复核` | `7.1`, `7.5` | `specs/general-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGeneralSemanticsChecker.kt` |  |  | 尚未定位样例 |
| Function | `UNABLE_TO_INFER_RETURN_TYPE` | `checker` | `Function` | `部分覆盖` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionSemanticsChecker.kt` |  |  | 尚未定位稳定 inline 样例 |
| Function | `UNABLE_TO_INFER_GENERIC_FUNC` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` |  |  |  |  |
| Function | `INVALID_CALLED_OBJECT` | `checker` | `Function` | `部分覆盖` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionSemanticsChecker.kt` |  |  | 尚未定位稳定 inline 样例 |
| Function | `INVALID_RETURN` | `checker` | `Function` | `部分覆盖` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirReturnLegalityChecker.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` |  |  |  |
| Function | `INVALID_RETURN_IN_STATIC_INIT` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionSemanticsChecker.kt` |  |  | 尚未定位样例 |
| Function | `INVALID_SUBSCRIPT_ASSIGN_PARAMETER` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` |  |  |  |  |
| Function | `INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` |  |  |  |  |
| Function | `INVALID_SUBSCRIPT_ASSIGN_RETURN` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` |  |  |  |  |
| Function | `STATIC_FUNCTION_OVERLOAD_CONFLICTS` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionSemanticsChecker.kt` |  |  | 尚未定位样例 |
| Function | `USE_MUTABLE_FUNC_ALONE` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` |  |  |  |  |
| Function | `UNSAFE_FUNC_CAN_ONLY_BE_CALLED` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` |  |  |  |  |
| Function | `AMBIGUOUS_MATCH_PRIMITIVE_EXTEND` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` |  |  |  |  |
| Function | `CANNOT_HAVE_DEFAULT_PARAM` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` |  |  |  |  |
| Function | `TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` |  |  |  |  |
| Function | `LAMBDA_MUST_HAVE_TYPE_ANNOTATION` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionLambdaChecker.kt` |  |  | 尚未定位样例 |
| Function | `USE_FUNC_CAPTURE_VAR_ALONE` | `checker` | `Function` | `未复核` | `7.2`, `7.5` | `specs/function-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionLambdaChecker.kt` |  |  | 尚未定位样例 |
| Expression | `UNABLE_TO_INFER_EXPR` | `checker` | `Expression` | `部分覆盖` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` |  |  | 尚未定位稳定 inline 样例 |
| Expression | `EXCEED_FLOAT_LITERAL_RANGE` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `FLOAT_LITERAL_TOO_LARGE` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `FLOAT_LITERAL_TOO_SMALL` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `INVALID_UNARY_EXPR` | `checker` | `Expression` | `部分覆盖` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/testData/diagnostics/operator/unaryMinus.cj`; `cfir/analysis-tests/testData/diagnostics/operator/unaryNot.cj` |  | 需核对是否仍有 `UNRESOLVED_REFERENCE` 混报 |
| Expression | `INVALID_UNARY_EXPR_WITH_TARGET` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt` |  |  | 尚未定位样例 |
| Expression | `INVALID_SUBSCRIPT_EXPR` | `checker` | `Expression` | `部分覆盖` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt`; `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` | `cfir/analysis-tests/build/test-results/test/TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisDiagnosticsTestGenerated$Operator.xml` |  | 当前与旧期望存在 `UNRESOLVED_REFERENCE` / `INVALID_SUBSCRIPT_EXPR` 分歧 |
| Expression | `CANNOT_ASSIGN_TO_SUBSCRIPT` | `checker` | `Expression` | `部分覆盖` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` |  |  | 尚需补 dedicated inline 样例 |
| Expression | `NOT_MEMBER_OF` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `MEMBER_NOT_IMPORTED` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `CANNOT_ASSIGN_TO_IMMUTABLE` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `UNQUALIFIED_LEFT_VALUE_ASSIGNED` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `DIFFERENT_OR_PATTERN` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `VAR_IN_OR_PATTERN` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `VAR_IN_OR_CONDITION` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `UNREACHABLE_PATTERN` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `OPTIONAL_CHAIN_NON_OPTIONAL` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `CAPTURE_BEFORE_INITIALIZATION` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `INTERPOLATION_IN_CONST_PATTERN` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `CANNOT_REF_TO_PKG_NAME` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| Expression | `USE_EXPR_WITHOUT_IMPORT` | `checker` | `Expression` | `未复核` | `7.3`, `7.5` | `specs/expression-semantics-checker/spec.md` |  |  |  |  |
| GenericDeep | `GENERIC_TYPE_INCONSISTENT` | `resolve` | `GenericDeep-Inference` | `未复核` | `6.1`, `6.2`, `6.3`, `6.4` | `specs/generic-deep-checker/spec.md` |  |  |  |  |
| GenericDeep | `GENERIC_ARGUMENT_NO_MATCH` | `resolve` | `GenericDeep-Inference` | `未复核` | `6.1`, `6.2`, `6.3`, `6.4` | `specs/generic-deep-checker/spec.md` |  |  |  |  |
| GenericDeep | `GENERIC_CONSTRAINT_NOT_LOOSER` | `checker` | `GenericDeep` | `未复核` | `8.5`, `8.6` | `specs/generic-deep-checker/spec.md` |  |  |  |  |
| GenericDeep | `GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS` | `resolve` | `GenericDeep-Inference` | `未复核` | `6.1`, `6.2`, `6.3`, `6.4` | `specs/generic-deep-checker/spec.md` |  |  |  |  |
| GenericDeep | `GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY` | `checker` | `GenericDeep` | `未复核` | `8.5`, `8.6` | `specs/generic-deep-checker/spec.md` |  |  |  |  |
| GenericDeep | `GENERIC_PARAM_DIRECTLY_RECURSIVE` | `checker` | `GenericDeep` | `未复核` | `8.5`, `8.6` | `specs/generic-deep-checker/spec.md` |  |  |  |  |
| GenericDeep | `UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE` | `checker` | `GenericDeep` | `未复核` | `8.5`, `8.6` | `specs/generic-deep-checker/spec.md` |  |  |  |  |
| GenericDeep | `GENERIC_STATIC_ACCESS` | `checker` | `GenericDeep` | `未复核` | `8.5`, `8.6` | `specs/generic-deep-checker/spec.md` |  |  |  |  |
| GenericDeep | `PRIMITIVE_TYPE_AS_GENERICS_ARG` | `checker` | `GenericDeep` | `未复核` | `8.5`, `8.6` | `specs/generic-deep-checker/spec.md` |  |  |  |  |
| GenericDeep | `MEET_CONSTRAINT_INDIRECTLY` | `checker` | `GenericDeep` | `未复核` | `8.5`, `8.6` | `specs/generic-deep-checker/spec.md` |  |  |  |  |
| GenericDeep | `GENERIC_UPPER_BOUNDS_MUST_BE_JAVA_IN_JAVA` | `checker` | `GenericDeep` | `未复核` | `8.5`, `8.6` | `specs/generic-deep-checker/spec.md` |  |  |  |  |
| InheritanceDeep | `INHERIT_MEMBER_KIND_INCONSISTENT` | `checker` | `InheritanceDeep` | `未复核` | `8.1`, `8.6` | `specs/inheritance-deep-checker/spec.md` |  |  |  |  |
| InheritanceDeep | `INHERIT_SUPER_MEMBER_KIND_INCONSISTENT` | `checker` | `InheritanceDeep` | `未复核` | `8.1`, `8.6` | `specs/inheritance-deep-checker/spec.md` |  |  |  |  |
| InheritanceDeep | `INHERIT_MEMBER_TYPE_INCONSISTENT` | `checker` | `InheritanceDeep` | `未复核` | `8.1`, `8.6` | `specs/inheritance-deep-checker/spec.md` |  |  |  |  |
| InheritanceDeep | `INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC` | `checker` | `InheritanceDeep` | `未复核` | `8.1`, `8.6` | `specs/inheritance-deep-checker/spec.md` |  |  |  |  |
| InheritanceDeep | `INVALID_MEMBER_VISIBILITY_IN_CLASS` | `checker` | `InheritanceDeep` | `未复核` | `8.1`, `8.6` | `specs/inheritance-deep-checker/spec.md` |  |  |  |  |
| InheritanceDeep | `CANNOT_INHERIT_SEALED` | `checker` | `InheritanceDeep` | `未复核` | `8.1`, `8.6` | `specs/inheritance-deep-checker/spec.md` |  |  |  |  |
| InheritanceDeep | `INHERIT_THREAD_CONTEXT_INVALID` | `checker` | `InheritanceDeep` | `未复核` | `8.1`, `8.6` | `specs/inheritance-deep-checker/spec.md` |  |  |  |  |
| InheritanceDeep | `INHERIT_THREAD_CONTEXT_NOT_OPEN` | `checker` | `InheritanceDeep` | `未复核` | `8.1`, `8.6` | `specs/inheritance-deep-checker/spec.md` |  |  |  |  |
| InheritanceDeep | `INHERIT_NOT_RETURN_THIS` | `checker` | `InheritanceDeep` | `未复核` | `8.1`, `8.6` | `specs/inheritance-deep-checker/spec.md` |  |  |  |  |
| Spawn | `SPAWN_ARG_INVALID` | `checker` | `Spawn` | `未复核` | `9.7`, `9.10` | `specs/spawn-semantics-checker/spec.md` |  |  |  |  |
| Spawn | `SPAWN_ARG_NO_EFFECT` | `checker` | `Spawn` | `未复核` | `9.7`, `9.10` | `specs/spawn-semantics-checker/spec.md` |  |  |  |  |
| Interface | `INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL` | `checker` | `Interface` | `未复核` | `9.8`, `9.10` | `specs/interface-semantics-checker/spec.md` |  |  |  |  |
| ClassStructSemantics | `TYPE_UNINITIALIZED_STATIC_FIELD` | `checker` | `ClassStruct` | `未复核` | `8.2`, `8.6` | `specs/class-struct-semantics-checker/spec.md` |  |  |  |  |
| ClassStructSemantics | `INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER` | `checker` | `ClassStruct` | `未复核` | `8.2`, `8.6` | `specs/class-struct-semantics-checker/spec.md` |  |  |  |  |
| ClassStructSemantics | `NON_ABSTRACT_CLASS_CANNOT_BE_SEALED` | `checker` | `ClassStruct` | `未复核` | `8.2`, `8.6` | `specs/class-struct-semantics-checker/spec.md` |  |  |  |  |
| ClassStructSemantics | `STATIC_VARIABLE_USE_GENERIC_PARAMETER` | `checker` | `ClassStruct` | `未复核` | `8.2`, `8.6` | `specs/class-struct-semantics-checker/spec.md` |  |  |  |  |
| ClassStructSemantics | `CSTRUCT_CANNOT_IMPL_INTERFACES` | `checker` | `ClassStruct` | `未复核` | `8.2`, `8.6` | `specs/class-struct-semantics-checker/spec.md` |  |  |  |  |
| ClassStructSemantics | `EXPORT_SAME_PRIVATE_DECL` | `checker` | `ClassStruct` | `未复核` | `8.2`, `8.6` | `specs/class-struct-semantics-checker/spec.md` |  |  |  |  |
| ExtendExtra | `EXTEND_FUNCTION_CANNOT_OVERRIDDEN` | `checker` | `ExtendExtra` | `未复核` | `9.6`, `9.10` | `specs/extend-extra-checker/spec.md` |  |  |  |  |
| ExtendExtra | `EXTEND_MEMBER_CANNOT_SHADOW` | `checker` | `ExtendExtra` | `未复核` | `9.6`, `9.10` | `specs/extend-extra-checker/spec.md` |  |  |  |  |
| ExtendExtra | `EXTEND_ILLEGAL_MEMBER` | `checker` | `ExtendExtra` | `未复核` | `9.6`, `9.10` | `specs/extend-extra-checker/spec.md` |  |  |  |  |
| ExtendExtra | `EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE` | `checker` | `ExtendExtra` | `未复核` | `9.6`, `9.10` | `specs/extend-extra-checker/spec.md` |  |  |  |  |
| ExtendExtra | `EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND` | `checker` | `ExtendExtra` | `未复核` | `9.6`, `9.10` | `specs/extend-extra-checker/spec.md` |  |  |  |  |
| ExtendExtra | `EXTEND_A_JAVA_TYPE` | `checker` | `ExtendExtra` | `未复核` | `9.6`, `9.10` | `specs/extend-extra-checker/spec.md` |  |  |  |  |
| ExtendExtra | `EXTEND_REF_TARGET_CANNOT_BE_JAVA_IMPL` | `checker` | `ExtendExtra` | `未复核` | `9.6`, `9.10` | `specs/extend-extra-checker/spec.md` |  |  |  |  |
| ExtendExtra | `TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE` | `checker` | `ExtendExtra` | `未复核` | `9.6`, `9.10` | `specs/extend-extra-checker/spec.md` |  |  |  |  |
| Property | `PROPERTY_MUST_HAVE_ACCESSORS` | `checker` | `Property` | `未复核` | `8.3`, `8.6` | `specs/property-semantics-checker/spec.md` |  |  |  |  |
| Property | `IMMUTABLE_PROPERTY_WITH_SETTER` | `checker` | `Property` | `未复核` | `8.3`, `8.6` | `specs/property-semantics-checker/spec.md` |  |  |  |  |
| Property | `PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT` | `checker` | `Property` | `未复核` | `8.3`, `8.6` | `specs/property-semantics-checker/spec.md` |  |  |  |  |
| Property | `PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT` | `checker` | `Property` | `未复核` | `8.3`, `8.6` | `specs/property-semantics-checker/spec.md` |  |  |  |  |
| Property | `PROPERTY_MUST_IMPLEMENT_BOTH` | `checker` | `Property` | `未复核` | `8.3`, `8.6` | `specs/property-semantics-checker/spec.md` |  |  |  |  |
| ConstDeclaration | `EXPECT_CONST` | `checker` | `ConstDeclaration` | `未复核` | `8.4`, `8.6` | `specs/const-declaration-checker/spec.md` |  |  |  |  |
| ConstDeclaration | `CANNOT_DEFINE_VAR_IN_CONST_FUNCTION` | `checker` | `ConstDeclaration` | `未复核` | `8.4`, `8.6` | `specs/const-declaration-checker/spec.md` |  |  |  |  |
| ConstDeclaration | `NO_CONST_INIT` | `checker` | `ConstDeclaration` | `未复核` | `8.4`, `8.6` | `specs/const-declaration-checker/spec.md` |  |  |  |  |
| ConstDeclaration | `CLASS_CONST_INIT_WITH_VAR` | `checker` | `ConstDeclaration` | `未复核` | `8.4`, `8.6` | `specs/const-declaration-checker/spec.md` |  |  |  |  |
| AnnotationExtra | `ANNOTATION_ARG_TARGET` | `checker` | `AnnotationExtra` | `未复核` | `9.1`, `9.10` | `specs/annotation-extra-checker/spec.md` |  |  |  |  |
| AnnotationExtra | `ANNOTATION_ARG_TARGET_ARRAY_LIT` | `checker` | `AnnotationExtra` | `未复核` | `9.1`, `9.10` | `specs/annotation-extra-checker/spec.md` |  |  |  |  |
| AnnotationExtra | `ANNOTATION_NON_PUBLIC` | `checker` | `AnnotationExtra` | `未复核` | `9.1`, `9.10` | `specs/annotation-extra-checker/spec.md` |  |  |  |  |
| AnnotationExtra | `ANNOTATION_CUSTOM_PLACE` | `checker` | `AnnotationExtra` | `未复核` | `9.1`, `9.10` | `specs/annotation-extra-checker/spec.md` |  |  |  |  |
| AnnotationExtra | `ANNOTATION_ERROR_ARG_NUM` | `checker` | `AnnotationExtra` | `未复核` | `9.1`, `9.10` | `specs/annotation-extra-checker/spec.md` |  |  |  |  |
| AnnotationExtra | `ANNOTATION_ERROR_ARG_RANGE` | `checker` | `AnnotationExtra` | `未复核` | `9.1`, `9.10` | `specs/annotation-extra-checker/spec.md` |  |  |  |  |
| AnnotationExtra | `ANNOTATION_ERROR_OBJECT` | `checker` | `AnnotationExtra` | `未复核` | `9.1`, `9.10` | `specs/annotation-extra-checker/spec.md` |  |  |  |  |
| AnnotationExtra | `CANNOT_USE_ANNOTATION_JFFI` | `checker` | `AnnotationExtra` | `未复核` | `9.1`, `9.10` | `specs/annotation-extra-checker/spec.md` |  |  |  |  |
| AnnotationExtra | `ANNOTATION_NOT_APPLICABLE_JFFI` | `checker` | `AnnotationExtra` | `未复核` | `9.1`, `9.10` | `specs/annotation-extra-checker/spec.md` |  |  |  |  |
| Inout | `INOUT_MODIFY_CSTRING_OR_ZEROSIZED` | `checker` | `Inout` | `未复核` | `9.2`, `9.10` | `specs/inout-semantics-checker/spec.md` |  |  |  |  |
| Inout | `INOUT_MODIFY_NON_CTYPE` | `checker` | `Inout` | `未复核` | `9.2`, `9.10` | `specs/inout-semantics-checker/spec.md` |  |  |  |  |
| Inout | `INOUT_MUST_BE_VAR_VARIABLE` | `checker` | `Inout` | `未复核` | `9.2`, `9.10` | `specs/inout-semantics-checker/spec.md` |  |  |  |  |
| Inout | `INOUT_MODIFY_HEAP_VARIABLE` | `checker` | `Inout` | `未复核` | `9.2`, `9.10` | `specs/inout-semantics-checker/spec.md` |  |  |  |  |
| Inout | `INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING` | `checker` | `Inout` | `未复核` | `9.2`, `9.10` | `specs/inout-semantics-checker/spec.md` |  |  |  |  |
| Inout | `INOUT_MISMATCH` | `checker` | `Inout` | `未复核` | `9.2`, `9.10` | `specs/inout-semantics-checker/spec.md` |  |  |  |  |
| Inout | `INVALID_INOUT_ARGUMENT` | `checker` | `Inout` | `未复核` | `9.2`, `9.10` | `specs/inout-semantics-checker/spec.md` |  |  |  |  |
| Inout | `DUPLICATE_INOUT_ARGUMENT` | `checker` | `Inout` | `未复核` | `9.2`, `9.10` | `specs/inout-semantics-checker/spec.md` |  |  |  |  |
| VArrayExtra | `VARRAY_ARGS_NUMBER_MISMATCH` | `checker` | `VArrayExtra` | `未复核` | `9.3`, `9.10` | `specs/varray-extra-checker/spec.md` |  |  |  |  |
| VArrayExtra | `VARRAY_SUBSCRIPT_NUM` | `checker` | `VArrayExtra` | `未复核` | `9.3`, `9.10` | `specs/varray-extra-checker/spec.md` |  |  |  |  |
| VArrayExtra | `VARRAY_IN_CFUNC` | `checker` | `VArrayExtra` | `未复核` | `9.3`, `9.10` | `specs/varray-extra-checker/spec.md` |  |  |  |  |
| VArrayExtra | `VARRAY_ARG_TYPE_WITH_REFTYPE` | `checker` | `VArrayExtra` | `未复核` | `9.3`, `9.10` | `specs/varray-extra-checker/spec.md` |  |  |  |  |
| EffectsExtra | `RESUMPTION_HANDLE_TYPE_ERROR` | `checker` | `EffectsExtra` | `未复核` | `9.4`, `9.10` | `specs/effects-extra-checker/spec.md` |  |  |  |  |
| EffectsExtra | `RESUMPTION_INCORRECT_RETURN_TYPE` | `checker` | `EffectsExtra` | `未复核` | `9.4`, `9.10` | `specs/effects-extra-checker/spec.md` |  |  |  |  |
| EffectsExtra | `COMMAND_RESUMPTION_MISMATCH` | `checker` | `EffectsExtra` | `未复核` | `9.4`, `9.10` | `specs/effects-extra-checker/spec.md` |  |  |  |  |
| EffectsExtra | `RESUME_WRONG_RESUMPTION_TYPE` | `checker` | `EffectsExtra` | `未复核` | `9.4`, `9.10` | `specs/effects-extra-checker/spec.md` |  |  |  |  |
| EffectsExtra | `RETURN_IN_TRY_HANDLE_BLOCK` | `checker` | `EffectsExtra` | `未复核` | `9.4`, `9.10` | `specs/effects-extra-checker/spec.md` |  |  |  |  |
| EffectsExtra | `USELESS_COMMAND_TYPE` | `checker` | `EffectsExtra` | `未复核` | `9.4`, `9.10` | `specs/effects-extra-checker/spec.md` |  |  |  |  |
| Deprecated | `DEPRECATED_ERROR` | `checker` | `Deprecated` | `未复核` | `9.5`, `9.10` | `specs/deprecated-semantics-checker/spec.md` |  |  |  |  |
| Deprecated | `DEPRECATED_WARNING` | `checker` | `Deprecated` | `未复核` | `9.5`, `9.10` | `specs/deprecated-semantics-checker/spec.md` |  |  |  |  |
| Deprecated | `DEPRECATION_WEAKENING` | `checker` | `Deprecated` | `未复核` | `9.5`, `9.10` | `specs/deprecated-semantics-checker/spec.md` |  |  |  |  |
| Deprecated | `DEPRECATION_OVERRIDE_ERROR` | `checker` | `Deprecated` | `未复核` | `9.5`, `9.10` | `specs/deprecated-semantics-checker/spec.md` |  |  |  |  |
| Deprecated | `DEPRECATION_OVERRIDE_WARNING` | `checker` | `Deprecated` | `未复核` | `9.5`, `9.10` | `specs/deprecated-semantics-checker/spec.md` |  |  |  |  |
| Deprecated | `DEPRECATION_REDEF_ERROR` | `checker` | `Deprecated` | `未复核` | `9.5`, `9.10` | `specs/deprecated-semantics-checker/spec.md` |  |  |  |  |
| Deprecated | `DEPRECATION_REDEF_WARNING` | `checker` | `Deprecated` | `未复核` | `9.5`, `9.10` | `specs/deprecated-semantics-checker/spec.md` |  |  |  |  |
| CommonSpecific | `COMMON_OPEN_CLASS_NO_INIT` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `MULTIPLE_COMMON_IMPLEMENTATIONS` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `NOT_MATCHED` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_VAR_NOT_MATCH_LET` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_INIT_COMMON_PRIMARY_CONSTRUCTOR` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_HAS_DIFFERENT_KIND` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_PRIMARY_UNMATCHED_VAR_DECL` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_HAS_DIFFERENT_TYPE` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_HAS_DIFFERENT_MODIFIER` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_HAS_DIFFERENT_ANNOTATION` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_HAS_DEPRECATED_ANNOTATION` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_HAS_DIFFERENT_PARAMETER` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_HAS_DIFFERENT_SUPER_TYPE` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `SPECIFIC_HAS_DUPLICATE_EXTENSIONS` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `COMMON_PACKAGE_HAS_MAIN` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `COMMON_STATIC_LET_CANT_BE_INITIALIZED_IN_STATIC_INIT` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `COMMON_GENERIC_FROZEN_NOT_SUPPORTED` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `COMMON_GENERIC_RENAME_NOT_SUPPORTED` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| CommonSpecific | `COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED` | `checker` | `CommonSpecific` | `未复核` | `11.1`, `11.3` | `specs/common-specific-checker/spec.md` |  |  |  |  |
| JavaInterop | `JAVA_INCORRECT_USE_BETWEEN_TYPES` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `JAVA_NON_JTYPE` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `JAVA_INVALID_UNIT` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `JAVA_APP_INHERIT_EXT` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `JAVA_UNSUPPORTED_DECL` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `MISSING_JAVA_INTEROP_ANNOTATION` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `SHADOW_CANNOT_IN_TYPE_ARGS` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `UNSUPPORTED_TYPE_ARGUMENT_IN_JAVA_INTEROP` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `STATIC_MEMBER_IN_INTERFACE_MUST_HAS_BODY` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `DEFINE_JAVA_ANNOTATION` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `INVALID_USE_OF_JAVA_ANNOTATION` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `INVALID_USE_OF_ANNOTATION_JFFI` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `VARIABLE_OF_JAVA_TYPE` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `GENERIC_PARAMETER_OF_JAVA_TYPE` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaInterop | `JAVA_INTEROP_NOT_SUPPORTED` | `checker` | `JavaInterop` | `未复核` | `10.1`, `10.10` | `specs/java-interop-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_MIRROR_CTOR_ARG_MUST_BE_JAVA_MIRROR` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_MIRROR_METHOD_ARG_MUST_BE_JAVA_MIRROR` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_MIRROR_METHOD_RET_UNSUPPORTED` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_MIRROR_PROP_MUST_BE_JAVA_MIRROR` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_MIRROR_SUBTYPE_MUST_BE_ANNOTATED` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_MIRROR_CANNOT_INHERIT_PURE_CANGJIE_TYPE` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_IMPL_CANNOT_INHERIT_PURE_CANGJIE_TYPE` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_MIRROR_SUBTYPE_ANNO_MUST_INHERIT_MIRROR` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_MIRROR_CANNOT_BE_EXTENDED_WITH_INTERFACE` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_IMPL_CANNOT_BE_EXTENDED_WITH_INTERFACE` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_IMPL_REDEFINITION` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_HAS_DEFAULT_ANNOTATION_ARGS` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_HAS_DEFAULT_ANNOTATION_IS_IN_WRONG_PLACE` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| JavaMirror | `JAVA_HAS_DEFAULT_CONFLICT_WITH_STATIC` | `checker` | `JavaMirror` | `未复核` | `10.2`, `10.10` | `specs/java-mirror-checker/spec.md` |  |  |  |  |
| CJMapping | `CJMAPPING_STRUCT_GENERIC_NOT_SUPPORTED` | `checker` | `CJMapping` | `未复核` | `10.3`, `10.10` | `specs/cjmapping-checker/spec.md` |  |  |  |  |
| CJMapping | `CJMAPPING_STRUCT_INHERITANCE_INTERFACE_NOT_SUPPORTED` | `checker` | `CJMapping` | `未复核` | `10.3`, `10.10` | `specs/cjmapping-checker/spec.md` |  |  |  |  |
| CJMapping | `CJMAPPING_DECL_NOT_SUPPORTED` | `checker` | `CJMapping` | `未复核` | `10.3`, `10.10` | `specs/cjmapping-checker/spec.md` |  |  |  |  |
| CJMapping | `CJMAPPING_METHOD_ARG_NOT_SUPPORTED` | `checker` | `CJMapping` | `未复核` | `10.3`, `10.10` | `specs/cjmapping-checker/spec.md` |  |  |  |  |
| CJMapping | `CJMAPPING_METHOD_RET_UNSUPPORTED` | `checker` | `CJMapping` | `未复核` | `10.3`, `10.10` | `specs/cjmapping-checker/spec.md` |  |  |  |  |
| CJMapping | `CJ_MAPPING_GENERIC_METHOD_NOT_GET_INSTANCE_CONFIG` | `checker` | `CJMapping` | `未复核` | `10.3`, `10.10` | `specs/cjmapping-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_INTEROP_CTOR_PARAM_MUST_BE_OBJC_COMPATIBLE` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_INTEROP_METHOD_PARAM_MUST_BE_OBJC_COMPATIBLE` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_INTEROP_METHOD_RET_MUST_BE_OBJC_COMPATIBLE` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_INTEROP_PROP_MUST_BE_OBJC_COMPATIBLE` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_INTEROP_FIELD_MUST_BE_OBJC_COMPATIBLE` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_MIRROR_DECL_CANNOT_INHERIT` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_MIRROR_SUBTYPE_CANNOT_MULTIPLE_INHERIT` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_MIRROR_SUBTYPE_MUST_BE_ANNOTATED` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_MIRROR_MUST_INHERIT_MIRROR` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_MIRROR_INTEROPLIB_MUST_BE_IMPORTED` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_INTEROP_NOT_SUPPORTED` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_POINTER_ARGUMENT_MUST_BE_OBJC_COMPATIBLE` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_INTEROP_TOPLEVEL_PARAM_MUST_BE_OBJC_COMPATIBLE` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_INTEROP_TOPLEVEL_RET_MUST_BE_OBJC_COMPATIBLE` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_METHOD_MUST_HAVE_FOREIGN_NAME` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_CTOR_MUST_HAVE_FOREIGN_NAME` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_FUNC_ARGUMENT_MUST_BE_OBJC_COMPATIBLE` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_FUNC_CALL_PROPERTY_CAN_ONLY_BE_CALLED` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCInterop | `OBJC_SETTER_NAME_ON_IMMUTABLE_PROP` | `checker` | `ObjCInterop` | `未复核` | `10.4`, `10.10` | `specs/objc-interop-checker/spec.md` |  |  |  |  |
| ObjCCJMapping | `OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED` | `checker` | `ObjCCJMapping` | `未复核` | `10.5`, `10.10` | `specs/objc-cjmapping-checker/spec.md` |  |  |  |  |
| ObjCCJMapping | `OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED` | `checker` | `ObjCCJMapping` | `未复核` | `10.5`, `10.10` | `specs/objc-cjmapping-checker/spec.md` |  |  |  |  |
| ForeignName | `FOREIGN_NAME_APPEARED_IN_CHILD` | `checker` | `ForeignName` | `未复核` | `10.6`, `10.10` | `specs/foreign-name-checker/spec.md` |  |  |  |  |
| ForeignName | `FOREIGN_NAME_CONFLICTING_ANNOTATION` | `checker` | `ForeignName` | `未复核` | `10.6`, `10.10` | `specs/foreign-name-checker/spec.md` |  |  |  |  |
| ForeignName | `FOREIGN_NAME_CONFLICTING_DERIVED_ANNOTATION` | `checker` | `ForeignName` | `未复核` | `10.6`, `10.10` | `specs/foreign-name-checker/spec.md` |  |  |  |  |
| IfAvailable | `IFAVAILABLE_ARG_NO_NAME` | `checker` | `IfAvailable` | `未复核` | `10.7`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| IfAvailable | `IFAVAILABLE_ARG_NOT_LITERAL` | `checker` | `IfAvailable` | `未复核` | `10.7`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| IfAvailable | `IFAVAILABLE_UNKNOWN_ARG_NAME` | `checker` | `IfAvailable` | `未复核` | `10.7`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| IfAvailable | `IFAVAILABLE_LEVEL_LIMIT` | `checker` | `IfAvailable` | `未复核` | `10.7`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| APILevel | `APILEVEL_MULTI_ANNO` | `checker` | `APILevel` | `未复核` | `10.8`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| APILevel | `APILEVEL_MISSING_ARG` | `checker` | `APILevel` | `未复核` | `10.8`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| APILevel | `ONLY_LITERAL_SUPPORT` | `checker` | `APILevel` | `未复核` | `10.8`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| APILevel | `APILEVEL_REF_HIGHER` | `checker` | `APILevel` | `未复核` | `10.8`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| APILevel | `APILEVEL_SYSCAP_WARNING` | `checker` | `APILevel` | `未复核` | `10.8`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| APILevel | `APILEVEL_SYSCAP_ERROR` | `checker` | `APILevel` | `未复核` | `10.8`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| APILevel | `APILEVEL_MULTI_DIFF_SYSCAP` | `checker` | `APILevel` | `未复核` | `10.8`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| Hide | `HIDE_MULTI_ANNOTATION` | `checker` | `Hide` | `未复核` | `10.9`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| Hide | `HIDE_AT_FUNC_PARAM` | `checker` | `Hide` | `未复核` | `10.9`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| Hide | `HIDE_MISSING_HIDE` | `checker` | `Hide` | `未复核` | `10.9`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| Hide | `HIDE_COMPILE_TIME_INVISIBLE` | `checker` | `Hide` | `未复核` | `10.9`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| Hide | `HIDE_DIFF_PARAM` | `checker` | `Hide` | `未复核` | `10.9`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| Hide | `HIDE_MUST_AT_END` | `checker` | `Hide` | `未复核` | `10.9`, `10.10` | `specs/if-available-api-level-hide-checker/spec.md` |  |  |  |  |
| Unused | `UNUSED_IMPORT` | `checker` | `Unused` | `未复核` | `9.9`, `9.10` | `specs/unused-checker/spec.md` |  |  |  |  |
| Mock | `MOCK_DISABLED` | `checker` | `Mock` | `未复核` | `11.2`, `11.3` | `specs/mock-semantics-checker/spec.md` |  |  |  |  |
| Mock | `MOCK_NOT_IN_TEST_MODE` | `checker` | `Mock` | `未复核` | `11.2`, `11.3` | `specs/mock-semantics-checker/spec.md` |  |  |  |  |
| Mock | `MOCK_UNSUPPORTED_TYPE` | `checker` | `Mock` | `未复核` | `11.2`, `11.3` | `specs/mock-semantics-checker/spec.md` |  |  |  |  |
| Mock | `MOCK_WRONG_STATIC_DECL` | `checker` | `Mock` | `未复核` | `11.2`, `11.3` | `specs/mock-semantics-checker/spec.md` |  |  |  |  |
| Mock | `MOCK_DOESNT_SUPPORT_MOCKING` | `checker` | `Mock` | `未复核` | `11.2`, `11.3` | `specs/mock-semantics-checker/spec.md` |  |  |  |  |
| Mock | `MOCK_FROZEN_UNSUPPORTED` | `checker` | `Mock` | `未复核` | `11.2`, `11.3` | `specs/mock-semantics-checker/spec.md` |  |  |  |  |
| Mock | `MOCK_FROZEN_REQUIRED` | `checker` | `Mock` | `未复核` | `11.2`, `11.3` | `specs/mock-semantics-checker/spec.md` |  |  |  |  |
