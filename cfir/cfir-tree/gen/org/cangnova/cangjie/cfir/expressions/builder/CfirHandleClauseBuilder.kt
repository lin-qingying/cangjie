

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirHandleClause
import org.cangnova.cangjie.cfir.expressions.impl.CfirHandleClauseImpl
import org.cangnova.cangjie.cfir.patterns.CfirCommandTypePattern
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirHandleClauseBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangJieType? = null
    lateinit var commandPattern: CfirCommandTypePattern
    lateinit var body: CfirBlock

    fun build(): CfirHandleClause {
        return CfirHandleClauseImpl(
            source,
            annotations.toMutableOrEmpty(),
            coneTypeOrNull,
            commandPattern,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildHandleClause(init: CfirHandleClauseBuilder.() -> Unit): CfirHandleClause {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirHandleClauseBuilder().apply(init).build()
}
