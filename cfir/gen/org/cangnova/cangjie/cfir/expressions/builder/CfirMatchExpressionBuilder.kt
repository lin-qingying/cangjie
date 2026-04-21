

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExhaustivenessStatus
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirMatchExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirMatchExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var subject: CfirExpression? = null
    val branches: MutableList<CfirMatchBranch> = mutableListOf()
    var exhaustiveness: CfirMatchExhaustivenessStatus = CfirMatchExhaustivenessStatus.Unknown

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirMatchExpression {
        return CfirMatchExpressionImpl(
            source,
            annotations,
            coneTypeOrNull,
            subject,
            branches,
            exhaustiveness,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildMatchExpression(init: CfirMatchExpressionBuilder.() -> Unit = {}): CfirMatchExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirMatchExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildMatchExpressionCopy(original: CfirMatchExpression, init: CfirMatchExpressionBuilder.() -> Unit = {}): CfirMatchExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirMatchExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.subject = original.subject
    copyBuilder.branches.addAll(original.branches)
    copyBuilder.exhaustiveness = original.exhaustiveness
    return copyBuilder.apply(init).build()
}
