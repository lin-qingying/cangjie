package org.cangnova.cangjie.chir.core.transformation

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpressionDispatcher
import org.cangnova.cangjie.chir.core.expression.ChirExpressionDomainHandler
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.visitor.ChirWalker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 CHIR 变换基础设施的公共契约。
 *
 * 该测试覆盖遍历器、重写会话回滚和表达式分发器，确保变换框架在修改 CHIR 图时保持可观察且可恢复。
 */
class ChirTransformationContractsTest {

    /**
     * 校验 CHIR 遍历器会访问函数块中的全部表达式。
     *
     * 该用例使用计数器观察表达式访问次数，固定包、模块、函数、块到表达式的遍历路径。
     */
    @Test
    fun `walker visits all expressions`() {
        val pkg = samplePackage()
        var expressionCount = 0

        val walker = object : ChirWalker() {
            /**
             * 记录遍历器访问到的表达式数量。
             *
             * 该回调只统计访问次数，不改变被遍历的 CHIR 图。
             */
            override fun onExpression(expression: org.cangnova.cangjie.chir.core.expression.ChirExpression) {
                expressionCount += 1
            }
        }
        walker.visitPackage(pkg)

        assertEquals(1, expressionCount)
    }

    /**
     * 校验重写会话在生成非法图时会回滚到原始快照。
     *
     * 该用例故意把入口块分支改到缺失目标，确认被拒绝的变更不会污染会话内的 CHIR 包。
     */
    @Test
    fun `rewrite session rolls back invalid graph mutation`() {
        val pkg = samplePackage()
        val session = ChirRewriteSession(pkg)

        val result = session.apply { original ->
            original.copy(
                modules = original.modules.map { module ->
                    module.copy(
                        declarations = module.declarations.map { declaration ->
                            val fn = declaration as? DefaultChirFunctionDeclaration ?: return@map declaration
                            fn.copy(
                                blocks = fn.blocks.map { block ->
                                    if (block.semanticId.value == "block:entry") {
                                        block.copy(
                                            terminator = ChirBranchTerminator(
                                                semanticId = ChirSemanticId("term:bad"),
                                                targetBlockId = ChirSemanticId("block:missing"),
                                            ),
                                        )
                                    } else {
                                        block
                                    }
                                },
                            )
                        },
                    )
                },
            )
        }

        assertTrue(result is ChirRewriteResult.Rejected)
        val unchanged = session.snapshot()
        assertEquals(pkg, unchanged)
    }

    /**
     * 校验表达式分发器会按表达式领域调用对应处理器。
     *
     * 该用例以二元表达式为样本，固定 dispatcher 到 `handleBinary` 的路由契约。
     */
    @Test
    fun `dispatcher routes binary expression by domain`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val expression = ChirBinaryExpression(
            semanticId = ChirSemanticId("expr:bin"),
            operator = "add",
            left = ChirLocalValue(ChirSemanticId("v:left"), intType, "left"),
            right = ChirLocalValue(ChirSemanticId("v:right"), intType, "right"),
            resultType = intType,
        )

        val tag = ChirExpressionDispatcher.dispatch(expression, object : ChirExpressionDomainHandler<String> {
            override fun handleUnary(expression: org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression): String = "unary"
            override fun handleBinary(expression: ChirBinaryExpression): String = "binary"
            override fun handleMemory(expression: org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression): String = "memory"
            override fun handleCall(expression: ChirCallExpression): String = "call"
            override fun handleOthers(expression: org.cangnova.cangjie.chir.core.expression.ChirOtherExpression): String = "others"
        })

        assertEquals("binary", tag)
    }

    /**
     * 构造包含一个二元表达式和两个基本块的变换样本包。
     *
     * 样本用于遍历、重写和分发测试共享同一份 CHIR 结构，避免各用例隐含不同图形假设。
     */
    private fun samplePackage(): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val expression = ChirBinaryExpression(
            semanticId = ChirSemanticId("expr:1"),
            operator = "add",
            left = ChirLocalValue(ChirSemanticId("v:left"), intType, "left"),
            right = ChirLocalValue(ChirSemanticId("v:right"), intType, "right"),
            resultType = intType,
        )

        val fn = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:sample"),
            name = "sample",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(expression),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:branch"),
                        targetBlockId = ChirSemanticId("block:exit"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:exit"),
                    name = "exit",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("v:ret"), intType, "ret"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        return ChirPackage(
            semanticId = ChirSemanticId("pkg:sample"),
            name = "sample.pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:sample"),
                    name = "sample.mod",
                    declarations = listOf(fn),
                ),
            ),
        )
    }
}
