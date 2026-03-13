package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
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
    private val providers: List<CfirSymbolProvider>,
) : CfirSymbolProvider() {

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassSymbol? {
        for (provider in providers) {
            val symbol = provider.getClassLikeSymbolByClassId(classId)
            if (symbol != null) return symbol
        }
        return null
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        return providers.flatMap { it.getTopLevelCallableSymbols(packageFqName, name) }
    }

    override fun hasPackage(fqName: FqName): Boolean {
        return providers.any { it.hasPackage(fqName) }
    }
}
