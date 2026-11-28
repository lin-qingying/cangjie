package org.cangnova.cangjie.wasm.codegen.lowering

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
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
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirBlockGroupValue
import org.cangnova.cangjie.chir.core.value.ChirBlockValue
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirGlobalValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedVariableValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.chir.core.value.ChirValue
import org.cangnova.cangjie.wasm.codegen.diagnostics.WasmCodegenException
import org.cangnova.cangjie.wasm.ir.WasmFunction
import org.cangnova.cangjie.wasm.ir.WasmFunctionType
import org.cangnova.cangjie.wasm.ir.WasmInstruction
import org.cangnova.cangjie.wasm.ir.WasmLocal
import org.cangnova.cangjie.wasm.ir.WasmNumericOperator
import org.cangnova.cangjie.wasm.ir.WasmValueType

/** 将单个 CHIR CFG lowering 为 structured Wasm 函数，并用 locals 保存 block dispatcher 状态。 */
internal class ChirFunctionLowerer(
    private val plan: WasmCodegenPlan,
    private val declaration: ChirFunctionDeclaration,
) {
    private val mapper = plan.mapper
    private val signature: WasmFunctionType = plan.functionTypeById.getValue(declaration.semanticId)
    private val localLayout = WasmFunctionLocalLayout(declaration, mapper)
    private val blocks: List<ChirBlock> = orderBlocks()
    private val blockIndexById = blocks.mapIndexed { index, block -> block.semanticId to index }.toMap()
    private val blockLabelById = blocks.mapIndexed { index, block -> block.semanticId to "case_$index" }.toMap()
    private val dispatchLabel = "dispatch_loop"
    private val exitLabel = "dispatch_exit"
    private val phiExpressionsByBlock: Map<ChirSemanticId, List<ChirOtherExpression>> = collectPhis()
    private val predecessorsByBlock: Map<ChirSemanticId, Set<ChirSemanticId>> = collectPredecessors()

    /** Lower the complete CHIR function, including locals and dispatcher control flow. */
    fun lower(): WasmFunction {
        validateFunctionShape()
        validatePhiInputs()
        val entryIndex = blockIndexById.getValue(declaration.entryBlockId)
        val body = buildList {
            if (localLayout.usesStack) {
                add(WasmInstruction.Call(plan.stackMarkFunctionName(declaration.semanticId)))
                add(WasmInstruction.LocalSet(localLayout.frameBase))
            }
            declaration.parameters.forEach { parameter ->
                val normalization = mapper.normalize(parameter.type, parameter.semanticId)
                if (normalization.isNotEmpty()) {
                    val local = localLayout.valueLocal(parameter.semanticId)
                    add(WasmInstruction.LocalGet(local))
                    addAll(normalization)
                    add(WasmInstruction.LocalSet(local))
                }
            }
            add(WasmInstruction.I32Const(entryIndex))
            add(WasmInstruction.LocalSet(localLayout.programCounter))
            add(
                WasmInstruction.Block(
                    exitLabel,
                    listOf(
                        WasmInstruction.Loop(
                            dispatchLabel,
                            buildDispatchCases(blocks.lastIndex),
                        ),
                    ),
                ),
            )
            add(WasmInstruction.Unreachable)
        }
        return WasmFunction(
            name = plan.definedFunctionName(declaration.semanticId),
            type = signature,
            parameters = localLayout.parameters,
            locals = localLayout.locals,
            body = body,
        )
    }

    private fun buildDispatchCases(index: Int): List<WasmInstruction> {
        if (index < 0) {
            return listOf(
                WasmInstruction.LocalGet(localLayout.programCounter),
                WasmInstruction.BrTable(blocks.map { blockLabelById.getValue(it.semanticId) }, exitLabel),
            )
        }

        val block = blocks[index]
        val nestedDispatch = WasmInstruction.Block(blockLabelById.getValue(block.semanticId), buildDispatchCases(index - 1))
        val loweredBlock = lowerBlock(block)
        return buildList {
            add(nestedDispatch)
            addAll(loweredBlock.instructions)
            if (loweredBlock.branchesToDispatcher) add(WasmInstruction.Br(dispatchLabel))
        }
    }

    private fun lowerReturn(terminator: ChirReturnTerminator): LoweredTerminator {
        val returnValue = terminator.returnValue
        if (signature.results.isEmpty()) {
            if (returnValue != null && mapper.valueTypeOrNull(returnValue.type, terminator.semanticId) != null) {
                throw WasmCodegenException("Unit-returning function has a value return", terminator.semanticId)
            }
            return LoweredTerminator(
                restoreStack() + WasmInstruction.Return,
                branchesToDispatcher = false,
            )
        }
        val value = returnValue ?: throw WasmCodegenException(
            "function '${declaration.name}' returns a value but its return terminator is empty",
            terminator.semanticId,
        )
        if (value.type != declaration.returnType) {
            throw WasmCodegenException(
                "return type ${value.type.renderName} does not match ${declaration.returnType.renderName}",
                terminator.semanticId,
            )
        }
        val returnLocal = localLayout.returnValue
            ?: throw WasmCodegenException("missing return local for function '${declaration.name}'", terminator.semanticId)
        return LoweredTerminator(
            valueInstructions(value) + listOf(
                WasmInstruction.LocalSet(returnLocal),
            ) + restoreStack() + listOf(WasmInstruction.LocalGet(returnLocal), WasmInstruction.Return),
            branchesToDispatcher = false,
        )
    }

    private fun restoreStack(): List<WasmInstruction> = if (localLayout.usesStack) {
        listOf(
            WasmInstruction.LocalGet(localLayout.frameBase),
            WasmInstruction.Call(plan.stackRestoreFunctionName(declaration.semanticId)),
        )
    } else {
        emptyList()
    }

    private fun lowerThrow(
        block: ChirBlock,
        terminator: ChirThrowTerminator,
    ): LoweredTerminator {
        val exceptionLocal = localLayout.exceptionValue
        requireI32(terminator.exceptionValue.type, terminator.semanticId, "thrown exception")
        val setup = valueInstructions(terminator.exceptionValue) + WasmInstruction.LocalSet(exceptionLocal)
        val dispatches = terminator.catchDispatches.ifEmpty {
            val fallback = terminator.unwindTargetBlockId
            if (fallback == null) emptyList() else listOf(
                org.cangnova.cangjie.chir.core.controlflow.ChirCatchDispatchEntry(
                    catchBlockId = fallback,
                    exceptionBinding = terminator.unwindExceptionBinding,
                    exceptionTypeName = null,
                ),
            )
        }
        if (dispatches.isEmpty()) {
            return LoweredTerminator(
                setup + restoreStack() + listOf(
                    WasmInstruction.LocalGet(exceptionLocal),
                    WasmInstruction.Call(plan.throwFunctionName(terminator.semanticId)),
                    WasmInstruction.Unreachable,
                ),
                branchesToDispatcher = false,
            )
        }

        val instructions = setup.toMutableList()
        dispatches.forEachIndexed { index, dispatch ->
            val branch = mutableListOf<WasmInstruction>()
            val exceptionTypeName = dispatch.exceptionTypeName
            dispatch.exceptionBinding?.let { binding -> branch += bindException(binding, terminator.semanticId) }
            branch += edgeInstructions(block.semanticId, dispatch.catchBlockId, terminator.semanticId)
            branch += WasmInstruction.Br(dispatchLabel)
            if (exceptionTypeName == null) {
                instructions += branch
                return LoweredTerminator(instructions, branchesToDispatcher = false)
            }
            instructions += listOf(
                WasmInstruction.LocalGet(exceptionLocal),
                WasmInstruction.I32Const(stableTypeId(exceptionTypeName)),
                WasmInstruction.Call(plan.exceptionMatchesFunctionName(terminator.semanticId)),
                WasmInstruction.If(
                    label = "catch_${blockIndexById.getValue(block.semanticId)}_${index}",
                    thenBody = branch,
                ),
            )
        }
        instructions += listOf(
            WasmInstruction.LocalGet(exceptionLocal),
            WasmInstruction.Call(plan.throwFunctionName(terminator.semanticId)),
            WasmInstruction.Unreachable,
        )
        return LoweredTerminator(instructions, branchesToDispatcher = false)
    }

    private fun bindException(bindingName: String, semanticId: ChirSemanticId): List<WasmInstruction> {
        val bindingLocal = localLayout.sourceLocal(bindingName)
            ?: throw WasmCodegenException("exception binding '$bindingName' is not a local in '${declaration.name}'", semanticId)
        if (localLayout.typeOf(bindingLocal) != WasmValueType.I32) {
            throw WasmCodegenException("exception binding '$bindingName' must use the wasm32 reference representation", semanticId)
        }
        return listOf(WasmInstruction.LocalGet(localLayout.exceptionValue), WasmInstruction.LocalSet(bindingLocal))
    }

    private fun lowerExpression(expression: ChirExpression): List<WasmInstruction> = when (expression) {
        is ChirUnaryExpression -> lowerUnary(expression)
        is ChirBinaryExpression -> lowerBinary(expression)
        is ChirMemoryExpression -> lowerMemory(expression)
        is ChirCallExpression -> lowerCall(expression)
        is ChirOtherExpression -> lowerOther(expression)
        else -> throw WasmCodegenException(
            "unsupported CHIR expression ${expression::class.simpleName}",
            expression.semanticId,
        )
    }

    private fun lowerUnary(expression: ChirUnaryExpression): List<WasmInstruction> {
        val operator = ChirUnaryOperator.parse(expression.operator)
            ?: throw WasmCodegenException("unsupported CHIR unary operator '${expression.operator}'", expression.semanticId)
        if (isFloat16(expression.operand.type)) {
            if (operator != ChirUnaryOperator.FLOAT_NEG && operator != ChirUnaryOperator.IDENTITY) {
                throw WasmCodegenException("${operator.canonicalName} is not defined for Float16 values", expression.semanticId)
            }
            if (expression.resultType != expression.operand.type) {
                throw WasmCodegenException("Float16 unary result type differs from its operand", expression.semanticId)
            }
            return valueInstructions(expression.operand) + WasmInstruction.Call(plan.runtimeFunctionName(expression.semanticId)) +
                mapper.normalize(expression.resultType, expression.semanticId)
        }
        genericIdentifier(expression.operand.type)?.let { identifier ->
            val resultGeneric = genericIdentifier(expression.resultType)
            val isBoolResult = expression.resultType == ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
            if (operator != ChirUnaryOperator.LOGICAL_NOT && resultGeneric != identifier) {
                throw WasmCodegenException("generic unary result does not preserve type '$identifier'", expression.semanticId)
            }
            if (operator == ChirUnaryOperator.LOGICAL_NOT && !isBoolResult) {
                throw WasmCodegenException("generic logical not must return Bool", expression.semanticId)
            }
            return valueInstructions(expression.operand) +
                WasmInstruction.I32Const(WasmRuntimeTypeIds.of(identifier)) +
                WasmInstruction.Call(plan.runtimeFunctionName(expression.semanticId)) +
                mapper.normalize(expression.resultType, expression.semanticId)
        }
        val type = mapper.valueType(expression.resultType, expression.semanticId)
        val operandType = mapper.valueType(expression.operand.type, expression.semanticId)
        if (type != operandType) {
            throw WasmCodegenException("unary operand and result Wasm types differ", expression.semanticId)
        }
        val value = valueInstructions(expression.operand)
        return when (operator) {
            ChirUnaryOperator.INT_NEG -> {
                if (!mapper.isInteger(expression.resultType, expression.semanticId)) {
                    throw WasmCodegenException("integer negation requires a Cangjie integer operand", expression.semanticId)
                }
                when (type) {
                WasmValueType.I32 -> listOf(WasmInstruction.I32Const(0)) + value + WasmInstruction.Numeric(WasmNumericOperator.I32_SUB)
                WasmValueType.I64 -> listOf(WasmInstruction.I64Const(0)) + value + WasmInstruction.Numeric(WasmNumericOperator.I64_SUB)
                else -> throw WasmCodegenException("integer negation requires an integer operand", expression.semanticId)
                } + mapper.normalize(expression.resultType, expression.semanticId)
            }
            ChirUnaryOperator.FLOAT_NEG -> value + listOf(
                WasmInstruction.Numeric(
                    if (type == WasmValueType.F32) WasmNumericOperator.F32_NEG
                    else if (type == WasmValueType.F64) WasmNumericOperator.F64_NEG
                    else throw WasmCodegenException("floating negation requires a floating operand", expression.semanticId),
                ),
            )
            ChirUnaryOperator.BIT_NOT -> {
                if (!mapper.isInteger(expression.resultType, expression.semanticId) && !mapper.isBoolean(expression.resultType, expression.semanticId)) {
                    throw WasmCodegenException("bitwise not requires an integer or Bool operand", expression.semanticId)
                }
                when (type) {
                    WasmValueType.I32 -> listOf(WasmInstruction.I32Const(-1)) + value + WasmInstruction.Numeric(WasmNumericOperator.I32_XOR)
                    WasmValueType.I64 -> listOf(WasmInstruction.I64Const(-1)) + value + WasmInstruction.Numeric(WasmNumericOperator.I64_XOR)
                    else -> throw WasmCodegenException("bitwise not requires an integer operand", expression.semanticId)
                } + mapper.normalize(expression.resultType, expression.semanticId)
            }
            ChirUnaryOperator.LOGICAL_NOT -> {
                requireBoolean(expression.operand.type, expression.semanticId, "logical not operand")
                listOf(WasmInstruction.I32Const(0)) + value + listOf(WasmInstruction.Numeric(WasmNumericOperator.I32_EQ))
            }
            ChirUnaryOperator.IDENTITY -> value + mapper.normalize(expression.resultType, expression.semanticId)
        }
    }

    private fun lowerBinary(expression: ChirBinaryExpression): List<WasmInstruction> {
        val operator = ChirBinaryOperator.parse(expression.operator)
            ?: throw WasmCodegenException("unsupported CHIR binary operator '${expression.operator}'", expression.semanticId)
        if (isFloat16(expression.left.type)) {
            if (!isFloat16(expression.right.type)) {
                throw WasmCodegenException("Float16 binary operation has a non-Float16 right operand", expression.semanticId)
            }
            val comparison = operator.family == ChirBinaryOperatorFamily.COMPARISON
            val expectedResult = if (comparison) ChirResolvedTypeRef(ChirPrimitiveType.BOOL) else expression.left.type
            if (expression.resultType != expectedResult) {
                throw WasmCodegenException("Float16 binary result type does not match its operator", expression.semanticId)
            }
            return valueInstructions(expression.left) + valueInstructions(expression.right) +
                WasmInstruction.Call(plan.runtimeFunctionName(expression.semanticId)) +
                mapper.normalize(expression.resultType, expression.semanticId)
        }
        genericIdentifier(expression.left.type)?.let { identifier ->
            if (genericIdentifier(expression.right.type) != identifier) {
                throw WasmCodegenException("generic binary operands have different type parameters", expression.semanticId)
            }
            val comparison = operator.family == ChirBinaryOperatorFamily.COMPARISON
            val resultMatches = if (comparison) {
                expression.resultType == ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
            } else {
                genericIdentifier(expression.resultType) == identifier
            }
            if (!resultMatches) throw WasmCodegenException("generic binary result type does not match its operator", expression.semanticId)
            return valueInstructions(expression.left) + valueInstructions(expression.right) +
                WasmInstruction.I32Const(WasmRuntimeTypeIds.of(identifier)) +
                WasmInstruction.Call(plan.runtimeFunctionName(expression.semanticId)) +
                mapper.normalize(expression.resultType, expression.semanticId)
        }
        validateBinaryDomain(operator, expression.left.type, expression.right.type, expression.resultType, expression.semanticId)
        val leftType = mapper.valueType(expression.left.type, expression.semanticId)
        val rightType = mapper.valueType(expression.right.type, expression.semanticId)
        if (leftType != rightType) throw WasmCodegenException("binary operands have different Wasm types", expression.semanticId)
        val isComparison = operator.family == ChirBinaryOperatorFamily.COMPARISON
        val resultType = mapper.valueType(expression.resultType, expression.semanticId)
        if (isComparison && (!mapper.isBoolean(expression.resultType, expression.semanticId) || resultType != WasmValueType.I32)) {
            throw WasmCodegenException("comparison result must use i32 Bool representation", expression.semanticId)
        }
        if (!isComparison && (resultType != leftType || expression.resultType != expression.left.type)) {
            throw WasmCodegenException("binary result type differs from its operands", expression.semanticId)
        }
        if ((operator == ChirBinaryOperator.SIGNED_REM) && leftType in setOf(WasmValueType.F32, WasmValueType.F64)) {
            return valueInstructions(expression.left) + valueInstructions(expression.right) +
                WasmInstruction.Call(plan.runtimeFunctionName(expression.semanticId))
        }
        val wasmOperator = binaryOperator(operator, leftType, expression.semanticId)
        return valueInstructions(expression.left) + valueInstructions(expression.right) +
            WasmInstruction.Numeric(wasmOperator) +
            if (isComparison) mapper.normalize(expression.resultType, expression.semanticId)
            else mapper.normalize(expression.resultType, expression.semanticId)
    }

    private fun binaryOperator(
        operator: ChirBinaryOperator,
        type: WasmValueType,
        semanticId: ChirSemanticId,
    ): WasmNumericOperator {
        val floating = type == WasmValueType.F32 || type == WasmValueType.F64
        val int32 = type == WasmValueType.I32
        fun integer(i32: WasmNumericOperator, i64: WasmNumericOperator): WasmNumericOperator =
            if (int32) i32 else if (type == WasmValueType.I64) i64
            else throw WasmCodegenException("operator ${operator.canonicalName} requires a numeric operand", semanticId)
        fun float(f32: WasmNumericOperator, f64: WasmNumericOperator): WasmNumericOperator =
            if (type == WasmValueType.F32) f32 else if (type == WasmValueType.F64) f64
            else throw WasmCodegenException("operator ${operator.canonicalName} requires a floating operand", semanticId)
        return when (operator) {
            ChirBinaryOperator.ADD -> if (floating) float(WasmNumericOperator.F32_ADD, WasmNumericOperator.F64_ADD)
                else integer(WasmNumericOperator.I32_ADD, WasmNumericOperator.I64_ADD)
            ChirBinaryOperator.SUB -> if (floating) float(WasmNumericOperator.F32_SUB, WasmNumericOperator.F64_SUB)
                else integer(WasmNumericOperator.I32_SUB, WasmNumericOperator.I64_SUB)
            ChirBinaryOperator.MUL -> if (floating) float(WasmNumericOperator.F32_MUL, WasmNumericOperator.F64_MUL)
                else integer(WasmNumericOperator.I32_MUL, WasmNumericOperator.I64_MUL)
            ChirBinaryOperator.SIGNED_DIV -> if (floating) float(WasmNumericOperator.F32_DIV, WasmNumericOperator.F64_DIV)
                else integer(WasmNumericOperator.I32_DIV_S, WasmNumericOperator.I64_DIV_S)
            ChirBinaryOperator.UNSIGNED_DIV -> integer(WasmNumericOperator.I32_DIV_U, WasmNumericOperator.I64_DIV_U)
            ChirBinaryOperator.SIGNED_REM -> integer(WasmNumericOperator.I32_REM_S, WasmNumericOperator.I64_REM_S)
            ChirBinaryOperator.UNSIGNED_REM -> integer(WasmNumericOperator.I32_REM_U, WasmNumericOperator.I64_REM_U)
            ChirBinaryOperator.BIT_AND -> integer(WasmNumericOperator.I32_AND, WasmNumericOperator.I64_AND)
            ChirBinaryOperator.BIT_OR -> integer(WasmNumericOperator.I32_OR, WasmNumericOperator.I64_OR)
            ChirBinaryOperator.BIT_XOR -> integer(WasmNumericOperator.I32_XOR, WasmNumericOperator.I64_XOR)
            ChirBinaryOperator.SHIFT_LEFT -> integer(WasmNumericOperator.I32_SHL, WasmNumericOperator.I64_SHL)
            ChirBinaryOperator.SIGNED_SHIFT_RIGHT -> integer(WasmNumericOperator.I32_SHR_S, WasmNumericOperator.I64_SHR_S)
            ChirBinaryOperator.UNSIGNED_SHIFT_RIGHT -> integer(WasmNumericOperator.I32_SHR_U, WasmNumericOperator.I64_SHR_U)
            ChirBinaryOperator.EQUAL, ChirBinaryOperator.FLOAT_EQUAL -> if (floating)
                float(WasmNumericOperator.F32_EQ, WasmNumericOperator.F64_EQ)
            else integer(WasmNumericOperator.I32_EQ, WasmNumericOperator.I64_EQ)
            ChirBinaryOperator.NOT_EQUAL, ChirBinaryOperator.FLOAT_NOT_EQUAL -> if (floating)
                float(WasmNumericOperator.F32_NE, WasmNumericOperator.F64_NE)
            else integer(WasmNumericOperator.I32_NE, WasmNumericOperator.I64_NE)
            ChirBinaryOperator.SIGNED_LESS, ChirBinaryOperator.FLOAT_LESS -> if (floating)
                float(WasmNumericOperator.F32_LT, WasmNumericOperator.F64_LT)
            else integer(WasmNumericOperator.I32_LT_S, WasmNumericOperator.I64_LT_S)
            ChirBinaryOperator.SIGNED_LESS_OR_EQUAL, ChirBinaryOperator.FLOAT_LESS_OR_EQUAL -> if (floating)
                float(WasmNumericOperator.F32_LE, WasmNumericOperator.F64_LE)
            else integer(WasmNumericOperator.I32_LE_S, WasmNumericOperator.I64_LE_S)
            ChirBinaryOperator.SIGNED_GREATER, ChirBinaryOperator.FLOAT_GREATER -> if (floating)
                float(WasmNumericOperator.F32_GT, WasmNumericOperator.F64_GT)
            else integer(WasmNumericOperator.I32_GT_S, WasmNumericOperator.I64_GT_S)
            ChirBinaryOperator.SIGNED_GREATER_OR_EQUAL, ChirBinaryOperator.FLOAT_GREATER_OR_EQUAL -> if (floating)
                float(WasmNumericOperator.F32_GE, WasmNumericOperator.F64_GE)
            else integer(WasmNumericOperator.I32_GE_S, WasmNumericOperator.I64_GE_S)
            ChirBinaryOperator.UNSIGNED_LESS -> integer(WasmNumericOperator.I32_LT_U, WasmNumericOperator.I64_LT_U)
            ChirBinaryOperator.UNSIGNED_LESS_OR_EQUAL -> integer(WasmNumericOperator.I32_LE_U, WasmNumericOperator.I64_LE_U)
            ChirBinaryOperator.UNSIGNED_GREATER -> integer(WasmNumericOperator.I32_GT_U, WasmNumericOperator.I64_GT_U)
            ChirBinaryOperator.UNSIGNED_GREATER_OR_EQUAL -> integer(WasmNumericOperator.I32_GE_U, WasmNumericOperator.I64_GE_U)
        }
    }

    private fun validateBinaryDomain(
        operator: ChirBinaryOperator,
        left: ChirTypeRef,
        right: ChirTypeRef,
        result: ChirTypeRef,
        semanticId: ChirSemanticId,
    ) {
        if (left != right) throw WasmCodegenException("binary CHIR operands have different Cangjie types", semanticId)
        val floating = mapper.isFloating(left, semanticId)
        val integer = mapper.isInteger(left, semanticId)
        val bool = mapper.isBoolean(left, semanticId)
        val unsigned = mapper.isUnsignedInteger(left, semanticId)
        val allowed = when {
            floating -> operator !in setOf(
                ChirBinaryOperator.UNSIGNED_DIV,
                ChirBinaryOperator.UNSIGNED_REM,
                ChirBinaryOperator.BIT_AND,
                ChirBinaryOperator.BIT_OR,
                ChirBinaryOperator.BIT_XOR,
                ChirBinaryOperator.SHIFT_LEFT,
                ChirBinaryOperator.SIGNED_SHIFT_RIGHT,
                ChirBinaryOperator.UNSIGNED_SHIFT_RIGHT,
                ChirBinaryOperator.UNSIGNED_LESS,
                ChirBinaryOperator.UNSIGNED_LESS_OR_EQUAL,
                ChirBinaryOperator.UNSIGNED_GREATER,
                ChirBinaryOperator.UNSIGNED_GREATER_OR_EQUAL,
            )
            integer && unsigned -> operator !in setOf(
                ChirBinaryOperator.SIGNED_DIV,
                ChirBinaryOperator.SIGNED_REM,
                ChirBinaryOperator.SIGNED_LESS,
                ChirBinaryOperator.SIGNED_LESS_OR_EQUAL,
                ChirBinaryOperator.SIGNED_GREATER,
                ChirBinaryOperator.SIGNED_GREATER_OR_EQUAL,
            )
            integer -> operator !in setOf(
                ChirBinaryOperator.UNSIGNED_DIV,
                ChirBinaryOperator.UNSIGNED_REM,
                ChirBinaryOperator.UNSIGNED_LESS,
                ChirBinaryOperator.UNSIGNED_LESS_OR_EQUAL,
                ChirBinaryOperator.UNSIGNED_GREATER,
                ChirBinaryOperator.UNSIGNED_GREATER_OR_EQUAL,
            )
            bool -> operator in setOf(
                ChirBinaryOperator.EQUAL,
                ChirBinaryOperator.NOT_EQUAL,
                ChirBinaryOperator.BIT_AND,
                ChirBinaryOperator.BIT_OR,
                ChirBinaryOperator.BIT_XOR,
            )
            else -> operator == ChirBinaryOperator.EQUAL || operator == ChirBinaryOperator.NOT_EQUAL
        }
        if (!allowed) {
            throw WasmCodegenException("operator ${operator.canonicalName} is not defined for ${left.renderName}", semanticId)
        }
        if (operator.family == ChirBinaryOperatorFamily.COMPARISON) {
            if (!mapper.isBoolean(result, semanticId)) {
                throw WasmCodegenException("comparison result must have Cangjie Bool type", semanticId)
            }
        } else if (result != left) {
            throw WasmCodegenException("non-comparison result type must match its operands", semanticId)
        }
    }

    private fun lowerMemory(expression: ChirMemoryExpression): List<WasmInstruction> {
        val operation = ChirMemoryOperation.parse(expression.operation)
            ?: throw WasmCodegenException("unsupported CHIR memory operation '${expression.operation}'", expression.semanticId)
        return when (operation) {
            ChirMemoryOperation.LOAD -> {
                val resultType = expression.resultType
                    ?: throw WasmCodegenException("CHIR load has no result type", expression.semanticId)
                val elementType = mapper.pointeeType(expression.address.type, expression.semanticId)
                if (mapper.valueType(elementType, expression.semanticId) != mapper.valueType(resultType, expression.semanticId)) {
                    throw WasmCodegenException("load result does not match pointer element type", expression.semanticId)
                }
                val loadKind = mapper.loadKind(elementType, expression.semanticId)
                valueInstructions(expression.address) + WasmInstruction.Load(
                    kind = loadKind,
                    alignmentExponent = alignmentExponent(loadKind.naturalAlignmentExponent, expression.semanticId, expression.address),
                )
            }
            ChirMemoryOperation.STORE -> {
                val value = expression.value
                    ?: throw WasmCodegenException("CHIR store has no value operand", expression.semanticId)
                val elementType = mapper.pointeeType(expression.address.type, expression.semanticId)
                if (mapper.valueType(elementType, expression.semanticId) != mapper.valueType(value.type, expression.semanticId)) {
                    throw WasmCodegenException("store value does not match pointer element type", expression.semanticId)
                }
                val storeKind = mapper.storeKind(elementType, expression.semanticId)
                valueInstructions(expression.address) + valueInstructions(value) +
                    WasmInstruction.Store(
                        kind = storeKind,
                        alignmentExponent = alignmentExponent(storeKind.naturalAlignmentExponent, expression.semanticId, expression.address, value),
                    )
            }
            ChirMemoryOperation.ALLOCA -> lowerAlloca(expression)
            ChirMemoryOperation.GET_ELEMENT_PTR,
            ChirMemoryOperation.GET_ELEMENT_PTR_INBOUNDS,
            -> {
                val index = expression.value
                    ?: throw WasmCodegenException("CHIR getelementptr has no index operand", expression.semanticId)
                val elementType = mapper.pointeeType(expression.address.type, expression.semanticId)
                val elementSize = mapper.byteSize(elementType, expression.semanticId)
                if (elementSize <= 0) throw WasmCodegenException("pointer arithmetic uses a zero-sized element", expression.semanticId)
                val indexType = mapper.valueType(index.type, expression.semanticId)
                val scaledIndex = buildList {
                    addAll(valueInstructions(index))
                    if (indexType == WasmValueType.I64) add(WasmInstruction.Numeric(WasmNumericOperator.I32_WRAP_I64))
                    else if (indexType != WasmValueType.I32) {
                        throw WasmCodegenException("pointer index must be an integer", expression.semanticId)
                    }
                    if (elementSize != 1) {
                        add(WasmInstruction.I32Const(elementSize))
                        add(WasmInstruction.Numeric(WasmNumericOperator.I32_MUL))
                    }
                }
                valueInstructions(expression.address) + scaledIndex + WasmInstruction.Numeric(WasmNumericOperator.I32_ADD)
            }
        }
    }

    private fun lowerAlloca(expression: ChirMemoryExpression): List<WasmInstruction> {
        val count = expression.value ?: expression.address
        val countType = mapper.valueType(count.type, expression.semanticId)
        if (countType != WasmValueType.I32 && countType != WasmValueType.I64) {
            throw WasmCodegenException("alloca element count must be an integer", expression.semanticId)
        }
        val resultType = expression.resultType
            ?: throw WasmCodegenException("alloca has no pointer result type", expression.semanticId)
        val elementType = mapper.pointeeType(resultType, expression.semanticId)
        val elementSize = mapper.byteSize(elementType, expression.semanticId)
        if (elementSize <= 0) throw WasmCodegenException("alloca cannot allocate a zero-sized type", expression.semanticId)
        val size64 = localLayout.allocationSize64
        val overflowLabel = "alloc_size_overflow_${blockIndexById.size}_${expression.semanticId.value}"
        val maximumCount = 0xffff_ffffL / elementSize.toLong()

        return buildList {
            addAll(valueInstructions(count))
            if (countType == WasmValueType.I32) add(WasmInstruction.Numeric(WasmNumericOperator.I64_EXTEND_I32_U))
            add(WasmInstruction.LocalTee(size64))
            add(WasmInstruction.I64Const(maximumCount))
            add(WasmInstruction.Numeric(WasmNumericOperator.I64_GT_U))
            add(WasmInstruction.If(overflowLabel, listOf(WasmInstruction.Unreachable)))
            add(WasmInstruction.LocalGet(size64))
            add(WasmInstruction.I64Const(elementSize.toLong()))
            add(WasmInstruction.Numeric(WasmNumericOperator.I64_MUL))
            add(WasmInstruction.I64Const(15))
            add(WasmInstruction.Numeric(WasmNumericOperator.I64_ADD))
            add(WasmInstruction.I64Const(-16))
            add(WasmInstruction.Numeric(WasmNumericOperator.I64_AND))
            add(WasmInstruction.LocalTee(size64))
            add(WasmInstruction.I64Const(0xffff_ffffL))
            add(WasmInstruction.Numeric(WasmNumericOperator.I64_GT_U))
            add(WasmInstruction.If("alloc_aligned_size_overflow_${blockIndexById.size}_${expression.semanticId.value}", listOf(WasmInstruction.Unreachable)))
            add(WasmInstruction.LocalGet(size64))
            add(WasmInstruction.Numeric(WasmNumericOperator.I32_WRAP_I64))
            add(WasmInstruction.Call(plan.stackAllocateFunctionName(expression.semanticId)))
        }
    }

    private fun lowerCall(expression: ChirCallExpression): List<WasmInstruction> {
        val calleeId = expression.callee.semanticId
        val directFunctionName = when (val callee = expression.callee) {
            is ChirFunctionValue -> plan.functionSymbolName(callee.semanticId)
            is ChirImportedFunctionValue -> plan.importedFunctionName(callee.semanticId)
            else -> null
        }
        val calleeType = plan.functionTypeById[calleeId] ?: plan.functionTypeByImportedId[calleeId]
            ?: plan.functionTypeForValue(expression.callee.type, expression.semanticId)
        if (calleeType.parameters.size != expression.arguments.size) {
            throw WasmCodegenException("call argument count does not match its CHIR function type", expression.semanticId)
        }
        expression.arguments.zip(calleeType.parameters).forEach { (argument, expected) ->
            if (mapper.valueType(argument.type, expression.semanticId) != expected) {
                throw WasmCodegenException("call argument type does not match function ${calleeId.value}", expression.semanticId)
            }
        }
        val result = mapper.valueTypeOrNull(expression.resultType, expression.semanticId)?.let(::listOf).orEmpty()
        if (result != calleeType.results) {
            throw WasmCodegenException("call result type does not match function ${calleeId.value}", expression.semanticId)
        }
        val arguments = expression.arguments.flatMap(::valueInstructions)
        val call = if (directFunctionName != null) {
            arguments + WasmInstruction.Call(directFunctionName)
        } else {
            arguments + valueInstructions(expression.callee) + WasmInstruction.CallIndirect(calleeType, plan.functionTableName)
        }
        return call + mapper.normalize(expression.resultType, expression.semanticId)
    }

    private fun lowerOther(expression: ChirOtherExpression): List<WasmInstruction> {
        val operation = ChirOtherOperation.parse(expression.operation)
            ?: throw WasmCodegenException("unsupported CHIR operation '${expression.operation}'", expression.semanticId)
        val operands = expression.operands
        return when (operation) {
            ChirOtherOperation.SELECT -> {
                val resultTypeRef = expression.resultType
                if (operands.size != 3 || resultTypeRef == null) {
                    throw WasmCodegenException("malformed CHIR select expression", expression.semanticId)
                }
                requireBoolean(operands[0].type, expression.semanticId, "select condition")
                val resultType = mapper.valueType(resultTypeRef, expression.semanticId)
                if (mapper.valueType(operands[1].type, expression.semanticId) != resultType ||
                    mapper.valueType(operands[2].type, expression.semanticId) != resultType
                ) {
                    throw WasmCodegenException("select value types do not match the result type", expression.semanticId)
                }
                valueInstructions(operands[1]) + valueInstructions(operands[2]) + valueInstructions(operands[0]) +
                    WasmInstruction.Select + mapper.normalize(resultTypeRef, expression.semanticId)
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
            -> lowerCast(operation, expression)
            ChirOtherOperation.PHI -> emptyList()
            ChirOtherOperation.STRING_BUILD,
            ChirOtherOperation.STRING_APPEND,
            ChirOtherOperation.STRING_RESULT,
            -> operands.flatMap(::valueInstructions) + WasmInstruction.Call(plan.runtimeFunctionName(expression.semanticId))
            ChirOtherOperation.TUPLE_PACK -> lowerTuplePack(expression)
            ChirOtherOperation.TUPLE_EXTRACT -> lowerTupleExtract(expression)
            else -> throw WasmCodegenException(
                "CHIR operation '${operation.canonicalName}' has no Wasm Core lowering",
                expression.semanticId,
            )
        }
    }

    private fun lowerCast(operation: ChirOtherOperation, expression: ChirOtherExpression): List<WasmInstruction> {
        val targetTypeRef = expression.resultType
        if (expression.operands.size != 1 || targetTypeRef == null) {
            throw WasmCodegenException("malformed CHIR cast '${operation.canonicalName}'", expression.semanticId)
        }
        val source = expression.operands.single()
        val sourceWasmType = mapper.valueType(source.type, expression.semanticId)
        val targetWasmType = mapper.valueType(targetTypeRef, expression.semanticId)
        val sourceGeneric = genericIdentifier(source.type)
        val targetGeneric = genericIdentifier(targetTypeRef)
        if (isFloat16(source.type) || isFloat16(targetTypeRef) || sourceGeneric != null || targetGeneric != null) {
            val instructions = valueInstructions(source).toMutableList()
            if (sourceGeneric != null || targetGeneric != null) {
                instructions += WasmInstruction.I32Const(WasmRuntimeTypeIds.of(sourceGeneric ?: source.type.renderName))
                instructions += WasmInstruction.I32Const(WasmRuntimeTypeIds.of(targetGeneric ?: targetTypeRef.renderName))
            }
            instructions += WasmInstruction.Call(plan.runtimeFunctionName(expression.semanticId))
            instructions += mapper.normalize(targetTypeRef, expression.semanticId)
            return instructions
        }
        val conversion = when (operation) {
            ChirOtherOperation.BITCAST -> bitcast(sourceWasmType, targetWasmType, expression.semanticId)
            ChirOtherOperation.PTRTOINT -> when {
                sourceWasmType == WasmValueType.I32 && targetWasmType == WasmValueType.I32 -> emptyList()
                sourceWasmType == WasmValueType.I32 && targetWasmType == WasmValueType.I64 ->
                    listOf(WasmInstruction.Numeric(WasmNumericOperator.I64_EXTEND_I32_U))
                else -> throw WasmCodegenException("ptrtoint requires a wasm32 pointer source", expression.semanticId)
            }
            ChirOtherOperation.INTTOPTR -> when {
                sourceWasmType == WasmValueType.I32 && targetWasmType == WasmValueType.I32 -> emptyList()
                sourceWasmType == WasmValueType.I64 && targetWasmType == WasmValueType.I32 ->
                    listOf(WasmInstruction.Numeric(WasmNumericOperator.I32_WRAP_I64))
                else -> throw WasmCodegenException("inttoptr requires an integer source", expression.semanticId)
            }
            ChirOtherOperation.TRUNC -> when {
                sourceWasmType == WasmValueType.I64 && targetWasmType == WasmValueType.I32 ->
                    listOf(WasmInstruction.Numeric(WasmNumericOperator.I32_WRAP_I64))
                sourceWasmType == targetWasmType -> emptyList()
                sourceWasmType == WasmValueType.F64 && targetWasmType == WasmValueType.F32 ->
                    listOf(WasmInstruction.Numeric(WasmNumericOperator.F32_DEMOTE_F64))
                else -> throw WasmCodegenException("unsupported TRUNC cast ${sourceWasmType.displayName} to ${targetWasmType.displayName}", expression.semanticId)
            }
            ChirOtherOperation.ZEXT, ChirOtherOperation.SEXT -> when {
                sourceWasmType == WasmValueType.I32 && targetWasmType == WasmValueType.I64 -> listOf(
                    WasmInstruction.Numeric(
                        if (operation == ChirOtherOperation.SEXT) WasmNumericOperator.I64_EXTEND_I32_S
                        else WasmNumericOperator.I64_EXTEND_I32_U,
                    ),
                )
                sourceWasmType == targetWasmType -> emptyList()
                else -> throw WasmCodegenException("unsupported integer extension ${sourceWasmType.displayName} to ${targetWasmType.displayName}", expression.semanticId)
            }
            ChirOtherOperation.FPTRUNC -> if (sourceWasmType == WasmValueType.F64 && targetWasmType == WasmValueType.F32) {
                listOf(WasmInstruction.Numeric(WasmNumericOperator.F32_DEMOTE_F64))
            } else throw WasmCodegenException("fptrunc requires f64 to f32", expression.semanticId)
            ChirOtherOperation.FPEXT -> if (sourceWasmType == WasmValueType.F32 && targetWasmType == WasmValueType.F64) {
                listOf(WasmInstruction.Numeric(WasmNumericOperator.F64_PROMOTE_F32))
            } else throw WasmCodegenException("fpext requires f32 to f64", expression.semanticId)
            ChirOtherOperation.SITOFP, ChirOtherOperation.UITOFP -> integerToFloat(
                sourceWasmType,
                targetWasmType,
                signed = operation == ChirOtherOperation.SITOFP,
                semanticId = expression.semanticId,
            )
            ChirOtherOperation.FPTOSI, ChirOtherOperation.FPTOUI -> floatToInteger(
                sourceWasmType,
                targetWasmType,
                signed = operation == ChirOtherOperation.FPTOSI,
                semanticId = expression.semanticId,
            )
            else -> throw WasmCodegenException("unsupported CHIR cast '${operation.canonicalName}'", expression.semanticId)
        }
        return valueInstructions(source) + conversion + mapper.normalize(targetTypeRef, expression.semanticId)
    }

    private fun bitcast(from: WasmValueType, to: WasmValueType, semanticId: ChirSemanticId): List<WasmInstruction> = when {
        from == to -> emptyList()
        from == WasmValueType.I32 && to == WasmValueType.F32 -> listOf(WasmInstruction.Numeric(WasmNumericOperator.F32_REINTERPRET_I32))
        from == WasmValueType.F32 && to == WasmValueType.I32 -> listOf(WasmInstruction.Numeric(WasmNumericOperator.I32_REINTERPRET_F32))
        from == WasmValueType.I64 && to == WasmValueType.F64 -> listOf(WasmInstruction.Numeric(WasmNumericOperator.F64_REINTERPRET_I64))
        from == WasmValueType.F64 && to == WasmValueType.I64 -> listOf(WasmInstruction.Numeric(WasmNumericOperator.I64_REINTERPRET_F64))
        else -> throw WasmCodegenException("invalid Wasm bitcast ${from.displayName} to ${to.displayName}", semanticId)
    }

    private fun integerToFloat(
        from: WasmValueType,
        to: WasmValueType,
        signed: Boolean,
        semanticId: ChirSemanticId,
    ): List<WasmInstruction> {
        val operator = when (from to to) {
            WasmValueType.I32 to WasmValueType.F32 -> if (signed) WasmNumericOperator.F32_CONVERT_I32_S else WasmNumericOperator.F32_CONVERT_I32_U
            WasmValueType.I32 to WasmValueType.F64 -> if (signed) WasmNumericOperator.F64_CONVERT_I32_S else WasmNumericOperator.F64_CONVERT_I32_U
            WasmValueType.I64 to WasmValueType.F32 -> if (signed) WasmNumericOperator.F32_CONVERT_I64_S else WasmNumericOperator.F32_CONVERT_I64_U
            WasmValueType.I64 to WasmValueType.F64 -> if (signed) WasmNumericOperator.F64_CONVERT_I64_S else WasmNumericOperator.F64_CONVERT_I64_U
            else -> throw WasmCodegenException("invalid integer-to-float cast ${from.displayName} to ${to.displayName}", semanticId)
        }
        return listOf(WasmInstruction.Numeric(operator))
    }

    private fun floatToInteger(
        from: WasmValueType,
        to: WasmValueType,
        signed: Boolean,
        semanticId: ChirSemanticId,
    ): List<WasmInstruction> {
        val operator = when (from to to) {
            WasmValueType.F32 to WasmValueType.I32 -> if (signed) WasmNumericOperator.I32_TRUNC_F32_S else WasmNumericOperator.I32_TRUNC_F32_U
            WasmValueType.F64 to WasmValueType.I32 -> if (signed) WasmNumericOperator.I32_TRUNC_F64_S else WasmNumericOperator.I32_TRUNC_F64_U
            WasmValueType.F32 to WasmValueType.I64 -> if (signed) WasmNumericOperator.I64_TRUNC_F32_S else WasmNumericOperator.I64_TRUNC_F32_U
            WasmValueType.F64 to WasmValueType.I64 -> if (signed) WasmNumericOperator.I64_TRUNC_F64_S else WasmNumericOperator.I64_TRUNC_F64_U
            else -> throw WasmCodegenException("invalid float-to-integer cast ${from.displayName} to ${to.displayName}", semanticId)
        }
        return listOf(WasmInstruction.Numeric(operator))
    }

    private fun lowerTuplePack(expression: ChirOtherExpression): List<WasmInstruction> {
        val resultType = expression.resultType
            ?: throw WasmCodegenException("tuple pack has no result type", expression.semanticId)
        val tupleType = (resultType as? ChirResolvedTypeRef)?.type as? org.cangnova.cangjie.chir.core.type.ChirTupleType
            ?: throw WasmCodegenException("tuple pack result is not a resolved tuple type", expression.semanticId)
        if (tupleType.elementTypes.size != expression.operands.size) {
            throw WasmCodegenException("tuple pack element count does not match its tuple type", expression.semanticId)
        }
        val layout = mapper.fieldLayout(tupleType.elementTypes, expression.semanticId)
        val pointerLocal = localLayout.tuplePointer
        val instructions = mutableListOf<WasmInstruction>(
            WasmInstruction.I32Const(maxOf(layout.size, 1)),
            WasmInstruction.Call(plan.allocationFunctionName(expression.semanticId)),
            WasmInstruction.LocalSet(pointerLocal),
        )
        expression.operands.forEachIndexed { index, value ->
            if (mapper.valueType(value.type, expression.semanticId) != mapper.valueType(tupleType.elementTypes[index], expression.semanticId)) {
                throw WasmCodegenException("tuple pack element $index has the wrong Wasm type", expression.semanticId)
            }
            instructions += WasmInstruction.LocalGet(pointerLocal)
            instructions += valueInstructions(value)
            instructions += WasmInstruction.Store(
                mapper.storeKind(tupleType.elementTypes[index], expression.semanticId),
                offset = layout.fieldOffsets[index].toUInt(),
            )
        }
        instructions += WasmInstruction.LocalGet(pointerLocal)
        return instructions
    }

    private fun lowerTupleExtract(expression: ChirOtherExpression): List<WasmInstruction> {
        val resultType = expression.resultType
        if (expression.operands.size != 2 || resultType == null) {
            throw WasmCodegenException("malformed tuple extract operation", expression.semanticId)
        }
        val tupleValue = expression.operands[0]
        val tupleType = (tupleValue.type as? ChirResolvedTypeRef)?.type as? org.cangnova.cangjie.chir.core.type.ChirTupleType
            ?: throw WasmCodegenException("tuple extract source is not a resolved tuple", expression.semanticId)
        val fieldIndex = (expression.operands[1] as? ChirConstantValue)?.literal?.toIntOrNull()
            ?: throw WasmCodegenException("tuple extract index must be an integer constant", expression.semanticId)
        val expectedType = tupleType.elementTypes.getOrNull(fieldIndex)
            ?: throw WasmCodegenException("tuple extract index $fieldIndex is out of range", expression.semanticId)
        if (mapper.valueType(expectedType, expression.semanticId) != mapper.valueType(resultType, expression.semanticId)) {
            throw WasmCodegenException("tuple extract result type does not match its field", expression.semanticId)
        }
        val offset = mapper.tupleFieldOffset(tupleType, fieldIndex, expression.semanticId).toUInt()
        return valueInstructions(tupleValue) + WasmInstruction.Load(
            mapper.loadKind(expectedType, expression.semanticId),
            offset = offset,
        )
    }

    private fun lowerBlock(block: ChirBlock): LoweredBlock {
        val instructions = mutableListOf<WasmInstruction>()
        block.expressions.forEach { expression ->
            if (expression is ChirOtherExpression && ChirOtherOperation.parse(expression.operation) == ChirOtherOperation.PHI) {
                return@forEach
            }
            val expressionInstructions = lowerExpression(expression)
            instructions += expressionInstructions
            localLayout.resultLocal(expression.semanticId)?.let { instructions += WasmInstruction.LocalSet(it) }
        }
        val terminator = lowerTerminator(block)
        instructions += terminator.instructions
        return LoweredBlock(instructions, terminator.branchesToDispatcher)
    }

    private fun lowerTerminator(block: ChirBlock): LoweredTerminator = when (val terminator = block.terminator) {
        is ChirReturnTerminator -> lowerReturn(terminator)
        is ChirBranchTerminator -> LoweredTerminator(
            edgeInstructions(block.semanticId, terminator.targetBlockId, terminator.semanticId),
            branchesToDispatcher = true,
        )
        is ChirConditionalBranchTerminator -> {
            requireBoolean(terminator.condition.type, terminator.semanticId, "conditional branch")
            LoweredTerminator(
                instructions = valueInstructions(terminator.condition) + WasmInstruction.If(
                    label = "cond_${blockIndexById.getValue(block.semanticId)}_${blockIndexById.getValue(terminator.trueTargetBlockId)}_${blockIndexById.getValue(terminator.falseTargetBlockId)}",
                    thenBody = edgeInstructions(block.semanticId, terminator.trueTargetBlockId, terminator.semanticId),
                    elseBody = edgeInstructions(block.semanticId, terminator.falseTargetBlockId, terminator.semanticId),
                ),
                branchesToDispatcher = true,
            )
        }
        is ChirThrowTerminator -> lowerThrow(block, terminator)
        is ChirUnwindTerminator -> LoweredTerminator(
            edgeInstructions(block.semanticId, terminator.targetBlockId, terminator.semanticId),
            branchesToDispatcher = true,
        )
        else -> throw WasmCodegenException("unsupported CHIR terminator ${terminator::class.simpleName}", terminator.semanticId)
    }

    private fun edgeInstructions(
        predecessorId: ChirSemanticId,
        targetId: ChirSemanticId,
        semanticId: ChirSemanticId,
    ): List<WasmInstruction> {
        val targetIndex = blockIndexById[targetId]
            ?: throw WasmCodegenException("branch target ${targetId.value} is outside '${declaration.name}'", semanticId)
        val result = mutableListOf<WasmInstruction>()
        phiExpressionsByBlock[targetId].orEmpty().forEach { phi ->
            val incoming = phi.operands.firstOrNull { value -> resolvePredecessor(value, phi.semanticId) == predecessorId }
                ?: throw WasmCodegenException(
                    "phi ${phi.semanticId.value} has no input for predecessor ${predecessorId.value}",
                    phi.semanticId,
                )
            result += valueInstructions(incoming)
            result += WasmInstruction.LocalSet(localLayout.resultLocal(phi.semanticId)!!)
        }
        result += WasmInstruction.I32Const(targetIndex)
        result += WasmInstruction.LocalSet(localLayout.programCounter)
        return result
    }

    private fun validatePhiInputs() {
        phiExpressionsByBlock.forEach { (target, phis) ->
            val predecessors = predecessorsByBlock[target].orEmpty()
            phis.forEach { phi ->
                val incoming = phi.operands.mapTo(hashSetOf()) { resolvePredecessor(it, phi.semanticId) }
                val expected = predecessors
                if (incoming != expected) {
                    throw WasmCodegenException(
                        "phi ${phi.semanticId.value} predecessors ${incoming.map { it.value }.sorted()} do not match CFG predecessors ${expected.map { it.value }.sorted()}",
                        phi.semanticId,
                    )
                }
            }
        }
    }

    private fun collectPhis(): Map<ChirSemanticId, List<ChirOtherExpression>> = blocks.associate { block ->
        block.semanticId to block.expressions.filterIsInstance<ChirOtherExpression>().filter {
            ChirOtherOperation.parse(it.operation) == ChirOtherOperation.PHI
        }
    }

    private fun collectPredecessors(): Map<ChirSemanticId, Set<ChirSemanticId>> {
        val result = blocks.associate { it.semanticId to linkedSetOf<ChirSemanticId>() }.toMutableMap()
        blocks.forEach { block ->
            when (val terminator = block.terminator) {
                is ChirBranchTerminator -> result[terminator.targetBlockId]?.add(block.semanticId)
                is ChirConditionalBranchTerminator -> {
                    result[terminator.trueTargetBlockId]?.add(block.semanticId)
                    result[terminator.falseTargetBlockId]?.add(block.semanticId)
                }
                is ChirUnwindTerminator -> result[terminator.targetBlockId]?.add(block.semanticId)
                is ChirThrowTerminator -> {
                    if (terminator.catchDispatches.isEmpty()) {
                        terminator.unwindTargetBlockId?.let { result[it]?.add(block.semanticId) }
                    } else {
                        terminator.catchDispatches.forEach { result[it.catchBlockId]?.add(block.semanticId) }
                    }
                }
                is ChirReturnTerminator -> Unit
                else -> throw WasmCodegenException("unsupported CHIR terminator in CFG", terminator.semanticId)
            }
        }
        return result
    }

    private fun orderBlocks(): List<ChirBlock> {
        if (declaration.blocks.isEmpty()) throw WasmCodegenException("function '${declaration.name}' has no CHIR blocks", declaration.semanticId)
        val byId = declaration.blocks.associateBy(ChirBlock::semanticId)
        if (byId.size != declaration.blocks.size) throw WasmCodegenException("function '${declaration.name}' has duplicate CHIR block ids", declaration.semanticId)
        val entry = byId[declaration.entryBlockId]
            ?: throw WasmCodegenException("function '${declaration.name}' has no entry block", declaration.entryBlockId)
        return listOf(entry) + declaration.blocks.filterNot { it.semanticId == entry.semanticId }.sortedBy { it.semanticId.value }
    }

    private fun validateFunctionShape() {
        if (declaration.parameters.size != signature.parameters.size) {
            throw WasmCodegenException("function parameter count differs from its Wasm signature", declaration.semanticId)
        }
        declaration.parameters.zip(signature.parameters).forEach { (parameter, wasmType) ->
            if (mapper.valueType(parameter.type, parameter.semanticId) != wasmType) {
                throw WasmCodegenException("parameter '${parameter.name}' has an inconsistent Wasm type", parameter.semanticId)
            }
        }
        blocks.forEach { block ->
            when (val terminator = block.terminator) {
                is ChirBranchTerminator -> requireBlock(terminator.targetBlockId, terminator.semanticId)
                is ChirConditionalBranchTerminator -> {
                    requireBlock(terminator.trueTargetBlockId, terminator.semanticId)
                    requireBlock(terminator.falseTargetBlockId, terminator.semanticId)
                }
                is ChirUnwindTerminator -> requireBlock(terminator.targetBlockId, terminator.semanticId)
                is ChirThrowTerminator -> {
                    terminator.unwindTargetBlockId?.let { requireBlock(it, terminator.semanticId) }
                    terminator.catchDispatches.forEach { requireBlock(it.catchBlockId, terminator.semanticId) }
                }
                is ChirReturnTerminator -> Unit
                else -> throw WasmCodegenException("unsupported CHIR terminator", terminator.semanticId)
            }
        }
    }

    private fun requireBlock(id: ChirSemanticId, sourceId: ChirSemanticId) {
        if (id !in blockIndexById) throw WasmCodegenException("branch references missing block ${id.value}", sourceId)
    }

    private fun resolvePredecessor(value: ChirValue, sourceId: ChirSemanticId): ChirSemanticId {
        val reference = value.predecessorReference()
            ?: throw WasmCodegenException("phi input ${value.semanticId.value} is missing its predecessor id", sourceId)
        val matches = blocks.filter { it.semanticId.value == reference || it.name == reference }
        if (matches.size != 1) {
            throw WasmCodegenException("phi predecessor '$reference' is missing or ambiguous", sourceId)
        }
        return matches.single().semanticId
    }

    private fun valueInstructions(value: ChirValue): List<WasmInstruction> = when (value) {
        is ChirConstantValue -> WasmLiteralParser.instruction(value, mapper, plan.dataPool)
        is ChirLocalValue -> listOf(WasmInstruction.LocalGet(localLayout.valueLocal(value.semanticId)))
        is ChirParameterValue -> listOf(WasmInstruction.LocalGet(localLayout.valueLocal(value.semanticId)))
        is ChirGlobalValue -> listOf(WasmInstruction.GlobalGet(plan.globalNameById[value.semanticId]
            ?: throw WasmCodegenException("global ${value.semanticId.value} is not declared in the package", value.semanticId))) +
            mapper.normalize(value.type, value.semanticId)
        is ChirImportedVariableValue -> listOf(WasmInstruction.GlobalGet(plan.importedGlobalName(value.semanticId))) +
            mapper.normalize(value.type, value.semanticId)
        is ChirBlockValue, is ChirBlockGroupValue -> throw WasmCodegenException(
            "CHIR basic block value cannot be used as a Wasm scalar",
            value.semanticId,
        )
        is ChirFunctionValue, is ChirImportedFunctionValue ->
            listOf(WasmInstruction.I32Const(plan.functionTableIndex(value.semanticId).toInt()))
    }

    private fun requireBoolean(type: ChirTypeRef, semanticId: ChirSemanticId, subject: String) {
        if (type != ChirResolvedTypeRef(ChirPrimitiveType.BOOL)) {
            throw WasmCodegenException("$subject must have Cangjie Bool type, got ${type.renderName}", semanticId)
        }
    }

    private fun requireI32(type: ChirTypeRef, semanticId: ChirSemanticId, subject: String) {
        if (mapper.valueType(type, semanticId) != WasmValueType.I32) {
            throw WasmCodegenException("$subject must use the wasm32 i32 representation", semanticId)
        }
    }

    private fun alignmentExponent(natural: UInt, semanticId: ChirSemanticId, vararg values: ChirValue): UInt {
        val alignmentText = values.asSequence()
            .flatMap { value -> value.attributes.asSequence().filterIsInstance<ChirStringAttribute>() }
            .firstOrNull { it.key == "align" }
            ?.value
            ?: return natural
        val requested = alignmentText.toUIntOrNull()
            ?: throw WasmCodegenException("invalid CHIR memory alignment '$alignmentText'", semanticId)
        if (requested == 0u || requested and (requested - 1u) != 0u) {
            throw WasmCodegenException("CHIR memory alignment must be a positive power of two", semanticId)
        }
        val exponent = requested.countTrailingZeroBits().toUInt()
        return minOf(exponent, natural)
    }

    private fun stableTypeId(typeName: String): Int {
        return WasmRuntimeTypeIds.of(typeName)
    }

    private fun genericIdentifier(typeRef: ChirTypeRef): String? =
        ((typeRef as? ChirResolvedTypeRef)?.type as? ChirGenericType)?.identifier

    private fun isFloat16(typeRef: ChirTypeRef): Boolean =
        (typeRef as? ChirResolvedTypeRef)?.type == ChirPrimitiveType.FLOAT16

    private fun ChirValue.predecessorReference(): String? = attributes
        .filterIsInstance<ChirStringAttribute>()
        .firstOrNull { it.key == "pred" }
        ?.value

    private data class LoweredBlock(val instructions: List<WasmInstruction>, val branchesToDispatcher: Boolean)
    private data class LoweredTerminator(val instructions: List<WasmInstruction>, val branchesToDispatcher: Boolean)

    private class WasmFunctionLocalLayout(
        private val declaration: ChirFunctionDeclaration,
        private val mapper: WasmTypeMapper,
    ) {
        private val valueLocalById = linkedMapOf<ChirSemanticId, WasmLocal>()
        private val localTypeByName = linkedMapOf<String, WasmValueType>()
        private val localNamesByChirName = linkedMapOf<String, MutableList<WasmLocal>>()
        private var nextValueLocal = 0
        private val generatedLocals = mutableListOf<WasmLocal>()
        private val scratchLocals = mutableListOf<WasmLocal>()

        val parameters = declaration.parameters.mapIndexed { index, parameter ->
            WasmLocal("arg_$index", mapper.valueType(parameter.type, parameter.semanticId)).also { local ->
                putValue(parameter.semanticId, local)
            }
        }

        val programCounter = scratch("pc", WasmValueType.I32)
        val usesStack = declaration.blocks.any { block ->
            block.expressions.any { expression ->
                expression is ChirMemoryExpression &&
                    ChirMemoryOperation.parse(expression.operation) == ChirMemoryOperation.ALLOCA
            }
        }
        val frameBase = if (usesStack) scratch("frame_base", WasmValueType.I32) else ""
        val allocationSize64 = scratch("alloc_size64", WasmValueType.I64)
        val tuplePointer = scratch("tuple_pointer", WasmValueType.I32)
        val exceptionValue = scratch("exception", WasmValueType.I32)
        val returnValue: String? = declaration.returnType.takeIf { mapper.valueTypeOrNull(it, declaration.semanticId) != null }
            ?.let { scratch("return_value", mapper.valueType(it, declaration.semanticId)) }

        val locals: List<WasmLocal>
            get() = generatedLocals + scratchLocals

        init {
            declaration.blocks.forEach { block ->
                block.expressions.forEach { expression ->
                    expression.resultType?.let { type ->
                        val wasmType = mapper.valueTypeOrNull(type, expression.semanticId)
                        if (wasmType != null) putValue(expression.semanticId, WasmLocal("value_${nextValueLocal++}", wasmType))
                    }
                    expression.values().forEach(::collectValue)
                }
                block.terminator.values().forEach(::collectValue)
            }
        }

        fun resultLocal(semanticId: ChirSemanticId): String? = valueLocalById[semanticId]?.name

        fun valueLocal(semanticId: ChirSemanticId): String = valueLocalById[semanticId]?.name
            ?: throw WasmCodegenException("CHIR local/parameter ${semanticId.value} has no Wasm local", semanticId)

        fun typeOf(localName: String): WasmValueType? = localTypeByName[localName]

        fun sourceLocal(name: String): String? {
            val candidates = localNamesByChirName[name].orEmpty()
            if (candidates.size > 1) throw WasmCodegenException("CHIR local name '$name' is ambiguous", declaration.semanticId)
            return candidates.singleOrNull()?.name
        }

        private fun collectValue(value: ChirValue) {
            when (value) {
                is ChirLocalValue -> {
                    val type = mapper.valueType(value.type, value.semanticId)
                    val local = valueLocalById[value.semanticId]
                    if (local != null) {
                        if (local.type != type) throw WasmCodegenException("CHIR local ${value.semanticId.value} has conflicting types", value.semanticId)
                        localNamesByChirName.getOrPut(value.name) { mutableListOf() }.addUnique(local)
                    } else {
                        val created = WasmLocal("value_${nextValueLocal++}", type)
                        putValue(value.semanticId, created)
                        localNamesByChirName.getOrPut(value.name) { mutableListOf() }.addUnique(created)
                    }
                }
                is ChirParameterValue -> {
                    val parameter = valueLocalById[value.semanticId]
                        ?: throw WasmCodegenException("CHIR parameter ${value.semanticId.value} is not declared", value.semanticId)
                    if (parameter.type != mapper.valueType(value.type, value.semanticId)) {
                        throw WasmCodegenException("CHIR parameter ${value.semanticId.value} has conflicting types", value.semanticId)
                    }
                    localNamesByChirName.getOrPut(value.name) { mutableListOf() }.addUnique(parameter)
                }
                else -> Unit
            }
        }

        private fun scratch(suffix: String, type: WasmValueType): String {
            val localName = "__wasm_$suffix"
            scratchLocals += WasmLocal(localName, type)
            localTypeByName[localName] = type
            return localName
        }

        private fun putValue(semanticId: ChirSemanticId, local: WasmLocal) {
            val old = valueLocalById.putIfAbsent(semanticId, local)
            if (old != null && old.type != local.type) {
                throw WasmCodegenException("CHIR value ${semanticId.value} has inconsistent Wasm types", semanticId)
            }
            if (old == null) {
                if (local.name.startsWith("value_")) generatedLocals += local
                localTypeByName[local.name] = local.type
            }
        }

        private fun MutableList<WasmLocal>.addUnique(local: WasmLocal) {
            if (none { it.name == local.name }) add(local)
        }
    }
}

private fun ChirExpression.values(): List<ChirValue> = when (this) {
    is org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression -> listOf(operand)
    is ChirBinaryExpression -> listOf(left, right)
    is ChirMemoryExpression -> listOfNotNull(address, value)
    is ChirCallExpression -> listOf(callee) + arguments
    is ChirOtherExpression -> operands
    else -> throw WasmCodegenException("unsupported CHIR expression ${this::class.simpleName}", semanticId)
}

private fun org.cangnova.cangjie.chir.core.controlflow.ChirTerminator.values(): List<ChirValue> = when (this) {
    is ChirReturnTerminator -> listOfNotNull(returnValue)
    is ChirBranchTerminator -> emptyList()
    is ChirConditionalBranchTerminator -> listOf(condition)
    is ChirThrowTerminator -> listOf(exceptionValue)
    is ChirUnwindTerminator -> emptyList()
    else -> throw WasmCodegenException("unsupported CHIR terminator ${this::class.simpleName}", semanticId)
}
