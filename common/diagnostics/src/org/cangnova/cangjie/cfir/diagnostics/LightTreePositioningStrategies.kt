package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes

object LightTreePositioningStrategies {
    val DEFAULT: LightTreePositioningStrategy = LightTreePositioningStrategy()
    val ACTUAL_DECLARATION_NAME: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val nameIdentifier = tree.nameIdentifier(node)
                ?: return super.mark(node, startOffset, endOffset, tree)
            return markElement(nameIdentifier, startOffset, endOffset, tree, node)
        }
    }
    val DECLARATION_START_TO_NAME: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val nameIdentifier = tree.nameIdentifier(node)
                ?: return ACTUAL_DECLARATION_NAME.mark(node, startOffset, endOffset, tree)

            val declarationStart = listOf(
                CjTokens.CLASS_KEYWORD,
                CjTokens.INTERFACE_KEYWORD,
                CjTokens.STRUCT_KEYWORD,
                CjTokens.ENUM_KEYWORD,
            ).firstNotNullOfOrNull { token ->
                tree.collectDescendantsOfType(node, token).firstOrNull()
            } ?: return ACTUAL_DECLARATION_NAME.mark(node, startOffset, endOffset, tree)

            return markRange(declarationStart, nameIdentifier, startOffset, endOffset, tree, node)
        }
    }
    val CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val startNode = sequenceOf(CjTokens.FUNC_KEYWORD, CjTokens.MAIN_KEYWORD, CjTokens.INIT_KEYWORD)
                .mapNotNull { token -> tree.findChildByType(node, token) }
                .firstOrNull()
                ?: return ACTUAL_DECLARATION_NAME.mark(node, startOffset, endOffset, tree)

            val endNode = tree.findChildByType(node, CjNodeTypes.VALUE_PARAMETER_LIST)
                ?: tree.findChildByType(node, CjTokens.IDENTIFIER)
                ?: return ACTUAL_DECLARATION_NAME.mark(node, startOffset, endOffset, tree)

            return markRange(startNode, endNode, startOffset, endOffset, tree, node)
        }
    }
    val IMPORT_LAST_NAME: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>
        ): List<TextRange> {
            val nodeToMark = when {
                node.tokenType != CjNodeTypes.IMPORT_ITEM -> node
                else -> tree.collectDescendantsOfType(node, CjNodeTypes.REFERENCE_EXPRESSION).lastOrNull() ?: node
            }
            return markElement(nodeToMark, startOffset, endOffset, tree, node)
        }
    }
    val IMPORT_ALIAS: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val aliasNode = tree.collectDescendantsOfType(node, CjNodeTypes.IMPORT_ALIAS).lastOrNull()
                ?: return IMPORT_LAST_NAME.mark(node, startOffset, endOffset, tree)
            val aliasIdentifier = tree.collectDescendantsOfType(aliasNode, CjTokens.IDENTIFIER).lastOrNull()
                ?: aliasNode
            return markElement(aliasIdentifier, startOffset, endOffset, tree, node)
        }
    }
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
    val VISIBILITY_MODIFIER: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val visibilityModifier = listOf(
                CjTokens.PUBLIC_KEYWORD,
                CjTokens.INTERNAL_KEYWORD,
                CjTokens.PROTECTED_KEYWORD,
                CjTokens.PRIVATE_KEYWORD,
            ).firstNotNullOfOrNull { token ->
                tree.collectDescendantsOfType(node, token).firstOrNull()
            } ?: return ACTUAL_DECLARATION_NAME.mark(node, startOffset, endOffset, tree)

            return markElement(visibilityModifier, startOffset, endOffset, tree, node)
        }
    }
    val OVERRIDE_MODIFIER: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val overrideModifier = tree.collectDescendantsOfType(node, CjTokens.OVERRIDE_KEYWORD).firstOrNull()
                ?: tree.collectDescendantsOfType(node, CjTokens.REDEF_KEYWORD).firstOrNull()
                ?: return ACTUAL_DECLARATION_NAME.mark(node, startOffset, endOffset, tree)
            return markElement(overrideModifier, startOffset, endOffset, tree, node)
        }
    }
    val MUT_MODIFIER: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val mutModifier = tree.collectDescendantsOfType(node, CjTokens.MUT_KEYWORD).firstOrNull()
                ?: return ACTUAL_DECLARATION_NAME.mark(node, startOffset, endOffset, tree)
            return markElement(mutModifier, startOffset, endOffset, tree, node)
        }
    }
    val THROW_KEYWORD: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val throwKeyword = tree.findChildByType(node, CjTokens.THROW_KEYWORD)
                ?: return super.mark(node, startOffset, endOffset, tree)
            return markElement(throwKeyword, startOffset, endOffset, tree, node)
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

    val NAME_OF_NAMED_ARGUMENT: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val valueArgumentName = when (node.tokenType) {
                CjNodeTypes.VALUE_ARGUMENT -> tree.findChildByType(node, CjNodeTypes.VALUE_ARGUMENT_NAME)
                else -> null
            } ?: return super.mark(node, startOffset, endOffset, tree)

            val referencedName = tree.findDescendantByType(valueArgumentName, CjNodeTypes.REFERENCE_EXPRESSION)
                ?: valueArgumentName
            return markElement(referencedName, startOffset, endOffset, tree, node)
        }
    }

    val VALUE_ARGUMENTS: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val argumentNodes = when (node.tokenType) {
                CjNodeTypes.CALL_EXPRESSION -> tree.findChildByType(node, CjNodeTypes.VALUE_ARGUMENT_LIST)
                    ?.let { listNode -> tree.childrenOfType(listNode, CjNodeTypes.VALUE_ARGUMENT) }
                    .orEmpty()

                CjNodeTypes.VALUE_ARGUMENT_LIST -> tree.childrenOfType(node, CjNodeTypes.VALUE_ARGUMENT)
                else -> emptyList()
            }
            if (argumentNodes.isEmpty()) return super.mark(node, startOffset, endOffset, tree)
            return markRange(argumentNodes.first(), argumentNodes.last(), startOffset, endOffset, tree, node)
        }
    }

    val VALUE_ARGUMENTS_LIST: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val valueArgumentList = when (node.tokenType) {
                CjNodeTypes.CALL_EXPRESSION -> tree.findChildByType(node, CjNodeTypes.VALUE_ARGUMENT_LIST)
                CjNodeTypes.VALUE_ARGUMENT_LIST -> node
                else -> null
            } ?: return VALUE_ARGUMENTS.mark(node, startOffset, endOffset, tree)

            return markElement(valueArgumentList, startOffset, endOffset, tree, node)
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

