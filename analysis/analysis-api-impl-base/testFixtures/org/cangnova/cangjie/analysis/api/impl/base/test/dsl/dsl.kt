package org.cangnova.cangjie.analysis.api.impl.base.test.dsl

import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData

/**
 * 声明 Analysis API component 测试分组。
 *
 * 该 helper 统一把目录前缀补成 `components/<name>`，使 generated tests 与 testData
 * 的组件目录结构保持一致。
 */
internal fun AnalysisApiTestGroup.component(
    directory: String,
    filter: (AnalysisApiTestConfiguratorFactoryData) -> Boolean = { true },
    init: AnalysisApiTestGroup.() -> Unit,
) {
    group("components/$directory", filter, init)
}
