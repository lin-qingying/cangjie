package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirPackageScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
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
 *
 * @property packageFqName 被查询的包名。
 * @property session 当前 use-site session。
 */
class CfirPackageMemberScope(
    private val packageFqName: FqName,
    val session: CfirSession,

    private val symbolProvider: CfirSymbolProvider = session.symbolProvider,
    private val excludedNames: Set<Name> = emptySet(),

) : CfirPackageScope() {

    /**
     * classifier 查询缓存。
     */
    private val classifierCache = HashMap<Name, List<CfirClassLikeSymbol<*>>>()

    /**
     * callable 查询缓存。
     */
    private val callableCache = HashMap<Name, List<CfirCallableSymbol<*>>>()

    /**
     * 函数查询缓存。
     */
    private val functionCache = HashMap<Name, List<CfirNamedFunctionSymbol>>()

    /**
     * 属性查询缓存。
     */
    private val propertyCache = HashMap<Name, List<CfirPropertySymbol>>()

    /**
     * 返回包内可能存在的 callable 名称集合。
     */
    override fun getCallableNames(): Set<Name> =
        symbolProvider.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageFqName).orEmpty()

    /**
     * 返回包内可能存在的 classifier 名称集合。
     */
    override fun getClassifierNames(): Set<Name> =
        symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName).orEmpty()

    /**
     * 按名称处理包级 classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        val symbols = classifierCache.getOrPut(name) {
            val knownNames = symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName).orEmpty()
            if (name !in knownNames) {
                emptyList()
            } else {
                listOfNotNull(symbolProvider.getClassLikeSymbolByClassId(org.cangnova.cangjie.name.ClassId(packageFqName, name)))
            }
        }
        symbols.forEach(processor)
    }

    /**
     * 按名称处理包级函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        val symbols = functionCache.getOrPut(name) {
            symbolProvider.getTopLevelFunctionSymbols(packageFqName, name)
        }
        symbols.forEach(processor)
    }

    /**
     * 按名称处理包级 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        val symbols = callableCache.getOrPut(name) {
            symbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        }
        symbols.forEach(processor)
    }

    /**
     * 按名称处理包级属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        val symbols = propertyCache.getOrPut(name) {
            symbolProvider.getTopLevelPropertySymbols(packageFqName, name)
        }
        symbols.forEach(processor)
    }

    /**
     * 包 scope 不支持直接跨 session 替换。
     */
    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession,
    ): CfirPackageScope? = null

    /**
     * 返回调试文本。
     */
    override fun toString(): String = "Use site scope of /$packageFqName"
}
