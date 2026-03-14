package org.cangnova.cangjie.llvm.api

data class LlvmSwitchCase(
    val value: LlvmValueRef,
    val target: LlvmBasicBlockRef,
)

class LlvmBuilder internal constructor(
    val ref: LlvmBuilderRef,
    private val owner: LlvmContext,
    private val bindings: LlvmBindings,
) : AutoCloseable {
    private var closed = false

    fun positionAtEnd(block: LlvmBasicBlockRef) {
        ensureOpen()
        bindings.builderPositionAtEnd(ref, block)
    }

    fun buildRet(value: LlvmValueRef): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildRet(ref, value)
    }

    fun buildRetVoid(): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildRetVoid(ref)
    }

    fun buildBr(dest: LlvmBasicBlockRef): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildBr(ref, dest)
    }

    fun buildCondBr(
        cond: LlvmValueRef,
        thenBlock: LlvmBasicBlockRef,
        elseBlock: LlvmBasicBlockRef,
    ): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildCondBr(ref, cond, thenBlock, elseBlock)
    }

    fun buildSwitch(
        value: LlvmValueRef,
        defaultBlock: LlvmBasicBlockRef,
        cases: List<LlvmSwitchCase>,
    ): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildSwitch(ref, value, defaultBlock, cases.map { it.value to it.target })
    }

    fun buildUnreachable(): LlvmValueRef {
        ensureOpen()
        return bindings.builderBuildUnreachable(ref)
    }

    fun buildAdd(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildAdd(ref, lhs, rhs, name)
    }

    fun buildSub(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildSub(ref, lhs, rhs, name)
    }

    fun buildMul(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildMul(ref, lhs, rhs, name)
    }

    fun buildSDiv(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildSDiv(ref, lhs, rhs, name)
    }

    fun buildUDiv(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildUDiv(ref, lhs, rhs, name)
    }

    fun buildSRem(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildSRem(ref, lhs, rhs, name)
    }

    fun buildURem(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildURem(ref, lhs, rhs, name)
    }

    fun buildFNeg(value: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFNeg(ref, value, name)
    }

    fun buildFAdd(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFAdd(ref, lhs, rhs, name)
    }

    fun buildFSub(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFSub(ref, lhs, rhs, name)
    }

    fun buildFMul(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFMul(ref, lhs, rhs, name)
    }

    fun buildFDiv(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFDiv(ref, lhs, rhs, name)
    }

    fun buildAnd(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildAnd(ref, lhs, rhs, name)
    }

    fun buildOr(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildOr(ref, lhs, rhs, name)
    }

    fun buildXor(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildXor(ref, lhs, rhs, name)
    }

    fun buildShl(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildShl(ref, lhs, rhs, name)
    }

    fun buildAShr(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildAShr(ref, lhs, rhs, name)
    }

    fun buildLShr(lhs: LlvmValueRef, rhs: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildLShr(ref, lhs, rhs, name)
    }

    fun buildICmp(
        predicate: LlvmIntPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String = "",
    ): LlvmValueRef = withOpen {
        bindings.builderBuildICmp(ref, predicate, lhs, rhs, name)
    }

    fun buildFCmp(
        predicate: LlvmFloatPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String = "",
    ): LlvmValueRef = withOpen {
        bindings.builderBuildFCmp(ref, predicate, lhs, rhs, name)
    }

    fun buildAlloca(type: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildAlloca(ref, type, name)
    }

    fun buildLoad(type: LlvmTypeRef, pointer: LlvmValueRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildLoad(ref, type, pointer, name)
    }

    fun buildStore(value: LlvmValueRef, pointer: LlvmValueRef): LlvmValueRef = withOpen {
        bindings.builderBuildStore(ref, value, pointer)
    }

    fun buildGep(
        elementType: LlvmTypeRef,
        pointer: LlvmValueRef,
        indices: List<LlvmValueRef>,
        inBounds: Boolean = true,
        name: String = "",
    ): LlvmValueRef = withOpen {
        bindings.builderBuildGep(ref, elementType, pointer, indices, inBounds, name)
    }

    fun buildTrunc(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildTrunc(ref, value, targetType, name)
    }

    fun buildZExt(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildZExt(ref, value, targetType, name)
    }

    fun buildSExt(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildSExt(ref, value, targetType, name)
    }

    fun buildFPTrunc(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFPTrunc(ref, value, targetType, name)
    }

    fun buildFPExt(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFPExt(ref, value, targetType, name)
    }

    fun buildFPToUI(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFPToUI(ref, value, targetType, name)
    }

    fun buildFPToSI(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildFPToSI(ref, value, targetType, name)
    }

    fun buildUIToFP(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildUIToFP(ref, value, targetType, name)
    }

    fun buildSIToFP(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildSIToFP(ref, value, targetType, name)
    }

    fun buildPtrToInt(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildPtrToInt(ref, value, targetType, name)
    }

    fun buildIntToPtr(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildIntToPtr(ref, value, targetType, name)
    }

    fun buildBitCast(value: LlvmValueRef, targetType: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildBitCast(ref, value, targetType, name)
    }

    fun buildCall(function: LlvmValueRef, args: List<LlvmValueRef>, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildCall(ref, function, args, name)
    }

    fun buildPhi(type: LlvmTypeRef, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildPhi(ref, type, name)
    }

    fun addPhiIncoming(phi: LlvmValueRef, incoming: List<LlvmPhiIncoming>) {
        ensureOpen()
        bindings.builderAddIncoming(phi, incoming)
    }

    fun buildSelect(
        condition: LlvmValueRef,
        thenValue: LlvmValueRef,
        elseValue: LlvmValueRef,
        name: String = "",
    ): LlvmValueRef = withOpen {
        bindings.builderBuildSelect(ref, condition, thenValue, elseValue, name)
    }

    fun buildExtractValue(aggregate: LlvmValueRef, index: Int, name: String = ""): LlvmValueRef = withOpen {
        bindings.builderBuildExtractValue(ref, aggregate, index, name)
    }

    fun buildInsertValue(
        aggregate: LlvmValueRef,
        element: LlvmValueRef,
        index: Int,
        name: String = "",
    ): LlvmValueRef = withOpen {
        bindings.builderBuildInsertValue(ref, aggregate, element, index, name)
    }

    override fun close() {
        if (closed) return
        closed = true
        bindings.builderDispose(ref)
    }

    private inline fun withOpen(block: () -> LlvmValueRef): LlvmValueRef {
        ensureOpen()
        return block()
    }

    private fun ensureOpen() {
        owner.ensureOpen()
        check(!closed) { "LLVM builder is already closed" }
    }
}
