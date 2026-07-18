package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.contains

/**
 * 判断类型树中是否仍含不能用于确定性 expected-type 兼容分类的分量。
 *
 * 该判定同时供早期 candidate stage 与 precollected refinement 使用，保证二者不会
 * 对泛型、推断变量和错误恢复类型作出不同的淘汰结论。
 */
internal fun ConeCangJieType.hasUncertainExpectedTypeCompatibilityShape(): Boolean = contains { type ->
    when (type) {
        is ConeTypeParameterType,
        is ConeTypeVariableType,
        is ConeErrorType,
        is ConeStubType,
        is ConePlaceholderType,
        is ConeQuestType,
        -> true

        else -> false
    }
}
