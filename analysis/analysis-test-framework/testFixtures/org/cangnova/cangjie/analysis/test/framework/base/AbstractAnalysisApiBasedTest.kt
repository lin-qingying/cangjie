package org.cangnova.cangjie.analysis.test.framework.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.TestWithDisposable
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructureProvider
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructureProviderImpl
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructureProvider
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.services.CaAnalysisApiEnvironmentManager
import org.cangnova.cangjie.analysis.test.services.CaAnalysisApiEnvironmentManagerImpl
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.AssertionsService
import org.cangnova.cangjie.test.services.MetaInfosCleanupPreprocessor
import org.cangnova.cangjie.test.services.SourceFileProvider
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.impl.JUnit5Assertions
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 所有 Analysis API 测试的基类。
 *
 * 该基类负责：
 * 1. 初始化共享的测试环境与 CoreEnvironment。
 * 2. 注册 Analysis API 所需的 application/project services。
 * 3. 通过测试配置器创建 Analysis API 测试模块结构。
 * 4. 提供 [analyzeForTest] 作为统一的会话进入点。
 */
abstract class AbstractAnalysisApiBasedTest : TestWithDisposable() {
    abstract val configurator: AnalysisApiTestConfigurator

    /**
     * 额外服务注册器会在配置器自带注册器之后执行。
     */
    open val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = emptyList()

    protected open fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        throw UnsupportedOperationException(
            "The test case is not fully implemented. " +
                "'${::doTestByMainFile.name}', '${::doTestByMainModuleAndOptionalMainFile.name}' or '${::doTest.name}' should be overridden",
        )
    }

    protected open fun doTestByMainModuleAndOptionalMainFile(
        mainFile: CjFile?,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        doTestByMainFile(mainFile ?: error("The main file is not found"), mainModule, testServices)
    }

    protected open fun doTest(testServices: TestServices) {
        val (mainFile, mainModule) = findMainFileAndModule(testServices)
        doTestByMainModuleAndOptionalMainFile(mainFile, mainModule, testServices)
    }

    protected lateinit var testDataPath: Path
        private set

    private var _testServices: TestServices? = null

    protected val testServices: TestServices
        get() = _testServices ?: error("`testServices` has not been initialized")

    data class ModuleWithMainFile(val mainFile: CjFile?, val module: CjTestModule)

    protected fun findMainFileAndModule(testServices: TestServices): ModuleWithMainFile {
        val moduleStructure = testServices.cjTestModuleStructure
        val modules = moduleStructure.mainModules

        val mainModule = modules.singleOrNull()
            ?: modules.firstOrNull(::isMainModule)
            ?: error("Cannot find the main test module among ${modules.map { it.name }}")

        val mainFile = findMainFile(mainModule)
        return ModuleWithMainFile(mainFile, mainModule)
    }

    protected open fun isMainModule(module: CjTestModule): Boolean {
        return module.name == DEFAULT_MODULE_NAME
    }

    protected fun findMainFile(module: CjTestModule): CjFile? {
        val cjFiles = module.cjFiles
        cjFiles.singleOrNull()?.let { return it }

        return cjFiles.firstOrNull { isMainFile(it, module) }
    }

    protected open fun isMainFile(file: CjFile, module: CjTestModule): Boolean {
        return file.virtualFile.nameWithoutExtension == "main" ||
            file.virtualFile.nameWithoutExtension == module.name
    }

    protected fun runTest(path: String) {
        runTest(path) { doTest(it) }
    }

    /**
     * Analysis API 测试执行主入口。
     *
     * 这里按 Kotlin Analysis 测试框架的顺序组织初始化流程，确保：
     * 1. `test-infrastructure` 的文件预处理先于模块构建。
     * 2. Analysis API 服务先注册，再初始化项目结构。
     * 3. 测试模块结构始终通过 provider 暴露，而不是散落在多个 service 中。
     */
    protected fun runTest(path: String, block: (TestServices) -> Unit) {
        testDataPath = configurator.computeTestDataPath(Paths.get(path))
        val testServices = TestServices()
        _testServices = testServices

        registerBaseTestServices(testServices)

        val environmentManager = CaAnalysisApiEnvironmentManagerImpl(testServices, disposable)
        testServices.register(CaAnalysisApiEnvironmentManager::class, environmentManager)
        environmentManager.initializeEnvironment()

        val application = environmentManager.getApplication() as MockApplication
        val project = environmentManager.getProject() as MockProject
        val allRegistrars = configurator.serviceRegistrars + additionalServiceRegistrars

        allRegistrars.forEach { it.registerApplicationServices(application, testServices) }

        val moduleStructure = configurator.createModules(testDataPath, testServices, project)
        testServices.cjTestModuleStructureProvider.registerModuleStructure(moduleStructure)

        allRegistrars.forEach { it.registerProjectExtensionPoints(project, testServices) }
        allRegistrars.forEach { it.registerProjectServices(project, testServices) }

        environmentManager.initializeProjectStructure()

        allRegistrars.forEach { it.registerProjectModelServices(project, disposable, testServices) }

        moduleStructure.mainModules.forEach { module ->
            configurator.prepareFilesInModule(module, testServices)
        }

        block(testServices)
    }

    /**
     * Analysis API 测试必须显式注册基础测试服务，否则无法复用既有 test-infrastructure
     * 的文件预处理与断言体系。
     */
    private fun registerBaseTestServices(testServices: TestServices) {
        testServices.register(AssertionsService::class, JUnit5Assertions)
        testServices.register(
            SourceFileProvider::class,
            SourceFileProvider(preprocessors = listOf(MetaInfosCleanupPreprocessor(testServices))),
        )
        testServices.register(
            CjTestModuleStructureProvider::class,
            CjTestModuleStructureProviderImpl(testServices),
        )
    }

    protected fun <R> analyzeForTest(contextElement: CjElement, action: CaSession.() -> R): R {
        return analyze(contextElement, action)
    }

    companion object {
        const val DEFAULT_MODULE_NAME = "main"
    }
}
