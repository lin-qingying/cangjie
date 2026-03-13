package org.cangnova.cangjie.cfir.declarations

/**
 * 声明属性容器，用于携带声明的附加元信息。
 * 类似 ConeAttributes，但作用于声明级别。
 */
class CfirDeclarationAttributes private constructor(
    private val attributes: Map<Key<*>, Any>,
) {
    fun <T : Any> get(key: Key<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return attributes[key] as? T
    }

    fun <T : Any> with(key: Key<T>, value: T): CfirDeclarationAttributes =
        CfirDeclarationAttributes(attributes + (key to value))

    val isEmpty: Boolean get() = attributes.isEmpty()

    abstract class Key<T : Any>(val name: String) {
        override fun toString(): String = name
    }

    companion object {
        val EMPTY = CfirDeclarationAttributes(emptyMap())
    }
}
