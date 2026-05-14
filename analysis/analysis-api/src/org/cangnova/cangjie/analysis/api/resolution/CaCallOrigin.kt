package org.cangnova.cangjie.analysis.api.resolution

/**
 * 调用的语义来源分类。
 *
 * 用于区分调用是普通用户代码、操作符约定还是构造器委托的一部分,
 * 影响诊断渲染、引用扫描以及部分快速修复的行为。
 */
enum class CaCallOrigin {
    /**
     * 普通调用,直接由源码 `foo(x)` 或类似形式触发。
     */
    REGULAR,

    /**
     * 操作符约定调用,例如 `a + b` 实际触发的 `plus`。
     */
    OPERATOR,

    /**
     * 构造器委托:`this(...)`。
     */
    CONSTRUCTOR_DELEGATION_THIS,

    /**
     * 构造器委托:`super(...)`。
     */
    CONSTRUCTOR_DELEGATION_SUPER,
}
