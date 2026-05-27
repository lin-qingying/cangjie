package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnalysisApiCfirDiagnosticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/diagnostics",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun collectDiagnostics(mainFile: CjFile) {
        val commonDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        }
        val allDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
        val extraDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS)
        }
        val experimentalDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS)
        }

        assertTrue(commonDiagnostics.any { it.factoryName == "UNRESOLVED_IMPORT" })
        assertEquals(commonDiagnostics.map { it.factoryName }, allDiagnostics.map { it.factoryName })
        assertTrue(extraDiagnostics.isEmpty())
        assertTrue(experimentalDiagnostics.isEmpty())
    }

    @Test
    fun extendDiagnostics(mainFile: CjFile) {
        val allDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        assertTrue(
            allDiagnostics.isEmpty(),
            "合法 extend 文件收集 diagnostics 不应抛异常，也不应产生额外诊断: " +
                allDiagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.textRange}" },
        )
    }

    @Test
    fun namedFunctionDiagnostics(mainFile: CjFile) {
        val function = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java).single()
        analyzeForTest(mainFile) {
            function.diagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
    }

    @Test
    fun interfaceTypeParameterDiagnostics(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        assertFalse(
            diagnostics.any { diagnostic ->
                diagnostic.factoryName == "UNRESOLVED_REFERENCE" && diagnostic.textRanges.any { range ->
                    mainFile.text.substring(range.startOffset, range.endOffset) == "T"
                }
            },
            "interface 成员签名中的外层类型参数 `T` 不应在 Analysis API 懒解析链路上退化成 UNRESOLVED_REFERENCE: " +
                diagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.text}" },
        )
    }
}
