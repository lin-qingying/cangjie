package org.cangnova.cangjie.llvm.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicLong

/**
 * LLVM API 契约测试。
 */
class LlvmApiContractTest {
    /**
     * 验证上下文关闭时按 builder、module、context 顺序释放资源。
     */
    @Test
    fun `context use closes builder module and context in order`() {
        val fake = FakeLlvmBindings()

        withLlvmBindingsForTest(fake) {
            LlvmContext().use { context ->
                context.createBuilder()
                context.createModule("m")
            }
        }

        assertEquals(
            listOf("contextCreate", "builderCreate", "moduleCreate:m", "builderDispose", "moduleDispose", "contextDispose"),
            fake.events,
        )
    }

    /**
     * 验证模块级公开操作会完整委托到底层绑定。
     */
    @Test
    fun `module operations delegate to bindings`() {
        val fake = FakeLlvmBindings()

        withLlvmBindingsForTest(fake) {
            LlvmContext().use { context ->
                val module = context.createModule("demo")
                module.setTargetTriple("x86_64-unknown-linux-gnu")
                module.setDataLayout("e-m:e-p:64:64")
                val f = module.addFunction("f", context.functionType(context.int32Type, emptyList()))
                val g = module.addGlobal("g", context.int32Type)
                val entry = module.appendBasicBlock(f, "entry")
                assertTrue(!f.isNull)
                assertTrue(!g.isNull)
                assertTrue(!entry.isNull)
                assertEquals("; fake-ir", module.irText())
                module.verify()
                module.verifyFunction(f)
                assertEquals("f", module.valueName(f))
                assertEquals(context.int32Type, module.valueType(f))
                assertEquals(byteArrayOf(0x42, 0x43).toList(), module.bitcodeBytes().toList())
            }
        }

        assertTrue(fake.events.contains("moduleSetTargetTriple:x86_64-unknown-linux-gnu"))
        assertTrue(fake.events.contains("moduleSetDataLayout:e-m:e-p:64:64"))
        assertTrue(fake.events.any { it.startsWith("moduleAddFunction:f") })
        assertTrue(fake.events.any { it.startsWith("moduleAddGlobal:g") })
        assertTrue(fake.events.contains("functionAppendBasicBlock:entry"))
        assertTrue(fake.events.contains("moduleToString"))
        assertTrue(fake.events.contains("moduleVerify"))
        assertTrue(fake.events.contains("functionVerify"))
        assertTrue(fake.events.contains("moduleWriteBitcodeToMemoryBuffer"))
    }

    /**
     * 验证模块校验失败时公开 API 抛出校验异常。
     */
    @Test
    fun `module verify throws verification exception on invalid module`() {
        val fake = FakeLlvmBindings(
            verificationResult = LlvmVerificationResult(ok = false, message = "broken module"),
        )

        val error = withLlvmBindingsForTest(fake) {
            assertThrows<LlvmVerificationException> {
                LlvmContext().use { context ->
                    context.createModule("broken").verify()
                }
            }
        }

        assertEquals("broken module", error.message)
    }

    /**
     * 验证 builder 核心指令会委托到底层绑定。
     */
    @Test
    fun `builder delegates core instructions to bindings`() {
        val fake = FakeLlvmBindings()

        withLlvmBindingsForTest(fake) {
            LlvmContext().use { context ->
                val builder = context.createBuilder()
                val lhs = LlvmValueRef(11)
                val rhs = LlvmValueRef(12)
                val constant = context.constInt(context.int32Type, 42)
                builder.buildAdd(lhs, rhs, "sum")
                builder.buildICmp(LlvmIntPredicate.EQ, lhs, rhs, "cmp")
                builder.buildRet(constant)
                builder.buildRet(lhs)
                builder.close()
            }
        }

        assertTrue(fake.events.contains("builderBuildAdd:sum"))
        assertTrue(fake.events.contains("builderBuildICmp:EQ:cmp"))
        assertTrue(fake.events.contains("builderBuildRet"))
    }

    /**
     * 验证目标机器的目标文件生成入口会委托到底层绑定。
     */
    @Test
    fun `target machine delegates object file emission to bindings`() {
        val fake = FakeLlvmBindings()

        withLlvmBindingsForTest(fake) {
            LlvmTargetMachines.initializeAll()
            assertEquals("x86_64-test", LlvmTargetMachines.defaultTriple())
            LlvmContext().use { context ->
                val module = context.createModule("target")
                LlvmTargetMachines.create(LlvmTargetMachineOptions(targetTriple = "x86_64-test")).use { targetMachine ->
                    targetMachine.emitObjectFile(module, "target.o")
                    assertEquals(byteArrayOf(0x7F, 0x45).toList(), targetMachine.emitObjectBytes(module).toList())
                }
            }
        }

        assertTrue(fake.events.contains("targetInitializeAll"))
        assertTrue(fake.events.contains("targetCreateMachine:x86_64-test"))
        assertTrue(fake.events.contains("targetMachineEmitObjectFile:target.o"))
        assertTrue(fake.events.contains("targetMachineEmitObjectBytes"))
        assertTrue(fake.events.contains("targetDisposeMachine"))
    }

