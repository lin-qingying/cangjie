package org.cangnova.cangjie.cfir.visitors

import org.cangnova.cangjie.cfir.CfirElement

/**
 * 默认转换器，所有节点默认不做变换（原样返回）。
 *
 * 子类覆盖感兴趣的 transform/visit 方法来实现特定转换。
 */
open class CfirDefaultTransformer<in D> : CfirTransformer<D>() {

    override fun <E : CfirElement> transformElement(element: E, data: D): E {
        element.transformChildren(this, data)
        return element
    }
}
