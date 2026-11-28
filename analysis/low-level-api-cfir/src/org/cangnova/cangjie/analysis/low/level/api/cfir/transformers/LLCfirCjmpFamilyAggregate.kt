package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCjmpDeclarationMatchFacts
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.session.CfirCjmpBatchFamilyAggregate
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.cfir.session.CfirCjmpMatchingMode
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.CfirCjmpStructureSnapshot
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cjmpSpecificDeclarationsInPackage
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.requiresDeferredCjmpFinalMatching
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.name.FqName

/**
 * LL 保留 specific-compilation 模式门禁；组发现、包级完成协调、冻结事实和有序求值
 * 统一委托给 CFIR shared aggregate，确保两个入口共用同一并发与取消语义。
 */
internal class LLCfirCjmpFamilyAggregate(
    private val session: CfirSession,
) : CfirCjmpFamilyAggregate {
    private val delegate = CfirCjmpBatchFamilyAggregate(session) { declaration, ownerFacts, matchingMode ->
        prepareDeclaration(declaration, ownerFacts, matchingMode)
    }

    private fun prepareDeclaration(
        declaration: CfirMemberDeclaration,
        ownerFacts: CfirCjmpDeclarationMatchFacts?,
        matchingMode: CfirCjmpMatchingMode,
    ): CfirCjmpDeclarationMatchFacts {
        declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val commonCandidates = prepareCjmpCommonCandidates(declaration, session, ownerFacts)
        return CfirCjmpMatchRunner.prepareDeclaration(declaration, session, ownerFacts, commonCandidates, matchingMode)
    }

    override fun structureFor(declaration: CfirDeclaration): CfirCjmpStructureSnapshot =
        delegate.structureFor(declaration)

    override fun matchingGroupFor(
        declaration: CfirDeclaration,
        declarationFacts: CfirCjmpDeclarationMatchFacts?,
        matchingMode: CfirCjmpMatchingMode,
    ): CfirCjmpFamilySnapshot {
        if (declarationFacts == null && matchingMode == CfirCjmpMatchingMode.FINAL &&
            session.requiresDeferredCjmpFinalMatching()
        ) {
            resolveSpecificFinalMatchingInGroup(declaration)
        }
        return delegate.matchingGroupFor(declaration, declarationFacts, matchingMode)
    }

    override fun defaultParameterStatesForBootstrap(declaration: CfirDeclaration) =
        delegate.defaultParameterStatesForBootstrap(declaration)

    override fun ensureFamilyComplete(packageFqName: FqName): CfirCjmpFamilySnapshot {
        check(session.isCjmpSpecificCompilationEnabled()) {
            "CJMP family completion requires a specific-compilation session"
        }
        if (session.requiresDeferredCjmpFinalMatching()) {
            resolveSpecificFinalMatchingDeclarations(session.cjmpSpecificDeclarationsInPackage(packageFqName))
        }
        return delegate.ensureFamilyComplete(packageFqName)
    }

    /**
     * 任一 FINAL 组快照都必须由 CJMP_FINAL_MATCHING 阶段先稳定组内 specific 签名并发布声明事实。
     * 该阶段推进发生在 provider 聚合启动 single-flight 计算之前，避免缓存 Quest 状态下的临时配对。
     */
    private fun resolveSpecificFinalMatchingInGroup(declaration: CfirDeclaration) {
        val structure = delegate.structureFor(declaration)
        resolveSpecificFinalMatchingDeclarations(structure.topLevelDeclarationsForMatching() + declaration)
    }
}

private fun resolveSpecificFinalMatchingDeclarations(declarations: Iterable<CfirDeclaration>) {
    val visited = HashSet<CfirBasedSymbol<*>>()
    fun resolveTree(declaration: CfirDeclaration) {
        if (!visited.add(declaration.symbol)) return
        if (declaration is CfirMemberDeclaration &&
            declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)
        ) {
            declaration.lazyResolveToPhase(CfirResolvePhase.CJMP_FINAL_MATCHING)
        }
        when (declaration) {
            is CfirClassLikeDeclaration -> declaration.declarations.forEach(::resolveTree)
            is CfirExtend -> declaration.declarations.forEach(::resolveTree)
            is CfirProperty -> listOfNotNull(declaration.getter, declaration.setter).forEach(::resolveTree)
            is CfirPatternVariable -> declaration.pattern.bindingVariables().forEach(::resolveTree)
            else -> Unit
        }
    }
    declarations.forEach(::resolveTree)
}
