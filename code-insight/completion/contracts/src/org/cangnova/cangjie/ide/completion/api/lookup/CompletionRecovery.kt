package org.cangnova.cangjie.ide.completion.api.lookup

import org.cangnova.cangjie.psi.CjElement

/**
 * 插入前校验结论。
 *
 * 校验的必要性来自补全的生命周期跨度：快照在分析期冻结，插入发生在之后，
 * 其间文档可能已被键入、格式化或协同编辑修改。继续用旧区间写入会破坏用户的编辑。
 */
sealed interface InsertionValidity {
    /** 快照仍然有效，可以写入。 */
    data object Valid : InsertionValidity

    /** 文档版本已变：快照的括号/实参判断不再可信。 */
    data class StaleDocument(val expectedStamp: Long, val actualStamp: Long) : InsertionValidity

    /** 替换区间越界或已不落在预期的位置元素上。 */
    data class OutOfRange(val startOffset: Int, val endOffset: Int, val documentLength: Int) : InsertionValidity

    /** 候选身份已失效：重新解析后目标不再存在或不再可见。 */
    data class DanglingCandidate(val identity: CompletionIdentity) : InsertionValidity

    /** 是否允许写入文档。 */
    val writable: Boolean
        get() = this is Valid
}

/**
 * 语义复查后的候选目标。
 *
 * 只保留**渲染后的字符串与导入计划**：复查发生在一次新的分析会话内，
 * 会话结束即可丢弃其中的符号/类型。若把裸符号塞进这里，就等于把分析生命周期
 * 泄漏到界面期，与 B5 的硬约束冲突。
 */
data class RecoveredTarget(
    val identity: CompletionIdentity,
    /** 复查时得到的完整限定名；与快照不一致说明身份已漂移。 */
    val qualifiedName: String,
    /** 复查时确认的导入计划；可能与分析期结论不同（文档已被用户改动）。 */
    val importPlan: CompletionImportPlan,
    /** 复查时目标是否仍在作用域内可见。 */
    val visible: Boolean,
)

/**
 * 候选语义复查服务。
 *
 * 补全弹窗展示与选择发生在分析会话**之后**：要判断「这个候选现在还能不能插」，
 * 必须重新进入分析恢复指针，而不是继续持有分析期算出的结论。
 */
interface CompletionRecoveryService {
    /**
     * 凭 [identity] 在 [useSite] 处重新进入分析并恢复候选目标。
     *
     * [useSite] 必须是**当初触发补全的那个文件**：可达性是相对使用点而言的，
     * 同一个声明在文件 A 里可直接引用、在文件 B 里可能需要 import。
     *
     * 返回 `null` 表示身份已失效（声明被删、作用域失效、索引失效）。
     * 实现**不得**吞掉分析异常后返回 `null`：失败与失效必须可区分，
     * 否则一次真实的分析崩溃会被伪装成「候选过期」。
     */
    fun recover(useSite: CjElement, identity: CompletionIdentity): RecoveredTarget?
}