package org.cangnova.cangjie.chir.core.analysis

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.pipeline.ChirAnalysisCache
import org.cangnova.cangjie.chir.core.pipeline.ChirAnalysisDescriptor
import org.cangnova.cangjie.chir.core.pipeline.ChirDataDomain
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChirBaselineAnalysisAndProviderTest {

    @Test
    fun `baseline analyses produce stable results`() {
        val function = sampleFunction()

        val reachable = ChirReachabilityAnalysis.reachableBlocks(function)
        assertEquals(2, reachable.size)

        val types = ChirTypeFlowAnalysis.expressionTypes(function)
        assertTrue(types.containsKey(ChirSemanticId("expr:add")))

        val constants = ChirConstValueAnalysis.constants(function)
        assertEquals("1", constants[ChirSemanticId("const:one")])
    }

    @Test
    fun `analysis provider does not reuse after invalidation`() {
        val cache = ChirAnalysisCache()
        val provider = ChirAnalysisResultProvider(cache)
        val descriptor = ChirAnalysisDescriptor<Int>(
            name = "counter-analysis",
            domains = setOf(ChirDataDomain.EXPRESSION),
        )

        var counter = 0
        val first = provider.getOrCompute(descriptor) { ++counter }
        val second = provider.getOrCompute(descriptor) { ++counter }
        assertEquals(first, second)

        cache.invalidate(setOf(ChirDataDomain.EXPRESSION))

        val third = provider.getOrCompute(descriptor) { ++counter }
        assertTrue(third > second)
    }

    private fun sampleFunction(): DefaultChirFunctionDeclaration {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        return DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:baseline"),
            name = "baseline",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:add"),
                            operator = "add",
                            left = ChirConstantValue(ChirSemanticId("const:one"), intType, "1"),
                            right = ChirConstantValue(ChirSemanticId("const:two"), intType, "2"),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:to-exit"),
                        targetBlockId = ChirSemanticId("block:exit"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:exit"),
                    name = "exit",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:marker"),
                            operation = "marker",
                            operands = emptyList(),
                            resultType = null,
                        ),
                    ),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:return")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
    }
}
