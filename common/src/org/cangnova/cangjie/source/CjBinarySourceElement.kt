package org.cangnova.cangjie.source

import com.intellij.lang.LighterASTNode
import com.intellij.lang.Language
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure

/**
 * 二进制来源不对应真实 PSI/LightTree，但在框架层仍需要以 [CjSourceElement] 统一承载。
 *
 * 该抽象只表达“语义来源容器”本身：
 * - 不伪造 PSI
 * - 不参与 fake source 派生
 * - 通过稳定的轻量 root 节点满足增量路径映射与统一 source 管线
 */
abstract class CjBinarySourceElement(
    private val debugText: String,
    val binaryFilePath: String?,
    private val stableIdentity: Any,
) : CjSourceElement() {
    final override val startOffset: Int
        get() = 0

    final override val endOffset: Int
        get() = 0

    final override val elementType: IElementType = BINARY_SOURCE_ELEMENT_TYPE

    final override val kind: CjSourceElementKind
        get() = CjRealSourceElementKind

    final override val lighterASTNode: LighterASTNode = BinaryLighterASTNode(
        tokenType = elementType,
        startOffset = startOffset,
        endOffset = endOffset,
        identity = stableIdentity,
    )

    final override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode> = BinaryTreeStructure(
        root = lighterASTNode,
        debugText = debugText,
    )

    final override fun getElementTextInContextForDebug(): String = debugText

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CjBinarySourceElement
        return stableIdentity == other.stableIdentity
    }

    final override fun hashCode(): Int = 31 * javaClass.hashCode() + stableIdentity.hashCode()
}

private val BINARY_SOURCE_ELEMENT_TYPE = IElementType("CJ_BINARY_SOURCE", Language.ANY)

private data class BinaryLighterASTNode(
    private val tokenType: IElementType,
    private val startOffset: Int,
    private val endOffset: Int,
    private val identity: Any,
) : LighterASTNode {
    override fun getTokenType(): IElementType = tokenType

    override fun getStartOffset(): Int = startOffset

    override fun getEndOffset(): Int = endOffset

    override fun toString(): String = "BinaryNode($identity)"
}

private class BinaryTreeStructure(
    private val root: LighterASTNode,
    private val debugText: String,
) : FlyweightCapableTreeStructure<LighterASTNode> {
    override fun getRoot(): LighterASTNode = root

    override fun getParent(node: LighterASTNode): LighterASTNode? = null

    override fun getChildren(node: LighterASTNode, into: Ref<Array<LighterASTNode>>): Int {
        into.set(LighterASTNode.EMPTY_ARRAY)
        return 0
    }

    override fun disposeChildren(children: Array<out LighterASTNode>?, count: Int) {}

    override fun toString(node: LighterASTNode): CharSequence = debugText

    override fun getStartOffset(node: LighterASTNode): Int = node.startOffset

    override fun getEndOffset(node: LighterASTNode): Int = node.endOffset
}
