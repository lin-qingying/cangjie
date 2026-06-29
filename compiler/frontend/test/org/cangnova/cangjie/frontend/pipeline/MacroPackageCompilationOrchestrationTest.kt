package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.PackageKind
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirReplaceHandle
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceContainerContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceScopeContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.CjIoFileSourceFile
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.targetPlatform
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@OptIn(CompilerConfiguration.Internals::class)
/**
 * 覆盖宏源码包按需编译选择、artifact 准备和外部 cjc 调度行为。
 */
class MacroPackageCompilationOrchestrationTest {
    /**
     * 每个测试独占的临时目录。
     */
    @TempDir
    lateinit var tempDir: Path

    /**
     * 验证未配置 orchestrator 时生成编译失败诊断。
     */
    @Test
    fun missingOrchestratorReportsCompileFailedDiagnostic() {
        val request = MacroSourcePackageCompilationRequest(
            packageFqName = FqName("macros.pkg"),
            sourceRoots = listOf("src/macros"),
            compileInvocationId = "compile-macros.pkg",
            sourceDiagnosticsRef = "diagnostics://macros.pkg",
        )

        val diagnostics = unresolvedMacroPackageCompilationDiagnostics(listOf(request))

        val diagnostic = diagnostics.single()
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED, diagnostic.kind)
        assertEquals(MacroConstructionDiagnostic.Origin.ORCHESTRATION, diagnostic.diagnosticOrigin)
        assertEquals(FqName("macros.pkg"), diagnostic.artifactPackage)
        assertEquals("compile-macros.pkg", diagnostic.compileInvocationId)
        assertEquals("diagnostics://macros.pkg", diagnostic.sourceDiagnosticsRef)
        assertTrue(diagnostic.message.contains("compile-macro"))
    }

    /**
     * 验证显式配置的 orchestrator 会收到请求列表和缓存上下文。
     */
    @Test
    fun configuredOrchestratorReceivesRequestsAndCacheContext() {
        val configuration = CompilerConfiguration()
        val cacheContext = MacroCompilationCacheContext(
            compilerOptionsFingerprint = "options",
            debugFlagsFingerprint = "debug",
            parallelFlagsFingerprint = "parallel",
            targetPlatform = CangJiePlatforms.cjvm,
            runtimeLoaderEnvironmentFingerprint = "loader-env",
        )
        val request = MacroSourcePackageCompilationRequest(
            packageFqName = FqName("macros.pkg"),
            sourceRoots = listOf("src/macros"),
        )

        configuration.macroSourcePackageCompilationRequests = listOf(request)
        configuration.macroCompilationCacheContext = cacheContext
        configuration.macroPackageCompilationOrchestrator = MacroPackageCompilationOrchestrator { requests, context ->
            assertEquals(listOf(request), requests)
            assertSame(configuration, context.configuration)
            assertEquals(configuration.macroExecutorAbiVersion, context.executorAbiVersion)
            assertEquals(cacheContext, context.cacheContext)
            MacroPackageCompilationResult()
        }

        val result = configuration.macroPackageCompilationOrchestrator!!.compileMacroPackages(
            configuration.macroSourcePackageCompilationRequests,
            MacroPackageCompilationContext(
                configuration = configuration,
                executorAbiVersion = configuration.macroExecutorAbiVersion,
                cacheContext = configuration.macroCompilationCacheContext,
            ),
        )

        assertTrue(result.diagnostics.isEmpty())
        assertTrue(result.artifactPackages.isEmpty())
    }

    /**
     * 验证默认宏缓存上下文继承编译配置中的 target platform。
     */
    @Test
    fun defaultMacroCacheContextInheritsConfigurationTargetPlatformPlaceholder() {
        val configuration = CompilerConfiguration().apply {
            targetPlatform = CangJiePlatforms.cjvm
        }

        assertSame(CangJiePlatforms.cjvm, configuration.macroCompilationCacheContext.targetPlatform)
    }

    /**
     * 验证 CFIR 前端默认安装外部 cjc 宏包编译 orchestrator。
     */
    @Test
    fun cfirFrontendConfigurationInstallsDefaultMacroPackageCompilationOrchestrator() {
        val configuration = CompilerConfiguration.createForCfirFrontend()
        configuration.initializeCfirFrontendMacroCompilationConfiguration()
        assertTrue(
            configuration.macroPackageCompilationOrchestrator is ExternalCjcMacroPackageCompilationOrchestrator,
            "CFIR frontend configuration should install the external cjc macro compilation orchestrator by default.",
        )
    }

    /**
     * 验证初始化前端配置时不会覆盖用户自定义 orchestrator。
     */
    @Test
    fun cfirFrontendInitializationPreservesCustomMacroPackageCompilationOrchestrator() {
        val custom = MacroPackageCompilationOrchestrator { _, _ -> MacroPackageCompilationResult() }
        val configuration = CompilerConfiguration().apply {
            macroPackageCompilationOrchestrator = custom
        }

        configuration.initializeCfirFrontendMacroCompilationConfiguration()

        assertSame(custom, configuration.macroPackageCompilationOrchestrator)
    }

    /**
     * 验证仅被 import 实际 demand 的宏源码包会被选中编译。
     */
    @Test
    fun expansionDemandSelectsOnlyImportedMacroSourcePackageRequests() {
        val fixture = macroDemandFixture(
            imports = listOf(macroImport("macros.pkg.Generated")),
            surface = macroSurface("Generated"),
        )
        val required = MacroSourcePackageCompilationRequest(
            packageFqName = FqName("macros.pkg"),
            sourceRoots = listOf("src/macros"),
        )
        val unrelated = MacroSourcePackageCompilationRequest(
            packageFqName = FqName("unused.pkg"),
            sourceRoots = listOf("src/unused"),
        )

        val selected = selectMacroSourcePackageCompilationRequestsForExpansion(
            preResults = listOf(fixture.pre),
            requests = listOf(required, unrelated),
            suppliedArtifacts = emptyList(),
        )

        assertEquals(listOf(required), selected)
    }

    /**
     * 验证全限定宏调用无需 import 也能选择对应宏源码包请求。
     */
    @Test
    fun expansionDemandSelectsQualifiedMacroSourcePackageRequestWithoutImport() {
        val fixture = macroDemandFixture(
            imports = emptyList(),
            surface = macroSurface("macros.pkg.Generated"),
        )
        val request = MacroSourcePackageCompilationRequest(
            packageFqName = FqName("macros.pkg"),
            sourceRoots = listOf("src/macros"),
        )

        val selected = selectMacroSourcePackageCompilationRequestsForExpansion(
            preResults = listOf(fixture.pre),
            requests = listOf(request),
            suppliedArtifacts = emptyList(),
        )

        assertEquals(listOf(request), selected)
    }

    /**
     * 验证 all-under import 能触发宏源码包请求选择。
     */
    @Test
    fun expansionDemandSelectsAllUnderImportedMacroSourcePackageRequest() {
        val fixture = macroDemandFixture(
            imports = listOf(macroImport("macros.pkg", isAllUnder = true)),
            surface = macroSurface("Generated"),
        )
        val request = MacroSourcePackageCompilationRequest(
            packageFqName = FqName("macros.pkg"),
            sourceRoots = listOf("src/macros"),
        )

        val selected = selectMacroSourcePackageCompilationRequestsForExpansion(
            preResults = listOf(fixture.pre),
            requests = listOf(request),
            suppliedArtifacts = emptyList(),
        )

        assertEquals(listOf(request), selected)
    }

    /**
     * 验证带 alias 的宏 import 能触发对应源码包请求选择。
     */
    @Test
    fun expansionDemandSelectsAliasedMacroSourcePackageRequest() {
        val fixture = macroDemandFixture(
            imports = listOf(macroImport("macros.pkg.Generated", alias = "Make")),
            surface = macroSurface("Make"),
        )
        val request = MacroSourcePackageCompilationRequest(
            packageFqName = FqName("macros.pkg"),
            sourceRoots = listOf("src/macros"),
        )

        val selected = selectMacroSourcePackageCompilationRequestsForExpansion(
            preResults = listOf(fixture.pre),
            requests = listOf(request),
            suppliedArtifacts = emptyList(),
        )

        assertEquals(listOf(request), selected)
    }

    /**
     * 验证已有 artifact 能满足 demand，避免重复编译源码包。
     */
    @Test
    fun expansionDemandDoesNotCompileSourcePackageWhenArtifactIsAlreadySupplied() {
        val fixture = macroDemandFixture(
            imports = listOf(macroImport("macros.pkg.Generated")),
            surface = macroSurface("Generated"),
        )
        val request = MacroSourcePackageCompilationRequest(
            packageFqName = FqName("macros.pkg"),
            sourceRoots = listOf("src/macros"),
        )
        val suppliedArtifact = MacroArtifactPackage(
            packageFqName = FqName("macros.pkg"),
            kind = MacroArtifactPackage.Kind.MACRO,
            cjoPath = "compiled/macros.pkg.cjo",
            dynamicLibPath = "compiled/lib-macro_macros.pkg.dll",
            dependenciesBchirPaths = emptyList(),
            origin = MacroArtifactPackage.Origin.ORCHESTRATION,
        )

        val selected = selectMacroSourcePackageCompilationRequestsForExpansion(
            preResults = listOf(fixture.pre),
            requests = listOf(request),
            suppliedArtifacts = listOf(suppliedArtifact),
        )

        assertTrue(selected.isEmpty())
    }

    /**
     * 验证同项目宏源码根可从 source root 中发现。
     */
    @Test
    fun expansionDemandDiscoversSameProjectMacroSourcePackageRoot() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src"))
        val macroSource = sourceRoot.resolve("macros.cj")
        Files.writeString(
            macroSource,
            "macro package macros.pkg\npublic macro Generated(input: Tokens): Tokens { input }\n"
        )
        val fixture = macroDemandFixture(
            imports = listOf(macroImport("macros.pkg.Generated")),
            surface = macroSurface("Generated"),
            extraFiles = listOf(macroPackageFile("macros.pkg", macroSource.toFile())),
        )
        val configuration = CompilerConfiguration().apply {
            addCangJieSourceRoot(sourceRoot.toString())
            macroPackageCompilationOrchestrator = MacroPackageCompilationOrchestrator { requests, _ ->
                val request = requests.single()
                assertEquals(FqName("macros.pkg"), request.packageFqName)
                assertEquals(listOf(sourceRoot.toFile().absoluteFile.normalize().path), request.sourceRoots)
                MacroPackageCompilationResult(
                    diagnostics = listOf(
                        MacroConstructionDiagnostic(
                            severity = MacroConstructionDiagnostic.Severity.ERROR,
                            message = "stop after request assertion",
                            kind = MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED,
                            artifactPackage = request.packageFqName,
                            diagnosticOrigin = MacroConstructionDiagnostic.Origin.ORCHESTRATION,
                        ),
                    ),
                )
            }
        }

        val result = prepareMacroArtifactDefinitionsForPreResults(configuration, listOf(fixture.pre))

        assertEquals(
            MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED,
            result.diagnostics.single().kind,
        )
    }

    /**
     * 验证缺失的同项目宏源码包会被编译、重新定位并解析为定义。
     */
    @Test
    fun expansionDemandCompilesMissingSameProjectMacroPackageThenRelocatesAndResolvesArtifact() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src"))
        val macroSource = sourceRoot.resolve("macros.cj")
        Files.writeString(
            macroSource,
            "macro package macros.pkg\npublic macro Generated(input: Tokens): Tokens { input }\n"
        )
        val outputDir = Files.createDirectories(tempDir.resolve("compiled"))
        val fixture = macroDemandFixture(
            imports = listOf(macroImport("macros.pkg.Generated")),
            surface = macroSurface("Generated"),
            extraFiles = listOf(macroPackageFile("macros.pkg", macroSource.toFile())),
        )
        var compileCalls = 0
        val configuration = CompilerConfiguration().apply {
            addCangJieSourceRoot(sourceRoot.toString())
            macroPackageCompilationOrchestrator = MacroPackageCompilationOrchestrator { requests, context ->
                compileCalls++
                assertEquals(listOf(FqName("macros.pkg")), requests.map { it.packageFqName })
                assertEquals(CompilerConfiguration().macroExecutorAbiVersion, context.executorAbiVersion)
                writeCompiledMacroArtifact(outputDir, "macros.pkg")
                MacroPackageCompilationResult(artifactSearchPaths = listOf(outputDir.toString()))
            }
        }

        val result = prepareMacroArtifactDefinitionsForPreResults(configuration, listOf(fixture.pre))

        assertEquals(1, compileCalls)
        assertTrue(result.diagnostics.isEmpty(), "Unexpected diagnostics: ${result.diagnostics}")
        assertEquals(listOf("Generated"), result.definitions.map { it.name.asString() })
        assertEquals(MacroDefinitionEntry.Source.MACRO_ARTIFACT, result.definitions.single().source)
        assertEquals(
            outputDir.resolve("lib-macro_macros.pkg.${dynamicLibraryExtension()}").toString(),
            result.definitions.single().libPath
        )
        assertEquals(listOf(FqName("macros.pkg")), result.locatedArtifacts.map { it.packageFqName })
    }

    /**
     * 验证禁用按需自动编译时直接报告缺失 artifact，且不调用 orchestrator。
     */
    @Test
    fun expansionDemandAutoCompilationDisabledReportsMissingArtifactWithoutCallingOrchestrator() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src"))
        val macroSource = sourceRoot.resolve("macros.cj")
        Files.writeString(
            macroSource,
            "macro package macros.pkg\npublic macro Generated(input: Tokens): Tokens { input }\n"
        )
        val fixture = macroDemandFixture(
            imports = listOf(macroImport("macros.pkg.Generated")),
            surface = macroSurface("Generated"),
            extraFiles = listOf(macroPackageFile("macros.pkg", macroSource.toFile())),
        )
        var compileCalls = 0
        val configuration = CompilerConfiguration().apply {
            addCangJieSourceRoot(sourceRoot.toString())
            macroExpansionDemandAutoCompilationEnabled = false
            macroPackageCompilationOrchestrator = MacroPackageCompilationOrchestrator { _, _ ->
                compileCalls++
                error("Auto compilation is disabled and must not invoke the orchestrator")
            }
        }

        val result = prepareMacroArtifactDefinitionsForPreResults(configuration, listOf(fixture.pre))

        assertEquals(0, compileCalls)
        assertTrue(result.definitions.isEmpty())
        val diagnostic = result.diagnostics.single()
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED, diagnostic.kind)
        assertEquals(FqName("macros.pkg"), diagnostic.artifactPackage)
        assertTrue(diagnostic.message.contains("disabled"))
    }

    /**
     * 验证找不到同项目宏源码根时报告缺失根诊断。
     */
    @Test
    fun expansionDemandWithoutSameProjectSourceRootReportsMissingRootDiagnostic() {
        val fixture = macroDemandFixture(
            imports = listOf(macroImport("macros.pkg.Generated")),
            surface = macroSurface("Generated"),
        )
        var compileCalls = 0
        val configuration = CompilerConfiguration().apply {
            macroPackageCompilationOrchestrator = MacroPackageCompilationOrchestrator { _, _ ->
                compileCalls++
                error("Missing same-project source root must be reported before invoking the orchestrator")
            }
        }

        val result = prepareMacroArtifactDefinitionsForPreResults(configuration, listOf(fixture.pre))

        assertEquals(0, compileCalls)
        assertTrue(result.definitions.isEmpty())
        val diagnostic = result.diagnostics.single()
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED, diagnostic.kind)
        assertEquals(MacroConstructionDiagnostic.Origin.ORCHESTRATION, diagnostic.diagnosticOrigin)
        assertEquals(FqName("macros.pkg"), diagnostic.artifactPackage)
        assertEquals(1, diagnostic.originSurfaceId)
        assertTrue(diagnostic.message.contains("no same-project macro source root"))
    }

    /**
     * 验证已有 classpath artifact 可直接解析，不再触发同项目源码包编译。
     */
    @Test
    fun existingArtifactIsResolvedWithoutRepeatedSameProjectMacroCompilation() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src"))
        val artifactRoot = Files.createDirectories(tempDir.resolve("artifacts"))
        val macroSource = sourceRoot.resolve("macros.cj")
        Files.writeString(
            macroSource,
            "macro package macros.pkg\npublic macro Generated(input: Tokens): Tokens { input }\n"
        )
        writeCompiledMacroArtifact(artifactRoot, "macros.pkg")
        val fixture = macroDemandFixture(
            imports = listOf(macroImport("macros.pkg.Generated")),
            surface = macroSurface("Generated"),
            extraFiles = listOf(macroPackageFile("macros.pkg", macroSource.toFile())),
        )
        var compileCalls = 0
        val configuration = CompilerConfiguration().apply {
            addCangJieSourceRoot(sourceRoot.toString())
            addClasspathRoot(artifactRoot.toString())
            macroPackageCompilationOrchestrator = MacroPackageCompilationOrchestrator { _, _ ->
                compileCalls++
                error("Existing artifact should satisfy demand before source compilation")
            }
        }

        val result = prepareMacroArtifactDefinitionsForPreResults(configuration, listOf(fixture.pre))

        assertEquals(0, compileCalls)
        assertTrue(result.diagnostics.isEmpty(), "Unexpected diagnostics: ${result.diagnostics}")
        assertEquals(listOf("Generated"), result.definitions.map { it.name.asString() })
        assertEquals(MacroArtifactPackage.Origin.EXTERNAL_PATH, result.locatedArtifacts.single().origin)
    }

    /**
     * 验证无效现有 artifact 只报告 resolver 诊断，不回退编译源码包。
     */
    @Test
    fun invalidExistingArtifactReportsResolverDiagnosticWithoutSourceCompilationFallback() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src"))
        val artifactRoot = Files.createDirectories(tempDir.resolve("artifacts"))
        val macroSource = sourceRoot.resolve("macros.cj")
        Files.writeString(
            macroSource,
            "macro package macros.pkg\npublic macro Generated(input: Tokens): Tokens { input }\n"
        )
        writeCompiledMacroArtifact(
            outputDir = artifactRoot,
            packageFqName = "macros.pkg",
            cjoPackageFqName = "ordinary.pkg",
            kind = PackageKind.Normal,
        )
        val fixture = macroDemandFixture(
            imports = listOf(macroImport("macros.pkg.Generated")),
            surface = macroSurface("Generated"),
            extraFiles = listOf(macroPackageFile("macros.pkg", macroSource.toFile())),
        )
        var compileCalls = 0
        val configuration = CompilerConfiguration().apply {
            addCangJieSourceRoot(sourceRoot.toString())
            addClasspathRoot(artifactRoot.toString())
            macroPackageCompilationOrchestrator = MacroPackageCompilationOrchestrator { _, _ ->
                compileCalls++
                error("Invalid existing artifact must be reported by resolver, not bypassed by fallback compilation")
            }
        }

        val result = prepareMacroArtifactDefinitionsForPreResults(configuration, listOf(fixture.pre))

        assertEquals(0, compileCalls)
        assertTrue(result.definitions.isEmpty())
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_UNDEFINED_PACKAGE, result.diagnostics.single().kind)
        assertEquals(FqName("macros.pkg"), result.diagnostics.single().artifactPackage)
    }

    /**
     * 验证 SDK 标准库宏包可直接从 SDK 定位解析。
     */
    @Test
    fun sdkStdMacroPackageIsResolvedFromSdkWithoutSameProjectCompilation() {
        val sdkHome = Files.createDirectories(tempDir.resolve("sdk"))
        val modulesDir =
            Files.createDirectories(sdkHome.resolve("modules").resolve("windows_x86_64_cjnative").resolve("std"))
        val runtimeDir =
            Files.createDirectories(sdkHome.resolve("runtime").resolve("lib").resolve("windows_x86_64_cjnative"))
        CjoPackageWriter.write(
            modulesDir.resolve("std.core.cjo"),
            CjoPackageMetadata(
                fullPackageName = "std.core",
                moduleName = "std",
                kind = PackageKind.Macro,
                declarations = listOf(CjoPackageDeclaration("Generated")),
            ),
        )
        Files.write(runtimeDir.resolve("libcangjie-std-core.${dynamicLibraryExtension()}"), byteArrayOf(1, 2, 3))
        val fixture = macroDemandFixture(
            imports = emptyList(),
            surface = macroSurface("std.core.Generated"),
        )
        var compileCalls = 0
        val configuration = CompilerConfiguration().apply {
            macroSdkHome = sdkHome.toString()
            macroPackageCompilationOrchestrator = MacroPackageCompilationOrchestrator { _, _ ->
                compileCalls++
                error("SDK std macro artifact should be resolved from SDK modules/runtime paths")
            }
        }

        val result = prepareMacroArtifactDefinitionsForPreResults(configuration, listOf(fixture.pre))

        assertEquals(0, compileCalls)
        assertTrue(result.diagnostics.isEmpty(), "Unexpected diagnostics: ${result.diagnostics}")
        assertEquals(listOf("Generated"), result.definitions.map { it.name.asString() })
        assertEquals(MacroArtifactPackage.Origin.SDK_STDLIB, result.locatedArtifacts.single().origin)
        assertTrue(requireNotNull(result.definitions.single().libPath).endsWith("libcangjie-std-core.${dynamicLibraryExtension()}"))
    }

    /**
     * 验证默认 cjc resolver 优先使用配置的宏 SDK 根。
     */
    @Test
    fun defaultCjcResolverUsesConfiguredMacroSdkHome() {
        val sdkHome = Files.createDirectories(tempDir.resolve("configured-sdk"))
        val cjc = writeCjcExecutable(sdkHome)

        assertEquals(cjc, DefaultMacroCompilerExecutableResolver.resolve(sdkHome.toString()))
    }

    /**
     * 验证配置 SDK 缺失时 resolver 使用 CANGJIE_HOME。
     */
    @Test
    fun defaultCjcResolverUsesCangjieHomeWhenConfiguredSdkIsMissing() {
        val cangjieHome = Files.createDirectories(tempDir.resolve("cangjie-home"))
        val cjc = writeCjcExecutable(cangjieHome)

        val resolved = DefaultMacroCompilerExecutableResolver.resolve(
            sdkHome = tempDir.resolve("missing-sdk").toString(),
            cangjieHome = cangjieHome.toString(),
            cjcHome = null,
            pathValue = null,
            userHome = tempDir.resolve("home"),
        )

        assertEquals(cjc, resolved)
    }

    /**
     * 验证 CANGJIE_HOME 缺失时 resolver 使用 cjc.home 系统属性。
     */
    @Test
    fun defaultCjcResolverUsesSystemPropertyHomeAfterCangjieHome() {
        val cjcHome = Files.createDirectories(tempDir.resolve("cjc-home"))
        val cjc = writeCjcExecutable(cjcHome)

        val resolved = DefaultMacroCompilerExecutableResolver.resolve(
            sdkHome = tempDir.resolve("missing-sdk").toString(),
            cangjieHome = tempDir.resolve("missing-cangjie-home").toString(),
            cjcHome = cjcHome.toString(),
            pathValue = null,
            userHome = tempDir.resolve("home"),
        )

        assertEquals(cjc, resolved)
    }

    /**
     * 验证显式 home 均缺失时 resolver 可从 PATH 查找 cjc。
     */
    @Test
    fun defaultCjcResolverUsesPathAfterExplicitHomes() {
        val pathDirectory = Files.createDirectories(tempDir.resolve("path-bin"))
        val cjc = writeCjcExecutable(pathDirectory, alreadyBinDirectory = true)

        val resolved = DefaultMacroCompilerExecutableResolver.resolve(
            sdkHome = tempDir.resolve("missing-sdk").toString(),
            cangjieHome = tempDir.resolve("missing-cangjie-home").toString(),
            cjcHome = tempDir.resolve("missing-cjc-home").toString(),
            pathValue = pathDirectory.toString(),
            userHome = tempDir.resolve("home"),
        )

        assertEquals(cjc, resolved)
    }

    /**
     * 验证 resolver 最后会选择用户目录中最新版本 SDK。
     */
    @Test
    fun defaultCjcResolverUsesNewestUserHomeSdkFallback() {
        val userHome = Files.createDirectories(tempDir.resolve("home"))
        val olderSdk = Files.createDirectories(userHome.resolve("sdk").resolve("cangjie-sdk-1.0.4").resolve("cangjie"))
        val newerSdk = Files.createDirectories(userHome.resolve("sdk").resolve("cangjie-sdk-1.0.5").resolve("cangjie"))
        writeCjcExecutable(olderSdk)
        val cjc = writeCjcExecutable(newerSdk)

        val resolved = DefaultMacroCompilerExecutableResolver.resolve(
            sdkHome = tempDir.resolve("missing-sdk").toString(),
            cangjieHome = null,
            cjcHome = null,
            pathValue = null,
            userHome = userHome,
        )

        assertEquals(cjc, resolved)
    }

    /**
     * 验证所有候选位置缺失时 resolver 报告找不到可执行文件。
     */
    @Test
    fun defaultCjcResolverReportsMissingExecutableWhenNoSourceMatches() {
        val error = kotlin.runCatching {
            DefaultMacroCompilerExecutableResolver.resolve(
                sdkHome = tempDir.resolve("missing-sdk").toString(),
                cangjieHome = tempDir.resolve("missing-cangjie-home").toString(),
                cjcHome = tempDir.resolve("missing-cjc-home").toString(),
                pathValue = tempDir.resolve("missing-path").toString(),
                userHome = tempDir.resolve("home"),
            )
        }.exceptionOrNull()

        assertTrue(requireNotNull(error).message.orEmpty().contains("Cannot find"))
    }

    /**
     * 验证外部 cjc orchestrator 可从编译输出生成 artifact 包。
     */
    @Test
    fun externalCjcOrchestratorProducesArtifactPackageFromCompiledOutputs() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src").resolve("macros"))
        val outputDir = Files.createDirectories(tempDir.resolve("out"))
        val cjoPath = outputDir.resolve("macros.pkg.cjo")
        CjoPackageWriter.write(
            cjoPath,
            CjoPackageMetadata(
                fullPackageName = "macros.pkg",
                moduleName = "macros",
                kind = PackageKind.Macro,
                declarations = listOf(CjoPackageDeclaration("Generated")),
            ),
        )
        val dylib = outputDir.resolve("lib-macro_macros.pkg.dll")
        Files.write(dylib, byteArrayOf(1, 2, 3))
        val depRoot = Files.createDirectories(tempDir.resolve("deps"))
        val depBchir = depRoot.resolve("std.core.bchir")
        Files.write(depBchir, byteArrayOf(9, 9))

        val orchestrator = ExternalCjcMacroPackageCompilationOrchestrator(
            executableResolver = MacroCompilerExecutableResolver { Path.of("C:/sdk/bin/cjc.exe") },
            commandRunner = MacroCompilerCommandRunner {
                MacroCompilerCommandResult(exitCode = 0, stdout = "ok", stderr = "")
            },
        )

        val result = orchestrator.compileMacroPackages(
            requests = listOf(
                MacroSourcePackageCompilationRequest(
                    packageFqName = FqName("macros.pkg"),
                    sourceRoots = listOf(sourceRoot.toString()),
                    importPaths = listOf(depRoot.toString()),
                    outputDirectory = outputDir.toString(),
                    compileInvocationId = "compile-macros.pkg",
                ),
            ),
            context = MacroPackageCompilationContext(
                configuration = CompilerConfiguration(),
                executorAbiVersion = "executor-abi-v1",
                cacheContext = MacroCompilationCacheContext(),
            ),
        )

        assertTrue(result.diagnostics.isEmpty(), "Unexpected diagnostics: ${result.diagnostics}")
        val artifact = result.artifactPackages.single()
        assertEquals(FqName("macros.pkg"), artifact.packageFqName)
        assertEquals(cjoPath.toString(), artifact.cjoPath)
        assertEquals(dylib.toString(), artifact.dynamicLibPath)
        assertEquals(listOf(depBchir.toString()), artifact.dependenciesBchirPaths)
        assertEquals("executor-abi-v1", artifact.abiVersion)
        assertEquals(MacroArtifactPackage.Origin.ORCHESTRATION, artifact.origin)
        assertEquals("compile-macros.pkg", artifact.compileInvocationId)
    }

    /**
     * 验证 orchestrator 会通过 CANGJIE_LIBRARY 环境变量传递 classpath。
     */
    @Test
    fun externalCjcOrchestratorPassesClasspathViaCangjieLibraryEnvironment() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src").resolve("macros"))
        val outputDir = Files.createDirectories(tempDir.resolve("out"))
        val classpathA = Files.createDirectories(tempDir.resolve("classpath-a"))
        val classpathB = Files.createDirectories(tempDir.resolve("classpath-b"))
        writeCompiledMacroArtifact(outputDir, "macros.pkg")

        var capturedCommand: MacroCompilerCommand? = null
        val orchestrator = ExternalCjcMacroPackageCompilationOrchestrator(
            executableResolver = MacroCompilerExecutableResolver { Path.of("C:/sdk/bin/cjc.exe") },
            commandRunner = MacroCompilerCommandRunner { command ->
                capturedCommand = command
                MacroCompilerCommandResult(exitCode = 0, stdout = "ok", stderr = "")
            },
        )

        val result = orchestrator.compileMacroPackages(
            requests = listOf(
                MacroSourcePackageCompilationRequest(
                    packageFqName = FqName("macros.pkg"),
                    sourceRoots = listOf(sourceRoot.toString()),
                    classpath = listOf(classpathA.toString(), classpathB.toString(), classpathA.toString()),
                    outputDirectory = outputDir.toString(),
                ),
            ),
            context = MacroPackageCompilationContext(
                configuration = CompilerConfiguration(),
                executorAbiVersion = "executor-abi-v1",
                cacheContext = MacroCompilationCacheContext(),
            ),
        )

        assertTrue(result.diagnostics.isEmpty(), "Unexpected diagnostics: ${result.diagnostics}")
        val command = requireNotNull(capturedCommand)
        assertEquals(
            listOf(classpathA.toString(), classpathB.toString()).joinToString(File.pathSeparator),
            command.environment["CANGJIE_LIBRARY"],
        )
    }

    /**
     * 验证外部 cjc 非零退出会生成编译失败诊断并持久化输出。
     */
    @Test
    fun externalCjcOrchestratorReturnsCompileFailedDiagnosticWithPersistedOutputReference() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src").resolve("broken"))
        val outputDir = Files.createDirectories(tempDir.resolve("broken-out"))
        val orchestrator = ExternalCjcMacroPackageCompilationOrchestrator(
            executableResolver = MacroCompilerExecutableResolver { Path.of("C:/sdk/bin/cjc.exe") },
            commandRunner = MacroCompilerCommandRunner {
                MacroCompilerCommandResult(exitCode = 1, stdout = "", stderr = "macro compilation failed")
            },
        )

        val result = orchestrator.compileMacroPackages(
            requests = listOf(
                MacroSourcePackageCompilationRequest(
                    packageFqName = FqName("broken.pkg"),
                    sourceRoots = listOf(sourceRoot.toString()),
                    outputDirectory = outputDir.toString(),
                    compileInvocationId = "compile-broken.pkg",
                ),
            ),
            context = MacroPackageCompilationContext(
                configuration = CompilerConfiguration(),
                executorAbiVersion = "executor-abi-v1",
                cacheContext = MacroCompilationCacheContext(),
            ),
        )

        assertTrue(result.artifactPackages.isEmpty())
        val diagnostic = result.diagnostics.single()
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED, diagnostic.kind)
        assertEquals("compile-broken.pkg", diagnostic.compileInvocationId)
        assertTrue(diagnostic.message.contains("exit code 1"))
        val diagnosticsRef = requireNotNull(diagnostic.sourceDiagnosticsRef)
        assertTrue(Files.exists(Path.of(diagnosticsRef)))
        val storedOutput = Files.readString(Path.of(diagnosticsRef))
        assertTrue(storedOutput.contains("macro compilation failed"))
    }

    /**
     * 验证空 source root 和多 source root 在调用命令前被拒绝。
     */
    @Test
    fun externalCjcOrchestratorRejectsEmptyAndMultipleSourceRootsBeforeCommandInvocation() {
        var commandCalls = 0
        val orchestrator = ExternalCjcMacroPackageCompilationOrchestrator(
            executableResolver = MacroCompilerExecutableResolver { Path.of("C:/sdk/bin/cjc.exe") },
            commandRunner = MacroCompilerCommandRunner {
                commandCalls++
                error("Invalid source roots must be reported before cjc command invocation")
            },
        )

        val result = orchestrator.compileMacroPackages(
            requests = listOf(
                MacroSourcePackageCompilationRequest(
                    packageFqName = FqName("empty.pkg"),
                    sourceRoots = emptyList(),
                    compileInvocationId = "compile-empty.pkg",
                ),
                MacroSourcePackageCompilationRequest(
                    packageFqName = FqName("multi.pkg"),
                    sourceRoots = listOf("src/one", "src/two"),
                    compileInvocationId = "compile-multi.pkg",
                ),
            ),
            context = MacroPackageCompilationContext(
                configuration = CompilerConfiguration(),
                executorAbiVersion = "executor-abi-v1",
                cacheContext = MacroCompilationCacheContext(),
            ),
        )

        assertEquals(0, commandCalls)
        assertTrue(result.artifactPackages.isEmpty())
        assertTrue(result.artifactSearchPaths.isEmpty())
        assertEquals(2, result.diagnostics.size)
        assertEquals(
            listOf("compile-empty.pkg", "compile-multi.pkg"),
            result.diagnostics.map { it.compileInvocationId })
        assertTrue(result.diagnostics[0].message.contains("has no source roots"))
        assertTrue(result.diagnostics[1].message.contains("exactly one package source root"))
    }

    /**
     * 验证 command runner 抛出的异常会被记录为诊断并持久化堆栈。
     */
    @Test
    fun externalCjcOrchestratorPersistsCommandRunnerExceptionDiagnostic() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src").resolve("throwing"))
        val outputDir = Files.createDirectories(tempDir.resolve("throwing-out"))
        val orchestrator = ExternalCjcMacroPackageCompilationOrchestrator(
            executableResolver = MacroCompilerExecutableResolver { Path.of("C:/sdk/bin/cjc.exe") },
            commandRunner = MacroCompilerCommandRunner {
                throw IllegalStateException("runner exploded")
            },
        )

        val result = orchestrator.compileMacroPackages(
            requests = listOf(
                MacroSourcePackageCompilationRequest(
                    packageFqName = FqName("throwing.pkg"),
                    sourceRoots = listOf(sourceRoot.toString()),
                    outputDirectory = outputDir.toString(),
                    compileInvocationId = "compile-throwing.pkg",
                ),
            ),
            context = MacroPackageCompilationContext(
                configuration = CompilerConfiguration(),
                executorAbiVersion = "executor-abi-v1",
                cacheContext = MacroCompilationCacheContext(),
            ),
        )

        assertTrue(result.artifactPackages.isEmpty())
        val diagnostic = result.diagnostics.single()
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED, diagnostic.kind)
        assertEquals("compile-throwing.pkg", diagnostic.compileInvocationId)
        assertTrue(diagnostic.message.contains("invocation failed before completion"))
        assertTrue(diagnostic.message.contains("runner exploded"))
        val diagnosticsRef = requireNotNull(diagnostic.sourceDiagnosticsRef)
        assertTrue(Files.exists(Path.of(diagnosticsRef)))
        val storedOutput = Files.readString(Path.of(diagnosticsRef))
        assertTrue(storedOutput.contains("runner exploded"))
    }

    /**
     * 验证命令成功但缺少 artifact 文件时报告编译失败诊断。
     */
    @Test
    fun externalCjcOrchestratorReportsSuccessfulInvocationWithMissingArtifacts() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src").resolve("missing-artifact"))
        val outputDir = Files.createDirectories(tempDir.resolve("missing-artifact-out"))
        val orchestrator = ExternalCjcMacroPackageCompilationOrchestrator(
            executableResolver = MacroCompilerExecutableResolver { Path.of("C:/sdk/bin/cjc.exe") },
            commandRunner = MacroCompilerCommandRunner {
                MacroCompilerCommandResult(exitCode = 0, stdout = "compiled", stderr = "")
            },
        )

        val result = orchestrator.compileMacroPackages(
            requests = listOf(
                MacroSourcePackageCompilationRequest(
                    packageFqName = FqName("missing.pkg"),
                    sourceRoots = listOf(sourceRoot.toString()),
                    outputDirectory = outputDir.toString(),
                    compileInvocationId = "compile-missing.pkg",
                ),
            ),
            context = MacroPackageCompilationContext(
                configuration = CompilerConfiguration(),
                executorAbiVersion = "executor-abi-v1",
                cacheContext = MacroCompilationCacheContext(),
            ),
        )

        assertTrue(result.artifactPackages.isEmpty())
        assertTrue(result.artifactSearchPaths.isEmpty())
        val diagnostic = result.diagnostics.single()
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED, diagnostic.kind)
        assertEquals("compile-missing.pkg", diagnostic.compileInvocationId)
        assertTrue(diagnostic.message.contains("compilation succeeded but required artifact files were not found"))
        val diagnosticsRef = requireNotNull(diagnostic.sourceDiagnosticsRef)
        assertTrue(Files.exists(Path.of(diagnosticsRef)))
        assertTrue(Files.readString(Path.of(diagnosticsRef)).contains("compiled"))
    }

    /**
     * 宏包 demand 测试夹具。
     */
    private data class MacroDemandFixture(
        /**
         * 预宏 raw 构建结果。
         */
        val pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
    )

    /**
     * 构造带 import 和宏 surface 的 demand fixture。
     */
    private fun macroDemandFixture(
        imports: List<CfirImport>,
        surface: MacroSurface,
        extraFiles: List<CfirFile> = emptyList(),
    ): MacroDemandFixture {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = CfirSourceModuleData(
            name = Name.identifier("test"),
            dependencies = emptyList(),
            refinementDependencies = emptyList(),
            targetPlatform = CangJiePlatforms.defaultCangJiePlatform,
            platform = CfirPlatform.DEFAULT,
        ).also {
            it.bindSession(session)
            session.register(org.cangnova.cangjie.cfir.common.CfirModuleData::class, it)
        }
        val file = buildFile {
            source = null
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "sample.cj"
            sourceFile = null
            packageDirective = buildPackageDirective {
                source = null
                packageFqName = FqName("app")
                isMacroPackage = false
            }
            this.imports += imports
            sourceFileLinesMapping = null
        }
        return MacroDemandFixture(
            pre = buildPreMacroRawFiles(
                session,
                listOf(file) + extraFiles.onEach { it.moduleData.bindSession(session) },
                listOf(listOf(surface)) + List(extraFiles.size) { emptyList() },
            ),
        )
    }

    /**
     * 构造同项目宏包源文件对应的 CFIR 文件。
     */
    private fun macroPackageFile(packageFqName: String, file: File): CfirFile = buildFile {
        source = null
        moduleData = CfirSourceModuleData(
            name = Name.identifier("test"),
            dependencies = emptyList(),
            refinementDependencies = emptyList(),
            targetPlatform = CangJiePlatforms.defaultCangJiePlatform,
            platform = CfirPlatform.DEFAULT,
        )
        resolvePhase = CfirResolvePhase.RAW_CFIR
        origin = CfirDeclarationOrigin.Library
        attributes = CfirDeclarationAttributes.EMPTY
        symbol = CfirFileSymbol()
        name = file.name
        sourceFile = CjIoFileSourceFile(file)
        packageDirective = buildPackageDirective {
            source = null
            this.packageFqName = FqName(packageFqName)
            isMacroPackage = true
        }
        sourceFileLinesMapping = null
    }

    /**
     * 构造测试用宏 import。
     */
    private fun macroImport(fqName: String, isAllUnder: Boolean = false, alias: String? = null): CfirImport =
        buildImport {
            source = null
            importedFqName = FqName(fqName)
            this.isAllUnder = isAllUnder
            aliasName = alias?.let(Name::identifier)
            aliasSource = null
        }

    /**
     * 构造测试用表达式宏 surface。
     */
    private fun macroSurface(qualifiedName: String): MacroSurfaceExpr = MacroSurfaceExpr(
        surfaceId = 1,
        qualifiedName = FqName(qualifiedName),
        kind = MacroSurface.Kind.PLAIN,
        hasParenthesis = true,
        attrTokens = emptyList(),
        inputTokens = listOf(MacroSurfaceToken("arg", 0, 3)),
        sourceRange = null,
        scopeContext = MacroSurfaceScopeContext(FqName("app"), null, null),
        modifiers = emptyList(),
        carriedAnnotations = emptyList(),
        capturedRawSyntax = "@$qualifiedName(arg)",
        containerContext = MacroSurfaceContainerContext(
            outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.FUNCTION_BODY,
            isInsidePrimaryConstructor = false,
            isInsideEnumBody = false,
            isInsideBlock = true,
        ),
        replaceHandle = CfirReplaceHandle(1),
    )

    /**
     * 写入一组可被 locator/resolver 识别的已编译宏 artifact 文件。
     */
    private fun writeCompiledMacroArtifact(
        outputDir: Path,
        packageFqName: String,
        cjoPackageFqName: String = packageFqName,
        kind: UByte = PackageKind.Macro,
        declarations: List<String> = listOf("Generated"),
    ) {
        CjoPackageWriter.write(
            outputDir.resolve("${packageFqName}.cjo"),
            CjoPackageMetadata(
                fullPackageName = cjoPackageFqName,
                moduleName = "macros",
                kind = kind,
                declarations = declarations.map(::CjoPackageDeclaration),
            ),
        )
        Files.write(
            outputDir.resolve("lib-macro_${toCjoFileName(FqName(packageFqName))}.dll"),
            byteArrayOf(1, 2, 3),
        )
    }

    /**
     * 写入测试用 cjc 可执行文件占位。
     */
    private fun writeCjcExecutable(root: Path, alreadyBinDirectory: Boolean = false): Path {
        val executableName =
            if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) "cjc.exe" else "cjc"
        val binDirectory = if (alreadyBinDirectory) root else root.resolve("bin")
        val cjc = binDirectory.resolve(executableName)
        Files.createDirectories(cjc.parent)
        Files.write(cjc, byteArrayOf(1, 2, 3))
        return cjc
    }

    /**
     * 从 pre-macro 结果调用 artifact 准备入口。
     */
    private fun prepareMacroArtifactDefinitionsForPreResults(
        configuration: CompilerConfiguration,
        preResults: List<PreMacroRawBuildResult>,
    ): MacroExpansionArtifactPreparationResult =
        prepareMacroArtifactDefinitionsForExpansion(
            configuration,
            preResults.map(MacroDemandClassification::create),
        )
}
