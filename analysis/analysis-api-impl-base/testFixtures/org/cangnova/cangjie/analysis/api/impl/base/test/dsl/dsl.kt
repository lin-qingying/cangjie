package org.cangnova.cangjie.analysis.api.impl.base.test.dsl

import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData

internal fun AnalysisApiTestGroup.component(
    directory: String,
    filter: (AnalysisApiTestConfiguratorFactoryData) -> Boolean = { true },
    init: AnalysisApiTestGroup.() -> Unit,
) {
    group("components/$directory", filter, init)
}
