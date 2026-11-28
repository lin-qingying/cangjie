package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.classifyExtendPeerPackageRelation
import org.cangnova.cangjie.cfir.resolve.providers.memberOwnerAccessContext
import org.cangnova.cangjie.cfir.session.extendRuleQueryService
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerDisposition
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 官方 UpdateExtendMap 的实例化 owner 输入：有真实 specificImplementation 的 common
 * extend 由合并后的 specific owner 代表。函数名字或物理 same-key 不构成删除依据。
 */
fun CfirSession.instantiationExtendOwners(extends: List<CfirExtend>): List<CfirExtend> {
    if (!isCjmpSpecificCompilationEnabled()) return extends
    val replaced = extends.asSequence()
        .filter { it.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC) }
        .flatMap { specific -> cjmpFamilyAggregate.matchingGroupFor(specific).commonCounterpartsFor(specific).asSequence() }
        .filterIsInstance<CfirExtend>()
        .map { it.symbol }
        .toSet()
    return extends.filterNot { it.symbol in replaced }
}

/** 实例化补入只取实际合并声明体，不把已替换成员从物理 declarations 再次补回。 */
fun CfirSession.mergedMemberDeclarationsForInstantiation(owner: CfirDeclaration): List<CfirDeclaration> {
    if (isCjmpSpecificCompilationEnabled() && owner.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)) {
        return cjmpFamilyAggregate.matchingGroupFor(owner).mergedDeclarationsFor(owner)
    }
    return when (owner) {
        is CfirClassLikeDeclaration -> owner.declarations
        is CfirExtend -> owner.declarations
        else -> error("Instantiation members require a nominal or extend owner")
    }
}

/** GetVisibleExtendMembersForExtend 的共享 owner/peer 输入，声明检查与成员表装配使用同一分类。 */
fun CfirSession.instantiationRelatedMemberExtends(
    owner: CfirExtend,
    directTargetExtends: List<CfirExtend>,
    currentAccessContext: CfirAccessContext,
    symbolProvider: CfirSymbolProvider = this.symbolProvider,
    expandedConeType: (CfirTypeAlias) -> ConeCangJieType? = { it.expandedTypeRef.coneTypeOrNull },
): Set<CfirExtend> {
    val ownerTargetType = owner.extendedTypeRef.coneTypeOrNull ?: return emptySet()
    val matchedPeers = instantiationExtendOwners(directTargetExtends).filter { peer -> peer === owner ||
        peer.extendedTypeRef.coneTypeOrNull?.let { pattern ->
            createExtendDeclarationSubstitution(this, peer, pattern, ownerTargetType, symbolProvider, expandedConeType)
        } != null
    }
    return instantiationRelatedPreparedMemberExtends(owner, matchedPeers, currentAccessContext) { peer ->
        extendRuleQueryService.extendMemberPeerDecision(owner, peer).disposition
    }
}

/** 已完成目标匹配的 peer 输入；访问上下文仍由当前使用处提供，不在查询时重新读取 alias。 */
fun CfirSession.instantiationRelatedPreparedMemberExtends(
    owner: CfirExtend,
    matchedPeers: List<CfirExtend>,
    currentAccessContext: CfirAccessContext,
    disposition: (CfirExtend) -> CfirExtendMemberPeerDisposition,
): Set<CfirExtend> {
    val ownerAccessContext = memberOwnerAccessContext(owner)
    return buildSet {
        add(owner)
        for (peer in matchedPeers) {
            if (peer === owner) continue
            val packageRelation = classifyExtendPeerPackageRelation(owner, peer, currentAccessContext, ownerAccessContext)
            if (!packageRelation.appliesInterfaceOrdering) {
                if (packageRelation.peerAccessibleFromOwner) add(peer)
                continue
            }
            if (!packageRelation.bothExternalAndPeerVisible && !packageRelation.peerAccessibleFromOwner) continue
            when (disposition(peer)) {
                CfirExtendMemberPeerDisposition.EXCLUDE -> continue
                CfirExtendMemberPeerDisposition.INCLUDE,
                CfirExtendMemberPeerDisposition.UNDECIDABLE -> add(peer)
            }
        }
    }
}
