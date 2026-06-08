package org.cangnova.cangjie.jvm.codegen.function

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRefType
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
import org.cangnova.cangjie.jvm.codegen.context.JvmAbiAttributes
import org.cangnova.cangjie.jvm.codegen.context.JvmBackendContext
import org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * JVM method 生成器。函数参数与表达式结果通过 CHIR semanticId 绑定到 JVM local slot。
 */
class JvmFunctionCodegen(
    private val context: JvmBackendContext,
    private val module: ChirModule,
    private val ownerInternalName: String,
    private val classWriter: ClassWriter,
    private val function: ChirFunctionDeclaration,
    private val methodAccess: Int = Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
    private val isStaticMethod: Boolean = true,
) {
    private val methodName: String = context.namePolicy.functionJvmName(function)
    private val parameterTypes: List<Type> = function.parameters.map { context.typeMapper.mapValueType(it.type) }
    private val returnType: Type = context.typeMapper.mapReturnType(function.returnType)
    private val methodDescriptor: String = Type.getMethodDescriptor(returnType, *parameterTypes.toTypedArray())
    private val blockLabels: Map<ChirSemanticId, Label> = function.blocks.associate { it.semanticId to Label() }
    private val localSlots = linkedMapOf<ChirSemanticId, LocalSlot>()
    private val localSlotsByName = linkedMapOf<String, LocalSlot>()
    private var nextLocalSlot: Int = 0

    fun generate() {
        verifyFunctionShape()
        val method = classWriter.visitMethod(
            methodAccess,
            methodName,
            methodDescriptor,
            null,
            null,
        )
        method.visitCode()
        initializeParameterSlots()
        orderedBlocks().forEach { block ->
            method.visitLabel(labelFor(block.semanticId))
            block.expressions.forEach { expression -> emitExpression(method, expression) }
            emitTerminator(method, block.terminator)
        }
        method.visitMaxs(0, 0)
        method.visitEnd()
    }

    fun generateMainBridge() {
        val bridge = classWriter.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "main",
            "([Ljava/lang/String;)V",
            null,
            null,
        )
        bridge.visitCode()
        bridge.visitMethodInsn(Opcodes.INVOKESTATIC, ownerInternalName, methodName, methodDescriptor, false)
        bridge.visitInsn(Opcodes.RETURN)
        bridge.visitMaxs(0, 0)
        bridge.visitEnd()
    }

    private fun initializeParameterSlots() {
        if (!isStaticMethod) {
            nextLocalSlot = 1
        }
        function.parameters.forEachIndexed { index, parameter ->
            val type = parameterTypes[index]
            bindLocal(parameter.semanticId, parameter.name, LocalSlot(nextLocalSlot, type))
            nextLocalSlot += type.size
        }
    }

    private fun emitExpression(method: MethodVisitor, expression: ChirExpression) {
        when (expression) {
            is ChirUnaryExpression -> emitUnaryExpression(method, expression)
            is ChirBinaryExpression -> emitBinaryExpression(method, expression)
            is ChirCallExpression -> emitCallExpression(method, expression)
            is ChirMemoryExpression -> emitMemoryExpression(method, expression)
            is ChirOtherExpression -> emitOtherExpression(method, expression)
            else -> throw JvmCodegenException("unsupported expression ${expression::class.simpleName}", expression.semanticId)
        }
    }

    private fun emitUnaryExpression(method: MethodVisitor, expression: ChirUnaryExpression) {
        val resultType = context.typeMapper.mapValueType(expression.resultType)
        emitValue(method, expression.operand)
        when (expression.operator.lowercase().trim()) {
            "neg", "ineg" -> method.visitInsn(resultType.getOpcode(Opcodes.INEG))
            "fneg" -> method.visitInsn(resultType.getOpcode(Opcodes.FNEG))
            "copy", "mov", "identity" -> Unit
            "not", "bitnot" -> {
                requireIntLikeType(resultType, expression.semanticId)
                emitMinusOne(method, resultType)
                method.visitInsn(resultType.getOpcode(Opcodes.IXOR))
            }
            "logical_not", "lnot" -> {
                requireBoolean(resultType, expression.semanticId)
                val trueLabel = Label()
                val endLabel = Label()
                method.visitJumpInsn(Opcodes.IFEQ, trueLabel)
                method.visitInsn(Opcodes.ICONST_0)
                method.visitJumpInsn(Opcodes.GOTO, endLabel)
                method.visitLabel(trueLabel)
                method.visitInsn(Opcodes.ICONST_1)
                method.visitLabel(endLabel)
            }
            else -> throw JvmCodegenException("unsupported JVM unary operator '${expression.operator}'", expression.semanticId)
        }
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    private fun emitBinaryExpression(method: MethodVisitor, expression: ChirBinaryExpression) {
        val resultType = context.typeMapper.mapValueType(expression.resultType)
        val operandType = context.typeMapper.mapValueType(expression.left.type)
        emitValue(method, expression.left)
        emitValue(method, expression.right)
        when (val op = expression.operator.lowercase().trim()) {
            "add", "+", "plus" -> emitArithmetic(method, expression.semanticId, operandType, Opcodes.IADD)
            "sub", "-", "minus" -> emitArithmetic(method, expression.semanticId, operandType, Opcodes.ISUB)
            "mul", "*", "times" -> emitArithmetic(method, expression.semanticId, operandType, Opcodes.IMUL)
            "div", "/" -> emitArithmetic(method, expression.semanticId, operandType, Opcodes.IDIV)
            "udiv" -> emitUnsignedDivRem(method, expression.semanticId, operandType, "divideUnsigned")
            "rem", "%" -> emitArithmetic(method, expression.semanticId, operandType, Opcodes.IREM)
            "urem" -> emitUnsignedDivRem(method, expression.semanticId, operandType, "remainderUnsigned")
            "and" -> emitIntLike(method, expression.semanticId, operandType, Opcodes.IAND)
            "or" -> emitIntLike(method, expression.semanticId, operandType, Opcodes.IOR)
            "xor" -> emitIntLike(method, expression.semanticId, operandType, Opcodes.IXOR)
            "shl" -> emitShift(method, expression.semanticId, operandType, Opcodes.ISHL)
            "ashr" -> emitShift(method, expression.semanticId, operandType, Opcodes.ISHR)
            "lshr" -> emitShift(method, expression.semanticId, operandType, Opcodes.IUSHR)
            "eq", "==" -> emitComparison(method, operandType, Opcodes.IF_ICMPEQ, Opcodes.IFEQ)
            "ne", "!=" -> emitComparison(method, operandType, Opcodes.IF_ICMPNE, Opcodes.IFNE)
            "lt", "<", "slt" -> emitComparison(method, operandType, Opcodes.IF_ICMPLT, Opcodes.IFLT)
            "le", "<=", "sle" -> emitComparison(method, operandType, Opcodes.IF_ICMPLE, Opcodes.IFLE)
            "gt", ">", "sgt" -> emitComparison(method, operandType, Opcodes.IF_ICMPGT, Opcodes.IFGT)
            "ge", ">=", "sge" -> emitComparison(method, operandType, Opcodes.IF_ICMPGE, Opcodes.IFGE)
            "ult" -> emitUnsignedComparison(method, expression.semanticId, operandType, Opcodes.IFLT)
            "ule" -> emitUnsignedComparison(method, expression.semanticId, operandType, Opcodes.IFLE)
            "ugt" -> emitUnsignedComparison(method, expression.semanticId, operandType, Opcodes.IFGT)
            "uge" -> emitUnsignedComparison(method, expression.semanticId, operandType, Opcodes.IFGE)
            "feq" -> emitComparison(method, operandType, Opcodes.IF_ICMPEQ, Opcodes.IFEQ)
            "fne" -> emitComparison(method, operandType, Opcodes.IF_ICMPNE, Opcodes.IFNE)
            "flt" -> emitComparison(method, operandType, Opcodes.IF_ICMPLT, Opcodes.IFLT)
            "fle" -> emitComparison(method, operandType, Opcodes.IF_ICMPLE, Opcodes.IFLE)
            "fgt" -> emitComparison(method, operandType, Opcodes.IF_ICMPGT, Opcodes.IFGT)
            "fge" -> emitComparison(method, operandType, Opcodes.IF_ICMPGE, Opcodes.IFGE)
            else -> throw JvmCodegenException("unsupported JVM binary operator '$op'", expression.semanticId)
        }
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    private fun emitCallExpression(method: MethodVisitor, expression: ChirCallExpression) {
        val callee = expression.callee
        val functionType = (callee.type as? ChirResolvedTypeRef)?.type as? ChirFunctionType
            ?: throw JvmCodegenException("JVM call callee must carry ChirFunctionType", expression.semanticId)
        val descriptor = context.typeMapper.methodDescriptor(functionType.returnType, functionType.parameterTypes)
        val resultType = context.typeMapper.mapReturnType(expression.resultType)
        when (callee) {
            is ChirFunctionValue -> {
                if (functionType.receiverType != null) {
                    throw JvmCodegenException("local JVM function calls do not support receiver functions yet", expression.semanticId)
                }
                expression.arguments.forEach { emitValue(method, it) }
                method.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    ownerInternalName,
                    context.namePolicy.functionJvmName(resolveLocalFunction(callee.name, descriptor, expression.semanticId)),
                    descriptor,
                    false,
                )
            }
            is ChirImportedFunctionValue -> emitImportedFunctionCall(method, expression, callee, functionType, descriptor)
            else -> throw JvmCodegenException(
                "unsupported JVM call callee value ${callee::class.simpleName}",
                expression.semanticId,
            )
        }
        if (resultType != Type.VOID_TYPE) {
            storeExpressionResult(method, expression.semanticId, resultType)
        }
    }

    /**
     * 根据 CHIR imported function 上的显式 JVM ABI 属性生成调用。
     *
     * receiverType 不参与 JVM method descriptor，它只规定 CHIR 调用参数中的第一个值是实例 receiver。
     */
    private fun emitImportedFunctionCall(
        method: MethodVisitor,
        expression: ChirCallExpression,
        callee: ChirImportedFunctionValue,
        functionType: ChirFunctionType,
        descriptor: String,
    ) {
        val owner = JvmAbiAttributes.requireString(callee.attributes, JvmAbiAttributes.OWNER, callee.semanticId)
        val name = JvmAbiAttributes.requireString(callee.attributes, JvmAbiAttributes.NAME, callee.semanticId)
        val invokeKind = JvmAbiAttributes.requireString(callee.attributes, JvmAbiAttributes.INVOKE_KIND, callee.semanticId)
        val receiverType = functionType.receiverType
        val valueArguments = if (receiverType == null) {
            expression.arguments
        } else {
            if (expression.arguments.isEmpty()) {
                throw JvmCodegenException("imported JVM instance call '$name' requires receiver argument", expression.semanticId)
            }
            expression.arguments
        }
        valueArguments.forEach { emitValue(method, it) }
        when (invokeKind) {
            "static" -> {
                if (receiverType != null) {
                    throw JvmCodegenException("static imported JVM call '$name' must not declare receiverType", expression.semanticId)
                }
                method.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, descriptor, false)
            }
            "virtual" -> {
                requireImportedReceiver(receiverType, expression, name)
                method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, descriptor, false)
            }
            "interface" -> {
                requireImportedReceiver(receiverType, expression, name)
                method.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, name, descriptor, true)
            }
            "special" -> {
                requireImportedReceiver(receiverType, expression, name)
                method.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, descriptor, false)
            }
            else -> throw JvmCodegenException("unsupported JVM invoke kind '$invokeKind'", expression.semanticId)
        }
    }

    private fun requireImportedReceiver(
        receiverType: ChirTypeRef?,
        expression: ChirCallExpression,
        name: String,
    ) {
        if (receiverType == null) {
            throw JvmCodegenException("imported JVM instance call '$name' requires function receiverType", expression.semanticId)
        }
    }

    private fun emitMemoryExpression(method: MethodVisitor, expression: ChirMemoryExpression) {
        when (val operation = expression.operation.lowercase().trim()) {
            "alloca" -> {
                if (expression.value != null || expression.resultType != null) {
                    throw JvmCodegenException("JVM alloca must only carry an address operand", expression.semanticId)
                }
                ensureAddressSlot(expression.address, expression.semanticId)
            }
            "store" -> {
                val value = expression.value
                    ?: throw JvmCodegenException("JVM store requires value operand", expression.semanticId)
                if (expression.resultType != null) {
                    throw JvmCodegenException("JVM store must not carry result type", expression.semanticId)
                }
                val targetType = refTargetType(expression.address, expression.semanticId)
                if (value.type != targetType) {
                    throw JvmCodegenException(
                        "JVM store value type ${value.type.renderName} does not match target ${targetType.renderName}",
                        expression.semanticId,
                    )
                }
                val slot = ensureAddressSlot(expression.address, expression.semanticId)
                emitValue(method, value)
                method.visitVarInsn(slot.type.getOpcode(Opcodes.ISTORE), slot.index)
            }
            "load" -> {
                val targetType = refTargetType(expression.address, expression.semanticId)
                val resultType = expression.resultType
                    ?: throw JvmCodegenException("JVM load requires result type", expression.semanticId)
                if (resultType != targetType) {
                    throw JvmCodegenException(
                        "JVM load result type ${resultType.renderName} does not match target ${targetType.renderName}",
                        expression.semanticId,
                    )
                }
                if (expression.value != null) {
                    throw JvmCodegenException("JVM load must not carry value operand", expression.semanticId)
                }
                val slot = ensureAddressSlot(expression.address, expression.semanticId)
                method.visitVarInsn(slot.type.getOpcode(Opcodes.ILOAD), slot.index)
                storeExpressionResult(method, expression.semanticId, slot.type)
            }
            else -> throw JvmCodegenException("unsupported JVM memory operation '$operation'", expression.semanticId)
        }
    }

    private fun emitOtherExpression(method: MethodVisitor, expression: ChirOtherExpression) {
        when (val operation = expression.operation.lowercase().trim()) {
            "select" -> emitSelectExpression(method, expression)
            "bitcast" -> emitJvmBitcast(method, expression)
            "jvm.new" -> emitJvmNew(method, expression)
            "jvm.getfield" -> emitJvmGetField(method, expression)
            "jvm.putfield" -> emitJvmPutField(method, expression)
            "jvm.getstatic" -> emitJvmGetStatic(method, expression)
            "jvm.putstatic" -> emitJvmPutStatic(method, expression)
            "jvm.newarray" -> emitJvmNewArray(method, expression)
            "jvm.arrayload" -> emitJvmArrayLoad(method, expression)
            "jvm.arraystore" -> emitJvmArrayStore(method, expression)
            "jvm.arraylength" -> emitJvmArrayLength(method, expression)
            "jvm.checkcast" -> emitJvmCheckCast(method, expression)
            "jvm.instanceof" -> emitJvmInstanceOf(method, expression)
            "trunc",
            "zext",
            "sext",
            "fptrunc",
            "fpext",
            "sitofp",
            "uitofp",
            "fptosi",
            "fptoui",
            -> emitNumericCast(method, expression)
            "ptrtoint",
            "inttoptr",
            -> throw JvmCodegenException(
                "JVM backend cannot lower pointer cast '$operation' without an object/address ABI",
                expression.semanticId,
            )
            "phi" -> throw JvmCodegenException(
                "JVM backend requires CFG-level phi lowering before method bytecode generation",
                expression.semanticId,
            )
            else -> throw JvmCodegenException(
                "JVM backend does not support other expression '${expression.operation}'",
                expression.semanticId,
            )
        }
    }

    private fun emitSelectExpression(method: MethodVisitor, expression: ChirOtherExpression) {
        if (expression.operands.size != 3 || expression.resultType == null) {
            throw JvmCodegenException("malformed JVM select expression", expression.semanticId)
        }
        val resultType = expression.resultType
            ?: throw JvmCodegenException("JVM select expression requires result type", expression.semanticId)
        val condition = expression.operands[0]
        val trueValue = expression.operands[1]
        val falseValue = expression.operands[2]
        if (condition.type != ChirResolvedTypeRef(ChirPrimitiveType.BOOL)) {
            throw JvmCodegenException("JVM select condition must be bool", expression.semanticId)
        }
        if (trueValue.type != resultType || falseValue.type != resultType) {
            throw JvmCodegenException("JVM select arm types must match result type", expression.semanticId)
        }
        val falseLabel = Label()
        val endLabel = Label()
        emitValue(method, condition)
        method.visitJumpInsn(Opcodes.IFEQ, falseLabel)
        emitValue(method, trueValue)
        method.visitJumpInsn(Opcodes.GOTO, endLabel)
        method.visitLabel(falseLabel)
        emitValue(method, falseValue)
        method.visitLabel(endLabel)
        storeExpressionResult(method, expression.semanticId, context.typeMapper.mapValueType(resultType))
    }

    private fun emitJvmBitcast(method: MethodVisitor, expression: ChirOtherExpression) {
        val source = expression.singleOperand("bitcast")
        val targetTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM bitcast requires result type", expression.semanticId)
        val sourceType = context.typeMapper.mapValueType(source.type)
        val targetType = context.typeMapper.mapValueType(targetTypeRef)
        if (sourceType != targetType) {
            throw JvmCodegenException(
                "JVM bitcast requires identical carrier types, actual ${sourceType.descriptor} -> ${targetType.descriptor}",
                expression.semanticId,
            )
        }
        emitValue(method, source)
        storeExpressionResult(method, expression.semanticId, targetType)
    }

    private fun emitNumericCast(method: MethodVisitor, expression: ChirOtherExpression) {
        val source = expression.singleOperand(expression.operation)
        val targetTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM cast '${expression.operation}' requires result type", expression.semanticId)
        val sourceType = context.typeMapper.mapValueType(source.type)
        val targetType = context.typeMapper.mapValueType(targetTypeRef)
        emitValue(method, source)
        emitPrimitiveCast(method, expression.semanticId, sourceType, targetType)
        storeExpressionResult(method, expression.semanticId, targetType)
    }

    private fun emitJvmNew(method: MethodVisitor, expression: ChirOtherExpression) {
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM new expression requires result type", expression.semanticId)
        val resultType = context.typeMapper.mapValueType(resultTypeRef)
        val owner = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.OWNER)
            ?: objectInternalName(resultType, expression.semanticId, "new result")
        val descriptor = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: Type.getMethodDescriptor(
                Type.VOID_TYPE,
                *expression.operands.map { context.typeMapper.mapValueType(it.type) }.toTypedArray(),
            )
        method.visitTypeInsn(Opcodes.NEW, owner)
        method.visitInsn(Opcodes.DUP)
        expression.operands.forEach { emitValue(method, it) }
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", descriptor, false)
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    private fun emitJvmGetField(method: MethodVisitor, expression: ChirOtherExpression) {
        val receiver = expression.requireOperandCount("jvm.getField", 1)[0]
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM getField expression requires result type", expression.semanticId)
        val owner = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.OWNER)
            ?: objectInternalName(context.typeMapper.mapValueType(receiver.type), expression.semanticId, "field receiver")
        val name = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.NAME, expression.semanticId)
        val descriptor = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(resultTypeRef).descriptor
        emitValue(method, receiver)
        method.visitFieldInsn(Opcodes.GETFIELD, owner, name, descriptor)
        storeExpressionResult(method, expression.semanticId, context.typeMapper.mapValueType(resultTypeRef))
    }

    private fun emitJvmPutField(method: MethodVisitor, expression: ChirOtherExpression) {
        val operands = expression.requireOperandCount("jvm.putField", 2)
        val receiver = operands[0]
        val value = operands[1]
        expression.requireNoResultType("jvm.putField")
        val owner = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.OWNER)
            ?: objectInternalName(context.typeMapper.mapValueType(receiver.type), expression.semanticId, "field receiver")
        val name = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.NAME, expression.semanticId)
        val descriptor = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(value.type).descriptor
        emitValue(method, receiver)
        emitValue(method, value)
        method.visitFieldInsn(Opcodes.PUTFIELD, owner, name, descriptor)
    }

    private fun emitJvmGetStatic(method: MethodVisitor, expression: ChirOtherExpression) {
        expression.requireOperandCount("jvm.getStatic", 0)
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM getStatic expression requires result type", expression.semanticId)
        val owner = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.OWNER, expression.semanticId)
        val name = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.NAME, expression.semanticId)
        val descriptor = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(resultTypeRef).descriptor
        method.visitFieldInsn(Opcodes.GETSTATIC, owner, name, descriptor)
        storeExpressionResult(method, expression.semanticId, context.typeMapper.mapValueType(resultTypeRef))
    }

    private fun emitJvmPutStatic(method: MethodVisitor, expression: ChirOtherExpression) {
        val value = expression.requireOperandCount("jvm.putStatic", 1)[0]
        expression.requireNoResultType("jvm.putStatic")
        val owner = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.OWNER, expression.semanticId)
        val name = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.NAME, expression.semanticId)
        val descriptor = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(value.type).descriptor
        emitValue(method, value)
        method.visitFieldInsn(Opcodes.PUTSTATIC, owner, name, descriptor)
    }

    private fun emitJvmNewArray(method: MethodVisitor, expression: ChirOtherExpression) {
        val size = expression.requireOperandCount("jvm.newArray", 1)[0]
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM newArray expression requires result type", expression.semanticId)
        val arrayType = context.typeMapper.mapValueType(resultTypeRef)
        if (arrayType.sort != Type.ARRAY) {
            throw JvmCodegenException("JVM newArray result must be array type, actual ${arrayType.descriptor}", expression.semanticId)
        }
        val sizeType = context.typeMapper.mapValueType(size.type)
        if (sizeType !in intArraySizeTypes) {
            throw JvmCodegenException("JVM newArray size must be int-like, actual ${sizeType.descriptor}", expression.semanticId)
        }
        emitValue(method, size)
        val elementType = arrayElementType(arrayType, expression.semanticId)
        when (elementType.sort) {
            Type.BOOLEAN -> method.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN)
            Type.BYTE -> method.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE)
            Type.CHAR -> method.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR)
            Type.SHORT -> method.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT)
            Type.INT -> method.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT)
            Type.LONG -> method.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG)
            Type.FLOAT -> method.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT)
            Type.DOUBLE -> method.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE)
            Type.OBJECT -> method.visitTypeInsn(Opcodes.ANEWARRAY, elementType.internalName)
            Type.ARRAY -> method.visitTypeInsn(Opcodes.ANEWARRAY, elementType.descriptor)
            else -> throw JvmCodegenException("JVM newArray does not support element type ${elementType.descriptor}", expression.semanticId)
        }
        storeExpressionResult(method, expression.semanticId, arrayType)
    }

    private fun emitJvmArrayLoad(method: MethodVisitor, expression: ChirOtherExpression) {
        val operands = expression.requireOperandCount("jvm.arrayLoad", 2)
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM arrayLoad expression requires result type", expression.semanticId)
        val resultType = context.typeMapper.mapValueType(resultTypeRef)
        val arrayType = emitArrayAndIndex(method, operands[0], operands[1], expression.semanticId)
        val elementType = arrayElementType(arrayType, expression.semanticId)
        if (elementType != resultType) {
            throw JvmCodegenException(
                "JVM arrayLoad result type ${resultType.descriptor} does not match array element ${elementType.descriptor}",
                expression.semanticId,
            )
        }
        method.visitInsn(resultType.getOpcode(Opcodes.IALOAD))
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    private fun emitJvmArrayStore(method: MethodVisitor, expression: ChirOtherExpression) {
        val operands = expression.requireOperandCount("jvm.arrayStore", 3)
        expression.requireNoResultType("jvm.arrayStore")
        val valueType = context.typeMapper.mapValueType(operands[2].type)
        val arrayType = emitArrayAndIndex(method, operands[0], operands[1], expression.semanticId)
        val elementType = arrayElementType(arrayType, expression.semanticId)
        if (elementType != valueType) {
            throw JvmCodegenException(
                "JVM arrayStore value type ${valueType.descriptor} does not match array element ${elementType.descriptor}",
                expression.semanticId,
            )
        }
        emitValue(method, operands[2])
        method.visitInsn(valueType.getOpcode(Opcodes.IASTORE))
    }

    private fun emitJvmArrayLength(method: MethodVisitor, expression: ChirOtherExpression) {
        val array = expression.requireOperandCount("jvm.arrayLength", 1)[0]
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM arrayLength expression requires result type", expression.semanticId)
        val resultType = context.typeMapper.mapValueType(resultTypeRef)
        if (resultType != Type.INT_TYPE) {
            throw JvmCodegenException("JVM arrayLength result must be int, actual ${resultType.descriptor}", expression.semanticId)
        }
        val arrayType = context.typeMapper.mapValueType(array.type)
        if (arrayType.sort != Type.ARRAY) {
            throw JvmCodegenException("JVM arrayLength requires array value, actual ${arrayType.descriptor}", expression.semanticId)
        }
        emitValue(method, array)
        method.visitInsn(Opcodes.ARRAYLENGTH)
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    private fun emitJvmCheckCast(method: MethodVisitor, expression: ChirOtherExpression) {
        val source = expression.singleOperand("jvm.checkCast")
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM checkCast expression requires result type", expression.semanticId)
        val targetType = context.typeMapper.mapValueType(resultTypeRef)
        val targetInternalName = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.TYPE)
            ?: objectOrArrayInternalName(targetType, expression.semanticId, "checkCast target")
        emitValue(method, source)
        method.visitTypeInsn(Opcodes.CHECKCAST, targetInternalName)
        storeExpressionResult(method, expression.semanticId, targetType)
    }

    private fun emitJvmInstanceOf(method: MethodVisitor, expression: ChirOtherExpression) {
        val source = expression.singleOperand("jvm.instanceOf")
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM instanceOf expression requires result type", expression.semanticId)
        val resultType = context.typeMapper.mapValueType(resultTypeRef)
        if (resultType != Type.BOOLEAN_TYPE) {
            throw JvmCodegenException("JVM instanceOf result must be bool, actual ${resultType.descriptor}", expression.semanticId)
        }
        val targetInternalName = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.TYPE, expression.semanticId)
        emitValue(method, source)
        method.visitTypeInsn(Opcodes.INSTANCEOF, targetInternalName)
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    private fun emitTerminator(method: MethodVisitor, terminator: Any) {
        when (terminator) {
            is ChirReturnTerminator -> emitReturn(method, terminator)
            is ChirBranchTerminator -> method.visitJumpInsn(Opcodes.GOTO, labelFor(terminator.targetBlockId))
            is ChirConditionalBranchTerminator -> {
                emitValue(method, terminator.condition)
                method.visitJumpInsn(Opcodes.IFNE, labelFor(terminator.trueTargetBlockId))
                method.visitJumpInsn(Opcodes.GOTO, labelFor(terminator.falseTargetBlockId))
            }
            is ChirThrowTerminator -> emitThrow(method, terminator)
            is ChirUnwindTerminator -> throw JvmCodegenException(
                "JVM backend does not yet support unwind terminator",
                terminator.semanticId,
            )
            else -> throw JvmCodegenException(
                "unsupported JVM terminator ${terminator::class.simpleName}",
                function.semanticId,
            )
        }
    }

    private fun emitReturn(method: MethodVisitor, terminator: ChirReturnTerminator) {
        val returnValue = terminator.returnValue
        if (returnType == Type.VOID_TYPE) {
            if (returnValue != null) {
                throw JvmCodegenException("void JVM method cannot return a value", terminator.semanticId)
            }
            method.visitInsn(Opcodes.RETURN)
            return
        }
        if (returnValue == null) {
            throw JvmCodegenException("non-void JVM method must return a value", terminator.semanticId)
        }
        emitValue(method, returnValue)
        method.visitInsn(returnType.getOpcode(Opcodes.IRETURN))
    }

    private fun emitThrow(method: MethodVisitor, terminator: ChirThrowTerminator) {
        val exceptionType = context.typeMapper.mapValueType(terminator.exceptionValue.type)
        objectInternalName(exceptionType, terminator.semanticId, "throw exception")
        emitValue(method, terminator.exceptionValue)
        method.visitInsn(Opcodes.ATHROW)
    }

    private fun emitValue(method: MethodVisitor, value: ChirValue) {
        when (value) {
            is ChirConstantValue -> emitConstant(method, value)
            is ChirParameterValue -> loadSlot(method, value.semanticId, value.name, value.type)
            is ChirLocalValue -> loadSlot(method, value.semanticId, value.name, value.type)
            is ChirFunctionValue -> throw JvmCodegenException(
                "function value '${value.name}' cannot be materialized as a JVM stack value",
                value.semanticId,
            )
            is ChirGlobalValue -> throw JvmCodegenException("JVM backend does not yet support global value '${value.name}'", value.semanticId)
            is ChirImportedFunctionValue -> throw JvmCodegenException(
                "imported function value '${value.name}' cannot be materialized as a JVM stack value",
                value.semanticId,
            )
            is ChirImportedVariableValue -> emitImportedVariable(method, value)
            is ChirBlockValue, is ChirBlockGroupValue -> throw JvmCodegenException(
                "block value cannot be materialized as a JVM stack value",
                value.semanticId,
            )
        }
    }

    private fun emitImportedVariable(method: MethodVisitor, value: ChirImportedVariableValue) {
        val owner = JvmAbiAttributes.requireString(value.attributes, JvmAbiAttributes.OWNER, value.semanticId)
        val name = JvmAbiAttributes.requireString(value.attributes, JvmAbiAttributes.NAME, value.semanticId)
        method.visitFieldInsn(
            Opcodes.GETSTATIC,
            owner,
            name,
            context.typeMapper.mapValueType(value.type).descriptor,
        )
    }

    private fun emitConstant(method: MethodVisitor, value: ChirConstantValue) {
        when (val type = context.typeMapper.mapValueType(value.type)) {
            Type.BOOLEAN_TYPE -> method.visitInsn(if (value.literal.equals("true", ignoreCase = true) || value.literal == "1") Opcodes.ICONST_1 else Opcodes.ICONST_0)
            Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE -> method.visitLdcInsn(value.literal.toInt())
            Type.LONG_TYPE -> method.visitLdcInsn(value.literal.toLong())
            Type.FLOAT_TYPE -> method.visitLdcInsn(value.literal.toFloat())
            Type.DOUBLE_TYPE -> method.visitLdcInsn(value.literal.toDouble())
            Type.getObjectType("java/lang/String") -> method.visitLdcInsn(value.literal)
            else -> throw JvmCodegenException("unsupported JVM constant type ${type.descriptor}", value.semanticId)
        }
    }

    private fun storeExpressionResult(method: MethodVisitor, id: ChirSemanticId, type: Type) {
        val slot = localSlots.getOrPut(id) {
            LocalSlot(nextLocalSlot, type).also {
                nextLocalSlot += type.size
            }
        }
        localSlotsByName[resultLocalName(id)] = slot
        if (slot.type != type) {
            throw JvmCodegenException("JVM local slot type mismatch: expected ${slot.type}, actual $type", id)
        }
        method.visitVarInsn(type.getOpcode(Opcodes.ISTORE), slot.index)
    }

    private fun loadSlot(method: MethodVisitor, id: ChirSemanticId, name: String, expectedType: ChirTypeRef) {
        val slot = localSlots[id]
            ?: localSlotsByName[normalizeLocalName(name)]
            ?: localSlotsByName[resultLocalName(id)]
            ?: throw JvmCodegenException("unbound JVM local value '$name'", id)
        val type = context.typeMapper.mapValueType(expectedType)
        if (slot.type != type) {
            throw JvmCodegenException("JVM local load type mismatch: expected ${slot.type}, actual $type", id)
        }
        method.visitVarInsn(type.getOpcode(Opcodes.ILOAD), slot.index)
    }

    private fun ensureAddressSlot(address: ChirValue, nodeId: ChirSemanticId): LocalSlot {
        val targetType = refTargetType(address, nodeId)
        val type = context.typeMapper.mapValueType(targetType)
        val name = address.displayName ?: address.semanticId.value
        val existing = localSlots[address.semanticId]
            ?: localSlotsByName[normalizeLocalName(name)]
        if (existing != null) {
            if (existing.type != type) {
                throw JvmCodegenException("JVM address slot type mismatch: expected ${existing.type}, actual $type", nodeId)
            }
            return existing
        }
        return LocalSlot(nextLocalSlot, type).also { slot ->
            bindLocal(address.semanticId, name, slot)
            nextLocalSlot += type.size
        }
    }

    private fun refTargetType(address: ChirValue, nodeId: ChirSemanticId): ChirTypeRef {
        val referenceType = (address.type as? ChirResolvedTypeRef)?.type as? ChirRefType
            ?: throw JvmCodegenException("JVM memory address must have CHIR ref type, actual ${address.type.renderName}", nodeId)
        return referenceType.referencedType
    }

    private fun emitArithmetic(method: MethodVisitor, id: ChirSemanticId, type: Type, intOpcode: Int) {
        requireArithmeticType(type, id)
        method.visitInsn(type.getOpcode(intOpcode))
    }

    private fun emitUnsignedDivRem(method: MethodVisitor, id: ChirSemanticId, type: Type, methodName: String) {
        requireIntLikeType(type, id)
        when (type) {
            Type.LONG_TYPE -> method.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Long",
                methodName,
                "(JJ)J",
                false,
            )
            Type.BYTE_TYPE,
            Type.SHORT_TYPE,
            Type.INT_TYPE,
            -> {
                val right = storeTemp(method, type)
                val left = storeTemp(method, type)
                emitUnsignedIntLikeFromSlot(method, left, type)
                emitUnsignedIntLikeFromSlot(method, right, type)
                method.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Integer",
                    methodName,
                    "(II)I",
                    false,
                )
            }
            else -> throw JvmCodegenException("JVM unsigned operation does not support type ${type.descriptor}", id)
        }
    }

    private fun emitIntLike(method: MethodVisitor, id: ChirSemanticId, type: Type, intOpcode: Int) {
        requireIntLikeType(type, id)
        method.visitInsn(type.getOpcode(intOpcode))
    }

    private fun emitShift(method: MethodVisitor, id: ChirSemanticId, type: Type, intOpcode: Int) {
        requireIntLikeType(type, id)
        if (type == Type.LONG_TYPE) {
            method.visitInsn(Opcodes.L2I)
        }
        method.visitInsn(type.getOpcode(intOpcode))
    }

    private fun emitComparison(method: MethodVisitor, type: Type, intCompareOpcode: Int, zeroCompareOpcode: Int) {
        val trueLabel = Label()
        val endLabel = Label()
        when (type) {
            Type.INT_TYPE, Type.SHORT_TYPE, Type.BYTE_TYPE, Type.BOOLEAN_TYPE -> method.visitJumpInsn(intCompareOpcode, trueLabel)
            Type.LONG_TYPE -> {
                method.visitInsn(Opcodes.LCMP)
                method.visitJumpInsn(zeroCompareOpcode, trueLabel)
            }
            Type.FLOAT_TYPE -> {
                method.visitInsn(Opcodes.FCMPL)
                method.visitJumpInsn(zeroCompareOpcode, trueLabel)
            }
            Type.DOUBLE_TYPE -> {
                method.visitInsn(Opcodes.DCMPL)
                method.visitJumpInsn(zeroCompareOpcode, trueLabel)
            }
            else -> throw JvmCodegenException("unsupported JVM comparison type ${type.descriptor}", function.semanticId)
        }
        method.visitInsn(Opcodes.ICONST_0)
        method.visitJumpInsn(Opcodes.GOTO, endLabel)
        method.visitLabel(trueLabel)
        method.visitInsn(Opcodes.ICONST_1)
        method.visitLabel(endLabel)
    }

    private fun emitUnsignedComparison(method: MethodVisitor, id: ChirSemanticId, type: Type, zeroCompareOpcode: Int) {
        requireIntLikeType(type, id)
        when (type) {
            Type.LONG_TYPE -> method.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Long",
                "compareUnsigned",
                "(JJ)I",
                false,
            )
            Type.BYTE_TYPE,
            Type.SHORT_TYPE,
            Type.INT_TYPE,
            -> {
                val right = storeTemp(method, type)
                val left = storeTemp(method, type)
                emitUnsignedIntLikeFromSlot(method, left, type)
                emitUnsignedIntLikeFromSlot(method, right, type)
                method.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Integer",
                    "compareUnsigned",
                    "(II)I",
                    false,
                )
            }
            else -> throw JvmCodegenException("JVM unsigned comparison does not support type ${type.descriptor}", id)
        }
        emitIntZeroComparison(method, zeroCompareOpcode)
    }

    private fun emitIntZeroComparison(method: MethodVisitor, zeroCompareOpcode: Int) {
        val trueLabel = Label()
        val endLabel = Label()
        method.visitJumpInsn(zeroCompareOpcode, trueLabel)
        method.visitInsn(Opcodes.ICONST_0)
        method.visitJumpInsn(Opcodes.GOTO, endLabel)
        method.visitLabel(trueLabel)
        method.visitInsn(Opcodes.ICONST_1)
        method.visitLabel(endLabel)
    }

    private fun emitPrimitiveCast(method: MethodVisitor, id: ChirSemanticId, source: Type, target: Type) {
        if (source == target) return
        when (source) {
            Type.BYTE_TYPE,
            Type.SHORT_TYPE,
            Type.INT_TYPE,
            Type.BOOLEAN_TYPE,
            -> emitIntToPrimitiveCast(method, id, target)
            Type.LONG_TYPE -> emitLongToPrimitiveCast(method, id, target)
            Type.FLOAT_TYPE -> emitFloatToPrimitiveCast(method, id, target)
            Type.DOUBLE_TYPE -> emitDoubleToPrimitiveCast(method, id, target)
            else -> throw JvmCodegenException("JVM cast source type ${source.descriptor} is not primitive", id)
        }
    }

    private fun emitIntToPrimitiveCast(method: MethodVisitor, id: ChirSemanticId, target: Type) {
        when (target) {
            Type.BYTE_TYPE -> method.visitInsn(Opcodes.I2B)
            Type.SHORT_TYPE -> method.visitInsn(Opcodes.I2S)
            Type.INT_TYPE, Type.BOOLEAN_TYPE -> Unit
            Type.LONG_TYPE -> method.visitInsn(Opcodes.I2L)
            Type.FLOAT_TYPE -> method.visitInsn(Opcodes.I2F)
            Type.DOUBLE_TYPE -> method.visitInsn(Opcodes.I2D)
            else -> throw JvmCodegenException("JVM cast target type ${target.descriptor} is not primitive", id)
        }
    }

    private fun emitLongToPrimitiveCast(method: MethodVisitor, id: ChirSemanticId, target: Type) {
        when (target) {
            Type.BYTE_TYPE -> {
                method.visitInsn(Opcodes.L2I)
                method.visitInsn(Opcodes.I2B)
            }
            Type.SHORT_TYPE -> {
                method.visitInsn(Opcodes.L2I)
                method.visitInsn(Opcodes.I2S)
            }
            Type.INT_TYPE, Type.BOOLEAN_TYPE -> method.visitInsn(Opcodes.L2I)
            Type.FLOAT_TYPE -> method.visitInsn(Opcodes.L2F)
            Type.DOUBLE_TYPE -> method.visitInsn(Opcodes.L2D)
            else -> throw JvmCodegenException("JVM cast target type ${target.descriptor} is not primitive", id)
        }
    }

    private fun emitFloatToPrimitiveCast(method: MethodVisitor, id: ChirSemanticId, target: Type) {
        when (target) {
            Type.BYTE_TYPE -> {
                method.visitInsn(Opcodes.F2I)
                method.visitInsn(Opcodes.I2B)
            }
            Type.SHORT_TYPE -> {
                method.visitInsn(Opcodes.F2I)
                method.visitInsn(Opcodes.I2S)
            }
            Type.INT_TYPE, Type.BOOLEAN_TYPE -> method.visitInsn(Opcodes.F2I)
            Type.LONG_TYPE -> method.visitInsn(Opcodes.F2L)
            Type.DOUBLE_TYPE -> method.visitInsn(Opcodes.F2D)
            else -> throw JvmCodegenException("JVM cast target type ${target.descriptor} is not primitive", id)
        }
    }

    private fun emitDoubleToPrimitiveCast(method: MethodVisitor, id: ChirSemanticId, target: Type) {
        when (target) {
            Type.BYTE_TYPE -> {
                method.visitInsn(Opcodes.D2I)
                method.visitInsn(Opcodes.I2B)
            }
            Type.SHORT_TYPE -> {
                method.visitInsn(Opcodes.D2I)
                method.visitInsn(Opcodes.I2S)
            }
            Type.INT_TYPE, Type.BOOLEAN_TYPE -> method.visitInsn(Opcodes.D2I)
            Type.LONG_TYPE -> method.visitInsn(Opcodes.D2L)
            Type.FLOAT_TYPE -> method.visitInsn(Opcodes.D2F)
            else -> throw JvmCodegenException("JVM cast target type ${target.descriptor} is not primitive", id)
        }
    }

    private fun emitMinusOne(method: MethodVisitor, type: Type) {
        when (type) {
            Type.LONG_TYPE -> method.visitLdcInsn(-1L)
            Type.BYTE_TYPE,
            Type.SHORT_TYPE,
            Type.INT_TYPE,
            -> method.visitInsn(Opcodes.ICONST_M1)
            else -> throw JvmCodegenException("JVM bitnot does not support type ${type.descriptor}", function.semanticId)
        }
    }

    private fun storeTemp(method: MethodVisitor, type: Type): LocalSlot {
        val slot = LocalSlot(nextLocalSlot, type)
        nextLocalSlot += type.size
        method.visitVarInsn(type.getOpcode(Opcodes.ISTORE), slot.index)
        return slot
    }

    private fun emitUnsignedIntLikeFromSlot(method: MethodVisitor, slot: LocalSlot, type: Type) {
        method.visitVarInsn(type.getOpcode(Opcodes.ILOAD), slot.index)
        when (type) {
            Type.BYTE_TYPE -> {
                method.visitLdcInsn(0xFF)
                method.visitInsn(Opcodes.IAND)
            }
            Type.SHORT_TYPE -> {
                method.visitLdcInsn(0xFFFF)
                method.visitInsn(Opcodes.IAND)
            }
            Type.INT_TYPE -> Unit
            else -> throw JvmCodegenException("JVM unsigned int carrier does not support type ${type.descriptor}", function.semanticId)
        }
    }

    private fun ChirOtherExpression.singleOperand(operation: String): ChirValue {
        if (operands.size != 1) {
            throw JvmCodegenException("JVM $operation expression requires exactly one operand", semanticId)
        }
        return operands.single()
    }

    private fun ChirOtherExpression.requireOperandCount(operation: String, expected: Int): List<ChirValue> {
        if (operands.size != expected) {
            throw JvmCodegenException(
                "JVM $operation expression requires $expected operand(s), actual ${operands.size}",
                semanticId,
            )
        }
        return operands
    }

    private fun ChirOtherExpression.requireNoResultType(operation: String) {
        if (resultType != null) {
            throw JvmCodegenException("JVM $operation expression must not declare result type", semanticId)
        }
    }

    private fun emitArrayAndIndex(
        method: MethodVisitor,
        array: ChirValue,
        index: ChirValue,
        nodeId: ChirSemanticId,
    ): Type {
        val arrayType = context.typeMapper.mapValueType(array.type)
        if (arrayType.sort != Type.ARRAY) {
            throw JvmCodegenException("JVM array operation requires array value, actual ${arrayType.descriptor}", nodeId)
        }
        val indexType = context.typeMapper.mapValueType(index.type)
        if (indexType !in intArraySizeTypes) {
            throw JvmCodegenException("JVM array index must be int-like, actual ${indexType.descriptor}", nodeId)
        }
        emitValue(method, array)
        emitValue(method, index)
        return arrayType
    }

    private fun arrayElementType(arrayType: Type, nodeId: ChirSemanticId): Type {
        if (arrayType.sort != Type.ARRAY) {
            throw JvmCodegenException("JVM value is not an array type: ${arrayType.descriptor}", nodeId)
        }
        return Type.getType(arrayType.descriptor.substring(1))
    }

    private fun objectInternalName(type: Type, nodeId: ChirSemanticId, location: String): String {
        if (type.sort != Type.OBJECT) {
            throw JvmCodegenException("JVM $location must be object type, actual ${type.descriptor}", nodeId)
        }
        return type.internalName
    }

    private fun objectOrArrayInternalName(type: Type, nodeId: ChirSemanticId, location: String): String {
        return when (type.sort) {
            Type.OBJECT -> type.internalName
            Type.ARRAY -> type.descriptor
            else -> throw JvmCodegenException("JVM $location must be object or array type, actual ${type.descriptor}", nodeId)
        }
    }

    private fun orderedBlocks(): List<ChirBlock> = buildList {
        val entry = function.blocks.firstOrNull { it.semanticId == function.entryBlockId }
        if (entry != null) add(entry)
        function.blocks.forEach { block ->
            if (entry == null || block.semanticId != entry.semanticId) add(block)
        }
    }

    private fun labelFor(id: ChirSemanticId): Label {
        return blockLabels[id] ?: throw JvmCodegenException("missing JVM block label", id)
    }

    private fun resolveLocalFunction(name: String, descriptor: String, nodeId: ChirSemanticId): ChirFunctionDeclaration {
        return module.declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .firstOrNull { candidate ->
                candidate.name == name &&
                    context.typeMapper.methodDescriptor(
                        candidate.returnType,
                        candidate.parameters.map(ChirVariableDeclaration::type),
                    ) == descriptor
            }
            ?: throw JvmCodegenException("unresolved local JVM function '$name$descriptor'", nodeId)
    }

    private fun verifyFunctionShape() {
        if (function.blocks.isEmpty()) {
            throw JvmCodegenException("JVM function '${function.name}' must contain at least one block", function.semanticId)
        }
        if (function.blocks.none { it.semanticId == function.entryBlockId }) {
            throw JvmCodegenException("JVM function '${function.name}' entry block is missing", function.entryBlockId)
        }
    }

    private fun requireArithmeticType(type: Type, id: ChirSemanticId) {
        if (type !in arithmeticTypes) {
            throw JvmCodegenException("JVM arithmetic does not support type ${type.descriptor}", id)
        }
    }

    private fun requireIntLikeType(type: Type, id: ChirSemanticId) {
        if (type !in intLikeTypes) {
            throw JvmCodegenException("JVM integer operation does not support type ${type.descriptor}", id)
        }
    }

    private fun requireBoolean(type: Type, id: ChirSemanticId) {
        if (type != Type.BOOLEAN_TYPE) {
            throw JvmCodegenException("JVM boolean operation requires boolean type, actual ${type.descriptor}", id)
        }
    }

    private fun bindLocal(id: ChirSemanticId, name: String, slot: LocalSlot) {
        localSlots[id] = slot
        localSlotsByName[normalizeLocalName(name)] = slot
    }

    private fun resultLocalName(id: ChirSemanticId): String = normalizeLocalName(id.value)

    private fun normalizeLocalName(raw: String): String {
        val sanitized = raw.replace(Regex("[^A-Za-z0-9_.$]"), "_").trim('_')
        if (sanitized.isBlank()) return "local"
        return if (sanitized.first().isDigit()) "local_$sanitized" else sanitized
    }

    private data class LocalSlot(val index: Int, val type: Type)

    private companion object {
        val arithmeticTypes = setOf(Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE, Type.DOUBLE_TYPE)
        val intLikeTypes = setOf(Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE)
        val intArraySizeTypes = setOf(Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE)
    }
}
