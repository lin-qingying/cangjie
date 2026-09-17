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
 * Low-level CFIR binary-library 测试配置器。
 *
 * 对齐 source 变体的职责边界：main 模块按 `TestModuleKind.LibraryBinary` 装配成
 * [org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule]，
 * low-level 用例在其上消费 LLResolutionFacade / session，覆盖从 binary 库视图
 * 解析声明的路径。工厂层要求 LibraryBinary 只能以 Normal session 运行。
 */
fun analysisApiCfirBinaryTestConfigurator(): AnalysisApiTestConfigurator {
    return AnalysisApiCfirBinaryTestConfigurator()
}

/**
 * 通过 analysis-api-cfir 的 LibraryBinary 模块配置器承载 low-level CFIR 测试配置。
 */
private class AnalysisApiCfirBinaryTestConfigurator : AnalysisApiTestConfigurator() {
    /**
     * 实际执行项目结构、服务注册和文件准备工作的 Analysis API CFIR 配置器。
     */
    private val delegate = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(
            frontend = FrontendKind.Cfir,
            moduleKind = TestModuleKind.LibraryBinary,
            analysisSessionMode = AnalysisSessionMode.Normal,
            analysisApiMode = AnalysisApiMode.Ide,
        ),
    )

    /**
     * 测试数据文件名前缀集合，直接沿用委托配置器的约定。
     */
    override val testPrefixes: List<String>
        get() = delegate.testPrefixes

    /**
     * 当前 configurator 的 Analysis API 宿主运行模式，沿用委托配置器的约定。
     */
    override val analysisApiMode: AnalysisApiMode
        get() = delegate.analysisApiMode

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
     * 计算测试数据路径，保持与委托配置器一致。
     */
    override fun computeTestDataPath(path: java.nio.file.Path): java.nio.file.Path =
        delegate.computeTestDataPath(path)
}
