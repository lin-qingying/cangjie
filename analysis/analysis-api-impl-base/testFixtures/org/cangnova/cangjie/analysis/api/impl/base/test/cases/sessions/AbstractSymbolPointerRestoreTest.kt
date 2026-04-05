package org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedCallableName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetNameText
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * 符号指针恢复抽象测试。
 *
 * 这里锁定跨 `analyze {}` 边界的最小公开语义：
 * 1. 符号可创建 pointer；
 * 2. pointer 可在下一次 analysis 中恢复；
 * 3. 恢复后的符号仍保持稳定的公开名称。
 */
abstract class AbstractSymbolPointerRestoreTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val referenceExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .single { it.referencedName == directives.targetNameText }

        lateinit var pointer: CaSymbolPointer<CaSymbol>

        analyzeForTest(referenceExpression) {
            val symbol = referenceExpression.resolveToSymbol()
            assertNotNull(symbol, "simple-name 应能解析到公开符号。")
            pointer = symbol!!.createPointer()
            assertEquals(directives.expectedCallableName, symbol.name)
        }

        analyzeForTest(referenceExpression) {
            val restoredSymbol = restoreSymbol(pointer)
            assertNotNull(restoredSymbol, "符号指针跨 analyze 边界恢复失败。")
            assertEquals(directives.expectedCallableName, restoredSymbol!!.name)
        }
    }
}
