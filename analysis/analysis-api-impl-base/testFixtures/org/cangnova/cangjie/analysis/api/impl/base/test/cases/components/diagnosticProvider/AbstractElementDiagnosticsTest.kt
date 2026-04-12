package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiElementDiagnosticTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.diagnosticTargetElementText
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedElementDiagnostics
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * `element.diagnostics()` generated 测试。
 *
 * 这里锁定 element-level 入口能否把目标元素上的诊断恢复出来，
 * 作为 file-level collectDiagnostics 的补充链路。
 */
abstract class AbstractElementDiagnosticsTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiElementDiagnosticTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val target = findTargetElement(mainFile, directives.diagnosticTargetElementText)

        analyzeForTest(target) {
            val diagnostics = target.diagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> diagnostic.factoryName }
                .sorted()

            assertEquals(directives.expectedElementDiagnostics, diagnostics, "element diagnostics 结果不符合预期。")
        }
    }

    private fun findTargetElement(mainFile: CjFile, elementText: String): CjElement {
        val candidates = PsiTreeUtil.findChildrenOfType(mainFile, CjElement::class.java)
            .filter { element -> element.text == elementText }
            .filter { element ->
                generateSequence(element.parent) { current -> current.parent }
                    .filterIsInstance<CjElement>()
                    .none { parentElement -> parentElement.text == elementText }
            }

        return candidates.singleOrNull()
            ?: error("Cannot uniquely locate diagnostic target `$elementText` in `${mainFile.name}`.")
    }
}
