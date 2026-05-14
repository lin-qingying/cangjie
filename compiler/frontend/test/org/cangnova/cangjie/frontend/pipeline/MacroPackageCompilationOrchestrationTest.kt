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
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceContainerContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceScopeContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.CjIoFileSourceFile
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@OptIn(CompilerConfiguration.Internals::class)
class MacroPackageCompilationOrchestrationTest {
    @TempDir
    lateinit var tempDir: Path

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

    @Test
    fun configuredOrchestratorReceivesRequestsAndCacheContext() {
        val configuration = CompilerConfiguration()
        val cacheContext = MacroCompilationCacheContext(
            compilerOptionsFingerprint = "options",
            debugFlagsFingerprint = "debug",
            parallelFlagsFingerprint = "parallel",
            targetPlatform = "windows-x64",
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

    @Test
    fun cfirFrontendConfigurationInstallsDefaultMacroPackageCompilationOrchestrator() {
        val configuration = CompilerConfiguration.createForCfirFrontend()
        configuration.initializeCfirFrontendMacroCompilationConfiguration()
        assertTrue(
            configuration.macroPackageCompilationOrchestrator is ExternalCjcMacroPackageCompilationOrchestrator,
            "CFIR frontend configuration should install the external cjc macro compilation orchestrator by default.",
        )
    }

    @Test
    fun cfirFrontendInitializationPreservesCustomMacroPackageCompilationOrchestrator() {
        val custom = MacroPackageCompilationOrchestrator { _, _ -> MacroPackageCompilationResult() }
        val configuration = CompilerConfiguration().apply {
            macroPackageCompilationOrchestrator = custom
        }

        configuration.initializeCfirFrontendMacroCompilationConfiguration()

        assertSame(custom, configuration.macroPackageCompilationOrchestrator)
    }

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

    @Test
    fun expansionDemandDiscoversSameProjectMacroSourcePackageRoot() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src"))
        val macroSource = sourceRoot.resolve("macros.cj")
        Files.writeString(macroSource, "macro package macros.pkg\npublic macro Generated(input: Tokens): Tokens { input }\n")
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

        val result = prepareMacroArtifactDefinitionsForExpansion(configuration, listOf(fixture.pre))

        assertEquals(
            MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED,
            result.diagnostics.single().kind,
        )
    }

    @Test
    fun expansionDemandCompilesMissingSameProjectMacroPackageThenRelocatesAndResolvesArtifact() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src"))
        val macroSource = sourceRoot.resolve("macros.cj")
        Files.writeString(macroSource, "macro package macros.pkg\npublic macro Generated(input: Tokens): Tokens { input }\n")
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

        val result = prepareMacroArtifactDefinitionsForExpansion(configuration, listOf(fixture.pre))

        assertEquals(1, compileCalls)
        assertTrue(result.diagnostics.isEmpty(), "Unexpected diagnostics: ${result.diagnostics}")
        assertEquals(listOf("Generated"), result.definitions.map { it.name.asString() })
        assertEquals(MacroDefinitionEntry.Source.MACRO_ARTIFACT, result.definitions.single().source)
        assertEquals(outputDir.resolve("lib-macro_macros.pkg.${dynamicLibraryExtension()}").toString(), result.definitions.single().libPath)
        assertEquals(listOf(FqName("macros.pkg")), result.locatedArtifacts.map { it.packageFqName })
    }

    @Test
    fun expansionDemandAutoCompilationDisabledReportsMissingArtifactWithoutCallingOrchestrator() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src"))
        val macroSource = sourceRoot.resolve("macros.cj")
        Files.writeString(macroSource, "macro package macros.pkg\npublic macro Generated(input: Tokens): Tokens { input }\n")
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

        val result = prepareMacroArtifactDefinitionsForExpansion(configuration, listOf(fixture.pre))

