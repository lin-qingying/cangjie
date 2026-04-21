package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseCall
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseCallArgumentMapping
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaCallApplicability
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.cfir.symbols.getExtendPublicSymbols
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirBackedSymbol
import org.cangnova.cangjie.analysis.api.components.CaResolver
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaCallKind
import org.cangnova.cangjie.analysis.api.resolution.CaCallOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.resolver.AllCandidatesResolver
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.diagnostic.ConeHiddenCandidateError
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.OverloadCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.createConeDiagnosticForCandidateWithError
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjQualifiedExpression
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

/**
 * CFIR resolver 组件。
 *
 * 该组件只负责把公开 Analysis API 的解析请求映射到 session 内部协议，
 * 不再直接接触 low-level facade。
 */
internal class CaCfirResolver(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaResolver, CaCfirSessionComponent {
    override fun CjReferenceExpression.resolveToSymbols(): Collection<CaSymbol> = withValidityAssertion {
        val matchBranchBindings = restoreMatchBranchPatternBindings(this@resolveToSymbols).distinctSymbols()
        if (matchBranchBindings.isNotEmpty()) {
            return@withValidityAssertion matchBranchBindings
        }

        val callBackedSymbols = restoreCallBackedSymbols(this@resolveToSymbols).distinctSymbols()
        if (callBackedSymbols.isNotEmpty()) {
            return@withValidityAssertion callBackedSymbols
        }

        analysisSession.symbolQueries.resolveSymbols(this@resolveToSymbols)
            .map(analysisSession::getPublicSymbol)
            .distinctSymbols()
    }

    override fun CjElement.resolveToCall(): CaCallInfo? = withValidityAssertion {
        analysisSession.cacheStorage.getOrCreateCallInfo(this@resolveToCall) {
            resolveCallInfo(this@resolveToCall)
        }
    }

    /**
     * `match` 分支中的模式绑定属于源码局部声明。
     *
     * 它们在当前仓库里还没有完全通过 low-level reference 索引稳定暴露，
     * 但其语义边界在 PSI 上是明确的：只能解析到当前分支条件侧声明的具名绑定。
     * 因此这里直接基于 `CjMatchEntry.conditions` 恢复同分支 binding symbol，
     * 保证不同分支的同名绑定不会混淆。
     */
    private fun restoreMatchBranchPatternBindings(reference: CjReferenceExpression): Collection<CaSymbol> {
        val simpleName = reference as? CjSimpleNameExpression ?: return emptyList()
        val matchEntry = simpleName.getStrictParentOfType<CjMatchEntry>() ?: return emptyList()
        val arrow = matchEntry.arrow ?: return emptyList()
        if (simpleName.textOffset <= arrow.textOffset) {
            return emptyList()
        }

        return matchEntry.conditions.asSequence()
            .flatMap { condition ->
                sequence {
                    yieldAll(condition.getAllBindings().asSequence())
                    yieldAll(com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(condition, CjVarOrEnumPattern::class.java).asSequence())
                }
            }
            .filter { declaration -> declaration.name == simpleName.referencedName }
            .mapNotNull { declaration ->
                resolvePatternBindingSymbolByPsi(declaration)
                    ?: (declaration as? CjVarOrEnumPattern)?.reference?.let(::resolvePatternBindingSymbolByPsi)
            }
            .toList()
    }

    private fun Collection<CaSymbol>.distinctSymbols(): List<CaSymbol> {
        return distinctBy { symbol ->
            symbol.publicSymbolCacheKeyOrNull() ?: "${symbol::class.qualifiedName}@${System.identityHashCode(symbol)}"
        }
    }

    /**
     * `resolveToSymbol()` 不能只盯住“当前 PSI 节点恰好被 low-level 语义索引命中”这一种形态。
     *
     * 对位上游的调用入口设计，call-shaped PSI 也需要稳定映射回目标 callable；
     * 仓颉这里同样需要把 `call info` 作为正式语义来源之一，而不是让 `CjCallExpression`
     * 因为索引锚点落在父节点/子节点就直接解析失败。
     */
    private fun restoreCallBackedSymbols(reference: CjReferenceExpression): Collection<CaSymbol> {
        val callInfo = generateSequence(reference as com.intellij.psi.PsiElement?) { current -> current.parent }
            .filterIsInstance<CjElement>()
            .mapNotNull { element ->
                analysisSession.cacheStorage.getOrCreateCallInfo(element) {
                    resolveCallInfo(element)
                }
            }
            .firstOrNull { resolvedCallInfo ->
                resolvedCallInfo.successfulCall?.target != null || resolvedCallInfo.calls.any { call -> call.target != null }
            }
            ?: return emptyList()

        val resolvedTargets = buildList {
            callInfo.successfulCall?.target?.let(::add)
            callInfo.calls.mapNotNullTo(this) { call -> call.target }
        }

        val extendDispatchTargets = restoreExtendDispatchTargets(reference, callInfo)
        if (extendDispatchTargets.isNotEmpty()) {
            return extendDispatchTargets
        }

        return resolvedTargets
    }

    /**
     * extend 成员调用在 low-level `call target` 上可能先落到被实现的接口成员。
     *
     * 为了让引用、导航、查找用法统一指向真正承载实现体的 extend 成员，
     * 这里基于接收者类型把目标回收到对应的 extend declared-member scope。
     */
    private fun restoreExtendDispatchTargets(
        reference: CjReferenceExpression,
        callInfo: CaCallInfo,
    ): Collection<CaSymbol> {
        val memberName = (reference as? CjSimpleNameExpression)?.referencedNameAsName
            ?: callInfo.successfulCall?.calleeName
            ?: return emptyList()

        val receiverClassId = (callInfo.successfulCall?.explicitReceiverType as? CaClassLikeType)?.classId
            ?: callInfo.calls.asSequence()
                .mapNotNull { call -> (call.explicitReceiverType as? CaClassLikeType)?.classId }
                .firstOrNull()
            ?: return emptyList()

        return analysisSession.getExtendPublicSymbols(receiverClassId)
            .flatMap { extendSymbol ->
                with(analysisSession) {
                    extendSymbol.declaredMemberScope.getCallableSymbols(memberName)
                }
            }
            .onEach { symbol ->
                (symbol as? CaCfirBackedSymbol<*>)?.backingSymbol?.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            }
            .distinctSymbols()
    }

    private fun resolvePatternBindingSymbolByPsi(psi: com.intellij.psi.PsiElement): CaPatternBindingSymbol? {
        return analysisSession.symbolQueries.lookupSymbolsByPsi(psi)
            .map(analysisSession::getPublicSymbol)
            .filterIsInstance<CaPatternBindingSymbol>()
            .firstOrNull()
    }

    /**
     * 调用解析结果对齐 Kotlin `KaFirResolver.resolveToCall()` 的组件职责：
     * resolver 直接从后端解析结果构造公开调用模型，不经 low-level API 私有 call-info 包装层。
     */
    private fun resolveCallInfo(element: CjElement): CaCallInfo? {
        val qualifiedAccess = element.getOrBuildCfir(analysisSession.resolutionFacade) as? CfirQualifiedAccessExpression
            ?: element.getParentOfType<CjQualifiedExpression>(strict = false)
                ?.getOrBuildCfir(analysisSession.resolutionFacade) as? CfirQualifiedAccessExpression
            ?: return null
        val calleeName = (qualifiedAccess.calleeReference as? CfirNamedReference)?.name ?: return null

        val calls = AllCandidatesResolver(analysisSession.cfirSession)
            .getAllCandidates(
                resolutionFacade = analysisSession.resolutionFacade,
                qualifiedAccess = qualifiedAccess,
                calleeName = calleeName,
                element = element,
                resolutionMode = ResolutionMode.ContextIndependent,
            )
            .mapNotNull { overloadCandidate -> overloadCandidate.toAnalysisCallOrNull() }

        val successfulCall = calls.firstOrNull { call ->
            call.applicability == CaCallApplicability.RESOLVED ||
                call.applicability == CaCallApplicability.RESOLVED_LOW_PRIORITY
        }

        return CaBaseCallInfo(
            successfulCall = successfulCall,
            calls = calls,
            token = analysisSession.token,
        )
    }

    private fun OverloadCandidate.toAnalysisCallOrNull(): CaCall? {
        val applicability = candidate.toAnalysisApplicabilityOrNull() ?: return null
        return candidate.toAnalysisCall(applicability)
    }

    private fun Candidate.toAnalysisApplicabilityOrNull(): CaCallApplicability? {
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

        return applicability.asAnalysisApplicability()
    }

    private fun Candidate.toAnalysisCall(applicability: CaCallApplicability): CaCall = CaBaseCall(
        kind = callInfo.callKind.asAnalysisKind(),
        origin = callInfo.origin.asAnalysisOrigin(),
        applicability = applicability,
        isImplicitInvoke = callInfo.isImplicitInvoke,
        calleeName = callInfo.name,
        target = (symbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>)
            ?.let(analysisSession::getPublicSymbol) as? CaCallableSymbol,
        explicitReceiverType = callInfo.explicitReceiver?.coneTypeOrNull?.asCaType(analysisSession),
        dispatchReceiverType = dispatchReceiverExpression()?.coneTypeOrNull?.asCaType(analysisSession),
        extensionReceiverType = chosenExtensionReceiverExpression()?.coneTypeOrNull?.asCaType(analysisSession),
        contextArgumentTypes = contextArguments().map { expression -> expression.coneTypeOrNull?.asCaType(analysisSession) },
        argumentTypes = callInfo.arguments.map { expression -> expression.coneTypeOrNull?.asCaType(analysisSession) },
        typeArguments = callInfo.typeArguments.map { typeRef -> typeRef.coneTypeOrNull?.asCaType(analysisSession) },
        argumentMapping = createAnalysisArgumentMapping(),
        token = analysisSession.token,
    )

    private fun Candidate.createAnalysisArgumentMapping() = if (argumentMappingInitialized) {
        arguments.mapIndexed { index, argumentAtom ->
            val parameter = argumentMapping[argumentAtom]
            CaBaseCallArgumentMapping(
                argumentIndex = index,
                parameterName = parameter?.name,
                parameterType = parameter?.returnTypeRef?.coneTypeOrNull?.asCaType(analysisSession),
                token = analysisSession.token,
            )
        }
    } else {
        emptyList()
    }

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
