package org.cangnova.cangjie.analysis.api.evaluation

/**
 * 标量常量的种类。
 *
 * 对应 [CaScalarCompileTimeValue] 中可能出现的仓颉内置基础类型族,
 * 调用方据此选择渲染器、字面量回构造或类型推断回退路径。
 */
enum class CaScalarValueKind {
    /** 布尔常量(`true` / `false`)。 */
    BOOLEAN,

    /** 整数常量,涵盖 Int8/16/32/64、UInt 各宽度以及 IntNative。 */
    INTEGER,

    /** 浮点常量,涵盖 Float16/Float32/Float64。 */
    FLOAT,

    /** Rune(Unicode 码点)常量。 */
    RUNE,

    /** 字符串常量。 */
    STRING,

    /** `Unit` 单值常量。 */
    UNIT,

    /** 无法判定具体种类(求值失败或类型缺失)。 */
    UNKNOWN,
}
