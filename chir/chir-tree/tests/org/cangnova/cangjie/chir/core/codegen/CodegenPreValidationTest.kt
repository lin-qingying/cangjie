package org.cangnova.cangjie.chir.core.codegen

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.pipeline.DefaultChirToLlvmCodeGenerator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 代码生成前 CHIR 校验开关的测试。
 */
class CodegenPreValidationTest {
    /**
     * 验证非法 CHIR 会在 lowering 前被代码生成器拒绝。
     */
    @Test
    fun `generator rejects invalid CHIR before lowering`() {
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

        val error = assertThrows<IllegalArgumentException> {
            DefaultChirToLlvmCodeGenerator().generate(
                ChirCodegenInput(
                    chirPackage = ChirPackage(
                        semanticId = ChirSemanticId("pkg:broken"),
                        name = "broken",
                        modules = listOf(
                            ChirModule(
                                semanticId = ChirSemanticId("mod:broken"),
                                name = "broken",
                                declarations = listOf(function),
                            ),
                        ),
                    ),
                    options = CodegenOptions(
                        enabled = true,
                        verifyBeforeWrite = true,
                        validateChirBeforeLowering = true,
                        emitBitcode = false,
                        emitComments = false,
                        emitModuleHeader = false,
                        emitRuntimeDeclarations = false,
                    ),
                ),
            )
        }
        assertTrue(error.message?.contains("invalid CHIR package") == true, error.message)
        assertTrue(error.message?.contains("INVALID_CFG") == true, error.message)
    }
}
