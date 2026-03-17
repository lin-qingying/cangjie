package org.cangnova.cangjie.llvm.api

/**
 * LLVM 上下文封装。
 *
 * 管理类型构造、模块/构建器创建及资源生命周期。
 */
class LlvmContext internal constructor(
    private val bindings: LlvmBindings = LlvmBindingRegistry.bindings,
) : AutoCloseable {
    val ref: LlvmContextRef = bindings.contextCreate()

    private val ownedModules = mutableListOf<LlvmModule>()
    private val ownedBuilders = mutableListOf<LlvmBuilder>()
    private var closed = false

    val int1Type: LlvmTypeRef get() = intType(1)
    val int8Type: LlvmTypeRef get() = intType(8)
    val int16Type: LlvmTypeRef get() = intType(16)
    val int32Type: LlvmTypeRef get() = intType(32)
    val int64Type: LlvmTypeRef get() = intType(64)
    val floatType: LlvmTypeRef get() = bindings.contextFloatType(ref)
    val doubleType: LlvmTypeRef get() = bindings.contextDoubleType(ref)
    val voidType: LlvmTypeRef get() = bindings.contextVoidType(ref)
    val ptrType: LlvmTypeRef get() = bindings.contextPtrType(ref)

    fun intType(bits: Int): LlvmTypeRef {
        ensureOpen()
        return bindings.contextIntType(ref, bits)
    }

    fun functionType(
        returnType: LlvmTypeRef,
        parameterTypes: List<LlvmTypeRef>,
        isVarArg: Boolean = false,
    ): LlvmTypeRef {
        ensureOpen()
        return bindings.contextFunctionType(returnType, parameterTypes, isVarArg)
    }

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

    fun arrayType(elementType: LlvmTypeRef, size: Int): LlvmTypeRef {
        ensureOpen()
        return bindings.contextArrayType(elementType, size)
    }

    fun createModule(name: String): LlvmModule {
        ensureOpen()
        val moduleRef = bindings.moduleCreateInContext(name, ref)
        return LlvmModule(moduleRef, this, bindings).also { ownedModules += it }
    }

    fun createBuilder(): LlvmBuilder {
        ensureOpen()
        val builderRef = bindings.builderCreateInContext(ref)
        return LlvmBuilder(builderRef, this, bindings).also { ownedBuilders += it }
    }

    override fun close() {
        if (closed) return
        closed = true
        ownedBuilders.toList().forEach { it.close() }
        ownedModules.toList().forEach { it.close() }
        bindings.contextDispose(ref)
    }

    internal fun ensureOpen() {
        check(!closed) { "LLVM context is already closed" }
    }
}
