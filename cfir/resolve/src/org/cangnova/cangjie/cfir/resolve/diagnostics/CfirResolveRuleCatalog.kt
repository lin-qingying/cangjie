package org.cangnova.cangjie.cfir.resolve.diagnostics

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase

data class CfirResolveRuleDefinition(
    val id: String,
    val phase: CfirResolvePhase,
    val officialReference: String,
)

/**
 * Rule ID to official compiler mapping.
 *
 * This catalog is the single source of truth for semantic alignment references.
 */
object CfirResolveRuleCatalog {
    val IMPORTS_BINDING = CfirResolveRuleDefinition(
        id = "RULE_IMPORTS_BINDING",
        phase = CfirResolvePhase.IMPORTS,
        officialReference = "external/cangjie_compiler/src/Sema/TypeChecker.cpp#TypeCheckForPackages",
    )
    val IMPORTS_CONFLICT = CfirResolveRuleDefinition(
        id = "RULE_IMPORTS_CONFLICT",
        phase = CfirResolvePhase.IMPORTS,
        officialReference = "external/cangjie_compiler/src/Sema/CheckUnusedImportImpl.cpp#CollectNeedCheckImports",
    )

    val SUPER_TYPES_DUPLICATE_INTERFACE = CfirResolveRuleDefinition(
        id = "RULE_SUPER_TYPES_DUPLICATE",
        phase = CfirResolvePhase.SUPER_TYPES,
        officialReference = "external/cangjie_compiler/src/Sema/TypeChecker.cpp#CheckInstDupSuperInterfaces",
    )
    val SUPER_TYPES_SELF_REFERENCE = CfirResolveRuleDefinition(
        id = "RULE_SUPER_TYPES_SELF_REFERENCE",
        phase = CfirResolvePhase.SUPER_TYPES,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckClassLike.cpp#CheckClassInheritance",
    )
    val SUPER_TYPES_INTERFACE_CANNOT_INHERIT_CLASS = CfirResolveRuleDefinition(
        id = "RULE_SUPER_TYPES_INTERFACE_CANNOT_INHERIT_CLASS",
        phase = CfirResolvePhase.SUPER_TYPES,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckClassLike.cpp#CheckClassInheritance",
    )
    val SUPER_TYPES_MULTIPLE_CLASS_SUPER_TYPES = CfirResolveRuleDefinition(
        id = "RULE_SUPER_TYPES_MULTIPLE_CLASS_SUPER_TYPES",
        phase = CfirResolvePhase.SUPER_TYPES,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckClassLike.cpp#CheckClassInheritance",
    )

    val TYPES_EXPLICIT_TYPE_RESOLUTION = CfirResolveRuleDefinition(
        id = "RULE_TYPES_EXPLICIT_TYPE_RESOLUTION",
        phase = CfirResolvePhase.TYPES,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckType.cpp",
    )
    val TYPES_ERROR_RECOVERY = CfirResolveRuleDefinition(
        id = "RULE_TYPES_ERROR_RECOVERY",
        phase = CfirResolvePhase.TYPES,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckUtil.cpp",
    )

    val STATUS_MODIFIER_LEGALITY = CfirResolveRuleDefinition(
        id = "RULE_STATUS_MODIFIER_LEGALITY",
        phase = CfirResolvePhase.STATUS,
        officialReference = "external/cangjie_compiler/src/Sema/DeclAttributeChecker.cpp",
    )
    val STATUS_VISIBILITY_CONSTRAINT = CfirResolveRuleDefinition(
        id = "RULE_STATUS_VISIBILITY_CONSTRAINT",
        phase = CfirResolvePhase.STATUS,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckDecl.cpp#CheckTypeAliasAccess",
    )

