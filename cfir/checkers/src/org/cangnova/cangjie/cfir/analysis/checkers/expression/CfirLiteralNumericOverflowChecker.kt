package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement

object CfirLiteralNumericOverflowChecker : CfirLiteralExpressionChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirLiteralExpression) {
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
}

