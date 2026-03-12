/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.analysis.checkers.expression

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangjie.cfir.analysis.checkersComponent
import org.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangjie.cfir.expressions.*
import org.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.utils.exceptions.rethrowExceptionWithDetails
import org.cangnova.cangjie.utils.exceptions.withFirEntry

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@OptIn(CheckersComponentInternal::class)
class CfirExpressionCheckersDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
    private val checkers: CfirExpressionCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    constructor(session: CfirSession, reporter: PendingDiagnosticReporter, mppKind: MppCheckerKind) : this(
        session,
        reporter,
        when (mppKind) {
            MppCheckerKind.Common -> session.checkersComponent.commonCfirExpressionCheckers
            MppCheckerKind.Platform -> session.checkersComponent.platformCfirExpressionCheckers
        }
    )

    override fun visitElement(element: CfirElement, data: CheckerContext) {
        if (element is CfirExpression) {
            error("${element::class.simpleName} should call parent checkers inside ${this::class.simpleName}")
        }
    }

    override fun visitLiteralExpression(literalExpression: CfirLiteralExpression, data: CheckerContext) {
        checkers.allLiteralExpressionCheckers.check(literalExpression, data)
    }

    override fun visitFunctionCall(functionCall: CfirFunctionCall, data: CheckerContext) {
        checkers.allFunctionCallCheckers.check(functionCall, data)
    }

    override fun visitPropertyAccess(propertyAccess: CfirPropertyAccess, data: CheckerContext) {
        checkers.allPropertyAccessCheckers.check(propertyAccess, data)
    }

    override fun visitQualifiedAccess(qualifiedAccess: CfirQualifiedAccess, data: CheckerContext) {
        checkers.allQualifiedAccessCheckers.check(qualifiedAccess, data)
    }

    override fun visitAssignment(assignment: CfirAssignment, data: CheckerContext) {
        checkers.allAssignmentCheckers.check(assignment, data)
    }

    override fun visitBinaryOp(binaryOp: CfirBinaryOp, data: CheckerContext) {
        checkers.allBinaryOpCheckers.check(binaryOp, data)
    }

    override fun visitComparisonExpression(comparisonExpression: CfirComparisonExpression, data: CheckerContext) {
        checkers.allComparisonExpressionCheckers.check(comparisonExpression, data)
    }

    override fun visitTypeOperator(typeOperator: CfirTypeOperator, data: CheckerContext) {
        checkers.allTypeOperatorCheckers.check(typeOperator, data)
    }

    override fun visitIfExpression(ifExpression: CfirIfExpression, data: CheckerContext) {
        checkers.allIfExpressionCheckers.check(ifExpression, data)
    }

    override fun visitMatchExpression(matchExpression: CfirMatchExpression, data: CheckerContext) {
        checkers.allMatchExpressionCheckers.check(matchExpression, data)
    }

    override fun visitTryExpression(tryExpression: CfirTryExpression, data: CheckerContext) {
        checkers.allTryExpressionCheckers.check(tryExpression, data)
    }

    override fun visitThrowExpression(throwExpression: CfirThrowExpression, data: CheckerContext) {
        checkers.allThrowExpressionCheckers.check(throwExpression, data)
    }

    override fun visitReturnExpression(returnExpression: CfirReturnExpression, data: CheckerContext) {
        checkers.allReturnExpressionCheckers.check(returnExpression, data)
    }

    override fun visitJumpExpression(jumpExpression: CfirJumpExpression, data: CheckerContext) {
        checkers.allJumpExpressionCheckers.check(jumpExpression, data)
    }

    override fun visitRangeExpression(rangeExpression: CfirRangeExpression, data: CheckerContext) {
        checkers.allRangeExpressionCheckers.check(rangeExpression, data)
    }

    override fun visitSubscriptExpression(subscriptExpression: CfirSubscriptExpression, data: CheckerContext) {
        checkers.allSubscriptExpressionCheckers.check(subscriptExpression, data)
    }

    override fun visitErrorExpression(errorExpression: CfirErrorExpression, data: CheckerContext) {
        checkers.allErrorExpressionCheckers.check(errorExpression, data)
    }

    override fun visitExpression(expression: CfirExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(expression, data)
    }

    private inline fun <reified E : CfirStatement> Array<CfirExpressionChecker<E>>.check(
        element: E,
        context: CheckerContext
    ) {
        for (checker in this) {
            try {
                context(context, reporter) {
                    checker.check(element)
                }
            } catch (e: Exception) {
                rethrowExceptionWithDetails("Exception in expression checkers", e) {
                    withFirEntry("element", element)
                    context.containingFilePath?.let { withEntry("file", it) }
                }
            }
        }
    }
}
