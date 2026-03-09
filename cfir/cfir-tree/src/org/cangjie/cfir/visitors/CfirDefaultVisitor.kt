package org.cangjie.cfir.visitors

import org.cangjie.cfir.CfirElement

/**
 * 默认访问者，所有 visit 方法统一委托到 visitElement。
 *
 * 用于只需要处理少量节点类型的场景。
 */
abstract class CfirDefaultVisitor<out R, in D> : CfirVisitor<R, D>()

/**
 * 无返回值的访问者辅助类。
 */
abstract class CfirVoidVisitor : CfirVisitor<Unit, Nothing?>() {
    fun visitAll(elements: List<CfirElement>) {
        elements.forEach { it.accept(this, null) }
    }
}
