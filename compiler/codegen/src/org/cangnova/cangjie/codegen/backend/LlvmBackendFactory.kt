package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.api.LlvmBackendKind
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendException
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendVersionMismatchException

open class LlvmBackendFactory {
    fun createAndInitialize(options: CodegenOptions): LlvmBackend {
        return when (options.llvmBackendKind) {
            LlvmBackendKind.IN_MEMORY -> InMemoryLlvmBackendApi().also(LlvmBackendApi::initialize)
            LlvmBackendKind.JNI -> createJniOrFallback(options)
        }
    }

    private fun createJniOrFallback(options: CodegenOptions): LlvmBackend {
        val jniBackend = createJniBackend()
        return try {
            jniBackend.initialize()
            checkVersion(jniBackend, options.requiredLlvmMajorVersion)
            jniBackend
        } catch (error: LlvmBackendException) {
            if (options.failOnUnavailable) {
                throw error
            }
            InMemoryLlvmBackendApi().also(LlvmBackendApi::initialize)
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
