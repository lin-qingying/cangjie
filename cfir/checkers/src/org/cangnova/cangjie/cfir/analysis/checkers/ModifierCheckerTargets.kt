package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens.ABSTRACT_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.CONST_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.FOREIGN_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.INTERNAL_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.MUT_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.OPEN_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.OPERATOR_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.OVERRIDE_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.PRIVATE_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.PROTECTED_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.PUBLIC_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.REDEF_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.SEALED_KEYWORD
import org.cangnova.cangjie.lexer.CjTokens.STATIC_KEYWORD
import java.util.EnumSet

internal enum class CangJieTarget(val description: String, val isDefault: Boolean = true) {
    CLASS("class"),
    EXTEND("extend"),
    CLASS_ONLY("class", false),
    STRUCT("struct", false),
    ENUM("enum", false),
    INTERFACE("interface", false),
    ENUM_ENTRY("enum constructor", false),
    PROPERTY("property"),
    VARIABLE("variable"),
    TYPEALIAS("typealias", false),
    EXPRESSION("expression", false),
    FIELD("field"),
    LOCAL_VARIABLE("local variable"),
    INITIALIZER("initializer", false),
    VALUE_PARAMETER("value parameter"),
    MEMBER_VARIABLE("member variable", false),
    MEMBER_PROPERTY("member property", false),
    CLASS_MEMBER_PROPERTY("class member property", false),
    STRUCT_MEMBER_PROPERTY("struct member property", false),
    EXTEND_MEMBER_PROPERTY("extend member property", false),
    INTERFACE_MEMBER_PROPERTY("interface member property", false),
    ENUM_MEMBER_PROPERTY("enum member property", false),
    MACRO("macro"),
    CONSTRUCTOR("constructor"),
    PROPERTY_GETTER("getter"),
    PROPERTY_SETTER("setter"),
    LAMBDA_EXPRESSION("lambda expression", false),
    TOP_LEVEL_FUNCTION("top level function", false),
    TOP_LEVEL_VARIABLE("top level variable", false),
    BACKING_FIELD("backing field"),
    TOP_LEVEL_PROPERTY("top level property", false),
    FILE("file", false),
    TYPE_PROJECTION("type projection", false),
    FUNCTION("function"),
    ANONYMOUS_FUNCTION("anonymous function", false),
    LOCAL_FUNCTION("local function", false),
    TYPE_PARAMETER("type parameter", false),
    MEMBER_FUNCTION("member function", false),
    STRUCT_MEMBER_FUNCTION("struct member function", false),
    CLASS_MEMBER_FUNCTION("class member function", false),
    INTERFACE_MEMBER_FUNCTION("interface member function", false),
    EXTEND_MEMBER_FUNCTION("extend member function", false),
    ENUM_MEMBER_FUNCTION("enum member function", false),
    TYPE("type usage", false),
    ;

    companion object {
        val CLASS_LIST = listOf(CLASS_ONLY, CLASS)
        val STRUCT_LIST = listOf(STRUCT, CLASS)
        val INTERFACE_LIST = listOf(INTERFACE, CLASS)
        val ENUM_LIST = listOf(ENUM, CLASS)
        val ENUM_ENTRY_LIST = listOf(ENUM_ENTRY, PROPERTY, VARIABLE, FIELD)
        val EXTEND_LIST = listOf(EXTEND)
        val FUNCTION_LIST = listOf(FUNCTION)
        val FILE_LIST = listOf(FILE)

        fun classActualTargets(owner: CfirClassLikeDeclaration): List<CangJieTarget> = when (owner) {
            is CfirClass -> CLASS_LIST
            is CfirStruct -> STRUCT_LIST
            is CfirInterface -> INTERFACE_LIST
            is CfirEnum -> ENUM_LIST
            else -> CLASS_LIST
        }
    }
}

private val defaultVisibilityTargets: Set<CangJieTarget> = EnumSet.of(
    CangJieTarget.CLASS_ONLY,
    CangJieTarget.STRUCT,
    CangJieTarget.INTERFACE,
    CangJieTarget.ENUM,
    CangJieTarget.MEMBER_FUNCTION,
    CangJieTarget.TOP_LEVEL_FUNCTION,
    CangJieTarget.MEMBER_VARIABLE,
    CangJieTarget.VARIABLE,
    CangJieTarget.FUNCTION,
    CangJieTarget.MEMBER_PROPERTY,
    CangJieTarget.TOP_LEVEL_VARIABLE,
    CangJieTarget.CONSTRUCTOR,
    CangJieTarget.TYPEALIAS,
)

