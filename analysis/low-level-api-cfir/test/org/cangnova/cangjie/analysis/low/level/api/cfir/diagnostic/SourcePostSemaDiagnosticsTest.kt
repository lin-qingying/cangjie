package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 通过真实 IDE structure-element 收集入口验证 Sema 与后续常量流诊断的阶段边界。 */
class SourcePostSemaDiagnosticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics/postSema",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /** 单声明根必须保留 CFA，且同一模式只能报告一次。 */
    @Test
    fun constantPattern(mainFile: CjFile) {
        assertDiagnosticNames(mainFile, listOf(CfirErrors.UNREACHABLE_PATTERN.name))
    }

    /** 即使语义错误位于后面的分支，CFA 也必须等待完整声明的 Sema 结果。 */
    @Test
    fun laterSemanticError(mainFile: CjFile) {
        assertDiagnosticNames(mainFile, listOf(CfirErrors.UNRESOLVED_REFERENCE.name))
    }

    private fun assertDiagnosticNames(mainFile: CjFile, expected: List<String>) {
        val diagnostics = mainFile.collectDiagnosticsForFile(
            mainFile.getResolutionFacadeForTest(),
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS,
        )
        assertEquals(expected.sorted(), diagnostics.map { it.factoryName }.sorted())
    }
}
