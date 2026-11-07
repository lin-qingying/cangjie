package org.cangnova.cangjie.ide.codeInsight.fixes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Test

/**
 * F6 测试 DSL 自身的回归：验证断言工具的三个入口按预期工作。
 *
 * 这组用例同时是 DSL 用法示例——新增 quick-fix 的测试照此书写。
 */
@OptIn(CaExperimentalApi::class)
class QuickFixTestSupportTest : AbstractAnalysisApiExecutionTest(
    "code-insight/fixes/testData/p1",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * fixesFor + assertSingleFix：正常路径一条修复。
     */
    @Test
    fun dslProducesAndAssertsSingleFix(unusedImportProducesRemoveFix: CjFile) {
        val fixes = analyzeForTest(unusedImportProducesRemoveFix) {
            with(QuickFixTestSupport) {
                fixesFor<CaCfirDiagnostic.UnusedImport>(
                    collectCommonDiagnostics(unusedImportProducesRemoveFix),
                    P1QuickFixFactories.unusedImport,
                )
            }
        }
        QuickFixTestSupport.assertSingleFix(fixes, "Remove unused import")
    }

    /**
     * assertNoFix：降级路径（replacement 为空）零修复。
     */
    @Test
    fun dslAssertsNoFixOnDegradedPath(deprecatedWarningProducesReplaceFix: CjFile) {
        val fixes = analyzeForTest(deprecatedWarningProducesReplaceFix) {
            with(QuickFixTestSupport) {
                fixesFor<CaCfirDiagnostic.DeprecatedWarning>(
                    collectCommonDiagnostics(deprecatedWarningProducesReplaceFix),
                    P1QuickFixFactories.deprecatedWarning,
                )
            }
        }
        QuickFixTestSupport.assertNoFix(fixes)
    }
}
