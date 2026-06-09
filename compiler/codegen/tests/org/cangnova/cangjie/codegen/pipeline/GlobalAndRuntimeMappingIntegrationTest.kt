package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.ChirPackageMembers
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GlobalAndRuntimeMappingIntegrationTest {
    @Test
    fun `emits globals constants and package runtime entry mappings`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val initFunctionId = ChirSemanticId("fn:init")
        val literalInitFunctionId = ChirSemanticId("fn:literal_init")

        val initFunction = DefaultChirFunctionDeclaration(
            semanticId = initFunctionId,
            name = "init",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:init"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:init:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:init"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:init"),
        )
        val literalInitFunction = DefaultChirFunctionDeclaration(
            semanticId = literalInitFunctionId,
            name = "literal_init",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:literal_init"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:literal_init:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:literal_init"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:literal_init"),
        )

        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:global"),
                    name = "global",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:global"),
                            name = "global",
                            declarations = listOf(initFunction, literalInitFunction),
                        ),
                    ),
                    members = ChirPackageMembers(
                        globalVariables = listOf(
                            DefaultChirVariableDeclaration(
                                semanticId = ChirSemanticId("global:mutable"),
                                name = "g_mut",
                                type = intType,
                                mutable = true,
                            ),
                            DefaultChirVariableDeclaration(
                                semanticId = ChirSemanticId("global:const"),
                                name = "g_const",
                                type = intType,
                                mutable = false,
                                attributes = setOf(
                                    ChirStringAttribute("initializer", "7"),
                                    ChirStringAttribute("linkage", "internal"),
                                ),
                            ),
                        ),
                        importedVariables = listOf(
                            DefaultChirVariableDeclaration(
                                semanticId = ChirSemanticId("global:imported"),
                                name = "g_ext",
                                type = intType,
                                mutable = true,
                                attributes = setOf(ChirStringAttribute("linkage", "external")),
                            ),
                        ),
                    ),
                    packageInitFunctionId = initFunctionId,
                    packageLiteralInitFunctionId = literalInitFunctionId,
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
        assertTrue(ir.contains("@g_mut = global i32 0"), ir)
        assertTrue(ir.contains("@g_const = internal constant i32 7"), ir)
        assertTrue(ir.contains("@g_ext = external global i32"), ir)
        assertTrue(ir.contains("@cangjie.package.init = internal constant ptr @init"), ir)
        assertTrue(ir.contains("@cangjie.package.literal_init = internal constant ptr @literal_init"), ir)
    }

    @Test
    fun `rejects package runtime entry mapping with missing target function`() {
        val error = assertThrows<CodegenLoweringException> {
            DefaultChirToLlvmCodeGenerator().generate(
                ChirCodegenInput(
                    chirPackage = ChirPackage(
                        semanticId = ChirSemanticId("pkg:missing-entry"),
                        name = "missing-entry",
                        modules = listOf(
                            ChirModule(
                                semanticId = ChirSemanticId("mod:missing-entry"),
                                name = "missing-entry",
                                declarations = emptyList(),
                            ),
                        ),
                        packageInitFunctionId = ChirSemanticId("fn:missing-init"),
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

        assertTrue(error.message?.contains("cangjie.package.init") == true, error.message)
        assertTrue(error.message?.contains("fn:missing-init") == true, error.message)
    }
}
