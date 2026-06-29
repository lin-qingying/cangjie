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

/**
 * CFIR Analysis API diagnostics facade 的回归测试。
 *
 * 这里覆盖文件级与声明级 diagnostics 查询，确保 checker filter、extend 声明和接口类型参数
 * 在 Analysis API 懒解析链路中保持一致的诊断边界。
 */
class AnalysisApiCfirDiagnosticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/diagnostics",
) {
    /**
     * 使用 standalone CFIR 配置执行 diagnostics facade 测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证 common、extended、experimental 等 checker filter 对同一文件的诊断集合划分。
     */
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

    /**
     * 验证合法 extend 文件在扩展与公共 checker 合并模式下不会产生额外诊断。
     */
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

    /**
     * 验证命名函数声明级 diagnostics 查询不会破坏后续文件级 diagnostics 收集。
     */
    @Test
    fun namedFunctionDiagnostics(mainFile: CjFile) {
        val function = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java).single()
        analyzeForTest(mainFile) {
            function.diagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
    }

    /**
     * 验证接口成员签名可以解析外层类型参数，不会在懒解析 diagnostics 中误报未解析引用。
     */
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
