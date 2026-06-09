package org.cangnova.cangjie.llvm.jni

import org.cangnova.cangjie.llvm.api.LlvmBasicBlockRef
import org.cangnova.cangjie.llvm.api.LlvmBindings
import org.cangnova.cangjie.llvm.api.LlvmBuilderRef
import org.cangnova.cangjie.llvm.api.LlvmCodeGenOptimizationLevel
import org.cangnova.cangjie.llvm.api.LlvmCodeModel
import org.cangnova.cangjie.llvm.api.LlvmContextRef
import org.cangnova.cangjie.llvm.api.LlvmException
import org.cangnova.cangjie.llvm.api.LlvmFloatPredicate
import org.cangnova.cangjie.llvm.api.LlvmIntPredicate
import org.cangnova.cangjie.llvm.api.LlvmModuleRef
import org.cangnova.cangjie.llvm.api.LlvmPhiIncoming
import org.cangnova.cangjie.llvm.api.LlvmRelocationMode
import org.cangnova.cangjie.llvm.api.LlvmTargetMachineOptions
import org.cangnova.cangjie.llvm.api.LlvmTargetMachineRef
import org.cangnova.cangjie.llvm.api.LlvmTypeRef
import org.cangnova.cangjie.llvm.api.LlvmValueRef
import org.cangnova.cangjie.llvm.api.LlvmVerificationResult

/**
 * LLVM JNI 生产绑定实现。
 *
 * 该对象把公开 API 层的类型安全句柄映射到 LlvmNative 的原生 long 句柄，
 * 是 JVM 内 CHIR -> LLVM 后端实际调用 LLVM C API 的基础入口。
 */
internal object JniLlvmBindings : LlvmBindings {
    override fun targetInitializeAll() {
        LlvmNative.targetInitializeAll()
    }

    override fun targetDefaultTriple(): String = LlvmNative.targetDefaultTriple()

    override fun targetCreateMachine(options: LlvmTargetMachineOptions): LlvmTargetMachineRef =
        LlvmTargetMachineRef(
            LlvmNative.targetCreateMachine(
                options.targetTriple,
                options.cpu,
                options.features,
                options.optimizationLevel.toNativeLevel(),
                options.relocationMode.toNativeMode(),
                options.codeModel.toNativeModel(),
            ),
        )

    override fun targetDisposeMachine(machine: LlvmTargetMachineRef) {
        LlvmNative.targetDisposeMachine(machine.address)
    }

    override fun targetMachineEmitObjectFile(machine: LlvmTargetMachineRef, module: LlvmModuleRef, outputPath: String) {
        LlvmNative.targetMachineEmitObjectFile(machine.address, module.address, outputPath)
    }

    override fun targetMachineEmitObjectBytes(machine: LlvmTargetMachineRef, module: LlvmModuleRef): ByteArray =
        LlvmNative.targetMachineEmitObjectBytes(machine.address, module.address)

    override fun contextCreate(): LlvmContextRef = LlvmContextRef(LlvmNative.contextCreate())

    override fun contextDispose(context: LlvmContextRef) {
        LlvmNative.contextDispose(context.address)
    }

    override fun moduleCreateInContext(name: String, context: LlvmContextRef): LlvmModuleRef =
        LlvmModuleRef(LlvmNative.moduleCreateInContext(name, context.address))

    override fun moduleDispose(module: LlvmModuleRef) {
        LlvmNative.moduleDispose(module.address)
    }

    override fun moduleSetTargetTriple(module: LlvmModuleRef, targetTriple: String) {
        LlvmNative.moduleSetTargetTriple(module.address, targetTriple)
    }

    override fun moduleSetDataLayout(module: LlvmModuleRef, dataLayout: String) {
        LlvmNative.moduleSetDataLayout(module.address, dataLayout)
    }

    override fun moduleAddFunction(module: LlvmModuleRef, name: String, functionType: LlvmTypeRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.moduleAddFunction(module.address, name, functionType.address))

    override fun moduleAddGlobal(module: LlvmModuleRef, type: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.moduleAddGlobal(module.address, type.address, name))

    override fun moduleParseAssemblyInContext(
        name: String,
        assembly: String,
        context: LlvmContextRef,
    ): LlvmModuleRef = LlvmModuleRef(LlvmNative.moduleParseAssemblyInContext(name, assembly, context.address))

