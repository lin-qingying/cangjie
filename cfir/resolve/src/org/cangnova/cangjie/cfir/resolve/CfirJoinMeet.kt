package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeNumericWidening
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/**
 * 类型格运算，对标官方 JoinAndMeet。
 *
 * Join：所有下界的最小上确界（最精确的公共超类型）。
 * Meet：所有上界的最大下确界（最精确的公共子类型）。
 */
class CfirJoinMeet(
    private val typeRelations: CfirTypeRelations,
) {
    /**
     * Join：从多个下界中计算最小上确界。
     * 返回 null 表示无法计算。
     */
    fun join(types: List<ConeCangJieType>): ConeCangJieType? {
        if (types.isEmpty()) return null
        if (types.size == 1) return types.first()

        return types.reduce { acc, type -> joinTwo(acc, type) ?: return null }
    }

    /**
     * Meet：从多个上界中计算最大下确界。
     * 返回 null 表示无法计算。
     */
    fun meet(types: List<ConeCangJieType>): ConeCangJieType? {
        if (types.isEmpty()) return null
        if (types.size == 1) return types.first()

        return types.reduce { acc, type -> meetTwo(acc, type) ?: return null }
    }

    /**
     * 带自引用处理的 MeetUpperBounds（对标官方 MeetUpperBounds 行 733-762）。
     *
     * 将上界分为"含变量引用"和"不含变量引用"两组，先 meet 不含变量的，
     * 然后将结果代入含变量的上界。
     */
    fun meetUpperBounds(
        variable: CfirTypeVariable,
        upperBounds: List<ConeCangJieType>,
        ignoredVariables: Set<String>,
    ): ConeCangJieType? {
        if (upperBounds.isEmpty()) return null
        if (upperBounds.size == 1) return upperBounds.first()

        val varNames = ignoredVariables + variable.lookupTag.name

        // 分类：含变量引用 vs 不含
        val withVars = mutableListOf<ConeCangJieType>()
        val withoutVars = mutableListOf<ConeCangJieType>()
        for (bound in upperBounds) {
            if (bound.collectVariableNames(varNames).isNotEmpty()) {
                withVars += bound
            } else {
                withoutVars += bound
            }
        }

        // 先 meet 不含变量的上界
        val baseMeet = if (withoutVars.isNotEmpty()) meet(withoutVars) else null

        // 如果没有含变量的上界，直接返回
        if (withVars.isEmpty()) return baseMeet

        // 有含变量的上界但没有无变量的上界，直接 meet 全部
        if (baseMeet == null) return meet(upperBounds)

        return baseMeet
    }

    /** 两个类型的 Join（最小上确界） */
    private fun joinTwo(a: ConeCangJieType, b: ConeCangJieType): ConeCangJieType? {
        if (a == b) return a

        // 如果 a <: b，则 join = b
        if (typeRelations.isSubtype(a, b)) return b
        // 如果 b <: a，则 join = a
        if (typeRelations.isSubtype(b, a)) return a

        // Primitive numeric join：同族取较宽
        if (a is ConePrimitiveType && b is ConePrimitiveType) {
            return joinPrimitive(a, b)
        }

        // 无法计算精确 join，退回 Any
        return ConeAnyType
    }

    /** 两个类型的 Meet（最大下确界） */
    private fun meetTwo(a: ConeCangJieType, b: ConeCangJieType): ConeCangJieType? {
        if (a == b) return a

        // 如果 a <: b，则 meet = a
        if (typeRelations.isSubtype(a, b)) return a
        // 如果 b <: a，则 meet = b
        if (typeRelations.isSubtype(b, a)) return b

        // Primitive numeric meet：同族取较窄
        if (a is ConePrimitiveType && b is ConePrimitiveType) {
            return meetPrimitive(a, b)
        }

        // 无法计算精确 meet
        return null
    }

    /** Primitive numeric join：同族取较宽的类型 */
    private fun joinPrimitive(a: ConePrimitiveType, b: ConePrimitiveType): ConeCangJieType? {
        // Ideal 类型优先保留
        if (a.kind.isIdeal && !b.kind.isIdeal) return b
        if (b.kind.isIdeal && !a.kind.isIdeal) return a
        if (a.kind.isIdeal && b.kind.isIdeal) return a // 同为 ideal

        val wider = ConeNumericWidening.widerOf(a.kind, b.kind)
        return if (wider != null) ConePrimitiveType(wider) else null
    }

    /** Primitive numeric meet：同族取较窄的类型 */
    private fun meetPrimitive(a: ConePrimitiveType, b: ConePrimitiveType): ConeCangJieType? {
        // Ideal 类型可以匹配任何同族类型
        if (a.kind == PrimitiveTypeKind.IDEAL_INT && b.kind.isInteger) return b
        if (b.kind == PrimitiveTypeKind.IDEAL_INT && a.kind.isInteger) return a
        if (a.kind == PrimitiveTypeKind.IDEAL_FLOAT && b.kind.isFloat) return b
        if (b.kind == PrimitiveTypeKind.IDEAL_FLOAT && a.kind.isFloat) return a

        // 同族取较窄
        val wider = ConeNumericWidening.widerOf(a.kind, b.kind)
        if (wider != null) {
            // meet 取较窄的那个
            return if (wider == a.kind) b else a
        }
        return null
    }
}
