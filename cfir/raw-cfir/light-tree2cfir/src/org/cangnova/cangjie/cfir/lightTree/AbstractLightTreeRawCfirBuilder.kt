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
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.ElementTypeUtils.isExpression
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceElementKind
import org.cangnova.cangjie.source.toCjLightSourceElement

abstract class AbstractLightTreeRawCfirBuilder(
    baseSession: CfirSession,
    protected val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    protected val source: CharSequence,
    context: Context<LighterASTNode> = Context(),
) : AbstractRawCfirBuilder<LighterASTNode>(baseSession, context) {
    companion object {
        protected val ignoredTokens: TokenSet = TokenSet.orSet(
            CjTokens.COMMENTS,
            TokenSet.create(CjTokens.WHITE_SPACE, CjTokens.SEMICOLON, TokenType.ERROR_ELEMENT, TokenType.BAD_CHARACTER),
        )
    }

    protected fun LighterASTNode.toCjSourceElement(
        kind: CjSourceElementKind = CjRealSourceElementKind,
    ): CjLightSourceElement {
        val startOffset = tree.getStartOffset(this)
        val endOffset = tree.getEndOffset(this)
        return toCjLightSourceElement(tree, kind, startOffset, endOffset)
    }

    protected fun LighterASTNode.toSource(): CjSourceElement =
        toSourceElement() as CjSourceElement

    override fun LighterASTNode.toSourceElement(): AbstractCjSourceElement =
        toCjSourceElement()

    override fun LighterASTNode.elementType(): IElementType = tokenType

    override fun LighterASTNode.asText(): String = getNodeText(this, source)

    protected fun LighterASTNode.getParent(): LighterASTNode? = tree.getParent(this)

    protected fun LighterASTNode?.getChildrenAsArray(): Array<out LighterASTNode?> {
        if (this == null) return arrayOf()
        val childrenRef = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, childrenRef)
        return childrenRef.get()
    }

    protected fun LighterASTNode?.getFirstChild(): LighterASTNode? =
        getChildrenAsArray().firstOrNull()

    protected inline fun LighterASTNode.forEachChildren(action: (LighterASTNode) -> Unit) {
        for (child in getChildrenAsArray()) {
            if (child == null) break
            if (ignoredTokens.contains(child.tokenType)) continue
            action(child)
        }
    }

    protected inline fun <T> LighterASTNode.forEachChildrenReturnList(
        collector: (LighterASTNode, MutableList<T>) -> Unit,
    ): MutableList<T> {
        val result = mutableListOf<T>()
        forEachChildren { child -> collector(child, result) }
        return result
    }

    protected fun LighterASTNode.getChildNodeByType(type: IElementType): LighterASTNode? {
        return getChildrenAsArray().firstOrNull { it?.tokenType == type }
    }

    protected fun LighterASTNode.getFirstChildExpression(): LighterASTNode? {
        forEachChildren {
            if (it.isExpression()) return it
        }
        return null
    }

    protected fun LighterASTNode.getFirstChildExpressionUnwrapped(): LighterASTNode? {
        val expression = getFirstChildExpression() ?: return null
        return if (expression.tokenType == CjNodeTypes.PARENTHESIZED) {
            expression.getFirstChildExpressionUnwrapped()
        } else {
            expression
        }
    }

    protected fun LighterASTNode.getLastChildExpression(): LighterASTNode? {
        var result: LighterASTNode? = null
        forEachChildren {
            if (it.isExpression()) {
                result = it
            }
        }
        return result
    }

    protected fun callableIdFor(name: Name): CallableId {
        return if (context.inLocalContext) CallableId(name) else CallableId(packageFqName, name)
    }
}
