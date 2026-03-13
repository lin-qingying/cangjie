package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 最小可用的符号 provider（K2 对齐：空符号源）。
 */
class CfirEmptySymbolProvider : CfirSymbolProvider() {
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassSymbol? = null

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> = emptyList()

    override fun hasPackage(fqName: FqName): Boolean = false
}

