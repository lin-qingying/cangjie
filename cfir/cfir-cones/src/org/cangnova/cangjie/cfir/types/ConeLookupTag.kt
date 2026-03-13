package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId

/**
 * 类型查找标签，用于延迟解析和缓存类型查找结果。
 */
abstract class ConeLookupTag {
    abstract val name: String
    override fun toString(): String = name
}

/**
 * 类类型查找标签（类/接口/结构体/枚举）。
 */
abstract class ConeClassLikeLookupTag : ConeLookupTag() {
    abstract val classId: ClassId
    override val name: String get() = classId.shortClassName.asString()
}

/**
 * 简单的类查找标签实现。
 */
class ConeClassLookupTagImpl(override val classId: ClassId) : ConeClassLikeLookupTag() {
    override fun equals(other: Any?): Boolean = other is ConeClassLookupTagImpl && classId == other.classId
    override fun hashCode(): Int = classId.hashCode()
}

/**
 * 泛型参数查找标签。
 */
class ConeTypeParameterLookupTag(
    override val name: String,
) : ConeLookupTag() {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}
