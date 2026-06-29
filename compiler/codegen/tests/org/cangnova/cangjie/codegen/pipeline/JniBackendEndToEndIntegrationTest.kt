package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.api.LlvmBackendKind
import org.cangnova.cangjie.codegen.backend.JniLlvmBackend
import org.cangnova.cangjie.codegen.backend.LlvmBackendEmissionOptions
import org.cangnova.cangjie.llvm.api.LlvmTargetMachines
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * JNI LLVM 后端端到端发射路径的集成测试。
 */
class JniBackendEndToEndIntegrationTest {
    /**
     * 测试启用开关和原生库路径配置。
     */
    companion object {
        /**
         * 启用 JNI 集成测试的环境变量。
         */
        private const val ENABLE_FLAG = "CANGJIE_LLVM_JNI_INTEGRATION"
        /**
         * 指定 JNI 原生库路径的环境变量。
         */
        private const val LIB_PATH_FLAG = "CANGJIE_LLVM_JNI_LIBRARY_PATH"
        /**
         * 指定原生工具链根目录的环境变量。
         */
        private const val NATIVE_HOME_FLAG = "CANGJIE_NATIVE_HOME"
        /**
         * 启用 JNI 集成测试的系统属性。
         */
        private const val ENABLE_PROP = "cangjie.llvm.jni.integration"
        /**
         * 指定原生工具链根目录的系统属性。
         */
        private const val NATIVE_HOME_PROP = "cangjie.native.home"

        /**
         * 根据环境变量或系统属性配置 JNI 原生库路径。
         */
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

    /**
     * 验证 JNI 后端可以为简单 CHIR module 生成稳定 LLVM IR。
     */
    @Test
    fun `jni backend produces stable ir for simple chir module`() {
        val codegen = DefaultChirToLlvmCodeGenerator()
        val chirPackage = simpleReturnPackage()

        val jniOutputFirst = codegen.generate(
            ChirCodegenInput(
                chirPackage = chirPackage,
                options = CodegenOptions(
                    enabled = true,
                    llvmBackendKind = LlvmBackendKind.JNI,
                    failOnUnavailable = true,
                    emitLoweringTrace = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val jniOutputSecond = codegen.generate(
            ChirCodegenInput(
                chirPackage = chirPackage,
                options = CodegenOptions(
                    enabled = true,
                    llvmBackendKind = LlvmBackendKind.JNI,
                    failOnUnavailable = true,
                    emitLoweringTrace = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        assertTrue(jniOutputFirst.loweringTrace.any { it == "backend=jni" }, jniOutputFirst.loweringTrace.joinToString("\n"))
        assertTrue(jniOutputSecond.loweringTrace.any { it == "backend=jni" }, jniOutputSecond.loweringTrace.joinToString("\n"))
        assertEquals(1, jniOutputFirst.modules.size)
        assertEquals(1, jniOutputSecond.modules.size)
        assertEquals(jniOutputSecond.modules.single().ir, jniOutputFirst.modules.single().ir)
    }

    /**
     * 验证 JNI 后端可以从 LLVM IR 生成真实 bitcode 字节。
     */
    @Test
    fun `jni backend emits real llvm bitcode bytes`() {
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = simpleReturnPackage(),
                options = CodegenOptions(
                    enabled = true,
                    llvmBackendKind = LlvmBackendKind.JNI,
                    failOnUnavailable = true,
                    emitBitcode = true,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val bitcode = output.modules.single().bitcode ?: error("missing bitcode bytes")
        assertTrue(bitcode.size >= 4)
        assertEquals(0x42.toByte(), bitcode[0])
        assertEquals(0x43.toByte(), bitcode[1])
        assertEquals(0xC0.toByte(), bitcode[2])
        assertEquals(0xDE.toByte(), bitcode[3])
    }

    /**
     * 验证 JNI 后端可以从生成的 CHIR LLVM IR 写出 object file。
     */
    @Test
    fun `jni backend emits object file from generated chir llvm ir`() {
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = simpleReturnPackage(),
                options = CodegenOptions(
                    enabled = true,
                    llvmBackendKind = LlvmBackendKind.JNI,
                    failOnUnavailable = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val backend = JniLlvmBackend()
        backend.initialize()
        LlvmTargetMachines.initializeAll()
        val targetTriple = LlvmTargetMachines.defaultTriple()
        val outputPath = Files.createTempFile("cangjie-codegen-", ".o")
        val module = output.modules.single()
        backend.emitObjectFile(
            module.name,
            module.ir,
            LlvmBackendEmissionOptions(targetTriple = targetTriple),
            outputPath.toString(),
        )

        assertTrue(Files.exists(outputPath))
        assertTrue(Files.size(outputPath) > 0)
    }

    /**
     * 验证完整 codegen pipeline 能通过 JNI 后端生成 object code 字节。
     */
    @Test
    fun `jni codegen pipeline emits object code bytes`() {
        JniLlvmBackend().initialize()
        LlvmTargetMachines.initializeAll()
        val targetTriple = LlvmTargetMachines.defaultTriple()

        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = simpleReturnPackage(),
                options = CodegenOptions(
                    enabled = true,
                    llvmBackendKind = LlvmBackendKind.JNI,
                    failOnUnavailable = true,
                    emitBitcode = false,
                    emitObjectCode = true,
                    optimizeLlvmModule = true,
                    targetTriple = targetTriple,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val objectCode = output.modules.single().objectCode ?: error("missing object code bytes")
        assertTrue(objectCode.isNotEmpty())
    }

    /**
     * 构造返回常量的最小 CHIR package。
     */
    private fun simpleReturnPackage(): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:main"),
            name = "main",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:forty-two"),
                            type = intType,
                            literal = "42",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        return ChirPackage(
            semanticId = ChirSemanticId("pkg:jni-e2e"),
            name = "jni-e2e",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:jni-e2e"),
                    name = "jni-e2e",
                    declarations = listOf(function),
                ),
            ),
        )
    }
}
