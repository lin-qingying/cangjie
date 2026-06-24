package org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria

import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.isSameType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType

/**
 * 矩阵第一列模式列表。
 */
val CfirMatrix.firstColumn: List<CfirMatchPattern>
    get() = mapNotNull { row -> row.firstOrNull() }

/**
 * 矩阵第一列的统一类型。
 *
 * 第一列存在多个不同类型时抛出 [MarangetException]。
 */
val CfirMatrix.firstColumnType: ConeCangJieType?
    get() {
        val firstTypes = firstColumn.map { it.type }.takeIf { it.isNotEmpty() } ?: return null
        return firstTypes.customDistinct(::isSameType).singleOrNull()
            ?: throw MarangetException("matrix first-column types are inconsistent")
    }

/**
 * 校验矩阵类型一致性。
 *
 * enum 模式必须匹配对应 enum 类型，且矩阵中所有模式类型必须一致。
 */
fun CfirMatrix.isWellTyped(): Boolean {
    val enumPatternsValid = flatten().all { pattern ->
        when (val kind = pattern.kind) {
            is CfirMatchPatternKind.Enum -> {
                val enumType = pattern.type as? ConeEnumType
                enumType != null && enumType.classId == kind.enumClassId
            }

            else -> true
        }
    }
    if (!enumPatternsValid) return false

    val types = flatten().map { it.type }
    return types.isEmpty() || types.customDistinct(::isSameType).size == 1
}

/**
 * 使用自定义比较器去重列表。
 */
fun <T> List<T>.customDistinct(comparator: (T, T) -> Boolean): List<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        if (result.none { comparator(it, item) }) result += item
    }
    return result
}
