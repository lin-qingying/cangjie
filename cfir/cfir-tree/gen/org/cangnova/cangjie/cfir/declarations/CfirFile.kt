

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangjie.cfir.declarations

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangjie.cfir.tree.generator.CfirTree.file]
 */
abstract class CfirFile : CfirDeclaration() {
    abstract override val source: CfirSourceElement?
    abstract override val symbol: CfirSymbol<*>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val annotations: List<CfirAnnotation>
    abstract override val moduleData: CfirModuleData
    abstract override val resolvePhase: CfirResolvePhase
    abstract override val attributes: CfirDeclarationAttributes
    abstract val name: String
    abstract val packageDirective: CfirPackageDirective
    abstract val imports: List<CfirImport>
    abstract val declarations: List<CfirDeclaration>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformFile(this, data) as E
}
