package org.cangnova.cangjie.cfir.serialization

/**
 * .cjo 文件格式常量。
 *
 * .cjo 是仓颉编译器的编译产物格式，使用 FlatBuffers 序列化。
 */
object CjoConstants {
    /** FlatBuffers 文件标识符 */
    const val FILE_IDENTIFIER = "CJOF"

    /** .cjo 文件扩展名 */
    const val FILE_EXTENSION = ".cjo"

    /** 当前支持的 .cjo 格式主版本。 */
    const val VERSION_MAJOR: Int = 1
    /** 当前支持的 .cjo 格式次版本。 */
    const val VERSION_MINOR: Int = 0
    /** 当前支持的 .cjo 格式补丁版本。 */
    const val VERSION_PATCH: Int = 5

    /** 包名分隔符（仓颉用 '.' 分隔包路径） */
    const val PACKAGE_SEPARATOR = '.'

    /** 将完整包名转换为 .cjo 文件的相对路径，如 "std.core" → "std/core.cjo" */
    fun packageNameToPath(fullPkgName: String): String {
        return fullPkgName.replace(PACKAGE_SEPARATOR, '/') + FILE_EXTENSION
    }
}
