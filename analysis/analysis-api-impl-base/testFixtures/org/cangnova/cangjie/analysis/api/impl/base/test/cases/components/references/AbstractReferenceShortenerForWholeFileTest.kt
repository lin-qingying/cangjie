package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * `shortenWholeFile` 抽象测试。
 */
abstract class AbstractReferenceShortenerForWholeFileTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val expectedOperations = directives[AnalysisApiComponentTestDirectives.EXPECTED_REFERENCE_SHORTENING_OPERATION].sorted()

        analyzeForTest(mainFile) {
            val command = mainFile.collectReferenceShortenings(mainFile.textRange)
            val actualOperations = command.operations.map { operation ->
                listOf(
                    operation.expression.text,
                    operation.shortName.asString(),
                    operation.decision.status.name,
                    operation.decision.requiredImport?.toString() ?: "-",
                ).joinToString("|")
            }.sorted()

            assertEquals(expectedOperations, actualOperations)
        }
    }
}
