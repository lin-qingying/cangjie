package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic

import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFormatVersionPolicy
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.registerCliCompilerAndCommonComponents
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

/** 确认 CJO 加载事实经 LL 符号 provider 与默认 checker 管线进入 IDE 诊断。 */
class CfirCjmpLoadDiagnosticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics/cjmpLoad",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
    override val additionalDirectives = super.additionalDirectives + CfirDiagnosticsDirectives

    @Test
    fun rejectedBinaryPackageIsReportedAtImport(mainFile: CjFile) {
        val libraryRoot = Files.createTempDirectory("cjmp-load-diagnostic-ll")
        try {
            val requestedPackage = FqName("loadlib")
            val cjoPath = libraryRoot.resolve(CjoConstants.packageNameToPath(requestedPackage.asString()))
            Files.createDirectories(cjoPath.parent)
            Files.write(
                cjoPath,
                CjoPackageWriter.toByteArray(
                    CjoPackageMetadata(
                        fullPackageName = "different.package",
                        moduleName = "invalid-library",
                    ),
                ),
            )

            val sourceFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
            val sourceSession = sourceFile.moduleData.session as LLCfirSession
            val previousProvider = sourceSession.symbolProvider as LLModuleWithDependenciesSymbolProvider

            val librarySession = object : CfirSession(CfirSession.Kind.Library) {}.also {
                it.registerCliCompilerAndCommonComponents(LanguageVersionSettingsImpl.DEFAULT)
            }
            val libraryModule = CfirBinaryDependenciesModuleData(Name.special("<cjmp-load-diagnostic-library>")).apply {
                bindSession(librarySession)
            }
            val libraryProvider = CfirDeserializedSymbolProvider(
                session = librarySession,
                cjoManager = CjoManager(
                    CjoSearchPath { moduleName ->
                        if (moduleName == "CANGJIE_LIBRARY") libraryRoot.toString() else null
                    },
                    CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT),
                ),
                cangjieScopeProvider = CfirCangJieScopeProvider(),
                libraryModuleData = libraryModule,
            )

            // 通过生产反序列化 provider 触发真实包名加载门，再由 LL 模块 provider 暴露该事实。
            libraryProvider.getClassLikeSymbolsByClassId(
                ClassId(requestedPackage, Name.identifier("Unavailable")),
            )
            assertTrue(
                libraryProvider.getCjmpLoadDiagnostics(requestedPackage).isNotEmpty(),
                "the rejected CJO must retain its structured load fact",
            )
            sourceSession.register(
                CfirSymbolProvider::class,
                LLModuleWithDependenciesSymbolProvider(
                    session = sourceSession,
                    providers = previousProvider.providers + libraryProvider,
                    dependencyProvider = previousProvider.dependencyProvider,
                ),
            )

            val diagnostics = mainFile.collectDiagnosticsForFile(
                mainFile.getResolutionFacadeForTest(),
                DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS,
            )
            val loadDiagnostic = diagnostics.single {
                it.factoryName == "CFIR_CJMP_COMMON_CJO_PACKAGE_MISMATCH"
            }

            assertTrue(loadDiagnostic.severity.isError)
            assertEquals("loadlib", loadDiagnostic.psiElement.text, "diagnostic points at the failed import package")
            assertFalse(
                diagnostics.any { it.factoryName == "CFIR_UNRESOLVED_IMPORT" },
                "a known CJO rejection must not degrade into an unresolved-import symptom",
            )
        } finally {
            libraryRoot.toFile().deleteRecursively()
        }
    }
}
