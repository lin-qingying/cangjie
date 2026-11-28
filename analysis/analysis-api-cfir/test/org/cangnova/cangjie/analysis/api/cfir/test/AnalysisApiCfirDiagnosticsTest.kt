package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.diagnostics.CaSeverity
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFormatVersionPolicy
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
import org.cangnova.cangjie.psi.CjNamedFunction
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * CFIR Analysis API diagnostics facade 的回归测试。
 *
 * 这里覆盖文件级与声明级 diagnostics 查询，确保 checker filter、extend 声明和接口类型参数
 * 在 Analysis API 懒解析链路中保持一致的诊断边界。
 */
class AnalysisApiCfirDiagnosticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/diagnostics",
) {
    /**
     * 使用 standalone CFIR 配置执行 diagnostics facade 测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证 common、extended、experimental 等 checker filter 对同一文件的诊断集合划分。
     */
    @Test
    fun collectDiagnostics(mainFile: CjFile) {
        val commonDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        }
        val allDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
        val extraDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS)
        }
        val experimentalDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS)
        }

        assertTrue(commonDiagnostics.any { it.factoryName == "UNRESOLVED_IMPORT" })
        assertEquals(commonDiagnostics.map { it.factoryName }, allDiagnostics.map { it.factoryName })
        assertTrue(extraDiagnostics.isEmpty())
        assertTrue(experimentalDiagnostics.isEmpty())
    }

    /** Analysis API 的文件级 diagnostics 必须转换并返回 provider 产生的 CJMP 加载诊断。 */
    @Test
    fun collectCjmpLoadDiagnostics(mainFile: CjFile) {
        val libraryRoot = Files.createTempDirectory("cjmp-load-diagnostic-analysis-api")
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

            val diagnostics = analyzeForTest(mainFile) {
                val analysisSession = this as CaCfirSession
                val cfirFile = mainFile.getOrBuildCfirFile(analysisSession.resolutionFacade)
                val sourceSession = cfirFile.moduleData.session as LLCfirSession
                val previousProvider = sourceSession.symbolProvider

                val librarySession = object : CfirSession(CfirSession.Kind.Library) {}.also {
                    it.registerCliCompilerAndCommonComponents(LanguageVersionSettingsImpl.DEFAULT)
                }
                val libraryModule = CfirBinaryDependenciesModuleData(Name.special("<cjmp-load-diagnostic-library>"))
                    .apply { bindSession(librarySession) }
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

                libraryProvider.getClassLikeSymbolsByClassId(
                    ClassId(requestedPackage, Name.identifier("Unavailable")),
                )
                sourceSession.register(
                    CfirSymbolProvider::class,
                    CfirCompositeSymbolProvider(sourceSession, listOf(previousProvider, libraryProvider)),
                )

                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
            }

            val loadDiagnostic = diagnostics.singleOrNull {
                it.factoryName == "CJMP_COMMON_CJO_PACKAGE_MISMATCH"
            } ?: error("Expected CJMP load diagnostic, got ${diagnostics.map { it.factoryName }}")
            assertTrue(loadDiagnostic is CaCfirDiagnostic.CjmpCommonCjoPackageMismatch)
            val typedLoadDiagnostic = loadDiagnostic as CaCfirDiagnostic.CjmpCommonCjoPackageMismatch
            assertEquals(CaSeverity.ERROR, loadDiagnostic.severity)
            assertEquals("loadlib", loadDiagnostic.psi.text, "diagnostic points at the failed import package")
            assertEquals("loadlib", typedLoadDiagnostic.expectedPackage)
            assertEquals("different.package", typedLoadDiagnostic.actualPackage)
            assertFalse(
                diagnostics.any { it.factoryName == "UNRESOLVED_IMPORT" },
                "a known CJO rejection must not degrade into an unresolved-import symptom",
            )
        } finally {
            libraryRoot.toFile().deleteRecursively()
        }
    }

    /**
     * 验证合法 extend 文件在扩展与公共 checker 合并模式下不会产生额外诊断。
     */
    @Test
    fun extendDiagnostics(mainFile: CjFile) {
        val allDiagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        assertTrue(
            allDiagnostics.isEmpty(),
            "合法 extend 文件收集 diagnostics 不应抛异常，也不应产生额外诊断: " +
                allDiagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.textRange}" },
        )
    }

    /**
     * 验证命名函数声明级 diagnostics 查询不会破坏后续文件级 diagnostics 收集。
     */
    @Test
    fun namedFunctionDiagnostics(mainFile: CjFile) {
        val function = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java).single()
        analyzeForTest(mainFile) {
            function.diagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
    }

    /**
     * 验证接口成员签名可以解析外层类型参数，不会在懒解析 diagnostics 中误报未解析引用。
     */
    @Test
    fun interfaceTypeParameterDiagnostics(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        assertFalse(
            diagnostics.any { diagnostic ->
                diagnostic.factoryName == "UNRESOLVED_REFERENCE" && diagnostic.textRanges.any { range ->
                    mainFile.text.substring(range.startOffset, range.endOffset) == "T"
                }
            },
            "interface 成员签名中的外层类型参数 `T` 不应在 Analysis API 懒解析链路上退化成 UNRESOLVED_REFERENCE: " +
                diagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.text}" },
        )
    }

    /** Analysis API exposes the peer range and witness attached to an undecidable extend-order diagnostic. */
    @Test
    fun extendCheckSequencePeerNote(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
        val sequenceDiagnostics = diagnostics.filter {
            it.factoryName == "EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE"
        }

        assertEquals(2, sequenceDiagnostics.size)
        for (diagnostic in sequenceDiagnostics) {
            val peerNote = diagnostic.relatedInformation.single()
            val peerPsi = requireNotNull(peerNote.psi)
            assertNotNull(peerNote.textRange)
            assertEquals(mainFile, peerPsi.containingFile)
            assertEquals("<", peerPsi.text, "the generic extend note anchors at the official identifier token")
            assertEquals(peerPsi.textLength, peerNote.textRange?.length)
            assertNotEquals(diagnostic.psi, peerPsi, "the note navigates to the peer declaration range")
            assertTrue(peerNote.message.contains("I1"))
            assertTrue(peerNote.message.contains("I3"))
        }
        assertTrue(
            sequenceDiagnostics.any {
                val note = it.relatedInformation.single().message
                "I2" in note && "I1" in note && "I4" in note && "I3" in note
            },
            "the peer note preserves both opposing subtype relations",
        )
    }
}
