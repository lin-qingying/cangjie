package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.source.CjLightSourceElement

/**
 * LightTree 诊断定位策略基类。
 */
open class LightTreePositioningStrategy {
    /**
     * 根据 LightTree 源元素计算诊断文本范围。
     */
    open fun markCjDiagnostic(element: CjLightSourceElement, diagnostic: CjDiagnostic): List<TextRange> {
        return mark(element.lighterASTNode, element.startOffset, element.endOffset, element.treeStructure)
    }

    /**
     * 根据轻量树节点和源范围计算诊断文本范围。
     */
    open fun mark(
        node: LighterASTNode,
        startOffset: Int,
        endOffset: Int,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
    ): List<TextRange> {
        return markElement(node, startOffset, endOffset, tree)
    }

    /**
     * 判断轻量树节点是否可作为有效诊断位置。
     */
    open fun isValid(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
        return !hasSyntaxErrors(node, tree)
    }
}

/**
 * 标记单个轻量树节点。
 */
fun markElement(
    node: LighterASTNode,
    startOffset: Int,
    endOffset: Int,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    originalNode: LighterASTNode = node,
): List<TextRange> = markRange(node, node, startOffset, endOffset, tree, originalNode)

/**
 * 标记两个轻量树节点之间的范围。
 */
fun markRange(
    from: LighterASTNode,
    to: LighterASTNode,
    startOffset: Int,
    endOffset: Int,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    originalNode: LighterASTNode,
): List<TextRange> = listOf(markSingleElement(from, to, startOffset, endOffset, tree, originalNode))

/**
 * 计算单个轻量树范围对应的 TextRange。
 */
fun markSingleElement(
    from: LighterASTNode,
    to: LighterASTNode,
    startOffset: Int,
    endOffset: Int,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    originalNode: LighterASTNode,
): TextRange {
    val betterFrom = from.nonFillerFirstChildOrSelf(tree)
    val betterTo = to.nonFillerLastChildOrSelf(tree)
    val startDelta = tree.getStartOffset(betterFrom) - tree.getStartOffset(originalNode)
    val endDelta = tree.getEndOffset(betterTo) - tree.getEndOffset(originalNode)
    return TextRange(startDelta + startOffset, endDelta + endOffset)
}

/**
 * 返回第一个非空白/注释子节点；没有时返回自身。
 */
private fun LighterASTNode.nonFillerFirstChildOrSelf(tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode =
    tree.getChildrenArray(this).firstOrNull { it != null && !it.isFiller() } ?: this

/**
 * 返回最后一个非空白/注释子节点；没有时返回自身。
 */
private fun LighterASTNode.nonFillerLastChildOrSelf(tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode =
    tree.getChildrenArray(this).lastOrNull { it != null && !it.isFiller() } ?: this

/**
 * 判断轻量树节点是否为空白或注释填充节点。
 */
private fun LighterASTNode.isFiller(): Boolean {
    return tokenType == TokenType.WHITE_SPACE ||
            tokenType == CjTokens.EOL_COMMENT ||
            tokenType == CjTokens.BLOCK_COMMENT ||
            tokenType == CjTokens.SHEBANG_COMMENT ||
            tokenType == CjTokens.DOC_COMMENT
}

/**
 * 判断节点自身或其最后子节点链上是否包含语法错误。
 */
private fun hasSyntaxErrors(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
    if (node.tokenType == TokenType.ERROR_ELEMENT) return true

    val children = tree.getChildrenArray(node).filterNotNull()
    return children.isNotEmpty() && hasSyntaxErrors(children.last(), tree)
}

/**
 * 读取轻量树节点的子节点数组。
 */
internal fun FlyweightCapableTreeStructure<LighterASTNode>.getChildrenArray(node: LighterASTNode): Array<LighterASTNode?> {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get() ?: emptyArray()
}

/**
 * 查找指定类型的直接子节点。
 */
internal fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(
    node: LighterASTNode,
    type: IElementType,
): LighterASTNode? {
    return getChildrenArray(node).firstOrNull { it?.tokenType == type }
}
