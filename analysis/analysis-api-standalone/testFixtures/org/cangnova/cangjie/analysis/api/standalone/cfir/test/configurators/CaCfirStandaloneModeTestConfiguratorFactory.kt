package org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators

import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind

/**
 * Standalone 模式下的 CFIR Analysis API configurator 工厂。
 *
 * 这里先把 standalone 所有权与入口位置对齐 Kotlin：
 * standalone factory 归 `analysis-api-standalone` testFixtures 持有，
 * 不再放在 IDE/CFIR 通用工厂中混合分派。
 */
object CaCfirStandaloneModeTestConfiguratorFactory : AnalysisApiTestConfiguratorFactory() {
    override fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator {
        requireSupported(data)
        return CaCfirStandaloneAnalysisApiTestConfigurator
    }

    override fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean {
        return when {
            data.frontend != FrontendKind.Cfir -> false
            data.analysisSessionMode != AnalysisSessionMode.Normal -> false
            data.analysisApiMode != AnalysisApiMode.Standalone -> false
            else -> when (data.moduleKind) {
                TestModuleKind.Source,
                TestModuleKind.LibraryBinary,
                TestModuleKind.CodeFragment,
                    -> true

                TestModuleKind.LibraryBinaryDecompiled,
                TestModuleKind.LibrarySource,
                TestModuleKind.ScriptSource,
                TestModuleKind.NotUnderContentRoot,
                TestModuleKind.NotUnderContentRootWithDependencies,
                    -> false
            }
        }
    }
}
