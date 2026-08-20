# 仓颉官方编译器诊断消息保护清单

本文档是官方诊断定义的仓库内快照，供诊断名称和消息对照使用；它不是本项目的诊断支持矩阵，也不保证每个条目都由 CFIR 实现。语言与诊断的权威来源是官方仓颉编译器和官方语言资料。

## 1. 来源与快照边界

- 官方实现来源：`external/cangjie_compiler/include/cangjie/Basic`。
- 定义入口：`DiagnosticsAll.def`（legacy 诊断集）和 `DiagRefactor/DiagnosticAll.def`（refactor 诊断集）。
- 本快照提取于 2026-03-16；更新 `external/cangjie_compiler` 时必须重新提取、复核数量并提交快照差异。
- 对具体诊断的位置、严重性和触发条件，应使用待验证的源码调用官方 `cjc`；不能只从本目录的消息文本推断语义。

## 2. 统计

- Legacy 总条目：292
- Refactor 总条目：730
- 合计条目：1022
- Legacy 分布：ERROR=281, NOTE=6, WARNING=5
- Refactor 分布：ERROR=665, WARNING=65

## 3. 本项目当前 CFIR 诊断文案入口

- 文件：`cfir/diagnostics/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt`
- 当前渲染条目：

| Key | Template |
|---|---|
| `CfirErrors.INVALID_DECLARATION` | {0} |
| `CfirErrors.TYPES_ERROR_RECOVERY` | {1} |
| `CfirErrors.IMPORT_TARGET_NOT_FOUND` | {1} |
| `CfirErrors.IMPORT_CONFLICT` | {1} |
| `CfirErrors.IMPORT_ALIAS_CONFLICT` | {1} |
| `CfirErrors.SUPER_TYPES_SELF_REFERENCE` | {1} |
| `CfirErrors.SUPER_TYPES_DUPLICATE` | {1} |
| `CfirErrors.ILLEGAL_EXTENDED_TYPE` | {1} |
| `CfirErrors.EXTEND_DUPLICATE_INTERFACE` | {1} |
| `CfirErrors.EXTEND_NOT_INTERFACE` | {1} |
| `CfirErrors.INTERFACE_CANNOT_INHERIT_CLASS` | {1} |
| `CfirErrors.MULTIPLE_CLASS_SUPER_TYPES` | {1} |
| `CfirErrors.STATUS_MODIFIER_LEGALITY` | {1} |
| `CfirErrors.NON_EXHAUSTIVE_MATCH` | match expression is not exhaustive. Missing cases: {0} |
| `TYPE_MISMATCH` | Type mismatch: inferred type is ''{1}'', but ''{0}'' was expected. |
| `ARGUMENT_TYPE_MISMATCH` | Argument type mismatch: actual type is ''{0}'', but ''{1}'' was expected. |
| `RETURN_TYPE_MISMATCH` | Return type mismatch: expected ''{0}'', actual ''{1}''. |

## 4. 按指定模块目录速查

本节按你指定的五个维度拆分：`Sema / Parser / Lexer / Macro / CHIR`。

### 4.1 `Sema`

- Count: 520

