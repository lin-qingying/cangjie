
package org.cangjie.cfir.common

/**
 * 源码位置信息，将 CFIR 节点关联到原始源码。
 */
interface CfirSourceElement {
    val startOffset: Int
    val endOffset: Int
    val filePath: String?
}

/**
 * 具体的源码位置实现。
 */
data class CfirRealSourceElement(
    override val startOffset: Int,
    override val endOffset: Int,
    override val filePath: String?,
) : CfirSourceElement {
    override fun toString(): String = buildString {
        filePath?.let { append("$it:") }
        append("$startOffset..$endOffset")
    }
}

/**
 * 合成（编译器生成）节点没有源码位置。
 */
object CfirSyntheticSourceElement : CfirSourceElement {
    override val startOffset: Int get() = -1
    override val endOffset: Int get() = -1
    override val filePath: String? get() = null
    override fun toString(): String = "<synthetic>"
}
