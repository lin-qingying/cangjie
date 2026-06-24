package org.cangnova.cangjie.cfir.visitors

import org.cangnova.cangjie.cfir.CfirElement

/**
 * 不返回结果且不携带上下文数据的 CFIR visitor 基类。
 */
abstract class CfirVoidVisitor : CfirVisitor<Unit, Nothing?>() {
    /**
     * 依次访问元素列表。
     */
    fun visitAll(elements: List<CfirElement>) {
        elements.forEach { it.accept(this, null) }
    }
}
