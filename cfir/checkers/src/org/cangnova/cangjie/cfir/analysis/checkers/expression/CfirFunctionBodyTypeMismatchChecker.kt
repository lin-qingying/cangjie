package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.analysis.diagnostics.specificTypeMismatchDiagnostic
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 函数体尾表达式返回类型检查器。
 *
 * 仓颉函数允许“block 最后一条表达式即返回值”，
 * 因此当函数显式声明了返回类型时，需要把最外层 body block 的尾表达式
 * 也按返回值参与 `RETURN_TYPE_MISMATCH` 检查。
 */
object CfirFunctionBodyTypeMismatchChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        val block = expression as? CfirBlock ?: return
        val containingFunction = context.findClosestDeclaration<CfirFunction> { it.body === block } ?: return
        if (containingFunction.returnTypeRef is CfirImplicitTypeRef) return

        val tailExpression = block.statements.lastOrNull() as? CfirExpression ?: return
        if (tailExpression is CfirReturnExpression) return

        val actualType = tailExpression.coneTypeOrNull ?: return
        if (actualType is ConeErrorType) return

        val expectedType = when (containingFunction) {
            is CfirConstructor -> ConePrimitiveType.UNIT
            else -> (containingFunction.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        }
        if (expectedType is ConeErrorType) return

        specificTypeMismatchDiagnostic(
            source = tailExpression.source ?: return,
            expectedType = expectedType,
            actualType = actualType,
            session = context.session,
        )?.let { diagnostic ->
            reporter.report(diagnostic, context)
            return
        }

        if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, actualType, expectedType)) {
            reporter.reportOn(
                source = tailExpression.source,
                factory = CfirErrors.RETURN_TYPE_MISMATCH,
                a = expectedType,
                b = actualType,
                c = false,
            )
        }
    }
}
