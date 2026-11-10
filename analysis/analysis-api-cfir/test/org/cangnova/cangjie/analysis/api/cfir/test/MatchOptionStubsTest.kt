package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * STUBS 链路：`None` 这类无参 enum constructor 必须能按目标类型 `?UInt64` 定型。
 *
 * 该用例锁定 `StubBasedCfirMemberDeserializer` 的 owner 记录：enum constructor 没有
 * dispatch receiver，必须像 binary 反序列化那样显式写入 `containingClassForStaticMemberAttr`，
 * 否则 `getContainingClass` 为空，owner 的 `T` 不会进入推断，`return None` 会报
 * `RETURN_TYPE_MISMATCH(expected Option<UInt64>, actual Option<T>)`。
 */
class MatchOptionStubsTest : AbstractOriginDiagnosticsTest(
    "analysis/analysis-api-cfir/testData/diagnostics",
    CaDeserializedDeclarationsOrigin.STUBS,
) {
    override val configurator = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(
            frontend = FrontendKind.Cfir,
            moduleKind = TestModuleKind.Source,
            analysisSessionMode = AnalysisSessionMode.Normal,
            analysisApiMode = AnalysisApiMode.Ide,
        ),
    )

    @Test
    fun matchOption(mainFile: CjFile) {
        assertExpectedOrigin(mainFile)

        val diagnostics = collectDiagnosticSnapshot(mainFile)

        assertTrue(
            diagnostics.none { it.contains("RETURN_TYPE_MISMATCH") },
            "STUBS 下 `None` 必须由目标类型定型，不应产生 RETURN_TYPE_MISMATCH：$diagnostics",
        )
        assertTrue(
            diagnostics.any { it.contains("UNREACHABLE_PATTERN") },
            "常量 subject `Some(1)` 的 `None` 分支应报 UNREACHABLE_PATTERN：$diagnostics",
        )
    }
}
