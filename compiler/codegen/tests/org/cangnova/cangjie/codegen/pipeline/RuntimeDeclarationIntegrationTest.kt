package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.backend.LlvmBackendApi
import org.cangnova.cangjie.codegen.backend.LlvmBackendEmissionOptions
import org.cangnova.cangjie.codegen.context.CGContext
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.cangnova.cangjie.codegen.module.CGModule
import org.cangnova.cangjie.codegen.runtime.RuntimeSymbol
import org.cangnova.cangjie.codegen.runtime.RuntimeSymbolTable
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 运行时符号声明发射的集成测试。
 */
class RuntimeDeclarationIntegrationTest {
    /**
     * 验证运行时符号表中的默认符号会发射为 LLVM declare。
     */
    @Test
    fun `emits runtime declarations from runtime symbol table`() {
        val module = emptyModule()
        val ir = CGModule(
            context = contextFor(module),
            module = module,
            backendApi = NoOpBackend,
        ).lower().ir

        assertTrue(ir.contains("declare void @cangjie.throw(ptr)"), ir)
        assertTrue(ir.contains("declare ptr @cangjie.alloc(i64)"), ir)
        assertTrue(ir.contains("declare void @cangjie.gc.barrier(ptr, ptr)"), ir)
    }

    /**
     * 验证格式错误的运行时签名会在 module lowering 时失败。
     */
    @Test
    fun `rejects malformed runtime signatures`() {
        val module = emptyModule()
        val runtimeSymbols = RuntimeSymbolTable().also {
            it.register(RuntimeSymbol("cangjie.bad", "not-a-signature"))
        }

        val error = assertThrows<CodegenLoweringException> {
            CGModule(
                context = contextFor(module, runtimeSymbols),
                module = module,
                backendApi = NoOpBackend,
            ).lower()
        }

        assertTrue(error.message?.contains("cangjie.bad") == true, error.message)
        assertTrue(error.message?.contains("invalid LLVM signature") == true, error.message)
    }

    /**
     * 为指定 module 构造带运行时符号表的 codegen 上下文。
     */
    private fun contextFor(
        module: ChirModule,
        runtimeSymbols: RuntimeSymbolTable = RuntimeSymbolTable(),
    ): CGContext {
        return CGContext(
            inputPackage = ChirPackage(
                semanticId = ChirSemanticId("pkg:${module.name}"),
                name = module.name,
                modules = listOf(module),
            ),
            options = CodegenOptions(
                enabled = true,
                emitBitcode = false,
                emitComments = false,
                emitModuleHeader = false,
                emitRuntimeDeclarations = true,
            ),
            runtimeSymbols = runtimeSymbols,
        )
    }

    /**
     * 构造不含声明的测试 module。
     */
    private fun emptyModule(): ChirModule {
        return ChirModule(
            semanticId = ChirSemanticId("mod:runtime"),
            name = "runtime",
            declarations = emptyList(),
        )
    }

    /**
     * 测试专用空 LLVM 后端。
     */
    private object NoOpBackend : LlvmBackendApi {
        /**
         * 后端标识。
         */
        override val id: String = "noop"

        /**
         * 空初始化。
         */
        override fun initialize() = Unit

        /**
         * 返回空 bitcode 字节。
         */
        override fun emitBitcode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray =
            ByteArray(0)

        /**
         * 返回空 object code 字节。
         */
        override fun emitObjectCode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray =
            ByteArray(0)

        /**
         * 不写出 object file。
         */
        override fun emitObjectFile(
            moduleName: String,
            llvmIr: String,
            options: LlvmBackendEmissionOptions,
            outputPath: String,
        ) = Unit
    }
}
