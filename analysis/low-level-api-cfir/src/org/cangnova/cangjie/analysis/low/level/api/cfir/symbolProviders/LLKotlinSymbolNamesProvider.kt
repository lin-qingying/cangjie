

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.cangnova.cangjie.cfir.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirDelegatingCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.filterToSetOrEmpty

/**
 * A [CfirSymbolNamesProvider] that fetches top-level names from a Kotlin [declarationProvider].
 *
 * @param allowKotlinPackage Whether the associated symbol provider is allowed to provide symbols from the `kotlin` package.
 */
internal open class LLCfirKotlinSymbolNamesProvider(
    private val declarationProvider: KotlinDeclarationProvider,
    private val allowKotlinPackage: Boolean? = null,
) : CfirSymbolNamesProvider() {
    override fun getPackageNames(): Set<String>? = declarationProvider.computePackageNames()?.excludeKotlinPackageNamesIfNecessary()

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = declarationProvider.hasSpecificClassifierPackageNamesComputation

    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? =
        declarationProvider
            .computePackageNamesWithTopLevelClassifiers()
            ?.excludeKotlinPackageNamesIfNecessary()

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> {
        if (allowKotlinPackage == false && packageFqName.isKotlinPackage()) return emptySet()

        return declarationProvider.getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName)
    }

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = declarationProvider.hasSpecificCallablePackageNamesComputation

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? =
        declarationProvider
            .computePackageNamesWithTopLevelCallables()
            ?.excludeKotlinPackageNamesIfNecessary()

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        if (allowKotlinPackage == false && packageFqName.isKotlinPackage()) return emptySet()

        return declarationProvider.getTopLevelCallableNamesInPackage(packageFqName).ifEmpty { emptySet() }
    }

    private fun Set<String>.excludeKotlinPackageNamesIfNecessary(): Set<String> {
        if (allowKotlinPackage == false && any { it.isKotlinPackage() }) {
            return filterToSetOrEmpty { !it.isKotlinPackage() }
        }
        return this
    }

    companion object {
        fun cached(
            session: CfirSession,
            declarationProvider: KotlinDeclarationProvider,
            allowKotlinPackage: Boolean? = null,
        ): CfirCachedSymbolNamesProvider =
            CfirDelegatingCachedSymbolNamesProvider(session, LLCfirKotlinSymbolNamesProvider(declarationProvider, allowKotlinPackage))
    }
}
