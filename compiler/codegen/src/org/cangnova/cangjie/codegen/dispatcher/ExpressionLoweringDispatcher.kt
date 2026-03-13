package org.cangnova.cangjie.codegen.dispatcher

import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.cangnova.cangjie.codegen.function.CGFunction

class ExpressionLoweringDispatcher {
    fun lower(function: CGFunction, expression: ChirExpression): List<String> {
        return when (expression) {
            is ChirUnaryExpression -> lowerUnary(function, expression)
            is ChirBinaryExpression -> lowerBinary(function, expression)
            is ChirMemoryExpression -> lowerMemory(function, expression)
            is ChirCallExpression -> lowerCall(function, expression)
            is ChirOtherExpression -> lowerOther(function, expression)
            else -> throw CodegenLoweringException(
                "unsupported expression type: ${expression::class.simpleName}",
                expression.semanticId,
            )
        }
    }

    private fun lowerUnary(function: CGFunction, expression: ChirUnaryExpression): List<String> {
        val result = function.resultRef(expression.semanticId)
        val type = function.lowerType(expression.resultType)
        val operand = function.renderValue(expression.operand)
        return when (expression.operator.lowercase()) {
            "neg", "ineg" -> listOf("  $result = sub $type 0, $operand")
            "fneg" -> listOf("  $result = fneg $type $operand")
            "not", "bitnot" -> listOf("  $result = xor $type $operand, -1")
            "logical_not", "lnot" -> listOf("  $result = xor i1 $operand, true")
            "copy", "mov", "identity" -> listOf("  $result = add $type $operand, 0")
            else -> throw CodegenLoweringException(
                "unsupported unary operator '${expression.operator}'",
                expression.semanticId,
            )
        }
    }

    private fun lowerBinary(function: CGFunction, expression: ChirBinaryExpression): List<String> {
        val result = function.resultRef(expression.semanticId)
        val leftType = function.lowerType(expression.left.type)
        val left = function.renderValue(expression.left)
        val right = function.renderValue(expression.right)

        val op = expression.operator.lowercase()
        val mnemonic = when (op) {
            "add", "+", "plus" -> if (isFloatType(leftType)) "fadd" else "add"
            "sub", "-", "minus" -> if (isFloatType(leftType)) "fsub" else "sub"
            "mul", "*", "times" -> if (isFloatType(leftType)) "fmul" else "mul"
            "div", "/" -> if (isFloatType(leftType)) "fdiv" else "sdiv"
            "udiv" -> "udiv"
            "rem", "%" -> if (isFloatType(leftType)) "frem" else "srem"
            "urem" -> "urem"
            "and" -> "and"
            "or" -> "or"
            "xor" -> "xor"
            "shl" -> "shl"
            "ashr" -> "ashr"
            "lshr" -> "lshr"
            "eq", "==" -> if (isFloatType(leftType)) "fcmp oeq" else "icmp eq"
            "ne", "!=" -> if (isFloatType(leftType)) "fcmp one" else "icmp ne"
            "lt", "<", "slt" -> if (isFloatType(leftType)) "fcmp olt" else "icmp slt"
            "le", "<=", "sle" -> if (isFloatType(leftType)) "fcmp ole" else "icmp sle"
            "gt", ">", "sgt" -> if (isFloatType(leftType)) "fcmp ogt" else "icmp sgt"
            "ge", ">=", "sge" -> if (isFloatType(leftType)) "fcmp oge" else "icmp sge"
            "ult" -> "icmp ult"
            "ule" -> "icmp ule"
            "ugt" -> "icmp ugt"
            "uge" -> "icmp uge"
            "feq" -> "fcmp oeq"
            "fne" -> "fcmp one"
            "flt" -> "fcmp olt"
            "fle" -> "fcmp ole"
            "fgt" -> "fcmp ogt"
            "fge" -> "fcmp oge"
            else -> null
        }

        if (mnemonic == null) {
            throw CodegenLoweringException(
                "unsupported binary operator '${expression.operator}'",
                expression.semanticId,
            )
        }
        return if (mnemonic.startsWith("icmp ") || mnemonic.startsWith("fcmp ")) {
            listOf("  $result = $mnemonic $leftType $left, $right")
        } else {
            listOf("  $result = $mnemonic $leftType $left, $right")
        }
    }

