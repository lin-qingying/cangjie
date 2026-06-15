package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

object CfirLiteralNumericOverflowChecker : CfirLiteralExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirLiteralExpression) {
        if (checkSourceSignedLiteral(expression)) return
        if (context.isReceiverOfUnarySignedLiteral(expression)) return

        val source = expression.source as? AbstractCjSourceElement ?: return
        val parsed = CfirIntConstantEvalUtils.parseIntLiteral(expression) ?: return
        val suffixType = CfirIntConstantEvalUtils.coneTypeForExplicitSuffix(parsed.explicitSuffix)
        val targetType = suffixType ?: expression.coneTypeOrNull ?: ConePrimitiveType.INT64
        val range = CfirIntConstantEvalUtils.rangeForExplicitSuffix(parsed.explicitSuffix)
            ?: CfirIntConstantEvalUtils.rangeForLiteralTargetType(targetType)
            ?: return

        if (!range.contains(parsed.value)) {
            reporter.reportOn(
                source,
                CfirErrors.LITERAL_NUMERIC_OVERFLOW,
                parsed.originalText,
                targetType,
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSourceSignedLiteral(expression: CfirLiteralExpression): Boolean {
        val source = expression.source as? AbstractCjSourceElement ?: return false
        val sign = context.unarySignBefore(source) ?: return false
        val parsed = CfirIntConstantEvalUtils.parseIntLiteral(expression) ?: return false
        val signedValue = if (sign.char == '-') parsed.value.negate() else parsed.value
        val suffixType = CfirIntConstantEvalUtils.coneTypeForExplicitSuffix(parsed.explicitSuffix)
        val targetType = suffixType ?: expression.coneTypeOrNull ?: ConePrimitiveType.INT64
        val range = CfirIntConstantEvalUtils.rangeForExplicitSuffix(parsed.explicitSuffix)
            ?: CfirIntConstantEvalUtils.rangeForLiteralTargetType(targetType)
            ?: return true

        if (!range.contains(signedValue)) {
            reporter.reportOn(
                CjOffsetsOnlySourceElement(sign.offset, source.endOffset),
                CfirErrors.LITERAL_NUMERIC_OVERFLOW,
                "${sign.char}${parsed.originalText}",
                targetType,
            )
        }
        return true
    }
}

/**
 * 带符号整数字面量范围检查。
 *
 * 对齐 Kotlin FIR 对一元正负整数字面量的框架处理方式：`+1` / `-1` 不是普通
 * 二元算术溢出检查的一部分，而是一个带符号的整数字面量语义单元。仓颉官方
 * `ChkLitConstExprRange` 同样以最终带符号值判断范围，因此 `-9223372036854775808`
 * 是合法的 Int64 下界，不能先把内部正数字面量单独报溢出。
 */
object CfirSignedLiteralNumericOverflowChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val operatorName = expression.extractOperatorName()
        if (operatorName != OperatorNameConventions.UNARY_MINUS && operatorName != OperatorNameConventions.UNARY_PLUS) return
        if (expression.argumentList.arguments.isNotEmpty()) return
        val receiver = expression.explicitReceiver as? CfirLiteralExpression ?: return
        if (context.unarySignBefore(receiver.source as? AbstractCjSourceElement) != null) return

        val source = expression.source as? AbstractCjSourceElement ?: return
        val parsed = CfirIntConstantEvalUtils.parseSignedIntExpression(expression) ?: return
        val suffixType = CfirIntConstantEvalUtils.coneTypeForExplicitSuffix(parsed.explicitSuffix)
        val targetType = suffixType ?: expression.coneTypeOrNull ?: ConePrimitiveType.INT64
        val range = CfirIntConstantEvalUtils.rangeForExplicitSuffix(parsed.explicitSuffix)
            ?: CfirIntConstantEvalUtils.rangeForLiteralTargetType(targetType)
            ?: return

        if (!range.contains(parsed.value)) {
            reporter.reportOn(
                source,
                CfirErrors.LITERAL_NUMERIC_OVERFLOW,
                parsed.originalText,
                targetType,
            )
        }
    }
}

private data class UnarySign(val char: Char, val offset: Int)

private fun CheckerContext.unarySignBefore(source: AbstractCjSourceElement?): UnarySign? {
    source ?: return null
    val text: CharSequence = containingFileSymbol?.sourceFile?.let { sourceFile ->
        when (sourceFile) {
            is CjInMemoryTextSourceFile -> sourceFile.text
            else -> sourceFile.getContentsAsStream().reader(Charsets.UTF_8).use { it.readText() }
        }
    } ?: return null
    var signOffset = source.startOffset - 1
    while (signOffset >= 0 && text[signOffset].isWhitespace()) {
        signOffset--
    }
    if (signOffset < 0) return null
    val sign = text[signOffset]
    if (sign != '-' && sign != '+') return null

    var previousOffset = signOffset - 1
    while (previousOffset >= 0 && text[previousOffset].isWhitespace()) {
        previousOffset--
    }
    if (previousOffset < 0) return UnarySign(sign, signOffset)

    val previous = text[previousOffset]
    if (previous.isLetterOrDigit() || previous == '_' || previous == ')' || previous == ']' || previous == '}') {
        return null
    }
    return UnarySign(sign, signOffset)
}

private fun CheckerContext.isReceiverOfUnarySignedLiteral(expression: CfirLiteralExpression): Boolean {
    return containingElements.asReversed()
        .drop(1)
        .filterIsInstance<CfirFunctionCall>()
        .any { parent ->
            parent.explicitReceiver === expression &&
                    parent.argumentList.arguments.isEmpty() &&
                    (parent.extractOperatorName() == OperatorNameConventions.UNARY_MINUS ||
                            parent.extractOperatorName() == OperatorNameConventions.UNARY_PLUS)
        }
}

private fun CfirFunctionCall.extractOperatorName(): Name? {
    val reference = calleeReference
    return when (reference) {
        is CfirResolvedNamedReference -> reference.name
        is CfirNamedReference -> reference.name
        else -> null
    }
}
