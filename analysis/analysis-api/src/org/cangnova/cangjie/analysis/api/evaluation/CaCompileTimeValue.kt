package org.cangnova.cangjie.analysis.api.evaluation

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

/**
 * Analysis API 对外公开的编译期值模型。
 *
 * 这里不把编译期值退化成单个字符串，而是显式区分：
 * - 标量字面量
 * - 元组字面量
 * - 集合字面量
 *
 * 这样 IDE、LSP、渲染层和后续重构工具都可以在同一套结构化语义之上工作，
 * 而不是反复解析一段已经渲染过的文本。
 */
sealed interface CaCompileTimeValue : CaLifetimeOwner {
    /**
     * 当前编译期值的稳定文本表示。
     */
    val renderedText: String
}

/**
 * 标量编译期值。
 */
interface CaScalarCompileTimeValue : CaCompileTimeValue {
    val kind: CaScalarValueKind
}

/**
 * 元组编译期值。
 */
interface CaTupleCompileTimeValue : CaCompileTimeValue {
    val elements: List<CaCompileTimeValue>
}

/**
 * 集合编译期值。
 */
interface CaCollectionCompileTimeValue : CaCompileTimeValue {
    val elements: List<CaCompileTimeValue>
}

/**
 * 标量字面量种类。
 */
enum class CaScalarValueKind {
    BOOLEAN,
    INTEGER,
    FLOAT,
    RUNE,
    STRING,
    UNIT,
    UNKNOWN,
}
