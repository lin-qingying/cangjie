package org.cangnova.cangjie.analysis.api.symbols

/**
 * 符号在源码结构中的声明位置。
 *
 * 与 [CaSymbolOrigin] 的"来源"职责正交：location 描述符号 _写在哪个语法容器里_，
 * 不关心它是源码、库还是合成的。
 *
 * 相比 Kotlin 的位置模型，仓颉额外引入 [EXTEND] 来显式区分 `extend` 成员和普通类成员，
 * 因为这两者在仓颉语义中是分别建模的容器。
 */
enum class CaSymbolLocation {
    /**
     * 顶层位置。
     *
     * 用于不属于任何其他符号的声明：file 符号、package 符号，以及包/文件下的顶层声明。
     */
    TOP_LEVEL,

    /**
     * 隶属于 class / interface / struct / enum 等 class-like 声明体的成员位置。
     */
    CLASS,

    /**
     * 隶属于 `extend` 声明体内的成员位置。
     *
     * 仓颉的扩展成员与类体内的成员在公共 API 上必须可区分，
     * 因此独立于 [CLASS] 单独列出。
     */
    EXTEND,

    /**
     * 隶属于 property 内部的位置。
     *
     * 例如 property 的 getter/setter 这类与 property 强绑定的子声明。
     */
    PROPERTY,

    /**
     * 函数体或其他执行体内部的局部位置。
     *
     * 注意：局部类的成员位置仍然是 [CLASS]，[LOCAL] 不会向其成员传递。
     */
    LOCAL,
}

/**
 * 当前符号是否位于顶层。
 *
 * @see CaSymbolLocation.TOP_LEVEL
 */
val CaSymbol.isTopLevel: Boolean
    get() = location == CaSymbolLocation.TOP_LEVEL

/**
 * 当前符号是否定义于函数体等执行体内部。
 *
 * @see CaSymbolLocation.LOCAL
 */
val CaSymbol.isLocal: Boolean
    get() = location == CaSymbolLocation.LOCAL
