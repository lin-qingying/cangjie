

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirAnonymousFunctionExpressionImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirAnonymousFunctionExpressionBuilder {
    var source: CjSourceElement? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var anonymousFunction: CfirAnonymousFunction
    var isTrailingLambda: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var supportMock: Boolean = false

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirAnonymousFunctionExpression {
        return CfirAnonymousFunctionExpressionImpl(
            source,
            mappedExpression,
            overflowStrategy,
            anonymousFunction,
            isTrailingLambda,
            supportMock,
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
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.anonymousFunction = original.anonymousFunction
    copyBuilder.isTrailingLambda = original.isTrailingLambda
    copyBuilder.supportMock = original.supportMock
    return copyBuilder.apply(init).build()
}
