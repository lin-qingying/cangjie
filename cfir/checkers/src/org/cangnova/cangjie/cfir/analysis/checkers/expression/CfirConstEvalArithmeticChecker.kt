package org.cangnova.cangjie.cfir.analysis.checkers.expression

import java.math.BigInteger
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

object CfirConstEvalArithmeticChecker : CfirFunctionCallChecker() {
    private val PLUS = OperatorNameConventions.PLUS
    private val MINUS = OperatorNameConventions.MINUS
    private val TIMES = OperatorNameConventions.TIMES
    private val DIV = OperatorNameConventions.DIV
    private val REM = OperatorNameConventions.REM
    private val LEFT_SHIFT = OperatorNameConventions.LEFT_SHIFT
    private val RIGHT_SHIFT = OperatorNameConventions.RIGHT_SHIFT
    private val SUPPORTED = setOf(PLUS, MINUS, TIMES, DIV, REM, LEFT_SHIFT, RIGHT_SHIFT)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        val operatorName = extractOperatorName(expression) ?: return
        if (operatorName !in SUPPORTED) return
        if (expression.isPrimitiveCompoundAssignmentCall(context)) return
        if (context.isSubscriptIndexExpression(source)) return

        val rightExpression = expression.argumentList.arguments.singleOrNull() ?: return

        if (operatorName == LEFT_SHIFT || operatorName == RIGHT_SHIFT) {
            checkShiftConstant(expression, source, rightExpression)
            return
        }

        val leftLiteral = expression.explicitReceiver as? CfirLiteralExpression ?: return
        val left = CfirIntConstantEvalUtils.parseIntLiteral(leftLiteral) ?: return
        val right = CfirIntConstantEvalUtils.parseSignedIntExpression(rightExpression) ?: return

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

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkShiftConstant(
        expression: CfirFunctionCall,
        source: AbstractCjSourceElement,
        rightExpression: CfirExpression,
    ) {
        val right = CfirIntConstantEvalUtils.parseSignedIntExpression(rightExpression) ?: return
        if (right.value < BigInteger.ZERO) {
            reporter.reportOn(source, CfirErrors.CONST_EVAL_NEGATIVE_SHIFT_COUNT)
            return
        }

        val bitWidth = CfirIntConstantEvalUtils.bitWidthForIntegerType(expression.coneTypeOrNull) ?: return
        if (right.value >= BigInteger.valueOf(bitWidth.toLong())) {
            reporter.reportOn(source, CfirErrors.CONST_EVAL_SHIFT_COUNT_OVERFLOW)
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

private fun CheckerContext.isSubscriptIndexExpression(source: AbstractCjSourceElement): Boolean {
    val text: CharSequence = containingFileSymbol?.sourceFile?.let { sourceFile ->
        when (sourceFile) {
            is CjInMemoryTextSourceFile -> sourceFile.text
            else -> sourceFile.getContentsAsStream().reader(Charsets.UTF_8).use { it.readText() }
        }
    } ?: return false

    var bracketDepth = 0
    var offset = source.startOffset - 1
    while (offset >= 0) {
        when (text[offset]) {
            ']' -> bracketDepth++
            '[' -> {
                if (bracketDepth == 0) {
                    return text.hasSubscriptReceiverBefore(offset)
                }
                bracketDepth--
            }
            '\n', ';' -> if (bracketDepth == 0) return false
        }
        offset--
    }
    return false
}

private fun CharSequence.hasSubscriptReceiverBefore(leftBracketOffset: Int): Boolean {
    var offset = leftBracketOffset - 1
    while (offset >= 0 && this[offset].isWhitespace()) {
        offset--
    }
    if (offset < 0) return false

    val previous = this[offset]
    return previous.isLetterOrDigit() || previous == '_' || previous == ')' || previous == ']'
}
