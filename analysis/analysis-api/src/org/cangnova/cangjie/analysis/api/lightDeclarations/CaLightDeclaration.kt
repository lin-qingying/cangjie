package org.cangnova.cangjie.analysis.api.lightDeclarations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * Light declaration 视图的统一基础接口。
 *
 * "Light declaration" 是介于符号(symbol) 与 PSI 之间的轻量声明视图:
 * - 不需要触发完整的 CFIR 解析就能获取声明的对外形态;
 * - 既能描述源码声明,也能描述反编译/合成产物;
 * - 主要服务于 IDE 树形结构、引用扫描、analysis-tools 的一致性比对等场景。
 *
 * 具体角色由 [kind] 区分,详见 [CaLightDeclarationKind]。
 */
interface CaLightDeclaration : CaLifetimeOwner {
    /**
     * 声明角色(类、扩展或可调用)。
     */
    val kind: CaLightDeclarationKind

    /**
     * 声明名;对匿名 / 局部声明可能为 `null`。
     */
    val name: String?

    /**
     * 该声明所属模块,无法确定时为 `null`。
     */
    val module: CaModule?

    /**
     * 直接声明在该声明上的注解。
     */
    val annotations: List<CaAnnotation>

    /**
     * 声明的来源信息,例如对应的源 PSI、反编译节点或合成痕迹。
     */
    val origin: CaLightDeclarationOrigin
}
