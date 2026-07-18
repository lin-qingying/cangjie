package org.cangnova.cangjie.cfir.analysis.checkers.expression

import java.math.BigInteger
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

/**
 * 常量算术表达式求值诊断检查器。
 *
 * 该检查器补齐整数常量表达式中的除零、移位计数和结果溢出诊断。它只处理可静态求值的
 * primitive operator 调用，并避开复合赋值和下标索引等由其他检查器负责的语义区域。
 */
object CfirConstEvalArithmeticChecker : CfirFunctionCallChecker() {
    /**
     * 加法 operator 名称。
     */
    private val PLUS = OperatorNameConventions.PLUS

    /**
     * 减法 operator 名称。
     */
    private val MINUS = OperatorNameConventions.MINUS

    /**
     * 乘法 operator 名称。
     */
    private val TIMES = OperatorNameConventions.TIMES

    /**
     * 除法 operator 名称。
     */
    private val DIV = OperatorNameConventions.DIV

    /**
     * 取余 operator 名称。
     */
    private val REM = OperatorNameConventions.REM

    /**
     * 左移 operator 名称。
     */
    private val LEFT_SHIFT = OperatorNameConventions.LEFT_SHIFT

    /**
     * 右移 operator 名称。
     */
    private val RIGHT_SHIFT = OperatorNameConventions.RIGHT_SHIFT

    /**
     * 幂运算 operator 名称。
     */
    private val EXPONENTIATION = OperatorNameConventions.EXPONENTIATION

    /**
     * 当前检查器可递归常量求值的 operator 集合。
     */
    private val EVALUATABLE = setOf(PLUS, MINUS, TIMES, DIV, REM, LEFT_SHIFT, RIGHT_SHIFT, EXPONENTIATION)

    /**
     * 需要检查求值结果范围的 operator 集合。
     */
    private val OVERFLOW_REPORTING = setOf(PLUS, MINUS, TIMES, DIV, REM, LEFT_SHIFT, RIGHT_SHIFT, EXPONENTIATION)

    /**
     * 检查函数调用形式的整数常量算术表达式。
     *
     * 入口先过滤非目标 operator，再根据 operator 类型分别处理移位计数、除零和结果溢出。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        val operatorName = extractOperatorName(expression) ?: return
        if (operatorName !in EVALUATABLE) return
        if (expression.isPrimitiveCompoundAssignmentCall(context)) return
        if (context.isSubscriptIndexExpression(source)) return

        val rightExpression = expression.argumentList.arguments.singleOrNull() ?: return

        if (operatorName == LEFT_SHIFT || operatorName == RIGHT_SHIFT) {
            checkShiftConstant(expression, source, rightExpression)
            return
        }

        val right = evaluateIntegerConstantExpression(rightExpression) ?: return
        val isPrimitiveOperatorCall = expression.isResolvedPrimitiveOperatorCall(operatorName)

        if ((operatorName == DIV || operatorName == REM) && right.value == BigInteger.ZERO) {
            if (expression.hasUInt64LeftAndInt64ZeroRight()) {
                reporter.reportOn(
                    source,
                    CfirErrors.INVALID_BINARY_OPERATOR,
                    operatorName.asString(),
                    "UInt64",
                    "Int64",
                )
                return
            }
            if (isPrimitiveOperatorCall) {
                reporter.reportOn(source, CfirErrors.CONST_EVAL_DIVIDE_BY_ZERO, operatorName.asString())
                return
            }
        }

        if (operatorName !in OVERFLOW_REPORTING) return
        if (!isPrimitiveOperatorCall) return
        val result = evaluateIntegerConstantExpression(expression)?.value ?: return

        val rangeTargetType = context.expectedInitializerTypeFor(source) ?: expression.overflowRangeTypeOrNull(operatorName)
        val range = CfirIntConstantEvalUtils.rangeForLiteralTargetType(rangeTargetType) ?: return
        if (expression.hasOverflowingIntegerConstantOperand(context)) return
        if (!range.contains(result)) {
            reporter.reportOn(source, CfirErrors.CONST_EVAL_ARITHMETIC_OVERFLOW, operatorName.asString())
        }
    }

    /**
     * 检查常量移位表达式的右操作数。
     *
     * 右操作数为负数或大于等于结果类型位宽时，分别报告常量求值移位诊断。
     */
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

