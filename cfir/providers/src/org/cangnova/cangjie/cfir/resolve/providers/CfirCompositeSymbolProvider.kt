package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 组合多个 [CfirSymbolProvider]，仅负责 symbol lookup 聚合。
 */
class CfirCompositeSymbolProvider(
    session: CfirSession,
    val providers: List<CfirSymbolProvider>,
) : CfirSymbolProvider(session) {
    override val symbolNamesProvider: CfirSymbolNamesProvider = object : CfirSymbolNamesProvider {
        override fun getPackageNames(): Set<FqName>? = mergeNameSets { it.getPackageNames() }

        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
            mergeNameSets { it.getTopLevelClassifierNamesInPackage(packageFqName) }

        override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
            mergeNameSets { it.getTopLevelCallableNamesInPackage(packageFqName) }

        private fun <T> mergeNameSets(getter: (CfirSymbolNamesProvider) -> Set<T>?): Set<T>? {
            val merged = LinkedHashSet<T>()
            var hasKnownSet = false
            for (provider in providers) {
                val names = getter(provider.symbolNamesProvider) ?: continue
                hasKnownSet = true
                merged += names
            }
            return if (hasKnownSet) merged else null
        }
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        for (provider in providers) {
            provider.getClassLikeSymbolByClassId(classId)?.let { return it }
        }
        return null
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
        for (provider in providers) {
            provider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
        }
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        for (provider in providers) {
            provider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        }
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        for (provider in providers) {
            provider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        }
    }

    override fun hasPackage(fqName: FqName): Boolean =
        providers.any { it.hasPackage(fqName) }
}
