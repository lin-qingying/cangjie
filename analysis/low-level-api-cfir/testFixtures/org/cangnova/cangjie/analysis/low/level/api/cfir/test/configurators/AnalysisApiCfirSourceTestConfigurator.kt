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

/**
 * 通过 analysis-api-cfir 的源码模块配置器承载 low-level CFIR 测试配置。
 */
private class AnalysisApiCfirSourceTestConfigurator(
    analyseInDependentSession: Boolean,
) : AnalysisApiTestConfigurator() {
    /**
     * 实际执行项目结构、服务注册和文件准备工作的 Analysis API CFIR 配置器。
     */
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

    /**
     * 测试数据文件名前缀集合，直接沿用委托配置器的约定。
     */
    override val testPrefixes: List<String>
        get() = delegate.testPrefixes

    /**
     * 当前测试是否应在 dependent analysis session 中执行。
     */
    override val analyseInDependentSession: Boolean
        get() = delegate.analyseInDependentSession

    /**
     * 当前配置器需要注册到测试项目中的 Analysis API 服务集合。
     */
    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = delegate.serviceRegistrars

    /**
     * 将基础测试配置写入 builder，并让委托配置器负责 disposable 生命周期绑定。
     */
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        delegate.configureTest(builder, disposable)
    }

    /**
     * 根据测试模块结构创建 Analysis API 可消费的仓颉测试模块结构。
     */
    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): CjTestModuleStructure = delegate.createModules(moduleStructure, testServices, project)

    /**
     * 在模块文件进入 low-level CFIR 测试前复用委托配置器的文件准备逻辑。
     */
    override fun prepareFilesInModule(cjTestModule: CjTestModule, testServices: TestServices) {
        delegate.prepareFilesInModule(cjTestModule, testServices)
    }

    /**
     * 计算测试数据路径，保持与 analysis-api-cfir 源码测试配置完全一致。
     */
    override fun computeTestDataPath(path: java.nio.file.Path): java.nio.file.Path =
        delegate.computeTestDataPath(path)
}
