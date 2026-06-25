package org.cangnova.cangjie.cfir.types

/**
 * 稳定占位类型。
 *
 * 调用解析流水线在真实类型尚不可用时，用该类型保留一个显式类型实参槽位。
 *
 * @property debugName 调试输出中展示的占位名。
 * @property attributes 占位类型携带的属性。
 */
class ConePlaceholderType(
    /** 调试输出中展示的占位名。 */
    val debugName: String = "_",
    /** 占位类型携带的属性。 */
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeSimpleCangJieType(), ConeTypeConstructorMarker {
    /**
     * 占位类型不携带类型实参。
     */
    override val typeArguments: List<ConeTypeProjection>
        get() = emptyList()

    /**
     * 占位类型使用引用相等，避免不同占位槽被结构合并。
     */
    override fun equals(other: Any?): Boolean = this === other

    /**
     * 与引用相等匹配的 identity hash。
     */
    override fun hashCode(): Int = System.identityHashCode(this)

}
