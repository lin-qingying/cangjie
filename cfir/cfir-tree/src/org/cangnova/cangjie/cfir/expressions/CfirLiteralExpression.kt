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
     * 字节字面量（`b'x'` 语法）。
     *
     * 对齐官方 `LitConstKind::RUNE_BYTE`：`GetNumLitTypeKind` 固定返回
     * `TYPE_UINT8`，因此该字面量是定型 `UInt8` 值，不参与 ideal int 收敛。
     */
    BYTE,

    /**
     * 字符串字面量。
     */
    STRING,

    /** Java 互操作字符串字面量；官方 LitConstKind::JSTRING 与 STRING 分开参与 CJMP 比较。 */
    JSTRING,

    /**
     * Unit 字面量。
     */
    UNIT,
}
