

package org.cangnova.cangjie.generators.tree

/**
 * 生成器常用标准类型引用集合。
 */
object StandardTypes {
    val unit = type<Unit>()
    val nothing = type("kotlin", "Nothing")
    val boolean = type<Boolean>()
    val string = type<String>()
    val int = type<Int>()
    val array = type<Array<*>>()
    val list = type<List<*>>()
    val mutableList = type("kotlin.collections", "MutableList")
    val collection = type<Collection<*>>()
    val map = type<Map<*, *>>()
    val hashMap = type("kotlin.collections", "HashMap", TypeKind.Class)
}
