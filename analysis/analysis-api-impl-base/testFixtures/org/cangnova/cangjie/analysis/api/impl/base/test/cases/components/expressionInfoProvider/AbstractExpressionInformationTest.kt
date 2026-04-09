package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionInfoProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiExpressionInformationTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.expressionInfoTargetExpressionText
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedIsCompileTimeConstant
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedIsStatementLike
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * expression information 基础 generated 测试。
 *
 * 这里先锁定仓颉当前公开 API 里已经稳定存在的两个入口：
 * - `isStatementLike`
 * - `isCompileTimeConstant`
 */
abstract class AbstractExpressionInformationTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiExpressionInformationTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val expression = findExpression(mainFile, directives.expressionInfoTargetExpressionText)

        analyzeForTest(expression) {
            assertEquals(
                directives.expectedIsStatementLike,
                expression.isStatementLike,
                "isStatementLike 结果不符合预期。",
            )
            assertEquals(
                directives.expectedIsCompileTimeConstant,
                expression.isCompileTimeConstant,
                "isCompileTimeConstant 结果不符合预期。",
            )
        }
    }

    private fun findExpression(mainFile: CjFile, expressionText: String): CjExpression {
        return PsiTreeUtil.findChildrenOfType(mainFile, CjExpression::class.java)
            .singleOrNull { expression -> expression.text == expressionText }
            ?: error("Cannot uniquely locate expression `$expressionText` in `${mainFile.name}`.")
    }
}
