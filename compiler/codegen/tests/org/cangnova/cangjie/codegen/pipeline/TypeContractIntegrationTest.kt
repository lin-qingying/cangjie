package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TypeContractIntegrationTest {
    @Test
    fun `rejects return value type mismatch`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)

        val error = assertThrows<CodegenLoweringException> {
            generate(
                function = DefaultChirFunctionDeclaration(
                    semanticId = ChirSemanticId("fn:return_mismatch"),
                    name = "return_mismatch",
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
                                    semanticId = ChirSemanticId("const:true"),
                                    type = boolType,
                                    literal = "true",
                                ),
                            ),
                        ),
                    ),
                    entryBlockId = ChirSemanticId("block:entry"),
                ),
            )
        }

        assertTrue(error.message?.contains("return value type mismatch") == true, error.message)
    }

    @Test
    fun `rejects arithmetic result type mismatch`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)

        val error = assertThrows<CodegenLoweringException> {
            generate(
                function = DefaultChirFunctionDeclaration(
                    semanticId = ChirSemanticId("fn:binary_mismatch"),
                    name = "binary_mismatch",
                    returnType = intType,
                    parameters = emptyList(),
                    blocks = listOf(
                        ChirBlock(
                            semanticId = ChirSemanticId("block:entry"),
                            name = "entry",
                            expressions = listOf(
                                ChirBinaryExpression(
                                    semanticId = ChirSemanticId("expr:add"),
                                    operator = "add",
                                    left = ChirConstantValue(
                                        semanticId = ChirSemanticId("const:left"),
                                        type = intType,
                                        literal = "1",
                                    ),
                                    right = ChirConstantValue(
                                        semanticId = ChirSemanticId("const:right"),
                                        type = intType,
                                        literal = "2",
                                    ),
                                    resultType = boolType,
                                ),
                            ),
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
                ),
            )
        }

        assertTrue(error.message?.contains("binary result type mismatch") == true, error.message)
    }

    @Test
    fun `rejects conditional branch with non boolean condition`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)

        val error = assertThrows<CodegenLoweringException> {
            generate(
                function = DefaultChirFunctionDeclaration(
                    semanticId = ChirSemanticId("fn:cond_mismatch"),
                    name = "cond_mismatch",
                    returnType = intType,
                    parameters = emptyList(),
                    blocks = listOf(
                        ChirBlock(
                            semanticId = ChirSemanticId("block:entry"),
                            name = "entry",
                            expressions = emptyList(),
                            terminator = ChirConditionalBranchTerminator(
                                semanticId = ChirSemanticId("term:branch"),
                                condition = ChirConstantValue(
                                    semanticId = ChirSemanticId("const:one"),
                                    type = intType,
                                    literal = "1",
                                ),
                                trueTargetBlockId = ChirSemanticId("block:then"),
                                falseTargetBlockId = ChirSemanticId("block:else"),
                            ),
                        ),
                        returningBlock("block:then", intType),
                        returningBlock("block:else", intType),
                    ),
                    entryBlockId = ChirSemanticId("block:entry"),
                ),
            )
        }

        assertTrue(error.message?.contains("conditional branch condition type mismatch") == true, error.message)
    }

    @Test
    fun `rejects return value that references undeclared parameter`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)

        val error = assertThrows<CodegenLoweringException> {
            generate(
                function = DefaultChirFunctionDeclaration(
                    semanticId = ChirSemanticId("fn:undeclared_param"),
                    name = "undeclared_param",
                    returnType = intType,
                    parameters = emptyList(),
                    blocks = listOf(
                        ChirBlock(
                            semanticId = ChirSemanticId("block:entry"),
                            name = "entry",
                            expressions = emptyList(),
                            terminator = ChirReturnTerminator(
                                semanticId = ChirSemanticId("term:return"),
                                returnValue = ChirParameterValue(
                                    semanticId = ChirSemanticId("param:missing"),
                                    type = intType,
                                    name = "missing",
                                    ownerFunctionId = ChirSemanticId("fn:undeclared_param"),
                                ),
                            ),
                        ),
                    ),
                    entryBlockId = ChirSemanticId("block:entry"),
                ),
            )
        }

        assertTrue(error.message?.contains("is not declared in function") == true, error.message)
    }

    @Test
    fun `rejects throw terminator with missing unwind target`() {
        val voidType = ChirResolvedTypeRef(ChirPrimitiveType.VOID)
        val exceptionType = ChirResolvedTypeRef(ChirCPointerType(ChirResolvedTypeRef(ChirPrimitiveType.INT8)))

        val error = assertThrows<CodegenLoweringException> {
            generate(
                function = DefaultChirFunctionDeclaration(
                    semanticId = ChirSemanticId("fn:throw_missing_unwind"),
                    name = "throw_missing_unwind",
                    returnType = voidType,
                    parameters = emptyList(),
                    blocks = listOf(
                        ChirBlock(
                            semanticId = ChirSemanticId("block:entry"),
                            name = "entry",
                            expressions = emptyList(),
                            terminator = ChirThrowTerminator(
                                semanticId = ChirSemanticId("term:throw"),
                                exceptionValue = ChirConstantValue(
                                    semanticId = ChirSemanticId("const:null"),
                                    type = exceptionType,
                                    literal = "null",
                                ),
                                unwindTargetBlockId = ChirSemanticId("block:missing"),
                            ),
                        ),
                    ),
                    entryBlockId = ChirSemanticId("block:entry"),
                ),
            )
        }

        assertTrue(error.message?.contains("throw unwind target block:missing missing") == true, error.message)
    }

    private fun generate(function: DefaultChirFunctionDeclaration) {
        DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:${function.name}"),
                    name = function.name,
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:${function.name}"),
                            name = function.name,
                            declarations = listOf(function),
                        ),
                    ),
                ),
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = false,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )
    }

    private fun returningBlock(id: String, intType: ChirResolvedTypeRef): ChirBlock {
        return ChirBlock(
            semanticId = ChirSemanticId(id),
            name = id.substringAfter(':'),
            expressions = emptyList(),
            terminator = ChirReturnTerminator(
                semanticId = ChirSemanticId("term:$id:return"),
                returnValue = ChirConstantValue(
                    semanticId = ChirSemanticId("const:$id:zero"),
                    type = intType,
                    literal = "0",
                ),
            ),
        )
    }
}
