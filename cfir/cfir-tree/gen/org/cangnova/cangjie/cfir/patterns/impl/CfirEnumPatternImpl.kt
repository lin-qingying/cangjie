

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.patterns.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.patterns.CfirEnumPattern
import org.cangjie.cfir.patterns.CfirPattern
import org.cangjie.cfir.references.CfirReference
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirEnumPatternImpl @CfirImplementationDetail constructor(
    override val constructorReference: CfirReference,
    override val arguments: List<CfirPattern>,
) : CfirEnumPattern() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        constructorReference.accept(visitor, data)
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirEnumPatternImpl {
        constructorReference.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        arguments.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        return this
    }
}
