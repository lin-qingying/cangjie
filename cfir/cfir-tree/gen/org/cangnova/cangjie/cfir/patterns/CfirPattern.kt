

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.pattern]
 */
sealed class CfirPattern : CfirPureAbstractElement(), CfirElement, CfirPatternTreeNode {
    abstract override val source: CjSourceElement?
    abstract val semanticTypeRef: CfirTypeRef?
    abstract val constantValues: List<CfirConstantValue>
    abstract val contextExpression: CfirExpression?
    /**
     * 该 pattern 节点所属的唯一模式树宿主；反向 owner 不属于 CFIR 子树。
     */
    abstract override val patternTreeOwner: CfirPatternTreeOwner?

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        semanticTypeRef?.accept(visitor, data)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitPattern(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformPattern(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract fun replaceSemanticTypeRef(newSemanticTypeRef: CfirTypeRef?)

    abstract fun replaceConstantValues(newConstantValues: List<CfirConstantValue>)

    abstract fun replaceContextExpression(newContextExpression: CfirExpression?)

    abstract override fun replacePatternTreeOwner(newPatternTreeOwner: CfirPatternTreeOwner?)

    abstract fun <D> transformSemanticTypeRef(transformer: CfirTransformer<D>, data: D): CfirPattern
}
