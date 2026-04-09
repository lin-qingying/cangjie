package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.dataFlowInfoProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiDataFlowInfoTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.dataFlowTargetExpressionText
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedCompileTimeValueText
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedDataFlowExpressionType
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedDataFlowStability
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedIsPureReference
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * data flow 基础快照 generated 测试。
 *
 * 这一组用例先把仓颉当前公开 `CaDataFlowInfo` 里已经稳定暴露的字段锁住：
 * - expressionType
 * - compileTimeValue
 * - isPureReference
 * - stability
 */
abstract class AbstractDataFlowInfoTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiDataFlowInfoTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val expression = findExpression(mainFile, directives.dataFlowTargetExpressionText)

        analyzeForTest(expression) {
            val info = expression.getDataFlowInfo()
            assertEquals(
                directives.expectedDataFlowStability,
                info.stability,
                "dataFlowInfo.stability 结果不符合预期。",
            )
            assertEquals(
                directives.expectedIsPureReference,
                info.isPureReference,
                "dataFlowInfo.isPureReference 结果不符合预期。",
            )
            assertEquals(
                directives.expectedCompileTimeValueText,
                info.compileTimeValue?.renderedText,
                "dataFlowInfo.compileTimeValue 结果不符合预期。",
            )
            assertEquals(
                directives.expectedDataFlowExpressionType,
                info.expressionType?.render(CaTypeRendererForSource.WITH_SHORT_NAMES)?.let(::normalizeTypeRendering),
                "dataFlowInfo.expressionType 结果不符合预期。",
            )
        }
    }

    private fun findExpression(mainFile: CjFile, expressionText: String): CjExpression {
        return PsiTreeUtil.findChildrenOfType(mainFile, CjExpression::class.java)
            .singleOrNull { expression -> expression.text == expressionText }
            ?: error("Cannot uniquely locate expression `$expressionText` in `${mainFile.name}`.")
    }
}
