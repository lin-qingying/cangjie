package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.api.LlvmBackendKind
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendVersionMismatchException

/**
 * LLVM 后端工厂。
 *
 * 工厂负责根据 codegen 选项选择后端、按需初始化原生绑定，并在实际需要原生产物时校验 LLVM 版本。
 */
open class LlvmBackendFactory {
    /**
     * 创建并按选项初始化 LLVM 后端。
     */
    fun createAndInitialize(options: CodegenOptions): LlvmBackend {
        return when (options.llvmBackendKind) {
            LlvmBackendKind.JNI -> {
                val jniBackend = createJniBackend()
                val requiresNativeEmission = options.emitBitcode || options.emitObjectCode
                if (requiresNativeEmission || options.failOnUnavailable) {
                    jniBackend.initialize()
                }
                if (requiresNativeEmission) {
                    checkVersion(jniBackend, options.requiredLlvmMajorVersion)
                }
                jniBackend
            }
        }
    }

    /**
     * 创建 JNI LLVM 后端实例。
     *
     * 测试可覆写该方法注入 fake 后端。
     */
    protected open fun createJniBackend(): LlvmBackend = JniLlvmBackend()

    /**
     * 校验后端报告的 LLVM 主版本与配置要求一致。
     */
    private fun checkVersion(backend: LlvmBackend, expectedMajorVersion: Int) {
        val actualVersion = backend.capabilities.llvmVersion ?: throw LlvmBackendVersionMismatchException(
            expectedMajor = expectedMajorVersion,
            actualVersion = "<missing>",
        )
        val actualMajor = actualVersion.takeWhile { it.isDigit() }.toIntOrNull()
            ?: throw LlvmBackendVersionMismatchException(
                expectedMajor = expectedMajorVersion,
                actualVersion = actualVersion,
            )
        if (actualMajor != expectedMajorVersion) {
            throw LlvmBackendVersionMismatchException(
                expectedMajor = expectedMajorVersion,
                actualVersion = actualVersion,
            )
        }
    }
}
