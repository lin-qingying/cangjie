package org.cangnova.cangjie.analysis.api.cfir.resolve

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFileDeclaredTopLevelScope
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.psi
import java.util.concurrent.ConcurrentHashMap

/**
 * low-level 真实作用域提供器。
 *
 * 这里直接暴露底层 `CfirContainingNamesAwareScope` / `CfirTypeScope`，
 * 不再引入 `snapshot` 协议层。上层 Analysis API 只负责把真实 scope
 * 映射为公开 `CaScope` 视图。
 */
internal class CaCfirScopeProvider(
    private val moduleResolveComponents: CaCfirModuleResolveComponents,
) {
    private val scopeSession = ScopeSession()

    private val fileDeclaredScopeCache = ConcurrentHashMap<CjFile, CfirContainingNamesAwareScope>()
    private val packageScopeCache = ConcurrentHashMap<FqName, CfirContainingNamesAwareScope>()
    private val declaredMemberScopeCache = ConcurrentHashMap<ClassId, CfirContainingNamesAwareScope>()
    private val memberScopeCache = ConcurrentHashMap<ClassId, CfirTypeScope>()

    fun getFileDeclaredScope(file: CjFile): CfirContainingNamesAwareScope {
        return fileDeclaredScopeCache.computeIfAbsent(file) { targetFile ->
            val cfirFile = cfirFiles.firstOrNull { candidate ->
                (candidate.source?.psi as? CjFile) == targetFile
            } ?: error("Cannot find CFIR file for `${targetFile.name}`")
            CfirFileDeclaredTopLevelScope(cfirFile)
        }
    }

    fun getPackageScope(packageFqName: FqName): CfirContainingNamesAwareScope? {
        packageScopeCache[packageFqName]?.let { return it }

        val packageScope = session.cangjieScopeProvider.getPackageMemberScope(
            packageFqName = packageFqName,
            symbolProvider = symbolProvider,
            useSiteSession = session,
            scopeSession = scopeSession,
        )
        val hasVisiblePackage = visibleSymbolProvider.hasPackage(packageFqName)
        val hasMembers = packageScope.getCallableNames().isNotEmpty() || packageScope.getClassifierNames().isNotEmpty()
        if (!hasVisiblePackage && !hasMembers) {
            return null
        }

        val existing = packageScopeCache.putIfAbsent(packageFqName, packageScope)
        return existing ?: packageScope
    }

    fun getDeclaredMemberScope(classId: ClassId): CfirContainingNamesAwareScope? {
        declaredMemberScopeCache[classId]?.let { return it }

        val classSymbol = visibleSymbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        val scope = scopeSession.getOrBuild(classId, DeclaredMemberScopeKey) {
            CfirClassDeclaredMemberScope(classSymbol)
        }
        val existing = declaredMemberScopeCache.putIfAbsent(classId, scope)
        return existing ?: scope
    }

    /**
     * 返回 use-site member scope 的底层 `CfirTypeScope`。
     *
     * override / relation 查询与公开 `memberScope` 必须共享同一份底层 scope，
     * 因此这里直接缓存真实 `CfirTypeScope`。
     */
    fun getMemberScope(classId: ClassId): CfirTypeScope? {
        memberScopeCache[classId]?.let { return it }

        val classSymbol = visibleSymbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        val scope = scopeSession.getOrBuild(classId, UseSiteMemberScopeKey) {
            CfirClassUseSiteMemberScope(
                classSymbol = classSymbol,
                symbolProvider = symbolProvider,
                extendProvider = session.extendProviderOrNull,
                directSupertypeProvider = session.directSupertypeProviderOrNull,
            )
        }
        val existing = memberScopeCache.putIfAbsent(classId, scope)
        return existing ?: scope
    }

    /**
     * 为类型恢复对应的底层 `CfirTypeScope`。
     *
     * 当前仓颉只为能够稳定映射到 class-like 的类型公开成员作用域，
     * 因此这里仍以 `ClassId` 作为恢复锚点。
     */
    fun getTypeScope(type: ConeCangJieType): CfirTypeScope? {
        val classId = type.classIdOrPrimitiveClassId ?: return null
        return getMemberScope(classId)
    }

    private val session
        get() = moduleResolveComponents.session

    private val cfirFiles
        get() = moduleResolveComponents.cfirFiles

    private val visibleSymbolProvider
        get() = moduleResolveComponents.visibleSymbolProvider

    private val symbolProvider
        get() = session.symbolProvider

    private object DeclaredMemberScopeKey : ScopeSessionKey<ClassId, CfirClassDeclaredMemberScope>()

    private object UseSiteMemberScopeKey : ScopeSessionKey<ClassId, CfirClassUseSiteMemberScope>()
}
