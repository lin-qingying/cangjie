package org.cangnova.cangjie.source

/**
 * 不依赖 PSI 的源码位置。
 *
 * 用于承载序列化元数据中保留的原始路径和一基行列，保证二进制来源仍能进入统一诊断通道。
 */
data class CjSourceLocation(
    /** 原始源码路径。 */
    val filePath: String,
    /** 一基源码行号。 */
    val line: Int,
    /** 一基源码列号。 */
    val column: Int,
) {
    init {
        require(filePath.isNotBlank()) { "A source location must have a file path" }
        require(line > 0) { "A source location line must be one-based" }
        require(column > 0) { "A source location column must be one-based" }
    }
}
