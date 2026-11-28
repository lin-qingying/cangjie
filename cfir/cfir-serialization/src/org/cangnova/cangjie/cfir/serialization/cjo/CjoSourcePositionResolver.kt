package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Package
import PackageFormat.Position
import org.cangnova.cangjie.source.CjSourceLocation

/** CJO Position 独立于 FullId：当前包文件一基，导入包一基但其文件零基。 */
internal object CjoSourcePositionResolver {
    /** 按位置自身的包与文件身份读取源码位置；无效或不可用的位置不生成诊断锚点。 */
    fun resolve(
        pkg: Package,
        position: Position?,
        packages: CjoLoadedPackageProvider,
    ): CjSourceLocation? {
        if (position == null || position.line <= 0 || position.column <= 0) return null
        val sourcePackage: Package
        val fileIndex: Long
        if (position.pkgId == 0u) {
            sourcePackage = pkg
            fileIndex = position.file.toLong() - 1L
        } else {
            val importIndex = position.pkgId.toLong() - 1L
            if (importIndex !in 0 until pkg.importsLength.toLong()) return null
            val packageName = pkg.imports(importIndex.toInt())?.takeIf(String::isNotBlank) ?: return null
            sourcePackage = packages.loadPackageSnapshot(packageName)?.pkg ?: return null
            fileIndex = position.file.toLong()
        }
        if (fileIndex !in 0 until sourcePackage.allFilesLength.toLong()) return null
        val filePath = sourcePackage.allFiles(fileIndex.toInt())?.takeIf(String::isNotBlank) ?: return null
        return CjSourceLocation(filePath, position.line, position.column)
    }
}
