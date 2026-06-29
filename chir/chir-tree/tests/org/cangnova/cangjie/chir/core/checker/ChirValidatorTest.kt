package org.cangnova.cangjie.chir.core.checker

import org.cangnova.cangjie.chir.core.context.DefaultChirContext
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.pipeline.ChirPipelineGate
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirUnresolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 CHIR 验证器对控制流、类型、表达式和终结器错误的诊断覆盖。
 *
 * 该测试固定验证报告、格式化输出和流水线入口的失败契约，确保非法 CHIR 图不会进入后续阶段。
 */
class ChirValidatorTest {

    /**
     * 校验验证器会报告非法 CFG，且格式化器包含错误上下文。
     *
     * 该用例固定缺失分支目标的 `INVALID_CFG` 诊断和函数节点标识输出。
     */
    @Test
    fun `validator reports invalid cfg and formatter emits context`() {
        val pkg = invalidPackage()
        val report = DefaultChirValidator().validatePackage(pkg, DefaultChirContext())

        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "INVALID_CFG" })

        val formatted = ChirValidationReportFormatter.render(report)
        assertTrue(formatted.contains("INVALID_CFG"))
        assertTrue(formatted.contains("node=fn:invalid"))
    }

    /**
     * 校验流水线门禁会阻止非法 CHIR 包进入指定阶段。
     *
     * 该用例确保阶段入口复用验证器结果，并以异常形式暴露不可继续处理的包。
     */
    @Test
    fun `pipeline gate blocks invalid package`() {
        val pkg = invalidPackage()
        assertThrows(IllegalArgumentException::class.java) {
            ChirPipelineGate.requireValidForStage(pkg, stageName = "unit-test")
        }
    }

    /**
     * 校验返回值类型与函数声明返回类型不一致时会产生错误。
     *
     * 该用例构造返回布尔值的整型函数，固定 `RETURN_TYPE_MISMATCH` 诊断。
     */
    @Test
    fun `validator reports return type mismatch as error`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:return-mismatch"),
            name = "returnMismatch",
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
        )
        val pkg = packageWith(function)

        val report = DefaultChirValidator().validatePackage(pkg, DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "RETURN_TYPE_MISMATCH" })
    }

    /**
     * 校验自定义类型成员函数中的错误也会被验证器递归报告。
     *
     * 该用例确保类声明内部的成员函数不会绕过返回类型检查，并保留出错终结器节点标识。
     */
    @Test
    fun `validator reports member function errors inside custom type declarations`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val memberFunction = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:Box.badMember"),
            name = "badMember",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:Box.badMember"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:Box.badMember:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:Box.badMember:true"),
                            type = boolType,
                            literal = "true",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:Box.badMember"),
        )
        val box = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:Box"),
            name = "Box",
            memberDeclarations = listOf(memberFunction),
        )

        val report = DefaultChirValidator().validatePackage(packageWith(box), DefaultChirContext())

        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "RETURN_TYPE_MISMATCH" && it.nodeId == ChirSemanticId("term:Box.badMember:return") })
    }

    /**
     * 校验条件分支的条件值必须是布尔类型。
     *
     * 该用例使用整型常量作为条件，固定 `BRANCH_CONDITION_TYPE_MISMATCH` 诊断。
     */
    @Test
    fun `validator rejects non bool conditional branch condition`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:bad-branch-condition"),
            name = "badBranchCondition",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirConditionalBranchTerminator(
                        semanticId = ChirSemanticId("term:conditional"),
                        condition = ChirConstantValue(
                            semanticId = ChirSemanticId("const:one"),
                            type = intType,
                            literal = "1",
                        ),
                        trueTargetBlockId = ChirSemanticId("block:true"),
                        falseTargetBlockId = ChirSemanticId("block:false"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:true"),
                    name = "true",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:true:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:true:return"), intType, "1"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:false"),
                    name = "false",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:false:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:false:return"), intType, "0"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val report = DefaultChirValidator().validatePackage(packageWith(function), DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "BRANCH_CONDITION_TYPE_MISMATCH" })
    }

    /**
     * 校验函数调用实参与形参类型不一致时会产生错误。
     *
     * 该用例构造需要整型参数却传入布尔常量的调用表达式，固定调用参数类型诊断。
     */
    @Test
    fun `validator reports call argument type mismatch`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)

        val callExpr = ChirCallExpression(
            semanticId = ChirSemanticId("expr:call"),
            callee = ChirImportedFunctionValue(
                semanticId = ChirSemanticId("imp:callee"),
                type = ChirResolvedTypeRef(ChirFunctionType(parameterTypes = listOf(intType), returnType = intType)),
                name = "callee",
            ),
            arguments = listOf(
                ChirConstantValue(
                    semanticId = ChirSemanticId("const:true"),
                    type = boolType,
                    literal = "true",
                ),
            ),
            resultType = intType,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:call-mismatch"),
            name = "callMismatch",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(callExpr),
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
        val pkg = packageWith(function)

        val report = DefaultChirValidator().validatePackage(pkg, DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "CALL_ARGUMENT_TYPE_MISMATCH" })
    }

    /**
     * 校验未知的 other 表达式操作不会被验证器接受。
     *
     * 该用例固定 `UNSUPPORTED_OTHER_OPERATION` 诊断，防止后端处理未定义扩展操作。
     */
    @Test
    fun `validator rejects unsupported other operation`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unsupported-other"),
            name = "unsupportedOther",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:unknown"),
                            operation = "mystery-op",
                            operands = emptyList(),
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
        val pkg = packageWith(function)

        val report = DefaultChirValidator().validatePackage(pkg, DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "UNSUPPORTED_OTHER_OPERATION" })
    }

    /**
     * 校验未知一元操作符会被验证器拒绝。
     *
     * 该用例固定 `UNSUPPORTED_UNARY_OPERATOR` 诊断，确保操作集合之外的一元表达式无法通过校验。
     */
    @Test
    fun `validator rejects unsupported unary operator`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unsupported-unary"),
            name = "unsupportedUnary",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirUnaryExpression(
                            semanticId = ChirSemanticId("expr:unary"),
                            operator = "mystery-unary",
                            operand = ChirConstantValue(
                                semanticId = ChirSemanticId("const:one"),
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
        val report = DefaultChirValidator().validatePackage(packageWith(function), DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "UNSUPPORTED_UNARY_OPERATOR" })
    }

    /**
     * 校验未知二元操作符会被验证器拒绝。
     *
     * 该用例固定 `UNSUPPORTED_BINARY_OPERATOR` 诊断，确保操作集合之外的二元表达式无法通过校验。
     */
    @Test
    fun `validator rejects unsupported binary operator`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unsupported-binary"),
            name = "unsupportedBinary",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:binary"),
                            operator = "mystery-binary",
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
        val report = DefaultChirValidator().validatePackage(packageWith(function), DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "UNSUPPORTED_BINARY_OPERATOR" })
    }

    /**
     * 校验降级前遗留的未解析类型引用会被验证器拒绝。
     *
     * 该用例固定 `UNRESOLVED_TYPE_REFERENCE` 诊断，防止类型消解不完整的 CHIR 进入后续阶段。
     */
    @Test
    fun `validator rejects unresolved type references before lowering`() {
        val unresolved = ChirUnresolvedTypeRef("TPlaceholder")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unresolved-type"),
            name = "unresolvedType",
            returnType = unresolved,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:return")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        val pkg = packageWith(function)

        val report = DefaultChirValidator().validatePackage(pkg, DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "UNRESOLVED_TYPE_REFERENCE" })
    }

    /**
     * 校验未知终结器实现会被验证器拒绝。
     *
     * 该用例通过匿名终结器模拟未纳入验证器分派表的控制流结尾，固定 `UNSUPPORTED_TERMINATOR` 诊断。
     */
    @Test
    fun `validator rejects unsupported terminator kind`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val unsupported = object : ChirTerminator {
            /**
             * 未知终结器的测试语义标识。
             *
             * 该标识用于验证报告定位 unsupported terminator 的节点来源。
             */
            override val semanticId: ChirSemanticId = ChirSemanticId("term:unknown")
        }
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unsupported-terminator"),
            name = "unsupportedTerminator",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = unsupported,
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val report = DefaultChirValidator().validatePackage(packageWith(function), DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "UNSUPPORTED_TERMINATOR" })
    }

    /**
     * 构造包含缺失分支目标的非法 CHIR 包。
     *
     * 该样本用于 CFG 校验和流水线门禁测试，统一提供可复现的非法控制流图。
     */
    private fun invalidPackage(): ChirPackage {
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:invalid"),
            name = "invalid",
            returnType = ChirResolvedTypeRef(ChirPrimitiveType.INT32),
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
                ChirBlock(
                    semanticId = ChirSemanticId("block:exit"),
                    name = "exit",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:return")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        return ChirPackage(
            semanticId = ChirSemanticId("pkg:invalid"),
            name = "invalid.pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:invalid"),
                    name = "invalid.mod",
                    declarations = listOf(function),
                ),
            ),
        )
    }

    /**
     * 用给定声明构造单模块测试包。
     *
     * 该辅助方法为验证器用例提供统一包壳，使每个测试只关注需要触发的声明级错误。
     */
    private fun packageWith(vararg declarations: ChirDeclaration): ChirPackage {
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:test"),
            name = "test.pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:test"),
                    name = "test.mod",
                    declarations = declarations.toList(),
                ),
            ),
        )
    }
}
