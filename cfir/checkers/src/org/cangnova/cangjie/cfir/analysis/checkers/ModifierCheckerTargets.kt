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
 */

package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.correspondingProperty
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosest
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.modifier.DeclarationKind
import org.cangnova.cangjie.cfir.analysis.checkers.modifier.ModifierTarget
import org.cangnova.cangjie.cfir.analysis.checkers.modifier.ModifierTargetPredicate
import org.cangnova.cangjie.cfir.analysis.checkers.modifier.Site
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.source.CjFakeSourceElementKind

/**
 * 修饰符目标推导与允许表。
 *
 * 重构后的二维模型：声明种类（[DeclarationKind]）+ 作用位置（[Site]）正交组合成 [ModifierTarget]，
 * 替代旧 `CangJieTarget` 一维枚举的笛卡尔积子项（`CLASS_MEMBER_FUNCTION`/`STRUCT_MEMBER_FUNCTION`/...）。
 *
 * 与注解定位体系 `CangjieAnnotationTarget`（对齐官方 10 项）分开建模——
 * 注解侧编译器内部 = 用户语言层，粒度一致；修饰符侧编译器内部需细分，由本文件承担。
 */

/** class-like 种类集合（class/struct/interface/enum/extend），用于谓词组合。 */
private val CLASS_LIKE: Set<DeclarationKind> = setOf(
    DeclarationKind.CLASS,
    DeclarationKind.STRUCT,
    DeclarationKind.INTERFACE,
    DeclarationKind.ENUM,
    DeclarationKind.EXTEND,
)

/** class-like 容器种类（不含 extend），用于"成员承载于哪种容器"的细分判定。 */
private val CLASS_LIKE_CONTAINERS: Set<DeclarationKind> = setOf(
    DeclarationKind.CLASS,
    DeclarationKind.STRUCT,
    DeclarationKind.INTERFACE,
    DeclarationKind.ENUM,
    DeclarationKind.EXTEND,
)

/** visibility 修饰符默认允许的声明种类（顶层 + 成员 + class-like 头）。 */
private val defaultVisibilityKinds: Set<DeclarationKind> = setOf(
    DeclarationKind.CLASS,
    DeclarationKind.STRUCT,
    DeclarationKind.INTERFACE,
    DeclarationKind.ENUM,
    DeclarationKind.EXTEND,
    DeclarationKind.FUNCTION,
    DeclarationKind.PROPERTY,
    DeclarationKind.VARIABLE,
    DeclarationKind.CONSTRUCTOR,
    DeclarationKind.TYPEALIAS,
    DeclarationKind.STATIC_INITIALIZER,
    DeclarationKind.ENUM_CONSTRUCTOR,
)

/** 已弃用的直接目标约束表；当前仓颉主干没有启用项。 */
internal val deprecatedTargetMap: Map<CjKeywordToken, ModifierTargetPredicate> = emptyMap()

/** 已弃用的父目标约束表；当前仓颉主干没有启用项。 */
internal val deprecatedParentTargetMap: Map<CjKeywordToken, ModifierTargetPredicate> = emptyMap()

/**
 * 每个修饰符允许出现的直接声明目标谓词表。
 *
 * 替代旧 `possibleTargetMap: Map<Token, Set<CangJieTarget>>`——
 * 改用 [ModifierTargetPredicate] 表达"任意 class-like 的成员函数"这类共享组合，
 * 避免手工列举笛卡尔积子项产生不一致。
 */
