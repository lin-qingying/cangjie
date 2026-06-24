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

package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.correspondingProperty
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import java.util.*

/**
 * 修饰符和注解目标检查使用的仓颉声明目标分类。
 *
 * @property description 诊断消息中展示的目标描述。
 * @property isDefault 该目标是否可作为默认目标参与诊断展示。
 */
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
    STATIC_INITIALIZER("static initializer", false),
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

    /** 常用目标组合和 class-like 实际目标分类工具。 */
    companion object {
        /** class 声明默认目标组合。 */
        val CLASS_LIST = listOf(CLASS_ONLY, CLASS)
        /** struct 声明默认目标组合。 */
        val STRUCT_LIST = listOf(STRUCT, CLASS)
        /** interface 声明默认目标组合。 */
        val INTERFACE_LIST = listOf(INTERFACE, CLASS)
        /** enum 声明默认目标组合。 */
        val ENUM_LIST = listOf(ENUM, CLASS)
        /** enum 构造器默认目标组合。 */
        val ENUM_ENTRY_LIST = listOf(ENUM_ENTRY, PROPERTY, VARIABLE, FIELD)
        /** extend 声明默认目标组合。 */
        val EXTEND_LIST = listOf(EXTEND)
        /** 函数默认目标组合。 */
        val FUNCTION_LIST = listOf(FUNCTION)
        /** 文件默认目标组合。 */
        val FILE_LIST = listOf(FILE)

        /** 根据 class-like 声明的具体种类返回其实际目标组合。 */
        fun classActualTargets(owner: CfirClassLikeDeclaration): List<CangJieTarget> = when (owner) {
            is CfirClass -> CLASS_LIST
            is CfirStruct -> STRUCT_LIST
            is CfirInterface -> INTERFACE_LIST
            is CfirEnum -> ENUM_LIST
            else -> CLASS_LIST
        }
    }
}

/** visibility 修饰符默认允许出现的声明目标集合。 */
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

/** 已弃用的父目标约束表；当前仓颉主干没有启用项。 */
internal val deprecatedParentTargetMap: Map<CjKeywordToken, Set<CangJieTarget>> = emptyMap()

