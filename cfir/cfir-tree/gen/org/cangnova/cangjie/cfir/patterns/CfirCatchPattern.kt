

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.catchPattern]
 */
abstract class CfirCatchPattern : CfirPureAbstractElement(), CfirElement {
    abstract override val source: CjSourceElement?
    abstract val bindingName: Name?
    abstract val isWildcard: Boolean
    abstract val typeRefs: List<CfirTypeRef>
    abstract val bindingVariable: CfirPatternBindingVariable?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitCatchPattern(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformCatchPattern(this, data) as E

    abstract fun <D> transformTypeRefs(transformer: CfirTransformer<D>, data: D): CfirCatchPattern

    abstract fun <D> transformBindingVariable(transformer: CfirTransformer<D>, data: D): CfirCatchPattern
}
