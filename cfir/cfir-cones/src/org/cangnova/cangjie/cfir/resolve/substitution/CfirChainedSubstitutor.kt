package org.cangnova.cangjie.cfir.resolve.substitution

import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection

/**
 * 顺序组合两个替换器：先应用 [first]，再将 [second] 应用于替换后的类型。
 *
 * 对齐 Kotlin `ChainedSubstitutor`。不能直接合并映射，否则 `T -> U -> Int64`
 * 会停留在 U，继承实例化后继续推导函数类型参数也会丢失第二次替换。
 */
data class CfirChainedSubstitutor(
    private val first: ConeSubstitutor,
    private val second: ConeSubstitutor,
) : ConeSubstitutor() {
    override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? {
        val firstResult = first.substituteOrNull(type)
        return second.substituteOrNull(firstResult ?: type) ?: firstResult
    }

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        val firstResult = first.substituteArgument(projection, index)
        return second.substituteArgument(firstResult ?: projection, index) ?: firstResult
    }
}
