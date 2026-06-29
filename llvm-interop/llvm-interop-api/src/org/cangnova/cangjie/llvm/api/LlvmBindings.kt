package org.cangnova.cangjie.llvm.api

/**
 * LLVM 整数比较谓词。
 */
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

/**
 * LLVM 浮点比较谓词。
 */
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

/**
 * PHI 指令的一条 incoming 分支信息。
 */
data class LlvmPhiIncoming(
    /**
     * 从 [block] 流入 PHI 的值。
     */
    val value: LlvmValueRef,
    /**
     * 产生 incoming 值的前驱基本块。
     */
    val block: LlvmBasicBlockRef,
)

/**
 * 模块校验结果。
 */
data class LlvmVerificationResult(
    /**
     * 校验是否通过。
     */
    val ok: Boolean,
    /**
     * 校验失败时返回的 LLVM 诊断文本。
     */
    val message: String? = null,
)

/**
 * LLVM 绑定抽象层。
 *
 * 由不同后端实现（如 JNI）提供具体能力。
 */
interface LlvmBindings {
    /**
     * 初始化 LLVM 支持的所有 target/backend。
     */
    fun targetInitializeAll() = unsupportedUnit()
    /**
     * 获取当前机器默认目标三元组。
     */
    fun targetDefaultTriple(): String = unsupported()
    /**
     * 按选项创建 LLVMTargetMachine。
     */
    fun targetCreateMachine(options: LlvmTargetMachineOptions): LlvmTargetMachineRef = unsupported()
    /**
     * 释放 LLVMTargetMachine。
     */
    fun targetDisposeMachine(machine: LlvmTargetMachineRef) = unsupportedUnit()
    /**
     * 将模块通过目标机器写出为目标文件。
     */
    fun targetMachineEmitObjectFile(machine: LlvmTargetMachineRef, module: LlvmModuleRef, outputPath: String) = unsupportedUnit()
    /**
     * 将模块通过目标机器写出为目标文件字节。
     */
    fun targetMachineEmitObjectBytes(machine: LlvmTargetMachineRef, module: LlvmModuleRef): ByteArray = unsupported()

    /**
     * 创建 LLVMContext。
     */
    fun contextCreate(): LlvmContextRef = unsupported()
    /**
     * 释放 LLVMContext。
     */
    fun contextDispose(context: LlvmContextRef) = unsupportedUnit()

