

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirCall
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
interface CfirCallBuilder {
    abstract var source: CjSourceElement?
    abstract val annotations: MutableList<CfirAnnotation>
    abstract var argumentList: CfirArgumentList
    fun build(): CfirCall
}
