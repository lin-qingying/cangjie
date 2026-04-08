

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.constPattern]
 */
abstract class CfirConstPattern : CfirPattern() {
    abstract override val source: CjSourceElement?
    abstract val expression: CfirExpression

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitConstPattern(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformConstPattern(this, data) as E

    abstract fun <D> transformExpression(transformer: CfirTransformer<D>, data: D): CfirConstPattern
}
