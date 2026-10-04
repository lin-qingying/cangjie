package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjNamedFunction
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * use-site 可见性回归。
 *
 * 可见性必须服从使用点上下文：委托给编译器唯一的可访问性服务后，
 * private 成员在 owner 内部可见、在其它类型处不可见，internal 成员受包边界约束。
 * 这里同时固定「不可见必须被过滤」与「可见不得被误过滤」两侧。
 */
@OptIn(CaExperimentalApi::class)
class AnalysisApiCfirUseSiteVisibilityTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/components/useSiteVisibility",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** private 字段在其它类型的使用点不可见（官方 cjc 报 `can not access field 'secret'`）。 */
    @Test
    fun privateMemberHiddenFromOtherType(mainFile: CjFile) {
        val useSite = mainFile.useSiteIn("peek")

        analyzeForTest(mainFile) {
            val secret = PsiTreeUtil.findChildrenOfType(mainFile, CjFieldVariable::class.java)
                .single { it.name == "secret" }
            val checker = createUseSiteVisibilityChecker(mainFile.symbol, null, useSite)

            assertFalse(checker.isVisible(secret.symbol), "private 字段在其它类型中必须不可见")
        }
    }

    /** 同一 private 字段在 owner 内部可见，可见性不得被过度过滤。 */
    @Test
    fun privateMemberVisibleInsideOwner(mainFile: CjFile) {
        val useSite = mainFile.useSiteIn("reveal")

        analyzeForTest(mainFile) {
            val secret = PsiTreeUtil.findChildrenOfType(mainFile, CjFieldVariable::class.java)
                .single { it.name == "secret" }
            val checker = createUseSiteVisibilityChecker(mainFile.symbol, null, useSite)

            assertTrue(checker.isVisible(secret.symbol), "private 字段在 owner 内部必须可见")
        }
    }

    /** 包级 internal 成员在同一文件可见。 */
    @Test
    fun internalMemberHiddenAcrossPackage(mainFile: CjFile) {
        val useSite = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == "shared" }

        analyzeForTest(mainFile) {
            val shared = useSite.symbol
            val checker = createUseSiteVisibilityChecker(mainFile.symbol, null, useSite)

            assertTrue(checker.isVisible(shared), "internal 成员在同一包内必须可见")
        }
    }
}

/**
 * 取得指定函数中作为成员访问接收者的表达式。
 */
private fun CjFile.useSiteIn(functionName: String) =
    PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java)
        .single { it.name == functionName }
        .bodyExpression
        ?: error("函数 `$functionName` 缺少函数体表达式")