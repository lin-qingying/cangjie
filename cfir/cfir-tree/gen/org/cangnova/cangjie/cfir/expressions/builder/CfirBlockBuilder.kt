

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.impl.CfirBlockImpl
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType

@CfirBuilderDsl
class CfirBlockBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    var coneTypeOrNull: ConeCangjieType? = null
    val statements: MutableList<CfirElement> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirBlock {
        return CfirBlockImpl(
            source,
            annotations,
            coneTypeOrNull,
            statements,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildBlock(init: CfirBlockBuilder.() -> Unit = {}): CfirBlock {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirBlockBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildBlockCopy(original: CfirBlock, init: CfirBlockBuilder.() -> Unit = {}): CfirBlock {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirBlockBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.statements.addAll(original.statements)
    return copyBuilder.apply(init).build()
}
