package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes

/**
 * LightTree 前端使用的诊断定位策略集合。
 */
object LightTreePositioningStrategies {
    /**
     * 默认定位策略，直接标记源元素范围。
     */
    val DEFAULT: LightTreePositioningStrategy = LightTreePositioningStrategy()
    /**
     * 标记声明的实际名称标识符。
     */
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
    /**
     * 标记声明起始关键字到名称标识符的范围。
     */
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
    /**
     * 标记可调用声明签名中不含修饰符的主体范围。
     */
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
    /**
     * 标记 import 路径最后一个被引用名称。
     */
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
    /**
     * 标记 import alias 的别名标识符。
     */
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
    /**
     * 标记初始化器中的等号 token。
     */
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
    /**
     * 标记声明上的可见性修饰符。
     */
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
    /**
     * 标记 override 或 redef 修饰符。
     */
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
    /**
     * 标记 mut 修饰符。
     */
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
    /**
     * 标记 throw 表达式中的 throw 关键字。
     */
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
    /**
     * 标记数组字面量左中括号。
     */
    val ARRAY_LITERAL_LEFT_BRACKET: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val leftBracket = tree.findChildByType(node, CjTokens.LBRACKET)
                ?: return super.mark(node, startOffset, endOffset, tree)
            return markElement(leftBracket, startOffset, endOffset, tree, node)
        }
    }

    /**
     * 标记表达式中的操作符引用。
     */
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

    /**
     * 标记具名实参的参数名。
     */
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

    /**
     * 标记调用表达式中的实参范围。
     */
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

                CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
                CjNodeTypes.SAFE_ACCESS_EXPRESSION,
                -> tree.lastExpressionChild(node)
                    ?.takeIf { it.tokenType == CjNodeTypes.CALL_EXPRESSION }
                    ?.let { selectorCall -> tree.findChildByType(selectorCall, CjNodeTypes.VALUE_ARGUMENT_LIST) }
                    ?.let { listNode -> tree.childrenOfType(listNode, CjNodeTypes.VALUE_ARGUMENT) }
                    .orEmpty()

                CjNodeTypes.VALUE_ARGUMENT_LIST -> tree.childrenOfType(node, CjNodeTypes.VALUE_ARGUMENT)
                else -> emptyList()
            }
            if (argumentNodes.isEmpty()) return super.mark(node, startOffset, endOffset, tree)
            return markRange(argumentNodes.first(), argumentNodes.last(), startOffset, endOffset, tree, node)
        }
    }

    /**
     * 标记调用表达式的实参列表节点。
     */
    val VALUE_ARGUMENTS_LIST: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            val valueArgumentList = when (node.tokenType) {
                CjNodeTypes.CALL_EXPRESSION -> tree.findChildByType(node, CjNodeTypes.VALUE_ARGUMENT_LIST)
                CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
                CjNodeTypes.SAFE_ACCESS_EXPRESSION,
                -> tree.lastExpressionChild(node)
                    ?.takeIf { it.tokenType == CjNodeTypes.CALL_EXPRESSION }
                    ?.let { selectorCall -> tree.findChildByType(selectorCall, CjNodeTypes.VALUE_ARGUMENT_LIST) }
                CjNodeTypes.VALUE_ARGUMENT_LIST -> node
                else -> null
            } ?: return VALUE_ARGUMENTS.mark(node, startOffset, endOffset, tree)

            return markElement(valueArgumentList, startOffset, endOffset, tree, node)
        }
    }

    /**
     * 标记限定表达式中的引用表达式。
     */
    val REFERENCE_BY_QUALIFIED: LightTreePositioningStrategy = FindReferencePositioningStrategy(false)
    /**
     * 标记限定表达式中最终被引用的名称。
     */
    val REFERENCED_NAME_BY_QUALIFIED: LightTreePositioningStrategy = FindReferencePositioningStrategy(true)

    /**
     * 在不同轻量树表达式形状中寻找应标记引用节点的策略。
     */
    private class FindReferencePositioningStrategy(
        /**
         * 是否跳过括号并定位到最终被引用名称。
         */
        private val locateReferencedName: Boolean,
    ) : LightTreePositioningStrategy() {
        /**
         * 根据轻量树节点类型选择引用节点并返回其标记范围。
         */
        override fun mark(
            node: LighterASTNode,
            startOffset: Int,
            endOffset: Int,
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
        ): List<TextRange> {
            var nodeToMark = when (node.tokenType) {
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
            if (locateReferencedName && nodeToMark.tokenType == CjNodeTypes.REFERENCE_EXPRESSION) {
                nodeToMark = tree.referencedNameIdentifier(nodeToMark) ?: nodeToMark
            }
            return markElement(nodeToMark, startOffset, endOffset, tree, node)
        }
    }
}

/**
 * 从调用、限定或括号表达式中取得引用表达式节点。
 */
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

/**
 * 返回第一个表达式形态的直接子节点。
 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.firstExpressionChild(node: LighterASTNode): LighterASTNode? {
    return getChildrenArray(node)
        .filterNotNull()
        .firstOrNull { it.isExpressionLike() || it.tokenType == CjNodeTypes.PARENTHESIZED }
}

/**
 * 返回指定 token 类型的直接子节点列表。
 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.childrenOfType(
    node: LighterASTNode,
    tokenType: IElementType,
): List<LighterASTNode> {
    return getChildrenArray(node)
        .filterNotNull()
        .filter { it.tokenType == tokenType }
}

/**
 * 返回最后一个表达式形态的直接子节点。
 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.lastExpressionChild(node: LighterASTNode): LighterASTNode? {
    return getChildrenArray(node)
        .filterNotNull()
        .lastOrNull { it.isExpressionLike() || it.tokenType == CjNodeTypes.PARENTHESIZED }
}

/**
 * 深度优先查找第一个指定 token 类型的后代节点。
 */
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

/**
 * 深度优先查找最后一个指定 token 类型的后代节点。
 */
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

/**
 * 判断轻量树节点是否可作为表达式形态参与定位。
 */
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

/**
 * 查找声明或操作符名称对应的标识节点。
 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.nameIdentifier(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, CjTokens.IDENTIFIER)
        ?: findChildByType(node, CjNodeTypes.OPERATION_NAME)
        ?: findChildByType(node, CjTokens.INIT_KEYWORD)

/**
 * 返回名称引用节点中的完整名称 token，不包含同一引用节点携带的泛型实参。
 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.referencedNameIdentifier(
    node: LighterASTNode,
): LighterASTNode? =
    findChildByType(node, CjTokens.IDENTIFIER)
        ?: findChildByType(node, CjTokens.THIS_KEYWORD)
        ?: findChildByType(node, CjTokens.SUPER_KEYWORD)
        ?: findChildByType(node, CjTokens.VARRAY_KEYWORD)

/**
 * 收集指定类型的所有后代节点。
 */
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
