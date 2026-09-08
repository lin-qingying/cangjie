package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 仓颉类型交叉工具。
 *
 * 对齐 FIR ConeTypeIntersector：展开交叉后移除严格父类型，再按类型系统的等价关系
 * 去重。例如 Int64 与 Equal<Int64> 的交叉仍为 Int64，不能把冗余接口带入后续泛型调用。
 * 仓颉的 Option 装箱属于表达式转换，不参与交叉类型的父类型剪枝。
 */
object ConeTypeIntersector {
    /**
     * 对 [types] 做交叉规范化并返回交叉结果。
     */
    fun intersectTypes(context: ConeTypeContext, types: Collection<ConeCangJieType>): ConeCangJieType {
        val normalized = linkedSetOf<ConeCangJieType>()

        fun collect(type: ConeCangJieType) {
            when (type) {
                is ConeIntersectionType -> type.intersectedTypes.forEach(::collect)
                else -> normalized += type
            }
        }

        types.forEach(::collect)

        if (normalized.isEmpty()) {
            return context.anyType() as ConeCangJieType
        }

        if (normalized.size == 1) {
            return normalized.single()
        }

        val result = normalized.toMutableList()
        result.removeIfInRelation { candidate, other ->
            AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(context, other, candidate) &&
                    !AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(context, candidate, other)
        }
        result.removeIfInRelation { candidate, other ->
            AbstractTypeChecker.equalTypes(context, candidate, other)
        }
        check(result.isNotEmpty()) { "Intersection normalization removed all types: $types" }
        return result.singleOrNull() ?: ConeIntersectionType(result)
    }

    /** 使用类型关系去重，不依赖 Cone 实例的对象身份或渲染字符串。 */
    private inline fun MutableList<ConeCangJieType>.removeIfInRelation(
        predicate: (ConeCangJieType, ConeCangJieType) -> Boolean,
    ) {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val candidate = iterator.next()
            if (candidate !is ConeErrorType && any { other ->
                    other !== candidate && other !is ConeErrorType && predicate(candidate, other)
                }
            ) iterator.remove()
        }
    }
}
