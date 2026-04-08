

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResumeExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirResumeExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirResumeExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var withExpression: CfirExpression? = null
    var throwingExpression: CfirExpression? = null

    fun build(): CfirResumeExpression {
        return CfirResumeExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            withExpression,
            throwingExpression,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResumeExpression(init: CfirResumeExpressionBuilder.() -> Unit = {}): CfirResumeExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirResumeExpressionBuilder().apply(init).build()
}
