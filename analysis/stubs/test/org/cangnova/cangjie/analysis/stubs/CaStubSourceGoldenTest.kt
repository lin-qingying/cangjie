package org.cangnova.cangjie.analysis.stubs

import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions
import org.junit.jupiter.api.Test

class CaStubSourceGoldenTest : AbstractAnalysisApiExecutionTest(
    "analysis/stubs/testData/source",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun topLevelDeclarations(mainFile: CjFile, testServices: TestServices) {
        val summary = CaStubSummaryBuilder().build(mainFile)
        testServices.assertions.assertEqualsToTestOutputFile(
            actual = renderSummary(summary),
            extension = ".stubs.txt",
        )
    }

    private fun renderSummary(summary: CaStubFileSummary): String {
        return buildString {
            appendLine("fileKey=${summary.fileKey.substringAfterLast('/').substringAfterLast('\\')}")
            appendLine("kind=${summary.stubKind ?: "<missing>"}")
            appendLine("package=${summary.packageFqName?.asString() ?: "<missing>"}")
            appendLine("topLevelClassifiers=${summary.topLevelClassifierNames.map { it.asString() }.sorted()}")
            appendLine("topLevelCallables=${summary.topLevelCallableNames.map { it.asString() }.sorted()}")
            appendLine("classMembers=")
            if (summary.classMemberNames.isEmpty()) {
                append("  <none>")
            } else {
                summary.classMemberNames.toSortedMap(compareBy { it.asString() }).forEach { (classId, names) ->
                    appendLine("  ${classId.asFqNameString()}=${names.map { it.asString() }.sorted()}")
                }
            }
        }.trimEnd()
    }

    private fun String.normalizeLineSeparators(): String = replace("\r\n", "\n")
}
