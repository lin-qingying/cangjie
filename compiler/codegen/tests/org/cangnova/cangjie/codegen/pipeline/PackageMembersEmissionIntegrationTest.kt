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
import org.cangnova.cangjie.codegen.api.ModulePartitionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PackageMembersEmissionIntegrationTest {
    @Test
    fun `single module emits package functions globals and module functions together`() {
        val output = generate(ModulePartitionMode.SINGLE_MODULE)
        val ir = output.single().ir

        assertTrue(ir.contains("@pkg_value = global i32 1"), ir)
        assertTrue(ir.contains("@cangjie.package.init = internal constant ptr @pkg_init"), ir)
        assertTrue(ir.contains("define i32 @pkg_init()"), ir)
        assertTrue(ir.contains("define i32 @module_main()"), ir)
    }

    @Test
    fun `per chir module emits package definitions only in package module`() {
        val modules = generate(ModulePartitionMode.PER_CHIR_MODULE)

        assertEquals(2, modules.size)
        val packageModule = modules.first { it.name == "pkg.package" }
        val chirModule = modules.first { it.name == "pkg.main" }

        assertTrue(packageModule.ir.contains("@pkg_value = global i32 1"), packageModule.ir)
        assertTrue(packageModule.ir.contains("define i32 @pkg_init()"), packageModule.ir)
        assertFalse(chirModule.ir.contains("@pkg_value = global i32 1"), chirModule.ir)
        assertFalse(chirModule.ir.contains("define i32 @pkg_init()"), chirModule.ir)
        assertTrue(chirModule.ir.contains("define i32 @module_main()"), chirModule.ir)
    }

    private fun generate(partitionMode: ModulePartitionMode) =
        DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = packageWithMembers(),
                options = CodegenOptions(
                    enabled = true,
                    partitionMode = partitionMode,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        ).modules

    private fun packageWithMembers(): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val initId = ChirSemanticId("fn:pkg_init")
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:pkg"),
            name = "pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:main"),
                    name = "main",
                    declarations = listOf(simpleFunction("module_main", "0", intType)),
                ),
            ),
            members = ChirPackageMembers(
                globalVariables = listOf(
                    DefaultChirVariableDeclaration(
                        semanticId = ChirSemanticId("global:pkg_value"),
                        name = "pkg_value",
                        type = intType,
                        mutable = true,
                        attributes = setOf(ChirStringAttribute("initializer", "1")),
                    ),
                ),
                globalFunctions = listOf(simpleFunction("pkg_init", "7", intType, initId)),
            ),
            packageInitFunctionId = initId,
        )
    }

    private fun simpleFunction(
        name: String,
        literal: String,
        intType: ChirResolvedTypeRef,
        semanticId: ChirSemanticId = ChirSemanticId("fn:$name"),
    ): DefaultChirFunctionDeclaration {
        return DefaultChirFunctionDeclaration(
            semanticId = semanticId,
            name = name,
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:$name:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:$name:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:$name:return"),
                            type = intType,
                            literal = literal,
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:$name:entry"),
        )
    }
}
