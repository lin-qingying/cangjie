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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class JniBackendEndToEndIntegrationTest {
    companion object {
        private const val ENABLE_FLAG = "CANGJIE_LLVM_JNI_INTEGRATION"
        private const val LIB_PATH_FLAG = "CANGJIE_LLVM_JNI_LIBRARY_PATH"

        @JvmStatic
        @BeforeAll
        fun setupNativePath() {
            assumeTrue(
                System.getenv(ENABLE_FLAG).equals("true", ignoreCase = true),
                "integration test disabled, set $ENABLE_FLAG=true to enable",
            )
            val libraryPath = System.getenv(LIB_PATH_FLAG).orEmpty()
            assumeTrue(libraryPath.isNotBlank(), "missing $LIB_PATH_FLAG")
            System.setProperty("cangjie.llvm.native.library.path", libraryPath)
        }
    }

    @Test
    fun `jni backend produces same ir as in memory backend for simple chir module`() {
        val codegen = DefaultChirToLlvmCodeGenerator()
        val chirPackage = simpleReturnPackage()

        val jniOutput = codegen.generate(
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

        val inMemoryOutput = codegen.generate(
            ChirCodegenInput(
                chirPackage = chirPackage,
                options = CodegenOptions(
                    enabled = true,
                    llvmBackendKind = LlvmBackendKind.IN_MEMORY,
                    emitLoweringTrace = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        assertTrue(jniOutput.loweringTrace.any { it == "backend=jni" }, jniOutput.loweringTrace.joinToString("\n"))
        assertTrue(inMemoryOutput.loweringTrace.any { it == "backend=in-memory" }, inMemoryOutput.loweringTrace.joinToString("\n"))
        assertEquals(1, jniOutput.modules.size)
        assertEquals(1, inMemoryOutput.modules.size)
        assertEquals(inMemoryOutput.modules.single().ir, jniOutput.modules.single().ir)
    }

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
