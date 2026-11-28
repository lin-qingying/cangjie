

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.patterns.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirVarOrEnumPattern
import org.cangnova.cangjie.cfir.patterns.impl.CfirVarOrEnumPatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirVarOrEnumPatternBuilder {
    var source: CjSourceElement? = null
    var semanticTypeRef: CfirTypeRef? = null
    val constantValues: MutableList<CfirConstantValue> = mutableListOf()
    var contextExpression: CfirExpression? = null
    lateinit var name: Name
    var bindingVariable: CfirPatternBindingVariable? = null

    fun build(): CfirVarOrEnumPattern {
        return CfirVarOrEnumPatternImpl(
            source,
            semanticTypeRef,
            constantValues,
            contextExpression,
            name,
            bindingVariable,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildVarOrEnumPattern(init: CfirVarOrEnumPatternBuilder.() -> Unit): CfirVarOrEnumPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirVarOrEnumPatternBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildVarOrEnumPatternCopy(original: CfirVarOrEnumPattern, init: CfirVarOrEnumPatternBuilder.() -> Unit): CfirVarOrEnumPattern {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirVarOrEnumPatternBuilder()
    copyBuilder.source = original.source
    copyBuilder.semanticTypeRef = original.semanticTypeRef
    copyBuilder.constantValues.addAll(original.constantValues)
    copyBuilder.contextExpression = original.contextExpression
    copyBuilder.name = original.name
    copyBuilder.bindingVariable = original.bindingVariable
    return copyBuilder.apply(init).build()
}
