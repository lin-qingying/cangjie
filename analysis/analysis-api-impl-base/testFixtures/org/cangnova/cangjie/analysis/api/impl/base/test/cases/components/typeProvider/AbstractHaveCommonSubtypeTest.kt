package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `typeProvider.haveCommonSubtype` 的抽象测试。
 */
abstract class AbstractHaveCommonSubtypeTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val left = testServices.expressionMarkerProvider
            .getTopmostSelectedElementOfType<CjExpression>(mainFile, qualifier = "left")
        val right = testServices.expressionMarkerProvider
            .getTopmostSelectedElementOfType<CjExpression>(mainFile, qualifier = "right")

        val actual = analyzeForTest(mainFile) {
            val leftType = left.expressionType ?: error("Left expression `${left.text}` has no type.")
            val rightType = right.expressionType ?: error("Right expression `${right.text}` has no type.")
            val haveCommonSubtype = leftType.semanticallyEquals(rightType) ||
                leftType.isSubTypeOf(rightType) ||
                rightType.isSubTypeOf(leftType)

            buildString {
                appendLine("left: ${left.text}")
                appendLine("leftType: ${leftType.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES).let(::normalizeTypeRendering)}")
                appendLine("right: ${right.text}")
                appendLine("rightType: ${rightType.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES).let(::normalizeTypeRendering)}")
                appendLine("haveCommonSubtype: $haveCommonSubtype")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