internal val deprecatedParentTargetMap: Map<CjKeywordToken, Set<CangJieTarget>> = emptyMap()

internal val possibleTargetMap: Map<CjKeywordToken, Set<CangJieTarget>> = mapOf(
    STATIC_KEYWORD to EnumSet.of(
        CangJieTarget.MEMBER_FUNCTION,
        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.MEMBER_VARIABLE,
        CangJieTarget.STRUCT_MEMBER_FUNCTION,
        CangJieTarget.EXTEND_MEMBER_FUNCTION,
        CangJieTarget.INTERFACE_MEMBER_FUNCTION,
    ),
    ABSTRACT_KEYWORD to EnumSet.of(
        CangJieTarget.CLASS_ONLY,
    ),
    MUT_KEYWORD to EnumSet.of(
        CangJieTarget.INTERFACE_MEMBER_FUNCTION,
        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.STRUCT_MEMBER_FUNCTION,
    ),
    OPEN_KEYWORD to EnumSet.of(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.INTERFACE,
        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.MEMBER_FUNCTION,
    ),
    SEALED_KEYWORD to EnumSet.of(CangJieTarget.CLASS_ONLY, CangJieTarget.INTERFACE),
    REDEF_KEYWORD to EnumSet.of(
        CangJieTarget.STRUCT_MEMBER_FUNCTION,
        CangJieTarget.STRUCT_MEMBER_PROPERTY,
        CangJieTarget.ENUM_MEMBER_FUNCTION,
        CangJieTarget.ENUM_MEMBER_PROPERTY,
        CangJieTarget.CLASS_MEMBER_FUNCTION,
        CangJieTarget.CLASS_MEMBER_PROPERTY,
        CangJieTarget.INTERFACE_MEMBER_FUNCTION,
        CangJieTarget.INTERFACE_MEMBER_PROPERTY,
        CangJieTarget.EXTEND_MEMBER_FUNCTION,
    ),
    OVERRIDE_KEYWORD to EnumSet.of(
        CangJieTarget.EXTEND_MEMBER_FUNCTION,
        CangJieTarget.STRUCT_MEMBER_FUNCTION,
        CangJieTarget.STRUCT_MEMBER_PROPERTY,
        CangJieTarget.ENUM_MEMBER_FUNCTION,
        CangJieTarget.ENUM_MEMBER_PROPERTY,
        CangJieTarget.CLASS_MEMBER_FUNCTION,
        CangJieTarget.CLASS_MEMBER_PROPERTY,
        CangJieTarget.INTERFACE_MEMBER_FUNCTION,
        CangJieTarget.INTERFACE_MEMBER_PROPERTY,
    ),
    PRIVATE_KEYWORD to (defaultVisibilityTargets + CangJieTarget.BACKING_FIELD),
    PUBLIC_KEYWORD to (defaultVisibilityTargets + CangJieTarget.MACRO),
    INTERNAL_KEYWORD to (defaultVisibilityTargets + CangJieTarget.BACKING_FIELD),
    PROTECTED_KEYWORD to EnumSet.of(
        CangJieTarget.FUNCTION,
        CangJieTarget.VARIABLE,
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.STRUCT,
        CangJieTarget.INTERFACE,
        CangJieTarget.ENUM,
        CangJieTarget.MEMBER_FUNCTION,
        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.MEMBER_VARIABLE,
        CangJieTarget.CONSTRUCTOR,
        CangJieTarget.TYPEALIAS,
    ),
    CONST_KEYWORD to EnumSet.of(
        CangJieTarget.FUNCTION,
        CangJieTarget.STRUCT_MEMBER_FUNCTION,
        CangJieTarget.CONSTRUCTOR,
    ),
    OPERATOR_KEYWORD to EnumSet.of(
        CangJieTarget.MEMBER_FUNCTION,
        CangJieTarget.INTERFACE_MEMBER_FUNCTION,
        CangJieTarget.STRUCT_MEMBER_FUNCTION,
        CangJieTarget.EXTEND_MEMBER_FUNCTION,
    ),
    // foreign 函数签名当前按 first-party 前端的顶层 CFFI 入口建模，
    // 不放宽到局部函数或匿名函数，避免把尚未建模的语义提前合法化。
    FOREIGN_KEYWORD to EnumSet.of(
        CangJieTarget.TOP_LEVEL_FUNCTION,
    ),
)

