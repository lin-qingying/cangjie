package org.cangnova.cangjie.name

/**
 * 仓颉 class-like 声明的稳定标识。
 *
 * 仓颉当前只有顶层 class-like 声明进入公开类型标识体系，因此 `ClassId` 只表示：
 * - 包名
 * - 顶层声明名
 *
 * `relativeClassName` 在这里保留 Kotlin 兼容字段名，但语义已经收紧为“顶层声明名”。
 * 它不再承载任何层级化的类型声明结构。
 */
data class ClassId(
    val packageFqName: FqName,
    val relativeClassName: FqName,
) {
    constructor(packageFqName: FqName, topLevelName: Name) : this(
        packageFqName,
        FqName.topLevel(topLevelName),
    )

    init {
        assert(!relativeClassName.isRoot) {
            "Class name must not be root: $packageFqName"
        }
        assert(relativeClassName.parent().isRoot) {
            "Cangjie ClassId must point to a top-level declaration: $packageFqName/$relativeClassName"
        }
    }

    val shortClassName: Name
        get() = relativeClassName.shortName()

    fun asSingleFqName(): FqName =
        if (packageFqName.isRoot) relativeClassName
        else FqName(packageFqName.asString() + "." + relativeClassName.asString())

    fun startsWith(segment: Name): Boolean = packageFqName.startsWith(segment)

    fun asString(): String =
        if (packageFqName.isRoot) {
            relativeClassName.asString()
        } else {
            buildString {
                append(packageFqName.asString().replace('.', '/'))
                append("/")
                append(relativeClassName.asString())
            }
        }

    fun asFqNameString(): String =
        if (packageFqName.isRoot) {
            relativeClassName.asString()
        } else {
            buildString {
                append(packageFqName.asString())
                append(".")
                append(relativeClassName.asString())
            }
        }

    override fun toString(): String =
        if (packageFqName.isRoot) "/$relativeClassName" else asString()

    companion object {
        @JvmStatic
        fun topLevel(topLevelFqName: FqName): ClassId =
            ClassId(topLevelFqName.parent(), topLevelFqName.shortName())

        @JvmStatic
        fun fromString(string: String): ClassId {
            val lastSlashIndex = string.lastIndexOf("/")
            val packageName: String
            val className: String
            if (lastSlashIndex == -1) {
                packageName = ""
                className = string
            } else {
                packageName = string.substring(0, lastSlashIndex).replace('/', '.')
                className = string.substring(lastSlashIndex + 1)
            }
            return ClassId(FqName(packageName), FqName(className))
        }
    }
}
