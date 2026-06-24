package org.cangnova.cangjie.cfir.types

/**
 * 仓颉类型交叉工具。
 *
 * 当前约束系统已经移除了 captured type 参与交叉的路径，因此这里仅负责对一组已知类型做
 * 轻量规范化：展开嵌套交叉类型、移除重复项，并在只剩单个候选时直接返回该类型。
 *
 * 更激进的 subtype 剪枝应继续留在统一类型检查器/近似器中，而不是在 resolve 诊断路径里
 * 额外引入一套独立规则。
 */
object ConeTypeIntersector {
    /**
     * 对 [types] 做轻量交叉规范化并返回交叉结果。
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

        return ConeIntersectionType(normalized.toList())
    }
}
