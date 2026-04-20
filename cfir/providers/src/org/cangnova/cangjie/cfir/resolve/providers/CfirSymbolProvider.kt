package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

@RequiresOptIn
annotation class CfirSymbolProviderInternals

/**
 * 对齐 Kotlin FIR `FirSymbolProvider` 的符号查询基类。
 *
 * 该层只负责 symbol lookup 与热路径查询，不承载 owner/container 元信息。
 */
abstract class CfirSymbolProvider(val session: CfirSession) : CfirSessionComponent {
    abstract val symbolNamesProvider: CfirSymbolNamesProvider

    /**
     * 返回给定 [classId] 的 class-like symbol；若不存在则返回 `null`。
     */
    abstract fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>?

    @OptIn(CfirSymbolProviderInternals::class)
    open fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        return buildList { getTopLevelCallableSymbolsTo(this, packageFqName, name) }
    }

    @CfirSymbolProviderInternals
    abstract fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    )

    @OptIn(CfirSymbolProviderInternals::class)
    open fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<CfirNamedFunctionSymbol> {
        return buildList { getTopLevelFunctionSymbolsTo(this, packageFqName, name) }
    }

    @CfirSymbolProviderInternals
    abstract fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    )

    @OptIn(CfirSymbolProviderInternals::class)
    open fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<CfirPropertySymbol> {
        return buildList { getTopLevelPropertySymbolsTo(this, packageFqName, name) }
    }

    @CfirSymbolProviderInternals
    abstract fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    )

    abstract fun hasPackage(fqName: FqName): Boolean
}
