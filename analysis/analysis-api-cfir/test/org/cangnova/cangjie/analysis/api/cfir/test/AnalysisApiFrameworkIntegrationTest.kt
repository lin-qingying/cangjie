package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 锁定 Analysis API 测试框架的模块级入口行为。
 */
class AnalysisApiFrameworkIntegrationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/framework",
) {
    /**
     * 使用 standalone CFIR 配置运行测试框架主模块选择用例。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证测试框架会把带入口文件的 `app` 模块选为主分析模块。
     */
    @Test
    fun selectMainModule(mainFile: CjFile, mainModule: CjTestModule) {
        assertEquals("app", mainModule.name)
        assertEquals("entry.cj", mainFile.name)
    }
}
