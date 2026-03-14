package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.api.LlvmBackendKind
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendMissingSymbolsException
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendUnavailableException
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendVersionMismatchException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LlvmBackendFactoryTest {
    @Test
    fun `fallbacks to in-memory backend when interop tool is unavailable in non-strict mode`() {
        val factory = LlvmBackendFactory(
            toolRunner = FakeNativeInteropToolRunner(
                probeError = LlvmBackendUnavailableException("tool missing"),
            ),
        )

        val backend = factory.createAndInitialize(
            CodegenOptions(
                llvmBackendKind = LlvmBackendKind.NATIVE_INTEROP,
                nativeInteropFailOnUnavailable = false,
            ),
        )

        assertEquals("in-memory", backend.id)
    }

    @Test
    fun `throws when interop tool is unavailable in strict mode`() {
        val factory = LlvmBackendFactory(
            toolRunner = FakeNativeInteropToolRunner(
                probeError = LlvmBackendUnavailableException("tool missing"),
            ),
        )

        assertThrows<LlvmBackendUnavailableException> {
            factory.createAndInitialize(
                CodegenOptions(
                    llvmBackendKind = LlvmBackendKind.NATIVE_INTEROP,
                    nativeInteropFailOnUnavailable = true,
                ),
            )
        }
    }

    @Test
    fun `throws on llvm version mismatch`() {
        val backend = NativeInteropLlvmBackendApi(
            tool = "fake-tool",
            requiredMajorVersion = 18,
            runner = FakeNativeInteropToolRunner(
                probeResult = NativeInteropProbeResult(
                    llvmVersion = "17.0.6",
                    symbols = defaultSymbols,
                ),
            ),
        )

        val error = assertThrows<LlvmBackendVersionMismatchException> { backend.initialize() }
        assertEquals(18, error.expectedMajor)
        assertEquals("17.0.6", error.actualVersion)
    }

    @Test
    fun `throws on missing required symbols`() {
        val backend = NativeInteropLlvmBackendApi(
            tool = "fake-tool",
            requiredMajorVersion = 18,
            runner = FakeNativeInteropToolRunner(
                probeResult = NativeInteropProbeResult(
                    llvmVersion = "18.1.0",
                    symbols = setOf("LLVMGetVersion"),
                ),
            ),
        )

        val error = assertThrows<LlvmBackendMissingSymbolsException> { backend.initialize() }
        assertTrue(error.symbols.contains("LLVMContextCreate"))
    }

    @Test
    fun `uses interop backend for bitcode emission when probe passes`() {
        val backend = NativeInteropLlvmBackendApi(
            tool = "fake-tool",
            requiredMajorVersion = 18,
            runner = FakeNativeInteropToolRunner(
                probeResult = NativeInteropProbeResult(
                    llvmVersion = "18.0.0",
                    symbols = defaultSymbols,
                ),
                bitcodeResult = byteArrayOf(0x42, 0x43),
            ),
        )

        backend.initialize()
        val bitcode = backend.emitBitcode("sample", "define i32 @main() { ret i32 0 }")
        assertTrue(bitcode.contentEquals(byteArrayOf(0x42, 0x43)))
    }

    private class FakeNativeInteropToolRunner(
        private val probeResult: NativeInteropProbeResult? = null,
        private val probeError: Throwable? = null,
        private val bitcodeResult: ByteArray = byteArrayOf(),
    ) : NativeInteropToolRunner {
        override fun probe(tool: String): NativeInteropProbeResult {
            probeError?.let { throw it }
            return probeResult ?: error("probeResult is required when probeError is null")
        }

        override fun emitBitcode(tool: String, moduleName: String, llvmIr: String): ByteArray = bitcodeResult
    }

    private companion object {
        val defaultSymbols = setOf(
            "LLVMGetVersion",
            "LLVMContextCreate",
            "LLVMModuleCreateWithName",
            "LLVMAddFunction",
            "LLVMPrintModuleToString",
        )
    }
}
