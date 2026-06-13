

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirTypeConversion
import org.cangnova.cangjie.cfir.expressions.impl.CfirTypeConversionImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirTypeConversionBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var argument: CfirExpression
    lateinit var targetTypeRef: CfirTypeRef

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTypeConversion {
        return CfirTypeConversionImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            argument,
            targetTypeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeConversion(init: CfirTypeConversionBuilder.() -> Unit): CfirTypeConversion {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTypeConversionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeConversionCopy(original: CfirTypeConversion, init: CfirTypeConversionBuilder.() -> Unit): CfirTypeConversion {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTypeConversionBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.argument = original.argument
    copyBuilder.targetTypeRef = original.targetTypeRef
    return copyBuilder.apply(init).build()
}
