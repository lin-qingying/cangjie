package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.cfir.resolve.transformers.CfirCjmpMatcherTransformer
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.session.requiresDeferredCjmpFinalMatching
import org.cangnova.cangjie.cfir.session.getContainingCjmpDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase

/** alpha.66 在稳定签名上完成最终 CJMP 匹配；所有 sibling 推进和快照准备均在目标写锁外执行。 */
internal object LLCfirCjmpFinalMatchingLazyResolver : LLCfirLazyResolver(CfirResolvePhase.CJMP_FINAL_MATCHING) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver =
        LLCfirCjmpFinalMatchingTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        val declaration = target as? CfirMemberDeclaration ?: return
        // Root 验证全部配对事实（包括依规则停止前进的叶子），叶子不另建 root facts。
        if (declaration is CfirPatternBindingVariable && declaration.patternTreeOwner is CfirPatternVariable) return
        val session = declaration.moduleData.session
        if (!session.requiresDeferredCjmpFinalMatching() ||
            !declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC) ||
            !CfirCjmpMatchRunner.canHaveCommonCounterpart(declaration)
        ) return
        val facts = checkNotNull(declaration.cjmpMatchFacts) { "CJMP_FINAL_MATCHING did not publish root facts" }
        check(facts.ownedSymbols.any { it === declaration.symbol }) { "CJMP result must preserve its real declaration identity" }
        checkNotNull(declaration.cjmpMatchResult) { "CJMP_FINAL_MATCHING did not publish the declaration-owned result" }
    }
}

private class LLCfirCjmpFinalMatchingTargetResolver(target: LLCfirResolveTarget) :
    LLCfirTargetResolver(target, CfirResolvePhase.CJMP_FINAL_MATCHING) {
    private val transformer = CfirCjmpMatcherTransformer(
        resolveTargetSession,
        resolveTargetScopeSession,
        CfirResolvePhase.CJMP_FINAL_MATCHING,
    )
    private var preparedFacts: CfirCjmpDeclarationMatchFacts? = null
    private var preparedFamilySnapshot: org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot? = null

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        if (!resolveTargetSession.requiresDeferredCjmpFinalMatching() || target !is CfirMemberDeclaration) return false
        // resolveDependencies 已经通过原模式根完成实际配对及所有叶子的阶段发布。
        if (target is CfirPatternBindingVariable && target.resolvePhase >= resolverPhase) return true
        target.lazyResolveToPhase(resolverPhase.previous)

        val structure = resolveTargetSession.cjmpFamilyAggregate.structureFor(target)
        val signatureRoots = structure.topLevelDeclarationsForMatching() + target
        val visited = HashSet<CfirBasedSymbol<*>>()
        fun resolveSignatureTree(declaration: CfirDeclaration) {
            if (!visited.add(declaration.symbol)) return
            if (declaration is CfirMemberDeclaration &&
                declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)
            ) {
                declaration.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
            }
            when (declaration) {
                is CfirClassLikeDeclaration -> declaration.declarations.forEach(::resolveSignatureTree)
                is CfirExtend -> declaration.declarations.forEach(::resolveSignatureTree)
                is CfirProperty -> listOfNotNull(declaration.getter, declaration.setter).forEach(::resolveSignatureTree)
                is CfirPatternVariable -> declaration.pattern.bindingVariables().forEach(::resolveSignatureTree)
                else -> Unit
            }
        }
        signatureRoots.forEach(::resolveSignatureTree)

        val owner = (target as? CfirCallableDeclaration)?.getContainingCjmpDeclaration()
        val ownerFacts = owner?.let {
            CfirCjmpMatchRunner.prepareDeclaration(
                checkNotNull(it as? CfirMemberDeclaration) { "A callable owner must be a CJMP declaration" },
                resolveTargetSession,
            )
        }
        val commonCandidates = prepareCjmpCommonCandidates(target, resolveTargetSession, ownerFacts)
        val facts = target.cjmpMatchFacts ?: CfirCjmpMatchRunner.prepareDeclaration(
            target,
            resolveTargetSession,
            preparedOwnerFacts = ownerFacts,
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
        if (!resolveTargetSession.requiresDeferredCjmpFinalMatching() || target !is CfirMemberDeclaration) return
        transformer.transformMemberDeclaration(
            target,
            checkNotNull(preparedFacts) { "CJMP final inputs must be prepared outside the target lock" },
            preparedFamilySnapshot,
        )
    }

    override fun withDeclarationStateUnderLock(target: CfirElementWithResolveState, action: () -> Unit) {
        if (target is CfirMemberDeclaration && resolveTargetSession.requiresDeferredCjmpFinalMatching()) {
            withCjmpDeclarationState(target, action)
        } else {
            action()
        }
    }
}
