

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
import org.cangnova.cangjie.cfir.expressions.CfirArrayCreationExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionArgument
import org.cangnova.cangjie.cfir.expressions.impl.CfirArrayCreationExpressionImpl
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirArrayCreationExpressionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    val arguments: MutableList<CfirFunctionArgument> = mutableListOf()
    var initializerSymbol: CfirFunctionSymbol<*>? = null
    var isValueArray: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirArrayCreationExpression {
        return CfirArrayCreationExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            arguments,
            initializerSymbol,
            isValueArray,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildArrayCreationExpression(init: CfirArrayCreationExpressionBuilder.() -> Unit): CfirArrayCreationExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirArrayCreationExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildArrayCreationExpressionCopy(original: CfirArrayCreationExpression, init: CfirArrayCreationExpressionBuilder.() -> Unit): CfirArrayCreationExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirArrayCreationExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.arguments.addAll(original.arguments)
    copyBuilder.initializerSymbol = original.initializerSymbol
    copyBuilder.isValueArray = original.isValueArray
    return copyBuilder.apply(init).build()
}
