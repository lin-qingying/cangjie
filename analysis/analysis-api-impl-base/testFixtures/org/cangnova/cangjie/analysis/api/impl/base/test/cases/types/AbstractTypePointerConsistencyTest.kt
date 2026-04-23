package org.cangnova.cangjie.analysis.api.impl.base.test.cases.types

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedExpressionType
import org.cangnova.cangjie.analysis.api.impl.base.test.targetCallText

import org.cangnova.cangjie.analysis.api.session.restoreType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer

import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * `types` 层的类型指针一致性抽象测试。
 */
abstract class AbstractTypePointerConsistencyTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val callExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)
            .single { it.text == directives.targetCallText }

        lateinit var typePointer: CaTypePointer<CaType>

        analyzeForTest(callExpression) {
            val expressionType = callExpression.expressionType
            assertNotNull(expressionType, "调用表达式没有查询到类型。")
            assertEquals(directives.expectedExpressionType, normalizeTypeRendering(expressionType!!.render()))
            typePointer = expressionType.createPointer()
        }

        analyzeForTest(callExpression) {
            val restoredType = restoreType(typePointer)

            assertNotNull(restoredType, "类型指针恢复失败。")
            assertEquals(
                directives.expectedExpressionType,
                normalizeTypeRendering(restoredType!!.render()),
                "恢复后的类型渲染不一致。",
            )
        }
    }
}
