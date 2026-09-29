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
    /** 组织限定符独立于包路径，并参与导入身份比较。 */
    val organizationName: Name? = null,
) {

    /**
     * 不含 alias 的路径字符串。
     */
    val pathStr: String
        get() = (organizationName?.let { "${it.render()}::" } ?: "") +
            fqName.toUnsafe().render() + if (isAllUnder) ".*" else ""

    /**
     * 渲染完整 import path 字符串。
     */
    override fun toString(): String {
        return pathStr + if (alias != null) " as " + alias.render() else ""
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
        private const val IDENTIFIER_PATTERN = "(?:[_\\p{L}][_\\p{L}\\p{N}]*|`[^`\\r\\n]+`)"
        private val IDENTIFIER_REGEX = Regex(IDENTIFIER_PATTERN)
        private val IMPORT_PATTERN = Regex(
            "^(?:($IDENTIFIER_PATTERN)::)?($IDENTIFIER_PATTERN(?:\\.$IDENTIFIER_PATTERN)*)(\\.\\*)?(?:\\s+as\\s+($IDENTIFIER_PATTERN))?$",
        )

        /**
         * 从字符串解析 import path。
         */
        @JvmStatic
        fun fromString(pathStr: String): ImportPath {
            val match = requireNotNull(IMPORT_PATTERN.matchEntire(pathStr.trim())) { "Invalid import path: $pathStr" }
            val organizationName = match.groups[1]?.value?.let { Name.identifier(it.removeSurrounding("`")) }
            val fqName = FqName.fromSegments(
                IDENTIFIER_REGEX.findAll(match.groupValues[2]).map { it.value.removeSurrounding("`") }.toList(),
            )
            val isAllUnder = match.groups[3] != null
            val alias = match.groups[4]?.value?.let { Name.identifier(it.removeSurrounding("`")) }
            require(!isAllUnder || alias == null) { "An all-under import cannot have an alias: $pathStr" }
            return ImportPath(fqName, isAllUnder, alias, organizationName)
        }
    }
}

/** 导入路径的组织与包部分；组织限定分组允许包路径为根。 */
data class ImportPathPrefix(val organizationName: Name?, val fqName: FqName)

/** 按语法归属组合分组前缀和局部路径，重复的包段仍须保留。 */
fun ImportPathPrefix?.resolveImportPath(
    localFqName: FqName,
    localOrganizationName: Name? = null,
): ImportPathPrefix {
    if (this == null) return ImportPathPrefix(localOrganizationName, localFqName)
    require(localOrganizationName == null) { "A grouped import cannot introduce another organization qualifier" }
    return ImportPathPrefix(organizationName, fqName.child(localFqName))
}
