package org.cangnova.cangjie.analysis.api.symbols

/**
 * 符号的可见性。
 *
 * 覆盖仓颉源码中可显式书写的可见性修饰，以及无修饰时的语义兜底值（[UNKNOWN]、[LOCAL]）。
 *
 * 对齐 Kotlin Analysis API 的 `KaSymbolVisibility`，
 * 但收敛为仓颉语言实际使用的可见性集合（去掉 Java 体系特有的 package-protected / package-private）。
 */
enum class CaSymbolVisibility {
    /**
     * 仅在所在声明（或顶层时仅在所在文件）内部可见。
     */
    PRIVATE,

    /**
     * `private(this)` 等"绑定到接收者"的更严格可见性。
     *
     * 仅在当前实例自身内部可见，不能通过其他同类型实例访问。
     */
    PRIVATE_TO_THIS,

    /**
     * `protected` 可见性：在所在声明体内、以及其子类型中可见。
     */
    PROTECTED,

    /**
     * `internal` 可见性：在同一模块/包范围内可见。
     */
    INTERNAL,

    /**
     * `public` 可见性：到处可见。
     */
    PUBLIC,

    /**
     * 局部声明的可见性。
     *
     * 用于函数体等执行体内部的声明，没有显式可见性修饰。
     */
    LOCAL,

    /**
     * 可见性未知。
     *
     * 通常用于无法准确推断时的兜底，例如错误代码场景。
     */
    UNKNOWN,
}
