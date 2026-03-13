

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangjie.cfir.declarations

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.CfirPureAbstractElement
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.expressions.CfirStatement
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangjie.cfir.tree.generator.CfirTree.declaration]
 */
sealed class CfirDeclaration : CfirPureAbstractElement(), CfirElement, CfirStatement {
    abstract override val source: CfirSourceElement?
    abstract val symbol: CfirSymbol<*>
    abstract val origin: CfirDeclarationOrigin
    abstract val annotations: List<CfirAnnotation>
    abstract val moduleData: CfirModuleData
    abstract val resolvePhase: CfirResolvePhase
    abstract val attributes: CfirDeclarationAttributes

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformDeclaration(this, data) as E
}
