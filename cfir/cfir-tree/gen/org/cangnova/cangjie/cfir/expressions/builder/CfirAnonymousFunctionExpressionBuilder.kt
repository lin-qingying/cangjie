

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirAnonymousFunctionExpressionImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirAnonymousFunctionExpressionBuilder {
    var source: CjSourceElement? = null
    lateinit var anonymousFunction: CfirAnonymousFunction
    var isTrailingLambda: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirAnonymousFunctionExpression {
        return CfirAnonymousFunctionExpressionImpl(
            source,
            anonymousFunction,
            isTrailingLambda,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousFunctionExpression(init: CfirAnonymousFunctionExpressionBuilder.() -> Unit): CfirAnonymousFunctionExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirAnonymousFunctionExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousFunctionExpressionCopy(original: CfirAnonymousFunctionExpression, init: CfirAnonymousFunctionExpressionBuilder.() -> Unit): CfirAnonymousFunctionExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirAnonymousFunctionExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.anonymousFunction = original.anonymousFunction
    copyBuilder.isTrailingLambda = original.isTrailingLambda
    return copyBuilder.apply(init).build()
}
