package org.cangnova.cangjie.chir.core.regression

import org.cangnova.cangjie.chir.core.analysis.ChirReachabilityAnalysis
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.pipeline.ChirAnalysisCache
import org.cangnova.cangjie.chir.core.pipeline.ChirPassContext
import org.cangnova.cangjie.chir.core.pipeline.ChirPassExecutionOutput
import org.cangnova.cangjie.chir.core.pipeline.ChirPassMetadata
import org.cangnova.cangjie.chir.core.pipeline.ChirPipelinePass
import org.cangnova.cangjie.chir.core.pipeline.ChirPipelineScheduler
import org.cangnova.cangjie.chir.core.printer.ChirInspector
import org.cangnova.cangjie.chir.core.printer.ChirPrinter
import org.cangnova.cangjie.chir.core.testkit.ChirTestAssertions
import org.cangnova.cangjie.chir.core.testkit.ChirTestFixtures
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 覆盖 CHIR 核心能力之间的端到端回归组合。
 *
 * 该测试把验证器、序列化、打印、分析、流水线调度和检查器放在同一份样本上运行，
 * 用于及时发现核心工具之间的契约断裂。
 */
class ChirRegressionSuiteTest {

    /**
     * 校验基线回归套件覆盖不变量、往返序列化、流水线和调试工具。
     *
     * 该用例不追求单一组件细节，而是固定 CHIR 样本在多个公共工具中的联合可用性。
     */
    @Test
    fun `baseline regression suite covers invariant roundtrip pipeline and tooling`() {
        val chirPackage = ChirTestFixtures.codecPackage()

        val report = DefaultChirValidator().validatePackage(chirPackage)
        assertFalse(report.hasErrors)

        ChirTestAssertions.assertCodecRoundTrip(chirPackage)
        ChirTestAssertions.assertPrinterStable(chirPackage)

        val function = chirPackage.modules.single().declarations.single() as org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
        val reachable = ChirReachabilityAnalysis.reachableBlocks(function)
        assertTrue(reachable.contains(function.entryBlockId))

        val scheduler = ChirPipelineScheduler(
            listOf(
                object : ChirPipelinePass {
                    /**
                     * 定义测试流水线 pass 的最小元数据。
                     *
                     * 该名称用于调度器记录执行顺序，确保匿名 pass 也遵守标准元数据契约。
                     */
                    override val metadata: ChirPassMetadata = ChirPassMetadata(name = "smoke")

                    /**
                     * 执行测试流水线 pass 并返回固定摘要。
                     *
                     * 该实现只验证调度和记录链路，不修改分析缓存内容。
                     */
                    override fun execute(cache: ChirAnalysisCache): ChirPassExecutionOutput =
                        ChirPassExecutionOutput(summary = "ok")
                },
            ),
        )
        val context = ChirPassContext()
        scheduler.execute(context, ChirAnalysisCache())
        assertTrue(context.records.size == 1)

        val printed = ChirPrinter.print(chirPackage)
        val inspected = ChirInspector.inspect(chirPackage)
        assertTrue(printed.contains("package codec.pkg"))
        assertTrue(inspected.contains("\"functionCount\": 1"))
    }
}
