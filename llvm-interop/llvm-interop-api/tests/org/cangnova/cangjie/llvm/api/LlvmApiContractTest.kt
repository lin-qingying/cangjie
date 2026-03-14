package org.cangnova.cangjie.llvm.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicLong

class LlvmApiContractTest {
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
                assertTrue(!f.isNull)
                assertTrue(!g.isNull)
                assertEquals("; fake-ir", module.irText())
                module.verify()
            }
        }

        assertTrue(fake.events.contains("moduleSetTargetTriple:x86_64-unknown-linux-gnu"))
        assertTrue(fake.events.contains("moduleSetDataLayout:e-m:e-p:64:64"))
        assertTrue(fake.events.any { it.startsWith("moduleAddFunction:f") })
        assertTrue(fake.events.any { it.startsWith("moduleAddGlobal:g") })
        assertTrue(fake.events.contains("moduleToString"))
        assertTrue(fake.events.contains("moduleVerify"))
    }

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

    @Test
    fun `builder delegates core instructions to bindings`() {
        val fake = FakeLlvmBindings()

        withLlvmBindingsForTest(fake) {
            LlvmContext().use { context ->
                val builder = context.createBuilder()
                val lhs = LlvmValueRef(11)
                val rhs = LlvmValueRef(12)
                builder.buildAdd(lhs, rhs, "sum")
                builder.buildICmp(LlvmIntPredicate.EQ, lhs, rhs, "cmp")
                builder.buildRet(lhs)
                builder.close()
            }
        }

        assertTrue(fake.events.contains("builderBuildAdd:sum"))
        assertTrue(fake.events.contains("builderBuildICmp:EQ:cmp"))
        assertTrue(fake.events.contains("builderBuildRet"))
    }

    @Test
    fun `exceptions expose expected fields`() {
        val mismatch = LlvmVersionMismatchException(expectedMajor = 18, actualVersion = "17.0.6")
        assertEquals(18, mismatch.expectedMajor)
        assertEquals("17.0.6", mismatch.actualVersion)
        assertTrue(mismatch.message!!.contains("expected 18"))
    }
}

private class FakeLlvmBindings(
    private val verificationResult: LlvmVerificationResult = LlvmVerificationResult(ok = true),
) : LlvmBindings {
    val events = mutableListOf<String>()
    private val ids = AtomicLong(1L)

    override fun contextCreate(): LlvmContextRef {
        events += "contextCreate"
        return LlvmContextRef(ids.getAndIncrement())
    }

    override fun contextDispose(context: LlvmContextRef) {
        events += "contextDispose"
    }

    override fun moduleCreateInContext(name: String, context: LlvmContextRef): LlvmModuleRef {
        events += "moduleCreate:$name"
        return LlvmModuleRef(ids.getAndIncrement())
    }

    override fun moduleDispose(module: LlvmModuleRef) {
        events += "moduleDispose"
    }

    override fun moduleSetTargetTriple(module: LlvmModuleRef, targetTriple: String) {
        events += "moduleSetTargetTriple:$targetTriple"
    }

    override fun moduleSetDataLayout(module: LlvmModuleRef, dataLayout: String) {
        events += "moduleSetDataLayout:$dataLayout"
    }

    override fun moduleAddFunction(module: LlvmModuleRef, name: String, functionType: LlvmTypeRef): LlvmValueRef {
        events += "moduleAddFunction:$name"
        return LlvmValueRef(ids.getAndIncrement())
    }

    override fun moduleAddGlobal(module: LlvmModuleRef, type: LlvmTypeRef, name: String): LlvmValueRef {
        events += "moduleAddGlobal:$name"
        return LlvmValueRef(ids.getAndIncrement())
    }

    override fun moduleToString(module: LlvmModuleRef): String {
        events += "moduleToString"
        return "; fake-ir"
    }

    override fun moduleVerify(module: LlvmModuleRef): LlvmVerificationResult {
        events += "moduleVerify"
        return verificationResult
    }

    override fun contextIntType(context: LlvmContextRef, bits: Int): LlvmTypeRef = LlvmTypeRef(1000 + bits.toLong())
    override fun contextFloatType(context: LlvmContextRef): LlvmTypeRef = LlvmTypeRef(2001)
    override fun contextDoubleType(context: LlvmContextRef): LlvmTypeRef = LlvmTypeRef(2002)
    override fun contextVoidType(context: LlvmContextRef): LlvmTypeRef = LlvmTypeRef(2003)
    override fun contextPtrType(context: LlvmContextRef): LlvmTypeRef = LlvmTypeRef(2004)

    override fun contextFunctionType(
        returnType: LlvmTypeRef,
        parameterTypes: List<LlvmTypeRef>,
        isVarArg: Boolean,
    ): LlvmTypeRef = LlvmTypeRef(3001)

    override fun contextNamedStructType(context: LlvmContextRef, name: String): LlvmTypeRef = LlvmTypeRef(3002)
    override fun structSetBody(type: LlvmTypeRef, elementTypes: List<LlvmTypeRef>, isPacked: Boolean) = Unit
    override fun contextArrayType(elementType: LlvmTypeRef, size: Int): LlvmTypeRef = LlvmTypeRef(3003)

    override fun builderCreateInContext(context: LlvmContextRef): LlvmBuilderRef {
        events += "builderCreate"
        return LlvmBuilderRef(ids.getAndIncrement())
    }

    override fun builderDispose(builder: LlvmBuilderRef) {
        events += "builderDispose"
    }

    override fun builderBuildRet(builder: LlvmBuilderRef, value: LlvmValueRef): LlvmValueRef {
        events += "builderBuildRet"
        return LlvmValueRef(ids.getAndIncrement())
    }

    override fun builderBuildAdd(builder: LlvmBuilderRef, lhs: LlvmValueRef, rhs: LlvmValueRef, name: String): LlvmValueRef {
        events += "builderBuildAdd:$name"
        return LlvmValueRef(ids.getAndIncrement())
    }

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
