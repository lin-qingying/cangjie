package org.cangnova.cangjie.cfir.types

/**
 * 类型属性，用于携带类型的附加元信息。
 */
class ConeAttributes private constructor(
    private val attributes: Map<Key<*>, Any>,
) {
    fun <T : Any> get(key: Key<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return attributes[key] as? T
    }

    fun <T : Any> with(key: Key<T>, value: T): ConeAttributes {
        return ConeAttributes(attributes + (key to value))
    }

    fun <T : Any> without(key: Key<T>): ConeAttributes {
        return ConeAttributes(attributes - key)
    }

    val isEmpty: Boolean get() = attributes.isEmpty()

    override fun equals(other: Any?): Boolean =
        other is ConeAttributes && attributes == other.attributes

    override fun hashCode(): Int = attributes.hashCode()

    override fun toString(): String =
        if (isEmpty) "[]" else attributes.entries.joinToString(prefix = "[", postfix = "]") { "${it.key}=${it.value}" }

    abstract class Key<T : Any>(val name: String) {
        override fun toString(): String = name
    }

    companion object {
        val EMPTY = ConeAttributes(emptyMap())
    }
}
