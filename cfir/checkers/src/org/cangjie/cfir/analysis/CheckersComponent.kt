/*
 * Copyright 2010-2024 cangjie.
 */

package org.cangjie.cfir.analysis

import org.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangjie.cfir.analysis.checkers.declaration.ComposedCfirDeclarationCheckers
import org.cangjie.cfir.analysis.checkers.declaration.CfirDeclarationCheckers
import org.cangjie.cfir.analysis.checkers.expression.ComposedCfirExpressionCheckers
import org.cangjie.cfir.analysis.checkers.expression.CfirExpressionCheckers
import org.cangjie.cfir.analysis.checkers.type.CfirTypeCheckers
import org.cangjie.cfir.analysis.checkers.type.ComposedCfirTypeCheckers
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.session.CfirSessionComponent

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class CheckersComponentInternal

class CheckersComponent : CfirSessionComponent {
    val commonCfirDeclarationCheckers: ComposedCfirDeclarationCheckers = ComposedCfirDeclarationCheckers(MppCheckerKind.Common)
    val platformCfirDeclarationCheckers: ComposedCfirDeclarationCheckers = ComposedCfirDeclarationCheckers(MppCheckerKind.Platform)

    val commonCfirExpressionCheckers: ComposedCfirExpressionCheckers = ComposedCfirExpressionCheckers(MppCheckerKind.Common)
    val platformCfirExpressionCheckers: ComposedCfirExpressionCheckers = ComposedCfirExpressionCheckers(MppCheckerKind.Platform)

    val commonCfirTypeCheckers: ComposedCfirTypeCheckers = ComposedCfirTypeCheckers(MppCheckerKind.Common)
    val platformCfirTypeCheckers: ComposedCfirTypeCheckers = ComposedCfirTypeCheckers(MppCheckerKind.Platform)

    @OptIn(CheckersComponentInternal::class)
    fun register(checkers: CfirDeclarationCheckers) {
        commonCfirDeclarationCheckers.register(checkers)
        platformCfirDeclarationCheckers.register(checkers)
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
