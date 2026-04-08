

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

internal class CfirArgumentListImpl(
    override val source: CjSourceElement?,
    override val arguments: MutableList<CfirExpression>,
) : CfirArgumentList() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirArgumentListImpl {
        transformArguments(transformer, data)
        return this
    }

    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirArgumentListImpl {
        arguments.transformInplace(transformer, data)
        return this
    }
}
