package org.cangnova.cangjie.codegen.dispatcher

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirBinaryOperator
import org.cangnova.cangjie.chir.core.expression.ChirBinaryOperatorFamily
import org.cangnova.cangjie.chir.core.expression.ChirOverflowStrategy
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirConstantExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryOperation
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherOperation
import org.cangnova.cangjie.chir.core.expression.ChirIntrinsicKind
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryOperator
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirBoxType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeRelations
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirValue
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.cangnova.cangjie.codegen.function.CGFunction
import org.cangnova.cangjie.codegen.types.LlvmPowerRuntime
import org.cangnova.cangjie.codegen.types.LlvmRawArrayCopyStrategy
import org.cangnova.cangjie.codegen.types.LlvmRawArrayRuntime
import org.cangnova.cangjie.codegen.types.LlvmIntegerCastSemantics
import org.cangnova.cangjie.codegen.types.LlvmIntegerDomain
import org.cangnova.cangjie.codegen.types.LlvmOverflowExceptionRuntime
import org.cangnova.cangjie.codegen.types.LlvmStringLiteralRuntime
import org.cangnova.cangjie.codegen.types.referencedAggregateType

/**
 * CHIR 表达式到 LLVM IR 指令文本的分派器。
 */
class ExpressionLoweringDispatcher {
    /**
     * 根据 CHIR 表达式具体类型分派到对应 lowering 实现。
     */
    fun lower(function: CGFunction, expression: ChirExpression): List<String> {
        return when (expression) {
            is ChirUnaryExpression -> lowerUnary(function, expression)
            is ChirBinaryExpression -> lowerBinary(function, expression)
            is ChirMemoryExpression -> lowerMemory(function, expression)
            is ChirCallExpression -> lowerCall(function, expression)
            is ChirTupleExpression -> lowerTuple(function, expression)
            is ChirFieldExpression -> lowerField(function, expression)
            is ChirConstantExpression -> lowerConstant(function, expression)
            is ChirOtherExpression -> lowerOther(function, expression)
            else -> throw CodegenLoweringException(
                "unsupported expression type: ${expression::class.simpleName}",
                expression.semanticId,
            )
        }
    }