internal val deprecatedTargetMap: Map<CjKeywordToken, Set<CangJieTarget>> = emptyMap()

internal val redundantTargetMap: Map<CjKeywordToken, Set<CangJieTarget>> = mapOf(
    OPEN_KEYWORD to EnumSet.of(CangJieTarget.INTERFACE),
)

internal interface TargetAllowedPredicate {
    fun isAllowed(target: CangJieTarget, languageVersionSettings: LanguageVersionSettings): Boolean
}

private fun always(target: CangJieTarget, vararg targets: CangJieTarget): TargetAllowedPredicate {
    val targetSet = EnumSet.of(target, *targets)
    return object : TargetAllowedPredicate {
        override fun isAllowed(target: CangJieTarget, languageVersionSettings: LanguageVersionSettings): Boolean {
            return target in targetSet
        }
    }
}

internal val possibleParentTargetPredicateMap: Map<CjKeywordToken, TargetAllowedPredicate> = mapOf(
    OVERRIDE_KEYWORD to always(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.STRUCT,
        CangJieTarget.INTERFACE,
        CangJieTarget.ENUM,
        CangJieTarget.ENUM_ENTRY,
    ),
    PROTECTED_KEYWORD to always(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.ENUM,
    ),
    INTERNAL_KEYWORD to always(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.STRUCT,
        CangJieTarget.ENUM,
        CangJieTarget.ENUM_ENTRY,
        CangJieTarget.FILE,
    ),
    PRIVATE_KEYWORD to always(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.STRUCT,
        CangJieTarget.ENUM,
        CangJieTarget.ENUM_ENTRY,
        CangJieTarget.FILE,
    ),
)

internal fun CheckerContext.actualTargetsFor(declaration: CfirDeclaration): List<CangJieTarget> = when (declaration) {
    is CfirClassLikeDeclaration -> CangJieTarget.classActualTargets(declaration)
    is CfirExtend -> CangJieTarget.EXTEND_LIST
    is CfirProperty -> when (closestContainingTypeDeclaration()) {
        is CfirStruct -> AnnotationTargetLists.T_STRUCT_MEMBER_PROPERTY.defaultTargets
        is CfirInterface -> AnnotationTargetLists.T_INTERFACE_MEMBER_PROPERTY.defaultTargets
        is CfirExtend -> AnnotationTargetLists.T_EXTEND_MEMBER_PROPERTY.defaultTargets
        is CfirClass -> AnnotationTargetLists.T_CLASS_MEMBER_PROPERTY.defaultTargets
        is CfirEnum -> AnnotationTargetLists.T_ENUM_MEMBER_PROPERTY.defaultTargets
        else -> AnnotationTargetLists.T_MEMBER_PROPERTY.defaultTargets
    }
    is CfirPatternVariable -> if (declaration.isLocal) {
        AnnotationTargetLists.T_LOCAL_VARIABLE.defaultTargets
    } else {
        AnnotationTargetLists.T_TOP_LEVEL_VARIABLE.defaultTargets
    }
    is CfirPatternBindingVariable -> if (declaration.isLocal) {
        AnnotationTargetLists.T_LOCAL_VARIABLE.defaultTargets
    } else {
        AnnotationTargetLists.T_TOP_LEVEL_VARIABLE.defaultTargets
    }
    is CfirFieldVariable -> AnnotationTargetLists.T_MEMBER_VARIABLE.defaultTargets
    is CfirValueParameter -> if (declaration.isVar) {
        AnnotationTargetLists.T_VALUE_PARAMETER_WITH_LET.defaultTargets
    } else {
        AnnotationTargetLists.T_VALUE_PARAMETER_WITHOUT_LET.defaultTargets
    }
    is CfirEnumConstructor -> CangJieTarget.ENUM_ENTRY_LIST
    is CfirConstructor -> AnnotationTargetLists.T_CONSTRUCTOR.defaultTargets
    is CfirMacroDeclaration -> AnnotationTargetLists.T_MACRO.defaultTargets
    is CfirAnonymousFunction -> AnnotationTargetLists.T_FUNCTION_EXPRESSION.defaultTargets
    is CfirMainFunction -> classifyFunctionTargets(declaration)
    is CfirFinalizer -> classifyFunctionTargets(declaration)
    is CfirNamedFunction -> classifyFunctionTargets(declaration)
    is CfirFunction -> classifyFunctionTargets(declaration)
    is CfirTypeAlias -> AnnotationTargetLists.T_TYPEALIAS.defaultTargets
    is CfirFile -> AnnotationTargetLists.T_FILE.defaultTargets
    is CfirTypeParameter -> AnnotationTargetLists.T_TYPE_PARAMETER.defaultTargets
    else -> AnnotationTargetLists.EMPTY.defaultTargets
}

