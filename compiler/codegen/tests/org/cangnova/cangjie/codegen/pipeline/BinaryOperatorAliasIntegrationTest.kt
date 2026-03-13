package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
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

class BinaryOperatorAliasIntegrationTest {
    @Test
    fun `maps alias operators through enum parser`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:alias"),
            name = "alias",
            returnType = boolType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:add"),
                            operator = "+",
                            left = ChirConstantValue(
                                semanticId = ChirSemanticId("const:a"),
                                type = intType,
                                literal = "1",
                            ),
                            right = ChirConstantValue(
                                semanticId = ChirSemanticId("const:b"),
                                type = intType,
                                literal = "2",
                            ),
                            resultType = intType,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:cmp"),
                            operator = "ult",
                            left = ChirLocalValue(
                                semanticId = ChirSemanticId("expr:add"),
                                type = intType,
                                name = "expr_add",
                            ),
                            right = ChirConstantValue(
                                semanticId = ChirSemanticId("const:c"),
                                type = intType,
                                literal = "5",
                            ),
                            resultType = boolType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:cmp"),
                            type = boolType,
                            name = "expr_cmp",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:alias"),
                    name = "alias",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:alias"),
                            name = "alias",
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
        assertTrue(ir.contains("%expr_add = add i32 1, 2"), ir)
        assertTrue(ir.contains("%expr_cmp = icmp ult i32 %expr_add, 5"), ir)
        assertTrue(ir.contains("ret i1 %expr_cmp"), ir)
    }
}
