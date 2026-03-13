package org.cangnova.cangjie.codegen.dispatcher

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOperationSets
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
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
        val operand = function.renderValue(expression.operand)
        return when (parseUnaryOp(expression.operator)) {
            UnaryOp.NEG -> listOf("  $result = sub $type 0, $operand")
            UnaryOp.FNEG -> listOf("  $result = fneg $type $operand")
            UnaryOp.BIT_NOT -> listOf("  $result = xor $type $operand, -1")
            UnaryOp.LOGICAL_NOT -> listOf("  $result = xor i1 $operand, true")
            UnaryOp.IDENTITY -> listOf("  $result = add $type $operand, 0")
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

        val mnemonic = when (parseBinaryOp(expression.operator)) {
            BinaryOp.ADD -> if (isFloatType(leftType)) "fadd" else "add"
            BinaryOp.SUB -> if (isFloatType(leftType)) "fsub" else "sub"
            BinaryOp.MUL -> if (isFloatType(leftType)) "fmul" else "mul"
            BinaryOp.DIV -> if (isFloatType(leftType)) "fdiv" else "sdiv"
            BinaryOp.UDIV -> "udiv"
            BinaryOp.REM -> if (isFloatType(leftType)) "frem" else "srem"
            BinaryOp.UREM -> "urem"
            BinaryOp.AND -> "and"
            BinaryOp.OR -> "or"
            BinaryOp.XOR -> "xor"
            BinaryOp.SHL -> "shl"
            BinaryOp.ASHR -> "ashr"
            BinaryOp.LSHR -> "lshr"
            BinaryOp.EQ -> if (isFloatType(leftType)) "fcmp oeq" else "icmp eq"
            BinaryOp.NE -> if (isFloatType(leftType)) "fcmp one" else "icmp ne"
            BinaryOp.LT -> if (isFloatType(leftType)) "fcmp olt" else "icmp slt"
            BinaryOp.LE -> if (isFloatType(leftType)) "fcmp ole" else "icmp sle"
            BinaryOp.GT -> if (isFloatType(leftType)) "fcmp ogt" else "icmp sgt"
            BinaryOp.GE -> if (isFloatType(leftType)) "fcmp oge" else "icmp sge"
            BinaryOp.ULT -> "icmp ult"
            BinaryOp.ULE -> "icmp ule"
            BinaryOp.UGT -> "icmp ugt"
            BinaryOp.UGE -> "icmp uge"
            BinaryOp.FEQ -> "fcmp oeq"
            BinaryOp.FNE -> "fcmp one"
            BinaryOp.FLT -> "fcmp olt"
            BinaryOp.FLE -> "fcmp ole"
            BinaryOp.FGT -> "fcmp ogt"
            BinaryOp.FGE -> "fcmp oge"
            null -> null
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
        val operation = parseMemoryOp(expression.operation)
        val address = function.renderValue(expression.address)
        val alignmentSuffix = alignSuffixFrom(expression.address, expression.value)
        return when (operation) {
            MemoryOp.LOAD -> {
                val resultType = expression.resultType?.let(function::lowerType) ?: "ptr"
                listOf("  ${function.resultRef(expression.semanticId)} = load $resultType, ptr $address$alignmentSuffix")
            }
            MemoryOp.STORE -> {
                val value = expression.value ?: throw CodegenLoweringException(
                    "store requires value operand",
                    expression.semanticId,
                )
                listOf("  store ${function.renderTypedValue(value)}, ptr $address$alignmentSuffix")
            }
            MemoryOp.ALLOCA -> {
                val allocatedType = expression.resultType?.let { pointeeType(function, it) } ?: "i8"
                val countValue = expression.value ?: expression.address
                val countType = function.lowerType(countValue.type)
                val count = ", $countType ${function.renderValue(countValue)}"
                listOf("  ${function.resultRef(expression.semanticId)} = alloca $allocatedType$count$alignmentSuffix")
            }
            MemoryOp.GEP, MemoryOp.GEP_INBOUNDS -> {
                val elementType = pointeeType(function, expression.address.type)
                val indexValue = expression.value
                val indexType = indexValue?.let { function.lowerType(it.type) } ?: "i64"
                val index = indexValue?.let(function::renderValue) ?: "0"
                val inboundsToken = if (operation == MemoryOp.GEP) "" else " inbounds"
                listOf(
                    "  ${function.resultRef(expression.semanticId)} = getelementptr$inboundsToken $elementType, ptr $address, $indexType $index",
                )
            }
            else -> throw CodegenLoweringException(
                "unsupported memory operation '${expression.operation}'",
                expression.semanticId,
            )
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
        val op = parseOtherOp(expression.operation)
        val result = function.resultRef(expression.semanticId)
        val targetType = expression.resultType?.let(function::lowerType)
        val operands = expression.operands

        return when (op) {
            OtherOp.SELECT -> {
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
            OtherOp.BITCAST,
            OtherOp.PTRTOINT,
            OtherOp.INTTOPTR,
            OtherOp.TRUNC,
            OtherOp.ZEXT,
            OtherOp.SEXT,
            OtherOp.FPTRUNC,
            OtherOp.FPEXT,
            OtherOp.SITOFP,
            OtherOp.UITOFP,
            OtherOp.FPTOSI,
            OtherOp.FPTOUI,
            -> {
                if (operands.isEmpty() || targetType == null) {
                    throw CodegenLoweringException(
                        "malformed cast expression",
                        expression.semanticId,
                    )
                }
                val source = operands.first()
                val sourceType = function.lowerType(source.type)
                val sourceValue = function.renderValue(source)
                listOf("  $result = ${op.llvmMnemonic} $sourceType $sourceValue to $targetType")
            }
            OtherOp.PHI -> {
                if (operands.isEmpty() || targetType == null) {
                    throw CodegenLoweringException(
                        "malformed phi expression",
                        expression.semanticId,
                    )
                }
                val incoming = operands.map { operand ->
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

    private fun isFloatType(llvmType: String): Boolean {
        return llvmType == "half" || llvmType == "float" || llvmType == "double"
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

    private fun pointeeType(function: CGFunction, typeRef: org.cangnova.cangjie.chir.core.type.ChirTypeRef): String {
        val resolved = (typeRef as? ChirResolvedTypeRef)?.type
        return when (resolved) {
            is ChirCPointerType -> function.lowerType(resolved.pointeeType)
            is ChirRefType -> function.lowerType(resolved.referencedType)
            else -> "i8"
        }
    }

    private fun parseUnaryOp(raw: String): UnaryOp? {
        val normalized = raw.lowercase().trim()
        if (normalized !in ChirOperationSets.unaryOperators) return null
        return when (normalized) {
            "neg", "ineg" -> UnaryOp.NEG
            "fneg" -> UnaryOp.FNEG
            "not", "bitnot" -> UnaryOp.BIT_NOT
            "logical_not", "lnot" -> UnaryOp.LOGICAL_NOT
            "copy", "mov", "identity" -> UnaryOp.IDENTITY
            else -> null
        }
    }

    private fun parseMemoryOp(raw: String): MemoryOp? {
        val normalized = raw.lowercase().trim()
        if (normalized !in ChirOperationSets.memoryOperations) return null
        return when (normalized) {
            "load" -> MemoryOp.LOAD
            "store" -> MemoryOp.STORE
            "alloca" -> MemoryOp.ALLOCA
            "gep", "getelementptr" -> MemoryOp.GEP
            "getelementptr inbounds" -> MemoryOp.GEP_INBOUNDS
            "getelementptr.inbounds" -> MemoryOp.GEP_INBOUNDS
            else -> null
        }
    }

    private fun parseBinaryOp(raw: String): BinaryOp? {
        val normalized = raw.lowercase().trim()
        if (normalized !in ChirOperationSets.binaryOperators) return null
        return when (normalized) {
            "add", "+", "plus" -> BinaryOp.ADD
            "sub", "-", "minus" -> BinaryOp.SUB
            "mul", "*", "times" -> BinaryOp.MUL
            "div", "/" -> BinaryOp.DIV
            "udiv" -> BinaryOp.UDIV
            "rem", "%" -> BinaryOp.REM
            "urem" -> BinaryOp.UREM
            "and" -> BinaryOp.AND
            "or" -> BinaryOp.OR
            "xor" -> BinaryOp.XOR
            "shl" -> BinaryOp.SHL
            "ashr" -> BinaryOp.ASHR
            "lshr" -> BinaryOp.LSHR
            "eq", "==" -> BinaryOp.EQ
            "ne", "!=" -> BinaryOp.NE
            "lt", "<", "slt" -> BinaryOp.LT
            "le", "<=", "sle" -> BinaryOp.LE
            "gt", ">", "sgt" -> BinaryOp.GT
            "ge", ">=", "sge" -> BinaryOp.GE
            "ult" -> BinaryOp.ULT
            "ule" -> BinaryOp.ULE
            "ugt" -> BinaryOp.UGT
            "uge" -> BinaryOp.UGE
            "feq" -> BinaryOp.FEQ
            "fne" -> BinaryOp.FNE
            "flt" -> BinaryOp.FLT
            "fle" -> BinaryOp.FLE
            "fgt" -> BinaryOp.FGT
            "fge" -> BinaryOp.FGE
            else -> null
        }
    }

    private fun parseOtherOp(raw: String): OtherOp? {
        val normalized = raw.lowercase().trim()
        if (normalized !in ChirOperationSets.otherOperations) return null
        return when (normalized) {
            "select" -> OtherOp.SELECT
            "bitcast" -> OtherOp.BITCAST
            "ptrtoint" -> OtherOp.PTRTOINT
            "inttoptr" -> OtherOp.INTTOPTR
            "trunc" -> OtherOp.TRUNC
            "zext" -> OtherOp.ZEXT
            "sext" -> OtherOp.SEXT
            "fptrunc" -> OtherOp.FPTRUNC
            "fpext" -> OtherOp.FPEXT
            "sitofp" -> OtherOp.SITOFP
            "uitofp" -> OtherOp.UITOFP
            "fptosi" -> OtherOp.FPTOSI
            "fptoui" -> OtherOp.FPTOUI
            "phi" -> OtherOp.PHI
            else -> null
        }
    }

    private enum class UnaryOp {
        NEG,
        FNEG,
        BIT_NOT,
        LOGICAL_NOT,
        IDENTITY,
    }

    private enum class BinaryOp {
        ADD,
        SUB,
        MUL,
        DIV,
        UDIV,
        REM,
        UREM,
        AND,
        OR,
        XOR,
        SHL,
        ASHR,
        LSHR,
        EQ,
        NE,
        LT,
        LE,
        GT,
        GE,
        ULT,
        ULE,
        UGT,
        UGE,
        FEQ,
        FNE,
        FLT,
        FLE,
        FGT,
        FGE,
    }

    private enum class MemoryOp {
        LOAD,
        STORE,
        ALLOCA,
        GEP,
        GEP_INBOUNDS,
    }

    private enum class OtherOp(val llvmMnemonic: String) {
        SELECT("select"),
        BITCAST("bitcast"),
        PTRTOINT("ptrtoint"),
        INTTOPTR("inttoptr"),
        TRUNC("trunc"),
        ZEXT("zext"),
        SEXT("sext"),
        FPTRUNC("fptrunc"),
        FPEXT("fpext"),
        SITOFP("sitofp"),
        UITOFP("uitofp"),
        FPTOSI("fptosi"),
        FPTOUI("fptoui"),
        PHI("phi"),
    }

}
