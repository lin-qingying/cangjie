package org.cangnova.cangjie.chir.core.analysis

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChirDataFlowEngineTest {

    @Test
    fun `forward engine converges and marks reachable flow`() {
        val function = sampleFunction()
        val lattice = object : ChirLattice<Boolean> {
            override fun top(): Boolean = false
            override fun join(left: Boolean, right: Boolean): Boolean = left || right
        }

        val engine = ChirDataFlowEngine(
            direction = ChirDataFlowDirection.FORWARD,
            lattice = lattice,
            entrySeed = true,
            transfer = { _, incoming -> incoming },
        )

        val result = engine.analyze(function)
        assertTrue(result.iterations > 0)
        assertTrue(result.outState.values.any { it })
    }

    @Test
    fun `backward engine converges`() {
        val function = sampleFunction()
        val lattice = object : ChirLattice<Int> {
            override fun top(): Int = 0
            override fun join(left: Int, right: Int): Int = maxOf(left, right)
        }

        val engine = ChirDataFlowEngine(
            direction = ChirDataFlowDirection.BACKWARD,
            lattice = lattice,
            exitSeed = 1,
            transfer = { _, outgoing -> outgoing + 1 },
        )

        val result = engine.analyze(function)
        assertTrue(result.iterations > 0)
        assertTrue(result.inState.values.all { it >= 1 })
    }

    private fun sampleFunction(): DefaultChirFunctionDeclaration {
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        return DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:df"),
            name = "df",
            returnType = unitType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirConditionalBranchTerminator(
                        semanticId = ChirSemanticId("term:cond"),
                        condition = org.cangnova.cangjie.chir.core.value.ChirConstantValue(
                            semanticId = ChirSemanticId("value:cond"),
                            type = ChirResolvedTypeRef(ChirPrimitiveType.BOOL),
                            literal = "true",
                        ),
                        trueTargetBlockId = ChirSemanticId("block:then"),
                        falseTargetBlockId = ChirSemanticId("block:else"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:then"),
                    name = "then",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:goto-exit"),
                        targetBlockId = ChirSemanticId("block:exit"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:else"),
                    name = "else",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:else-exit"),
                        targetBlockId = ChirSemanticId("block:exit"),
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
    }
}
