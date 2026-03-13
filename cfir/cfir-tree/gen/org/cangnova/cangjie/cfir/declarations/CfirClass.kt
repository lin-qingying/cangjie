

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.classDeclaration]
 */
abstract class CfirClass : CfirClassLikeDeclaration() {
    abstract override val source: CjSourceElement?
    abstract override val symbol: CfirSymbol<*>
    abstract override val origin: CfirDeclarationOrigin
    abstract override var annotations: List<CfirAnnotation>
    abstract override val moduleData: CfirModuleData
    abstract override var resolvePhase: CfirResolvePhase
    abstract override val attributes: CfirDeclarationAttributes
    abstract var status: CfirDeclarationStatus
    abstract var typeParameters: List<CfirTypeParameter>
    abstract var superTypeRefs: List<CfirTypeRef>
    abstract var declarations: List<CfirDeclaration>
    abstract val name: Name
    abstract val classKind: CfirClassKind

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformClass(this, data) as E

    override abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    override abstract fun replaceResolvePhase(newResolvePhase: CfirResolvePhase)


    abstract fun replaceStatus(newStatus: CfirDeclarationStatus)


    override abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirClass


    abstract fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirClass


    abstract fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirClass


    abstract fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirClass


    abstract fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirClass

}
