package org.cangnova.cangjie.analysis.api.completion

/**
 * Analysis API 对补全候选的稳定语义判断结果。
 *
 * 表示候选项相对当前上下文的可达性状态，
 * IDE 据此选择直接插入、追加 import 或彻底隐藏候选。
 *
 * **四种状态不得压成一种**（设计 §6.6）：「确定不可访问」「不适用」「未知」「需要导入」
 * 的处置完全不同——未知时**不得**隐藏候选（判定不出来不等于不可用），
 * 而「不适用」虽然要移除，原因也与权限/遮蔽不同。区分写在这里，
 * 消费侧的行为规则用 [shouldHide] / [isDeterminedUsable] 表达，不各自 `when` 一遍。
 */
enum class CaCompletionCandidateStatus {
    /** 候选项当前已可访问,无需任何额外动作即可直接插入。 */
    DIRECT,

    /** 候选项可用,但需要先插入对应的 import 才能引用。 */
    REQUIRES_IMPORT,

    /** 候选项**确定**不可访问（权限、遮蔽等原因），应从补全列表中隐藏。 */
    HIDDEN,

    /**
     * 该符号**不适用**作引用目标（例如文件符号本身）。
     *
     * 从列表移除，但原因与 [HIDDEN] 不同：不是「看不见」，而是「不是一类东西」。
     */
    NOT_APPLICABLE,

    /**
     * **判定不出**可达性（既非直接可达，也拿不到导入路径）。
     *
     * 与 [HIDDEN] 的分界线是「确定不可用」与「不知道」：
     * 未知**不得**据此隐藏候选——把不确定当否定的代价是用户少了候选且毫无提示。
     */
    UNKNOWN,
    ;

    /**
     * 是否应从候选列表中移除。
     *
     * **只有确定不可用才算**：未知不算（见 [UNKNOWN] 的说明）。
     */
    val shouldHide: Boolean
        get() = this == HIDDEN || this == NOT_APPLICABLE

    /**
     * 是否**确定**可用——即可以立刻给出一个能落地的动作（直接插入，或补一条 import 后插入）。
     *
     * 需要「确定性动作」的场景（例如引用缩短必须写下一个一定能解析的短名）按本属性判，
     * 而不是按 [shouldHide] 的反面：`UNKNOWN` 两种都不是。
     */
    val isDeterminedUsable: Boolean
        get() = this == DIRECT || this == REQUIRES_IMPORT
}
