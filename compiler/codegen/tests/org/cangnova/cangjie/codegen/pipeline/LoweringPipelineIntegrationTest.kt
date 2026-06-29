package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * CHIR 到 LLVM lowering pipeline 的集成测试。
 */
class LoweringPipelineIntegrationTest {
    /**
     * 验证开启 trace 时 pipeline 会输出 pass 追踪行。
     */
    @Test
    fun `emits lowering trace when enabled`() {
        val generator = DefaultChirToLlvmCodeGenerator()
        val output = generator.generate(
            ChirCodegenInput(
                chirPackage = validPackage(),
                options = CodegenOptions(
                    enabled = true,
                    emitLoweringTrace = true,
                    verifyBeforeWrite = true,
                    validateChirBeforeLowering = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        assertTrue(output.loweringTrace.any { it.contains("symbol-collection") }, output.loweringTrace.joinToString("\n"))
        assertTrue(output.loweringTrace.any { it.contains("type-mapping") }, output.loweringTrace.joinToString("\n"))
        assertTrue(output.loweringTrace.any { it.contains("control-flow-contract") }, output.loweringTrace.joinToString("\n"))
    }

    /**
     * 验证控制流 pass 会拒绝无效 CFG。
     */
    @Test
    fun `control-flow pass rejects invalid cfg`() {
        val generator = DefaultChirToLlvmCodeGenerator()
        val error = assertThrows<IllegalArgumentException> {
            generator.generate(
                ChirCodegenInput(
                    chirPackage = invalidCfgPackage(),
                    options = CodegenOptions(
                        enabled = true,
                        validateChirBeforeLowering = false,
                        emitLoweringTrace = true,
                        verifyBeforeWrite = true,
                        emitBitcode = false,
                        emitComments = false,
                        emitModuleHeader = false,
                        emitRuntimeDeclarations = false,
                    ),
                ),
            )
        }

        assertTrue(error.message?.contains("control-flow contract") == true, error.message)
    }

    /**
     * 构造控制流合法的测试 package。
     */
    private fun validPackage(): ChirPackage {
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
                            semanticId = ChirSemanticId("const:zero"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:valid"),
            name = "valid",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:valid"),
                    name = "valid",
                    declarations = listOf(function),
                ),
            ),
        )
    }

    /**
     * 构造入口块缺失的非法 CFG package。
     */
    private fun invalidCfgPackage(): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:broken"),
            name = "broken",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:branch"),
                        targetBlockId = ChirSemanticId("block:missing"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:broken"),
            name = "broken",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:broken"),
                    name = "broken",
                    declarations = listOf(function),
                ),
            ),
        )
    }
}
