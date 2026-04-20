package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseCall
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseCallArgumentMapping
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaCallApplicability
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaCallKind
import org.cangnova.cangjie.analysis.api.resolution.CaCallOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getCallInfo
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLCall
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLCallArgumentMapping
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLCallInfo
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

/**
 * CFIR 会话内的诊断与调用查询服务。
 *
 * 该层统一承载 low-level 查询以及 low-level -> public 的稳定映射。
 */
internal class CaCfirSessionDiagnosticQueryService(
    private val analysisSession: CaCfirSession,
    private val resolutionFacade: LLResolutionFacade,
    private val cacheStore: CaCfirSessionCacheStore,
) {
    fun queryCallInfo(element: CjElement): CaCallInfo? =
        cacheStore.getOrCreateCallInfo(element) {
            element.getCallInfo(resolutionFacade)?.asAnalysisCallInfo()
        }

    fun queryDiagnostics(
        element: PsiElement,
        filter: DiagnosticCheckerFilter,
    ): List<CjPsiDiagnostic> = cacheStore.getOrCreateDiagnostics(element, filter) {
        resolutionFacade.getDiagnostics(element, filter)
    }

    fun queryFileDiagnostics(
        file: CjFile,
        filter: DiagnosticCheckerFilter,
    ): Collection<CjPsiDiagnostic> = cacheStore.getOrCreateFileDiagnostics(file, filter) {
        resolutionFacade.collectDiagnosticsForFile(file, filter)
    }

    private fun LLCallInfo.asAnalysisCallInfo(): CaCallInfo = CaBaseCallInfo(
        successfulCall = successfulCall?.asAnalysisCall(),
        calls = calls.map { call -> call.asAnalysisCall() },
        token = analysisSession.token,
    )

    private fun LLCall.asAnalysisCall() = CaBaseCall(
        kind = kind.asAnalysisKind(),
        origin = origin.asAnalysisOrigin(),
        applicability = applicability.asAnalysisApplicability(),
        isImplicitInvoke = isImplicitInvoke,
        calleeName = calleeName,
        target = target?.let(analysisSession::getPublicSymbol) as? CaCallableSymbol,
        explicitReceiverType = explicitReceiverType?.asCaType(analysisSession),
        dispatchReceiverType = dispatchReceiverType?.asCaType(analysisSession),
        extensionReceiverType = extensionReceiverType?.asCaType(analysisSession),
        contextArgumentTypes = contextArgumentTypes.map { type -> type?.asCaType(analysisSession) },
        argumentTypes = argumentTypes.map { type -> type?.asCaType(analysisSession) },
        typeArguments = typeArguments.map { type -> type?.asCaType(analysisSession) },
        argumentMapping = argumentMapping.map { mapping -> mapping.asAnalysisCallArgumentMapping() },
        token = analysisSession.token,
    )

    private fun LLCallArgumentMapping.asAnalysisCallArgumentMapping() = CaBaseCallArgumentMapping(
        argumentIndex = argumentIndex,
        parameterName = parameterName,
        parameterType = parameterType?.asCaType(analysisSession),
        token = analysisSession.token,
    )

    private fun CallKind.asAnalysisKind(): CaCallKind = when (this) {
        CallKind.Function,
        CallKind.NamedValueAccess,
        CallKind.EnumConstructorCall,
            -> CaCallKind.FUNCTION
    }

    private fun CfirFunctionCallOrigin.asAnalysisOrigin(): CaCallOrigin = when (this) {
        CfirFunctionCallOrigin.Regular -> CaCallOrigin.REGULAR
        CfirFunctionCallOrigin.Operator -> CaCallOrigin.OPERATOR
        CfirFunctionCallOrigin.ConstructorDelegationThis -> CaCallOrigin.CONSTRUCTOR_DELEGATION_THIS
        CfirFunctionCallOrigin.ConstructorDelegationSuper -> CaCallOrigin.CONSTRUCTOR_DELEGATION_SUPER
    }

    private fun CandidateApplicability.asAnalysisApplicability(): CaCallApplicability = when (this) {
        CandidateApplicability.HIDDEN -> CaCallApplicability.HIDDEN
        CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER -> CaCallApplicability.INAPPLICABLE_WRONG_RECEIVER
        CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR -> CaCallApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR
        CandidateApplicability.INAPPLICABLE -> CaCallApplicability.INAPPLICABLE
        CandidateApplicability.VISIBILITY_ERROR -> CaCallApplicability.VISIBILITY_ERROR
        CandidateApplicability.UNSAFE_CALL -> CaCallApplicability.UNSAFE_CALL
        CandidateApplicability.UNSTABLE_SMARTCAST -> CaCallApplicability.UNSTABLE_SMARTCAST
        CandidateApplicability.CONVENTION_ERROR -> CaCallApplicability.CONVENTION_ERROR
        CandidateApplicability.RESOLVED_LOW_PRIORITY -> CaCallApplicability.RESOLVED_LOW_PRIORITY
        CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY -> CaCallApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY
        CandidateApplicability.RESOLVED_WITH_ERROR -> CaCallApplicability.RESOLVED_WITH_ERROR
        CandidateApplicability.RESOLVED -> CaCallApplicability.RESOLVED
    }
}
