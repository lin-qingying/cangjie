package org.cangnova.cangjie.analysis.test.framework.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.util.Disposer
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.registerApplicationServices
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.registerProjectExtensionPoints
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.registerProjectServices
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.analysis.test.framework.TestWithDisposable
import org.cangnova.cangjie.analysis.test.framework.AnalysisApiTestDirectives
import org.cangnova.cangjie.analysis.test.framework.analysisApiMainFileName
import org.cangnova.cangjie.analysis.test.framework.directives.ModificationEventDirectives
import org.cangnova.cangjie.analysis.test.framework.isAnalysisApiMainModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructureProvider
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructureProviderImpl
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructureProvider
import org.cangnova.cangjie.analysis.test.framework.services.ExpressionMarkerProvider
import org.cangnova.cangjie.analysis.test.framework.services.ExpressionMarkersSourceFilePreprocessor
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.registerProjectModelServices
import org.cangnova.cangjie.analysis.test.services.CaAnalysisApiEnvironmentManager
import org.cangnova.cangjie.analysis.test.services.CaAnalysisApiEnvironmentManagerImpl
import org.cangnova.cangjie.analysis.test.services.environmentManager
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.CangJieTestInfo
import org.cangnova.cangjie.test.NonGroupingPhaseTestConfiguration
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.builders.testConfiguration
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TemporaryDirectoryManager
import org.cangnova.cangjie.test.services.AssertionsService
import org.cangnova.cangjie.test.services.MetaInfosCleanupPreprocessor
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.impl.JUnit5Assertions
import org.cangnova.cangjie.test.services.impl.TemporaryDirectoryManagerImpl
import org.cangnova.cangjie.test.toCangJieTestInfo
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

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
    /**
     * 当前测试使用的 Analysis API configurator。
     */
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
        get() = listOf(ExpressionMarkerProvider.Directives)

    /**
     * 以主文件和主模块为入口执行测试。
     */
    protected open fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        throw UnsupportedOperationException(
            "The test case is not fully implemented. " +
                "'${::doTestByMainFile.name}', '${::doTestByMainModuleAndOptionalMainFile.name}' " +
                "or '${::doTest.name}' should be overridden",
        )
    }

    /**
     * 以主模块和可选主文件为入口执行测试。
     */
    protected open fun doTestByMainModuleAndOptionalMainFile(
        mainFile: CjFile?,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        doTestByMainFile(mainFile ?: error("The main file is not found"), mainModule, testServices)
    }

    /**
     * 以测试服务容器为入口执行测试。
     */
    protected open fun doTest(testServices: TestServices) {
        val (mainFile, mainModule) = findMainFileAndModule(testServices)
        doTestByMainModuleAndOptionalMainFile(mainFile, mainModule, testServices)
    }

    /**
     * 当前测试数据文件路径。
     */
    protected lateinit var testDataPath: Path
        private set

    /**
     * 当前测试运行中的测试服务容器。
     */
    private var _testServices: TestServices? = null

    /**
     * 当前 JUnit 测试信息。
     */
    private lateinit var currentTestInfo: CangJieTestInfo

    /**
     * 当前测试运行中的非空测试服务容器。
     */
    protected val testServices: TestServices
        get() = _testServices ?: error("`testServices` has not been initialized")

    /**
     * 在每个测试用例开始前记录 JUnit 测试信息。
     */
    @BeforeEach
    fun initTestInfo(testInfo: TestInfo) {
        currentTestInfo = testInfo.toCangJieTestInfo()
    }

    /**
     * 主文件和主模块的成对结果。
     */
    data class ModuleWithMainFile(
        /**
         * 当前测试解析出的主文件；无主文件场景下为 `null`。
         */
        val mainFile: CjFile?,
        /**
         * 当前测试解析出的主模块。
         */
        val module: CjTestModule,
    )

    /**
     * 根据测试模块结构、指令和命名约定查找主文件与主模块。
     */
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

    /**
     * 判断测试模块是否是主模块。
     */
    protected open fun isMainModule(module: CjTestModule): Boolean {
        return module.testModule.isAnalysisApiMainModule
    }

    /**
     * 在指定模块内查找测试主文件。
     */
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

    /**
     * 判断指定文件是否应作为当前模块的主文件。
     */
    protected open fun isMainFile(file: CjFile, module: CjTestModule): Boolean {
        val expressionMarkerProvider = testServices.expressionMarkerProvider
        if (expressionMarkerProvider?.getCaretOrNull(file) != null ||
            expressionMarkerProvider?.getSelectionOrNull(file) != null
        ) {
            return true
        }

        val fileNameWithoutExtension = file.virtualFile?.nameWithoutExtension
            ?: file.name.substringBeforeLast('.', file.name)
        return fileNameWithoutExtension == "main" || fileNameWithoutExtension == module.name
    }

    /**
     * 按当前测试数据路径解析并断言 Analysis API golden 输出文件。
     *
     * 文件命名和变体解析规则对齐 Kotlin `AbstractAnalysisApiBasedTest.assertEqualsToTestOutputFile`：
     * 默认输出为同目录同名 `.txt`，`configurator.testPrefixes` 中靠后的变体优先级更高。
     */
    @Suppress("UnusedReceiverParameter")
    protected fun AssertionsService.assertEqualsToTestOutputFile(
        actual: String,
        extension: String = ".txt",
        subdirectoryName: String? = null,
        testPrefixes: List<String> = configurator.testPrefixes,
    ) {
        assertEqualsToFile(
            expectedFile = getTestOutputFile(
                extension = extension,
                subdirectoryName = subdirectoryName,
                testPrefixes = testPrefixes,
            ).toFile(),
            actual = actual,
        )
    }

    /**
     * 返回当前测试数据对应的输出文件；若存在变体文件，则按 `testPrefixes` 顺序取最后一个匹配项。
     */
    protected fun getTestOutputFile(
        extension: String = "txt",
        subdirectoryName: String? = null,
        testPrefixes: List<String> = configurator.testPrefixes,
    ): Path {
        for (variant in testPrefixes) {
            findVariantTestOutputFile(extension, subdirectoryName, variant)?.let { return it }
        }
        return getDefaultTestOutputFile(extension, subdirectoryName)
    }

    /**
     * 构造当前测试数据对应的默认 golden 输出文件路径。
     */
    private fun getDefaultTestOutputFile(extension: String, subdirectoryName: String?): Path =
        buildTestOutputFilePath(extension, subdirectoryName, variant = null)

    /**
     * 查找指定变体对应的 golden 输出文件。
     */
    private fun findVariantTestOutputFile(extension: String, subdirectoryName: String?, variant: String): Path? =
        buildTestOutputFilePath(extension, subdirectoryName, variant).takeIf { it.exists() }

    /**
     * 根据扩展名、子目录和变体名称构造 golden 输出文件路径。
     */
    private fun buildTestOutputFilePath(extension: String, subdirectoryName: String?, variant: String?): Path {
        val extensionWithDot = "." + extension.removePrefix(".")
        val baseName = testDataPath.nameWithoutExtension
        val directoryPath = subdirectoryName?.let { testDataPath.resolveSibling(it) } ?: testDataPath.parent

        val relativePath = if (variant != null) {
            "$baseName.$variant$extensionWithDot"
        } else {
            baseName + extensionWithDot
        }
        return directoryPath.resolve(relativePath)
    }

    /**
     * 使用默认 [doTest] 入口运行指定测试数据。
     */
    protected fun runTest(path: String) {
        runTest(path) { doTest(it) }
    }

    /**
     * 允许 configurator 在 TestConfigurationBuilder 层补充配置。
     */
    protected open fun configureTest(builder: TestConfigurationBuilder) {
        configurator.configureTest(builder, disposable)
        if (additionalDirectives.isNotEmpty()) {
            builder.useDirectives(*additionalDirectives.toTypedArray())
        }
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
        val testConfiguration = createTestConfiguration()
        val testServices = testConfiguration.testServices
        _testServices = testServices
        Disposer.register(disposable, testConfiguration.rootDisposable)

        registerBaseTestServices(testServices)

        val testModuleStructure = testConfiguration.moduleStructureExtractor.splitTestDataByModules(
            testDataPath.toString(),
            testConfiguration.directives,
        )
        testServices.register(TestModuleStructure::class, testModuleStructure)

        val environmentManager = CaAnalysisApiEnvironmentManagerImpl(testServices, disposable)
        testServices.register(CaAnalysisApiEnvironmentManager::class, environmentManager)
        environmentManager.initializeEnvironment()

        val application = environmentManager.getApplication() as MockApplication
        val project = environmentManager.getProject() as MockProject
        val registrars = configurator.serviceRegistrars + additionalServiceRegistrars

        registrars.registerApplicationServices(application, testServices)

        val moduleStructure = configurator.createModules(
            moduleStructure = testModuleStructure,
            testServices = testServices,
            project = project,
        )
        testServices.cjTestModuleStructureProvider.registerModuleStructure(moduleStructure)

        registrars.registerProjectExtensionPoints(project, testServices)
        registrars.registerProjectServices(project, testServices)

        environmentManager.initializeProjectStructure()

        registrars.registerProjectModelServices(project, testServices)

        moduleStructure.mainModules.forEach { module ->
            configurator.prepareFilesInModule(module, testServices)
        }

        block(testServices)
    }

    /**
     * 构造当前测试用例使用的 non-grouping 测试配置。
     */
    private fun createTestConfiguration(): NonGroupingPhaseTestConfiguration {
        return testConfiguration(testDataPath.toString()) {
            configureTest(this)
            globalDefaults {
                frontend = FrontendKinds.CFIR
                dependencyKind = DependencyKind.Source
            }
            useDirectives(AnalysisApiTestDirectives, ModificationEventDirectives)
            assertions = JUnit5Assertions
            testInfo = currentTestInfo
            useSourcePreprocessor(
                ::ExpressionMarkersSourceFilePreprocessor,
                ::MetaInfosCleanupPreprocessor,
            )
            useAdditionalService<TemporaryDirectoryManager>(::TemporaryDirectoryManagerImpl)
        }
    }

    /**
     * Analysis API 测试必须显式注册基础测试服务，
     * 才能复用 test-infrastructure 的文件预处理与断言体系。
     */
    private fun registerBaseTestServices(testServices: TestServices) {
        testServices.register(ExpressionMarkerProvider::class, ExpressionMarkerProvider())
        testServices.register(
            CjTestModuleStructureProvider::class,
            CjTestModuleStructureProviderImpl(testServices),
        )
    }

    /**
     * 以指定上下文元素进入 Analysis API session。
     */
    protected fun <R> analyzeForTest(contextElement: CjElement, action: CaSession.() -> R): R {
        return analyze(contextElement, action)
    }

    /**
     * 对齐 Kotlin copy-aware 分析入口。
     *
     * dependent session 模式下，测试应当在复制文件中的同构 PSI 上执行，
     * 避免继续引用原文件元素而绕开 dangling/dependent 语义边界。
     */
    protected fun <E : CjElement, R> copyAwareAnalyzeForTest(
        contextElement: E,
        action: CaSession.(E) -> R,
    ): R {
        return if (configurator.analyseInDependentSession) {
            val originalContainingFile = contextElement.containingFile as? CjFile
                ?: error("copyAwareAnalyzeForTest requires a CjFile-backed context element: $contextElement")
            val fileCopy = originalContainingFile.copy() as CjFile
            analyze(getDependentElementFromFile(contextElement, fileCopy), action = { action(getDependentElementFromFile(contextElement, fileCopy)) })
        } else {
            analyze(contextElement, action = { action(contextElement) })
        }
    }

    /**
     * 把原文件中的 PSI 元素映射到 copy-aware 上下文文件中。
     */
    protected fun <E : CjElement> getDependentElementFromFile(originalElement: E, contextFile: CjFile): E {
        if (!configurator.analyseInDependentSession || originalElement.containingFile != contextFile.originalFile) {
            return originalElement
        }
        return PsiTreeUtil.findSameElementInCopy(originalElement, contextFile)
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
