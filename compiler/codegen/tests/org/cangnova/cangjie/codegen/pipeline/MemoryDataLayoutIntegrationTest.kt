package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemoryDataLayoutIntegrationTest {
    @Test
    fun `emits typed memory operations and alignment`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val i64Type = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val ptrIntType = ChirResolvedTypeRef(ChirCPointerType(intType))

        val allocaResult = ChirLocalValue(
            semanticId = ChirSemanticId("expr:alloca"),
            type = ptrIntType,
            name = "expr_alloca",
            attributes = setOf(ChirStringAttribute("align", "8")),
        )
        val loadResult = ChirLocalValue(
            semanticId = ChirSemanticId("expr:load"),
            type = intType,
            name = "expr_load",
        )

        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:memory"),
            name = "memory",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:alloca"),
                            operation = "alloca",
                            address = ChirConstantValue(
                                semanticId = ChirSemanticId("const:count"),
                                type = i64Type,
                                literal = "1",
                                attributes = setOf(ChirStringAttribute("align", "16")),
                            ),
                            resultType = ptrIntType,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:store"),
                            operation = "store",
                            address = allocaResult,
                            value = ChirConstantValue(
                                semanticId = ChirSemanticId("const:forty_two"),
                                type = intType,
                                literal = "42",
                            ),
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:load"),
                            operation = "load",
                            address = allocaResult,
                            resultType = intType,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:gep"),
                            operation = "getelementptr.inbounds",
                            address = allocaResult,
                            value = ChirConstantValue(
                                semanticId = ChirSemanticId("const:index"),
                                type = intType,
                                literal = "0",
                            ),
                            resultType = ptrIntType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = loadResult,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:memory"),
                    name = "memory",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:memory"),
                            name = "memory",
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
        assertTrue(ir.contains("%expr_alloca = alloca i32, i64 1, align 16"), ir)
        assertTrue(ir.contains("store i32 42, ptr %expr_alloca, align 8"), ir)
        assertTrue(ir.contains("%expr_load = load i32, ptr %expr_alloca, align 8"), ir)
        assertTrue(ir.contains("%expr_gep = getelementptr inbounds i32, ptr %expr_alloca, i32 0"), ir)
        assertTrue(ir.contains("ret i32 %expr_load"), ir)
    }
}
