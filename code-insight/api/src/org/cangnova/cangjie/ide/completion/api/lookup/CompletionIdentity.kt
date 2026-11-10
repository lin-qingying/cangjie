package org.cangnova.cangjie.ide.completion.api.lookup

/**
 * 候选身份类别。
 *
 * 身份类别决定「分析会话结束后如何恢复语义」：符号与类型靠指针重新解析，
 * 包与关键字本身即身份，命名实参靠「所属调用 + 参数名」复验。
 */
enum class CompletionIdentityKind {
    /** 声明符号（顶层、成员、局部、参数）。 */
    SYMBOL,

    /** 类型（类、接口、结构、别名）。 */
    TYPE,

    /** 包。 */
    PACKAGE,

    /** 语言关键字。 */
    KEYWORD,

    /** 命名实参。 */
    NAMED_ARGUMENT,

    /** 枚举值或有参构造器。 */
    ENUM_ENTRY,

    /** 文档标签或引用。 */
    DOCUMENTATION,

    /** 声明建议（生成新声明而非引用已有声明）。 */
    DECLARATION_SUGGESTION,
}

/**
 * 候选的稳定身份。
 *
 * 分析会在一次会话内给出符号/类型，但补全弹窗的生命周期**长于**该会话：
 * 用户可能滚动、切换文件、修改文档后才选择候选。因此身份必须在分析期
 * 就固化为**不含裸符号/类型/会话**的字符串句柄，界面期再凭它重新进入分析恢复。
 *
 * [deduplicationKey] 与 [lookupString] 必须分开：仓颉允许同名不同参数的声明共存，
 * 只按显示名去重会丢掉重载候选。
 */
interface CompletionIdentity {
    /** 身份类别。 */
    val kind: CompletionIdentityKind

    /**
     * 重新进入分析时用于恢复的稳定句柄。
     *
     * 约定为「符号指针的渲染文本」或「包全名」，**不得**是内存地址或对象 `toString()`。
     */
    val handle: String

    /** 候选显示名。 */
    val name: String

    /** 完整限定名；包与声明恒有值，关键字与命名实参可为空串。 */
    val qualifiedName: String

    /**
     * 同一次补全内的去重键。
     *
     * 同名不同签名必须给出不同键；仅显示名相同的候选不得互相吞掉。
     */
    val deduplicationKey: String
}

/**
 * 候选身份的标准实现。
 *
 * 全部字段在构造后不变，可安全跨会话传递。
 */
data class CompletionIdentityData(
    override val kind: CompletionIdentityKind,
    override val handle: String,
    override val name: String,
    override val qualifiedName: String = "",
    override val deduplicationKey: String = handle,
) : CompletionIdentity {
    companion object {
        /**
         * 构造符号/类型身份：去重键默认取稳定句柄。
         *
         * 签名差异由调用方显式并入 [handle]，从而让同名重载天然得到不同键。
         */
        fun of(
            kind: CompletionIdentityKind,
            handle: String,
            name: String,
            qualifiedName: String = "",
        ): CompletionIdentityData = CompletionIdentityData(
            kind = kind,
            handle = handle,
            name = name,
            qualifiedName = qualifiedName,
            deduplicationKey = handle,
        )
    }
}