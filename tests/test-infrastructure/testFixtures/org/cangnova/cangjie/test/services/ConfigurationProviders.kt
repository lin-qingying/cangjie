package org.cangnova.cangjie.test.services

import com.intellij.openapi.Disposable
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.common.messages.MessageCollector
import org.cangnova.cangjie.test.model.TestModule

interface CompilerConfigurationProvider : TestService {
    fun createCompilerConfiguration(module: TestModule): CompilerConfiguration
    val testRootDisposable: Disposable

    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration = createCompilerConfiguration(module)
}

class CompilerConfigurationProviderImpl(
    private val testServices: TestServices,
    @Suppress("UNUSED_PARAMETER") override val testRootDisposable: Disposable,
    private val environmentConfigurators: List<AbstractEnvironmentConfigurator>,
) : CompilerConfigurationProvider {
    override fun createCompilerConfiguration(module: TestModule): CompilerConfiguration {
        val configuration = CompilerConfiguration.create(messageCollector = MessageCollector.NONE)
        environmentConfigurators.forEach { _ -> /* reserved for phase-specific configurators */ }
        return configuration
    }
}

val TestServices.compilerConfigurationProvider: CompilerConfigurationProvider by TestServices.testServiceAccessor()

fun interface RuntimeClasspathProvider : TestService {
    fun runtimeClasspath(module: TestModule): List<String>
}

class RuntimeClasspathProvidersContainer(
    val providers: List<RuntimeClasspathProvider>,
) : TestService

class EnvironmentConfiguratorsProvider(
    val configurators: List<AbstractEnvironmentConfigurator>,
) : TestService

class DefaultRegisteredDirectivesProvider(
    val directives: org.cangnova.cangjie.test.directives.model.RegisteredDirectives,
) : TestService
