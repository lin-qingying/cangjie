package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.noSubPackage
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.descriptors.Visibilities

/**
 * source import 的重导出视图。
 *
 * 这里只表达“本包可见名 -> 被导入包真实名”的映射，
 * 后续 provider 再决定去 source 还是 delegated provider 取 symbol。
 *
 * @property importedPackageFqName 被导入声明真实所在包。
 * @property importedName 被导入声明短名；星号导入时为 `null`。
 * @property exportedName 当前包对外暴露的短名；星号导入时为 `null`。
 * @property isAllUnder 是否为星号导入。
 */
internal data class CfirReexportImportInfo(
    /**
     * 被重导出声明真实所在的包名。
     */
    val importedPackageFqName: FqName,
    /** 被导入包的组织限定，与普通包路径分开传递。 */
    val organizationName: Name?,
    /**
     * 被重导出的具体短名；星号导入会保持为 `null`，由目标包名称集合决定。
     */
    val importedName: Name?,
    /**
     * 当前包向外暴露的短名；别名导入时等于 alias，星号导入时为 `null`。
     */
    val exportedName: Name?,
    /**
     * 是否为 all-under import，决定 provider 是否合并目标包的完整顶层名称集合。
     */
    val isAllUnder: Boolean,
)

/**
 * 仓颉 `public/protected import` 会把导入成员作为本包 API 重导出；
 * 默认 `import` 等价于 `private import`，不参与重导出。
 */
fun CfirImport.isReexportingSourceImport(): Boolean =
    visibility == Visibilities.Public || visibility == Visibilities.Protected

/**
 * 判断 import 是否对同包其它源码文件可见。
 *
 * `internal import` 不属于 public/protected API 重导出，但它会参与包级名称解析；
 * unused-import 检查仍需要基于包级使用情况决定是否报告。
 */
fun CfirImport.isPackageVisibleSourceImport(): Boolean =
    visibility == Visibilities.Public || visibility == Visibilities.Protected || visibility == Visibilities.Internal

/**
 * 判断当前 import 是否免于未使用导入检查。
 *
 * `public/protected import` 始终作为包 API 重导出；`internal import` 仅在支持子包的默认
 * 编译模式下豁免。private 与未显式标注可见性的 import 始终参加文件级检查。
 */
fun CfirImport.isUnusedImportCheckExempt(session: CfirSession): Boolean =
    when (visibility) {
        Visibilities.Public, Visibilities.Protected -> true
        Visibilities.Internal -> !session.noSubPackage
        else -> false
    }

/**
 * 将当前 import 转换为 source provider 可使用的 reexport 信息。
 */
internal fun CfirImport.reexportInfoOrNull(): CfirReexportImportInfo? {
    if (!isPackageVisibleSourceImport()) return null

    val importedFqName = importedFqName?.takeUnless { it.isRoot } ?: return null
    val importedPackageFqName = if (isAllUnder) importedFqName else importedFqName.parent()
    val importedName = importedFqName.shortName().takeUnless { isAllUnder }
    val exportedName = importedName?.let { aliasName ?: it }
    return CfirReexportImportInfo(
        importedPackageFqName = importedPackageFqName,
        organizationName = organizationName,
        importedName = importedName,
        exportedName = exportedName,
        isAllUnder = isAllUnder,
    )
}
