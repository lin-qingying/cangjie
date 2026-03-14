package org.cangnova.cangjie.llvm.api

enum class LlvmIntPredicate {
    EQ,
    NE,
    UGT,
    UGE,
    ULT,
    ULE,
    SGT,
    SGE,
    SLT,
    SLE,
}

enum class LlvmFloatPredicate {
    FALSE,
    OEQ,
    OGT,
    OGE,
    OLT,
    OLE,
    ONE,
    ORD,
    UNO,
    UEQ,
    UGT,
    UGE,
    ULT,
    ULE,
    UNE,
    TRUE,
}

data class LlvmPhiIncoming(
    val value: LlvmValueRef,
    val block: LlvmBasicBlockRef,
)

data class LlvmVerificationResult(
    val ok: Boolean,
    val message: String? = null,
)

internal interface LlvmBindings {
    fun contextCreate(): LlvmContextRef = unsupported()
    fun contextDispose(context: LlvmContextRef) = unsupportedUnit()

    fun moduleCreateInContext(name: String, context: LlvmContextRef): LlvmModuleRef = unsupported()
    fun moduleDispose(module: LlvmModuleRef) = unsupportedUnit()
    fun moduleSetTargetTriple(module: LlvmModuleRef, targetTriple: String) = unsupportedUnit()
    fun moduleSetDataLayout(module: LlvmModuleRef, dataLayout: String) = unsupportedUnit()
    fun moduleAddFunction(module: LlvmModuleRef, name: String, functionType: LlvmTypeRef): LlvmValueRef = unsupported()
    fun moduleAddGlobal(module: LlvmModuleRef, type: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun moduleToString(module: LlvmModuleRef): String = unsupported()
    fun moduleVerify(module: LlvmModuleRef): LlvmVerificationResult = unsupported()

    fun contextIntType(context: LlvmContextRef, bits: Int): LlvmTypeRef = unsupported()
    fun contextFloatType(context: LlvmContextRef): LlvmTypeRef = unsupported()
    fun contextDoubleType(context: LlvmContextRef): LlvmTypeRef = unsupported()
    fun contextVoidType(context: LlvmContextRef): LlvmTypeRef = unsupported()
    fun contextPtrType(context: LlvmContextRef): LlvmTypeRef = unsupported()
    fun contextFunctionType(
        returnType: LlvmTypeRef,
        parameterTypes: List<LlvmTypeRef>,
        isVarArg: Boolean,
    ): LlvmTypeRef = unsupported()

    fun contextNamedStructType(context: LlvmContextRef, name: String): LlvmTypeRef = unsupported()
    fun structSetBody(type: LlvmTypeRef, elementTypes: List<LlvmTypeRef>, isPacked: Boolean) = unsupportedUnit()
    fun contextArrayType(elementType: LlvmTypeRef, size: Int): LlvmTypeRef = unsupported()

    fun builderCreateInContext(context: LlvmContextRef): LlvmBuilderRef = unsupported()
    fun builderDispose(builder: LlvmBuilderRef) = unsupportedUnit()
    fun builderPositionAtEnd(builder: LlvmBuilderRef, block: LlvmBasicBlockRef) = unsupportedUnit()

    fun builderBuildRet(builder: LlvmBuilderRef, value: LlvmValueRef): LlvmValueRef = unsupported()
    fun builderBuildRetVoid(builder: LlvmBuilderRef): LlvmValueRef = unsupported()
    fun builderBuildBr(builder: LlvmBuilderRef, dest: LlvmBasicBlockRef): LlvmValueRef = unsupported()
    fun builderBuildCondBr(
        builder: LlvmBuilderRef,
        cond: LlvmValueRef,
        thenBlock: LlvmBasicBlockRef,
        elseBlock: LlvmBasicBlockRef,
    ): LlvmValueRef = unsupported()

    fun builderBuildSwitch(
        builder: LlvmBuilderRef,
        value: LlvmValueRef,
        defaultBlock: LlvmBasicBlockRef,
        cases: List<Pair<LlvmValueRef, LlvmBasicBlockRef>>,
    ): LlvmValueRef = unsupported()

    fun builderBuildUnreachable(builder: LlvmBuilderRef): LlvmValueRef = unsupported()

    fun builderBuildAdd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildSub(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildMul(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildSDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildUDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildSRem(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildURem(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildFNeg(builder: LlvmBuilderRef, value: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildFAdd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildFSub(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildFMul(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildFDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()

    fun builderBuildAnd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildOr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildXor(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildShl(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildAShr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildLShr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()

    fun builderBuildICmp(
        builder: LlvmBuilderRef,
        predicate: LlvmIntPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String,
    ): LlvmValueRef = unsupported()

    fun builderBuildFCmp(
        builder: LlvmBuilderRef,
        predicate: LlvmFloatPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String,
    ): LlvmValueRef = unsupported()

    fun builderBuildAlloca(builder: LlvmBuilderRef, type: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildLoad(builder: LlvmBuilderRef, type: LlvmTypeRef, pointer: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildStore(builder: LlvmBuilderRef, value: LlvmValueRef, pointer: LlvmValueRef): LlvmValueRef = unsupported()
    fun builderBuildGep(
        builder: LlvmBuilderRef,
        elementType: LlvmTypeRef,
        pointer: LlvmValueRef,
        indices: List<LlvmValueRef>,
        inBounds: Boolean,
        name: String,
    ): LlvmValueRef = unsupported()

    fun builderBuildTrunc(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildZExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildSExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildFPTrunc(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildFPExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildFPToUI(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildFPToSI(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildUIToFP(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildSIToFP(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildPtrToInt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildIntToPtr(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderBuildBitCast(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()

    fun builderBuildCall(
        builder: LlvmBuilderRef,
        function: LlvmValueRef,
        args: List<LlvmValueRef>,
        name: String,
    ): LlvmValueRef = unsupported()

    fun builderBuildPhi(builder: LlvmBuilderRef, type: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    fun builderAddIncoming(phi: LlvmValueRef, incoming: List<LlvmPhiIncoming>) = unsupportedUnit()
    fun builderBuildSelect(
        builder: LlvmBuilderRef,
        condition: LlvmValueRef,
        thenValue: LlvmValueRef,
        elseValue: LlvmValueRef,
        name: String,
    ): LlvmValueRef = unsupported()

    fun builderBuildExtractValue(
        builder: LlvmBuilderRef,
        aggregate: LlvmValueRef,
        index: Int,
        name: String,
    ): LlvmValueRef = unsupported()

    fun builderBuildInsertValue(
        builder: LlvmBuilderRef,
        aggregate: LlvmValueRef,
        element: LlvmValueRef,
        index: Int,
        name: String,
    ): LlvmValueRef = unsupported()
}

internal object LlvmBindingRegistry {
    @Volatile
    var bindings: LlvmBindings = object : LlvmBindings {}
}

internal inline fun <T> withLlvmBindingsForTest(bindings: LlvmBindings, block: () -> T): T {
    val previous = LlvmBindingRegistry.bindings
    LlvmBindingRegistry.bindings = bindings
    return try {
        block()
    } finally {
        LlvmBindingRegistry.bindings = previous
    }
}

private fun <T> unsupported(): T {
    throw LlvmBackendUnavailableException("LLVM native bindings are not installed")
}

private fun unsupportedUnit(): Unit {
    throw LlvmBackendUnavailableException("LLVM native bindings are not installed")
}
