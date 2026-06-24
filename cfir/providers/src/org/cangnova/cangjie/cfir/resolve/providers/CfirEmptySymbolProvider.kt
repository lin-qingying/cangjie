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
    /**
     * 空符号源使用精确为空的名称 provider。
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider
        get() = CfirEmptySymbolNamesProvider

    /**
     * 空符号源不包含任何 class-like symbol。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? = null

    /**
     * 空符号源不会向 [destination] 追加 callable symbol。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    /**
     * 空符号源不会向 [destination] 追加函数 symbol。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    /**
     * 空符号源不会向 [destination] 追加属性 symbol。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    /**
     * 空符号源不拥有任何包。
     */
    override fun hasPackage(fqName: FqName): Boolean = false
}
