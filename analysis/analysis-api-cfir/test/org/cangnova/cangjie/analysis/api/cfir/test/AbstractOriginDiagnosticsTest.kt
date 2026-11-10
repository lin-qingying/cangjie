package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * 双 origin 回归测试的公共基类。
 *
 * STUBS/BINARIES 两个子类共享同一份 fixture，并各自断言自己运行在预期 origin 下；
 * 诊断快照使用统一格式，方便逐字对照两条链路的语义结果。
 */
abstract class AbstractOriginDiagnosticsTest(
    testDir: String,
    private val expectedOrigin: CaDeserializedDeclarationsOrigin,
) : AbstractAnalysisApiExecutionTest(testDir) {

    /**
     * 断言当前测试确实运行在预期 origin 下，避免“测试名是 STUBS、实际跑 BINARIES”的假覆盖。
     */
    protected fun assertExpectedOrigin(mainFile: CjFile) {
        assertEquals(
            expectedOrigin,
            CaPlatformSettings.getInstance(mainFile.project).deserializedDeclarationsOrigin,
            "该用例必须运行在 $expectedOrigin 配置下",
        )
    }

    /**
     * 在 [analyzeForTest] 生命周期内收集规范化的诊断快照。
     *
     * 快照包含 severity、factoryName 与命中源码文本，足以区分类型推断/owner 类漂移。
     */
    protected fun collectDiagnosticSnapshot(mainFile: CjFile): List<String> = analyzeForTest(mainFile) {
        mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            .map { diagnostic ->
                val ranges = diagnostic.textRanges.joinToString { range ->
                    mainFile.text.substring(range.startOffset, range.endOffset)
                }
                "${diagnostic.severity} ${diagnostic.factoryName}@[$ranges]"
            }
    }
}
