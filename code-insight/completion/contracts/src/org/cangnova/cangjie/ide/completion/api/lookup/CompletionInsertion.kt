package org.cangnova.cangjie.ide.completion.api.lookup

/**
 * 插入后 caret 的落点策略。
 *
 * 平台用负偏移表达「相对插入文本末尾」，正偏移表达「相对插入起点」。
 * 这里用显式枚举而非裸数字，避免各调用点各自猜测偏移符号。
 */
enum class CaretPolicy {
    /** caret 停在插入文本之后（默认，用于声明名/关键字/包名等无括号候选）。 */
    AFTER_INSERTED_TEXT,

    /** caret 落在新写入的左括号之后。 */
    INSIDE_NEW_PARENTHESIS,

    /**
     * caret 停在**已存在**的右括号之前。
     *
     * 用于「候选后面已经有实参和闭合括号」的情况：直接插到末尾会把用户写好的实参挤到括号外。
     */
    BEFORE_EXISTING_CLOSING_PARENTHESIS,

    /** 命名实参候选：caret 落在冒号后的第一个实参位置。 */
    AFTER_NAMED_ARGUMENT_COLON,
}

/**
 * 候选的插入数据。
 *
 * 这里定的是**意图**：这个候选要不要跟随一对调用括号、caret 该落在哪一侧。
 * 括号的**实际形态**由写入时的文档决定——用户可能已经手写了左括号甚至填好了实参，
 * 分析期的文本只代表当时的状态。两件事分开，才不会出现「补出 `foo((`」或
 * 「把用户写好的实参挤到括号外」。
 *
 * 除文档现状之外，这些结论在分析期就已冻结：插入阶段处在写事务里，
 * 不能查询 Analysis（可能触发跨线程违规），也不该临时决定语义相关的展示数据。
 */
interface CompletionInsertion {
    /** 插入后是否需要跟随左括号。 */
    val insertParenthesis: Boolean

    /**
     * 命名实参候选在参数名前插入的前缀文本。
     *
     * 仓颉的命名实参写作 `name: `；非命名实参候选为空串。
     * 该字段**不得**被填成 Kotlin 的 `name = ` 形式。
     */
    val namedArgumentPrefix: String

    /** caret 落点策略。 */
    val caretPolicy: CaretPolicy

    /**
     * 分析完成时的文档版本戳。
     *
     * 插入前必须复验：文档在此期间被改动（键入、格式化、协同编辑）时，
     * 基于旧快照算出的替换区间与括号判断都可能指向错误位置，此时必须拒绝写入而不是硬插。
     *
     * 取触发补全时**原始文件**的文档修改戳；分析副本上的改动不代表用户文档已变。
     */
    val documentStamp: Long
}

/**
 * 插入数据的标准实现。
 */
data class CompletionInsertionData(
    override val insertParenthesis: Boolean = false,
    override val namedArgumentPrefix: String = "",
    override val caretPolicy: CaretPolicy = CaretPolicy.AFTER_INSERTED_TEXT,
    override val documentStamp: Long = 0L,
) : CompletionInsertion