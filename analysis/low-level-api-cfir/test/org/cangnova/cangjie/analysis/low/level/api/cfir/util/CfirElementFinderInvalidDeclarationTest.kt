@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.DiagnosticCollectionFailures
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildInvalidDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirInvalidDeclarationSymbol
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 文件结构索引必须跳过不可按名称查找的 invalid 声明，并继续处理有效兄弟。 */
class CfirElementFinderInvalidDeclarationTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/designationInvalidDeclaration",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun invalidDeclarationDoesNotHideValidSibling(mainFile: CjFile) {
        val cfirFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val sourceFunction = mainFile.declarations.filterIsInstance<CjNamedFunction>().single()
        val indexedFunction = cfirFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val invalidDeclaration = buildInvalidDeclaration {
            moduleData = cfirFile.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            symbol = CfirInvalidDeclarationSymbol()
            origin = CfirDeclarationOrigin.Synthetic.Error
            attributes = CfirDeclarationAttributes.EMPTY
            reason = "synthetic invalid sibling for designation regression"
        }
        (cfirFile.declarations as MutableList<CfirDeclaration>).add(invalidDeclaration)

        assertSame(invalidDeclaration, cfirFile.declarations.last())
        assertSame(
            indexedFunction,
            CfirElementFinder.collectDesignationPath(cfirFile, sourceFunction)?.target,
            "Designation lookup must find the valid sibling after indexing the invalid leaf",
        )

        DiagnosticCollectionFailures.clearAll()
        val failures = try {
            analyzeForTest(mainFile) {
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            }
            DiagnosticCollectionFailures.drain()
        } finally {
            DiagnosticCollectionFailures.clearAll()
        }
        assertTrue(
            failures.isEmpty(),
            "Diagnostic collection isolated ${failures.size} exception(s):\n" +
                failures.joinToString("\n") { it.stackTraceToString() },
        )
    }
}
