package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirStringLiteralValueNormalizer
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.text

/** CJMP 注解实参按字面量种类及官方解析后的值比较。 */
data class CfirCjmpLiteralConstant(
    val kind: CfirLiteralKind,
    val value: String?,
)

/** 将官方注解匹配器支持的字面量投影为稳定的比较表示。 */
fun CfirExpression.cjmpAnnotationLiteralConstant(): CfirCjmpLiteralConstant? = when (this) {
    is CfirLiteralExpression -> cjmpAnnotationLiteralConstant()
    is CfirFunctionCall -> cjmpUnaryMinusLiteralConstant()
    else -> null
}

/** 返回源码中仍保留为一元表达式的负数操作数，供 CJO 按表达式节点序列化。 */
fun CfirFunctionCall.cjmpUnaryMinusOperand(): CfirExpression? {
    if (origin != CfirFunctionCallOrigin.Operator ||
        (calleeReference as? CfirNamedReference)?.name != OperatorNameConventions.UNARY_MINUS ||
        dispatchReceiver != null ||
        argumentList.arguments.isNotEmpty() ||
        typeArguments.isNotEmpty()
    ) {
        return null
    }

    return explicitReceiver
}

private fun CfirFunctionCall.cjmpUnaryMinusLiteralConstant(): CfirCjmpLiteralConstant? {
    val operand = cjmpUnaryMinusOperand() as? CfirLiteralExpression ?: return null
    if (operand.kind != CfirLiteralKind.INT && operand.kind != CfirLiteralKind.FLOAT) return null

    val literal = operand.cjmpAnnotationLiteralConstant() ?: return null
    val value = literal.value?.takeIf { it.isNotEmpty() && !it.startsWith('-') } ?: return null
    return CfirCjmpLiteralConstant(literal.kind, "-$value")
}

private fun CfirLiteralExpression.cjmpAnnotationLiteralConstant(): CfirCjmpLiteralConstant? {
    // CJO 的 source 保留位置与调试身份；其 value 已由 loader 恢复，不能再次解析调试文本。
    val sourceText = source?.takeUnless { it is CjBinarySourceElement }?.text?.toString()
    val comparableValue = when (kind) {
        CfirLiteralKind.STRING, CfirLiteralKind.JSTRING ->
            if (sourceText == null) value?.toString()
            else CfirStringLiteralValueNormalizer.normalizeSourceLiteral(sourceText)

        CfirLiteralKind.BYTE -> sourceText ?: value?.toString()

        CfirLiteralKind.RUNE ->
            if (sourceText == null) value?.toString()
            else CfirStringLiteralValueNormalizer.normalizeRuneSourceLiteral(sourceText)

        else -> value?.toString()
    }
    if (comparableValue == null && kind != CfirLiteralKind.UNIT) return null
    return CfirCjmpLiteralConstant(kind, comparableValue)
}
