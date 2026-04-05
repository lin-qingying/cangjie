package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.renderer

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedRenderedCallableSymbol
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedRenderedClassSymbol
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedRenderedType
import org.cangnova.cangjie.analysis.api.impl.base.test.targetCallText
import org.cangnova.cangjie.analysis.api.impl.base.test.targetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetFunctionName
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * renderer 抽象测试。
 *
 * 这里锁定公开渲染协议的三个最小语义单元：
 * 1. class-like 符号渲染；
 * 2. callable 符号渲染；
 * 3. 公开类型渲染。
 */
abstract class AbstractRendererTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val targetClass = PsiTreeUtil.findChildrenOfType(mainFile, CjClass::class.java)
            .single { it.name == directives.targetClassName }
        val targetCall = PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)
            .single { it.text == directives.targetCallText }

        analyzeForTest(targetCall) {
            val classSymbol = getClassLikeSymbol(targetClass.getClassId()!!)
            val callableSymbol = getTopLevelCallableSymbols(
                mainFile.packageFqName,
                Name.identifier(directives.targetFunctionName),
            ).singleOrNull()
            val renderedType = targetCall.expressionType?.render()

            assertNotNull(classSymbol, "renderer 测试需要可恢复的 class-like 符号。")
            assertNotNull(callableSymbol, "renderer 测试需要可恢复的 callable 符号。")
            assertNotNull(renderedType, "renderer 测试需要可恢复的表达式类型。")

            assertEquals(directives.expectedRenderedClassSymbol, classSymbol!!.render())
            assertEquals(directives.expectedRenderedCallableSymbol, callableSymbol!!.render())
            assertEquals(directives.expectedRenderedType, normalizeTypeRendering(renderedType!!))
        }
    }
}
