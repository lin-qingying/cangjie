package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.diagnosticFactoryForReturnTypeMismatch
import org.cangnova.cangjie.cfir.analysis.checkers.isSubtypeForTypeMismatch
import org.cangnova.cangjie.cfir.analysis.diagnostics.specificTypeMismatchDiagnostic
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExhaustivenessStatus
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.typeContext

/**
 * 函数体尾表达式返回类型检查器。
 *
 * 对齐官方 `TypeChecker::CheckFuncBody`：显式非 Unit 返回类型才将最外层
 * body block 按返回值检查；显式 Unit 返回类型只综合分析函数体，后续插入
 * `return ()`，不把普通尾表达式强制当作 Unit 返回值。
 */
object CfirFunctionBodyTypeMismatchChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        val block = expression as? CfirBlock ?: return
        val containingFunction = context.findClosestDeclaration<CfirFunction> { it.body === block } ?: return
        if (containingFunction.returnTypeRef is CfirImplicitTypeRef) return

        if (block.statements.dropLast(1).any { it is CfirReturnExpression }) return
        val tailExpression = block.statements.lastOrNull() as? CfirExpression ?: return
        if (tailExpression.isTerminatingFunctionBodyTail()) return

        val actualType = tailExpression.coneTypeOrNull ?: return
        if (actualType is ConeErrorType) return

        val expectedType = when (containingFunction) {
            is CfirConstructor -> ConePrimitiveType.UNIT
            else -> (containingFunction.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        }
        if (expectedType is ConeErrorType) return
        if (expectedType.isUnit) return

        specificTypeMismatchDiagnostic(
            source = tailExpression.source ?: return,
            expectedType = expectedType,
            actualType = actualType,
            session = context.session,
        )?.let { diagnostic ->
            reporter.report(diagnostic, context)
            return
        }

        if (!isSubtypeForTypeMismatch(context.session, context.session.typeContext, actualType, expectedType)) {
            reporter.reportOn(
                source = tailExpression.source,
                factory = diagnosticFactoryForReturnTypeMismatch(context.session, expectedType),
                a = expectedType,
                b = actualType,
                c = false,
            )
        }
    }
}

/**
 * 函数体尾位置若被显式控制流终止，不存在需要与函数返回类型比较的隐式返回值。
 */
private fun CfirExpression.isTerminatingFunctionBodyTail(): Boolean {
    return when (this) {
        is CfirReturnExpression,
        is CfirThrowExpression,
        -> true

        is CfirBlock -> {
            val tailExpression = statements.lastOrNull() as? CfirExpression ?: return false
            tailExpression.isTerminatingFunctionBodyTail()
        }

        is CfirIfExpression -> {
            val elseBranch = elseBranch ?: return false
            thenBranch.isTerminatingFunctionBodyTail() && elseBranch.isTerminatingFunctionBodyTail()
        }

        is CfirMatchExpression -> {
            exhaustiveness is CfirMatchExhaustivenessStatus.Exhaustive &&
                    branches.isNotEmpty() &&
                    branches.all { it.body.isTerminatingFunctionBodyTail() }
        }

        is CfirTryExpression -> {
            val finallyTerminates = finallyBlock?.isTerminatingFunctionBodyTail() == true
            finallyTerminates ||
                    (tryBlock.isTerminatingFunctionBodyTail() &&
                            catches.all { it.body.isTerminatingFunctionBodyTail() } &&
                            handlers.all { it.body.isTerminatingFunctionBodyTail() })
        }

        else -> false
    }
}
