

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirImportNamespace
import org.cangnova.cangjie.cfir.resolve.providers.CfirImportNamespaceContext
import org.cangnova.cangjie.cfir.resolve.providers.compositeImportNamespace
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * A symbol provider which combines multiple individual symbol providers of the same type. Combined symbol providers typically have an
 * advantage over naively querying the list of individual symbol providers, such as caching or a single index access with a combined scope.
 */
internal abstract class LLCombinedSymbolProvider<P : CfirSymbolProvider>(session: CfirSession) : CfirSymbolProvider(session) {
    /**
     * 当前 combined provider 聚合的底层 provider 列表。
     */
    abstract val providers: List<P>

    /**
     * 聚合底层 provider 中**库侧**声明的名称视图，供名称发现使用。
     *
     * 取的是各子 provider 自己的库侧视图，因此源码符号源不会因组合重新进入名称发现通道：
     * 源码名字由宿主名称索引给出，源码符号源的库侧贡献为空视图。
     *
     * 必须延迟初始化：[providers] 由子类在构造参数中提供，基类初始化期尚不可读。
     */
    override val libraryDeclarationsNamesProvider: CfirSymbolNamesProvider by lazy {
        CfirCompositeCachedSymbolNamesProvider.create(
            session,
            providers.map { it.libraryDeclarationsNamesProvider },
        )
    }

    /** 汇总子源码模块的宏生成声明名称，并保留 snapshot 发布后的实时可见性。 */
    override val macroGeneratedSourceDeclarationsNamesProvider: CfirSymbolNamesProvider by lazy {
        CfirCompositeSymbolNamesProvider.create(
            providers.map { it.macroGeneratedSourceDeclarationsNamesProvider },
        )
    }

    override fun getImportNamespace(context: CfirImportNamespaceContext): CfirImportNamespace =
        compositeImportNamespace(providers, context)

    /** 候选枚举保持各 provider 的全部结果，单结果查询继续维持 classpath 优先级。 */
    override fun getClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>> =
        providers.flatMap { it.getClassLikeSymbolsByClassId(classId) }.distinct()

    /**
     * Estimates the number of symbols contained in the combined symbol provider's own caches. The metric does not include the cache sizes
     * of the individual symbol providers.
     *
     * The purpose of this metric is to estimate the current **cache overhead** of a combined symbol provider.
     */
    abstract fun estimateSymbolCacheSize(): Long
}
