

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirQualifierPart
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.impl.CfirQualifierPartImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirQualifierPartBuilder {
    var source: CjSourceElement? = null
    lateinit var name: Name
    val typeArguments: MutableList<CfirTypeRef> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirQualifierPart {
        return CfirQualifierPartImpl(
            source,
            name,
            typeArguments.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildQualifierPart(init: CfirQualifierPartBuilder.() -> Unit): CfirQualifierPart {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirQualifierPartBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildQualifierPartCopy(original: CfirQualifierPart, init: CfirQualifierPartBuilder.() -> Unit): CfirQualifierPart {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirQualifierPartBuilder()
    copyBuilder.source = original.source
    copyBuilder.name = original.name
    copyBuilder.typeArguments.addAll(original.typeArguments)
    return copyBuilder.apply(init).build()
}
