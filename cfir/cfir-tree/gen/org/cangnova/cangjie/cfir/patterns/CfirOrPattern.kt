

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.orPattern]
 */
abstract class CfirOrPattern : CfirPattern() {
    abstract override val source: CjSourceElement?
    /**
     * 该 pattern 节点所属的唯一模式树宿主；反向 owner 不属于 CFIR 子树。
     */
    abstract override val patternTreeOwner: CfirPatternTreeOwner?
    abstract val alternatives: List<CfirPattern>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitOrPattern(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformOrPattern(this, data) as E

    abstract override fun replacePatternTreeOwner(newPatternTreeOwner: CfirPatternTreeOwner?)

    abstract fun replaceAlternatives(newAlternatives: List<CfirPattern>)

    abstract fun <D> transformAlternatives(transformer: CfirTransformer<D>, data: D): CfirOrPattern
}
