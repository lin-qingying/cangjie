

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirDelegatingCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 从仓颉平台声明索引读取顶层符号名字集合的 CFIR 名称提供器。
 *
 * 该类型只负责名字级索引，不创建符号本体。CFIR 作用域和组合符号提供器通过它判断包内可能存在的
 * class-like 与 callable 名字，从而避免在补全、作用域枚举和独立模式下反复扫描 PSI。
 */
@OptIn(CaPlatformInterface::class)
internal open class LLCfirCangJieSymbolNamesProvider(
    /**
     * 提供包名、顶层 class-like 名称和顶层 callable 名称的平台声明索引。
     */
    private val declarationProvider: CangJieDeclarationProvider,
) : CfirSymbolNamesProvider() {
    /**
     * 返回平台可枚举的全部包名；当平台无法精确枚举时返回 `null`。
     */
    override fun getPackageNames(): Set<String>? =
        declarationProvider.computePackageNames()

    /**
     * 标记平台是否能够只针对含有顶层 class-like 声明的包进行精确枚举。
     */
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = declarationProvider.hasSpecificClassifierPackageNamesComputation

    /**
     * 返回包含顶层 class-like 声明的包名集合；当平台无法精确枚举时返回 `null`。
     */
    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? =
        declarationProvider.computePackageNamesWithTopLevelClassifiers()

    /**
     * 返回指定 [packageFqName] 内的顶层 class-like 简名集合。
     */
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
        declarationProvider.getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName)

    /**
     * 标记平台是否能够只针对含有顶层 callable 声明的包进行精确枚举。
     */
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = declarationProvider.hasSpecificCallablePackageNamesComputation

    /**
     * 返回包含顶层 callable 声明的包名集合；当平台无法精确枚举时返回 `null`。
     */
    override fun getPackageNamesWithTopLevelCallables(): Set<String>? =
        declarationProvider.computePackageNamesWithTopLevelCallables()

    /**
     * 返回指定 [packageFqName] 内的顶层 callable 简名集合。
     */
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> =
        declarationProvider.getTopLevelCallableNamesInPackage(packageFqName).ifEmpty { emptySet() }

    companion object {
        /**
         * 为 [declarationProvider] 创建绑定到 [session] 的缓存名称提供器。
         *
         * 返回值把当前委托实现包进 [CfirDelegatingCachedSymbolNamesProvider]，由会话级缓存统一管理名称集合生命周期。
         */
        fun cached(
            session: CfirSession,
            declarationProvider: CangJieDeclarationProvider,
        ): CfirCachedSymbolNamesProvider =
            CfirDelegatingCachedSymbolNamesProvider(session, LLCfirCangJieSymbolNamesProvider(declarationProvider))
    }
}
