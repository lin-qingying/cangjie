

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirEnumPatternImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var constructorReference: CfirReference,
    override val arguments: MutableList<CfirPattern>,
) : CfirEnumPattern() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        constructorReference.accept(visitor, data)
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirEnumPatternImpl {
        transformConstructorReference(transformer, data)
        transformArguments(transformer, data)
        return this
    }

    override fun <D> transformConstructorReference(transformer: CfirTransformer<D>, data: D): CfirEnumPatternImpl {
        constructorReference = constructorReference.transform(transformer, data)
        return this
    }

    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirEnumPatternImpl {
        arguments.transformInplace(transformer, data)
        return this
    }
}
