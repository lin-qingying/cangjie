package org.cangnova.cangjie.analysis.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
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
    private val resolutionFacade: LLResolutionFacade,
    private val cacheStore: CaCfirSessionCacheStore,
) {
    fun queryFileDeclaredScope(file: CjFile): CfirContainingNamesAwareScope =
        cacheStore.getOrCreateFileDeclaredScope(file) { resolutionFacade.getFileDeclaredScope(file) }

    fun queryPackageScope(packageFqName: FqName): CfirContainingNamesAwareScope? =
        cacheStore.getOrCreatePackageScope(packageFqName) {
            resolutionFacade.getPackageScope(packageFqName)
        }

    fun queryDeclaredMemberScope(classId: ClassId): CfirContainingNamesAwareScope? =
        cacheStore.getOrCreateDeclaredMemberScope(classId) {
            resolutionFacade.getDeclaredMemberScope(classId)
        }

    fun queryMemberScope(classId: ClassId): CfirTypeScope? =
        cacheStore.getOrCreateMemberScope(classId) {
            resolutionFacade.getMemberScope(classId)
        }

    fun queryTypeScope(type: ConeCangJieType): CfirTypeScope? =
        cacheStore.getOrCreateTypeScope(type) {
            resolutionFacade.getTypeScope(type)
        }

    fun hasVisiblePackage(packageFqName: FqName): Boolean =
        cacheStore.getOrCreatePackageVisibility(packageFqName) {
            resolutionFacade.hasPackage(packageFqName)
        }
}
