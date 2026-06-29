package org.cangnova.cangjie.chir.core.pipeline

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 CHIR 流水线调度器的依赖排序、错误检测和缓存失效行为。
 *
 * 该测试使用轻量 pass 固定调度器对依赖图、数据域写入和执行记录的处理契约。
 */
class ChirPipelineSchedulerTest {

    /**
     * 校验调度器会根据依赖关系重新排序 pass。
     *
     * 该用例确认被依赖的 pass 先执行，并且执行上下文记录所有 pass 的运行结果。
     */
    @Test
    fun `scheduler orders passes by dependency`() {
        val order = mutableListOf<String>()
        val scheduler = ChirPipelineScheduler(
            listOf(
                testPass("type-lowering", dependsOn = setOf("build-cfg"), orderSink = order),
                testPass("build-cfg", orderSink = order),
            ),
        )

        val context = ChirPassContext()
        scheduler.execute(context, ChirAnalysisCache())

        assertEquals(listOf("build-cfg", "type-lowering"), order)
        assertEquals(2, context.records.size)
    }

    /**
     * 校验调度器会拒绝存在环的依赖图。
     *
     * 该用例固定循环依赖的失败行为，防止流水线在无法拓扑排序时继续执行。
     */
    @Test
    fun `scheduler fails on cyclic dependency`() {
        val scheduler = ChirPipelineScheduler(
            listOf(
                testPass("a", dependsOn = setOf("b")),
                testPass("b", dependsOn = setOf("a")),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            scheduler.sortedPasses()
        }
    }

    /**
     * 校验 pass 写入数据域后会使对应分析缓存失效。
     *
     * 该用例同时检查缓存删除、触达节点记录和渲染摘要，保证调度执行结果可追踪。
     */
    @Test
    fun `analysis cache invalidates domains touched by pass`() {
        val analysisCache = ChirAnalysisCache()
        val descriptor = ChirAnalysisDescriptor<String>(
            name = "const-analysis",
            domains = setOf(ChirDataDomain.EXPRESSION),
        )
        analysisCache.put(descriptor, "cached")

        val scheduler = ChirPipelineScheduler(
            listOf(
                object : ChirPipelinePass {
                    /**
                     * 描述该测试 pass 会写入表达式数据域。
                     *
                     * 调度器据此在执行后失效表达式相关分析缓存。
                     */
                    override val metadata: ChirPassMetadata = ChirPassMetadata(
                        name = "rewrite-expression",
                        writes = setOf(ChirDataDomain.EXPRESSION),
                    )

                    /**
                     * 执行表达式重写测试 pass 并报告触达节点。
                     *
                     * 该实现不直接修改 CHIR，只返回足以驱动缓存失效和执行记录的元数据。
                     */
                    override fun execute(cache: ChirAnalysisCache): ChirPassExecutionOutput {
                        return ChirPassExecutionOutput(
                            touchedNodes = setOf(ChirSemanticId("expr:1")),
                            summary = "rewrote expression",
                        )
                    }
                },
            ),
        )

        val context = ChirPassContext()
        scheduler.execute(context, analysisCache)

        assertTrue(analysisCache.get(descriptor) == null)
        val record = context.records.single()
        assertTrue(record.summary?.contains("invalidates=EXPRESSION") == true)
        assertTrue(record.touchedNodes.contains(ChirSemanticId("expr:1")))
        assertTrue(context.renderSummary().contains("rewrite-expression"))
    }

    /**
     * 校验调度器会拒绝不存在的依赖名称。
     *
     * 该用例固定缺失依赖的异常路径，避免流水线配置错误被静默忽略。
     */
    @Test
    fun `scheduler fails for missing dependency`() {
        val scheduler = ChirPipelineScheduler(
            listOf(
                testPass("optimize", dependsOn = setOf("missing-pass")),
            ),
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            scheduler.sortedPasses()
        }
        assertNotNull(ex.message)
    }

    /**
     * 创建可记录执行顺序的测试流水线 pass。
     *
     * 该辅助方法统一构造依赖、读写数据域和可选执行顺序记录，供调度器用例复用。
     */
    private fun testPass(
        name: String,
        dependsOn: Set<String> = emptySet(),
        orderSink: MutableList<String>? = null,
    ): ChirPipelinePass {
        return object : ChirPipelinePass {
            /**
             * 描述测试 pass 的名称、依赖和数据域读写集合。
             *
             * 调度器依据这些元数据完成拓扑排序和后续缓存失效决策。
             */
            override val metadata: ChirPassMetadata = ChirPassMetadata(
                name = name,
                dependsOn = dependsOn,
                reads = setOf(ChirDataDomain.CONTROL_FLOW),
                writes = emptySet(),
            )

            /**
             * 执行测试 pass，并在需要时记录执行顺序。
             *
             * 返回固定摘要以便调度上下文能够生成稳定的执行记录。
             */
            override fun execute(cache: ChirAnalysisCache): ChirPassExecutionOutput {
                orderSink?.add(name)
                return ChirPassExecutionOutput(summary = "ok")
            }
        }
    }
}
