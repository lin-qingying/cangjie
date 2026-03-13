

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name

class CfirBindingPatternImpl @CfirImplementationDetail constructor(
    override val name: Name,
    override var typeRef: CfirTypeRef?,
    override var nestedPattern: CfirPattern?,
) : CfirBindingPattern() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        typeRef?.accept(visitor, data)
        nestedPattern?.accept(visitor, data)
    }

    override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirBindingPattern
     {
        this.typeRef = typeRef?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirTypeRef?
        return this
    }

    override fun <D> transformNestedPattern(transformer: CfirTransformer<D>, data: D): CfirBindingPattern
     {
        this.nestedPattern = nestedPattern?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirPattern?
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirBindingPatternImpl {
        transformTypeRef(transformer, data)
        transformNestedPattern(transformer, data)
        return this
    }
}
