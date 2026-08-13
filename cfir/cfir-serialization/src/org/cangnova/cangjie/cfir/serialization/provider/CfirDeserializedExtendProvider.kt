package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.expandedExtendTargetKey
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
    /** 当前 session 可见的反序列化 symbol provider 列表。 */
    private val providers: List<AbstractCfirDeserializedSymbolProvider>,
) : CfirExtendProvider {
    /** 延迟构造的 extend 查询索引，首次查询时遍历所有可见包。 */
    private val index: ExtendIndex by lazy(LazyThreadSafetyMode.PUBLICATION) { buildIndex() }

    /** 按规范化目标 key 查询库中声明的 extend。 */
    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> =
        index.byTargetKey[targetKey].orEmpty()

    /** 按 class-like 目标类型查询 extend。 */
    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> =
        getExtendsForTarget(CfirExtendTargetKey.ClassLike(classId))

    /** 查询指定包内所有顶层 extend 声明。 */
    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> =
        index.byPackage[packageFqName].orEmpty()

    /** 查询内建基础类型族对应的 extend 声明。 */
    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> =
        kind.extendLookupKinds.flatMap { getExtendsForClass(it.classId) }.distinct()

    /** 通过 extend 成员 callable 符号反查其所属 extend。 */
    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? =
        index.byCallableSymbol[symbol]

    /** 反查反序列化 extend 所属包名。 */
    override fun getPackageFqName(extend: CfirExtend): FqName? =
        index.byDeclarationPackage[extend]

    /**
     * 遍历所有反序列化包并建立 extend 查询索引。
     *
     * 索引同时覆盖目标类型、包名、成员 callable 到 owner extend 的反查关系。
     */
    private fun buildIndex(): ExtendIndex {
        val byTargetKey = linkedMapOf<CfirExtendTargetKey, MutableList<CfirExtend>>()
        val byPackage = linkedMapOf<FqName, MutableList<CfirExtend>>()
        val byCallableSymbol = linkedMapOf<CfirCallableSymbol<*>, CfirExtend>()
        val byDeclarationPackage = linkedMapOf<CfirExtend, FqName>()

        for (provider in providers) {
            val packageNames = provider.symbolNamesProvider.getPackageNames().orEmpty()
            for (packageName in packageNames) {
                val packageFqName = FqName(packageName)
                val extends = provider.getTopLevelExtendDeclarations(packageFqName)
                if (extends.isEmpty()) continue

                byPackage.getOrPut(packageFqName) { mutableListOf() }.addAll(extends)
                for (extend in extends) {
                    byDeclarationPackage[extend] = packageFqName
                    val targetKey =
                        extend.extendedTypeRef.coneTypeOrNull?.expandedExtendTargetKey ?: continue
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
            byDeclarationPackage = byDeclarationPackage,
        )
    }

    /** 反序列化 extend provider 的多维查询索引。 */
    private data class ExtendIndex(
        /** extend 目标类型 key 到声明列表的索引。 */
        val byTargetKey: Map<CfirExtendTargetKey, List<CfirExtend>>,
        /** 包名到该包顶层 extend 声明列表的索引。 */
        val byPackage: Map<FqName, List<CfirExtend>>,
        /** extend 成员 callable symbol 到 owner extend 的反查索引。 */
        val byCallableSymbol: Map<CfirCallableSymbol<*>, CfirExtend>,
        /** extend 声明到所属包名的反查索引。 */
        val byDeclarationPackage: Map<CfirExtend, FqName>,
    )
}

/**
 * 展开 [CfirSymbolProvider] 树中的反序列化 provider。
 *
 * 该工具用于从任意符号 provider 集合中提取二进制 extend 元数据来源，供 entrypoint 的库
 * extend provider 与 LL/IDE 侧组合 extend provider 共用。
 */
fun CfirSymbolProvider.flattenDeserializedProviders(): List<AbstractCfirDeserializedSymbolProvider> {
    return when (this) {
        is CfirCompositeSymbolProvider -> providers.flatMap(CfirSymbolProvider::flattenDeserializedProviders)
        is AbstractCfirDeserializedSymbolProvider -> listOf(this)
        else -> emptyList()
    }
}
