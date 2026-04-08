package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.targetClassName
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * `scopeProvider.declaredMemberScope / memberScope` 的抽象测试。
 *
 * 这里同时验证：
 * 1. `declaredMemberScope` 只暴露当前类直接声明的成员。
 * 2. `memberScope` 合并 use-site 语义下可见的成员。
 */
abstract class AbstractMemberScopeTest : AbstractScopeProviderTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val targetClass = PsiTreeUtil.findChildrenOfType(mainFile, CjClass::class.java)
            .single { it.name == directives.targetClassName }
        val expectedDeclaredNames = directives[AnalysisApiComponentTestDirectives.DECLARED_MEMBER_SCOPE_AVAILABLE_NAME]
        val expectedDeclaredClassifiers = directives[AnalysisApiComponentTestDirectives.DECLARED_MEMBER_SCOPE_CLASSIFIER]
        val expectedDeclaredCallables = directives[AnalysisApiComponentTestDirectives.DECLARED_MEMBER_SCOPE_CALLABLE]
        val expectedMemberNames = directives[AnalysisApiComponentTestDirectives.MEMBER_SCOPE_AVAILABLE_NAME]
        val expectedMemberClassifiers = directives[AnalysisApiComponentTestDirectives.MEMBER_SCOPE_CLASSIFIER]
        val expectedMemberCallables = directives[AnalysisApiComponentTestDirectives.MEMBER_SCOPE_CALLABLE]

        analyzeForTest(mainFile) {
            val classSymbol = getClassLikeSymbol(targetClass.getClassId()!!)
            val declaredMemberScope = classSymbol?.declaredMemberScope
            val memberScope = classSymbol?.memberScope

            assertNotNull(classSymbol, "目标 class-like 符号应可从 Analysis API 获取。")
            assertNotNull(declaredMemberScope, "声明成员作用域不应为空。")
            assertNotNull(memberScope, "use-site 成员作用域不应为空。")

            assertScopeContents(
                scope = declaredMemberScope!!,
                expectedAvailableNames = expectedDeclaredNames,
                expectedClassifiers = expectedDeclaredClassifiers,
                expectedCallables = expectedDeclaredCallables,
                scopeLabel = "声明成员作用域",
            )
            assertScopeContents(
                scope = memberScope!!,
                expectedAvailableNames = expectedMemberNames,
                expectedClassifiers = expectedMemberClassifiers,
                expectedCallables = expectedMemberCallables,
                scopeLabel = "use-site 成员作用域",
            )
        }
    }
}
