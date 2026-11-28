

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePatternMatchingKind
import org.cangnova.cangjie.cfir.patterns.impl.CfirTypePatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirTypePatternBuilder {
    var source: CjSourceElement? = null
    var semanticTypeRef: CfirTypeRef? = null
    val constantValues: MutableList<CfirConstantValue> = mutableListOf()
    var contextExpression: CfirExpression? = null
    var matchingKind: CfirTypePatternMatchingKind = CfirTypePatternMatchingKind.UNKNOWN
    lateinit var typeRef: CfirTypeRef
    lateinit var nestedPattern: CfirPattern
    var matchBeforeRuntime: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var needRuntimeTypeCheck: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTypePattern {
        return CfirTypePatternImpl(
            source,
            semanticTypeRef,
            constantValues,
            contextExpression,
            matchingKind,
            typeRef,
            nestedPattern,
            matchBeforeRuntime,
            needRuntimeTypeCheck,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypePattern(init: CfirTypePatternBuilder.() -> Unit): CfirTypePattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTypePatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypePatternCopy(original: CfirTypePattern, init: CfirTypePatternBuilder.() -> Unit): CfirTypePattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTypePatternBuilder()
    copyBuilder.source = original.source
    copyBuilder.semanticTypeRef = original.semanticTypeRef
    copyBuilder.constantValues.addAll(original.constantValues)
    copyBuilder.contextExpression = original.contextExpression
    copyBuilder.matchingKind = original.matchingKind
    copyBuilder.typeRef = original.typeRef
    copyBuilder.nestedPattern = original.nestedPattern
    copyBuilder.matchBeforeRuntime = original.matchBeforeRuntime
    copyBuilder.needRuntimeTypeCheck = original.needRuntimeTypeCheck
    return copyBuilder.apply(init).build()
}
