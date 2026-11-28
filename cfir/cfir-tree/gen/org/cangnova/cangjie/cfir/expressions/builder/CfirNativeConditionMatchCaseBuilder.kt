

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirNativeConditionMatchCase
import org.cangnova.cangjie.cfir.expressions.impl.CfirNativeConditionMatchCaseImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirNativeConditionMatchCaseBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var condition: CfirExpression
    lateinit var body: CfirBlock

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirNativeConditionMatchCase {
        return CfirNativeConditionMatchCaseImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            condition,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildNativeConditionMatchCase(init: CfirNativeConditionMatchCaseBuilder.() -> Unit): CfirNativeConditionMatchCase {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirNativeConditionMatchCaseBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildNativeConditionMatchCaseCopy(original: CfirNativeConditionMatchCase, init: CfirNativeConditionMatchCaseBuilder.() -> Unit): CfirNativeConditionMatchCase {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirNativeConditionMatchCaseBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.condition = original.condition
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
