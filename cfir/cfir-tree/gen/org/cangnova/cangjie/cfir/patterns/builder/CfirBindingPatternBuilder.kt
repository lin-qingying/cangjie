

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirBindingPatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirBindingPatternBuilder {
    var source: CjSourceElement? = null
    var semanticTypeRef: CfirTypeRef? = null
    val constantValues: MutableList<CfirConstantValue> = mutableListOf()
    var contextExpression: CfirExpression? = null
    lateinit var name: Name
    var typeRef: CfirTypeRef? = null
    var bindingVariable: CfirPatternBindingVariable? = null
    var nestedPattern: CfirPattern? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirBindingPattern {
        return CfirBindingPatternImpl(
            source,
            semanticTypeRef,
            constantValues,
            contextExpression,
            name,
            typeRef,
            bindingVariable,
            nestedPattern,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildBindingPattern(init: CfirBindingPatternBuilder.() -> Unit): CfirBindingPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirBindingPatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildBindingPatternCopy(original: CfirBindingPattern, init: CfirBindingPatternBuilder.() -> Unit): CfirBindingPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirBindingPatternBuilder()
    copyBuilder.source = original.source
    copyBuilder.semanticTypeRef = original.semanticTypeRef
    copyBuilder.constantValues.addAll(original.constantValues)
    copyBuilder.contextExpression = original.contextExpression
    copyBuilder.name = original.name
    copyBuilder.typeRef = original.typeRef
    copyBuilder.bindingVariable = original.bindingVariable
    copyBuilder.nestedPattern = original.nestedPattern
    return copyBuilder.apply(init).build()
}
