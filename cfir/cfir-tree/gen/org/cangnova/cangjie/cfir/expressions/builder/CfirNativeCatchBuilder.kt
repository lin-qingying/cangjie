

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirNativeCatch
import org.cangnova.cangjie.cfir.expressions.impl.CfirNativeCatchImpl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirNativeCatchBuilder {
    var source: CjSourceElement? = null
    lateinit var pattern: CfirPattern
    lateinit var body: CfirBlock

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirNativeCatch {
        return CfirNativeCatchImpl(
            source,
            pattern,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildNativeCatch(init: CfirNativeCatchBuilder.() -> Unit): CfirNativeCatch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirNativeCatchBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildNativeCatchCopy(original: CfirNativeCatch, init: CfirNativeCatchBuilder.() -> Unit): CfirNativeCatch {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirNativeCatchBuilder()
    copyBuilder.source = original.source
    copyBuilder.pattern = original.pattern
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
