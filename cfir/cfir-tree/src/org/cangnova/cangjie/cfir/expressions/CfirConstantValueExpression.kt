package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 已求值常量的语义叶节点。
 *
 * annotation.argumentList 继续持有唯一语法树；argumentMapping 和 argumentView 投影此叶。
 * 对象字段、枚举载荷和 tuple 元素是值图，不能再次作为源语法进行 resolve 或诊断遍历。
 */
public class CfirConstantValueExpression(
    override val source: CjSourceElement?,
    public val constantValue: CfirConstantValue,
    annotations: List<CfirAnnotation> = emptyList(),
) : CfirExpression() {
    override var annotations: List<CfirAnnotation> = annotations
        private set

    override val coneTypeOrNull: ConeCangJieType
        get() = constantValue.type

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        require(newConeTypeOrNull == constantValue.type) { "A computed value cannot change its runtime type" }
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement =
        transformAnnotations(transformer, data)

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirExpression {
        annotations = annotations.map { it.transform<CfirAnnotation, D>(transformer, data) }
        return this
    }
}
