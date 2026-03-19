

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

internal class CfirExpressionPatternImpl(
    override val source: CjSourceElement?,
    override var expression: CfirExpression,
) : CfirExpressionPattern() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        expression.accept(visitor, data)
    }

    override fun <D> transformExpression(transformer: CfirTransformer<D>, data: D): CfirExpressionPattern
     {
        this.expression = expression.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirExpression
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirExpressionPatternImpl {
        transformExpression(transformer, data)
        return this
    }
}