    /**
     * 验证模块 pass manager 会传递 pass pipeline 和目标机器句柄。
     */
    @Test
    fun `module pass manager delegates pass pipeline to bindings`() {
        val fake = FakeLlvmBindings()

        withLlvmBindingsForTest(fake) {
            LlvmContext().use { context ->
                val module = context.createModule("passes")
                LlvmPassManagers.createModulePassManager(
                    LlvmPassPipeline.defaultOptimization(LlvmCodeGenOptimizationLevel.DEFAULT),
                ).use { passManager ->
                    passManager.run(module)
                }
            }
        }

        assertTrue(fake.events.contains("moduleRunPasses:default<O2>:0"))
    }

    /**
     * 验证 LLVM 异常暴露预期字段和消息。
     */
    @Test
    fun `exceptions expose expected fields`() {
        val mismatch = LlvmVersionMismatchException(expectedMajor = 18, actualVersion = "17.0.6")
        assertEquals(18, mismatch.expectedMajor)
        assertEquals("17.0.6", mismatch.actualVersion)
        assertTrue(mismatch.message!!.contains("expected 18"))
    }
}

/**
 * LLVM 绑定测试替身。
 */
private class FakeLlvmBindings(
    /**
     * 模块和函数校验调用返回的预设结果。
     */
    private val verificationResult: LlvmVerificationResult = LlvmVerificationResult(ok = true),
) : LlvmBindings {
    /**
     * 记录所有被调用的绑定事件。
     */
    val events = mutableListOf<String>()
    /**
     * 生成伪 LLVM 句柄地址的递增计数器。
     */
    private val ids = AtomicLong(1L)

    /**
     * 记录 target 初始化事件。
     */
    override fun targetInitializeAll() {
        events += "targetInitializeAll"
    }

    /**
     * 返回固定测试目标三元组。
     */
    override fun targetDefaultTriple(): String = "x86_64-test"

    /**
     * 记录目标机器创建事件并返回伪句柄。
     */
    override fun targetCreateMachine(options: LlvmTargetMachineOptions): LlvmTargetMachineRef {
        events += "targetCreateMachine:${options.targetTriple}"
        return LlvmTargetMachineRef(ids.getAndIncrement())
    }

    /**
     * 记录目标机器释放事件。
     */
    override fun targetDisposeMachine(machine: LlvmTargetMachineRef) {
        events += "targetDisposeMachine"
    }

    /**
     * 记录目标文件输出路径。
     */
    override fun targetMachineEmitObjectFile(machine: LlvmTargetMachineRef, module: LlvmModuleRef, outputPath: String) {
        events += "targetMachineEmitObjectFile:$outputPath"
    }

    /**
     * 记录目标文件字节输出事件并返回 ELF 头部伪字节。
     */
    override fun targetMachineEmitObjectBytes(machine: LlvmTargetMachineRef, module: LlvmModuleRef): ByteArray {
        events += "targetMachineEmitObjectBytes"
        return byteArrayOf(0x7F, 0x45)
    }

    /**
     * 记录上下文创建事件并返回伪上下文句柄。
     */
    override fun contextCreate(): LlvmContextRef {
        events += "contextCreate"
        return LlvmContextRef(ids.getAndIncrement())
    }

    /**
     * 记录上下文释放事件。
     */
    override fun contextDispose(context: LlvmContextRef) {
        events += "contextDispose"
    }

    /**
     * 记录模块创建事件并返回伪模块句柄。
     */
    override fun moduleCreateInContext(name: String, context: LlvmContextRef): LlvmModuleRef {
        events += "moduleCreate:$name"
        return LlvmModuleRef(ids.getAndIncrement())
    }

    /**
     * 记录模块释放事件。
     */
    override fun moduleDispose(module: LlvmModuleRef) {
        events += "moduleDispose"
    }

    /**
     * 记录目标三元组设置事件。
     */
    override fun moduleSetTargetTriple(module: LlvmModuleRef, targetTriple: String) {
        events += "moduleSetTargetTriple:$targetTriple"
    }

    /**
     * 记录数据布局设置事件。
     */
    override fun moduleSetDataLayout(module: LlvmModuleRef, dataLayout: String) {
        events += "moduleSetDataLayout:$dataLayout"
    }

    /**
     * 记录函数添加事件并返回伪函数句柄。
     */
    override fun moduleAddFunction(module: LlvmModuleRef, name: String, functionType: LlvmTypeRef): LlvmValueRef {
        events += "moduleAddFunction:$name"
        return LlvmValueRef(ids.getAndIncrement())
    }

    /**
     * 记录全局变量添加事件并返回伪 value 句柄。
     */
    override fun moduleAddGlobal(module: LlvmModuleRef, type: LlvmTypeRef, name: String): LlvmValueRef {
        events += "moduleAddGlobal:$name"
        return LlvmValueRef(ids.getAndIncrement())
    }

    /**
     * 记录基本块追加事件并返回伪基本块句柄。
     */
    override fun functionAppendBasicBlock(function: LlvmValueRef, name: String): LlvmBasicBlockRef {
        events += "functionAppendBasicBlock:$name"
        return LlvmBasicBlockRef(ids.getAndIncrement())
    }

    /**
     * 记录模块打印事件并返回伪 IR 文本。
     */
    override fun moduleToString(module: LlvmModuleRef): String {
        events += "moduleToString"
        return "; fake-ir"
    }

    /**
     * 记录模块校验事件并返回预设校验结果。
     */
    override fun moduleVerify(module: LlvmModuleRef): LlvmVerificationResult {
        events += "moduleVerify"
        return verificationResult
    }

    /**
     * 记录 bitcode 内存输出事件并返回伪 bitcode 字节。
     */
    override fun moduleWriteBitcodeToMemoryBuffer(module: LlvmModuleRef): ByteArray {
        events += "moduleWriteBitcodeToMemoryBuffer"
        return byteArrayOf(0x42, 0x43)
    }

    /**
     * 记录 pass pipeline 运行事件。
     */
    override fun moduleRunPasses(module: LlvmModuleRef, passPipeline: String, targetMachine: LlvmTargetMachineRef) {
        events += "moduleRunPasses:$passPipeline:${targetMachine.address}"
    }

    /**
     * 记录函数校验事件并返回预设校验结果。
     */
    override fun functionVerify(function: LlvmValueRef): LlvmVerificationResult {
        events += "functionVerify"
        return verificationResult
    }

    /**
     * 返回按 bit 宽度编码的伪整数类型句柄。
     */
    override fun contextIntType(context: LlvmContextRef, bits: Int): LlvmTypeRef = LlvmTypeRef(1000 + bits.toLong())
    /**
     * 返回伪 float 类型句柄。
     */
    override fun contextFloatType(context: LlvmContextRef): LlvmTypeRef = LlvmTypeRef(2001)
    /**
     * 返回伪 double 类型句柄。
     */
    override fun contextDoubleType(context: LlvmContextRef): LlvmTypeRef = LlvmTypeRef(2002)
    /**
     * 返回伪 void 类型句柄。
     */
    override fun contextVoidType(context: LlvmContextRef): LlvmTypeRef = LlvmTypeRef(2003)
    /**
     * 返回伪 pointer 类型句柄。
     */
    override fun contextPtrType(context: LlvmContextRef): LlvmTypeRef = LlvmTypeRef(2004)

    /**
     * 返回伪函数类型句柄。
     */
    override fun contextFunctionType(
        returnType: LlvmTypeRef,
        parameterTypes: List<LlvmTypeRef>,
        isVarArg: Boolean,
    ): LlvmTypeRef = LlvmTypeRef(3001)

    /**
     * 返回伪具名结构体类型句柄。
     */
    override fun contextNamedStructType(context: LlvmContextRef, name: String): LlvmTypeRef = LlvmTypeRef(3002)
    /**
     * 测试替身中结构体 body 设置为空操作。
     */
    override fun structSetBody(type: LlvmTypeRef, elementTypes: List<LlvmTypeRef>, isPacked: Boolean) = Unit
    /**
     * 返回伪数组类型句柄。
     */
    override fun contextArrayType(elementType: LlvmTypeRef, size: Int): LlvmTypeRef = LlvmTypeRef(3003)
    /**
     * 直接把常量值编码为伪 LLVM value 句柄。
     */
    override fun constInt(type: LlvmTypeRef, value: Long, signExtend: Boolean): LlvmValueRef = LlvmValueRef(value)
    /**
     * 返回固定伪 value 名称。
     */
    override fun valueGetName(value: LlvmValueRef): String = "f"
    /**
     * 返回固定伪 value 类型。
     */
    override fun valueGetType(value: LlvmValueRef): LlvmTypeRef = LlvmTypeRef(1032)

    /**
     * 记录 builder 创建事件并返回伪 builder 句柄。
     */
    override fun builderCreateInContext(context: LlvmContextRef): LlvmBuilderRef {
        events += "builderCreate"
        return LlvmBuilderRef(ids.getAndIncrement())
    }

    /**
     * 记录 builder 释放事件。
     */
    override fun builderDispose(builder: LlvmBuilderRef) {
        events += "builderDispose"
    }

    /**
     * 记录返回指令构造事件。
     */
    override fun builderBuildRet(builder: LlvmBuilderRef, value: LlvmValueRef): LlvmValueRef {
        events += "builderBuildRet"
        return LlvmValueRef(ids.getAndIncrement())
    }

    /**
     * 记录加法指令构造事件。
     */
    override fun builderBuildAdd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef {
        events += "builderBuildAdd:$name"
        return LlvmValueRef(ids.getAndIncrement())
    }

    /**
     * 记录整数比较指令构造事件。
     */
    override fun builderBuildICmp(
        builder: LlvmBuilderRef,
        predicate: LlvmIntPredicate,
        lhs: LlvmValueRef,
        rhs: LlvmValueRef,
        name: String,
    ): LlvmValueRef {
        events += "builderBuildICmp:${predicate.name}:$name"
        return LlvmValueRef(ids.getAndIncrement())
    }
}