| Set | Severity | ID | Message |
|---|---|---|---|
| Basic/DiagnosticsAll.def | ERROR | `sema_diag_begin` |  |
| Basic/DiagnosticsAll.def | ERROR | `sema_diag_report_error_message` | %s |
| Basic/DiagnosticsAll.def | NOTE | `sema_diag_report_note_message` | %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_incompatible_expo_target_type` | the type of an exponentiation expression is either 'Int64' or 'Float64', which conflicts the type '%s' required  \| by the context |
| Basic/DiagnosticsAll.def | ERROR | `sema_ambiguous_expo_right_operand_type` | both 'Int64' and 'Float64' is compatible with the right operand's type of this exponentiation (assignment)  \| expression; please give an explicit one |
| Basic/DiagnosticsAll.def | ERROR | `sema_not_a_type` | '%s' is not a type |
| Basic/DiagnosticsAll.def | ERROR | `sema_incompatible_func_body_and_return_type` | the return type of this function cannot be calculated from the function body and all the return expressions |
| Basic/DiagnosticsAll.def | ERROR | `sema_type_must_toplevel` | %s type must be toplevel |
| Basic/DiagnosticsAll.def | ERROR | `sema_undeclared_type_name` | undeclared type name '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_undeclared_identifier` | undeclared identifier '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_redefinition` | redefinition of declaration '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_expand_macro_redefinition` | redefinition of macro '%s' |
| Basic/DiagnosticsAll.def | NOTE | `sema_previous_decl` | '%s' is previously declared here |
| Basic/DiagnosticsAll.def | ERROR | `sema_used_before_initialization` | variable '%s' is used before initialization |
| Basic/DiagnosticsAll.def | ERROR | `sema_global_var_used_before_initialization` | global/static variable '%s' is used before initialization during initializing '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_duplicated_item_in_enum` | '%s' is already exist in enum '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_constructor_in_enum` | invalid constructor in enum declaration |
| Basic/DiagnosticsAll.def | ERROR | `sema_multiple_constructor_in_enum` | find multiple constructor '%s' of enum declaration |
| Basic/DiagnosticsAll.def | ERROR | `sema_enum_constructor_type_not_match` | no matching enum constructor '%s' for given arguments |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_enum_member_access` | base of member access can not be enum variable |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_type_param_of_enum_member_access` | type arguments cannot appear after '%s' when enum type '%s' is given |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_loop_control` | 'break' or 'continue' must be used inside a loop |
| Basic/DiagnosticsAll.def | ERROR | `sema_cannot_currying` | %s cannot have more than one parameter list |
| Basic/DiagnosticsAll.def | ERROR | `sema_value_type_recursive` | value type recursive detected: '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_inheritance_cycle` | inheritance cycle detected: '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_typealias_cycle` | type cycle detected: '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_typealias_external_refer_internal` | '%s' type '%s' refers to '%s' type '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_coalescing` | type of left operand does not support coalescing operation. coalescing is only valid for 'Option' |
| Basic/DiagnosticsAll.def | ERROR | `sema_div_zero` | division by 0 |
| Basic/DiagnosticsAll.def | ERROR | `sema_mod_zero` | mod by 0 |
| Basic/DiagnosticsAll.def | ERROR | `sema_arithmetical_op_overflow` | overflow in '%s' calculation |
| Basic/DiagnosticsAll.def | ERROR | `sema_shift_count_overflow` | shift count overflow |
| Basic/DiagnosticsAll.def | ERROR | `sema_negative_shift_count` | shift count is negative |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_capture_var_cannot_assign` | %s captured a mutable variable %s, %s cannot be assigned to a variable |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_capture_var_cannot_return` | %s captured a mutable variable %s, %s cannot be used as a return value |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_capture_var_cannot_param` | %s captured a mutable variable %s, %s cannot be used as a param |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_capture_var_cannot_expr` | %s captured a mutable variable %s, %s cannot be used as a expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_unary_expr` | invalid unary operator '%s' on type '%s' |
| Basic/DiagnosticsAll.def | NOTE | `sema_invalid_unary_expr_note` | you may want to implement operator func %s() for type '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_fail_flow_expr_operand_has_named_param` | flow operand cannot contain named parameter |
| Basic/DiagnosticsAll.def | ERROR | `sema_operator_overload_invalid_num_parameter` | invalid number of parameters for operator '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_operator_overload_built_in_unary_operator` | operator func %s() of type %s is a built-in function and cannot be overridden |
| Basic/DiagnosticsAll.def | ERROR | `sema_operator_overload_built_in_binary_operator` | operator func %s(%s) of type %s is a built-in function and cannot be overridden |
| Basic/DiagnosticsAll.def | ERROR | `sema_operator_overload_can_not_has_default_param` | optional parameter can not be used in operator overload function |
| Basic/DiagnosticsAll.def | ERROR | `sema_empty_arrayLit_type_undefined` | array literal type cannot be inferred |
| Basic/DiagnosticsAll.def | ERROR | `sema_inconsistency_elemType` | inconsistent element type for %s literal |
| Basic/DiagnosticsAll.def | ERROR | `sema_tuple_pattern_not_match` | %s isn't a tuple to match tuple pattern |
| Basic/DiagnosticsAll.def | ERROR | `sema_unsupport_operator` | not supported operator: '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_size_type_error` | array size must be of type Int64 |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_expression_param_type_error` | array init expression parameter's type must be type Int64 |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_expression_type_error` | array init expression must has type (Int64)->T |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_single_element_type_error` | array init with single element must be subtype of 'List' or 'Collection' |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_element_type_error` | array initialize element type error |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_too_much_argument` | too much arguments given for array constructor, only accept 0~2 arguments |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_first_arg_cannot_be_named` | array's first argument cannot be named argument |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_second_arg_cannot_be_named` | array's second argument cannot be named argument when type is (Int64)->T |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_second_wrong_named_arg` | array's second argument must have named prefix 'repeat:' when type is T |
| Basic/DiagnosticsAll.def | ERROR | `sema_pointer_too_much_argument` | too much arguments given for CPointer constructor, only accept 0~1 arguments |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_too_many_arguments` | too many arguments given to CFunc constructor, only accept 1 argument |
| Basic/DiagnosticsAll.def | ERROR | `sema_pointer_single_element_type_error` | the single argument of CPointer constructor must be 'CPointer' or 'CFunc' |
| Basic/DiagnosticsAll.def | ERROR | `sema_pointer_unknow_generic_type` | 'CPointer' generic type cannot be inferred |
| Basic/DiagnosticsAll.def | ERROR | `sema_builtin_invalid_index` | %s index must be an integer literal |
| Basic/DiagnosticsAll.def | ERROR | `sema_builtin_index_in_bound` | %s index must be in bounds |
| Basic/DiagnosticsAll.def | ERROR | `sema_tuple_element_cmp_not_bool` | the '%s' operation between type '%s' and type '%s' is not evaluated to a Bool |
| Basic/DiagnosticsAll.def | ERROR | `sema_tuple_cmp_not_supported` | operator '%s' between tuple type '%s' and '%s' is not supported |
| Basic/DiagnosticsAll.def | ERROR | `sema_step_non_zero_range` | step cannot be zero in range expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_inconsistency_range_elemType` | start and stop must be of the same type in range expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_range_step_not_int64` | step must be Int64 in range expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_no_match_function_declaration_for_call` | no matching function declaration for function call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_no_match_function_declaration_for_ref` | no matching function declaration for function reference '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_no_match_constructor` | no matching constructor for call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_ambiguous_arg_type` | ambiguous arguments type in call expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_ambiguous_match` | ambiguous match for function call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_ambiguous_constructor_match` | ambiguous match for constructor call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_ambiguous_func_ref` | ambiguous match for reference '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_mismatched_type_for_pattern_in_vardecl` | the pattern in this variable declaration can not match its type |
| Basic/DiagnosticsAll.def | ERROR | `sema_parameters_and_arguments_mismatch` | parameters and arguments mismatch |
| Basic/DiagnosticsAll.def | ERROR | `sema_cstruct_cannot_autobox` | struct with @C cannot implicitly used as '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_unit_cannot_as_cfunc_arg` | Unit cannot be used as argument type of CFunc |
| Basic/DiagnosticsAll.def | ERROR | `sema_overload_conflicts` | %s '%s' has overload conflicts |
| Basic/DiagnosticsAll.def | ERROR | `sema_no_match_operator_function_call` | no matching function for operator '()' function call |
| Basic/DiagnosticsAll.def | ERROR | `sema_pattern_can_not_be_assigned` | the pattern isn't irrefutable pattern and it can not be initialized |
| Basic/DiagnosticsAll.def | ERROR | `sema_unknown_named_argument` | unknown named argument prefix '%s:' |
| Basic/DiagnosticsAll.def | ERROR | `sema_multiple_named_argument` | named argument prefix '%s:' cannot appeared more than once in call |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_named_arguments` | invalid named arguments prefix '%s:', target is not a named parameter |
| Basic/DiagnosticsAll.def | ERROR | `sema_unsupport_named_argument` | named argument cannot be used in variable function call |
| Basic/DiagnosticsAll.def | ERROR | `sema_pattern_literal_expected` | only const literal is allowed in const pattern |
| Basic/DiagnosticsAll.def | ERROR | `sema_pattern_not_match` | %s pattern is not matched |
| Basic/DiagnosticsAll.def | ERROR | `sema_not_overload_in_match` | no overloaded '==' function in match case pattern |
| Basic/DiagnosticsAll.def | ERROR | `sema_type_incompatible` | type incompatible in this %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_subscript_set_not_supported` | type %s does not have operator func [](index, value) for index type %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_subscript_get_set_not_supported` | type %s does not have both operator func [](index) and operator func [](index, value) for index type %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_tuple_pattern_with_correct_size_expected` | tuple pattern with correct size expected |
| Basic/DiagnosticsAll.def | ERROR | `sema_enum_pattern_param_size_error` | enum pattern's parameters size is wrong |
| Basic/DiagnosticsAll.def | ERROR | `sema_match_case_must_have_default` | at least one default case, such as wildcard pattern, variable pattern or '[...]' for sequence pattern in match  \| case. |
| Basic/DiagnosticsAll.def | ERROR | `sema_match_case_has_no_type` | this match case has no type |
| Basic/DiagnosticsAll.def | ERROR | `sema_package_internal_decl_obtain_illegal` | %s '%s' in package '%s' cannot be obtained |
| Basic/DiagnosticsAll.def | ERROR | `sema_package_name_conflict` | package name '%s' is conflicted with other imported package name, please use 'as' to eliminate conflict. |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_access_control` | can not access field '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_access_function` | can not access function '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_file_hash` | There is invalid file hash in access control check |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_no_override_or_redefine_modifier` | do not need '%s' modifier for %s '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_unexpected_param_for_entry` | 'main' cannot be defined with parameter whose type is not 'Array<String>' |
| Basic/DiagnosticsAll.def | ERROR | `sema_unexpected_return_type_for_entry` | return type of 'main' is not 'Integer' or 'Unit' |
| Basic/DiagnosticsAll.def | ERROR | `sema_redefinition_entry` | multiple 'main's are found in source files |
| Basic/DiagnosticsAll.def | ERROR | `sema_missing_entry` | 'main' is missing |
| Basic/DiagnosticsAll.def | ERROR | `sema_numeric_convert_must_be_numeric` | the expression for numeric type conversion must have a numeric type |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_ctor_must_be_cpointer` | argument type of 'CFunc' constructor must be of type 'CPointer' |
| Basic/DiagnosticsAll.def | ERROR | `sema_ref_not_be_type` | expected member name or constructor call after '%s' type name |
| Basic/DiagnosticsAll.def | ERROR | `sema_expr_in_forin_must_has_iterator` | the type %s of expression in for-in expression does not implement Iterator |
| Basic/DiagnosticsAll.def | ERROR | `sema_forin_pattern_must_be_irrefutable` | the pattern in for-in expression must be irrefutable |
| Basic/DiagnosticsAll.def | ERROR | `sema_wrong_forin_guard` | pattern guard should be Boolean type |
| Basic/DiagnosticsAll.def | ERROR | `sema_generics_type_variable_not_defined` | generics type variable '%s' has not defined |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_type_argument_not_match_constraint` | generics type arguments do not match the constraint of '%s' |
| Basic/DiagnosticsAll.def | NOTE | `sema_which_constraint_not_match` | '%s' is not a subtype of %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_type_without_type_argument` | generic type should be used with type argument |
| Basic/DiagnosticsAll.def | ERROR | `sema_non_generic_function_with_type_argument` | non-generic function should not be used with type argument |
| Basic/DiagnosticsAll.def | ERROR | `sema_throw_expr_with_wrong_type` | the object thrown must derive from `core.Exception` |
| Basic/DiagnosticsAll.def | ERROR | `sema_except_catch_type_error` | the exception catch type must be class and extends from core.Exception or core.Error |
| Basic/DiagnosticsAll.def | ERROR | `sema_no_core_object` | `core` package should be imported |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_intrinsic_decl` | intrinsic function '%s' cannot be declared in '%s' package |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_infinite_instantiation` | generic infinite instantiation |
| Basic/DiagnosticsAll.def | ERROR | `sema_forbid_generic_nonstatic_method` | non-static generic member function '%s' is not supported |
| Basic/DiagnosticsAll.def | ERROR | `sema_forbid_generic_constructor` | generic constructor '%s' is not supported |
| Basic/DiagnosticsAll.def | ERROR | `sema_forbid_generic_finalizer` | generic finalizer '%s' is not supported |
| Basic/DiagnosticsAll.def | ERROR | `sema_import_not_in_current_module` | this package does not belong to the current module, please write its module name explicitly. |
| Basic/DiagnosticsAll.def | ERROR | `sema_flow_expressions_use_this_or_super` | '%s' is not allowed to be used in flow expressions |
| Basic/DiagnosticsAll.def | ERROR | `sema_symbol_not_collected` | reference node named '%s' is not collected in symbol table. |
| Basic/DiagnosticsAll.def | ERROR | `sema_cannot_convert_literal` | cannot convert %s literal to type '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_cannot_have_parameter` | %s cannot have parameter |
| Basic/DiagnosticsAll.def | ERROR | `sema_only_cfunc_can_use_annotation` | only CFunc can use '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_annotation_error_arg_num` | '%s' should have %s arg |
| Basic/DiagnosticsAll.def | ERROR | `sema_annotation_calling_conv_not_support` | '@CallingConv' have not support '%s' yet |
| Basic/DiagnosticsAll.def | ERROR | `sema_annotation_invalid_args_type` | '%s' arg should be right type |
| Basic/DiagnosticsAll.def | ERROR | `sema_unexpected_wrapper` | unexpected %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_native_var_error` | variable can not be modified with 'foreign' and implicit @C. |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_scope_use_of_annotation` | '%s' can only be used in top-level scope |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_use_of_annotation` | %s cannot be modified with '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_cffi_cannot_have_type_param` | %s cannot have type parameters |
| Basic/DiagnosticsAll.def | ERROR | `sema_unsafe_function_invoke_failed` | unsafe function or native function should be invoked in unsafe context. |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_capture_var_not_ctype` | captured variable mustn't be struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_cannot_capture_var` | cannot capture variable %s in CFunc lambda expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_cannot_capture_this` | '%s' is not allowed to be captured in CFunc lambda expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_ctype_generic_argument` | generic argument mustn't be struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_cpointer_generic_type` | generic type of CPointer must be instantiated with CType |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_cfunc_arg_type` | arguments type of CFunc must be instantiated with CType |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_type` | cfunc type must be a function type |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_tuple_field_ctype` | tuple member mustn't be struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_enum_pattern_func_cty_error` | member func '%s' is forbidden in enum '%s' with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_enum_pattern_func_param_cty_error` | member func '%s' of enum '%s' mustn't has struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_ctype_member` | non-static member variable '%s' of %s '%s' cannot be struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_member_of_cstruct` | member variable '%s' of struct '%s' with @C must be instantiated with CType |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_cannot_have_unit_args` | CFunc cannot have arguments of type Unit |
| Basic/DiagnosticsAll.def | ERROR | `sema_cstruct_cannot_have_unit_fields` | member variables cannot be type Unit in struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_cannot_have_named_args` | CFunc cannot have named arguments |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_var_cannot_have_var_param` | CFunc with variable-length parameters cannot be assigned to variables |
| Basic/DiagnosticsAll.def | ERROR | `sema_inheritance_non_ref_type` | inheritance is not a ref type: '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_class_uninitialized_field` | the uninitialized member variable '%s' is not initialized in the constructor of class or struct |
| Basic/DiagnosticsAll.def | ERROR | `sema_this_or_super_not_allowed_to_initialize_non_static_member` | '%s' is not allowed to initialize non-static member |
| Basic/DiagnosticsAll.def | ERROR | `sema_this_or_super_not_allowed_to_initialize_static_member` | '%s' is not allowed to initialize static member |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_usage_of_super_member` | super member '%s' is not allowed to be used before calling super() |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_usage_of_member` | '%s' is not allowed to be accessed before all member variables are initialized |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_assignment_to_this_expr` | cannot assign a value to 'this' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_override_member_in_class` | cannot override non-abstract %s '%s' with abstract %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_override_or_redefine_member_in_interface` | cannot override implemented interface %s '%s' with abstract %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_this_in_interface` | 'this' can not be used in interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_use_super_in_interface` | 'super' cannot be used in interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_super_use_error_inside_non_class` | 'super' can only be used in class |
| Basic/DiagnosticsAll.def | ERROR | `sema_assignment_of_member_variable_cannot_use_this_or_super` | '%s' is not allowed to be used in %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_super_alone` | invalid super expression, it can only be used on the left-hand side of a dot |
| Basic/DiagnosticsAll.def | ERROR | `sema_abstract_class_can_not_be_instantiated` | abstract class '%s' can not be instantiated |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_can_not_be_instantiated` | interface '%s' can not be instantiated |
| Basic/DiagnosticsAll.def | ERROR | `sema_non_inheritable_super_class` | super class '%s' is not inheritable |
| Basic/DiagnosticsAll.def | ERROR | `sema_superclass_must_be_placed_at_first` | super class '%s' must be placed at the beginning of supertype list |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_multi_inheritance` | only one super class may appear in supertype list of class '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_this_call_outside_ctor` | invalid calling '%s' outside the constructor |
| Basic/DiagnosticsAll.def | ERROR | `sema_privated_abstract_func_in_class` | private abstract %s is forbidden in abstract class '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_string_implementation` | the type '%s' should implement interface 'ToString' in the 'core' package |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_tokens_implementation` | the type '%s' should implement interface 'ToTokens' in the 'ast' package |
| Basic/DiagnosticsAll.def | ERROR | `sema_multiple_primary_constructors` | %s '%s' cannot have more than one primary constructor |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_is_not_inheritable` | '%s' interface is not able to be inherited |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_is_not_implementable` | '%s' interface is not able to be implemented explicitly |
| Basic/DiagnosticsAll.def | ERROR | `sema_inherit_duplicate_interface` | %s '%s' inherits or implements duplicate interface '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_return_unit` | return expressions in a constructor must be either 'return' or 'return ()' |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_place_of_calling_this_or_super` | call to '%s' must be first expression in constructor of %s '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_place_of_calling_this_primary_constructor` | invalid calling 'this' in primary constructor |
| Basic/DiagnosticsAll.def | ERROR | `sema_this_super_use_error_outside_class` | '%s' cannot be used outside class or struct or interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_missing_func_body` | %s '%s' can not be abstract |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_member_must_be_implemented` | interface %s '%s' must be implemented in '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_member_must_be_implemented_in_struct` | interface %s '%s' must be implemented in '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_member_variable_can_not_shadow` | the variable '%s' must not shadow a member variable of the supertype |
| Basic/DiagnosticsAll.def | ERROR | `sema_missing_overridden_func` | 'override' %s '%s' does not have an overridden %s in its supertype |
| Basic/DiagnosticsAll.def | ERROR | `sema_missing_redefined_func` | 'redef' %s '%s' does not have a redefined 'static' %s in its supertype |
| Basic/DiagnosticsAll.def | ERROR | `sema_static_and_non_static_member_cannot_have_same_name` | %s member '%s' cannot have the same name with %s member in %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_member_type_argument_different` | type argument of function '%s' is different in parent class or interfaces |
| Basic/DiagnosticsAll.def | ERROR | `sema_c_type_cannot_implement_interface` | c type '%s' cannot implement interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_access_non_static_member` | '%s' is non-static member, cannot access by type name |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_access_interface_field` | field '%s' cannot be accessed without interface name '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_access_inner_classlike` | inner %s '%s' cannot be accessed without name '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_cannot_modify_var` | instance member variable '%s' cannot be modified in immutable function |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_capture_this` | 'this' is not allowed to be captured in constructor of %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_object_cannot_access_static_member` | object cannot access static member '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_abstract_method_cannot_be_accessed_directly` | abstract method '%s' cannot be accessed directly |
| Basic/DiagnosticsAll.def | ERROR | `sema_return_type_invariance` | return type of '%s' can only be class/interface type which implements or inherits the interface type '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_static_members_cannot_call_members` | non-static variable '%s' cannot be referenced from a static context |
| Basic/DiagnosticsAll.def | ERROR | `sema_class_inherit_non_class_nor_interface` | class '%s' can only inherit a class or implement interfaces |
| Basic/DiagnosticsAll.def | ERROR | `sema_type_implement_non_interface` | %s '%s' can only implement interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_inherit_non_interface` | interface '%s' can only inherit interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_in_operator_overload` | generic is not allowed in operator overload function |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_this_outside_struct_constructor` | 'this' is only allowed to be used inside constructor or function for struct |
| Basic/DiagnosticsAll.def | ERROR | `sema_static_function_cannot_access_non_static_member` | '%s' is non-static member, cannot be accessed by static function '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_static_variable_cannot_access_non_static_member` | '%s' is non-static member, cannot be accessed by static variable |
| Basic/DiagnosticsAll.def | ERROR | `sema_static_lambdaExpr_cannot_access_non_static` | invalid use of non-static member '%s' in static lambda expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_redef_modify_static_func` | 'redef' cannot be used to modify an instance '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_member_used_in_open_constructor` | instance member %s '%s' cannot be accessed in the constructor of open class '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_finalizer_forbidden_in_class` | finalizer is forbidden in class '%s' that is %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_no_member_match_in_upper_bounds` | no member match for generic member access when searching in upper bounds |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_ambiguous_method_match_in_upper_bounds` | ambiguous method '%s' match for generic member access function call when searching in upper bounds |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_no_method_match_in_upper_bounds` | no method '%s' match for generic member access function call when searching in upper bounds |
| Basic/DiagnosticsAll.def | ERROR | `sema_cannot_instantiated_by_incomplete_type` | can not instantiate '%s' by %s for it has unimplemented static member |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_field_expose_access` | '%s' is not a static member of %s '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_capture_this_or_instance_field_in_func` | '%s' cannot be captured in the %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_use_this_as_an_expression_in_func` | 'this' cannot be used as an expression in the %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_incompatible_mut_modifier_between_struct_and_interface` | 'mut' modifier of '%s' is incompatible with that in interface '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_immutable_function_cannot_access_mutable_function` | immutable function '%s' cannot access mutable function '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_position_of_this_type` | 'This' type is not allowed here |
| Basic/DiagnosticsAll.def | ERROR | `sema_property_override_implement_type_diff` | The type of the override/implement property must be the same |
| Basic/DiagnosticsAll.def | WARNING | `sema_capture_has_shadow_variable` | the variable '%s' actually captures this decl %p, can not captures the decl %p |
| Basic/DiagnosticsAll.def | WARNING | `sema_useless_exception_type` | useless exception type |
| Basic/DiagnosticsAll.def | WARNING | `sema_ignore_open` | the current member should not have 'open' modifier because it is in a non-inheritable class |
| Basic/DiagnosticsAll.def | NOTE | `sema_found_candidate_decl` | found candidate |
| Basic/DiagnosticsAll.def | NOTE | `sema_found_possible_candidate_decl` | found possible candidate |
| Basic/DiagnosticsAll.def | ERROR | `sema_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_node_after_check` | semantic error |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unable_to_infer_decl` | unable to infer declaration type, please add type annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mismatched_types` | mismatched types \| expected '%s', found '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mismatched_types_multiple_assign` | mismatched types \| the expression has type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mismatched_types_because` | mismatched types \| expected '%s', found '%s' \| expected '%s' because of %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ambiguous_use` | ambiguous use of '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_undeclared_identifier` | undeclared identifier '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_undefined_variable` | variable '%s' is used before being defined |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_redefinition` | redefinition of declaration '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_conflict_with_sub_package` | top-level declaration '%s' is conflicted with possible sub-package '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_core_object_not_found_when_no_prelude` | class 'Object' of package 'std/core' is not found, cannot use '--no-prelude' option |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_accessibility_with_main_hint` | '%s' declaration uses %s types \| %s '%s' contains %s type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_accessibility` | '%s' declaration uses %s types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_param_miss_match` | mismatched number of parameters \| expected '%s', found '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unable_to_infer_return_type` | unable to infer return type, please add type annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unable_to_infer_generic_func` | unable to infer generic argument of this function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_called_object` | called object is not a function or constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_return` | 'return' must be used inside a function body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_return_in_static_init` | 'return' cannot be used inside the static initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_wrong_number_of_arguments` | %s for parameter list '%s' in call \| expected %s, found %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unordered_arguments` | positional argument cannot appear after named argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_param_named_mismatched` | parameter name mismatched |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_need_named_argument` | missing argument prefix %s for named parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_subscript_assign_parameter` | overloaded operator '[]' can only have one named parameter 'value' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_subscript_assign_parameter_num` | overloaded operator '[]' should have at least one positional parameter for index |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_subscript_assign_return` | the return type of subscript assignment must be 'Unit' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_overload_conflicts` | %s '%s' has overload conflicts |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_static_function_overload_conflicts` | overloaded functions '%s' cannot mix static and non-static |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_use_mutable_func_alone` | mutable function '%s' cannot be used alone as reference |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unsafe_func_can_only_be_called` | the unsafe function can only be called rather than as name reference |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ambiguous_match_primitive_extend` | ambiguous match for function call '%s' of these extended type: %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_immutable_access_mutable_func` | cannot use mutable function on immutable value \| is immutable |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_recursive_constructor_call` | recursive constructor calling detected |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_have_default_param` | optional parameter cannot be used in %s function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_trailing_lambda_cannot_used_for_non_function` | trailing lambda cannot be used for %s \| declaration type of parameter: '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unable_to_infer_expr` | unable to infer the type of this expression, please add type annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_exceed_num_value_range` | the number '%s' exceeds the value range of type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_exceed_float_literal_range` | the number '%s' exceeds the value range of floating-point literal |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_float_literal_too_large` | magnitude of floating-point literal too large for type '%s', maximum is %s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_float_literal_too_small` | magnitude of floating-point literal too small for type '%s', minimum is %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_unary_expr` | invalid unary operator '%s' on type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_unary_expr_with_target` | invalid unary operator '%s' on type '%s' with return type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_binary_expr` | invalid binary operator '%s' on type '%s' and '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_subscript_expr` | invalid subscript operator [] on type '%s' with index %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_assign_to_subscript` | cannot assign to this subscript expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_not_member_of` | '%s' is not a member of %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_member_not_imported` | '%s' is not imported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_assign_to_immutable` | cannot assign to immutable value |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unqualified_left_value_assigned` | '%s' can not be assigned |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_not_found_from_generic_upper_bounds` | '%s' is not found for generic type '%s' in its upper bounds |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_different_or_pattern` | patterns connected by '\|' should be of the same kind \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_var_in_or_pattern` | cannot introduce variables in patterns connected by '\|' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_var_in_or_condition` | cannot introduce variables in conditions connected by '\|\|' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_nonexhuastive_patterns` | non-exhaustive patterns \| the selector is of type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_unreachable_pattern` | unreachable pattern |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_lambdaExpr_must_have_type_annotation` | parameters of this lambda expression must have type annotations |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_use_func_capture_var_alone` | %s capturing mutable variables needs to be called directly |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_enum_constructor_with_param_must_have_args` | enum constructor '%s' must be used with arguments |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_optional_chain_non_optional` | cannot use optional chaining \| cannot use optional chaining on non-optional value of type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_capture_before_initialization` | cannot capture variable '%s' before initialization |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_interpolation_in_const_pattern` | cannot use string interpolation in constant pattern |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_ref_to_pkg_name` | package name cannot be referred independently |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_use_expr_without_import` | import '%s' to use the '%s' expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_func_without_type_arg` | type arguments needed for the generic function%s \| cannot infer type arguments for the generic function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_type_inconsistent` | generic types substitutions are inconsistent for '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_argument_no_match` | type argument's number does not match type parameter's number |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_constraint_not_looser` | the constraint of type parameter is not looser than parent's constraint |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_instantiation_causes_ambiguous_functions` | generic instantiation '%s' causes ambiguous function '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_multiple_class_upperbounds` | generic parameter '%s' cannot have two or more class upper bounds '%s' without subtype relation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_param_exist_in_class_irrelevant_upperbound_recursively` | generic parameter '%s' cannot be used in class irrelevant upper bounds '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_param_directly_recursive` | generic parameter '%s' is bounded directly recursively with '%s' which is forbidden |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_upper_bound_must_be_class_or_interface` | the upper bound '%s' of generic parameter '%s' must be class or interface |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_member_kind_inconsistent` | %s member '%s' cannot have the same name with %s member in %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_super_member_kind_inconsistent` | inherited members '%s' have inconsistent decl types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_member_type_inconsistent` | %s of the inherited %s members '%s' are not identical and not in subtype relation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_abstract_class_static_unimplement_func` | abstract class '%s' cannot contain unimplemented static %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_override` | cannot override %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_member_visibility_in_class` | the visibility of an '%s' %s must be 'public' or 'protected' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_weak_visibility` | a deriving member must be at least as visible as its base member \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_inherit_sealed` | cannot %s %s 'sealed' %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_thread_context_invalid` | user defined decl '%s' not support to inherit, implement or extend 'ThreadContext' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_thread_context_not_open` | '%s' cannot be modified with 'open' when inherit, implement or extend 'ThreadContext' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_not_return_this` | an open function that returns 'This' must keep the return type 'This' when overridden |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_return_type_incompatible` | return type of '%s' is not identical or not a subtype of the overridden/redefined/implement function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_spawn_arg_invalid` | invalid argument of spawn expr, user-defined `ThreadContext` types are prohibited now |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_spawn_arg_no_effect` | argument of spawn expr does not take effect at current backend |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_interface_call_with_unimplemented_call` | static invocation contains unimplemented static %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_type_uninitialized_static_field` | the static member variable '%s' is not initialized |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_instance_func_cannot_be_used_in_finalizer` | instance %s cannot be used in the finalizer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_no_non_param_constructor_in_super_class` | there is no non-parameter constructor in super class, please invoke super call explicitly |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_non_abstract_class_cannot_be_sealed` | non-abstract class cannot be modified by 'sealed' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_static_variable_use_generic_parameter` | static member cannot depend on generic parameter '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cstruct_cannot_impl_interfaces` | struct with @C cannot implement interfaces |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_class_need_abstract_modifier_or_func_need_impl` | class '%s' missing abstract modifier, otherwise abstract function or property should be implemented |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_need_member_implementation` | implementation of function or property is needed in '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_export_same_private_decl` | currently, it is not possible to export two private declarations with the same name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_function_cannot_overridden` | cannot override %s '%s' in extend of supertype |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_member_cannot_shadow` | extend member '%s' is not allowed to shadow members of '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_illegal_extended_type` | extending type '%s' is not allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_generic_must_be_used` | type parameter%s must be used in extended type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_duplicate_interface` | interface '%s' has been implemented by '%s', please remove it |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_not_interface` | expected an interface, found non-interface type \| expected an interface here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_illegal_member` | illegal extend member, only functions, props, associated types are allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_use_super` | 'super' is not allowed inside an extend declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_type_cannot_extend_imported_interface` | %s type '%s' cannot extend imported interface |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_c_type_cannot_extend_interface` | c type '%s' cannot support interface extend |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_immutable_type_extend_assignment_index_operator` | it's illegal to extend index assignment operator '[](index, value)' for immutable type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_immutable_type_illegal_property` | there cannot have mutable property in immutable type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_interface_is_not_extendable` | interface '%s' is not able to be extended |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_mut_modifier_extend_of_struct` | 'mut' modifier is illegal in extend body of '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_check_sequence_cannot_decide` | unable to decide which extension happens first |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_export_extend_depend_non_export_extend` | exported extension cannot indirectly export the functions '%s' of the non-exported extension |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_property_must_have_accessors` | property must have accessors |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_immutable_property_with_setter` | immutable property cannot have setter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_property_have_same_declaration_in_inherit_mut` | property '%s' should have 'mut' modifier |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_property_have_same_declaration_in_inherit_immut` | property '%s' should be immutable |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_property_must_implement_both` | property must implement both getter/setter of interface property '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_expect_const` | expected 'const' %s \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_define_var_in_const_funciton` | cannot define 'var' variable in 'const' function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_no_const_init` | cannot define 'const' member function without 'const' constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_class_const_init_with_var` | cannot define 'const' constructor with 'var' members in class |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_no_const_init` | class with '@Annotation' should have 'const' constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_arg_target` | '@Annotation' can only have one named argument 'target' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_arg_target_array_lit` | the argument of '@Annotation' should be array literal |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_annotation_non_public` | '@Annotation' modifying non-'public' class is invisible at runtime |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_custom_place` | cannot use custom annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_modify_cstring_or_zerosized` | the expression qualified by 'inout' cannot be of %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_modify_non_ctype` | the type of experssion qualified by 'inout' must meet 'CType' constraint |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_must_be_var_variable` | 'inout' can only qualify variable defined with 'var' \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_modify_heap_variable` | the variable qualified by 'inout' cannot be directly or indirectly derived from an instance of a 'class' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_can_only_used_in_cfunc_calling` | 'inout' can only be used in a 'CFunc' calling |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_mismatch` | mismatch 'inout' of function argument with type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_error_arg_num` | '%s' should have %s arg |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_error_arg_range` | '%s' only supports %s as arg |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_error_object` | '%s' can only modify %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_incorrect_use_between_types` | type annotated with '@Java["ext"]' can only be used within the declaration which has '@Java["ext"]'  \| annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_non_jtype` | %s type in %s '%s' with '@Java' must meet JType constraint |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_invalid_unit` | %s type in %s '%s' with '@Java' can not be 'Unit' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_app_inherit_ext` | only types annotated with '@Java["ext"] can %s from a type annotated with '@Java["ext"]' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_unsupported_decl` | %s is not supported in %s '%s' annotated with '@Java' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_missing_java_interop_annotation` | %s '%s' should have '@Java' annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_static_access` | cannot access static member with generic parameter in '@Java' types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_primitive_type_as_generics_arg` | only reference types are available for '@Java' generics |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_meet_constraint_indirectly` | types that meet constraints by 'extend' cannot be used in '@Java' generics |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_static_member_in_interface_must_has_body` | static functions in '@Java'-annotated interfaces must have a body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_a_java_type` | types annotated with '@Java' cannot be extended |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_upper_bounds_must_be_java_in_java` | generic type's upper bound in types annotated with '@Java' should be annotated with '@Java' too |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_define_java_annotation` | types annotated with '@Java' cannot be annotated with '@Annotation' together |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_use_of_java_annotation` | imported Java annotations can only be used with types annotated with '@Java' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_use_of_annotation_jffi` | only imported Java annotations can be used with types annotated with '@Java' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_not_applicable_jffi` | '@%s' not applicable to %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_use_annotation_jffi` | cannot use annotation here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_shadow_cannot_in_type_args` | '%s' is not allowd to be used here as type argument, because it shadows field '%s' with its super type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unsupported_type_argument_in_java_interop` | type argument in java interoperation should meet 'JType' constraint |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmapping_struct_generic_not_supported` | cangjie mirror struct type generic %s is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmapping_struct_inheritance_interface_not_supported` | cangjie mirror struct type inheritance interface is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmapping_decl_not_supported` | cangjie mirror decl type is not supported for %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmapping_method_arg_not_supported` | argument type of cangjie mirror decl type member function is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmapping_method_ret_unsupported` | return type '%s' of function inside %s type is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cj_mapping_generic_method_not_get_instance_config` | Instance configuration '%s' has incorrect format. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_varray_size_match` | mismatch 'VArray' type's size \| expected size is %s, found %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_varray_args_number_mismatch` | 'VArray' constructor accepts only one argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_varray_subscript_num` | 'VArray' accepts exactly one subscript index with type of 'Int64' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_varray_in_cfunc` | return type of CFunc cannot be 'VArray' type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_varray_arg_type_with_reftype` | '%s' directly or indirectly contains an unsupported type \| contain unsupported instance member variable with type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_cfunc_return_type` | return type of CFunc must be instantiated with CType |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_disabled` | mocking features are disabled, you can enable them by passing %s compilation option explicitly, or using default  \| mode |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_not_in_test_mode` | mocking features can be used only in the test mode, please pass %s compilation option to compile the package in  \| the test mode |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_unsupported_type` | only mocking of classes or interfaces is supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_wrong_static_decl` | static/top-level declaration to mock shouldn't be private, local, constant or constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_doesnt_support_mocking` | '%s' doesn't support mocking, please be sure that its package '%s' is mock-compatible (was compiled with %s  \| compilation option) |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_frozen_unsupported` | mocking of frozen declarations (marked with @Frozen annotation) are not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_frozen_required` | generic wrapper function '%s' for createMock/createSpy calls should be marked with @Frozen annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_command_handle_type_error` | the command handle type must implement 'effect.Command<T>' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_resumption_handle_type_error` | the type of the resumption must extend 'effect.Resumption' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_resumption_incorrect_return_type` | the return type of the resumption ('%s') does not match the type of the try block ('%s') |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_command_resumption_mismatch` | the parameter type of the resumption ('%s') does not match the result type of the command ('%s') |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_implicit_resume_outside_handler` | 'resume' outside of an immediate handler must have a resumption argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_resume_no_with` | a resumption of non-Unit type '%s' must have a 'with' or 'throwing' clause |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_resume_wrong_resumption_type` | resumptions must be of type 'core.Resumption<T>', but actual type is '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mismatching_handle_block` | The type of this handle block is '%s', which mismatches the smallest common supertype '%s' of previous branches. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_return_in_try_handle_block` | Return statements are not allowed within try/handle blocks |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_command_incompatible_type` | type '%s' does not implement compatible instantiations of 'Command<T>' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_resume_throwing_mismatch_type` | the type of the `resume throwing` must be a subtype of core.Exception or core.Error |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_useless_command_type` | useless command type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_deprecated_error` | %s '%s' is deprecated%s%s \| deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_deprecated_warning` | %s '%s' is deprecated%s%s \| deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_deprecation_weakening` | strictness of @Deprecated can not be weaken on inheritors |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_deprecation_override_error` | overridden %s '%s' should be marked with @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_deprecation_override_warning` | overridden %s '%s' should be marked with @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_deprecation_redef_error` | redefined %s '%s' should be marked with @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_deprecation_redef_warning` | redefined %s '%s' should be marked with @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_open_class_no_init` | please implement the constructor explicitly for common open class '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_multiple_common_implementations` | 'common' %s has several specific implementations |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_direct_extension_has_duplicate_private_members` | declaration 'common' extend '%s' has a conflicting private %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_direct_extension_has_common_private_members` | 'common' and 'private' modifier conflict on %s '%s' declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_not_matched` | '%s' %s can not find '%s' match |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_var_not_match_let` | 'specific' '%s' can not match 'common' '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_init_common_primary_constructor` | 'specific' init can not be used to implement primary 'common' constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_primary_unmatched_var_decl` | parameter in 'specific' primary constructor must also be a member variable declaration  \| if it's a member variable declaration in 'common' primary constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_different_type` | 'specific' %s type is not equal to 'common' type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_member_must_have_implementation` | the member %s must have body in 'specific' %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_different_modifier` | 'specific' %s modifier is not match 'common' modifier |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_different_annotation` | 'specific' %s annotation is not match 'common' annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_deprecated_annotation` | '%s' annotation is not allowed on specific %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmp_parameter_default_value_both_sides` | parameter default value should be on either 'common' or 'specific' side, not both |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_different_parameter` | 'specific' function parameter is not match 'common' parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_different_super_type` | 'specific' %s super types is not match 'common' super types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_duplicate_extensions` | declaration 'specific' extend '%s' has a conflicting extension |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_package_has_main` | main function cannot be used in common package part |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_static_let_cant_be_initialized_in_static_init` | 'common' static let '%s' can not be initialized in static init |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_assign_to_common_immutable_in_ctor` | cannot assign to immutable variable '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmp_abstract_class_member_has_no_explicit_modifier` | '%s' abstract class %s must have explicit '%s' or 'abstract' modifier |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_explicitly_abstract_can_not_have_body` | abstract %s can not have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_explicitly_abstract_only_for_cjmp_abstract_class` | only common/specific class can have explicitly abstract %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_open_abstract_specific_can_not_replace_open_common` | open common %s can not be overridden with abstract specific %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmp_non_specific_abstract_member_in_specific_class` | specific abstract class '%s' cannot have non-specific abstract %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_generic_frozen_not_supported` | common/specific declaration %s with generics cannot be @Frozen |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_generic_rename_not_supported` | common/specific generic rename is not supported yet |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_specific_annotation_not_allowed` | annotation %s is not allowed on a common/specific declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_ctor_arg_must_be_java_mirror` | argument type of java-mirrored constructor must be of @JavaMirror type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_method_arg_must_be_java_mirror` | argument type of java-mirrored function must be of @JavaMirror type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_method_ret_unsupported` | return type '%s' of function inside %s class is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_prop_must_be_java_mirror` | property of java-mirrored declaration must be of @JavaMirror type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_subtype_must_be_annotated` | super declaration '%s' is inheritable only for declaration annotated with @JavaMirror or @JavaImpl |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_cannot_inherit_pure_cangjie_type` | @JavaMirror-annotated declaration cannot inherit pure cangjie type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_impl_cannot_inherit_pure_cangjie_type` | @JavaImpl-annotated declaration cannot inherit pure cangjie type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_subtype_anno_must_inherit_mirror` | @JavaImpl-annotated declaration must inherit @JavaMirror-annotated declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_cannot_be_extended_with_interface` | @JavaMirror class cannot be extended with interface |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_impl_cannot_be_extended_with_interface` | @JavaImpl class cannot be extended with interface |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_impl_redefinition` | redefinition of java declaration '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_interoplib_must_be_imported` | interoplib.interop must be imported to use java interoperability |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_interop_not_supported` | Java interoperability feature '%s' is not yet supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_ref_target_cannot_be_java_impl` | extend declaration ref target cannot be java impl |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_variable_of_java_type` | %s can not store objects of java interoperability type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_parameter_of_java_type` | Can not instantiate generic '%s' with java interoperability type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_java_interoplib_version_too_old` | java interoplib.interop library's version is too old. Compiler was built expecting versoin '%s'.  \| Compatibility problems could happen. Use it at your own risk |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_java_interoplib_version_mismatch` | java interoplib.interop library's version is '%s', but compiler was built expecting version '%s'.  \| Compatibility problems could happen. Use it at your own risk |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_has_default_annotation_args` | '@JavaHasDefault' can't have arguments |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_has_default_annotation_is_in_wrong_place` | '@JavaHasDefault' can be used only on @JavaMirror interface methods. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_has_default_conflict_with_static` | Illegal combination of '@JavaHasDefault' annotation and 'static' modifier. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_ctor_param_must_be_objc_compatible` | param type of %s constructor must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_method_param_must_be_objc_compatible` | param type of %s method must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_method_ret_must_be_objc_compatible` | return type of %s method must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_prop_must_be_objc_compatible` | %s property type must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_field_must_be_objc_compatible` | %s field type must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_decl_cannot_inherit` | Objective-C mirror cannot inherit other supertypes |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_subtype_cannot_multiple_inherit` | Objective-C mirror subtype cannot inherit multiple types (only 1 interface or 1 class is allowed) |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_subtype_must_be_annotated` | Objective-C mirror subtype must be annotated with @ObjCMirror or @ObjCImpl |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_subtype_must_inherit_mirror` | @ObjCImpl declaration must inherit @ObjCMirror |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_must_inherit_mirror` | @ObjCMirror declaration cannot inherit not @ObjCMirror declarations |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_interoplib_must_be_imported` | interoplib.objc must be imported to use Objective-C interoperability |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_not_supported` | Objective-C interoperability feature '%s' is not yet supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_pointer_argument_must_be_objc_compatible` | ObjCPointer can only be used with Objective-C compatible types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_toplevel_param_must_be_objc_compatible` | param type of Objective-C mirror top-level function '%s' must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_toplevel_ret_must_be_objc_compatible` | return type of Objective-C mirror top-level function '%s' must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_method_must_have_foreign_name` | %s declaration method '%s' with more than one parameter must have @ForeignName annotation \| %s declaration method with more than one parameter must have must have @ForeignName annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_ctor_must_have_foreign_name` | %s declaration constructor with more than one parameter must have @ForeignName annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_func_argument_must_be_objc_compatible` | %s can only be used with function type over Objective-C compatible types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_func_call_property_can_only_be_called` | %s property 'call' can only be called directly, no other operations are permitted |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_impl_must_have_objc_mirror_super_class` | @ObjCImpl class must have @ObjCMirror super class |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_setter_name_on_immutable_prop` | @ForeignSetterName cannot be specified on immutable property |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_cjmapping_inheritance_interface_not_supported` | cangjie mirror decl type inheritance interface is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_cjmapping_generic_not_supported` | cangjie mirror decl type generic %s is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_foreign_name_appeared_in_child` | @%s could not appear on overridden declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_foreign_name_conflicting_annotation` | Declaration '%s' has a conflicting @%s annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_foreign_name_conflicting_derived_annotation` | Declaration '%s' has a conflicting derived @%s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ifavailable_arg_no_name` | the first argument of @IfAvailable expression must have a name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ifavailable_arg_not_literal` | the first argument of @IfAvailable expression must be a literal expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ifavailable_unknow_arg_name` | unknown parameter name '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_apilevel_multi_anno` | annotate more than one '@!APILevel' |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_apilevel_missing_arg` | annotation missing named argument '%s' or unable to read as numerical value |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_only_literal_support` | only %s literal values are supported for now |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_apilevel_ref_higher` | cannot reference '%s'(level: %s) which higher than level of the current scope(level: %s) |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_apilevel_syscap_warning` | inappropriate syscap '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_apilevel_syscap_error` | inappropriate syscap '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_apilevel_multi_diff_syscap` | declaration mark with different syscap |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ifavailable_level_limit` | `@IfAvaliable` feature is not avaliable in device where the APILevel is less than 19 due to missing capatability  \| in ROM |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_hide_multi_annotation` | cannot be annotated with '@!Hide' more than once |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_hide_at_func_param` | function parameter cannot be annotated with '@!Hide' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_hide_missing_hide` | should be marked with '@!Hide' to be hidden |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_hide_compile_time_invisible` | 'Hide' annotation must be visible at compile time |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_hide_diff_param` | the parameter 'isChecked' of '@!Hide' is %s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_hide_must_at_end` | annotation '%s' must be placed below all macros and annotations |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_unused_import` | unused import '%s' \| unused import |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_diag_end` |  |

### 4.2 `Parser`

- Count: 248

| Set | Severity | ID | Message |
|---|---|---|---|
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_name` | expected %s %s, found %s \| expected %s here \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_newline_between_at_and_mc` | unexpected '<NL>' between '@' and the macro invocation '%s' \| unexpected '<NL>' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expect_escape_dollar_token` | expected identifier or '(' after '$' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_varray_type_parameter` | expected type parameters after 'VArray' keyword |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_varray_type_args_mismatch` | expected %s between '<' and '>' of 'VArray' type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expect_integer_literal_varray` | expected an integer literal than or equal to 0 after '$' to specificate the size of 'VArray' type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_varray_with_paren` | expected '(' or '{' after 'VArray' for 'VArray' constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_import` | expected 'import' after module name, found %s \| expected 'import' here \| after module name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_module_name` | expected module name after keyword 'from', found %s \| expected module name here \| after keyword 'from' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_right_delimiter` | unclosed delimiter: '%s' \| expected '%s' here \| to match this opening '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_not_allowed_raw_identifier` | escaped identifier with backticks '%s' is not allowed \| don't use backticks here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_return_type` | there should be no return type in a %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unmatched_right_delimiter` | unmatched delimiter: '%s' \| unmatched delimiter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_literal` | expected literal after '-', found %s \| expected literal here \| after this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_pattern` | expected pattern, found %s \| expected pattern here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_backarrow_in_let_cond` | expected '<-' in %s expression, found %s \| expected '<-' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_left_paren_after` | expected '(' after '%s', found %s \| expected '(' here \| after this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_left_angle_after` | expected '<' after '%s', found %s \| expected '<' here \| after this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_expr_or_decl_in` | expected expression or declaration, found %s \| expected expression or declaration here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_catch_or_finally_in_try` | expected 'catch' or 'finally' after try block, found %s \| expected 'catch' or 'finally' here \| after try block |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_catch_or_handle_or_finally_in_try` | expected 'catch', 'handle' or 'finally' after try block, found %s \| expected 'catch', 'handle' or 'finally' here \| after try block |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_colon_in_catch_pattern` | expected ':' in exception type pattern, found %s \| expected ':' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_colon_in_effect_pattern` | expected ':' in effect type pattern, found %s \| expected ':' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_wildcard_or_exception_pattern` | expected wildcard or exception type pattern, found %s \| expected wildcard or exception type pattern here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_wildcard_or_effect_pattern` | expected wildcard or effect type pattern, found %s \| expected wildcard or effect type pattern here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_double_arrow_in_case` | expected '=>' in case, found %s \| expected '=>' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_selector_or_match_expression_body` | expected '(' or '{' after 'match', found %s \| expected '(' or '{' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_left_brace` | expected '{', found %s \| expected '{' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_left_paren` | expected '(', found %s \| expected '(' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_case` | expected 'case' in match, found %s \| expected 'case' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_line_break` | expected '(' after 'quote', found line break \| unexpected line break here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_paren_or_brace_after_try` | expected '(' or '{' after 'try', found %s \| expected '(' or '{' here \| after this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_assignment` | expected '=', found %s \| expected '=' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_in_forin_expression` | expected 'in' in for-in expression, found %s \| expected 'in' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_while_in_do_while` | expected 'while' in do-while expression, found %s \| expected 'while' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_double_arrow_in_lambda` | expected '=>' in lambda expression, found %s \| expected '=>' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_ccd_in_lambda` | expected one of ',', ':' or '=>', found %s \| expected one of ',', ':' or '=>' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_character` | expected %s, found %s \| expected %s here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_character_after` | expected %s after '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_importing_by_package_name_is_not_supported` | expected '.' \| expected '.' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_decl` | expected declaration, found %s \| expected declaration here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_one_of_identifier_or_pattern` | expected identifier or pattern after '%s', found %s \| expected identifier or pattern here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_get_or_set_in_prop` | expected 'get' or 'set' in prop body, found %s \| expected 'get' or 'set' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_where_brace` | expected '{' or 'where', found %s \| expected '{' or 'where' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_lt_brace` | expected '{' or '<', found %s \| expected '{' or '<' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_lt_paren` | expected '(' or '<', found %s \| expected '(' or '<' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_identifier_lp` | expected ')' or identifier, found %s \| expected ')' or identifier here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_dot_lparen` | expected ',' or ')', found %s \| expected ',' or ')' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_arrow_in_func_type` | expected '->' in function type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_colon_in_range` | unexpected ':' in index access \| unexpected ':' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_lsquare_after` | expected '[' after '%s', found %s \| expected '[' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_type_argument` | expected type argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_parameter_rp` | expected one parameter name or ')', found %s \| expected one parameter name or ')' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_no_newline_after` | expected no new-line character after %s \| expected no new-line character here \| after %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_if_let_andand` | expected '&&', '\|\|', or ')', got 'where' \| did you mean to write '&&' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicated_item` | duplicated %s '%s'%s \| duplicated %s \| previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_nl_warning` | possibly confusing line terminator \|  \| possibly confusing line terminator between '%s' and '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_function_name` | 'main' declaration doesn't need 'func' keyword \|  \| help: try to remove 'func' keyword |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_macro_decl_define_in_macro_package` | macro declaration must be defined in macro package \| expected to be defined in macro package |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_public_before_macro_decl` | macro declaration must be modified with 'public' \| expected 'public' before here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_unexpected_empty_parameter` | unexpected empty parameters in macro declaration \| expected paratmeters here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_expected_right_parameter_nums` | too many parameters in macro declaration \| too many parameters here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_illegal_param_type` | macro declaration's parameter type must be 'Tokens' \| expected 'Tokens' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_illegal_ret_type` | macro declaration's return type must be 'Tokens' \| expected 'Tokens' here, got '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_illegal_named_param` | cannot use named parameter in macro declaration \| unexpected '!' here \| expected '%s : Tokens' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_define_conflicted_with_builtin` | macro declaration name '%s' is conflicted with builtin %s identifier \| unexpected macro identifier here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_call_illegal_with_builtin` | unexpected '[' for builtin macro '%s' \| expected '(' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_declaration_in_scope` | unexpected %s in %s \| unexpected %s \| in %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_const_expected_initializer` | const variable declaration must be initialized \| expected a initializer here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_const_modifier_on_variable` | unexpected modifier 'const' on var or let variable \| unexpected modifier |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_var_must_be_initialized` | variable in top-level scope must be initialized  \| expected '=' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_one_of_type_or_initializer` | variable declaration '%s' needs either type or initializer \| expected ':' or '=' after variable name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_type_or_init_in_pattern` | variable declaration in pattern needs either type or initializer \| expected ':' or '=' after pattern |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_named_parameter_after_unnamed` | unnamed parameters must come before named parameters \| unexpected unnamed parameter here \| because it must come before this named parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_member_parameter_after_regular` | regular parameters must come before member variable parameters \| unexpected parameter here \| because it must come before this member variable parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_decl_cannot_inherit_their_self` | declaration '%s' cannot inherit itself \| illegal super declaration here \| because '%s' cannot inherit itself |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_intrinsic_function_must_be_toplevel` | intrinsic function must be toplevel scope \| intrinsic function must be toplevel scope |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_intrinsic_function_cannot_have_body` | intrinsic function cannot have body \| intrinsic function cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_abstract_func_must_have_return_type` | abstract function must have return type \| abstract function must have return type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicated_get_or_set` | duplicated '%s' in prop \| duplicated '%s' \| previous one |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unknown_enum_constructor` | unknown enum constructor \| unknown enum constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_getter_setter_cannot_be_generic` | '%s' cannot be generic \| unexpected generic here \| in '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_where` | unexpected 'where' in non-generic declaration \| unexpected 'where' here \| because this declaration is non-generic |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_setter_must_contain_one_parameter` | setter must contain 1 parameter \| expected 1 parameter inside |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_setter_can_only_accept_one_parameter` | setter can only accept 1 parameter \| can only accept 1 parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicated_intrinsic_function` | duplicated intrinsic function '%s' \| duplicated intrinsic function \| the previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_missing_body` | body of %s is missing \| missing %s body here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_super_declaration` | cannot inherit from type: '%s' \| this type cannot be inherited |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_static_init_can_not_accept_any_parameter` | static initializer cannot have any parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_finalizer_can_not_accept_any_parameter` | finalizer cannot have any parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_quote_dollar_expr` | invalid expression after the operator '$' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_lambda_expr_in_toplevel` | unexpected lambda expression in top-level scope \| unexpected lambda expression in top-level scope |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_trailing_closure_only_follow_name` | trailing closure can only be used on function calls with function or variable names |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_left_hand_expr` | invalid left-hand expression of assignment '%s' \|  \| cannot assign to this expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_chained_none_associative` | %s operators cannot be chained \| |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicated_step_op` | duplicated step operator ':' on range expression \| redundant operator \| previous one \| on range expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_step_op` | invalid step operator ':' on %s expression \| invalid operator \| on %s expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_expression` | expected expression after %s, found %s \| expected expression here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_incre_expr` | cannot %s a un-assignable expression \|  \| cannot assign to this expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unrecognized_token_after_macro_node` | unrecognized operator %s after declaration \| unrecognized operator \| after declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_operator_or_end` | expected operator or end of expression, found %s \| expected operator or end of expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cannot_have_assi_in_init` | cannot have assignment expression in initializer \| cannot have assignment expression here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_case_body_cannot_be_empty` | match case cannot be empty \| match case cannot be empty |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_redefined_resource_name` | resource name '%s' is already defined \| redefinition of resource name \| previous one |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_newline_not_allowed_between_spawn_and_argument` | unexpected newlines between 'spawn' and the argument followed it |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_no_arguments_in_spawn` | expected no %s in lambda expression of spawn \| cannot contain %s \| in spawn |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_overloaded_operator` | cannot overload operator %s  \| cannot overload this operator |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_empty_string_interpolation` | string interpolation cannot be empty \| empty string interpolation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_unicode_scalar` | code point '%s' is too large \| unrecognized code point |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_wildcard_can_not_be_used_as_member_name` | wildcard cannot be used as member name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_expected_found` | unexpected %s \| expected %s, found %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cannot_operator_a_tuple` | cannot '%s' a tuple |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_parentheses` | type before arrow of function type should be surrounded by parentheses |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_this_type_not_allow` | 'This' type is not allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_tuple_decl_type` | Legacy tuple type syntax no longer allowed after version 0.28.4 \| use ',' instead |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_type` | expected type name after %s, found %s \| expected type name here \| after %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_newline_not_allowed_between_quest_and_type` | unexpected newlines between '?' and the type followed it |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_redundant_arrow_after_func_type` | redundant '->' after function type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_all_parameters_must_be_named` | in a parameter type list, either all parameters must be named, or none of them; mixed is not allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_only_tuple_and_func_type_allow_type_parameter_name` | unexpected %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_declaration_pattern` | %s patterns cannot be used in class or struct body \|  \| in %s body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_or_pattern` | '\|' is not allowed here \| expected ',' or ')', found '\|' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_tuple_pattern_expected_more_field` | 1-element tuple pattern is not allowed \| 1-element tuple pattern is not allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_type_pattern_in_let_cond` | type pattern is not allowed in %s expression \| type pattern is not allowed here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_macro_decl_in_macro_package` | cannot use 'public' on %s declarations in a macro package \|  \| macro package defined here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_package_as_all` | The alias name should contain '.*' suffix after import-all \| expected '%s*' \| after import-all |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_package_name_length_overflow` | length of package name '%s' overflow \| length overflow |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_package_name_has_backtick` | cannot using '`' in package name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_macro_expand_input_args` | unexpected '[' after '\' for macro argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_macro_expand_attr_args` | unexpected '(' after '\' for macro attribute argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_macro_expand_input_args_without_paren` | unexpected parameters for macro invocation here \| expected declaration like: function, enum, class, interface, variable, property, extend ... |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_macro_expand_input_without_paren_in_paramlist` | unexpected parameters for macro invocation \| expected '(' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_ifavailable_arg_no_name` | @IfAvailable expect an argument name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_ifavailable_not_lambda` | @IfAvailable expect a literal lambda here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_anno_on` | unexpected annotation '%s' on %s \| unexpected annotation here \| on %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_overflow_annotation` | unexpected overflow annotation before '%s' \| unexpected overflow annotation \| before this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unrecognized_expression_in_when` | unrecognized expression '%s' in annotation '@When' \| unrecognized expression here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unrecognized_attr_in_anno` | unrecognized attribute '%s' in annotation '@%s' \| unexpected attribute here |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_empty_attribute` | empty attribute of annotation '@%s' \| empty attribute here |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_duplicated_attr_value` | duplicated attribute value: '%s' \| duplicated value here \| the previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_unsafe_will_be_ignored` | 'unsafe' modifier will be ignored in backend '%s' \| will be ignored in backend '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicated_annotation` | duplicated annotation: '%s' \| duplicated annotation here \| the previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_conflict_annotation` | '%s' and '%s' annotations conflict on %s \| unexpected annotation \| because it is conflicted with this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_fail_expected_annotation` | expected annotation '%s' \| declare annotation before this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_common_and_specific_in_the_same_file` | 'common' and 'specific' declarations can not be in the same file |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_common_function_must_have_return_type` | 'common' function return type must be specified |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_specific_function_must_have_return_type` | 'specific' function return type must be specified |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_specific_function_parameter_cannot_have_default_value` | 'specific' %s parameter can not have default value |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_specific_member_must_have_implementation` | the member %s must have body in 'specific' %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_type_with_cjmp_var` | '%s' %s type must be specified |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cjmp_outdecl_miss_match` | %s is %s, but %s is not %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cjmp_static_init` | static init can not be '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_common_in_non_common_file` | common declaration must be defined in common package part |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_specific_in_non_specific_file` | specific declaration must be defined in specific package part |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cjmp_generic_decl` | generic declaration can not be '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cjmp_pattern_decl` | %s pattern can not be '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cjmp_in_common_ctor_required` | at least one constructor is required in common %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_explicitly_abstract_only_for_cjmp_abstract_class` | only common/specific or Native FFI mirror abstract classes can have explicitly abstract %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_modifier_in_scope` | unexpected modifier '%s' on %s%s \| unexpected modifier \| on %s \| in %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_conflict_modifier` | '%s' and '%s' modifiers conflict on %s \| unexpected modifier \| because it is conflicted with this \| on %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_no_modifier` | expected no modifier before %s, found '%s' \| expected no modifier here \| before %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicate_modifier` | duplicated modifier: '%s' \| duplicated modifier \| previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicate_type_parameter_name` | duplicated type parameter name: '%s' \| duplicated type parameter name \| previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_type_in` | unexpected type in '%s' \| unexpected type here \| in %s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_redundant_modifier` | redundant modifier: '%s' \| %s implies '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_variable_length_parameter_can_not_be_first` | variable length parameter can not be the first parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_variable_length_parameter_must_in_the_end` | variable length parameter must in the end of the parameter list |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_variable_length_parameter_only_in_the_foreign_function` | variable length parameter can only show in the foreign function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_foreign_func_should_not_be_generic` | foreign function should not be generic function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_foreign_func_must_declare_return_type` | foreign function must declare its return type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_foreign_function_with_body` | foreign function can not have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_static_for_const_member_var` | expected static before const member variable \| const member variable must be modified by static |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_wrong_argument` | argument '%s' of @Deprecated should be %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_argument_duplication` | argument '%s' of @Deprecated can not be duplicated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_arguments_must_be_lit_const_expr` | argument of @Deprecated is not string literal or boolean value. Variables not allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_empty_string_argument` | argument '%s' of @Deprecated must not be empty string |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_unknown_argument` | unknown argument '%s' in @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_invalid_target` | %s can not be target of @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_annotation_max_one_argument` | %s requires zero or one%s argument \| expected %s here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_annotation_one_argument` | %s requires exactly one%s argument \| expected %s here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_annotation_no_arguments` | %s accepts no arguments \| unexpected argument(s) |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_foreign_name_on_ffi_decl_member` | @ForeignName could only be used on FFI declaration member |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_function_cannot_have_body` | java-mirrored function '%s' cannot have body \| java-mirrored function cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_function_must_have_return_type` | java-mirrored function must have return type \| java-mirrored function must have return type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_prop_cannot_have_setter` | java-mirrored property cannot have setter \| java-mirrored property cannot have setter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_prop_cannot_have_getter` | java-mirrored property cannot have getter \| java-mirrored property cannot have getter |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_java_mirror_prop_is_deprecated` | java-mirrored property is deprecated, use field instead |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_decl_cannot_have_primary_ctor` | java-mirrored declaration cannot have primary constructor \| java-mirrored declaration cannot have primary constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_constructor_cannot_have_body` | java-mirrored constructor cannot have body \| java-mirrored constructor cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_have_private_member` | java-mirrored declaration cannot have private member |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_have_static_init` | java-mirrored declaration cannot have static initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_have_finalizer` | java-mirrored declaration cannot have finalizer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_have_const_member` | java-mirrored declaration cannot have const member |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_be_sealed` | @JavaMirror declaration cannot be sealed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_be_generic` | @JavaImpl declaration cannot be generic |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_be_abstract` | @JavaImpl declaration cannot be abstract |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_be_sealed` | @JavaImpl declaration cannot be sealed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_have_static_init` | @JavaImpl declaration cannot have static initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_have_open_prop` | java-mirrored declaration cannot have open property |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_be_open` | @JavaImpl class cannot be open |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_be_interface` | interface cannot be @JavaImpl |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_have_primary_ctor` | @ObjCMirror declaration cannot have primary constructor \| @ObjCMirror declaration cannot have primary constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_ctor_cannot_have_body` | @ObjCMirror declaration constructor cannot have body \| @ObjCMirror declaration constructor cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_method_cannot_have_body` | @ObjCMirror declaration method '%s' cannot have body \| @ObjCMirror declaration method cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_method_must_have_return_type` | @ObjCMirror declaration method must have return type \| @ObjCMirror declaration method must have return type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_be_sealed` | @ObjCMirror declaration cannot be sealed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_have_private_member` | @ObjCMirror declaration cannot have private member |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_have_static_init` | @ObjCMirror declaration cannot have static initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_have_finalizer` | @ObjCMirror declaration cannot have finalizer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_have_const_member` | @ObjCMirror declaration cannot have const member |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_be_generic` | @ObjCImpl declaration cannot be generic |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_be_abstract` | @ObjCImpl declaration cannot be abstract |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_be_sealed` | @ObjCImpl declaration cannot be sealed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_have_static_init` | @ObjCImpl declaration cannot have static initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_be_open` | @ObjCImpl class cannot be open |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_be_interface` | interface cannot be @ObjCImpl |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_field_cannot_have_initializer` | @ObjCMirror declaration field cannot have initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_field_cannot_be_static` | @ObjCMirror declaration field cannot be 'static' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_prop_cannot_have_getter` | @ObjCMirror declaration property '%s' cannot have getter \| @ObjCMirror declaration property cannot have getter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_prop_cannot_have_setter` | @ObjCMirror declaration property '%s' cannot have setter \| @ObjCMirror declaration property cannot have setter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_cannot_be_foreign` | @ObjCMirror top-level function '%s' cannot be foreign |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_cannot_be_c` | @ObjCMirror top-level function '%s' cannot be @C |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_cannot_be_generic` | @ObjCMirror top-level function '%s' cannot be generic |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_cannot_have_body` | @ObjCMirror top-level function '%s' cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_must_have_explicit_type` | @ObjCMirror top-level function '%s' must have result type explicitly specified |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_cannot_be_const` | @ObjCMirror top-level function '%s' cannot be const |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_must_be_top_level` | @ObjCMirror function '%s' can only be declared on top-level |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_init_method_must_be_static` | @ObjCInit method must be modified with 'static' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_init_method_must_be_in_mirror_class` | @ObjCInit method must be declared within '@ObjCMirror' class |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_optional_method_must_be_in_mirror_class` | @ObjCOptional method must be declared within '@ObjCMirror' interface |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_interop_not_supported` | Objective-C interoperability feature '%s' is not yet supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_diag_error` | %s \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_diag_warning` | %s \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_expected_position_compare_operator` | expected '= or < or <=' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_expected_hash_id_compare_operator` | expected '=' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_invalid_query_value` | should be identifer, positive integer, 'foo*' or '*foo' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_expected_query_symbol` | expected identifier or '( # _' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_expected_logic_symbol` | expected '&&' or '\|\|', but got '%s' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_position_illegal_file_id` | the first element of position tuple should be file id |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_position_illegal_line_num` | the second element of position tuple should be line number |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_position_illegal_column_num` | the third element of position tuple should be column number |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_position_comma_required` | comma required here to seperate position tuple |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_hashid_illegal_file_hash` | the first element of hash id should be file hash |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_hashid_illegal_fieldid` | the second element of hash id should be field id |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_diag_end` |  |

### 4.3 `Lexer`

- Count: 39

| Set | Severity | ID | Message |
|---|---|---|---|
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_diag_begin` | lex_diag_begin |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unknown_start_of_token` | unknown start of token: %s \| unknown start of token: %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unexpected_digit` | unexpected digit '%s' in %s \| unexpected digit \| because %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_cannot_start_with_digit` | cannot start a(n) %s literal with a '%s' digit \| unexpected digit |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_digit` | expected %s digit, found '%s' \| expected %s digit |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unexpected_decimal_point` | unexpected decimal point '.' in %s base number \| unexpected decimal point \| because of this %s prefix |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unexpected_exponent_part` | unexpected exponent part '%s__' in %s \| unexpected exponent part \| because %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_exponent_part` | expected exponent part in hexadecimal float number '%s' \| expected exponent part \| because it is hexadecimal float number |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_identifier_after_dollar` | expected identifier after '$', found %s \| expected identifier after '$' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unrecognized_symbol` | unrecognized symbol '%s' \| unrecognized symbol |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_identifier` | expected identifier, found '%s' \| expected identifier |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_back_quote` | expected '`', found '%s' \| expected '`' \| to match this opening backquote |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_single_line_string` | unterminated single-line string \| unterminated single-line string \| because this interpolation is not terminated with a '}' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_multi_line_string` | unterminated multi-line string \| unterminated multi-line string \| because this interpolation is not terminated with a '}' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_interpolation` | unterminated string interpolation \| unterminated string interpolation \| because it is in single-line string |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_multiline_string_start_from_newline` | multi-line string must start with newline character \| expected to start with newline character \| because it is in multi-line string |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_raw_string` | unterminated raw string \| unterminated raw string \| because it interpolation is not terminated with a '}' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_quote_in_raw_string` | expected '#' or '"' in raw string, found '%s' \| expected '#' or '"' \| because it is raw string prefix |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unrecognized_escape` | unrecognized escape '%s' in %s literal \| unrecognized escape \| because it is in %s literal |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_block_comment` | unterminated block comment \| unterminated block comment |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_left_bracket` | expected '{' in unicode escape, found '%s' \| expected '{' \| because it is in this Unicode escape |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_right_bracket` | expected '}', found '%s' \| expected '}' \| to match this opening '{' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_right_bracket_or_hexadecimal` | expected '}' or hexadecimal digit in unicode escape, found '%s' \| expected '}' or hexadecimal digit \| because it is in this Unicode escape |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_character` | expected a character in rune literal, found '%s' \| expected a character \| because it is in rune literal |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_letter_after_underscore` | this cannot be an identifier \| expected a Unicode XID_Continue after underscore |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_integer_suffix` | illegal integer suffix '%s' \| expected valid integer type suffix \| invalid type suffix |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_float_suffix` | illegal float number suffix '%s' \| expected valid float number type suffix \| invalid type suffix |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_non_decimal_float` | float suffix can only be in decimal \| expected valid digits \| invalid type suffix |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_character_in_char_literal` | expected one character in rune literal \| expected one character in rune literal |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_char_literal` | unterminated rune literal \| unterminated rune literal |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_characters_overflow` | rune literal may only contain one character \| may only contain one character |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unknown_suffix` | unknown suffix '%s' for number literal \| unknown suffix '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_UTF8_encoding_byte` | illegal byte '%s' in UTF-8 encoding \| illegal byte in UTF-8 encoding |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_unicode` | illegal character:%s \| illegal character:%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `lex_unsecure_unicode` | unsecure character:%s \| unsecure character:%s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_uni_character_literal` | illegal unicode scalar value '\u{%s}' \| unicode scalar value must be in range '\u{0000}' to '\u{D7FF}' or '\u{E000}' to '\u{10FFFF}' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_too_many_digits` | %s contains too many digits \| too many digits for \u \| at most 2 digits in an escaped byte character |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unrecognized_char_in_binary_string` | unrecognized character '%s' in %s \| unrecognized character here \| in %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_diag_end` | lex_diag_end |

### 4.4 `Macro`

- Count: 40

| Set | Severity | ID | Message |
|---|---|---|---|
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_diag_begin` |  |
| Basic/DiagnosticsAll.def | ERROR | `macro_undeclared_identifier` | undeclared identifier '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_attributed_macro` | expect an attributed macro '%s', but find a plain one |
| Basic/DiagnosticsAll.def | ERROR | `macro_evaluate_failed` | macro evaluation has failed for macro call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_macro_definition` | expect macro defintion '%s', but found another Declareation |
| Basic/DiagnosticsAll.def | ERROR | `macro_undefined_pkg_name` | undefined macro package '%s', expect a right package name for macro |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_plain_macro` | expect a plain macro '%s', but find an attributed one |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_invalid_attr_tokens` | illegal attribute tokens in macro call, which is '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_invalid_input_tokens` | illegal input tokens in macro call |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_invalid_escape` | illegal escape in macro call |
| Basic/DiagnosticsAll.def | ERROR | `macro_unexpect_def_and_call_in_same_pkg` | macro's call and definition can not in one package |
| Basic/DiagnosticsAll.def | ERROR | `macro_init_interpter_faild` | initialization of interpreter for macro expansion failed |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_invalid_node_replace` | invalid node expanded in macro call's father node |
| Basic/DiagnosticsAll.def | ERROR | `macro_ambiguous_match` | ambiguous match for macro call %s |
| Basic/DiagnosticsAll.def | ERROR | `macro_using_error` | macro using error, '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_failed` | macro expansion failed for macro call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_cannot_find_method` | Cannot find method from dynamic libs for macro call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_decl_not_enum_constructor` | expected declaration, but found enum constructor %s |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_declaration` | expected declaration, but found other node |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_enum_constructor` | expected an enum constructor |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_one_enum_constructor` | expected one enum constructor, but found more than one node |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_one_expr_or_pattern` | expected one expr or pattern, but found more than one node |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_one_expr` | expected one expr, but found exprs or other node |
| Basic/DiagnosticsAll.def | ERROR | `macro_unexpect_no_expr` | expected one expr, but not found expr |
| Basic/DiagnosticsAll.def | ERROR | `macro_call_map_to_empty_value_token` | Cannot handle result of macro call: token '%s' is empty |
| Basic/DiagnosticsAll.def | ERROR | `macro_call_map_info_failed` | Cannot map information for macro call due to token '%s' mismatch with '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_call_save_file_failed` | Failed to save results of macro call to file |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_code_should_not_have_macrocall` | Code generated by macro '%s' should not have macro call. |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_cannot_find_dependency` | cannot find BCHIR file for macro package dependency %s. |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_exception_occurred` | exception occurred during macro expansion. |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_failed_init_top_level` | Interpreter failed when initializing top-level. |
| Basic/DiagnosticsAll.def | ERROR | `macro_named_parameter_after_unnamed` | unnamed parameters must come before named parameters. |
| Basic/DiagnosticsAll.def | ERROR | `macro_unexpected_empty_parameter` | unexpected empty tokens after the macro expansion for the func parameters. |
| Basic/DiagnosticsAll.def | ERROR | `macro_assert_parent_context_failed` | The macro call '%s' should with the surround code contains a call '%s'. |
| Basic/DiagnosticsAll.def | ERROR | `macro_build_in_unexpect_params` | The build-in macro '%s' expected an empty parameter, but found non-empty one. |
| Basic/DiagnosticsAll.def | ERROR | `macro_build_in_unexpect_params_attrs` | The build-in macro '%s' is a plain macro, but find an attributed one. |
| Basic/DiagnosticsAll.def | ERROR | `macro_is_deprecated_error` | macro '%s' is deprecated%s%s |
| Basic/DiagnosticsAll.def | WARNING | `macro_is_deprecated_warning` | macro '%s' is deprecated%s%s |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_atexcl` | macro expansion cannot be prefixed with '@!' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_diag_end` |  |

### 4.5 `CHIR`

- Count: 47

| Set | Severity | ID | Message |
|---|---|---|---|
| Basic/DiagnosticsAll.def | ERROR | `chir_diag_begin` |  |
| Basic/DiagnosticsAll.def | ERROR | `chir_var_might_circular_dependency` | %s might have circular dependency |
| Basic/DiagnosticsAll.def | ERROR | `chir_file_might_circular_dependency` | %s might have file circular dependency |
| Basic/DiagnosticsAll.def | ERROR | `chir_used_before_initialization` | variable '%s' is used before initialization |
| Basic/DiagnosticsAll.def | ERROR | `chir_sancov_illegal_usage_of_pc_table` | use '--sanitizer-coverage-pc-table, [inline-bool-flag\|inline-8bit-counters\|trace-pc-guard]' instead |
| Basic/DiagnosticsAll.def | ERROR | `chir_sancov_illegal_usage_of_level` | '--sanitizer-coverage-level' is illegal here |
| Basic/DiagnosticsAll.def | ERROR | `interp_cannot_interp_node` | ['%s'] failed to interpret node '%s' |
| Basic/DiagnosticsAll.def | ERROR | `interp_malloc_failed` | failed allocating memory |
| Basic/DiagnosticsAll.def | ERROR | `interp_unsupported` | ['%s'] unsupported '%s' |
| Basic/DiagnosticsAll.def | ERROR | `interp_unsupported_type` | ['%s'] unsupported type '%s' |
| Basic/DiagnosticsAll.def | ERROR | `interp_cannot_convert_array2ffi` | cannot convert array to FFI value |
| Basic/DiagnosticsAll.def | ERROR | `interp_cannot_load_incremental_bchir` | could not load previous incremental BCHIR |
| Basic/DiagnosticsAll.def | ERROR | `const_eval_exception` | an exception was thrown while evaluating constant |
| Basic/DiagnosticsAll.def | ERROR | `const_eval_load_dep` | failed to load const eval dependency '%s' |
| Basic/DiagnosticsAll.def | ERROR | `const_eval_unsupported` | tried to run non-const code in const eval |
| Basic/DiagnosticsAll.def | ERROR | `chir_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_diag_begin` | chir_diag_begin |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_used_before_initialization` | variable '%s' is used before initialization |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_illegal_usage_of_member` | '%s' is not allowed to be accessed before all member variables are initialized |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_cannot_assign_initialized_let_variable` | cannot assign to value which is an initialized 'let' constant |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_class_uninitialized_field` | not all the member variables are initialized in this constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_illegal_usage_of_super_member` | super member '%s' is not allowed to be used before calling super() |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_arithmetic_operator_overflow` | arithmetic operation '%s' overflow \| operation '%s' would overflow |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_idx_out_of_bounds` | array index is out of bounds \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_divisor_is_zero` | %s by zero \| attempt to %s by zero |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_shift_length_overflow` | shift operation overflow \| attempt to shift %s bits on %s type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_typecast_overflow` | integer type conversion overflow \| type conversion from %s to %s would overflow |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_step_non_zero_range` | step cannot be zero in range expression |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_statement` | unreachable statement \| unreachable statement \| any code following this expression is unreachable |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_function` | unreachable function  \| function with parameter of nothing type will never be executed |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_return` | unreachable return \| unreachable return |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_if` | unreachable '%s' expression \| unreachable '%s' expression \| any code following this expression is unreachable |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable` | unreachable '%s' \| unreachable '%s' \| any code following this expression is unreachable |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_expression_hint` | unreachable expression \| unreachable expression \| any code following this expression is unreachable |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_block_in_expression` | unreachable block in '%s' expression \| unreachable block in '%s' expression |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_block` | unreachable block \| unreachable block |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unused_expression` | unused expression \| unused expression |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unused_function` | unused function:'%s' \| unused function |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unused_function_main` | unused function:'main' \| unused function |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_expression` | unreachable expression \| unreachable expression |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unused_variable` | unused variable:'%s' \| unused variable |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unused_operator` | unused result of the operator:'%s' \| unused result of the operator |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_unreachable_pattern` | unreachable pattern |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_eval_support` | const evaluation has been disabled by compiler option `--no-interp-const-eval` |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_annotation_not_applicable` | '@%s' not applicable to %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_native_ffi_java_illegal_type_cast` | Illegal type cast from Java type '%s' to non Java type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_diag_end` | chir_diag_end |

## 5. 官方诊断消息（完整快照）

字段说明：`Set | Severity | ID | Message`。

| Set | Severity | ID | Message |
|---|---|---|---|
| Basic/DiagnosticsAll.def | ERROR | `chir_diag_begin` |  |
| Basic/DiagnosticsAll.def | ERROR | `chir_var_might_circular_dependency` | %s might have circular dependency |
| Basic/DiagnosticsAll.def | ERROR | `chir_file_might_circular_dependency` | %s might have file circular dependency |
| Basic/DiagnosticsAll.def | ERROR | `chir_used_before_initialization` | variable '%s' is used before initialization |
| Basic/DiagnosticsAll.def | ERROR | `chir_sancov_illegal_usage_of_pc_table` | use '--sanitizer-coverage-pc-table, [inline-bool-flag\|inline-8bit-counters\|trace-pc-guard]' instead |
| Basic/DiagnosticsAll.def | ERROR | `chir_sancov_illegal_usage_of_level` | '--sanitizer-coverage-level' is illegal here |
| Basic/DiagnosticsAll.def | ERROR | `interp_cannot_interp_node` | ['%s'] failed to interpret node '%s' |
| Basic/DiagnosticsAll.def | ERROR | `interp_malloc_failed` | failed allocating memory |
| Basic/DiagnosticsAll.def | ERROR | `interp_unsupported` | ['%s'] unsupported '%s' |
| Basic/DiagnosticsAll.def | ERROR | `interp_unsupported_type` | ['%s'] unsupported type '%s' |
| Basic/DiagnosticsAll.def | ERROR | `interp_cannot_convert_array2ffi` | cannot convert array to FFI value |
| Basic/DiagnosticsAll.def | ERROR | `interp_cannot_load_incremental_bchir` | could not load previous incremental BCHIR |
| Basic/DiagnosticsAll.def | ERROR | `const_eval_exception` | an exception was thrown while evaluating constant |
| Basic/DiagnosticsAll.def | ERROR | `const_eval_load_dep` | failed to load const eval dependency '%s' |
| Basic/DiagnosticsAll.def | ERROR | `const_eval_unsupported` | tried to run non-const code in const eval |
| Basic/DiagnosticsAll.def | ERROR | `chir_diag_end` |  |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_diag_begin` |  |
| Basic/DiagnosticsAll.def | ERROR | `macro_undeclared_identifier` | undeclared identifier '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_attributed_macro` | expect an attributed macro '%s', but find a plain one |
| Basic/DiagnosticsAll.def | ERROR | `macro_evaluate_failed` | macro evaluation has failed for macro call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_macro_definition` | expect macro defintion '%s', but found another Declareation |
| Basic/DiagnosticsAll.def | ERROR | `macro_undefined_pkg_name` | undefined macro package '%s', expect a right package name for macro |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_plain_macro` | expect a plain macro '%s', but find an attributed one |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_invalid_attr_tokens` | illegal attribute tokens in macro call, which is '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_invalid_input_tokens` | illegal input tokens in macro call |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_invalid_escape` | illegal escape in macro call |
| Basic/DiagnosticsAll.def | ERROR | `macro_unexpect_def_and_call_in_same_pkg` | macro's call and definition can not in one package |
| Basic/DiagnosticsAll.def | ERROR | `macro_init_interpter_faild` | initialization of interpreter for macro expansion failed |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_invalid_node_replace` | invalid node expanded in macro call's father node |
| Basic/DiagnosticsAll.def | ERROR | `macro_ambiguous_match` | ambiguous match for macro call %s |
| Basic/DiagnosticsAll.def | ERROR | `macro_using_error` | macro using error, '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_failed` | macro expansion failed for macro call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `not_find_macro_library` | not find macro definition library path |
| Basic/DiagnosticsAll.def | ERROR | `can_not_open_macro_library` | Cannot dlopen from the dynamic library '%s' for macro |
| Basic/DiagnosticsAll.def | ERROR | `macro_cannot_find_method` | Cannot find method from dynamic libs for macro call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_decl_not_enum_constructor` | expected declaration, but found enum constructor %s |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_declaration` | expected declaration, but found other node |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_enum_constructor` | expected an enum constructor |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_one_enum_constructor` | expected one enum constructor, but found more than one node |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_one_expr_or_pattern` | expected one expr or pattern, but found more than one node |
| Basic/DiagnosticsAll.def | ERROR | `macro_expect_one_expr` | expected one expr, but found exprs or other node |
| Basic/DiagnosticsAll.def | ERROR | `macro_unexpect_no_expr` | expected one expr, but not found expr |
| Basic/DiagnosticsAll.def | ERROR | `macro_call_map_to_empty_value_token` | Cannot handle result of macro call: token '%s' is empty |
| Basic/DiagnosticsAll.def | ERROR | `macro_call_map_info_failed` | Cannot map information for macro call due to token '%s' mismatch with '%s' |
| Basic/DiagnosticsAll.def | ERROR | `macro_call_save_file_failed` | Failed to save results of macro call to file |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_code_should_not_have_macrocall` | Code generated by macro '%s' should not have macro call. |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_cannot_find_dependency` | cannot find BCHIR file for macro package dependency %s. |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_exception_occurred` | exception occurred during macro expansion. |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_failed_init_top_level` | Interpreter failed when initializing top-level. |
| Basic/DiagnosticsAll.def | ERROR | `macro_named_parameter_after_unnamed` | unnamed parameters must come before named parameters. |
| Basic/DiagnosticsAll.def | ERROR | `macro_unexpected_empty_parameter` | unexpected empty tokens after the macro expansion for the func parameters. |
| Basic/DiagnosticsAll.def | ERROR | `macro_assert_parent_context_failed` | The macro call '%s' should with the surround code contains a call '%s'. |
| Basic/DiagnosticsAll.def | ERROR | `macro_build_in_unexpect_params` | The build-in macro '%s' expected an empty parameter, but found non-empty one. |
| Basic/DiagnosticsAll.def | ERROR | `macro_build_in_unexpect_params_attrs` | The build-in macro '%s' is a plain macro, but find an attributed one. |
| Basic/DiagnosticsAll.def | ERROR | `macro_is_deprecated_error` | macro '%s' is deprecated%s%s |
| Basic/DiagnosticsAll.def | WARNING | `macro_is_deprecated_warning` | macro '%s' is deprecated%s%s |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_atexcl` | macro expansion cannot be prefixed with '@!' |
| Basic/DiagnosticsAll.def | ERROR | `macro_expand_diag_end` |  |
| Basic/DiagnosticsAll.def | ERROR | `sema_diag_begin` |  |
| Basic/DiagnosticsAll.def | ERROR | `sema_diag_report_error_message` | %s |
| Basic/DiagnosticsAll.def | NOTE | `sema_diag_report_note_message` | %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_incompatible_expo_target_type` | the type of an exponentiation expression is either 'Int64' or 'Float64', which conflicts the type '%s' required  \| by the context |
| Basic/DiagnosticsAll.def | ERROR | `sema_ambiguous_expo_right_operand_type` | both 'Int64' and 'Float64' is compatible with the right operand's type of this exponentiation (assignment)  \| expression; please give an explicit one |
| Basic/DiagnosticsAll.def | ERROR | `sema_not_a_type` | '%s' is not a type |
| Basic/DiagnosticsAll.def | ERROR | `sema_incompatible_func_body_and_return_type` | the return type of this function cannot be calculated from the function body and all the return expressions |
| Basic/DiagnosticsAll.def | ERROR | `sema_type_must_toplevel` | %s type must be toplevel |
| Basic/DiagnosticsAll.def | ERROR | `sema_undeclared_type_name` | undeclared type name '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_undeclared_identifier` | undeclared identifier '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_redefinition` | redefinition of declaration '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_expand_macro_redefinition` | redefinition of macro '%s' |
| Basic/DiagnosticsAll.def | NOTE | `sema_previous_decl` | '%s' is previously declared here |
| Basic/DiagnosticsAll.def | ERROR | `sema_used_before_initialization` | variable '%s' is used before initialization |
| Basic/DiagnosticsAll.def | ERROR | `sema_global_var_used_before_initialization` | global/static variable '%s' is used before initialization during initializing '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_duplicated_item_in_enum` | '%s' is already exist in enum '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_constructor_in_enum` | invalid constructor in enum declaration |
| Basic/DiagnosticsAll.def | ERROR | `sema_multiple_constructor_in_enum` | find multiple constructor '%s' of enum declaration |
| Basic/DiagnosticsAll.def | ERROR | `sema_enum_constructor_type_not_match` | no matching enum constructor '%s' for given arguments |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_enum_member_access` | base of member access can not be enum variable |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_type_param_of_enum_member_access` | type arguments cannot appear after '%s' when enum type '%s' is given |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_loop_control` | 'break' or 'continue' must be used inside a loop |
| Basic/DiagnosticsAll.def | ERROR | `sema_cannot_currying` | %s cannot have more than one parameter list |
| Basic/DiagnosticsAll.def | ERROR | `sema_value_type_recursive` | value type recursive detected: '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_inheritance_cycle` | inheritance cycle detected: '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_typealias_cycle` | type cycle detected: '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_typealias_external_refer_internal` | '%s' type '%s' refers to '%s' type '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_coalescing` | type of left operand does not support coalescing operation. coalescing is only valid for 'Option' |
| Basic/DiagnosticsAll.def | ERROR | `sema_div_zero` | division by 0 |
| Basic/DiagnosticsAll.def | ERROR | `sema_mod_zero` | mod by 0 |
| Basic/DiagnosticsAll.def | ERROR | `sema_arithmetical_op_overflow` | overflow in '%s' calculation |
| Basic/DiagnosticsAll.def | ERROR | `sema_shift_count_overflow` | shift count overflow |
| Basic/DiagnosticsAll.def | ERROR | `sema_negative_shift_count` | shift count is negative |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_capture_var_cannot_assign` | %s captured a mutable variable %s, %s cannot be assigned to a variable |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_capture_var_cannot_return` | %s captured a mutable variable %s, %s cannot be used as a return value |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_capture_var_cannot_param` | %s captured a mutable variable %s, %s cannot be used as a param |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_capture_var_cannot_expr` | %s captured a mutable variable %s, %s cannot be used as a expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_unary_expr` | invalid unary operator '%s' on type '%s' |
| Basic/DiagnosticsAll.def | NOTE | `sema_invalid_unary_expr_note` | you may want to implement operator func %s() for type '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_fail_flow_expr_operand_has_named_param` | flow operand cannot contain named parameter |
| Basic/DiagnosticsAll.def | ERROR | `sema_operator_overload_invalid_num_parameter` | invalid number of parameters for operator '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_operator_overload_built_in_unary_operator` | operator func %s() of type %s is a built-in function and cannot be overridden |
| Basic/DiagnosticsAll.def | ERROR | `sema_operator_overload_built_in_binary_operator` | operator func %s(%s) of type %s is a built-in function and cannot be overridden |
| Basic/DiagnosticsAll.def | ERROR | `sema_operator_overload_can_not_has_default_param` | optional parameter can not be used in operator overload function |
| Basic/DiagnosticsAll.def | ERROR | `sema_empty_arrayLit_type_undefined` | array literal type cannot be inferred |
| Basic/DiagnosticsAll.def | ERROR | `sema_inconsistency_elemType` | inconsistent element type for %s literal |
| Basic/DiagnosticsAll.def | ERROR | `sema_tuple_pattern_not_match` | %s isn't a tuple to match tuple pattern |
| Basic/DiagnosticsAll.def | ERROR | `sema_unsupport_operator` | not supported operator: '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_size_type_error` | array size must be of type Int64 |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_expression_param_type_error` | array init expression parameter's type must be type Int64 |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_expression_type_error` | array init expression must has type (Int64)->T |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_single_element_type_error` | array init with single element must be subtype of 'List' or 'Collection' |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_element_type_error` | array initialize element type error |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_too_much_argument` | too much arguments given for array constructor, only accept 0~2 arguments |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_first_arg_cannot_be_named` | array's first argument cannot be named argument |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_second_arg_cannot_be_named` | array's second argument cannot be named argument when type is (Int64)->T |
| Basic/DiagnosticsAll.def | ERROR | `sema_array_second_wrong_named_arg` | array's second argument must have named prefix 'repeat:' when type is T |
| Basic/DiagnosticsAll.def | ERROR | `sema_pointer_too_much_argument` | too much arguments given for CPointer constructor, only accept 0~1 arguments |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_too_many_arguments` | too many arguments given to CFunc constructor, only accept 1 argument |
| Basic/DiagnosticsAll.def | ERROR | `sema_pointer_single_element_type_error` | the single argument of CPointer constructor must be 'CPointer' or 'CFunc' |
| Basic/DiagnosticsAll.def | ERROR | `sema_pointer_unknow_generic_type` | 'CPointer' generic type cannot be inferred |
| Basic/DiagnosticsAll.def | ERROR | `sema_builtin_invalid_index` | %s index must be an integer literal |
| Basic/DiagnosticsAll.def | ERROR | `sema_builtin_index_in_bound` | %s index must be in bounds |
| Basic/DiagnosticsAll.def | ERROR | `sema_tuple_element_cmp_not_bool` | the '%s' operation between type '%s' and type '%s' is not evaluated to a Bool |
| Basic/DiagnosticsAll.def | ERROR | `sema_tuple_cmp_not_supported` | operator '%s' between tuple type '%s' and '%s' is not supported |
| Basic/DiagnosticsAll.def | ERROR | `sema_step_non_zero_range` | step cannot be zero in range expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_inconsistency_range_elemType` | start and stop must be of the same type in range expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_range_step_not_int64` | step must be Int64 in range expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_no_match_function_declaration_for_call` | no matching function declaration for function call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_no_match_function_declaration_for_ref` | no matching function declaration for function reference '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_no_match_constructor` | no matching constructor for call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_ambiguous_arg_type` | ambiguous arguments type in call expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_ambiguous_match` | ambiguous match for function call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_ambiguous_constructor_match` | ambiguous match for constructor call '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_ambiguous_func_ref` | ambiguous match for reference '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_mismatched_type_for_pattern_in_vardecl` | the pattern in this variable declaration can not match its type |
| Basic/DiagnosticsAll.def | ERROR | `sema_parameters_and_arguments_mismatch` | parameters and arguments mismatch |
| Basic/DiagnosticsAll.def | ERROR | `sema_cstruct_cannot_autobox` | struct with @C cannot implicitly used as '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_unit_cannot_as_cfunc_arg` | Unit cannot be used as argument type of CFunc |
| Basic/DiagnosticsAll.def | ERROR | `sema_overload_conflicts` | %s '%s' has overload conflicts |
| Basic/DiagnosticsAll.def | ERROR | `sema_no_match_operator_function_call` | no matching function for operator '()' function call |
| Basic/DiagnosticsAll.def | ERROR | `sema_pattern_can_not_be_assigned` | the pattern isn't irrefutable pattern and it can not be initialized |
| Basic/DiagnosticsAll.def | ERROR | `sema_unknown_named_argument` | unknown named argument prefix '%s:' |
| Basic/DiagnosticsAll.def | ERROR | `sema_multiple_named_argument` | named argument prefix '%s:' cannot appeared more than once in call |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_named_arguments` | invalid named arguments prefix '%s:', target is not a named parameter |
| Basic/DiagnosticsAll.def | ERROR | `sema_unsupport_named_argument` | named argument cannot be used in variable function call |
| Basic/DiagnosticsAll.def | ERROR | `sema_pattern_literal_expected` | only const literal is allowed in const pattern |
| Basic/DiagnosticsAll.def | ERROR | `sema_pattern_not_match` | %s pattern is not matched |
| Basic/DiagnosticsAll.def | ERROR | `sema_not_overload_in_match` | no overloaded '==' function in match case pattern |
| Basic/DiagnosticsAll.def | ERROR | `sema_type_incompatible` | type incompatible in this %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_subscript_set_not_supported` | type %s does not have operator func [](index, value) for index type %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_subscript_get_set_not_supported` | type %s does not have both operator func [](index) and operator func [](index, value) for index type %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_tuple_pattern_with_correct_size_expected` | tuple pattern with correct size expected |
| Basic/DiagnosticsAll.def | ERROR | `sema_enum_pattern_param_size_error` | enum pattern's parameters size is wrong |
| Basic/DiagnosticsAll.def | ERROR | `sema_match_case_must_have_default` | at least one default case, such as wildcard pattern, variable pattern or '[...]' for sequence pattern in match  \| case. |
| Basic/DiagnosticsAll.def | ERROR | `sema_match_case_has_no_type` | this match case has no type |
| Basic/DiagnosticsAll.def | ERROR | `sema_package_internal_decl_obtain_illegal` | %s '%s' in package '%s' cannot be obtained |
| Basic/DiagnosticsAll.def | ERROR | `sema_package_name_conflict` | package name '%s' is conflicted with other imported package name, please use 'as' to eliminate conflict. |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_access_control` | can not access field '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_access_function` | can not access function '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_file_hash` | There is invalid file hash in access control check |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_no_override_or_redefine_modifier` | do not need '%s' modifier for %s '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_unexpected_param_for_entry` | 'main' cannot be defined with parameter whose type is not 'Array<String>' |
| Basic/DiagnosticsAll.def | ERROR | `sema_unexpected_return_type_for_entry` | return type of 'main' is not 'Integer' or 'Unit' |
| Basic/DiagnosticsAll.def | ERROR | `sema_redefinition_entry` | multiple 'main's are found in source files |
| Basic/DiagnosticsAll.def | ERROR | `sema_missing_entry` | 'main' is missing |
| Basic/DiagnosticsAll.def | ERROR | `sema_numeric_convert_must_be_numeric` | the expression for numeric type conversion must have a numeric type |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_ctor_must_be_cpointer` | argument type of 'CFunc' constructor must be of type 'CPointer' |
| Basic/DiagnosticsAll.def | ERROR | `sema_ref_not_be_type` | expected member name or constructor call after '%s' type name |
| Basic/DiagnosticsAll.def | ERROR | `sema_expr_in_forin_must_has_iterator` | the type %s of expression in for-in expression does not implement Iterator |
| Basic/DiagnosticsAll.def | ERROR | `sema_forin_pattern_must_be_irrefutable` | the pattern in for-in expression must be irrefutable |
| Basic/DiagnosticsAll.def | ERROR | `sema_wrong_forin_guard` | pattern guard should be Boolean type |
| Basic/DiagnosticsAll.def | ERROR | `sema_generics_type_variable_not_defined` | generics type variable '%s' has not defined |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_type_argument_not_match_constraint` | generics type arguments do not match the constraint of '%s' |
| Basic/DiagnosticsAll.def | NOTE | `sema_which_constraint_not_match` | '%s' is not a subtype of %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_type_without_type_argument` | generic type should be used with type argument |
| Basic/DiagnosticsAll.def | ERROR | `sema_non_generic_function_with_type_argument` | non-generic function should not be used with type argument |
| Basic/DiagnosticsAll.def | ERROR | `sema_throw_expr_with_wrong_type` | the object thrown must derive from `core.Exception` |
| Basic/DiagnosticsAll.def | ERROR | `sema_except_catch_type_error` | the exception catch type must be class and extends from core.Exception or core.Error |
| Basic/DiagnosticsAll.def | ERROR | `sema_no_core_object` | `core` package should be imported |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_intrinsic_decl` | intrinsic function '%s' cannot be declared in '%s' package |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_infinite_instantiation` | generic infinite instantiation |
| Basic/DiagnosticsAll.def | ERROR | `sema_forbid_generic_nonstatic_method` | non-static generic member function '%s' is not supported |
| Basic/DiagnosticsAll.def | ERROR | `sema_forbid_generic_constructor` | generic constructor '%s' is not supported |
| Basic/DiagnosticsAll.def | ERROR | `sema_forbid_generic_finalizer` | generic finalizer '%s' is not supported |
| Basic/DiagnosticsAll.def | ERROR | `sema_import_not_in_current_module` | this package does not belong to the current module, please write its module name explicitly. |
| Basic/DiagnosticsAll.def | ERROR | `sema_flow_expressions_use_this_or_super` | '%s' is not allowed to be used in flow expressions |
| Basic/DiagnosticsAll.def | ERROR | `sema_symbol_not_collected` | reference node named '%s' is not collected in symbol table. |
| Basic/DiagnosticsAll.def | ERROR | `sema_cannot_convert_literal` | cannot convert %s literal to type '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_cannot_have_parameter` | %s cannot have parameter |
| Basic/DiagnosticsAll.def | ERROR | `sema_only_cfunc_can_use_annotation` | only CFunc can use '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_annotation_error_arg_num` | '%s' should have %s arg |
| Basic/DiagnosticsAll.def | ERROR | `sema_annotation_calling_conv_not_support` | '@CallingConv' have not support '%s' yet |
| Basic/DiagnosticsAll.def | ERROR | `sema_annotation_invalid_args_type` | '%s' arg should be right type |
| Basic/DiagnosticsAll.def | ERROR | `sema_unexpected_wrapper` | unexpected %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_native_var_error` | variable can not be modified with 'foreign' and implicit @C. |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_scope_use_of_annotation` | '%s' can only be used in top-level scope |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_use_of_annotation` | %s cannot be modified with '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_cffi_cannot_have_type_param` | %s cannot have type parameters |
| Basic/DiagnosticsAll.def | ERROR | `sema_unsafe_function_invoke_failed` | unsafe function or native function should be invoked in unsafe context. |
| Basic/DiagnosticsAll.def | ERROR | `sema_func_capture_var_not_ctype` | captured variable mustn't be struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_cannot_capture_var` | cannot capture variable %s in CFunc lambda expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_cannot_capture_this` | '%s' is not allowed to be captured in CFunc lambda expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_ctype_generic_argument` | generic argument mustn't be struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_cpointer_generic_type` | generic type of CPointer must be instantiated with CType |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_cfunc_arg_type` | arguments type of CFunc must be instantiated with CType |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_type` | cfunc type must be a function type |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_tuple_field_ctype` | tuple member mustn't be struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_enum_pattern_func_cty_error` | member func '%s' is forbidden in enum '%s' with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_enum_pattern_func_param_cty_error` | member func '%s' of enum '%s' mustn't has struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_ctype_member` | non-static member variable '%s' of %s '%s' cannot be struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_member_of_cstruct` | member variable '%s' of struct '%s' with @C must be instantiated with CType |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_cannot_have_unit_args` | CFunc cannot have arguments of type Unit |
| Basic/DiagnosticsAll.def | ERROR | `sema_cstruct_cannot_have_unit_fields` | member variables cannot be type Unit in struct with @C |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_cannot_have_named_args` | CFunc cannot have named arguments |
| Basic/DiagnosticsAll.def | ERROR | `sema_cfunc_var_cannot_have_var_param` | CFunc with variable-length parameters cannot be assigned to variables |
| Basic/DiagnosticsAll.def | ERROR | `sema_inheritance_non_ref_type` | inheritance is not a ref type: '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_class_uninitialized_field` | the uninitialized member variable '%s' is not initialized in the constructor of class or struct |
| Basic/DiagnosticsAll.def | ERROR | `sema_this_or_super_not_allowed_to_initialize_non_static_member` | '%s' is not allowed to initialize non-static member |
| Basic/DiagnosticsAll.def | ERROR | `sema_this_or_super_not_allowed_to_initialize_static_member` | '%s' is not allowed to initialize static member |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_usage_of_super_member` | super member '%s' is not allowed to be used before calling super() |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_usage_of_member` | '%s' is not allowed to be accessed before all member variables are initialized |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_assignment_to_this_expr` | cannot assign a value to 'this' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_override_member_in_class` | cannot override non-abstract %s '%s' with abstract %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_override_or_redefine_member_in_interface` | cannot override implemented interface %s '%s' with abstract %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_this_in_interface` | 'this' can not be used in interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_use_super_in_interface` | 'super' cannot be used in interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_super_use_error_inside_non_class` | 'super' can only be used in class |
| Basic/DiagnosticsAll.def | ERROR | `sema_assignment_of_member_variable_cannot_use_this_or_super` | '%s' is not allowed to be used in %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_super_alone` | invalid super expression, it can only be used on the left-hand side of a dot |
| Basic/DiagnosticsAll.def | ERROR | `sema_abstract_class_can_not_be_instantiated` | abstract class '%s' can not be instantiated |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_can_not_be_instantiated` | interface '%s' can not be instantiated |
| Basic/DiagnosticsAll.def | ERROR | `sema_non_inheritable_super_class` | super class '%s' is not inheritable |
| Basic/DiagnosticsAll.def | ERROR | `sema_superclass_must_be_placed_at_first` | super class '%s' must be placed at the beginning of supertype list |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_multi_inheritance` | only one super class may appear in supertype list of class '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_this_call_outside_ctor` | invalid calling '%s' outside the constructor |
| Basic/DiagnosticsAll.def | ERROR | `sema_privated_abstract_func_in_class` | private abstract %s is forbidden in abstract class '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_string_implementation` | the type '%s' should implement interface 'ToString' in the 'core' package |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_tokens_implementation` | the type '%s' should implement interface 'ToTokens' in the 'ast' package |
| Basic/DiagnosticsAll.def | ERROR | `sema_multiple_primary_constructors` | %s '%s' cannot have more than one primary constructor |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_is_not_inheritable` | '%s' interface is not able to be inherited |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_is_not_implementable` | '%s' interface is not able to be implemented explicitly |
| Basic/DiagnosticsAll.def | ERROR | `sema_inherit_duplicate_interface` | %s '%s' inherits or implements duplicate interface '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_return_unit` | return expressions in a constructor must be either 'return' or 'return ()' |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_place_of_calling_this_or_super` | call to '%s' must be first expression in constructor of %s '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_place_of_calling_this_primary_constructor` | invalid calling 'this' in primary constructor |
| Basic/DiagnosticsAll.def | ERROR | `sema_this_super_use_error_outside_class` | '%s' cannot be used outside class or struct or interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_missing_func_body` | %s '%s' can not be abstract |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_member_must_be_implemented` | interface %s '%s' must be implemented in '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_member_must_be_implemented_in_struct` | interface %s '%s' must be implemented in '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_member_variable_can_not_shadow` | the variable '%s' must not shadow a member variable of the supertype |
| Basic/DiagnosticsAll.def | ERROR | `sema_missing_overridden_func` | 'override' %s '%s' does not have an overridden %s in its supertype |
| Basic/DiagnosticsAll.def | ERROR | `sema_missing_redefined_func` | 'redef' %s '%s' does not have a redefined 'static' %s in its supertype |
| Basic/DiagnosticsAll.def | ERROR | `sema_static_and_non_static_member_cannot_have_same_name` | %s member '%s' cannot have the same name with %s member in %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_member_type_argument_different` | type argument of function '%s' is different in parent class or interfaces |
| Basic/DiagnosticsAll.def | ERROR | `sema_c_type_cannot_implement_interface` | c type '%s' cannot implement interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_access_non_static_member` | '%s' is non-static member, cannot access by type name |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_access_interface_field` | field '%s' cannot be accessed without interface name '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_access_inner_classlike` | inner %s '%s' cannot be accessed without name '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_cannot_modify_var` | instance member variable '%s' cannot be modified in immutable function |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_capture_this` | 'this' is not allowed to be captured in constructor of %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_object_cannot_access_static_member` | object cannot access static member '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_abstract_method_cannot_be_accessed_directly` | abstract method '%s' cannot be accessed directly |
| Basic/DiagnosticsAll.def | ERROR | `sema_return_type_invariance` | return type of '%s' can only be class/interface type which implements or inherits the interface type '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_static_members_cannot_call_members` | non-static variable '%s' cannot be referenced from a static context |
| Basic/DiagnosticsAll.def | ERROR | `sema_class_inherit_non_class_nor_interface` | class '%s' can only inherit a class or implement interfaces |
| Basic/DiagnosticsAll.def | ERROR | `sema_type_implement_non_interface` | %s '%s' can only implement interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_interface_inherit_non_interface` | interface '%s' can only inherit interface |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_in_operator_overload` | generic is not allowed in operator overload function |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_this_outside_struct_constructor` | 'this' is only allowed to be used inside constructor or function for struct |
| Basic/DiagnosticsAll.def | ERROR | `sema_static_function_cannot_access_non_static_member` | '%s' is non-static member, cannot be accessed by static function '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_static_variable_cannot_access_non_static_member` | '%s' is non-static member, cannot be accessed by static variable |
| Basic/DiagnosticsAll.def | ERROR | `sema_static_lambdaExpr_cannot_access_non_static` | invalid use of non-static member '%s' in static lambda expression |
| Basic/DiagnosticsAll.def | ERROR | `sema_redef_modify_static_func` | 'redef' cannot be used to modify an instance '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_illegal_member_used_in_open_constructor` | instance member %s '%s' cannot be accessed in the constructor of open class '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_finalizer_forbidden_in_class` | finalizer is forbidden in class '%s' that is %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_no_member_match_in_upper_bounds` | no member match for generic member access when searching in upper bounds |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_ambiguous_method_match_in_upper_bounds` | ambiguous method '%s' match for generic member access function call when searching in upper bounds |
| Basic/DiagnosticsAll.def | ERROR | `sema_generic_no_method_match_in_upper_bounds` | no method '%s' match for generic member access function call when searching in upper bounds |
| Basic/DiagnosticsAll.def | ERROR | `sema_cannot_instantiated_by_incomplete_type` | can not instantiate '%s' by %s for it has unimplemented static member |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_field_expose_access` | '%s' is not a static member of %s '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_capture_this_or_instance_field_in_func` | '%s' cannot be captured in the %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_use_this_as_an_expression_in_func` | 'this' cannot be used as an expression in the %s |
| Basic/DiagnosticsAll.def | ERROR | `sema_incompatible_mut_modifier_between_struct_and_interface` | 'mut' modifier of '%s' is incompatible with that in interface '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_immutable_function_cannot_access_mutable_function` | immutable function '%s' cannot access mutable function '%s' |
| Basic/DiagnosticsAll.def | ERROR | `sema_invalid_position_of_this_type` | 'This' type is not allowed here |
| Basic/DiagnosticsAll.def | ERROR | `sema_property_override_implement_type_diff` | The type of the override/implement property must be the same |
| Basic/DiagnosticsAll.def | WARNING | `typealias_unused_type_parameters` | type arg(s) %s are not used |
| Basic/DiagnosticsAll.def | WARNING | `sema_capture_has_shadow_variable` | the variable '%s' actually captures this decl %p, can not captures the decl %p |
| Basic/DiagnosticsAll.def | WARNING | `sema_useless_exception_type` | useless exception type |
| Basic/DiagnosticsAll.def | WARNING | `sema_ignore_open` | the current member should not have 'open' modifier because it is in a non-inheritable class |
| Basic/DiagnosticsAll.def | NOTE | `sema_found_candidate_decl` | found candidate |
| Basic/DiagnosticsAll.def | NOTE | `sema_found_possible_candidate_decl` | found possible candidate |
| Basic/DiagnosticsAll.def | ERROR | `sema_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_diag_begin` | chir_diag_begin |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_used_before_initialization` | variable '%s' is used before initialization |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_illegal_usage_of_member` | '%s' is not allowed to be accessed before all member variables are initialized |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_cannot_assign_initialized_let_variable` | cannot assign to value which is an initialized 'let' constant |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_class_uninitialized_field` | not all the member variables are initialized in this constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_illegal_usage_of_super_member` | super member '%s' is not allowed to be used before calling super() |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_arithmetic_operator_overflow` | arithmetic operation '%s' overflow \| operation '%s' would overflow |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_idx_out_of_bounds` | array index is out of bounds \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_divisor_is_zero` | %s by zero \| attempt to %s by zero |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_shift_length_overflow` | shift operation overflow \| attempt to shift %s bits on %s type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_typecast_overflow` | integer type conversion overflow \| type conversion from %s to %s would overflow |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_step_non_zero_range` | step cannot be zero in range expression |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_statement` | unreachable statement \| unreachable statement \| any code following this expression is unreachable |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_function` | unreachable function  \| function with parameter of nothing type will never be executed |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_return` | unreachable return \| unreachable return |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_if` | unreachable '%s' expression \| unreachable '%s' expression \| any code following this expression is unreachable |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable` | unreachable '%s' \| unreachable '%s' \| any code following this expression is unreachable |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_expression_hint` | unreachable expression \| unreachable expression \| any code following this expression is unreachable |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_block_in_expression` | unreachable block in '%s' expression \| unreachable block in '%s' expression |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_block` | unreachable block \| unreachable block |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unused_expression` | unused expression \| unused expression |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unused_function` | unused function:'%s' \| unused function |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unused_function_main` | unused function:'main' \| unused function |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unreachable_expression` | unreachable expression \| unreachable expression |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unused_variable` | unused variable:'%s' \| unused variable |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_dce_unused_operator` | unused result of the operator:'%s' \| unused result of the operator |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `chir_unreachable_pattern` | unreachable pattern |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_eval_support` | const evaluation has been disabled by compiler option `--no-interp-const-eval` |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_annotation_not_applicable` | '@%s' not applicable to %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `frontend_can_not_handle_to_many_chir` | Can't handle more than one CHIR file |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_native_ffi_java_illegal_type_cast` | Illegal type cast from Java type '%s' to non Java type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `chir_diag_end` | chir_diag_end |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `conditional_compilation_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `conditional_compilation_not_support_op` | conditional compilation '%s' not support this op '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `conditional_compilation_not_support_this_condition` | conditional compilation have not supported this condition: '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `conditional_compilation_not_support_builtin_value` | builtin condition '%s' do not support '%s', supported listed: '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `conditional_compilation_not_support_cjc_version_format` | cjc version's format should be 'xx.xx.xx' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `conditional_compilation_invalid_condition_expr` | conditional compilation not support this expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `conditional_compilation_not_have_condition_expr` | conditional compilation should have condition expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `conditional_compilation_invalid_condition_value` | conditional compilation's condition value should be string literal without interpolation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `conditional_compilation_just_support_block` | conditional compilation macro '@If' and '@Else' only support used in block |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `conditional_compilation_unexpected_after_macro` | unexpected conditional compilation directive |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `conditional_compilation_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `no_such_file_or_directory` | No such file or directory: '%s'. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `no_such_directory` | No such directory: '%s'. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `not_a_directory` | Not a directory: '%s'. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `not_a_file` | Not a file: '%s'. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `permission_denied` | Permission denied: '%s'. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `invalid_path` | Invalid path: '%s'. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `no_such_file_or_directory_of_macro_obj` | No such file or directory: '%s', please check if input file exists, separate paths by space if there are multiple input files |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `input_file_overwritten_by_generated_output` | The input file '%s' would be overwritten by the generated output. |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_no_such_file` | No such file: '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_no_such_file_or_directory` | No such file or directory: '%s'.%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_no_such_directory` | No such directory: '%s'.%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_not_a_directory` | Not a directory: '%s'.%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_not_a_file` | Not a file: '%s'.%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_permission_denied` | Permission denied: '%s'.%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_invalid_path` | Invalid path: '%s'.%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_not_pdba_file` | Not a .pdba file: '%s' ('%s').%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_not_cjo_file` | Not a .cjo file: '%s' ('%s').%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_not_cj_file` | Not a .cj file: '%s' ('%s').%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_not_chir_file` | Not a .chir file: '%s' ('%s').%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_not_bc_file` | Not a .bc file: '%s' ('%s').%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_not_archive_file` | Not a .a file: '%s' ('%s').%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_not_object_file` | Not a .o file: '%s' ('%s').%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_argument_unused` | the arg '%s' may be unused |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_warning_path_close_to_length_limit` | the path length for %s is close to the system limit (%s characters). The compilation has a possibility to fail. |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_invalid_compile_target` | --compile-target only takes effect when --output-type=obj, this option will be ignored in other scenarios |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_static_std_for_ohos` | Statically link packages of the std module is not supported for OHOS |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_require_package_directory` | expected one package path to build |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_require_package_directory_scan_dependency` | expected one package path to scan dependency |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_require_one_package_directory` | expect exact one package path to build |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_require_one_package_directory_scan_dependency` | expect exact one .cjo file to scan dependency |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_source_cjo_empty` | expected one .cjo file to scan dependency |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_not_accept_cjo_inputs_when` | not accept .cjo inputs when %s is specified |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_source_file_empty` | expected at least one source code file when compiling source code |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_invalid_source_file` | source file '%s' doesn't exist |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_invalid_file_or_directory` | invalid file or directory '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_invalid_binary_file` | invalid binary file '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_invalid_unsupport_extension` | unsupported output extension '.%s', extension '.%s' is required |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_invalid_require_extension` | output to '%s' is not allowed, extension '.%s' is required |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_path_exceeds_length_limit` | the path length for %s exceeds the system limit (%s characters) |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_unsupported_target_cpu` | cpu type '%s' is not supported for the current target |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_require_chir_directory` | expected one chir path to build |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_require_experimental` | compilation with input object files is currently experimental and requires the --experimental flag to be enabled |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_cfg_value_err` | user defined condition should be like: k1=v1,k2=v2 or <directory path of cfg.toml> |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_cfg_toml_content_err` | user defined condition in cfg.toml should be like: k1 = "v1", and each key-value pair occupies a separate line. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `dirver_cfg_invaild_identifier` | user defined condition variable should be a vaild identifier |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `dirver_cfg_same_with_builtin` | user defined condition's key can not be the same with builtin condition |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `dirver_cfg_key_repeat` | user defined condition's key can not repeat |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `dirver_cfg_not_a_dir` | '%s' is not a vaild directory path, please check it or pass the key-value pair like: k1=v1,k2=v2. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_cfg_file_read_failed` | read '%s' failed, due to '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_cfg_path_ignored` | conditional compilation has been set by key-value mode, cfg.toml will be ignored |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_invalid_compile_as_exe` | --compile-as-exe only takes effect when lto mode is enabled |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_invalid_compile_as_exe_platform` | Windows, Mac, IOS does not support --compile-as-exe |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_useless_option` | option '%s' is deprecated and will be removed in the future |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_unsupport_compile_package_with_source_file` | when having --package flag, not support compiling packages with source file '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_unsupport_compile_source_file_with_path` | when compiling source file, not support compiling it with directory '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `driver_deprecated_option` | option '%s' is deprecated and will be removed in future%s. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_pgo_both_gen_and_use` | using both '--pgo-instr-gen' and '--pgo-instr-use' is not allowed. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_ohos_pgo_gen_without_file` | using '--pgo-instr-gen' on ohos targets needs to specify the profile generation path, like '--pgo-instr-gen=/data/storage/el2/base/cjpgo/cj_%m_%p.profraw'. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_pgo_invalid_profile_extension` | Not a .profdata file: '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `not_a_valid_plugin` | '%s' is not a valid compiler plugin to be loaded or executed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `plugin_throws_exception` | exception occurs while executing compiler plugin(s) |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `driver_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `frontend_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `frontend_failed_to_detect_cangjie_home` | failed to detect cangjie home, reason: %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `frontend_failed_to_detect_cangjie_modules` | failed to detect cangjie modules, reason: %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `frontend_invalid_output_path` | can not generate file to path: '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `frontend_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `incremental_compilation_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `cache_search_error` | compilation cache '%s' is missing |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `incremental_compilation_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_diag_begin` | lex_diag_begin |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unknown_start_of_token` | unknown start of token: %s \| unknown start of token: %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unexpected_digit` | unexpected digit '%s' in %s \| unexpected digit \| because %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_cannot_start_with_digit` | cannot start a(n) %s literal with a '%s' digit \| unexpected digit |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_digit` | expected %s digit, found '%s' \| expected %s digit |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unexpected_decimal_point` | unexpected decimal point '.' in %s base number \| unexpected decimal point \| because of this %s prefix |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unexpected_exponent_part` | unexpected exponent part '%s__' in %s \| unexpected exponent part \| because %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_exponent_part` | expected exponent part in hexadecimal float number '%s' \| expected exponent part \| because it is hexadecimal float number |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_identifier_after_dollar` | expected identifier after '$', found %s \| expected identifier after '$' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unrecognized_symbol` | unrecognized symbol '%s' \| unrecognized symbol |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_identifier` | expected identifier, found '%s' \| expected identifier |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_back_quote` | expected '`', found '%s' \| expected '`' \| to match this opening backquote |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_single_line_string` | unterminated single-line string \| unterminated single-line string \| because this interpolation is not terminated with a '}' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_multi_line_string` | unterminated multi-line string \| unterminated multi-line string \| because this interpolation is not terminated with a '}' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_interpolation` | unterminated string interpolation \| unterminated string interpolation \| because it is in single-line string |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_multiline_string_start_from_newline` | multi-line string must start with newline character \| expected to start with newline character \| because it is in multi-line string |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_raw_string` | unterminated raw string \| unterminated raw string \| because it interpolation is not terminated with a '}' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_quote_in_raw_string` | expected '#' or '"' in raw string, found '%s' \| expected '#' or '"' \| because it is raw string prefix |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unrecognized_escape` | unrecognized escape '%s' in %s literal \| unrecognized escape \| because it is in %s literal |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_block_comment` | unterminated block comment \| unterminated block comment |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_left_bracket` | expected '{' in unicode escape, found '%s' \| expected '{' \| because it is in this Unicode escape |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_right_bracket` | expected '}', found '%s' \| expected '}' \| to match this opening '{' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_right_bracket_or_hexadecimal` | expected '}' or hexadecimal digit in unicode escape, found '%s' \| expected '}' or hexadecimal digit \| because it is in this Unicode escape |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_character` | expected a character in rune literal, found '%s' \| expected a character \| because it is in rune literal |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_letter_after_underscore` | this cannot be an identifier \| expected a Unicode XID_Continue after underscore |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_integer_suffix` | illegal integer suffix '%s' \| expected valid integer type suffix \| invalid type suffix |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_float_suffix` | illegal float number suffix '%s' \| expected valid float number type suffix \| invalid type suffix |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_non_decimal_float` | float suffix can only be in decimal \| expected valid digits \| invalid type suffix |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_expected_character_in_char_literal` | expected one character in rune literal \| expected one character in rune literal |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unterminated_char_literal` | unterminated rune literal \| unterminated rune literal |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_characters_overflow` | rune literal may only contain one character \| may only contain one character |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unknown_suffix` | unknown suffix '%s' for number literal \| unknown suffix '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_UTF8_encoding_byte` | illegal byte '%s' in UTF-8 encoding \| illegal byte in UTF-8 encoding |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_unicode` | illegal character:%s \| illegal character:%s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `lex_unsecure_unicode` | unsecure character:%s \| unsecure character:%s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_illegal_uni_character_literal` | illegal unicode scalar value '\u{%s}' \| unicode scalar value must be in range '\u{0000}' to '\u{D7FF}' or '\u{E000}' to '\u{10FFFF}' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_too_many_digits` | %s contains too many digits \| too many digits for \u \| at most 2 digits in an escaped byte character |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_unrecognized_char_in_binary_string` | unrecognized character '%s' in %s \| unrecognized character here \| in %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `lex_diag_end` | lex_diag_end |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `module_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `module_read_file_to_buffer_failed` | read file '%s' failed, reason: '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `module_common_part_path_is_required` | specify common part path of when compiling specific part of package. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `module_read_file_conflicted` | the file '%s' is a duplicate source file |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `module_version_not_identical` | ast file for '%s' created by cjc which version is '%s' and current cjc version is '%s', it may cause crash because lack information of declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `module_open_bcFile_failed` | can't open output file '%s': '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `module_loaded_ast_failed` | validation of %s file '%s' failed, please confirm it was created by compiler whose version is '%s'. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `module_same_name_with_indirect_dependent_pkg` | failed to load dependent package '%s' for package '%s', which have same package name with source package |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `module_unsupport_circular_dependencies` | packages %s are in circular dependencies. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `module_common_cjo_wrong_package` | common part is for another package '%s', expected the same as for current package '%s'. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `module_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `import_package_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_unsupport_save` | unsupported %s '%s' when saving AST |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_unsupported_load` | unsupported %s when loading AST |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_invalid_cjo_dependency` | version of package '%s' and its dependent package '%s' are incompatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_decl_not_find_in_package` | '%s' is not accessible in package '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_search_error` | can not find package '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_import_itself_illegal` | package '%s' should not import itself |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_missed_cjo_main_pkg_part_for_test_pkg` | package '%s' is being compiled with --test-only option, but no dependency for production part of this package provided |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_multiple_package_declarations` | found more than one package declaration for the package |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_name_not_identical_lsp` | package name supposed to be '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_name_inconsistent_with_macro` | package name with macro should be consistent in the same package. |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `package_shadowed_import` | imported decl '%s' is shadowed, it will be ignored by compiler |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `package_conflict_import` | imported decl '%s' is conflicted with other import |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_cannot_export_macro_package` | it is not allowed to re-export a macro package in a package. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_import_inconsistent` | The imported cjo file failed verification, the package name is %s which is not '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_mocking_support_inconsistency` | dependent package '%s' is compiled with mocking support, the current package must be compiled with mocking support too (pass '%s' compilation option) |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_accessibility` | package '%s' is '%s' which cannot be imported by %s package '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_re_export_package_name` | imported package name '%s' cannot be modified by '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `package_root_package_should_be_public` | root package can only modified by 'public' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `packages_visibility_inconsistent` | package must have one visibility level (%s != %s) |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `packages_macro_inconsistent` | package must be either macro or not |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `feature_already_seen_name` | feature is already declared |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `feature_null_declaration` | features declaration must be included in every file within the source set where it is used |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `feature_different_consistency` | feature names must be consistent across all files within a source set |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `import_package_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_name` | expected %s %s, found %s \| expected %s here \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_newline_between_at_and_mc` | unexpected '<NL>' between '@' and the macro invocation '%s' \| unexpected '<NL>' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expect_escape_dollar_token` | expected identifier or '(' after '$' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_varray_type_parameter` | expected type parameters after 'VArray' keyword |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_varray_type_args_mismatch` | expected %s between '<' and '>' of 'VArray' type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expect_integer_literal_varray` | expected an integer literal than or equal to 0 after '$' to specificate the size of 'VArray' type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_varray_with_paren` | expected '(' or '{' after 'VArray' for 'VArray' constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_import` | expected 'import' after module name, found %s \| expected 'import' here \| after module name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_module_name` | expected module name after keyword 'from', found %s \| expected module name here \| after keyword 'from' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_right_delimiter` | unclosed delimiter: '%s' \| expected '%s' here \| to match this opening '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_not_allowed_raw_identifier` | escaped identifier with backticks '%s' is not allowed \| don't use backticks here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_return_type` | there should be no return type in a %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unmatched_right_delimiter` | unmatched delimiter: '%s' \| unmatched delimiter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_literal` | expected literal after '-', found %s \| expected literal here \| after this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_pattern` | expected pattern, found %s \| expected pattern here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_backarrow_in_let_cond` | expected '<-' in %s expression, found %s \| expected '<-' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_left_paren_after` | expected '(' after '%s', found %s \| expected '(' here \| after this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_left_angle_after` | expected '<' after '%s', found %s \| expected '<' here \| after this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_expr_or_decl_in` | expected expression or declaration, found %s \| expected expression or declaration here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_catch_or_finally_in_try` | expected 'catch' or 'finally' after try block, found %s \| expected 'catch' or 'finally' here \| after try block |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_catch_or_handle_or_finally_in_try` | expected 'catch', 'handle' or 'finally' after try block, found %s \| expected 'catch', 'handle' or 'finally' here \| after try block |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_colon_in_catch_pattern` | expected ':' in exception type pattern, found %s \| expected ':' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_colon_in_effect_pattern` | expected ':' in effect type pattern, found %s \| expected ':' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_wildcard_or_exception_pattern` | expected wildcard or exception type pattern, found %s \| expected wildcard or exception type pattern here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_wildcard_or_effect_pattern` | expected wildcard or effect type pattern, found %s \| expected wildcard or effect type pattern here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_double_arrow_in_case` | expected '=>' in case, found %s \| expected '=>' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_selector_or_match_expression_body` | expected '(' or '{' after 'match', found %s \| expected '(' or '{' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_left_brace` | expected '{', found %s \| expected '{' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_left_paren` | expected '(', found %s \| expected '(' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_case` | expected 'case' in match, found %s \| expected 'case' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_line_break` | expected '(' after 'quote', found line break \| unexpected line break here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_paren_or_brace_after_try` | expected '(' or '{' after 'try', found %s \| expected '(' or '{' here \| after this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_assignment` | expected '=', found %s \| expected '=' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_in_forin_expression` | expected 'in' in for-in expression, found %s \| expected 'in' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_while_in_do_while` | expected 'while' in do-while expression, found %s \| expected 'while' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_double_arrow_in_lambda` | expected '=>' in lambda expression, found %s \| expected '=>' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_ccd_in_lambda` | expected one of ',', ':' or '=>', found %s \| expected one of ',', ':' or '=>' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_character` | expected %s, found %s \| expected %s here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_character_after` | expected %s after '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_importing_by_package_name_is_not_supported` | expected '.' \| expected '.' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_decl` | expected declaration, found %s \| expected declaration here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_one_of_identifier_or_pattern` | expected identifier or pattern after '%s', found %s \| expected identifier or pattern here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_get_or_set_in_prop` | expected 'get' or 'set' in prop body, found %s \| expected 'get' or 'set' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_where_brace` | expected '{' or 'where', found %s \| expected '{' or 'where' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_lt_brace` | expected '{' or '<', found %s \| expected '{' or '<' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_lt_paren` | expected '(' or '<', found %s \| expected '(' or '<' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_identifier_lp` | expected ')' or identifier, found %s \| expected ')' or identifier here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_dot_lparen` | expected ',' or ')', found %s \| expected ',' or ')' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_arrow_in_func_type` | expected '->' in function type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_colon_in_range` | unexpected ':' in index access \| unexpected ':' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_lsquare_after` | expected '[' after '%s', found %s \| expected '[' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_type_argument` | expected type argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_parameter_rp` | expected one parameter name or ')', found %s \| expected one parameter name or ')' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_no_newline_after` | expected no new-line character after %s \| expected no new-line character here \| after %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_if_let_andand` | expected '&&', '\|\|', or ')', got 'where' \| did you mean to write '&&' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicated_item` | duplicated %s '%s'%s \| duplicated %s \| previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_nl_warning` | possibly confusing line terminator \|  \| possibly confusing line terminator between '%s' and '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_function_name` | 'main' declaration doesn't need 'func' keyword \|  \| help: try to remove 'func' keyword |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_macro_decl_define_in_macro_package` | macro declaration must be defined in macro package \| expected to be defined in macro package |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_public_before_macro_decl` | macro declaration must be modified with 'public' \| expected 'public' before here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_unexpected_empty_parameter` | unexpected empty parameters in macro declaration \| expected paratmeters here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_expected_right_parameter_nums` | too many parameters in macro declaration \| too many parameters here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_illegal_param_type` | macro declaration's parameter type must be 'Tokens' \| expected 'Tokens' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_illegal_ret_type` | macro declaration's return type must be 'Tokens' \| expected 'Tokens' here, got '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_illegal_named_param` | cannot use named parameter in macro declaration \| unexpected '!' here \| expected '%s : Tokens' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_define_conflicted_with_builtin` | macro declaration name '%s' is conflicted with builtin %s identifier \| unexpected macro identifier here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_macro_call_illegal_with_builtin` | unexpected '[' for builtin macro '%s' \| expected '(' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_declaration_in_scope` | unexpected %s in %s \| unexpected %s \| in %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_const_expected_initializer` | const variable declaration must be initialized \| expected a initializer here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_const_modifier_on_variable` | unexpected modifier 'const' on var or let variable \| unexpected modifier |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_var_must_be_initialized` | variable in top-level scope must be initialized  \| expected '=' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_one_of_type_or_initializer` | variable declaration '%s' needs either type or initializer \| expected ':' or '=' after variable name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_type_or_init_in_pattern` | variable declaration in pattern needs either type or initializer \| expected ':' or '=' after pattern |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_named_parameter_after_unnamed` | unnamed parameters must come before named parameters \| unexpected unnamed parameter here \| because it must come before this named parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_member_parameter_after_regular` | regular parameters must come before member variable parameters \| unexpected parameter here \| because it must come before this member variable parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_decl_cannot_inherit_their_self` | declaration '%s' cannot inherit itself \| illegal super declaration here \| because '%s' cannot inherit itself |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_intrinsic_function_must_be_toplevel` | intrinsic function must be toplevel scope \| intrinsic function must be toplevel scope |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_intrinsic_function_cannot_have_body` | intrinsic function cannot have body \| intrinsic function cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_abstract_func_must_have_return_type` | abstract function must have return type \| abstract function must have return type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicated_get_or_set` | duplicated '%s' in prop \| duplicated '%s' \| previous one |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unknown_enum_constructor` | unknown enum constructor \| unknown enum constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_getter_setter_cannot_be_generic` | '%s' cannot be generic \| unexpected generic here \| in '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_where` | unexpected 'where' in non-generic declaration \| unexpected 'where' here \| because this declaration is non-generic |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_setter_must_contain_one_parameter` | setter must contain 1 parameter \| expected 1 parameter inside |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_setter_can_only_accept_one_parameter` | setter can only accept 1 parameter \| can only accept 1 parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicated_intrinsic_function` | duplicated intrinsic function '%s' \| duplicated intrinsic function \| the previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_missing_body` | body of %s is missing \| missing %s body here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_super_declaration` | cannot inherit from type: '%s' \| this type cannot be inherited |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_static_init_can_not_accept_any_parameter` | static initializer cannot have any parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_finalizer_can_not_accept_any_parameter` | finalizer cannot have any parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_quote_dollar_expr` | invalid expression after the operator '$' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_lambda_expr_in_toplevel` | unexpected lambda expression in top-level scope \| unexpected lambda expression in top-level scope |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_trailing_closure_only_follow_name` | trailing closure can only be used on function calls with function or variable names |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_left_hand_expr` | invalid left-hand expression of assignment '%s' \|  \| cannot assign to this expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_chained_none_associative` | %s operators cannot be chained \| |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicated_step_op` | duplicated step operator ':' on range expression \| redundant operator \| previous one \| on range expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_step_op` | invalid step operator ':' on %s expression \| invalid operator \| on %s expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_expression` | expected expression after %s, found %s \| expected expression here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_incre_expr` | cannot %s a un-assignable expression \|  \| cannot assign to this expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unrecognized_token_after_macro_node` | unrecognized operator %s after declaration \| unrecognized operator \| after declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_operator_or_end` | expected operator or end of expression, found %s \| expected operator or end of expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cannot_have_assi_in_init` | cannot have assignment expression in initializer \| cannot have assignment expression here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_case_body_cannot_be_empty` | match case cannot be empty \| match case cannot be empty |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_redefined_resource_name` | resource name '%s' is already defined \| redefinition of resource name \| previous one |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_newline_not_allowed_between_spawn_and_argument` | unexpected newlines between 'spawn' and the argument followed it |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_no_arguments_in_spawn` | expected no %s in lambda expression of spawn \| cannot contain %s \| in spawn |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_overloaded_operator` | cannot overload operator %s  \| cannot overload this operator |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_empty_string_interpolation` | string interpolation cannot be empty \| empty string interpolation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_invalid_unicode_scalar` | code point '%s' is too large \| unrecognized code point |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_wildcard_can_not_be_used_as_member_name` | wildcard cannot be used as member name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_expected_found` | unexpected %s \| expected %s, found %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cannot_operator_a_tuple` | cannot '%s' a tuple |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_parentheses` | type before arrow of function type should be surrounded by parentheses |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_this_type_not_allow` | 'This' type is not allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_tuple_decl_type` | Legacy tuple type syntax no longer allowed after version 0.28.4 \| use ',' instead |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_type` | expected type name after %s, found %s \| expected type name here \| after %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_newline_not_allowed_between_quest_and_type` | unexpected newlines between '?' and the type followed it |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_redundant_arrow_after_func_type` | redundant '->' after function type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_all_parameters_must_be_named` | in a parameter type list, either all parameters must be named, or none of them; mixed is not allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_only_tuple_and_func_type_allow_type_parameter_name` | unexpected %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_declaration_pattern` | %s patterns cannot be used in class or struct body \|  \| in %s body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_or_pattern` | '\|' is not allowed here \| expected ',' or ')', found '\|' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_tuple_pattern_expected_more_field` | 1-element tuple pattern is not allowed \| 1-element tuple pattern is not allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_type_pattern_in_let_cond` | type pattern is not allowed in %s expression \| type pattern is not allowed here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_macro_decl_in_macro_package` | cannot use 'public' on %s declarations in a macro package \|  \| macro package defined here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_package_as_all` | The alias name should contain '.*' suffix after import-all \| expected '%s*' \| after import-all |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_package_name_length_overflow` | length of package name '%s' overflow \| length overflow |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_package_name_has_backtick` | cannot using '`' in package name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_macro_expand_input_args` | unexpected '[' after '\' for macro argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_macro_expand_attr_args` | unexpected '(' after '\' for macro attribute argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_macro_expand_input_args_without_paren` | unexpected parameters for macro invocation here \| expected declaration like: function, enum, class, interface, variable, property, extend ... |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_macro_expand_input_without_paren_in_paramlist` | unexpected parameters for macro invocation \| expected '(' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_ifavailable_arg_no_name` | @IfAvailable expect an argument name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_ifavailable_not_lambda` | @IfAvailable expect a literal lambda here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_anno_on` | unexpected annotation '%s' on %s \| unexpected annotation here \| on %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_overflow_annotation` | unexpected overflow annotation before '%s' \| unexpected overflow annotation \| before this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unrecognized_expression_in_when` | unrecognized expression '%s' in annotation '@When' \| unrecognized expression here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unrecognized_attr_in_anno` | unrecognized attribute '%s' in annotation '@%s' \| unexpected attribute here |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_empty_attribute` | empty attribute of annotation '@%s' \| empty attribute here |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_duplicated_attr_value` | duplicated attribute value: '%s' \| duplicated value here \| the previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_unsafe_will_be_ignored` | 'unsafe' modifier will be ignored in backend '%s' \| will be ignored in backend '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicated_annotation` | duplicated annotation: '%s' \| duplicated annotation here \| the previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_conflict_annotation` | '%s' and '%s' annotations conflict on %s \| unexpected annotation \| because it is conflicted with this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_fail_expected_annotation` | expected annotation '%s' \| declare annotation before this |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_common_and_specific_in_the_same_file` | 'common' and 'specific' declarations can not be in the same file |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_common_function_must_have_return_type` | 'common' function return type must be specified |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_specific_function_must_have_return_type` | 'specific' function return type must be specified |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_specific_function_parameter_cannot_have_default_value` | 'specific' %s parameter can not have default value |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_specific_member_must_have_implementation` | the member %s must have body in 'specific' %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_type_with_cjmp_var` | '%s' %s type must be specified |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cjmp_outdecl_miss_match` | %s is %s, but %s is not %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cjmp_static_init` | static init can not be '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_common_in_non_common_file` | common declaration must be defined in common package part |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_specific_in_non_specific_file` | specific declaration must be defined in specific package part |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cjmp_generic_decl` | generic declaration can not be '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cjmp_pattern_decl` | %s pattern can not be '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_cjmp_in_common_ctor_required` | at least one constructor is required in common %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_explicitly_abstract_only_for_cjmp_abstract_class` | only common/specific or Native FFI mirror abstract classes can have explicitly abstract %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_illegal_modifier_in_scope` | unexpected modifier '%s' on %s%s \| unexpected modifier \| on %s \| in %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_conflict_modifier` | '%s' and '%s' modifiers conflict on %s \| unexpected modifier \| because it is conflicted with this \| on %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_no_modifier` | expected no modifier before %s, found '%s' \| expected no modifier here \| before %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicate_modifier` | duplicated modifier: '%s' \| duplicated modifier \| previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_duplicate_type_parameter_name` | duplicated type parameter name: '%s' \| duplicated type parameter name \| previous one is here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_unexpected_type_in` | unexpected type in '%s' \| unexpected type here \| in %s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_redundant_modifier` | redundant modifier: '%s' \| %s implies '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_variable_length_parameter_can_not_be_first` | variable length parameter can not be the first parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_variable_length_parameter_must_in_the_end` | variable length parameter must in the end of the parameter list |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_variable_length_parameter_only_in_the_foreign_function` | variable length parameter can only show in the foreign function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_foreign_func_should_not_be_generic` | foreign function should not be generic function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_foreign_func_must_declare_return_type` | foreign function must declare its return type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_foreign_function_with_body` | foreign function can not have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_expected_static_for_const_member_var` | expected static before const member variable \| const member variable must be modified by static |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_wrong_argument` | argument '%s' of @Deprecated should be %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_argument_duplication` | argument '%s' of @Deprecated can not be duplicated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_arguments_must_be_lit_const_expr` | argument of @Deprecated is not string literal or boolean value. Variables not allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_empty_string_argument` | argument '%s' of @Deprecated must not be empty string |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_unknown_argument` | unknown argument '%s' in @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_deprecated_invalid_target` | %s can not be target of @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_annotation_max_one_argument` | %s requires zero or one%s argument \| expected %s here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_annotation_one_argument` | %s requires exactly one%s argument \| expected %s here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_annotation_no_arguments` | %s accepts no arguments \| unexpected argument(s) |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_foreign_name_on_ffi_decl_member` | @ForeignName could only be used on FFI declaration member |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_function_cannot_have_body` | java-mirrored function '%s' cannot have body \| java-mirrored function cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_function_must_have_return_type` | java-mirrored function must have return type \| java-mirrored function must have return type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_prop_cannot_have_setter` | java-mirrored property cannot have setter \| java-mirrored property cannot have setter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_prop_cannot_have_getter` | java-mirrored property cannot have getter \| java-mirrored property cannot have getter |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_java_mirror_prop_is_deprecated` | java-mirrored property is deprecated, use field instead |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_decl_cannot_have_primary_ctor` | java-mirrored declaration cannot have primary constructor \| java-mirrored declaration cannot have primary constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_constructor_cannot_have_body` | java-mirrored constructor cannot have body \| java-mirrored constructor cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_have_private_member` | java-mirrored declaration cannot have private member |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_have_static_init` | java-mirrored declaration cannot have static initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_have_finalizer` | java-mirrored declaration cannot have finalizer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_have_const_member` | java-mirrored declaration cannot have const member |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_be_sealed` | @JavaMirror declaration cannot be sealed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_be_generic` | @JavaImpl declaration cannot be generic |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_be_abstract` | @JavaImpl declaration cannot be abstract |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_be_sealed` | @JavaImpl declaration cannot be sealed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_have_static_init` | @JavaImpl declaration cannot have static initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_mirror_cannot_have_open_prop` | java-mirrored declaration cannot have open property |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_be_open` | @JavaImpl class cannot be open |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_java_impl_cannot_be_interface` | interface cannot be @JavaImpl |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_have_primary_ctor` | @ObjCMirror declaration cannot have primary constructor \| @ObjCMirror declaration cannot have primary constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_ctor_cannot_have_body` | @ObjCMirror declaration constructor cannot have body \| @ObjCMirror declaration constructor cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_method_cannot_have_body` | @ObjCMirror declaration method '%s' cannot have body \| @ObjCMirror declaration method cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_method_must_have_return_type` | @ObjCMirror declaration method must have return type \| @ObjCMirror declaration method must have return type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_be_sealed` | @ObjCMirror declaration cannot be sealed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_have_private_member` | @ObjCMirror declaration cannot have private member |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_have_static_init` | @ObjCMirror declaration cannot have static initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_have_finalizer` | @ObjCMirror declaration cannot have finalizer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_cannot_have_const_member` | @ObjCMirror declaration cannot have const member |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_be_generic` | @ObjCImpl declaration cannot be generic |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_be_abstract` | @ObjCImpl declaration cannot be abstract |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_be_sealed` | @ObjCImpl declaration cannot be sealed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_have_static_init` | @ObjCImpl declaration cannot have static initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_be_open` | @ObjCImpl class cannot be open |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_impl_cannot_be_interface` | interface cannot be @ObjCImpl |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_field_cannot_have_initializer` | @ObjCMirror declaration field cannot have initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_field_cannot_be_static` | @ObjCMirror declaration field cannot be 'static' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_prop_cannot_have_getter` | @ObjCMirror declaration property '%s' cannot have getter \| @ObjCMirror declaration property cannot have getter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_prop_cannot_have_setter` | @ObjCMirror declaration property '%s' cannot have setter \| @ObjCMirror declaration property cannot have setter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_cannot_be_foreign` | @ObjCMirror top-level function '%s' cannot be foreign |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_cannot_be_c` | @ObjCMirror top-level function '%s' cannot be @C |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_cannot_be_generic` | @ObjCMirror top-level function '%s' cannot be generic |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_cannot_have_body` | @ObjCMirror top-level function '%s' cannot have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_must_have_explicit_type` | @ObjCMirror top-level function '%s' must have result type explicitly specified |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_cannot_be_const` | @ObjCMirror top-level function '%s' cannot be const |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_mirror_func_must_be_top_level` | @ObjCMirror function '%s' can only be declared on top-level |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_init_method_must_be_static` | @ObjCInit method must be modified with 'static' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_init_method_must_be_in_mirror_class` | @ObjCInit method must be declared within '@ObjCMirror' class |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_optional_method_must_be_in_mirror_class` | @ObjCOptional method must be declared within '@ObjCMirror' interface |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_objc_interop_not_supported` | Objective-C interoperability feature '%s' is not yet supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_diag_error` | %s \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `parse_diag_warning` | %s \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_expected_position_compare_operator` | expected '= or < or <=' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_expected_hash_id_compare_operator` | expected '=' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_invalid_query_value` | should be identifer, positive integer, 'foo*' or '*foo' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_expected_query_symbol` | expected identifier or '( # _' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_expected_logic_symbol` | expected '&&' or '\|\|', but got '%s' here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_position_illegal_file_id` | the first element of position tuple should be file id |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_position_illegal_line_num` | the second element of position tuple should be line number |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_position_illegal_column_num` | the third element of position tuple should be column number |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_position_comma_required` | comma required here to seperate position tuple |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_hashid_illegal_file_hash` | the first element of hash id should be file hash |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_hashid_illegal_fieldid` | the second element of hash id should be field id |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `searcher_past_the_end_of_array` | Searcher error: id number '%s' past the end of array and it would be ignored. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `searcher_empty_number` | Searcher error: number empty. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `searcher_invalid_number` | Searcher error: number '%s' must be positive integer. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `searcher_invalid_scope_name` | Searcher error: scope name doesn't support suffix search *'%s'. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `parse_query_diag_end` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_diag_begin` |  |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_node_after_check` | semantic error |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unable_to_infer_decl` | unable to infer declaration type, please add type annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mismatched_types` | mismatched types \| expected '%s', found '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mismatched_types_multiple_assign` | mismatched types \| the expression has type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mismatched_types_because` | mismatched types \| expected '%s', found '%s' \| expected '%s' because of %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ambiguous_use` | ambiguous use of '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_undeclared_identifier` | undeclared identifier '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_undefined_variable` | variable '%s' is used before being defined |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_redefinition` | redefinition of declaration '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_conflict_with_sub_package` | top-level declaration '%s' is conflicted with possible sub-package '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_core_object_not_found_when_no_prelude` | class 'Object' of package 'std/core' is not found, cannot use '--no-prelude' option |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_accessibility_with_main_hint` | '%s' declaration uses %s types \| %s '%s' contains %s type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_accessibility` | '%s' declaration uses %s types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_param_miss_match` | mismatched number of parameters \| expected '%s', found '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unable_to_infer_return_type` | unable to infer return type, please add type annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unable_to_infer_generic_func` | unable to infer generic argument of this function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_called_object` | called object is not a function or constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_return` | 'return' must be used inside a function body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_return_in_static_init` | 'return' cannot be used inside the static initializer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_wrong_number_of_arguments` | %s for parameter list '%s' in call \| expected %s, found %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unordered_arguments` | positional argument cannot appear after named argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_param_named_mismatched` | parameter name mismatched |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_need_named_argument` | missing argument prefix %s for named parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_subscript_assign_parameter` | overloaded operator '[]' can only have one named parameter 'value' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_subscript_assign_parameter_num` | overloaded operator '[]' should have at least one positional parameter for index |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_subscript_assign_return` | the return type of subscript assignment must be 'Unit' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_overload_conflicts` | %s '%s' has overload conflicts |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_static_function_overload_conflicts` | overloaded functions '%s' cannot mix static and non-static |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_use_mutable_func_alone` | mutable function '%s' cannot be used alone as reference |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unsafe_func_can_only_be_called` | the unsafe function can only be called rather than as name reference |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ambiguous_match_primitive_extend` | ambiguous match for function call '%s' of these extended type: %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_immutable_access_mutable_func` | cannot use mutable function on immutable value \| is immutable |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_recursive_constructor_call` | recursive constructor calling detected |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_have_default_param` | optional parameter cannot be used in %s function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_trailing_lambda_cannot_used_for_non_function` | trailing lambda cannot be used for %s \| declaration type of parameter: '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unable_to_infer_expr` | unable to infer the type of this expression, please add type annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_exceed_num_value_range` | the number '%s' exceeds the value range of type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_exceed_float_literal_range` | the number '%s' exceeds the value range of floating-point literal |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_float_literal_too_large` | magnitude of floating-point literal too large for type '%s', maximum is %s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_float_literal_too_small` | magnitude of floating-point literal too small for type '%s', minimum is %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_unary_expr` | invalid unary operator '%s' on type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_unary_expr_with_target` | invalid unary operator '%s' on type '%s' with return type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_binary_expr` | invalid binary operator '%s' on type '%s' and '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_subscript_expr` | invalid subscript operator [] on type '%s' with index %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_assign_to_subscript` | cannot assign to this subscript expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_not_member_of` | '%s' is not a member of %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_member_not_imported` | '%s' is not imported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_assign_to_immutable` | cannot assign to immutable value |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unqualified_left_value_assigned` | '%s' can not be assigned |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_not_found_from_generic_upper_bounds` | '%s' is not found for generic type '%s' in its upper bounds |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_different_or_pattern` | patterns connected by '\|' should be of the same kind \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_var_in_or_pattern` | cannot introduce variables in patterns connected by '\|' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_var_in_or_condition` | cannot introduce variables in conditions connected by '\|\|' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_nonexhuastive_patterns` | non-exhaustive patterns \| the selector is of type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_unreachable_pattern` | unreachable pattern |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_lambdaExpr_must_have_type_annotation` | parameters of this lambda expression must have type annotations |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_use_func_capture_var_alone` | %s capturing mutable variables needs to be called directly |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_enum_constructor_with_param_must_have_args` | enum constructor '%s' must be used with arguments |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_optional_chain_non_optional` | cannot use optional chaining \| cannot use optional chaining on non-optional value of type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_capture_before_initialization` | cannot capture variable '%s' before initialization |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_interpolation_in_const_pattern` | cannot use string interpolation in constant pattern |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_ref_to_pkg_name` | package name cannot be referred independently |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_use_expr_without_import` | import '%s' to use the '%s' expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_func_without_type_arg` | type arguments needed for the generic function%s \| cannot infer type arguments for the generic function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_type_inconsistent` | generic types substitutions are inconsistent for '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_argument_no_match` | type argument's number does not match type parameter's number |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_constraint_not_looser` | the constraint of type parameter is not looser than parent's constraint |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_instantiation_causes_ambiguous_functions` | generic instantiation '%s' causes ambiguous function '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_multiple_class_upperbounds` | generic parameter '%s' cannot have two or more class upper bounds '%s' without subtype relation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_param_exist_in_class_irrelevant_upperbound_recursively` | generic parameter '%s' cannot be used in class irrelevant upper bounds '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_param_directly_recursive` | generic parameter '%s' is bounded directly recursively with '%s' which is forbidden |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_upper_bound_must_be_class_or_interface` | the upper bound '%s' of generic parameter '%s' must be class or interface |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_member_kind_inconsistent` | %s member '%s' cannot have the same name with %s member in %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_super_member_kind_inconsistent` | inherited members '%s' have inconsistent decl types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_member_type_inconsistent` | %s of the inherited %s members '%s' are not identical and not in subtype relation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_abstract_class_static_unimplement_func` | abstract class '%s' cannot contain unimplemented static %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_override` | cannot override %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_member_visibility_in_class` | the visibility of an '%s' %s must be 'public' or 'protected' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_weak_visibility` | a deriving member must be at least as visible as its base member \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_inherit_sealed` | cannot %s %s 'sealed' %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_thread_context_invalid` | user defined decl '%s' not support to inherit, implement or extend 'ThreadContext' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_thread_context_not_open` | '%s' cannot be modified with 'open' when inherit, implement or extend 'ThreadContext' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inherit_not_return_this` | an open function that returns 'This' must keep the return type 'This' when overridden |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_return_type_incompatible` | return type of '%s' is not identical or not a subtype of the overridden/redefined/implement function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_spawn_arg_invalid` | invalid argument of spawn expr, user-defined `ThreadContext` types are prohibited now |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_spawn_arg_no_effect` | argument of spawn expr does not take effect at current backend |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_interface_call_with_unimplemented_call` | static invocation contains unimplemented static %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_type_uninitialized_static_field` | the static member variable '%s' is not initialized |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_instance_func_cannot_be_used_in_finalizer` | instance %s cannot be used in the finalizer |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_no_non_param_constructor_in_super_class` | there is no non-parameter constructor in super class, please invoke super call explicitly |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_non_abstract_class_cannot_be_sealed` | non-abstract class cannot be modified by 'sealed' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_static_variable_use_generic_parameter` | static member cannot depend on generic parameter '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cstruct_cannot_impl_interfaces` | struct with @C cannot implement interfaces |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_class_need_abstract_modifier_or_func_need_impl` | class '%s' missing abstract modifier, otherwise abstract function or property should be implemented |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_need_member_implementation` | implementation of function or property is needed in '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_export_same_private_decl` | currently, it is not possible to export two private declarations with the same name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_function_cannot_overridden` | cannot override %s '%s' in extend of supertype |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_member_cannot_shadow` | extend member '%s' is not allowed to shadow members of '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_illegal_extended_type` | extending type '%s' is not allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_generic_must_be_used` | type parameter%s must be used in extended type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_duplicate_interface` | interface '%s' has been implemented by '%s', please remove it |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_not_interface` | expected an interface, found non-interface type \| expected an interface here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_illegal_member` | illegal extend member, only functions, props, associated types are allowed |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_use_super` | 'super' is not allowed inside an extend declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_type_cannot_extend_imported_interface` | %s type '%s' cannot extend imported interface |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_c_type_cannot_extend_interface` | c type '%s' cannot support interface extend |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_immutable_type_extend_assignment_index_operator` | it's illegal to extend index assignment operator '[](index, value)' for immutable type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_immutable_type_illegal_property` | there cannot have mutable property in immutable type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_interface_is_not_extendable` | interface '%s' is not able to be extended |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_mut_modifier_extend_of_struct` | 'mut' modifier is illegal in extend body of '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_check_sequence_cannot_decide` | unable to decide which extension happens first |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_export_extend_depend_non_export_extend` | exported extension cannot indirectly export the functions '%s' of the non-exported extension |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_property_must_have_accessors` | property must have accessors |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_immutable_property_with_setter` | immutable property cannot have setter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_property_have_same_declaration_in_inherit_mut` | property '%s' should have 'mut' modifier |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_property_have_same_declaration_in_inherit_immut` | property '%s' should be immutable |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_property_must_implement_both` | property must implement both getter/setter of interface property '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_expect_const` | expected 'const' %s \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_define_var_in_const_funciton` | cannot define 'var' variable in 'const' function |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_no_const_init` | cannot define 'const' member function without 'const' constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_class_const_init_with_var` | cannot define 'const' constructor with 'var' members in class |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_no_const_init` | class with '@Annotation' should have 'const' constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_arg_target` | '@Annotation' can only have one named argument 'target' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_arg_target_array_lit` | the argument of '@Annotation' should be array literal |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_annotation_non_public` | '@Annotation' modifying non-'public' class is invisible at runtime |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_custom_place` | cannot use custom annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_modify_cstring_or_zerosized` | the expression qualified by 'inout' cannot be of %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_modify_non_ctype` | the type of experssion qualified by 'inout' must meet 'CType' constraint |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_must_be_var_variable` | 'inout' can only qualify variable defined with 'var' \| %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_modify_heap_variable` | the variable qualified by 'inout' cannot be directly or indirectly derived from an instance of a 'class' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_can_only_used_in_cfunc_calling` | 'inout' can only be used in a 'CFunc' calling |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_inout_mismatch` | mismatch 'inout' of function argument with type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_error_arg_num` | '%s' should have %s arg |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_error_arg_range` | '%s' only supports %s as arg |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_error_object` | '%s' can only modify %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_incorrect_use_between_types` | type annotated with '@Java["ext"]' can only be used within the declaration which has '@Java["ext"]'  \| annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_non_jtype` | %s type in %s '%s' with '@Java' must meet JType constraint |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_invalid_unit` | %s type in %s '%s' with '@Java' can not be 'Unit' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_app_inherit_ext` | only types annotated with '@Java["ext"] can %s from a type annotated with '@Java["ext"]' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_unsupported_decl` | %s is not supported in %s '%s' annotated with '@Java' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_missing_java_interop_annotation` | %s '%s' should have '@Java' annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_static_access` | cannot access static member with generic parameter in '@Java' types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_primitive_type_as_generics_arg` | only reference types are available for '@Java' generics |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_meet_constraint_indirectly` | types that meet constraints by 'extend' cannot be used in '@Java' generics |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_static_member_in_interface_must_has_body` | static functions in '@Java'-annotated interfaces must have a body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_a_java_type` | types annotated with '@Java' cannot be extended |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_upper_bounds_must_be_java_in_java` | generic type's upper bound in types annotated with '@Java' should be annotated with '@Java' too |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_define_java_annotation` | types annotated with '@Java' cannot be annotated with '@Annotation' together |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_use_of_java_annotation` | imported Java annotations can only be used with types annotated with '@Java' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_use_of_annotation_jffi` | only imported Java annotations can be used with types annotated with '@Java' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_annotation_not_applicable_jffi` | '@%s' not applicable to %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cannot_use_annotation_jffi` | cannot use annotation here |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_shadow_cannot_in_type_args` | '%s' is not allowd to be used here as type argument, because it shadows field '%s' with its super type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_unsupported_type_argument_in_java_interop` | type argument in java interoperation should meet 'JType' constraint |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmapping_struct_generic_not_supported` | cangjie mirror struct type generic %s is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmapping_struct_inheritance_interface_not_supported` | cangjie mirror struct type inheritance interface is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmapping_decl_not_supported` | cangjie mirror decl type is not supported for %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmapping_method_arg_not_supported` | argument type of cangjie mirror decl type member function is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmapping_method_ret_unsupported` | return type '%s' of function inside %s type is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cj_mapping_generic_method_not_get_instance_config` | Instance configuration '%s' has incorrect format. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_varray_size_match` | mismatch 'VArray' type's size \| expected size is %s, found %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_varray_args_number_mismatch` | 'VArray' constructor accepts only one argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_varray_subscript_num` | 'VArray' accepts exactly one subscript index with type of 'Int64' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_varray_in_cfunc` | return type of CFunc cannot be 'VArray' type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_varray_arg_type_with_reftype` | '%s' directly or indirectly contains an unsupported type \| contain unsupported instance member variable with type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_invalid_cfunc_return_type` | return type of CFunc must be instantiated with CType |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_disabled` | mocking features are disabled, you can enable them by passing %s compilation option explicitly, or using default  \| mode |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_not_in_test_mode` | mocking features can be used only in the test mode, please pass %s compilation option to compile the package in  \| the test mode |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_unsupported_type` | only mocking of classes or interfaces is supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_wrong_static_decl` | static/top-level declaration to mock shouldn't be private, local, constant or constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_doesnt_support_mocking` | '%s' doesn't support mocking, please be sure that its package '%s' is mock-compatible (was compiled with %s  \| compilation option) |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_frozen_unsupported` | mocking of frozen declarations (marked with @Frozen annotation) are not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mock_frozen_required` | generic wrapper function '%s' for createMock/createSpy calls should be marked with @Frozen annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_command_handle_type_error` | the command handle type must implement 'effect.Command<T>' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_resumption_handle_type_error` | the type of the resumption must extend 'effect.Resumption' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_resumption_incorrect_return_type` | the return type of the resumption ('%s') does not match the type of the try block ('%s') |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_command_resumption_mismatch` | the parameter type of the resumption ('%s') does not match the result type of the command ('%s') |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_implicit_resume_outside_handler` | 'resume' outside of an immediate handler must have a resumption argument |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_resume_no_with` | a resumption of non-Unit type '%s' must have a 'with' or 'throwing' clause |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_resume_wrong_resumption_type` | resumptions must be of type 'core.Resumption<T>', but actual type is '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_mismatching_handle_block` | The type of this handle block is '%s', which mismatches the smallest common supertype '%s' of previous branches. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_return_in_try_handle_block` | Return statements are not allowed within try/handle blocks |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_command_incompatible_type` | type '%s' does not implement compatible instantiations of 'Command<T>' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_resume_throwing_mismatch_type` | the type of the `resume throwing` must be a subtype of core.Exception or core.Error |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_useless_command_type` | useless command type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_deprecated_error` | %s '%s' is deprecated%s%s \| deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_deprecated_warning` | %s '%s' is deprecated%s%s \| deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_deprecation_weakening` | strictness of @Deprecated can not be weaken on inheritors |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_deprecation_override_error` | overridden %s '%s' should be marked with @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_deprecation_override_warning` | overridden %s '%s' should be marked with @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_deprecation_redef_error` | redefined %s '%s' should be marked with @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_deprecation_redef_warning` | redefined %s '%s' should be marked with @Deprecated |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_open_class_no_init` | please implement the constructor explicitly for common open class '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_multiple_common_implementations` | 'common' %s has several specific implementations |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_direct_extension_has_duplicate_private_members` | declaration 'common' extend '%s' has a conflicting private %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_direct_extension_has_common_private_members` | 'common' and 'private' modifier conflict on %s '%s' declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_not_matched` | '%s' %s can not find '%s' match |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_var_not_match_let` | 'specific' '%s' can not match 'common' '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_init_common_primary_constructor` | 'specific' init can not be used to implement primary 'common' constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `specific_has_different_kind` | 'specific' decl kind(%s) is not equal to 'common'(%s) |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_primary_unmatched_var_decl` | parameter in 'specific' primary constructor must also be a member variable declaration  \| if it's a member variable declaration in 'common' primary constructor |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `common_non_exaustive_platfrom_exaustive_mismatch` | exhaustive 'common' %s cannot be matched with non-exhaustive 'specific' %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_different_type` | 'specific' %s type is not equal to 'common' type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_member_must_have_implementation` | the member %s must have body in 'specific' %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_different_modifier` | 'specific' %s modifier is not match 'common' modifier |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_different_annotation` | 'specific' %s annotation is not match 'common' annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_deprecated_annotation` | '%s' annotation is not allowed on specific %s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmp_parameter_default_value_both_sides` | parameter default value should be on either 'common' or 'specific' side, not both |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_different_parameter` | 'specific' function parameter is not match 'common' parameter |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_different_super_type` | 'specific' %s super types is not match 'common' super types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_specific_has_duplicate_extensions` | declaration 'specific' extend '%s' has a conflicting extension |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_package_has_main` | main function cannot be used in common package part |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_static_let_cant_be_initialized_in_static_init` | 'common' static let '%s' can not be initialized in static init |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_assign_to_common_immutable_in_ctor` | cannot assign to immutable variable '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmp_abstract_class_member_has_no_explicit_modifier` | '%s' abstract class %s must have explicit '%s' or 'abstract' modifier |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_explicitly_abstract_can_not_have_body` | abstract %s can not have body |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_explicitly_abstract_only_for_cjmp_abstract_class` | only common/specific class can have explicitly abstract %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_open_abstract_specific_can_not_replace_open_common` | open common %s can not be overridden with abstract specific %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_cjmp_non_specific_abstract_member_in_specific_class` | specific abstract class '%s' cannot have non-specific abstract %s |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_generic_frozen_not_supported` | common/specific declaration %s with generics cannot be @Frozen |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_generic_rename_not_supported` | common/specific generic rename is not supported yet |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_common_specific_annotation_not_allowed` | annotation %s is not allowed on a common/specific declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_ctor_arg_must_be_java_mirror` | argument type of java-mirrored constructor must be of @JavaMirror type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_method_arg_must_be_java_mirror` | argument type of java-mirrored function must be of @JavaMirror type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_method_ret_unsupported` | return type '%s' of function inside %s class is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_prop_must_be_java_mirror` | property of java-mirrored declaration must be of @JavaMirror type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_subtype_must_be_annotated` | super declaration '%s' is inheritable only for declaration annotated with @JavaMirror or @JavaImpl |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_cannot_inherit_pure_cangjie_type` | @JavaMirror-annotated declaration cannot inherit pure cangjie type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_impl_cannot_inherit_pure_cangjie_type` | @JavaImpl-annotated declaration cannot inherit pure cangjie type |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_subtype_anno_must_inherit_mirror` | @JavaImpl-annotated declaration must inherit @JavaMirror-annotated declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_cannot_be_extended_with_interface` | @JavaMirror class cannot be extended with interface |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_impl_cannot_be_extended_with_interface` | @JavaImpl class cannot be extended with interface |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_impl_redefinition` | redefinition of java declaration '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_mirror_interoplib_must_be_imported` | interoplib.interop must be imported to use java interoperability |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_interop_not_supported` | Java interoperability feature '%s' is not yet supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_extend_ref_target_cannot_be_java_impl` | extend declaration ref target cannot be java impl |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_variable_of_java_type` | %s can not store objects of java interoperability type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_generic_parameter_of_java_type` | Can not instantiate generic '%s' with java interoperability type '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_java_interoplib_version_too_old` | java interoplib.interop library's version is too old. Compiler was built expecting versoin '%s'.  \| Compatibility problems could happen. Use it at your own risk |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_java_interoplib_version_mismatch` | java interoplib.interop library's version is '%s', but compiler was built expecting version '%s'.  \| Compatibility problems could happen. Use it at your own risk |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_has_default_annotation_args` | '@JavaHasDefault' can't have arguments |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_has_default_annotation_is_in_wrong_place` | '@JavaHasDefault' can be used only on @JavaMirror interface methods. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_java_has_default_conflict_with_static` | Illegal combination of '@JavaHasDefault' annotation and 'static' modifier. |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_ctor_param_must_be_objc_compatible` | param type of %s constructor must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_method_param_must_be_objc_compatible` | param type of %s method must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_method_ret_must_be_objc_compatible` | return type of %s method must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_prop_must_be_objc_compatible` | %s property type must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_field_must_be_objc_compatible` | %s field type must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_decl_cannot_inherit` | Objective-C mirror cannot inherit other supertypes |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_subtype_cannot_multiple_inherit` | Objective-C mirror subtype cannot inherit multiple types (only 1 interface or 1 class is allowed) |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_subtype_must_be_annotated` | Objective-C mirror subtype must be annotated with @ObjCMirror or @ObjCImpl |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_subtype_must_inherit_mirror` | @ObjCImpl declaration must inherit @ObjCMirror |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_must_inherit_mirror` | @ObjCMirror declaration cannot inherit not @ObjCMirror declarations |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_mirror_interoplib_must_be_imported` | interoplib.objc must be imported to use Objective-C interoperability |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_not_supported` | Objective-C interoperability feature '%s' is not yet supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_pointer_argument_must_be_objc_compatible` | ObjCPointer can only be used with Objective-C compatible types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_toplevel_param_must_be_objc_compatible` | param type of Objective-C mirror top-level function '%s' must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_interop_toplevel_ret_must_be_objc_compatible` | return type of Objective-C mirror top-level function '%s' must be Objective-C compatible |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_method_must_have_foreign_name` | %s declaration method '%s' with more than one parameter must have @ForeignName annotation \| %s declaration method with more than one parameter must have must have @ForeignName annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_ctor_must_have_foreign_name` | %s declaration constructor with more than one parameter must have @ForeignName annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_func_argument_must_be_objc_compatible` | %s can only be used with function type over Objective-C compatible types |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_func_call_property_can_only_be_called` | %s property 'call' can only be called directly, no other operations are permitted |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_impl_must_have_objc_mirror_super_class` | @ObjCImpl class must have @ObjCMirror super class |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_setter_name_on_immutable_prop` | @ForeignSetterName cannot be specified on immutable property |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_cjmapping_inheritance_interface_not_supported` | cangjie mirror decl type inheritance interface is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_objc_cjmapping_generic_not_supported` | cangjie mirror decl type generic %s is not supported |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_foreign_name_appeared_in_child` | @%s could not appear on overridden declaration |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_foreign_name_conflicting_annotation` | Declaration '%s' has a conflicting @%s annotation |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_foreign_name_conflicting_derived_annotation` | Declaration '%s' has a conflicting derived @%s '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ifavailable_arg_no_name` | the first argument of @IfAvailable expression must have a name |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ifavailable_arg_not_literal` | the first argument of @IfAvailable expression must be a literal expression |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ifavailable_unknow_arg_name` | unknown parameter name '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_apilevel_multi_anno` | annotate more than one '@!APILevel' |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_apilevel_missing_arg` | annotation missing named argument '%s' or unable to read as numerical value |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_only_literal_support` | only %s literal values are supported for now |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_apilevel_ref_higher` | cannot reference '%s'(level: %s) which higher than level of the current scope(level: %s) |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_apilevel_syscap_warning` | inappropriate syscap '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_apilevel_syscap_error` | inappropriate syscap '%s' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_apilevel_multi_diff_syscap` | declaration mark with different syscap |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_ifavailable_level_limit` | `@IfAvaliable` feature is not avaliable in device where the APILevel is less than 19 due to missing capatability  \| in ROM |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_hide_multi_annotation` | cannot be annotated with '@!Hide' more than once |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_hide_at_func_param` | function parameter cannot be annotated with '@!Hide' |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_hide_missing_hide` | should be marked with '@!Hide' to be hidden |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_hide_compile_time_invisible` | 'Hide' annotation must be visible at compile time |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_hide_diff_param` | the parameter 'isChecked' of '@!Hide' is %s |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_hide_must_at_end` | annotation '%s' must be placed below all macros and annotations |
| Basic/DiagRefactor/DiagnosticAll.def | WARNING | `sema_unused_import` | unused import '%s' \| unused import |
| Basic/DiagRefactor/DiagnosticAll.def | ERROR | `sema_diag_end` |  |

## 6. 使用方式

- 在本项目新增或调整诊断时，先以官方定义名和消息作为候选，再用最小仓颉程序经 `cjc` 验证实际触发行为和位置。
- CFIR 的诊断实现、映射和测试期望是独立维护对象；不得将“目录中存在”解释为“本项目已支持”。
- 需要更新快照时，记录上游修订来源和提取命令，并同时复核本节的统计值。

相关入口：[测试约定](../TESTING_CONVENTIONS.md)、[CFIR 诊断框架](../cfir/checkers/README.md)、[官方仓颉编译器](https://gitcode.com/Cangjie/cangjie_compiler)。
