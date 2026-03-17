

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

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
    override val source: CjSourceElement?,
    override var constructorReference: CfirReference,
    override var arguments: List<CfirPattern>,
) : CfirEnumPattern() {

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
