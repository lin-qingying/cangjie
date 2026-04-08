package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 组合多个 [CfirSymbolProvider]，按注册顺序优先级查找。
 *
 * 对齐 Kotlin K2 的 FirCachingCompositeSymbolProvider。
 * 类查找返回首个命中结果；可调用查找合并所有 provider 的结果。
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

    override fun getClassLikeSymbolByClassId(classId: ClassId):  CfirClassLikeSymbol<*>? {
        for (provider in providers) {
            val symbol = provider.getClassLikeSymbolByClassId(classId)
            if (symbol != null) return symbol
        }
        return null
    }

    override fun getTopLevelClassifierSymbols(packageFqName: FqName, name: Name): List<CfirClassLikeSymbol<*>> {
        val merged = LinkedHashSet<CfirClassLikeSymbol<*>>()
        for (provider in providers) {
            merged += provider.getTopLevelClassifierSymbols(packageFqName, name)
        }
        return merged.toList()
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        return providers.flatMap { it.getTopLevelCallableSymbols(packageFqName, name) }
    }

    override fun hasPackage(fqName: FqName): Boolean {
        return providers.any { it.hasPackage(fqName) }
    }

    override fun getClassIdBySymbol(classSymbol: CfirClassSymbol): ClassId? {
        for (provider in providers) {
            val classId = provider.getClassIdBySymbol(classSymbol)
            if (classId != null) return classId
        }
        return null
    }

    override fun getEnumConstructorOwnerClassId(symbol: CfirEnumConstructorSymbol): ClassId? {
        for (provider in providers) {
            val classId = provider.getEnumConstructorOwnerClassId(symbol)
            if (classId != null) return classId
        }
        return null
    }

    override fun getContainingFile(symbol: CfirSymbol<*>): CfirFile? {
        val normalizedSymbol = symbol.unwrapForDeclarationMetadataLookup()
        for (provider in providers) {
            val file = provider.getContainingFile(normalizedSymbol)
            if (file != null) return file
        }
        return null
    }

    override fun getContainingClassId(symbol: CfirCallableSymbol<*>): ClassId? {
        val normalizedSymbol = symbol.unwrapCallableForDeclarationMetadataLookup()
        for (provider in providers) {
            val classId = provider.getContainingClassId(normalizedSymbol)
            if (classId != null) return classId
        }
        return null
    }
}
