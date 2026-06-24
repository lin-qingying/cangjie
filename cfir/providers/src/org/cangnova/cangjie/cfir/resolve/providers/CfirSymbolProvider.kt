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

/**
 * 标记 provider 内部的批量填充式符号查询 API。
 *
 * 这些 API 直接向调用方提供的 mutable destination 写入结果，避免热路径中反复创建中间集合；
 * 普通调用方应优先使用返回只读列表的公开包装方法。
 */
@RequiresOptIn
annotation class CfirSymbolProviderInternals

/**
 * 对齐 Kotlin FIR `FirSymbolProvider` 的符号查询基类。
 *
 * 该层只负责 symbol lookup 与热路径查询，不承载 owner/container 元信息。
 */
abstract class CfirSymbolProvider(val session: CfirSession) : CfirSessionComponent {
    /**
     * 当前符号源的名称索引。
     *
     * 解析 scope 会先通过该索引做包名与短名过滤，再进入实际 symbol 查询。
     */
    abstract val symbolNamesProvider: CfirSymbolNamesProvider

    /**
     * 返回给定 [classId] 的 class-like symbol；若不存在则返回 `null`。
     */
    abstract fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>?

    /**
     * 返回指定包名与短名下的所有顶层 callable symbol。
     *
     * 该包装方法负责创建结果集合，实际填充由 [getTopLevelCallableSymbolsTo] 完成。
     */
    @OptIn(CfirSymbolProviderInternals::class)
    open fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        return buildList { getTopLevelCallableSymbolsTo(this, packageFqName, name) }
    }

    /**
     * 将指定包名与短名下的顶层 callable symbol 追加到 [destination]。
     *
     * 实现不得清空 [destination]；composite provider 依赖追加语义来聚合多来源结果。
     */
    @CfirSymbolProviderInternals
    abstract fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    )

    /**
     * 返回指定包名与短名下的所有顶层函数 symbol。
     */
    @OptIn(CfirSymbolProviderInternals::class)
    open fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<CfirNamedFunctionSymbol> {
        return buildList { getTopLevelFunctionSymbolsTo(this, packageFqName, name) }
    }

    /**
     * 将指定包名与短名下的顶层函数 symbol 追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    abstract fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    )

    /**
     * 返回指定包名与短名下的所有顶层属性 symbol。
     */
    @OptIn(CfirSymbolProviderInternals::class)
    open fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<CfirPropertySymbol> {
        return buildList { getTopLevelPropertySymbolsTo(this, packageFqName, name) }
    }

    /**
     * 将指定包名与短名下的顶层属性 symbol 追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    abstract fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    )

    /**
     * 判断该符号源是否可能提供 [fqName] 包下的声明。
     */
    abstract fun hasPackage(fqName: FqName): Boolean
}
