package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirPackageScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 包级声明 scope。
 *
 * 委托 [CfirSymbolProvider] 查找指定包内的顶级函数、属性和类/接口符号。
 * 按名称缓存查找结果以避免重复查询。
 *
 * 参考 K2 FirPackageMemberScope。
 */
class CfirPackageMemberScope(
    private val packageFqName: FqName,
    private val symbolProvider: CfirSymbolProvider,
) : CfirPackageScope() {

    private val classifierCache = HashMap<Name, List<CfirClassLikeSymbol<*>>>()
    private val callableCache = HashMap<Name, List<CfirCallableSymbol<*>>>()
    private val functionCache = HashMap<Name, List<CfirFunctionSymbol<*>>>()
    private val propertyCache = HashMap<Name, List<CfirPropertySymbol>>()

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        val symbols = classifierCache.getOrPut(name) {
            val classId = org.cangnova.cangjie.name.ClassId(packageFqName, FqName.topLevel(name), isLocal = false)
            listOfNotNull(symbolProvider.getClassLikeSymbolByClassId(classId))
        }
        symbols.forEach(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol<*>) -> Unit) {
        val symbols = functionCache.getOrPut(name) {
            symbolProvider.getTopLevelFunctionSymbols(packageFqName, name)
        }
        symbols.forEach(processor)
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        val symbols = callableCache.getOrPut(name) {
            symbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        }
        symbols.forEach(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        val symbols = propertyCache.getOrPut(name) {
            symbolProvider.getTopLevelPropertySymbols(packageFqName, name)
        }
        symbols.forEach(processor)
    }
}
