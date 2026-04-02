

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

internal class CfirOrPatternImpl(
    override val source: CjSourceElement?,
    override val alternatives: MutableList<CfirPattern>,
) : CfirOrPattern() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        alternatives.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirOrPatternImpl {
        transformAlternatives(transformer, data)
        return this
    }

    override fun <D> transformAlternatives(transformer: CfirTransformer<D>, data: D): CfirOrPatternImpl {
        alternatives.transformInplace(transformer, data)
        return this
    }
}
