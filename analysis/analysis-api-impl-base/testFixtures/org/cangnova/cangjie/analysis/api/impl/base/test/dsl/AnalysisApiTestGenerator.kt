package org.cangnova.cangjie.analysis.api.impl.base.test.dsl

import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactory
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite

class AnalysisApiTestGenerator(
    val suite: TestGroupSuite,
    val configuratorFactories: List<AnalysisApiTestConfiguratorFactory>,
) {
    fun run(init: AnalysisApiTestGroup.() -> Unit) {
        AnalysisApiTestGroup(this, { true }, null).init()
    }
}
