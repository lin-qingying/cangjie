

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirFeaturesDirective
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.impl.CfirFeaturesDirectiveImpl
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirFeaturesDirectiveBuilder {
    var source: CjSourceElement? = null
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    val featureIds: MutableList<String> = mutableListOf()

    fun build(): CfirFeaturesDirective {
        return CfirFeaturesDirectiveImpl(
            source,
            annotations.toMutableOrEmpty(),
            featureIds,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFeaturesDirective(init: CfirFeaturesDirectiveBuilder.() -> Unit = {}): CfirFeaturesDirective {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirFeaturesDirectiveBuilder().apply(init).build()
}
