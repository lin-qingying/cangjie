

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbolWithClassId
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
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
    abstract override val symbol: CfirClassifierSymbolWithClassId<*>
    abstract val declarations: List<CfirDeclaration>
    abstract val superTypeRefs: List<CfirTypeRef>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitClassLikeDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformClassLikeDeclaration(this, data) as E

    override abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    override abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirClassLikeDeclaration


    abstract fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirClassLikeDeclaration


    abstract fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirClassLikeDeclaration

}
