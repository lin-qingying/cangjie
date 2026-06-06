

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.mergeDeclarationProviders
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.mergePackageProviders
import org.cangnova.cangjie.analysis.api.platform.caches.NullableCaffeineCache
import org.cangnova.cangjie.analysis.api.platform.caches.getOrPut
import org.cangnova.cangjie.analysis.api.platform.caches.withStatsCounter
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLCangJieSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import java.time.Duration

/**
 * [LLCombinedCangJieSymbolProvider] combines multiple [LLCangJieSymbolProvider]s with the following advantages:
 *
 * - The combined symbol provider can combine the "names in package" sets built by individual providers. The name set can then be checked
 *   once instead of for each subordinate symbol provider. Because CangJie symbol providers are ordered first in
 *   [LLDependenciesSymbolProvider][org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLDependenciesSymbolProvider],
 *   this check is especially fruitful.
 * - For a given class or callable ID, indices can be accessed once to get relevant PSI elements. Then the correct symbol provider(s) to
 *   call can be found out via the PSI element's [CaModule][org.cangnova.cangjie.analysis.api.projectStructure.CaModule]s. This avoids the
 *   need to call every single subordinate symbol provider.
 * - A small Caffeine cache can avoid most index accesses for classes, because many names are requested multiple times, with a minor memory
 *   footprint.
 * - Caffeine caches for functions and variables use time-based eviction, which allows them to scale up in short bursts when many callables
 *   are requested.
 *
 * @param declarationProvider The declaration provider must have a scope which combines the scopes of the individual [providers].
 */
@OptIn(CaPlatformInterface::class)

