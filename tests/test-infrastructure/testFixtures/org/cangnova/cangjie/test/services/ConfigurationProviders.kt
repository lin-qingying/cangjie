package org.cangnova.cangjie.test.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.config.*
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.TestModule

/**
 * 表示 `CompilerConfigurationProvider`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
abstract class CompilerConfigurationProvider(val testServices: TestServices) : TestService {
    /**
     * 提供 `createCompilerConfiguration` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    abstract fun createCompilerConfiguration(module: TestModule): CompilerConfiguration
    /**
     * 保存 `testRootDisposable`，供测试服务在测试执行期间读取或传递。
     */
    abstract val testRootDisposable: Disposable

    /**
     * 执行 `getCompilerConfiguration` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, CompilationStage.FIRST)
    /**
     * 提供 `getCangJieCoreEnvironment` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    protected abstract fun getCangJieCoreEnvironment(module: TestModule): CangJieCoreEnvironment

    /**
     * 提供 `getProject` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    open fun getProject(module: TestModule): Project {
        return getCangJieCoreEnvironment(module).project
    }

    /**
     * 提供 `getCompilerConfiguration` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    abstract fun getCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration
    /**
     * 保存 `configurators`，供测试服务在测试执行期间读取或传递。
     */
    abstract val configurators: List<AbstractEnvironmentConfigurator>
}


/**
 * 表示 `CompilerConfigurationProviderImpl`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
open class CompilerConfigurationProviderImpl(
    testServices: TestServices,
    /**
     * 保存 `configurators`，供测试服务在测试执行期间读取或传递。
     */
    @Suppress("UNUSED_PARAMETER") override val testRootDisposable: Disposable,
    override val configurators: List<AbstractEnvironmentConfigurator>,
) : CompilerConfigurationProvider(testServices) {
    /**
     * 保存 `environmentCache`，供测试服务在测试执行期间读取或传递。
     */
    private val environmentCache: MutableMap<TestModule, CangJieCoreEnvironment> = mutableMapOf()

    /**
     * 保存 `configurationCache`，供测试服务在测试执行期间读取或传递。
     */
    private val configurationCache: MutableMap<Pair<TestModule, CompilationStage>, CompilerConfiguration> = mutableMapOf()
    /**
     * 执行 `getCangJieCoreEnvironment` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun getCangJieCoreEnvironment(module: TestModule): CangJieCoreEnvironment {
        return environmentCache.getOrPut(module) {
            createCangJieCoreEnvironment(module)
        }
    }

    /**
     * 提供 `createCangJieCoreEnvironment` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    protected open fun createCangJieCoreEnvironment(module: TestModule): CangJieCoreEnvironment {
        val configuration = getCompilerConfiguration(module, CompilationStage.FIRST)
        val environment = CangJieCoreEnvironment.create(
            parentDisposable = testRootDisposable,
            mode = CangJieCoreEnvironmentMode.UnitTest,
        )

        configureProject(environment.project, module, configuration)
        return environment
    }

    /**
     * 执行 `createCompilerConfiguration` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun createCompilerConfiguration(module: TestModule): CompilerConfiguration {
        return createCompilerConfiguration(module, CompilationStage.FIRST)
    }

    /**
     * 执行 `getCompilerConfiguration` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun getCompilerConfiguration(
        module: TestModule,
        compilationStage: CompilationStage,
    ): CompilerConfiguration {
        return configurationCache.getOrPut(module to compilationStage) {
            createCompilerConfiguration(module, compilationStage)
        }
    }

    /**
     * TODO: 实现测试项目的编译器扩展注册
     * 当前方法体为空，未调用任何配置器。
     * 应遍历 configurators 列表，对每个配置器调用 legacyRegisterCompilerExtensions(project, module, configuration)，
     * 以便在测试环境中注册编译器插件、分析扩展等。
     * 待前端门面（frontendBasedFacades）机制完善后，此方法将通过 TEST_ONLY_PROJECT_CONFIGURATION_CALLBACK 回调触发。
     */
    fun configureProject(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        // TODO: 解除注释并实现以下逻辑：
        // for (configurator in configurators) {
        //     configurator.legacyRegisterCompilerExtensions(project, module, configuration)
        // }
    }

    /**
     * 执行 `createCompilerConfiguration` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    @OptIn(TestInfrastructureInternals::class)
    fun createCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration {
        return createCompilerConfiguration(testServices, module, configurators, compilationStage)
    }
}

/**
 * 表示 `TestMessageCollector`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
private class TestMessageCollector : MessageCollector {
    /**
     * 维护 `hasErrorsFlag`，供测试服务在测试执行期间读取或传递。
     */
    private var hasErrorsFlag: Boolean = false

    /**
     * 执行 `clear` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun clear() {
        hasErrorsFlag = false
    }

    /**
     * 执行 `report` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
    ) {
        if (severity.isError) {
            hasErrorsFlag = true
        }
    }

    /**
     * 执行 `hasErrors` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun hasErrors(): Boolean = hasErrorsFlag
}

/**
 * 保存 `TestServices.compilerConfigurationProvider`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.compilerConfigurationProvider: CompilerConfigurationProvider by TestServices.testServiceAccessor()
/**
 * 保存 `TestServices.runtimeClasspathProviderContainer`，供测试服务在测试执行期间读取或传递。
 */
private val TestServices.runtimeClasspathProviderContainer: RuntimeClasspathProvidersContainer by TestServices.testServiceAccessor()
/**
 * 保存 `TestServices.runtimeClasspathProviders`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.runtimeClasspathProviders: List<RuntimeClasspathProvider>
    get() = runtimeClasspathProviderContainer.providers

/**
 * 执行 `interface` 对应的测试服务流程，维持测试框架的阶段契约。
 */
fun interface RuntimeClasspathProvider : TestService {
    fun runtimeClasspath(module: TestModule): List<String>
}

/**
 * 表示 `RuntimeClasspathProvidersContainer`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class RuntimeClasspathProvidersContainer(
    /**
     * 保存 `providers`，供测试服务在测试执行期间读取或传递。
     */
    val providers: List<RuntimeClasspathProvider>,
) : TestService

