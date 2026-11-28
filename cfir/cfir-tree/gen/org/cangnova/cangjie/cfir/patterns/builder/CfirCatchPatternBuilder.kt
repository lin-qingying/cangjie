

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirCatchPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirCatchPatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirCatchPatternBuilder {
    var source: CjSourceElement? = null
    var semanticTypeRef: CfirTypeRef? = null
    val constantValues: MutableList<CfirConstantValue> = mutableListOf()
    var contextExpression: CfirExpression? = null
    lateinit var nestedPattern: CfirPattern
    val typeRefs: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirCatchPattern {
        return CfirCatchPatternImpl(
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
inline fun buildCatchPattern(init: CfirCatchPatternBuilder.() -> Unit): CfirCatchPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirCatchPatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildCatchPatternCopy(original: CfirCatchPattern, init: CfirCatchPatternBuilder.() -> Unit): CfirCatchPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirCatchPatternBuilder()
    copyBuilder.source = original.source
    copyBuilder.semanticTypeRef = original.semanticTypeRef
    copyBuilder.constantValues.addAll(original.constantValues)
    copyBuilder.contextExpression = original.contextExpression
    copyBuilder.nestedPattern = original.nestedPattern
    copyBuilder.typeRefs.addAll(original.typeRefs)
    return copyBuilder.apply(init).build()
}
