package org.cangnova.cangjie.analysis.stubs

import org.junit.jupiter.api.Test
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

class CaStubSourceGoldenTest {
    @Test
    fun sourceSummaries() {
        CaStubTestSupport.withEnvironment("CaStubSourceGoldenTest") { environment ->
            val fixtureDir = CaStubTestSupport.locateRepositoryRoot()
                .resolve("analysis")
                .resolve("stubs")
                .resolve("testData")
                .resolve("source")

            val summaryBuilder = CaStubSummaryBuilder()
            val sourceFiles = fixtureDir.listDirectoryEntries("*.cj").sortedBy { it.name }
            require(sourceFiles.isNotEmpty()) { "No source stub fixtures found under $fixtureDir" }

            sourceFiles.forEach { sourceFile ->
                val psiFile = CaStubTestSupport.createSourceFile(
                    environment = environment,
                    fileName = sourceFile.name,
                    text = sourceFile.readText(),
                )
                val summary = summaryBuilder.build(psiFile)
                val actual = CaStubTestSupport.renderSummary(summary)
                val expectedFile = sourceFile.resolveSibling("${sourceFile.fileName.toString().removeSuffix(".${sourceFile.extension}")}.stubs.txt")
                CaStubTestSupport.assertMatchesGolden(actual, expectedFile)
            }
        }
    }
}