internal fun CheckerContext.actualParentTargets(): List<CangJieTarget> = when (val parent = containingDeclarations.lastOrNull()) {
    is CfirClassLikeDeclaration -> CangJieTarget.classActualTargets(parent)
    is CfirExtend -> CangJieTarget.EXTEND_LIST
    is CfirEnumConstructor -> CangJieTarget.ENUM_ENTRY_LIST
    is CfirConstructor -> AnnotationTargetLists.T_CONSTRUCTOR.defaultTargets
    is CfirFunction -> CangJieTarget.FUNCTION_LIST
    else -> CangJieTarget.FILE_LIST
}

internal fun List<CangJieTarget>.firstOrThisDescription(): String = firstOrNull()?.description ?: "this"

private fun CheckerContext.closestContainingTypeDeclaration(): CfirDeclaration? =
    findClosestDeclaration<CfirDeclaration> { declaration ->
        declaration is CfirClassLikeDeclaration || declaration is CfirExtend
    }

private fun CheckerContext.classifyFunctionTargets(function: CfirFunction): List<CangJieTarget> = when {
    function is CfirAnonymousFunction -> AnnotationTargetLists.T_FUNCTION_EXPRESSION.defaultTargets
    function.isLocal -> AnnotationTargetLists.T_LOCAL_FUNCTION.defaultTargets
    closestContainingTypeDeclaration() is CfirStruct -> AnnotationTargetLists.T_STRUCT_MEMBER_FUNCTION.defaultTargets
    closestContainingTypeDeclaration() is CfirInterface -> AnnotationTargetLists.T_INTERFACE_MEMBER_FUNCTION.defaultTargets
    closestContainingTypeDeclaration() is CfirExtend -> AnnotationTargetLists.T_EXTEND_MEMBER_FUNCTION.defaultTargets
    closestContainingTypeDeclaration() is CfirClass -> AnnotationTargetLists.T_CLASS_MEMBER_FUNCTION.defaultTargets
    closestContainingTypeDeclaration() is CfirEnum -> AnnotationTargetLists.T_ENUM_MEMBER_FUNCTION.defaultTargets
    closestContainingTypeDeclaration() is CfirClassLikeDeclaration -> AnnotationTargetLists.T_MEMBER_FUNCTION.defaultTargets
    else -> AnnotationTargetLists.T_TOP_LEVEL_FUNCTION.defaultTargets
}

private class AnnotationTargetList(
    val defaultTargets: List<CangJieTarget>,
    val canBeSubstituted: List<CangJieTarget> = emptyList(),
    val onlyWithUseSiteTarget: List<CangJieTarget> = emptyList(),
)

