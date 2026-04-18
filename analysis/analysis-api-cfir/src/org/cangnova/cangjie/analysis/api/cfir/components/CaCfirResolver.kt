package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.getExtendPublicSymbols
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallApplicability
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallArgumentMappingSnapshot
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallInfoSnapshot
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallKind
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallOrigin
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallSnapshot
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirBackedSymbol
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.components.CaResolver
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseCall
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseCallArgumentMapping
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseCallInfo
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaCallApplicability
import org.cangnova.cangjie.analysis.api.resolution.CaCallArgumentMapping
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaCallKind
import org.cangnova.cangjie.analysis.api.resolution.CaCallOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings

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
        analysisSession.diagnosticQueries.queryCallInfo(this@resolveToCall)?.asAnalysisCallInfo(analysisSession, token)
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
     * Kotlin Analysis 在调用入口上会把 call-shaped PSI 也稳定映射回目标 callable；
     * 仓颉这里同样需要把 `call info` 作为正式语义来源之一，而不是让 `CjCallExpression`
     * 因为索引锚点落在父节点/子节点就直接解析失败。
     */
    private fun restoreCallBackedSymbols(reference: CjReferenceExpression): Collection<CaSymbol> {
        val snapshot = generateSequence(reference as com.intellij.psi.PsiElement?) { current -> current.parent }
            .mapNotNull(analysisSession.diagnosticQueries::queryCallInfo)
            .firstOrNull { callInfo ->
                callInfo.successfulCall?.target != null || callInfo.calls.any { call -> call.target != null }
            }
            ?: return emptyList()

        val lowLevelTargets = buildList {
            snapshot.successfulCall?.target?.let(::add)
            snapshot.calls.mapNotNullTo(this) { call -> call.target }
        }

        val extendDispatchTargets = restoreExtendDispatchTargets(reference, snapshot)
        if (extendDispatchTargets.isNotEmpty()) {
            return extendDispatchTargets
        }

        return lowLevelTargets.map(analysisSession::getPublicSymbol)
    }

    /**
     * extend 成员调用在 low-level `call target` 上可能先落到被实现的接口成员。
     *
     * 为了让引用、导航、查找用法统一指向真正承载实现体的 extend 成员，
     * 这里基于接收者类型把目标回收到对应的 extend declared-member scope。
     */
    private fun restoreExtendDispatchTargets(
        reference: CjReferenceExpression,
        snapshot: CaCfirCallInfoSnapshot,
    ): Collection<CaSymbol> {
        val memberName = (reference as? CjSimpleNameExpression)?.referencedNameAsName
            ?: snapshot.successfulCall?.calleeName
            ?: return emptyList()

        val receiverClassId = snapshot.successfulCall?.explicitReceiverType?.classIdOrPrimitiveClassId
            ?: snapshot.calls.asSequence()
                .mapNotNull { call -> call.explicitReceiverType?.classIdOrPrimitiveClassId }
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
}

/**
 * 对齐 Kotlin `KaFirResolver` 的分层：
 * CFIR resolver 在组件层直接把 low-level 调用 snapshot 转成公开 Analysis API 调用模型，
 * 而不是再额外引入一层独立的 “CallBridge” 文件。
 */
private fun CaCfirCallInfoSnapshot.asAnalysisCallInfo(
    analysisSession: CaCfirSession,
    token: CaLifetimeToken,
): CaCallInfo {
    val mappedCalls = calls.map { callSnapshot -> callSnapshot.asAnalysisCall(analysisSession, token) }
    val mappedSuccessfulCall = successfulCall?.asAnalysisCall(analysisSession, token)
    return CaBaseCallInfo(
        successfulCall = mappedSuccessfulCall,
        calls = mappedCalls,
        token = token,
    )
}

private fun CaCfirCallSnapshot.asAnalysisCall(
    analysisSession: CaCfirSession,
    token: CaLifetimeToken,
): CaCall {
    return CaBaseCall(
        kind = kind.asAnalysisKind(),
        origin = origin.asAnalysisOrigin(),
        applicability = applicability.asAnalysisApplicability(),
        isImplicitInvoke = isImplicitInvoke,
        calleeName = calleeName,
        target = target?.let(analysisSession::getPublicSymbol) as? CaCallableSymbol,
        explicitReceiverType = explicitReceiverType?.asCaType(analysisSession),
        dispatchReceiverType = dispatchReceiverType?.asCaType(analysisSession),
        extensionReceiverType = extensionReceiverType?.asCaType(analysisSession),
        contextArgumentTypes = contextArgumentTypes.map { argumentType -> argumentType?.asCaType(analysisSession) },
        argumentTypes = argumentTypes.map { argumentType -> argumentType?.asCaType(analysisSession) },
        typeArguments = typeArguments.map { typeArgument -> typeArgument?.asCaType(analysisSession) },
        argumentMapping = argumentMapping.map { mapping ->
            mapping.asAnalysisCallArgumentMapping(analysisSession, token)
        },
        token = token,
    )
}

private fun CaCfirCallKind.asAnalysisKind(): CaCallKind = when (this) {
    CaCfirCallKind.FUNCTION -> CaCallKind.FUNCTION
}

private fun CaCfirCallOrigin.asAnalysisOrigin(): CaCallOrigin = when (this) {
    CaCfirCallOrigin.REGULAR -> CaCallOrigin.REGULAR
    CaCfirCallOrigin.OPERATOR -> CaCallOrigin.OPERATOR
    CaCfirCallOrigin.CONSTRUCTOR_DELEGATION_THIS -> CaCallOrigin.CONSTRUCTOR_DELEGATION_THIS
    CaCfirCallOrigin.CONSTRUCTOR_DELEGATION_SUPER -> CaCallOrigin.CONSTRUCTOR_DELEGATION_SUPER
}

private fun CaCfirCallApplicability.asAnalysisApplicability(): CaCallApplicability = when (this) {
    CaCfirCallApplicability.HIDDEN -> CaCallApplicability.HIDDEN
    CaCfirCallApplicability.INAPPLICABLE_WRONG_RECEIVER -> CaCallApplicability.INAPPLICABLE_WRONG_RECEIVER
    CaCfirCallApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR -> CaCallApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR
    CaCfirCallApplicability.INAPPLICABLE -> CaCallApplicability.INAPPLICABLE
    CaCfirCallApplicability.VISIBILITY_ERROR -> CaCallApplicability.VISIBILITY_ERROR
    CaCfirCallApplicability.UNSAFE_CALL -> CaCallApplicability.UNSAFE_CALL
    CaCfirCallApplicability.UNSTABLE_SMARTCAST -> CaCallApplicability.UNSTABLE_SMARTCAST
    CaCfirCallApplicability.CONVENTION_ERROR -> CaCallApplicability.CONVENTION_ERROR
    CaCfirCallApplicability.RESOLVED_LOW_PRIORITY -> CaCallApplicability.RESOLVED_LOW_PRIORITY
    CaCfirCallApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY -> CaCallApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY
    CaCfirCallApplicability.RESOLVED_WITH_ERROR -> CaCallApplicability.RESOLVED_WITH_ERROR
    CaCfirCallApplicability.RESOLVED -> CaCallApplicability.RESOLVED
}

private fun CaCfirCallArgumentMappingSnapshot.asAnalysisCallArgumentMapping(
    analysisSession: CaCfirSession,
    token: CaLifetimeToken,
): CaCallArgumentMapping {
    return CaBaseCallArgumentMapping(
        argumentIndex = argumentIndex,
        parameterName = parameterName,
        parameterType = parameterType?.asCaType(analysisSession),
        token = token,
    )
}
