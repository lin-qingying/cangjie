package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes

object LightTreePositioningStrategies {
    val DEFAULT: LightTreePositioningStrategy = LightTreePositioningStrategy()

    val INITIALIZER_EQ: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val eqToken = tree.findChildByType(node, CjTokens.EQ)
            return if (eqToken != null) markElement(eqToken, startOffset, endOffset, tree, node)
            else super.mark(node, startOffset, endOffset, tree)
        }
    }

    val OPERATOR: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ) = when (node.tokenType) {
            CjNodeTypes.BINARY_EXPRESSION,
            CjNodeTypes.BINARY_WITH_TYPE,
            CjNodeTypes.PREFIX_EXPRESSION,
            CjNodeTypes.POSTFIX_EXPRESSION,
            -> {
                val operationReference = tree.findChildByType(node, CjNodeTypes.OPERATION_REFERENCE) ?: node
                markElement(operationReference, startOffset, endOffset, tree, node)
            }

            else -> super.mark(node, startOffset, endOffset, tree)
        }
    }

    val REFERENCE_BY_QUALIFIED: LightTreePositioningStrategy = FindReferencePositioningStrategy(false)
    val REFERENCED_NAME_BY_QUALIFIED: LightTreePositioningStrategy = FindReferencePositioningStrategy(true)

    private class FindReferencePositioningStrategy(
        private val locateReferencedName: Boolean,
    ) : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val nodeToMark = when (node.tokenType) {
                CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
                CjNodeTypes.SAFE_ACCESS_EXPRESSION,
                -> {
                    val selector = tree.lastExpressionChild(node)
                    when (selector?.tokenType) {
                        CjNodeTypes.CALL_EXPRESSION -> tree.referenceExpression(selector, locateReferencedName) ?: selector
                        CjNodeTypes.REFERENCE_EXPRESSION -> selector
                        else -> node
                    }
                }

                CjNodeTypes.CALL_EXPRESSION -> tree.referenceExpression(node, locateReferencedName) ?: node
                CjNodeTypes.IMPORT_DIRECTIVE -> tree.findLastDescendantByType(node, CjNodeTypes.REFERENCE_EXPRESSION) ?: node
                CjNodeTypes.TYPE_REFERENCE -> tree.findDescendantByType(node, CjNodeTypes.REFERENCE_EXPRESSION) ?: node

                CjNodeTypes.BINARY_EXPRESSION,
                CjNodeTypes.BINARY_WITH_TYPE,
                CjNodeTypes.PREFIX_EXPRESSION,
                CjNodeTypes.POSTFIX_EXPRESSION,
                -> tree.findChildByType(node, CjNodeTypes.OPERATION_REFERENCE) ?: node

                else -> node
            }
            return markElement(nodeToMark, startOffset, endOffset, tree, node)
        }
    }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.referenceExpression(
    node: LighterASTNode,
    locateReferencedName: Boolean,
): LighterASTNode? {
    var result = firstExpressionChild(node)
    while (locateReferencedName && result != null && result.tokenType == CjNodeTypes.PARENTHESIZED) {
        result = firstExpressionChild(result)
    }
    return result
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.firstExpressionChild(node: LighterASTNode): LighterASTNode? {
    return getChildrenArray(node)
        .filterNotNull()
        .firstOrNull { it.isExpressionLike() || it.tokenType == CjNodeTypes.PARENTHESIZED }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.lastExpressionChild(node: LighterASTNode): LighterASTNode? {
    return getChildrenArray(node)
        .filterNotNull()
        .lastOrNull { it.isExpressionLike() || it.tokenType == CjNodeTypes.PARENTHESIZED }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.findDescendantByType(
    node: LighterASTNode,
    tokenType: IElementType,
): LighterASTNode? {
    if (node.tokenType == tokenType) return node
    for (child in getChildrenArray(node).filterNotNull()) {
        val found = findDescendantByType(child, tokenType)
        if (found != null) return found
    }
    return null
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.findLastDescendantByType(
    node: LighterASTNode,
    tokenType: IElementType,
): LighterASTNode? {
    var last: LighterASTNode? = null
    fun dfs(current: LighterASTNode) {
        if (current.tokenType == tokenType) last = current
        for (child in getChildrenArray(current).filterNotNull()) dfs(child)
    }
    dfs(node)
    return last
}

private fun LighterASTNode.isExpressionLike(): Boolean {
    return tokenType == CjNodeTypes.REFERENCE_EXPRESSION ||
            tokenType == CjNodeTypes.CALL_EXPRESSION ||
            tokenType == CjNodeTypes.DOT_QUALIFIED_EXPRESSION ||
            tokenType == CjNodeTypes.SAFE_ACCESS_EXPRESSION ||
            tokenType == CjNodeTypes.BINARY_EXPRESSION ||
            tokenType == CjNodeTypes.BINARY_WITH_TYPE ||
            tokenType == CjNodeTypes.PREFIX_EXPRESSION ||
            tokenType == CjNodeTypes.POSTFIX_EXPRESSION
}
