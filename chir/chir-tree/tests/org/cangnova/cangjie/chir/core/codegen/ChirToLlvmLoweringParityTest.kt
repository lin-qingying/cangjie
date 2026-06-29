package org.cangnova.cangjie.chir.core.codegen

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.pipeline.DefaultChirToLlvmCodeGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

/**
 * 校验 CHIR 到 LLVM IR 降级器的基线输出与失败路径。
 *
 * 该测试覆盖简单返回、常见表达式、控制流分支以及结构校验错误，确保后端入口在写出 IR
 * 之前能够保持稳定的文本输出和明确的非法 CHIR 报错契约。
 */
class ChirToLlvmLoweringParityTest {
    /**
     * 校验无参整型返回函数会被降级为最小 LLVM 函数定义。
     *
     * 该用例固定入口块、返回终结器和常量返回值的 IR 文本，作为后续降级改动的基础回归样本。
     */
    @Test
    fun `emits exact baseline for simple return`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:main"),
            name = "main",
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
                            semanticId = ChirSemanticId("const:zero"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:simple"),
                    name = "simple",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:simple"),
                            name = "simple",
                            declarations = listOf(function),
                        ),
                    ),
                ),
                options = testOptions(),
            ),
        )

        val expected = """
define i32 @main() {
entry:
  ret i32 0
}
        """.trimIndent()
        assertEquals(expected, normalizeIr(output.modules.single().ir))
    }

    /**
     * 覆盖一元、二元、内存、调用、select 和条件分支的组合降级。
     *
     * 该用例验证表达式结果名、参数类型、基本块标签和分支目标在 LLVM 文本中保持一致，
     * 防止复杂函数降级时丢失 CHIR 语义标识或错误拼接操作数。
     */
    @Test
    fun `covers unary binary memory call and branch lowering`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val i64Type = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val ptrIntType = ChirResolvedTypeRef(ChirCPointerType(intType))
        val functionId = ChirSemanticId("fn:pipeline")

        val paramADecl = DefaultChirVariableDeclaration(ChirSemanticId("param:a"), "a", intType, mutable = false)
        val paramBDecl = DefaultChirVariableDeclaration(ChirSemanticId("param:b"), "b", intType, mutable = false)
        val paramFlagDecl = DefaultChirVariableDeclaration(ChirSemanticId("param:flag"), "flag", boolType, mutable = false)

        val paramA = ChirParameterValue(ChirSemanticId("param:a"), intType, "a", functionId)
        val paramB = ChirParameterValue(ChirSemanticId("param:b"), intType, "b", functionId)
        val paramFlag = ChirParameterValue(ChirSemanticId("param:flag"), boolType, "flag", functionId)

        val localAlloca = ChirLocalValue(ChirSemanticId("expr:alloca"), ptrIntType, "expr_alloca")
        val localLoad = ChirLocalValue(ChirSemanticId("expr:load"), intType, "expr_load")
        val localCall = ChirLocalValue(ChirSemanticId("expr:call"), intType, "expr_call")
        val localSelect = ChirLocalValue(ChirSemanticId("expr:select"), intType, "expr_select")

        val helper = ChirImportedFunctionValue(
            semanticId = ChirSemanticId("imp:helper"),
            type = ChirResolvedTypeRef(ChirFunctionType(parameterTypes = listOf(intType), returnType = intType)),
            name = "helper",
        )

        val function = DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = "pipeline",
            returnType = intType,
            parameters = listOf(paramADecl, paramBDecl, paramFlagDecl),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirUnaryExpression(
                            semanticId = ChirSemanticId("expr:neg"),
                            operator = "neg",
                            operand = paramA,
                            resultType = intType,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:mul"),
                            operator = "mul",
                            left = paramA,
                            right = paramB,
                            resultType = intType,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:alloca"),
                            operation = "alloca",
                            address = ChirConstantValue(ChirSemanticId("const:size"), i64Type, "1"),
                            resultType = ptrIntType,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:store"),
                            operation = "store",
                            address = localAlloca,
                            value = paramA,
                            resultType = null,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:load"),
                            operation = "load",
                            address = localAlloca,
                            value = null,
                            resultType = intType,
                        ),
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:call"),
                            callee = helper,
                            arguments = listOf(localLoad),
                            resultType = intType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:select"),
                            operation = "select",
                            operands = listOf(paramFlag, paramA, paramB),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirConditionalBranchTerminator(
                        semanticId = ChirSemanticId("term:branch"),
                        condition = paramFlag,
                        trueTargetBlockId = ChirSemanticId("block:then"),
                        falseTargetBlockId = ChirSemanticId("block:else"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:then"),
                    name = "then",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return-then"),
                        returnValue = localSelect,
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:else"),
                    name = "else",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return-else"),
                        returnValue = localCall,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:pipeline"),
                    name = "pipeline",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:pipeline"),
                            name = "pipeline",
                            declarations = listOf(function),
                        ),
                    ),
                ),
                options = testOptions(),
            ),
        )

        val ir = output.modules.single().ir
        assertTrue(ir.contains("define i32 @pipeline(i32 %a, i32 %b, i1 %flag) {"), ir)
        assertTrue(ir.contains("  %expr_neg = sub i32 0, %a"), ir)
        assertTrue(ir.contains("  %expr_mul = mul i32 %a, %b"), ir)
        assertTrue(ir.contains("  %expr_alloca = alloca i32, i64 1"), ir)
        assertTrue(ir.contains("  store i32 %a, ptr %expr_alloca"), ir)
        assertTrue(ir.contains("  %expr_load = load i32, ptr %expr_alloca"), ir)
        assertTrue(ir.contains("  %expr_call = call i32 @helper(i32 %expr_load)"), ir)
        assertTrue(ir.contains("  %expr_select = select i1 %flag, i32 %a, i32 %b"), ir)
        assertTrue(ir.contains("  br i1 %flag, label %then, label %else"), ir)
        assertTrue(ir.contains("then:"), ir)
        assertTrue(ir.contains("  ret i32 %expr_select"), ir)
        assertTrue(ir.contains("else:"), ir)
        assertTrue(ir.contains("  ret i32 %expr_call"), ir)
    }

    /**
     * 校验分支终结器指向缺失基本块时会在生成前失败。
     *
     * 该用例锁定后端预校验的错误路径，确保非法控制流不会继续进入 LLVM 文本写出阶段。
     */
    @Test
    fun `fails verification when branch target is missing`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:broken"),
            name = "broken",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:branch"),
                        targetBlockId = ChirSemanticId("block:missing"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        assertThrows<IllegalArgumentException> {
            DefaultChirToLlvmCodeGenerator().generate(
                ChirCodegenInput(
                    chirPackage = ChirPackage(
                        semanticId = ChirSemanticId("pkg:broken"),
                        name = "broken",
                        modules = listOf(
                            ChirModule(
                                semanticId = ChirSemanticId("mod:broken"),
                                name = "broken",
                                declarations = listOf(function),
                            ),
                        ),
                    ),
                    options = testOptions(),
                ),
            )
        }
    }

    /**
     * 校验 phi 表达式缺失前驱映射时会快速报错。
     *
     * 该用例保证 phi 降级必须具备完整的前驱来源信息，避免生成不完整或语义错误的 LLVM phi 节点。
     */
    @Test
    fun `fails fast when phi node misses predecessor mapping`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:phi"),
            name = "phi_case",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:phi"),
                            operation = "phi",
                            operands = listOf(
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:one"),
                                    type = intType,
                                    literal = "1",
                                ),
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

        val error = assertThrows<IllegalArgumentException> {
            DefaultChirToLlvmCodeGenerator().generate(
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
                    options = testOptions(),
                ),
            )
        }
        assertTrue(error.message?.contains("missing required 'pred' attribute") == true, error.message)
    }

    /**
     * 创建测试专用的代码生成选项。
     *
     * 选项开启写出前校验并关闭非必要输出，使断言只关注 CHIR 到 LLVM IR 的核心文本。
     */
    private fun testOptions() = CodegenOptions(
        enabled = true,
        verifyBeforeWrite = true,
        emitBitcode = false,
        emitComments = false,
        emitModuleHeader = false,
        emitRuntimeDeclarations = false,
    )

    /**
     * 规范化 LLVM IR 文本的换行和末尾空白。
     *
     * 该辅助方法让基线断言不受 Windows 与 Unix 换行差异影响，同时保留 IR 内容本身。
     */
    private fun normalizeIr(ir: String): String {
        return ir.replace("\r\n", "\n").trimEnd()
    }
}
