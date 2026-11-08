package org.cangnova.cangjie.codeinsight.hints

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.psi.CjFile

/**
 * 仓颉 inlay hint 的共享收集入口。
 *
 * 该类只产出与宿主无关的提示；IDE 与 LSP 分别负责把提示转换为各自的协议对象。
 *
 * 生命周期约束：`session` 由调用方在 `analyze` 块内提供，收集过程必须在块内完成——
 * 产出的 [CangJieInlayHint] 只包含已渲染文本与 PSI 层锚点，不携带任何 lifetime owner。
 */
object CangJieInlayHintCollector {
    /**
     * 收集 `file` 内（可限定到 `range`）的全部提示。
     *
     * @param range `null` 表示整篇文件；LSP 传入请求范围。
     * @param session 调用方已进入的活跃分析会话，本函数不自行 `analyze`。
     */
    fun collect(
        file: CjFile,
        range: TextRange?,
        session: CaSession,
        options: CangJieInlayHintOptions,
    ): List<CangJieInlayHint> {
        if (file.project.isDefault) return emptyList()
        // 空 range 是显式的「这一屏没有要求」，不必再遍历 PSI。
        if (range != null && range.isEmpty) return emptyList()

        val hints = ArrayList<CangJieInlayHint>()
        hints += CjTypeHintsCollector.collect(file, range, session, options)
        if (options.showParameterNames) {
            hints += CjParameterNameHintsCollector.collect(file, range, session)
        }
        if (options.showRangeValues) {
            hints += CjRangeValueHintsCollector.collect(file, range, session)
        }
        if (options.showLambdaReturns) {
            hints += CjLambdaHintsCollector.collect(file, range, session)
        }
        return hints.sortedBy { it.offset }
    }
}
