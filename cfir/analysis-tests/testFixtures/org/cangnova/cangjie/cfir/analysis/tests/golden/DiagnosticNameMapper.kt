/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.tests.golden

/**
 * 诊断名映射表：项目 UPPER_CASE 名 ↔ cjc snake_case DiagKind。
 *
 * 项目使用 `UPPER_CASE`（去掉 `CFIR_` 前缀后），cjc 使用 `sema_`/`parse_`/`package_` 前缀的 snake_case。
 * 映射不是 1:1 的——cjc 的 `sema_mismatched_types` 对应项目中的多个诊断。
 */
object DiagnosticNameMapper {

    /** 项目诊断名 → cjc DiagKind 的映射 */
    private val projectToCjc = mapOf(
        // ── TypeCheck / 类型不匹配（多对一：cjc 统一使用 sema_mismatched_types）──
        "TYPE_MISMATCH" to "sema_mismatched_types",
        "MISMATCHED_TYPES" to "sema_mismatched_types",
        "RETURN_TYPE_MISMATCH" to "sema_mismatched_types",
        "ARGUMENT_TYPE_MISMATCH" to "sema_mismatched_types",
        "ASSIGNMENT_TYPE_MISMATCH" to "sema_mismatched_types",
        "OVERRIDING_RETURN_TYPE_MISMATCH" to "sema_mismatched_types",
        "PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF" to "sema_property_override_implement_type_diff",
        "MISSING_ENTRY" to "sema_missing_entry",
        "PATTERN_INITIALIZER_TYPE_MISMATCH" to "sema_mismatched_types",
        "TRY_BRANCH_TYPE_MISMATCH" to "sema_mismatched_types",
        "COMMAND_INCOMPATIBLE_TYPE" to "sema_mismatched_types",
        "TYPE_INCOMPATIBLE" to "sema_type_incompatible",

        // ── Resolve ──
        "NO_CONSTRUCTOR" to "sema_no_match_constructor",
        "INVALID_ACCESS_CONTROL" to "sema_invalid_access_control",
        "REF_NOT_BE_TYPE" to "sema_ref_not_be_type",
        "UNDECLARED_TYPE_NAME" to "sema_undeclared_type_name",
        "UNRESOLVED_REFERENCE" to "sema_undeclared_identifier",
        "INVISIBLE_MEMBER" to "sema_no_matching_function",
        "INVISIBLE_REFERENCE" to "sema_undeclared_identifier",
        "CLASS_NOT_OPEN_FOR_INHERITANCE" to "sema_class_not_open_for_inheritance",
        "EXPLICIT_SUPER_CALL_REQUIRED" to "sema_explicit_super_call_required",
        "AMBIGUOUS_FUNCTION_CALL" to "sema_ambiguous_call",
        "AMBIGUOUS_CONSTRUCTOR_CALL" to "sema_ambiguous_constructor_call",
        "AMBIGUOUS_ARG_TYPE" to "sema_ambiguous_arg_type",
        "AMBIGUOUS_FUNCTION_REFERENCE" to "sema_ambiguous_function_reference",
        "NO_MATCHING_OPERATOR_INVOKE" to "sema_no_matching_operator_invoke",
        "NO_MATCH_OPERATOR_FUNCTION_CALL" to "sema_no_match_operator_function_call",
        "ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR" to "sema_enum_type_cannot_be_used_as_constructor",
        "CANNOT_OVERRIDE_INVISIBLE_MEMBER" to "sema_cannot_override_invisible_member",
        "CANNOT_OVERRIDE" to "sema_cannot_override",

        // ── Imports ──
        "UNRESOLVED_IMPORT" to "package_import_not_found",
        "IMPORT_CONFLICT" to "package_conflict_import",
        "IMPORT_ALIAS_CONFLICT" to "package_import_alias_conflict",

        // ── Throw/Catch ──
        "THROW_EXPR_WITH_WRONG_TYPE" to "sema_throw_expr_with_wrong_type",
        "CATCH_TYPE_MUST_EXTEND_EXCEPTION" to "sema_except_catch_type_error",
        "DUPLICATE_CATCH_BLOCK" to "sema_duplicate_catch_block",
        "UNREACHABLE_CATCH_BLOCK" to "sema_unreachable_catch_block",
        "TRY_RESOURCE_MUST_IMPLEMENT_RESOURCE" to "sema_try_resource_must_implement_resource",

        // ── DeclarationStatus / 修饰符 ──
        "STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE" to "sema_static_cannot_be_open_abstract_override",
        "MISSING_FUNC_BODY" to "sema_missing_func_body",
        "MUT_ONLY_ON_FUNCTION" to "sema_mut_only_on_function",
        "NOTHING_TO_OVERRIDE" to "sema_nothing_to_override",
        "INCOMPATIBLE_MODIFIERS" to "sema_incompatible_modifiers",
        "WRONG_MODIFIER_TARGET" to "sema_wrong_modifier_target",
        "DEPRECATED_MODIFIER_FOR_TARGET" to "sema_deprecated_modifier_for_target",
        "DEPRECATED_MODIFIER_PAIR" to "sema_deprecated_modifier_pair",
        "DEPRECATED_MODIFIER_CONTAINING_DECLARATION" to "sema_deprecated_modifier_containing_declaration",

        // ── Redeclaration ──
        "CLASSIFIER_REDECLARATION" to "sema_redeclaration",
        "REDECLARATION" to "sema_redeclaration",
        "CONFLICTING_OVERLOADS" to "sema_conflicting_overloads",

        // ── Inheritance ──
        "INTERFACE_CANNOT_INHERIT_CLASS" to "sema_interface_cannot_inherit_class",
        "MULTIPLE_CLASS_SUPER_TYPES" to "sema_multiple_class_inheritance",
        "OVERRIDE_STATIC_ERROR" to "sema_override_static_error",
        "SUPER_TYPES_DUPLICATE" to "sema_super_types_duplicate",
        "SUPER_TYPES_SELF_REFERENCE" to "sema_super_types_self_reference",
        "ENUM_SUPER_NOT_ALLOWED" to "sema_enum_super_not_allowed",
        "STRUCT_SUPER_NOT_ALLOWED" to "sema_struct_super_not_allowed",
        "INTERFACE_SUPER_NOT_ALLOWED" to "sema_interface_super_not_allowed",
        "INVALID_MEMBER_VISIBILITY_IN_CLASS" to "sema_invalid_member_visibility_in_class",
        "INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE" to "sema_incompatible_mut_modifier_between_struct_and_interface",
        "ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY" to "sema_abstract_method_cannot_be_accessed_directly",

        // ── Constructor ──
        "ILLEGAL_THIS_OR_SUPER_CALL" to "sema_illegal_this_or_super_call",
        "MULTIPLE_PRIMARY_CONSTRUCTORS" to "sema_multiple_primary_constructors",
        "ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER" to "sema_illegal_place_of_calling_this_or_super",
        "ILLEGAL_PLACE_OF_CALLING_THIS_PRIMARY_CONSTRUCTOR" to "sema_illegal_place_of_calling_this_primary_constructor",
        "NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS" to "sema_no_non_param_constructor_in_super_class",
        "RECURSIVE_CONSTRUCTOR_CALL" to "sema_recursive_constructor_call",
        "VALUE_TYPE_RECURSIVE" to "sema_value_type_recursive",

        // ── Call / Arguments ──
        "TOO_MANY_ARGUMENTS" to "sema_too_many_arguments",
        "NO_VALUE_FOR_PARAMETER" to "sema_no_value_for_parameter",
        "ARGUMENT_PASSED_TWICE" to "sema_argument_passed_twice",
        "NAMED_ARGUMENTS_NOT_ALLOWED" to "sema_named_arguments_not_allowed",
        "NAMED_PARAMETER_NOT_FOUND" to "sema_named_parameter_not_found",
        "MIXING_NAMED_AND_POSITIONAL_ARGUMENTS" to "sema_mixing_named_and_positional_arguments",
        "NEED_NAMED_ARGUMENT" to "sema_need_named_argument",

        // ── Inference ──
        "NEW_INFERENCE_ERROR" to "sema_new_inference_error",
        "CANNOT_INFER_PARAMETER_TYPE" to "sema_cannot_infer_parameter_type",
        "ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED" to "sema_empty_arrayLit_type_undefined",
        "BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION" to "sema_builder_inference_multi_lambda_restriction",
        "INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION" to "sema_inferred_type_variable_into_empty_intersection",
        "INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION" to "sema_inferred_type_variable_into_possible_empty_intersection",

        // ── Generics / Constraints ──
        "GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT" to "sema_generic_type_without_type_argument",
        "GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT" to "sema_generic_type_argument_not_match_constraint",
        "GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS" to "sema_generic_no_member_match_in_upper_bounds",
        "GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS" to "sema_generic_no_method_match_in_upper_bounds",
        "CONFLICTING_UPPER_BOUNDS" to "sema_conflicting_upper_bounds",
        "NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER" to "sema_name_in_constraint_is_not_a_type_parameter",
        "ONLY_ONE_CLASS_BOUND_ALLOWED" to "sema_only_one_class_bound_allowed",
        "REPEATED_BOUND" to "sema_repeated_bound",
        "CANNOT_CURRYING" to "sema_cannot_currying",
        "CANNOT_HAVE_PARAMETER" to "sema_cannot_have_parameter",
        "FORBID_GENERIC_FINALIZER" to "sema_forbid_generic_finalizer",
        "FINALIZER_FORBIDDEN_IN_CLASS" to "sema_finalizer_forbidden_in_class",

        // ── Jump / Loop ──
        "INVALID_LOOP_CONTROL" to "sema_invalid_loop_control",

        // ── Mut / Immutable ──
        "IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION" to "sema_immutable_function_cannot_access_mutable_function",
        "CANNOT_MODIFY_VAR" to "sema_cannot_modify_var",

        // ── Initialization ──
        "USED_BEFORE_INITIALIZATION" to "sema_used_before_initialization",

        // ── Match / Pattern ──
        "MATCH_CASE_HAS_NO_TYPE" to "sema_match_case_has_no_type",
        "NOT_OVERLOAD_IN_MATCH" to "sema_not_overload_in_match",
        "NOT_MATCHED" to "sema_not_matched",
        "NON_EXHAUSTIVE_MATCH" to "sema_non_exhaustive_match",
        "ENUM_PATTERN_PARAM_SIZE_ERROR" to "sema_enum_pattern_param_size_error",
        "TUPLE_PATTERN_NOT_MATCH" to "sema_tuple_pattern_not_match",
        "PATTERN_NOT_MATCH" to "sema_pattern_not_match",

        // ── Const eval ──
        "CONST_EVAL_ARITHMETIC_OVERFLOW" to "sema_const_eval_arithmetic_overflow",
        "CONST_EVAL_DIVIDE_BY_ZERO" to "sema_const_eval_divide_by_zero",
        "CONST_EVAL_NEGATIVE_SHIFT_COUNT" to "sema_const_eval_negative_shift_count",
        "CONST_EVAL_SHIFT_COUNT_OVERFLOW" to "sema_const_eval_shift_count_overflow",
        "LITERAL_NUMERIC_OVERFLOW" to "sema_literal_numeric_overflow",

        // ── Operator ──
        "INVALID_BINARY_OPERATOR" to "sema_invalid_binary_expr",

        // ── Inout ──
        "INOUT_MUST_BE_VAR_VARIABLE" to "sema_inout_must_be_var_variable",
        "DUPLICATE_INOUT_ARGUMENT" to "sema_duplicate_inout_argument",
        "INVALID_INOUT_ARGUMENT" to "sema_invalid_inout_argument",

        // ── Effects / Handle ──
        "MISMATCHING_HANDLE_BLOCK" to "sema_mismatching_handle_block",
        "COMMAND_HANDLE_TYPE_ERROR" to "sema_command_handle_type_error",
        "RESUME_NO_WITH" to "sema_resume_no_with",
        "RESUME_THROWING_MISMATCH_TYPE" to "sema_resume_throwing_mismatch_type",
        "IMPLICIT_RESUME_OUTSIDE_HANDLER" to "sema_implicit_resume_outside_handler",
        "RETURN_IN_HANDLE_BLOCK" to "sema_return_in_try_handle_block",

        // ── Range ──
        "RANGE_STEP_NOT_INT" to "sema_range_step_not_int",
        "RANGE_STEP_CANNOT_BE_ZERO" to "sema_range_step_cannot_be_zero",
        "RANGE_DIRECTION_STEP_MISMATCH" to "sema_range_direction_step_mismatch",
        "INCONSISTENCY_RANGE_ELEMENT_TYPE" to "sema_inconsistency_range_element_type",

        // ── Interop / Foreign ──
        "INVALID_CFUNC_RETURN_TYPE" to "sema_invalid_cfunc_return_type",
        "INVALID_CFUNC_PARAMETER_TYPE" to "sema_invalid_cfunc_parameter_type",
        "INVALID_CALLING_CONVENTION_TARGET" to "sema_invalid_calling_convention_target",
        "EXTEND_JAVA_TYPE_NOT_ALLOWED" to "sema_extend_a_java_type",
        "OBJC_METHOD_MUST_HAVE_FOREIGN_NAME" to "sema_objc_method_must_have_foreign_name",
        "OBJC_IMPL_MUST_INHERIT_MIRROR" to "sema_objc_impl_must_have_objc_mirror_super_class",
        "OBJC_CJ_MAPPING_MUST_BE_CLASS" to "sema_objc_cj_mapping_must_be_class",

        // ── Mock ──
        "MOCK_DISABLED" to "sema_mock_disabled",
        "MOCK_NOT_IN_TEST_MODE" to "sema_mock_not_in_test_mode",
        "MOCK_UNSUPPORTED_TYPE" to "sema_mock_unsupported_type",

        // ── Annotation ──
        "ANNOTATION_NO_CONST_INIT" to "sema_annotation_no_const_init",

        // ── Varray ──
        "VARRAY_SIZE_MISMATCH" to "sema_varray_size_mismatch",
        "VARRAY_IN_CFUNC" to "sema_varray_in_cfunc",
        "VARRAY_SUBSCRIPT_NUM" to "sema_varray_subscript_num",
        "VARRAY_ARGS_NUMBER_MISMATCH" to "sema_varray_args_number_mismatch",
        "VARRAY_ARG_TYPE_WITH_REFTYPE" to "sema_varray_arg_type_with_reftype",
        "BUILTIN_INDEX_IN_BOUND" to "sema_builtin_index_in_bound",

        // ── Common/Specific ──
        "COMMON_OPEN_CLASS_NO_INIT" to "parse_cjmp_in_common_ctor_required",
        "SPECIFIC_HAS_DIFFERENT_TYPE" to "sema_specific_has_different_kind",
        "PARAM_NAMED_MISMATCHED" to "sema_param_named_mismatched",

        // ── Finally ──
        "FINALLY_BLOCK_MUST_NOT_PRODUCE_RESULT" to "sema_finally_block_must_not_produce_result",

        // ── General ──
        "CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE" to "sema_core_object_not_found_when_no_prelude",
        "TYPEALIAS_UNUSED_TYPE_PARAMETERS" to "typealias_unused_type_parameters",
        "TYPEALIAS_CYCLE" to "sema_typealias_cycle",
    )

    /** cjc DiagKind → 项目诊断名集合（一对多） */
    private val cjcToProject: Map<String, Set<String>> = buildMap<String, MutableSet<String>> {
        for ((proj, cjc) in projectToCjc) {
            getOrPut(cjc) { mutableSetOf() }.add(proj)
        }
    }

    /**
     * 将项目诊断名转换为对应的 cjc DiagKind（如有映射）。
     */
    fun projectToCjcKind(projectName: String): String? = projectToCjc[projectName]

    /**
     * 将 cjc DiagKind 转换为对应的项目诊断名集合。
     */
    fun cjcKindToProjectNames(cjcKind: String): Set<String> = cjcToProject[cjcKind] ?: emptySet()

    /**
     * 从 cjc DiagKind 推断项目风格名称（启发式转换，用于报告显示）。
     *
     * 去掉 `sema_`/`parse_`/`package_` 前缀后转大写。
     */
    fun cjcKindToHeuristicName(cjcKind: String): String {
        val stripped = cjcKind
            .removePrefix("sema_")
            .removePrefix("parse_")
            .removePrefix("package_")
        return stripped.uppercase()
    }
}
