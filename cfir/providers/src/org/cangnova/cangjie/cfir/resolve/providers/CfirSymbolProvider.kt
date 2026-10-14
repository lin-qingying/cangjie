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
    /** 在导入命名空间中查询成员；有重导出能力的 provider 必须按导出边解释组织限定。 */
    open fun getImportNamespace(context: CfirImportNamespaceContext): CfirImportNamespace =
        CfirDirectImportNamespace(this, context)

    /**
     * 当前符号源的名称索引。
     *
     * 解析 scope 会先通过该索引做包名与短名过滤，再进入实际 symbol 查询。
     */
    abstract val symbolNamesProvider: CfirSymbolNamesProvider

    /**
     * 当前符号源中**库侧**声明（`.cjo` 反序列化库、stub 库、合成 builtins）的名称视图。
     *
     * 名称发现（按名字枚举项目里有哪些声明）只走该视图，不走 [symbolNamesProvider]：
     * 后者包含源码符号源，而枚举源码名字意味着遍历源码文件——在 IDE 与批量宿主里，
     * 那一步由**宿主的名称索引**承担，而不是由每个符号源自己扫一遍。
     *
     * 默认是**空视图**而不是「无法枚举」：以源码 PSI 为事实来源的符号源，其库侧贡献确定为零，
     * 这是「知道没有」而不是「不知道」。这个区别在组合时是硬约束——
     * 组合方按并集合并，一个「无法枚举」会把整组合拖成 `null`，
     * 于是真实存在的库侧名字会因为某个源码符号源在场而全部消失。
     *
     * 覆写它意味着该符号源的声明**全部**属于库侧（`.cjo`/stub/合成），
     * 漏覆写的表现是该符号源从名称发现通道**缺席**，而不是静默把全项目扫描带回来。
     */
    open val libraryDeclarationsNamesProvider: CfirSymbolNamesProvider
        get() = CfirEmptySymbolNamesProvider

    /**
     * 当前源码模块中已展开宏生成的源码声明名称视图。
     *
     * 这一路只包含宏 snapshot 中来源映射可确认的生成声明；物理源码名称继续由宿主索引发现，
     * 库声明继续走 [libraryDeclarationsNamesProvider]。
     */
    open val macroGeneratedSourceDeclarationsNamesProvider: CfirSymbolNamesProvider
        get() = CfirEmptySymbolNamesProvider

    /**
     * 返回给定 [classId] 的 class-like symbol；若不存在则返回 `null`。
     */
    abstract fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>?

    /**
     * 返回给定 [classId] 的全部 class-like 候选。
     *
     * 单一 provider 默认至多提供一个候选；组合 provider 必须覆盖该方法并聚合全部来源，
     * 供可用性过滤与重声明判定在“选择首项”之前完成。
     */
    open fun getClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>> =
        listOfNotNull(getClassLikeSymbolByClassId(classId))

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

/**
 * 判断 [fqName] 是否是已知包，或是某个已知子包的命名空间前缀。
 *
 * 包符号源的 [CfirSymbolProvider.hasPackage] 保留精确包查询语义；限定名解析则需要接受
 * Kotlin FIR `allSubPackages` 所代表的父级命名空间视图。包名索引保存的是声明包，因此可从
 * 已知完整包名构造这些前缀，而不把任意名字前缀误当作包。
 */
fun CfirSymbolProvider.hasPackageOrSubpackages(fqName: FqName): Boolean {
    if (fqName.isRoot || hasPackage(fqName)) return true

    val subpackagePrefix = "${fqName.asString()}."
    return symbolNamesProvider.getPackageNames()
        ?.any { packageName -> packageName.startsWith(subpackagePrefix) }
        ?: false
}
