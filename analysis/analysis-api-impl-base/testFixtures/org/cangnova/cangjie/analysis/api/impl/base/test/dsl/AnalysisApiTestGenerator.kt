package org.cangnova.cangjie.analysis.api.impl.base.test.dsl

import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactory
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite

/**
 * Analysis API generated tests 的顶层生成入口。
 *
 * 该类把 Kotlin 测试生成 DSL 的 `TestGroupSuite` 与仓颉 Analysis API 的 configurator factory 列表绑定在一起，
 * 后续 `AnalysisApiTestGroup` 会基于这些 factory 展开不同 frontend、module kind、session mode 和 API mode 的测试类。
 */
class AnalysisApiTestGenerator(
    /**
     * 底层 Kotlin 测试生成器使用的 suite 容器。
     *
     * 所有 Analysis API test group 和 test class 最终都会写入该 suite。
     */
    val suite: TestGroupSuite,
    /**
     * 当前生成任务可使用的 Analysis API 测试配置器工厂。
     *
     * 生成 DSL 会为每种配置组合选择唯一支持的 factory，并把它写入生成测试类的 `getConfigurator()`。
     */
    val configuratorFactories: List<AnalysisApiTestConfiguratorFactory>,
) {
    /**
     * 执行 Analysis API 测试生成 DSL。
     *
     * 根分组默认不过滤任何配置组合，调用者在 `init` 中继续声明 component、group 和 test。
     */
    fun run(init: AnalysisApiTestGroup.() -> Unit) {
        AnalysisApiTestGroup(this, { true }, null).init()
    }
}
