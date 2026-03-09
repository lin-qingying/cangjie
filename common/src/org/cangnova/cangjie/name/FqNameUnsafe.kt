package org.cangnova.cangjie.name

import java.util.regex.Pattern

class FqNameUnsafe {
    private val fqName: String

    @Transient
    private var safe: FqName? = null

    @Transient
    private var parent: FqNameUnsafe? = null

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

    fun asString(): String = fqName

    val isSafe: Boolean
        get() = safe != null || asString().indexOf('<') < 0

    fun toSafe(): FqName {
        if (safe != null) return safe!!
        safe = FqName(this)
        return safe!!
    }

    val isRoot: Boolean
        get() = fqName.isEmpty()

    fun parent(): FqNameUnsafe {
        if (parent != null) return parent!!
        check(!isRoot) { "root" }
        compute()
        return parent!!
    }

    fun child(name: Name): FqNameUnsafe {
        val childFqName = if (isRoot) name.asString() else fqName + "." + name.asString()
        return FqNameUnsafe(childFqName, this, name)
    }

    fun child(fqname: FqName): FqNameUnsafe {
        var current = this
        for (name in fqname.pathSegments()) {
            current = current.child(name)
        }
        return current
    }

    fun shortName(): Name {
        if (shortName != null) return shortName!!
        check(!isRoot) { "root" }
        compute()
        return shortName!!
    }

    fun shortNameOrSpecial(): Name =
        if (isRoot) ROOT_NAME else shortName()

    fun pathSegments(): List<Name> =
        if (isRoot) emptyList() else SPLIT_BY_DOTS.split(fqName).map(STRING_TO_NAME)

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

    fun startsWith(other: FqNameUnsafe): Boolean {
        if (isRoot) return false
        val thisLength = fqName.length
        val otherLength = other.fqName.length
        if (thisLength < otherLength) return false
        return (thisLength == otherLength || fqName[otherLength] == '.') &&
            fqName.regionMatches(0, other.fqName, 0, otherLength)
    }

    override fun toString(): String = if (isRoot) ROOT_NAME.asString() else fqName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FqNameUnsafe) return false
        return fqName == other.fqName
    }

    override fun hashCode(): Int = fqName.hashCode()

    companion object {
        private val ROOT_NAME = Name.special("<root>")
        private val SPLIT_BY_DOTS: Pattern = Pattern.compile("\\.")
        private val STRING_TO_NAME: (String) -> Name = { Name.guessByFirstCharacter(it) }

        fun isValid(qualifiedName: String?): Boolean =
            qualifiedName != null && qualifiedName.indexOf('/') < 0 && qualifiedName.indexOf('*') < 0

        fun topLevel(shortName: Name): FqNameUnsafe =
            FqNameUnsafe(shortName.asString(), FqName.ROOT.toUnsafe(), shortName)
    }
}