/**
 * 表示 `EnvironmentConfiguratorsProvider`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class EnvironmentConfiguratorsProvider(
    /**
     * 保存 `configurators`，供测试服务在测试执行期间读取或传递。
     */
    val configurators: List<AbstractEnvironmentConfigurator>,
) : TestService




/**
 * 执行 `createCompilerConfiguration` 对应的测试服务流程，维持测试框架的阶段契约。
 */
@TestInfrastructureInternals
fun createCompilerConfiguration(
    testServices: TestServices,
    module: TestModule,
    configurators: List<AbstractEnvironmentConfigurator>,
    compilationStage: CompilationStage,
): CompilerConfiguration {
    val messageCollector = TestMessageCollector()
    val configuration = if (testServices.defaultsProvider.frontendKind == FrontendKinds.CFIR) {
        CompilerConfiguration.createForCfirFrontend(messageCollector = messageCollector)
    } else {
        CompilerConfiguration.create(messageCollector = messageCollector)
    }
    configuration[CommonConfigurationKeys.MODULE_NAME] = module.name
    module.languageVersionSettings?.let { configuration.languageVersionSettings = it }

    /**
     * phased / pipeline facade 也必须复用和普通 CFIR facade 相同的基础编译配置：
     * - module languageVersionSettings
     * - source roots
     * - 对应 compilationStage 的 environment configurators
     *
     * 否则 phased 测试拿到的并不是真实前端环境，只会在 without-alias-expansion 之类
     * 依赖 LanguageVersionSettings 的场景中暴露“假接线”。
     */
    for (configurator in configurators) {
        if (compilationStage == configurator.compilationStage) {
            configurator.configureCompileConfigurationWithAdditionalConfigurationKeys(configuration, module)
        }
    }

    module.files.forEach { file ->
        val realFile = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(file)
        configuration.addCangJieSourceRoot(
            path = realFile.canonicalPath,
            hmppModuleName = module.name,
        )
    }

    return configuration
}

/**
 * 提供 `set` 对应的测试服务流程，维持测试框架的阶段契约。
 */
private operator fun <T : Any> CompilerConfiguration.set(key: CompilerConfigurationKey<T>, value: T) {
    put(key, value)
}
