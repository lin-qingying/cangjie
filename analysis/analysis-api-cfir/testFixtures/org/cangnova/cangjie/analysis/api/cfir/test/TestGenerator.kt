package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.impl.base.test.dsl.AnalysisApiTestGenerator
import org.cangnova.cangjie.analysis.api.impl.base.test.dsl.generateAnalysisApiTests
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        AnalysisApiTestGenerator(this, listOf(CaCfirAnalysisApiTestConfiguratorFactory)).run {
            generateAnalysisApiTests()
        }
    }
}
