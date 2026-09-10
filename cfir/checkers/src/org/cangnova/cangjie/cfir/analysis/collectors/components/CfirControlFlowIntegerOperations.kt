package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions.AND
import org.cangnova.cangjie.name.OperatorNameConventions.DIV
import org.cangnova.cangjie.name.OperatorNameConventions.EXPONENTIATION
import org.cangnova.cangjie.name.OperatorNameConventions.LEFT_SHIFT
import org.cangnova.cangjie.name.OperatorNameConventions.MINUS
import org.cangnova.cangjie.name.OperatorNameConventions.NOT
import org.cangnova.cangjie.name.OperatorNameConventions.OR
import org.cangnova.cangjie.name.OperatorNameConventions.PLUS
import org.cangnova.cangjie.name.OperatorNameConventions.REM
import org.cangnova.cangjie.name.OperatorNameConventions.RIGHT_SHIFT
import org.cangnova.cangjie.name.OperatorNameConventions.TIMES
import org.cangnova.cangjie.name.OperatorNameConventions.UNARY_MINUS
import org.cangnova.cangjie.name.OperatorNameConventions.UNARY_PLUS
import org.cangnova.cangjie.name.OperatorNameConventions.XOR
import java.math.BigInteger

/**
 * CFG 常量域中的内建整数运算，对位官方 CHIR ConstAnalysis。
 *
 * 调用方先用真实 primitive 签名确认运算；这里仅处理值和目标位宽。算术或转换越界、
 * 除零和非法位移没有正常结果，返回未知；诊断仍由相应 checker 负责。
 */
internal object CfirControlFlowIntegerOperations {
    /** 默认 throwing 溢出策略下，只有目标整数范围内的结果才能进入常量域。 */
    fun checked(value: BigInteger, type: ConePrimitiveType): BigInteger? =
        CfirIntConstantEvalUtils.rangeForLiteralTargetType(type)?.let { range -> value.takeIf(range::contains) }

    /** 一元正负号和按位取反；取反与官方 CutOffHighBits 一样按目标位宽截断。 */
    fun unary(name: Name, value: BigInteger?, type: ConePrimitiveType): BigInteger? {
        value ?: return null
        return when (name) {
            UNARY_PLUS -> checked(value, type)
            UNARY_MINUS -> checked(value.negate(), type)
            NOT -> truncate(value.not(), type)
            else -> null
        }
    }

    /** 二元整数运算；部分未知操作数只使用官方明确实现的恒定结果规则。 */
    fun binary(name: Name, left: BigInteger?, right: BigInteger?, type: ConePrimitiveType): BigInteger? {
        when (name) {
            DIV, REM -> {
                if (right == BigInteger.ZERO) return null
                if (left == BigInteger.ZERO || name == REM && right == BigInteger.ONE) return BigInteger.ZERO
            }
            TIMES -> if (left == BigInteger.ZERO || right == BigInteger.ZERO) return BigInteger.ZERO
            EXPONENTIATION -> {
                if (right == BigInteger.ZERO) return BigInteger.ONE
                if (left == BigInteger.ZERO || left == BigInteger.ONE) return left
            }
        }
        left ?: return null
        right ?: return null
        return when (name) {
            PLUS -> checked(left + right, type)
            MINUS -> checked(left - right, type)
            TIMES -> checked(left * right, type)
            DIV -> checked(left / right, type)
            REM -> checked(left % right, type)
            AND -> truncate(left.and(right), type)
            OR -> truncate(left.or(right), type)
            XOR -> truncate(left.xor(right), type)
            LEFT_SHIFT, RIGHT_SHIFT -> {
                val width = CfirIntConstantEvalUtils.bitWidthForIntegerType(type) ?: return null
                if (right.signum() < 0 || right >= BigInteger.valueOf(width.toLong())) return null
                val result = if (name == LEFT_SHIFT) left.shiftLeft(right.toInt()) else left.shiftRight(right.toInt())
                truncate(result, type)
            }
            EXPONENTIATION -> power(left, right, type)
            else -> null
        }
    }

    /** 按补码保留低位；位运算不使用算术溢出规则。 */
    private fun truncate(value: BigInteger, type: ConePrimitiveType): BigInteger? {
        val width = CfirIntConstantEvalUtils.bitWidthForIntegerType(type) ?: return null
        val range = CfirIntConstantEvalUtils.rangeForLiteralTargetType(type) ?: return null
        val modulus = BigInteger.ONE.shiftLeft(width)
        val lowBits = value.mod(modulus)
        return if (range.min.signum() < 0 && lowBits.testBit(width - 1)) lowBits - modulus else lowBits
    }

    /** 有界快速幂；每步检查真实目标范围，UInt64 指数无需收窄为 JVM Int。 */
    private fun power(base: BigInteger, exponent: BigInteger, type: ConePrimitiveType): BigInteger? {
        if (exponent.signum() < 0) return null
        var remaining = exponent
        var factor = base
        var result = BigInteger.ONE
        while (remaining.signum() > 0) {
            if (remaining.testBit(0)) result = checked(result * factor, type) ?: return null
            remaining = remaining.shiftRight(1)
            if (remaining.signum() > 0) factor = checked(factor * factor, type) ?: return null
        }
        return result
    }
}
