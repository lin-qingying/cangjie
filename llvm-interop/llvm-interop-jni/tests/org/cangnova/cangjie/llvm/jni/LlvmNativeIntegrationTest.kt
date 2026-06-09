package org.cangnova.cangjie.llvm.jni

import org.cangnova.cangjie.llvm.api.LlvmContext
import org.cangnova.cangjie.llvm.api.LlvmCodeGenOptimizationLevel
import org.cangnova.cangjie.llvm.api.LlvmPassPipeline
import org.cangnova.cangjie.llvm.api.LlvmPassManagers
import org.cangnova.cangjie.llvm.api.LlvmTargetMachineOptions
import org.cangnova.cangjie.llvm.api.LlvmTargetMachines
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * LLVM JNI 集成测试。
 */
class LlvmNativeIntegrationTest {
    companion object {
        private const val ENABLE_FLAG = "CANGJIE_LLVM_JNI_INTEGRATION"
        private const val LIB_PATH_FLAG = "CANGJIE_LLVM_JNI_LIBRARY_PATH"
        private const val NATIVE_HOME_FLAG = "CANGJIE_NATIVE_HOME"
        private const val ENABLE_PROP = "cangjie.llvm.jni.integration"
        private const val NATIVE_HOME_PROP = "cangjie.native.home"

        @JvmStatic
        @BeforeAll
        fun setupNativePath() {
            assumeTrue(
                System.getenv(ENABLE_FLAG).equals("true", ignoreCase = true)
                    || System.getProperty(ENABLE_PROP).equals("true", ignoreCase = true),
                "integration test disabled, set $ENABLE_FLAG=true or -D$ENABLE_PROP=true",
            )
            val libraryPath = System.getenv(LIB_PATH_FLAG).orEmpty()
            if (libraryPath.isNotBlank()) {
                System.setProperty("cangjie.llvm.native.library.path", libraryPath)
                return
            }

            val nativeHome = System.getenv(NATIVE_HOME_FLAG)
                ?: System.getProperty(NATIVE_HOME_PROP).orEmpty()
            assumeTrue(
                nativeHome.isNotBlank(),
                "missing $LIB_PATH_FLAG or $NATIVE_HOME_FLAG or -D$NATIVE_HOME_PROP",
            )
            System.setProperty(NATIVE_HOME_PROP, nativeHome)
        }
    }

    @Test
    fun `can build function body through public llvm api binding`() {
        assertTrue(LlvmNative.isAvailable, LlvmNative.loadDiagnostics)
        LlvmNative.installBindings()

        LlvmContext().use { context ->
            val module = context.createModule("api_binding_module")
            val int32Type = context.int32Type
            val functionType = context.functionType(int32Type, emptyList())
            val function = module.addFunction("main", functionType)
            val entry = module.appendBasicBlock(function, "entry")

            context.createBuilder().use { builder ->
                builder.positionAtEnd(entry)
                builder.buildRet(context.constInt(int32Type, 42))
            }

            module.verifyFunction(function)
            module.verify()
            val ir = module.irText()
            assertTrue(ir.contains("define i32 @main()"), ir)
            assertTrue(ir.contains("ret i32 42"), ir)
            assertEquals("main", module.valueName(function))

            val bitcode = module.bitcodeBytes()
            assertFalse(bitcode.isEmpty())
        }
    }

    @Test
    fun `can emit object file through public target machine binding`() {
        assertTrue(LlvmNative.isAvailable, LlvmNative.loadDiagnostics)
        LlvmNative.installBindings()
        LlvmTargetMachines.initializeAll()
        val targetTriple = LlvmTargetMachines.defaultTriple()
        assertTrue(targetTriple.isNotBlank())

        LlvmContext().use { context ->
            val module = context.createModule("object_emission_module")
            module.setTargetTriple(targetTriple)
            val int32Type = context.int32Type
            val function = module.addFunction("main", context.functionType(int32Type, emptyList()))
            val entry = module.appendBasicBlock(function, "entry")
            context.createBuilder().use { builder ->
                builder.positionAtEnd(entry)
                builder.buildRet(context.constInt(int32Type, 42))
            }
            module.verify()

            val output = Files.createTempFile("llvm-jni-object-", ".o")
            LlvmTargetMachines.create(LlvmTargetMachineOptions(targetTriple = targetTriple)).use { targetMachine ->
                targetMachine.emitObjectFile(module, output.toString())
                val objectBytes = targetMachine.emitObjectBytes(module)
                assertTrue(objectBytes.isNotEmpty())
            }

            assertTrue(Files.exists(output))
            assertTrue(Files.size(output) > 0)
        }
    }

