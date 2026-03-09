package org.cangnova.cangjie.name

import kotlin.text.iterator

data class Name(private val name: String, val isSpecial: Boolean) : Comparable<Name> {

    fun asString(): String = name

    val identifier: String
        get() {
            check(!isSpecial) { "not identifier: $this" }
            return asString()
        }

    val identifierOrNullIfSpecial: String?
        get() = if (isSpecial) null else asString()

    fun asStringStripSpecialMarkers(): String =
        if (isSpecial) asString().substring(1, asString().length - 1) else asString()

    override operator fun compareTo(other: Name): Int = name.compareTo(other.name)

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Name) return false
        if (isSpecial != other.isSpecial) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + if (isSpecial) 1 else 0
        return result
    }

    companion object {
        @JvmField
        val ERROR_NAME = Name("<error>", true)

        @JvmStatic
        fun identifier(name: String): Name = Name(name, false)

        @JvmStatic
        fun isValidIdentifier(name: String): Boolean {
            if (name.isEmpty() || name.startsWith("<")) return false
            for (element in name) {
                if (element == '.' || element == '/' || element == '\\') {
                    return false
                }
            }
            return true
        }

        @JvmStatic
        fun identifierIfValid(name: String): Name? =
            if (!isValidIdentifier(name)) null else identifier(name)

        @JvmStatic
        fun special(name: String): Name {
            require(name.startsWith("<")) { "special name must start with '<': $name" }
            return Name(name, true)
        }

        @JvmStatic
        fun guessByFirstCharacter(name: String): Name =
            if (name.startsWith("<")) special(name) else identifier(name)
    }
}
