package org.cangnova.cangjie.codegen.dispatcher

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirBinaryOperator
import org.cangnova.cangjie.chir.core.expression.ChirBinaryOperatorFamily
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryOperation
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherOperation
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryOperator
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirValue
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
        val operandType = function.lowerType(expression.operand.type)
        val operand = function.renderValue(expression.operand)
        val operator = ChirUnaryOperator.parse(expression.operator)
            ?: throw CodegenLoweringException(
                "unsupported unary operator '${expression.operator}'",
                expression.semanticId,
            )
        return when (operator) {
            ChirUnaryOperator.INT_NEG -> {
                requireSameType(type, operandType, expression.semanticId, "integer negation result")
                requireIntegerType(type, expression.semanticId, "integer negation")
                listOf("  $result = sub $type 0, $operand")
            }
            ChirUnaryOperator.FLOAT_NEG -> {
                requireSameType(type, operandType, expression.semanticId, "floating negation result")
                requireFloatType(type, expression.semanticId, "floating negation")
                listOf("  $result = fneg $type $operand")
            }
            ChirUnaryOperator.BIT_NOT -> {
                requireSameType(type, operandType, expression.semanticId, "bitwise not result")
                requireIntegerType(type, expression.semanticId, "bitwise not")
                listOf("  $result = xor $type $operand, -1")
            }
            ChirUnaryOperator.LOGICAL_NOT -> {
                requireSameType("i1", type, expression.semanticId, "logical not result")
                requireSameType("i1", operandType, expression.semanticId, "logical not operand")
                listOf("  $result = xor i1 $operand, true")
            }
            ChirUnaryOperator.IDENTITY -> {
                requireSameType(type, operandType, expression.semanticId, "identity result")
                when {
                    isFloatType(type) -> listOf("  $result = fadd $type $operand, 0.0")
                    isIntegerType(type) -> listOf("  $result = add $type $operand, 0")
                    else -> throw CodegenLoweringException(
                        "identity requires integer or floating LLVM type, got $type",
                        expression.semanticId,
                    )
                }
            }
        }
    }

    private fun lowerBinary(function: CGFunction, expression: ChirBinaryExpression): List<String> {
        val result = function.resultRef(expression.semanticId)
        val leftType = function.lowerType(expression.left.type)
        val rightType = function.lowerType(expression.right.type)
        val resultType = function.lowerType(expression.resultType)
        val left = function.renderValue(expression.left)
        val right = function.renderValue(expression.right)

        val operator = ChirBinaryOperator.parse(expression.operator)
            ?: throw CodegenLoweringException(
                "unsupported binary operator '${expression.operator}'",
                expression.semanticId,
            )
        requireSameType(leftType, rightType, expression.semanticId, "binary operands")
        if (operator.family == ChirBinaryOperatorFamily.COMPARISON) {
            requireSameType("i1", resultType, expression.semanticId, "comparison result")
        } else {
            requireSameType(leftType, resultType, expression.semanticId, "binary result")
        }
        val mnemonic = when (operator) {
            ChirBinaryOperator.ADD -> if (isFloatType(leftType)) "fadd" else "add"
            ChirBinaryOperator.SUB -> if (isFloatType(leftType)) "fsub" else "sub"
            ChirBinaryOperator.MUL -> if (isFloatType(leftType)) "fmul" else "mul"
            ChirBinaryOperator.SIGNED_DIV -> if (isFloatType(leftType)) "fdiv" else "sdiv"
            ChirBinaryOperator.UNSIGNED_DIV -> "udiv"
            ChirBinaryOperator.SIGNED_REM -> if (isFloatType(leftType)) "frem" else "srem"
            ChirBinaryOperator.UNSIGNED_REM -> "urem"
            ChirBinaryOperator.BIT_AND -> "and"
            ChirBinaryOperator.BIT_OR -> "or"
            ChirBinaryOperator.BIT_XOR -> "xor"
            ChirBinaryOperator.SHIFT_LEFT -> "shl"
            ChirBinaryOperator.SIGNED_SHIFT_RIGHT -> "ashr"
            ChirBinaryOperator.UNSIGNED_SHIFT_RIGHT -> "lshr"
            ChirBinaryOperator.EQUAL -> if (isFloatType(leftType)) "fcmp oeq" else "icmp eq"
            ChirBinaryOperator.NOT_EQUAL -> if (isFloatType(leftType)) "fcmp one" else "icmp ne"
            ChirBinaryOperator.SIGNED_LESS -> if (isFloatType(leftType)) "fcmp olt" else "icmp slt"
            ChirBinaryOperator.SIGNED_LESS_OR_EQUAL -> if (isFloatType(leftType)) "fcmp ole" else "icmp sle"
            ChirBinaryOperator.SIGNED_GREATER -> if (isFloatType(leftType)) "fcmp ogt" else "icmp sgt"
            ChirBinaryOperator.SIGNED_GREATER_OR_EQUAL -> if (isFloatType(leftType)) "fcmp oge" else "icmp sge"
            ChirBinaryOperator.UNSIGNED_LESS -> "icmp ult"
            ChirBinaryOperator.UNSIGNED_LESS_OR_EQUAL -> "icmp ule"
            ChirBinaryOperator.UNSIGNED_GREATER -> "icmp ugt"
            ChirBinaryOperator.UNSIGNED_GREATER_OR_EQUAL -> "icmp uge"
            ChirBinaryOperator.FLOAT_EQUAL -> "fcmp oeq"
            ChirBinaryOperator.FLOAT_NOT_EQUAL -> "fcmp one"
            ChirBinaryOperator.FLOAT_LESS -> "fcmp olt"
            ChirBinaryOperator.FLOAT_LESS_OR_EQUAL -> "fcmp ole"
            ChirBinaryOperator.FLOAT_GREATER -> "fcmp ogt"
            ChirBinaryOperator.FLOAT_GREATER_OR_EQUAL -> "fcmp oge"
        }
        return if (mnemonic.startsWith("icmp ") || mnemonic.startsWith("fcmp ")) {
            listOf("  $result = $mnemonic $leftType $left, $right")
        } else {
            listOf("  $result = $mnemonic $leftType $left, $right")
        }
    }

    private fun lowerMemory(function: CGFunction, expression: ChirMemoryExpression): List<String> {
        val operation = ChirMemoryOperation.parse(expression.operation)
            ?: throw CodegenLoweringException(
                "unsupported memory operation '${expression.operation}'",
                expression.semanticId,
            )
        val address = function.renderValue(expression.address)
        val alignmentSuffix = alignSuffixFrom(expression.address, expression.value)
        return when (operation) {
            ChirMemoryOperation.LOAD -> {
                val resultType = expression.resultType?.let(function::lowerType)
                    ?: throw CodegenLoweringException(
                        "load requires an explicit result type",
                        expression.semanticId,
                    )
                val elementType = pointeeType(function, expression.address.type, expression.semanticId)
                requireSameType(elementType, resultType, expression.semanticId, "load result")
                listOf("  ${function.resultRef(expression.semanticId)} = load $resultType, ptr $address$alignmentSuffix")
            }
            ChirMemoryOperation.STORE -> {
                val value = expression.value ?: throw CodegenLoweringException(
                    "store requires value operand",
                    expression.semanticId,
                )
                val elementType = pointeeType(function, expression.address.type, expression.semanticId)
                val valueType = function.lowerType(value.type)
                requireSameType(elementType, valueType, expression.semanticId, "store value")
                listOf("  store ${function.renderTypedValue(value)}, ptr $address$alignmentSuffix")
            }
            ChirMemoryOperation.ALLOCA -> {
                val resultType = expression.resultType ?: throw CodegenLoweringException(
                    "alloca requires a pointer or reference result type",
                    expression.semanticId,
                )
                val allocatedType = pointeeType(function, resultType, expression.semanticId)
                val countValue = expression.value ?: expression.address
                val countType = function.lowerType(countValue.type)
                requireIntegerType(countType, expression.semanticId, "alloca element count")
                val count = ", $countType ${function.renderValue(countValue)}"
                listOf("  ${function.resultRef(expression.semanticId)} = alloca $allocatedType$count$alignmentSuffix")
            }
            ChirMemoryOperation.GET_ELEMENT_PTR,
            ChirMemoryOperation.GET_ELEMENT_PTR_INBOUNDS,
            -> {
                val elementType = pointeeType(function, expression.address.type, expression.semanticId)
                val indexValue = expression.value ?: throw CodegenLoweringException(
                    "getelementptr requires an explicit index operand",
                    expression.semanticId,
                )
                val indexType = function.lowerType(indexValue.type)
                requireIntegerType(indexType, expression.semanticId, "getelementptr index")
                val index = function.renderValue(indexValue)
                val inboundsToken = if (operation == ChirMemoryOperation.GET_ELEMENT_PTR) "" else " inbounds"
                listOf(
                    "  ${function.resultRef(expression.semanticId)} = getelementptr$inboundsToken $elementType, ptr $address, $indexType $index",
                )
            }
        }
    }

    private fun lowerCall(function: CGFunction, expression: ChirCallExpression): List<String> {
        val args = expression.arguments.joinToString(", ") { function.renderCallArgument(it) }
        val callee = function.renderValue(expression.callee)
        val resultType = function.lowerType(expression.resultType)
        val callTailKind = function.callTailKind(expression.callee.attributes)
        val callingConvention = function.attributeValue(expression.callee.attributes, "calling_conv")
            ?: function.attributeValue(expression.callee.attributes, "cc")
        val callPrefix = buildString {
            if (!callTailKind.isNullOrBlank()) {
                append(callTailKind)
                append(' ')
            }
            append("call")
            if (!callingConvention.isNullOrBlank()) {
                append(' ')
                append(callingConvention)
            }
        }
        return if (resultType == "void") {
            listOf("  $callPrefix void $callee($args)")
        } else {
            listOf("  ${function.resultRef(expression.semanticId)} = $callPrefix $resultType $callee($args)")
        }
    }

    private fun lowerOther(function: CGFunction, expression: ChirOtherExpression): List<String> {
        val op = ChirOtherOperation.parse(expression.operation)
            ?: throw CodegenLoweringException(
                "unsupported other operation '${expression.operation}'",
                expression.semanticId,
            )
        val result = function.resultRef(expression.semanticId)
        val targetType = expression.resultType?.let(function::lowerType)
        val operands = expression.operands

        return when (op) {
            ChirOtherOperation.SELECT -> {
                if (operands.size != 3 || targetType == null) {
                    throw CodegenLoweringException(
                        "malformed select expression",
                        expression.semanticId,
                    )
                }
                val conditionType = function.lowerType(operands[0].type)
                val lhsType = function.lowerType(operands[1].type)
                val rhsType = function.lowerType(operands[2].type)
                requireSameType("i1", conditionType, expression.semanticId, "select condition")
                requireSameType(targetType, lhsType, expression.semanticId, "select true operand")
                requireSameType(targetType, rhsType, expression.semanticId, "select false operand")
                val cond = function.renderValue(operands[0])
                val lhs = function.renderValue(operands[1])
                val rhs = function.renderValue(operands[2])
                listOf("  $result = select i1 $cond, $targetType $lhs, $targetType $rhs")
            }
            ChirOtherOperation.BITCAST,
            ChirOtherOperation.PTRTOINT,
            ChirOtherOperation.INTTOPTR,
            ChirOtherOperation.TRUNC,
            ChirOtherOperation.ZEXT,
            ChirOtherOperation.SEXT,
            ChirOtherOperation.FPTRUNC,
            ChirOtherOperation.FPEXT,
            ChirOtherOperation.SITOFP,
            ChirOtherOperation.UITOFP,
            ChirOtherOperation.FPTOSI,
            ChirOtherOperation.FPTOUI,
            -> {
                if (operands.size != 1 || targetType == null) {
                    throw CodegenLoweringException(
                        "malformed cast expression",
                        expression.semanticId,
                    )
                }
                val source = operands.first()
                val sourceType = function.lowerType(source.type)
                val sourceValue = function.renderValue(source)
                listOf("  $result = ${castMnemonic(op)} $sourceType $sourceValue to $targetType")
            }
            ChirOtherOperation.PHI -> {
                if (operands.isEmpty() || targetType == null) {
                    throw CodegenLoweringException(
                        "malformed phi expression",
                        expression.semanticId,
                    )
                }
                val incoming = operands.map { operand ->
                    val operandType = function.lowerType(operand.type)
                    requireSameType(targetType, operandType, expression.semanticId, "phi incoming operand")
                    val predecessorRef = operand.attributes
                        .asSequence()
                        .filterIsInstance<ChirStringAttribute>()
                        .firstOrNull { it.key == "pred" }
                        ?.value
                        ?: throw CodegenLoweringException(
                            "phi operand ${operand.semanticId.value} is missing required 'pred' attribute",
                            expression.semanticId,
                        )
                    val predecessorLabel = function.resolveBlockLabel(predecessorRef)
                        ?: throw CodegenLoweringException(
                            "phi predecessor '$predecessorRef' cannot be resolved to a function block",
                            expression.semanticId,
                        )
                    "[ ${function.renderValue(operand)}, %$predecessorLabel ]"
                }
                listOf("  $result = phi $targetType ${incoming.joinToString(", ")}")
            }
            else -> throw CodegenLoweringException(
                "unsupported other operation '${expression.operation}'",
                expression.semanticId,
            )
        }
    }

    private fun castMnemonic(operation: ChirOtherOperation): String {
        return when (operation) {
            ChirOtherOperation.BITCAST -> "bitcast"
            ChirOtherOperation.PTRTOINT -> "ptrtoint"
            ChirOtherOperation.INTTOPTR -> "inttoptr"
            ChirOtherOperation.TRUNC -> "trunc"
            ChirOtherOperation.ZEXT -> "zext"
            ChirOtherOperation.SEXT -> "sext"
            ChirOtherOperation.FPTRUNC -> "fptrunc"
            ChirOtherOperation.FPEXT -> "fpext"
            ChirOtherOperation.SITOFP -> "sitofp"
            ChirOtherOperation.UITOFP -> "uitofp"
            ChirOtherOperation.FPTOSI -> "fptosi"
            ChirOtherOperation.FPTOUI -> "fptoui"
            else -> throw CodegenLoweringException(
                "operation '${operation.canonicalName}' is not an LLVM cast operation",
                null,
            )
        }
    }

    private fun isFloatType(llvmType: String): Boolean {
        return llvmType == "half" || llvmType == "float" || llvmType == "double"
    }

    private fun requireFloatType(llvmType: String, sourceId: ChirSemanticId, subject: String) {
        if (!isFloatType(llvmType)) {
            throw CodegenLoweringException(
                "$subject requires floating LLVM type, got $llvmType",
                sourceId,
            )
        }
    }

    private fun requireIntegerType(llvmType: String, sourceId: ChirSemanticId, subject: String) {
        if (!isIntegerType(llvmType)) {
            throw CodegenLoweringException(
                "$subject requires integer LLVM type, got $llvmType",
                sourceId,
            )
        }
    }

    private fun isIntegerType(llvmType: String): Boolean = llvmType.matches(integerTypeRegex)

    private fun requireSameType(expected: String, actual: String, sourceId: ChirSemanticId, subject: String) {
        if (expected != actual) {
            throw CodegenLoweringException(
                "$subject type mismatch: expected $expected, got $actual",
                sourceId,
            )
        }
    }

    private fun alignSuffixFrom(vararg values: ChirValue?): String {
        val alignValue = values.asSequence()
            .filterNotNull()
            .flatMap { value -> value.attributes.asSequence() }
            .filterIsInstance<ChirStringAttribute>()
            .firstOrNull { it.key == "align" }
            ?.value
        return if (alignValue.isNullOrBlank()) "" else ", align $alignValue"
    }

    private fun pointeeType(
        function: CGFunction,
        typeRef: ChirTypeRef,
        sourceId: ChirSemanticId,
    ): String {
        val resolved = (typeRef as? ChirResolvedTypeRef)?.type
        return when (resolved) {
            is ChirCPointerType -> function.lowerType(resolved.pointeeType)
            is ChirRefType -> function.lowerType(resolved.referencedType)
            else -> throw CodegenLoweringException(
                "memory operation requires pointer/ref type, got '${typeRef.renderName}'",
                sourceId,
            )
        }
    }

    private companion object {
        val integerTypeRegex = Regex("i\\d+")
    }
}
