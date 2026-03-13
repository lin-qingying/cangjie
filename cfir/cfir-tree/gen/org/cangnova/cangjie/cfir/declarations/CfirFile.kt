

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.file]
 */
abstract class CfirFile : CfirDeclaration() {
    abstract override val source: CjSourceElement?
    abstract override val symbol: CfirSymbol<*>
    abstract override val origin: CfirDeclarationOrigin
    abstract override var annotations: List<CfirAnnotation>
    abstract override val moduleData: CfirModuleData
    abstract override var resolvePhase: CfirResolvePhase
    abstract override val attributes: CfirDeclarationAttributes
    abstract val name: String
    abstract var packageDirective: CfirPackageDirective
    abstract var imports: List<CfirImport>
    abstract var declarations: List<CfirDeclaration>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformFile(this, data) as E

    override abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    override abstract fun replaceResolvePhase(newResolvePhase: CfirResolvePhase)


    override abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirFile


    abstract fun <D> transformPackageDirective(transformer: CfirTransformer<D>, data: D): CfirFile


    abstract fun <D> transformImports(transformer: CfirTransformer<D>, data: D): CfirFile


    abstract fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirFile

}
