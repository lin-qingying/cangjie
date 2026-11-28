package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirPhaseUpdater
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpResolver
import org.cangnova.cangjie.cfir.resolve.transformers.CfirCjmpMatcherTransformer
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.CfirCjmpStructureSnapshot
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.session.requiresDeferredCjmpFinalMatching
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef

/** LL 单目标只发布所属 root 的不可变输入；组占用/默认值由显式聚合消费者确定。 */
internal object LLCfirCjmpMatchingLazyResolver : LLCfirLazyResolver(CfirResolvePhase.CJMP_MATCHING) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirCjmpMatchingTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        val declaration = target as? CfirMemberDeclaration ?: return
        if (!declaration.moduleData.session.isCjmpSpecificCompilationEnabled()) return
        if (declaration.moduleData.session.requiresDeferredCjmpFinalMatching()) return
        if (!CfirCjmpMatchRunner.canHaveCommonCounterpart(declaration) ||
            !declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)
        ) return
        val facts = checkNotNull(declaration.cjmpMatchFacts) { "CJMP_MATCHING did not publish root facts" }
        check(facts.ownedSymbols.any { it === declaration.symbol }) { "CJMP result must preserve its real declaration identity" }
        checkNotNull(declaration.cjmpMatchResult) { "CJMP_MATCHING did not publish the declaration-owned result" }
    }
}

private class LLCfirCjmpMatchingTargetResolver(target: LLCfirResolveTarget) : LLCfirTargetResolver(target, CfirResolvePhase.CJMP_MATCHING) {
    private val transformer = CfirCjmpMatcherTransformer(resolveTargetSession, resolveTargetScopeSession)
    private var preparedFacts: CfirCjmpDeclarationMatchFacts? = null
    private var preparedFamilySnapshot: CfirCjmpFamilySnapshot? = null
    private var preparedStructureSnapshot: CfirCjmpStructureSnapshot? = null

    @Deprecated("Only called through withClassLike", level = DeprecationLevel.ERROR)
    override fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        cfirClassLike.lazyResolveToPhase(resolverPhase.previous)
        performResolve(cfirClassLike)
        action()
    }

    @Deprecated("Only called through withExtend", level = DeprecationLevel.ERROR)
    override fun withContainingExtend(extend: CfirExtend, action: () -> Unit) {
        extend.lazyResolveToPhase(resolverPhase.previous)
        performResolve(extend)
        action()
    }

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        if (!resolveTargetSession.isCjmpSpecificCompilationEnabled() || target !is CfirMemberDeclaration) return false
        target.lazyResolveToPhase(resolverPhase.previous)
        if (resolveTargetSession.requiresDeferredCjmpFinalMatching()) {
            if (target !is CfirClassLikeDeclaration && target !is CfirExtend) return false
            preparedStructureSnapshot = resolveTargetSession.cjmpFamilyAggregate.structureFor(target)
            return false
        }
        // 候选阶段推进、extend key 比较和隐式签名解析均在 specific 目标锁外完成。
        val commonCandidates = prepareCjmpCommonCandidates(target, resolveTargetSession)
        val facts = target.cjmpMatchFacts ?: CfirCjmpMatchRunner.prepareDeclaration(
            target,
            resolveTargetSession,
            preparedCommonCandidates = commonCandidates,
        )
        preparedFacts = facts
        preparedFamilySnapshot = if (
            target.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC) &&
            CfirCjmpMatchRunner.canHaveCommonCounterpart(target)
        ) {
            resolveTargetSession.cjmpFamilyAggregate.matchingGroupFor(target, facts)
        } else {
            null
        }
        return false
    }

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        if (!resolveTargetSession.isCjmpSpecificCompilationEnabled() || target !is CfirMemberDeclaration) return
        if (resolveTargetSession.requiresDeferredCjmpFinalMatching()) {
            transformer.transformMemberDeclaration(target, preparedStructureSnapshot = preparedStructureSnapshot)
            return
        }
        transformer.transformMemberDeclaration(
            target,
            checkNotNull(preparedFacts) { "CJMP inputs must be prepared outside the target lock" },
            preparedFamilySnapshot,
        )
    }

    override fun withDeclarationStateUnderLock(target: CfirElementWithResolveState, action: () -> Unit) {
        if (target is CfirMemberDeclaration && resolveTargetSession.isCjmpSpecificCompilationEnabled()) {
            withCjmpDeclarationState(target, action)
        } else action()
    }

}

/** 单目标和 family fallback 共用相同候选筛选及隐式签名阶段，避免两类快照输入不一致。 */
internal fun prepareCjmpCommonCandidates(
    specific: CfirMemberDeclaration,
    session: CfirSession,
    ownerFacts: CfirCjmpDeclarationMatchFacts? = null,
): List<CfirDeclaration> {
    val commonCandidates = CfirCjmpResolver.findCommonCandidatesForDeclaration(specific, session, ownerFacts)
    commonCandidates.filterIsInstance<CfirMemberDeclaration>()
        .filter { it.requiresImplicitSignatureResolution() }
        .forEach { commonDeclaration ->
            commonDeclaration.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
        }
    return commonCandidates
}

private fun CfirMemberDeclaration.requiresImplicitSignatureResolution(): Boolean = when (this) {
    is CfirPatternVariable -> returnTypeRef is CfirImplicitTypeRef ||
            pattern.bindingVariables().any { it.returnTypeRef is CfirImplicitTypeRef }
    is CfirCallableDeclaration -> returnTypeRef is CfirImplicitTypeRef
    else -> false
}

/** 根事实、签名子节点和阶段 updater 共用的可恢复事务。 */
internal fun <Result> withCjmpDeclarationState(
    target: CfirMemberDeclaration,
    action: () -> Result,
): Result = resolveWithKeeper(target, Unit, CJMP_MATCHING_STATE_KEEPER, action = action)

/** 根属性与 updater 可能已更新的签名子节点共同恢复；根阶段由通用锁管理。 */
@OptIn(ResolveStateAccess::class)
private val CJMP_MATCHING_STATE_KEEPER: StateKeeper<CfirMemberDeclaration, Unit> = stateKeeper { builder, target, context ->
    builder.add({ it.cjmpMatchFacts }, { declaration, facts -> declaration.cjmpMatchFacts = facts })
    builder.add({ it.cjmpMatchResult }, { declaration, result -> declaration.cjmpMatchResult = result })
    fun preserveMatchResult(declaration: CfirDeclaration) {
        if (declaration === target) return
        builder.entity(declaration, context) { _, _ ->
            builder.add({ it.cjmpMatchResult }, { owned, result -> owned.cjmpMatchResult = result })
        }
    }
    when (target) {
        is CfirProperty -> listOfNotNull(target.getter, target.setter).forEach(::preserveMatchResult)
        is CfirPatternVariable -> target.pattern.bindingVariables().forEach(::preserveMatchResult)
        else -> Unit
    }
    fun preserveInternals(element: CfirElementWithResolveState) {
        LLCfirPhaseUpdater.forEachSignatureInternal(element) { child ->
            builder.entity(child, context) { _, _ ->
                builder.add({ it.resolveState }, { internal, state -> internal.resolveState = state })
            }
            preserveInternals(child)
        }
    }
    preserveInternals(target)
}
