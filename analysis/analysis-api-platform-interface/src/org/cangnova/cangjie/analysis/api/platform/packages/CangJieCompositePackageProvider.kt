package org.cangnova.cangjie.analysis.api.platform.packages

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaCompositeProviderFactory
import org.cangnova.cangjie.analysis.api.platform.CangJieCompositeProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 多个包 provider 的组合实现。
 */
@CaPlatformInterface
class CangJieCompositePackageProvider private constructor(
    /**
     * 被组合的包 provider 列表。
     */
    override val providers: List<CangJiePackageProvider>,
) : CangJiePackageProvider, CangJieCompositeProvider<CangJiePackageProvider> {
    /**
     * 任一 provider 可见该包时返回 true。
     */
    override fun doesPackageExist(packageFqName: FqName): Boolean =
        providers.any { it.doesPackageExist(packageFqName) }

    /**
     * 合并所有 provider 返回的直接子包名称。
     */
    override fun getSubpackageNames(packageFqName: FqName): Set<Name> =
        providers.flatMapTo(mutableSetOf()) { it.getSubpackageNames(packageFqName) }

    @CaPlatformInterface
    companion object {
        /**
         * 包 provider 的标准组合工厂。
         */
        val factory: CaCompositeProviderFactory<CangJiePackageProvider> = CaCompositeProviderFactory(
            CangJieEmptyPackageProvider,
            ::CangJieCompositePackageProvider,
        )

        /**
         * 创建组合包 provider。
         */
        fun create(providers: List<CangJiePackageProvider>): CangJiePackageProvider = factory.create(providers)
    }
}
