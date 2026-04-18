package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirAnalysisScopeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirAnnotationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirCInteropComponent
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirCompletionCandidateChecker
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirDataFlowProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirDefaultImportProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirDiagnosticProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirDocProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirEvaluator
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirExpressionInformationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirExpressionTypeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirImportOptimizer
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirOriginalPsiProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirReferenceShortener
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirRenderer
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirResolver
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirScopeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSignatureProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSignatureSubstitutor
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSourceProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSubstitutorProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSymbolInformationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSymbolRelationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeCreator
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeInformationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeRelationChecker
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirVisibilityChecker
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbolProvider
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacade
import org.cangnova.cangjie.analysis.api.impl.base.CaBaseSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * CFIR 后端分析会话。
 *
 * 这个类只承担会话编排职责：
 * 1. 组装公开 Analysis API 组件；
 * 2. 暴露统一的 session 内部协议；
 * 3. 将缓存实现与 low-level 查询分别委托给独立服务。
 *
 * 因此 `analysis-api-cfir` 的组件层只依赖会话协议，
 * 而不再直接操作 low-level facade 或私有缓存结构。
 */
internal class CaCfirSession private constructor(
    val project: Project,
    val resolutionFacade: CaCfirResolutionFacade,
    token: CaLifetimeToken,
    analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSession(
    token = token,
    resolver = CaCfirResolver(analysisSessionProvider),
    symbolRelationProvider = CaCfirSymbolRelationProvider(analysisSessionProvider),
    symbolProvider = CaCfirSymbolProvider(analysisSessionProvider),
    symbolInformationProvider = CaCfirSymbolInformationProvider(analysisSessionProvider),
    annotationProvider = CaCfirAnnotationProvider(analysisSessionProvider),
    signatureProvider = CaCfirSignatureProvider(analysisSessionProvider),
    diagnosticProvider = CaCfirDiagnosticProvider(analysisSessionProvider),
    scopeProvider = CaCfirScopeProvider(analysisSessionProvider),
    analysisScopeProvider = CaCfirAnalysisScopeProvider(analysisSessionProvider),
    defaultImportProvider = CaCfirDefaultImportProvider(analysisSessionProvider),
    completionCandidateChecker = CaCfirCompletionCandidateChecker(analysisSessionProvider),
    expressionTypeProvider = CaCfirExpressionTypeProvider(analysisSessionProvider),
    expressionInformationProvider = CaCfirExpressionInformationProvider(analysisSessionProvider),
    evaluator = CaCfirEvaluator(analysisSessionProvider),
    dataFlowProvider = CaCfirDataFlowProvider(analysisSessionProvider),
    typeProvider = CaCfirTypeProvider(analysisSessionProvider),
    typeInformationProvider = CaCfirTypeInformationProvider(analysisSessionProvider),
    typeRelationChecker = CaCfirTypeRelationChecker(analysisSessionProvider),
    typeCreator = CaCfirTypeCreator(analysisSessionProvider),
    substitutorProvider = CaCfirSubstitutorProvider(analysisSessionProvider),
    signatureSubstitutor = CaCfirSignatureSubstitutor(analysisSessionProvider),
    referenceShortener = CaCfirReferenceShortener(analysisSessionProvider),
    importOptimizer = CaCfirImportOptimizer(analysisSessionProvider),
    renderer = CaCfirRenderer(analysisSessionProvider),
    visibilityChecker = CaCfirVisibilityChecker(analysisSessionProvider),
    originalPsiProvider = CaCfirOriginalPsiProvider(analysisSessionProvider),
    sourceProvider = CaCfirSourceProvider(analysisSessionProvider),
    cInteropComponent = CaCfirCInteropComponent(analysisSessionProvider),
    docProvider = CaCfirDocProvider(analysisSessionProvider),
) {
    override val useSiteModule: CaModule
        get() = resolutionFacade.useSiteModule

    internal val cfirSession: CfirSession
        get() = resolutionFacade.useSiteCfirSession

    /**
     * public symbol 的构造统一委托给 Kotlin 风格的 builder。
     *
     * session 只负责缓存、查询与组件编排，不再把具体 symbol 组装逻辑直接堆在 factory 文件里。
     */
    internal val cfirSymbolBuilder: CaSymbolByCfirBuilder by lazy {
        CaSymbolByCfirBuilder(project, this, token)
    }

    /**
     * 会话级缓存存储。
     *
     * 对齐 Kotlin `KaFirSession.cacheStorage` 的角色：session 只持有缓存基础设施，
     * 不再自己暴露一大批逐项转发方法。
     */
    internal val cacheStorage = CaCfirSessionCacheStore()

    /**
     * 会话级语义查询服务集合。
     *
     * 组件层直接依赖这些稳定服务对象，而不是把 `CaCfirSession` 当成总线式查询入口。
     */
    internal val diagnosticQueries = CaCfirSessionDiagnosticQueryService(
        resolutionFacade = resolutionFacade,
        cacheStore = cacheStorage,
    )
    internal val scopeQueries = CaCfirSessionScopeQueryService(
        resolutionFacade = resolutionFacade,
        cacheStore = cacheStorage,
    )
    internal val symbolQueries = CaCfirSessionSymbolQueryService(
        resolutionFacade = resolutionFacade,
        cacheStore = cacheStorage,
    )
    internal val typeQueries = CaCfirSessionTypeQueryService(
        resolutionFacade = resolutionFacade,
        cacheStore = cacheStorage,
    )

    companion object {
        fun create(
            project: Project,
            resolutionFacade: CaCfirResolutionFacade,
            token: CaLifetimeToken,
        ): CaCfirSession {
            lateinit var session: CaCfirSession
            val sessionProvider: () -> CaCfirSession = { session }
            session = CaCfirSession(
                project = project,
                resolutionFacade = resolutionFacade,
                token = token,
                analysisSessionProvider = sessionProvider,
            )
            return session
        }
    }
}
