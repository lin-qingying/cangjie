

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.symbols.CfirCodeFragmentSymbol
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.codeFragment]
 */
abstract class CfirCodeFragment : CfirDeclaration() {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val symbol: CfirCodeFragmentSymbol
    abstract val block: CfirBlock

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitCodeFragment(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformCodeFragment(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract fun replaceBlock(newBlock: CfirBlock)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirCodeFragment

    abstract fun <D> transformBlock(transformer: CfirTransformer<D>, data: D): CfirCodeFragment
}
