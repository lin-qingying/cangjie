

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.extend]
 */
abstract class CfirExtend : CfirMemberDeclaration(), CfirTypeParametersOwner {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val contentSource: CfirContentSource
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val typeParameters: List<CfirTypeParameter>
    abstract override val symbol: CfirExtendSymbol
    abstract override val status: CfirDeclarationStatus
    abstract val extendedTypeRef: CfirTypeRef
    abstract val superTypeRefs: List<CfirTypeRef>
    abstract val declarations: List<CfirDeclaration>

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        extendedTypeRef.accept(visitor, data)
        superTypeRefs.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitExtend(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformExtend(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceStatus(newStatus: CfirDeclarationStatus)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirExtend

    abstract override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirExtend

    abstract override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirExtend

    abstract fun <D> transformExtendedTypeRef(transformer: CfirTransformer<D>, data: D): CfirExtend

    abstract fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirExtend

    abstract fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirExtend
}