    @Test
    fun `can run module pass pipeline through public llvm api binding`() {
        assertTrue(LlvmNative.isAvailable, LlvmNative.loadDiagnostics)
        LlvmNative.installBindings()

        LlvmContext().use { context ->
            val module = context.createModule("pass_pipeline_module")
            val int32Type = context.int32Type
            val function = module.addFunction("main", context.functionType(int32Type, emptyList()))
            val entry = module.appendBasicBlock(function, "entry")
            context.createBuilder().use { builder ->
                builder.positionAtEnd(entry)
                builder.buildRet(context.constInt(int32Type, 42))
            }

            module.verify()
            LlvmPassManagers.createModulePassManager(
                LlvmPassPipeline.defaultOptimization(LlvmCodeGenOptimizationLevel.LESS),
            ).use { passManager ->
                passManager.run(module)
            }
            module.verify()
            assertTrue(module.bitcodeBytes().isNotEmpty())
        }
    }

    @Test
    fun `can create and verify module through JNI bridge`() {
        assertTrue(LlvmNative.isAvailable, LlvmNative.loadDiagnostics)
        var context = 0L
        var module = 0L
        try {
            context = LlvmNative.contextCreate()
            assertNotEquals(0L, context)

            module = LlvmNative.moduleCreateInContext("integration_test_module", context)
            assertNotEquals(0L, module)

            val int32Type = LlvmNative.intTypeInContext(context, 32)
            val fnType = LlvmNative.functionType(int32Type, longArrayOf(), false)
            val function = LlvmNative.moduleAddFunction(module, "main", fnType)
            assertNotEquals(0L, function)

            LlvmNative.moduleVerify(module)
            val ir = LlvmNative.modulePrintToString(module)
            assertTrue(ir.contains("@main"))

            val bitcode = LlvmNative.writeBitcodeToMemoryBuffer(module)
            assertFalse(bitcode.isEmpty())
            assertEquals("main", LlvmNative.valueGetName(function))
        } finally {
            if (module != 0L) {
                LlvmNative.moduleDispose(module)
            }
            if (context != 0L) {
                LlvmNative.contextDispose(context)
            }
        }
    }

    @Test
    fun `can generate llvm ir file through JNI bridge`() {
        assertTrue(LlvmNative.isAvailable, LlvmNative.loadDiagnostics)
        var context = 0L
        var module = 0L
        try {
            context = LlvmNative.contextCreate()
            assertNotEquals(0L, context)

            module = LlvmNative.moduleCreateInContext("ir_file_generation_module", context)
            assertNotEquals(0L, module)

            val int32Type = LlvmNative.intTypeInContext(context, 32)
            val fnType = LlvmNative.functionType(int32Type, longArrayOf(), false)
            LlvmNative.moduleAddFunction(module, "main", fnType)

            LlvmNative.moduleVerify(module)
            val ir = LlvmNative.modulePrintToString(module)
            val output = Files.createTempFile("llvm-jni-", ".ll")
            Files.writeString(output, ir)

            assertTrue(Files.exists(output))
            assertTrue(Files.size(output) > 0)
        } finally {
            if (module != 0L) {
                LlvmNative.moduleDispose(module)
            }
            if (context != 0L) {
                LlvmNative.contextDispose(context)
            }
        }
    }

    @Test
    fun `can parse llvm assembly and emit bitcode`() {
        assertTrue(LlvmNative.isAvailable, LlvmNative.loadDiagnostics)
        var context = 0L
        var module = 0L
        try {
            context = LlvmNative.contextCreate()
            val llvmIr = """
                define i32 @main() {
                entry:
                  ret i32 42
                }
            """.trimIndent()
            module = LlvmNative.moduleParseAssemblyInContext("parse_test", llvmIr, context)
            assertNotEquals(0L, module)
            LlvmNative.moduleVerify(module)
            val bitcode = LlvmNative.writeBitcodeToMemoryBuffer(module)
            assertTrue(bitcode.size >= 4)
        } finally {
            if (module != 0L) {
                LlvmNative.moduleDispose(module)
            }
            if (context != 0L) {
                LlvmNative.contextDispose(context)
            }
        }
    }
}