private object AnnotationTargetLists {
    val T_MEMBER_VARIABLE = targetList(CangJieTarget.MEMBER_VARIABLE, CangJieTarget.VARIABLE)
    val T_MEMBER_PROPERTY = targetList(CangJieTarget.MEMBER_PROPERTY, CangJieTarget.PROPERTY)
    val T_STRUCT_MEMBER_PROPERTY = targetList(CangJieTarget.STRUCT_MEMBER_PROPERTY, *T_MEMBER_PROPERTY.defaultTargets.toTypedArray())
    val T_CLASS_MEMBER_PROPERTY = targetList(CangJieTarget.CLASS_MEMBER_PROPERTY, *T_MEMBER_PROPERTY.defaultTargets.toTypedArray())
    val T_ENUM_MEMBER_PROPERTY = targetList(CangJieTarget.ENUM_MEMBER_PROPERTY, *T_MEMBER_PROPERTY.defaultTargets.toTypedArray())
    val T_EXTEND_MEMBER_PROPERTY = targetList(CangJieTarget.EXTEND_MEMBER_PROPERTY, *T_MEMBER_PROPERTY.defaultTargets.toTypedArray())
    val T_INTERFACE_MEMBER_PROPERTY = targetList(CangJieTarget.INTERFACE_MEMBER_PROPERTY, *T_MEMBER_PROPERTY.defaultTargets.toTypedArray())
    val T_LOCAL_VARIABLE = targetList(CangJieTarget.LOCAL_VARIABLE)
    val T_TOP_LEVEL_VARIABLE = targetList(CangJieTarget.TOP_LEVEL_VARIABLE, CangJieTarget.VARIABLE)
    val T_VALUE_PARAMETER_WITHOUT_LET = targetList(CangJieTarget.VALUE_PARAMETER)
    val T_VALUE_PARAMETER_WITH_LET = targetList(
        CangJieTarget.VALUE_PARAMETER,
        CangJieTarget.VARIABLE,
        CangJieTarget.MEMBER_PROPERTY,
    ) {
        extraTargets(CangJieTarget.FIELD)
    }
    val T_CONSTRUCTOR = targetList(CangJieTarget.CONSTRUCTOR)
    val T_MACRO = targetList(CangJieTarget.MACRO)
    val T_FUNCTION_EXPRESSION = targetList(
        CangJieTarget.ANONYMOUS_FUNCTION,
        CangJieTarget.FUNCTION,
        CangJieTarget.EXPRESSION,
    )
    val T_LOCAL_FUNCTION = targetList(CangJieTarget.LOCAL_FUNCTION, CangJieTarget.FUNCTION)
    val T_MEMBER_FUNCTION = targetList(CangJieTarget.MEMBER_FUNCTION, CangJieTarget.FUNCTION)
    val T_TOP_LEVEL_FUNCTION = targetList(CangJieTarget.TOP_LEVEL_FUNCTION, CangJieTarget.FUNCTION)
    val T_STRUCT_MEMBER_FUNCTION = targetList(CangJieTarget.STRUCT_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray())
    val T_CLASS_MEMBER_FUNCTION = targetList(CangJieTarget.CLASS_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray())
    val T_ENUM_MEMBER_FUNCTION = targetList(CangJieTarget.ENUM_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray())
    val T_INTERFACE_MEMBER_FUNCTION = targetList(CangJieTarget.INTERFACE_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray())
    val T_EXTEND_MEMBER_FUNCTION = targetList(CangJieTarget.EXTEND_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray())
    val T_TYPEALIAS = targetList(CangJieTarget.TYPEALIAS)
    val T_FILE = targetList(CangJieTarget.FILE)
    val T_TYPE_PARAMETER = targetList(CangJieTarget.TYPE_PARAMETER)
    val EMPTY = targetList()

    private fun targetList(vararg targets: CangJieTarget, otherTargets: TargetListBuilder.() -> Unit = {}): AnnotationTargetList {
        val builder = TargetListBuilder(*targets)
        builder.otherTargets()
        return builder.build()
    }

    private class TargetListBuilder(vararg val defaultTargets: CangJieTarget) {
        private var canBeSubstituted: List<CangJieTarget> = emptyList()
        private var onlyWithUseSiteTarget: List<CangJieTarget> = emptyList()

        fun extraTargets(vararg targets: CangJieTarget) {
            canBeSubstituted = targets.toList()
        }

        fun onlyWithUseSiteTarget(vararg targets: CangJieTarget) {
            onlyWithUseSiteTarget = targets.toList()
        }

        fun build(): AnnotationTargetList = AnnotationTargetList(defaultTargets.toList(), canBeSubstituted, onlyWithUseSiteTarget)
    }
}
