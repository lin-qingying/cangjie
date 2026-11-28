

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation

@CfirBuilderDsl
interface CfirAnnotationContainerBuilder {
    abstract val annotations: MutableList<CfirAnnotation>

    fun build(): CfirAnnotationContainer
}
