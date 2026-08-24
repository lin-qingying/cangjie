package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirTypeConversion
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * 编译期整数类型转换溢出检查。
 *
 * 数值转换表达式拥有自己的溢出语义：先递归计算其参数中的无溢出整数常量转换，
 * 再以当前目标类型的完整范围判断。这样诊断稳定地落在发生截断的整个转换表达式上，
 * 而不会错误落在其中的字面量或已成功的内层转换上。
 */
object CfirTypeConversionOverflowChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        val conversion = expression as? CfirTypeConversion ?: return
        val targetType = conversion.targetTypeRef.coneTypeOrNull as? ConePrimitiveType ?: return
        val argumentValue = conversion.argument.evaluateIntegerConstantAfterConversions() ?: return
        val targetRange = CfirIntConstantEvalUtils.rangeForLiteralTargetType(targetType) ?: return
        if (targetRange.contains(argumentValue.value)) return

        val source = conversion.source ?: return
        val sourceType = conversion.argument.coneTypeOrNull ?: return
        context.recordTypeConversionOverflow(source)
        reporter.reportOn(source, CfirErrors.TYPECAST_OVERFLOW, sourceType, targetType)
    }
}

/**
 * 计算由字面量、一元正负号和已成功的整数类型转换组成的常量表达式。
 *
 * 某一层转换溢出时返回 `null`，使外层不会基于一个不存在的转换结果重复报告。
 */
private fun CfirExpression.evaluateIntegerConstantAfterConversions(): CfirIntConstantEvalUtils.ParsedSignedIntExpression? {
    CfirIntConstantEvalUtils.parseSignedIntExpression(this)?.let { return it }

    val conversion = this as? CfirTypeConversion ?: return null
    val targetType = conversion.targetTypeRef.coneTypeOrNull as? ConePrimitiveType ?: return null
    val argumentValue = conversion.argument.evaluateIntegerConstantAfterConversions() ?: return null
    val targetRange = CfirIntConstantEvalUtils.rangeForLiteralTargetType(targetType) ?: return null
    return argumentValue.takeIf { targetRange.contains(it.value) }
}
