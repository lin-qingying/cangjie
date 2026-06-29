package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedDeclarationReturnType
import org.cangnova.cangjie.analysis.api.impl.base.test.targetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetFunctionName
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * `expressionTypeProvider.returnType` 的抽象测试。
 *
 * 该测试同时比较 class default type 与函数声明 return type，确保公开类型入口返回一致的类型视图。
 */
abstract class AbstractDeclarationReturnTypeTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行声明返回类型查询测试。
     *
     * 方法定位目标函数或 callable 声明，并比较公开 return type 的渲染文本。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val userClass = PsiTreeUtil.findChildrenOfType(mainFile, CjClass::class.java)
            .single { it.name == directives.targetClassName }
        val targetFunction = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == directives.targetFunctionName }

        analyzeForTest(mainFile) {
            val classSymbol = getClassLikeSymbol(userClass.getClassId()!!)
            val declarationReturnType = targetFunction.returnType

            assertNotNull(classSymbol, "类声明没有查询到 class-like 符号。")
            assertNotNull(declarationReturnType, "函数声明没有查询到返回类型。")
            assertEquals(directives.expectedDeclarationReturnType, normalizeTypeRendering(classSymbol!!.defaultType.render()))
            assertEquals(directives.expectedDeclarationReturnType, normalizeTypeRendering(declarationReturnType!!.render()))
        }
    }
}
