package org.cangnova.cangjie.codegen.types

import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirBinaryOperator
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException

/** 官方仓颉幂运算对应的 LLVM ABI 符号。 */
internal object LlvmPowerRuntime {
    const val FLOAT64_FLOAT64_FUNCTION = "CJ_CORE_CPow"
    const val FLOAT64_INT64_FUNCTION = "CJ_CORE_FastPowerDoubleInt64"
    const val INT64_UINT64_FUNCTION = "cangjie_fast_power_i64_u64"
}

/** 当前 LLVM module 中仓颉官方幂运算所需的 ABI 声明和整数幂实现。 */
internal data class LlvmPowerRuntimePlan(
    val declarations: List<String>,
    val functionDefinitions: List<String>,
)

/**
 * 按官方三种幂运算签名准备运行时调用和 Int64/UInt64 wrapping helper。
 */
internal fun collectLlvmPowerRuntimePlan(functions: Collection<ChirFunctionDeclaration>): LlvmPowerRuntimePlan {
    val operations = functions.asSequence()
        .flatMap { function -> function.blocks.asSequence() }
        .flatMap { block -> block.expressions.asSequence() }
        .filterIsInstance<ChirBinaryExpression>()
        .filter { ChirBinaryOperator.parse(it.operator) == ChirBinaryOperator.POWER }
        .toList()
    if (operations.isEmpty()) return LlvmPowerRuntimePlan(emptyList(), emptyList())

    var needsFloat64Pow = false
    var needsFloat64Int64Pow = false
    var needsInt64UInt64Pow = false
    operations.forEach { expression ->
        val left = (expression.left.type as? ChirResolvedTypeRef)?.type
        val right = (expression.right.type as? ChirResolvedTypeRef)?.type
        when {
            left == ChirPrimitiveType.FLOAT64 && right == ChirPrimitiveType.FLOAT64 -> needsFloat64Pow = true
            left == ChirPrimitiveType.FLOAT64 && right == ChirPrimitiveType.INT64 -> needsFloat64Int64Pow = true
            left == ChirPrimitiveType.INT64 && right == ChirPrimitiveType.UINT64 -> needsInt64UInt64Pow = true
            else -> throw CodegenLoweringException(
                "power expression has unsupported operand types ${expression.left.type.renderName} ** ${expression.right.type.renderName}",
                expression.semanticId,
            )
        }
    }

    val declarations = buildList {
        if (needsFloat64Pow) {
            add("declare double @${LlvmPowerRuntime.FLOAT64_FLOAT64_FUNCTION}(double, double)")
        }
        if (needsFloat64Int64Pow) {
            add("declare double @${LlvmPowerRuntime.FLOAT64_INT64_FUNCTION}(double, i64)")
        }
    }
    val definitions = if (needsInt64UInt64Pow) listOf(int64UInt64PowerDefinition) else emptyList()
    return LlvmPowerRuntimePlan(declarations, definitions)
}

private val int64UInt64PowerDefinition = """
    define internal i64 @${LlvmPowerRuntime.INT64_UINT64_FUNCTION}(i64 %base, i64 %exponent) {
    entry:
      %base_is_one = icmp eq i64 %base, 1
      br i1 %base_is_one, label %base_one, label %check_exponent
    base_one:
      ret i64 1
    check_exponent:
      %exponent_is_zero = icmp eq i64 %exponent, 0
      br i1 %exponent_is_zero, label %exponent_zero, label %exponent_positive
    exponent_zero:
      ret i64 1
    exponent_positive:
      %half_exponent = lshr i64 %exponent, 1
      %half_power = call i64 @${LlvmPowerRuntime.INT64_UINT64_FUNCTION}(i64 %base, i64 %half_exponent)
      %squared_power = mul i64 %half_power, %half_power
      %exponent_low_bit = and i64 %exponent, 1
      %exponent_is_odd = icmp eq i64 %exponent_low_bit, 1
      br i1 %exponent_is_odd, label %odd_exponent, label %even_exponent
    odd_exponent:
      %odd_power = mul i64 %base, %squared_power
      ret i64 %odd_power
    even_exponent:
      ret i64 %squared_power
    }
""".trimIndent()
