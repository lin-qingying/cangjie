

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirMatchBranchImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var pattern: CfirPattern,
    override var guard: CfirExpression?,
    override var body: CfirBlock,
) : CfirMatchBranch() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        pattern.accept(visitor, data)
        guard?.accept(visitor, data)
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirMatchBranchImpl {
        transformAnnotations(transformer, data)
        transformPattern(transformer, data)
        transformGuard(transformer, data)
        transformBody(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirMatchBranchImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformPattern(transformer: CfirTransformer<D>, data: D): CfirMatchBranchImpl {
        pattern = pattern.transform(transformer, data)
        return this
    }

    override fun <D> transformGuard(transformer: CfirTransformer<D>, data: D): CfirMatchBranchImpl {
        guard = guard?.transform(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirMatchBranchImpl {
        body = body.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }
}
