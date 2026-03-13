

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.bindingPattern]
 */
abstract class CfirBindingPattern : CfirPattern() {
    abstract override val source: CjSourceElement?
    abstract val name: Name
    abstract var typeRef: CfirTypeRef?
    abstract var nestedPattern: CfirPattern?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitBindingPattern(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformBindingPattern(this, data) as E

    abstract fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirBindingPattern


    abstract fun <D> transformNestedPattern(transformer: CfirTransformer<D>, data: D): CfirBindingPattern

}
