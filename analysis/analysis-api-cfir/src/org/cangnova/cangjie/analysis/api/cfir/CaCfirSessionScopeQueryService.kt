package org.cangnova.cangjie.analysis.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirCompositeTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFileDeclaredTopLevelScope
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR 会话内的作用域查询服务。
 *
 * 这里严格只组合仓库中真实存在的 CFIR scope 构件：
 * - 文件 scope 走 `CfirFileDeclaredTopLevelScope`
 * - 包 scope 走 `CfirScopeProvider.getPackageMemberScope`
 * - 成员 scope 走 `CfirClassDeclaredMemberScope` / `CfirClassUseSiteMemberScope`
 * - 类型 scope 复用接收者建 scope 的同一套类型展开策略
 */
internal class CaCfirSessionScopeQueryService(
    private val resolutionFacade: LLResolutionFacade,
    private val cacheStore: CaCfirSessionCacheStore,
) {
    private val useSiteSession get() = resolutionFacade.useSiteCfirSession
    private val scopeSession get() = resolutionFacade.getScopeSessionFor(useSiteSession)

    fun queryFileDeclaredScope(file: CjFile): CfirContainingNamesAwareScope =
        cacheStore.getOrCreateFileDeclaredScope(file) {
            CfirFileDeclaredTopLevelScope(file.getOrBuildCfirFile(resolutionFacade))
        }

    fun queryPackageScope(packageFqName: FqName): CfirContainingNamesAwareScope? =
        cacheStore.getOrCreatePackageScope(packageFqName) {
            if (!useSiteSession.symbolProvider.hasPackage(packageFqName)) {
                return@getOrCreatePackageScope null
            }
            useSiteSession.cangjieScopeProvider.getPackageMemberScope(
                packageFqName = packageFqName,
                symbolProvider = useSiteSession.symbolProvider,
                useSiteSession = useSiteSession,
                scopeSession = scopeSession,
            )
        }

    fun queryDeclaredMemberScope(classId: ClassId): CfirContainingNamesAwareScope? =
        cacheStore.getOrCreateDeclaredMemberScope(classId) {
            val classSymbol = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return@getOrCreateDeclaredMemberScope null
            CfirClassDeclaredMemberScope(classSymbol)
        }

    fun queryMemberScope(classId: ClassId): CfirTypeScope? =
        cacheStore.getOrCreateMemberScope(classId) {
            val classSymbol = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return@getOrCreateMemberScope null
            val classDeclaration = classSymbol.cfir as? CfirClass ?: return@getOrCreateMemberScope null
            useSiteSession.cangjieScopeProvider.getUseSiteMemberScope(
                klass = classDeclaration,
                useSiteSession = useSiteSession,
                scopeSession = scopeSession,
            )
        }

    fun queryTypeScope(type: ConeCangJieType): CfirTypeScope? =
        cacheStore.getOrCreateTypeScope(type) {
            buildTypeScope(type)
        }

    fun hasVisiblePackage(packageFqName: FqName): Boolean =
        cacheStore.getOrCreatePackageVisibility(packageFqName) {
            useSiteSession.symbolProvider.hasPackage(packageFqName)
        }

    /**
     * 类型 scope 的职责属于 analysis-api-cfir 层。
     *
     * low-level facade 只负责构建/解析 CFIR，不承担“任意类型如何映射成 use-site scope”的上层语义。
     * 这里直接对齐 `CfirReceivers.typeToScope()` 的组织方式，但仅依赖当前仓库真实存在的构件。
     */
    private fun buildTypeScope(type: ConeCangJieType): CfirTypeScope? {
        val scopes = linkedSetOf<CfirTypeScope>()
        collectTypeScopes(
            type = type,
            destination = scopes,
            visitedClassIds = linkedSetOf(),
            visitedTypeParameters = linkedSetOf(),
        )
        return when (scopes.size) {
            0 -> null
            1 -> scopes.single()
            else -> CfirCompositeTypeScope(scopes.toList())
        }
    }

    private fun collectTypeScopes(
        type: ConeCangJieType,
        destination: MutableSet<CfirTypeScope>,
        visitedClassIds: MutableSet<ClassId>,
        visitedTypeParameters: MutableSet<org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag>,
    ) {
        when (type) {
            is ConeTypeVariableType -> {
                val originalTypeParameter = type.typeConstructor.originalTypeParameter as? org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
                    ?: return
                collectTypeParameterBoundsScopes(originalTypeParameter, destination, visitedClassIds, visitedTypeParameters)
            }

            is ConeTypeParameterType -> {
                collectTypeParameterBoundsScopes(type.lookupTag, destination, visitedClassIds, visitedTypeParameters)
            }

            is ConeIntersectionType -> {
                type.intersectedTypes.forEach { intersectedType ->
                    collectTypeScopes(intersectedType, destination, visitedClassIds, visitedTypeParameters)
                }
            }

            else -> {
                val classId = type.classIdOrPrimitiveClassId ?: return
                if (!visitedClassIds.add(classId)) return

                val symbol = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return
                val rawScope = CfirClassUseSiteMemberScope(
                    session = useSiteSession,
                    classSymbol = symbol,
                    symbolProvider = useSiteSession.symbolProvider,
                    extendProvider = useSiteSession.extendProvider,
                    directSupertypeProvider = useSiteSession.directSupertypeProviderOrNull,
                    ownerType = type,
                )
                destination += CfirClassSubstitutionScope(useSiteSession, rawScope, type)
            }
        }
    }

    private fun collectTypeParameterBoundsScopes(
        lookupTag: org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag,
        destination: MutableSet<CfirTypeScope>,
        visitedClassIds: MutableSet<ClassId>,
        visitedTypeParameters: MutableSet<org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag>,
    ) {
        if (!visitedTypeParameters.add(lookupTag)) return

        collectTypeParameterUpperBounds(ConeTypeParameterTypeImpl(lookupTag)).forEach { upperBound ->
            collectTypeScopes(upperBound, destination, visitedClassIds, visitedTypeParameters)
        }
    }

    private fun collectTypeParameterUpperBounds(typeParameterType: ConeTypeParameterType): Set<ConeCangJieType> {
        val upperBounds = linkedSetOf<ConeCangJieType>()
        val seen = linkedSetOf<ConeCangJieType>()

        fun collect(type: ConeCangJieType) {
            if (!seen.add(type)) return

            when (type) {
                is ConeErrorType -> Unit
                is ConeTypeParameterType -> type.lookupTag.collectUpperBoundsTo(::collect)
                is ConeTypeVariableType -> {
                    val originalTypeParameter = type.typeConstructor.originalTypeParameter as? org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
                        ?: return
                    originalTypeParameter.collectUpperBoundsTo(::collect)
                }
                is ConeIntersectionType -> type.intersectedTypes.forEach(::collect)
                else -> upperBounds += type
            }
        }

        collect(typeParameterType)
        return upperBounds
    }

    private fun org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag.collectUpperBoundsTo(
        collect: (ConeCangJieType) -> Unit,
    ) {
        typeParameterSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        typeParameterSymbol.resolvedBounds.map { bound -> bound.coneType }.forEach(collect)
    }
}
