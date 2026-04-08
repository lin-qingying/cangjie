

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.structDeclaration]
 */
abstract class CfirStruct : CfirClassLikeDeclaration(), CfirControlFlowGraphOwner {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val controlFlowGraphReference: CfirControlFlowGraphReference?
    abstract override val status: CfirDeclarationStatus
    abstract override val typeParameters: List<CfirTypeParameter>
    abstract override val symbol: CfirStructSymbol
    abstract override val superTypeRefs: List<CfirTypeRef>
    abstract override val declarations: List<CfirDeclaration>
    abstract override val name: Name

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitStruct(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformStruct(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: CfirControlFlowGraphReference?)

    abstract override fun replaceStatus(newStatus: CfirDeclarationStatus)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirStruct

    abstract override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirStruct

    abstract override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirStruct

    abstract override fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirStruct

    abstract override fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirStruct
}
