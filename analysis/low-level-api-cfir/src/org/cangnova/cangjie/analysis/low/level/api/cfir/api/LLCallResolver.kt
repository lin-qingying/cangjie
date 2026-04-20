package org.cangnova.cangjie.analysis.low.level.api.cfir.api

import org.cangnova.cangjie.analysis.low.level.api.cfir.util.ContextCollector
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.diagnostic.ConeHiddenCandidateError
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.resolve.body.CfirCallResolver
import org.cangnova.cangjie.cfir.resolve.body.CfirDeclarationsResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirExpressionsResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.OverloadCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.createConeDiagnosticForCandidateWithError
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjQualifiedExpression
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

/**
 * low-level 调用查询器。
 *
 * 这里直接复用 CFIR 主干的 `CfirCallResolver.collectAllCandidates(...)`，
 * 对齐 Kotlin low-level `AllCandidatesResolver` 的架构位置，
 * 不再让上层 analysis-api 依赖私有 snapshot。
 */
internal class LLCallResolver(
    private val cfirSession: CfirSession,
) {
    fun resolveCallInfo(
        resolutionFacade: LLResolutionFacade,
        element: CjElement,
    ): LLCallInfo? {
        val qualifiedAccess = element.getOrBuildCfir(resolutionFacade) as? CfirQualifiedAccessExpression
            ?: element.getParentOfType<CjQualifiedExpression>(strict = false)?.getOrBuildCfir(resolutionFacade) as? CfirQualifiedAccessExpression
            ?: return null

        val calleeName = (qualifiedAccess.calleeReference as? CfirNamedReference)?.name ?: return null
        val resolver = createCallResolver(resolutionFacade, element) ?: return null
        val calls = resolver.collectAllCandidates(
            qualifiedAccess = qualifiedAccess,
            name = calleeName,
            resolutionMode = ResolutionMode.ContextIndependent,
        ).mapNotNull(OverloadCandidate::toLlCall)

        val successfulCall = calls.firstOrNull { call ->
            call.applicability == CandidateApplicability.RESOLVED ||
                call.applicability == CandidateApplicability.RESOLVED_LOW_PRIORITY
        }

        return LLCallInfo(
            successfulCall = successfulCall,
            calls = calls,
        )
    }

    private fun createCallResolver(
        resolutionFacade: LLResolutionFacade,
        element: CjElement,
    ): CfirCallResolver? {
        val cfirFile = element.containingCjFile.getOrBuildCfirFile(resolutionFacade)
        val towerDataContext = ContextCollector.process(resolutionFacade, cfirFile, element)?.towerDataContext ?: return null
        val bodyResolveContext = BodyResolveContext(isContextCollectorMode = true)
        bodyResolveContext.file = cfirFile
        bodyResolveContext.replaceTowerDataContext(towerDataContext)

        val dispatcher = object : CfirAbstractBodyResolveTransformerDispatcher(CfirResolvePhase.BODY_RESOLVE) {
            override val context: BodyResolveContext = bodyResolveContext
            override val components: BodyResolveTransformerComponents =
                BodyResolveTransformerComponents(
                    cfirSession,
                    resolutionFacade.getScopeSessionFor(cfirSession),
                    this,
                    bodyResolveContext,
                )
            override val expressionsTransformer: CfirExpressionsResolveTransformer =
                CfirExpressionsResolveTransformer(this)
            override val declarationsTransformer: CfirDeclarationsResolveTransformer =
                CfirDeclarationsResolveTransformer(this)
        }

        dispatcher.components.callResolver.initTransformer(dispatcher.expressionsTransformer)
        return dispatcher.components.callResolver
    }
}

private fun OverloadCandidate.toLlCall(): LLCall? {
    val candidate = candidate
    val applicability = candidate.toLlApplicability() ?: return null
    return candidate.toLLCall().copy(applicability = applicability)
}

private fun Candidate.toLlApplicability(): CandidateApplicability? {
    val applicability = if (isSuccessful) {
        lowestApplicability
    } else {
        createConeDiagnosticForCandidateWithError(lowestApplicability, this).let { diagnostic ->
            if (diagnostic is ConeHiddenCandidateError) {
                return null
            }
            lowestApplicability
        }
    }

    return applicability
}
