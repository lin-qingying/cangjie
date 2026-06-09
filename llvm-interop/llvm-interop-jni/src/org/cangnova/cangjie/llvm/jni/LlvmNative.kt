package org.cangnova.cangjie.llvm.jni

import org.cangnova.cangjie.llvm.api.LlvmBackendUnavailableException
import org.cangnova.cangjie.llvm.api.installLlvmBindings

/**
 * LLVM JNI 原生入口。
 *
 * 提供 Kotlin 侧到本地 LLVM C API 的静态桥接方法。
 */
object LlvmNative {
    @JvmField
    val isAvailable: Boolean

    @JvmField
    val loadDiagnostics: String

    init {
        val loadResult = NativeLibraryLoader.default().load()
        isAvailable = loadResult.loaded
        loadDiagnostics = loadResult.diagnostics
    }

    /**
     * 将 JNI 实现安装为当前进程的 LLVM API 绑定。
     */
    fun installBindings() {
        if (!isAvailable) {
            throw LlvmBackendUnavailableException("LLVM JNI native bindings are unavailable: $loadDiagnostics")
        }
        installLlvmBindings(JniLlvmBindings)
    }

    @JvmStatic external fun targetInitializeAll()
    @JvmStatic external fun targetDefaultTriple(): String
    @JvmStatic external fun targetCreateMachine(
        targetTriple: String,
        cpu: String,
        features: String,
        optimizationLevel: Int,
        relocationMode: Int,
        codeModel: Int,
    ): Long
    @JvmStatic external fun targetDisposeMachine(targetMachine: Long)
    @JvmStatic external fun targetMachineEmitObjectFile(targetMachine: Long, module: Long, outputPath: String)
    @JvmStatic external fun targetMachineEmitObjectBytes(targetMachine: Long, module: Long): ByteArray

    @JvmStatic external fun contextCreate(): Long
    @JvmStatic external fun contextDispose(context: Long)

    @JvmStatic external fun moduleCreateInContext(name: String, context: Long): Long
    @JvmStatic external fun moduleDispose(module: Long)
    @JvmStatic external fun moduleSetTargetTriple(module: Long, targetTriple: String)
    @JvmStatic external fun moduleSetDataLayout(module: Long, dataLayout: String)
    @JvmStatic external fun moduleAddFunction(module: Long, name: String, functionType: Long): Long
    @JvmStatic external fun moduleAddGlobal(module: Long, type: Long, name: String): Long
    @JvmStatic external fun functionAppendBasicBlock(function: Long, name: String): Long
    @JvmStatic external fun moduleParseAssemblyInContext(name: String, assembly: String, context: Long): Long
    @JvmStatic external fun modulePrintToString(module: Long): String
    @JvmStatic external fun moduleVerify(module: Long)
    @JvmStatic external fun moduleRunPasses(module: Long, passPipeline: String, targetMachine: Long)

    @JvmStatic external fun intTypeInContext(context: Long, bits: Int): Long
    @JvmStatic external fun floatTypeInContext(context: Long): Long
    @JvmStatic external fun doubleTypeInContext(context: Long): Long
    @JvmStatic external fun voidTypeInContext(context: Long): Long
    @JvmStatic external fun ptrTypeInContext(context: Long): Long
    @JvmStatic external fun functionType(returnType: Long, paramTypes: LongArray, isVarArg: Boolean): Long
    @JvmStatic external fun namedStructTypeInContext(context: Long, name: String): Long
    @JvmStatic external fun structSetBody(type: Long, elementTypes: LongArray, isPacked: Boolean)
    @JvmStatic external fun arrayType(elementType: Long, size: Int): Long

    @JvmStatic external fun constInt(type: Long, value: Long, signExtend: Boolean): Long
    @JvmStatic external fun constReal(type: Long, value: Double): Long
    @JvmStatic external fun constNull(type: Long): Long
    @JvmStatic external fun valueGetName(value: Long): String
    @JvmStatic external fun valueGetType(value: Long): Long

    @JvmStatic external fun builderCreateInContext(context: Long): Long
    @JvmStatic external fun builderDispose(builder: Long)
    @JvmStatic external fun builderPositionAtEnd(builder: Long, block: Long)
    @JvmStatic external fun builderBuildRet(builder: Long, value: Long): Long
    @JvmStatic external fun builderBuildRetVoid(builder: Long): Long
    @JvmStatic external fun builderBuildBr(builder: Long, block: Long): Long
    @JvmStatic external fun builderBuildCondBr(builder: Long, condition: Long, thenBlock: Long, elseBlock: Long): Long
    @JvmStatic external fun builderBuildSwitch(builder: Long, value: Long, defaultBlock: Long, caseValues: LongArray, caseBlocks: LongArray): Long
    @JvmStatic external fun builderBuildUnreachable(builder: Long): Long
    @JvmStatic external fun builderBuildAdd(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildSub(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildMul(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildSDiv(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildUDiv(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildSRem(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildURem(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildFNeg(builder: Long, value: Long, name: String): Long
    @JvmStatic external fun builderBuildFAdd(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildFSub(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildFMul(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildFDiv(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildAnd(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildOr(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildXor(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildShl(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildAShr(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildLShr(builder: Long, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildICmp(builder: Long, predicate: Int, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildFCmp(builder: Long, predicate: Int, lhs: Long, rhs: Long, name: String): Long
    @JvmStatic external fun builderBuildAlloca(builder: Long, type: Long, name: String): Long
    @JvmStatic external fun builderBuildLoad(builder: Long, type: Long, pointer: Long, name: String): Long
    @JvmStatic external fun builderBuildStore(builder: Long, value: Long, pointer: Long): Long
    @JvmStatic external fun builderBuildGep(builder: Long, elementType: Long, pointer: Long, indices: LongArray, inBounds: Boolean, name: String): Long
    @JvmStatic external fun builderBuildTrunc(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildZExt(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildSExt(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildFPTrunc(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildFPExt(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildFPToUI(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildFPToSI(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildUIToFP(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildSIToFP(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildPtrToInt(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildIntToPtr(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildBitCast(builder: Long, value: Long, targetType: Long, name: String): Long
    @JvmStatic external fun builderBuildCall(builder: Long, callee: Long, args: LongArray, name: String): Long
    @JvmStatic external fun builderBuildPhi(builder: Long, type: Long, name: String): Long
    @JvmStatic external fun builderAddIncoming(phi: Long, values: LongArray, blocks: LongArray)
    @JvmStatic external fun builderBuildSelect(builder: Long, condition: Long, thenValue: Long, elseValue: Long, name: String): Long
    @JvmStatic external fun builderBuildExtractValue(builder: Long, aggregate: Long, index: Int, name: String): Long
    @JvmStatic external fun builderBuildInsertValue(builder: Long, aggregate: Long, element: Long, index: Int, name: String): Long

    @JvmStatic external fun verifyFunction(function: Long)
    @JvmStatic external fun writeBitcodeToFile(module: Long, outputPath: String): Int
    @JvmStatic external fun writeBitcodeToMemoryBuffer(module: Long): ByteArray
}
