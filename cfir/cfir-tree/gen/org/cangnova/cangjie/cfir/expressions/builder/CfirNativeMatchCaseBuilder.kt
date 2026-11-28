

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
import org.cangnova.cangjie.cfir.expressions.CfirNativeMatchCase
import org.cangnova.cangjie.cfir.expressions.impl.CfirNativeMatchCaseImpl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirNativeMatchCaseBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    val patterns: MutableList<CfirPattern> = mutableListOf()
    var guard: CfirExpression? = null
    lateinit var body: CfirBlock

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirNativeMatchCase {
        return CfirNativeMatchCaseImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            patterns,
            guard,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildNativeMatchCase(init: CfirNativeMatchCaseBuilder.() -> Unit): CfirNativeMatchCase {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirNativeMatchCaseBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildNativeMatchCaseCopy(original: CfirNativeMatchCase, init: CfirNativeMatchCaseBuilder.() -> Unit): CfirNativeMatchCase {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirNativeMatchCaseBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.patterns.addAll(original.patterns)
    copyBuilder.guard = original.guard
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
