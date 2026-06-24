package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

/**
 * 无符号源码形态的整数字面量范围检查器。
 *
 * 该检查器处理普通整数字面量本身的范围，带源码正负号的一元字面量会委托给
 * [checkSourceSignedLiteral]，避免把 `-Int64.MIN_VALUE` 的内部正数字面量误报溢出。
 */
object CfirLiteralNumericOverflowChecker : CfirLiteralExpressionChecker() {
    /**
     * 检查整数字面量是否超出显式后缀、上下文目标类型或默认 Int64 范围。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirLiteralExpression) {
        if (checkSourceSignedLiteral(expression)) return
        if (context.isReceiverOfUnarySignedLiteral(expression)) return

        val source = expression.source as? AbstractCjSourceElement ?: return
        val parsed = CfirIntConstantEvalUtils.parseIntLiteral(expression) ?: return
        val suffixType = CfirIntConstantEvalUtils.coneTypeForExplicitSuffix(parsed.explicitSuffix)
        val targetType = suffixType
            ?: context.binaryDivisionOperandTargetTypeFor(source, isSigned = false)
            ?: expression.coneTypeOrNull
            ?: ConePrimitiveType.INT64
        val range = CfirIntConstantEvalUtils.rangeForExplicitSuffix(parsed.explicitSuffix)
            ?: CfirIntConstantEvalUtils.rangeForPositiveLiteralTargetType(targetType)
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

    /**
     * 检查源码中直接带 `+` / `-` 前缀的整数字面量。
     *
     * 返回 `true` 表示当前字面量已经作为带符号整体处理，调用方不应继续按无符号内部值检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSourceSignedLiteral(expression: CfirLiteralExpression): Boolean {
        val source = expression.source as? AbstractCjSourceElement ?: return false
        val sign = context.unarySignBefore(source) ?: return false
        if (context.isInsideInvalidBinaryOperator(source)) return true
        val parsed = CfirIntConstantEvalUtils.parseIntLiteral(expression) ?: return false
        val signedValue = if (sign.char == '-') parsed.value.negate() else parsed.value
        val suffixType = CfirIntConstantEvalUtils.coneTypeForExplicitSuffix(parsed.explicitSuffix)
        val targetType = suffixType
            ?: context.binaryDivisionOperandTargetTypeFor(source, isSigned = true)
            ?: context.expectedInitializerTypeFor(source)
            ?: expression.coneTypeOrNull
            ?: ConePrimitiveType.INT64
        val range = CfirIntConstantEvalUtils.rangeForExplicitSuffix(parsed.explicitSuffix)
            ?: CfirIntConstantEvalUtils.rangeForSignedLiteralTargetType(targetType)
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
    /**
     * 检查解析为一元 operator 调用的带符号整数字面量。
     *
     * 该入口覆盖源码正负号未能直接贴在 literal source 前方的场景，仍按一个带符号常量单元判断范围。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val operatorName = expression.extractOperatorName()
        if (operatorName != OperatorNameConventions.UNARY_MINUS && operatorName != OperatorNameConventions.UNARY_PLUS) return
        if (expression.argumentList.arguments.isNotEmpty()) return
        val receiver = expression.explicitReceiver as? CfirLiteralExpression ?: return
        if (context.unarySignBefore(receiver.source as? AbstractCjSourceElement) != null) return

        val source = expression.source as? AbstractCjSourceElement ?: return
        if (context.isInsideInvalidBinaryOperator(source)) return
        val parsed = CfirIntConstantEvalUtils.parseSignedIntExpression(expression) ?: return
        val suffixType = CfirIntConstantEvalUtils.coneTypeForExplicitSuffix(parsed.explicitSuffix)
        val targetType = suffixType
            ?: context.binaryDivisionOperandTargetTypeFor(source, isSigned = true)
            ?: context.expectedInitializerTypeFor(source)
            ?: expression.coneTypeOrNull
            ?: ConePrimitiveType.INT64
        val range = CfirIntConstantEvalUtils.rangeForExplicitSuffix(parsed.explicitSuffix)
            ?: CfirIntConstantEvalUtils.rangeForSignedLiteralTargetType(targetType)
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

/**
 * 查找包含当前字面量的显式声明初始化目标类型。
 */
private fun CheckerContext.expectedInitializerTypeFor(source: AbstractCjSourceElement): ConeCangJieType? {
    for (declaration in containingDeclarations.asReversed()) {
        when (declaration) {
            is CfirVariable ->
                if (declaration.initializer?.source.contains(source)) return declaration.returnTypeRef.explicitConeTypeOrNull()

            is CfirFieldVariable ->
                if (declaration.initializer?.source.contains(source)) return declaration.returnTypeRef.explicitConeTypeOrNull()

            is CfirPatternVariable ->
                if (declaration.initializer?.source.contains(source)) return declaration.returnTypeRef.explicitConeTypeOrNull()

            else -> Unit
        }
    }
    return null
}

/**
 * 返回显式类型引用的 cone 类型。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.explicitConeTypeOrNull(): ConeCangJieType? =
    if (source != null) coneTypeOrNull else null

/**
 * 判断可空源码范围是否包含指定源码范围。
 */
private fun org.cangnova.cangjie.source.CjSourceElement?.contains(source: AbstractCjSourceElement): Boolean {
    val container = this as? AbstractCjSourceElement ?: return false
    return container.startOffset <= source.startOffset && source.endOffset <= container.endOffset
}

/**
 * 判断源码范围是否位于已经解析为非法二元 operator 的调用内部。
 *
 * 这类表达式的诊断由解析错误负责，字面量范围检查不重复报告。
 */
private fun CheckerContext.isInsideInvalidBinaryOperator(source: AbstractCjSourceElement): Boolean {
    return containingElements.asReversed()
        .filterIsInstance<CfirFunctionCall>()
        .any { call ->
            val diagnostic = (call.coneTypeOrNull as? ConeErrorType)?.diagnostic as? ConeUnresolvedNameError
                ?: return@any false
            diagnostic.operator != null && call.source.contains(source)
        }
}

/**
 * 在除法或取余表达式中推导操作数字面量的目标类型。
 *
 * 右操作数固定按 Int64 检查；左操作数只有在右操作数超出 Int64 且当前 literal 是带符号场景时，
 * 才按 UInt64 特殊语义处理。
 */
private fun CheckerContext.binaryDivisionOperandTargetTypeFor(
    source: AbstractCjSourceElement,
    isSigned: Boolean,
): ConePrimitiveType? {
    val binaryCall = containingElements.asReversed()
        .filterIsInstance<CfirFunctionCall>()
        .firstOrNull { call ->
            val operatorName = call.extractOperatorName()
            (operatorName == OperatorNameConventions.DIV || operatorName == OperatorNameConventions.REM) &&
                    call.source.contains(source)
        } ?: return null

    val rightOperand = binaryCall.argumentList.arguments.singleOrNull() ?: return null
    if (rightOperand.source.contains(source)) {
        return ConePrimitiveType.INT64
    }

    val leftOperand = binaryCall.explicitReceiver ?: return null
    if (!leftOperand.source.contains(source) || !isSigned) return null

    val rightValue = CfirIntConstantEvalUtils.parseSignedIntExpression(rightOperand)?.value ?: return null
    val int64Range = CfirIntConstantEvalUtils.rangeForLiteralTargetType(ConePrimitiveType.INT64) ?: return null
    return if (!int64Range.contains(rightValue)) ConePrimitiveType.UINT64 else null
}

/**
 * 源码中位于字面量前方的一元符号。
 *
 * @property char 符号字符，只能是 `+` 或 `-`。
 * @property offset 符号在源文件中的起始偏移。
 */
private data class UnarySign(val char: Char, val offset: Int)

/**
 * 读取字面量源码范围前方紧邻的一元符号。
 *
 * 如果前一个非空白字符表明该符号属于二元表达式，则返回 `null`。
 */
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

/**
 * 判断字面量是否已经作为一元正负 operator 调用的 receiver。
 */
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

/**
 * 从函数调用引用中提取 operator 名称。
 */
private fun CfirFunctionCall.extractOperatorName(): Name? {
    val reference = calleeReference
    return when (reference) {
        is CfirResolvedNamedReference -> reference.name
        is CfirNamedReference -> reference.name
        else -> null
    }
}
