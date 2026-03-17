/*
 * Copyright 2010-2024 cangjie.
 */

package org.cangnova.cangjie.cfir.analysis

import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.analysis.checkers.CommonLanguageVersionSettingsCheckers.languageVersionSettingsCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.LanguageVersionSettingsCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.config.ComposedLanguageVersionSettingsCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.ComposedCfirDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ComposedCfirExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.type.CfirTypeCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.type.ComposedCfirTypeCheckers
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class CheckersComponentInternal

class CheckersComponent : CfirSessionComponent {
    val commonCfirDeclarationCheckers: ComposedCfirDeclarationCheckers = ComposedCfirDeclarationCheckers(CheckerDispatchKind.Common)
    val platformCfirDeclarationCheckers: ComposedCfirDeclarationCheckers = ComposedCfirDeclarationCheckers(CheckerDispatchKind.Platform)

    val commonCfirExpressionCheckers: ComposedCfirExpressionCheckers = ComposedCfirExpressionCheckers(CheckerDispatchKind.Common)
    val platformCfirExpressionCheckers: ComposedCfirExpressionCheckers = ComposedCfirExpressionCheckers(CheckerDispatchKind.Platform)
    val languageVersionSettingsCheckers: LanguageVersionSettingsCheckers
        field = ComposedLanguageVersionSettingsCheckers()

    val commonCfirTypeCheckers: ComposedCfirTypeCheckers = ComposedCfirTypeCheckers(CheckerDispatchKind.Common)
    val platformCfirTypeCheckers: ComposedCfirTypeCheckers = ComposedCfirTypeCheckers(CheckerDispatchKind.Platform)

    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: CfirDeclarationCheckers) {
        commonCfirDeclarationCheckers.register(checkers)
        platformCfirDeclarationCheckers.register(checkers)
    }
    @SessionConfiguration
    @OptIn( CheckersComponentInternal::class)
    fun register(checkers: LanguageVersionSettingsCheckers) {
        languageVersionSettingsCheckers.register(checkers)
    }

    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: CfirExpressionCheckers) {
        commonCfirExpressionCheckers.register(checkers)
        platformCfirExpressionCheckers.register(checkers)
    }

    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: CfirTypeCheckers) {
        commonCfirTypeCheckers.register(checkers)
        platformCfirTypeCheckers.register(checkers)
    }
}

val CfirSession.checkersComponent: CheckersComponent
    get() = nullableCheckersComponent ?: error("Expected `${CheckersComponent::class}` to be registered in current session.")

val CfirSession.nullableCheckersComponent: CheckersComponent? by CfirSession.nullableSessionComponentAccessor()

