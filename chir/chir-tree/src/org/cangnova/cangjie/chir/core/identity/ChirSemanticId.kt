package org.cangnova.cangjie.chir.core.identity

@JvmInline
value class ChirSemanticId(val value: String) {
    init {
        require(value.isNotBlank()) { "semantic id must not be blank" }
    }

    override fun toString(): String = value
}
