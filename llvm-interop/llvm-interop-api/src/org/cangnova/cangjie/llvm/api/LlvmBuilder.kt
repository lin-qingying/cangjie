package org.cangnova.cangjie.llvm.api

/**
 * `switch` 指令分支项。
 */
data class LlvmSwitchCase(
    /**
     * switch case 匹配的常量值。
     */
    val value: LlvmValueRef,
    /**
     * 匹配成功后跳转到的基本块。
     */
    val target: LlvmBasicBlockRef,
)

/**
 * LLVM IR 构建器封装。
 */
class LlvmBuilder internal constructor(
    /**
     * LLVMBuilder 原生句柄。
     */
    val ref: LlvmBuilderRef,
    /**
     * 创建并拥有该 builder 生命周期的上下文。
     */
    private val owner: LlvmContext,
    /**
     * 实际执行 builder 操作的 LLVM 绑定实现。
     */
    private val bindings: LlvmBindings,
) : AutoCloseable {
    /**
     * 当前 builder 是否已经关闭。
     */
    private var closed = false

    /**
     * 将 builder 定位到指定基本块末尾。
     */
    fun positionAtEnd(block: LlvmBasicBlockRef) {
        ensureOpen()
        bindings.builderPositionAtEnd(ref, block)
    }

    /**
     * 构造返回指定值的 `ret` 指令。
     */
    fun buildRet(value: LlvmValueRef): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildRet(ref, value)
    }

    /**
     * 构造 `ret void` 指令。
     */
    fun buildRetVoid(): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildRetVoid(ref)
    }

    /**
     * 构造无条件分支指令。
     */
    fun buildBr(dest: LlvmBasicBlockRef): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildBr(ref, dest)
    }

    /**
     * 构造条件分支指令。
     */
    fun buildCondBr(
        cond: LlvmValueRef,
        thenBlock: LlvmBasicBlockRef,
        elseBlock: LlvmBasicBlockRef,
    ): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildCondBr(ref, cond, thenBlock, elseBlock)
    }

    /**
     * 构造 switch 分支指令。
     */
    fun buildSwitch(
        value: LlvmValueRef,
        defaultBlock: LlvmBasicBlockRef,
        cases: List<LlvmSwitchCase>,
    ): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildSwitch(ref, value, defaultBlock, cases.map { it.value to it.target })
    }

    /**
     * 构造 unreachable 指令。
     */
    fun buildUnreachable(): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildUnreachable(ref)
    }

    /**
     * 构造整数加法指令。
     */
    fun buildAdd(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildAdd(ref, lhs, rhs, name)
    }

    /**
     * 构造整数减法指令。
     */
    fun buildSub(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildSub(ref, lhs, rhs, name)
    }

    /**
     * 构造整数乘法指令。
     */
    fun buildMul(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildMul(ref, lhs, rhs, name)
    }

    /**
     * 构造有符号整数除法指令。
     */
    fun buildSDiv(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildSDiv(ref, lhs, rhs, name)
    }

    /**
     * 构造无符号整数除法指令。
     */
    fun buildUDiv(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildUDiv(ref, lhs, rhs, name)
    }

    /**
     * 构造有符号整数取余指令。
     */
    fun buildSRem(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildSRem(ref, lhs, rhs, name)
    }

    /**
     * 构造无符号整数取余指令。
     */
    fun buildURem(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildURem(ref, lhs, rhs, name)
    }

    /**
     * 构造浮点取负指令。
     */
    fun buildFNeg(value: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFNeg(ref, value, name)
    }

    /**
     * 构造浮点加法指令。
     */
    fun buildFAdd(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFAdd(ref, lhs, rhs, name)
    }

    /**
     * 构造浮点减法指令。
     */
    fun buildFSub(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFSub(ref, lhs, rhs, name)
    }

    /**
     * 构造浮点乘法指令。
     */
    fun buildFMul(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFMul(ref, lhs, rhs, name)
    }

    /**
     * 构造浮点除法指令。
     */
    fun buildFDiv(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFDiv(ref, lhs, rhs, name)
    }

    /**
     * 构造按位与指令。
     */
    fun buildAnd(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildAnd(ref, lhs, rhs, name)
    }

    /**
     * 构造按位或指令。
     */
    fun buildOr(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildOr(ref, lhs, rhs, name)
    }

    /**
     * 构造按位异或指令。
     */
    fun buildXor(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildXor(ref, lhs, rhs, name)
    }

    /**
     * 构造左移指令。
     */
    fun buildShl(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildShl(ref, lhs, rhs, name)
    }

    /**
     * 构造算术右移指令。
     */
    fun buildAShr(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildAShr(ref, lhs, rhs, name)
    }

    /**
     * 构造逻辑右移指令。
     */
    fun buildLShr(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildLShr(ref, lhs, rhs, name)
    }

    /**
     * 构造整数比较指令。
     */
    fun buildICmp(
        predicate: LlvmIntPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String = "",
    ): LlvmValueRef = withOpen {
        bindings.builderBuildICmp(ref, predicate, lhs, rhs, name)
    }

    /**
     * 构造浮点比较指令。
     */
    fun buildFCmp(
        predicate: LlvmFloatPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String = "",
    ): LlvmValueRef = withOpen {
        bindings.builderBuildFCmp(ref, predicate, lhs, rhs, name)
    }

    /**
     * 构造栈分配指令。
     */
    fun buildAlloca(type: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildAlloca(ref, type, name)
    }

    /**
     * 构造 load 指令。
     */
    fun buildLoad(type: LlvmTypeRef, pointer: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildLoad(ref, type, pointer, name)
    }

    /**
     * 构造 store 指令。
     */
    fun buildStore(value: LlvmValueRef, pointer: LlvmValueRef): LlvmValueRef = withOpen {
        bindings.builderBuildStore(ref, value, pointer)
    }

    /**
     * 构造 getelementptr 指令。
     */
    fun buildGep(
        elementType: LlvmTypeRef,
        pointer: LlvmValueRef,
        indices: List<LlvmValueRef>,
        inBounds: Boolean = true,
        name: String = "",
    ): LlvmValueRef = withOpen {
        bindings.builderBuildGep(ref, elementType, pointer, indices, inBounds, name)
    }

    /**
     * 构造整数截断指令。
     */
    fun buildTrunc(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildTrunc(ref, value, targetType, name)
    }

    /**
     * 构造零扩展指令。
     */
    fun buildZExt(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildZExt(ref, value, targetType, name)
    }

    /**
     * 构造符号扩展指令。
     */
    fun buildSExt(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildSExt(ref, value, targetType, name)
    }

    /**
     * 构造浮点截断指令。
     */
    fun buildFPTrunc(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFPTrunc(ref, value, targetType, name)
    }

    /**
     * 构造浮点扩展指令。
     */
    fun buildFPExt(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFPExt(ref, value, targetType, name)
    }

    /**
     * 构造浮点到无符号整数转换指令。
     */
    fun buildFPToUI(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFPToUI(ref, value, targetType, name)
    }

    /**
     * 构造浮点到有符号整数转换指令。
     */
    fun buildFPToSI(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFPToSI(ref, value, targetType, name)
    }

    /**
     * 构造无符号整数到浮点转换指令。
     */
    fun buildUIToFP(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildUIToFP(ref, value, targetType, name)
    }

    /**
     * 构造有符号整数到浮点转换指令。
     */
    fun buildSIToFP(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildSIToFP(ref, value, targetType, name)
    }

    /**
     * 构造指针到整数转换指令。
     */
    fun buildPtrToInt(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildPtrToInt(ref, value, targetType, name)
    }

    /**
     * 构造整数到指针转换指令。
     */
    fun buildIntToPtr(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildIntToPtr(ref, value, targetType, name)
    }

    /**
     * 构造 bitcast 指令。
     */
    fun buildBitCast(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildBitCast(ref, value, targetType, name)
    }

    /**
     * 构造函数调用指令。
     */
    fun buildCall(function: LlvmValueRef, args: List<LlvmValueRef>, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildCall(ref, function, args, name)
    }

    /**
     * 构造 PHI 指令。
     */
    fun buildPhi(type: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildPhi(ref, type, name)
    }

    /**
     * 为 PHI 指令追加 incoming 分支。
     */
    fun addPhiIncoming(phi: LlvmValueRef, incoming: List<LlvmPhiIncoming>) {
        ensureOpen()
        bindings.builderAddIncoming(phi, incoming)
    }

    /**
     * 构造 select 指令。
     */
    fun buildSelect(
        condition: LlvmValueRef,
        thenValue: LlvmValueRef,
        elseValue: LlvmValueRef,
        name: String = "",
    ): LlvmValueRef = withOpen {
        bindings.builderBuildSelect(ref, condition, thenValue, elseValue, name)
    }

    /**
     * 构造 extractvalue 指令。
     */
    fun buildExtractValue(aggregate: LlvmValueRef, index: Int, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildExtractValue(ref, aggregate, index, name)
    }

    /**
     * 构造 insertvalue 指令。
     */
    fun buildInsertValue(
        aggregate: LlvmValueRef,
        element: LlvmValueRef,
        index: Int,
        name: String = "",
    ): LlvmValueRef = withOpen {
        bindings.builderBuildInsertValue(ref, aggregate, element, index, name)
    }

    /**
     * 释放 builder 原生资源。
     */
    override fun close() {
        if (closed) return
        closed = true
        bindings.builderDispose(ref)
    }

    /**
     * 校验 builder 可用后执行返回 LLVM value 的构建逻辑。
     */
    private inline fun withOpen(block: () -> LlvmValueRef): LlvmValueRef {
        ensureOpen()
        return block()
    }

    /**
     * 确认 owner 上下文和当前 builder 均未关闭。
     */
    private fun ensureOpen() {
        owner.ensureOpen()
        check(!closed) { "LLVM builder is already closed" }
    }
}
