package org.cangnova.cangjie.analysis.api.lightDeclarations

import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 扩展声明(`extend`)的 light declaration 视图。
 *
 * 仓颉允许通过 `extend` 为既有类型新增成员或实现接口,这类声明在引用
 * 扫描、IDE 大纲、analysis-tools 一致性比对中需要稳定的对外形态,
 * 因此独立建模为 [CaLightExtendDeclaration]。
 */
interface CaLightExtendDeclaration : CaLightDeclaration {
    /**
     * 扩展声明的稳定标识(字符串形式),用于唯一指代同名扩展场景。
     */
    val extendId: String

    /**
     * 被扩展类型的 [ClassId];对扩展元组等不可寻址类型可能为 `null`。
     */
    val targetClassId: ClassId?

    /**
     * 被扩展的目标类型(可能携带类型参数等更精细信息)。
     */
    val extendedType: CaType

    /**
     * 扩展上声明的类型形参名列表。
     */
    val typeParameters: List<Name>

    /**
     * 扩展声明追加的接口列表。
     */
    val superTypes: List<CaType>

    /**
     * 扩展中新增的成员声明视图。
     */
    val members: List<CaLightDeclaration>
}
