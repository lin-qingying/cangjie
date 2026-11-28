package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.utils.exceptions.CangJieRuntimeExceptionWithAttachments

/**
 * 符号在本次分析会话中不可用。
 *
 * 符号恢复是**部分函数**：声明可能属于当前会话不可见的模块、可能尚未进入索引，
 * 也可能是本身没有全局身份的局部声明。这些都是环境状态，不是编程错误，
 * 因此该异常属于「可预期失败」通道：
 *
 * - 消费方按 [reason] 分类降级（返回 error type、跳过候选、记 debug 日志）；
 * - 禁止再对 `message` 做文本匹配来区分缺失类型；
 * - 只有真正的框架契约破坏才使用 `IllegalStateException` / `error(...)` 这类无条件失败通道。
 *
 * 继承 [CangJieRuntimeExceptionWithAttachments] 而不是 `IllegalStateException`：
 * 后者会被既有 `catch (IllegalStateException)` 误吞，且语义上把「环境缺失」错标成「程序错误」。
 *
 * 该类型位于 Analysis API 公共层，使补全、提示、检查等上层模块无需依赖 low-level 实现。
 */
open class CaSymbolUnavailableException(
    /**
     * 不可用原因分类。
     */
    val reason: CaSymbolUnavailabilityReason,
    message: String,
    cause: Throwable? = null,
) : CangJieRuntimeExceptionWithAttachments(message, cause)

/**
 * 符号不可用的原因分类。
 *
 * 每个取值都必须有真实的产出点，且语义互斥：上层据此决定日志级别与降级方式。
 * 「暂时想不到归哪类」不接受兜底值——那种情况下应当新增一个分类并显式产出。
 */
enum class CaSymbolUnavailabilityReason {
    /**
     * 声明应当存在于索引中，但取不到（索引未就绪、dumb 模式，或声明并不属于该模块）。
     *
     * 这是**可预期缺失**：它的正常恢复路径是「等索引就绪后重算」，因此不得缓存成永久结论。
     */
    MISSING_DECLARATION,

    /**
     * 声明类型尚未被符号恢复链覆盖（新增了库声明形态但没加处理分支）。
     *
     * 这是**代码缺陷**而不是环境状态，必须保持 warn 可见：静默降级会让新形态的声明
     * 悄无声息地失去符号，直到有人发现补全/导航缺东西。
     */
    UNHANDLED_DECLARATION_KIND,

    /**
     * 反编译/stub PSI 已失效（重建后会重新 physical），需要重建后才能恢复符号。
     */
    CORRUPTED_COMPILED_PSI,
    ;

    /**
     * 该原因是否属于「可预期的缺失」，即不应作为错误上报。
     */
    val isExpectedAbsence: Boolean
        get() = this == MISSING_DECLARATION

    /**
     * 该缺失是否**可能自愈**（等索引就绪或 PSI 重建后重算会成功）。
     *
     * 缓存层据此决定是否允许把否定结果写进长生命周期缓存：
     * 可自愈的缺失一旦被持久化，就会退化成「同一个会话永久查不到」。
     */
    val mayRecoverLater: Boolean
        get() = this == MISSING_DECLARATION || this == CORRUPTED_COMPILED_PSI
}
