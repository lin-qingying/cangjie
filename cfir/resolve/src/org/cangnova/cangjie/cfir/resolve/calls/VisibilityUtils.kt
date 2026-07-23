package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.visibility.CfirVisibilityChecker
import org.cangnova.cangjie.cfir.resolve.calls.visibility.getOwnerClassId
import org.cangnova.cangjie.cfir.resolve.calls.visibility.moduleVisibilityChecker
import org.cangnova.cangjie.cfir.resolve.providers.canAccessPackageInternalDeclaration
import org.cangnova.cangjie.cfir.resolve.providers.canAccessPackageProtectedDeclaration
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId

/**
 * 判断成员声明对当前调用候选是否可见。
 */
fun isVisible(
    visibilityChecker: CfirVisibilityChecker,
    declaration: CfirMemberDeclaration,
    candidate: Candidate,
): Boolean {
    val session = candidate.callInfo.session
    val useSiteFile = candidate.callInfo.containingFile
    val containingDeclarations = candidate.callInfo.containingDeclarations
    /*
     * synthetic typealias constructor 没有注册在 provider 文件索引中；其可见性 owner、
     * 声明文件和 nominal owner 都来自展开类型的原始构造器。若继续用 synthetic symbol，
     * internal/package 可见构造器会因 containing file 缺失被错误隐藏。
     */
    val visibilityOwner = (declaration as? CfirConstructor)
        ?.typeAliasConstructorInfo
        ?.originalConstructor
        ?: declaration
    val declarationContainingFile = visibilityOwner.symbol.getContainingFile()

    return when (visibilityOwner.status.visibility) {
        Visibilities.Public -> true
        Visibilities.Internal -> {
            canSeePackageInternalDeclaration(useSiteFile, declarationContainingFile)
        }
        Visibilities.Private -> {
            val ownerExtend = visibilityOwner.containingExtendOrNull()
            if (ownerExtend != null) {
                return canSeePrivateExtendMemberOf(ownerExtend, containingDeclarations)
            }

            val ownerClassId = visibilityOwner.symbol.getOwnerClassId()
            // 对齐官方 TypeCheckUtil::IsLegalAccess：
            // 顶层 private 按同文件可见，成员 private 只能在同一 nominal 声明内部访问。
            when (ownerClassId) {
                null -> canSeePrivateTopLevelDeclarationFromFile(useSiteFile, declarationContainingFile)
                else -> canSeePrivateMemberOf(ownerClassId, containingDeclarations)
            }
        }
        Visibilities.Protected -> {
            canSeePackageProtectedDeclaration(useSiteFile, declarationContainingFile) ||
                    visibilityOwner.protectedOwnerClassId()?.let { ownerClassId ->
                        canSeeProtectedMemberOf(ownerClassId, containingDeclarations, session)
                    } == true ||
                    visibilityOwner.containingExtendOrNull()?.let { ownerExtend ->
                        canSeePrivateExtendMemberOf(ownerExtend, containingDeclarations)
                    } == true
        }
        else -> visibilityChecker.platformVisibilityCheck(visibilityOwner.status.visibility, visibilityOwner, candidate)
    }
}

/**
 * extend 私有成员的 owner 是 extend 声明体本身，不是目标类型，也不是所在文件。
 *
 * 官方 `TypeCheckUtil::IsLegalAccess` 对成员 private 要求当前 composite
 * 与目标 outer decl 相同；仓颉 extend body 在 CFIR 中以 [CfirExtend] 容器表达，
 * 因此这里直接用 provider 的 owner extend 索引判断同一声明体。
 */
private fun CfirMemberDeclaration.containingExtendOrNull(
): CfirExtend? {
    val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return null
    return callableSymbol.getContainingExtend()
}

/**
 * 判断当前位置是否处于指定 extend 声明体内部。
 */
private fun canSeePrivateExtendMemberOf(
    ownerExtend: CfirExtend,
    containingDeclarations: List<CfirDeclaration>,
): Boolean {
    return containingDeclarations.any { it === ownerExtend }
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

/**
 * protected 的包级关系使用官方 Modules::GetPackageRelation 语义，
 * 不能退化成 CFIR session/moduleData 等构建系统概念。
 */
private fun canSeePackageProtectedDeclaration(
    useSiteFile: CfirFile,
    declarationContainingFile: CfirFile?,
): Boolean {
    declarationContainingFile ?: return false
    val declarationPackage = declarationContainingFile.packageDirective.packageFqName
    val useSitePackage = useSiteFile.packageDirective.packageFqName
    return canAccessPackageProtectedDeclaration(useSitePackage, declarationPackage)
}

/**
 * 判断顶层 private 声明是否与使用点位于同一文件。
 */
private fun canSeePrivateTopLevelDeclarationFromFile(
    useSiteFile: CfirFile,
    declarationContainingFile: CfirFile?,
): Boolean {
    return useSiteFile == declarationContainingFile
}

/**
 * 判断当前位置是否处于指定 owner class 内部。
 */
private fun canSeePrivateMemberOf(
    ownerClassId: ClassId,
    containingDeclarations: List<CfirDeclaration>,
): Boolean {
    return containingDeclarations.asSequence()
        .filterIsInstance<CfirClassLikeDeclaration>()
        .map { it.symbol.classId }
        .any { it == ownerClassId }
}

/**
 * 返回 protected 成员可见性检查使用的 owner classId。
 */
private fun CfirMemberDeclaration.protectedOwnerClassId(): ClassId? {
    symbol.getOwnerClassId()?.let { return it }
    return containingExtendOrNull()
        ?.extendedTypeRef
        ?.coneTypeOrNull
        ?.classIdOrPrimitiveClassId
}

/**
 * 判断当前位置是否可以通过继承关系访问 protected 成员。
 */
private fun canSeeProtectedMemberOf(
    ownerClassId: ClassId,
    containingDeclarations: List<CfirDeclaration>,
    session: CfirSession,
): Boolean {
    return containingDeclarations.asReversed().asSequence()
        .filterIsInstance<CfirClassLikeDeclaration>()
        .mapNotNull { it.symbol as? CfirClassLikeSymbol<*> }
        .any { currentClass ->
            currentClass.classId == ownerClassId ||
                    currentClass.constructType().hasSupertypeWithClassId(ownerClassId, session)
        }
}

/**
 * 判断类型是否具有指定 classId 的父类型。
 */
private fun ConeCangJieType.hasSupertypeWithClassId(
    ownerClassId: ClassId,
    session: CfirSession,
): Boolean {
    val supertypeProvider = session.typeAwareSupertypeProviderOrNull ?: return false
    val visited = linkedSetOf<ConeCangJieType>()
    val queue = ArrayDeque<ConeCangJieType>()
    queue += this

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!visited.add(current)) continue
        if (current.classIdOrPrimitiveClassId == ownerClassId) return true
        queue.addAll(supertypeProvider.getDirectSupertypes(current))
    }
    return false
}
