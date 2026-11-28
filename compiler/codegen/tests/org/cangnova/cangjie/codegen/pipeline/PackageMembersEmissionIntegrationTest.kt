package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.ChirPackageMembers
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirThisType
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.api.ModulePartitionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 包级成员发射策略的集成测试。
 */
class PackageMembersEmissionIntegrationTest {
    /**
     * 验证单 module 模式会同时发射包级函数、全局变量和模块函数。
     */
    @Test
    fun `single module emits package functions globals and module functions together`() {
        val output = generate(ModulePartitionMode.SINGLE_MODULE)
        val ir = output.single().ir

        assertTrue(ir.contains("@pkg_value = global i32 1"), ir)
        assertTrue(ir.contains("@cangjie.package.init = internal constant ptr @pkg_init"), ir)
        assertTrue(ir.contains("define i32 @pkg_init()"), ir)
        assertTrue(ir.contains("define i32 @widget_member(ptr"), ir)
        assertTrue(ir.contains("define i32 @module_main()"), ir)
    }

    /**
     * 验证按 CHIR module 切分时包级定义只发射到 package module。
     */
    @Test
    fun `per chir module emits package definitions only in package module`() {
        val modules = generate(ModulePartitionMode.PER_CHIR_MODULE)

        assertEquals(2, modules.size)
        val packageModule = modules.first { it.name == "pkg.package" }
        val chirModule = modules.first { it.name == "pkg.main" }

        assertTrue(packageModule.ir.contains("@pkg_value = global i32 1"), packageModule.ir)
        assertTrue(packageModule.ir.contains("define i32 @pkg_init()"), packageModule.ir)
        assertTrue(packageModule.ir.contains("define i32 @widget_member(ptr"), packageModule.ir)
        assertFalse(chirModule.ir.contains("@pkg_value = global i32 1"), chirModule.ir)
        assertFalse(chirModule.ir.contains("define i32 @pkg_init()"), chirModule.ir)
        assertFalse(chirModule.ir.contains("define i32 @widget_member"), chirModule.ir)
        assertTrue(chirModule.ir.contains("define i32 @module_main()"), chirModule.ir)
    }

    @Test
    fun `per chir module emits package type members without globals or package initializers`() {
        val source = packageWithMembers().copy(
            members = ChirPackageMembers(),
            packageInitFunctionId = null,
        )
        val output = generate(source, ModulePartitionMode.PER_CHIR_MODULE)

        assertEquals(listOf("pkg.package", "pkg.main"), output.map { it.name })
        assertTrue(output.first().ir.contains("define i32 @widget_member(ptr"), output.first().ir)
        assertFalse(output.last().ir.contains("define i32 @widget_member"), output.last().ir)
        assertTrue(output.last().ir.contains("define i32 @module_main()"), output.last().ir)
    }

