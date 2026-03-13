package org.cangjie.cfir.types

/**
 * 泛型参数类型，对应仓颉编译器中的 GenericsTy。
 *
 * 表示一个尚未替换的泛型参数引用，如 T in `class Foo<T>`。
 */
class ConeTypeParameterType(
    val lookupTag: ConeTypeParameterLookupTag,
    /** 上界约束列表（通过 `<:` 约束） */
    val upperBounds: List<ConeCangjieType> = emptyList(),
    /** 下界类型（内部使用） */
    val lowerBound: ConeCangjieType? = null,
    /** 是否为类型别名参数 */
    val isAliasParam: Boolean = false,
    /** 是否为占位符类型变量 */
    val isPlaceholder: Boolean = false,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    val name: String get() = lookupTag.name

    override fun equals(other: Any?): Boolean =
        other is ConeTypeParameterType && lookupTag == other.lookupTag

    override fun hashCode(): Int = lookupTag.hashCode()

    override fun toString(): String = buildString {
        append(name)
        if (upperBounds.isNotEmpty()) {
            append(" <: ")
            upperBounds.joinTo(this, " & ")
        }
    }
}
