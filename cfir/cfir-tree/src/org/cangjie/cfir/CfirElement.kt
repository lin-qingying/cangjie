package org.cangjie.cfir

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 所有 CFIR 节点的根接口。
 *
 * 每个 CFIR 节点都可以携带源码位置信息，并支持访问者模式遍历。
 */
interface CfirElement {
    /** 源码位置信息，合成节点为 null */
    val source: CfirSourceElement?

    /** 接受访问者 */
    fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R

    /** 对子节点应用转换器 */
    fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement
}
