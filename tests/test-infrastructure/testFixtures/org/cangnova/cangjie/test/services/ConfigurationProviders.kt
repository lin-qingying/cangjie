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

abstract class CompilerConfigurationProvider(val testServices: TestServices) : TestService {
    abstract fun createCompilerConfiguration(module: TestModule): CompilerConfiguration
    abstract val testRootDisposable: Disposable

    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, CompilationStage.FIRST)
    protected abstract fun getCangJieCoreEnvironment(module: TestModule): CangJieCoreEnvironment

    open fun getProject(module: TestModule): Project {
        return getCangJieCoreEnvironment(module).project
    }

    abstract fun getCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration
    abstract val configurators: List<AbstractEnvironmentConfigurator>
}


open class CompilerConfigurationProviderImpl(
    testServices: TestServices,
    @Suppress("UNUSED_PARAMETER") override val testRootDisposable: Disposable,
    override val configurators: List<AbstractEnvironmentConfigurator>,
) : CompilerConfigurationProvider(testServices) {
    private val environmentCache: MutableMap<TestModule, CangJieCoreEnvironment> = mutableMapOf()

    private val configurationCache: MutableMap<Pair<TestModule, CompilationStage>, CompilerConfiguration> = mutableMapOf()
    override fun getCangJieCoreEnvironment(module: TestModule): CangJieCoreEnvironment {
        return environmentCache.getOrPut(module) {
            createCangJieCoreEnvironment(module)
        }
    }

    protected open fun createCangJieCoreEnvironment(module: TestModule): CangJieCoreEnvironment {
        val configuration = getCompilerConfiguration(module, CompilationStage.FIRST)
        val environment = CangJieCoreEnvironment.create(
            parentDisposable = testRootDisposable,
            mode = CangJieCoreEnvironmentMode.UnitTest,
        )

        configureProject(environment.project, module, configuration)
        return environment
    }

    override fun createCompilerConfiguration(module: TestModule): CompilerConfiguration {
        return createCompilerConfiguration(module, CompilationStage.FIRST)
    }

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

    @OptIn(TestInfrastructureInternals::class)
    fun createCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration {
        return createCompilerConfiguration(testServices, module, configurators, compilationStage)
    }
}

private class TestMessageCollector : MessageCollector {
    private var hasErrorsFlag: Boolean = false

    override fun clear() {
        hasErrorsFlag = false
    }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
    ) {
        if (severity.isError) {
            hasErrorsFlag = true
        }
    }

    override fun hasErrors(): Boolean = hasErrorsFlag
}

val TestServices.compilerConfigurationProvider: CompilerConfigurationProvider by TestServices.testServiceAccessor()
private val TestServices.runtimeClasspathProviderContainer: RuntimeClasspathProvidersContainer by TestServices.testServiceAccessor()
val TestServices.runtimeClasspathProviders: List<RuntimeClasspathProvider>
    get() = runtimeClasspathProviderContainer.providers

fun interface RuntimeClasspathProvider : TestService {
    fun runtimeClasspath(module: TestModule): List<String>
}

class RuntimeClasspathProvidersContainer(
    val providers: List<RuntimeClasspathProvider>,
) : TestService

class EnvironmentConfiguratorsProvider(
    val configurators: List<AbstractEnvironmentConfigurator>,
) : TestService




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

private operator fun <T : Any> CompilerConfiguration.set(key: CompilerConfigurationKey<T>, value: T) {
    put(key, value)
}
