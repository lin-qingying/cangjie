package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * 引用缩短计划抽象测试。
 *
 * 这里不执行 PSI 改写，只验证公开计划对象是否稳定表达：
 * 1. 哪个表达式可被缩短；
 * 2. 缩短后的短名；
 * 3. 当前可见性决策；
 * 4. 是否需要补 import。
 */
abstract class AbstractReferenceShorteningPlanTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行文件级引用缩短计划测试。
     *
     * 方法只比较公开 plan 中的 operations，不执行真实 PSI 改写。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val expectedOperations = directives[AnalysisApiComponentTestDirectives.EXPECTED_REFERENCE_SHORTENING_OPERATION].sorted()

        analyzeForTest(mainFile) {
            val plan = mainFile.collectReferenceShorteningPlan()
            val actualOperations = plan.operations.map { operation ->
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
