package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.visibility.CfirVisibilityChecker
import org.cangnova.cangjie.cfir.resolve.calls.visibility.getOwnerClassId
import org.cangnova.cangjie.cfir.resolve.calls.visibility.moduleVisibilityChecker
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId

fun isVisible(
    visibilityChecker: CfirVisibilityChecker,
    declaration: CfirMemberDeclaration,
    candidate: Candidate,
): Boolean {
    val session = candidate.callInfo.session
    val useSiteFile = candidate.callInfo.containingFile
    val containingDeclarations = candidate.callInfo.containingDeclarations

    return when (declaration.status.visibility) {
        Visibilities.Public -> true
        Visibilities.Internal -> {
            declaration.moduleData == session.moduleData || session.moduleVisibilityChecker?.isInFriendModule(declaration) == true
        }
        Visibilities.Private -> {
            val ownerClassId = declaration.symbol.getOwnerClassId(session.cfirProvider)
            when (ownerClassId) {
                null -> canSeePrivateTopLevelDeclarationFromFile(session, useSiteFile, declaration)
                else -> canSeePrivateMemberOf(ownerClassId, containingDeclarations)
            }
        }
        Visibilities.Protected -> {
            val ownerClassId = declaration.symbol.getOwnerClassId(session.cfirProvider) ?: return false
            canSeeProtectedMemberOf(ownerClassId, containingDeclarations)
        }
        else -> visibilityChecker.platformVisibilityCheck(declaration.status.visibility, declaration, candidate)
    }
}

private fun canSeePrivateTopLevelDeclarationFromFile(
    session: org.cangnova.cangjie.cfir.session.CfirSession,
    useSiteFile: CfirFile,
    declaration: CfirMemberDeclaration,
): Boolean {
    val declarationContainingFile = session.cfirProvider.getContainingFile(declaration.symbol) ?: return false
    return useSiteFile == declarationContainingFile
}

private fun canSeePrivateMemberOf(
    ownerClassId: ClassId,
    containingDeclarations: List<CfirDeclaration>,
): Boolean {
    return containingDeclarations.asSequence()
        .filterIsInstance<CfirClassLikeDeclaration>()
        .map { it.symbol.classId }
        .any { it == ownerClassId || it.isNestedWithin(ownerClassId) }
}

private fun canSeeProtectedMemberOf(
    ownerClassId: ClassId,
    containingDeclarations: List<CfirDeclaration>,
): Boolean {
    return containingDeclarations.asSequence()
        .filterIsInstance<CfirClassLikeDeclaration>()
        .map { it.symbol.classId }
        .any { it == ownerClassId || it.isNestedWithin(ownerClassId) }
}

private fun ClassId.isNestedWithin(owner: ClassId): Boolean {
    var current: ClassId? = this
    while (current != null) {
        if (current == owner) return true
        current = current.outerClassId
    }
    return false
}
