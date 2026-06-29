package org.cangnova.cangjie.analysis.test.framework.test.configurators

/**
 * Analysis API 测试 configurator 工厂基础层。
 *
 * 该工厂负责把“前端、宿主模式、session 模式、模块种类”折叠成具体 configurator。
 * 这与 Kotlin Analysis 测试框架保持同一职责边界：生成矩阵，不承载具体测试逻辑。
 */
abstract class AnalysisApiTestConfiguratorFactory {
    /**
     * 根据测试维度创建具体 configurator。
     */
    abstract fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator

    /**
     * 判断当前工厂是否支持给定测试维度。
     */
    abstract fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean

    /**
     * 要求当前工厂支持给定测试维度，否则抛出明确错误。
     */
    protected fun requireSupported(data: AnalysisApiTestConfiguratorFactoryData) {
        if (!supportMode(data)) {
            unsupportedModeError(data)
        }
    }

    /**
     * 构造当前工厂不支持某组测试维度时的失败路径。
     */
    protected fun unsupportedModeError(data: AnalysisApiTestConfiguratorFactoryData): Nothing {
        error("${this::class} does not support $data")
    }
}

/**
 * Analysis API 测试生成矩阵中的单个维度组合。
 */
data class AnalysisApiTestConfiguratorFactoryData(
    /**
     * 测试使用的前端实现。
     */
    val frontend: FrontendKind,
    /**
     * 测试模块映射到的 Analysis API 模块种类。
     */
    val moduleKind: TestModuleKind,
    /**
     * 测试使用的 analysis session 模式。
     */
    val analysisSessionMode: AnalysisSessionMode,
    /**
     * 测试使用的宿主 Analysis API 模式。
     */
    val analysisApiMode: AnalysisApiMode,
)

/**
 * 根据模块种类推导测试数据默认文件扩展名。
 */
fun AnalysisApiTestConfiguratorFactoryData.defaultExtension(): String = when (this.moduleKind) {
    TestModuleKind.ScriptSource -> "cjs"
    else -> "cj"
}

/**
 * Analysis API 测试 session 运行模式。
 */
enum class AnalysisSessionMode(val suffix: String) {
    /**
     * 普通单模块 use-site session。
     */
    Normal("Normal"),

    /**
     * dependent session，用于覆盖依赖模块视角。
     */
    Dependent("Dependent"),
}

/**
 * Analysis API 测试宿主运行模式。
 */
enum class AnalysisApiMode(val suffix: String) {
    /**
     * IDE 宿主模式。
     */
    Ide("Ide"),

    /**
     * standalone/headless 宿主模式。
     */
    Standalone("Standalone"),
}

/**
 * Analysis API 测试使用的前端种类。
 */
enum class FrontendKind(val suffix: String) {
    /**
     * CFIR 前端。
     */
    Cfir("Cfir"),
}
