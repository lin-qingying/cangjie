package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.AnalysisFlag
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey
import org.cangnova.cangjie.config.CommonConfigurationKeys
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.cfir.entrypoint.configuration.noPrelude
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.config.TestPhaseDirectives
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.NO_PRELUDE
import org.cangnova.cangjie.test.config.addSourcesForDependsOnClosure
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.WITH_STDLIB
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.CFIR_PARSER
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirective
import org.cangnova.cangjie.test.directives.model.ValueDirective
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue
import org.cangnova.cangjie.test.model.ServicesAndDirectivesContainer
import org.cangnova.cangjie.test.model.TestModule
import java.io.File
import kotlin.collections.set

@DslMarker
annotation class DefaultsDsl

abstract class AbstractEnvironmentConfigurator : ServicesAndDirectivesContainer {
    open fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion,
    ): Map<AnalysisFlag<*>, Any?> = emptyMap()

    abstract fun configureCompileConfigurationWithAdditionalConfigurationKeys(
        configuration: CompilerConfiguration,
        module: TestModule
    )

    open val compilationStage: CompilationStage
        get() = CompilationStage.FIRST

}

abstract class EnvironmentConfigurator(protected val testServices: TestServices) : AbstractEnvironmentConfigurator() {
    final override fun configureCompileConfigurationWithAdditionalConfigurationKeys(
        configuration: CompilerConfiguration,
        module: TestModule,
    ) {
        val extractor = DirectiveToConfigurationKeyExtractor()
        extractor.provideConfigurationKeys()
        extractor.configure(configuration, module.directives)
        configureCompilerConfiguration(configuration, module)
    }
    open fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {}

    protected open fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {}

}

class CommonEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion,
    ): Map<AnalysisFlag<*>, Any?> {
        return buildMap {
            if (NO_PRELUDE in directives) {
                put(AnalysisFlags.noPrelude, true)
            }
        }
    }

    override fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {
        register(CfirDiagnosticsDirectives.DUMP_INFERENCE_LOGS, CommonConfigurationKeys.DUMP_INFERENCE_LOGS)
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val noPreludeEnabled = module.hasDirective(NO_PRELUDE)
        addRuntimeClasspathRoots(configuration, module)
        configuration.noPrelude = noPreludeEnabled
        if (WITH_STDLIB in module.directives && !noPreludeEnabled) {
            addStdlibClasspathRoots(configuration)
        }
        setupCliConfiguration(module, configuration)

    }

    private fun setupCliConfiguration(
        module: TestModule,
        configuration: CompilerConfiguration,
    ){



        when (module.directives[CFIR_PARSER].lastOrNull()) {
            CfirParser.Psi -> configuration.useLightTree = false
            CfirParser.LightTree -> configuration.useLightTree = true
            null -> {}
        }

        configuration.addSourcesForDependsOnClosure(module, testServices)

    }

    private fun addRuntimeClasspathRoots(configuration: CompilerConfiguration, module: TestModule) {
        val runtimeRoots = testServices.runtimeClasspathProviders
            .asSequence()
            .flatMap { it.runtimeClasspath(module).asSequence() }
            .map(::File)
            .filter { it.path.isNotBlank() }
            .map { it.normalize() }
            .distinctBy { it.absolutePath }
            .toList()

        runtimeRoots.forEach { configuration.addClasspathRoot(it.path) }
    }

    private fun addStdlibClasspathRoots(configuration: CompilerConfiguration) {
        resolveStdlibRoots()
            .distinctBy { it.absolutePath }
            .forEach { configuration.addClasspathRoot(it.path) }
    }

    private fun resolveStdlibRoots(): List<File> {
        val fromEnv = System.getenv("CANGJIE_STDLIB_MODULE")
            ?.split(File.pathSeparator)
            .orEmpty()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map(::File)
            .map(::normalizeStdlibRoot)
            .filter { it.exists() && it.isDirectory }
            .toList()
        if (fromEnv.isNotEmpty()) return fromEnv

        val fallbackCandidates = listOf(
            File("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative"),
            File("cfir/cfir-serialization/build/resources/test/cjo-sdk/windows_x86_64_cjnative"),
        )

        return fallbackCandidates
            .asSequence()
            .filter { it.exists() && it.isDirectory }
            .map(::normalizeStdlibRoot)
            .toList()
    }

    private fun normalizeStdlibRoot(path: File): File {
        val normalized = path.normalize()
        if (normalized.resolve("std/std.core.cjo").isFile) return normalized
        if (normalized.resolve("std.core.cjo").isFile) return normalized.parentFile ?: normalized
        return normalized
    }
}

private fun TestModule.hasDirective(directive: SimpleDirective): Boolean {
    return directive in directives || files.any { directive in it.directives }
}

class DirectiveToConfigurationKeyExtractor {
    private val booleanDirectivesMap = mutableMapOf<SimpleDirective, CompilerConfigurationKey<Boolean>>()
    private val invertedBooleanDirectives = mutableSetOf<SimpleDirective>()
    private val valueDirectivesMap = mutableMapOf<ValueDirective<*>, CompilerConfigurationKey<*>>()

    fun register(
        directive: SimpleDirective,
        key: CompilerConfigurationKey<Boolean>,
        isInverted: Boolean = false
    ) {
        booleanDirectivesMap[directive] = key
        if (isInverted) {
            invertedBooleanDirectives += directive
        }
    }

    fun <T : Any> register(
        directive: ValueDirective<T>,
        key: CompilerConfigurationKey<T>
    ) {
        valueDirectivesMap[directive] = key
    }

    fun configure(configuration: CompilerConfiguration, registeredDirectives: RegisteredDirectives) {
        for ((directive, key) in booleanDirectivesMap) {
            if (directive in registeredDirectives) {
                val value = directive !in invertedBooleanDirectives
                configuration.put(key, value)
            }
        }
        for ((directive, key) in valueDirectivesMap) {
            val value = registeredDirectives.singleOrZeroValue(directive) ?: continue
            configuration.put(key, value)
        }
    }
}
