

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirConstructedClassTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.impl.CfirConstructedClassTypeParameterRefImpl
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirConstructedClassTypeParameterRefBuilder {
    var source: CjSourceElement? = null
    lateinit var symbol: CfirTypeParameterSymbol

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirConstructedClassTypeParameterRef {
        return CfirConstructedClassTypeParameterRefImpl(
            source,
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildConstructedClassTypeParameterRef(init: CfirConstructedClassTypeParameterRefBuilder.() -> Unit): CfirConstructedClassTypeParameterRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirConstructedClassTypeParameterRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildConstructedClassTypeParameterRefCopy(original: CfirConstructedClassTypeParameterRef, init: CfirConstructedClassTypeParameterRefBuilder.() -> Unit): CfirConstructedClassTypeParameterRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirConstructedClassTypeParameterRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.symbol = original.symbol
    return copyBuilder.apply(init).build()
}
