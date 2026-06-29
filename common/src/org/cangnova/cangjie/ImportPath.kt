package org.cangnova.cangjie

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * import 声明中的导入路径。
 */
data class ImportPath @JvmOverloads constructor(
    /**
     * 被导入目标的完整限定名。
     */
    val fqName: FqName,
    /**
     * 是否为星号导入。
     */
    val isAllUnder: Boolean,
    /**
     * import alias 名称。
     */
    val alias: Name? = null,
) {

    /**
     * 不含 alias 的路径字符串。
     */
    val pathStr: String
        get() = fqName.toUnsafe().render() + if (isAllUnder) ".*" else ""

    /**
     * 渲染完整 import path 字符串。
     */
    override fun toString(): String {
        return pathStr + if (alias != null) " as " + alias.asString() else ""
    }

    /**
     * 判断该 import 是否声明了 alias。
     */
    fun hasAlias(): Boolean {
        return alias != null
    }

    /**
     * 返回实际引入作用域的短名称；星号导入没有单一名称。
     */
    val importedName: Name?
        get() {
            if (!isAllUnder) {
                return alias ?: fqName.shortName()
            }

            return null
        }

    companion object {
        /**
         * 从字符串解析 import path。
         */
        @JvmStatic
        fun fromString(pathStr: String): ImportPath {
            return if (pathStr.endsWith(".*")) {
                ImportPath(FqName(pathStr.substring(0, pathStr.length - 2)), isAllUnder = true)
            } else {
                ImportPath(FqName(pathStr), isAllUnder = false)
            }
        }
    }
}
