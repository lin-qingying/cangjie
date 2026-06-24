package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.inria

import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.isSameType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType

/** 返回模式矩阵首列中存在的模式，用于 Maranget 行特化。 */
val CfirMatrix.firstColumn: List<CfirMatchPattern>
    get() = mapNotNull { row -> row.firstOrNull() }

/** 推断模式矩阵首列的唯一类型；类型不一致时视为矩阵不满足算法不变量。 */
val CfirMatrix.firstColumnType: ConeCangJieType?
    get() {
        val firstTypes = firstColumn.map { it.type }.takeIf { it.isNotEmpty() } ?: return null
        return firstTypes.customDistinct(::isSameType).singleOrNull()
            ?: throw MarangetException("matrix first-column types are inconsistent")
    }

/** 校验矩阵内 enum 构造器归属和模式类型一致性是否满足 Maranget 算法前提。 */
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

/** 使用调用方提供的等价关系执行稳定去重，保留第一次出现的元素。 */
fun <T> List<T>.customDistinct(comparator: (T, T) -> Boolean): List<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        if (result.none { comparator(it, item) }) result += item
    }
    return result
}
