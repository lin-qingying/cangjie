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

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class CheckersComponentInternal

class CheckersComponent : CfirSessionComponent {
    val declarationCheckers: ComposedDeclarationCheckers = ComposedDeclarationCheckers()
    val expressionCheckers: ComposedExpressionCheckers = ComposedExpressionCheckers()
    val languageVersionSettingsCheckers: LanguageVersionSettingsCheckers
        field = ComposedLanguageVersionSettingsCheckers()

    val typeCheckers: ComposedTypeCheckers = ComposedTypeCheckers()

    @OptIn(CheckersComponentInternal::class)
    fun register(checkers:DeclarationCheckers) {
        declarationCheckers.register(checkers)
    }
    @SessionConfiguration
    @OptIn( CheckersComponentInternal::class)
    fun register(checkers: LanguageVersionSettingsCheckers) {
        languageVersionSettingsCheckers.register(checkers)
    }

    @OptIn(CheckersComponentInternal::class)
    fun register(checkers:ExpressionCheckers) {
        expressionCheckers.register(checkers)
    }

    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: TypeCheckers) {
        typeCheckers.register(checkers)
    }
}

val CfirSession.checkersComponent: CheckersComponent
    get() = nullableCheckersComponent ?: error("Expected `${CheckersComponent::class}` to be registered in current session.")

val CfirSession.nullableCheckersComponent: CheckersComponent? by CfirSession.nullableSessionComponentAccessor()

