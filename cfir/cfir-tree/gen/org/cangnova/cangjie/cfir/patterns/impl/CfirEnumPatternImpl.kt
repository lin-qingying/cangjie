

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirEnumPatternImpl @CfirImplementationDetail constructor(
    override var constructorReference: CfirReference,
    override var arguments: List<CfirPattern>,
) : CfirEnumPattern() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        constructorReference.accept(visitor, data)
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformConstructorReference(transformer: CfirTransformer<D>, data: D): CfirEnumPattern
     {
        this.constructorReference = constructorReference.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirReference
        return this
    }

    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirEnumPattern
     {
        this.arguments = arguments.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirPattern }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirEnumPatternImpl {
        transformConstructorReference(transformer, data)
        transformArguments(transformer, data)
        return this
    }
}
