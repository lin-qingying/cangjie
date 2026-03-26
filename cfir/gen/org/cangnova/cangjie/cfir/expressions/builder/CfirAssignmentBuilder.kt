

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirAssignmentImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirAssignmentBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var lValue: CfirExpression
    lateinit var rValue: CfirExpression

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirAssignment {
        return CfirAssignmentImpl(
            source,
            annotations,
            coneTypeOrNull,
            lValue,
            rValue,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAssignment(init: CfirAssignmentBuilder.() -> Unit): CfirAssignment {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirAssignmentBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAssignmentCopy(original: CfirAssignment, init: CfirAssignmentBuilder.() -> Unit): CfirAssignment {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirAssignmentBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.lValue = original.lValue
    copyBuilder.rValue = original.rValue
    return copyBuilder.apply(init).build()
}
