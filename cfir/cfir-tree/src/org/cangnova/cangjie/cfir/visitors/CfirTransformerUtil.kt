package org.cangnova.cangjie.cfir.visitors

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.MutableOrEmptyList

/**
 * 转换单个 CFIR 元素并保持静态类型。
 */
fun <T : CfirElement, D> T.transformSingle(transformer: CfirTransformer<D>, data: D): T {
    return (this as CfirPureAbstractElement).transform<T, D>(transformer, data)
}

/**
 * 原地转换 [MutableOrEmptyList] 中的元素。
 */
fun <T : CfirElement, D> MutableOrEmptyList<T>.transformInplace(transformer: CfirTransformer<D>, data: D) {
    list?.transformInplace(transformer, data)
}

/**
 * 原地转换可变列表中的 CFIR 元素。
 */
fun <T : CfirElement, D> MutableList<T>.transformInplace(transformer: CfirTransformer<D>, data: D) {
    val iterator = this.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next() as CfirPureAbstractElement
        val result = next.transform<T, D>(transformer, data)
        if (result !== next) {
            iterator.set(result)
        }
    }
}