    /**
     * 在指定上下文中创建模块。
     */
    fun moduleCreateInContext(name: String, context: LlvmContextRef): LlvmModuleRef = unsupported()
    /**
     * 释放 LLVMModule。
     */
    fun moduleDispose(module: LlvmModuleRef) = unsupportedUnit()
    /**
     * 设置模块目标三元组。
     */
    fun moduleSetTargetTriple(module: LlvmModuleRef, targetTriple: String) = unsupportedUnit()
    /**
     * 设置模块数据布局。
     */
    fun moduleSetDataLayout(module: LlvmModuleRef, dataLayout: String) = unsupportedUnit()
    /**
     * 向模块添加函数。
     */
    fun moduleAddFunction(module: LlvmModuleRef, name: String, functionType: LlvmTypeRef): LlvmValueRef = unsupported()
    /**
     * 向模块添加全局变量。
     */
    fun moduleAddGlobal(module: LlvmModuleRef, type: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 在上下文中解析 LLVM assembly 为模块。
     */
    fun moduleParseAssemblyInContext(name: String, assembly: String, context: LlvmContextRef): LlvmModuleRef = unsupported()
    /**
     * 将模块打印为 LLVM IR 文本。
     */
    fun moduleToString(module: LlvmModuleRef): String = unsupported()
    /**
     * 校验模块。
     */
    fun moduleVerify(module: LlvmModuleRef): LlvmVerificationResult = unsupported()
    /**
     * 将模块写出为 bitcode 字节。
     */
    fun moduleWriteBitcodeToMemoryBuffer(module: LlvmModuleRef): ByteArray = unsupported()
    /**
     * 将模块 bitcode 写入文件。
     */
    fun moduleWriteBitcodeToFile(module: LlvmModuleRef, outputPath: String): Int = unsupported()
    /**
     * 在模块上运行 pass pipeline。
     */
    fun moduleRunPasses(module: LlvmModuleRef, passPipeline: String, targetMachine: LlvmTargetMachineRef) = unsupportedUnit()
    /**
     * 向函数追加基本块。
     */
    fun functionAppendBasicBlock(function: LlvmValueRef, name: String): LlvmBasicBlockRef = unsupported()
    /**
     * 校验函数。
     */
    fun functionVerify(function: LlvmValueRef): LlvmVerificationResult = unsupported()

    /**
     * 在上下文中创建指定 bit 宽度的整数类型。
     */
    fun contextIntType(context: LlvmContextRef, bits: Int): LlvmTypeRef = unsupported()
    /**
     * 获取上下文中的 float 类型。
     */
    fun contextFloatType(context: LlvmContextRef): LlvmTypeRef = unsupported()
    /**
     * 获取上下文中的 double 类型。
     */
    fun contextDoubleType(context: LlvmContextRef): LlvmTypeRef = unsupported()
    /**
     * 获取上下文中的 void 类型。
     */
    fun contextVoidType(context: LlvmContextRef): LlvmTypeRef = unsupported()
    /**
     * 获取上下文中的 opaque pointer 类型。
     */
    fun contextPtrType(context: LlvmContextRef): LlvmTypeRef = unsupported()
    /**
     * 创建函数类型。
     */
    fun contextFunctionType(
        returnType: LlvmTypeRef,
        parameterTypes: List<LlvmTypeRef>,
        isVarArg: Boolean,
    ): LlvmTypeRef = unsupported()

    /**
     * 创建具名结构体类型。
     */
    fun contextNamedStructType(context: LlvmContextRef, name: String): LlvmTypeRef = unsupported()
    /**
     * 设置结构体字段类型列表。
     */
    fun structSetBody(type: LlvmTypeRef, elementTypes: List<LlvmTypeRef>, isPacked: Boolean) = unsupportedUnit()
    /**
     * 创建数组类型。
     */
    fun contextArrayType(elementType: LlvmTypeRef, size: Int): LlvmTypeRef = unsupported()
    /**
     * 创建整数常量。
     */
    fun constInt(type: LlvmTypeRef, value: Long, signExtend: Boolean): LlvmValueRef = unsupported()
    /**
     * 创建浮点常量。
     */
    fun constReal(type: LlvmTypeRef, value: Double): LlvmValueRef = unsupported()
    /**
     * 创建指定类型的 null 常量。
     */
    fun constNull(type: LlvmTypeRef): LlvmValueRef = unsupported()
    /**
     * 获取 LLVM value 名称。
     */
    fun valueGetName(value: LlvmValueRef): String = unsupported()
    /**
     * 获取 LLVM value 类型。
     */
    fun valueGetType(value: LlvmValueRef): LlvmTypeRef = unsupported()

    /**
     * 在上下文中创建 IR builder。
     */
    fun builderCreateInContext(context: LlvmContextRef): LlvmBuilderRef = unsupported()
    /**
     * 释放 IR builder。
     */
    fun builderDispose(builder: LlvmBuilderRef) = unsupportedUnit()
    /**
     * 将 builder 定位到基本块末尾。
     */
    fun builderPositionAtEnd(builder: LlvmBuilderRef, block: LlvmBasicBlockRef) = unsupportedUnit()

    /**
     * 构造返回值指令。
     */
    fun builderBuildRet(builder: LlvmBuilderRef, value: LlvmValueRef): LlvmValueRef = unsupported()
    /**
     * 构造 void 返回指令。
     */
    fun builderBuildRetVoid(builder: LlvmBuilderRef): LlvmValueRef = unsupported()
    /**
     * 构造无条件分支指令。
     */
    fun builderBuildBr(builder: LlvmBuilderRef, dest: LlvmBasicBlockRef): LlvmValueRef = unsupported()
    /**
     * 构造条件分支指令。
     */
    fun builderBuildCondBr(
        builder: LlvmBuilderRef,
        cond: LlvmValueRef,
        thenBlock: LlvmBasicBlockRef,
        elseBlock: LlvmBasicBlockRef,
    ): LlvmValueRef = unsupported()

    /**
     * 构造 switch 指令。
     */
    fun builderBuildSwitch(
        builder: LlvmBuilderRef,
        value: LlvmValueRef,
        defaultBlock: LlvmBasicBlockRef,
        cases: List<Pair<LlvmValueRef, LlvmBasicBlockRef>>,
    ): LlvmValueRef = unsupported()

    /**
     * 构造 unreachable 指令。
     */
    fun builderBuildUnreachable(builder: LlvmBuilderRef): LlvmValueRef = unsupported()

    /**
     * 构造整数加法指令。
     */
    fun builderBuildAdd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造整数减法指令。
     */
    fun builderBuildSub(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造整数乘法指令。
     */
    fun builderBuildMul(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造有符号整数除法指令。
     */
    fun builderBuildSDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造无符号整数除法指令。
     */
    fun builderBuildUDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造有符号整数取余指令。
     */
    fun builderBuildSRem(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造无符号整数取余指令。
     */
    fun builderBuildURem(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造浮点取负指令。
     */
    fun builderBuildFNeg(builder: LlvmBuilderRef, value: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造浮点加法指令。
     */
    fun builderBuildFAdd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造浮点减法指令。
     */
    fun builderBuildFSub(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造浮点乘法指令。
     */
    fun builderBuildFMul(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造浮点除法指令。
     */
    fun builderBuildFDiv(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()

    /**
     * 构造按位与指令。
     */
    fun builderBuildAnd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造按位或指令。
     */
    fun builderBuildOr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造按位异或指令。
     */
    fun builderBuildXor(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造左移指令。
     */
    fun builderBuildShl(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造算术右移指令。
     */
    fun builderBuildAShr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造逻辑右移指令。
     */
    fun builderBuildLShr(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef = unsupported()

    /**
     * 构造整数比较指令。
     */
    fun builderBuildICmp(
        builder: LlvmBuilderRef,
        predicate: LlvmIntPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String,
    ): LlvmValueRef = unsupported()

    /**
     * 构造浮点比较指令。
     */
    fun builderBuildFCmp(
        builder: LlvmBuilderRef,
        predicate: LlvmFloatPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String,
    ): LlvmValueRef = unsupported()

    /**
     * 构造 alloca 指令。
     */
    fun builderBuildAlloca(builder: LlvmBuilderRef, type: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造 load 指令。
     */
    fun builderBuildLoad(builder: LlvmBuilderRef, type: LlvmTypeRef, pointer: LlvmValueRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造 store 指令。
     */
    fun builderBuildStore(builder: LlvmBuilderRef, value: LlvmValueRef, pointer: LlvmValueRef): LlvmValueRef = unsupported()
    /**
     * 构造 getelementptr 指令。
     */
    fun builderBuildGep(
        builder: LlvmBuilderRef,
        elementType: LlvmTypeRef,
        pointer: LlvmValueRef,
        indices: List<LlvmValueRef>,
        inBounds: Boolean,
        name: String,
    ): LlvmValueRef = unsupported()

    /**
     * 构造整数截断指令。
     */
    fun builderBuildTrunc(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造零扩展指令。
     */
    fun builderBuildZExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造符号扩展指令。
     */
    fun builderBuildSExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造浮点截断指令。
     */
    fun builderBuildFPTrunc(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造浮点扩展指令。
     */
    fun builderBuildFPExt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造浮点到无符号整数转换指令。
     */
    fun builderBuildFPToUI(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造浮点到有符号整数转换指令。
     */
    fun builderBuildFPToSI(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造无符号整数到浮点转换指令。
     */
    fun builderBuildUIToFP(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造有符号整数到浮点转换指令。
     */
    fun builderBuildSIToFP(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造指针到整数转换指令。
     */
    fun builderBuildPtrToInt(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造整数到指针转换指令。
     */
    fun builderBuildIntToPtr(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 构造 bitcast 指令。
     */
    fun builderBuildBitCast(builder: LlvmBuilderRef, value: LlvmValueRef, targetType: LlvmTypeRef, name: String): LlvmValueRef = unsupported()

    /**
     * 构造 call 指令。
     */
    fun builderBuildCall(
        builder: LlvmBuilderRef,
        function: LlvmValueRef,
        args: List<LlvmValueRef>,
        name: String,
    ): LlvmValueRef = unsupported()

    /**
     * 构造 PHI 指令。
     */
    fun builderBuildPhi(builder: LlvmBuilderRef, type: LlvmTypeRef, name: String): LlvmValueRef = unsupported()
    /**
     * 为 PHI 指令追加 incoming 值。
     */
    fun builderAddIncoming(phi: LlvmValueRef, incoming: List<LlvmPhiIncoming>) = unsupportedUnit()
    /**
     * 构造 select 指令。
     */
    fun builderBuildSelect(
        builder: LlvmBuilderRef,
        condition: LlvmValueRef,
        thenValue: LlvmValueRef,
        elseValue: LlvmValueRef,
        name: String,
    ): LlvmValueRef = unsupported()

    /**
     * 构造 extractvalue 指令。
     */
    fun builderBuildExtractValue(
        builder: LlvmBuilderRef,
        aggregate: LlvmValueRef,
        index: Int,
        name: String,
    ): LlvmValueRef = unsupported()

    /**
     * 构造 insertvalue 指令。
     */
    fun builderBuildInsertValue(
        builder: LlvmBuilderRef,
        aggregate: LlvmValueRef,
        element: LlvmValueRef,
        index: Int,
        name: String,
    ): LlvmValueRef = unsupported()
}

/**
 * 当前进程内的 LLVM 绑定注册表。
 */
internal object LlvmBindingRegistry {
    /**
     * 当前进程正在使用的 LLVM 绑定实现。
     */
    @Volatile
    var bindings: LlvmBindings = object : LlvmBindings {}
}

/**
 * 安装当前进程使用的 LLVM 绑定实现。
 *
 * 生产后端应在完成原生库加载和版本校验后调用该入口。
 */
fun installLlvmBindings(bindings: LlvmBindings) {
    LlvmBindingRegistry.bindings = bindings
}

/**
 * 在测试范围内临时替换 LLVM 绑定实现。
 */
internal inline fun <T> withLlvmBindingsForTest(bindings: LlvmBindings, block: () -> T): T {
    val previous = LlvmBindingRegistry.bindings
    LlvmBindingRegistry.bindings = bindings
    return try {
        block()
    } finally {
        LlvmBindingRegistry.bindings = previous
    }
}

/**
 * 默认的返回值型未安装绑定错误。
 */
private fun <T> unsupported(): T {
    throw LlvmBackendUnavailableException("LLVM native bindings are not installed")
}

/**
 * 默认的 Unit 型未安装绑定错误。
 */
private fun unsupportedUnit(): Unit {
    throw LlvmBackendUnavailableException("LLVM native bindings are not installed")
}
