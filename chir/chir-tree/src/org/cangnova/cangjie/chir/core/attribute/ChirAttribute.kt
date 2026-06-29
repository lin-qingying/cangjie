package org.cangnova.cangjie.chir.core.attribute

/**
 * CHIR 节点属性公共接口。
 */
sealed interface ChirAttribute {
    /**
     * 属性键名。
     */
    val key: String
}

/**
 * 布尔属性。
 */
data class ChirBooleanAttribute(
    /**
     * 属性键名。
     */
    override val key: String,

    /**
     * 属性是否启用。
     */
    val enabled: Boolean,
) : ChirAttribute

/**
 * 字符串属性。
 */
data class ChirStringAttribute(
    /**
     * 属性键名。
     */
    override val key: String,

    /**
     * 属性字符串值。
     */
    val value: String,
) : ChirAttribute
