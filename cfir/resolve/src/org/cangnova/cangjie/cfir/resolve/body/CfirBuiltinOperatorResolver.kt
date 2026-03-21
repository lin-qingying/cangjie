package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.Name

/**
 * 内建操作符解析器。
 * 当常规函数调用解析失败时，它会尝试把调用识别为内建操作符，
 * 并直接给出结果类型。
 */
object CfirBuiltinOperatorResolver {

    private val ARITHMETIC_OPS = setOf(
        "plus", "minus", "times", "div", "rem", "pow",
    )

    private val BITWISE_OPS = setOf("and", "or", "xor")

    private val SHIFT_OPS = setOf("shl", "shr")

    private val COMPARISON_OPS = setOf("equal", "notEqual", "less", "greater", "lessEqual", "greaterEqual")

    private val UNARY_NUMERIC_OPS = setOf(
        "unaryMinus", "unaryPlus", "inc", "dec", "postInc", "postDec",
    )

    private val UNARY_INTEGER_OPS = setOf("inv")

    /**
     * 尝试解析一次内建操作符调用。
     */
    fun tryResolveBuiltinOperator(
        name: Name,
        receiverType: ConeCangJieType?,
        argumentTypes: List<ConeCangJieType>,
    ): ConeCangJieType? {
        if (receiverType == null) return null
        val opName = name.asString()

        return when {
            // 二元算术操作符：接收者和参数都必须是数值类型
            opName in ARITHMETIC_OPS && argumentTypes.size == 1 ->
                resolveArithmeticOp(receiverType, argumentTypes.first())

            // 二元位操作符：接收者和参数都必须是整数类型
            opName in BITWISE_OPS && argumentTypes.size == 1 ->
                resolveBitwiseOp(receiverType, argumentTypes.first())

            // 移位操作符：接收者必须是整数类型
            opName in SHIFT_OPS && argumentTypes.size == 1 ->
                resolveShiftOp(receiverType)

            // 比较操作符：两端必须是可比较的兼容类型
            opName in COMPARISON_OPS && argumentTypes.size == 1 ->
                resolveComparisonOp(receiverType, argumentTypes.first())

            // 一元数值操作符：接收者必须是数值类型
            opName in UNARY_NUMERIC_OPS && argumentTypes.isEmpty() ->
                resolveUnaryNumericOp(receiverType)

            // 一元整数操作符：接收者必须是整数类型
            opName in UNARY_INTEGER_OPS && argumentTypes.isEmpty() ->
                resolveUnaryIntegerOp(receiverType)

            // `not` 操作符：接收者必须是 Bool
            opName == "not" && argumentTypes.isEmpty() ->
                resolveNotOp(receiverType)

            else -> null
        }
    }

    private fun resolveArithmeticOp(
        receiverType: ConeCangJieType,
        argType: ConeCangJieType,
    ): ConeCangJieType? {
        if (!receiverType.isNumericType || !argType.isNumericType) return null
        return resolveNumericPromotion(receiverType, argType)
    }

    private fun resolveBitwiseOp(
        receiverType: ConeCangJieType,
        argType: ConeCangJieType,
    ): ConeCangJieType? {
        if (!receiverType.isIntegerType || !argType.isIntegerType) return null
        return resolveNumericPromotion(receiverType, argType)
    }

    private fun resolveShiftOp(receiverType: ConeCangJieType): ConeCangJieType? {
        if (!receiverType.isIntegerType) return null
        return receiverType
    }

    private fun resolveUnaryNumericOp(receiverType: ConeCangJieType): ConeCangJieType? {
        if (!receiverType.isNumericType) return null
        return receiverType
    }

    private fun resolveUnaryIntegerOp(receiverType: ConeCangJieType): ConeCangJieType? {
        if (!receiverType.isIntegerType) return null
        return receiverType
    }

    /**
     * 比较操作符的解析。
     */
    private fun resolveComparisonOp(
        receiverType: ConeCangJieType,
        argType: ConeCangJieType,
    ): ConeCangJieType? {
        // 两端都是数值类型
        if (receiverType.isNumericType && argType.isNumericType) {
            return ConePrimitiveType.BOOLEAN
        }

        // 两端都是 Bool
        if (receiverType.isBoolean && argType.isBoolean) {
            return ConePrimitiveType.BOOLEAN
        }

        // 两端都是 Rune
        if (receiverType.isRune && argType.isRune) {
            return ConePrimitiveType.BOOLEAN
        }

        // 两端都是 String
        if (receiverType.isString && argType.isString) {
            return ConePrimitiveType.BOOLEAN
        }

        // 不属于内建比较
        return null
    }

    private fun resolveNotOp(receiverType: ConeCangJieType): ConeCangJieType? {
        if (!receiverType.isBoolean) return null
        return ConePrimitiveType.BOOLEAN
    }

    /**
     * 数值类型提升与拓宽规则。
     */
    private fun resolveNumericPromotion(
        receiverType: ConeCangJieType,
        argType: ConeCangJieType,
    ): ConeCangJieType {
        val receiverIsIdeal = receiverType.isIdealType
        val argIsIdeal = argType.isIdealType

        return when {
            // IdealType 规则
            receiverIsIdeal && argIsIdeal -> receiverType
            receiverIsIdeal -> argType
            argIsIdeal -> receiverType
            // 混合宽度规则：同族中选择更宽的类型
            receiverType is ConePrimitiveType && argType is ConePrimitiveType -> {
                val wider = ConeNumericWidening.widerOf(receiverType.kind, argType.kind)
                if (wider != null) ConePrimitiveType(wider) else receiverType
            }
            // 其他情况回退到接收者类型
            else -> receiverType
        }
    }
}

