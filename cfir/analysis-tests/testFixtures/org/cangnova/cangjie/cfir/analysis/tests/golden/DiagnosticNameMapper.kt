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
        // TypeCheck
        "TYPE_MISMATCH" to "sema_mismatched_types",
        "RETURN_TYPE_MISMATCH" to "sema_mismatched_types",
        "ARGUMENT_TYPE_MISMATCH" to "sema_mismatched_types",
        "ASSIGNMENT_TYPE_MISMATCH" to "sema_mismatched_types",
        // Resolve
        "NO_CONSTRUCTOR" to "sema_no_matching_constructor",
        "UNRESOLVED_REFERENCE" to "sema_undeclared_identifier",
        "INVISIBLE_MEMBER" to "sema_no_matching_function",
        "INVISIBLE_REFERENCE" to "sema_undeclared_identifier",
        "CLASS_NOT_OPEN_FOR_INHERITANCE" to "sema_class_not_open_for_inheritance",
        "EXPLICIT_SUPER_CALL_REQUIRED" to "sema_explicit_super_call_required",
        // Imports
        "UNRESOLVED_IMPORT" to "package_import_not_found",
        "IMPORT_CONFLICT" to "package_conflict_import",
        // Throw/Catch
        "THROW_EXPR_WITH_WRONG_TYPE" to "sema_throw_expr_with_wrong_type",
        "CATCH_TYPE_MUST_EXTEND_EXCEPTION" to "sema_except_catch_type_error",
        // DeclarationStatus
        "STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE" to "sema_static_cannot_be_open_abstract_override",
        "MUT_ONLY_ON_FUNCTION" to "sema_mut_only_on_function",
        "NOTHING_TO_OVERRIDE" to "sema_nothing_to_override",
        "CLASSIFIER_REDECLARATION" to "sema_redeclaration",
        // Inheritance
        "INTERFACE_CANNOT_INHERIT_CLASS" to "sema_interface_cannot_inherit_class",
        "MULTIPLE_CLASS_SUPERS" to "sema_multiple_class_inheritance",
        "OVERRIDE_RETURN_TYPE_MISMATCH" to "sema_mismatched_types",
        "INCOMPATIBLE_MODIFIERS" to "sema_incompatible_modifiers",
        "WRONG_MODIFIER_TARGET" to "sema_wrong_modifier_target",
        "OVERRIDE_STATIC_ERROR" to "sema_override_static_error",
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
