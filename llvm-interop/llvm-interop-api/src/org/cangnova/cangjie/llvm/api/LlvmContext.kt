package org.cangnova.cangjie.llvm.api

/**
 * LLVM 上下文封装。
 *
 * 管理类型构造、模块/构建器创建及资源生命周期。
 */
class LlvmContext private constructor(
    /**
     * 当前上下文使用的 LLVM 绑定实现。
     */
    private val bindings: LlvmBindings = LlvmBindingRegistry.bindings,
) : AutoCloseable {
    constructor() : this(LlvmBindingRegistry.bindings)

    /**
     * LLVMContext 原生句柄。
     */
    val ref: LlvmContextRef = bindings.contextCreate()

    /**
     * 由当前上下文创建并需要随上下文关闭的模块列表。
     */
    private val ownedModules = mutableListOf<LlvmModule>()
    /**
     * 由当前上下文创建并需要随上下文关闭的 IR builder 列表。
     */
    private val ownedBuilders = mutableListOf<LlvmBuilder>()
    /**
     * 当前上下文是否已经关闭。
     */
    private var closed = false

    /**
     * 1 位整数类型。
     */
    val int1Type: LlvmTypeRef get() = intType(1)
    /**
     * 8 位整数类型。
     */
    val int8Type: LlvmTypeRef get() = intType(8)
    /**
     * 16 位整数类型。
     */
    val int16Type: LlvmTypeRef get() = intType(16)
    /**
     * 32 位整数类型。
     */
    val int32Type: LlvmTypeRef get() = intType(32)
    /**
     * 64 位整数类型。
     */
    val int64Type: LlvmTypeRef get() = intType(64)
    /**
     * LLVM float 类型。
     */
    val floatType: LlvmTypeRef get() = bindings.contextFloatType(ref)
    /**
     * LLVM double 类型。
     */
    val doubleType: LlvmTypeRef get() = bindings.contextDoubleType(ref)
    /**
     * LLVM void 类型。
     */
    val voidType: LlvmTypeRef get() = bindings.contextVoidType(ref)
    /**
     * LLVM opaque pointer 类型。
     */
    val ptrType: LlvmTypeRef get() = bindings.contextPtrType(ref)

    /**
     * 创建指定 bit 宽度的整数类型。
     */
    fun intType(bits: Int): LlvmTypeRef {
        ensureOpen()
        return bindings.contextIntType(ref, bits)
    }

    /**
     * 创建函数类型。
     */
    fun functionType(
        returnType: LlvmTypeRef,
        parameterTypes: List<LlvmTypeRef>,
        isVarArg: Boolean = false,
    ): LlvmTypeRef {
        ensureOpen()
        return bindings.contextFunctionType(returnType, parameterTypes, isVarArg)
    }

    /**
     * 创建具名结构体类型，并可选设置结构体 body。
     */
    fun namedStructType(
        name: String,
        elementTypes: List<LlvmTypeRef> = emptyList(),
        isPacked: Boolean = false,
    ): LlvmTypeRef {
        ensureOpen()
        val type = bindings.contextNamedStructType(ref, name)
        if (elementTypes.isNotEmpty()) {
            bindings.structSetBody(type, elementTypes, isPacked)
        }
        return type
    }

    /**
     * 创建数组类型。
     */
    fun arrayType(elementType: LlvmTypeRef, size: Int): LlvmTypeRef {
        ensureOpen()
        return bindings.contextArrayType(elementType, size)
    }

    /**
     * 创建整数常量。
     */
    fun constInt(type: LlvmTypeRef, value: Long, signExtend: Boolean = false): LlvmValueRef {
        ensureOpen()
        return bindings.constInt(type, value, signExtend)
    }

    /**
     * 创建浮点常量。
     */
    fun constReal(type: LlvmTypeRef, value: Double): LlvmValueRef {
        ensureOpen()
        return bindings.constReal(type, value)
    }

    /**
     * 创建指定类型的 null 常量。
     */
    fun constNull(type: LlvmTypeRef): LlvmValueRef {
        ensureOpen()
        return bindings.constNull(type)
    }

    /**
     * 在当前上下文中创建新模块，并纳入上下文生命周期管理。
     */
    fun createModule(name: String): LlvmModule {
        ensureOpen()
        val moduleRef = bindings.moduleCreateInContext(name, ref)
        return LlvmModule(moduleRef, this, bindings).also { ownedModules += it }
    }

    /**
     * 在当前上下文中解析 LLVM assembly 文本为模块。
     */
    fun parseModule(name: String, assembly: String): LlvmModule {
        ensureOpen()
        val moduleRef = bindings.moduleParseAssemblyInContext(name, assembly, ref)
        return LlvmModule(moduleRef, this, bindings).also { ownedModules += it }
    }

    /**
     * 在当前上下文中创建 IR builder，并纳入上下文生命周期管理。
     */
    fun createBuilder(): LlvmBuilder {
        ensureOpen()
        val builderRef = bindings.builderCreateInContext(ref)
        return LlvmBuilder(builderRef, this, bindings).also { ownedBuilders += it }
    }

    /**
     * 关闭 builder、模块和上下文本身。
     */
    override fun close() {
        if (closed) return
        closed = true
        ownedBuilders.toList().forEach { it.close() }
        ownedModules.toList().forEach { it.close() }
        bindings.contextDispose(ref)
    }

    /**
     * 确认当前上下文尚未关闭。
     */
    internal fun ensureOpen() {
        check(!closed) { "LLVM context is already closed" }
    }
}
