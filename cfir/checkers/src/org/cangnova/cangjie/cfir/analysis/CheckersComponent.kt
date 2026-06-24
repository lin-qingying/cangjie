/*
 * Copyright 2010-2024 cangjie.
 */

package org.cangnova.cangjie.cfir.analysis

import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.analysis.checkers.CommonLanguageVersionSettingsCheckers.languageVersionSettingsCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.LanguageVersionSettingsCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.config.ComposedLanguageVersionSettingsCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.ComposedDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration. DeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ComposedExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression. ExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.type. TypeCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.type.ComposedTypeCheckers
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

/** 标记只允许 checker 组件注册流程内部调用的 API。 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class CheckersComponentInternal

/** 挂载到 `CfirSession` 上的 checker 聚合组件，统一保存声明、表达式、类型和语言设置 checker。 */
class CheckersComponent : CfirSessionComponent {
    /** 当前 session 中已经注册的声明 checker 聚合器。 */
    val declarationCheckers: ComposedDeclarationCheckers = ComposedDeclarationCheckers()

    /** 当前 session 中已经注册的表达式 checker 聚合器。 */
    val expressionCheckers: ComposedExpressionCheckers = ComposedExpressionCheckers()

    /** 当前 session 中已经注册的语言版本设置 checker 聚合器。 */
    val languageVersionSettingsCheckers: LanguageVersionSettingsCheckers
        field = ComposedLanguageVersionSettingsCheckers()

    /** 当前 session 中已经注册的类型 checker 聚合器。 */
    val typeCheckers: ComposedTypeCheckers = ComposedTypeCheckers()

    /** 将一组声明 checker 注册到当前 session 的声明 checker 聚合器。 */
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers:DeclarationCheckers) {
        declarationCheckers.register(checkers)
    }

    /** 将一组语言版本设置 checker 注册到当前 session 的语言设置 checker 聚合器。 */
    @SessionConfiguration
    @OptIn( CheckersComponentInternal::class)
    fun register(checkers: LanguageVersionSettingsCheckers) {
        languageVersionSettingsCheckers.register(checkers)
    }

    /** 将一组表达式 checker 注册到当前 session 的表达式 checker 聚合器。 */
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers:ExpressionCheckers) {
        expressionCheckers.register(checkers)
    }

    /** 将一组类型 checker 注册到当前 session 的类型 checker 聚合器。 */
    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: TypeCheckers) {
        typeCheckers.register(checkers)
    }
}

/** 获取当前 session 中必须存在的 checker 组件；未注册时直接报告 session 配置错误。 */
val CfirSession.checkersComponent: CheckersComponent
    get() = nullableCheckersComponent ?: error("Expected `${CheckersComponent::class}` to be registered in current session.")

/** 获取当前 session 中可空的 checker 组件，用于注册前或可选诊断流程。 */
val CfirSession.nullableCheckersComponent: CheckersComponent? by CfirSession.nullableSessionComponentAccessor()