    /**
     * 判断表达式是否是 `UInt64 / 0_i64` 或 `%` 的特殊非法 operator 场景。
     *
     * 官方语义对超过 Int64 正上界的无后缀左操作数会先落到 UInt64，再与右侧 Int64 零形成
     * operator 不匹配，而不是普通除零。
     */
    private fun CfirFunctionCall.hasUInt64LeftAndInt64ZeroRight(): Boolean {
        val left = explicitReceiver ?: return false
        val parsedLeft = CfirIntConstantEvalUtils.parseSignedIntExpression(left) ?: return false
        if (parsedLeft.explicitSuffix != null && parsedLeft.explicitSuffix != "u64") return false
        if (parsedLeft.value <= BigInteger.valueOf(Long.MAX_VALUE)) return false
        val right = argumentList.arguments.singleOrNull() ?: return false
        val parsedRight = CfirIntConstantEvalUtils.parseSignedIntExpression(right) ?: return false
        return parsedRight.value == BigInteger.ZERO &&
                (parsedRight.explicitSuffix == null || parsedRight.explicitSuffix == "i64")
    }

    /**
     * 尝试把表达式递归求值为有符号整数字面量。
     *
     * 该函数先处理单个字面量，再处理左右操作数都可求值的 primitive operator 调用。
     */
    private fun evaluateIntegerConstantExpression(expression: CfirExpression): CfirIntConstantEvalUtils.ParsedSignedIntExpression? {
        CfirIntConstantEvalUtils.parseSignedIntExpression(expression)?.let { return it }

        val call = expression as? CfirFunctionCall ?: return null
        val operatorName = extractOperatorName(call) ?: return null
        if (operatorName !in EVALUATABLE || !call.isResolvedPrimitiveOperatorCall(operatorName)) return null
        val leftExpression = call.explicitReceiver ?: return null
        val rightExpression = call.argumentList.arguments.singleOrNull() ?: return null
        val left = evaluateIntegerConstantExpression(leftExpression) ?: return null
        val right = evaluateIntegerConstantExpression(rightExpression) ?: return null

        val value = when (operatorName) {
            PLUS -> left.value + right.value
            MINUS -> left.value - right.value
            TIMES -> left.value * right.value
            DIV -> if (right.value == BigInteger.ZERO) return null else left.value / right.value
            REM -> if (right.value == BigInteger.ZERO) return null else left.value % right.value
            LEFT_SHIFT -> shiftLeftOrNull(left.value, right.value) ?: return null
            RIGHT_SHIFT -> shiftRightOrNull(left.value, right.value) ?: return null
            EXPONENTIATION -> powOrNull(left.value, right.value) ?: return null
            else -> return null
        }
        return CfirIntConstantEvalUtils.ParsedSignedIntExpression(
            originalText = sourceTextForConstantExpression(left, operatorName, right),
            value = value,
            explicitSuffix = null,
        )
    }

    /**
     * 合成递归常量表达式的诊断展示文本。
     */
    private fun sourceTextForConstantExpression(
        left: CfirIntConstantEvalUtils.ParsedSignedIntExpression,
        operatorName: Name,
        right: CfirIntConstantEvalUtils.ParsedSignedIntExpression,
    ): String = "${left.originalText} ${operatorName.asString()} ${right.originalText}"

    /**
     * 安全执行 BigInteger 左移。
     *
     * 非法计数或超出 JVM `Int` 计数范围时返回 `null`，由调用方停止递归求值。
     */
    private fun shiftLeftOrNull(value: BigInteger, count: BigInteger): BigInteger? {
        if (count < BigInteger.ZERO || count > BigInteger.valueOf(Int.MAX_VALUE.toLong())) return null
        return value.shiftLeft(count.toInt())
    }

    /**
     * 安全执行 BigInteger 右移。
     */
    private fun shiftRightOrNull(value: BigInteger, count: BigInteger): BigInteger? {
        if (count < BigInteger.ZERO || count > BigInteger.valueOf(Int.MAX_VALUE.toLong())) return null
        return value.shiftRight(count.toInt())
    }

    /**
     * 安全执行 BigInteger 幂运算。
     */
    private fun powOrNull(value: BigInteger, exponent: BigInteger): BigInteger? {
        if (exponent < BigInteger.ZERO || exponent > BigInteger.valueOf(Int.MAX_VALUE.toLong())) return null
        return value.pow(exponent.toInt())
    }

    /**
     * 判断调用是否已解析为 primitive operator。
     *
     * 已解析引用、非 ideal primitive 结果类型或内建 primitive operator 表都可以证明当前调用属于
     * primitive 算术语义。
     */
    private fun CfirFunctionCall.isResolvedPrimitiveOperatorCall(operatorName: Name): Boolean {
        if (calleeReference is CfirResolvedNamedReference) return true
        val expressionType = coneTypeOrNull
        if (expressionType is ConePrimitiveType && !expressionType.kind.isIdeal) return true
        val receiverType = explicitReceiver?.coneTypeOrNull ?: return false
        val argumentTypes = argumentList.arguments.map { argument ->
            argument.coneTypeOrNull ?: return false
        }
        return BuiltinPrimitiveOperators.resolve(operatorName, receiverType, argumentTypes) != null
    }

