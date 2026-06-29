package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiShorteningCommandTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.targetExpressionText
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * `shortenRange` 抽象测试。
 *
 * 这里验证 element/range 入口是否能从文件级全量 plan 中正确投影出当前选择范围命令。
 */
abstract class AbstractReferenceShortenerTest : AbstractAnalysisApiComponentTest() {
    /**
     * 当前范围缩短测试额外注册的目标表达式指令。
     *
     * 该指令指定要作为 shorten range 入口的点限定表达式。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiShorteningCommandTestDirectives

    /**
     * 执行指定表达式范围内的引用缩短命令测试。
     *
     * 方法定位点限定表达式，收集 element-level shortening command，并比较命令操作列表。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val expectedOperations = directives[AnalysisApiComponentTestDirectives.EXPECTED_REFERENCE_SHORTENING_OPERATION].sorted()
        val targetExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjDotQualifiedExpression::class.java)
            .single { expression -> expression.text == directives.targetExpressionText }

        analyzeForTest(targetExpression) {
            val command = targetExpression.collectReferenceShorteningsInElement()
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
