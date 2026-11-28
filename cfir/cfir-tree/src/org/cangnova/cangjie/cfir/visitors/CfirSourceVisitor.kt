package org.cangnova.cangjie.cfir.visitors

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 保留语义载荷的来源协议。IR 与类型可以同时被 scope、receiver 和 DFA 快照持有，
 * 高层状态 owner 通过该公共抽象交出已有载荷，不反向依赖 resolve 或 CFG 的实现。
 */
interface CfirSourceVisitor {
    fun visitElement(element: CfirElement)
    fun visitType(type: ConeCangJieType)
}
