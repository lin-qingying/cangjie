package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.ChirPackageMembers
import org.cangnova.cangjie.chir.core.type.ChirBoxType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirEnumCaseType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirThisType
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TypeDeclarationIntegrationTest {
    @Test
    fun `emits nominal type declarations from all CHIR use sites`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val recordType = ChirResolvedTypeRef(
            ChirStructType(
                name = "demo:Record<Int32>",
                fieldTypes = listOf(intType),
            ),
        )
        val boxRecordType = ChirResolvedTypeRef(ChirBoxType(recordType))
        val resultEnumType = ChirResolvedTypeRef(
            ChirEnumType(
                name = "demo:Result",
                cases = listOf(ChirEnumCaseType("ok", payloadTypes = listOf(recordType))),
            ),
        )

        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:make_record"),
            name = "make_record",
            returnType = recordType,
            parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:box"),
                    name = "box",
                    type = boxRecordType,
                    mutable = false,
                ),
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:state"),
                    name = "state",
                    type = resultEnumType,
                    mutable = false,
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
                            semanticId = ChirSemanticId("const:record"),
                            type = recordType,
                            literal = "zeroinitializer",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val ir = generateIr(
            ChirPackage(
                semanticId = ChirSemanticId("pkg:types"),
                name = "types",
                modules = listOf(
                    ChirModule(
                        semanticId = ChirSemanticId("mod:types"),
                        name = "types",
                        declarations = listOf(function),
                    ),
                ),
            ),
        )

        assertTrue(ir.contains("%struct.demo_Record_Int32 = type { i32 }"), ir)
        assertTrue(ir.contains("%box.demo_Record_Int32 = type { %struct.demo_Record_Int32 }"), ir)
        assertTrue(ir.contains("%enum.demo_Result = type { i32 }"), ir)
        assertTrue(
            ir.contains("define %struct.demo_Record_Int32 @make_record(%box.demo_Record_Int32 %box, %enum.demo_Result %state)"),
            ir,
        )
    }

    @Test
    fun `uses canonical names for declared class named and this types`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val aliasType = ChirResolvedTypeRef(
            ChirNamedType(
                renderName = "demo:Alias",
                typeArguments = listOf(intType),
            ),
        )
        val thisType = ChirResolvedTypeRef(ChirThisType("demo:Widget"))
        val classType = ChirResolvedTypeRef(
            ChirClassType(
                name = "demo:Widget",
                fieldTypes = listOf(intType),
            ),
        )

        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:alias"),
            name = "alias",
            returnType = aliasType,
            parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:this"),
                    name = "self",
                    type = thisType,
                    mutable = false,
                ),
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:widget"),
                    name = "widget",
                    type = classType,
                    mutable = false,
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
                            semanticId = ChirSemanticId("const:alias"),
                            type = aliasType,
                            literal = "zeroinitializer",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val ir = generateIr(
            ChirPackage(
                semanticId = ChirSemanticId("pkg:canonical"),
                name = "canonical",
                modules = listOf(
                    ChirModule(
                        semanticId = ChirSemanticId("mod:canonical"),
                        name = "canonical",
                        declarations = listOf(function),
                    ),
                ),
                typeDefinitions = listOf(
                    DefaultChirClassDeclaration(
                        semanticId = ChirSemanticId("type:widget"),
                        name = "demo:Widget",
                        memberDeclarations = listOf(
                            DefaultChirVariableDeclaration(
                                semanticId = ChirSemanticId("field:value"),
                                name = "value",
                                type = intType,
                                mutable = true,
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(ir.contains("%class.demo_Widget = type { i32 }"), ir)
        assertTrue(ir.contains("%type.demo_Alias_int32 = type opaque"), ir)
        assertTrue(ir.contains("%this.demo_Widget = type opaque"), ir)
        assertTrue(
            ir.contains("define %type.demo_Alias_int32 @alias(%this.demo_Widget %self, %class.demo_Widget %widget)"),
            ir,
        )
    }

    @Test
    fun `rejects memory lowering without pointer result contract`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:broken_alloca"),
            name = "broken_alloca",
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
                                type = intType,
                                literal = "1",
                            ),
                            resultType = intType,
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
        )

        val error = assertThrows<CodegenLoweringException> {
            generateIr(
                ChirPackage(
                    semanticId = ChirSemanticId("pkg:broken_alloca"),
                    name = "broken_alloca",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:broken_alloca"),
                            name = "broken_alloca",
                            declarations = listOf(function),
                        ),
                    ),
                ),
                validateBeforeLowering = false,
            )
        }

        assertTrue(error.message?.contains("memory operation requires pointer/ref type") == true, error.message)
    }

    @Test
    fun `rejects aggregate global without explicit initializer`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val recordType = ChirResolvedTypeRef(
            ChirStructType(
                name = "demo:GlobalRecord",
                fieldTypes = listOf(intType),
            ),
        )

        val error = assertThrows<CodegenLoweringException> {
            generateIr(
                ChirPackage(
                    semanticId = ChirSemanticId("pkg:broken_global"),
                    name = "broken_global",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:broken_global"),
                            name = "broken_global",
                            declarations = emptyList(),
                        ),
                    ),
                    members = ChirPackageMembers(
                        globalVariables = listOf(
                            DefaultChirVariableDeclaration(
                                semanticId = ChirSemanticId("global:record"),
                                name = "record",
                                type = recordType,
                                mutable = true,
                            ),
                        ),
                    ),
                ),
            )
        }

        assertTrue(error.message?.contains("requires an explicit initializer") == true, error.message)
    }

    private fun generateIr(
        chirPackage: ChirPackage,
        validateBeforeLowering: Boolean = true,
    ): String {
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = chirPackage,
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = validateBeforeLowering,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )
        return output.modules.single().ir
    }
}
