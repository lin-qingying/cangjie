package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * `shortenWholeFile` 抽象测试。
 *
 * 该测试验证整个文件范围内的引用缩短命令是否与公开 shortening plan 期望一致。
 */
abstract class AbstractReferenceShortenerForWholeFileTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行整文件引用缩短命令测试。
     *
     * 方法收集文件 textRange 内所有 shortening operations，并以稳定管道分隔格式比较。
     */
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
