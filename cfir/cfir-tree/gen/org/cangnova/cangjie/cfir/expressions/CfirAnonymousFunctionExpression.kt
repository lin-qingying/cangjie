

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.anonymousFunctionExpression]
 */
abstract class CfirAnonymousFunctionExpression : CfirExpression() {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    abstract val anonymousFunction: CfirAnonymousFunction
    abstract val isTrailingLambda: Boolean

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitAnonymousFunctionExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformAnonymousFunctionExpression(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract fun replaceAnonymousFunction(newAnonymousFunction: CfirAnonymousFunction)

    abstract fun replaceIsTrailingLambda(newIsTrailingLambda: Boolean)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirAnonymousFunctionExpression

    abstract fun <D> transformAnonymousFunction(transformer: CfirTransformer<D>, data: D): CfirAnonymousFunctionExpression
}
