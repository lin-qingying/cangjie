package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 代码片段作用域的执行回归（任务 3.7 / 3.8）。
 *
 * 覆盖：片段自身顶层声明构成的成员作用域、该作用域的种类与次序，
 * 以及「不泄漏宿主专有符号」——表达式片段没有自己的声明，
 * 不得凭空多出一层片段成员作用域。
 *
 * 片段 PSI 由 [CjPsiFactory] 在运行时构造并锚定到宿主文件：
 * 代码片段是宿主行为，不存在等价的静态源文件形状。
 */
@OptIn(CaExperimentalApi::class)
class CodeFragmentScopeExecutionTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/codeFragmentScope",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 正例：块片段内的顶层声明必须可见，且被投影成 `CODE_FRAGMENT_MEMBER_SCOPE`；
     * 宿主文件的顶层声明同样可见——片段成员作用域**叠加**在宿主上下文之上，不是替换它。
     */
    @Test
    fun blockFragmentExposesOwnDeclarations(mainFile: CjFile) {
        val fragment = CjPsiFactory.contextual(mainFile).createBlockCodeFragment(
            """
            func fragmentHelper(): Int64 {
                1
            }

            hostTarget()
            """.trimIndent(),
            mainFile,
        )
        val position = fragment.lastReference("hostTarget")

        analyzeForTest(fragment) {
            val context = fragment.scopeContext(position)
            val memberScopes = context.scopes.filter { it.kind == CaScopeKind.CODE_FRAGMENT_MEMBER_SCOPE }

            assertTrue(
                memberScopes.isNotEmpty(),
                "片段顶层声明必须投影为 CODE_FRAGMENT_MEMBER_SCOPE，实际 ${context.scopes.map { it.kind }}",
            )

            val names = context.compositeScope().getAllPossibleNames().map { it.asString() }
            assertTrue("fragmentHelper" in names, "片段自身顶层声明必须可见，实际 $names")
            assertTrue("hostTarget" in names, "宿主文件的顶层声明必须可见，实际 $names")
        }
    }

    /**
     * 反例：表达式片段没有自己的顶层声明，不得凭空产生片段成员作用域。
     *
     * 这条负例同时锁住「不泄漏宿主专有符号」：片段成员作用域只装片段自己的声明，
     * 实现若图省事把宿主作用域复制一份进来，这里就会多出不该有的层。
     */
    @Test
    fun expressionFragmentHasNoMemberScope(mainFile: CjFile) {
        val fragment = CjPsiFactory.contextual(mainFile)
            .createExpressionCodeFragment("hostTarget()", mainFile)
        val position = fragment.lastReference("hostTarget")

        analyzeForTest(fragment) {
            val context = fragment.scopeContext(position)
            assertTrue(
                context.scopes.none { it.kind == CaScopeKind.CODE_FRAGMENT_MEMBER_SCOPE },
                "表达式片段没有自身声明，不得产生片段成员作用域，实际 ${context.scopes.map { it.kind }}",
            )
        }
    }
}

/**
 * 取出片段内最后一个引用指定名字的表达式。
 */
private fun CjCodeFragment.lastReference(name: String): CjSimpleNameExpression =
    PsiTreeUtil.findChildrenOfType(this, CjSimpleNameExpression::class.java).last { it.referencedName == name }