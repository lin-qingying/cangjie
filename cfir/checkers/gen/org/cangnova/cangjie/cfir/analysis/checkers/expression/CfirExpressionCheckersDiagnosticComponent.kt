

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkersComponent
import org.cangnova.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.utils.exceptions.rethrowExceptionWithDetails
import org.cangnova.cangjie.utils.exceptions.withFirEntry

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

@OptIn(CheckersComponentInternal::class)
class CfirExpressionCheckersDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
    private val checkers: CfirExpressionCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    constructor(session: CfirSession, reporter: PendingDiagnosticReporter, dispatchKind: CheckerDispatchKind) : this(
        session,
        reporter,
        when (dispatchKind) {
            CheckerDispatchKind.Common -> session.checkersComponent.commonCfirExpressionCheckers
            CheckerDispatchKind.Platform -> session.checkersComponent.platformCfirExpressionCheckers
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

    override fun visitBlock(block: CfirBlock, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(block, data)
    }

    override fun visitLazyBlock(lazyBlock: CfirLazyBlock, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(lazyBlock, data)
    }

    override fun visitLazyExpression(lazyExpression: CfirLazyExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(lazyExpression, data)
    }

    override fun visitStringInterpolation(stringInterpolation: CfirStringInterpolation, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(stringInterpolation, data)
    }

    override fun visitMatchBranch(matchBranch: CfirMatchBranch, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(matchBranch, data)
    }

    override fun visitCatch(catch: CfirCatch, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(catch, data)
    }

    override fun visitLoopExpression(loopExpression: CfirLoopExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(loopExpression, data)
    }

    override fun visitForInExpression(forInExpression: CfirForInExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(forInExpression, data)
    }

    override fun visitLambdaExpression(lambdaExpression: CfirLambdaExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(lambdaExpression, data)
    }

    override fun visitArrayLiteral(arrayLiteral: CfirArrayLiteral, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(arrayLiteral, data)
    }

    override fun visitTupleLiteral(tupleLiteral: CfirTupleLiteral, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(tupleLiteral, data)
    }

    override fun visitSpawnExpression(spawnExpression: CfirSpawnExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(spawnExpression, data)
    }

    override fun visitSynchronizedExpression(synchronizedExpression: CfirSynchronizedExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(synchronizedExpression, data)
    }

    override fun visitUnsafeExpression(unsafeExpression: CfirUnsafeExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(unsafeExpression, data)
    }

    override fun visitQuoteExpression(quoteExpression: CfirQuoteExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(quoteExpression, data)
    }

    override fun visitMacroExpression(macroExpression: CfirMacroExpression, data: CheckerContext) {
        checkers.allBasicExpressionCheckers.check(macroExpression, data)
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
