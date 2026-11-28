/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * 两个 extend 是否按接口继承顺序过滤，由包归属和 use-site 可见性共同决定。
 *
 * 只有同包扩展，或消费包可见的两个外部包扩展，才应用接口先后关系；其它跨包扩展
 * 按声明 owner 的文件可见性收集，不应用接口顺序过滤。
 */
data class CfirExtendPeerPackageRelation(
    val areInSamePackage: Boolean,
    val bothExternalAndPeerVisible: Boolean,
    val peerAccessibleFromOwner: Boolean,
) {
    val appliesInterfaceOrdering: Boolean
        get() = areInSamePackage || bothExternalAndPeerVisible
}

/**
 * 按官方 `GetVisibleExtendMembersForExtend` 的包分支分类 owner/peer 扩展关系。
 *
 * 复用该结果可确保声明期 shadow 检查和泛型实例化成员图对跨包 peer 使用同一规则。
 */
fun CfirSession.classifyExtendPeerPackageRelation(
    owner: CfirExtend,
    peer: CfirExtend,
    currentAccessContext: CfirAccessContext,
    ownerAccessContext: CfirAccessContext?,
): CfirExtendPeerPackageRelation {
    val ownerPackage = extendProvider.getPackageFqName(owner)
    val peerPackage = extendProvider.getPackageFqName(peer)
    val currentPackage = currentAccessContext.useSiteFile?.packageDirective?.packageFqName
    val areInSamePackage = ownerPackage != null && peerPackage == ownerPackage
    val isCjmpCommonPeer = isCjmpSpecificCompilationEnabled() &&
        owner.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC) &&
        peer.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON)
    val bothExternalPackages = currentPackage != null &&
        ownerPackage != null && peerPackage != null &&
        ownerPackage != currentPackage && peerPackage != currentPackage
    val bothExternalAndPeerVisible = bothExternalPackages && if (isCjmpCommonPeer) {
        peer.status.visibility.isPublicAPI
    } else {
        accessibilityChecker.checkExtend(peer, currentAccessContext) is CfirAccessibilityResult.Accessible
    }
    val peerAccessibleFromOwner = when {
        // common peer 属于独立 source session；同包规则可直接判定，不能用 specific 文件导入表检查 common 声明。
        isCjmpCommonPeer && areInSamePackage -> true
        ownerAccessContext != null -> accessibilityChecker.checkExtend(peer, ownerAccessContext) is
                CfirAccessibilityResult.Accessible
        else -> areInSamePackage || peer.status.visibility.isPublicAPI
    }

    return CfirExtendPeerPackageRelation(
        areInSamePackage = areInSamePackage,
        bothExternalAndPeerVisible = bothExternalAndPeerVisible,
        peerAccessibleFromOwner = peerAccessibleFromOwner,
    )
}

/** 在 owner extend 自身的文件与声明链中创建可见性上下文。 */
fun CfirSession.memberOwnerAccessContext(extend: CfirExtend): CfirAccessContext? {
    val ownerFile = extendProvider.getContainingFile(extend) ?: return null
    return CfirAccessContext(
        useSiteFile = ownerFile,
        containingDeclarations = listOf(extend),
        receiverType = extend.extendedTypeRef.coneTypeOrNull,
        lookupOrigin = CfirLookupOrigin.MEMBER,
        kind = CfirAccessKind.EXTEND,
    )
}
