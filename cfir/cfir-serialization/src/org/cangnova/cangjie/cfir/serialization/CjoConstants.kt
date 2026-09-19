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

    /**
     * 官方 ModuleFormat.CjoVersion 的格式主版本。
     *
     * 这是二进制 schema 版本，不是仓颉语言发布版本；官方 v1.0.0 到
     * v1.1.3 的 CJO 头部均使用 0.1.0。
     */
    const val VERSION_MAJOR: Int = 0
    /** 官方 CJO 格式次版本。 */
    const val VERSION_MINOR: Int = 1
    /** 官方 CJO 格式修订版本。 */
    const val VERSION_PATCH: Int = 0

    /** 包名分隔符（仓颉用 '.' 分隔包路径） */
    const val PACKAGE_SEPARATOR = '.'

    /** 将完整包名转换为 .cjo 文件的相对路径，如 "std.core" → "std/core.cjo" */
    fun packageNameToPath(fullPkgName: String): String {
        return fullPkgName.replace(PACKAGE_SEPARATOR, '/') + FILE_EXTENSION
    }
}
