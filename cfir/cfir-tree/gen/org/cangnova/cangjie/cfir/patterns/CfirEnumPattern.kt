

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.enumPattern]
 */
abstract class CfirEnumPattern : CfirPattern() {
    abstract override val source: CjSourceElement?
    abstract override val semanticTypeRef: CfirTypeRef?
    abstract override val constantValues: List<CfirConstantValue>
    abstract override val contextExpression: CfirExpression?
    /**
     * 该 pattern 节点所属的唯一模式树宿主；反向 owner 不属于 CFIR 子树。
     */
    abstract override val patternTreeOwner: CfirPatternTreeOwner?
    abstract val constructor: CfirExpression
    abstract val constructorReference: CfirReference?
    abstract val arguments: List<CfirPattern>

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        semanticTypeRef?.accept(visitor, data)
        constructor.accept(visitor, data)
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitEnumPattern(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformEnumPattern(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceSemanticTypeRef(newSemanticTypeRef: CfirTypeRef?)

    abstract override fun replaceConstantValues(newConstantValues: List<CfirConstantValue>)

    abstract override fun replaceContextExpression(newContextExpression: CfirExpression?)

    abstract override fun replacePatternTreeOwner(newPatternTreeOwner: CfirPatternTreeOwner?)

    abstract fun replaceConstructor(newConstructor: CfirExpression)

    abstract fun replaceArguments(newArguments: List<CfirPattern>)

    abstract override fun <D> transformSemanticTypeRef(transformer: CfirTransformer<D>, data: D): CfirEnumPattern

    abstract fun <D> transformConstructor(transformer: CfirTransformer<D>, data: D): CfirEnumPattern

    abstract fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirEnumPattern
}
