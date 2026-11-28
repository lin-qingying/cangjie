

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirConstPatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirConstPatternBuilder {
    var source: CjSourceElement? = null
    var semanticTypeRef: CfirTypeRef? = null
    val constantValues: MutableList<CfirConstantValue> = mutableListOf()
    var contextExpression: CfirExpression? = null
    lateinit var expression: CfirExpression
    var operatorCall: CfirExpression? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirConstPattern {
        return CfirConstPatternImpl(
            source,
            semanticTypeRef,
            constantValues,
            contextExpression,
            expression,
            operatorCall,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildConstPattern(init: CfirConstPatternBuilder.() -> Unit): CfirConstPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirConstPatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildConstPatternCopy(original: CfirConstPattern, init: CfirConstPatternBuilder.() -> Unit): CfirConstPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirConstPatternBuilder()
    copyBuilder.source = original.source
    copyBuilder.semanticTypeRef = original.semanticTypeRef
    copyBuilder.constantValues.addAll(original.constantValues)
    copyBuilder.contextExpression = original.contextExpression
    copyBuilder.expression = original.expression
    copyBuilder.operatorCall = original.operatorCall
    return copyBuilder.apply(init).build()
}
