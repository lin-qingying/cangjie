

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.expressions.builder

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirLazyExpressionImpl

@OptIn(CfirImplementationDetail::class)
fun buildLazyExpression(): CfirLazyExpression {
    return CfirLazyExpressionImpl()
}