internal val possibleTargetMap: Map<CjKeywordToken, ModifierTargetPredicate> = mapOf(
    STATIC_KEYWORD to ModifierTargetPredicate.memberOf(
        DeclarationKind.FUNCTION,
        DeclarationKind.PROPERTY,
        DeclarationKind.VARIABLE,
    ),

    ABSTRACT_KEYWORD to ModifierTargetPredicate.headOf(DeclarationKind.CLASS),

    MUT_KEYWORD to ModifierTargetPredicate.memberOf(
        DeclarationKind.FUNCTION,
        DeclarationKind.PROPERTY,
    ),

    OPEN_KEYWORD to ModifierTargetPredicate.anyOf(
        ModifierTargetPredicate.headOf(DeclarationKind.CLASS, DeclarationKind.INTERFACE),
        ModifierTargetPredicate.memberOf(DeclarationKind.PROPERTY, DeclarationKind.FUNCTION),
    ),

    SEALED_KEYWORD to ModifierTargetPredicate.headOf(DeclarationKind.CLASS, DeclarationKind.INTERFACE),

    REDEF_KEYWORD to ModifierTargetPredicate.memberOf(
        DeclarationKind.FUNCTION,
        DeclarationKind.PROPERTY,
    ),

    OVERRIDE_KEYWORD to ModifierTargetPredicate.memberOf(
        DeclarationKind.FUNCTION,
        DeclarationKind.PROPERTY,
    ),

    PRIVATE_KEYWORD to ModifierTargetPredicate.anyOf(
        ModifierTargetPredicate.anySiteOf(*defaultVisibilityKinds.toTypedArray()),
        ModifierTargetPredicate.memberOf(DeclarationKind.PROPERTY),
    ),

    PUBLIC_KEYWORD to ModifierTargetPredicate.anyOf(
        ModifierTargetPredicate.anySiteOf(*defaultVisibilityKinds.toTypedArray()),
        ModifierTargetPredicate.headOf(DeclarationKind.MACRO),
    ),

    INTERNAL_KEYWORD to ModifierTargetPredicate.anyOf(
        ModifierTargetPredicate.anySiteOf(*defaultVisibilityKinds.toTypedArray()),
        ModifierTargetPredicate.memberOf(DeclarationKind.PROPERTY),
    ),

    PROTECTED_KEYWORD to ModifierTargetPredicate.anyOf(
        ModifierTargetPredicate.anySiteOf(
            DeclarationKind.FUNCTION,
            DeclarationKind.VARIABLE,
            DeclarationKind.CLASS,
            DeclarationKind.STRUCT,
            DeclarationKind.INTERFACE,
            DeclarationKind.ENUM,
            DeclarationKind.EXTEND,
            DeclarationKind.CONSTRUCTOR,
            DeclarationKind.TYPEALIAS,
        ),
        ModifierTargetPredicate.memberOf(
            DeclarationKind.FUNCTION,
            DeclarationKind.PROPERTY,
            DeclarationKind.VARIABLE,
        ),
    ),

    CONST_KEYWORD to ModifierTargetPredicate.anyOf(
        ModifierTargetPredicate.anySiteOf(
            DeclarationKind.FUNCTION,
            DeclarationKind.CONSTRUCTOR,
            DeclarationKind.VARIABLE,
            DeclarationKind.STATIC_INITIALIZER,
        ),
        ModifierTargetPredicate.memberOf(DeclarationKind.FUNCTION, DeclarationKind.VARIABLE),
    ),

    OPERATOR_KEYWORD to ModifierTargetPredicate.memberOf(DeclarationKind.FUNCTION),

    // foreign 函数签名当前按 first-party 前端的顶层 CFFI 入口建模，
    // 不放宽到局部函数或匿名函数，避免把尚未建模的语义提前合法化。
    FOREIGN_KEYWORD to ModifierTargetPredicate.headOf(DeclarationKind.FUNCTION),

    UNSAFE_KEYWORD to ModifierTargetPredicate.anySiteOf(DeclarationKind.FUNCTION),
)

/** 在允许目标内仍然属于冗余的修饰符目标谓词表。 */
internal val redundantTargetMap: Map<CjKeywordToken, ModifierTargetPredicate> = mapOf(
    // interface 头本就隐含 open，显式 open 冗余；仅 interface，不含 class/struct/enum。
    OPEN_KEYWORD to ModifierTargetPredicate.headOf(DeclarationKind.INTERFACE),
)

