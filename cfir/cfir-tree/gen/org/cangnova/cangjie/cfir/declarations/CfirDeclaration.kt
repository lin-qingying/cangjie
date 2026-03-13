

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.declaration]
 */
sealed class CfirDeclaration : CfirPureAbstractElement(), CfirElement, CfirStatement {
    abstract override val source: CjSourceElement?
    abstract val symbol: CfirSymbol<*>
    abstract val origin: CfirDeclarationOrigin
    abstract var annotations: List<CfirAnnotation>
    abstract val moduleData: CfirModuleData
    abstract var resolvePhase: CfirResolvePhase
    abstract val attributes: CfirDeclarationAttributes

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformDeclaration(this, data) as E

    abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    abstract fun replaceResolvePhase(newResolvePhase: CfirResolvePhase)


    abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirDeclaration

}
