package org.cangnova.cangjie.analysis.api.cfir

import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacade
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirScopeSnapshot
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR 会话内的作用域查询服务。
 *
 * 文件、包、声明成员、use-site 成员以及类型作用域都统一经由这一层访问，
 * 以保持 `analysis-api-cfir` 对 low-level scope 入口的单一依赖。
 */
internal class CaCfirSessionScopeQueryService(
    private val resolutionFacade: CaCfirResolutionFacade,
    private val cacheStore: CaCfirSessionCacheStore,
) {
    fun queryFileScope(file: CjFile): CaCfirScopeSnapshot =
        cacheStore.getOrCreateFileScope(file) { resolutionFacade.getFileScope(file) }

    fun queryPackageScope(packageFqName: FqName): CaCfirScopeSnapshot? =
        cacheStore.getOrCreatePackageScope(packageFqName) {
            resolutionFacade.getPackageScope(packageFqName)
        }

    fun queryDeclaredMemberScope(classId: ClassId): CaCfirScopeSnapshot? =
        cacheStore.getOrCreateDeclaredMemberScope(classId) {
            resolutionFacade.getDeclaredMemberScope(classId)
        }

    fun queryMemberScope(classId: ClassId): CaCfirScopeSnapshot? =
        cacheStore.getOrCreateMemberScope(classId) {
            resolutionFacade.getMemberScope(classId)
        }

    fun queryTypeScope(type: ConeCangJieType): CaCfirScopeSnapshot? =
        cacheStore.getOrCreateTypeScope(type) {
            resolutionFacade.getTypeScope(type)
        }

    fun hasVisiblePackage(packageFqName: FqName): Boolean =
        cacheStore.getOrCreatePackageVisibility(packageFqName) {
            resolutionFacade.hasPackage(packageFqName)
        }
}
