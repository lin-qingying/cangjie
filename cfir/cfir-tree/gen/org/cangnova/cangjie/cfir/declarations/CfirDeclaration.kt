

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.declaration]
 */
sealed class CfirDeclaration : CfirElementWithResolveState(), CfirAnnotationContainer {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract val symbol: CfirBasedSymbol<*>
    abstract val origin: CfirDeclarationOrigin
    abstract val attributes: CfirDeclarationAttributes

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformDeclaration(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirDeclaration

    override fun toString(): String = CfirRenderer.withReadability().renderElementAsString(this)

}
