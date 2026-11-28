

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirCommandTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirCommandTypePatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirCommandTypePatternBuilder {
    var source: CjSourceElement? = null
    var semanticTypeRef: CfirTypeRef? = null
    val constantValues: MutableList<CfirConstantValue> = mutableListOf()
    var contextExpression: CfirExpression? = null
    lateinit var nestedPattern: CfirPattern
    val typeRefs: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirCommandTypePattern {
        return CfirCommandTypePatternImpl(
            source,
            semanticTypeRef,
            constantValues,
            contextExpression,
            nestedPattern,
            typeRefs,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCommandTypePattern(init: CfirCommandTypePatternBuilder.() -> Unit): CfirCommandTypePattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirCommandTypePatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildCommandTypePatternCopy(original: CfirCommandTypePattern, init: CfirCommandTypePatternBuilder.() -> Unit): CfirCommandTypePattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirCommandTypePatternBuilder()
    copyBuilder.source = original.source
    copyBuilder.semanticTypeRef = original.semanticTypeRef
    copyBuilder.constantValues.addAll(original.constantValues)
    copyBuilder.contextExpression = original.contextExpression
    copyBuilder.nestedPattern = original.nestedPattern
    copyBuilder.typeRefs.addAll(original.typeRefs)
    return copyBuilder.apply(init).build()
}
