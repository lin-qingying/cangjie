package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.scope
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.impl.base.test.targetCallText
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * `scopeProvider.type.scope` 的抽象测试。
 *
 * 这里从表达式类型出发选择目标 scope，scope 内容渲染交给基座统一处理。
 */
abstract class AbstractTypeScopeTest : AbstractScopeTestBase() {
    context(session: CaSession)
    override fun getScope(mainFile: CjFile, testServices: TestServices): CaScope {
        val module = testServices.cjTestModuleStructure.requireModuleByFile(mainFile)
        val directives = directivesForMainFile(mainFile, module)
        val targetCall = PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)
            .single { it.text == directives.targetCallText }
        val expressionType = with(session) { targetCall.expressionType }
        val typeScope = expressionType?.scope

        assertNotNull(expressionType, "目标表达式应可查询到公开类型。")
        assertNotNull(typeScope, "类型作用域不应为空。")
        return typeScope!!
    }
}
