

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
interface CfirQualifiedAccessExpressionBuilder {
    abstract var source: CjSourceElement?
    abstract val annotations: MutableList<CfirAnnotation>
    abstract var coneTypeOrNull: ConeCangJieType?
    abstract var dispatchReceiver: CfirExpression?
    abstract var explicitReceiver: CfirExpression?
    abstract val typeArguments: MutableList<CfirTypeRef>

    fun build(): CfirQualifiedAccessExpression
}
