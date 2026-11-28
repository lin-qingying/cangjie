@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic

import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.DiagnosticCollectionFailures
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 用日志中的 mariadb-driver 源文件验证 PSI→CFIR 与整文件诊断收集。 */
class LspReportedSourceDiagnosticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics/lspReported",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun typesInteger(mainFile: CjFile) {
        val cfirFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val invalidDeclarations = collectDeclarations(cfirFile).filterIsInstance<CfirInvalidDeclaration>()
        assertTrue(
            invalidDeclarations.isEmpty(),
            "The reported source unexpectedly produced invalid CFIR declarations:\n" +
                invalidDeclarations.joinToString("\n") { declaration ->
                    "reason=${declaration.reason}; source=${declaration.source}; " +
                        "psi=${declaration.psi?.javaClass?.name}; text=${declaration.psi?.text}"
                },
        )

        DiagnosticCollectionFailures.clear()
        val failures = try {
            analyzeForTest(mainFile) {
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            }
            DiagnosticCollectionFailures.drain()
        } finally {
            DiagnosticCollectionFailures.clear()
        }
        assertTrue(
            failures.isEmpty(),
            "Diagnostic collection isolated ${failures.size} exception(s):\n" +
                failures.joinToString("\n") { failure -> failure.stackTraceToString() },
        )
    }

    private fun collectDeclarations(root: CfirDeclaration): List<CfirDeclaration> = buildList {
        add(root)
        val children = when (root) {
            is CfirFile -> root.declarations
            is CfirClassLikeDeclaration -> root.declarations
            is CfirExtend -> root.declarations
            else -> emptyList()
        }
        children.forEach { addAll(collectDeclarations(it)) }
    }
}
