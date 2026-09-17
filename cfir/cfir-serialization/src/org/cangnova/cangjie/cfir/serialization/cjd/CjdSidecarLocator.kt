package org.cangnova.cangjie.cfir.serialization.cjd

import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import java.nio.file.Files
import java.nio.file.Path

/** 定位与 `.cjo` 同目录、同名的声明元数据文件，不搜索其它目录。 */
object CjdSidecarLocator {
    /** 仅替换文件名的末尾后缀；非法的 `.cjo` 路径属于调用方契约错误。 */
    fun deriveCjdPath(cjoPath: Path): Path {
        val fileName = cjoPath.fileName?.toString()
        require(fileName != null && fileName.endsWith(CjoConstants.FILE_EXTENSION)) {
            "not a cjo path: $cjoPath"
        }
        val stem = fileName.removeSuffix(CjoConstants.FILE_EXTENSION)
        return cjoPath.resolveSibling(stem + CjSourceKind.DECLARATION_SUFFIX)
    }

    /** 缺失、非普通文件或不可读时静默跳过；不缓存，允许后续加载发现新文件。 */
    fun findReadable(cjoPath: Path): Path? =
        deriveCjdPath(cjoPath).takeIf { Files.isRegularFile(it) && Files.isReadable(it) }
}
