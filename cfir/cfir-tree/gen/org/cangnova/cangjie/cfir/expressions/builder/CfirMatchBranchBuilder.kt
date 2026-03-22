

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.impl.CfirMatchBranchImpl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirMatchBranchBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var pattern: CfirPattern
    var guard: CfirExpression? = null
    lateinit var body: CfirBlock

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirMatchBranch {
        return CfirMatchBranchImpl(
            source,
            annotations,
            coneTypeOrNull,
            pattern,
            guard,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildMatchBranch(init: CfirMatchBranchBuilder.() -> Unit): CfirMatchBranch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirMatchBranchBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildMatchBranchCopy(original: CfirMatchBranch, init: CfirMatchBranchBuilder.() -> Unit): CfirMatchBranch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirMatchBranchBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.pattern = original.pattern
    copyBuilder.guard = original.guard
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
