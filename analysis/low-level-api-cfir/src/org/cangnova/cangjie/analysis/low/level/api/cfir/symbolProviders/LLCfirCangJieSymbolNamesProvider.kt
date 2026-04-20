

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirDelegatingCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * A [CfirSymbolNamesProvider] that fetches top-level names from a CangJie [declarationProvider].
 */
@OptIn(CaPlatformInterface::class)
internal open class LLCfirCangJieSymbolNamesProvider(
    private val declarationProvider: CangJieDeclarationProvider,
) : CfirSymbolNamesProvider() {
    override fun getPackageNames(): Set<String>? =
        declarationProvider.computePackageNames()

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = declarationProvider.hasSpecificClassifierPackageNamesComputation

    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? =
        declarationProvider.computePackageNamesWithTopLevelClassifiers()

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
        declarationProvider.getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName)

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = declarationProvider.hasSpecificCallablePackageNamesComputation

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? =
        declarationProvider.computePackageNamesWithTopLevelCallables()

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> =
        declarationProvider.getTopLevelCallableNamesInPackage(packageFqName).ifEmpty { emptySet() }

    companion object {
        fun cached(
            session: CfirSession,
            declarationProvider: CangJieDeclarationProvider,
        ): CfirCachedSymbolNamesProvider =
            CfirDelegatingCachedSymbolNamesProvider(session, LLCfirCangJieSymbolNamesProvider(declarationProvider))
    }
}
