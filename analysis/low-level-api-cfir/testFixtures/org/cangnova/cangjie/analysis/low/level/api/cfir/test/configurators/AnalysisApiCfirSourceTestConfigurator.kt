package org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators

import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind

/**
 * Low-level CFIR source-module 测试配置器。
 *
 * 对齐 Kotlin `AnalysisApiFirSourceTestConfigurator` 的职责：source 测试仍复用
 * Analysis API 框架搭建项目结构与 IDE mode 服务，low-level 用例只读取对应的
 * resolution facade / session 结构。
 */
fun analysisApiCfirSourceTestConfigurator(analyseInDependentSession: Boolean): AnalysisApiTestConfigurator {
    return CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(
            frontend = FrontendKind.Cfir,
            moduleKind = TestModuleKind.Source,
            analysisSessionMode = if (analyseInDependentSession) {
                AnalysisSessionMode.Dependent
            } else {
                AnalysisSessionMode.Normal
            },
            analysisApiMode = AnalysisApiMode.Ide,
        ),
    )
}
