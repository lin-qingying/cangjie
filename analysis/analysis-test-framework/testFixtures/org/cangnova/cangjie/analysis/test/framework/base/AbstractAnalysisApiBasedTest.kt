package org.cangnova.cangjie.analysis.test.framework.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.analysis.test.framework.TestWithDisposable
import org.cangnova.cangjie.analysis.test.framework.analysisApiMainFileName
import org.cangnova.cangjie.analysis.test.framework.isAnalysisApiMainModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructureProvider
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructureProviderImpl
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructureProvider
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.registerApplicationServices
import org.cangnova.cangjie.analysis.test.framework.test.configurators.registerProjectExtensionPoints
import org.cangnova.cangjie.analysis.test.framework.test.configurators.registerProjectModelServices
import org.cangnova.cangjie.analysis.test.framework.test.configurators.registerProjectServices
import org.cangnova.cangjie.analysis.test.services.CaAnalysisApiEnvironmentManager
import org.cangnova.cangjie.analysis.test.services.CaAnalysisApiEnvironmentManagerImpl
import org.cangnova.cangjie.analysis.test.services.environmentManager
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.AssertionsService
import org.cangnova.cangjie.test.services.MetaInfosCleanupPreprocessor
import org.cangnova.cangjie.test.services.SourceFileProvider
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.impl.JUnit5Assertions
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 所有 Analysis API 测试的统一基座。
 *
 * 该基座负责：
 * 1. 初始化 headless 测试环境和基础测试服务；
 * 2. 注册 Analysis API 所需的应用、项目和模型服务；
 * 3. 通过 configurator 构建统一的测试模块结构；
 * 4. 暴露以 use-site module 为边界的会话进入方式。
 */
abstract class AbstractAnalysisApiBasedTest : TestWithDisposable() {
    abstract val configurator: AnalysisApiTestConfigurator

    /**
     * 额外服务注册器会在 configurator 自带注册器之后执行。
     */
    open val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = emptyList()

    /**
     * 允许具体测试把额外的文件或模块指令容器接入框架。
     */
    open val additionalDirectives: List<DirectivesContainer>
        get() = emptyList()

