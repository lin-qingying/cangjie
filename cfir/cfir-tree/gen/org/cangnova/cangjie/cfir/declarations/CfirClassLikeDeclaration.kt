

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.classLikeDeclaration]
 */
sealed class CfirClassLikeDeclaration : CfirMemberDeclaration() {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val typeParameters: List<CfirTypeParameterRef>
    abstract override val status: CfirDeclarationStatus
    abstract override val isLocal: Boolean
    abstract override val symbol: CfirClassLikeSymbol<*>
    abstract val declarations: List<CfirDeclaration>
    abstract val name: Name
    abstract val superTypeRefs: List<CfirTypeRef>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitClassLikeDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformClassLikeDeclaration(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceStatus(newStatus: CfirDeclarationStatus)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirClassLikeDeclaration

    abstract override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirClassLikeDeclaration

    abstract override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirClassLikeDeclaration

    abstract fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirClassLikeDeclaration

    abstract fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirClassLikeDeclaration
}
