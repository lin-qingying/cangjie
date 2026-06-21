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

class ChirRegressionSuiteTest {

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
                    override val metadata: ChirPassMetadata = ChirPassMetadata(name = "smoke")
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
