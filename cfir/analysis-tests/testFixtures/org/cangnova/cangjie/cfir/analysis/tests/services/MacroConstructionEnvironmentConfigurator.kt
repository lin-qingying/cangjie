/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.cfir.analysis.tests.services

import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.frontend.pipeline.MacroSourcePackageCompilationRequest
import org.cangnova.cangjie.frontend.pipeline.MacroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.macroExecutorFactory
import org.cangnova.cangjie.frontend.pipeline.macroBackgroundAutoCompilationEnabled
import org.cangnova.cangjie.frontend.pipeline.macroConstructionMode
import org.cangnova.cangjie.frontend.pipeline.macroExpansionDemandAutoCompilationEnabled
import org.cangnova.cangjie.frontend.pipeline.macroSdkHome
import org.cangnova.cangjie.frontend.pipeline.macroSourcePackageCompilationRequests
import org.cangnova.cangjie.macro.process.LspMacroServerMacroExecutor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.test.directives.MacroConstructionDirectives
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.EnvironmentConfigurator
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.getOrCreateTempDirectory
import org.cangnova.cangjie.test.services.moduleStructure
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 为 macro 端到端测试提供真实同项目 macro source root。
 *
 * 本配置器不解析 `public macro` 生成假 `.cjo/.dll`，也不注入
 * `MacroDefinitionEntry` 或 stub executor。测试里的 `macro package` 文件只会
 * 被复制到临时源码根，后续由 production orchestrator 调用
 * `cjc -p <root> --compile-macro -o <dir>` 生成真实 artifact。
 */
class MacroConstructionEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.macroBackgroundAutoCompilationEnabled = false
        configuration.macroExpansionDemandAutoCompilationEnabled =
            MacroConstructionDirectives.DISABLE_EXPANSION_DEMAND_AUTO_COMPILE_MACRO_PACKAGES !in module.directives

        val sourcePackages = collectSourceMacroPackages(module)
        if (sourcePackages.isNotEmpty()) {
            val testCaseId = testServices.moduleStructure.originalTestDataFiles
                .singleOrNull()
                ?.nameWithoutExtension
                ?.sanitizeFileName()
                ?: module.name.sanitizeFileName()
            configuration.macroSourcePackageCompilationRequests =
                configuration.macroSourcePackageCompilationRequests + sourcePackages.map { sourcePackage ->
                    val sourceRoot = materializeSourcePackage(testCaseId, sourcePackage)
                    val outputDirectory = testServices.getOrCreateTempDirectory(
                        "macro-out-$testCaseId-${sourcePackage.packageFqName.asString().sanitizeFileName()}",
                    )
                    MacroSourcePackageCompilationRequest(
                        packageFqName = sourcePackage.packageFqName,
                        sourceRoots = listOf(sourceRoot.path),
                        importPaths = configuration.classpathRoots.map { it.path },
                        classpath = configuration.classpathRoots.map { it.path },
                        outputDirectory = outputDirectory.path,
                        compileInvocationId = "test-macro-source-$testCaseId-${sourcePackage.packageFqName.asString()}",
                    )
                }
        }

        if (configuration.macroExecutorFactory == null) {
            val executable = File(
                configuration.macroSdkHome,
                "tools/bin/${if (isWindows()) "LSPMacroServer.exe" else "LSPMacroServer"}",
            ).path
            configuration.macroExecutorFactory = MacroExecutorFactory { LspMacroServerMacroExecutor(executable) }
        }

        configuration.macroConstructionMode = MacroConstructionService.Mode.DEGRADED
    }

    private fun materializeSourcePackage(testCaseId: String, sourcePackage: SourceMacroPackage): File {
        val root = testServices.getOrCreateTempDirectory(
            "macro-source-$testCaseId-${sourcePackage.packageFqName.asString().sanitizeFileName()}",
        )
        for (file in sourcePackage.files) {
            val target = File(root, file.relativePath)
            target.parentFile?.mkdirs()
            target.writeText(file.originalContent, StandardCharsets.UTF_8)
        }
        return root
    }

    private fun collectSourceMacroPackages(module: TestModule): List<SourceMacroPackage> {
        val result = linkedMapOf<FqName, MutableList<TestFile>>()
        for (file in module.files) {
            val packageName = macroPackageRegex.find(file.originalContent)?.groupValues?.get(1) ?: continue
            result.getOrPut(FqName(packageName)) { mutableListOf() } += file
        }
        return result.map { (packageFqName, files) ->
            SourceMacroPackage(packageFqName, files)
        }
    }

    private data class SourceMacroPackage(
        val packageFqName: FqName,
        val files: List<TestFile>,
    )

    private companion object {
        val macroPackageRegex = Regex("""(?m)^\s*macro\s+package\s+([A-Za-z_][A-Za-z0-9_.]*)\s*$""")
    }
}

private fun String.sanitizeFileName(): String =
    replace(Regex("""[^A-Za-z0-9_.-]"""), "_")

private fun isWindows(): Boolean = System.getProperty("os.name").contains("Windows", ignoreCase = true)
