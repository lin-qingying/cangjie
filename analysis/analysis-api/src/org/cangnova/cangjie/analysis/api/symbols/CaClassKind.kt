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
}
