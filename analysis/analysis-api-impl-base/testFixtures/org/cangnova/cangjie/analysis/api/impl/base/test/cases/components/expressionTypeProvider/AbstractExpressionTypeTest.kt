package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedExpressionType
import org.cangnova.cangjie.analysis.api.impl.base.test.targetCallText
import org.cangnova.cangjie.analysis.api.impl.base.test.targetClassName
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * `expressionTypeProvider.expressionType` 的抽象测试。
 *
 * 该测试从调用表达式、解析到的 callable 返回类型和目标 class default type 三个入口交叉验证类型一致性。
 */
abstract class AbstractExpressionTypeTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行表达式类型查询测试。
     *
     * 方法定位目标表达式，查询公开 expression type，并将规范化后的类型渲染与期望比较。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val userClass = PsiTreeUtil.findChildrenOfType(mainFile, CjClass::class.java)
            .single { it.name == directives.targetClassName }
        val callExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)
            .single { it.text == directives.targetCallText }

        analyzeForTest(callExpression) {
            val expressionType = callExpression.expressionType
            val resolvedSymbol = callExpression.resolveToSymbol()
            val callableSymbol = resolvedSymbol as? CaCallableSymbol
            val classSymbol = getClassLikeSymbol(userClass.getClassId()!!)

            assertNotNull(expressionType, "调用表达式没有查询到类型。")
            assertNotNull(resolvedSymbol, "调用表达式没有解析到符号。")
            assertNotNull(classSymbol, "类声明没有查询到 class-like 符号。")
            assertTrue(callableSymbol != null, "调用表达式应解析为 callable 符号。")
            assertNotNull(callableSymbol!!.returnType, "callable 符号应暴露返回类型。")
            assertEquals(directives.expectedExpressionType, normalizeTypeRendering(expressionType!!.render()))
            assertEquals(directives.expectedExpressionType, normalizeTypeRendering(callableSymbol.returnType!!.render()))
            assertEquals(directives.expectedExpressionType, normalizeTypeRendering(classSymbol!!.defaultType.render()))
            assertFalse(expressionType.isErrorType, "合法调用表达式不应返回错误类型。")
        }
    }
}
