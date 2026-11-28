

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
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionArgument
import org.cangnova.cangjie.cfir.expressions.impl.CfirFunctionArgumentImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirFunctionArgumentBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    var value: CfirExpression? = null
    var isDefaultValue: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var withInout: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirFunctionArgument {
        return CfirFunctionArgumentImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            value,
            isDefaultValue,
            withInout,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionArgument(init: CfirFunctionArgumentBuilder.() -> Unit): CfirFunctionArgument {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirFunctionArgumentBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionArgumentCopy(original: CfirFunctionArgument, init: CfirFunctionArgumentBuilder.() -> Unit): CfirFunctionArgument {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirFunctionArgumentBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.value = original.value
    copyBuilder.isDefaultValue = original.isDefaultValue
    copyBuilder.withInout = original.withInout
    return copyBuilder.apply(init).build()
}
