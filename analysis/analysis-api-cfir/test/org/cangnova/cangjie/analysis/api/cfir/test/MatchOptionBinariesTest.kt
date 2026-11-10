package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * BINARIES 对照：与 [MatchOptionStubsTest] 使用同一份 fixture，锁定直接 `.cjo` 路线的行为。
 *
 * 两条路线都必须把 `None` 定型为 `?UInt64`，且只报 `UNREACHABLE_PATTERN`。
 */
class MatchOptionBinariesTest : AbstractOriginDiagnosticsTest(
    "analysis/analysis-api-cfir/testData/diagnostics",
    CaDeserializedDeclarationsOrigin.BINARIES,
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun matchOption(mainFile: CjFile) {
        assertExpectedOrigin(mainFile)

        val diagnostics = collectDiagnosticSnapshot(mainFile)

        assertTrue(
            diagnostics.none { it.contains("RETURN_TYPE_MISMATCH") },
            "BINARIES 下 `None` 必须由目标类型定型，不应产生 RETURN_TYPE_MISMATCH：$diagnostics",
        )
        assertTrue(
            diagnostics.any { it.contains("UNREACHABLE_PATTERN") },
            "常量 subject `Some(1)` 的 `None` 分支应报 UNREACHABLE_PATTERN：$diagnostics",
        )
    }
}
