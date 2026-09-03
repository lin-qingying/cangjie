

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAugmentedAssignment
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

internal class CfirAugmentedAssignmentImpl(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override val operation: Name,
    override val operationSource: CjSourceElement?,
    override var leftArgument: CfirExpression,
    override var rightArgument: CfirExpression,
) : CfirAugmentedAssignment() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        leftArgument.accept(visitor, data)
        rightArgument.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirAugmentedAssignmentImpl {
        transformAnnotations(transformer, data)
        transformLeftArgument(transformer, data)
        transformRightArgument(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirAugmentedAssignmentImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformLeftArgument(transformer: CfirTransformer<D>, data: D): CfirAugmentedAssignmentImpl {
        leftArgument = leftArgument.transform(transformer, data)
        return this
    }

    override fun <D> transformRightArgument(transformer: CfirTransformer<D>, data: D): CfirAugmentedAssignmentImpl {
        rightArgument = rightArgument.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }
}
