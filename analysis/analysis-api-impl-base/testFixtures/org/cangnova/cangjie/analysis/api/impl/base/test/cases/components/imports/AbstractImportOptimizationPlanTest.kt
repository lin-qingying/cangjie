package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.imports

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * 导入优化计划抽象测试。
 *
 * 这里锁定公开 `CaImportOptimizationPlan` 的四类稳定结果：
 * retained / duplicate / unused / missing。
 */
abstract class AbstractImportOptimizationPlanTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行 import optimization plan 测试。
     *
     * 方法将公开计划中的 retained、duplicate、unused、missing 四类结果规范化为字符串并排序比较。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val expectedRetained = directives[AnalysisApiComponentTestDirectives.EXPECTED_RETAINED_IMPORT].sorted()
        val expectedDuplicates = directives[AnalysisApiComponentTestDirectives.EXPECTED_DUPLICATE_IMPORT].sorted()
        val expectedUnused = directives[AnalysisApiComponentTestDirectives.EXPECTED_UNUSED_IMPORT].sorted()
        val expectedMissing = directives[AnalysisApiComponentTestDirectives.EXPECTED_MISSING_IMPORT].sorted()

        analyzeForTest(mainFile) {
            val plan = mainFile.collectImportOptimizationPlan()
            val actualRetained = plan.retainedImports.mapNotNull { importInfo ->
                importInfo.importedFqName?.asString()?.let { fqName ->
                    buildString {
                        append(fqName)
                        if (importInfo.isAllUnder) append(".*")
                        importInfo.aliasName?.let { alias -> append(" as ").append(alias) }
                    }
                }
            }.sorted()
            val actualDuplicates = plan.duplicateImports.mapNotNull { importInfo ->
                importInfo.importedFqName?.asString()?.let { fqName ->
                    buildString {
                        append(fqName)
                        if (importInfo.isAllUnder) append(".*")
                        importInfo.aliasName?.let { alias -> append(" as ").append(alias) }
                    }
                }
            }.sorted()
            val actualUnused = plan.unusedImports.mapNotNull { importInfo ->
                importInfo.importedFqName?.asString()?.let { fqName ->
                    buildString {
                        append(fqName)
                        if (importInfo.isAllUnder) append(".*")
                        importInfo.aliasName?.let { alias -> append(" as ").append(alias) }
                    }
                }
            }.sorted()
            val actualMissing = plan.missingImports.map { it.toString() }.sorted()

            assertEquals(expectedRetained, actualRetained)
            assertEquals(expectedDuplicates, actualDuplicates)
            assertEquals(expectedUnused, actualUnused)
            assertEquals(expectedMissing, actualMissing)
        }
    }
}
