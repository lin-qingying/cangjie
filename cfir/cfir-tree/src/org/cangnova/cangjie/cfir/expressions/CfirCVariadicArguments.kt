package org.cangnova.cangjie.cfir.expressions

import java.util.Collections

/**
 * C ellipsis 的正式实参角色。ellipsis 不产生形参；fixed 数量与有序 extras 是独立产品。
 * 泛型槽允许候选持有 atom、完成树持有 expression，两层复用同一合同而不互相依赖。
 * null 表示没有 ellipsis；非 null 且 extras 为空表示合法的零 extra 调用。
 */
class CfirCVariadicArguments<A>(val fixedParameterCount: Int, extraArguments: Collection<A>) {
    val extraArguments: List<A> = Collections.unmodifiableList(extraArguments.toList())

    init {
        require(fixedParameterCount >= 0)
    }

    fun <B> map(transform: (A) -> B): CfirCVariadicArguments<B> =
        CfirCVariadicArguments(fixedParameterCount, extraArguments.map(transform))
}
