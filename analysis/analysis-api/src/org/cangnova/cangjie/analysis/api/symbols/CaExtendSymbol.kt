package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.ClassId

/**
 * `extend` 声明的公开语义视图。
 *
 * 在仓颉中 `extend` 是独立于 class 体的成员容器，能为既有类型补充成员并声明其遵循的协议。
 * 它与 [CaClassSymbol] 平行存在：扩展成员的 [CaSymbolLocation] 是 [CaSymbolLocation.EXTEND]。
 *
 * 与 Kotlin 的 extension function/property 不同 —— 仓颉的 `extend` 是 _显式声明_ 的一阶实体，
 * 因此值得拥有独立的符号类型而不是把扩展挂在某个 callable 上。
 */
interface CaExtendSymbol : CaDeclarationSymbol, CaDeclarationContainerSymbol, CaTypeParameterOwnerSymbol {
    /**
     * 当前 `extend` 在编译期使用的稳定唯一标识。
     *
     * 用于在跨 Session、跨模块场景下定位同一份扩展声明。
     */
    val extendId: String

    /**
     * 被扩展类型的稳定 [ClassId]。
     *
     * 当被扩展对象是匿名类型或无法表达的类型构造时可能为 `null`。
     */
    val targetClassId: ClassId?

    /**
     * 被扩展类型的实例化形式。
     *
     * 这与 [targetClassId] 互补：前者给出"具体类型实参填好"的视图，后者只给静态身份。
     */
    val extendedType: CaType

    /**
     * 当前扩展显式声明遵循的协议/超类型列表。
     */
    val superTypes: List<CaType>
}