/**
 * class-like 类型头父目标共享常量。
 *
 * 用于 `override`/`protected`/`internal`/`private` 等修饰符的父目标谓词，
 * 避免多张表手工枚举产生不一致（历史上 `override` 漏 `EXTEND` 即此类不一致）。
 *
 * 含 `EXTEND`：与 `possibleTargetMap[OVERRIDE]` 允许 extend 成员 override 对齐——
 * extend 内写 override 在目标维度和父目标维度应同等允许。
 */
private val CLASS_LIKE_PARENTS: Set<DeclarationKind> = setOf(
    DeclarationKind.CLASS,
    DeclarationKind.STRUCT,
    DeclarationKind.INTERFACE,
    DeclarationKind.ENUM,
    DeclarationKind.EXTEND,
    DeclarationKind.ENUM_CONSTRUCTOR,
)

/**
 * 允许 `private`/`internal` 成员的 class-like 容器集合——**不含 INTERFACE**。
 *
 * 仓颉语义里 interface 成员默认 public，`private`/`internal` 修饰符对 interface 成员无意义，
 * 应由 [possibleParentTargetPredicateMap] 的 `PRIVATE_KEYWORD`/`INTERNAL_KEYWORD` 谓词排除 INTERFACE 头，
 * 致使 `checkParent` 报 `WRONG_MODIFIER_CONTAINING_DECLARATION`。
 *
 * 不直接改 [CLASS_LIKE_PARENTS]——那条常量被 `OVERRIDE`/`PROTECTED` 共享，
 * interface 成员 override 是合法路径，不能误伤。
 */
private val PRIVATE_OR_INTERNAL_PARENTS: Set<DeclarationKind> = setOf(
    DeclarationKind.CLASS,
    DeclarationKind.STRUCT,
    DeclarationKind.ENUM,
    DeclarationKind.EXTEND,
    DeclarationKind.ENUM_CONSTRUCTOR,
)

/** 每个修饰符允许出现的父声明目标谓词表。 */
internal val possibleParentTargetPredicateMap: Map<CjKeywordToken, ModifierTargetPredicate> = mapOf(
    OVERRIDE_KEYWORD to ModifierTargetPredicate.headOf(*CLASS_LIKE_PARENTS.toTypedArray()),
    PROTECTED_KEYWORD to ModifierTargetPredicate.anyOf(
        ModifierTargetPredicate.headOf(*CLASS_LIKE_PARENTS.toTypedArray()),
        ModifierTargetPredicate.headOf(DeclarationKind.FILE),
    ),
    INTERNAL_KEYWORD to ModifierTargetPredicate.anyOf(
        ModifierTargetPredicate.headOf(*PRIVATE_OR_INTERNAL_PARENTS.toTypedArray()),
        ModifierTargetPredicate.headOf(DeclarationKind.FILE),
    ),
    PRIVATE_KEYWORD to ModifierTargetPredicate.anyOf(
        ModifierTargetPredicate.headOf(*PRIVATE_OR_INTERNAL_PARENTS.toTypedArray()),
        ModifierTargetPredicate.headOf(DeclarationKind.FILE),
    ),
)

