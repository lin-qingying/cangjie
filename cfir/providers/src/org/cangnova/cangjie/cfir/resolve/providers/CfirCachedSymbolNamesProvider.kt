package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.caches.cfirCachesFactory
import org.cangnova.cangjie.cfir.caches.getValue
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.flatMapToNullableSet

/**
 * 对齐 Kotlin FIR `FirCachedSymbolNamesProvider` 的缓存包装。
 */
abstract class CfirCachedSymbolNamesProvider(protected val session: CfirSession) : CfirSymbolNamesProvider() {
    abstract fun computePackageNames(): Set<String>?
    abstract fun computePackageNamesWithTopLevelClassifiers(): Set<String>?
    abstract fun computeTopLevelClassifierNames(packageFqName: FqName): Set<Name>?
    abstract fun computePackageNamesWithTopLevelCallables(): Set<String>?
    abstract fun computeTopLevelCallableNames(packageFqName: FqName): Set<Name>?

    private val cachedPackageNames by lazy(LazyThreadSafetyMode.PUBLICATION) {
        computePackageNames()
    }

    private val topLevelClassifierPackageNames by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (hasSpecificClassifierPackageNamesComputation) {
            computePackageNamesWithTopLevelClassifiers()?.let { return@lazy it }
        }
        cachedPackageNames
    }

    private val topLevelClassifierNamesByPackage = session.cfirCachesFactory.createPossiblySoftLazyValue {
        session.cfirCachesFactory.createCache<FqName, Set<Name>?, Nothing?> { packageFqName, _ ->
            computeTopLevelClassifierNames(packageFqName)
        }
    }

    private val topLevelCallablePackageNames by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (hasSpecificCallablePackageNamesComputation) {
            computePackageNamesWithTopLevelCallables()?.let { return@lazy it }
        }
        cachedPackageNames
    }

    private val topLevelCallableNamesByPackage = session.cfirCachesFactory.createPossiblySoftLazyValue {
        session.cfirCachesFactory.createCache<FqName, Set<Name>?, Nothing?> { packageFqName, _ ->
            computeTopLevelCallableNames(packageFqName)
        }
    }

    override fun getPackageNames(): Set<String>? = cachedPackageNames

    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? = topLevelClassifierPackageNames

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? {
        val packageNames = getPackageNamesWithTopLevelClassifiers()
        if (packageNames != null && packageFqName.asString() !in packageNames) return emptySet()
        return getTopLevelClassifierNamesInPackageSkippingPackageCheck(packageFqName)
    }

    protected fun getTopLevelClassifierNamesInPackageSkippingPackageCheck(packageFqName: FqName): Set<Name>? =
        topLevelClassifierNamesByPackage.getValue().getValue(packageFqName)

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? = topLevelCallablePackageNames

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? {
        val packageNames = getPackageNamesWithTopLevelCallables()
        if (packageNames != null && packageFqName.asString() !in packageNames) return emptySet()
        return topLevelCallableNamesByPackage.getValue().getValue(packageFqName)
    }
}

class CfirDelegatingCachedSymbolNamesProvider(
    session: CfirSession,
    private val delegate: CfirSymbolNamesProvider,
) : CfirCachedSymbolNamesProvider(session) {
    override fun computePackageNames(): Set<String>? = delegate.getPackageNames()

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = delegate.hasSpecificClassifierPackageNamesComputation

    override fun computePackageNamesWithTopLevelClassifiers(): Set<String>? =
        delegate.getPackageNamesWithTopLevelClassifiers()

    override fun computeTopLevelClassifierNames(packageFqName: FqName): Set<Name>? =
        delegate.getTopLevelClassifierNamesInPackage(packageFqName)

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = delegate.hasSpecificCallablePackageNamesComputation

    override fun computePackageNamesWithTopLevelCallables(): Set<String>? =
        delegate.getPackageNamesWithTopLevelCallables()

    override fun computeTopLevelCallableNames(packageFqName: FqName): Set<Name>? =
        delegate.getTopLevelCallableNamesInPackage(packageFqName)

    override val mayHaveSyntheticFunctionTypes: Boolean
        get() = delegate.mayHaveSyntheticFunctionTypes

    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean =
        delegate.mayHaveSyntheticFunctionType(classId)
}

open class CfirCompositeCachedSymbolNamesProvider(
    session: CfirSession,
    val providers: List<CfirSymbolNamesProvider>,
) : CfirCachedSymbolNamesProvider(session) {
    override fun computePackageNames(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNames() }

    override val hasSpecificClassifierPackageNamesComputation: Boolean =
        providers.any { it.hasSpecificClassifierPackageNamesComputation }

    override fun computePackageNamesWithTopLevelClassifiers(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelClassifiers() }

    override fun computeTopLevelClassifierNames(packageFqName: FqName): Set<Name>? =
        providers.flatMapToNullableSet { it.getTopLevelClassifierNamesInPackage(packageFqName) }

    override val hasSpecificCallablePackageNamesComputation: Boolean =
        providers.any { it.hasSpecificCallablePackageNamesComputation }

    override fun computePackageNamesWithTopLevelCallables(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelCallables() }

    override fun computeTopLevelCallableNames(packageFqName: FqName): Set<Name>? =
        providers.flatMapToNullableSet { it.getTopLevelCallableNamesInPackage(packageFqName) }

    override val mayHaveSyntheticFunctionTypes: Boolean =
        providers.any { it.mayHaveSyntheticFunctionTypes }

    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean =
        providers.any { it.mayHaveSyntheticFunctionType(classId) }

    companion object {
        fun create(session: CfirSession, providers: List<CfirSymbolNamesProvider>): CfirSymbolNamesProvider = when (providers.size) {
            0 -> CfirEmptySymbolNamesProvider
            1 -> when (val provider = providers.single()) {
                is CfirCachedSymbolNamesProvider -> provider
                else -> CfirDelegatingCachedSymbolNamesProvider(session, provider)
            }
            else -> CfirCompositeCachedSymbolNamesProvider(session, providers)
        }

        fun fromSymbolProviders(session: CfirSession, providers: List<CfirSymbolProvider>): CfirSymbolNamesProvider =
            create(session, providers.map { it.symbolNamesProvider })
    }
}
