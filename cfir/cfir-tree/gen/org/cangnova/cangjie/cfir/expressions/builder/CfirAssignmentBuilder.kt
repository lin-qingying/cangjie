

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
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirAssignmentTypeMismatchOutcome
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirAssignmentImpl
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirAssignmentBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    var mappedExpression: CfirExpression? = null
    var overflowStrategy: CangjieOverflowStrategy? = null
    lateinit var lValue: CfirExpression
    lateinit var rValue: CfirExpression
    var augmentedOperation: Name? = null
    var operation: CfirSourceOperator = CfirSourceOperator.ASSIGN
    var isCompound: Boolean = false
    var typeMismatchOutcome: CfirAssignmentTypeMismatchOutcome? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirAssignment {
        return CfirAssignmentImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            mappedExpression,
            overflowStrategy,
            lValue,
            rValue,
            augmentedOperation,
            operation,
            isCompound,
            typeMismatchOutcome,
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
    copyBuilder.mappedExpression = original.mappedExpression
    copyBuilder.overflowStrategy = original.overflowStrategy
    copyBuilder.lValue = original.lValue
    copyBuilder.rValue = original.rValue
    copyBuilder.augmentedOperation = original.augmentedOperation
    copyBuilder.operation = original.operation
    copyBuilder.isCompound = original.isCompound
    copyBuilder.typeMismatchOutcome = original.typeMismatchOutcome
    return copyBuilder.apply(init).build()
}
