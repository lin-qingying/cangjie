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
 * 空符号源，对齐 Kotlin FIR 的 empty symbol provider。
 */
class CfirEmptySymbolProvider(session: CfirSession) : CfirSymbolProvider(session) {
    override val symbolNamesProvider: CfirSymbolNamesProvider
        get() = CfirEmptySymbolNamesProvider

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? = null

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    override fun hasPackage(fqName: FqName): Boolean = false
}
