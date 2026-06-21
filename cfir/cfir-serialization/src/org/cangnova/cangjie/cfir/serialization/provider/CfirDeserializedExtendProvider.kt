package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.declaredExtendTargetKey
import org.cangnova.cangjie.cfir.types.extendLookupKinds
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 反序列化 extend provider。
 *
 * 除了按目标类型建索引，也要把 extend 内部 callable 映射回 owner extend，
 * 这样库符号路径与源码路径在 substitution scope 上保持一致。
 */
class CfirDeserializedExtendProvider(
    private val providers: List<AbstractCfirDeserializedSymbolProvider>,
) : CfirExtendProvider {
    private val index: ExtendIndex by lazy(LazyThreadSafetyMode.PUBLICATION) { buildIndex() }

    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> =
        index.byTargetKey[targetKey].orEmpty()

    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> =
        getExtendsForTarget(CfirExtendTargetKey.ClassLike(classId))

    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> =
        index.byPackage[packageFqName].orEmpty()

    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> =
        kind.extendLookupKinds.flatMap { getExtendsForClass(it.classId) }.distinct()

    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? =
        index.byCallableSymbol[symbol]

    private fun buildIndex(): ExtendIndex {
        val byTargetKey = linkedMapOf<CfirExtendTargetKey, MutableList<CfirExtend>>()
        val byPackage = linkedMapOf<FqName, MutableList<CfirExtend>>()
        val byCallableSymbol = linkedMapOf<CfirCallableSymbol<*>, CfirExtend>()

        for (provider in providers) {
            val packageNames = provider.symbolNamesProvider.getPackageNames().orEmpty()
            for (packageName in packageNames) {
                val packageFqName = FqName(packageName)
                val extends = provider.getTopLevelExtendDeclarations(packageFqName)
                if (extends.isEmpty()) continue

                byPackage.getOrPut(packageFqName) { mutableListOf() }.addAll(extends)
                for (extend in extends) {
                    val targetKey =
                        extend.extendedTypeRef.coneTypeOrNull?.declaredExtendTargetKey ?: continue
                    byTargetKey.getOrPut(targetKey) { mutableListOf() }.add(extend)
                    for (declaration in extend.declarations) {
                        val callableDeclaration = declaration as? CfirCallableDeclaration ?: continue
                        byCallableSymbol[callableDeclaration.symbol] = extend
                    }
                }
            }
        }

        return ExtendIndex(
            byTargetKey = byTargetKey.mapValues { (_, extends) -> extends.distinct() },
            byPackage = byPackage.mapValues { (_, extends) -> extends.distinct() },
            byCallableSymbol = byCallableSymbol,
        )
    }

    private data class ExtendIndex(
        val byTargetKey: Map<CfirExtendTargetKey, List<CfirExtend>>,
        val byPackage: Map<FqName, List<CfirExtend>>,
        val byCallableSymbol: Map<CfirCallableSymbol<*>, CfirExtend>,
    )
}