private fun FlyweightCapableTreeStructure<LighterASTNode>.childrenOfType(
    node: LighterASTNode,
    tokenType: IElementType,
): List<LighterASTNode> {
    return getChildrenArray(node)
        .filterNotNull()
        .filter { it.tokenType == tokenType }
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

private fun FlyweightCapableTreeStructure<LighterASTNode>.nameIdentifier(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, CjTokens.IDENTIFIER)
        ?: findChildByType(node, CjTokens.INIT_KEYWORD)

fun FlyweightCapableTreeStructure<LighterASTNode>.collectDescendantsOfType(
    node: LighterASTNode, type: IElementType,
    predicate: (LighterASTNode) -> Boolean = { true }
): List<LighterASTNode> {
    val result = mutableListOf<LighterASTNode>()

    fun FlyweightCapableTreeStructure<LighterASTNode>.collectDescendantByType(node: LighterASTNode) {
        val childrenRef = Ref<Array<LighterASTNode?>>()
        getChildren(node, childrenRef)

        val childrenRefGet = childrenRef.get()
        if (childrenRefGet != null) {
            for (child in childrenRefGet) {
                if (child?.tokenType == type && predicate(child)) {
                    result.add(child)
                }

                if (child != null) {
                    collectDescendantByType(child)
                }
            }
        }
    }

    collectDescendantByType(node)

    return result
}
