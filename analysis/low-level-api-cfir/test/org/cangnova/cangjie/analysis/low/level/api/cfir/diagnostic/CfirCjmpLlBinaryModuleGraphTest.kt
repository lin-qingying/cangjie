@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
    org.cangnova.cangjie.analysis.api.CaImplementationDetail::class,
)

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.cfir.deserialization.cjmpModuleDataProvider
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirConfiguredAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiBaseTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiIdeModeTestServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CaLibraryModuleImpl
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CaSourceModuleImpl
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.supportsCjmpFeature
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjmpCommonPartLoadGate
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFormatVersion
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFunctionBodyInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFunctionInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFunctionTypeInfoMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoModuleOptionMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSchemaProfile
import org.cangnova.cangjie.cfir.serialization.cjo.CjoTypeMetadata
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticKind
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnostic
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.TargetPlatform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import PackageFormat.TypeKind

/** 通过真实 LL session/module graph 验证 common CJO 的发现与模块归属。 */
class CfirCjmpLlBinaryModuleGraphTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics/cjmpLoad",
) {
    override val configurator = CaCfirConfiguredAnalysisApiTestConfigurator(
        moduleKind = TestModuleKind.LibraryBinary,
        serviceRegistrars = listOf(
            CaAnalysisApiBaseTestServiceRegistrar,
            CaAnalysisApiDecompiledTestServiceRegistrar,
            CaCfirConfiguredAnalysisApiTestConfigurator.CaCfirAnalysisApiServiceRegistrar(),
            CaAnalysisApiIdeModeTestServiceRegistrar,
        ),
        analyseInDependentSession = false,
        analysisApiMode = AnalysisApiMode.Ide,
    )

    @Test
    fun specificSourceLoadsDependsOnCommonLibraryCjo(mainFile: CjFile, mainModule: CjTestModule) {
        val workingDirectory = Path.of("").toAbsolutePath().normalize()
        val libraryRoot = Files.createTempDirectory(workingDirectory, "cjmp-ll-binary-module-graph")
        val regularLibraryRoot = Files.createTempDirectory("cjmp-ll-regular-module-graph")
        try {
            val packageName = FqName("loadlib")
            val cjoPath = libraryRoot.resolve(CjoConstants.packageNameToPath(packageName.asString()))
            val explicitCommonCjoPath = workingDirectory.relativize(cjoPath)
            Files.createDirectories(cjoPath.parent)
            CjoPackageWriter.write(
                cjoPath,
                CjoPackageMetadata(
                    fullPackageName = packageName.asString(),
                    moduleName = "common-library",
                    schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
                    cjoVersion = CjoFormatVersion(
                        major = CjmpCommonPartLoadGate.supportedVersion.major,
                        minor = (CjmpCommonPartLoadGate.supportedVersion.minor.toInt() + 1).toUByte(),
                        patch = CjmpCommonPartLoadGate.supportedVersion.patch,
                    ),
                    options = CjoModuleOptionMetadata(debug = true, optLevel = 2u.toUByte()),
                    declarations = listOf(
                        CjoPackageDeclaration(
                            identifier = "commonValue",
                            type = 2u,
                            info = CjoFunctionInfo(
                                body = CjoFunctionBodyInfo(returnType = 1u),
                            ),
                        ),
                    ),
                    types = listOf(
                        CjoTypeMetadata(kind = TypeKind.Int64),
                        CjoTypeMetadata(
                            kind = TypeKind.Func,
                            semanticInfo = CjoFunctionTypeInfoMetadata(
                                returnType = 1u, isC = false, hasVariableLenArg = false,
                            ),
                        ),
                    ),
                ),
            )

            val regularLibraryVirtualFile = requireNotNull(
                StandardFileSystems.local().refreshAndFindFileByPath(
                    regularLibraryRoot.toAbsolutePath().normalize().toString().replace('\\', '/'),
                ),
            )
            val regularLibraryRootPsi = requireNotNull(
                PsiManager.getInstance(mainFile.project).findDirectory(regularLibraryVirtualFile),
            )
            val commonLibraryModule = CaLibraryModuleImpl(
                libraryName = "common-library",
                project = mainFile.project,
                binaryRoots = emptyList(),
                targetPlatform = TargetPlatform(
                    CangJiePlatforms.cjNative.componentPlatforms + CangJiePlatforms.cjvm.componentPlatforms,
                ),
                cjmpCommonPartCjoPaths = listOf(explicitCommonCjoPath),
            )
            val regularLibraryModule = CaLibraryModuleImpl(
                libraryName = "regular-library",
                project = mainFile.project,
                binaryRoots = listOf(regularLibraryRootPsi),
            )
            val specificSourceModule = mainModule.caModule as CaSourceModuleImpl
            specificSourceModule.directDependsOnDependencies += commonLibraryModule
            specificSourceModule.directRegularDependencies += regularLibraryModule

            val loadedCommonPart: LoadedCommonPart =
                ApplicationManager.getApplication().runReadAction<LoadedCommonPart> {
                    val cache = LLCfirSessionCache.getInstance(mainFile.project)
                    val specific = cache.getSession(specificSourceModule)
                    val common = cache.getSession(commonLibraryModule, preferBinary = true)
                    val regular = cache.getSession(regularLibraryModule, preferBinary = true)
                    val diagnostics = specific.symbolProvider.getCjmpLoadDiagnostics(packageName, specific)
                    val commonFunctions = common.symbolProvider
                        .getTopLevelFunctionSymbols(packageName, Name.identifier("commonValue"))
                    val function = specific.symbolProvider
                        .getTopLevelFunctionSymbols(packageName, Name.identifier("commonValue"))
                        .singleOrNull()
                        ?: error(
                            "specific module did not expose common CJO function; " +
                                    "direct common functions=${commonFunctions.size}, " +
                                    "common hasPackage=${common.symbolProvider.hasPackage(packageName)}, " +
                                    "common callable names=${common.symbolProvider.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageName)}, " +
                                    "common diagnostics=${common.symbolProvider.getCjmpLoadDiagnostics(packageName)}, " +
                                    "diagnostics=$diagnostics, mode=${specific.cjmpSettings.mode}, " +
                                    "dependsOn=${specific.moduleData.refinementDependencies}",
                        )
                    LoadedCommonPart(specific, common, regular, function.cfir, diagnostics)
                }

            assertEquals(
                listOf(commonLibraryModule),
                loadedCommonPart.specificSession.moduleData.refinementDependencies
                    .filterIsInstance<LLCfirModuleData>()
                    .map { it.caModule },
                "the production source session must retain the graph's direct depends-on module",
            )
            assertEquals(
                loadedCommonPart.commonSession.moduleData,
                requireNotNull(loadedCommonPart.specificSession.cjmpModuleDataProvider)
                    .getModuleData(cjoPath),
                "the source module's production path map must assign the common CJO to its depends-on module",
            )
            assertEquals(
                loadedCommonPart.regularSession.moduleData,
                requireNotNull(loadedCommonPart.specificSession.cjmpModuleDataProvider)
                    .getModuleData(regularLibraryRoot.resolve("ordinarylib/ordinarylib.cjo")),
                "an ordinary dependency CJO path must retain its regular library module ownership",
            )
            assertEquals(
                null,
                loadedCommonPart.specificSession.cjmpModuleDataProvider
                    ?.getModuleData(libraryRoot.resolve("unowned/unowned.cjo")),
                "unowned CJO paths must not fall through to source module data",
            )
            assertEquals(
                listOf(cjoPath.toAbsolutePath().normalize().toString()),
                loadedCommonPart.specificSession.cjmpSettings.commonPartCjoPaths,
                "the LL source session must retain the exact common CJO input path",
            )
            assertSame(
                commonLibraryModule,
                (loadedCommonPart.commonFunction.moduleData as LLCfirModuleData).caModule,
                "the declaration loaded from the common CJO must carry the depends-on library identity",
            )
            assertSame(
                loadedCommonPart.commonSession.moduleData,
                loadedCommonPart.commonFunction.moduleData,
                "the binary provider must deserialize the common CJO into its actual LL module data",
            )

            assertTrue(
                loadedCommonPart.loadDiagnostics.any { it.kind == CfirCjmpLoadDiagnosticKind.DEBUG_MISMATCH },
                "the real LL path must forward the specific caller's CJMP settings and the common CJO input path " +
                        "to its binary provider",
            )
            assertTrue(
                loadedCommonPart.loadDiagnostics.any { it.kind == CfirCjmpLoadDiagnosticKind.OPT_LEVEL_MISMATCH },
                "the common CJO's optimization level must be checked against the specific source module",
            )
            assertEquals(CfirCjmpMode.SPECIFIC, loadedCommonPart.specificSession.cjmpSettings.mode)

            val formatCheckingSpecificModule = CaSourceModuleImpl(
                name = "format-checking-specific",
                languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_3_0_ALPHA_05),
                project = mainFile.project,
                psiRoots = specificSourceModule.psiRoots,
            ).apply {
                directDependsOnDependencies += commonLibraryModule
                directRegularDependencies += regularLibraryModule
            }
            val formatCheckingDiagnostics = ApplicationManager.getApplication().runReadAction<List<CfirCjmpLoadDiagnostic>> {
                val session = LLCfirSessionCache.getInstance(mainFile.project).getSession(formatCheckingSpecificModule)
                assertEquals(CfirCjmpMode.SPECIFIC, session.cjmpSettings.mode)
                assertTrue(
                    session.languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.CjoFormatVersionCompatibility),
                )
                session.symbolProvider.getCjmpLoadDiagnostics(packageName, session)
            }
            assertTrue(
                formatCheckingDiagnostics.any { it.kind == CfirCjmpLoadDiagnosticKind.CJO_VERSION },
                "the format gate must use the consuming source session even when the shared library session uses older settings",
            )
        } finally {
            libraryRoot.toFile().deleteRecursively()
            regularLibraryRoot.toFile().deleteRecursively()
        }
    }

    private data class LoadedCommonPart(
        val specificSession: LLCfirSession,
        val commonSession: LLCfirSession,
        val regularSession: LLCfirSession,
        val commonFunction: CfirNamedFunction,
        val loadDiagnostics: List<CfirCjmpLoadDiagnostic>,
    )
}