    override fun moduleToString(module: LlvmModuleRef): String = LlvmNative.modulePrintToString(module.address)

    override fun moduleVerify(module: LlvmModuleRef): LlvmVerificationResult =
        verificationResult { LlvmNative.moduleVerify(module.address) }

    override fun moduleWriteBitcodeToMemoryBuffer(module: LlvmModuleRef): ByteArray =
        LlvmNative.writeBitcodeToMemoryBuffer(module.address)

    override fun moduleWriteBitcodeToFile(module: LlvmModuleRef, outputPath: String): Int =
        LlvmNative.writeBitcodeToFile(module.address, outputPath)

    override fun moduleRunPasses(module: LlvmModuleRef, passPipeline: String, targetMachine: LlvmTargetMachineRef) {
        LlvmNative.moduleRunPasses(module.address, passPipeline, targetMachine.address)
    }

    override fun functionAppendBasicBlock(function: LlvmValueRef, name: String): LlvmBasicBlockRef =
        LlvmBasicBlockRef(LlvmNative.functionAppendBasicBlock(function.address, name))

    override fun functionVerify(function: LlvmValueRef): LlvmVerificationResult =
        verificationResult { LlvmNative.verifyFunction(function.address) }

    override fun contextIntType(context: LlvmContextRef, bits: Int): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.intTypeInContext(context.address, bits))

    override fun contextFloatType(context: LlvmContextRef): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.floatTypeInContext(context.address))

    override fun contextDoubleType(context: LlvmContextRef): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.doubleTypeInContext(context.address))

    override fun contextVoidType(context: LlvmContextRef): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.voidTypeInContext(context.address))

    override fun contextPtrType(context: LlvmContextRef): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.ptrTypeInContext(context.address))

    override fun contextFunctionType(
        returnType: LlvmTypeRef,
        parameterTypes: List<LlvmTypeRef>,
        isVarArg: Boolean,
    ): LlvmTypeRef = LlvmTypeRef(
        LlvmNative.functionType(returnType.address, parameterTypes.toTypeAddressArray(), isVarArg),
    )

    override fun contextNamedStructType(context: LlvmContextRef, name: String): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.namedStructTypeInContext(context.address, name))

    override fun structSetBody(type: LlvmTypeRef, elementTypes: List<LlvmTypeRef>, isPacked: Boolean) {
        LlvmNative.structSetBody(type.address, elementTypes.toTypeAddressArray(), isPacked)
    }

    override fun contextArrayType(elementType: LlvmTypeRef, size: Int): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.arrayType(elementType.address, size))

    override fun constInt(type: LlvmTypeRef, value: Long, signExtend: Boolean): LlvmValueRef =
        LlvmValueRef(LlvmNative.constInt(type.address, value, signExtend))

    override fun constReal(type: LlvmTypeRef, value: Double): LlvmValueRef =
        LlvmValueRef(LlvmNative.constReal(type.address, value))

    override fun constNull(type: LlvmTypeRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.constNull(type.address))

    override fun valueGetName(value: LlvmValueRef): String = LlvmNative.valueGetName(value.address)

    override fun valueGetType(value: LlvmValueRef): LlvmTypeRef = LlvmTypeRef(LlvmNative.valueGetType(value.address))

    override fun builderCreateInContext(context: LlvmContextRef): LlvmBuilderRef =
        LlvmBuilderRef(LlvmNative.builderCreateInContext(context.address))

    override fun builderDispose(builder: LlvmBuilderRef) {
        LlvmNative.builderDispose(builder.address)
    }

    override fun builderPositionAtEnd(builder: LlvmBuilderRef, block: LlvmBasicBlockRef) {
        LlvmNative.builderPositionAtEnd(builder.address, block.address)
    }

    override fun builderBuildRet(builder: LlvmBuilderRef, value: LlvmValueRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildRet(builder.address, value.address))

    override fun builderBuildRetVoid(builder: LlvmBuilderRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildRetVoid(builder.address))

    override fun builderBuildBr(builder: LlvmBuilderRef, dest: LlvmBasicBlockRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildBr(builder.address, dest.address))

    override fun builderBuildCondBr(
        builder: LlvmBuilderRef,
        cond: LlvmValueRef,
        thenBlock: LlvmBasicBlockRef,
        elseBlock: LlvmBasicBlockRef,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildCondBr(builder.address, cond.address, thenBlock.address, elseBlock.address),
    )

    override fun builderBuildSwitch(
        builder: LlvmBuilderRef,
        value: LlvmValueRef,
        defaultBlock: LlvmBasicBlockRef,
        cases: List<Pair<LlvmValueRef, LlvmBasicBlockRef>>,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildSwitch(
            builder.address,
            value.address,
            defaultBlock.address,
            LongArray(cases.size) { index -> cases[index].first.address },
            LongArray(cases.size) { index -> cases[index].second.address },
        ),
    )

    override fun builderBuildUnreachable(builder: LlvmBuilderRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildUnreachable(builder.address))

    override fun builderBuildAdd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildAdd(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildSub(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildSub(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildMul(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildMul(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildSDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildSDiv(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildUDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildUDiv(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildSRem(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildSRem(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildURem(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildURem(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildFNeg(builder: LlvmBuilderRef, value: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFNeg(builder.address, value.address, name))

    override fun builderBuildFAdd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFAdd(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildFSub(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFSub(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildFMul(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFMul(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildFDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFDiv(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildAnd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildAnd(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildOr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildOr(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildXor(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildXor(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildShl(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildShl(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildAShr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildAShr(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildLShr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildLShr(builder.address, lhs.address, rhs.address, name))

    override fun builderBuildICmp(
        builder: LlvmBuilderRef,
        predicate: LlvmIntPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildICmp(builder.address, predicate.toNativePredicate(), lhs.address, rhs.address, name),
    )

    override fun builderBuildFCmp(
        builder: LlvmBuilderRef,
        predicate: LlvmFloatPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildFCmp(builder.address, predicate.toNativePredicate(), lhs.address, rhs.address, name),
    )

    override fun builderBuildAlloca(builder: LlvmBuilderRef, type: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildAlloca(builder.address, type.address, name))

    override fun builderBuildLoad(
        builder: LlvmBuilderRef,
        type: LlvmTypeRef,
        pointer: LlvmValueRef,
        name: String,
    ): LlvmValueRef = LlvmValueRef(LlvmNative.builderBuildLoad(builder.address, type.address, pointer.address, name))

    override fun builderBuildStore(builder: LlvmBuilderRef, value: LlvmValueRef, pointer: LlvmValueRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildStore(builder.address, value.address, pointer.address))

    override fun builderBuildGep(
        builder: LlvmBuilderRef,
        elementType: LlvmTypeRef,
        pointer: LlvmValueRef,
        indices: List<LlvmValueRef>,
        inBounds: Boolean,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildGep(builder.address, elementType.address, pointer.address, indices.toValueAddressArray(), inBounds, name),
    )

    override fun builderBuildTrunc(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildTrunc(builder.address, value.address, targetType.address, name))

    override fun builderBuildZExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildZExt(builder.address, value.address, targetType.address, name))

    override fun builderBuildSExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildSExt(builder.address, value.address, targetType.address, name))

    override fun builderBuildFPTrunc(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFPTrunc(builder.address, value.address, targetType.address, name))

    override fun builderBuildFPExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFPExt(builder.address, value.address, targetType.address, name))

    override fun builderBuildFPToUI(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFPToUI(builder.address, value.address, targetType.address, name))

    override fun builderBuildFPToSI(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFPToSI(builder.address, value.address, targetType.address, name))

    override fun builderBuildUIToFP(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildUIToFP(builder.address, value.address, targetType.address, name))

    override fun builderBuildSIToFP(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildSIToFP(builder.address, value.address, targetType.address, name))

    override fun builderBuildPtrToInt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildPtrToInt(builder.address, value.address, targetType.address, name))

    override fun builderBuildIntToPtr(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildIntToPtr(builder.address, value.address, targetType.address, name))

    override fun builderBuildBitCast(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildBitCast(builder.address, value.address, targetType.address, name))

    override fun builderBuildCall(
        builder: LlvmBuilderRef,
        function: LlvmValueRef,
        args: List<LlvmValueRef>,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildCall(builder.address, function.address, args.toValueAddressArray(), name),
    )

    override fun builderBuildPhi(builder: LlvmBuilderRef, type: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildPhi(builder.address, type.address, name))

    override fun builderAddIncoming(phi: LlvmValueRef, incoming: List<LlvmPhiIncoming>) {
        LlvmNative.builderAddIncoming(
            phi.address,
            LongArray(incoming.size) { index -> incoming[index].value.address },
            LongArray(incoming.size) { index -> incoming[index].block.address },
        )
    }

    override fun builderBuildSelect(
        builder: LlvmBuilderRef,
        condition: LlvmValueRef,
        thenValue: LlvmValueRef,
        elseValue: LlvmValueRef,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildSelect(builder.address, condition.address, thenValue.address, elseValue.address, name),
    )

    override fun builderBuildExtractValue(
        builder: LlvmBuilderRef,
        aggregate: LlvmValueRef,
        index: Int,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildExtractValue(builder.address, aggregate.address, index, name),
    )

    override fun builderBuildInsertValue(
        builder: LlvmBuilderRef,
        aggregate: LlvmValueRef,
        element: LlvmValueRef,
        index: Int,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildInsertValue(builder.address, aggregate.address, element.address, index, name),
    )

    private fun verificationResult(verify: () -> Unit): LlvmVerificationResult =
        try {
            verify()
            LlvmVerificationResult(ok = true)
        } catch (error: LlvmException) {
            LlvmVerificationResult(ok = false, message = error.message)
        }

    private fun LlvmIntPredicate.toNativePredicate(): Int =
        when (this) {
            LlvmIntPredicate.EQ -> 32
            LlvmIntPredicate.NE -> 33
            LlvmIntPredicate.UGT -> 34
            LlvmIntPredicate.UGE -> 35
            LlvmIntPredicate.ULT -> 36
            LlvmIntPredicate.ULE -> 37
            LlvmIntPredicate.SGT -> 38
            LlvmIntPredicate.SGE -> 39
            LlvmIntPredicate.SLT -> 40
            LlvmIntPredicate.SLE -> 41
        }

    private fun LlvmFloatPredicate.toNativePredicate(): Int =
        when (this) {
            LlvmFloatPredicate.FALSE -> 0
            LlvmFloatPredicate.OEQ -> 1
            LlvmFloatPredicate.OGT -> 2
            LlvmFloatPredicate.OGE -> 3
            LlvmFloatPredicate.OLT -> 4
            LlvmFloatPredicate.OLE -> 5
            LlvmFloatPredicate.ONE -> 6
            LlvmFloatPredicate.ORD -> 7
            LlvmFloatPredicate.UNO -> 8
            LlvmFloatPredicate.UEQ -> 9
            LlvmFloatPredicate.UGT -> 10
            LlvmFloatPredicate.UGE -> 11
            LlvmFloatPredicate.ULT -> 12
            LlvmFloatPredicate.ULE -> 13
            LlvmFloatPredicate.UNE -> 14
            LlvmFloatPredicate.TRUE -> 15
        }

    private fun LlvmCodeGenOptimizationLevel.toNativeLevel(): Int =
        when (this) {
            LlvmCodeGenOptimizationLevel.NONE -> 0
            LlvmCodeGenOptimizationLevel.LESS -> 1
            LlvmCodeGenOptimizationLevel.DEFAULT -> 2
            LlvmCodeGenOptimizationLevel.AGGRESSIVE -> 3
        }

    private fun LlvmRelocationMode.toNativeMode(): Int =
        when (this) {
            LlvmRelocationMode.DEFAULT -> 0
            LlvmRelocationMode.STATIC -> 1
            LlvmRelocationMode.PIC -> 2
            LlvmRelocationMode.DYNAMIC_NO_PIC -> 3
            LlvmRelocationMode.ROPI -> 4
            LlvmRelocationMode.RWPI -> 5
            LlvmRelocationMode.ROPI_RWPI -> 6
        }

    private fun LlvmCodeModel.toNativeModel(): Int =
        when (this) {
            LlvmCodeModel.DEFAULT -> 0
            LlvmCodeModel.JIT_DEFAULT -> 1
            LlvmCodeModel.TINY -> 2
            LlvmCodeModel.SMALL -> 3
            LlvmCodeModel.KERNEL -> 4
            LlvmCodeModel.MEDIUM -> 5
            LlvmCodeModel.LARGE -> 6
        }

    private fun List<LlvmTypeRef>.toTypeAddressArray(): LongArray = LongArray(size) { index -> this[index].address }

    private fun List<LlvmValueRef>.toValueAddressArray(): LongArray = LongArray(size) { index -> this[index].address }
}
