package org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators

import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirConfiguredAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirConfiguredAnalysisApiTestConfigurator.CaCfirAnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiBaseTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneSessionServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind

/**
 * 默认的 CFIR standalone 测试配置器。
 *
 * 所有权对齐 Kotlin standalone testFixtures：
 * standalone 模式的 concrete configurator 归 `analysis-api-standalone` 模块持有。
 *
 * 对齐 Kotlin standalone `testPrefixes = ["standalone.fir"]`：
 * standalone 输出使用专用 golden 变体 `standalone.cfir`，
 * 避免与 IDE 输出共享同一 golden 文件而互相覆盖。
 */
object CaCfirStandaloneAnalysisApiTestConfigurator : CaCfirConfiguredAnalysisApiTestConfigurator(
    moduleKind = TestModuleKind.Source,
    serviceRegistrars = listOf(
        CaAnalysisApiBaseTestServiceRegistrar,
        CaCfirAnalysisApiServiceRegistrar(),
        CaStandaloneSessionServiceRegistrar,
        CaStandaloneModeTestServiceRegistrar,
    ),
    analyseInDependentSession = false,
    analysisApiMode = AnalysisApiMode.Standalone,
) {
    override val testPrefixes: List<String>
        get() = listOf("standalone.cfir")
}
