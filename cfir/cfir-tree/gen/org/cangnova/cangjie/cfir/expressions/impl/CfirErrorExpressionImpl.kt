

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

class CfirErrorExpressionImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var annotations: List<CfirAnnotation>,
    override val diagnostic: ConeDiagnostic,
    override var expression: CfirExpression?,
    override var nonExpressionElement: CfirElement?,
) : CfirErrorExpression() {
    override val coneTypeOrNull: ConeCangJieType?
        get() = expression?.coneTypeOrNull ?: ConeErrorType(ConeUnreportedDuplicateDiagnostic(diagnostic))

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        expression?.accept(visitor, data)
        nonExpressionElement?.accept(visitor, data)
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)
     {
        this.annotations = newAnnotations
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)
     {
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirErrorExpression
     {
        this.annotations = annotations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirAnnotation }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirErrorExpressionImpl {
        transformAnnotations(transformer, data)
        expression?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data)
        nonExpressionElement?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data)
        return this
    }
}
