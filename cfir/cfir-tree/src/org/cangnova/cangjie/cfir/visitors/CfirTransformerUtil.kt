package org.cangnova.cangjie.cfir.visitors

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirPureAbstractElement

fun <T : CfirElement, D> T.transformSingle(transformer: CfirTransformer<D>, data: D): T {
    return (this as CfirPureAbstractElement).transform<T, D>(transformer, data)
}
