

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
interface CfirDeclarationBuilder {
    abstract var source: CjSourceElement?
    abstract var moduleData: CfirModuleData
    abstract var resolvePhase: CfirResolvePhase
    abstract var contentSource: CfirContentSource
    abstract val annotations: MutableList<CfirAnnotation>
    abstract var origin: CfirDeclarationOrigin
    abstract var attributes: CfirDeclarationAttributes
    fun build(): CfirDeclaration
}
