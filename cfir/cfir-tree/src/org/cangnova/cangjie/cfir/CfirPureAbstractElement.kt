package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * 纯抽象 CFIR 元素基类。
 *
 * 该类要求子类显式实现 children 访问与转换逻辑，不提供默认空实现。
 */
abstract class CfirPureAbstractElement : CfirElement {
    /**
     * 访问当前元素的所有子节点。
     */
    abstract override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D)

    /**
     * 转换当前元素的所有子节点。
     */
    abstract override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement
}