    /**
     * 推导算术结果溢出应使用的目标范围类型。
     *
     * 幂运算的 ideal receiver 默认按 Int64 检查；普通算术优先使用显式后缀或 receiver 的具体类型，
     * 最后回退到表达式结果类型。
     */
    private fun CfirFunctionCall.overflowRangeTypeOrNull(operatorName: Name): ConeCangJieType? {
        if (operatorName == EXPONENTIATION) {
            val receiverType = explicitReceiver?.coneTypeOrNull
            if ((receiverType as? ConePrimitiveType)?.kind?.isIdeal == true) {
                return ConePrimitiveType.INT64
            }
        }
        if (operatorName == PLUS || operatorName == MINUS || operatorName == TIMES ||
            operatorName == DIV || operatorName == REM
        ) {
            explicitReceiver?.let(CfirIntConstantEvalUtils::parseSignedIntExpression)
                ?.explicitSuffix
                ?.let(CfirIntConstantEvalUtils::coneTypeForExplicitSuffix)
                ?.let { return it }
            val receiverType = explicitReceiver?.coneTypeOrNull as? ConePrimitiveType
            if (receiverType != null && !receiverType.kind.isIdeal) {
                return receiverType
            }
        }
        return coneTypeOrNull
    }

    /**
     * 判断当前表达式的任一整数常量子操作数是否已经溢出。
     *
     * 子表达式已溢出时外层不重复报告，保持诊断定位在最内层实际越界的常量表达式上。
     */
    private fun CfirFunctionCall.hasOverflowingIntegerConstantOperand(context: CheckerContext): Boolean {
        val operands = listOfNotNull(explicitReceiver) + argumentList.arguments
        return operands.any { operand ->
            val operandCall = operand as? CfirFunctionCall ?: return@any false
            val operandOperator = extractOperatorName(operandCall) ?: return@any false
            if (operandOperator !in OVERFLOW_REPORTING) return@any false
            if (!operandCall.isResolvedPrimitiveOperatorCall(operandOperator)) return@any false
            val operandValue = evaluateIntegerConstantExpression(operandCall)?.value ?: return@any false
            val operandSource = operandCall.source as? AbstractCjSourceElement
            val operandRangeTargetType = operandSource?.let { context.expectedInitializerTypeFor(it) }
                ?: operandCall.overflowRangeTypeOrNull(operandOperator)
            val operandRange = CfirIntConstantEvalUtils.rangeForLiteralTargetType(operandRangeTargetType)
                ?: return@any false
            !operandRange.contains(operandValue) || operandCall.hasOverflowingIntegerConstantOperand(context)
        }
    }

    /**
     * 从函数调用引用中提取 operator 名称。
     */
    private fun extractOperatorName(expression: CfirFunctionCall): Name? {
        val reference = expression.calleeReference
        return when (reference) {
            is CfirResolvedNamedReference -> reference.name
            is CfirNamedReference -> reference.name
            else -> null
        }
    }
}

/**
 * 判断源码范围是否处于下标索引表达式中。
 *
 * 下标索引的整数范围与算术常量溢出由下标语义检查负责，这里通过源码文本向左匹配最近的
 * `[` 来避免重复诊断。
 */
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

/**
 * 查找包含当前表达式的显式声明初始化目标类型。
 *
 * 只在变量、字段和模式变量拥有源码类型引用时返回目标类型，用于常量表达式按初始化目标范围检查。
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
 * 返回显式写出的类型引用对应的 cone 类型。
 *
 * 没有源码的隐式类型引用不应作为字面量范围目标。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.explicitConeTypeOrNull(): ConeCangJieType? =
    if (source != null) coneTypeOrNull else null

/**
 * 判断可空源码范围是否包含指定源码范围。
 */
private fun CjSourceElement?.contains(source: AbstractCjSourceElement): Boolean {
    val container = this as? AbstractCjSourceElement ?: return false
    return container.startOffset <= source.startOffset && source.endOffset <= container.endOffset
}

/**
 * 判断 `[` 前方是否存在可作为下标接收者的源码片段。
 */
private fun CharSequence.hasSubscriptReceiverBefore(leftBracketOffset: Int): Boolean {
    var offset = leftBracketOffset - 1
    while (offset >= 0 && this[offset].isWhitespace()) {
        offset--
    }
    if (offset < 0) return false

    val previous = this[offset]
    return previous.isLetterOrDigit() || previous == '_' || previous == ')' || previous == ']'
}
