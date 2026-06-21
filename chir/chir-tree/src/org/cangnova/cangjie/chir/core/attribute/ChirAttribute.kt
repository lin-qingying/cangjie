package org.cangnova.cangjie.chir.core.attribute

sealed interface ChirAttribute {
    val key: String
}

data class ChirBooleanAttribute(
    override val key: String,
    val enabled: Boolean,
) : ChirAttribute

data class ChirStringAttribute(
    override val key: String,
    val value: String,
) : ChirAttribute
