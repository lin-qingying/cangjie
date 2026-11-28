package org.cangnova.cangjie.analysis.api.symbols

/**
 * class-like 声明的种类。
 *
 * 用于在 [CaClassSymbol] 上区分仓颉中四类真实类型声明的形态。
 * 类型别名（typealias）不在此枚举内，它由 [CaTypeAliasSymbol] 单独承载。
 */
enum class CaClassKind {
    /**
     * `class` 声明：可以被继承（按其模态决定开放程度）。
     */
    CLASS,

    /**
     * `interface` 声明：只能被实现 / 继承，不直接实例化。
     */
    INTERFACE,

    /**
     * `struct` 声明：仓颉的值类型聚合声明。
     */
    STRUCT,

    /**
     * `enum` 声明：以枚举构造器列举所有实例的代数数据类型。
     */
    ENUM,

    /**
     * primitive 内建类型（`Int64`、`UInt8` 等）的合成声明形态。
     *
     * primitive 没有源码声明；它在符号层以合成 class-like 参与名称解析、
     * qualifier 成员查找与导航，公开 API 需要能与其余种类一起被区分。
     */
    PRIMITIVE,
}