/** 根据当前声明节点和外层 context 推导实际修饰符目标列表。 */
internal fun CheckerContext.actualTargetsFor(declaration: CfirDeclaration): List<ModifierTarget> = when (declaration) {
    is CfirClass -> listOf(ModifierTarget.head(DeclarationKind.CLASS))
    is CfirStruct -> listOf(ModifierTarget.head(DeclarationKind.STRUCT))
    is CfirInterface -> listOf(ModifierTarget.head(DeclarationKind.INTERFACE))
    is CfirEnum -> listOf(ModifierTarget.head(DeclarationKind.ENUM))
    is CfirExtend -> listOf(ModifierTarget.head(DeclarationKind.EXTEND))
    is CfirProperty -> listOf(ModifierTarget.member(DeclarationKind.PROPERTY, container = closestContainingTypeKind()))
    is CfirPatternVariable, is CfirPatternBindingVariable -> {
        if ((declaration as CfirVariable).isLocal) {
            listOf(ModifierTarget.local(DeclarationKind.VARIABLE))
        } else {
            listOf(ModifierTarget.head(DeclarationKind.VARIABLE))
        }
    }

    is CfirFieldVariable -> {
        if (declaration.isLocal) {
            listOf(ModifierTarget.local(DeclarationKind.VARIABLE))
        } else {
            listOf(ModifierTarget.member(DeclarationKind.VARIABLE, container = closestContainingTypeKind()))
        }
    }

    is CfirValueParameter -> {
        if (declaration.correspondingProperty != null) {
            listOf(
                ModifierTarget.member(DeclarationKind.PROPERTY),
                ModifierTarget.member(DeclarationKind.VARIABLE),
            )
        } else {
            listOf(ModifierTarget.member(DeclarationKind.VALUE_PARAMETER))
        }
    }

    is CfirEnumConstructor -> listOf(ModifierTarget.head(DeclarationKind.ENUM_CONSTRUCTOR))
    is CfirConstructor -> {
        if (declaration.status.isStatic) {
            listOf(ModifierTarget.head(DeclarationKind.STATIC_INITIALIZER))
        } else {
            listOf(ModifierTarget.head(DeclarationKind.CONSTRUCTOR))
        }
    }

    is CfirMacroDeclaration -> listOf(ModifierTarget.head(DeclarationKind.MACRO))
    is CfirAnonymousFunction -> listOf(ModifierTarget.local(DeclarationKind.LAMBDA))
    is CfirMainFunction -> listOf(ModifierTarget.head(DeclarationKind.FUNCTION))
    is CfirFinalizer -> {
        if (declaration.isLocal) {
            listOf(ModifierTarget.local(DeclarationKind.FUNCTION))
        } else {
            listOf(ModifierTarget.member(DeclarationKind.FUNCTION, container = closestContainingTypeKind()))
        }
    }

    is CfirNamedFunction -> {
        if (declaration.isLocal) {
            listOf(ModifierTarget.local(DeclarationKind.FUNCTION))
        } else {
            listOfModifierTargetForNonLocalFunction(closestContainingTypeKind())
        }
    }

    is CfirFunction -> listOfModifierTargetForNonLocalFunction(closestContainingTypeKind())
    is CfirTypeAlias -> listOf(ModifierTarget.head(DeclarationKind.TYPEALIAS))
    is CfirFile -> listOf(ModifierTarget.head(DeclarationKind.FILE))
    is CfirTypeParameter -> listOf(ModifierTarget.head(DeclarationKind.TYPE_PARAMETER))
    else -> emptyList()
}

/**
 * 推导非局部函数（顶层头或类型成员）的修饰符目标。
 *
 * 容器种类为空 → 顶层声明头（[Site.HEAD]）；否则 → 类型成员（[Site.MEMBER]），携带容器种类。
 * 修复点：顶层函数此前一律被错打为 `member(FUNCTION, container=null)`，
 * 致使 `headOf(...)` 谓词错过它、`memberOf(...)` 谓词误命中它，触发误报或漏报。
 */
private fun listOfModifierTargetForNonLocalFunction(container: DeclarationKind?): List<ModifierTarget> =
    if (container == null) {
        listOf(ModifierTarget.head(DeclarationKind.FUNCTION))
    } else {
        listOf(ModifierTarget.member(DeclarationKind.FUNCTION, container = container))
    }


/**
 * 根据当前声明栈推导修饰符所在父级声明的目标列表。
 *
 * 修复点：
 * - lambda（[CfirAnonymousFunction]）单列分支，输出 [DeclarationKind.LAMBDA] 而非 FUNCTION，
 *   致使诊断文案显示 `lambda` 不显示 `function`。
 * - 父声明为 null（查不到有意义外层）时返回空列表，不再兜底为 FILE——避免误把 lambda 链等无容器场景放行。
 */
