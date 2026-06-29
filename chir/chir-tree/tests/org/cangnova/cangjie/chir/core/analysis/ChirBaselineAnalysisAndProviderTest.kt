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

/**
 * 校验 CHIR 基线分析和分析结果提供器的缓存契约。
 *
 * 该测试覆盖可达性、类型流、常量提取以及缓存失效，确保分析管线在共享数据域上保持稳定行为。
 */
class ChirBaselineAnalysisAndProviderTest {

    /**
     * 校验内置基线分析会产出稳定且可查询的结果。
     *
     * 该用例固定可达块数量、表达式类型映射和常量值映射，作为分析结果结构的回归基线。
     */
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

    /**
     * 校验分析结果提供器在对应数据域失效后不会复用旧结果。
     *
     * 该用例通过计数器观察缓存命中和失效后的重新计算，确保分析缓存遵循数据域级别的生命周期。
     */
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

    /**
     * 构造包含表达式、常量和两段控制流的分析样本函数。
     *
     * 样本同时服务可达性分析、类型流分析和常量分析，保证三个基线分析读取同一份 CHIR 图。
     */
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
