

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangjie.cfir.declarations

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name

/**
 * Generated from: [org.cangjie.cfir.tree.generator.CfirTree.property]
 */
abstract class CfirProperty : CfirCallableDeclaration() {
    abstract override val source: CfirSourceElement?
    abstract override val symbol: CfirSymbol<*>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val annotations: List<CfirAnnotation>
    abstract override val moduleData: CfirModuleData
    abstract override val resolvePhase: CfirResolvePhase
    abstract override val attributes: CfirDeclarationAttributes
    abstract val status: CfirDeclarationStatus
    abstract val typeParameters: List<CfirTypeParameter>
    abstract val returnTypeRef: CfirTypeRef
    abstract val name: Name
    abstract val initializer: CfirExpression?
    abstract val getter: CfirFunction?
    abstract val setter: CfirFunction?
    abstract val isVar: Boolean

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitProperty(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformProperty(this, data) as E
}
