package org.cangnova.cangjie.name

class FqName {
    private val fqName: FqNameUnsafe

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

    fun asString(): String = fqName.asString()

    fun toUnsafe(): FqNameUnsafe = fqName

    val isRoot: Boolean
        get() = fqName.isRoot

    fun parent(): FqName {
        if (parent != null) return parent!!
        check(!isRoot) { "this is root: ${this.asString()}" }
        parent = FqName(fqName.parent())
        return parent!!
    }

    fun firstSegment(): Name? {
        if (isRoot) return null
        var current: FqName = this
        while (!current.parent().isRoot) {
            current = current.parent()
        }
        return current.shortName()
    }

    fun isSingleSegment(): Boolean {
        if (isRoot) return false
        return parent().isRoot
    }

    fun child(name: Name): FqName = FqName(fqName.child(name), this)

    fun child(name: FqName): FqName = FqName(fqName.child(name), this)

    fun shortName(): Name = fqName.shortName()

    fun shortNameOrSpecial(): Name = fqName.shortNameOrSpecial()

    fun pathSegments(): List<Name> = fqName.pathSegments()

    fun startsWith(segment: Name): Boolean = fqName.startsWith(segment)

    fun startsWith(other: FqName): Boolean = fqName.startsWith(other.fqName)

    override fun toString(): String = fqName.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FqName) return false
        return fqName == other.fqName
    }

    override fun hashCode(): Int = fqName.hashCode()

    companion object {
        @JvmField
        val ROOT: FqName = FqName("")

        fun fromSegments(names: List<String>): FqName = FqName(names.joinToString("."))

        @JvmStatic
        fun topLevel(shortName: Name): FqName = FqName(FqNameUnsafe.topLevel(shortName))

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
