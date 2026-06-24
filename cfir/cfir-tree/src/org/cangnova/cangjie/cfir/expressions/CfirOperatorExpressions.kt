package org.cangnova.cangjie.cfir.expressions

/**
 * 短路/组合类二元操作符分类。
 *
 * @property symbol 语言源码中的操作符文本。
 */
enum class CfirBinaryOpKind(val symbol: String) {
    /**
     * 逻辑与 `&&`。
     */
    AND("&&"),

    /**
     * 逻辑或 `||`。
     */
    OR("||"),

    /**
     * 空合并 `??`。
     */
    COALESCING("??"),

    /**
     * 管道 `|>`。
     */
    PIPELINE("|>"),

    /**
     * 函数组合 `~>`。
     */
    COMPOSITION("~>"),
}

/**
 * 比较操作符分类。
 *
 * @property symbol 语言源码中的比较操作符文本。
 */
enum class CfirComparisonOp(val symbol: String) {
    /**
     * 小于 `<`。
     */
    LT("<"),

    /**
     * 大于 `>`。
     */
    GT(">"),

    /**
     * 小于等于 `<=`。
     */
    LE("<="),

    /**
     * 大于等于 `>=`。
     */
    GE(">="),

    /**
     * 等于 `==`。
     */
    EQ("=="),

    /**
     * 不等于 `!=`。
     */
    NE("!="),
}
