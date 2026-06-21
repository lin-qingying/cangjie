package org.cangnova.cangjie.chir.core.pipeline

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChirPipelineSchedulerTest {

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
                    override val metadata: ChirPassMetadata = ChirPassMetadata(
                        name = "rewrite-expression",
                        writes = setOf(ChirDataDomain.EXPRESSION),
                    )

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

    private fun testPass(
        name: String,
        dependsOn: Set<String> = emptySet(),
        orderSink: MutableList<String>? = null,
    ): ChirPipelinePass {
        return object : ChirPipelinePass {
            override val metadata: ChirPassMetadata = ChirPassMetadata(
                name = name,
                dependsOn = dependsOn,
                reads = setOf(ChirDataDomain.CONTROL_FLOW),
                writes = emptySet(),
            )

            override fun execute(cache: ChirAnalysisCache): ChirPassExecutionOutput {
                orderSink?.add(name)
                return ChirPassExecutionOutput(summary = "ok")
            }
        }
    }
}
