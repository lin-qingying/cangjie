

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.builder.CfirAnnotationContainerBuilder
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
interface CfirFunctionBuilder : CfirDeclarationBuilder, CfirAnnotationContainerBuilder {
    abstract override var source: CjSourceElement?
    abstract override var moduleData: CfirModuleData
    abstract override var resolvePhase: CfirResolvePhase
    abstract override var contentSource: CfirContentSource
    abstract override val annotations: MutableList<CfirAnnotation>
    abstract override var origin: CfirDeclarationOrigin
    abstract override var attributes: CfirDeclarationAttributes
    abstract var isLocal: Boolean
    abstract var returnTypeRef: CfirTypeRef
    abstract var deprecationsProvider: DeprecationsProvider
    abstract var dispatchReceiverType: ConeSimpleCangJieType?
    abstract var body: CfirBlock?
    abstract var status: CfirDeclarationStatus
    abstract val valueParameters: MutableList<CfirValueParameter>
    abstract var hasVariableLenArg: Boolean
    override fun build(): CfirFunction
}