    /**
     * 降低一元表达式，覆盖整数取负、浮点取负、按位取反、逻辑非和 identity。
     */
    private fun lowerUnary(function: CGFunction, expression: ChirUnaryExpression): List<String> {
        val result = function.resultRef(expression)
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

    /**
     * 降低二元表达式，覆盖算术、位运算、移位和比较操作。
     */
    private fun lowerBinary(function: CGFunction, expression: ChirBinaryExpression): List<String> {
        val result = function.resultRef(expression)
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
        if (operator == ChirBinaryOperator.POWER) {
            return lowerPower(expression, result, leftType, rightType, resultType, left, right)
        }
        val rightOperand: String
        val shiftCountInstructions: List<String>
        if (operator.family == ChirBinaryOperatorFamily.SHIFT) {
            requireSameType("i64", rightType, expression.semanticId, "shift count")
            val shiftWidth = leftType.removePrefix("i").toIntOrNull()
            if (shiftWidth !in setOf(8, 16, 32, 64)) {
                throw CodegenLoweringException(
                    "shift value must lower to an 8-, 16-, 32-, or 64-bit integer; got $leftType",
                    expression.semanticId,
                )
            }
            if (shiftWidth == 64) {
                rightOperand = right
                shiftCountInstructions = emptyList()
            } else {
                rightOperand = function.intermediateRef(expression.semanticId, 0)
                shiftCountInstructions = listOf("  $rightOperand = trunc i64 $right to $leftType")
            }
        } else {
            requireSameType(leftType, rightType, expression.semanticId, "binary operands")
            rightOperand = right
            shiftCountInstructions = emptyList()
        }
        if (operator.family == ChirBinaryOperatorFamily.COMPARISON) {
            requireSameType("i1", resultType, expression.semanticId, "comparison result")
        } else {
            requireSameType(leftType, resultType, expression.semanticId, "binary result")
        }
        val mnemonic = when (operator) {
            ChirBinaryOperator.ADD -> if (isFloatType(leftType)) "fadd" else "add"
            ChirBinaryOperator.SUB -> if (isFloatType(leftType)) "fsub" else "sub"
            ChirBinaryOperator.MUL -> if (isFloatType(leftType)) "fmul" else "mul"
            ChirBinaryOperator.POWER -> throw CodegenLoweringException(
                "POWER reached LLVM instruction selection without ABI lowering",
                expression.semanticId,
            )
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
            shiftCountInstructions + "  $result = $mnemonic $leftType $left, $rightOperand"
        } else {
            shiftCountInstructions + "  $result = $mnemonic $leftType $left, $rightOperand"
        }
    }

    /** 按仓颉官方签名和运行时 ABI 降低三种受支持的幂运算。 */
    private fun lowerPower(
        expression: ChirBinaryExpression,
        result: String,
        leftType: String,
        rightType: String,
        resultType: String,
        left: String,
        right: String,
    ): List<String> {
        val leftPrimitive = (expression.left.type as? ChirResolvedTypeRef)?.type
        val rightPrimitive = (expression.right.type as? ChirResolvedTypeRef)?.type
        val resultPrimitive = (expression.resultType as? ChirResolvedTypeRef)?.type
        val signature = Triple(leftPrimitive, rightPrimitive, resultPrimitive)
        return when (signature) {
            Triple(ChirPrimitiveType.FLOAT64, ChirPrimitiveType.FLOAT64, ChirPrimitiveType.FLOAT64) -> {
                requireSameType("double", leftType, expression.semanticId, "Float64 power base")
                requireSameType("double", rightType, expression.semanticId, "Float64 power exponent")
                requireSameType("double", resultType, expression.semanticId, "Float64 power result")
                listOf(
                    "  $result = call double @${LlvmPowerRuntime.FLOAT64_FLOAT64_FUNCTION}(double $left, double $right)",
                )
            }
            Triple(ChirPrimitiveType.FLOAT64, ChirPrimitiveType.INT64, ChirPrimitiveType.FLOAT64) -> {
                requireSameType("double", leftType, expression.semanticId, "Float64 power base")
                requireSameType("i64", rightType, expression.semanticId, "Int64 power exponent")
                requireSameType("double", resultType, expression.semanticId, "Float64 power result")
                listOf(
                    "  $result = call double @${LlvmPowerRuntime.FLOAT64_INT64_FUNCTION}(double $left, i64 $right)",
                )
            }
            Triple(ChirPrimitiveType.INT64, ChirPrimitiveType.UINT64, ChirPrimitiveType.INT64) -> {
                requireSameType("i64", leftType, expression.semanticId, "Int64 power base")
                requireSameType("i64", rightType, expression.semanticId, "UInt64 power exponent")
                requireSameType("i64", resultType, expression.semanticId, "Int64 power result")
                if (expression.overflowStrategy !in setOf(
                        ChirOverflowStrategy.NOT_APPLICABLE,
                        ChirOverflowStrategy.WRAPPING,
                    )
                ) {
                    throw CodegenLoweringException(
                        "Int64 ** UInt64 LLVM lowering does not implement ${expression.overflowStrategy} overflow semantics",
                        expression.semanticId,
                    )
                }
                listOf(
                    "  $result = call i64 @${LlvmPowerRuntime.INT64_UINT64_FUNCTION}(i64 $left, i64 $right)",
                )
            }
            else -> throw CodegenLoweringException(
                "power expression has unsupported signature " +
                    "${expression.left.type.renderName} ** ${expression.right.type.renderName} -> ${expression.resultType.renderName}",
                expression.semanticId,
            )
        }
    }

    /**
     * 降低 CHIR memory 表达式为 LLVM load/store/alloca/getelementptr 指令。
     */
    private fun lowerMemory(function: CGFunction, expression: ChirMemoryExpression): List<String> {
        val operation = ChirMemoryOperation.parse(expression.operation)
            ?: throw CodegenLoweringException(
                "unsupported memory operation '${expression.operation}'",
                expression.semanticId,
            )
        if (operation in setOf(ChirMemoryOperation.STORE, ChirMemoryOperation.STORE_ELEMENT_BY_NAME) &&
            expression.value?.let { value -> (value.type as? ChirResolvedTypeRef)?.type == ChirPrimitiveType.NOTHING } == true
        ) {
            // NOTHING 没有运行时载荷；官方 CHIR 用它表示结果槽尚未写入目标类型的值。
            return emptyList()
        }
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
                listOf("  ${function.resultRef(expression)} = load $resultType, ptr $address$alignmentSuffix")
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
                listOf("  ${function.resultRef(expression)} = alloca $allocatedType$count$alignmentSuffix")
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
                    "  ${function.resultRef(expression)} = getelementptr$inboundsToken $elementType, ptr $address, $indexType $index",
                )
            }
            ChirMemoryOperation.GET_ELEMENT_BY_NAME,
            ChirMemoryOperation.STORE_ELEMENT_BY_NAME,
            -> lowerNamedMemory(function, expression, operation)
        }
    }

    /**
     * 按 CHIR nominal 字段布局将官方具名成员访问降低为 GEP 与 load/store。
     */
    private fun lowerNamedMemory(
        function: CGFunction,
        expression: ChirMemoryExpression,
        operation: ChirMemoryOperation,
    ): List<String> {
        if (expression.memberNames.isEmpty()) {
            throw CodegenLoweringException(
                "${operation.canonicalName} requires a non-empty member path",
                expression.semanticId,
            )
        }
        var aggregateType = referencedAggregateType(expression.address.type, expression.semanticId)
        var address = function.renderValue(expression.address)
        val instructions = mutableListOf<String>()
        var finalMemberType: ChirTypeRef? = null
        var finalMemberUsesStringAbi = false

        expression.memberNames.forEachIndexed { index, memberName ->
            if ((aggregateType as? ChirResolvedTypeRef)?.type is ChirClassType) {
                val payloadAddress = function.intermediateRef(
                    expression.semanticId,
                    expression.memberNames.size * 2 + index,
                )
                instructions += "  $payloadAddress = getelementptr ptr, ptr $address, i32 1"
                address = payloadAddress
            }
            val member = function.resolveAggregateMember(aggregateType, memberName, expression.semanticId)
            val memberAddress = function.intermediateRef(expression.semanticId, index)
            instructions += "  $memberAddress = getelementptr ${function.lowerType(aggregateType)}, ptr $address, i32 0, i32 ${member.index}"
            finalMemberType = member.type
            finalMemberUsesStringAbi = LlvmStringLiteralRuntime.isBackingArrayField(
                aggregateType,
                member.index,
                member.type,
            )
            if (index < expression.memberNames.lastIndex) {
                val memberType = (member.type as? ChirResolvedTypeRef)?.type
                    ?: throw CodegenLoweringException(
                        "intermediate member '${memberName}' has unresolved type ${member.type.renderName}",
                        expression.semanticId,
                    )
                when (memberType) {
                    is ChirRefType -> {
                        val dereferencedAddress = function.intermediateRef(
                            expression.semanticId,
                            expression.memberNames.size + index,
                        )
                        instructions += "  $dereferencedAddress = load ptr, ptr $memberAddress"
                        address = dereferencedAddress
                        aggregateType = memberType.referencedType
                    }
                    is ChirCPointerType -> {
                        val dereferencedAddress = function.intermediateRef(
                            expression.semanticId,
                            expression.memberNames.size + index,
                        )
                        instructions += "  $dereferencedAddress = load ptr, ptr $memberAddress"
                        address = dereferencedAddress
                        aggregateType = memberType.pointeeType
                    }
                    else -> {
                        address = memberAddress
                        aggregateType = member.type
                    }
                }
            } else {
                address = memberAddress
            }
        }

        val memberType = checkNotNull(finalMemberType)
        return when (operation) {
            ChirMemoryOperation.GET_ELEMENT_BY_NAME -> {
                val resultType = expression.resultType ?: throw CodegenLoweringException(
                    "get-element-by-name requires an explicit result type",
                    expression.semanticId,
                )
                if (!ChirTypeRelations.areEquivalent(memberType, resultType)) {
                    throw CodegenLoweringException(
                        "get-element-by-name result type ${resultType.renderName} does not match member type ${memberType.renderName}",
                        expression.semanticId,
                    )
                }
                val llvmType = function.lowerType(resultType)
                if (finalMemberUsesStringAbi) {
                    requireSameType("ptr", llvmType, expression.semanticId, "String backing-array result")
                    val storedValue = function.intermediateRef(expression.semanticId, expression.memberNames.size * 3)
                    instructions += "  $storedValue = load ${LlvmStringLiteralRuntime.BACKING_ARRAY_LLVM_TYPE}, ptr $address"
                    instructions += "  ${function.resultRef(expression)} = addrspacecast ${LlvmStringLiteralRuntime.BACKING_ARRAY_LLVM_TYPE} $storedValue to $llvmType"
                } else {
                    instructions += "  ${function.resultRef(expression)} = load $llvmType, ptr $address"
                }
                instructions
            }
            ChirMemoryOperation.STORE_ELEMENT_BY_NAME -> {
                val value = expression.value ?: throw CodegenLoweringException(
                    "store-element-by-name requires a stored value",
                    expression.semanticId,
                )
                if (!ChirTypeRelations.areEquivalent(memberType, value.type)) {
                    throw CodegenLoweringException(
                        "store-element-by-name value type ${value.type.renderName} does not match member type ${memberType.renderName}",
                        expression.semanticId,
                    )
                }
                if (finalMemberUsesStringAbi) {
                    val valueType = function.lowerType(value.type)
                    requireSameType("ptr", valueType, expression.semanticId, "String backing-array store value")
                    val storedValue = function.intermediateRef(expression.semanticId, expression.memberNames.size * 3)
                    instructions += "  $storedValue = addrspacecast ptr ${function.renderValue(value)} to ${LlvmStringLiteralRuntime.BACKING_ARRAY_LLVM_TYPE}"
                    instructions += "  store ${LlvmStringLiteralRuntime.BACKING_ARRAY_LLVM_TYPE} $storedValue, ptr $address"
                } else {
                    instructions += "  store ${function.renderTypedValue(value)}, ptr $address"
                }
                instructions
            }
            else -> throw IllegalArgumentException("${operation.canonicalName} is not a name-based memory operation")
        }
    }

    /**
     * 降低 CHIR 调用表达式为 LLVM call 指令。
     */
    private fun lowerCall(function: CGFunction, expression: ChirCallExpression): List<String> {
        val instructions = mutableListOf<String>()
        val args = expression.arguments.map(function::renderCallArgument).toMutableList()
        var nextTypeInfoTemporaryOrdinal = 16
        expression.instantiatedTypeArguments.forEach { typeArgument ->
            val typeInfo = function.lowerTypeInfoValue(typeArgument, expression.semanticId, nextTypeInfoTemporaryOrdinal)
            instructions += typeInfo.instructions
            args += "ptr ${typeInfo.value}"
            nextTypeInfoTemporaryOrdinal = typeInfo.nextTemporaryOrdinal
        }
        val memberCalleeId = when (val callee = expression.callee) {
            is ChirFunctionValue -> callee.semanticId
            is ChirImportedFunctionValue -> callee.semanticId
            else -> null
        }
        val thisType = expression.thisType
        val isStaticMemberCall = memberCalleeId != null && function.isStaticMemberFunction(memberCalleeId)
        if (thisType != null) {
            val typeInfo = function.lowerTypeInfoValue(
                thisType,
                expression.semanticId,
                nextTypeInfoTemporaryOrdinal,
            )
            instructions += typeInfo.instructions
            args += "ptr ${typeInfo.value}"
            nextTypeInfoTemporaryOrdinal = typeInfo.nextTemporaryOrdinal
            if (isStaticMemberCall) {
                val thisTypeInfo = function.lowerStaticCallThisTypeInfoValue(
                    targetFunctionId = checkNotNull(memberCalleeId),
                    semanticId = expression.semanticId,
                    firstTemporaryOrdinal = nextTypeInfoTemporaryOrdinal,
                    alreadyLoweredTypeInfo = typeInfo,
                )
                instructions += thisTypeInfo.instructions
                args += "ptr ${thisTypeInfo.value}"
            }
        } else if (memberCalleeId != null && function.isMemberFunction(memberCalleeId)) {
            throw CodegenLoweringException(
                "member call to ${expression.callee.displayName} is missing its owner TypeInfo",
                expression.semanticId,
            )
        }
        val callee = function.renderValue(expression.callee)
        val resultType = function.lowerFunctionReturnType(expression.resultType)
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
            instructions + "  $callPrefix void $callee(${args.joinToString(", ")})"
        } else {
            instructions + "  ${function.resultRef(expression)} = $callPrefix $resultType $callee(${args.joinToString(", ")})"
        }
    }

    /**
     * 按 CHIR 结果类型布局构造原生元组或结构体值。
     */
    private fun lowerTuple(function: CGFunction, expression: ChirTupleExpression): List<String> {
        val resultType = (expression.resultType as? ChirResolvedTypeRef)?.type
        val fieldTypes = when (resultType) {
            is ChirTupleType -> resultType.elementTypes
            is ChirStructType -> ChirTypeRelations.instantiatedStructFieldTypes(resultType)
            else -> throw CodegenLoweringException(
                "LLVM tuple/struct construction requires a tuple or struct result type, got ${expression.resultType.renderName}",
                expression.semanticId,
            )
        }
        if (expression.elements.size != fieldTypes.size) {
            throw CodegenLoweringException(
                "aggregate element count ${expression.elements.size} does not match result field count ${fieldTypes.size}",
                expression.semanticId,
            )
        }

        val aggregateType = function.lowerType(expression.resultType)
        if (aggregateType == "void") {
            throw CodegenLoweringException("aggregate construction cannot produce LLVM void", expression.semanticId)
        }
        if (expression.elements.isEmpty()) {
            throw CodegenLoweringException("official CHIR tuple construction requires at least one element", expression.semanticId)
        }

        val instructions = ArrayList<String>(expression.elements.size)
        var currentValue = "undef"
        expression.elements.forEachIndexed { index, element ->
            val fieldType = fieldTypes[index]
            if (!ChirTypeRelations.areEquivalent(fieldType, element.type)) {
                throw CodegenLoweringException(
                    "aggregate element #$index type ${element.type.renderName} does not match field type ${fieldType.renderName}",
                    expression.semanticId,
                )
            }
            val result = if (index == expression.elements.lastIndex) {
                function.resultRef(expression)
            } else {
                function.intermediateRef(expression.semanticId, index)
            }
            val fieldTypeName = function.lowerType(fieldType)
            val elementValue = function.renderValue(element)
            val isStringBackingArray = LlvmStringLiteralRuntime.isBackingArrayField(
                expression.resultType,
                index,
                fieldType,
            )
            val storedFieldType = if (isStringBackingArray) {
                requireSameType("ptr", fieldTypeName, expression.semanticId, "String backing-array CHIR value")
                LlvmStringLiteralRuntime.BACKING_ARRAY_LLVM_TYPE
            } else {
                fieldTypeName
            }
            val storedElementValue = if (isStringBackingArray) {
                val cast = function.intermediateRef(expression.semanticId, expression.elements.size + index)
                instructions += "  $cast = addrspacecast ptr $elementValue to $storedFieldType"
                cast
            } else {
                elementValue
            }
            instructions += "  $result = insertvalue $aggregateType $currentValue, $storedFieldType $storedElementValue, $index"
            currentValue = result
        }
        return instructions
    }

    /**
     * 降低官方 CHIR 值字段路径为 LLVM 聚合值提取。
     */
    private fun lowerField(function: CGFunction, expression: ChirFieldExpression): List<String> {
        val projection = ChirTypeRelations.projectFieldType(expression.base.type, expression.path)
            ?: throw CodegenLoweringException(
                "field path ${expression.path} is invalid for ${expression.base.type.renderName}",
                expression.semanticId,
            )
        val isBooleanEnumSelector = projection.isEnumSelector &&
            (projection.resultType as? ChirResolvedTypeRef)?.type == ChirPrimitiveType.BOOL
        if (!ChirTypeRelations.areEquivalent(projection.resultType, expression.resultType)) {
            throw CodegenLoweringException(
                "field result type ${expression.resultType.renderName} does not match projected type ${projection.resultType.renderName}",
                expression.semanticId,
            )
        }

        val aggregateType = function.lowerType(expression.base.type)
        val aggregateValue = function.renderValue(expression.base)
        val result = function.resultRef(expression)
        val indices = expression.path.joinToString(", ")
        val enumType = (expression.base.type as? ChirResolvedTypeRef)?.type as? ChirEnumType
        if (projection.isEnumSelector && enumType?.hasTrivialDiscriminantValueRepresentation == true) {
            val resultType = function.lowerType(expression.resultType)
            requireSameType(aggregateType, resultType, expression.semanticId, "trivial enum discriminant")
            return listOf("  $result = select i1 true, $resultType $aggregateValue, $resultType $aggregateValue")
        }
        if (!isBooleanEnumSelector) {
            val resultType = function.lowerType(expression.resultType)
            if (LlvmStringLiteralRuntime.isStringType(expression.base.type) &&
                expression.path == listOf(0uL) && resultType == "ptr"
            ) {
                val fieldValue = function.intermediateRef(expression.semanticId, 0)
                return listOf(
                    "  $fieldValue = extractvalue $aggregateType $aggregateValue, $indices",
                    "  ${function.resultRef(expression)} = addrspacecast ptr addrspace(1) $fieldValue to ptr",
                )
            }
            return listOf("  $result = extractvalue $aggregateType $aggregateValue, $indices")
        }

        val selectorValue = function.intermediateRef(expression.semanticId, 0)
        return listOf(
            "  $selectorValue = extractvalue $aggregateType $aggregateValue, $indices",
            "  $result = trunc i32 $selectorValue to i1",
        )
    }

    /**
     * 将官方 CHIR 常量绑定物化为 LLVM SSA 结果。
     */
    private fun lowerConstant(function: CGFunction, expression: ChirConstantExpression): List<String> {
        if ((expression.resultType as? ChirResolvedTypeRef)?.type == ChirPrimitiveType.NOTHING) {
            return emptyList()
        }
        if (!ChirTypeRelations.isOfficialChirTypeCompatible(expression.resultType, expression.value.type)) {
            throw CodegenLoweringException(
                "constant result type ${expression.resultType.renderName} differs from literal type ${expression.value.type.renderName}",
                expression.semanticId,
            )
        }
        val type = function.lowerType(expression.resultType)
        if (type == "void") return emptyList()
        val literalType = function.lowerType(expression.value.type)
        val nullToPointer = expression.value.literal.equals("null", ignoreCase = true) &&
            (expression.value.type as? ChirResolvedTypeRef)?.type == ChirPrimitiveType.NOTHING &&
            type == "ptr"
        val representableAssignment = ChirTypeRelations.isAssignable(expression.value.type, expression.resultType) &&
            literalType == type
        if (!nullToPointer && !representableAssignment) {
            throw CodegenLoweringException(
                "constant literal ${expression.value.type.renderName} cannot be represented as ${expression.resultType.renderName} " +
                    "without a value conversion",
                expression.semanticId,
            )
        }
        val value = if (nullToPointer) "null" else function.renderValue(expression.value)
        val result = function.resultRef(expression)
        return listOf("  $result = select i1 true, $type $value, $type $value")
    }

    /**
     * 降低 CHIR other 表达式中的 select、cast 和 phi 操作。
     */
    private fun lowerOther(function: CGFunction, expression: ChirOtherExpression): List<String> {
        if (expression.debugMetadata != null) return emptyList()
        val intrinsicKind = expression.intrinsicKind
        when (intrinsicKind) {
            ChirIntrinsicKind.ARRAY_BUILT_IN_COPY_TO -> return lowerArrayBuiltInCopyTo(function, expression)
            ChirIntrinsicKind.ARRAY_GET_UNCHECKED -> return lowerArrayGetUnchecked(function, expression)
            ChirIntrinsicKind.RAW_ARRAY_REFEQ -> return lowerRawArrayReferenceEquals(function, expression)
            ChirIntrinsicKind.VECTOR_COMPARE_32 -> return lowerVectorCompare32(function, expression)
            ChirIntrinsicKind.VECTOR_INDEX_BYTE_32 -> return lowerVectorIndexByte32(function, expression)
            null -> Unit
            else -> throw CodegenLoweringException(
                "unsupported official intrinsic '${intrinsicKind.canonicalName}'",
                expression.semanticId,
            )
        }
        val op = ChirOtherOperation.parse(expression.operation)
            ?: throw CodegenLoweringException(
                "unsupported other operation '${expression.operation}'",
                expression.semanticId,
            )
        val result = function.resultRef(expression)
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
            ChirOtherOperation.RAW_ARRAY_ALLOCATE -> lowerRawArrayAllocate(function, expression)
            ChirOtherOperation.RAW_ARRAY_LITERAL_INIT -> lowerRawArrayLiteralInit(expression)
            ChirOtherOperation.CANGJIE_TYPE_CAST_NA,
            ChirOtherOperation.CANGJIE_TYPE_CAST_CHECKED,
            ChirOtherOperation.CANGJIE_TYPE_CAST_WRAPPING,
            ChirOtherOperation.CANGJIE_TYPE_CAST_THROWING,
            ChirOtherOperation.CANGJIE_TYPE_CAST_SATURATING,
            -> lowerCangjieTypeCast(function, expression, op)
            ChirOtherOperation.RAW_ARRAY_INIT_BY_VALUE -> lowerRawArrayInitByValue(function, expression)
            else -> throw CodegenLoweringException(
                "unsupported other operation '${expression.operation}' with operand types [${operands.joinToString { it.type.renderName }}] and result type ${expression.resultType?.renderName ?: "<missing>"}",
                expression.semanticId,
            )
        }
    }

    /** 按官方 RawArray 布局、GC 写屏障和运行时复制尺寸限制降低 ARRAY_BUILT_IN_COPY_TO。 */
    private fun lowerArrayBuiltInCopyTo(function: CGFunction, expression: ChirOtherExpression): List<String> {
        if (expression.operands.size != 5) {
            throw CodegenLoweringException(
                "ARRAY_BUILT_IN_COPY_TO requires source, destination, source index, destination index, and length",
                expression.semanticId,
            )
        }
        val source = expression.operands[0]
        val destination = expression.operands[1]
        val sourceIndex = expression.operands[2]
        val destinationIndex = expression.operands[3]
        val elementCount = expression.operands[4]
        val sourceType = LlvmRawArrayRuntime.arrayType(source.type, expression.semanticId)
        val destinationType = LlvmRawArrayRuntime.arrayType(destination.type, expression.semanticId)
        if (sourceType.dimensions != 1u || destinationType.dimensions != 1u ||
            !ChirTypeRelations.areEquivalent(sourceType.elementType, destinationType.elementType)
        ) {
            throw CodegenLoweringException(
                "ARRAY_BUILT_IN_COPY_TO requires one-dimensional arrays with identical element types",
                expression.semanticId,
            )
        }
        listOf(sourceIndex, destinationIndex, elementCount).forEachIndexed { index, value ->
            requireSameType("i64", function.lowerType(value.type), expression.semanticId, "ARRAY_BUILT_IN_COPY_TO index/count#$index")
        }
        expression.resultType?.let { resultType ->
            val resultKind = (resultType as? ChirResolvedTypeRef)?.type
            if (resultKind != ChirPrimitiveType.UNIT && resultKind != ChirPrimitiveType.VOID) {
                throw CodegenLoweringException(
                    "ARRAY_BUILT_IN_COPY_TO must have Unit result type",
                    expression.semanticId,
                )
            }
        }

        val elementType = sourceType.elementType
        val strategy = LlvmRawArrayRuntime.copyStrategy(elementType)
        if (strategy == LlvmRawArrayCopyStrategy.GENERIC &&
            (elementType as? ChirResolvedTypeRef)?.type !is ChirGenericType
        ) {
            throw CodegenLoweringException(
                "ARRAY_BUILT_IN_COPY_TO requires runtime TypeInfo for composite generic element ${elementType.renderName}",
                expression.semanticId,
            )
        }

        val sourceArray = function.renderValue(source)
        val destinationArray = function.renderValue(destination)
        val sourceIndexValue = function.renderValue(sourceIndex)
        val destinationIndexValue = function.renderValue(destinationIndex)
        val elementCountValue = function.renderValue(elementCount)
        val instructions = mutableListOf<String>()
        val elementLlvmType = function.lowerType(elementType)
        val typeSize = function.intermediateRef(expression.semanticId, 8)

        when (strategy) {
            LlvmRawArrayCopyStrategy.GENERIC -> {
                val typeInfo = function.lowerTypeInfoValue(elementType, expression.semanticId, 16)
                instructions += typeInfo.instructions
                val sizeAddress = function.intermediateRef(expression.semanticId, 9)
                val elementSize = function.intermediateRef(expression.semanticId, 10)
                instructions += listOf(
                    "  $sizeAddress = getelementptr ${LlvmRawArrayRuntime.TYPE_INFO_TYPE_NAME}, " +
                        "ptr ${typeInfo.value}, i32 0, i32 4",
                    "  $elementSize = load i32, ptr $sizeAddress",
                    "  $typeSize = sext i32 $elementSize to i64",
                )
            }

            LlvmRawArrayCopyStrategy.REFERENCE -> {
                instructions += "  $typeSize = ptrtoint ptr getelementptr (ptr, ptr null, i32 1) to i64"
            }

            LlvmRawArrayCopyStrategy.STRUCT,
            LlvmRawArrayCopyStrategy.MEMMOVE,
            -> instructions += "  $typeSize = ptrtoint ptr getelementptr ($elementLlvmType, ptr null, i32 1) to i64"
        }

        val sourceBase = function.intermediateRef(expression.semanticId, 0)
        val sourceLength = function.intermediateRef(expression.semanticId, 1)
        val sourceData = function.intermediateRef(expression.semanticId, 2)
        val destinationBase = function.intermediateRef(expression.semanticId, 3)
        val destinationLength = function.intermediateRef(expression.semanticId, 4)
        val destinationData = function.intermediateRef(expression.semanticId, 5)
        val sourceByteIndex = function.intermediateRef(expression.semanticId, 6)
        val destinationByteIndex = function.intermediateRef(expression.semanticId, 7)
        val copyByteCount = function.intermediateRef(expression.semanticId, 11)
        val sourceArrayAddress = function.intermediateRef(expression.semanticId, 12)
        val destinationArrayAddress = function.intermediateRef(expression.semanticId, 13)
        val sourceDataAddress = function.intermediateRef(expression.semanticId, 14)
        val destinationDataAddress = function.intermediateRef(expression.semanticId, 15)
        instructions += listOf(
            "  $sourceBase = addrspacecast ptr $sourceArray to ptr addrspace(1)",
            "  $destinationBase = addrspacecast ptr $destinationArray to ptr addrspace(1)",
            "  $sourceByteIndex = mul i64 $sourceIndexValue, $typeSize",
            "  $destinationByteIndex = mul i64 $destinationIndexValue, $typeSize",
            "  $copyByteCount = mul i64 $elementCountValue, $typeSize",
        )
        if (strategy == LlvmRawArrayCopyStrategy.GENERIC) {
            val pointerSize = function.intermediateRef(expression.semanticId, 17)
            val payloadOffset = function.intermediateRef(expression.semanticId, 18)
            val sourceOffset = function.intermediateRef(expression.semanticId, 19)
            val destinationOffset = function.intermediateRef(expression.semanticId, 20)
            instructions += listOf(
                "  $pointerSize = ptrtoint ptr getelementptr (ptr, ptr null, i32 1) to i64",
                "  $payloadOffset = add i64 $pointerSize, 8",
                "  $sourceOffset = add i64 $payloadOffset, $sourceByteIndex",
                "  $destinationOffset = add i64 $payloadOffset, $destinationByteIndex",
                "  $sourceData = getelementptr i8, ptr $sourceArray, i64 $sourceOffset",
                "  $destinationData = getelementptr i8, ptr $destinationArray, i64 $destinationOffset",
            )
        } else {
            instructions += listOf(
                "  $sourceLength = getelementptr ptr, ptr $sourceArray, i32 1",
                "  $sourceDataAddress = getelementptr inbounds { { i64 }, [0 x $elementLlvmType] }, " +
                    "ptr $sourceLength, i32 0, i32 1, i64 $sourceIndexValue",
                "  $sourceData = addrspacecast ptr $sourceDataAddress to ptr addrspace(1)",
                "  $destinationLength = getelementptr ptr, ptr $destinationArray, i32 1",
                "  $destinationDataAddress = getelementptr inbounds { { i64 }, [0 x $elementLlvmType] }, " +
                    "ptr $destinationLength, i32 0, i32 1, i64 $destinationIndexValue",
                "  $destinationData = addrspacecast ptr $destinationDataAddress to ptr addrspace(1)",
            )
        }

        val iteratorAddress = function.intermediateRef(expression.semanticId, 21)
        val iterator = function.intermediateRef(expression.semanticId, 22)
        val nextBoundary = function.intermediateRef(expression.semanticId, 23)
        val hasNextChunk = function.intermediateRef(expression.semanticId, 24)
        val sourceChunk = function.intermediateRef(expression.semanticId, 27)
        val destinationChunk = function.intermediateRef(expression.semanticId, 28)
        val nextIterator = function.intermediateRef(expression.semanticId, 29)
        val remainingBytes = function.intermediateRef(expression.semanticId, 30)
        val tailSource = function.intermediateRef(expression.semanticId, 33)
        val tailDestination = function.intermediateRef(expression.semanticId, 34)
        val tailIterator = function.intermediateRef(expression.semanticId, 35)
        val conditionLabel = function.syntheticBlockLabel(expression.semanticId, "rawarray.copy.condition")
        val bodyLabel = function.syntheticBlockLabel(expression.semanticId, "rawarray.copy.body")
        val tailLabel = function.syntheticBlockLabel(expression.semanticId, "rawarray.copy.tail")
        val endLabel = function.syntheticBlockLabel(expression.semanticId, "rawarray.copy.end")
        val chunkByteCount = "2147483640"
        instructions += listOf(
            "  $iteratorAddress = alloca i64",
            "  store i64 0, ptr $iteratorAddress",
            "  br label %$conditionLabel",
            "$conditionLabel:",
            "  $iterator = load i64, ptr $iteratorAddress",
            "  $nextBoundary = add i64 $iterator, $chunkByteCount",
            "  $hasNextChunk = icmp slt i64 $nextBoundary, $copyByteCount",
            "  br i1 $hasNextChunk, label %$bodyLabel, label %$tailLabel",
            "$bodyLabel:",
            "  $sourceChunk = getelementptr i8, ptr addrspace(1) $sourceData, i64 $iterator",
            "  $destinationChunk = getelementptr i8, ptr addrspace(1) $destinationData, i64 $iterator",
        )
        instructions += lowerArrayCopyChunk(
            strategy = strategy,
            sourceBase = sourceBase,
            sourceAddress = sourceChunk,
            destinationBase = destinationBase,
            destinationAddress = destinationChunk,
            byteCount = chunkByteCount,
        )
        instructions += listOf(
            "  $nextIterator = add i64 $iterator, $chunkByteCount",
            "  store i64 $nextIterator, ptr $iteratorAddress",
            "  br label %$conditionLabel",
            "$tailLabel:",
            "  $tailIterator = load i64, ptr $iteratorAddress",
            "  $remainingBytes = sub i64 $copyByteCount, $tailIterator",
            "  $tailSource = getelementptr i8, ptr addrspace(1) $sourceData, i64 $tailIterator",
            "  $tailDestination = getelementptr i8, ptr addrspace(1) $destinationData, i64 $tailIterator",
        )
        instructions += lowerArrayCopyChunk(
            strategy = strategy,
            sourceBase = sourceBase,
            sourceAddress = tailSource,
            destinationBase = destinationBase,
            destinationAddress = tailDestination,
            byteCount = remainingBytes,
        )
        instructions += listOf("  br label %$endLabel", "$endLabel:")
        return instructions
    }

    private fun lowerArrayCopyChunk(
        strategy: LlvmRawArrayCopyStrategy,
        sourceBase: String,
        sourceAddress: String,
        destinationBase: String,
        destinationAddress: String,
        byteCount: String,
    ): List<String> = when (strategy) {
        LlvmRawArrayCopyStrategy.MEMMOVE -> listOf(
            "  call void @llvm.memmove.p1.p1.i64(ptr addrspace(1) $destinationAddress, " +
                "ptr addrspace(1) $sourceAddress, i64 $byteCount, i1 false)",
        )

        LlvmRawArrayCopyStrategy.REFERENCE -> listOf(
            "  call void @llvm.cj.array.copy.ref.i64(ptr addrspace(1) $destinationBase, " +
                "ptr addrspace(1) $destinationAddress, ptr addrspace(1) $sourceBase, " +
                "ptr addrspace(1) $sourceAddress, i64 $byteCount)",
        )

        LlvmRawArrayCopyStrategy.STRUCT -> listOf(
            "  call void @llvm.cj.array.copy.struct.i64(ptr addrspace(1) $destinationBase, " +
                "ptr addrspace(1) $destinationAddress, ptr addrspace(1) $sourceBase, " +
                "ptr addrspace(1) $sourceAddress, i64 $byteCount)",
        )

        LlvmRawArrayCopyStrategy.GENERIC -> listOf(
            "  call void @llvm.cj.array.copy.generic.i64(ptr addrspace(1) $destinationBase, " +
                "ptr addrspace(1) $destinationAddress, ptr addrspace(1) $sourceBase, " +
                "ptr addrspace(1) $sourceAddress, i64 $byteCount)",
        )
    }

    /** RawArray 引用相等是对象身份比较，不读取或比较数组内容。 */
    private fun lowerRawArrayReferenceEquals(function: CGFunction, expression: ChirOtherExpression): List<String> {
        if (expression.operands.size != 2) {
            throw CodegenLoweringException(
                "RAW_ARRAY_REFEQ requires exactly two RawArray operands",
                expression.semanticId,
            )
        }
        val resultType = expression.resultType ?: throw CodegenLoweringException(
            "RAW_ARRAY_REFEQ requires an explicit Bool result type",
            expression.semanticId,
        )
        requireSameType("i1", function.lowerType(resultType), expression.semanticId, "RAW_ARRAY_REFEQ result")

        val left = expression.operands[0]
        val right = expression.operands[1]
        val leftArrayType = LlvmRawArrayRuntime.arrayType(left.type, expression.semanticId)
        val rightArrayType = LlvmRawArrayRuntime.arrayType(right.type, expression.semanticId)
        if (leftArrayType.dimensions != rightArrayType.dimensions ||
            !ChirTypeRelations.areEquivalent(leftArrayType.elementType, rightArrayType.elementType)
        ) {
            throw CodegenLoweringException(
                "RAW_ARRAY_REFEQ requires RawArray operands with equivalent element types",
                expression.semanticId,
            )
        }
        val operandLlvmType = function.lowerType(left.type)
        requireSameType(operandLlvmType, function.lowerType(right.type), expression.semanticId, "RAW_ARRAY_REFEQ operands")
        if (!operandLlvmType.startsWith("ptr")) {
            throw CodegenLoweringException(
                "RAW_ARRAY_REFEQ operands must use the RawArray pointer ABI",
                expression.semanticId,
            )
        }

        val result = function.resultRef(expression)
        return listOf(
            "  $result = icmp eq $operandLlvmType ${function.renderValue(left)}, ${function.renderValue(right)}",
        )
    }

    /**
     * 按官方 RawArray ABI 降低无边界检查的数组读取；对象头、长度字段和泛型元素尺寸均遵循运行时布局。
     */
    private fun lowerArrayGetUnchecked(function: CGFunction, expression: ChirOtherExpression): List<String> {
        if (expression.operands.size != 2) {
            throw CodegenLoweringException(
                "ARRAY_GET_UNCHECKED requires an array and an index operand",
                expression.semanticId,
            )
        }
        val array = expression.operands[0]
        val index = expression.operands[1]
        val arrayType = LlvmRawArrayRuntime.arrayType(array.type, expression.semanticId)
        val elementType = (arrayType.elementType as? ChirResolvedTypeRef)?.type
            ?: throw CodegenLoweringException(
                "ARRAY_GET_UNCHECKED requires a resolved component type",
                expression.semanticId,
            )
        val resultType = expression.resultType ?: throw CodegenLoweringException(
            "ARRAY_GET_UNCHECKED requires a result type",
            expression.semanticId,
        )
        requireSameType(
            function.lowerType(arrayType.elementType),
            function.lowerType(resultType),
            expression.semanticId,
            "ARRAY_GET_UNCHECKED result",
        )
        requireSameType("i64", function.lowerType(index.type), expression.semanticId, "ARRAY_GET_UNCHECKED index")

        val arrayValue = function.renderValue(array)
        val indexValue = function.renderValue(index)
        val result = function.resultRef(expression)
        val payloadAddress = function.intermediateRef(expression.semanticId, 0)
        val elementAddress = function.intermediateRef(expression.semanticId, 1)
        val instructions = mutableListOf<String>()

        if (elementType is ChirGenericType) {
            val typeInfo = function.lowerTypeInfoValue(arrayType.elementType, expression.semanticId, 16)
            instructions += typeInfo.instructions
            val sizeAddress = function.intermediateRef(expression.semanticId, 8)
            val elementSize = function.intermediateRef(expression.semanticId, 9)
            val elementSize64 = function.intermediateRef(expression.semanticId, 10)
            val elementOffset = function.intermediateRef(expression.semanticId, 11)
            val pointerSize = function.intermediateRef(expression.semanticId, 12)
            val dataOffset = function.intermediateRef(expression.semanticId, 13)
            val byteOffset = function.intermediateRef(expression.semanticId, 14)
            instructions += listOf(
                "  $sizeAddress = getelementptr ${LlvmRawArrayRuntime.TYPE_INFO_TYPE_NAME}, ptr ${typeInfo.value}, i32 0, i32 4",
                "  $elementSize = load i32, ptr $sizeAddress",
                "  $elementSize64 = sext i32 $elementSize to i64",
                "  $elementOffset = mul i64 $elementSize64, $indexValue",
                "  $pointerSize = ptrtoint ptr getelementptr (ptr, ptr null, i32 1) to i64",
                "  $dataOffset = add i64 $pointerSize, 8",
                "  $byteOffset = add i64 $dataOffset, $elementOffset",
                "  $elementAddress = getelementptr i8, ptr $arrayValue, i64 $byteOffset",
                "  $result = load ${function.lowerType(resultType)}, ptr $elementAddress",
            )
        } else {
            if (LlvmRawArrayRuntime.containsGeneric(arrayType.elementType)) {
                throw CodegenLoweringException(
                    "ARRAY_GET_UNCHECKED does not support a composite generic component layout ${arrayType.elementType.renderName}",
                    expression.semanticId,
                )
            }
            val elementLlvmType = function.lowerType(arrayType.elementType)
            val arrayLayoutType = "{ { i64 }, [0 x $elementLlvmType] }"
            instructions += listOf(
                "  $payloadAddress = getelementptr ptr, ptr $arrayValue, i32 1",
                "  $elementAddress = getelementptr inbounds $arrayLayoutType, ptr $payloadAddress, i32 0, i32 1, i64 $indexValue",
                "  $result = load ${function.lowerType(resultType)}, ptr $elementAddress",
            )
        }
        return instructions
    }

    /** 将向量 intrinsic 的 RawArray 与 Int64 偏移降低为首个元素地址。 */
    private fun lowerVectorArrayAddress(
        function: CGFunction,
        expression: ChirOtherExpression,
        array: ChirValue,
        offset: ChirValue,
        firstTemporaryOrdinal: Int,
    ): Pair<List<String>, String> {
        val arrayType = LlvmRawArrayRuntime.arrayType(array.type, expression.semanticId)
        requireSameType("i8", function.lowerType(arrayType.elementType), expression.semanticId, "vector intrinsic byte array")
        requireSameType("i64", function.lowerType(offset.type), expression.semanticId, "vector intrinsic byte offset")
        val payloadAddress = function.intermediateRef(expression.semanticId, firstTemporaryOrdinal)
        val elementAddress = function.intermediateRef(expression.semanticId, firstTemporaryOrdinal + 1)
        return listOf(
            "  $payloadAddress = getelementptr ptr, ptr ${function.renderValue(array)}, i32 1",
            "  $elementAddress = getelementptr inbounds { { i64 }, [0 x i8] }, ptr $payloadAddress, " +
                "i32 0, i32 1, i64 ${function.renderValue(offset)}",
        ) to elementAddress
    }

    /** 官方 vectorCompare32：返回两个 32 字节区间首个不相等字节的位置，相同则返回 32。 */
    private fun lowerVectorCompare32(function: CGFunction, expression: ChirOtherExpression): List<String> {
        if (expression.operands.size != 4) {
            throw CodegenLoweringException(
                "VECTOR_COMPARE_32 requires two RawArray values and two Int64 offsets",
                expression.semanticId,
            )
        }
        val resultType = expression.resultType ?: throw CodegenLoweringException(
            "VECTOR_COMPARE_32 requires an Int64 result type",
            expression.semanticId,
        )
        requireSameType("i64", function.lowerType(resultType), expression.semanticId, "VECTOR_COMPARE_32 result")
        val (firstArrayInstructions, firstAddress) = lowerVectorArrayAddress(
            function,
            expression,
            expression.operands[0],
            expression.operands[1],
            0,
        )
        val (secondArrayInstructions, secondAddress) = lowerVectorArrayAddress(
            function,
            expression,
            expression.operands[2],
            expression.operands[3],
            2,
        )
        val firstVector = function.intermediateRef(expression.semanticId, 4)
        val secondVector = function.intermediateRef(expression.semanticId, 5)
        val differingBytes = function.intermediateRef(expression.semanticId, 6)
        val differingMask = function.intermediateRef(expression.semanticId, 7)
        val firstDifference = function.intermediateRef(expression.semanticId, 8)
        val result = function.resultRef(expression)
        return firstArrayInstructions + secondArrayInstructions + listOf(
            "  $firstVector = load <32 x i8>, ptr $firstAddress, align 1",
            "  $secondVector = load <32 x i8>, ptr $secondAddress, align 1",
            "  $differingBytes = icmp ne <32 x i8> $firstVector, $secondVector",
            "  $differingMask = bitcast <32 x i1> $differingBytes to i32",
            "  $firstDifference = call i32 @llvm.cttz.i32(i32 $differingMask, i1 false)",
            "  $result = zext i32 $firstDifference to i64",
        )
    }

    /** 官方 vectorIndexByte32：返回 32 字节区间内首个匹配字节的位置，未找到则返回 32。 */
    private fun lowerVectorIndexByte32(function: CGFunction, expression: ChirOtherExpression): List<String> {
        if (expression.operands.size != 3) {
            throw CodegenLoweringException(
                "VECTOR_INDEX_BYTE_32 requires a RawArray value, an Int64 offset, and a byte",
                expression.semanticId,
            )
        }
        val resultType = expression.resultType ?: throw CodegenLoweringException(
            "VECTOR_INDEX_BYTE_32 requires an Int64 result type",
            expression.semanticId,
        )
        requireSameType("i64", function.lowerType(resultType), expression.semanticId, "VECTOR_INDEX_BYTE_32 result")
        requireSameType("i8", function.lowerType(expression.operands[2].type), expression.semanticId, "VECTOR_INDEX_BYTE_32 byte")
        val (arrayInstructions, elementAddress) = lowerVectorArrayAddress(
            function,
            expression,
            expression.operands[0],
            expression.operands[1],
            0,
        )
        val inputVector = function.intermediateRef(expression.semanticId, 2)
        val firstLane = function.intermediateRef(expression.semanticId, 3)
        val broadcastByte = function.intermediateRef(expression.semanticId, 4)
        val matchingBytes = function.intermediateRef(expression.semanticId, 5)
        val matchingMask = function.intermediateRef(expression.semanticId, 6)
        val firstMatch = function.intermediateRef(expression.semanticId, 7)
        val result = function.resultRef(expression)
        val byte = function.renderValue(expression.operands[2])
        return arrayInstructions + listOf(
            "  $inputVector = load <32 x i8>, ptr $elementAddress, align 1",
            "  $firstLane = insertelement <32 x i8> undef, i8 $byte, i32 0",
            "  $broadcastByte = shufflevector <32 x i8> $firstLane, <32 x i8> undef, <32 x i32> zeroinitializer",
            "  $matchingBytes = icmp eq <32 x i8> $inputVector, $broadcastByte",
            "  $matchingMask = bitcast <32 x i1> $matchingBytes to i32",
            "  $firstMatch = call i32 @llvm.cttz.i32(i32 $matchingMask, i1 false)",
            "  $result = zext i32 $firstMatch to i64",
        )
    }

    /**
     * 降低官方 CHIR 类型转换；整数依位宽/符号扩展，引用视图复用指针 ABI。
     */
    private fun lowerCangjieTypeCast(
        function: CGFunction,
        expression: ChirOtherExpression,
        operation: ChirOtherOperation,
    ): List<String> {
        val source = expression.operands.singleOrNull() ?: throw CodegenLoweringException(
            "Cangjie type cast requires one source operand",
            expression.semanticId,
        )
        val resultType = expression.resultType ?: throw CodegenLoweringException(
            "Cangjie type cast requires an explicit result type",
            expression.semanticId,
        )
        val sourceType = (source.type as? ChirResolvedTypeRef)?.type ?: throw CodegenLoweringException(
            "Cangjie type cast source ${source.type.renderName} is not resolved",
            expression.semanticId,
        )
        val targetType = (resultType as? ChirResolvedTypeRef)?.type ?: throw CodegenLoweringException(
            "Cangjie type cast target ${resultType.renderName} is not resolved",
            expression.semanticId,
        )
        val llvmSourceType = function.lowerType(source.type)
        val llvmTargetType = function.lowerType(resultType)
        val sourceIntegerDomain = LlvmIntegerCastSemantics.domain(sourceType)
        val targetIntegerDomain = LlvmIntegerCastSemantics.domain(targetType)
        if (sourceIntegerDomain != null && targetIntegerDomain != null) {
            val sourceValue = function.renderValue(source)
            return when (operation.typeCastOverflowStrategy) {
                ChirOverflowStrategy.THROWING -> {
                    if (LlvmIntegerCastSemantics.canCastWithoutOverflow(sourceIntegerDomain, targetIntegerDomain)) {
                        lowerIntegerCastValue(
                            function,
                            expression,
                            sourceValue,
                            llvmSourceType,
                            llvmTargetType,
                            sourceIntegerDomain,
                            targetIntegerDomain,
                        )
                    } else {
                        lowerThrowingIntegerCast(
                            function,
                            expression,
                            sourceValue,
                            llvmSourceType,
                            llvmTargetType,
                            sourceIntegerDomain,
                            targetIntegerDomain,
                        )
                    }
                }
                ChirOverflowStrategy.SATURATING -> lowerSaturatingIntegerCast(
                    function,
                    expression,
                    sourceValue,
                    llvmSourceType,
                    llvmTargetType,
                    sourceIntegerDomain,
                    targetIntegerDomain,
                )
                else -> lowerIntegerCastValue(
                    function,
                    expression,
                    sourceValue,
                    llvmSourceType,
                    llvmTargetType,
                    sourceIntegerDomain,
                    targetIntegerDomain,
                )
            }
        }

        if (sourceType == ChirPrimitiveType.BOOL && targetIntegerDomain != null) {
            val value = function.renderValue(source)
            val result = function.resultRef(expression)
            return listOf("  $result = zext $llvmSourceType $value to $llvmTargetType")
        }

        if (llvmSourceType != llvmTargetType) {
            throw CodegenLoweringException(
                "Cangjie type cast '${operation.canonicalName}' from ${source.type.renderName} to ${resultType.renderName} " +
                    "requires type-specific representation conversion",
                expression.semanticId,
            )
        }
        requireSameType(llvmSourceType, llvmTargetType, expression.semanticId, "Cangjie reference type cast")
        val value = function.renderValue(source)
        val result = function.resultRef(expression)
        return listOf("  $result = select i1 true, $llvmTargetType $value, $llvmTargetType $value")
    }

    /** 降低已由源/目标整数值域证明不会溢出的转换。 */
    private fun lowerIntegerCastValue(
        function: CGFunction,
        expression: ChirOtherExpression,
        sourceValue: String,
        llvmSourceType: String,
        llvmTargetType: String,
        source: LlvmIntegerDomain,
        target: LlvmIntegerDomain,
    ): List<String> {
        val result = function.resultRef(expression)
        return if (source.bitWidth == target.bitWidth) {
            listOf("  $result = select i1 true, $llvmTargetType $sourceValue, $llvmTargetType $sourceValue")
        } else {
            val mnemonic = when {
                target.bitWidth < source.bitWidth -> "trunc"
                source.isSigned -> "sext"
                else -> "zext"
            }
            listOf("  $result = $mnemonic $llvmSourceType $sourceValue to $llvmTargetType")
        }
    }

    /** 对 throwing cast 执行数学值域检查，溢出时构造并抛出官方 OverflowException。 */
    private fun lowerThrowingIntegerCast(
        function: CGFunction,
        expression: ChirOtherExpression,
        sourceValue: String,
        llvmSourceType: String,
        llvmTargetType: String,
        source: LlvmIntegerDomain,
        target: LlvmIntegerDomain,
    ): List<String> {
        val comparisonType = "i${maxOf(source.bitWidth, target.bitWidth) + 1}"
        val comparisonValue = function.intermediateRef(expression.semanticId, 0)
        val lowerCheck = LlvmIntegerCastSemantics.lowerBoundCanOverflow(source, target)
        val upperCheck = LlvmIntegerCastSemantics.upperBoundCanOverflow(source, target)
        val lowerInRange = function.intermediateRef(expression.semanticId, 1)
        val upperInRange = function.intermediateRef(expression.semanticId, 2)
        val lowerOkLabel = function.syntheticBlockLabel(expression.semanticId, "integer.cast.lower.ok")
        val lowerOverflowLabel = function.syntheticBlockLabel(expression.semanticId, "integer.cast.lower.overflow")
        val upperOkLabel = function.syntheticBlockLabel(expression.semanticId, "integer.cast.upper.ok")
        val upperOverflowLabel = function.syntheticBlockLabel(expression.semanticId, "integer.cast.upper.overflow")
        val castLabel = function.syntheticBlockLabel(expression.semanticId, "integer.cast.value")
        val instructions = mutableListOf(
            "  $comparisonValue = ${if (source.isSigned) "sext" else "zext"} $llvmSourceType $sourceValue to $comparisonType",
        )
        if (lowerCheck) {
            val message = LlvmIntegerCastSemantics.lowerBoundOverflowMessage(expression.resultType!!)
            instructions += listOf(
                "  $lowerInRange = icmp sge $comparisonType $comparisonValue, ${target.minimum}",
                "  br i1 $lowerInRange, label %$lowerOkLabel, label %$lowerOverflowLabel",
                "$lowerOverflowLabel:",
            )
            instructions += LlvmOverflowExceptionRuntime.throwInstructions(
                function,
                expression.semanticId,
                temporaryOrdinal = 3,
                message = message,
            )
            instructions += listOf("$lowerOkLabel:")
        }
        if (upperCheck) {
            val message = LlvmIntegerCastSemantics.upperBoundOverflowMessage(expression.resultType!!)
            instructions += listOf(
                "  $upperInRange = icmp sle $comparisonType $comparisonValue, ${target.maximum}",
                "  br i1 $upperInRange, label %$castLabel, label %$upperOverflowLabel",
                "$upperOverflowLabel:",
            )
            instructions += LlvmOverflowExceptionRuntime.throwInstructions(
                function,
                expression.semanticId,
                temporaryOrdinal = 4,
                message = message,
            )
            instructions += listOf("$castLabel:")
        } else if (lowerCheck) {
            instructions += listOf("  br label %$castLabel", "$castLabel:")
        }
        instructions += lowerIntegerCastValue(
            function,
            expression,
            sourceValue,
            llvmSourceType,
            llvmTargetType,
            source,
            target,
        )
        return instructions
    }

    /** 对 saturating cast 分别钳制到目标域下界与上界。 */
    private fun lowerSaturatingIntegerCast(
        function: CGFunction,
        expression: ChirOtherExpression,
        sourceValue: String,
        llvmSourceType: String,
        llvmTargetType: String,
        source: LlvmIntegerDomain,
        target: LlvmIntegerDomain,
    ): List<String> {
        if (LlvmIntegerCastSemantics.canCastWithoutOverflow(source, target)) {
            return lowerIntegerCastValue(
                function,
                expression,
                sourceValue,
                llvmSourceType,
                llvmTargetType,
                source,
                target,
            )
        }
        val comparisonType = "i${maxOf(source.bitWidth, target.bitWidth) + 1}"
        val comparisonValue = function.intermediateRef(expression.semanticId, 0)
        val convertedValue = function.intermediateRef(expression.semanticId, 1)
        val lowerInRange = function.intermediateRef(expression.semanticId, 2)
        val upperInRange = function.intermediateRef(expression.semanticId, 3)
        val lowerAdjusted = function.intermediateRef(expression.semanticId, 4)
        val result = function.resultRef(expression)
        val instructions = mutableListOf(
            "  $comparisonValue = ${if (source.isSigned) "sext" else "zext"} $llvmSourceType $sourceValue to $comparisonType",
        )
        val lowerValue = if (LlvmIntegerCastSemantics.lowerBoundCanOverflow(source, target)) {
            instructions += listOf(
                "  $lowerInRange = icmp sge $comparisonType $comparisonValue, ${target.minimum}",
            )
            lowerInRange
        } else {
            "true"
        }
        val upperValue = if (LlvmIntegerCastSemantics.upperBoundCanOverflow(source, target)) {
            instructions += listOf(
                "  $upperInRange = icmp sle $comparisonType $comparisonValue, ${target.maximum}",
            )
            upperInRange
        } else {
            "true"
        }
        instructions += lowerIntegerCastValue(
            function,
            expression,
            sourceValue,
            llvmSourceType,
            llvmTargetType,
            source,
            target,
        ).map { instruction -> instruction.replace(function.resultRef(expression), convertedValue) }
        instructions += listOf(
            "  $lowerAdjusted = select i1 $lowerValue, $llvmTargetType $convertedValue, $llvmTargetType ${target.minimum}",
            "  $result = select i1 $upperValue, $llvmTargetType $lowerAdjusted, $llvmTargetType ${target.maximum}",
        )
        return instructions
    }

    /** 返回 CHIR 定宽整数类型的位宽；原生整数在当前 LLVM ABI 上为 64 位。 */
    /**
     * 降低官方 RawArrayAllocate：先按有符号 Int64 检查长度，再使用带 component TypeInfo 的运行时分配 ABI。
     */
    private fun lowerRawArrayAllocate(function: CGFunction, expression: ChirOtherExpression): List<String> {
        val lengthValue = expression.operands.singleOrNull() ?: throw CodegenLoweringException(
            "RawArrayAllocate requires one Int64 length operand",
            expression.semanticId,
        )
        val lengthType = function.lowerType(lengthValue.type)
        requireSameType("i64", lengthType, expression.semanticId, "RawArray allocation length")
        val arrayType = LlvmRawArrayRuntime.arrayType(expression.resultType, expression.semanticId)
        val typeInfo = function.lowerTypeInfoValue(ChirResolvedTypeRef(arrayType), expression.semanticId)
        val allocationFunction = LlvmRawArrayRuntime.allocationFunction(arrayType, expression.semanticId)
        val length = function.renderValue(lengthValue)
        val validLength = function.intermediateRef(expression.semanticId, 0)
        val exceptionSizeAddress = function.intermediateRef(expression.semanticId, 1)
        val exceptionSize = function.intermediateRef(expression.semanticId, 2)
        val exceptionSizeWide = function.intermediateRef(expression.semanticId, 3)
        val exceptionSizeRounded = function.intermediateRef(expression.semanticId, 4)
        val exceptionSizeAligned = function.intermediateRef(expression.semanticId, 5)
        val typeInfoPointerSize = function.intermediateRef(expression.semanticId, 6)
        val exceptionAllocationSizeWide = function.intermediateRef(expression.semanticId, 7)
        val exceptionAllocationSize = function.intermediateRef(expression.semanticId, 8)
        val exceptionObject = function.intermediateRef(expression.semanticId, 9)
        val throwLabel = function.syntheticBlockLabel(expression.semanticId, "rawarray.negative_length")
        val allocateLabel = function.syntheticBlockLabel(expression.semanticId, "rawarray.nonnegative_length")

        return listOf(
            "  $validLength = icmp sge i64 $length, 0",
            "  br i1 $validLength, label %$allocateLabel, label %$throwLabel",
            "$throwLabel:",
            "  $exceptionSizeAddress = getelementptr inbounds %TypeInfo, ptr ${LlvmRawArrayRuntime.global(LlvmRawArrayRuntime.NEGATIVE_ARRAY_SIZE_TYPE_INFO)}, i32 0, i32 4",
            "  $exceptionSize = load i32, ptr $exceptionSizeAddress",
            "  $exceptionSizeWide = zext i32 $exceptionSize to i64",
            "  $exceptionSizeRounded = add i64 $exceptionSizeWide, 7",
            "  $exceptionSizeAligned = and i64 $exceptionSizeRounded, -8",
            "  $typeInfoPointerSize = ptrtoint ptr getelementptr (ptr, ptr null, i32 1) to i64",
            "  $exceptionAllocationSizeWide = add i64 $exceptionSizeAligned, $typeInfoPointerSize",
            "  $exceptionAllocationSize = trunc i64 $exceptionAllocationSizeWide to i32",
            "  $exceptionObject = call ptr @MCC_NewObject(ptr ${LlvmRawArrayRuntime.global(LlvmRawArrayRuntime.NEGATIVE_ARRAY_SIZE_TYPE_INFO)}, i32 $exceptionAllocationSize)",
            "  call void ${LlvmRawArrayRuntime.global(LlvmRawArrayRuntime.NEGATIVE_ARRAY_SIZE_CONSTRUCTOR)}(ptr $exceptionObject, ptr ${LlvmRawArrayRuntime.global(LlvmRawArrayRuntime.NEGATIVE_ARRAY_SIZE_TYPE_INFO)})",
            "  call void @MCC_ThrowException(ptr $exceptionObject)",
            "  unreachable",
            "$allocateLabel:",
        ) + typeInfo.instructions +
            "  ${function.resultRef(expression)} = call ptr @$allocationFunction(ptr ${typeInfo.value}, i64 $length)"
    }

    /**
     * 空 RawArray literal initializer 没有元素写入；非空初始化由与元素布局匹配的 lowering 路径处理。
     */
    private fun lowerRawArrayLiteralInit(expression: ChirOtherExpression): List<String> {
        val destination = expression.operands.firstOrNull() ?: throw CodegenLoweringException(
            "RawArray literal initialization requires a destination array",
            expression.semanticId,
        )
        LlvmRawArrayRuntime.arrayType(destination.type, expression.semanticId)
        if (expression.operands.size == 1) return emptyList()

        throw CodegenLoweringException(
            "non-empty RawArray literal initialization requires element-layout and GC-barrier lowering",
            expression.semanticId,
        )
    }

    /** 按官方 RawArray 布局逐元素初始化，并为托管引用和含引用聚合值发出 GC 写屏障。 */
    private fun lowerRawArrayInitByValue(function: CGFunction, expression: ChirOtherExpression): List<String> {
        if (expression.operands.size != 3) {
            throw CodegenLoweringException(
                "RawArray initialization requires an array, an Int64 element count, and an initial value; " +
                    "got operand types [${expression.operands.joinToString { it.type.renderName }}]",
                expression.semanticId,
            )
        }
        val destination = expression.operands[0]
        val elementCount = expression.operands[1]
        val initialValue = expression.operands[2]
        val arrayType = LlvmRawArrayRuntime.arrayType(destination.type, expression.semanticId)
        val elementType = (arrayType.elementType as? ChirResolvedTypeRef)?.type ?: throw CodegenLoweringException(
            "RawArray initialization requires a resolved component type",
            expression.semanticId,
        )
        requireSameType("i64", function.lowerType(elementCount.type), expression.semanticId, "RawArray initialization count")
        requireSameType(
            function.lowerType(arrayType.elementType),
            function.lowerType(initialValue.type),
            expression.semanticId,
            "RawArray initialization value",
        )
        val isReferenceElement = isManagedReferenceElement(elementType)
        if (!isReferenceElement && LlvmRawArrayRuntime.containsGeneric(arrayType.elementType)) {
            throw CodegenLoweringException(
                "RawArray initialization for composite generic component ${arrayType.elementType.renderName} " +
                    "requires runtime generic assignment lowering",
                expression.semanticId,
            )
        }

        val arrayValue = function.renderValue(destination)
        val countValue = function.renderValue(elementCount)
        val initialValueText = function.renderValue(initialValue)
        val elementLlvmType = function.lowerType(arrayType.elementType)
        val payloadAddress = function.intermediateRef(expression.semanticId, 0)
        val elementAddress = function.intermediateRef(expression.semanticId, 1)
        val index = function.intermediateRef(expression.semanticId, 2)
        val nextIndex = function.intermediateRef(expression.semanticId, 3)
        val continueLoop = function.intermediateRef(expression.semanticId, 4)
        val preheaderLabel = function.syntheticBlockLabel(expression.semanticId, "rawarray.init.preheader")
        val conditionLabel = function.syntheticBlockLabel(expression.semanticId, "rawarray.init.condition")
        val bodyLabel = function.syntheticBlockLabel(expression.semanticId, "rawarray.init.body")
        val endLabel = function.syntheticBlockLabel(expression.semanticId, "rawarray.init.end")
        val instructions = mutableListOf(
            "  $payloadAddress = getelementptr ptr, ptr $arrayValue, i32 1",
            "  br label %$preheaderLabel",
            "$preheaderLabel:",
            "  br label %$conditionLabel",
            "$conditionLabel:",
            "  $index = phi i64 [ 0, %$preheaderLabel ], [ $nextIndex, %$bodyLabel ]",
            "  $continueLoop = icmp slt i64 $index, $countValue",
            "  br i1 $continueLoop, label %$bodyLabel, label %$endLabel",
            "$bodyLabel:",
            "  $elementAddress = getelementptr inbounds { { i64 }, [0 x $elementLlvmType] }, " +
                "ptr $payloadAddress, i32 0, i32 1, i64 $index",
        )

        if (isReferenceElement) {
            val gcArray = function.intermediateRef(expression.semanticId, 5)
            val gcValue = function.intermediateRef(expression.semanticId, 6)
            val gcElementAddress = function.intermediateRef(expression.semanticId, 7)
            instructions += listOf(
                "  $gcArray = addrspacecast ptr $arrayValue to ptr addrspace(1)",
                "  $gcValue = addrspacecast ptr $initialValueText to ptr addrspace(1)",
                "  $gcElementAddress = addrspacecast ptr $elementAddress to ptr addrspace(1)",
                "  call void @llvm.cj.gcwrite.ref(ptr addrspace(1) $gcValue, ptr addrspace(1) $gcArray, " +
                    "ptr addrspace(1) $gcElementAddress)",
            )
        } else if (LlvmRawArrayRuntime.containsManagedReferences(arrayType.elementType)) {
            val sourceAddress = function.intermediateRef(expression.semanticId, 8)
            val gcArray = function.intermediateRef(expression.semanticId, 9)
            val gcElementAddress = function.intermediateRef(expression.semanticId, 10)
            val size = function.intermediateRef(expression.semanticId, 11)
            instructions += listOf(
                "  $sourceAddress = alloca $elementLlvmType",
                "  store $elementLlvmType $initialValueText, ptr $sourceAddress",
                "  $gcArray = addrspacecast ptr $arrayValue to ptr addrspace(1)",
                "  $gcElementAddress = addrspacecast ptr $elementAddress to ptr addrspace(1)",
                "  $size = ptrtoint ptr getelementptr ($elementLlvmType, ptr null, i32 1) to i64",
                "  call void @llvm.cj.gcwrite.struct.p0(ptr addrspace(1) $gcArray, " +
                    "ptr addrspace(1) $gcElementAddress, ptr $sourceAddress, i64 $size)",
            )
        } else {
            instructions += "  store $elementLlvmType $initialValueText, ptr $elementAddress"
        }

        instructions += listOf(
            "  $nextIndex = add i64 $index, 1",
            "  br label %$conditionLabel",
            "$endLabel:",
        )
        return instructions
    }

    private fun isManagedReferenceElement(type: ChirType): Boolean = when (type) {
        is ChirRefType, is ChirRawArrayType -> true
        else -> false
    }

    /**
     * 将 CHIR cast 操作映射为 LLVM cast 指令助记符。
     */
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

    /**
     * 判断 LLVM textual type 是否为浮点类型。
     */
    private fun isFloatType(llvmType: String): Boolean {
        return llvmType == "half" || llvmType == "float" || llvmType == "double"
    }

    /**
     * 要求 LLVM textual type 为浮点类型。
     */
    private fun requireFloatType(llvmType: String, sourceId: ChirSemanticId, subject: String) {
        if (!isFloatType(llvmType)) {
            throw CodegenLoweringException(
                "$subject requires floating LLVM type, got $llvmType",
                sourceId,
            )
        }
    }

    /**
     * 要求 LLVM textual type 为整数类型。
     */
    private fun requireIntegerType(llvmType: String, sourceId: ChirSemanticId, subject: String) {
        if (!isIntegerType(llvmType)) {
            throw CodegenLoweringException(
                "$subject requires integer LLVM type, got $llvmType",
                sourceId,
            )
        }
    }

    /**
     * 判断 LLVM textual type 是否为 `iN` 整数类型。
     */
    private fun isIntegerType(llvmType: String): Boolean = llvmType.matches(integerTypeRegex)

    /**
     * 要求两个 LLVM textual type 完全一致。
     */
    private fun requireSameType(expected: String, actual: String, sourceId: ChirSemanticId, subject: String) {
        if (expected != actual) {
            throw CodegenLoweringException(
                "$subject type mismatch: expected $expected, got $actual at CHIR ${sourceId.value}",
                sourceId,
            )
        }
    }

    /**
     * 从参与 memory 操作的 value 属性中提取 LLVM align 后缀。
     */
    private fun alignSuffixFrom(vararg values: ChirValue?): String {
        val alignValue = values.asSequence()
            .filterNotNull()
            .flatMap { value -> value.attributes.asSequence() }
            .filterIsInstance<ChirStringAttribute>()
            .firstOrNull { it.key == "align" }
            ?.value
        return if (alignValue.isNullOrBlank()) "" else ", align $alignValue"
    }

    /**
     * 从 CHIR pointer/ref 类型中取得 LLVM pointee type。
     */
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
