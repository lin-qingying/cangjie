package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.imports

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * 默认导入抽象测试。
 *
 * 这里直接锁定 use-site session 公开暴露的默认导入视图，
 * 避免 default imports 被后续实现改动悄悄退化成宿主私有细节。
 */
abstract class AbstractDefaultImportsTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val expectedRegularImports = directives[AnalysisApiComponentTestDirectives.EXPECTED_DEFAULT_REGULAR_IMPORT]
        val expectedLowPriorityImports = directives[AnalysisApiComponentTestDirectives.EXPECTED_DEFAULT_LOW_PRIORITY_IMPORT]
        val expectedExcludedImports = directives[AnalysisApiComponentTestDirectives.EXPECTED_EXCLUDED_IMPORT]

        analyzeForTest(mainFile) {
            val imports = defaultImports
            val actualRegularImports = imports.regularImports.map { it.toString() }
            val actualLowPriorityImports = imports.lowPriorityImports.map { it.toString() }
            val actualExcludedImports = imports.excludedImports.map { it.asString() }
            val actualAllImports = imports.allImports.map { it.toString() }

            assertEquals(expectedRegularImports, actualRegularImports)
            assertEquals(expectedLowPriorityImports, actualLowPriorityImports)
            assertEquals(expectedExcludedImports, actualExcludedImports)
            assertEquals(expectedRegularImports + expectedLowPriorityImports, actualAllImports)
        }
    }
}
