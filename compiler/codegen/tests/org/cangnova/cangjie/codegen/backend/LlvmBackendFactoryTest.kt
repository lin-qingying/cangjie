package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.api.LlvmBackendKind
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendUnavailableException
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendVersionMismatchException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * `LlvmBackendFactory` 的后端选择、初始化和版本校验测试。
 */
class LlvmBackendFactoryTest {
    /**
     * 验证默认可用 JNI 后端会被创建并报告完整能力。
     */
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

    /**
     * 验证默认 bitcode 发射路径即使非 strict 也会在 JNI 不可用时失败。
     */
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

    /**
     * 验证 strict 模式在 JNI 不可用时立即失败。
     */
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

    /**
     * 验证 strict IR-only 模式会初始化 JNI 后端但不要求版本存在。
     */
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

    /**
     * 验证 object emission 会初始化 JNI 后端并触发 LLVM 版本检查。
     */
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

    /**
     * 验证 object emission 下 LLVM 主版本不匹配会报错。
     */
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


    /**
     * 验证 strict JNI 后端初始化后的 LLVM 主版本不匹配会报错。
     */
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

    /**
     * 验证需要原生产物时缺失 LLVM 版本会报版本错误。
     */
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

    /**
     * 验证无法解析 LLVM 版本主号时会报版本错误。
     */
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

    /**
     * 测试专用 LLVM 后端工厂，用于注入 fake JNI 后端。
     */
    private class LlvmBackendFactoryForTest(
        /**
         * 工厂返回的固定 JNI 后端。
         */
        private val jniBackend: LlvmBackend,
    ) : LlvmBackendFactory() {
        /**
         * 返回测试注入的 JNI 后端。
         */
        override fun createJniBackend(): LlvmBackend = jniBackend
    }

    /**
     * 测试专用 JNI 原生门面。
     */
    private class FakeNativeFacade(
        /**
         * fake 后端可用状态。
         */
        private val available: Boolean,
        /**
         * fake 加载诊断。
         */
        private val diagnosticsMessage: String = "",
        /**
         * fake LLVM 版本。
         */
        private val version: String? = null,
    ) : JniNativeFacade {
        /**
         * 记录 API binding 是否已安装。
         */
        var apiBindingsInstalled: Boolean = false

        /**
         * fake 可用状态。
         */
        override val isAvailable: Boolean
            get() = available

        /**
         * fake 诊断文本。
         */
        override val diagnostics: String
            get() = diagnosticsMessage

        /**
         * fake LLVM 版本。
         */
        override val llvmVersion: String?
            get() = version

        /**
         * 标记 API binding 已安装。
         */
        override fun installApiBindings() {
            apiBindingsInstalled = true
        }

        /**
         * fake bitcode 发射直接返回 IR 字节。
         */
        override fun emitBitcode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray =
            llvmIr.toByteArray()

        /**
         * fake object code 发射直接返回 IR 字节。
         */
        override fun emitObjectCode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray =
            llvmIr.toByteArray()

        /**
         * fake object file 发射不写文件。
         */
        override fun emitObjectFile(
            moduleName: String,
            llvmIr: String,
            options: LlvmBackendEmissionOptions,
            outputPath: String,
        ) = Unit
    }
}
