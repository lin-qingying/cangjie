package org.cangnova.cangjie.analysis.api.standalone.cfir.test

import org.cangnova.cangjie.analysis.api.impl.base.test.dsl.AnalysisApiTestGenerator
import org.cangnova.cangjie.analysis.api.impl.base.test.dsl.generateAnalysisApiTests
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneModeTestConfiguratorFactory
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

/**
 * 生成 standalone CFIR Analysis API 测试套件。
 */
fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        AnalysisApiTestGenerator(this, listOf(CaCfirStandaloneModeTestConfiguratorFactory)).run {
            generateAnalysisApiTests()
        }
    }
}
