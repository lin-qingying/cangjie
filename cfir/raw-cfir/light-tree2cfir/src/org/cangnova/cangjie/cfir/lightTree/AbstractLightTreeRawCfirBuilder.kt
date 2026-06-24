package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.builder.AbstractRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.Context
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.ElementTypeUtils.isExpression
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceElementKind
import org.cangnova.cangjie.source.toCjLightSourceElement

/**
 * LightTree raw CFIR builder 的共享基类。
 *
 * 该基类为声明 builder 与表达式 builder 提供 LightTree source 映射、节点访问、
 * 子节点遍历和表达式子节点查找能力；语义转换逻辑仍由具体子类实现。
 *
 * @property tree 当前 LightTree 树结构。
 * @property source 当前源码文本。
 */
abstract class AbstractLightTreeRawCfirBuilder(
    baseSession: CfirSession,
    /** 当前 LightTree 树结构。 */
    protected val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    /** 当前源码文本。 */
    protected val source: CharSequence,
    context: Context<LighterASTNode> = Context(),
) : AbstractRawCfirBuilder<LighterASTNode>(baseSession, context) {
    /** LightTree builder 共享常量。 */
    companion object {
        /** 遍历子节点时忽略的空白、注释和错误 token。 */
        protected val ignoredTokens: TokenSet = TokenSet.orSet(
            CjTokens.COMMENTS,
            TokenSet.create(CjTokens.WHITE_SPACE, CjTokens.SEMICOLON, TokenType.ERROR_ELEMENT, TokenType.BAD_CHARACTER),
        )
    }

    /** 将当前 LightTree 节点映射为 [CjLightSourceElement]。 */
    protected fun LighterASTNode.toCjSourceElement(
        kind: CjSourceElementKind = CjRealSourceElementKind,
    ): CjLightSourceElement {
        val startOffset = tree.getStartOffset(this)
        val endOffset = tree.getEndOffset(this)
        return toCjLightSourceElement(tree, kind, startOffset, endOffset)
    }

    /** 将当前 LightTree 节点映射为 CFIR 可用的 [CjSourceElement]。 */
    protected fun LighterASTNode.toSource(): CjSourceElement =
        toSourceElement() as CjSourceElement

    /** 实现 raw builder source element 抽象。 */
    override fun LighterASTNode.toSourceElement(): AbstractCjSourceElement =
        toCjSourceElement()

    /** 返回 LightTree 节点 token type。 */
    override fun LighterASTNode.elementType(): IElementType = tokenType

    /** 返回 LightTree 节点对应的源码文本。 */
    override fun LighterASTNode.asText(): String = getNodeText(this, tree, source)

    /** 返回当前节点的父节点。 */
    protected fun LighterASTNode.getParent(): LighterASTNode? = tree.getParent(this)

    /** 返回当前节点的直接子节点数组；receiver 为 null 时返回空数组。 */
    protected fun LighterASTNode?.getChildrenAsArray(): Array<out LighterASTNode?> {
        if (this == null) return arrayOf()
        val childrenRef = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, childrenRef)
        return childrenRef.get()
    }

    /** 返回当前节点的第一个直接子节点。 */
    protected fun LighterASTNode?.getFirstChild(): LighterASTNode? =
        getChildrenAsArray().firstOrNull()

    /** 遍历非空且非 ignored token 的直接子节点。 */
    protected inline fun LighterASTNode.forEachChildren(action: (LighterASTNode) -> Unit) {
        for (child in getChildrenAsArray()) {
            if (child == null) break
            if (ignoredTokens.contains(child.tokenType)) continue
            action(child)
        }
    }

    /** 遍历直接子节点并把 [collector] 填充的结果列表返回。 */
    protected inline fun <T> LighterASTNode.forEachChildrenReturnList(
        collector: (LighterASTNode, MutableList<T>) -> Unit,
    ): MutableList<T> {
        val result = mutableListOf<T>()
        forEachChildren { child -> collector(child, result) }
        return result
    }

    /** 查找第一个直接子节点 token type 等于 [type] 的节点。 */
    protected fun LighterASTNode.getChildNodeByType(type: IElementType): LighterASTNode? {
        return getChildrenAsArray().firstOrNull { it?.tokenType == type }
    }

    /** 查找第一个直接表达式子节点。 */
    protected fun LighterASTNode.getFirstChildExpression(): LighterASTNode? {
        forEachChildren {
            if (it.isExpression()) return it
        }
        return null
    }

    /** 查找第一个表达式子节点，并递归剥离括号表达式。 */
    protected fun LighterASTNode.getFirstChildExpressionUnwrapped(): LighterASTNode? {
        val expression = getFirstChildExpression() ?: return null
        return if (expression.tokenType == CjNodeTypes.PARENTHESIZED) {
            expression.getFirstChildExpressionUnwrapped()
        } else {
            expression
        }
    }

    /** 查找最后一个直接表达式子节点。 */
    protected fun LighterASTNode.getLastChildExpression(): LighterASTNode? {
        var result: LighterASTNode? = null
        forEachChildren {
            if (it.isExpression()) {
                result = it
            }
        }
        return result
    }
}
