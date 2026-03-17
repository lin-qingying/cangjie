package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.api.LlvmBackendKind
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendVersionMismatchException

open class LlvmBackendFactory {
    fun createAndInitialize(options: CodegenOptions): LlvmBackend {
        return when (options.llvmBackendKind) {
            LlvmBackendKind.JNI -> {
                val jniBackend = createJniBackend()
                if (options.emitBitcode) {
                    jniBackend.initialize()
                    checkVersion(jniBackend, options.requiredLlvmMajorVersion)
                }
                jniBackend
            }
        }
    }

    protected open fun createJniBackend(): LlvmBackend = JniLlvmBackend()

    private fun checkVersion(backend: LlvmBackend, expectedMajorVersion: Int) {
        val actualVersion = backend.capabilities.llvmVersion ?: return
        val actualMajor = actualVersion.takeWhile { it.isDigit() }.toIntOrNull() ?: return
        if (actualMajor != expectedMajorVersion) {
            throw LlvmBackendVersionMismatchException(
                expectedMajor = expectedMajorVersion,
                actualVersion = actualVersion,
            )
        }
    }
}
