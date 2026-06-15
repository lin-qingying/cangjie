

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
interface CfirAbstractFunctionCallBuilder : CfirQualifiedAccessExpressionBuilder, CfirCallBuilder {
    abstract override var source: CjSourceElement?
    abstract override val annotations: MutableList<CfirAnnotation>
    abstract override var coneTypeOrNull: ConeCangJieType?
    abstract override var dispatchReceiver: CfirExpression?
    abstract override var explicitReceiver: CfirExpression?
    abstract override val typeArguments: MutableList<CfirTypeRef>
    abstract override var argumentList: CfirArgumentList
    abstract var calleeReference: CfirReference
    abstract var origin: CfirFunctionCallOrigin
    abstract var hasTrailingLambda: Boolean
    abstract var varraySizeLiteral: String?
    override fun build(): CfirFunctionCall
}