    protected open fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        throw UnsupportedOperationException(
            "The test case is not fully implemented. " +
                "'${::doTestByMainFile.name}', '${::doTestByMainModuleAndOptionalMainFile.name}' " +
                "or '${::doTest.name}' should be overridden",
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

    data class ModuleWithMainFile(
        val mainFile: CjFile?,
        val module: CjTestModule,
    )

    protected fun findMainFileAndModule(testServices: TestServices): ModuleWithMainFile {
        val modules = testServices.cjTestModuleStructure.mainModules
        val explicitMainModules = modules.filter(::isMainModule)

        require(explicitMainModules.size <= 1) {
            "发现多个 MAIN_MODULE 标记：${explicitMainModules.map { it.name }}"
        }

        val mainModule = modules.singleOrNull()
            ?: explicitMainModules.singleOrNull()
            ?: modules.firstOrNull { it.name == DEFAULT_MODULE_NAME }
            ?: error("Cannot find the main test module among ${modules.map { it.name }}")

        return ModuleWithMainFile(
            mainFile = findMainFile(mainModule),
            module = mainModule,
        )
    }

    protected open fun isMainModule(module: CjTestModule): Boolean {
        return module.testModule.isAnalysisApiMainModule
    }

    protected fun findMainFile(module: CjTestModule): CjFile? {
        val cjFiles = module.cjFiles
        module.testModule.analysisApiMainFileName?.let { declaredMainFileName ->
            return cjFiles.singleOrNull { file ->
                file.name == declaredMainFileName || file.virtualFile?.name == declaredMainFileName
            } ?: error("Module `${module.name}` does not contain declared MAIN_FILE_NAME `$declaredMainFileName`.")
        }

        cjFiles.singleOrNull()?.let { return it }
        return cjFiles.firstOrNull { isMainFile(it, module) }
    }

    protected open fun isMainFile(file: CjFile, module: CjTestModule): Boolean {
        val fileNameWithoutExtension = file.virtualFile?.nameWithoutExtension
            ?: file.name.substringBeforeLast('.', file.name)
        return fileNameWithoutExtension == "main" || fileNameWithoutExtension == module.name
    }

    protected fun runTest(path: String) {
        runTest(path) { doTest(it) }
    }

    /**
     * Analysis API 测试统一执行入口。
     *
     * 初始化顺序保持为：
     * 1. 基础测试服务；
     * 2. 环境管理器；
     * 3. 应用服务；
     * 4. 测试模块结构；
     * 5. 项目级服务与模型服务；
     * 6. 文件预处理；
     * 7. 测试体执行。
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
        val registrars = configurator.serviceRegistrars + additionalServiceRegistrars

        registrars.registerApplicationServices(application, testServices)

        val moduleStructure = configurator.createModules(
            testDataPath = testDataPath,
            testServices = testServices,
            project = project,
            additionalDirectives = additionalDirectives,
        )
        testServices.cjTestModuleStructureProvider.registerModuleStructure(moduleStructure)

        registrars.registerProjectExtensionPoints(project, testServices)
        registrars.registerProjectServices(project, testServices)

        environmentManager.initializeProjectStructure()

        registrars.registerProjectModelServices(project, disposable, testServices)

        moduleStructure.mainModules.forEach { module ->
            configurator.prepareFilesInModule(module, testServices)
        }

        block(testServices)
    }

    /**
     * Analysis API 测试必须显式注册基础测试服务，
     * 才能复用 test-infrastructure 的文件预处理与断言体系。
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

    /**
     * 按元素批量进入 Analysis API，会按 use-site module 分组复用 session。
     */
    protected fun <R> analyzeForTest(
        contextElements: Collection<CjElement>,
        action: CaSession.(CjElement) -> R,
    ): List<R> {
        return analyzeGroupedByUseSiteModule(
            items = contextElements,
            resolveElement = { it },
            action = action,
        )
    }

    /**
     * 按文件批量进入 Analysis API，会按 use-site module 分组复用 session。
     */
    protected fun <R> analyzeFilesForTest(
        files: Collection<CjFile>,
        action: CaSession.(CjFile) -> R,
    ): List<R> {
        return analyzeGroupedByUseSiteModule(
            items = files,
            resolveElement = { it },
            action = action,
        )
    }

    /**
     * 依据当前测试模块结构，为 PSI 元素定位 use-site 测试模块。
     */
    private fun findUseSiteTestModule(element: CjElement): CjTestModule {
        val containingFile = element.containingFile
            ?: error("Cannot resolve test module for PSI element without containing file: $element")
        return testServices.cjTestModuleStructure.requireModuleByFile(containingFile)
    }

    /**
     * 以测试声明的 use-site module 为边界批量执行 Analysis API。
     */
    private fun <T, R> analyzeGroupedByUseSiteModule(
        items: Collection<T>,
        resolveElement: (T) -> CjElement,
        action: CaSession.(T) -> R,
    ): List<R> {
        if (items.isEmpty()) return emptyList()

        val sessionProvider = CaSessionProvider.getInstance(testServices.environmentManager.getProject())
        val groupedItems = items.withIndex().groupBy(
            keySelector = { indexedItem ->
                findUseSiteTestModule(resolveElement(indexedItem.value)).caModule
            },
            valueTransform = { indexedItem ->
                indexedItem.index to indexedItem.value
            },
        )

        val results = arrayOfNulls<Any?>(items.size)
        sessionProvider.analyzeModules(groupedItems.keys) { useSiteModule ->
            groupedItems.getValue(useSiteModule).forEach { (index, item) ->
                results[index] = action(item)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return results.map { it as R }
    }

    companion object {
        const val DEFAULT_MODULE_NAME = "main"
    }
}
