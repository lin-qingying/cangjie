

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirAssignmentTypeMismatchOutcome
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirAssignmentImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var lValue: CfirExpression,
    override var rValue: CfirExpression,
    override var typeMismatchOutcome: CfirAssignmentTypeMismatchOutcome?,
) : CfirAssignment() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        lValue.accept(visitor, data)
        rValue.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirAssignmentImpl {
        transformAnnotations(transformer, data)
        transformLValue(transformer, data)
        transformRValue(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirAssignmentImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformLValue(transformer: CfirTransformer<D>, data: D): CfirAssignmentImpl {
        lValue = lValue.transform(transformer, data)
        return this
    }

    override fun <D> transformRValue(transformer: CfirTransformer<D>, data: D): CfirAssignmentImpl {
        rValue = rValue.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceTypeMismatchOutcome(newTypeMismatchOutcome: CfirAssignmentTypeMismatchOutcome?) {
        typeMismatchOutcome = newTypeMismatchOutcome
    }
}
