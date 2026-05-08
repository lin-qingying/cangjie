package org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators

import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirConfiguredAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirConfiguredAnalysisApiTestConfigurator.CaCfirAnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneSessionServiceRegistrar

/**
 * 默认的 CFIR standalone 测试配置器。
 *
 * 所有权对齐 Kotlin standalone testFixtures：
 * standalone 模式的 concrete configurator 归 `analysis-api-standalone` 模块持有。
 */
object CaCfirStandaloneAnalysisApiTestConfigurator : CaCfirConfiguredAnalysisApiTestConfigurator(
    serviceRegistrars = listOf(
        CaCfirAnalysisApiServiceRegistrar(),
        CaStandaloneSessionServiceRegistrar,
        CaStandaloneModeTestServiceRegistrar,
    ),
    analyseInDependentSession = false,
)
