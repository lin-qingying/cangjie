

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirBinaryOpImpl
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirBinaryOpBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangjieType? = null
    lateinit var kind: CfirBinaryOpKind
    lateinit var left: CfirExpression
    lateinit var right: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirBinaryOp {
        return CfirBinaryOpImpl(
            source,
            annotations,
            coneTypeOrNull,
            kind,
            left,
            right,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildBinaryOp(init: CfirBinaryOpBuilder.() -> Unit): CfirBinaryOp {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirBinaryOpBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildBinaryOpCopy(original: CfirBinaryOp, init: CfirBinaryOpBuilder.() -> Unit): CfirBinaryOp {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirBinaryOpBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.kind = original.kind
    copyBuilder.left = original.left
    copyBuilder.right = original.right
    return copyBuilder.apply(init).build()
}
