package org.cangnova.cangjie.chir.core.identity

/**
 * CHIR 节点、符号和引用使用的稳定语义标识。
 */
@JvmInline
value class ChirSemanticId(
    /**
     * 非空白语义标识文本。
     */
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "semantic id must not be blank" }
    }

    /**
     * 返回语义标识文本。
     */
    override fun toString(): String = value
}
