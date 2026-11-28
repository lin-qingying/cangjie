

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirEnumPatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirEnumPatternBuilder {
    var source: CjSourceElement? = null
    var semanticTypeRef: CfirTypeRef? = null
    val constantValues: MutableList<CfirConstantValue> = mutableListOf()
    var contextExpression: CfirExpression? = null
    lateinit var constructor: CfirExpression
    val arguments: MutableList<CfirPattern> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirEnumPattern {
        return CfirEnumPatternImpl(
            source,
            semanticTypeRef,
            constantValues,
            contextExpression,
            constructor,
            arguments,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumPattern(init: CfirEnumPatternBuilder.() -> Unit): CfirEnumPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirEnumPatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumPatternCopy(original: CfirEnumPattern, init: CfirEnumPatternBuilder.() -> Unit): CfirEnumPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirEnumPatternBuilder()
    copyBuilder.source = original.source
    copyBuilder.semanticTypeRef = original.semanticTypeRef
    copyBuilder.constantValues.addAll(original.constantValues)
    copyBuilder.contextExpression = original.contextExpression
    copyBuilder.constructor = original.constructor
    copyBuilder.arguments.addAll(original.arguments)
    return copyBuilder.apply(init).build()
}
