package org.cangnova.cangjie.cfir.resolve.substitution

import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection

/** 按 Kotlin 的链式替换顺序组合两个映射；空映射直接消去，不产生额外签名层。 */
data class ChainedSubstitutor private constructor(val first: ConeSubstitutor, val second: ConeSubstitutor) : ConeSubstitutor() {
    companion object {
        operator fun invoke(first: ConeSubstitutor, second: ConeSubstitutor): ConeSubstitutor {
            if (first == Empty) return second
            if (second == Empty) return first
            return ChainedSubstitutor(first, second)
        }
    }

    override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? {
        first.substituteOrNull(type)?.let { return second.substituteOrSelf(it) }
        return second.substituteOrNull(type)
    }

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        val firstResult = first.substituteArgument(projection, index)
        return second.substituteArgument(firstResult ?: projection, index) ?: firstResult
    }

    override fun toString(): String = "$first then $second"
}

fun ConeSubstitutor.chain(other: ConeSubstitutor): ConeSubstitutor = ChainedSubstitutor(this, other)
