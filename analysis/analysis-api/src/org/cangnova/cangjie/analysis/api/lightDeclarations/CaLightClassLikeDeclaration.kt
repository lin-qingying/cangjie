package org.cangnova.cangjie.analysis.api.lightDeclarations

import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 类型声明(class / interface / struct / enum 等)的 light declaration 视图。
 *
 * 用于:
 * - 在 IDE 树视图、结构视图中展示声明骨架;
 * - 在 analysis-tools 中做一致性检查时承载稳定的"声明面貌"。
 *
 * 不持有完整 body,只保留必要的对外形态(ID、超类型、成员列表等)。
 */
interface CaLightClassLikeDeclaration : CaLightDeclaration {
    /**
     * 类型的稳定 [ClassId];对不可寻址的合成声明可能为 `null`。
     */
    val classId: ClassId?

    /**
     * 形参名列表(仅名字,不含约束)。
     */
    val typeParameters: List<Name>

    /**
     * 父类型/父接口集合。
     */
    val superTypes: List<CaType>

    /**
     * 类型成员的 light declaration 列表(嵌套类型、函数、属性等)。
     */
    val members: List<CaLightDeclaration>
}
