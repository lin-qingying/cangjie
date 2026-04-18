

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirSpawnExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirSpawnExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var body: CfirBlock
    var threadContextArgument: CfirExpression? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirSpawnExpression {
        return CfirSpawnExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            body,
            threadContextArgument,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSpawnExpression(init: CfirSpawnExpressionBuilder.() -> Unit): CfirSpawnExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirSpawnExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildSpawnExpressionCopy(original: CfirSpawnExpression, init: CfirSpawnExpressionBuilder.() -> Unit): CfirSpawnExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirSpawnExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.body = original.body
    copyBuilder.threadContextArgument = original.threadContextArgument
    return copyBuilder.apply(init).build()
}
