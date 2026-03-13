package org.cangnova.cangjie.analysis.test.framework.test.configurators

/**
 * 测试配置器工厂基类（对齐 Kotlin 的 AnalysisApiTestConfiguratorFactory）。
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

fun AnalysisApiTestConfiguratorFactoryData.defaultExtension(): String = when (this.moduleKind) {
    TestModuleKind.ScriptSource -> "cjs"
    else -> "cj"
}

enum class AnalysisSessionMode(val suffix: String) {
    Normal("Normal"),
    Dependent("Dependent"),
}

enum class AnalysisApiMode(val suffix: String) {
    Ide("Ide"),
    Standalone("Standalone"),
}

enum class FrontendKind(val suffix: String) {
    Cfir("Cfir"),
}
