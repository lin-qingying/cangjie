package org.cangnova.cangjie.ide.completion.impl.shared

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.openapi.progress.ProcessCanceledException
import org.cangnova.cangjie.ide.completion.api.CompletionKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.CangJieCompletionService
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionRecoveryService
import org.cangnova.cangjie.ide.completion.impl.shared.dummy.CompletionDummyContext
import org.cangnova.cangjie.ide.completion.impl.shared.dummy.CompletionDummyIdentifierProvider
import org.cangnova.cangjie.ide.completion.impl.shared.insert.revalidateSelection
import org.cangnova.cangjie.ide.completion.impl.shared.lookup.LookupElementFactory
import org.cangnova.cangjie.psi.CjElement

/**
 * 仓颉原生补全的平台入口。
 *
 * 对齐 Kotlin `KotlinFirCompletionContributor` 的形状：继承平台 `CompletionContributor`，
 * 只负责把平台参数翻译成仓颉补全请求、把快照翻译成平台 lookup 元素，
 * 真正的语义计算由 [CangJieCompletionService] 完成。
 *
 * 平台细节与仓颉语义的边界：
 * - BASIC 与 SMART 共用同一次服务调用，`kind` 只影响期望类型处理；
 * - 重跑（re-parse）由平台通过新的 [CompletionParameters] 表达，本入口不缓存请求；
 * - 链式补全由独立入口触发，不在本类内实现。
 */
abstract class AbstractCangJieCompletionContributor : CompletionContributor() {
    /**
     * 取得当前生效的仓颉补全服务。
     *
     * 返回 `null` 表示语义补全未装配；此时本入口不产出候选，
     * 以便宿主明确暴露「未接入」而不是用词补全冒充。
     */
    protected abstract fun completionService(parameters: CompletionParameters): CangJieCompletionService?

    /**
     * 把 dummy 修正后的上下文翻译为仓颉补全请求。
     *
     * 位置与前缀都取自修正后的副本：平台原始位置仍带 `IntellijIdeaRulezzz`。
     */
    protected open fun buildRequest(
        parameters: CompletionParameters,
        dummy: CompletionDummyContext,
    ): CompletionRequest = completionRequestFrom(parameters, dummy)

    /**
     * 平台补全入口。
     *
     * 异常不在此吞掉：平台会把失败呈现为补全错误，
     * 静默返回空结果会让真实缺陷看起来像「没有候选」。
     */
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        // dummy 修正必须先于服务调用：仍在裸 dummy 上分析会得到空候选且不报错。
        val dummy = CompletionDummyIdentifierProvider.provide(parameters) ?: return
        val service = completionService(parameters) ?: return
        val request = buildRequest(parameters, dummy)
        result.addAllElements(service.complete(request).map(::toLookupElement))
    }

    /**
     * 补全类型到仓颉补全种类的映射。
     *
     * 平台只用 BASIC/SMART 区分手动与自动弹窗；仓颉侧沿用同一语义。
     */
    protected fun completionTypeOf(type: CompletionType): CompletionKind = completionKindOf(type)

    /**
     * 把稳定快照转换为平台 lookup 元素。
     *
     * 统一走 [LookupElementFactory]：图标、尾文本、插入处理器、平台优先级与
     * 快照回挂都在那里完成；这里只搬运快照中的不可变数据，
     * 不允许在 lookup 元素上捕获符号、类型或分析会话。
     */
    protected open fun toLookupElement(snapshot: CompletionSnapshot): LookupElement =
        LookupElementFactory.create(snapshot)

    /**
     * 候选语义复查服务；未装配时为 `null`。
     *
     * 未装配表示本宿主不做语义复查——插入仍受文档版本与区间校验约束，
     * 只是不会因为「候选指向的声明已经不在了」而拒绝写入。
     */
    protected open val recoveryService: CompletionRecoveryService? get() = null

    /**
     * 复查一个候选的语义目标，把结论回挂到 lookup 元素上。
     *
     * **谁调用**：宿主的 Lookup 监听器（候选被选中时）。之所以不在本模块安装监听器，
     * 是因为本模块的 `compileOnly` 只有 `intellij-core`、`analysis` 与 `indexing`，
     * 不含平台的 `LookupManager`；按 B1 的分工，Lookup 接线属于宿主。
     *
     * **为什么不在插入时查**：插入发生在写事务里，那里不能跑分析，
     * 而「这个候选现在还能不能插」只有分析能回答。
     *
     * **复查失败降级为「未复查」而不是「失效」**：线程模型不允许在 EDT 上分析、
     * 会话已过期、用户取消——这些环境限制不等于候选过期，判成失效会让用户
     * 在正常环境里选什么都插不进去。只有复查**确实回答**目标不存在时才拒绝写入。
     */
    fun revalidateSelection(item: LookupElement, useSite: CjElement) {
        val recovery = recoveryService ?: return
        item.revalidateSelection(useSite, recovery)
    }
}
