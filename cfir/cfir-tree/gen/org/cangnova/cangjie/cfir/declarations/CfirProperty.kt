

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.property]
 */
abstract class CfirProperty : CfirCallableDeclaration(), CfirControlFlowGraphOwner {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val isLocal: Boolean
    abstract override val dispatchReceiverType: ConeSimpleCangJieType?
    abstract override val controlFlowGraphReference: CfirControlFlowGraphReference?
    abstract override val symbol: CfirPropertySymbol
    abstract override val status: CfirDeclarationStatus
    abstract override val typeParameters: List<CfirTypeParameter>
    abstract override val returnTypeRef: CfirTypeRef
    abstract val name: Name
    abstract val getter: CfirFunction?
    abstract val setter: CfirFunction?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitProperty(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformProperty(this, data) as E

    override abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    override abstract fun replaceControlFlowGraphReference(newControlFlowGraphReference: CfirControlFlowGraphReference?)


    override abstract fun replaceStatus(newStatus: CfirDeclarationStatus)


    override abstract fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef)


    override abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirProperty


    override abstract fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirProperty


    override abstract fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirProperty


    override abstract fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirProperty


    abstract fun <D> transformGetter(transformer: CfirTransformer<D>, data: D): CfirProperty


    abstract fun <D> transformSetter(transformer: CfirTransformer<D>, data: D): CfirProperty

}
