

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirMatchExpressionImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var subject: CfirExpression?,
    override val branches: MutableList<CfirMatchBranch>,
    override var exhaustiveness: CfirMatchExhaustivenessStatus,
) : CfirMatchExpression() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        subject?.accept(visitor, data)
        branches.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirMatchExpressionImpl {
        transformAnnotations(transformer, data)
        transformSubject(transformer, data)
        transformBranches(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirMatchExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformSubject(transformer: CfirTransformer<D>, data: D): CfirMatchExpressionImpl {
        subject = subject?.transform(transformer, data)
        return this
    }

    override fun <D> transformBranches(transformer: CfirTransformer<D>, data: D): CfirMatchExpressionImpl {
        branches.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceExhaustiveness(newExhaustiveness: CfirMatchExhaustivenessStatus) {
        exhaustiveness = newExhaustiveness
    }
}