/** 每个修饰符允许出现的直接声明目标集合。 */
internal val possibleTargetMap: Map<CjKeywordToken, Set<CangJieTarget>> = mapOf(
    STATIC_KEYWORD to EnumSet.of(
        CangJieTarget.MEMBER_FUNCTION,
        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.MEMBER_VARIABLE,
        CangJieTarget.STRUCT_MEMBER_FUNCTION,
        CangJieTarget.EXTEND_MEMBER_FUNCTION,
        CangJieTarget.INTERFACE_MEMBER_FUNCTION,
        CangJieTarget.STATIC_INITIALIZER,
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
        CangJieTarget.VARIABLE,
        CangJieTarget.TOP_LEVEL_VARIABLE,
        CangJieTarget.LOCAL_VARIABLE,
        CangJieTarget.MEMBER_VARIABLE,
        CangJieTarget.STATIC_INITIALIZER,
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
    UNSAFE_KEYWORD to EnumSet.of(
        CangJieTarget.TOP_LEVEL_FUNCTION,
        CangJieTarget.MEMBER_FUNCTION,
        CangJieTarget.STRUCT_MEMBER_FUNCTION,
        CangJieTarget.CLASS_MEMBER_FUNCTION,
        CangJieTarget.INTERFACE_MEMBER_FUNCTION,
        CangJieTarget.EXTEND_MEMBER_FUNCTION,
        CangJieTarget.ENUM_MEMBER_FUNCTION,
        CangJieTarget.LOCAL_FUNCTION,
        CangJieTarget.FUNCTION,
    ),
)

/** 已弃用的直接目标约束表；当前仓颉主干没有启用项。 */
internal val deprecatedTargetMap: Map<CjKeywordToken, Set<CangJieTarget>> = emptyMap()

/** 在允许目标内仍然属于冗余的修饰符目标表。 */
internal val redundantTargetMap: Map<CjKeywordToken, Set<CangJieTarget>> = mapOf(
    OPEN_KEYWORD to EnumSet.of(CangJieTarget.INTERFACE),
)

/** 需要结合父目标和语言版本设置判定的修饰符目标谓词。 */
internal interface TargetAllowedPredicate {
    /** 判断修饰符是否允许出现在指定父目标下。 */
    fun isAllowed(target: CangJieTarget, languageVersionSettings: LanguageVersionSettings): Boolean
}

/** 创建一个只允许给定目标集合的父目标谓词。 */
private fun always(target: CangJieTarget, vararg targets: CangJieTarget): TargetAllowedPredicate {
    val targetSet = EnumSet.of(target, *targets)
    return object : TargetAllowedPredicate {
        /** 判断目标是否属于构造时捕获的允许集合。 */
        override fun isAllowed(target: CangJieTarget, languageVersionSettings: LanguageVersionSettings): Boolean {
            return target in targetSet
        }
    }
}

/** 每个修饰符允许出现的父声明目标谓词表。 */
internal val possibleParentTargetPredicateMap: Map<CjKeywordToken, TargetAllowedPredicate> = mapOf(
    OVERRIDE_KEYWORD to always(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.STRUCT,
        CangJieTarget.INTERFACE,
        CangJieTarget.ENUM,
        CangJieTarget.ENUM_ENTRY,
    ),
    PROTECTED_KEYWORD to always(
        CangJieTarget.FILE,
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.STRUCT,
        CangJieTarget.ENUM,
        CangJieTarget.EXTEND,
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
        CangJieTarget.EXTEND,
        CangJieTarget.FILE,
    ),
)

/** 根据当前声明节点和外层 context 推导实际修饰符目标列表。 */
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
    is CfirValueParameter -> if (declaration.correspondingProperty != null) {
        AnnotationTargetLists.T_VALUE_PARAMETER_WITH_LET.defaultTargets
    } else {
        AnnotationTargetLists.T_VALUE_PARAMETER_WITHOUT_LET.defaultTargets
    }
    is CfirEnumConstructor -> CangJieTarget.ENUM_ENTRY_LIST
    is CfirConstructor -> if (declaration.status.isStatic) {
        AnnotationTargetLists.T_STATIC_INITIALIZER.defaultTargets
    } else {
        AnnotationTargetLists.T_CONSTRUCTOR.defaultTargets
    }
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

/** 根据当前声明栈推导修饰符所在父级声明的目标列表。 */
internal fun CheckerContext.actualParentTargets(): List<CangJieTarget> = when (val parent = closestModifierContainingDeclaration()) {
    is CfirClassLikeDeclaration -> CangJieTarget.classActualTargets(parent)
    is CfirExtend -> CangJieTarget.EXTEND_LIST
    is CfirEnumConstructor -> CangJieTarget.ENUM_ENTRY_LIST
    is CfirConstructor -> AnnotationTargetLists.T_CONSTRUCTOR.defaultTargets
    is CfirFunction -> CangJieTarget.FUNCTION_LIST
    else -> CangJieTarget.FILE_LIST
}

/** 查找对修饰符归属有意义的最近外层声明。 */
private fun CheckerContext.closestModifierContainingDeclaration(): CfirDeclaration? =
    containingDeclarations.asReversed().firstOrNull { declaration ->
        // 对齐 Kotlin FirModifierChecker：属性参数修饰符的包含声明应越过主构造和 fake property，落到外层类型。
        declaration !is CfirProperty &&
                !(declaration is CfirConstructor && declaration.isPrimary) &&
                declaration.source?.kind !is CjFakeSourceElementKind
    }

/** 返回目标列表的首个描述；列表为空时使用通用 `this` 描述。 */
internal fun List<CangJieTarget>.firstOrThisDescription(): String = firstOrNull()?.description ?: "this"

/** 查找最近的 class-like 或 extend 声明。 */
private fun CheckerContext.closestContainingTypeDeclaration(): CfirDeclaration? =
    findClosestDeclaration<CfirDeclaration> { declaration ->
        declaration is CfirClassLikeDeclaration || declaration is CfirExtend
    }

/** 根据函数所处位置区分顶层、局部、匿名和各类成员函数目标。 */
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

/** 一个声明形态对应的默认目标和附加目标列表。 */
private class AnnotationTargetList(
    /** 该声明形态直接拥有的默认目标。 */
    val defaultTargets: List<CangJieTarget>,
    /** 可被 use-site 或属性展开替换的附加目标。 */
    val canBeSubstituted: List<CangJieTarget> = emptyList(),
    /** 只能通过显式 use-site target 使用的目标。 */
    val onlyWithUseSiteTarget: List<CangJieTarget> = emptyList(),
)

/** 仓颉各类声明形态到注解/修饰符目标列表的集中定义。 */
private object AnnotationTargetLists {
    /** 成员字段变量的目标列表。 */
    val T_MEMBER_VARIABLE = targetList(CangJieTarget.MEMBER_VARIABLE, CangJieTarget.VARIABLE)
    /** 普通成员属性的目标列表。 */
    val T_MEMBER_PROPERTY = targetList(CangJieTarget.MEMBER_PROPERTY, CangJieTarget.PROPERTY)
    /** struct 成员属性的目标列表。 */
    val T_STRUCT_MEMBER_PROPERTY = targetList(CangJieTarget.STRUCT_MEMBER_PROPERTY, *T_MEMBER_PROPERTY.defaultTargets.toTypedArray())
    /** class 成员属性的目标列表。 */
    val T_CLASS_MEMBER_PROPERTY = targetList(CangJieTarget.CLASS_MEMBER_PROPERTY, *T_MEMBER_PROPERTY.defaultTargets.toTypedArray())
    /** enum 成员属性的目标列表。 */
    val T_ENUM_MEMBER_PROPERTY = targetList(CangJieTarget.ENUM_MEMBER_PROPERTY, *T_MEMBER_PROPERTY.defaultTargets.toTypedArray())
    /** extend 成员属性的目标列表。 */
    val T_EXTEND_MEMBER_PROPERTY = targetList(CangJieTarget.EXTEND_MEMBER_PROPERTY, *T_MEMBER_PROPERTY.defaultTargets.toTypedArray())
    /** interface 成员属性的目标列表。 */
    val T_INTERFACE_MEMBER_PROPERTY = targetList(CangJieTarget.INTERFACE_MEMBER_PROPERTY, *T_MEMBER_PROPERTY.defaultTargets.toTypedArray())
    /** 局部变量的目标列表。 */
    val T_LOCAL_VARIABLE = targetList(CangJieTarget.LOCAL_VARIABLE)
    /** 顶层变量的目标列表。 */
    val T_TOP_LEVEL_VARIABLE = targetList(CangJieTarget.TOP_LEVEL_VARIABLE, CangJieTarget.VARIABLE)
    /** 不带 `let` 对应属性的值参数目标列表。 */
    val T_VALUE_PARAMETER_WITHOUT_LET = targetList(CangJieTarget.VALUE_PARAMETER)
    /** 带 `let` 对应属性的值参数目标列表。 */
    val T_VALUE_PARAMETER_WITH_LET = targetList(
        CangJieTarget.VALUE_PARAMETER,
        CangJieTarget.VARIABLE,
        CangJieTarget.MEMBER_PROPERTY,
    ) {
        extraTargets(CangJieTarget.FIELD)
    }
    /** 普通构造器目标列表。 */
    val T_CONSTRUCTOR = targetList(CangJieTarget.CONSTRUCTOR)
    /** 静态初始化器目标列表。 */
    val T_STATIC_INITIALIZER = targetList(CangJieTarget.STATIC_INITIALIZER)
    /** macro 声明目标列表。 */
    val T_MACRO = targetList(CangJieTarget.MACRO)
    /** 函数表达式目标列表。 */
    val T_FUNCTION_EXPRESSION = targetList(
        CangJieTarget.ANONYMOUS_FUNCTION,
        CangJieTarget.FUNCTION,
        CangJieTarget.EXPRESSION,
    )
    /** 局部函数目标列表。 */
    val T_LOCAL_FUNCTION = targetList(CangJieTarget.LOCAL_FUNCTION, CangJieTarget.FUNCTION)
    /** 普通成员函数目标列表。 */
    val T_MEMBER_FUNCTION = targetList(CangJieTarget.MEMBER_FUNCTION, CangJieTarget.FUNCTION)
    /** 顶层函数目标列表。 */
    val T_TOP_LEVEL_FUNCTION = targetList(CangJieTarget.TOP_LEVEL_FUNCTION, CangJieTarget.FUNCTION)
    /** struct 成员函数目标列表。 */
    val T_STRUCT_MEMBER_FUNCTION = targetList(CangJieTarget.STRUCT_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray())
    /** class 成员函数目标列表。 */
    val T_CLASS_MEMBER_FUNCTION = targetList(CangJieTarget.CLASS_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray())
    /** enum 成员函数目标列表。 */
    val T_ENUM_MEMBER_FUNCTION = targetList(CangJieTarget.ENUM_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray())
    /** interface 成员函数目标列表。 */
    val T_INTERFACE_MEMBER_FUNCTION = targetList(CangJieTarget.INTERFACE_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray())
    /** extend 成员函数目标列表。 */
    val T_EXTEND_MEMBER_FUNCTION = targetList(CangJieTarget.EXTEND_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray())
    /** 类型别名目标列表。 */
    val T_TYPEALIAS = targetList(CangJieTarget.TYPEALIAS)
    /** 文件目标列表。 */
    val T_FILE = targetList(CangJieTarget.FILE)
    /** 类型参数目标列表。 */
    val T_TYPE_PARAMETER = targetList(CangJieTarget.TYPE_PARAMETER)
    /** 空目标列表，用于不支持修饰符目标的声明。 */
    val EMPTY = targetList()

    /** 创建一个目标列表并应用额外目标配置。 */
    private fun targetList(vararg targets: CangJieTarget, otherTargets: TargetListBuilder.() -> Unit = {}): AnnotationTargetList {
        val builder = TargetListBuilder(*targets)
        builder.otherTargets()
        return builder.build()
    }

    /** 目标列表构造器，用于记录默认目标和可替换目标。 */
    private class TargetListBuilder(vararg val defaultTargets: CangJieTarget) {
        /** 可被替换出的附加目标。 */
        private var canBeSubstituted: List<CangJieTarget> = emptyList()
        /** 只能通过显式 use-site target 使用的目标。 */
        private var onlyWithUseSiteTarget: List<CangJieTarget> = emptyList()

        /** 注册可被替换出的附加目标。 */
        fun extraTargets(vararg targets: CangJieTarget) {
            canBeSubstituted = targets.toList()
        }

        /** 注册只能通过显式 use-site target 使用的目标。 */
        fun onlyWithUseSiteTarget(vararg targets: CangJieTarget) {
            onlyWithUseSiteTarget = targets.toList()
        }

        /** 构造不可变的目标列表。 */
        fun build(): AnnotationTargetList = AnnotationTargetList(defaultTargets.toList(), canBeSubstituted, onlyWithUseSiteTarget)
    }
}
