package org.cangnova.cangjie.ide.completion.impl.shared

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import org.cangnova.cangjie.ide.completion.api.CaCompletionKind
import org.cangnova.cangjie.ide.completion.api.CaCompletionRequest
import org.cangnova.cangjie.ide.completion.api.CaCompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.CaCangJieCompletionService
import org.cangnova.cangjie.ide.completion.impl.shared.dummy.CaCompletionDummyContext
import org.cangnova.cangjie.ide.completion.impl.shared.dummy.CaCompletionDummyIdentifierProvider
import org.cangnova.cangjie.ide.completion.impl.shared.lookup.CaLookupElementFactory

/**
 * 仓颉原生补全的平台入口。
 *
 * 对齐 Kotlin `KotlinFirCompletionContributor` 的形状：继承平台 `CompletionContributor`，
 * 只负责把平台参数翻译成仓颉补全请求、把快照翻译成平台 lookup 元素，
 * 真正的语义计算由 [CaCangJieCompletionService] 完成。
 *
 * 平台细节与仓颉语义的边界：
 * - BASIC 与 SMART 共用同一次服务调用，`kind` 只影响期望类型处理；
 * - 重跑（re-parse）由平台通过新的 [CompletionParameters] 表达，本入口不缓存请求；
 * - 链式补全由独立入口触发，不在本类内实现。
 */
abstract class CaCangJieCompletionContributor : CompletionContributor() {
    /**
     * 取得当前生效的仓颉补全服务。
     *
     * 返回 `null` 表示语义补全未装配；此时本入口不产出候选，
     * 以便宿主明确暴露「未接入」而不是用词补全冒充。
     */
    protected abstract fun completionService(parameters: CompletionParameters): CaCangJieCompletionService?

    /**
     * 把 dummy 修正后的上下文翻译为仓颉补全请求。
     *
     * 位置与前缀都取自修正后的副本：平台原始位置仍带 `IntellijIdeaRulezzz`。
     */
    protected open fun buildRequest(
        parameters: CompletionParameters,
        dummy: CaCompletionDummyContext,
    ): CaCompletionRequest = caCompletionRequestFrom(parameters, dummy)

    /**
     * 平台补全入口。
     *
     * 异常不在此吞掉：平台会把失败呈现为补全错误，
     * 静默返回空结果会让真实缺陷看起来像「没有候选」。
     */
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        // dummy 修正必须先于服务调用：仍在裸 dummy 上分析会得到空候选且不报错。
        val dummy = CaCompletionDummyIdentifierProvider.provide(parameters) ?: return
        val service = completionService(parameters) ?: return
        val request = buildRequest(parameters, dummy)
        val snapshots = service.complete(request)
        result.addAllElements(snapshots.map(::toLookupElement))
    }

    /**
     * 补全类型到仓颉补全种类的映射。
     *
     * 平台只用 BASIC/SMART 区分手动与自动弹窗；仓颉侧沿用同一语义。
     */
    protected fun completionTypeOf(type: CompletionType): CaCompletionKind = completionKindOf(type)

    /**
     * 把稳定快照转换为平台 lookup 元素。
     *
     * 统一走 [CaLookupElementFactory]：图标、尾文本、插入处理器、平台优先级与
     * 快照回挂都在那里完成；这里只搬运快照中的不可变数据，
     * 不允许在 lookup 元素上捕获符号、类型或分析会话。
     */
    protected open fun toLookupElement(snapshot: CaCompletionSnapshot): LookupElement =
        CaLookupElementFactory.create(snapshot)
}
