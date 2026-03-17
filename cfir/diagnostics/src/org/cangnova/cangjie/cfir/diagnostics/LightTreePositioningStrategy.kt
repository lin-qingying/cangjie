package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.cfir.source.CjLightSourceElement

open class LightTreePositioningStrategy {
    open fun markCjDiagnostic(element: CjLightSourceElement, diagnostic: CjDiagnostic): List<TextRange> {
        return mark(element.lighterASTNode, element.startOffset, element.endOffset, element.treeStructure)
    }

    open fun mark(
        node: LighterASTNode,
        startOffset: Int,
        endOffset: Int,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
    ): List<TextRange> {
        return markElement(node, startOffset, endOffset, tree)
    }

    open fun isValid(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
        return !hasSyntaxErrors(node, tree)
    }
}

fun markElement(
    node: LighterASTNode,
    startOffset: Int,
    endOffset: Int,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    originalNode: LighterASTNode = node,
): List<TextRange> = markRange(node, node, startOffset, endOffset, tree, originalNode)

fun markRange(
    from: LighterASTNode,
    to: LighterASTNode,
    startOffset: Int,
    endOffset: Int,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    originalNode: LighterASTNode,
): List<TextRange> = listOf(markSingleElement(from, to, startOffset, endOffset, tree, originalNode))

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

private fun LighterASTNode.nonFillerFirstChildOrSelf(tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode =
    tree.getChildrenArray(this).firstOrNull { it != null && !it.isFiller() } ?: this

private fun LighterASTNode.nonFillerLastChildOrSelf(tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode =
    tree.getChildrenArray(this).lastOrNull { it != null && !it.isFiller() } ?: this

private fun LighterASTNode.isFiller(): Boolean {
    return tokenType == TokenType.WHITE_SPACE ||
            tokenType == CjTokens.EOL_COMMENT ||
            tokenType == CjTokens.BLOCK_COMMENT ||
            tokenType == CjTokens.SHEBANG_COMMENT ||
            tokenType == CjTokens.DOC_COMMENT
}

private fun hasSyntaxErrors(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
    if (node.tokenType == TokenType.ERROR_ELEMENT) return true

    val children = tree.getChildrenArray(node).filterNotNull()
    return children.isNotEmpty() && hasSyntaxErrors(children.last(), tree)
}

internal fun FlyweightCapableTreeStructure<LighterASTNode>.getChildrenArray(node: LighterASTNode): Array<LighterASTNode?> {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get() ?: emptyArray()
}

internal fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(
    node: LighterASTNode,
    type: IElementType,
): LighterASTNode? {
    return getChildrenArray(node).firstOrNull { it?.tokenType == type }
}
