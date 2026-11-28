package org.cangnova.cangjie.ide.completion.api.lookup

import org.cangnova.cangjie.ide.completion.api.CompletionItemKind

/**
 * 候选所对应声明的**具体形态**。
 *
 * 与 [CompletionItemKind] 的分工不同：
 * - [CompletionItemKind] 描述「候选在补全里的形状类别」（局部 / 成员 / 顶层 / 关键字 …），
 *   决定排序与插入形态；
 * - 本枚举描述「这是哪一种声明」，只承载图标这类展示数据。
 *
 * 两者必须分开：**顶层**这一类里同时有 class、interface、struct、enum、typealias 与函数，
 * 只按 [CompletionItemKind] 取图标会让它们共用一个 class 图标，用户无法在弹窗里区分。
 * 反过来把形态塞进 [CompletionItemKind] 也不对：那会连带改动排序与 LSP 侧的类别映射。
 */
enum class CompletionSymbolKind {
    /** `class` 声明。 */
    CLASS,

    /** `interface` 声明。 */
    INTERFACE,

    /** `struct` 声明（仓颉的值类型聚合）。 */
    STRUCT,

    /** `enum` 声明。 */
    ENUM,

    /** `typealias` 声明。 */
    TYPE_ALIAS,

    /** 函数（含成员方法与顶层函数）。 */
    FUNCTION,

    /** 属性（`prop` 或带 `mut` 的成员变量）。 */
    PROPERTY,

    /** 成员字段。 */
    FIELD,

    /** 变量（局部 `let` / `var`）。 */
    VARIABLE,

    /** 形参。 */
    PARAMETER,

    /** 泛型类型参数。 */
    TYPE_PARAMETER,

    /** 构造器。 */
    CONSTRUCTOR,

    /** 枚举构造器。 */
    ENUM_CONSTRUCTOR,

    /** 包名。 */
    PACKAGE,

    /** 语言关键字。 */
    KEYWORD,

    /**
     * 形态未确定。
     *
     * 取它时图标回退到按 [CompletionItemKind] 的粗粒度映射——即引入本枚举之前的行为，
     * 因此漏标形态只会让图标退回旧样子，不会出现空图标。
     */
    OTHER,
    ;

    companion object {
        /**
         * 没有更精确信息时，按候选类别给出的兜底形态。
         *
         * 兜底值刻意保守：`TOP_LEVEL` 归 [OTHER] 而不是猜成 [CLASS]，
         * 因为猜错会把函数显示成类型，比不显示形态更容易误导。
         */
        fun fallbackFor(kind: CompletionItemKind): CompletionSymbolKind = when (kind) {
            CompletionItemKind.LOCAL -> VARIABLE
            CompletionItemKind.TYPE_PARAMETER -> TYPE_PARAMETER
            CompletionItemKind.MEMBER -> FUNCTION
            CompletionItemKind.STATIC_MEMBER -> FIELD
            CompletionItemKind.EXTEND_MEMBER -> FUNCTION
            CompletionItemKind.NAMED_ARGUMENT -> PARAMETER
            CompletionItemKind.ENUM_ENTRY -> ENUM_CONSTRUCTOR
            CompletionItemKind.PACKAGE -> PACKAGE
            CompletionItemKind.KEYWORD -> KEYWORD
            CompletionItemKind.TOP_LEVEL -> OTHER
            CompletionItemKind.IMPORT_MEMBER -> OTHER
            CompletionItemKind.DOCUMENTATION -> OTHER
            CompletionItemKind.DECLARATION_SUGGESTION -> OTHER
        }
    }
}
