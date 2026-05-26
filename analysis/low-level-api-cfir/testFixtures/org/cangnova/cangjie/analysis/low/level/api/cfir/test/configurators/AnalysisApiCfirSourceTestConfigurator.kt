package org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.services.TestServices

/**
 * Low-level CFIR source-module 测试配置器。
 *
 * 对齐 Kotlin `AnalysisApiFirSourceTestConfigurator` 的职责：source 测试复用
 * Analysis API IDE-mode 宿主与 transformed module structure，low-level 用例只在
 * 其上消费 LLResolutionFacade / session。
 */
fun analysisApiCfirSourceTestConfigurator(analyseInDependentSession: Boolean): AnalysisApiTestConfigurator {
    return AnalysisApiCfirSourceTestConfigurator(analyseInDependentSession)
}

private class AnalysisApiCfirSourceTestConfigurator(
    analyseInDependentSession: Boolean,
) : AnalysisApiTestConfigurator() {
    private val delegate = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(
            frontend = FrontendKind.Cfir,
            moduleKind = TestModuleKind.Source,
            analysisSessionMode = if (analyseInDependentSession) {
                AnalysisSessionMode.Dependent
            } else {
                AnalysisSessionMode.Normal
            },
            analysisApiMode = AnalysisApiMode.Ide,
        ),
    )

    override val testPrefixes: List<String>
        get() = delegate.testPrefixes

    override val analyseInDependentSession: Boolean
        get() = delegate.analyseInDependentSession

    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = delegate.serviceRegistrars

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        delegate.configureTest(builder, disposable)
    }

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): CjTestModuleStructure = delegate.createModules(moduleStructure, testServices, project)

    override fun prepareFilesInModule(cjTestModule: CjTestModule, testServices: TestServices) {
        delegate.prepareFilesInModule(cjTestModule, testServices)
    }

    override fun computeTestDataPath(path: java.nio.file.Path): java.nio.file.Path =
        delegate.computeTestDataPath(path)
}
