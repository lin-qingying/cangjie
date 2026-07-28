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
 * 组合多个 [CfirSymbolProvider]，仅负责 symbol lookup 聚合。
 */
class CfirCompositeSymbolProvider(
    session: CfirSession,
    /**
     * 按查询优先级排列的符号 provider。
     *
     * class-like 查询返回第一个命中项，顶层 callable 查询追加所有来源的结果。
     */
    val providers: List<CfirSymbolProvider>,
) : CfirSymbolProvider(session) {
    /**
     * 聚合后的名称过滤 provider。
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider =
        CfirCompositeSymbolNamesProvider.fromSymbolProviders(providers)

    /**
     * 按 provider 顺序返回第一个 class-like symbol 命中项。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        for (provider in providers) {
            provider.getClassLikeSymbolByClassId(classId)?.let { return it }
        }
        return null
    }

    /** 聚合所有子 provider 的 class-like 候选，不在 provider 层提前丢失同身份声明。 */
    override fun getClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>> =
        providers.flatMap { provider -> provider.getClassLikeSymbolsByClassId(classId) }.distinct()

    /**
     * 将所有子 provider 命中的顶层 callable symbol 追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
        for (provider in providers) {
            provider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
        }
    }

    /**
     * 将所有子 provider 命中的顶层函数 symbol 追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        for (provider in providers) {
            provider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        }
    }

    /**
     * 将所有子 provider 命中的顶层属性 symbol 追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        for (provider in providers) {
            provider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        }
    }

    /**
     * 任意子 provider 拥有指定包时返回 `true`。
     */
    override fun hasPackage(fqName: FqName): Boolean =
        providers.any { it.hasPackage(fqName) }
}
