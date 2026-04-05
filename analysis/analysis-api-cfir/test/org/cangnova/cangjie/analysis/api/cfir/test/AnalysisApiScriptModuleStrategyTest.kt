package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.CaScriptDependencyModule
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 锁定脚本模块在 low-level 解析策略中的依赖建模。
 *
 * 这组测试直接覆盖之前缺失的 `CaScriptDependencyModule` 路径，确保脚本 use-site
 * 在进入 file scope 分析时，low-level session 可以完整接上 builtins 与脚本依赖模块。
 */
class AnalysisApiScriptModuleStrategyTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/scriptModules",
) {
    override val configurator: AnalysisApiTestConfigurator =
        CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
            AnalysisApiTestConfiguratorFactoryData(
                frontend = FrontendKind.Cfir,
                moduleKind = TestModuleKind.ScriptSource,
                analysisSessionMode = AnalysisSessionMode.Normal,
                analysisApiMode = AnalysisApiMode.Standalone,
            ),
        )

    @Test
    fun scriptFileScope(mainFile: CjFile, mainModule: CjTestModule) {
        assertTrue(
            mainModule.auxiliaryModules.any { it is CaScriptDependencyModule },
            "脚本测试模块应显式建模脚本依赖模块，避免脚本 use-site 退化为普通 source module。",
        )

        val availableNames = analyzeForTest(mainFile) {
            mainFile.getFileScope().availableNames.map(Name::asString).sorted()
        }

        assertTrue(
            availableNames.containsAll(listOf("greet", "scriptMain")),
            "脚本 file scope 至少应暴露当前脚本声明的符号，当前得到：$availableNames",
        )
    }
}
