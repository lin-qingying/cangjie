package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.analysis.CheckersComponent
import org.cangnova.cangjie.cfir.analysis.checkers.CommonDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.CommonExpressionCheckers
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.pipeline.runCheckers
import org.cangnova.cangjie.cfir.pipeline.runResolution
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendIndexStore
import org.cangnova.cangjie.cfir.session.registerDiagnosticReporter

/**
 * use-site 模块对应的 low-level resolve components。
 *
 * 这一层承接 Kotlin `LLFirModuleResolveComponents` 的模块级职责边界：
 * 1. 持有当前模块的底层 session；
 * 2. 统一完成 source files -> Raw CFIR 构建；
 * 3. 统一完成 resolve/checkers 以及 diagnostics snapshot 固化。
 *
 * 目前仓库还没有完整的 file-structure cache 与 lazy declaration resolver，
 * 但这些职责不应继续散落在 facade service 中，因此先在 module-level components 中集中落位。
 */
internal class CaCfirModuleResolveComponents(
    val module: CaModule,
    val globalResolveComponents: CaCfirGlobalResolveComponents,
    val moduleProvider: CaCfirModuleProvider,
    val sessionProvider: CaCfirSessionProvider,
    val resolutionStrategyProvider: CaCfirModuleResolutionStrategyProvider,
) {
    val session: CfirSession by lazy(LazyThreadSafetyMode.NONE) {
        sessionProvider.getSession(module)
    }

    private val rawCfirFiles: List<CfirFile> by lazy(LazyThreadSafetyMode.NONE) {
        globalResolveComponents.getSourceFiles(module).map { cjFile ->
            PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL).buildCfirFile(cjFile).also { cfirFile ->
                val cfirProvider = session.cfirProvider as? CfirProviderImpl
                    ?: error(
                        "low-level CFIR session `${module.moduleDescription}` 必须暴露可记录文件的 CfirProviderImpl，" +
                            "实际得到 `${session.cfirProvider::class.simpleName}`。"
                    )
                cfirProvider.recordFile(cfirFile)
            }
        }
    }

    /**
     * module 级 resolved snapshot。
     *
     * facade / semantic queries / diagnostics 必须共享同一轮 `runResolution(...)` 的结果，
     * 不能再让一条链消费 raw files、另一条链消费 resolved files。
     */
    private val resolutionSnapshot by lazy(LazyThreadSafetyMode.NONE) {
        session.runResolution(rawCfirFiles)
    }

    val cfirFiles: List<CfirFile> by lazy(LazyThreadSafetyMode.NONE) {
        resolutionSnapshot.second
    }

    val diagnostics: DiagnosticBuckets by lazy(LazyThreadSafetyMode.NONE) {
        resolveDiagnostics()
    }

    /**
     * 当前 use-site 模块共享的 low-level scope snapshot provider。
     *
     * package/member/type scope 的缓存必须和模块级 resolve components 同生命周期，
     * 才能与 session、CFIR file 和 diagnostics 快照保持一致的失效边界。
     */
    val scopeProvider: CaCfirScopeProvider by lazy(LazyThreadSafetyMode.NONE) {
        CaCfirScopeProvider(this)
    }

    /**
     * use-site 模块闭包上的低层可见符号入口。
     *
     * 顶层包、class-like 与 callable 的查询必须和当前模块闭包、解析策略、session cache 共用同一份
     * low-level 语义边界，不能再由上层组件直接读取某一个 `CfirSession.symbolProvider`。
     */
    val visibleSymbolProvider: CaCfirVisibleSymbolProvider by lazy(LazyThreadSafetyMode.NONE) {
        CaCfirVisibleSymbolProvider(this)
    }

    /**
     * use-site 模块闭包上的源码声明定位器。
     *
     * low-level 当前还没有完整的 PSI-aware symbol provider，因此把源码声明定位统一收口在这里，
     * 避免 `analysis-api-cfir` 再自行遍历源码根目录。
     */
    val declarationLocator: CaCfirDeclarationLocator by lazy(LazyThreadSafetyMode.NONE) {
        CaCfirDeclarationLocator(this)
    }

    /**
     * use-site 模块闭包上的源码导航入口。
     *
     * 符号到 PSI、符号到文件的映射规则统一收敛在 low-level 层，
     * 上层 Analysis API 组件只消费稳定协议，不再自行拼接回查策略。
     */
    val sourceNavigationProvider: CaCfirSourceNavigationProvider by lazy(LazyThreadSafetyMode.NONE) {
        CaCfirSourceNavigationProvider(this)
    }

    /**
     * 暴露当前 use-site 视角下任意模块的解析策略。
     *
     * 这使得后续 scope/resolver/file-structure 组件不需要再次从模块类型反推解析方式，
     * 而是直接共享 low-level 已经确定的策略结果。
     */
    fun getResolutionStrategy(module: CaModule): CaCfirModuleResolutionStrategy {
        return resolutionStrategyProvider.getKind(module)
    }

    /**
     * 暴露当前 use-site 快照下的可见模块集合。
     *
     * 后续 scope/cache/invalidation 逻辑需要围绕同一闭包工作，而不是重新各自遍历依赖图。
     */
    val allModules: Set<CaModule>
        get() = moduleProvider.allModules

    private fun resolveDiagnostics(): DiagnosticBuckets {
        val resolveDiagnosticCollector = CfirDiagnosticCollector()
        session.register(
            CheckersComponent::class,
            CheckersComponent().apply {
                register(CommonDeclarationCheckers)
                register(CommonExpressionCheckers)
            },
        )
        session.registerDiagnosticReporter(resolveDiagnosticCollector)
        session.register(
            CfirExtendProvider::class,
            CfirSessionExtendProvider(session, session.extendIndexStore),
        )

        val (scopeSession, resolvedFiles) = resolutionSnapshot
        val checkerCollector = DiagnosticsCollectorImpl()
        session.runCheckers(
            scopeSession = scopeSession,
            firFiles = resolvedFiles,
            diagnosticsCollector = checkerCollector,
        )

        return DiagnosticBuckets(
            defaultDiagnostics = normalizeCommonDiagnostics(
                resolveDiagnostics = resolveDiagnosticCollector.rawDiagnostics.filterIsInstance<CjPsiDiagnostic>(),
                checkerDiagnostics = checkerCollector.diagnostics.filterIsInstance<CjPsiDiagnostic>(),
            ),
            extraDiagnostics = emptyList(),
            experimentalDiagnostics = emptyList(),
        )
    }

    /**
     * 当前对外暴露的是本次 low-level 分析快照里的稳定 common diagnostics。
     *
     * 在 module-level components 层统一做去重与排序，才能保证 Analysis API、LSP 与 standalone
     * 看到的是同一份稳定结果，而不是依赖底层 reporter 注册顺序。
     */
    private fun normalizeCommonDiagnostics(
        resolveDiagnostics: List<CjPsiDiagnostic>,
        checkerDiagnostics: List<CjPsiDiagnostic>,
    ): List<CjPsiDiagnostic> {
        return (resolveDiagnostics + checkerDiagnostics)
            .distinctBy { diagnostic ->
                Triple(diagnostic.factoryName, diagnostic.textRanges.firstOrNull() ?: TextRange.EMPTY_RANGE, diagnostic.psiElement)
            }
            .sortedWith(
                compareBy<CjPsiDiagnostic> { diagnostic -> diagnostic.textRanges.firstOrNull()?.startOffset ?: -1 }
                    .thenBy { diagnostic -> diagnostic.textRanges.firstOrNull()?.endOffset ?: -1 }
                    .thenBy { it.factoryName },
            )
    }
}
