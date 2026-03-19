package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.inria

import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatrix
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.isSameType
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeEnumType

val CfirMatrix.firstColumn: List<CfirMatchPattern>
    get() = mapNotNull { row -> row.firstOrNull() }

val CfirMatrix.firstColumnType: ConeCangjieType?
    get() {
        val firstTypes = firstColumn.map { it.type }.takeIf { it.isNotEmpty() } ?: return null
        return firstTypes.customDistinct(::isSameType).singleOrNull()
            ?: throw MarangetException("matrix first-column types are inconsistent")
    }

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

fun <T> List<T>.customDistinct(comparator: (T, T) -> Boolean): List<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        if (result.none { comparator(it, item) }) result += item
    }
    return result
}

