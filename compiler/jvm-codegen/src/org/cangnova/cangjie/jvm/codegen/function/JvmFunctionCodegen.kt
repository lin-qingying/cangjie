package org.cangnova.cangjie.jvm.codegen.function

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
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
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
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
import org.cangnova.cangjie.jvm.codegen.runtime.JvmRuntimeArtifacts
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * JVM method 生成器。函数参数与表达式结果通过 CHIR semanticId 绑定到 JVM local slot。
 */
class JvmFunctionCodegen(
    /**
     * JVM 后端共享上下文，提供类型映射、命名策略、ABI 属性读取与生成选项。
     */
    private val context: JvmBackendContext,
    /**
     * 当前函数所在的 CHIR 模块，用于解析同模块函数调用与模块 facade 成员访问。
     */
    private val module: ChirModule,
    /**
     * 当前方法所属 JVM class 的 internal name。
     */
    private val ownerInternalName: String,
    /**
     * 接收当前方法字节码的 ASM class writer。
     */
    private val classWriter: ClassWriter,
    /**
     * 正在生成为 JVM method 的 CHIR 函数声明。
     */
    private val function: ChirFunctionDeclaration,
    /**
     * 写入 JVM method 时使用的访问标记。
     */
    private val methodAccess: Int = Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
    /**
     * 指示当前方法是否按静态方法布局 local slot。
     */
    private val isStaticMethod: Boolean = true,
    /**
     * 指示当前 CHIR 函数是否按 JVM 构造器 `<init>` 生成。
     */
    private val isConstructor: Boolean = false,
    /**
     * 构造器生成时需要调用的父类 internal name；非构造器路径为空。
     */
    private val superInternalName: String? = null,
    /**
     * 当前模块 facade 的 internal name，函数体访问包级函数和全局变量时使用。
     */
    private val moduleFacadeInternalName: String = ownerInternalName,
) {
    /**
     * 当前方法在 JVM classfile 中的最终名称。
     */
    private val methodName: String = jvmMethodName(function)
    /**
     * CHIR 声明返回类型映射到 JVM 后的类型，构造器合法性校验会使用它。
     */
    private val declaredReturnType: Type = context.typeMapper.mapReturnType(function.returnType)
    /**
     * 当前方法的 JVM descriptor，优先来自显式 ABI 属性。
     */
    private val methodDescriptor: String = jvmMethodDescriptor(function)
    /**
     * 从 method descriptor 解析出的 JVM 参数类型列表。
     */
    private val parameterTypes: List<Type> = Type.getArgumentTypes(methodDescriptor).toList()
    /**
     * 从 method descriptor 解析出的 JVM 返回类型。
     */
    private val returnType: Type = Type.getReturnType(methodDescriptor)
    /**
     * CHIR 基本块 semanticId 到 ASM label 的稳定映射。
     */
    private val blockLabels: Map<ChirSemanticId, Label> = function.blocks.associate { it.semanticId to Label() }
    /**
     * 以 CHIR semanticId 索引的 JVM local slot 绑定表。
     */
    private val localSlots = linkedMapOf<ChirSemanticId, LocalSlot>()
    /**
     * 以规范化 CHIR 名称索引的 JVM local slot 绑定表，用于恢复按名称引用的局部值。
     */
    private val localSlotsByName = linkedMapOf<String, LocalSlot>()
    /**
     * 下一个可分配 JVM local slot 下标，按 JVM 类型 size 递增。
     */
    private var nextLocalSlot: Int = 0

    /**
     * 生成当前 CHIR 函数对应的 JVM method。
     *
     * 生成顺序为函数形状校验、方法头写入、参数 slot 初始化、可选父构造器调用、
     * 基本块按入口优先顺序发射，以及终结符控制流发射。
     */
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
        if (isConstructor) {
            emitSuperConstructorCall(method)
        }
        orderedBlocks().forEach { block ->
            method.visitLabel(labelFor(block.semanticId))
            block.expressions
                .filterNot(::isPhiExpression)
                .forEach { expression -> emitExpression(method, expression) }
            emitTerminator(method, block, block.terminator)
        }
        method.visitMaxs(0, 0)
        method.visitEnd()
    }

    /**
     * 为无参 Unit `main` 生成 Java 入口桥接方法。
     *
     * 桥接方法固定签名为 `main(String[]): void`，直接调用当前无参 JVM 方法并返回。
     */
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

    /**
     * 根据当前方法布局初始化参数 local slot。
     *
     * 实例方法先绑定 `this`/`self` 到 slot 0；随后按 method descriptor 中的 JVM 参数类型绑定 CHIR 参数。
     */
    private fun initializeParameterSlots() {
        if (!isStaticMethod) {
            val thisSlot = LocalSlot(0, Type.getObjectType(ownerInternalName))
            localSlotsByName["this"] = thisSlot
            localSlotsByName["self"] = thisSlot
            nextLocalSlot = 1
        }
        if (parameterTypes.size != function.parameters.size) {
            throw JvmCodegenException(
                "JVM function '${function.name}' descriptor parameter count ${parameterTypes.size} does not match CHIR parameters ${function.parameters.size}",
                function.semanticId,
            )
        }
        function.parameters.forEachIndexed { index, parameter ->
            val type = parameterTypes[index]
            bindLocal(parameter.semanticId, parameter.name, LocalSlot(nextLocalSlot, type))
            nextLocalSlot += type.size
        }
    }

    /**
     * 分派发射 CHIR 表达式到对应 JVM 字节码生成路径。
     */
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

    /**
     * 发射 CHIR 一元表达式。
     *
     * 支持数值取负、位取反、逻辑非和无操作复制，并将结果写回表达式 semanticId 对应的 local slot。
     */
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

    /**
     * 发射 CHIR 二元表达式。
     *
     * 根据操作符选择 JVM 算术、位运算、移位、有符号比较、无符号比较或相等性比较指令。
     */
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
            "eq", "==" -> emitEqualityComparison(method, operandType, Opcodes.IF_ICMPEQ, Opcodes.IF_ACMPEQ, Opcodes.IFEQ)
            "ne", "!=" -> emitEqualityComparison(method, operandType, Opcodes.IF_ICMPNE, Opcodes.IF_ACMPNE, Opcodes.IFNE)
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

    /**
     * 发射 CHIR 调用表达式。
     *
     * 本地函数、带 receiver 的本地函数、导入 JVM 函数和动态 MethodHandle 调用分别走不同路径；
     * 调用结果会按 CHIR 目标类型进行栈值适配后写入表达式结果 slot。
     */
    private fun emitCallExpression(method: MethodVisitor, expression: ChirCallExpression) {
        val callee = expression.callee
        val functionType = (callee.type as? ChirResolvedTypeRef)?.type as? ChirFunctionType
            ?: throw JvmCodegenException("JVM call callee must carry ChirFunctionType", expression.semanticId)
        val chirDescriptor = context.typeMapper.methodDescriptor(functionType.returnType, functionType.parameterTypes)
        val resultType = context.typeMapper.mapReturnType(expression.resultType)
        val actualStackType = when (callee) {
            is ChirFunctionValue -> {
                val descriptor = JvmAbiAttributes.optionalString(callee.attributes, JvmAbiAttributes.DESCRIPTOR) ?: chirDescriptor
                if (functionType.receiverType != null) {
                    emitLocalReceiverFunctionCall(method, expression, callee, functionType, descriptor)
                } else {
                    val owner = JvmAbiAttributes.optionalString(callee.attributes, JvmAbiAttributes.OWNER)
                    val explicitName = JvmAbiAttributes.optionalString(callee.attributes, JvmAbiAttributes.NAME)
                    val targetName = explicitName
                        ?: if (owner == null) {
                            jvmMethodName(resolveLocalFunction(callee.name, descriptor, expression.semanticId))
                        } else {
                            context.namePolicy.functionJvmName(callee.name)
                        }
                    emitArgumentsForDescriptor(method, expression.arguments, descriptor, expression.semanticId)
                    method.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        owner ?: moduleFacadeInternalName,
                        targetName,
                        descriptor,
                        false,
                    )
                    Type.getReturnType(descriptor)
                }
            }
            is ChirImportedFunctionValue -> emitImportedFunctionCall(method, expression, callee, functionType, chirDescriptor)
            else -> emitDynamicFunctionCall(method, expression, functionType)
        }
        if (resultType == Type.VOID_TYPE) {
            popStackValue(method, actualStackType)
            return
        }
        adaptStackValue(method, expression.semanticId, actualStackType, resultType)
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    /**
     * 发射动态函数值调用。
     *
     * 动态调用统一将实参装箱为 `Object[]`，通过 `MethodHandle.invokeWithArguments` 调用，
     * 再把 `Object` 结果还原为 CHIR 期望的 JVM carrier。
     */
    private fun emitDynamicFunctionCall(
        method: MethodVisitor,
        expression: ChirCallExpression,
        functionType: ChirFunctionType,
    ): Type {
        val resultType = context.typeMapper.mapReturnType(expression.resultType)
        val expectedArgumentTypes = buildList {
            functionType.receiverType?.let(::add)
            addAll(functionType.parameterTypes)
        }
        if (expectedArgumentTypes.size != expression.arguments.size) {
            throw JvmCodegenException(
                "dynamic JVM function call argument count mismatch: expected ${expectedArgumentTypes.size}, actual ${expression.arguments.size}",
                expression.semanticId,
            )
        }
        emitValue(method, expression.callee)
        emitObjectArray(method, expression.arguments)
        method.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/invoke/MethodHandle",
            "invokeWithArguments",
            "([Ljava/lang/Object;)Ljava/lang/Object;",
            false,
        )
        if (resultType == Type.VOID_TYPE) {
            method.visitInsn(Opcodes.POP)
            return Type.VOID_TYPE
        } else {
            emitCoerceObjectResult(method, expression.semanticId, resultType)
            return resultType
        }
    }

    /**
     * 发射带接收者的本地函数调用。
     *
     * receiverType 决定实例调用的 owner，第一实参作为 receiver，其余实参按 JVM descriptor 发射。
     */
    private fun emitLocalReceiverFunctionCall(
        method: MethodVisitor,
        expression: ChirCallExpression,
        callee: ChirFunctionValue,
        functionType: ChirFunctionType,
        descriptor: String,
    ): Type {
        val receiverType = functionType.receiverType
            ?: throw JvmCodegenException("local receiver call '${callee.name}' requires receiver type", expression.semanticId)
        if (expression.arguments.isEmpty()) {
            throw JvmCodegenException("local receiver call '${callee.name}' requires receiver argument", expression.semanticId)
        }
        val owner = JvmAbiAttributes.optionalString(callee.attributes, JvmAbiAttributes.OWNER)
            ?: objectInternalName(context.typeMapper.mapValueType(receiverType), expression.semanticId, "receiver call owner")
        val name = JvmAbiAttributes.optionalString(callee.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.functionJvmName(callee.name)
        val invokeKind = JvmAbiAttributes.optionalString(callee.attributes, JvmAbiAttributes.INVOKE_KIND) ?: "virtual"
        val opcode = when (invokeKind) {
            "virtual" -> Opcodes.INVOKEVIRTUAL
            "interface" -> Opcodes.INVOKEINTERFACE
            "special" -> Opcodes.INVOKESPECIAL
            else -> throw JvmCodegenException("unsupported JVM receiver invoke kind '$invokeKind'", expression.semanticId)
        }
        emitReceiverAndArgumentsForDescriptor(
            method = method,
            receiver = expression.arguments.first(),
            arguments = expression.arguments.drop(1),
            receiverType = Type.getObjectType(owner),
            descriptor = descriptor,
            nodeId = expression.semanticId,
        )
        method.visitMethodInsn(opcode, owner, name, descriptor, invokeKind == "interface")
        return Type.getReturnType(descriptor)
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
    ): Type {
        val owner = JvmAbiAttributes.requireString(callee.attributes, JvmAbiAttributes.OWNER, callee.semanticId)
        val name = JvmAbiAttributes.requireString(callee.attributes, JvmAbiAttributes.NAME, callee.semanticId)
        val invokeKind = JvmAbiAttributes.requireString(callee.attributes, JvmAbiAttributes.INVOKE_KIND, callee.semanticId)
        val jvmDescriptor = JvmAbiAttributes.optionalString(callee.attributes, JvmAbiAttributes.DESCRIPTOR) ?: descriptor
        val receiverType = functionType.receiverType
        return when (normalizeInvokeKind(invokeKind)) {
            "static" -> {
                if (receiverType != null) {
                    throw JvmCodegenException("static imported JVM call '$name' must not declare receiverType", expression.semanticId)
                }
                emitArgumentsForDescriptor(method, expression.arguments, jvmDescriptor, expression.semanticId)
                method.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, jvmDescriptor, false)
                Type.getReturnType(jvmDescriptor)
            }
            "virtual" -> {
                requireImportedReceiver(receiverType, expression, name)
                emitReceiverAndArgumentsForDescriptor(
                    method = method,
                    receiver = expression.arguments.first(),
                    arguments = expression.arguments.drop(1),
                    receiverType = Type.getObjectType(owner),
                    descriptor = jvmDescriptor,
                    nodeId = expression.semanticId,
                )
                method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, jvmDescriptor, false)
                Type.getReturnType(jvmDescriptor)
            }
            "interface" -> {
                requireImportedReceiver(receiverType, expression, name)
                emitReceiverAndArgumentsForDescriptor(
                    method = method,
                    receiver = expression.arguments.first(),
                    arguments = expression.arguments.drop(1),
                    receiverType = Type.getObjectType(owner),
                    descriptor = jvmDescriptor,
                    nodeId = expression.semanticId,
                )
                method.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, name, jvmDescriptor, true)
                Type.getReturnType(jvmDescriptor)
            }
            "special" -> {
                requireImportedReceiver(receiverType, expression, name)
                emitReceiverAndArgumentsForDescriptor(
                    method = method,
                    receiver = expression.arguments.first(),
                    arguments = expression.arguments.drop(1),
                    receiverType = Type.getObjectType(owner),
                    descriptor = jvmDescriptor,
                    nodeId = expression.semanticId,
                )
                method.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, jvmDescriptor, false)
                Type.getReturnType(jvmDescriptor)
            }
            "constructor" -> emitImportedConstructorCall(method, expression, owner, name, receiverType, functionType, callee)
            else -> throw JvmCodegenException("unsupported JVM invoke kind '$invokeKind'", expression.semanticId)
        }
    }

    /**
     * 发射导入 JVM 构造器调用。
     *
     * 该路径负责 `new`、`dup`、构造器实参发射和 `invokespecial <init>`，并返回新对象的 JVM 类型。
     */
    private fun emitImportedConstructorCall(
        method: MethodVisitor,
        expression: ChirCallExpression,
        owner: String,
        name: String,
        receiverType: ChirTypeRef?,
        functionType: ChirFunctionType,
        callee: ChirImportedFunctionValue,
    ): Type {
        if (receiverType != null) {
            throw JvmCodegenException("constructor imported JVM call '$name' must not declare receiverType", expression.semanticId)
        }
        if (name != "<init>") {
            throw JvmCodegenException("constructor imported JVM call must use '<init>' method name", expression.semanticId)
        }
        method.visitTypeInsn(Opcodes.NEW, owner)
        method.visitInsn(Opcodes.DUP)
        val descriptor = JvmAbiAttributes.optionalString(callee.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: constructorDescriptor(functionType)
        emitArgumentsForDescriptor(method, expression.arguments, descriptor, expression.semanticId)
        method.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            owner,
            "<init>",
            descriptor,
            false,
        )
        return Type.getObjectType(owner)
    }

    /**
     * 校验导入实例方法调用携带 receiverType 且实参列表包含 receiver。
     */
    private fun requireImportedReceiver(
        receiverType: ChirTypeRef?,
        expression: ChirCallExpression,
        name: String,
    ) {
        if (receiverType == null) {
            throw JvmCodegenException("imported JVM instance call '$name' requires function receiverType", expression.semanticId)
        }
        if (expression.arguments.isEmpty()) {
            throw JvmCodegenException("imported JVM instance call '$name' requires receiver argument", expression.semanticId)
        }
    }

    /**
     * 校验导入实例方法句柄携带 receiverType。
     */
    private fun requireImportedReceiver(
        receiverType: ChirTypeRef?,
        nodeId: ChirSemanticId,
        name: String,
    ) {
        if (receiverType == null) {
            throw JvmCodegenException("JVM instance function handle '$name' requires function receiverType", nodeId)
        }
    }

    /**
     * 发射 CHIR memory 表达式。
     *
     * CHIR ref 类型映射到 JVM local slot；CHIR cpointer 类型映射到 ByteBuffer 运行时访问。
     */
    private fun emitMemoryExpression(method: MethodVisitor, expression: ChirMemoryExpression) {
        when (val operation = expression.operation.lowercase().trim()) {
            "alloca" -> {
                if (expression.value != null) {
                    throw JvmCodegenException("JVM alloca must not carry a value operand", expression.semanticId)
                }
                if (expression.resultType == null) {
                    ensureAddressSlot(expression.address, expression.semanticId)
                } else {
                    emitPointerAlloca(method, expression)
                }
            }
            "store" -> {
                val value = expression.value
                    ?: throw JvmCodegenException("JVM store requires value operand", expression.semanticId)
                if (expression.resultType != null) {
                    throw JvmCodegenException("JVM store must not carry result type", expression.semanticId)
                }
                val targetType = memoryTargetType(expression.address, expression.semanticId)
                if (value.type != targetType) {
                    throw JvmCodegenException(
                        "JVM store value type ${value.type.renderName} does not match target ${targetType.renderName}",
                        expression.semanticId,
                    )
                }
                if (isPointerAddress(expression.address)) {
                    emitPointerStore(method, expression.address, value, expression.semanticId)
                } else {
                    val slot = ensureAddressSlot(expression.address, expression.semanticId)
                    emitValue(method, value)
                    method.visitVarInsn(slot.type.getOpcode(Opcodes.ISTORE), slot.index)
                }
            }
            "load" -> {
                val targetType = memoryTargetType(expression.address, expression.semanticId)
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
                if (isPointerAddress(expression.address)) {
                    emitPointerLoad(method, expression.address, resultType, expression.semanticId)
                } else {
                    val slot = ensureAddressSlot(expression.address, expression.semanticId)
                    method.visitVarInsn(slot.type.getOpcode(Opcodes.ILOAD), slot.index)
                    storeExpressionResult(method, expression.semanticId, slot.type)
                }
            }
            "gep", "getelementptr", "getelementptr.inbounds", "getelementptr inbounds" -> emitPointerGep(method, expression)
            else -> throw JvmCodegenException("unsupported JVM memory operation '$operation'", expression.semanticId)
        }
    }

    /**
     * 发射指针 alloca。
     *
     * 该实现以 `ByteBuffer.allocate(size)` 作为 JVM 指针 carrier，并把结果绑定到表达式 semanticId。
     */
    private fun emitPointerAlloca(method: MethodVisitor, expression: ChirMemoryExpression) {
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM pointer alloca requires result type", expression.semanticId)
        requirePointerType(resultTypeRef, expression.semanticId, "alloca result")
        val resultType = context.typeMapper.mapValueType(resultTypeRef)
        if (resultType != byteBufferType) {
            throw JvmCodegenException("JVM pointer alloca carrier must be ByteBuffer, actual ${resultType.descriptor}", expression.semanticId)
        }
        val sizeType = context.typeMapper.mapValueType(expression.address.type)
        if (sizeType !in intLikeTypes) {
            throw JvmCodegenException("JVM pointer alloca size must be integer-like, actual ${sizeType.descriptor}", expression.semanticId)
        }
        emitValue(method, expression.address)
        emitPrimitiveCast(method, expression.semanticId, sizeType, Type.INT_TYPE)
        method.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/nio/ByteBuffer",
            "allocate",
            "(I)Ljava/nio/ByteBuffer;",
            false,
        )
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    /**
     * 发射指针 GEP。
     *
     * 通过 duplicate、按元素字节宽度调整 position、再 slice 形成新的 ByteBuffer 视图。
     */
    private fun emitPointerGep(method: MethodVisitor, expression: ChirMemoryExpression) {
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM pointer gep requires result type", expression.semanticId)
        val addressPointerType = requirePointerType(expression.address.type, expression.semanticId, "gep address")
        requirePointerType(resultTypeRef, expression.semanticId, "gep result")
        emitValue(method, expression.address)
        method.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/nio/ByteBuffer",
            "duplicate",
            "()Ljava/nio/ByteBuffer;",
            false,
        )
        val index = expression.value
        if (index == null) {
            method.visitInsn(Opcodes.ICONST_0)
        } else {
            val indexType = context.typeMapper.mapValueType(index.type)
            if (indexType !in intLikeTypes) {
                throw JvmCodegenException("JVM pointer gep index must be integer-like, actual ${indexType.descriptor}", expression.semanticId)
            }
            emitValue(method, index)
            emitPrimitiveCast(method, expression.semanticId, indexType, Type.INT_TYPE)
        }
        val elementSize = pointerElementByteSize(addressPointerType.pointeeType, expression.semanticId)
        if (elementSize != 1) {
            method.visitLdcInsn(elementSize)
            method.visitInsn(Opcodes.IMUL)
        }
        method.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/nio/ByteBuffer",
            "position",
            "(I)Ljava/nio/ByteBuffer;",
            false,
        )
        method.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/nio/ByteBuffer",
            "slice",
            "()Ljava/nio/ByteBuffer;",
            false,
        )
        storeExpressionResult(method, expression.semanticId, byteBufferType)
    }

    /**
     * 从 ByteBuffer 指针 carrier 的当前位置读取目标类型值。
     */
    private fun emitPointerLoad(
        method: MethodVisitor,
        address: ChirValue,
        targetTypeRef: ChirTypeRef,
        nodeId: ChirSemanticId,
    ) {
        val targetType = context.typeMapper.mapValueType(targetTypeRef)
        val accessor = pointerAccessor(targetType, nodeId)
        emitValue(method, address)
        method.visitInsn(Opcodes.ICONST_0)
        method.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/nio/ByteBuffer",
            accessor.getMethodName,
            accessor.getDescriptor,
            false,
        )
        if (targetType == Type.BOOLEAN_TYPE) {
            emitIntZeroComparison(method, Opcodes.IFNE)
        }
        storeExpressionResult(method, nodeId, targetType)
    }

    /**
     * 将值写入 ByteBuffer 指针 carrier 的当前位置。
     */
    private fun emitPointerStore(
        method: MethodVisitor,
        address: ChirValue,
        value: ChirValue,
        nodeId: ChirSemanticId,
    ) {
        val valueType = context.typeMapper.mapValueType(value.type)
        val accessor = pointerAccessor(valueType, nodeId)
        emitValue(method, address)
        method.visitInsn(Opcodes.ICONST_0)
        emitValue(method, value)
        method.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/nio/ByteBuffer",
            accessor.putMethodName,
            accessor.putDescriptor,
            false,
        )
        method.visitInsn(Opcodes.POP)
    }

    /**
     * 发射 CHIR other 表达式中 JVM 后端支持的扩展操作。
     *
     * 这里覆盖 select、JVM field/array/type intrinsic、数值转换、指针整数互转等非基础表达式。
     */
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
            "ptrtoint" -> emitPointerToInt(method, expression)
            "inttoptr" -> emitIntToPointer(method, expression)
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

    /**
     * 发射三元 select 表达式。
     *
     * 条件必须是 bool，两个分支值必须与 resultType 完全一致；结果通过条件跳转选择后落到结果 slot。
     */
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

    /**
     * 发射 JVM bitcast。
     *
     * 当前 JVM 后端只允许 carrier 类型完全一致的 bitcast，避免在字节码层伪造不可验证转换。
     */
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

    /**
     * 发射 CHIR 数值转换表达式。
     *
     * 该方法统一处理符号/零扩展、浮点扩展/截断、整数浮点互转以及无符号辅助转换。
     */
    private fun emitNumericCast(method: MethodVisitor, expression: ChirOtherExpression) {
        val source = expression.singleOperand(expression.operation)
        val targetTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM cast '${expression.operation}' requires result type", expression.semanticId)
        val sourceType = context.typeMapper.mapValueType(source.type)
        val targetType = context.typeMapper.mapValueType(targetTypeRef)
        emitValue(method, source)
        when (expression.operation.lowercase().trim()) {
            "zext" -> emitZeroExtension(method, expression.semanticId, source.type, sourceType, targetType)
            "uitofp" -> emitUnsignedToFloatingCast(method, expression.semanticId, source.type, sourceType, targetType)
            "fptoui" -> emitFloatingToUnsignedCast(method, expression.semanticId, sourceType, targetTypeRef)
            else -> emitPrimitiveCast(method, expression.semanticId, sourceType, targetType)
        }
        storeExpressionResult(method, expression.semanticId, targetType)
    }

    /**
     * 发射 `ptrtoint`，将 ByteBuffer 指针 carrier 转为整数地址表示。
     */
    private fun emitPointerToInt(method: MethodVisitor, expression: ChirOtherExpression) {
        val source = expression.singleOperand("ptrtoint")
        val targetTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM ptrtoint requires result type", expression.semanticId)
        requirePointerType(source.type, expression.semanticId, "ptrtoint source")
        val targetType = context.typeMapper.mapValueType(targetTypeRef)
        if (targetType !in intLikeTypes) {
            throw JvmCodegenException("JVM ptrtoint target must be integer-like, actual ${targetType.descriptor}", expression.semanticId)
        }
        emitValue(method, source)
        method.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            JvmRuntimeArtifacts.POINTER_RUNTIME_INTERNAL_NAME,
            "toAddress",
            "(Ljava/nio/ByteBuffer;)J",
            false,
        )
        emitPrimitiveCast(method, expression.semanticId, Type.LONG_TYPE, targetType)
        storeExpressionResult(method, expression.semanticId, targetType)
    }

    /**
     * 发射 `inttoptr`，将整数地址表示恢复为 ByteBuffer 指针 carrier。
     */
    private fun emitIntToPointer(method: MethodVisitor, expression: ChirOtherExpression) {
        val source = expression.singleOperand("inttoptr")
        val targetTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM inttoptr requires result type", expression.semanticId)
        requirePointerType(targetTypeRef, expression.semanticId, "inttoptr result")
        val sourceType = context.typeMapper.mapValueType(source.type)
        if (sourceType !in intLikeTypes) {
            throw JvmCodegenException("JVM inttoptr source must be integer-like, actual ${sourceType.descriptor}", expression.semanticId)
        }
        emitValue(method, source)
        emitPrimitiveCast(method, expression.semanticId, sourceType, Type.LONG_TYPE)
        method.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            JvmRuntimeArtifacts.POINTER_RUNTIME_INTERNAL_NAME,
            "fromAddress",
            "(J)Ljava/nio/ByteBuffer;",
            false,
        )
        storeExpressionResult(method, expression.semanticId, byteBufferType)
    }

    /**
     * 发射 `jvm.new` intrinsic。
     *
     * 根据 ABI owner/descriptor 或 resultType 推导构造目标，生成 `new + dup + invokespecial <init>`。
     */
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
        emitArgumentsForDescriptor(method, expression.operands, descriptor, expression.semanticId)
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", descriptor, false)
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    /**
     * 发射 `jvm.getfield` intrinsic，并把字段值适配到 CHIR resultType。
     */
    private fun emitJvmGetField(method: MethodVisitor, expression: ChirOtherExpression) {
        val receiver = expression.requireOperandCount("jvm.getField", 1)[0]
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM getField expression requires result type", expression.semanticId)
        val owner = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.OWNER)
            ?: objectInternalName(context.typeMapper.mapValueType(receiver.type), expression.semanticId, "field receiver")
        val name = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.NAME, expression.semanticId)
        val descriptor = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(resultTypeRef).descriptor
        val fieldType = Type.getType(descriptor)
        val resultType = context.typeMapper.mapValueType(resultTypeRef)
        emitValue(method, receiver)
        adaptStackValue(
            method = method,
            nodeId = receiver.semanticId,
            actualType = context.typeMapper.mapValueType(receiver.type),
            targetType = Type.getObjectType(owner),
            location = "field receiver",
        )
        method.visitFieldInsn(Opcodes.GETFIELD, owner, name, descriptor)
        adaptStackValue(method, expression.semanticId, fieldType, resultType, "field read")
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    /**
     * 发射 `jvm.putfield` intrinsic。
     *
     * receiver 与写入值都会按字段 owner 和 descriptor 做栈值适配。
     */
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
        val fieldType = Type.getType(descriptor)
        emitValue(method, receiver)
        adaptStackValue(
            method = method,
            nodeId = receiver.semanticId,
            actualType = context.typeMapper.mapValueType(receiver.type),
            targetType = Type.getObjectType(owner),
            location = "field receiver",
        )
        emitValue(method, value)
        adaptStackValue(
            method = method,
            nodeId = value.semanticId,
            actualType = context.typeMapper.mapValueType(value.type),
            targetType = fieldType,
            location = "field write",
        )
        method.visitFieldInsn(Opcodes.PUTFIELD, owner, name, descriptor)
    }

    /**
     * 发射 `jvm.getstatic` intrinsic，并把静态字段值适配到 CHIR resultType。
     */
    private fun emitJvmGetStatic(method: MethodVisitor, expression: ChirOtherExpression) {
        expression.requireOperandCount("jvm.getStatic", 0)
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM getStatic expression requires result type", expression.semanticId)
        val owner = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.OWNER, expression.semanticId)
        val name = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.NAME, expression.semanticId)
        val descriptor = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(resultTypeRef).descriptor
        val fieldType = Type.getType(descriptor)
        val resultType = context.typeMapper.mapValueType(resultTypeRef)
        method.visitFieldInsn(Opcodes.GETSTATIC, owner, name, descriptor)
        adaptStackValue(method, expression.semanticId, fieldType, resultType, "static field read")
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    /**
     * 发射 `jvm.putstatic` intrinsic。
     */
    private fun emitJvmPutStatic(method: MethodVisitor, expression: ChirOtherExpression) {
        val value = expression.requireOperandCount("jvm.putStatic", 1)[0]
        expression.requireNoResultType("jvm.putStatic")
        val owner = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.OWNER, expression.semanticId)
        val name = JvmAbiAttributes.requireString(expression.attributes, JvmAbiAttributes.NAME, expression.semanticId)
        val descriptor = JvmAbiAttributes.optionalString(expression.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(value.type).descriptor
        val fieldType = Type.getType(descriptor)
        emitValue(method, value)
        adaptStackValue(
            method = method,
            nodeId = value.semanticId,
            actualType = context.typeMapper.mapValueType(value.type),
            targetType = fieldType,
            location = "static field write",
        )
        method.visitFieldInsn(Opcodes.PUTSTATIC, owner, name, descriptor)
    }

    /**
     * 发射 `jvm.newarray` intrinsic。
     *
     * 支持基本类型数组、对象数组、数组数组以及多维数组创建。
     */
    private fun emitJvmNewArray(method: MethodVisitor, expression: ChirOtherExpression) {
        val sizes = expression.operands
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM newArray expression requires result type", expression.semanticId)
        val arrayType = context.typeMapper.mapValueType(resultTypeRef)
        if (arrayType.sort != Type.ARRAY) {
            throw JvmCodegenException("JVM newArray result must be array type, actual ${arrayType.descriptor}", expression.semanticId)
        }
        if (sizes.isEmpty()) {
            throw JvmCodegenException("JVM newArray expression requires at least one size operand", expression.semanticId)
        }
        if (sizes.size > arrayType.dimensions) {
            throw JvmCodegenException(
                "JVM newArray has ${sizes.size} size operands for ${arrayType.dimensions}-dimension array",
                expression.semanticId,
            )
        }
        sizes.forEach { size -> emitArrayIndexValue(method, size, expression.semanticId) }
        if (sizes.size > 1) {
            method.visitMultiANewArrayInsn(arrayType.descriptor, sizes.size)
            storeExpressionResult(method, expression.semanticId, arrayType)
            return
        }
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

    /**
     * 发射 `jvm.arrayload` intrinsic。
     */
    private fun emitJvmArrayLoad(method: MethodVisitor, expression: ChirOtherExpression) {
        val operands = expression.requireOperandCount("jvm.arrayLoad", 2)
        val resultTypeRef = expression.resultType
            ?: throw JvmCodegenException("JVM arrayLoad expression requires result type", expression.semanticId)
        val resultType = context.typeMapper.mapValueType(resultTypeRef)
        val arrayType = emitArrayAndIndex(method, operands[0], operands[1], expression.semanticId)
        val elementType = arrayElementType(arrayType, expression.semanticId)
        method.visitInsn(elementType.getOpcode(Opcodes.IALOAD))
        adaptStackValue(method, expression.semanticId, elementType, resultType, "array load")
        storeExpressionResult(method, expression.semanticId, resultType)
    }

    /**
     * 发射 `jvm.arraystore` intrinsic。
     */
    private fun emitJvmArrayStore(method: MethodVisitor, expression: ChirOtherExpression) {
        val operands = expression.requireOperandCount("jvm.arrayStore", 3)
        expression.requireNoResultType("jvm.arrayStore")
        val valueType = context.typeMapper.mapValueType(operands[2].type)
        val arrayType = emitArrayAndIndex(method, operands[0], operands[1], expression.semanticId)
        val elementType = arrayElementType(arrayType, expression.semanticId)
        emitValue(method, operands[2])
        adaptStackValue(method, operands[2].semanticId, valueType, elementType, "array store")
        method.visitInsn(elementType.getOpcode(Opcodes.IASTORE))
    }

    /**
     * 发射 `jvm.arraylength` intrinsic，结果固定为 JVM int carrier。
     */
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

    /**
     * 发射 `jvm.checkcast` intrinsic。
     */
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

    /**
     * 发射 `jvm.instanceof` intrinsic，结果必须映射为 bool carrier。
     */
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

    /**
     * 发射 CHIR 基本块终结符。
     *
     * 分支和 unwind 在跳转前会为目标块 phi 表达式发射边赋值，return/throw 则走各自专用路径。
     */
    private fun emitTerminator(method: MethodVisitor, block: ChirBlock, terminator: Any) {
        when (terminator) {
            is ChirReturnTerminator -> emitReturn(method, terminator)
            is ChirBranchTerminator -> {
                emitPhiAssignmentsForEdge(method, block, terminator.targetBlockId)
                method.visitJumpInsn(Opcodes.GOTO, labelFor(terminator.targetBlockId))
            }
            is ChirConditionalBranchTerminator -> {
                val falsePathLabel = Label()
                emitValue(method, terminator.condition)
                method.visitJumpInsn(Opcodes.IFEQ, falsePathLabel)
                emitPhiAssignmentsForEdge(method, block, terminator.trueTargetBlockId)
                method.visitJumpInsn(Opcodes.GOTO, labelFor(terminator.trueTargetBlockId))
                method.visitLabel(falsePathLabel)
                emitPhiAssignmentsForEdge(method, block, terminator.falseTargetBlockId)
                method.visitJumpInsn(Opcodes.GOTO, labelFor(terminator.falseTargetBlockId))
            }
            is ChirThrowTerminator -> emitThrow(method, block, terminator)
            is ChirUnwindTerminator -> {
                emitPhiAssignmentsForEdge(method, block, terminator.targetBlockId)
                method.visitJumpInsn(Opcodes.GOTO, labelFor(terminator.targetBlockId))
            }
            else -> throw JvmCodegenException(
                "unsupported JVM terminator ${terminator::class.simpleName}",
                function.semanticId,
            )
        }
    }

    /**
     * 在控制流边上为目标块 phi 表达式发射 incoming 值赋值。
     *
     * 每个 phi operand 通过 `pred` 属性声明来源前驱块，匹配当前前驱后写入 phi 结果 slot。
     */
    private fun emitPhiAssignmentsForEdge(
        method: MethodVisitor,
        predecessorBlock: ChirBlock,
        targetBlockId: ChirSemanticId,
    ) {
        val targetBlock = function.blocks.firstOrNull { it.semanticId == targetBlockId }
            ?: throw JvmCodegenException("missing JVM phi target block", targetBlockId)
        targetBlock.expressions
            .filter(::isPhiExpression)
            .map { it as ChirOtherExpression }
            .forEach { phi ->
                val resultTypeRef = phi.resultType
                    ?: throw JvmCodegenException("JVM phi expression requires result type", phi.semanticId)
                val incoming = phi.operands.singleOrNull { operand -> operand.matchesPhiPredecessor(predecessorBlock) }
                    ?: throw JvmCodegenException(
                        "JVM phi expression is missing incoming value for predecessor ${predecessorBlock.semanticId.value}",
                        phi.semanticId,
                    )
                if (incoming.type != resultTypeRef) {
                    throw JvmCodegenException(
                        "JVM phi incoming type ${incoming.type.renderName} does not match result ${resultTypeRef.renderName}",
                        phi.semanticId,
                    )
                }
                val resultType = context.typeMapper.mapValueType(resultTypeRef)
                emitValue(method, incoming)
                storeExpressionResult(method, phi.semanticId, resultType)
            }
    }

    /**
     * 发射 return 终结符。
     *
     * 构造器和 void 方法只能无值返回；非 void 方法必须发射返回值并适配到 method descriptor 的返回类型。
     */
    private fun emitReturn(method: MethodVisitor, terminator: ChirReturnTerminator) {
        val returnValue = terminator.returnValue
        if (isConstructor) {
            if (returnValue != null) {
                throw JvmCodegenException("JVM constructor cannot return a value", terminator.semanticId)
            }
            method.visitInsn(Opcodes.RETURN)
            return
        }
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
        adaptStackValue(
            method = method,
            nodeId = returnValue.semanticId,
            actualType = context.typeMapper.mapValueType(returnValue.type),
            targetType = returnType,
            location = "return value",
        )
        method.visitInsn(returnType.getOpcode(Opcodes.IRETURN))
    }

    /**
     * 发射 throw 终结符。
     *
     * 没有 unwind 目标时直接 `athrow`；存在 unwind 目标时生成保护区和 catch-all handler，
     * 在 handler 中执行 phi 边赋值并跳转到 unwind 目标块。
     */
    private fun emitThrow(method: MethodVisitor, block: ChirBlock, terminator: ChirThrowTerminator) {
        val exceptionType = context.typeMapper.mapValueType(terminator.exceptionValue.type)
        objectInternalName(exceptionType, terminator.semanticId, "throw exception")
        val unwindTargetBlockId = terminator.unwindTargetBlockId
        if (unwindTargetBlockId == null) {
            emitValue(method, terminator.exceptionValue)
            method.visitInsn(Opcodes.ATHROW)
            return
        }

        val protectedStart = Label()
        val protectedEnd = Label()
        val handlerEntry = Label()
        method.visitTryCatchBlock(protectedStart, protectedEnd, handlerEntry, null)
        method.visitLabel(protectedStart)
        emitValue(method, terminator.exceptionValue)
        method.visitInsn(Opcodes.ATHROW)
        method.visitLabel(protectedEnd)
        method.visitLabel(handlerEntry)
        method.visitInsn(Opcodes.POP)
        emitPhiAssignmentsForEdge(method, block, unwindTargetBlockId)
        method.visitJumpInsn(Opcodes.GOTO, labelFor(unwindTargetBlockId))
    }

    /**
     * 将 CHIR value 物化为 JVM 操作数栈上的值。
     */
    private fun emitValue(method: MethodVisitor, value: ChirValue) {
        when (value) {
            is ChirConstantValue -> emitConstant(method, value)
            is ChirParameterValue -> loadSlot(method, value.semanticId, value.name, value.type)
            is ChirLocalValue -> loadSlot(method, value.semanticId, value.name, value.type)
            is ChirFunctionValue -> emitFunctionHandle(method, value)
            is ChirGlobalValue -> emitGlobalValue(method, value)
            is ChirImportedFunctionValue -> emitImportedFunctionHandle(method, value)
            is ChirImportedVariableValue -> emitImportedVariable(method, value)
            is ChirBlockValue, is ChirBlockGroupValue -> throw JvmCodegenException(
                "block value cannot be materialized as a JVM stack value",
                value.semanticId,
            )
        }
    }

    /**
     * 发射全局变量读取。
     *
     * 默认从模块 facade 的静态字段读取，也允许 ABI 属性指定外部 owner/name/descriptor。
     */
    private fun emitGlobalValue(method: MethodVisitor, value: ChirGlobalValue) {
        val owner = JvmAbiAttributes.optionalString(value.attributes, JvmAbiAttributes.OWNER) ?: moduleFacadeInternalName
        val name = JvmAbiAttributes.optionalString(value.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.fieldJvmName(value.name)
        val descriptor = JvmAbiAttributes.optionalString(value.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(value.type).descriptor
        val fieldType = Type.getType(descriptor)
        val valueType = context.typeMapper.mapValueType(value.type)
        method.visitFieldInsn(Opcodes.GETSTATIC, owner, name, descriptor)
        adaptStackValue(method, value.semanticId, fieldType, valueType, "global value")
    }

    /**
     * 将本地函数值发射为 JVM `MethodHandle`。
     */
    private fun emitFunctionHandle(method: MethodVisitor, value: ChirFunctionValue) {
        val functionType = (value.type as? ChirResolvedTypeRef)?.type as? ChirFunctionType
            ?: throw JvmCodegenException("JVM function value '${value.name}' must carry ChirFunctionType", value.semanticId)
        val owner = JvmAbiAttributes.optionalString(value.attributes, JvmAbiAttributes.OWNER)
            ?: functionType.receiverType?.let { objectInternalName(context.typeMapper.mapValueType(it), value.semanticId, "function handle owner") }
            ?: moduleFacadeInternalName
        val name = JvmAbiAttributes.optionalString(value.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.functionJvmName(value.name)
        val invokeKind = JvmAbiAttributes.optionalString(value.attributes, JvmAbiAttributes.INVOKE_KIND)
            ?: if (functionType.receiverType == null) "static" else "virtual"
        emitMethodHandleLookup(
            method = method,
            nodeId = value.semanticId,
            owner = owner,
            name = name,
            invokeKind = invokeKind,
            functionType = functionType,
            descriptor = JvmAbiAttributes.optionalString(value.attributes, JvmAbiAttributes.DESCRIPTOR),
        )
    }

    /**
     * 将导入 JVM 函数值发射为 `MethodHandle`。
     *
     * 导入函数必须携带 owner/name/invokeKind ABI 属性，descriptor 可选。
     */
    private fun emitImportedFunctionHandle(method: MethodVisitor, value: ChirImportedFunctionValue) {
        val functionType = (value.type as? ChirResolvedTypeRef)?.type as? ChirFunctionType
            ?: throw JvmCodegenException("JVM imported function value '${value.name}' must carry ChirFunctionType", value.semanticId)
        val owner = JvmAbiAttributes.requireString(value.attributes, JvmAbiAttributes.OWNER, value.semanticId)
        val name = JvmAbiAttributes.requireString(value.attributes, JvmAbiAttributes.NAME, value.semanticId)
        val invokeKind = JvmAbiAttributes.requireString(value.attributes, JvmAbiAttributes.INVOKE_KIND, value.semanticId)
        emitMethodHandleLookup(
            method = method,
            nodeId = value.semanticId,
            owner = owner,
            name = name,
            invokeKind = invokeKind,
            functionType = functionType,
            descriptor = JvmAbiAttributes.optionalString(value.attributes, JvmAbiAttributes.DESCRIPTOR),
        )
    }

    /**
     * 发射 MethodHandles.Lookup 查找逻辑。
     *
     * 根据 invokeKind 选择 findConstructor、findStatic、findVirtual 或 findSpecial，
     * 并按 CHIR 函数类型或显式 descriptor 构建 MethodType。
     */
    private fun emitMethodHandleLookup(
        method: MethodVisitor,
        nodeId: ChirSemanticId,
        owner: String,
        name: String,
        invokeKind: String,
        functionType: ChirFunctionType,
        descriptor: String?,
    ) {
        method.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/lang/invoke/MethodHandles",
            "lookup",
            "()Ljava/lang/invoke/MethodHandles\$Lookup;",
            false,
        )
        method.visitLdcInsn(Type.getObjectType(owner))
        when (normalizeInvokeKind(invokeKind)) {
            "constructor" -> {
                if (functionType.receiverType != null) {
                    throw JvmCodegenException("constructor JVM function handle '$name' must not declare receiverType", nodeId)
                }
                if (name != "<init>") {
                    throw JvmCodegenException("constructor JVM function handle must use '<init>' method name", nodeId)
                }
                if (descriptor == null) {
                    emitMethodType(method, ChirResolvedTypeRef(ChirPrimitiveType.VOID), functionType.parameterTypes)
                } else {
                    emitMethodTypeFromDescriptor(method, descriptor)
                }
                method.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles\$Lookup",
                    "findConstructor",
                    "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                    false,
                )
            }
            "static" -> {
                if (functionType.receiverType != null) {
                    throw JvmCodegenException("static JVM function handle '$name' must not declare receiverType", nodeId)
                }
                method.visitLdcInsn(name)
                if (descriptor == null) {
                    emitMethodType(method, functionType.returnType, functionType.parameterTypes)
                } else {
                    emitMethodTypeFromDescriptor(method, descriptor)
                }
                method.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles\$Lookup",
                    "findStatic",
                    "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                    false,
                )
            }
            "virtual", "interface" -> {
                requireImportedReceiver(functionType.receiverType, nodeId, name)
                method.visitLdcInsn(name)
                if (descriptor == null) {
                    emitMethodType(method, functionType.returnType, functionType.parameterTypes)
                } else {
                    emitMethodTypeFromDescriptor(method, descriptor)
                }
                method.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles\$Lookup",
                    "findVirtual",
                    "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                    false,
                )
            }
            "special" -> {
                requireImportedReceiver(functionType.receiverType, nodeId, name)
                method.visitLdcInsn(name)
                if (descriptor == null) {
                    emitMethodType(method, functionType.returnType, functionType.parameterTypes)
                } else {
                    emitMethodTypeFromDescriptor(method, descriptor)
                }
                method.visitLdcInsn(Type.getObjectType(owner))
                method.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles\$Lookup",
                    "findSpecial",
                    "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
                    false,
                )
            }
            else -> throw JvmCodegenException("unsupported JVM method handle invoke kind '$invokeKind'", nodeId)
        }
    }

    /**
     * 从 CHIR 函数类型构造 JVM 构造器 descriptor。
     */
    private fun constructorDescriptor(functionType: ChirFunctionType): String {
        return Type.getMethodDescriptor(
            Type.VOID_TYPE,
            *functionType.parameterTypes.map { context.typeMapper.mapValueType(it) }.toTypedArray(),
        )
    }

    /**
     * 规范化 ABI 中的 JVM 调用种类别名。
     */
    private fun normalizeInvokeKind(invokeKind: String): String {
        return when (val normalized = invokeKind.lowercase().trim()) {
            "constructor", "newspecial", "new-special", "new_special" -> "constructor"
            else -> normalized
        }
    }

    /**
     * 发射导入 JVM 静态变量读取。
     */
    private fun emitImportedVariable(method: MethodVisitor, value: ChirImportedVariableValue) {
        val owner = JvmAbiAttributes.requireString(value.attributes, JvmAbiAttributes.OWNER, value.semanticId)
        val name = JvmAbiAttributes.requireString(value.attributes, JvmAbiAttributes.NAME, value.semanticId)
        val descriptor = JvmAbiAttributes.optionalString(value.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(value.type).descriptor
        val fieldType = Type.getType(descriptor)
        val valueType = context.typeMapper.mapValueType(value.type)
        method.visitFieldInsn(
            Opcodes.GETSTATIC,
            owner,
            name,
            descriptor,
        )
        adaptStackValue(method, value.semanticId, fieldType, valueType, "imported variable")
    }

    /**
     * 发射 CHIR 常量到 JVM 操作数栈。
     *
     * 支持 null/nullptr、布尔、整数、浮点与字符串常量，整数常量会按有符号/无符号 carrier 解析。
     */
    private fun emitConstant(method: MethodVisitor, value: ChirConstantValue) {
        val type = context.typeMapper.mapValueType(value.type)
        val normalizedLiteral = value.literal.lowercase()
        if (
            (type.sort == Type.OBJECT || type.sort == Type.ARRAY) &&
            (normalizedLiteral == "nullptr" || (normalizedLiteral == "null" && type != stringObjectType))
        ) {
            method.visitInsn(Opcodes.ACONST_NULL)
            return
        }
        when (type) {
            Type.BOOLEAN_TYPE -> method.visitInsn(if (value.literal.equals("true", ignoreCase = true) || value.literal == "1") Opcodes.ICONST_1 else Opcodes.ICONST_0)
            Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE -> method.visitLdcInsn(parseIntCarrierLiteral(value))
            Type.LONG_TYPE -> method.visitLdcInsn(parseLongCarrierLiteral(value))
            Type.FLOAT_TYPE -> method.visitLdcInsn(value.literal.toFloat())
            Type.DOUBLE_TYPE -> method.visitLdcInsn(value.literal.toDouble())
            Type.getObjectType("java/lang/String") -> method.visitLdcInsn(value.literal)
            else -> throw JvmCodegenException("unsupported JVM constant type ${type.descriptor}", value.semanticId)
        }
    }

    /**
     * 将整数常量解析为 JVM int carrier。
     */
    private fun parseIntCarrierLiteral(value: ChirConstantValue): Int {
        return when (resolvedPrimitiveType(value.type)) {
            ChirPrimitiveType.UINT8,
            ChirPrimitiveType.UINT16,
            ChirPrimitiveType.UINT32,
            ChirPrimitiveType.UINT_NATIVE,
            -> parseUnsignedIntegerLiteral(value.literal).toInt()
            else -> parseSignedIntegerLiteral(value.literal).toInt()
        }
    }

    /**
     * 将整数常量解析为 JVM long carrier。
     */
    private fun parseLongCarrierLiteral(value: ChirConstantValue): Long {
        return when (resolvedPrimitiveType(value.type)) {
            ChirPrimitiveType.UINT64 -> parseUnsignedIntegerLiteral(value.literal)
            else -> parseSignedIntegerLiteral(value.literal)
        }
    }

    /**
     * 解析有符号整数字面量，支持正负号、下划线和常见进制前缀。
     */
    private fun parseSignedIntegerLiteral(literal: String): Long {
        val (negative, digits, radix) = splitIntegerLiteral(literal)
        val parsed = java.lang.Long.parseLong(digits, radix)
        return if (negative) -parsed else parsed
    }

    /**
     * 解析无符号整数字面量，负数字面量仍按有符号路径保持 JVM carrier 语义。
     */
    private fun parseUnsignedIntegerLiteral(literal: String): Long {
        val (negative, digits, radix) = splitIntegerLiteral(literal)
        if (negative) {
            return parseSignedIntegerLiteral(literal)
        }
        return java.lang.Long.parseUnsignedLong(digits, radix)
    }

    /**
     * 拆分整数字面量的符号、数字主体和进制。
     */
    private fun splitIntegerLiteral(literal: String): ParsedIntegerLiteral {
        val trimmed = literal.trim().replace("_", "")
        val negative = trimmed.startsWith("-")
        val unsigned = trimmed.removePrefix("+").removePrefix("-")
        val (digits, radix) = when {
            unsigned.startsWith("0x", ignoreCase = true) -> unsigned.drop(2) to 16
            unsigned.startsWith("0b", ignoreCase = true) -> unsigned.drop(2) to 2
            unsigned.startsWith("0o", ignoreCase = true) -> unsigned.drop(2) to 8
            else -> unsigned to 10
        }
        return ParsedIntegerLiteral(negative, digits, radix)
    }

    /**
     * 将表达式结果从 JVM 操作数栈存入与 semanticId 绑定的 local slot。
     *
     * 首次写入会分配 slot，并额外以结果 id 的规范化名称建立查找入口。
     */
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

    /**
     * 从 local slot 读取 CHIR value。
     *
     * 查找顺序为 semanticId、规范化名称、结果 id 名称；slot 类型与期望类型不一致时执行栈值适配。
     */
    private fun loadSlot(method: MethodVisitor, id: ChirSemanticId, name: String, expectedType: ChirTypeRef) {
        val slot = localSlots[id]
            ?: localSlotsByName[normalizeLocalName(name)]
            ?: localSlotsByName[resultLocalName(id)]
            ?: throw JvmCodegenException("unbound JVM local value '$name'", id)
        val type = context.typeMapper.mapValueType(expectedType)
        if (slot.type == type) {
            method.visitVarInsn(type.getOpcode(Opcodes.ILOAD), slot.index)
            return
        }
        method.visitVarInsn(slot.type.getOpcode(Opcodes.ILOAD), slot.index)
        adaptStackValue(method, id, slot.type, type, "local load")
    }

    /**
     * 确保 CHIR ref 地址拥有可读写的 JVM local slot。
     *
     * 对还没有绑定的地址分配新 slot，并用地址 semanticId 与名称同时登记。
     */
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

    /**
     * 解析 memory 操作目标值类型。
     *
     * CHIR ref 返回 referencedType，CHIR cpointer 返回 pointeeType。
     */
    private fun memoryTargetType(address: ChirValue, nodeId: ChirSemanticId): ChirTypeRef {
        val resolvedAddressType = (address.type as? ChirResolvedTypeRef)?.type
            ?: throw JvmCodegenException("JVM memory address must have resolved type, actual ${address.type.renderName}", nodeId)
        return when (resolvedAddressType) {
            is ChirRefType -> resolvedAddressType.referencedType
            is ChirCPointerType -> resolvedAddressType.pointeeType
            else -> throw JvmCodegenException("JVM memory address must have CHIR ref or cpointer type, actual ${address.type.renderName}", nodeId)
        }
    }

    /**
     * 解析 CHIR ref 地址引用的目标类型。
     */
    private fun refTargetType(address: ChirValue, nodeId: ChirSemanticId): ChirTypeRef {
        val referenceType = (address.type as? ChirResolvedTypeRef)?.type as? ChirRefType
            ?: throw JvmCodegenException("JVM memory address must have CHIR ref type, actual ${address.type.renderName}", nodeId)
        return referenceType.referencedType
    }

    /**
     * 判断地址值是否为 CHIR cpointer。
     */
    private fun isPointerAddress(address: ChirValue): Boolean {
        return ((address.type as? ChirResolvedTypeRef)?.type as? ChirCPointerType) != null
    }

    /**
     * 要求给定类型引用是 CHIR cpointer，并返回解析后的指针类型。
     */
    private fun requirePointerType(typeRef: ChirTypeRef, nodeId: ChirSemanticId, location: String): ChirCPointerType {
        return (typeRef as? ChirResolvedTypeRef)?.type as? ChirCPointerType
            ?: throw JvmCodegenException("JVM $location must have CHIR cpointer type, actual ${typeRef.renderName}", nodeId)
    }

    /**
     * 发射 JVM 数值算术指令。
     */
    private fun emitArithmetic(method: MethodVisitor, id: ChirSemanticId, type: Type, intOpcode: Int) {
        requireArithmeticType(type, id)
        method.visitInsn(type.getOpcode(intOpcode))
    }

    /**
     * 发射无符号除法或取余。
     *
     * long 使用 `Long.*Unsigned`；较窄整数先扩展到 int 后使用 `Integer.*Unsigned`。
     */
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

    /**
     * 发射整数类位运算指令。
     */
    private fun emitIntLike(method: MethodVisitor, id: ChirSemanticId, type: Type, intOpcode: Int) {
        requireIntLikeType(type, id)
        method.visitInsn(type.getOpcode(intOpcode))
    }

    /**
     * 发射 JVM 移位指令。
     *
     * long 移位前会把右操作数规约为 int，符合 JVM shift 指令的栈形状。
     */
    private fun emitShift(method: MethodVisitor, id: ChirSemanticId, type: Type, intOpcode: Int) {
        requireIntLikeType(type, id)
        if (type == Type.LONG_TYPE) {
            method.visitInsn(Opcodes.L2I)
        }
        method.visitInsn(type.getOpcode(intOpcode))
    }

    /**
     * 发射有符号数值比较，并在栈顶留下 0/1 布尔 carrier。
     */
    private fun emitComparison(
        method: MethodVisitor,
        type: Type,
        intCompareOpcode: Int,
        zeroCompareOpcode: Int,
    ) {
        val trueLabel = Label()
        val endLabel = Label()
        when (type) {
            Type.INT_TYPE, Type.SHORT_TYPE, Type.BYTE_TYPE, Type.BOOLEAN_TYPE -> method.visitJumpInsn(intCompareOpcode, trueLabel)
            Type.LONG_TYPE -> {
                method.visitInsn(Opcodes.LCMP)
                method.visitJumpInsn(zeroCompareOpcode, trueLabel)
            }
            Type.FLOAT_TYPE -> {
                method.visitInsn(floatingComparisonOpcode(zeroCompareOpcode))
                method.visitJumpInsn(zeroCompareOpcode, trueLabel)
            }
            Type.DOUBLE_TYPE -> {
                method.visitInsn(doubleComparisonOpcode(floatingComparisonOpcode(zeroCompareOpcode)))
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

    /**
     * 选择 float 比较使用的 `FCMPL` 或 `FCMPG` 指令。
     *
     * JVM 浮点比较必须按操作符选择 *CMPL/*CMPG，保证 NaN 下有序比较为 false。
     */
    private fun floatingComparisonOpcode(zeroCompareOpcode: Int): Int {
        return when (zeroCompareOpcode) {
            Opcodes.IFLT, Opcodes.IFLE -> Opcodes.FCMPG
            Opcodes.IFGT, Opcodes.IFGE, Opcodes.IFEQ, Opcodes.IFNE -> Opcodes.FCMPL
            else -> throw JvmCodegenException("unsupported JVM comparison jump opcode $zeroCompareOpcode", function.semanticId)
        }
    }

    /**
     * 将 float 比较指令映射为 double 比较指令。
     */
    private fun doubleComparisonOpcode(floatComparisonOpcode: Int): Int {
        return when (floatComparisonOpcode) {
            Opcodes.FCMPL -> Opcodes.DCMPL
            Opcodes.FCMPG -> Opcodes.DCMPG
            else -> throw JvmCodegenException("unsupported JVM floating comparison opcode $floatComparisonOpcode", function.semanticId)
        }
    }

    /**
     * 发射相等性比较。
     *
     * 引用类型使用 `if_acmp*`，其他类型复用数值比较路径。
     */
    private fun emitEqualityComparison(
        method: MethodVisitor,
        type: Type,
        intCompareOpcode: Int,
        referenceCompareOpcode: Int,
        zeroCompareOpcode: Int,
    ) {
        if (type.sort == Type.OBJECT || type.sort == Type.ARRAY) {
            emitReferenceComparison(method, referenceCompareOpcode)
        } else {
            emitComparison(method, type, intCompareOpcode, zeroCompareOpcode)
        }
    }

    /**
     * 发射引用相等或不等比较，并在栈顶留下 0/1。
     */
    private fun emitReferenceComparison(method: MethodVisitor, referenceCompareOpcode: Int) {
        val trueLabel = Label()
        val endLabel = Label()
        method.visitJumpInsn(referenceCompareOpcode, trueLabel)
        method.visitInsn(Opcodes.ICONST_0)
        method.visitJumpInsn(Opcodes.GOTO, endLabel)
        method.visitLabel(trueLabel)
        method.visitInsn(Opcodes.ICONST_1)
        method.visitLabel(endLabel)
    }

    /**
     * 发射无符号整数比较，并在栈顶留下 0/1。
     */
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

    /**
     * 将栈顶 int 与 0 比较，并把跳转结果规范化为 0/1。
     */
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

    /**
     * 发射 JVM 原始类型之间的转换。
     */
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

    /**
     * 发射无符号整数零扩展。
     *
     * 对较窄无符号整数使用掩码；`uint32` 到 long 时额外清理高位。
     */
    private fun emitZeroExtension(
        method: MethodVisitor,
        id: ChirSemanticId,
        sourceTypeRef: ChirTypeRef,
        sourceType: Type,
        targetType: Type,
    ) {
        when (resolvedPrimitiveType(sourceTypeRef)) {
            ChirPrimitiveType.UINT8 -> {
                method.visitLdcInsn(0xFF)
                method.visitInsn(Opcodes.IAND)
                emitPrimitiveCast(method, id, Type.INT_TYPE, targetType)
            }
            ChirPrimitiveType.UINT16 -> {
                method.visitLdcInsn(0xFFFF)
                method.visitInsn(Opcodes.IAND)
                emitPrimitiveCast(method, id, Type.INT_TYPE, targetType)
            }
            ChirPrimitiveType.UINT32,
            ChirPrimitiveType.UINT_NATIVE,
            -> {
                if (targetType == Type.LONG_TYPE) {
                    method.visitInsn(Opcodes.I2L)
                    method.visitLdcInsn(0xFFFF_FFFFL)
                    method.visitInsn(Opcodes.LAND)
                } else {
                    emitPrimitiveCast(method, id, sourceType, targetType)
                }
            }
            ChirPrimitiveType.UINT64 -> emitPrimitiveCast(method, id, sourceType, targetType)
            else -> emitPrimitiveCast(method, id, sourceType, targetType)
        }
    }

    /**
     * 发射无符号整数到浮点或其他数值类型的转换。
     */
    private fun emitUnsignedToFloatingCast(
        method: MethodVisitor,
        id: ChirSemanticId,
        sourceTypeRef: ChirTypeRef,
        sourceType: Type,
        targetType: Type,
    ) {
        if (targetType != Type.FLOAT_TYPE && targetType != Type.DOUBLE_TYPE) {
            emitZeroExtension(method, id, sourceTypeRef, sourceType, targetType)
            return
        }
        when (resolvedPrimitiveType(sourceTypeRef)) {
            ChirPrimitiveType.UINT8 -> {
                method.visitLdcInsn(0xFF)
                method.visitInsn(Opcodes.IAND)
                emitIntToFloating(method, targetType)
            }
            ChirPrimitiveType.UINT16 -> {
                method.visitLdcInsn(0xFFFF)
                method.visitInsn(Opcodes.IAND)
                emitIntToFloating(method, targetType)
            }
            ChirPrimitiveType.UINT32,
            ChirPrimitiveType.UINT_NATIVE,
            -> {
                method.visitInsn(Opcodes.I2L)
                method.visitLdcInsn(0xFFFF_FFFFL)
                method.visitInsn(Opcodes.LAND)
                emitLongToFloating(method, targetType)
            }
            ChirPrimitiveType.UINT64 -> {
                method.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Long",
                    "toUnsignedString",
                    "(J)Ljava/lang/String;",
                    false,
                )
                method.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Double",
                    "parseDouble",
                    "(Ljava/lang/String;)D",
                    false,
                )
                if (targetType == Type.FLOAT_TYPE) {
                    method.visitInsn(Opcodes.D2F)
                }
            }
            else -> emitPrimitiveCast(method, id, sourceType, targetType)
        }
    }

    /**
     * 发射浮点到无符号整数的转换。
     *
     * 该路径委托运行时辅助方法处理 JVM 本身缺失的无符号浮点截断语义。
     */
    private fun emitFloatingToUnsignedCast(
        method: MethodVisitor,
        id: ChirSemanticId,
        sourceType: Type,
        targetTypeRef: ChirTypeRef,
    ) {
        when (sourceType) {
            Type.FLOAT_TYPE -> method.visitInsn(Opcodes.F2D)
            Type.DOUBLE_TYPE -> Unit
            else -> throw JvmCodegenException("JVM fptoui source type ${sourceType.descriptor} is not floating-point", id)
        }
        val methodName = when (resolvedPrimitiveType(targetTypeRef)) {
            ChirPrimitiveType.UINT8 -> "doubleToUInt8"
            ChirPrimitiveType.UINT16 -> "doubleToUInt16"
            ChirPrimitiveType.UINT32,
            ChirPrimitiveType.UINT_NATIVE,
            -> "doubleToUInt32"
            ChirPrimitiveType.UINT64 -> "doubleToUInt64"
            else -> throw JvmCodegenException("JVM fptoui target type ${targetTypeRef.renderName} is not unsigned integer", id)
        }
        val targetDescriptor = context.typeMapper.mapValueType(targetTypeRef).descriptor
        method.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            JvmRuntimeArtifacts.UNSIGNED_RUNTIME_INTERNAL_NAME,
            methodName,
            "(D)$targetDescriptor",
            false,
        )
    }

    /**
     * 发射 int 到 float/double 的 JVM 转换指令。
     */
    private fun emitIntToFloating(method: MethodVisitor, targetType: Type) {
        when (targetType) {
            Type.FLOAT_TYPE -> method.visitInsn(Opcodes.I2F)
            Type.DOUBLE_TYPE -> method.visitInsn(Opcodes.I2D)
        }
    }

    /**
     * 发射 long 到 float/double 的 JVM 转换指令。
     */
    private fun emitLongToFloating(method: MethodVisitor, targetType: Type) {
        when (targetType) {
            Type.FLOAT_TYPE -> method.visitInsn(Opcodes.L2F)
            Type.DOUBLE_TYPE -> method.visitInsn(Opcodes.L2D)
        }
    }

    /**
     * 从 CHIR 类型引用中提取已解析的原始类型。
     */
    private fun resolvedPrimitiveType(typeRef: ChirTypeRef): ChirPrimitiveType? {
        return (typeRef as? ChirResolvedTypeRef)?.type as? ChirPrimitiveType
    }

    /**
     * 发射 int carrier 到目标原始类型的转换。
     */
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

    /**
     * 发射 long carrier 到目标原始类型的转换。
     */
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

    /**
     * 发射 float carrier 到目标原始类型的转换。
     */
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

    /**
     * 发射 double carrier 到目标原始类型的转换。
     */
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

    /**
     * 向栈顶压入与指定整数 carrier 匹配的 -1 常量。
     */
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

    /**
     * 将栈顶值暂存到新的 JVM local slot。
     */
    private fun storeTemp(method: MethodVisitor, type: Type): LocalSlot {
        val slot = LocalSlot(nextLocalSlot, type)
        nextLocalSlot += type.size
        method.visitVarInsn(type.getOpcode(Opcodes.ISTORE), slot.index)
        return slot
    }

    /**
     * 从临时 slot 读取无符号 int-like carrier，并按位宽清理高位。
     */
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

    /**
     * 要求 `ChirOtherExpression` 恰好只有一个操作数，并返回该操作数。
     */
    private fun ChirOtherExpression.singleOperand(operation: String): ChirValue {
        if (operands.size != 1) {
            throw JvmCodegenException("JVM $operation expression requires exactly one operand", semanticId)
        }
        return operands.single()
    }

    /**
     * 校验 `ChirOtherExpression` 的操作数数量。
     */
    private fun ChirOtherExpression.requireOperandCount(operation: String, expected: Int): List<ChirValue> {
        if (operands.size != expected) {
            throw JvmCodegenException(
                "JVM $operation expression requires $expected operand(s), actual ${operands.size}",
                semanticId,
            )
        }
        return operands
    }

    /**
     * 校验 `ChirOtherExpression` 不声明结果类型。
     */
    private fun ChirOtherExpression.requireNoResultType(operation: String) {
        if (resultType != null) {
            throw JvmCodegenException("JVM $operation expression must not declare result type", semanticId)
        }
    }

    /**
     * 判断表达式是否为 CFG phi 节点。
     */
    private fun isPhiExpression(expression: ChirExpression): Boolean {
        return expression is ChirOtherExpression && expression.operation.lowercase().trim() == "phi"
    }

    /**
     * 判断 phi incoming value 是否来源于给定前驱块。
     *
     * 匹配依据是 value 上的 `pred` 字符串属性，可使用前驱块 semanticId 或块名。
     */
    private fun ChirValue.matchesPhiPredecessor(predecessorBlock: ChirBlock): Boolean {
        val predecessor = attributes
            .asSequence()
            .filterIsInstance<ChirStringAttribute>()
            .singleOrNull { it.key == "pred" }
            ?.value
            ?: throw JvmCodegenException(
                "JVM phi operand ${semanticId.value} is missing required 'pred' attribute",
                predecessorBlock.semanticId,
            )
        return predecessor == predecessorBlock.semanticId.value || predecessor == predecessorBlock.name
    }

    /**
     * 获取 ByteBuffer 指针 carrier 对指定元素类型的 get/put 方法描述。
     */
    private fun pointerAccessor(type: Type, nodeId: ChirSemanticId): PointerAccessor {
        return when (type) {
            Type.BOOLEAN_TYPE, Type.BYTE_TYPE -> PointerAccessor("get", "(I)B", "put", "(IB)Ljava/nio/ByteBuffer;")
            Type.SHORT_TYPE -> PointerAccessor("getShort", "(I)S", "putShort", "(IS)Ljava/nio/ByteBuffer;")
            Type.INT_TYPE -> PointerAccessor("getInt", "(I)I", "putInt", "(II)Ljava/nio/ByteBuffer;")
            Type.LONG_TYPE -> PointerAccessor("getLong", "(I)J", "putLong", "(IJ)Ljava/nio/ByteBuffer;")
            Type.FLOAT_TYPE -> PointerAccessor("getFloat", "(I)F", "putFloat", "(IF)Ljava/nio/ByteBuffer;")
            Type.DOUBLE_TYPE -> PointerAccessor("getDouble", "(I)D", "putDouble", "(ID)Ljava/nio/ByteBuffer;")
            else -> throw JvmCodegenException("JVM pointer memory does not support element type ${type.descriptor}", nodeId)
        }
    }

    /**
     * 计算指针元素类型在 ByteBuffer 中占用的字节数。
     */
    private fun pointerElementByteSize(typeRef: ChirTypeRef, nodeId: ChirSemanticId): Int {
        val type = context.typeMapper.mapValueType(typeRef)
        return when (type) {
            Type.BOOLEAN_TYPE, Type.BYTE_TYPE -> 1
            Type.SHORT_TYPE -> 2
            Type.INT_TYPE, Type.FLOAT_TYPE -> 4
            Type.LONG_TYPE, Type.DOUBLE_TYPE -> 8
            else -> throw JvmCodegenException("JVM pointer gep does not support element type ${type.descriptor}", nodeId)
        }
    }

    /**
     * 从 JVM descriptor 字符串构建 `MethodType`。
     *
     * 运行时使用当前线程 context classloader 解析 descriptor 中的引用类型。
     */
    private fun emitMethodTypeFromDescriptor(method: MethodVisitor, descriptor: String) {
        method.visitLdcInsn(descriptor)
        method.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/lang/Thread",
            "currentThread",
            "()Ljava/lang/Thread;",
            false,
        )
        method.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/Thread",
            "getContextClassLoader",
            "()Ljava/lang/ClassLoader;",
            false,
        )
        method.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/lang/invoke/MethodType",
            "fromMethodDescriptorString",
            "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;",
            false,
        )
    }

    /**
     * 根据 CHIR 函数返回类型和参数类型构建 JVM `MethodType`。
     */
    private fun emitMethodType(
        method: MethodVisitor,
        returnTypeRef: ChirTypeRef,
        parameterTypeRefs: List<ChirTypeRef>,
    ) {
        emitClassLiteral(method, context.typeMapper.mapReturnType(returnTypeRef))
        method.visitLdcInsn(parameterTypeRefs.size)
        method.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class")
        parameterTypeRefs.forEachIndexed { index, parameterTypeRef ->
            method.visitInsn(Opcodes.DUP)
            method.visitLdcInsn(index)
            emitClassLiteral(method, context.typeMapper.mapValueType(parameterTypeRef))
            method.visitInsn(Opcodes.AASTORE)
        }
        method.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/lang/invoke/MethodType",
            "methodType",
            "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;",
            false,
        )
    }

    /**
     * 发射 JVM class literal。
     *
     * void 与 primitive 使用对应 wrapper 的 `TYPE` 字段，引用和数组类型直接用 ASM `Type` 常量。
     */
    private fun emitClassLiteral(method: MethodVisitor, type: Type) {
        if (type == Type.VOID_TYPE) {
            method.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;")
        } else if (type.sort in primitiveClassLiteralSorts) {
            val boxing = primitiveBoxing(type)
                ?: throw JvmCodegenException("JVM primitive class literal is missing wrapper for ${type.descriptor}", function.semanticId)
            method.visitFieldInsn(Opcodes.GETSTATIC, boxing.wrapperInternalName, "TYPE", "Ljava/lang/Class;")
        } else {
            method.visitLdcInsn(type)
        }
    }

    /**
     * 将一组 CHIR value 装箱为 `Object[]`。
     */
    private fun emitObjectArray(method: MethodVisitor, values: List<ChirValue>) {
        method.visitLdcInsn(values.size)
        method.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object")
        values.forEachIndexed { index, value ->
            method.visitInsn(Opcodes.DUP)
            method.visitLdcInsn(index)
            emitBoxedValue(method, value)
            method.visitInsn(Opcodes.AASTORE)
        }
    }

    /**
     * 发射 value 并在需要时装箱为 JVM wrapper 对象。
     */
    private fun emitBoxedValue(method: MethodVisitor, value: ChirValue) {
        val type = context.typeMapper.mapValueType(value.type)
        emitValue(method, value)
        val boxing = primitiveBoxing(type) ?: return
        method.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            boxing.wrapperInternalName,
            "valueOf",
            "(${type.descriptor})L${boxing.wrapperInternalName};",
            false,
        )
    }

    /**
     * 将动态调用返回的 `Object` 还原为目标 JVM carrier。
     */
    private fun emitCoerceObjectResult(method: MethodVisitor, nodeId: ChirSemanticId, targetType: Type) {
        val boxing = primitiveBoxing(targetType)
        if (boxing != null) {
            method.visitTypeInsn(Opcodes.CHECKCAST, boxing.wrapperInternalName)
            method.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                boxing.wrapperInternalName,
                boxing.unboxMethodName,
                "()${targetType.descriptor}",
                false,
            )
            return
        }
        when (targetType.sort) {
            Type.OBJECT -> method.visitTypeInsn(Opcodes.CHECKCAST, targetType.internalName)
            Type.ARRAY -> method.visitTypeInsn(Opcodes.CHECKCAST, targetType.descriptor)
            else -> throw JvmCodegenException("JVM dynamic call cannot coerce result to ${targetType.descriptor}", nodeId)
        }
    }

    /**
     * 发射实例调用的 receiver 与普通实参，并按 descriptor 进行类型适配。
     */
    private fun emitReceiverAndArgumentsForDescriptor(
        method: MethodVisitor,
        receiver: ChirValue,
        arguments: List<ChirValue>,
        receiverType: Type,
        descriptor: String,
        nodeId: ChirSemanticId,
    ) {
        emitValue(method, receiver)
        adaptStackValue(
            method = method,
            nodeId = receiver.semanticId,
            actualType = context.typeMapper.mapValueType(receiver.type),
            targetType = receiverType,
            location = "call receiver",
        )
        emitArgumentsForDescriptor(method, arguments, descriptor, nodeId)
    }

    /**
     * 按 JVM method descriptor 发射调用实参。
     *
     * 参数数量必须与 descriptor 完全一致，每个实参都会按目标 JVM 类型适配。
     */
    private fun emitArgumentsForDescriptor(
        method: MethodVisitor,
        arguments: List<ChirValue>,
        descriptor: String,
        nodeId: ChirSemanticId,
    ) {
        val targetTypes = Type.getArgumentTypes(descriptor)
        if (targetTypes.size != arguments.size) {
            throw JvmCodegenException(
                "JVM call descriptor argument count ${targetTypes.size} does not match CHIR arguments ${arguments.size}",
                nodeId,
            )
        }
        arguments.zip(targetTypes).forEach { (argument, targetType) ->
            emitValue(method, argument)
            adaptStackValue(
                method = method,
                nodeId = argument.semanticId,
                actualType = context.typeMapper.mapValueType(argument.type),
                targetType = targetType,
                location = "call argument",
            )
        }
    }

    /**
     * 将栈顶值适配到调用结果的目标 JVM 类型。
     */
    private fun adaptStackValue(method: MethodVisitor, nodeId: ChirSemanticId, actualType: Type, targetType: Type) {
        adaptStackValue(method, nodeId, actualType, targetType, "call result")
    }

    /**
     * 将 JVM 操作数栈顶的实际类型适配为目标类型。
     *
     * 支持原始类型转换、装箱、拆箱、引用 checkcast；不兼容组合会报告包含位置的后端异常。
     */
    private fun adaptStackValue(
        method: MethodVisitor,
        nodeId: ChirSemanticId,
        actualType: Type,
        targetType: Type,
        location: String,
    ) {
        if (actualType == targetType) return
        if (actualType == Type.VOID_TYPE) {
            throw JvmCodegenException("JVM $location is void but target expects ${targetType.descriptor}", nodeId)
        }
        val actualBoxing = primitiveBoxing(actualType)
        val targetBoxing = primitiveBoxing(targetType)
        if (actualBoxing != null && targetType.isReferenceCarrier()) {
            method.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                actualBoxing.wrapperInternalName,
                "valueOf",
                "(${actualType.descriptor})L${actualBoxing.wrapperInternalName};",
                false,
            )
            if (targetType != Type.getObjectType(actualBoxing.wrapperInternalName)) {
                method.visitTypeInsn(Opcodes.CHECKCAST, checkcastTypeOperand(targetType))
            }
            return
        }
        if (actualType.isReferenceCarrier() && targetBoxing != null) {
            method.visitTypeInsn(Opcodes.CHECKCAST, targetBoxing.wrapperInternalName)
            method.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                targetBoxing.wrapperInternalName,
                targetBoxing.unboxMethodName,
                "()${targetType.descriptor}",
                false,
            )
            return
        }
        if (actualBoxing != null && targetBoxing != null) {
            emitPrimitiveCast(method, nodeId, actualType, targetType)
            return
        }
        if (actualType.isReferenceCarrier() && targetType.isReferenceCarrier()) {
            method.visitTypeInsn(Opcodes.CHECKCAST, checkcastTypeOperand(targetType))
            return
        }
        throw JvmCodegenException(
            "JVM $location type ${actualType.descriptor} does not match target ${targetType.descriptor}",
            nodeId,
        )
    }

    /**
     * 丢弃栈顶值，按 JVM slot size 选择 `pop` 或 `pop2`。
     */
    private fun popStackValue(method: MethodVisitor, actualType: Type) {
        when {
            actualType == Type.VOID_TYPE -> Unit
            actualType.size == 2 -> method.visitInsn(Opcodes.POP2)
            else -> method.visitInsn(Opcodes.POP)
        }
    }

    /**
     * 获取 JVM 原始类型对应的 wrapper 与拆箱方法信息。
     */
    private fun primitiveBoxing(type: Type): PrimitiveBoxing? {
        return when (type) {
            Type.BOOLEAN_TYPE -> PrimitiveBoxing("java/lang/Boolean", "booleanValue")
            Type.BYTE_TYPE -> PrimitiveBoxing("java/lang/Byte", "byteValue")
            Type.SHORT_TYPE -> PrimitiveBoxing("java/lang/Short", "shortValue")
            Type.INT_TYPE -> PrimitiveBoxing("java/lang/Integer", "intValue")
            Type.LONG_TYPE -> PrimitiveBoxing("java/lang/Long", "longValue")
            Type.FLOAT_TYPE -> PrimitiveBoxing("java/lang/Float", "floatValue")
            Type.DOUBLE_TYPE -> PrimitiveBoxing("java/lang/Double", "doubleValue")
            else -> null
        }
    }

    /**
     * 发射数组值与索引值，并返回数组 JVM 类型。
     */
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
        emitArrayIndexValue(method, index, nodeId)
        return arrayType
    }

    /**
     * 发射数组长度或下标值，并强制适配为 JVM int。
     */
    private fun emitArrayIndexValue(method: MethodVisitor, value: ChirValue, nodeId: ChirSemanticId) {
        val valueType = context.typeMapper.mapValueType(value.type)
        if (valueType !in intArraySizeTypes) {
            throw JvmCodegenException("JVM array index/size must be int-like, actual ${valueType.descriptor}", nodeId)
        }
        emitValue(method, value)
        emitPrimitiveCast(method, nodeId, valueType, Type.INT_TYPE)
    }

    /**
     * 从 JVM 数组类型中解析元素类型。
     */
    private fun arrayElementType(arrayType: Type, nodeId: ChirSemanticId): Type {
        if (arrayType.sort != Type.ARRAY) {
            throw JvmCodegenException("JVM value is not an array type: ${arrayType.descriptor}", nodeId)
        }
        return Type.getType(arrayType.descriptor.substring(1))
    }

    /**
     * 要求 JVM 类型是对象类型，并返回 internal name。
     */
    private fun objectInternalName(type: Type, nodeId: ChirSemanticId, location: String): String {
        if (type.sort != Type.OBJECT) {
            throw JvmCodegenException("JVM $location must be object type, actual ${type.descriptor}", nodeId)
        }
        return type.internalName
    }

    /**
     * 要求 JVM 类型是对象或数组类型，并返回可用于 `checkcast`/type 指令的名称。
     */
    private fun objectOrArrayInternalName(type: Type, nodeId: ChirSemanticId, location: String): String {
        return when (type.sort) {
            Type.OBJECT -> type.internalName
            Type.ARRAY -> type.descriptor
            else -> throw JvmCodegenException("JVM $location must be object or array type, actual ${type.descriptor}", nodeId)
        }
    }

    /**
     * 判断 ASM 类型是否为 JVM 引用 carrier。
     */
    private fun Type.isReferenceCarrier(): Boolean = sort == Type.OBJECT || sort == Type.ARRAY

    /**
     * 计算 CHECKCAST 指令接受的类型操作数。
     */
    private fun checkcastTypeOperand(type: Type): String {
        return when (type.sort) {
            Type.OBJECT -> type.internalName
            Type.ARRAY -> type.descriptor
            else -> throw JvmCodegenException("JVM checkcast target must be reference carrier, actual ${type.descriptor}", function.semanticId)
        }
    }

    /**
     * 返回当前函数的基本块生成顺序。
     *
     * 入口块优先，其余块保持 CHIR 原始顺序，保证 label 已分配且控制流边可跳转。
     */
    private fun orderedBlocks(): List<ChirBlock> = buildList {
        val entry = function.blocks.firstOrNull { it.semanticId == function.entryBlockId }
        if (entry != null) add(entry)
        function.blocks.forEach { block ->
            if (entry == null || block.semanticId != entry.semanticId) add(block)
        }
    }

    /**
     * 取得基本块对应的 ASM label。
     */
    private fun labelFor(id: ChirSemanticId): Label {
        return blockLabels[id] ?: throw JvmCodegenException("missing JVM block label", id)
    }

    /**
     * 在当前模块中按名称和 JVM descriptor 解析本地函数声明。
     */
    private fun resolveLocalFunction(name: String, descriptor: String, nodeId: ChirSemanticId): ChirFunctionDeclaration {
        return module.declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .firstOrNull { candidate ->
                candidate.name == name &&
                    jvmMethodDescriptor(candidate) == descriptor
            }
            ?: throw JvmCodegenException("unresolved local JVM function '$name$descriptor'", nodeId)
    }

    /**
     * 计算函数的 JVM 方法名。
     */
    private fun jvmMethodName(function: ChirFunctionDeclaration): String {
        return JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.functionJvmName(function)
    }

    /**
     * 计算函数的 JVM method descriptor。
     *
     * 当前函数为构造器时返回类型固定为 void；其他函数按 CHIR 返回类型映射。
     */
    private fun jvmMethodDescriptor(function: ChirFunctionDeclaration): String {
        return JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: Type.getMethodDescriptor(
                if (isConstructor && function === this.function) Type.VOID_TYPE else context.typeMapper.mapReturnType(function.returnType),
                *function.parameters.map { context.typeMapper.mapValueType(it.type) }.toTypedArray(),
            )
    }

    /**
     * 校验当前函数是否满足 JVM method 生成的结构约束。
     */
    private fun verifyFunctionShape() {
        if (isConstructor) {
            if (methodName != "<init>") {
                throw JvmCodegenException("JVM constructor must use '<init>' method name", function.semanticId)
            }
            if (isStaticMethod || methodAccess and Opcodes.ACC_STATIC != 0) {
                throw JvmCodegenException("JVM constructor must be an instance method", function.semanticId)
            }
            if (declaredReturnType != Type.VOID_TYPE) {
                throw JvmCodegenException("JVM constructor '${function.name}' must declare Unit/Void return type", function.semanticId)
            }
        }
        if (function.blocks.isEmpty()) {
            throw JvmCodegenException("JVM function '${function.name}' must contain at least one block", function.semanticId)
        }
        if (function.blocks.none { it.semanticId == function.entryBlockId }) {
            throw JvmCodegenException("JVM function '${function.name}' entry block is missing", function.entryBlockId)
        }
    }

    /**
     * 在构造器开头发射对父类无参构造器的调用。
     */
    private fun emitSuperConstructorCall(method: MethodVisitor) {
        val targetSuperInternalName = superInternalName
            ?: throw JvmCodegenException("JVM constructor requires an owner super class", function.semanticId)
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, targetSuperInternalName, "<init>", "()V", false)
    }

    /**
     * 校验类型是否支持 JVM 算术操作。
     */
    private fun requireArithmeticType(type: Type, id: ChirSemanticId) {
        if (type !in arithmeticTypes) {
            throw JvmCodegenException("JVM arithmetic does not support type ${type.descriptor}", id)
        }
    }

    /**
     * 校验类型是否支持 JVM 整数类操作。
     */
    private fun requireIntLikeType(type: Type, id: ChirSemanticId) {
        if (type !in intLikeTypes) {
            throw JvmCodegenException("JVM integer operation does not support type ${type.descriptor}", id)
        }
    }

    /**
     * 校验类型是否为 JVM boolean carrier。
     */
    private fun requireBoolean(type: Type, id: ChirSemanticId) {
        if (type != Type.BOOLEAN_TYPE) {
            throw JvmCodegenException("JVM boolean operation requires boolean type, actual ${type.descriptor}", id)
        }
    }

    /**
     * 将 CHIR semanticId 与规范化名称绑定到同一个 JVM local slot。
     */
    private fun bindLocal(id: ChirSemanticId, name: String, slot: LocalSlot) {
        localSlots[id] = slot
        localSlotsByName[normalizeLocalName(name)] = slot
    }

    /**
     * 将表达式 semanticId 转为结果 local 名称。
     */
    private fun resultLocalName(id: ChirSemanticId): String = normalizeLocalName(id.value)

    /**
     * 将 CHIR 名称规范化为可作为 JVM local 查找键的稳定字符串。
     */
    private fun normalizeLocalName(raw: String): String {
        val sanitized = raw.replace(Regex("[^A-Za-z0-9_.$]"), "_").trim('_')
        if (sanitized.isBlank()) return "local"
        return if (sanitized.first().isDigit()) "local_$sanitized" else sanitized
    }

    /**
     * JVM local slot 绑定信息。
     */
    private data class LocalSlot(val index: Int, val type: Type)

    /**
     * ByteBuffer 指针访问方法描述。
     */
    private data class PointerAccessor(
        /**
         * 读取元素值的 ByteBuffer 方法名。
         */
        val getMethodName: String,
        /**
         * 读取元素值的 JVM descriptor。
         */
        val getDescriptor: String,
        /**
         * 写入元素值的 ByteBuffer 方法名。
         */
        val putMethodName: String,
        /**
         * 写入元素值的 JVM descriptor。
         */
        val putDescriptor: String,
    )

    /**
     * JVM 原始类型装箱/拆箱所需的 wrapper 信息。
     */
    private data class PrimitiveBoxing(
        /**
         * wrapper class 的 JVM internal name。
         */
        val wrapperInternalName: String,
        /**
         * wrapper 实例拆箱为原始类型的方法名。
         */
        val unboxMethodName: String,
    )

    /**
     * 规范化后的整数字面量结构。
     */
    private data class ParsedIntegerLiteral(
        /**
         * 字面量是否带负号。
         */
        val negative: Boolean,
        /**
         * 去除符号、进制前缀和下划线后的数字主体。
         */
        val digits: String,
        /**
         * 数字主体使用的进制。
         */
        val radix: Int,
    )

    /**
     * JVM 函数字节码生成共享常量。
     */
    private companion object {
        /**
         * `java.lang.String` 的 ASM 类型常量。
         */
        val stringObjectType: Type = Type.getObjectType("java/lang/String")
        /**
         * `java.nio.ByteBuffer` 的 ASM 类型常量，用作 CHIR cpointer carrier。
         */
        val byteBufferType: Type = Type.getObjectType("java/nio/ByteBuffer")
        /**
         * 支持普通算术操作的 JVM 类型集合。
         */
        val arithmeticTypes = setOf(Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE, Type.DOUBLE_TYPE)
        /**
         * 支持整数类位运算、移位和无符号操作的 JVM 类型集合。
         */
        val intLikeTypes = setOf(Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE)
        /**
         * 可作为 JVM 数组大小或下标输入的类型集合。
         */
        val intArraySizeTypes = setOf(Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE)
        /**
         * 需要通过 wrapper `TYPE` 字段获取 class literal 的 primitive sort 集合。
         */
        val primitiveClassLiteralSorts = setOf(
            Type.BOOLEAN,
            Type.BYTE,
            Type.SHORT,
            Type.INT,
            Type.LONG,
            Type.FLOAT,
            Type.DOUBLE,
        )
    }
}