        assertEquals(0, compileCalls)
        assertTrue(result.definitions.isEmpty())
        val diagnostic = result.diagnostics.single()
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED, diagnostic.kind)
        assertEquals(FqName("macros.pkg"), diagnostic.artifactPackage)
        assertTrue(diagnostic.message.contains("disabled"))
    }

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

        val result = prepareMacroArtifactDefinitionsForExpansion(configuration, listOf(fixture.pre))

        assertEquals(0, compileCalls)
        assertTrue(result.definitions.isEmpty())
        val diagnostic = result.diagnostics.single()
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED, diagnostic.kind)
        assertEquals(MacroConstructionDiagnostic.Origin.ORCHESTRATION, diagnostic.diagnosticOrigin)
        assertEquals(FqName("macros.pkg"), diagnostic.artifactPackage)
        assertEquals(1, diagnostic.originSurfaceId)
        assertTrue(diagnostic.message.contains("no same-project macro source root"))
    }

    @Test
    fun existingArtifactIsResolvedWithoutRepeatedSameProjectMacroCompilation() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src"))
        val artifactRoot = Files.createDirectories(tempDir.resolve("artifacts"))
        val macroSource = sourceRoot.resolve("macros.cj")
        Files.writeString(macroSource, "macro package macros.pkg\npublic macro Generated(input: Tokens): Tokens { input }\n")
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

        val result = prepareMacroArtifactDefinitionsForExpansion(configuration, listOf(fixture.pre))

        assertEquals(0, compileCalls)
        assertTrue(result.diagnostics.isEmpty(), "Unexpected diagnostics: ${result.diagnostics}")
        assertEquals(listOf("Generated"), result.definitions.map { it.name.asString() })
        assertEquals(MacroArtifactPackage.Origin.EXTERNAL_PATH, result.locatedArtifacts.single().origin)
    }

    @Test
    fun invalidExistingArtifactReportsResolverDiagnosticWithoutSourceCompilationFallback() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("src"))
        val artifactRoot = Files.createDirectories(tempDir.resolve("artifacts"))
        val macroSource = sourceRoot.resolve("macros.cj")
        Files.writeString(macroSource, "macro package macros.pkg\npublic macro Generated(input: Tokens): Tokens { input }\n")
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

        val result = prepareMacroArtifactDefinitionsForExpansion(configuration, listOf(fixture.pre))

        assertEquals(0, compileCalls)
        assertTrue(result.definitions.isEmpty())
        assertEquals(MacroConstructionDiagnostic.Kind.MACRO_UNDEFINED_PACKAGE, result.diagnostics.single().kind)
        assertEquals(FqName("macros.pkg"), result.diagnostics.single().artifactPackage)
    }

    @Test
    fun sdkStdMacroPackageIsResolvedFromSdkWithoutSameProjectCompilation() {
        val sdkHome = Files.createDirectories(tempDir.resolve("sdk"))
        val modulesDir = Files.createDirectories(sdkHome.resolve("modules").resolve("windows_x86_64_cjnative").resolve("std"))
        val runtimeDir = Files.createDirectories(sdkHome.resolve("runtime").resolve("lib").resolve("windows_x86_64_cjnative"))
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

        val result = prepareMacroArtifactDefinitionsForExpansion(configuration, listOf(fixture.pre))

        assertEquals(0, compileCalls)
        assertTrue(result.diagnostics.isEmpty(), "Unexpected diagnostics: ${result.diagnostics}")
        assertEquals(listOf("Generated"), result.definitions.map { it.name.asString() })
        assertEquals(MacroArtifactPackage.Origin.SDK_STDLIB, result.locatedArtifacts.single().origin)
        assertTrue(requireNotNull(result.definitions.single().libPath).endsWith("libcangjie-std-core.${dynamicLibraryExtension()}"))
    }

    @Test
    fun defaultCjcResolverUsesConfiguredMacroSdkHome() {
        val sdkHome = Files.createDirectories(tempDir.resolve("configured-sdk"))
        val cjc = writeCjcExecutable(sdkHome)

        assertEquals(cjc, DefaultMacroCompilerExecutableResolver.resolve(sdkHome.toString()))
    }

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
        assertEquals(listOf("compile-empty.pkg", "compile-multi.pkg"), result.diagnostics.map { it.compileInvocationId })
        assertTrue(result.diagnostics[0].message.contains("has no source roots"))
        assertTrue(result.diagnostics[1].message.contains("exactly one package source root"))
    }

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

    private data class MacroDemandFixture(
        val pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
    )

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

    private fun macroPackageFile(packageFqName: String, file: File): CfirFile = buildFile {
        source = null
        moduleData = CfirSourceModuleData(
            name = Name.identifier("test"),
            dependencies = emptyList(),
            refinementDependencies = emptyList(),
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

    private fun macroImport(fqName: String, isAllUnder: Boolean = false, alias: String? = null): CfirImport = buildImport {
        source = null
        importedFqName = FqName(fqName)
        this.isAllUnder = isAllUnder
        aliasName = alias?.let(Name::identifier)
        aliasSource = null
    }

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

    private fun writeCjcExecutable(root: Path, alreadyBinDirectory: Boolean = false): Path {
        val executableName = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) "cjc.exe" else "cjc"
        val binDirectory = if (alreadyBinDirectory) root else root.resolve("bin")
        val cjc = binDirectory.resolve(executableName)
        Files.createDirectories(cjc.parent)
        Files.write(cjc, byteArrayOf(1, 2, 3))
        return cjc
    }
}