    @Test
    fun `lowers type members nested in the current chir module`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val member = simpleFunction(
            name = "specific_widget_member",
            literal = "23",
            intType = intType,
            semanticId = ChirSemanticId("fn:SpecificWidget.member"),
        ).copy(isStatic = true)
        val moduleType = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:SpecificWidget"),
            name = "SpecificWidget",
            typeDeclarationId = ChirTypeDeclarationId("pkg", "SpecificWidget"),
            memberDeclarations = listOf(member),
        )
        val input = ChirPackage(
            semanticId = ChirSemanticId("pkg:specific-member"),
            name = "pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:specific-member"),
                    name = "specific",
                    declarations = listOf(moduleType, simpleFunction("specific_main", "0", intType)),
                ),
            ),
        )
        val output = generate(input, ModulePartitionMode.PER_CHIR_MODULE)

        assertEquals(listOf("pkg.specific"), output.map { it.name })
        assertTrue(
            output.single().ir.contains(
                "define i32 @specific_widget_member(ptr %ti.SpecificWidget.outer, ptr %ti.SpecificWidget.this)",
            ),
            output.single().ir,
        )
        assertTrue(output.single().ir.contains("define i32 @specific_main()"), output.single().ir)
    }

    /**
     * Imported member functions referenced from CHIR must be declared even though they are nested in a type.
     */
    @Test
    fun `emits imported member function declarations with owner type info`() {
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val importedConstructorId = ChirSemanticId("@_CNat5ArrayIG_E6<init>Hv")
        val importedConstructor = DefaultChirFunctionDeclaration(
            semanticId = importedConstructorId,
            name = "<init>",
            returnType = unitType,
            parameters = emptyList(),
            blocks = emptyList(),
            entryBlockId = null,
            hasBody = false,
            isImported = true,
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:call-imported-constructor"),
            name = "callImportedConstructor",
            returnType = unitType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:call-imported-constructor"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:return-imported-constructor")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:call-imported-constructor"),
        )
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:imported-member"),
                    name = "imported-member",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:imported-member"),
                            name = "main",
                            declarations = listOf(caller),
                        ),
                    ),
                    importedTypeDefinitions = listOf(
                        DefaultChirClassDeclaration(
                            semanticId = ChirSemanticId("class:std.core.Array"),
                            name = "Array",
                            typeDeclarationId = ChirTypeDeclarationId("std.core", "Array"),
                            memberDeclarations = listOf(importedConstructor),
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
        val symbol = "@\"_CNat5ArrayIG_E6\\3Cinit\\3EHv\""
        assertTrue(ir.contains("declare void $symbol(ptr)"), ir)
    }

    @Test
    fun `declares external member function values with call site owner type info`() {
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val arrayType = ChirResolvedTypeRef(
            ChirRefType(
                ChirResolvedTypeRef(
                    ChirClassType(
                        declarationId = ChirTypeDeclarationId("std.core", "Array"),
                        typeArguments = listOf(ChirResolvedTypeRef(ChirPrimitiveType.UINT8)),
                    ),
                ),
            ),
        )
        val constructorId = ChirSemanticId("@_CNat5ArrayIG_E6<init>Hv")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:call-external-member"),
            name = "callExternalMember",
            returnType = unitType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:call-external-member"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:call-external-member"),
                            callee = ChirFunctionValue(
                                semanticId = constructorId,
                                type = ChirResolvedTypeRef(ChirFunctionType(listOf(arrayType), unitType)),
                                name = "init",
                            ),
                            arguments = listOf(
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:external-member-receiver"),
                                    type = arrayType,
                                    literal = "null",
                                ),
                            ),
                            resultType = unitType,
                            thisType = arrayType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:return-external-member")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:call-external-member"),
        )
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:external-member"),
                    name = "external-member",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:external-member"),
                            name = "main",
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
        val symbol = "@\"_CNat5ArrayIG_E6\\3Cinit\\3EHv\""
        assertTrue(ir.contains("declare void $symbol(ptr, ptr)"), ir)
        assertTrue(ir.contains("call void $symbol(ptr null, ptr @\"std.core:Array<UInt8>.ti\")"), ir)
        assertTrue(ir.contains("@\"std.core:Array<UInt8>.ti\" = external global %TypeInfo"), ir)
    }

    @Test
    fun `static member calls pass both owner and this type information`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val ownerId = ChirTypeDeclarationId("pkg", "Widget")
        val staticFunctionId = ChirSemanticId("@static_owner_factory")
        val staticFunction = DefaultChirFunctionDeclaration(
            semanticId = staticFunctionId,
            name = "staticFactory",
            returnType = intType,
            parameters = emptyList(),
            blocks = emptyList(),
            entryBlockId = null,
            hasBody = false,
            isImported = true,
            isStatic = true,
        )
        val callId = ChirSemanticId("expr:call-static-owner-factory")
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:call-static-owner-factory"),
            name = "callStaticFactory",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:call-static-owner-factory"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = callId,
                            callee = ChirFunctionValue(
                                semanticId = staticFunctionId,
                                type = ChirResolvedTypeRef(ChirFunctionType(emptyList(), intType)),
                                name = "staticFactory",
                            ),
                            arguments = emptyList(),
                            resultType = intType,
                            thisType = ChirResolvedTypeRef(ChirThisType("Widget")),
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return-static-owner-factory"),
                        returnValue = org.cangnova.cangjie.chir.core.value.ChirLocalValue(
                            semanticId = callId,
                            type = intType,
                            name = "staticFactoryResult",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:call-static-owner-factory"),
            isStatic = true,
        )
        val instanceCallId = ChirSemanticId("expr:call-static-owner-factory-from-instance")
        val instanceReceiverType = ChirResolvedTypeRef(
            ChirRefType(ChirResolvedTypeRef(ChirClassType(declarationId = ownerId))),
        )
        val instanceCaller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:call-static-owner-factory-from-instance"),
            name = "callStaticFactoryFromInstance",
            returnType = intType,
            parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:Widget.this"),
                    name = "this",
                    type = instanceReceiverType,
                    mutable = false,
                ),
            ),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:call-static-owner-factory-from-instance"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = instanceCallId,
                            callee = ChirFunctionValue(
                                semanticId = staticFunctionId,
                                type = ChirResolvedTypeRef(ChirFunctionType(emptyList(), intType)),
                                name = "staticFactory",
                            ),
                            arguments = emptyList(),
                            resultType = intType,
                            thisType = ChirResolvedTypeRef(ChirThisType("Widget")),
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return-static-owner-factory-from-instance"),
                        returnValue = org.cangnova.cangjie.chir.core.value.ChirLocalValue(
                            semanticId = instanceCallId,
                            type = intType,
                            name = "staticFactoryResultFromInstance",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:call-static-owner-factory-from-instance"),
        )
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:static-member"),
                    name = "static-member",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:static-member"),
                            name = "main",
                            declarations = emptyList(),
                        ),
                    ),
                    typeDefinitions = listOf(
                        DefaultChirClassDeclaration(
                            semanticId = ChirSemanticId("class:Widget"),
                            name = "Widget",
                            typeDeclarationId = ownerId,
                            memberDeclarations = listOf(staticFunction, caller, instanceCaller),
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
        assertTrue(ir.contains("declare i32 @static_owner_factory(ptr, ptr)"), ir)
        assertTrue(ir.contains("define i32 @callStaticFactory(ptr %ti.Widget.outer, ptr %ti.Widget.this)"), ir)
        assertTrue(
            ir.contains("call i32 @static_owner_factory(ptr %ti.Widget.outer, ptr %ti.Widget.this)"),
            ir,
        )
        assertTrue(
            ir.contains("define i32 @callStaticFactoryFromInstance(ptr %this, ptr %ti.Widget.outer)"),
            ir,
        )
        assertTrue(ir.contains("addrspacecast ptr %this to ptr addrspace(1)"), ir)
        assertTrue(ir.contains("load ptr, ptr addrspace(1)"), ir)
    }

    /**
     * 按指定 partition mode 生成测试 package 的 LLVM 输出。
     */
    private fun generate(partitionMode: ModulePartitionMode) = generate(packageWithMembers(), partitionMode)

    private fun generate(chirPackage: ChirPackage, partitionMode: ModulePartitionMode) =
        DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = chirPackage,
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

    /**
     * 构造同时包含包级成员和普通模块成员的测试 package。
     */
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
            typeDefinitions = listOf(
                DefaultChirClassDeclaration(
                    semanticId = ChirSemanticId("class:Widget"),
                    name = "Widget",
                    typeDeclarationId = ChirTypeDeclarationId("pkg", "Widget"),
                    memberDeclarations = listOf(
                        simpleFunction(
                            name = "widget_member",
                            literal = "11",
                            intType = intType,
                            semanticId = ChirSemanticId("fn:Widget.widget_member"),
                        ).copy(isStatic = true),
                    ),
                ),
            ),
            packageInitFunctionId = initId,
        )
    }

    /**
     * 构造返回指定整数字面量的简单函数声明。
     */
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
