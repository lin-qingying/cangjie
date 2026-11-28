package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOverflowStrategy
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 二元操作符别名到 LLVM lowering 的集成测试。
 */
class BinaryOperatorAliasIntegrationTest {
    /**
     * 验证别名操作符会通过枚举解析映射到正确 LLVM 指令。
     */
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

    /** 验证仓颉三种官方幂运算签名使用各自规定的 LLVM/运行时 ABI。 */
    @Test
    fun `lowers official power signatures through runtime ABIs`() {
        val functions = listOf(
            powerFunction(
                name = "power_float64_float64",
                leftPrimitive = ChirPrimitiveType.FLOAT64,
                rightPrimitive = ChirPrimitiveType.FLOAT64,
                resultPrimitive = ChirPrimitiveType.FLOAT64,
            ),
            powerFunction(
                name = "power_float64_int64",
                leftPrimitive = ChirPrimitiveType.FLOAT64,
                rightPrimitive = ChirPrimitiveType.INT64,
                resultPrimitive = ChirPrimitiveType.FLOAT64,
            ),
            powerFunction(
                name = "power_int64_uint64",
                leftPrimitive = ChirPrimitiveType.INT64,
                rightPrimitive = ChirPrimitiveType.UINT64,
                resultPrimitive = ChirPrimitiveType.INT64,
                overflowStrategy = ChirOverflowStrategy.WRAPPING,
            ),
        )
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:power"),
                    name = "power",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:power"),
                            name = "power",
                            declarations = functions,
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
        assertTrue(ir.contains("declare double @CJ_CORE_CPow(double, double)"), ir)
        assertTrue(ir.contains("call double @CJ_CORE_CPow(double %base, double %exponent)"), ir)
        assertTrue(ir.contains("declare double @CJ_CORE_FastPowerDoubleInt64(double, i64)"), ir)
        assertTrue(ir.contains("call double @CJ_CORE_FastPowerDoubleInt64(double %base, i64 %exponent)"), ir)
        assertTrue(ir.contains("call i64 @cangjie_fast_power_i64_u64(i64 %base, i64 %exponent)"), ir)
        assertTrue(ir.contains("define internal i64 @cangjie_fast_power_i64_u64(i64 %base, i64 %exponent)"), ir)
    }

    private fun powerFunction(
        name: String,
        leftPrimitive: ChirPrimitiveType,
        rightPrimitive: ChirPrimitiveType,
        resultPrimitive: ChirPrimitiveType,
        overflowStrategy: ChirOverflowStrategy = ChirOverflowStrategy.NOT_APPLICABLE,
    ): DefaultChirFunctionDeclaration {
        val functionId = ChirSemanticId("fn:$name")
        val blockId = ChirSemanticId("block:$name")
        val leftParameterId = ChirSemanticId("param:$name:base")
        val rightParameterId = ChirSemanticId("param:$name:exponent")
        val leftType = ChirResolvedTypeRef(leftPrimitive)
        val rightType = ChirResolvedTypeRef(rightPrimitive)
        val resultType = ChirResolvedTypeRef(resultPrimitive)
        val expressionId = ChirSemanticId("expr:$name")
        val expression = ChirBinaryExpression(
            semanticId = expressionId,
            operator = "pow",
            left = ChirParameterValue(leftParameterId, leftType, "base", functionId),
            right = ChirParameterValue(rightParameterId, rightType, "exponent", functionId),
            resultType = resultType,
            overflowStrategy = overflowStrategy,
        )
        return DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = name,
            returnType = resultType,
            parameters = listOf(
                DefaultChirVariableDeclaration(leftParameterId, "base", leftType, mutable = false),
                DefaultChirVariableDeclaration(rightParameterId, "exponent", rightType, mutable = false),
            ),
            blocks = listOf(
                ChirBlock(
                    semanticId = blockId,
                    name = "entry",
                    expressions = listOf(expression),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:$name"),
                        returnValue = ChirLocalValue(expressionId, resultType, name),
                    ),
                ),
            ),
            entryBlockId = blockId,
        )
    }
}
