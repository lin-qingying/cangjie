

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.extend]
 */
abstract class CfirExtend : CfirMemberDeclaration() {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val symbol: CfirExtendSymbol
    abstract val status: CfirDeclarationStatus
    abstract val typeParameters: List<CfirTypeParameter>
    abstract val extendedTypeRef: CfirTypeRef
    abstract val superTypeRefs: List<CfirTypeRef>
    abstract val declarations: List<CfirDeclaration>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitExtend(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformExtend(this, data) as E

    override abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    abstract fun replaceStatus(newStatus: CfirDeclarationStatus)


    override abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirExtend


    abstract fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirExtend


    abstract fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirExtend


    abstract fun <D> transformExtendedTypeRef(transformer: CfirTransformer<D>, data: D): CfirExtend


    abstract fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirExtend


    abstract fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirExtend

}
