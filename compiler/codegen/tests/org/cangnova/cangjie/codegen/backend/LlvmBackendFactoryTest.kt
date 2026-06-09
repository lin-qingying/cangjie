package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.api.LlvmBackendKind
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendUnavailableException
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendVersionMismatchException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LlvmBackendFactoryTest {
    @Test
    fun `returns jni backend with default options when available`() {
        val factory = LlvmBackendFactoryForTest(
            jniBackend = JniLlvmBackend(
                native = FakeNativeFacade(
                    available = true,
                    version = "18.1.0",
                ),
            ),
        )

        val backend = factory.createAndInitialize(CodegenOptions())
        assertEquals("jni", backend.id)
        assertEquals(true, backend.capabilities.supportsInProcessIR)
        assertEquals(true, backend.capabilities.supportsOptimization)
        assertEquals(true, backend.capabilities.supportsTargetCodegen)
    }

    @Test
    fun `throws when jni is unavailable even in non-strict mode`() {
        val factory = LlvmBackendFactoryForTest(
            jniBackend = JniLlvmBackend(
                native = FakeNativeFacade(
                    available = false,
                    diagnosticsMessage = "not found",
                ),
            ),
        )

        assertThrows<LlvmBackendUnavailableException> {
            factory.createAndInitialize(
            CodegenOptions(
                llvmBackendKind = LlvmBackendKind.JNI,
                failOnUnavailable = false,
            ),
            )
        }
    }

    @Test
    fun `throws when jni is unavailable in strict mode`() {
        val factory = LlvmBackendFactoryForTest(
            jniBackend = JniLlvmBackend(
                native = FakeNativeFacade(
                    available = false,
                    diagnosticsMessage = "missing native library",
                ),
            ),
        )

        assertThrows<LlvmBackendUnavailableException> {
            factory.createAndInitialize(
                CodegenOptions(
                    llvmBackendKind = LlvmBackendKind.JNI,
                    failOnUnavailable = true,
                ),
            )
        }
    }

    @Test
    fun `strict ir only mode initializes jni backend`() {
        val native = FakeNativeFacade(
            available = true,
            version = null,
        )
        val factory = LlvmBackendFactoryForTest(
            jniBackend = JniLlvmBackend(native = native),
        )

        val backend = factory.createAndInitialize(
            CodegenOptions(
                llvmBackendKind = LlvmBackendKind.JNI,
                failOnUnavailable = true,
                emitBitcode = false,
            ),
        )

        assertEquals("jni", backend.id)
        assertEquals(true, native.apiBindingsInstalled)
    }

    @Test
    fun `object emission initializes jni backend and checks version`() {
        val native = FakeNativeFacade(
            available = true,
            version = "18.1.0",
        )
        val factory = LlvmBackendFactoryForTest(
            jniBackend = JniLlvmBackend(native = native),
        )

        val backend = factory.createAndInitialize(
            CodegenOptions(
                emitBitcode = false,
                emitObjectCode = true,
            ),
        )

        assertEquals("jni", backend.id)
        assertEquals(true, native.apiBindingsInstalled)
    }

    @Test
    fun `object emission checks llvm major version`() {
        val factory = LlvmBackendFactoryForTest(
            jniBackend = JniLlvmBackend(
                native = FakeNativeFacade(
                    available = true,
                    version = "17.0.6",
                ),
            ),
        )

        val error = assertThrows<LlvmBackendVersionMismatchException> {
            factory.createAndInitialize(
                CodegenOptions(
                    emitBitcode = false,
                    emitObjectCode = true,
                    requiredLlvmMajorVersion = 18,
                ),
            )
        }
        assertEquals(18, error.expectedMajor)
        assertEquals("17.0.6", error.actualVersion)
    }


    @Test
    fun `throws on llvm major version mismatch`() {
        val factory = LlvmBackendFactoryForTest(
            jniBackend = JniLlvmBackend(
                native = FakeNativeFacade(
                    available = true,
                    version = "17.0.6",
                ),
            ),
        )

        val error = assertThrows<LlvmBackendVersionMismatchException> {
            factory.createAndInitialize(
                CodegenOptions(
                    llvmBackendKind = LlvmBackendKind.JNI,
                    failOnUnavailable = true,
                    requiredLlvmMajorVersion = 18,
                ),
            )
        }
        assertEquals(18, error.expectedMajor)
        assertEquals("17.0.6", error.actualVersion)
    }

    @Test
    fun `throws when llvm version is missing after initialization`() {
        val factory = LlvmBackendFactoryForTest(
            jniBackend = JniLlvmBackend(
                native = FakeNativeFacade(
                    available = true,
                    version = null,
                ),
            ),
        )

        val error = assertThrows<LlvmBackendVersionMismatchException> {
            factory.createAndInitialize(
                CodegenOptions(
                    llvmBackendKind = LlvmBackendKind.JNI,
                    requiredLlvmMajorVersion = 18,
                ),
            )
        }
        assertEquals(18, error.expectedMajor)
        assertEquals("<missing>", error.actualVersion)
    }

    @Test
    fun `throws when llvm version cannot be parsed`() {
        val factory = LlvmBackendFactoryForTest(
            jniBackend = JniLlvmBackend(
                native = FakeNativeFacade(
                    available = true,
                    version = "LLVM-current",
                ),
            ),
        )

        val error = assertThrows<LlvmBackendVersionMismatchException> {
            factory.createAndInitialize(
                CodegenOptions(
                    llvmBackendKind = LlvmBackendKind.JNI,
                    requiredLlvmMajorVersion = 18,
                ),
            )
        }
        assertEquals(18, error.expectedMajor)
        assertEquals("LLVM-current", error.actualVersion)
    }

    private class LlvmBackendFactoryForTest(
        private val jniBackend: LlvmBackend,
    ) : LlvmBackendFactory() {
        override fun createJniBackend(): LlvmBackend = jniBackend
    }

    private class FakeNativeFacade(
        private val available: Boolean,
        private val diagnosticsMessage: String = "",
        private val version: String? = null,
    ) : JniNativeFacade {
        var apiBindingsInstalled: Boolean = false

        override val isAvailable: Boolean
            get() = available

        override val diagnostics: String
            get() = diagnosticsMessage

        override val llvmVersion: String?
            get() = version

        override fun installApiBindings() {
            apiBindingsInstalled = true
        }

        override fun emitBitcode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray =
            llvmIr.toByteArray()

        override fun emitObjectCode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray =
            llvmIr.toByteArray()

        override fun emitObjectFile(
            moduleName: String,
            llvmIr: String,
            options: LlvmBackendEmissionOptions,
            outputPath: String,
        ) = Unit
    }
}