    private fun lowerMemory(function: CGFunction, expression: ChirMemoryExpression): List<String> {
        val operation = expression.operation.lowercase()
        val address = function.renderValue(expression.address)
        return when (operation) {
            "load" -> {
                val resultType = expression.resultType?.let(function::lowerType) ?: "ptr"
                listOf("  ${function.resultRef(expression.semanticId)} = load $resultType, ptr $address")
            }
            "store" -> {
                val value = expression.value ?: throw CodegenLoweringException(
                    "store requires value operand",
                    expression.semanticId,
                )
                listOf("  store ${function.renderTypedValue(value)}, ptr $address")
            }
            "alloca" -> {
                val countValue = expression.value ?: expression.address
                val count = ", i64 ${function.renderValue(countValue)}"
                listOf("  ${function.resultRef(expression.semanticId)} = alloca i8$count")
            }
            "gep", "getelementptr", "getelementptr.inbounds" -> {
                val index = expression.value?.let(function::renderValue) ?: "0"
                listOf("  ${function.resultRef(expression.semanticId)} = getelementptr inbounds i8, ptr $address, i64 $index")
            }
            else -> throw CodegenLoweringException(
                "unsupported memory operation '${expression.operation}'",
                expression.semanticId,
            )
        }
    }

    private fun lowerCall(function: CGFunction, expression: ChirCallExpression): List<String> {
        val args = expression.arguments.joinToString(", ") { function.renderTypedValue(it) }
        val callee = function.renderValue(expression.callee)
        val resultType = function.lowerType(expression.resultType)
        return if (resultType == "void") {
            listOf("  call void $callee($args)")
        } else {
            listOf("  ${function.resultRef(expression.semanticId)} = call $resultType $callee($args)")
        }
    }

    private fun lowerOther(function: CGFunction, expression: ChirOtherExpression): List<String> {
        val op = expression.operation.lowercase()
        val result = function.resultRef(expression.semanticId)
        val targetType = expression.resultType?.let(function::lowerType)
        val operands = expression.operands

        return when (op) {
            "select" -> {
                if (operands.size < 3 || targetType == null) {
                    throw CodegenLoweringException(
                        "malformed select expression",
                        expression.semanticId,
                    )
                }
                val cond = function.renderValue(operands[0])
                val lhs = function.renderValue(operands[1])
                val rhs = function.renderValue(operands[2])
                listOf("  $result = select i1 $cond, $targetType $lhs, $targetType $rhs")
            }
            "bitcast", "ptrtoint", "inttoptr", "trunc", "zext", "sext", "fptrunc", "fpext", "sitofp", "uitofp", "fptosi", "fptoui" -> {
                if (operands.isEmpty() || targetType == null) {
                    throw CodegenLoweringException(
                        "malformed cast expression",
                        expression.semanticId,
                    )
                }
                val source = operands.first()
                val sourceType = function.lowerType(source.type)
                val sourceValue = function.renderValue(source)
                listOf("  $result = $op $sourceType $sourceValue to $targetType")
            }
            "phi" -> throw CodegenLoweringException(
                "phi lowering requires predecessor mapping and is not yet available in CHIR model",
                expression.semanticId,
            )
            else -> throw CodegenLoweringException(
                "unsupported other operation '${expression.operation}'",
                expression.semanticId,
            )
        }
    }

    private fun isFloatType(llvmType: String): Boolean {
        return llvmType == "half" || llvmType == "float" || llvmType == "double"
    }
}

