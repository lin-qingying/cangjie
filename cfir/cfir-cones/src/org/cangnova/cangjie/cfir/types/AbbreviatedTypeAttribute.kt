package org.cangnova.cangjie.cfir.types

import kotlin.reflect.KClass

/**
 * 保存 expanded type 对应的缩写 typealias 视图。
 *
 * 这层对齐 Kotlin FIR 的 `AbbreviatedTypeAttribute`：
 * - 共享语义层继续消费 expanded type；
 * - analysis / reference / renderer 需要保留 typealias 视图时，从 attribute 读取。
 */
class AbbreviatedTypeAttribute(
    override val coneType: ConeCangJieType,
) : ConeAttributeWithConeType<AbbreviatedTypeAttribute>() {
    override fun union(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute? = null

    override fun intersect(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute? = null

    override fun add(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute = other ?: this

    override fun isSubtypeOf(other: AbbreviatedTypeAttribute?): Boolean = true

    override fun toString(): String = "{${coneType.renderForDebugging()}=}"

    override fun copyWith(newType: ConeCangJieType): AbbreviatedTypeAttribute = AbbreviatedTypeAttribute(newType)

    override val key: KClass<out AbbreviatedTypeAttribute>
        get() = AbbreviatedTypeAttribute::class

    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.abbreviatedType: AbbreviatedTypeAttribute? by ConeAttributes.attributeAccessor<AbbreviatedTypeAttribute>()

val ConeCangJieType.abbreviatedType: ConeCangJieType?
    get() = attributes.abbreviatedType?.coneType

val ConeCangJieType.abbreviatedTypeOrSelf: ConeCangJieType
    get() = abbreviatedType ?: this

val ConeCangJieType.isTypealiasExpansion: Boolean
    get() = this != abbreviatedTypeOrSelf
