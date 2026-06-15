$remaining = @(
    "sema_abstract_method_cannot_be_accessed_directly",
    "sema_assignment_of_member_variable_cannot_use_this_or_super, sema_capture_has_shadow_variable",
    "sema_extend_duplicate_interface",
    "sema_func_capture_var_cannot_expr",
    "sema_generic_param_exist_in_class_irrelevant_upperbound_recursively",
    "sema_ignore_open",
    "sema_illegal_member_used_in_open_constructor",
    "sema_illegal_place_of_calling_this_primary_constructor",
    "sema_immutable_type_illegal_property",
    "sema_incompatible_mut_modifier_between_struct_and_interface",
    "sema_invalid_assignment_to_this_expr",
    "sema_invalid_mut_modifier_extend_of_struct",
    "sema_invalid_unary_expr",
    "sema_missing_redefined_func",
    "sema_multiple_primary_constructors",
    "sema_no_match_constructor",
    "sema_no_non_param_constructor_in_super_class",
    "sema_property_have_same_declaration_in_inherit_immut",
    "sema_property_have_same_declaration_in_inherit_mut",
    "sema_redefinition",
    "sema_tuple_element_cmp_not_bool",
    "sema_unreachable_pattern",
    "sema_unused_import",
    "sema_use_this_as_an_expression_in_func"
)

foreach ($old in $remaining) {
    $files = Get-ChildItem -Path "cfir\analysis-tests\testData\llt" -Recurse -Filter "*.cj" | Select-String -Pattern [regex]::Escape("<!$old!>") -SimpleMatch -List
    if ($files) {
        Write-Output "$old`t=> $($files.Count) file(s)"
    }
}
