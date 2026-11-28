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
    override fun getImportNamespace(context: CfirImportNamespaceContext): CfirImportNamespace =
        compositeImportNamespace(providers, context)

    /**
     * 聚合后的名称过滤 provider。
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider =
        CfirCompositeSymbolNamesProvider.fromSymbolProviders(providers)

    /**
     * 聚合子 provider 中的**库侧**名称视图。
     *
     * 只并入各子 provider 自己的库侧视图，因此源码符号源不会因组合而重新进入名称发现通道：
     * 源码符号源的库侧贡献为空视图，并入它不改变并集。
     */
    override val libraryDeclarationsNamesProvider: CfirSymbolNamesProvider =
        CfirCompositeSymbolNamesProvider.create(providers.map { it.libraryDeclarationsNamesProvider })

    /** 聚合各源码模块经宏展开生成的声明名称，不把普通源码名称重新带入名称发现通道。 */
    override val macroGeneratedSourceDeclarationsNamesProvider: CfirSymbolNamesProvider =
        CfirCompositeSymbolNamesProvider.create(providers.map { it.macroGeneratedSourceDeclarationsNamesProvider })

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
        selectCompositeClassifierCandidates(providers.flatMap { it.getClassLikeSymbolsByClassId(classId) })

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

    /** 汇总各反序列化来源的加载诊断，供消费侧文件 checker 统一报告。 */
    override fun getCjmpLoadDiagnostics(packageFqName: FqName) =
        providers.flatMap { it.getCjmpLoadDiagnostics(packageFqName) }.distinct()

    override fun getCjmpLoadDiagnostics(packageFqName: FqName, consumerSession: CfirSession) =
        providers.flatMap { it.getCjmpLoadDiagnostics(packageFqName, consumerSession) }.distinct()
}
