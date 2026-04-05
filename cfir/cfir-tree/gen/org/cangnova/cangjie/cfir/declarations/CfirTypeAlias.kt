

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.typeAlias]
 */
abstract class CfirTypeAlias : CfirClassLikeDeclaration() {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val declarations: List<CfirDeclaration>
    abstract override val superTypeRefs: List<CfirTypeRef>
    abstract override val symbol: CfirTypeAliasSymbol
    abstract override val status: CfirDeclarationStatus
    abstract override val typeParameters: List<CfirTypeParameter>
    abstract override val name: Name
    abstract val expandedTypeRef: CfirTypeRef

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTypeAlias(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformTypeAlias(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceStatus(newStatus: CfirDeclarationStatus)

    abstract fun replaceExpandedTypeRef(newExpandedTypeRef: CfirTypeRef)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirTypeAlias

    abstract override fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirTypeAlias

    abstract override fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirTypeAlias

    abstract override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirTypeAlias

    abstract override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirTypeAlias

    abstract fun <D> transformExpandedTypeRef(transformer: CfirTransformer<D>, data: D): CfirTypeAlias
}
