package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.attribute.ChirBooleanAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.ChirPackageMembers
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FunctionCallConventionIntegrationTest {
    @Test
    fun `emits imported declaration and call with calling convention and attributes`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val importedParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("arg:x"),
            name = "x",
            type = intType,
            mutable = false,
            attributes = setOf(ChirBooleanAttribute("noundef", enabled = true)),
        )
        val importedFunction = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:ext_add"),
            name = "ext_add",
            returnType = intType,
            parameters = listOf(importedParameter),
            blocks = emptyList(),
            entryBlockId = ChirSemanticId("block:none"),
            attributes = setOf(
                ChirStringAttribute("cc", "fastcc"),
                ChirBooleanAttribute("nounwind", enabled = true),
            ),
        )
        val calleeType = ChirResolvedTypeRef(ChirFunctionType(parameterTypes = listOf(intType), returnType = intType))
        val callExpression = ChirCallExpression(
            semanticId = ChirSemanticId("call_ret"),
            callee = ChirImportedFunctionValue(
                semanticId = ChirSemanticId("value:ext_add"),
                type = calleeType,
                name = "ext_add",
                attributes = setOf(
                    ChirStringAttribute("cc", "fastcc"),
                    ChirBooleanAttribute("tail", enabled = true),
                ),
            ),
            arguments = listOf(
                ChirConstantValue(
                    semanticId = ChirSemanticId("const:one"),
                    type = intType,
                    literal = "1",
                    attributes = setOf(ChirBooleanAttribute("noundef", enabled = true)),
                ),
            ),
            resultType = intType,
        )

        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:caller"),
            name = "caller",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(callExpression),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("local:call_ret"),
                            type = intType,
                            name = "call_ret",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:test"),
                    name = "test",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:test"),
                            name = "test",
                            declarations = listOf(caller),
                        ),
                    ),
                    members = ChirPackageMembers(importedFunctions = listOf(importedFunction)),
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
        assertTrue(ir.contains("declare fastcc i32 @ext_add(i32 noundef) nounwind"), ir)
        assertTrue(ir.contains("%call_ret = tail call fastcc i32 @ext_add(i32 noundef 1)"), ir)
        assertTrue(ir.contains("ret i32 %call_ret"), ir)
    }

    @Test
    fun `emits function header with linkage cc and parameter attributes`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:configured"),
            name = "configured",
            returnType = intType,
            parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:x"),
                    name = "x",
                    type = intType,
                    mutable = false,
                    attributes = setOf(
                        ChirBooleanAttribute("noundef", enabled = true),
                        ChirStringAttribute("align", "8"),
                    ),
                ),
            ),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:one"),
                            type = intType,
                            literal = "1",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
            attributes = setOf(
                ChirStringAttribute("linkage", "dso_local"),
                ChirStringAttribute("cc", "fastcc"),
                ChirBooleanAttribute("nounwind", enabled = true),
            ),
        )

        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:test"),
                    name = "test",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:test"),
                            name = "test",
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
        assertTrue(ir.contains("define dso_local fastcc i32 @configured(i32 noundef align 8 %x) nounwind"), ir)
        assertTrue(ir.contains("ret i32 1"), ir)
    }
}
