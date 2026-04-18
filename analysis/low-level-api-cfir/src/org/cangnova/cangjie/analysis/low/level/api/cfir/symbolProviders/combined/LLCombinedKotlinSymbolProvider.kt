/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.mergeDeclarationProviders
import org.cangnova.cangjie.analysis.api.platform.packages.KotlinPackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.mergePackageProviders
import org.cangnova.cangjie.analysis.api.platform.caches.NullableCaffeineCache
import org.cangnova.cangjie.analysis.api.platform.caches.getOrPut
import org.cangnova.cangjie.analysis.api.platform.caches.withStatsCounter
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLKotlinSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirPropertySymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import java.time.Duration

/**
 * [LLCombinedKotlinSymbolProvider] combines multiple [LLKotlinSymbolProvider]s with the following advantages:
 *
 * - The combined symbol provider can combine the "names in package" sets built by individual providers. The name set can then be checked
 *   once instead of for each subordinate symbol provider. Because Kotlin symbol providers are ordered first in
 *   [LLDependenciesSymbolProvider][org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLDependenciesSymbolProvider],
 *   this check is especially fruitful.
 * - For a given class or callable ID, indices can be accessed once to get relevant PSI elements. Then the correct symbol provider(s) to
 *   call can be found out via the PSI element's [CaModule][org.cangnova.cangjie.analysis.api.projectStructure.CaModule]s. This avoids the
 *   need to call every single subordinate symbol provider.
 * - A small Caffeine cache can avoid most index accesses for classes, because many names are requested multiple times, with a minor memory
 *   footprint.
 * - Caffeine caches for functions and properties use time-based eviction, which allows them to scale up in short bursts when many callables
 *   are requested.
 *
 * @param declarationProvider The declaration provider must have a scope which combines the scopes of the individual [providers].
 *
 * @param packageProviderForKotlinPackages This package provider should be combined from all [providers] which allow `kotlin` packages (see
 *  [LLKotlinSymbolProvider.allowKotlinPackage]). It may be `null` if no such provider exists. See [hasPackage] for a use case.
 */
internal class LLCombinedKotlinSymbolProvider private constructor(
    session: CfirSession,
    project: Project,
    providers: List<LLKotlinSymbolProvider>,
    private val declarationProvider: KotlinDeclarationProvider,
    private val packageProvider: KotlinPackageProvider,
    private val packageProviderForKotlinPackages: KotlinPackageProvider?,
) : LLSelectingCombinedSymbolProvider<LLKotlinSymbolProvider>(session, project, providers) {
    override val symbolNamesProvider: CfirSymbolNamesProvider = CfirCompositeCachedSymbolNamesProvider.fromSymbolProviders(session, providers)

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

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        if (!symbolNamesProvider.mayHaveTopLevelClassifier(classId)) return null

        return classifierCache.getOrPut(classId) { computeClassLikeSymbolByClassId(it) }
    }

    private fun computeClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        val candidates = declarationProvider.getAllClassesByClassId(classId) + declarationProvider.getAllTypeAliasesByClassId(classId)
        val (ktClass, provider) = selectCfirstElementInClasspathOrder(candidates) { it } ?: return null

        // We've picked the symbol provider via the `ktClass`, so `ktClass` must be contained in the symbol provider's module.
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return provider.getClassLikeSymbolByClassId(classId, ktClass)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return

        val callableId = CallableId(packageFqName, name)

        // Callables are provided very rarely (compared to functions/properties individually), so it's acceptable to hit caches and indices
        // twice here.
        destination.addAll(getTopLevelFunctionSymbolsFromCache(callableId))
        destination.addAll(getTopLevelPropertySymbolsFromCache(callableId))
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

    /**
     * Retrieves all callables of type [S] from the given [cache] or loads them with [getCallables] and [provide].
     *
     * We cannot use [KotlinDeclarationProvider.getTopLevelCallableFiles] like [LLKotlinSourceSymbolProvider][org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLKotlinSourceSymbolProvider]
     * for optimization because this approach only works for sources. Stub-based library symbol providers shouldn't access callables from
     * [CjFile][org.cangnova.cangjie.psi.CjFile]s.
     */
    private inline fun <A : CjCallableDeclaration, S : CfirCallableSymbol<*>> getCallablesFromCache(
        callableId: CallableId,
        cache: Cache<CallableId, List<S>>,
        crossinline getCallables: (CallableId) -> Collection<A>,
        crossinline provide: LLKotlinSymbolProvider.(MutableList<S>, CallableId, Collection<A>) -> Unit,
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
        val hasPackage = if (fqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)) {
            // If a package is a `kotlin` package, `packageProvider` might find it via the scope of an individual symbol provider that
            // disallows `kotlin` packages. Hence, the combined `getPackage` would erroneously find a package it shouldn't be able to find,
            // because calling that individual symbol provider directly would result in `null` (as it disallows `kotlin` packages). The
            // `packageProviderForKotlinPackages` solves this issue by including only scopes from symbol providers which allow `kotlin`
            // packages.
            packageProviderForKotlinPackages?.doesKotlinOnlyPackageExist(fqName) == true
        } else {
            packageProvider.doesKotlinOnlyPackageExist(fqName)
        }

        // Regarding caching `hasPackage`: The static (standalone) package provider precomputes its packages, while the IDE package provider
        // caches the results itself. Hence, it's currently unnecessary to provide another layer of caching here.
        return hasPackage
    }

    override fun estimateSymbolCacheSize(): Long = classifierCache.estimatedSize

    companion object {
        fun merge(session: LLCfirSession, project: Project, providers: List<LLKotlinSymbolProvider>): CfirSymbolProvider? =
            if (providers.size > 1) {
                val declarationProvider = project.mergeDeclarationProviders(providers.map { it.declarationProvider })

                val packageProvider = project.mergePackageProviders(providers.map { it.packageProvider })

                val packageProviderForKotlinPackages = providers
                    .filter { it.allowKotlinPackage }
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.packageProvider }
                    ?.let(project::mergePackageProviders)

                LLCombinedKotlinSymbolProvider(
                    session,
                    project,
                    providers,
                    declarationProvider,
                    packageProvider,
                    packageProviderForKotlinPackages,
                )
            } else providers.singleOrNull()
    }
}
