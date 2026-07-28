/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.AnalysisFlag
import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.cfir.entrypoint.configuration.CfirFrontendConfigurationKeys
import org.cangnova.cangjie.cfir.entrypoint.configuration.apiLevel
import org.cangnova.cangjie.cfir.entrypoint.configuration.apiLevelSyscapConfigPath
import org.cangnova.cangjie.cfir.entrypoint.configuration.noPrelude
import org.cangnova.cangjie.config.*
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.config.addSourcesForDependsOnClosure
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.API_LEVEL
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.API_LEVEL_SYSCAP
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.IMPORT_PATH
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.NO_PRELUDE
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.WITH_STDLIB
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.CFIR_PARSER
import org.cangnova.cangjie.test.directives.ConfigurationDirectives
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirective
import org.cangnova.cangjie.test.directives.model.ValueDirective
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue
import org.cangnova.cangjie.test.model.ServicesAndDirectivesContainer
import org.cangnova.cangjie.test.model.TestModule
import java.io.File

/**
 * 表示 `DefaultsDsl`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
@DslMarker
annotation class DefaultsDsl

/**
 * 表示 `AbstractEnvironmentConfigurator`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
abstract class AbstractEnvironmentConfigurator : ServicesAndDirectivesContainer {
    /**
     * 提供 `provideAdditionalAnalysisFlags` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    open fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion,
    ): Map<AnalysisFlag<*>, Any?> = emptyMap()

    /**
     * 提供 `configureCompileConfigurationWithAdditionalConfigurationKeys` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    abstract fun configureCompileConfigurationWithAdditionalConfigurationKeys(
        configuration: CompilerConfiguration,
        module: TestModule
    )

    /**
     * 保存 `compilationStage`，供测试服务在测试执行期间读取或传递。
     */
    open val compilationStage: CompilationStage
        get() = CompilationStage.FIRST

}

