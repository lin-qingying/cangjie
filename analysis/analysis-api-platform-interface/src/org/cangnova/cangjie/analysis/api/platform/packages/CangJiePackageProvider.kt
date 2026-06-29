package org.cangnova.cangjie.analysis.api.platform.packages

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CangJieComposableProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * `CangJiePackageProvider` 描述某个上下文里对仓颉可见的包集合。
 *
 * 仓颉主干没有 Kotlin multiplatform 那套 platform-specific package 语义，
 * 因此这里收紧为纯仓颉包存在性与子包查询契约。
 */
@CaPlatformInterface
interface CangJiePackageProvider : CangJieComposableProvider {
    /**
     * 判断指定包是否在当前上下文中可见。
     */
    fun doesPackageExist(packageFqName: FqName): Boolean

    /**
     * 返回指定包的直接子包名称集合。
     */
    fun getSubpackageNames(packageFqName: FqName): Set<Name>
}
