package org.cangnova.cangjie.analysis.api.platform.packages

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaCompositeProviderFactory
import org.cangnova.cangjie.analysis.api.platform.CangJieCompositeProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

@CaPlatformInterface
class CangJieCompositePackageProvider private constructor(
    override val providers: List<CangJiePackageProvider>,
) : CangJiePackageProvider, CangJieCompositeProvider<CangJiePackageProvider> {
    override fun doesPackageExist(packageFqName: FqName): Boolean =
        providers.any { it.doesPackageExist(packageFqName) }

    override fun getSubpackageNames(packageFqName: FqName): Set<Name> =
        providers.flatMapTo(mutableSetOf()) { it.getSubpackageNames(packageFqName) }

    @CaPlatformInterface
    companion object {
        val factory: CaCompositeProviderFactory<CangJiePackageProvider> = CaCompositeProviderFactory(
            CangJieEmptyPackageProvider,
            ::CangJieCompositePackageProvider,
        )

        fun create(providers: List<CangJiePackageProvider>): CangJiePackageProvider = factory.create(providers)
    }
}
