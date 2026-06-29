package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * @see AbstractCollectDiagnosticsTest
 */
abstract class AbstractElementDiagnosticsTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行单个 PSI 元素的诊断收集测试。
     *
     * 方法按指令定位目标元素，调用元素级 diagnostics 入口，并将诊断名称与文本范围写入 golden。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val targetDeclaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeByDirective(
            mainFile,
            mainModule.testModule,
            defaultType = CjElement::class,
        ) as CjElement

        analyzeForTest(mainFile) {
            val diagnostics = targetDeclaration.diagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)

            val actualText = buildString {
                if (diagnostics.isNotEmpty()) {
                    for (diagnostic in diagnostics) {
                        append(diagnostic.factoryName).append(": ")
                        diagnostic.textRanges.joinTo(this)
                        appendLine()
                    }
                } else {
                    appendLine("No diagnostics found")
                }
            }

            testServices.assertions.assertEqualsToTestOutputFile(actualText)
        }
    }
}
