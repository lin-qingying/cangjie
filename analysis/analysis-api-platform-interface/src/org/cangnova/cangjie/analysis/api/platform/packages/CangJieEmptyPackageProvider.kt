package org.cangnova.cangjie.analysis.api.platform.packages

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

@CaPlatformInterface
object CangJieEmptyPackageProvider : CangJiePackageProvider {
    override fun doesPackageExist(packageFqName: FqName): Boolean = false

    override fun getSubpackageNames(packageFqName: FqName): Set<Name> = emptySet()
}
