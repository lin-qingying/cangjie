package org.cangnova.cangjie.cfir.expressions

/**
 * 字面量表达式的种类。
 */
enum class CfirLiteralKind {
    /**
     * 整数字面量。
     */
    INT,

    /**
     * 浮点数字面量。
     */
    FLOAT,

    /**
     * 布尔字面量。
     */
    BOOLEAN,

    /**
     * Rune 字面量。
     */
    RUNE,

    /**
     * 字符串字面量。
     */
    STRING,

    /**
     * Unit 字面量。
     */
    UNIT,
}
