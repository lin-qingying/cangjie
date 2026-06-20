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
import org.cangnova.cangjie.cfir.resolve.providers.canAccessPackageInternalDeclaration
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
    val declarationContainingFile = session.cfirProvider.getContainingFile(declaration.symbol)

    return when (declaration.status.visibility) {
        Visibilities.Public -> true
        Visibilities.Internal -> {
            canSeePackageInternalDeclaration(useSiteFile, declarationContainingFile)
        }
        Visibilities.Private -> {
            val ownerClassId = declaration.symbol.getOwnerClassId(session.cfirProvider)
            // 对齐官方 TypeCheckUtil::IsLegalAccess：
            // 顶层 private 按同文件可见，成员 private 只能在同一 nominal 声明内部访问。
            when (ownerClassId) {
                null -> canSeePrivateTopLevelDeclarationFromFile(useSiteFile, declarationContainingFile)
                else -> canSeePrivateMemberOf(ownerClassId, containingDeclarations)
            }
        }
        Visibilities.Protected -> {
            declaration.moduleData == session.moduleData ||
                    session.moduleVisibilityChecker?.isInFriendModule(declaration) == true ||
                    declaration.symbol.getOwnerClassId(session.cfirProvider)?.let { ownerClassId ->
                        canSeeProtectedMemberOf(ownerClassId, containingDeclarations)
                    } == true
        }
        else -> visibilityChecker.platformVisibilityCheck(declaration.status.visibility, declaration, candidate)
    }
}

/**
 * 仓颉的 `internal` 按“声明包 + 子包”计算可见性。
 * 这里直接在候选过滤阶段使用共享 helper，
 * 避免 resolve、checker、type-accessibility 再各自复制一套规则。
 */
private fun canSeePackageInternalDeclaration(
    useSiteFile: CfirFile,
    declarationContainingFile: CfirFile?,
): Boolean {
    declarationContainingFile ?: return false
    val declarationPackage = declarationContainingFile.packageDirective.packageFqName
    val useSitePackage = useSiteFile.packageDirective.packageFqName
    return canAccessPackageInternalDeclaration(useSitePackage, declarationPackage)
}

private fun canSeePrivateTopLevelDeclarationFromFile(
    useSiteFile: CfirFile,
    declarationContainingFile: CfirFile?,
): Boolean {
    return useSiteFile == declarationContainingFile
}

private fun canSeePrivateMemberOf(
    ownerClassId: ClassId,
    containingDeclarations: List<CfirDeclaration>,
): Boolean {
    return containingDeclarations.asSequence()
        .filterIsInstance<CfirClassLikeDeclaration>()
        .map { it.symbol.classId }
        .any { it == ownerClassId }
}

private fun canSeeProtectedMemberOf(
    ownerClassId: ClassId,
    containingDeclarations: List<CfirDeclaration>,
): Boolean {
    return containingDeclarations.asSequence()
        .filterIsInstance<CfirClassLikeDeclaration>()
        .map { it.symbol.classId }
        .any { it == ownerClassId }
}
