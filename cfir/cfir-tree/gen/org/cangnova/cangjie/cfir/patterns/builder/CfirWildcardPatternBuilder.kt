

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirWildcardPatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirWildcardPatternBuilder {
    var source: CjSourceElement? = null
    var semanticTypeRef: CfirTypeRef? = null
    val constantValues: MutableList<CfirConstantValue> = mutableListOf()
    var contextExpression: CfirExpression? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirWildcardPattern {
        return CfirWildcardPatternImpl(
            source,
            semanticTypeRef,
            constantValues,
            contextExpression,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildWildcardPattern(init: CfirWildcardPatternBuilder.() -> Unit = {}): CfirWildcardPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirWildcardPatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildWildcardPatternCopy(original: CfirWildcardPattern, init: CfirWildcardPatternBuilder.() -> Unit = {}): CfirWildcardPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirWildcardPatternBuilder()
    copyBuilder.source = original.source
    copyBuilder.semanticTypeRef = original.semanticTypeRef
    copyBuilder.constantValues.addAll(original.constantValues)
    copyBuilder.contextExpression = original.contextExpression
    return copyBuilder.apply(init).build()
}
