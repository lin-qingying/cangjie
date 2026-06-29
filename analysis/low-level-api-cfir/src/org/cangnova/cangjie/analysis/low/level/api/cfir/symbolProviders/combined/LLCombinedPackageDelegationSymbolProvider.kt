

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined

import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.newHashMapWithExpectedSize

/**
 * [LLCombinedPackageDelegationSymbolProvider] combines multiple [CfirSymbolProvider]s by delegating to the appropriate individual providers
 * based on package names.
 *
 * Unlike [LLCombinedCangJieSymbolProvider], which delegates based on the modules of index-provided candidates, this provider builds a map
 * from package [FqName]s to lists of symbol providers that can provide symbols for that package. Then, for each symbol request, it queries
 * only the relevant providers for the given package.
 *
 * If any provider's [CfirSymbolNamesProvider.getPackageNames] returns `null`, [LLCombinedPackageDelegationSymbolProvider] cannot be used. In
 * that case, [merge] falls back to [CfirCompositeSymbolProvider], which queries all providers individually.
 *
 * The symbol provider affords its simple implementation due to the following factors:
 *
 * - There is no need to check `mayHaveTopLevel*` functions as the access to [providersByPackage] already covers the package name check: If
 *   a package cannot be provided by the combined symbol provider, the map will contain no entry for it. The callable name check itself is
 *   covered on the individual symbol provider level.
 * - There is no need for caches since the delegation is simple.
 * - [providersByPackage] is built in classpath order of the providers, so the symbol provider lists will reflect the classpath order,
 *   preserving it without a need for additional priority logic.
 *
 * ### Usage Notes
 *
 * [LLCombinedPackageDelegationSymbolProvider] is a better choice for combining library symbol providers than
 * [LLCombinedCangJieSymbolProvider]. This is because of the different lifetimes of library symbol providers and combined symbol providers:
 *
 * - Individual library symbol providers are invalidated infrequently, as library sessions outlast source sessions, so they generally have
 *   symbols already cached.
 * - Combined symbol providers are part of use-site source sessions, so they are invalidated more frequently.
 *
 * [LLCombinedCangJieSymbolProvider] accesses the index before calling into individual symbol providers. Using this symbol provider to
 * combine library symbol providers led to the following situation: As source sessions were invalidated, [LLCombinedCangJieSymbolProvider]
 * had to redo index accesses which would not have been performed by individual library symbol providers. This led to an overall performance
 * degradation in some cases.
 *
 * On the flipside, [LLCombinedPackageDelegationSymbolProvider] is not automatically better than [LLCombinedCangJieSymbolProvider]. Cfirst,
 * package delegation requires that all individual symbol providers can compute package sets, which currently isn't the case for source
 * symbol providers. But [LLCombinedCangJieSymbolProvider] might also be better in cases where multiple index accesses would be performed in
 * individual symbol providers, whereas [LLCombinedCangJieSymbolProvider] can perform a single index access.
 *
 * @param providersByPackage For the implementation of [hasPackage], [providersByPackage] must contain a transitive closure of all parent
 *  packages. For example, if the map contains 'foo.bar.baz', it must also contain 'foo.bar' and 'foo'. The entry for 'foo.bar' must contain
 *  all providers that have any package matching the prefix 'foo.bar*'.
 *
 *  [providersByPackage] has [String] keys instead of [FqName] keys to conserve memory. Furthermore, it uses arrays for the same purpose.
 */
internal class LLCombinedPackageDelegationSymbolProvider private constructor(
    session: CfirSession,
    /**
     * 被聚合的底层 provider 列表。
     */
    override val providers: List<CfirSymbolProvider>,

    /**
     * package 名称到可处理该 package 的 provider 数组映射。
     */
    private val providersByPackage: Map<String, Array<CfirSymbolProvider>>
) : LLCombinedSymbolProvider<CfirSymbolProvider>(session) {
    /**
     * 聚合底层 provider 的名称集合 provider。
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider =
        CfirCompositeCachedSymbolNamesProvider.fromSymbolProviders(session, providers)

    /**
     * 按 class id 查询 class-like symbol。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        val relevantProviders = providersByPackage[classId.packageFqName.asString()] ?: return null

        return relevantProviders.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }
    }

    @CfirSymbolProviderInternals
    /**
     * 收集顶层 callable symbol。
     */
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val relevantProviders = providersByPackage[packageFqName.asString()] ?: return

        relevantProviders.forEach { it.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }
    }

    @CfirSymbolProviderInternals
    /**
     * 收集顶层函数 symbol。
     */
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        val relevantProviders = providersByPackage[packageFqName.asString()] ?: return

        relevantProviders.forEach { it.getTopLevelFunctionSymbolsTo(destination, packageFqName, name) }
    }

    @CfirSymbolProviderInternals
    /**
     * 收集顶层属性 symbol。
     */
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        val relevantProviders = providersByPackage[packageFqName.asString()] ?: return

        relevantProviders.forEach { it.getTopLevelPropertySymbolsTo(destination, packageFqName, name) }
    }

    /**
     * 判断包是否存在。
     */
    override fun hasPackage(fqName: FqName): Boolean {
        val relevantProviders = providersByPackage[fqName.asString()] ?: return false

        // We still have to query individual providers since the package sets from symbol names providers may contain false positives, so
        // the `fqName` being in `providersByPackage` doesn't prove that the package exists.
        return relevantProviders.any { it.hasPackage(fqName) }
    }

    /**
     * 当前实现没有自有 symbol cache。
     */
    override fun estimateSymbolCacheSize(): Long = 0

    companion object {
        fun merge(session: CfirSession, providers: List<CfirSymbolProvider>): CfirSymbolProvider? =
            if (providers.size > 1) {
                val providersByPackage = buildPackageToProvidersMap(providers)
                if (providersByPackage != null) {
                    LLCombinedPackageDelegationSymbolProvider(session, providers, providersByPackage)
                } else {
                    CfirCompositeSymbolProvider(session, providers)
                }
            } else providers.singleOrNull()

        /**
         * Builds the "package to providers" map. If any package set is `null`, the resulting map will be `null` as well, and we'll need to
         * fall back to querying all providers individually.
         */
        private fun buildPackageToProvidersMap(providers: List<CfirSymbolProvider>): Map<String, Array<CfirSymbolProvider>>? {
            val providerListsByPackage = buildMap {
                providers.forEach { provider ->
                    val packageNames = provider.symbolNamesProvider.getPackageNames() ?: return null
                    packageNames.forEach { packageName ->
                        // We only use the `FqName` here for convenience. It won't be stored.
                        FqName(packageName).forEachFqName { fqName ->
                            val list = getOrPut(fqName.asString()) { mutableListOf() }

                            // Parent package names may overlap (e.g. 'foo.bar' and 'foo.baz' have the same parent 'foo'), so we have to be
                            // careful not to add the same provider multiple times. Since we're iterating provider by provider, the current
                            // provider will always be last in the list if it has been added, so we just need to check the last element.
                            if (list.lastOrNull() != provider) {
                                list.add(provider)
                            }
                        }
                    }
                }
            }

            // Avoid linked hash maps to conserve memory.
            return newHashMapWithExpectedSize<String, Array<CfirSymbolProvider>>(providerListsByPackage.size).apply {
                providerListsByPackage.forEach { (packageName, providers) ->
                    this[packageName] = providers.toTypedArray()
                }
            }
        }

        private inline fun FqName.forEachFqName(f: (FqName) -> Unit) {
            var current: FqName? = this
            while (current != null) {
                f(current)
                current = if (current.isRoot) null else current.parent()
            }
        }
    }
}
