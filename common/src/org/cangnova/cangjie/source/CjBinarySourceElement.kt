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
    /**
     * 调试输出中展示的二进制来源文本。
     */
    private val debugText: String,
    /**
     * 二进制文件路径；无法关联具体文件时为 null。
     */
    val binaryFilePath: String?,
    /**
     * 用于相等性和轻量节点身份的稳定对象。
     */
    private val stableIdentity: Any,
) : CjSourceElement() {
    /**
     * 二进制来源不对应真实文本范围，起始偏移固定为 0。
     */
    final override val startOffset: Int
        get() = 0

    /**
     * 二进制来源不对应真实文本范围，结束偏移固定为 0。
     */
    final override val endOffset: Int
        get() = 0

    /**
     * 二进制 source element 使用的固定元素类型。
     */
    final override val elementType: IElementType = BINARY_SOURCE_ELEMENT_TYPE

    /**
     * 二进制来源在统一 source 管线中视为真实来源。
     */
    final override val kind: CjSourceElementKind
        get() = CjRealSourceElementKind

    /**
     * 二进制来源对应的稳定轻量树根节点。
     */
    final override val lighterASTNode: LighterASTNode = BinaryLighterASTNode(
        tokenType = elementType,
        startOffset = startOffset,
        endOffset = endOffset,
        identity = stableIdentity,
    )

    /**
     * 只包含二进制根节点的轻量树结构。
     */
    final override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode> = BinaryTreeStructure(
        root = lighterASTNode,
        debugText = debugText,
    )

    /**
     * 返回二进制来源的调试文本。
     */
    final override fun getElementTextInContextForDebug(): String = debugText

    /**
     * 二进制来源按实现类型和稳定身份判断相等。
     */
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CjBinarySourceElement
        return stableIdentity == other.stableIdentity
    }

    /**
     * 返回实现类型和稳定身份组合后的哈希值。
     */
    final override fun hashCode(): Int = 31 * javaClass.hashCode() + stableIdentity.hashCode()
}

/**
 * 二进制 source element 使用的虚拟元素类型。
 */
private val BINARY_SOURCE_ELEMENT_TYPE = IElementType("CJ_BINARY_SOURCE", Language.ANY)

/**
 * 二进制来源使用的最小轻量树节点。
 */
private data class BinaryLighterASTNode(
    /**
     * 该节点暴露给轻量树 API 的元素类型。
     */
    private val tokenType: IElementType,
    /**
     * 该节点暴露给轻量树 API 的起始偏移。
     */
    private val startOffset: Int,
    /**
     * 该节点暴露给轻量树 API 的结束偏移。
     */
    private val endOffset: Int,
    /**
     * 用于调试输出的稳定身份。
     */
    private val identity: Any,
) : LighterASTNode {
    /**
     * 返回二进制虚拟节点的元素类型。
     */
    override fun getTokenType(): IElementType = tokenType

    /**
     * 返回二进制虚拟节点的起始偏移。
     */
    override fun getStartOffset(): Int = startOffset

    /**
     * 返回二进制虚拟节点的结束偏移。
     */
    override fun getEndOffset(): Int = endOffset

    /**
     * 返回包含稳定身份的调试字符串。
     */
    override fun toString(): String = "BinaryNode($identity)"
}

/**
 * 只包含一个二进制根节点的轻量树结构。
 */
private class BinaryTreeStructure(
    /**
     * 二进制来源的根节点。
     */
    private val root: LighterASTNode,
    /**
     * 根节点渲染时使用的调试文本。
     */
    private val debugText: String,
) : FlyweightCapableTreeStructure<LighterASTNode> {
    /**
     * 返回唯一根节点。
     */
    override fun getRoot(): LighterASTNode = root

    /**
     * 二进制根节点没有父节点。
     */
    override fun getParent(node: LighterASTNode): LighterASTNode? = null

    /**
     * 二进制根节点没有子节点。
     */
    override fun getChildren(node: LighterASTNode, into: Ref<Array<LighterASTNode>>): Int {
        into.set(LighterASTNode.EMPTY_ARRAY)
        return 0
    }

    /**
     * 二进制树没有需要释放的子节点数组。
     */
    override fun disposeChildren(children: Array<out LighterASTNode>?, count: Int) {}

    /**
     * 返回二进制来源调试文本。
     */
    override fun toString(node: LighterASTNode): CharSequence = debugText

    /**
     * 返回节点自身起始偏移。
     */
    override fun getStartOffset(node: LighterASTNode): Int = node.startOffset

    /**
     * 返回节点自身结束偏移。
     */
    override fun getEndOffset(node: LighterASTNode): Int = node.endOffset
}
