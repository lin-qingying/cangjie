package org.cangjie.analysis.test.framework.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.cangjie.analysis.api.CaSession
import org.cangjie.analysis.api.analyze
import org.cangjie.analysis.api.impl.base.projectStructure.AnalysisApiServiceRegistrar
import org.cangjie.analysis.test.framework.TestWithDisposable
import org.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructure
import org.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangjie.analysis.test.services.CaAnalysisApiEnvironmentManager
import org.cangjie.analysis.test.services.CaAnalysisApiEnvironmentManagerImpl
import org.cangjie.test.services.TestServices
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 所有 Analysis API 测试的基类（对齐 Kotlin 的 AbstractAnalysisApiBasedTest）。
 *
 * 通过 [configurator] 配置测试环境：
 * 1. 创建 [MockApplication] 和 [MockProject]
 * 2. 通过 [configurator] 的服务注册器注册所有平台服务
 * 3. 创建测试模块结构
 * 4. 提供 [analyzeForTest] 便捷方法进入分析会话
 *
 * 子类实现 [configurator] 以指定分析引擎（如 CFIR），
 * 然后在测试方法中使用 [analyzeForTest] 执行分析逻辑。
 *
 * 测试入口三级结构（由简到繁）：
 * - [doTestByMainFile] — 面向单文件测试
 * - [doTestByMainModuleAndOptionalMainFile] — 面向模块测试（主文件可选）
 * - [doTest] — 面向完整模块结构测试
 */
abstract class AbstractAnalysisApiBasedTest : TestWithDisposable() {
    abstract val configurator: AnalysisApiTestConfigurator

    /**
     * 额外的服务注册器（在配置器自带注册器之后执行）。
     *
     * 按惯例，覆盖时应包含 `super.additionalServiceRegistrars`。
     */
    open val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = emptyList()

    // region 测试入口

    /**
     * 以主文件为中心的测试入口。
     *
     * 适用场景：
     * - 收集文件的诊断信息
     * - 获取光标处元素并执行逻辑
     * - 对 [mainFile] 执行操作后对比其他文件状态
     *
     * @see findMainFile
     */
    protected open fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        throw UnsupportedOperationException(
            "The test case is not fully implemented. " +
                "'${::doTestByMainFile.name}', '${::doTestByMainModuleAndOptionalMainFile.name}' or '${::doTest.name}' should be overridden"
        )
    }

    /**
     * 以主模块为中心的测试入口（主文件可选）。
     *
     * 适用场景：
     * - 查找模块中所有声明
     * - 按限定名查找声明
     * - 处理模块中的所有文件
     *
     * 仅在 [doTestByMainFile] 不适用时使用。
     *
     * @param mainFile 如果存在的话，模块中的专用主文件
     */
    protected open fun doTestByMainModuleAndOptionalMainFile(
        mainFile: CjFile?,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        doTestByMainFile(mainFile ?: error("The main file is not found"), mainModule, testServices)
    }

    /**
     * 面向完整模块结构的测试入口。
     *
     * 适用场景：
     * - 查找所有模块中的所有文件
     * - 比较不同文件和不同模块中的声明
     *
     * 模块结构可通过 [TestServices.cjTestModuleStructure] 访问。
     *
     * 仅在 [doTestByMainModuleAndOptionalMainFile] 不适用时使用。
     */
    protected open fun doTest(testServices: TestServices) {
        val (mainFile, mainModule) = findMainFileAndModule(testServices)
        doTestByMainModuleAndOptionalMainFile(mainFile, mainModule, testServices)
    }

    // endregion

    // region 测试状态

    protected lateinit var testDataPath: Path
        private set

    private var _testServices: TestServices? = null

    protected val testServices: TestServices
        get() = _testServices ?: error("`testServices` has not been initialized")

    // endregion

    // region 模块/文件查找

    data class ModuleWithMainFile(val mainFile: CjFile?, val module: CjTestModule)

    protected fun findMainFileAndModule(testServices: TestServices): ModuleWithMainFile {
        val moduleStructure = testServices.cjTestModuleStructure
        val modules = moduleStructure.mainModules

        val mainModule = modules.singleOrNull()
            ?: modules.firstOrNull { isMainModule(it) }
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

    // endregion

    // region 测试执行

    protected fun runTest(path: String) {
        runTest(path) { doTest(it) }
    }

    /**
     * 测试执行主入口。
     *
     * 执行流程（对齐 Kotlin 的 ProjectStructureInitialisationPreAnalysisHandler）：
     * 1. 初始化环境（MockApplication + MockProject）
     * 2. 注册 application 级服务
     * 3. 创建测试模块结构
     * 4. 注册 project 级扩展点和服务
     * 5. 初始化项目结构
     * 6. 注册 project model 服务
     * 7. 执行测试逻辑
     */
    protected fun runTest(path: String, block: (TestServices) -> Unit) {
        testDataPath = configurator.computeTestDataPath(Paths.get(path))
        val testServices = TestServices()
        _testServices = testServices

        // 1. Initialize environment
        val environmentManager = CaAnalysisApiEnvironmentManagerImpl(disposable)
        testServices.register(CaAnalysisApiEnvironmentManager::class, environmentManager)
        environmentManager.initializeEnvironment()

        val application = environmentManager.getApplication() as MockApplication
        val project = environmentManager.getProject() as MockProject

        // Collect all registrars
        val allRegistrars = configurator.serviceRegistrars + additionalServiceRegistrars

        // 2. Register application services
        allRegistrars.forEach { it.registerApplicationServices(application, testServices) }

        // 3. Create test module structure
        val moduleStructure = configurator.createModules(testDataPath, testServices, project)
        testServices.register(CjTestModuleStructure::class, moduleStructure)

        // 4. Register project extension points and services
        allRegistrars.forEach { it.registerProjectExtensionPoints(project, testServices) }
        allRegistrars.forEach { it.registerProjectServices(project, testServices) }

        // 5. Initialize project structure
        environmentManager.initializeProjectStructure()

        // 6. Register project model services
        allRegistrars.forEach { it.registerProjectModelServices(project, disposable, testServices) }

        // 7. Prepare files in modules
        moduleStructure.mainModules.forEach { module ->
            configurator.prepareFilesInModule(module, testServices)
        }

        block(testServices)
    }

    // endregion

    // region 分析便捷方法

    /**
     * 直接分析 [contextElement]。
     *
     * 在测试中应优先使用此方法而非直接调用 [analyze]，因为它会执行额外检查。
     */
    protected fun <R> analyzeForTest(contextElement: CjElement, action: CaSession.() -> R): R {
        return analyze(contextElement, action)
    }

    // endregion

    companion object {
        const val DEFAULT_MODULE_NAME = "main"
    }
}
