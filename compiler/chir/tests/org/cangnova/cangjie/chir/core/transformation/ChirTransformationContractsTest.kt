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

class ChirTransformationContractsTest {

    @Test
    fun `walker visits all expressions`() {
        val pkg = samplePackage()
        var expressionCount = 0

        val walker = object : ChirWalker() {
            override fun onExpression(expression: org.cangnova.cangjie.chir.core.expression.ChirExpression) {
                expressionCount += 1
            }
        }
        walker.visitPackage(pkg)

        assertEquals(1, expressionCount)
    }

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
