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

class RuntimeDeclarationIntegrationTest {
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

    private fun emptyModule(): ChirModule {
        return ChirModule(
            semanticId = ChirSemanticId("mod:runtime"),
            name = "runtime",
            declarations = emptyList(),
        )
    }

    private object NoOpBackend : LlvmBackendApi {
        override val id: String = "noop"

        override fun initialize() = Unit

        override fun emitBitcode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray =
            ByteArray(0)

        override fun emitObjectCode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray =
            ByteArray(0)

        override fun emitObjectFile(
            moduleName: String,
            llvmIr: String,
            options: LlvmBackendEmissionOptions,
            outputPath: String,
        ) = Unit
    }
}
