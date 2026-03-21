

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.expressions.impl.CfirTypeOperatorImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirTypeOperatorBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var operation: CfirTypeOperationKind
    lateinit var argument: CfirExpression
    lateinit var typeRef: CfirTypeRef

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTypeOperator {
        return CfirTypeOperatorImpl(
            source,
            annotations,
            coneTypeOrNull,
            operation,
            argument,
            typeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeOperator(init: CfirTypeOperatorBuilder.() -> Unit): CfirTypeOperator {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTypeOperatorBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeOperatorCopy(original: CfirTypeOperator, init: CfirTypeOperatorBuilder.() -> Unit): CfirTypeOperator {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTypeOperatorBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.operation = original.operation
    copyBuilder.argument = original.argument
    copyBuilder.typeRef = original.typeRef
    return copyBuilder.apply(init).build()
}
