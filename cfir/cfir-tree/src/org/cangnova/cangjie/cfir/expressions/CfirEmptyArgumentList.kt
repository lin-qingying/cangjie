package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.source.CjSourceElement

/**
 * 空实参列表的单例实现。
 *
 * 用于没有任何实参的调用节点，避免为每个空调用重复分配列表对象。
 */
object CfirEmptyArgumentList : CfirAbstractArgumentList() {
    /**
     * 空实参列表始终返回空表达式列表。
     */
    override val arguments: List<CfirExpression>
        get() = emptyList()

    /**
     * 空实参列表没有独立源码位置。
     */
    override val source: CjSourceElement?
        get() = null
}
