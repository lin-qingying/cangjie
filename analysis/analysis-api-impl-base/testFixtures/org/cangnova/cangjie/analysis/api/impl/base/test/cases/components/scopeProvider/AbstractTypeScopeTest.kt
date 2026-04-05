package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.targetCallText
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * `scopeProvider.type.scope` 的抽象测试。
 *
 * 这里从表达式类型出发观察作用域，确保 `CaType.scope`
 * 是“类型值”层面的正式公开入口，而不是 class-like API 的别名。
 */
abstract class AbstractTypeScopeTest : AbstractScopeProviderTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val targetCall = PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)
            .single { it.text == directives.targetCallText }
        val expectedNames = directives[AnalysisApiComponentTestDirectives.TYPE_SCOPE_AVAILABLE_NAME]
        val expectedClassifiers = directives[AnalysisApiComponentTestDirectives.TYPE_SCOPE_CLASSIFIER]
        val expectedCallables = directives[AnalysisApiComponentTestDirectives.TYPE_SCOPE_CALLABLE]

        analyzeForTest(targetCall) {
            val expressionType = targetCall.expressionType
            val typeScope = expressionType?.scope

            assertNotNull(expressionType, "目标表达式应可查询到公开类型。")
            assertNotNull(typeScope, "类型作用域不应为空。")

            assertScopeContents(
                scope = typeScope!!,
                expectedAvailableNames = expectedNames,
                expectedClassifiers = expectedClassifiers,
                expectedCallables = expectedCallables,
                scopeLabel = "类型作用域",
            )
        }
    }
}
