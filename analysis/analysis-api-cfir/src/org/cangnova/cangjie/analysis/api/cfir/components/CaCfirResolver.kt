package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.getExtendPublicSymbols
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirBackedSymbol
import org.cangnova.cangjie.analysis.api.components.CaResolver
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
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
        analysisSession.diagnosticQueries.queryCallInfo(this@resolveToCall)
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
            .mapNotNull(analysisSession.diagnosticQueries::queryCallInfo)
            .firstOrNull { resolvedCallInfo ->
                resolvedCallInfo.successfulCall?.target != null || resolvedCallInfo.calls.any { call -> call.target != null }
            }
            ?: return emptyList()

        val lowLevelTargets = buildList {
            callInfo.successfulCall?.target?.let(::add)
            callInfo.calls.mapNotNullTo(this) { call -> call.target }
        }

        val extendDispatchTargets = restoreExtendDispatchTargets(reference, callInfo)
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
        callInfo: CaCallInfo,
    ): Collection<CaSymbol> {
        val memberName = (reference as? CjSimpleNameExpression)?.referencedNameAsName
            ?: callInfo.successfulCall?.calleeName
            ?: return emptyList()

        val receiverClassId = callInfo.successfulCall?.explicitReceiverType?.classIdOrPrimitiveClassId
            ?: callInfo.calls.asSequence()
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
