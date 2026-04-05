package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * 会话层行为回归测试。
 *
 * 这组测试覆盖跨 `analyze {}` 边界恢复公开符号指针的核心语义，
 * 避免后续修改 session / pointer 协议时再次退化为“只能在单次分析调用中使用符号”。
 */
class AnalysisApiSessionBehaviorTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/sessions",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun symbolPointerRestore(mainFile: CjFile) {
        val referenceExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "greet" }

        lateinit var pointer: CaSymbolPointer<CaSymbol>

        analyzeForTest(referenceExpression) {
            val resolvedSymbol = referenceExpression.resolveToSymbol()
            assertNotNull(resolvedSymbol, "simple-name 引用应能解析到公开符号。")

            pointer = resolvedSymbol!!.createPointer()
            assertEquals("greet", resolvedSymbol.name)
        }

        analyzeForTest(referenceExpression) {
            val restoredSymbol = restoreSymbol(pointer)
            assertNotNull(restoredSymbol, "符号指针跨 analyze 边界恢复失败。")
            assertEquals("greet", restoredSymbol!!.name)

            val restoredPsi = restoredSymbol.getOriginalPsi() as? CjNamedDeclaration
            assertNotNull(restoredPsi, "恢复后的公开符号应能回到原始 PSI。")
            assertEquals("greet", restoredPsi!!.name)
        }
    }
}