internal fun CheckerContext.actualParentTargets(): List<ModifierTarget> =
    when (val parent = closestModifierContainingSymbol()) {
        is CfirClassSymbol -> listOf(ModifierTarget.head(DeclarationKind.CLASS))
        is CfirStructSymbol -> listOf(ModifierTarget.head(DeclarationKind.STRUCT))
        is CfirInterfaceSymbol -> listOf(ModifierTarget.head(DeclarationKind.INTERFACE))
        is CfirEnumSymbol -> listOf(ModifierTarget.head(DeclarationKind.ENUM))
        is CfirExtendSymbol -> listOf(ModifierTarget.head(DeclarationKind.EXTEND))

        is CfirMacroDeclarationSymbol -> listOf(ModifierTarget.head(DeclarationKind.MACRO))
        is CfirEnumConstructorSymbol -> listOf(ModifierTarget.head(DeclarationKind.ENUM_CONSTRUCTOR))
        is CfirConstructorSymbol -> listOf(ModifierTarget.head(DeclarationKind.CONSTRUCTOR))
        is CfirAnonymousFunctionSymbol -> listOf(ModifierTarget.head(DeclarationKind.LAMBDA))
        is CfirFunctionSymbol<*> -> listOf(ModifierTarget.head(DeclarationKind.FUNCTION))
        is CfirTypeAliasSymbol -> listOf(ModifierTarget.head(DeclarationKind.TYPEALIAS))
        null -> emptyList()
        else -> listOf(ModifierTarget.head(DeclarationKind.FILE))
    }

/**
 * 查找对修饰符归属有意义的最近外层声明的**符号**。
 *
 * 对齐 Kotlin `FirModifierChecker` 的 `findClosest<FirBasedSymbol<*>>`：直接复用 [findClosest]，
 * 谓词按声明节点（经 `symbol.cfir` 反解）判别——跳过主构造、property、fake source。
 *
 * @param propertyParameterMode 是否处于 property 参数修饰符的查父场景，需要越过主构造和 fake property。
 */
private fun CheckerContext.closestModifierContainingSymbol(
    propertyParameterMode: Boolean = false,
): CfirBasedSymbol<*>? =
    findClosest<CfirBasedSymbol<*>> { symbol ->
        val declaration = symbol.cfir
        if (propertyParameterMode) {
            // 对齐 Kotlin FirModifierChecker：属性参数修饰符的包含声明应越过主构造和 fake property，落到外层类型。
            declaration !is CfirProperty &&
                    !(declaration is CfirConstructor && declaration.isPrimary) &&
                    declaration.source?.kind !is CjFakeSourceElementKind
        } else {
            declaration.source?.kind !is CjFakeSourceElementKind
        }
    }

/** 返回最近外层 class-like 容器种类，用于成员目标的 container 维度。 */
private fun CheckerContext.closestContainingTypeKind(): DeclarationKind? =
    findClosestDeclaration<CfirDeclaration> { declaration ->
        declaration is CfirClassLikeDeclaration || declaration is CfirExtend
    }?.let { parent ->
        when (parent) {
            is CfirExtend -> DeclarationKind.EXTEND
            is CfirClass -> DeclarationKind.CLASS
            is CfirStruct -> DeclarationKind.STRUCT
            is CfirInterface -> DeclarationKind.INTERFACE
            is CfirEnum -> DeclarationKind.ENUM
            else -> DeclarationKind.CLASS
        }
    }

/** 返回目标列表的首个描述；列表为空时使用通用 `this` 描述。 */
internal fun List<ModifierTarget>.firstOrThisDescription(): String =
    firstOrNull()?.let { target ->
        if (target.isHead) {
            target.kind.description
        } else {
            "${target.site.name.lowercase()} ${target.kind.description}"
        }
    } ?: "this"
