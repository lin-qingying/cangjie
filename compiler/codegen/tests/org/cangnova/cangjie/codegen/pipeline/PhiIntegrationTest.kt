package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * LLVM phi lowering 的集成测试。
 */
class PhiIntegrationTest {
    /**
     * 验证 phi 节点会携带正确前驱 block 映射。
     */
    @Test
    fun `emits phi node with predecessor mapping`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val i1Type = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val mergeBlockId = ChirSemanticId("block:merge")
        val thenBlockId = ChirSemanticId("block:then")
        val elseBlockId = ChirSemanticId("block:else")

        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:phi"),
            name = "phi",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirConditionalBranchTerminator(
                        semanticId = ChirSemanticId("term:entry:br"),
                        condition = ChirConstantValue(
                            semanticId = ChirSemanticId("const:cond"),
                            type = i1Type,
                            literal = "true",
                        ),
                        trueTargetBlockId = thenBlockId,
                        falseTargetBlockId = elseBlockId,
                    ),
                ),
                ChirBlock(
                    semanticId = thenBlockId,
                    name = "then",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:then:br"),
                        targetBlockId = mergeBlockId,
                    ),
                ),
                ChirBlock(
                    semanticId = elseBlockId,
                    name = "else",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:else:br"),
                        targetBlockId = mergeBlockId,
                    ),
                ),
                ChirBlock(
                    semanticId = mergeBlockId,
                    name = "merge",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:phi"),
                            operation = "phi",
                            operands = listOf(
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:then"),
                                    type = intType,
                                    literal = "1",
                                    attributes = setOf(ChirStringAttribute("pred", thenBlockId.value)),
                                ),
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:else"),
                                    type = intType,
                                    literal = "2",
                                    attributes = setOf(ChirStringAttribute("pred", elseBlockId.value)),
                                ),
                            ),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:merge:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:phi"),
                            type = intType,
                            name = "expr_phi",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:phi"),
                    name = "phi",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:phi"),
                            name = "phi",
                            declarations = listOf(function),
                        ),
                    ),
                ),
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val ir = output.modules.single().ir
        assertTrue(ir.contains("%expr_phi = phi i32 [ 1, %then ], [ 2, %else ]"), ir)
        assertTrue(ir.contains("ret i32 %expr_phi"), ir)
    }
}