internal class LLCombinedCangJieSymbolProvider private constructor(
    session: CfirSession,
    project: Project,
    providers: List<LLCangJieSymbolProvider>,
    private val declarationProvider: CangJieDeclarationProvider,
    private val packageProvider: CangJiePackageProvider,
) : LLSelectingCombinedSymbolProvider<LLCangJieSymbolProvider>(session, project, providers) {
    override val symbolNamesProvider: CfirSymbolNamesProvider =
        CfirCompositeSymbolProvider(session, providers).symbolNamesProvider

    private val classifierCache = NullableCaffeineCache<ClassId, CfirClassLikeSymbol<*>> {
        it
            .maximumSize(500)
            .withStatsCounter(LLStatisticsService.getInstance(project)?.symbolProviders?.combinedSymbolProviderClassCacheStatsCounter)
    }

    private val functionCache =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(5))
            .withStatsCounter(LLStatisticsService.getInstance(project)?.symbolProviders?.combinedSymbolProviderCallableCacheStatsCounter)
            .build<CallableId, List<CfirNamedFunctionSymbol>>()

    private val propertyCache =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(5))
            .withStatsCounter(LLStatisticsService.getInstance(project)?.symbolProviders?.combinedSymbolProviderCallableCacheStatsCounter)
            .build<CallableId, List<CfirPropertySymbol>>()

    private val macroCache =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(5))
            .withStatsCounter(LLStatisticsService.getInstance(project)?.symbolProviders?.combinedSymbolProviderCallableCacheStatsCounter)
            .build<CallableId, List<CfirCallableSymbol<*>>>()

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        if (!symbolNamesProvider.mayHaveTopLevelClassifier(classId)) return null

        return classifierCache.getOrPut(classId) { computeClassLikeSymbolByClassId(it) }
    }

    private fun computeClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        val candidates = declarationProvider.getAllClassesByClassId(classId) + declarationProvider.getAllTypeAliasesByClassId(classId)
        val (classLikeDeclaration, provider) = selectCfirstElementInClasspathOrder(candidates) { it } ?: return null

        // We've picked the symbol provider via the class-like declaration, so that declaration must be contained in the symbol provider's module.
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return provider.getClassLikeSymbolByClassId(classId, classLikeDeclaration)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return

        val callableId = CallableId(packageFqName, name)

        // Callables are provided very rarely (compared to functions/variables individually), so it's acceptable to hit caches and indices
        // for each CangJie top-level callable kind.
        destination.addAll(getTopLevelFunctionSymbolsFromCache(callableId))
        destination.addAll(getTopLevelPropertySymbolsFromCache(callableId))
        destination.addAll(getTopLevelMacroSymbolsFromCache(callableId))
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return

        destination.addAll(getTopLevelFunctionSymbolsFromCache(CallableId(packageFqName, name)))
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return

        destination.addAll(getTopLevelPropertySymbolsFromCache(CallableId(packageFqName, name)))
    }

    @OptIn(CfirSymbolProviderInternals::class)
    private fun getTopLevelFunctionSymbolsFromCache(callableId: CallableId): List<CfirNamedFunctionSymbol> =
        getCallablesFromCache(
            callableId,
            functionCache,
            declarationProvider::getTopLevelFunctions,
        ) { destination, callableId, functions ->
            getTopLevelFunctionSymbolsTo(destination, callableId, functions)
        }

    @OptIn(CfirSymbolProviderInternals::class)
    private fun getTopLevelPropertySymbolsFromCache(callableId: CallableId): List<CfirPropertySymbol> =
        getCallablesFromCache(
            callableId,
            propertyCache,
            declarationProvider::getTopLevelProperties,
        ) { destination, callableId, properties ->
            getTopLevelPropertySymbolsTo(destination, callableId, properties)
        }

    @OptIn(CfirSymbolProviderInternals::class)
    private fun getTopLevelMacroSymbolsFromCache(callableId: CallableId): List<CfirCallableSymbol<*>> =
        getCallablesFromCache(
            callableId,
            macroCache,
            declarationProvider::getTopLevelMacros,
        ) { destination, callableId, macros ->
            getTopLevelCallableSymbolsTo(destination, callableId, macros)
        }

    /**
     * Retrieves all callables of type [S] from the given [cache] or loads them with [getCallables] and [provide].
     *
     * We cannot use [CangJieDeclarationProvider.getTopLevelCallableFiles] like [LLCangJieSourceSymbolProvider][org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLCangJieSourceSymbolProvider]
     * for optimization because this approach only works for sources. Stub-based library symbol providers shouldn't access callables from
     * [CjFile][org.cangnova.cangjie.psi.CjFile]s.
     */
    private inline fun <A : CjCallableDeclaration, S : CfirCallableSymbol<*>> getCallablesFromCache(
        callableId: CallableId,
        cache: Cache<CallableId, List<S>>,
        crossinline getCallables: (CallableId) -> Collection<A>,
        crossinline provide: LLCangJieSymbolProvider.(MutableList<S>, CallableId, Collection<A>) -> Unit,
    ): List<S> =
        cache.getOrPut(callableId) {
            buildList {
                getCallables(callableId)
                    .groupBy { getModule(it) }
                    .forEach { (module, callables) ->
                        // If `module` cannot be found in the map, `callables` cannot be processed by any of the available providers,
                        // because none of them belong to the correct module. We can skip in that case because iterating through all
                        // providers wouldn't lead to any results for `callables`.
                        val provider = getProviderByModule(module) ?: return@forEach
                        provider.provide(this, callableId, callables)
                    }
            }
        }

    override fun hasPackage(fqName: FqName): Boolean {
        // Regarding caching `hasPackage`: The static (standalone) package provider precomputes its packages, while the IDE package provider
        // caches the results itself. Hence, it's currently unnecessary to provide another layer of caching here.
        return packageProvider.doesPackageExist(fqName)
    }

    override fun estimateSymbolCacheSize(): Long = classifierCache.estimatedSize

    companion object {
        fun merge(session: LLCfirSession, project: Project, providers: List<LLCangJieSymbolProvider>): CfirSymbolProvider? =
            if (providers.size > 1) {
                val declarationProvider = project.mergeDeclarationProviders(providers.map { it.declarationProvider })
                val packageProvider = project.mergePackageProviders(providers.map { it.packageProvider })

                LLCombinedCangJieSymbolProvider(
                    session,
                    project,
                    providers,
                    declarationProvider,
                    packageProvider,
                )
            } else providers.singleOrNull()
    }
}
