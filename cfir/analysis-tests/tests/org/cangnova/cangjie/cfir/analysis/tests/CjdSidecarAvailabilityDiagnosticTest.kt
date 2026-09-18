package org.cangnova.cangjie.cfir.analysis.tests

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.impl.BaseDiagnosticsCollector
import org.cangnova.cangjie.cfir.pipeline.runCheckers
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.providers.CfirBuiltinSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
import org.cangnova.cangjie.cfir.session.CfirApiLevelProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.registerCliCompilerAndCommonComponents
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.LanguageVersionSettingsImpl

/**
 * 引用点 availability 诊断的端到端验收（P4 收尾项 2）。
 *
 * 验证的目标链条：真实 SDK 的 `.cjo` + 同目录 `.cj.d` 经**生产 provider**
 * （[CfirDeserializedSymbolProvider]）加载并合并 sidecar 注解后，
 * `CfirApiLevelRefHigherChecker` 能据此对**源码引用点**产出 `APILEVEL_REF_HIGHER`。
 *
 * 与既有 SDK 用例的分工：
 * - `CjdBinaryAnnotationIntegrationTest.testConfiguredSdkRoundingModeAvailability` 证明
 *   "合并后的声明**自身**能看到 since=22"（数据面）；
 * - 本用例证明"该数据真的驱动了**引用点诊断**"（检查器面）—— 后者是 4.7 声称
 *   "APILevel checker 不做任何改动、自动受益"的唯一可执行证据。
 *
 * 为什么必须用限定访问：`CfirApiLevelRefHigherChecker` 只作用于
 * `CfirQualifiedAccessExpression`，所以引用目标必须是 `RoundingMode.Floor` 这样的成员访问，
 * 不能是裸标识符。
 */
class CjdSidecarAvailabilityDiagnosticTest : AbstractCfirAnalysisResolveTest() {

    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    @Test
    fun testApiLevelMergedFromSidecarDrivesReferenceDiagnostic() {
        val directory = System.getenv("CANGJIE_CJD_SDK_DIR")
        assumeTrue(!directory.isNullOrBlank(), "Set CANGJIE_CJD_SDK_DIR to enable real SDK validation")

        val session = createTestSession()

        // 库会话 + 反序列化 provider：与 CfirAbstractSessionFactory.createLibrarySession 的装配同构。
        // 组件清单用生产入口 registerCliCompilerAndCommonComponents（含 CfirLazyDeclarationResolver
        // 等 20 余个必需组件），不逐个手写 —— 手写必然漏项。
        val librarySession = object : CfirSession(CfirSession.Kind.Library) {}.also { library ->
            library.registerCliCompilerAndCommonComponents(LanguageVersionSettingsImpl.DEFAULT)
            library.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
        val cjoManager = CjoManager(CjoSearchPath { if (it == "CANGJIE_STDLIB_MODULE") directory else null })
        val libraryModule = CfirBinaryDependenciesModuleData(Name.special("<cjd-sdk-library>"))
        libraryModule.bindSession(librarySession)
        val libraryProvider = CfirDeserializedSymbolProvider(
            librarySession,
            cjoManager,
            CfirCangJieScopeProvider(),
            libraryModule,
        )
        // 库会话必须**自注册**符号 provider 及配套 provider 壳：反序列化 provider 在解析跨包引用时
        // 会查询 `session.symbolProvider` / `session.cfirProvider`，而那个 session 是它自己的库会话。
        librarySession.register(CfirSymbolProvider::class, libraryProvider)
        librarySession.register(CfirProvider::class, CfirLibrarySessionProvider(libraryProvider))
        librarySession.register(
            CfirExtendProvider::class,
            CfirExtendProviderComposer.fromSymbolProviders(listOf(libraryProvider)),
        )
        librarySession.register(
            org.cangnova.cangjie.cfir.session.StructuredProviders::class,
            org.cangnova.cangjie.cfir.session.StructuredProviders(
                sourceProviders = emptyList(),
                dependencyProviders = listOf(libraryProvider),
                sharedProvider = libraryProvider,
            ),
        )

        session.register(
            CfirSymbolProvider::class,
            CfirCompositeSymbolProvider(
                session,
                listOf(
                    session.cfirProvider.symbolProvider,
                    CfirBuiltinSymbolProvider(session),
                    libraryProvider,
                ),
            ),
        )
        // 项目 API level 低于目标声明的 since="22" ⇒ 引用必须被拒。
        session.register(CfirApiLevelProvider::class, object : CfirApiLevelProvider {
            override val projectApiLevel: Int get() = PROJECT_API_LEVEL
        })

        val cjFile = createCjFile("useApiLevelHigherThanProject", SOURCE)
        val cfirFile = cjFile.toCfirFile(session = session)

        // 对齐 analyse.kt 的 resolveAndCheckCfir 两段式。
        val resolveCollector = CfirDiagnosticCollector()
        resolveToPhase(cfirFile, session, targetPhase, resolveCollector)
        val checkerCollector = RecordingDiagnosticsCollector()
        session.runCheckers(ScopeSession(), listOf(cfirFile), checkerCollector)

        val resolvedNames = resolveCollector.rawDiagnostics.map { it.factoryName.removePrefix("CFIR_") }
        val checkerNames = checkerCollector.diagnostics.map { it.factoryName.removePrefix("CFIR_") }
        assertTrue(
            "UNRESOLVED_REFERENCE" !in resolvedNames,
            "引用必须先被解析到 .cjo 声明，否则本用例不构成端到端验收；resolve 阶段诊断：$resolvedNames",
        )
        assertTrue(
            "APILEVEL_REF_HIGHER" in checkerNames,
            "sidecar 合并出的 since=$TARGET_SINCE 必须驱动引用点诊断（projectApiLevel=$PROJECT_API_LEVEL）；" +
                "checker 阶段诊断：$checkerNames（resolve 阶段：$resolvedNames）",
        )
    }

    /** checker 阶段使用的内存诊断收集器（[BaseDiagnosticsCollector] 最小实现）。 */
    private class RecordingDiagnosticsCollector : BaseDiagnosticsCollector() {
        private val storage = mutableListOf<CjDiagnostic>()

        override val diagnostics: List<CjDiagnostic>
            get() = storage

        override val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>>
            get() = storage.groupBy { null }

        override val hasErrors: Boolean
            get() = storage.any { it.severity.isError }

        override val hasWarningsForWError: Boolean
            get() = false

        override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
            if (diagnostic != null) storage += diagnostic
        }
    }

    private companion object {
        /** 低于 `std.math.RoundingMode` 的 `@!APILevel[since: "22"]`。 */
        const val PROJECT_API_LEVEL = 20
        const val TARGET_SINCE = 22

        /** 限定访问是 `CfirQualifiedAccessExpression` 的必要形状（见类 KDoc）。 */
        val SOURCE = """
            package demo
            import std.math.*
            main(): Int64 {
                let mode = RoundingMode.Floor
                0
            }
        """.trimIndent()
    }
}
