package org.cangnova.cangjie.analysis.test.framework.test.configurators

/**
 * 默认的 CFIR Standalone 测试配置器兼容入口。
 *
 * 现有测试类大多直接依赖这个单例对象，因此这里保留稳定名字，
 * 但其实现已经转为复用可参数化 configurator，避免继续复制配置逻辑。
 */
object CaCfirStandaloneAnalysisApiTestConfigurator : CaCfirConfiguredAnalysisApiTestConfigurator(
    analysisApiMode = AnalysisApiMode.Standalone,
    analyseInDependentSession = false,
)
