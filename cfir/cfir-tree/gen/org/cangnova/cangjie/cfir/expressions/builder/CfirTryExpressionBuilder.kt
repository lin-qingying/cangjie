

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirTryExpressionImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirTryExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    val resources: MutableList<CfirFieldVariable> = mutableListOf()
    lateinit var tryBlock: CfirBlock
    val handlers: MutableList<CfirHandleClause> = mutableListOf()
    val catches: MutableList<CfirCatch> = mutableListOf()
    var finallyBlock: CfirBlock? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTryExpression {
        return CfirTryExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            resources,
            tryBlock,
            handlers,
            catches,
            finallyBlock,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTryExpression(init: CfirTryExpressionBuilder.() -> Unit): CfirTryExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTryExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTryExpressionCopy(original: CfirTryExpression, init: CfirTryExpressionBuilder.() -> Unit): CfirTryExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTryExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.resources.addAll(original.resources)
    copyBuilder.tryBlock = original.tryBlock
    copyBuilder.handlers.addAll(original.handlers)
    copyBuilder.catches.addAll(original.catches)
    copyBuilder.finallyBlock = original.finallyBlock
    return copyBuilder.apply(init).build()
}
