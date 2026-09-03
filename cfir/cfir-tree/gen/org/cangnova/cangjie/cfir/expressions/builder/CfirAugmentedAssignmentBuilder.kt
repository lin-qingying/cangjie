

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAugmentedAssignment
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirAugmentedAssignmentImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirAugmentedAssignmentBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var operation: Name
    var operationSource: CjSourceElement? = null
    lateinit var leftArgument: CfirExpression
    lateinit var rightArgument: CfirExpression

    fun build(): CfirAugmentedAssignment {
        return CfirAugmentedAssignmentImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            operation,
            operationSource,
            leftArgument,
            rightArgument,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAugmentedAssignment(init: CfirAugmentedAssignmentBuilder.() -> Unit): CfirAugmentedAssignment {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirAugmentedAssignmentBuilder().apply(init).build()
}
