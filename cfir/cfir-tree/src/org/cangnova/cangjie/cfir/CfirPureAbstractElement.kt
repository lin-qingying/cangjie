package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

abstract class CfirPureAbstractElement : CfirElement {
    abstract override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D)

    abstract override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement
}
