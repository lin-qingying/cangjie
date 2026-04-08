package org.cangnova.cangjie.analysis.test.framework.test.configurators

/**
 * Analysis API 测试 configurator 工厂基础层。
 *
 * 该工厂负责把“前端、宿主模式、session 模式、模块种类”折叠成具体 configurator。
 * 这与 Kotlin Analysis 测试框架保持同一职责边界：生成矩阵，不承载具体测试逻辑。
 */
abstract class AnalysisApiTestConfiguratorFactory {
    abstract fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator

    abstract fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean

    protected fun requireSupported(data: AnalysisApiTestConfiguratorFactoryData) {
        if (!supportMode(data)) {
            unsupportedModeError(data)
        }
    }

    protected fun unsupportedModeError(data: AnalysisApiTestConfiguratorFactoryData): Nothing {
        error("${this::class} does not support $data")
    }
}

data class AnalysisApiTestConfiguratorFactoryData(
    val frontend: FrontendKind,
    val moduleKind: TestModuleKind,
    val analysisSessionMode: AnalysisSessionMode,
    val analysisApiMode: AnalysisApiMode,
)

fun AnalysisApiTestConfiguratorFactoryData.defaultExtension(): String = "cj"

enum class AnalysisSessionMode(val suffix: String) {
    Normal("Normal"),
    Dependent("Dependent"),
}

enum class AnalysisApiMode(val suffix: String) {
    Ide("Ide"),
    Standalone("Standalone"),
    LspCompatible("LspCompatible"),
}

enum class FrontendKind(val suffix: String) {
    Cfir("Cfir"),
}
