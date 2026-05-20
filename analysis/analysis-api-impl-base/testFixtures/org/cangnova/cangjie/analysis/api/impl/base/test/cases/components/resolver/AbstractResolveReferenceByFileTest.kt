package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver

import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.isUsageSimpleNameForAnalysisApiTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `allByPsi` 文件级引用解析测试。
 *
 * 当前仓颉公开 API 只对 `CjReferenceExpression` 暴露 Analysis API 解析入口，
 * 因而这里仅遍历拥有主引用的 `CjReferenceExpression`，不硬接 Kotlin 的更宽泛 reference 家族。
 */
abstract class AbstractResolveReferenceByFileTest : AbstractResolveReferenceTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val references = mainFile.collectDescendantsOfType<CjSimpleNameExpression>()
            .filter { it.isUsageSimpleNameForAnalysisApiTest() }

        val actual = buildString {
            references.forEachIndexed { index, referenceExpression ->
                appendLine("CjSimpleNameExpression: ${referenceExpression.text}")
                append(renderReference(referenceExpression).prependIndent("  "))
                if (index != references.lastIndex) {
                    appendLine()
                    appendLine()
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = "reference.txt")
    }
}
