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
    fun `returns in-memory backend when configured as in-memory`() {
        val factory = LlvmBackendFactory()

        val backend = factory.createAndInitialize(
            CodegenOptions(
                llvmBackendKind = LlvmBackendKind.IN_MEMORY,
            ),
        )

        assertEquals("in-memory", backend.id)
    }

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
    }

    @Test
    fun `fallbacks to in-memory when jni is unavailable and non-strict`() {
        val factory = LlvmBackendFactoryForTest(
            jniBackend = JniLlvmBackend(
                native = FakeNativeFacade(
                    available = false,
                    diagnosticsMessage = "not found",
                ),
            ),
        )

        val backend = factory.createAndInitialize(
            CodegenOptions(
                llvmBackendKind = LlvmBackendKind.JNI,
                failOnUnavailable = false,
            ),
        )
        assertEquals("in-memory", backend.id)
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
        override val isAvailable: Boolean
            get() = available

        override val diagnostics: String
            get() = diagnosticsMessage

        override val llvmVersion: String?
            get() = version

        override fun emitBitcode(moduleName: String, llvmIr: String): ByteArray = llvmIr.toByteArray()
    }
}
