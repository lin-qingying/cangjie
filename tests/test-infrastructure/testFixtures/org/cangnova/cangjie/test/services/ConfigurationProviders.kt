package org.cangnova.cangjie.test.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.config.CommonConfigurationKeys
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.create
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.TestModule
import kotlin.text.set

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


class CompilerConfigurationProviderImpl(
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
        val messageCollector = TestMessageCollector()
        val configuration = if (testServices.defaultsProvider.frontendKind == FrontendKinds.CFIR) {
            CompilerConfiguration.createForCfirFrontend(messageCollector = messageCollector)
        } else {
            CompilerConfiguration.create(messageCollector = messageCollector)
        }

        // TODO: 实现阶段特定的环境配置器调用
        // 当前 environmentConfigurators 列表已注入但未被使用。
        // 应遍历 environmentConfigurators，对每个配置器调用其配置方法（例如 configureEnvironment 或 registerExtensions），
        // 以便在编译环境初始化阶段注入测试所需的自定义行为（如注册插件、设置语言版本等）。
        configurators.forEach { _ -> }

        module.files.forEach { file ->
            val realFile = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(file)
            configuration.addCangJieSourceRoot(
                path = realFile.canonicalPath,
                hmppModuleName = module.name,
            )
        }
        return configuration
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
     * 待 CLI 门面（cliBasedFacades）机制完善后，此方法将通过 TEST_ONLY_PROJECT_CONFIGURATION_CALLBACK 回调触发。
     */
    fun configureProject(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        // TODO: 解除注释并实现以下逻辑：
        // for (configurator in configurators) {
        //     configurator.legacyRegisterCompilerExtensions(project, module, configuration)
        // }
    }

    @OptIn(TestInfrastructureInternals::class)
    fun createCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration {
        return createCompilerConfiguration(
            testServices,
            module,
            configurators,
            compilationStage,
        ).also { configuration ->
            // TODO: 实现基于 CLI 门面的插件注册与项目配置回调
            // 当 testServices.cliBasedFacadesEnabled 为 true 时，需要：
            // 1. 通过 TEST_ONLY_PLUGIN_REGISTRATION_CALLBACK 注册编译器扩展（registerCompilerExtensions）
            // 2. 通过 TEST_ONLY_PROJECT_CONFIGURATION_CALLBACK 触发项目配置（configureProject）
            // 待 CLI 门面机制（cliBasedFacades）完善后解除注释：
            // if (testServices.cliBasedFacadesEnabled) {
            //     configuration.put(TEST_ONLY_PLUGIN_REGISTRATION_CALLBACK) { extensionStorage ->
            //         registerCompilerExtensions(extensionStorage, module, configuration)
            //     }
            //     configuration.put(TEST_ONLY_PROJECT_CONFIGURATION_CALLBACK) {
            //         configureProject(it, module, configuration)
            //     }
            // }
        }
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
    val configuration = CompilerConfiguration.create()
    configuration[CommonConfigurationKeys.MODULE_NAME] = module.name

    // TODO: 实现基于 compilationStage 和 configurators 的差异化配置
    // 当前所有编译阶段均返回同一份基础配置，configurators 参数未被使用。
    // 应根据 compilationStage（如 FIRST、SECOND 等）的不同，让对应的 configurators
    // 对 configuration 进行阶段特定的配置（如设置输出目录、启用增量编译、注入依赖路径等）。
    // 参考 Kotlin 测试框架中 AbstractEnvironmentConfigurator.configureCompilerConfiguration 的实现方式。
    for (configurator in configurators) {
        if (compilationStage == configurator.compilationStage) {
            configurator.configureCompileConfigurationWithAdditionalConfigurationKeys(configuration, module)
        }
    }
    return configuration
}

private operator fun <T : Any> CompilerConfiguration.set(key: CompilerConfigurationKey<T>, value: T) {
    put(key, value)
}
