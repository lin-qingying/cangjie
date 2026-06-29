package org.cangnova.cangjie.name

import java.util.regex.Pattern

/**
 * 可包含特殊名称片段的完整限定名。
 *
 * 与 [FqName] 不同，该类型允许 `<anonymous>` 等编译器内部特殊名称出现在路径中。
 */
class FqNameUnsafe {
    /**
     * 点分隔形式的完整限定名字符串。
     */
    private val fqName: String

    /**
     * 已转换出的安全 FqName 缓存。
     */
    @Transient
    private var safe: FqName? = null

    /**
     * 父级完整限定名缓存。
     */
    @Transient
    private var parent: FqNameUnsafe? = null

    /**
     * 最后一个路径片段缓存。
     */
    @Transient
    private var shortName: Name? = null

    internal constructor(fqName: String, safe: FqName) {
        this.fqName = fqName
        this.safe = safe
    }

    constructor(fqName: String) {
        this.fqName = fqName
    }

    private constructor(fqName: String, parent: FqNameUnsafe, shortName: Name) {
        this.fqName = fqName
        this.parent = parent
        this.shortName = shortName
    }

    /**
     * 惰性计算父级限定名和短名称缓存。
     */
    private fun compute() {
        val lastDot = fqName.lastIndexOf('.')
        if (lastDot >= 0) {
            shortName = Name.guessByFirstCharacter(fqName.substring(lastDot + 1))
            parent = FqNameUnsafe(fqName.substring(0, lastDot))
        } else {
            shortName = Name.guessByFirstCharacter(fqName)
            parent = FqName.ROOT.toUnsafe()
        }
    }

    /**
     * 返回点分隔形式的限定名字符串。
     */
    fun asString(): String = fqName

    /**
     * 当前限定名是否可以转换为安全 [FqName]。
     */
    val isSafe: Boolean
        get() = safe != null || asString().indexOf('<') < 0

    /**
     * 转换为安全 FqName，并缓存结果。
     */
    fun toSafe(): FqName {
        if (safe != null) return safe!!
        safe = FqName(this)
        return safe!!
    }

    /**
     * 是否表示根包。
     */
    val isRoot: Boolean
        get() = fqName.isEmpty()

    /**
     * 返回父级完整限定名；根包没有父级。
     */
    fun parent(): FqNameUnsafe {
        if (parent != null) return parent!!
        check(!isRoot) { "root" }
        compute()
        return parent!!
    }

    /**
     * 在当前限定名下追加一个名称片段。
     */
    fun child(name: Name): FqNameUnsafe {
        val childFqName = if (isRoot) name.asString() else fqName + "." + name.asString()
        return FqNameUnsafe(childFqName, this, name)
    }

    /**
     * 在当前限定名下追加另一个安全限定名的所有片段。
     */
    fun child(fqname: FqName): FqNameUnsafe {
        var current = this
        for (name in fqname.pathSegments()) {
            current = current.child(name)
        }
        return current
    }

    /**
     * 返回最后一个名称片段；根包没有短名称。
     */
    fun shortName(): Name {
        if (shortName != null) return shortName!!
        check(!isRoot) { "root" }
        compute()
        return shortName!!
    }

    /**
     * 返回短名称；根包返回特殊根名称。
     */
    fun shortNameOrSpecial(): Name =
        if (isRoot) ROOT_NAME else shortName()

    /**
     * 返回限定名的路径片段列表。
     */
    fun pathSegments(): List<Name> =
        if (isRoot) emptyList() else SPLIT_BY_DOTS.split(fqName).map(STRING_TO_NAME)

    /**
     * 判断限定名第一个片段是否等于指定名称。
     */
    fun startsWith(segment: Name): Boolean {
        if (isRoot) return false
        val firstDot = fqName.indexOf('.')
        val segmentAsString = segment.asString()
        return fqName.regionMatches(
            0,
            segmentAsString,
            0,
            if (firstDot == -1) maxOf(fqName.length, segmentAsString.length) else firstDot,
        )
    }

    /**
     * 判断当前限定名是否以另一个限定名为前缀。
     */
    fun startsWith(other: FqNameUnsafe): Boolean {
        if (isRoot) return false
        val thisLength = fqName.length
        val otherLength = other.fqName.length
        if (thisLength < otherLength) return false
        return (thisLength == otherLength || fqName[otherLength] == '.') &&
            fqName.regionMatches(0, other.fqName, 0, otherLength)
    }

    /**
     * 返回用于调试展示的限定名字符串。
     */
    override fun toString(): String = if (isRoot) ROOT_NAME.asString() else fqName

    /**
     * 按限定名字符串判断相等。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FqNameUnsafe) return false
        return fqName == other.fqName
    }

    /**
     * 返回限定名字符串的哈希值。
     */
    override fun hashCode(): Int = fqName.hashCode()

    companion object {
        /**
         * 根包短名称的特殊表示。
         */
        private val ROOT_NAME = Name.special("<root>")
        /**
         * 拆分限定名路径片段的点号正则。
         */
        private val SPLIT_BY_DOTS: Pattern = Pattern.compile("\\.")
        /**
         * 从字符串片段恢复 [Name] 的转换函数。
         */
        private val STRING_TO_NAME: (String) -> Name = { Name.guessByFirstCharacter(it) }

        /**
         * 判断字符串是否可以作为基础限定名输入。
         */
        fun isValid(qualifiedName: String?): Boolean =
            qualifiedName != null && qualifiedName.indexOf('/') < 0 && qualifiedName.indexOf('*') < 0

        /**
         * 构造只有一个短名称片段的顶层 unsafe FqName。
         */
        fun topLevel(shortName: Name): FqNameUnsafe =
            FqNameUnsafe(shortName.asString(), FqName.ROOT.toUnsafe(), shortName)
    }
}
