package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 跨模块符号查询基类。
 *
 * 提供从本模块和依赖模块中查找符号的统一入口。
 * 参考 K2 FirSymbolProvider（abstract class，vtable dispatch）。
 */
abstract class CfirSymbolProvider : CfirSessionComponent {

    /** 名称过滤器，子类可覆盖以提供快速过滤 */
    open val symbolNamesProvider: CfirSymbolNamesProvider
        get() = CfirSymbolNamesProvider.NO_FILTERING

    /** 通过 ClassId 查找类符号 */
    abstract fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassSymbol?

    /** 查找指定包下的顶级可调用符号 */
    abstract fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>>

    /** 检查指定包是否存在 */
    abstract fun hasPackage(fqName: FqName): Boolean

    /** 查找指定包下的顶级函数符号（默认从 getTopLevelCallableSymbols 过滤） */
    open fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<CfirFunctionSymbol> {
        return getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<CfirFunctionSymbol>()
    }

    /** 查找指定包下的顶级属性符号（默认从 getTopLevelCallableSymbols 过滤） */
    open fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<CfirPropertySymbol> {
        return getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<CfirPropertySymbol>()
    }
}
