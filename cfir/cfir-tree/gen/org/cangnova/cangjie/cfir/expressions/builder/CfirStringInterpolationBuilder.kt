

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
import org.cangnova.cangjie.cfir.expressions.CfirStringInterpolation
import org.cangnova.cangjie.cfir.expressions.impl.CfirStringInterpolationImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirStringInterpolationBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    val parts: MutableList<CfirExpression> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirStringInterpolation {
        return CfirStringInterpolationImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            parts,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildStringInterpolation(init: CfirStringInterpolationBuilder.() -> Unit = {}): CfirStringInterpolation {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirStringInterpolationBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildStringInterpolationCopy(original: CfirStringInterpolation, init: CfirStringInterpolationBuilder.() -> Unit = {}): CfirStringInterpolation {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirStringInterpolationBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.parts.addAll(original.parts)
    return copyBuilder.apply(init).build()
}
