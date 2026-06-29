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
    /**
     * 通过 JNI 初始化 LLVM 所有 target/backend。
     */
    override fun targetInitializeAll() {
        LlvmNative.targetInitializeAll()
    }

    /**
     * 通过 JNI 获取 LLVM 默认目标三元组。
     */
    override fun targetDefaultTriple(): String = LlvmNative.targetDefaultTriple()

    /**
     * 将 Kotlin 侧目标机器选项转换为原生枚举值并创建 LLVMTargetMachine。
     */
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

    /**
     * 通过 JNI 释放目标机器句柄。
     */
    override fun targetDisposeMachine(machine: LlvmTargetMachineRef) {
        LlvmNative.targetDisposeMachine(machine.address)
    }

    /**
     * 通过目标机器把模块生成到指定目标文件路径。
     */
    override fun targetMachineEmitObjectFile(machine: LlvmTargetMachineRef, module: LlvmModuleRef, outputPath: String) {
        LlvmNative.targetMachineEmitObjectFile(machine.address, module.address, outputPath)
    }

    /**
     * 通过目标机器把模块生成目标文件字节。
     */
    override fun targetMachineEmitObjectBytes(machine: LlvmTargetMachineRef, module: LlvmModuleRef): ByteArray =
        LlvmNative.targetMachineEmitObjectBytes(machine.address, module.address)

    /**
     * 创建 LLVMContext 并包装为类型安全句柄。
     */
    override fun contextCreate(): LlvmContextRef = LlvmContextRef(LlvmNative.contextCreate())

    /**
     * 释放 LLVMContext 句柄。
     */
    override fun contextDispose(context: LlvmContextRef) {
        LlvmNative.contextDispose(context.address)
    }

    /**
     * 在指定上下文中创建模块。
     */
    override fun moduleCreateInContext(name: String, context: LlvmContextRef): LlvmModuleRef =
        LlvmModuleRef(LlvmNative.moduleCreateInContext(name, context.address))

    /**
     * 释放 LLVMModule 句柄。
     */
    override fun moduleDispose(module: LlvmModuleRef) {
        LlvmNative.moduleDispose(module.address)
    }

    /**
     * 设置模块目标三元组。
     */
    override fun moduleSetTargetTriple(module: LlvmModuleRef, targetTriple: String) {
        LlvmNative.moduleSetTargetTriple(module.address, targetTriple)
    }

    /**
     * 设置模块数据布局。
     */
    override fun moduleSetDataLayout(module: LlvmModuleRef, dataLayout: String) {
        LlvmNative.moduleSetDataLayout(module.address, dataLayout)
    }

    /**
     * 向模块添加函数声明。
     */
    override fun moduleAddFunction(module: LlvmModuleRef, name: String, functionType: LlvmTypeRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.moduleAddFunction(module.address, name, functionType.address))

    /**
     * 向模块添加全局变量。
     */
    override fun moduleAddGlobal(module: LlvmModuleRef, type: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.moduleAddGlobal(module.address, type.address, name))

    /**
     * 在上下文中解析 LLVM assembly 文本为模块。
     */
    override fun moduleParseAssemblyInContext(
        name: String,
        assembly: String,
        context: LlvmContextRef,
    ): LlvmModuleRef = LlvmModuleRef(LlvmNative.moduleParseAssemblyInContext(name, assembly, context.address))

    /**
     * 将模块打印为 LLVM IR 文本。
     */
    override fun moduleToString(module: LlvmModuleRef): String = LlvmNative.modulePrintToString(module.address)

    /**
     * 校验模块并转换为 [LlvmVerificationResult]。
     */
    override fun moduleVerify(module: LlvmModuleRef): LlvmVerificationResult =
        verificationResult { LlvmNative.moduleVerify(module.address) }

    /**
     * 将模块写出为 bitcode 字节数组。
     */
    override fun moduleWriteBitcodeToMemoryBuffer(module: LlvmModuleRef): ByteArray =
        LlvmNative.writeBitcodeToMemoryBuffer(module.address)

    /**
     * 将模块 bitcode 写入文件。
     */
    override fun moduleWriteBitcodeToFile(module: LlvmModuleRef, outputPath: String): Int =
        LlvmNative.writeBitcodeToFile(module.address, outputPath)

    /**
     * 在模块上运行 LLVM pass pipeline。
     */
    override fun moduleRunPasses(module: LlvmModuleRef, passPipeline: String, targetMachine: LlvmTargetMachineRef) {
        LlvmNative.moduleRunPasses(module.address, passPipeline, targetMachine.address)
    }

    /**
     * 向函数追加基本块。
     */
    override fun functionAppendBasicBlock(function: LlvmValueRef, name: String): LlvmBasicBlockRef =
        LlvmBasicBlockRef(LlvmNative.functionAppendBasicBlock(function.address, name))

    /**
     * 校验函数并转换为 [LlvmVerificationResult]。
     */
    override fun functionVerify(function: LlvmValueRef): LlvmVerificationResult =
        verificationResult { LlvmNative.verifyFunction(function.address) }

    /**
     * 创建指定 bit 宽度的整数类型。
     */
    override fun contextIntType(context: LlvmContextRef, bits: Int): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.intTypeInContext(context.address, bits))

    /**
     * 获取上下文中的 float 类型。
     */
    override fun contextFloatType(context: LlvmContextRef): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.floatTypeInContext(context.address))

    /**
     * 获取上下文中的 double 类型。
     */
    override fun contextDoubleType(context: LlvmContextRef): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.doubleTypeInContext(context.address))

    /**
     * 获取上下文中的 void 类型。
     */
    override fun contextVoidType(context: LlvmContextRef): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.voidTypeInContext(context.address))

    /**
     * 获取上下文中的 opaque pointer 类型。
     */
    override fun contextPtrType(context: LlvmContextRef): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.ptrTypeInContext(context.address))

    /**
     * 创建函数类型并把参数类型列表转换为原生地址数组。
     */
    override fun contextFunctionType(
        returnType: LlvmTypeRef,
        parameterTypes: List<LlvmTypeRef>,
        isVarArg: Boolean,
    ): LlvmTypeRef = LlvmTypeRef(
        LlvmNative.functionType(returnType.address, parameterTypes.toTypeAddressArray(), isVarArg),
    )

    /**
     * 在上下文中创建具名结构体类型。
     */
    override fun contextNamedStructType(context: LlvmContextRef, name: String): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.namedStructTypeInContext(context.address, name))

    /**
     * 设置结构体 body 字段类型。
     */
    override fun structSetBody(type: LlvmTypeRef, elementTypes: List<LlvmTypeRef>, isPacked: Boolean) {
        LlvmNative.structSetBody(type.address, elementTypes.toTypeAddressArray(), isPacked)
    }

    /**
     * 创建数组类型。
     */
    override fun contextArrayType(elementType: LlvmTypeRef, size: Int): LlvmTypeRef =
        LlvmTypeRef(LlvmNative.arrayType(elementType.address, size))

    /**
     * 创建整数常量。
     */
    override fun constInt(type: LlvmTypeRef, value: Long, signExtend: Boolean): LlvmValueRef =
        LlvmValueRef(LlvmNative.constInt(type.address, value, signExtend))

    /**
     * 创建浮点常量。
     */
    override fun constReal(type: LlvmTypeRef, value: Double): LlvmValueRef =
        LlvmValueRef(LlvmNative.constReal(type.address, value))

    /**
     * 创建 null 常量。
     */
    override fun constNull(type: LlvmTypeRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.constNull(type.address))

    /**
     * 读取 LLVM value 名称。
     */
    override fun valueGetName(value: LlvmValueRef): String = LlvmNative.valueGetName(value.address)

    /**
     * 读取 LLVM value 类型。
     */
    override fun valueGetType(value: LlvmValueRef): LlvmTypeRef = LlvmTypeRef(LlvmNative.valueGetType(value.address))

    /**
     * 创建 LLVM IR builder。
     */
    override fun builderCreateInContext(context: LlvmContextRef): LlvmBuilderRef =
        LlvmBuilderRef(LlvmNative.builderCreateInContext(context.address))

    /**
     * 释放 LLVM IR builder。
     */
    override fun builderDispose(builder: LlvmBuilderRef) {
        LlvmNative.builderDispose(builder.address)
    }

    /**
     * 将 builder 定位到基本块末尾。
     */
    override fun builderPositionAtEnd(builder: LlvmBuilderRef, block: LlvmBasicBlockRef) {
        LlvmNative.builderPositionAtEnd(builder.address, block.address)
    }

    /**
     * 构造返回值指令。
     */
    override fun builderBuildRet(builder: LlvmBuilderRef, value: LlvmValueRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildRet(builder.address, value.address))

    /**
     * 构造 void 返回指令。
     */
    override fun builderBuildRetVoid(builder: LlvmBuilderRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildRetVoid(builder.address))

    /**
     * 构造无条件分支指令。
     */
    override fun builderBuildBr(builder: LlvmBuilderRef, dest: LlvmBasicBlockRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildBr(builder.address, dest.address))

    /**
     * 构造条件分支指令。
     */
    override fun builderBuildCondBr(
        builder: LlvmBuilderRef,
        cond: LlvmValueRef,
        thenBlock: LlvmBasicBlockRef,
        elseBlock: LlvmBasicBlockRef,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildCondBr(builder.address, cond.address, thenBlock.address, elseBlock.address),
    )

    /**
     * 构造 switch 指令并将 case 列表拆分为值数组与基本块数组。
     */
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

    /**
     * 构造 unreachable 指令。
     */
    override fun builderBuildUnreachable(builder: LlvmBuilderRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildUnreachable(builder.address))

    /**
     * 构造整数加法指令。
     */
    override fun builderBuildAdd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildAdd(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造整数减法指令。
     */
    override fun builderBuildSub(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildSub(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造整数乘法指令。
     */
    override fun builderBuildMul(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildMul(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造有符号整数除法指令。
     */
    override fun builderBuildSDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildSDiv(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造无符号整数除法指令。
     */
    override fun builderBuildUDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildUDiv(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造有符号整数取余指令。
     */
    override fun builderBuildSRem(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildSRem(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造无符号整数取余指令。
     */
    override fun builderBuildURem(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildURem(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造浮点取负指令。
     */
    override fun builderBuildFNeg(builder: LlvmBuilderRef, value: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFNeg(builder.address, value.address, name))

    /**
     * 构造浮点加法指令。
     */
    override fun builderBuildFAdd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFAdd(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造浮点减法指令。
     */
    override fun builderBuildFSub(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFSub(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造浮点乘法指令。
     */
    override fun builderBuildFMul(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFMul(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造浮点除法指令。
     */
    override fun builderBuildFDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFDiv(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造按位与指令。
     */
    override fun builderBuildAnd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildAnd(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造按位或指令。
     */
    override fun builderBuildOr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildOr(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造按位异或指令。
     */
    override fun builderBuildXor(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildXor(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造左移指令。
     */
    override fun builderBuildShl(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildShl(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造算术右移指令。
     */
    override fun builderBuildAShr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildAShr(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造逻辑右移指令。
     */
    override fun builderBuildLShr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildLShr(builder.address, lhs.address, rhs.address, name))

    /**
     * 构造整数比较指令。
     */
    override fun builderBuildICmp(
        builder: LlvmBuilderRef,
        predicate: LlvmIntPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildICmp(builder.address, predicate.toNativePredicate(), lhs.address, rhs.address, name),
    )

    /**
     * 构造浮点比较指令。
     */
    override fun builderBuildFCmp(
        builder: LlvmBuilderRef,
        predicate: LlvmFloatPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildFCmp(builder.address, predicate.toNativePredicate(), lhs.address, rhs.address, name),
    )

    /**
     * 构造 alloca 指令。
     */
    override fun builderBuildAlloca(builder: LlvmBuilderRef, type: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildAlloca(builder.address, type.address, name))

    /**
     * 构造 load 指令。
     */
    override fun builderBuildLoad(
        builder: LlvmBuilderRef,
        type: LlvmTypeRef,
        pointer: LlvmValueRef,
        name: String,
    ): LlvmValueRef = LlvmValueRef(LlvmNative.builderBuildLoad(builder.address, type.address, pointer.address, name))

    /**
     * 构造 store 指令。
     */
    override fun builderBuildStore(builder: LlvmBuilderRef, value: LlvmValueRef, pointer: LlvmValueRef): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildStore(builder.address, value.address, pointer.address))

    /**
     * 构造 getelementptr 指令，并将索引列表转换为地址数组。
     */
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

    /**
     * 构造整数截断指令。
     */
    override fun builderBuildTrunc(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildTrunc(builder.address, value.address, targetType.address, name))

    /**
     * 构造零扩展指令。
     */
    override fun builderBuildZExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildZExt(builder.address, value.address, targetType.address, name))

    /**
     * 构造符号扩展指令。
     */
    override fun builderBuildSExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildSExt(builder.address, value.address, targetType.address, name))

    /**
     * 构造浮点截断指令。
     */
    override fun builderBuildFPTrunc(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFPTrunc(builder.address, value.address, targetType.address, name))

    /**
     * 构造浮点扩展指令。
     */
    override fun builderBuildFPExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFPExt(builder.address, value.address, targetType.address, name))

    /**
     * 构造浮点到无符号整数转换指令。
     */
    override fun builderBuildFPToUI(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFPToUI(builder.address, value.address, targetType.address, name))

    /**
     * 构造浮点到有符号整数转换指令。
     */
    override fun builderBuildFPToSI(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildFPToSI(builder.address, value.address, targetType.address, name))

    /**
     * 构造无符号整数到浮点转换指令。
     */
    override fun builderBuildUIToFP(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildUIToFP(builder.address, value.address, targetType.address, name))

    /**
     * 构造有符号整数到浮点转换指令。
     */
    override fun builderBuildSIToFP(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildSIToFP(builder.address, value.address, targetType.address, name))

    /**
     * 构造指针到整数转换指令。
     */
    override fun builderBuildPtrToInt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildPtrToInt(builder.address, value.address, targetType.address, name))

    /**
     * 构造整数到指针转换指令。
     */
    override fun builderBuildIntToPtr(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildIntToPtr(builder.address, value.address, targetType.address, name))

    /**
     * 构造 bitcast 指令。
     */
    override fun builderBuildBitCast(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildBitCast(builder.address, value.address, targetType.address, name))

    /**
     * 构造 call 指令，并将实参数组转换为原生地址数组。
     */
    override fun builderBuildCall(
        builder: LlvmBuilderRef,
        function: LlvmValueRef,
        args: List<LlvmValueRef>,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildCall(builder.address, function.address, args.toValueAddressArray(), name),
    )

    /**
     * 构造 PHI 指令。
     */
    override fun builderBuildPhi(builder: LlvmBuilderRef, type: LlvmTypeRef, name: String): LlvmValueRef =
        LlvmValueRef(LlvmNative.builderBuildPhi(builder.address, type.address, name))

    /**
     * 为 PHI 指令追加 incoming 值和前驱块。
     */
    override fun builderAddIncoming(phi: LlvmValueRef, incoming: List<LlvmPhiIncoming>) {
        LlvmNative.builderAddIncoming(
            phi.address,
            LongArray(incoming.size) { index -> incoming[index].value.address },
            LongArray(incoming.size) { index -> incoming[index].block.address },
        )
    }

    /**
     * 构造 select 指令。
     */
    override fun builderBuildSelect(
        builder: LlvmBuilderRef,
        condition: LlvmValueRef,
        thenValue: LlvmValueRef,
        elseValue: LlvmValueRef,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildSelect(builder.address, condition.address, thenValue.address, elseValue.address, name),
    )

    /**
     * 构造 extractvalue 指令。
     */
    override fun builderBuildExtractValue(
        builder: LlvmBuilderRef,
        aggregate: LlvmValueRef,
        index: Int,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildExtractValue(builder.address, aggregate.address, index, name),
    )

    /**
     * 构造 insertvalue 指令。
     */
    override fun builderBuildInsertValue(
        builder: LlvmBuilderRef,
        aggregate: LlvmValueRef,
        element: LlvmValueRef,
        index: Int,
        name: String,
    ): LlvmValueRef = LlvmValueRef(
        LlvmNative.builderBuildInsertValue(builder.address, aggregate.address, element.address, index, name),
    )

    /**
     * 将会抛出异常的 JNI 校验调用转换为结构化校验结果。
     */
    private fun verificationResult(verify: () -> Unit): LlvmVerificationResult =
        try {
            verify()
            LlvmVerificationResult(ok = true)
        } catch (error: LlvmException) {
            LlvmVerificationResult(ok = false, message = error.message)
        }

    /**
     * 将 Kotlin 整数比较谓词映射为 LLVM C API 枚举整数。
     */
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

    /**
     * 将 Kotlin 浮点比较谓词映射为 LLVM C API 枚举整数。
     */
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

    /**
     * 将 Kotlin 优化级别映射为 JNI 原生枚举整数。
     */
    private fun LlvmCodeGenOptimizationLevel.toNativeLevel(): Int =
        when (this) {
            LlvmCodeGenOptimizationLevel.NONE -> 0
            LlvmCodeGenOptimizationLevel.LESS -> 1
            LlvmCodeGenOptimizationLevel.DEFAULT -> 2
            LlvmCodeGenOptimizationLevel.AGGRESSIVE -> 3
        }

    /**
     * 将 Kotlin 重定位模式映射为 JNI 原生枚举整数。
     */
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

    /**
     * 将 Kotlin code model 映射为 JNI 原生枚举整数。
     */
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

    /**
     * 将 LLVM type 句柄列表转换为原生地址数组。
     */
    private fun List<LlvmTypeRef>.toTypeAddressArray(): LongArray = LongArray(size) { index -> this[index].address }

    /**
     * 将 LLVM value 句柄列表转换为原生地址数组。
     */
    private fun List<LlvmValueRef>.toValueAddressArray(): LongArray = LongArray(size) { index -> this[index].address }
}
