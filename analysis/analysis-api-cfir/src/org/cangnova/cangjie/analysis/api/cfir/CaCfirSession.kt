package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.cfir.components.*
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacade
import org.cangnova.cangjie.analysis.api.impl.base.CaBaseSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * CFIR 实现的分析会话。
 *
 * 通过 [resolutionFacade] 间接访问底层 [CfirSession]，
 * 组件通过 [analysisSessionProvider] 回引本 session。
 * Kotlin 对应文件：`external/kotlin/analysis/analysis-api-cfir/src/org/jetbrains/kotlin/analysis/api/cfir/KaFirSession.kt`
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
    diagnosticProvider = CaCfirDiagnosticProvider(analysisSessionProvider),
    scopeProvider = CaCfirScopeProvider(analysisSessionProvider),
    completionCandidateChecker = CaCfirCompletionCandidateChecker(analysisSessionProvider),
    expressionTypeProvider = CaCfirExpressionTypeProvider(analysisSessionProvider),
    typeProvider = CaCfirTypeProvider(analysisSessionProvider),
    typeInformationProvider = CaCfirTypeInformationProvider(analysisSessionProvider),
    symbolProvider = CaCfirSymbolProvider(analysisSessionProvider),
    cInteropComponent = CaCfirCInteropComponent(analysisSessionProvider),
    symbolInformationProvider = CaCfirSymbolInformationProvider(analysisSessionProvider),
    typeRelationChecker = CaCfirTypeRelationChecker(analysisSessionProvider),
    expressionInformationProvider = CaCfirExpressionInformationProvider(analysisSessionProvider),
    evaluator = CaCfirEvaluator(analysisSessionProvider),
    referenceShortener = CaCfirReferenceShortener(analysisSessionProvider),
    importOptimizer = CaCfirImportOptimizer(analysisSessionProvider),
    renderer = CaCfirRenderer(analysisSessionProvider),
    visibilityChecker = CaCfirVisibilityChecker(analysisSessionProvider),
    originalPsiProvider = CaCfirOriginalPsiProvider(analysisSessionProvider),
    typeCreator = CaCfirTypeCreator(analysisSessionProvider),
    analysisScopeProvider = CaCfirAnalysisScopeProvider(analysisSessionProvider),
    signatureSubstitutor = CaCfirSignatureSubstitutor(analysisSessionProvider),
    substitutorProvider = CaCfirSubstitutorProvider(analysisSessionProvider),
    dataFlowProvider = CaCfirDataFlowProvider(analysisSessionProvider),
    sourceProvider = CaCfirSourceProvider(analysisSessionProvider),
    docProvider = CaCfirDocProvider(analysisSessionProvider),
) {
    override val useSiteModule: CaModule get() = resolutionFacade.useSiteModule

    internal val cfirSession: CfirSession get() = resolutionFacade.useSiteFirSession

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

