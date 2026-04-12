package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiDiagnosticTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedCommonDiagnostics
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedExperimentalDiagnostics
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedExtendedDiagnostics
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * `collectDiagnostics()` generated 测试。
 *
 * 这一组先对齐 Kotlin diagnosticsProvider 的 file-level 主入口，
 * 锁定不同 checker filter 下对外可见的 factoryName 集合。
 */
abstract class AbstractCollectDiagnosticsTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiDiagnosticTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)

        analyzeForTest(mainFile) {
            val common = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
                .map { diagnostic -> diagnostic.factoryName }
                .sorted()
            val extended = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS)
                .map { diagnostic -> diagnostic.factoryName }
                .sorted()
            val experimental = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS)
                .map { diagnostic -> diagnostic.factoryName }
                .sorted()
            val all = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                .map { diagnostic -> diagnostic.factoryName }
                .sorted()

            assertEquals(directives.expectedCommonDiagnostics, common, "common diagnostics 结果不符合预期。")
            assertEquals(directives.expectedExtendedDiagnostics, extended, "extended diagnostics 结果不符合预期。")
            assertEquals(directives.expectedExperimentalDiagnostics, experimental, "experimental diagnostics 结果不符合预期。")
            assertEquals((common + extended).sorted(), all, "EXTENDED_AND_COMMON_CHECKERS 应等于 common + extended。")
        }
    }
}