    val EXTENSIONS_DUPLICATE_INTERFACE = CfirResolveRuleDefinition(
        id = "RULE_EXTEND_DUPLICATE_INTERFACE",
        phase = CfirResolvePhase.EXTENSIONS,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckExtend.cpp#CheckExtendInterfaces",
    )
    val EXTENSIONS_NOT_INTERFACE = CfirResolveRuleDefinition(
        id = "RULE_EXTEND_NOT_INTERFACE",
        phase = CfirResolvePhase.EXTENSIONS,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckExtend.cpp#PreCheckExtend",
    )
    val EXTENSIONS_ILLEGAL_EXTENDED_TYPE = CfirResolveRuleDefinition(
        id = "RULE_ILLEGAL_EXTENDED_TYPE",
        phase = CfirResolvePhase.EXTENSIONS,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckExtend.cpp#CheckExtendedTypeValidity",
    )

    val IMPLICIT_TYPES_DECLARATION_BOUNDARY_INFERENCE = CfirResolveRuleDefinition(
        id = "RULE_IMPLICIT_TYPES_DECLARATION_BOUNDARY_INFERENCE",
        phase = CfirResolvePhase.IMPLICIT_TYPES,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckDecl.cpp#SynchronizeTypeAndInitializer",
    )
    val IMPLICIT_TYPES_CYCLIC_INFERENCE = CfirResolveRuleDefinition(
        id = "RULE_IMPLICIT_TYPES_CYCLIC_INFERENCE",
        phase = CfirResolvePhase.IMPLICIT_TYPES,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckDecl.cpp#CheckVarDecl",
    )

    val BODY_RESOLVE_CALL_BINDING = CfirResolveRuleDefinition(
        id = "RULE_BODY_RESOLVE_CALL_BINDING",
        phase = CfirResolvePhase.BODY_RESOLVE,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckCall.cpp",
    )
    val BODY_RESOLVE_CONTROL_FLOW_TYPING = CfirResolveRuleDefinition(
        id = "RULE_BODY_RESOLVE_CONTROL_FLOW_TYPING",
        phase = CfirResolvePhase.BODY_RESOLVE,
        officialReference = "external/cangjie_compiler/src/Sema/TypeCheckExpr.cpp",
    )

    val CHECKERS_FINAL_RESOLVE_DIAGNOSTICS = CfirResolveRuleDefinition(
        id = "RULE_CHECKERS_FINAL_RESOLVE_DIAGNOSTICS",
        phase = CfirResolvePhase.CHECKERS,
        officialReference = "external/cangjie_compiler/src/Sema/Diags.cpp",
    )
    val CHECKERS_DIAGNOSTIC_STABILITY = CfirResolveRuleDefinition(
        id = "RULE_CHECKERS_DIAGNOSTIC_STABILITY",
        phase = CfirResolvePhase.CHECKERS,
        officialReference = "external/cangjie_compiler/src/Sema/DiagSuppressor.cpp",
    )

    val all: List<CfirResolveRuleDefinition> = listOf(
        IMPORTS_BINDING,
        IMPORTS_CONFLICT,
        SUPER_TYPES_DUPLICATE_INTERFACE,
        SUPER_TYPES_SELF_REFERENCE,
        SUPER_TYPES_INTERFACE_CANNOT_INHERIT_CLASS,
        SUPER_TYPES_MULTIPLE_CLASS_SUPER_TYPES,
        TYPES_EXPLICIT_TYPE_RESOLUTION,
        TYPES_ERROR_RECOVERY,
        STATUS_MODIFIER_LEGALITY,
        STATUS_VISIBILITY_CONSTRAINT,
        EXTENSIONS_DUPLICATE_INTERFACE,
        EXTENSIONS_NOT_INTERFACE,
        EXTENSIONS_ILLEGAL_EXTENDED_TYPE,
        IMPLICIT_TYPES_DECLARATION_BOUNDARY_INFERENCE,
        IMPLICIT_TYPES_CYCLIC_INFERENCE,
        BODY_RESOLVE_CALL_BINDING,
        BODY_RESOLVE_CONTROL_FLOW_TYPING,
        CHECKERS_FINAL_RESOLVE_DIAGNOSTICS,
        CHECKERS_DIAGNOSTIC_STABILITY,
    )

    fun byId(id: String): CfirResolveRuleDefinition? = all.firstOrNull { it.id == id }
}
