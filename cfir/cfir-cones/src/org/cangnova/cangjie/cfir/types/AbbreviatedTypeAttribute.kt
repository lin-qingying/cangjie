package org.cangnova.cangjie.cfir.types

import kotlin.reflect.KClass

/**
 * 保存 expanded type 对应的缩写 typealias 视图。
 *
 * 这层对齐 Kotlin FIR 的 `AbbreviatedTypeAttribute`：
 * - 共享语义层继续消费 expanded type；
 * - analysis / reference / renderer 需要保留 typealias 视图时，从 attribute 读取。
 *
 * @property coneType 缩写视图中的 typealias 类型。
 */
class AbbreviatedTypeAttribute(
    override val coneType: ConeCangJieType,
) : ConeAttributeWithConeType<AbbreviatedTypeAttribute>() {
    /**
     * typealias 缩写属性不参与并集合并。
     */
    override fun union(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute? = null

    /**
     * typealias 缩写属性不参与交集合并。
     */
    override fun intersect(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute? = null

    /**
     * 展开链上优先保留更外层的缩写视图。
     */
    override fun add(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute = other ?: this

    /**
     * 缩写属性不限制类型子类型关系。
     */
    override fun isSubtypeOf(other: AbbreviatedTypeAttribute?): Boolean = true

    /**
     * 返回调试渲染形式。
     */
    override fun toString(): String = "{${coneType.renderForDebugging()}=}"

    /**
     * 用新的缩写类型复制属性。
     */
    override fun copyWith(newType: ConeCangJieType): AbbreviatedTypeAttribute = AbbreviatedTypeAttribute(newType)

    /**
     * 当前属性的注册键。
     */
    override val key: KClass<out AbbreviatedTypeAttribute>
        get() = AbbreviatedTypeAttribute::class

    /**
     * 推断声明返回类型时保留 typealias 缩写信息。
     */
    override val keepInInferredDeclarationType: Boolean
        get() = true
}

/**
 * 从属性集合中读取 typealias 缩写属性。
 */
val ConeAttributes.abbreviatedType: AbbreviatedTypeAttribute? by ConeAttributes.attributeAccessor<AbbreviatedTypeAttribute>()

/**
 * 当前类型附带的 typealias 缩写类型。
 */
val ConeCangJieType.abbreviatedType: ConeCangJieType?
    get() = attributes.abbreviatedType?.coneType

/**
 * 当前类型的 typealias 缩写视图；没有缩写时返回自身。
 */
val ConeCangJieType.abbreviatedTypeOrSelf: ConeCangJieType
    get() = abbreviatedType ?: this

/**
 * 当前类型是否是 typealias 展开后的真实类型视图。
 */
val ConeCangJieType.isTypealiasExpansion: Boolean
    get() = this != abbreviatedTypeOrSelf
