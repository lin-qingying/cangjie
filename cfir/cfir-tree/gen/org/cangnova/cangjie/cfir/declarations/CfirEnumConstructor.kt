

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFunctionBodyOwner
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.enumConstructor]
 */
abstract class CfirEnumConstructor : CfirCallableDeclaration(), CfirFunctionBodyOwner {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val contentSource: CfirContentSource
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val typeParameters: List<CfirTypeParameterRef>
    abstract override val isLocal: Boolean
    abstract override val returnTypeRef: CfirTypeRef
    abstract override val deprecationsProvider: DeprecationsProvider
    abstract override val dispatchReceiverType: ConeSimpleCangJieType?
    abstract override val controlFlowGraphReference: CfirControlFlowGraphReference?
    abstract override val valueParameters: List<CfirValueParameter>
    abstract override val body: CfirBlock?
    abstract override val symbol: CfirEnumConstructorSymbol
    abstract override val status: CfirDeclarationStatus
    abstract val name: Name

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        controlFlowGraphReference?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        status.accept(visitor, data)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitEnumConstructor(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformEnumConstructor(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef)

    abstract override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract override fun replaceDispatchReceiverType(newDispatchReceiverType: ConeSimpleCangJieType?)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: CfirControlFlowGraphReference?)

    abstract override fun replaceValueParameters(newValueParameters: List<CfirValueParameter>)

    abstract override fun replaceBody(newBody: CfirBlock?)

    abstract override fun replaceStatus(newStatus: CfirDeclarationStatus)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirEnumConstructor

    abstract override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirEnumConstructor

    abstract override fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirEnumConstructor

    abstract override fun <D> transformValueParameters(transformer: CfirTransformer<D>, data: D): CfirEnumConstructor

    abstract override fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirEnumConstructor

    abstract override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirEnumConstructor
}
