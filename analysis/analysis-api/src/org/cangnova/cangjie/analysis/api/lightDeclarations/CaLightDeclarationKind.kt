package org.cangnova.cangjie.analysis.api.lightDeclarations

/**
 * Light declaration 的角色分类。
 *
 * 用于在不展开完整 PSI 的前提下识别声明形态,
 * 是 [CaLightDeclaration] 与具体子接口之间的桥梁。
 */
enum class CaLightDeclarationKind {
    /**
     * 类型样声明,对应 [CaLightClassLikeDeclaration]。
     */
    CLASS_LIKE,

    /**
     * 扩展声明,对应 [CaLightExtendDeclaration]。
     */
    EXTEND,

    /**
     * 可调用声明(函数 / 属性 / 构造器),对应 [CaLightCallableDeclaration]。
     */
    CALLABLE,
}
