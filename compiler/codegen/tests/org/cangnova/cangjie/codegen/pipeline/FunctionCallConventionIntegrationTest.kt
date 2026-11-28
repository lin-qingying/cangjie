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

/**
 * 函数调用约定和 LLVM 属性发射的集成测试。
 */
class FunctionCallConventionIntegrationTest {
    /**
     * 验证同名重载导入函数按语义身份区分声明与调用符号。
     */
    @Test
    fun `names overloaded imported functions by semantic identity`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val functionType = ChirResolvedTypeRef(ChirFunctionType(parameterTypes = emptyList(), returnType = intType))
        val firstFunctionId = ChirSemanticId("@_CNfirstImport")
        val secondFunctionId = ChirSemanticId("@_CNsecond<init>")
        val importedFunctions = listOf(firstFunctionId, secondFunctionId).map { id ->
            DefaultChirFunctionDeclaration(
                semanticId = id,
                name = "externalOverload",
                returnType = intType,
                parameters = emptyList(),
                blocks = emptyList(),
                entryBlockId = null,
                hasBody = false,
                isImported = true,
            )
        }
        val firstCallId = ChirSemanticId("call:first")
        val secondCallId = ChirSemanticId("call:second")
        val firstCall = ChirCallExpression(
            semanticId = firstCallId,
            callee = ChirImportedFunctionValue(firstFunctionId, functionType, name = "externalOverload"),
            arguments = emptyList(),
            resultType = intType,
            resultLocalValue = ChirLocalValue(
                semanticId = firstCallId,
                type = intType,
                name = "firstResult",
                associatedExpressionId = firstCallId,
            ),
        )
        val secondCall = ChirCallExpression(
            semanticId = secondCallId,
            callee = ChirImportedFunctionValue(secondFunctionId, functionType, name = "externalOverload"),
            arguments = emptyList(),
            resultType = intType,
            resultLocalValue = ChirLocalValue(
                semanticId = secondCallId,
                type = intType,
                name = "secondResult",
                associatedExpressionId = secondCallId,
            ),
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
                    expressions = listOf(firstCall, secondCall),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = secondCallId,
                            type = intType,
                            name = "secondResult",
                            associatedExpressionId = secondCallId,
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
                    members = ChirPackageMembers(importedFunctions = importedFunctions),
                ),
                options = CodegenOptions(
                    validateChirBeforeLowering = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val ir = output.modules.single().ir
        assertTrue(ir.contains("declare i32 @_CNfirstImport()"), ir)
        assertTrue(ir.contains("""declare i32 @"_CNsecond\3Cinit\3E"()"""), ir)
        assertTrue(ir.contains("call i32 @_CNfirstImport()"), ir)
        assertTrue(ir.contains("""call i32 @"_CNsecond\3Cinit\3E"()"""), ir)
    }

    /**
     * 验证导入函数声明和调用会保留 calling convention 与参数属性。
     */
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
            entryBlockId = null,
            attributes = setOf(
                ChirStringAttribute("cc", "fastcc"),
                ChirBooleanAttribute("nounwind", enabled = true),
            ),
            hasBody = false,
            isImported = true,
        )
        val calleeType = ChirResolvedTypeRef(ChirFunctionType(parameterTypes = listOf(intType), returnType = intType))
        val callExpression = ChirCallExpression(
            semanticId = ChirSemanticId("call_ret"),
            callee = ChirImportedFunctionValue(
                semanticId = ChirSemanticId("fn:ext_add"),
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
                            semanticId = ChirSemanticId("call_ret"),
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

    /**
     * 验证函数定义头会发射 linkage、calling convention 和参数属性。
     */
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
