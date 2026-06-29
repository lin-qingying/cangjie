package org.cangnova.cangjie.analysis.api.platform.packages

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 不提供任何包信息的空包 provider。
 */
@CaPlatformInterface
object CangJieEmptyPackageProvider : CangJiePackageProvider {
    /**
     * 空 provider 中任何包都不存在。
     */
    override fun doesPackageExist(packageFqName: FqName): Boolean = false

    /**
     * 空 provider 不返回子包名称。
     */
    override fun getSubpackageNames(packageFqName: FqName): Set<Name> = emptySet()
}
