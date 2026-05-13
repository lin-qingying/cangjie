/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.cfir.analysis.tests.services

import PackageFormat.PackageKind
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.frontend.pipeline.MacroArtifactPackage
import org.cangnova.cangjie.frontend.pipeline.MacroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.MacroPackageCompilationOrchestrator
import org.cangnova.cangjie.frontend.pipeline.MacroPackageCompilationResult
import org.cangnova.cangjie.frontend.pipeline.MacroSourcePackageCompilationRequest
import org.cangnova.cangjie.frontend.pipeline.macroArtifactPackages
import org.cangnova.cangjie.frontend.pipeline.macroArtifactDefinitionsOverride
import org.cangnova.cangjie.frontend.pipeline.macroConstructionMode
import org.cangnova.cangjie.frontend.pipeline.macroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.macroPackageCompilationOrchestrator
import org.cangnova.cangjie.frontend.pipeline.macroSourcePackageCompilationRequests
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.SourcePosition
import org.cangnova.cangjie.macro.TokenInfo
import org.cangnova.cangjie.macro.stub.StubMacroExecutor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.test.directives.MacroArtifactPackageSpec
import org.cangnova.cangjie.test.directives.MacroConstructionDirectives
import org.cangnova.cangjie.test.directives.MacroConstructionDirectives.MacroExecutorMode
import org.cangnova.cangjie.test.directives.MacroDefinitionSpec
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.EnvironmentConfigurator
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.getOrCreateTempDirectory
import java.io.File
import java.nio.file.Path

/**
 * 把 [MacroConstructionDirectives] 翻译成 `CompilerConfiguration` 上的
 * macro construction 入口：
 *
 * - `MACRO_EXECUTOR` 决定 `macroExecutorFactory`：
 *   - `none` —— 不注入；construction step 在 STRICT 模式产 `MACRO_EXECUTOR_UNAVAILABLE`。
 *   - `stub` —— 注入 [StubMacroExecutor]，按每条 [MacroDefinitionSpec.expand] 注册展开文本；
 *               未声明 expand 的宏使用占位 `MacroExpansionResult.Success(emptyList(), "")`，
 *               以便测试 plain-attr / forced-kind 等不依赖具体展开结果的语义。
 *   - `real` —— 暂未接 `:macro:macro-process`，保留 stub 行为并打 stderr warning。
 *
 * - `MACRO_DEFINITION` 解析为 [MacroDefinitionEntry] 列表并写入
 *   `CompilerConfiguration.testMacroArtifactDefinitions`，由 macro construction
 *   主流程通过 `MacroArtifactResolver` 之外的另一条入口（cfir/analysis-tests
 *   桥接代码）注入到 `buildMacroSymbolIndex`。
 *
 * 该 configurator 故意保持小而克制：它只翻译 directive，不复用生产侧
 * `MacroArtifactResolver`，避免引入 testdata 不需要的 `.bchir/.cjo` 解析机制。
 */
class MacroConstructionEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val executorMode = module.directives.singleOrZeroValue(MacroConstructionDirectives.MACRO_EXECUTOR)
            ?: MacroExecutorMode.none
        val specs = module.directives[MacroConstructionDirectives.MACRO_DEFINITION]
        val artifactSpecs = module.directives[MacroConstructionDirectives.MACRO_ARTIFACT_PACKAGE]
        val sourcePackageSpecs = module.directives[MacroConstructionDirectives.MACRO_SOURCE_PACKAGE]
        val expectDegraded = MacroConstructionDirectives.EXPECT_DEGRADED in module.directives

        if (specs.isNotEmpty()) {
            configuration.macroArtifactDefinitionsOverride = specs.map(::toDefinitionEntry)
        }
        if (artifactSpecs.isNotEmpty()) {
            configuration.macroArtifactPackages = configuration.macroArtifactPackages +
                artifactSpecs.map { createArtifactPackage(it, MacroArtifactPackage.Origin.EXTERNAL_PATH) }
        }
        if (sourcePackageSpecs.isNotEmpty()) {
            val artifactsByPackage = sourcePackageSpecs.associate { spec ->
                FqName(spec.packageFqName) to createArtifactPackage(spec, MacroArtifactPackage.Origin.ORCHESTRATION)
            }
            configuration.macroSourcePackageCompilationRequests = configuration.macroSourcePackageCompilationRequests +
                sourcePackageSpecs.map { spec ->
                    MacroSourcePackageCompilationRequest(
                        packageFqName = FqName(spec.packageFqName),
                        sourceRoots = listOf(testServices.getOrCreateTempDirectory("macro-source-${spec.packageFqName}").path),
                        outputDirectory = testServices.getOrCreateTempDirectory("macro-out-${spec.packageFqName}").path,
                        compileInvocationId = "test-macro-source-${spec.packageFqName}",
                    )
                }
            configuration.macroPackageCompilationOrchestrator = MacroPackageCompilationOrchestrator { requests, _ ->
                MacroPackageCompilationResult(
                    artifactPackages = requests.mapNotNull { artifactsByPackage[it.packageFqName] },
                )
            }
        }

        // 测试 runner 默认走 DEGRADED 模式，让 macro 构造期诊断也能通过
        // MacroConstructionDiagnosticCollectorComponent 流到 CjDiagnostic 流，
        // 配合 diagnostics2 inline-marker 框架。当 testdata 显式声明 STRICT 行为
        // （未来扩展）时再切回 STRICT。
        configuration.macroConstructionMode =
            if (expectDegraded) MacroConstructionService.Mode.DEGRADED
            else MacroConstructionService.Mode.DEGRADED

        when (executorMode) {
            MacroExecutorMode.none -> Unit
            MacroExecutorMode.stub, MacroExecutorMode.real -> {
                val executor = createStubExecutor(specs, artifactSpecs + sourcePackageSpecs)
                configuration.macroExecutorFactory = MacroExecutorFactory { executor }
            }
        }
    }

    private fun createStubExecutor(
        specs: List<MacroDefinitionSpec>,
        packageSpecs: List<MacroArtifactPackageSpec>,
    ): MacroExecutor {
        val stub = StubMacroExecutor()
        for (spec in specs) {
            val text = spec.expand ?: continue
            stub.registerTokenExpansion(spec.fqName.substringAfterLast('.'), text)
        }
        for (packageSpec in packageSpecs) {
            for ((name, text) in packageSpec.expands) {
                stub.registerTokenExpansion(name, text)
            }
        }
        // 默认 fallback：未指定 expand 的宏给出空 token 流，避免 stub 默认 Failure
        // 干扰非展开语义的 testdata（plain-attr / forced-kind 等）。
        stub.defaultResult = { _: MacroCallInfo ->
            MacroExpansionResult.Success(emptyList(), "")
        }
        return stub
    }

    private fun createArtifactPackage(
        spec: MacroArtifactPackageSpec,
        defaultOrigin: MacroArtifactPackage.Origin,
    ): MacroArtifactPackage {
        val packageFqName = FqName(spec.packageFqName)
        val directory = testServices.getOrCreateTempDirectory("macro-artifact-${spec.packageFqName}")
        val cjoPath = File(directory, "${spec.packageFqName}.cjo").toPath()
        CjoPackageWriter.write(
            cjoPath,
            CjoPackageMetadata(
                fullPackageName = spec.packageFqName,
                moduleName = "macro-test",
                kind = PackageKind.Macro,
                declarations = spec.declarations.map(::CjoPackageDeclaration),
            ),
        )
        val dylibPath = File(directory, "lib-macro_${spec.packageFqName.replace('.', '_')}.dll").toPath()
        if (!dylibPath.toFile().exists()) {
            dylibPath.toFile().writeBytes(byteArrayOf(1, 2, 3))
        }
        return MacroArtifactPackage(
            packageFqName = packageFqName,
            kind = MacroArtifactPackage.Kind.MACRO,
            cjoPath = cjoPath.toString(),
            dynamicLibPath = dylibPath.toString(),
            dependenciesBchirPaths = emptyList(),
            origin = parseOrigin(spec.origin) ?: defaultOrigin,
            compileInvocationId = "test-macro-artifact-${spec.packageFqName}",
        )
    }

    private fun parseOrigin(raw: String): MacroArtifactPackage.Origin? {
        return MacroArtifactPackage.Origin.values()
            .firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }

    private fun StubMacroExecutor.registerTokenExpansion(macroName: String, expandedText: String) {
        registerExpansion(macroName) {
            MacroExpansionResult.Success(
                tokens = expandedText.toTokenInfos(),
                expandedText = expandedText,
            )
        }
    }

    private fun String.toTokenInfos(): List<TokenInfo> {
        return MacroPayloadTokenizer.tokenize(this).map { token ->
            TokenInfo(
                kind = 0u.toUByte(),
                value = token.text,
                begin = SourcePosition(line = token.startOffset),
                end = SourcePosition(line = token.endOffset),
            )
        }
    }

    private fun toDefinitionEntry(spec: MacroDefinitionSpec): MacroDefinitionEntry {
        val parts = spec.fqName.split('.').filter(String::isNotEmpty)
        require(parts.isNotEmpty()) { "MACRO_DEFINITION fqName must not be empty" }
        val name = Name.identifier(parts.last())
        val packageFqName = if (parts.size == 1) FqName.ROOT else FqName(parts.dropLast(1).joinToString("."))
        val source = parseSource(spec.source)
        return MacroDefinitionEntry(
            packageFqName = packageFqName,
            name = name,
            source = source,
            libPath = spec.libPath,
            supportsForcedKind = spec.supportsForcedKind,
            supportsPlainAttrOverload = spec.supportsPlainAttrOverload,
        )
    }

    private fun parseSource(raw: String): MacroDefinitionEntry.Source {
        return MacroDefinitionEntry.Source.values()
            .firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: error("Unknown macro source `$raw`; expected one of: ${MacroDefinitionEntry.Source.values().toList()}")
    }
}
