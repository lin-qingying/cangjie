package org.cangnova.cangjie.name

/**
 * 只包含普通名称片段的安全完整限定名。
 */
class FqName {
    /**
     * 底层 unsafe 表示，用于复用路径操作实现。
     */
    private val fqName: FqNameUnsafe

    /**
     * 父级 FqName 缓存。
     */
    @Transient
    private var parent: FqName? = null

    constructor(fqName: String) {
        this.fqName = FqNameUnsafe(fqName, this)
    }

    constructor(fqName: FqNameUnsafe) {
        this.fqName = fqName
    }

    private constructor(fqName: FqNameUnsafe, parent: FqName) {
        this.fqName = fqName
        this.parent = parent
    }

    /**
     * 返回点分隔形式的限定名字符串。
     */
    fun asString(): String = fqName.asString()

    /**
     * 返回底层 unsafe 表示。
     */
    fun toUnsafe(): FqNameUnsafe = fqName

    /**
     * 是否表示根包。
     */
    val isRoot: Boolean
        get() = fqName.isRoot

    /**
     * 返回父级限定名；根包没有父级。
     */
    fun parent(): FqName {
        if (parent != null) return parent!!
        check(!isRoot) { "this is root: ${this.asString()}" }
        parent = FqName(fqName.parent())
        return parent!!
    }

    /**
     * 返回限定名的第一个路径片段。
     */
    fun firstSegment(): Name? {
        if (isRoot) return null
        var current: FqName = this
        while (!current.parent().isRoot) {
            current = current.parent()
        }
        return current.shortName()
    }

    /**
     * 判断限定名是否只有一个路径片段。
     */
    fun isSingleSegment(): Boolean {
        if (isRoot) return false
        return parent().isRoot
    }

    /**
     * 在当前限定名下追加一个名称片段。
     */
    fun child(name: Name): FqName = FqName(fqName.child(name), this)

    /**
     * 在当前限定名下追加另一个限定名的所有片段。
     */
    fun child(name: FqName): FqName = FqName(fqName.child(name), this)

    /**
     * 返回最后一个名称片段。
     */
    fun shortName(): Name = fqName.shortName()

    /**
     * 返回短名称；根包返回特殊根名称。
     */
    fun shortNameOrSpecial(): Name = fqName.shortNameOrSpecial()

    /**
     * 返回所有路径片段。
     */
    fun pathSegments(): List<Name> = fqName.pathSegments()

    /**
     * 判断第一个路径片段是否等于指定名称。
     */
    fun startsWith(segment: Name): Boolean = fqName.startsWith(segment)

    /**
     * 判断当前限定名是否以另一个限定名为前缀。
     */
    fun startsWith(other: FqName): Boolean = fqName.startsWith(other.fqName)

    /**
     * 返回调试展示用的限定名字符串。
     */
    override fun toString(): String = fqName.toString()

    /**
     * 按底层限定名判断相等。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FqName) return false
        return fqName == other.fqName
    }

    /**
     * 返回底层限定名哈希值。
     */
    override fun hashCode(): Int = fqName.hashCode()

    companion object {
        /**
         * 根包 FqName。
         */
        @JvmField
        val ROOT: FqName = FqName("")

        /**
         * 从字符串路径片段构造 FqName。
         */
        fun fromSegments(names: List<String>): FqName = FqName(names.joinToString("."))

        /**
         * 构造只有一个短名称片段的顶层 FqName。
         */
        @JvmStatic
        fun topLevel(shortName: Name): FqName = FqName(FqNameUnsafe.topLevel(shortName))

        /**
         * 从点分隔字符串构造 FqName。
         */
        @JvmStatic
        fun fromString(fqName: String): FqName {
            val segments = fqName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var current = ROOT
            for (segment in segments) {
                current = current.child(Name.identifier(segment))
            }
            return current
        }
    }
}
