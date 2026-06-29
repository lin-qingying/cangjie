

package org.cangnova.cangjie.generators.tree

/**
 * 生成器常用标准类型引用集合。
 */
object StandardTypes {
    /**
     * Kotlin `Unit` 类型引用。
     */
    val unit = type<Unit>()
    /**
     * Kotlin `Nothing` 类型引用。
     */
    val nothing = type("kotlin", "Nothing")
    /**
     * Kotlin `Boolean` 类型引用。
     */
    val boolean = type<Boolean>()
    /**
     * Kotlin `String` 类型引用。
     */
    val string = type<String>()
    /**
     * Kotlin `Int` 类型引用。
     */
    val int = type<Int>()
    /**
     * Kotlin `Array` 类型引用。
     */
    val array = type<Array<*>>()
    /**
     * Kotlin `List` 类型引用。
     */
    val list = type<List<*>>()
    /**
     * Kotlin `MutableList` 类型引用。
     */
    val mutableList = type("kotlin.collections", "MutableList")
    /**
     * Kotlin `Collection` 类型引用。
     */
    val collection = type<Collection<*>>()
    /**
     * Kotlin `Map` 类型引用。
     */
    val map = type<Map<*, *>>()
    /**
     * Kotlin `HashMap` 类引用。
     */
    val hashMap = type("kotlin.collections", "HashMap", TypeKind.Class)
}