/**
 * 表示 `EnvironmentConfigurator`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
abstract class EnvironmentConfigurator(protected val testServices: TestServices) : AbstractEnvironmentConfigurator() {
    /**
     * 执行 `configureCompileConfigurationWithAdditionalConfigurationKeys` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    final override fun configureCompileConfigurationWithAdditionalConfigurationKeys(
        configuration: CompilerConfiguration,
        module: TestModule,
    ) {
        val extractor = DirectiveToConfigurationKeyExtractor()
        extractor.provideConfigurationKeys()
        extractor.configure(configuration, module.directives)
        configureCompilerConfiguration(configuration, module)
    }
    /**
     * 提供 `provideConfigurationKeys` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    open fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {}

    /**
     * 提供 `configureCompilerConfiguration` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    protected open fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {}

}

/**
 * 表示 `CommonEnvironmentConfigurator`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class CommonEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    /**
     * 保存 `directiveContainers`，供测试服务在测试执行期间读取或传递。
     */
    override val directiveContainers: List<org.cangnova.cangjie.test.directives.model.DirectivesContainer>
        get() = listOf(ConfigurationDirectives)

    /**
     * 执行 `provideAdditionalAnalysisFlags` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion,
    ): Map<AnalysisFlag<*>, Any?> {
        return buildMap {
            if (NO_PRELUDE in directives) {
                put(AnalysisFlags.noPrelude, true)
            }
            if (ConfigurationDirectives.DISABLE_TYPEALIAS_EXPANSION in directives) {
                put(AnalysisFlags.expandTypeAliasesInTypeResolution, false)
            }
        }
    }

    /**
     * 执行 `provideConfigurationKeys` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {
        register(CfirDiagnosticsDirectives.DUMP_INFERENCE_LOGS, CommonConfigurationKeys.DUMP_INFERENCE_LOGS)
        register(CfirDiagnosticsDirectives.CHECK_PROGRAM_ENTRY, CfirFrontendConfigurationKeys.CHECK_PROGRAM_ENTRY)
        register(CfirDiagnosticsDirectives.NO_SUB_PKG, CfirFrontendConfigurationKeys.NO_SUB_PACKAGE)
    }

    /**
     * 执行 `configureCompilerConfiguration` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val noPreludeEnabled = module.hasDirective(NO_PRELUDE)
        addRuntimeClasspathRoots(configuration, module)
        addImportPathRoots(configuration, module)
        configuration.noPrelude = noPreludeEnabled
        configuration.apiLevel = module.directives[API_LEVEL]
            .lastOrNull()
            ?.toIntOrNull()
        configuration.apiLevelSyscapConfigPath = module.directives[API_LEVEL_SYSCAP]
            .lastOrNull()
            ?.let { resolveTestDataPath(module, it) }
            ?.path
        if (WITH_STDLIB in module.directives && !noPreludeEnabled) {
            addStdlibClasspathRoots(configuration)
        }
        setupCliConfiguration(module, configuration)

    }

    /**
     * 提供 `setupCliConfiguration` 对应的测试服务流程，维持测试框架的阶段契约。
     */
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

    /**
     * 提供 `addRuntimeClasspathRoots` 对应的测试服务流程，维持测试框架的阶段契约。
     */
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

    /**
     * LLT 迁移后允许测试数据显式声明附加 import/classpath 根目录。
     *
     * 这里按测试数据文件所在目录解析相对路径，避免把旧 LLT `--import-path`
     * 语义退化成仓库根目录相对路径。
     */
    private fun addImportPathRoots(configuration: CompilerConfiguration, module: TestModule) {
        module.directives[IMPORT_PATH]
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .mapNotNull { resolveTestDataPath(module, it) }
            .filter { it.exists() }
            .distinctBy { it.absolutePath }
            .forEach { configuration.addClasspathRoot(it.path) }
    }

    /**
     * 按主测试文件所在目录解析测试指令中的路径。
     *
     * LLT 的 `--cfg` 与 `--import-path` 都以当前测试数据文件目录为基准；统一在这里
     * 转成规范化路径，避免 Gradle 工作目录改变测试语义。
     */
    private fun resolveTestDataPath(module: TestModule, rawPath: String): File? {
        val normalizedRawPath = rawPath.trim()
        if (normalizedRawPath.isEmpty()) return null

        val candidate = File(normalizedRawPath)
        if (candidate.isAbsolute) return candidate.normalize()

        val anchorDirectory = module.files
            .firstOrNull { !it.isAdditional }
            ?.originalFile
            ?.parentFile
            ?: return null
        return anchorDirectory.resolve(candidate).normalize()
    }

    /**
     * 提供 `addStdlibClasspathRoots` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun addStdlibClasspathRoots(configuration: CompilerConfiguration) {
        resolveStdlibRoots()
            .distinctBy { it.absolutePath }
            .forEach { configuration.addClasspathRoot(it.path) }
    }

    /**
     * 提供 `resolveStdlibRoots` 对应的测试服务流程，维持测试框架的阶段契约。
     */
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

    /**
     * 提供 `normalizeStdlibRoot` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun normalizeStdlibRoot(path: File): File {
        val normalized = path.normalize()
        if (normalized.resolve("std/std.core.cjo").isFile) return normalized
        if (normalized.resolve("std.core.cjo").isFile) return normalized.parentFile ?: normalized
        return normalized
    }
}

/**
 * 提供 `hasDirective` 对应的测试服务流程，维持测试框架的阶段契约。
 */
private fun TestModule.hasDirective(directive: SimpleDirective): Boolean {
    return directive in directives || files.any { directive in it.directives }
}

/**
 * 表示 `DirectiveToConfigurationKeyExtractor`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class DirectiveToConfigurationKeyExtractor {
    /**
     * 保存 `booleanDirectivesMap`，供测试服务在测试执行期间读取或传递。
     */
    private val booleanDirectivesMap = mutableMapOf<SimpleDirective, CompilerConfigurationKey<Boolean>>()
    /**
     * 保存 `invertedBooleanDirectives`，供测试服务在测试执行期间读取或传递。
     */
    private val invertedBooleanDirectives = mutableSetOf<SimpleDirective>()
    /**
     * 保存 `valueDirectivesMap`，供测试服务在测试执行期间读取或传递。
     */
    private val valueDirectivesMap = mutableMapOf<ValueDirective<*>, CompilerConfigurationKey<*>>()

    /**
     * 执行 `register` 对应的测试服务流程，维持测试框架的阶段契约。
     */
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

    /**
     * 执行 `register` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun <T : Any> register(
        directive: ValueDirective<T>,
        key: CompilerConfigurationKey<T>
    ) {
        valueDirectivesMap[directive] = key
    }

    /**
     * 执行 `configure` 对应的测试服务流程，维持测试框架的阶段契约。
     */
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
