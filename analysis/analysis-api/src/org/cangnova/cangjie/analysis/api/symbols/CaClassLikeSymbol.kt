package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * 具备稳定 [ClassId] 身份、可作为类型来源的 class-like 符号公共抽象。
 *
 * 它统一覆盖以下两种实体：
 * - 真实类型声明（[CaClassSymbol]：class / interface / struct / enum）；
 * - 类型别名（[CaTypeAliasSymbol]）。
 *
 * 两者共享 ClassId、命名能力与类型参数能力，但在 IS-A 语义和实例化语义上有本质区别，
 * 因此通过下游接口进一步分化。
 */
interface CaClassLikeSymbol : CaClassifierSymbol, CaNamedSymbol, CaTypeParameterOwnerSymbol {
    /**
     * 稳定的类型身份。
     *
     * 对匿名或局部声明可能为 `null`。
     */
    val classId: ClassId?
}
