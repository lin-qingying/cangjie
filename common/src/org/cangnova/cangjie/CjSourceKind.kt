package org.cangnova.cangjie

/**
 * 源文件种类。
 *
 * 与 [org.cangnova.cangjie.source.CjSourceElementKind]（真/假来源）正交：
 * 后者描述"这个元素是否来自真实源码"，本类型描述"这个文件承载的是实现还是声明"。
 */
enum class CjSourceKind {
    /** `.cj` 实现源文件。 */
    SOURCE,

    /** `.cj.d` 声明源文件。 */
    DECLARATION,

    /** `.cj.macrocall` 宏调用文件（由 `CangJieMacroCallFileType` 承载）。 */
    MACRO_CALL,
    ;

    /** 该种类是否为声明文件。 */
    val isDeclaration: Boolean get() = this == DECLARATION

    companion object {
        /** 官方 `CJ_D_FILE_EXTENSION`。 */
        const val DECLARATION_SUFFIX: String = ".cj.d"

        /** 官方 `CJ_EXTENSION`。 */
        const val SOURCE_SUFFIX: String = ".cj"

        /** `.cj.macrocall` 后缀（宏调用文件）。 */
        const val MACRO_CALL_SUFFIX: String = ".cj.macrocall"

        /**
         * 按文件名判定种类。
         *
         * 对齐官方 `HasCJDExtension`：字面后缀匹配，且后缀之前必须存在非空文件名。
         * 因此 `.cj.d` 与 `a.cj.d` 判定不同 —— 前者返回 [SOURCE]。
         */
        fun fromFileName(name: String): CjSourceKind = when {
            name.length > MACRO_CALL_SUFFIX.length && name.endsWith(MACRO_CALL_SUFFIX) -> MACRO_CALL
            name.length > DECLARATION_SUFFIX.length && name.endsWith(DECLARATION_SUFFIX) -> DECLARATION
            else -> SOURCE
        }
    }
}
