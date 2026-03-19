package org.cangnova.cangjie.cfir.analysis.checkers.expression

import java.math.BigInteger
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.name.Name

object CfirConstEvalArithmeticChecker : CfirFunctionCallChecker(CheckerDispatchKind.Common) {
    private val PLUS = Name.identifier("plus")
    private val MINUS = Name.identifier("minus")
    private val TIMES = Name.identifier("times")
    private val DIV = Name.identifier("div")
    private val REM = Name.identifier("rem")
    private val SUPPORTED = setOf(PLUS, MINUS, TIMES, DIV, REM)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        val operatorName = extractOperatorName(expression) ?: return
        if (operatorName !in SUPPORTED) return

        val leftLiteral = expression.explicitReceiver as? CfirLiteralExpression ?: return
        val rightLiteral = expression.arguments.singleOrNull() as? CfirLiteralExpression ?: return
        val left = CfirIntConstantEvalUtils.parseIntLiteral(leftLiteral) ?: return
        val right = CfirIntConstantEvalUtils.parseIntLiteral(rightLiteral) ?: return

        if ((operatorName == DIV || operatorName == REM) && right.value == BigInteger.ZERO) {
            reporter.reportOn(source, CfirErrors.CONST_EVAL_DIVIDE_BY_ZERO, operatorName.asString())
            return
        }

        val result = when (operatorName) {
            PLUS -> left.value + right.value
            MINUS -> left.value - right.value
            TIMES -> left.value * right.value
            DIV -> left.value / right.value
            REM -> left.value % right.value
            else -> return
        }

        val range = CfirIntConstantEvalUtils.rangeForLiteralTargetType(expression.coneTypeOrNull) ?: return
        if (!range.contains(result)) {
            reporter.reportOn(source, CfirErrors.CONST_EVAL_ARITHMETIC_OVERFLOW, operatorName.asString())
        }
    }

    private fun extractOperatorName(expression: CfirFunctionCall): Name? {
        val reference = expression.calleeReference
        return when (reference) {
            is CfirResolvedNamedReference -> reference.name
            is CfirNamedReference -> reference.name
            else -> null
        }
    }
}

