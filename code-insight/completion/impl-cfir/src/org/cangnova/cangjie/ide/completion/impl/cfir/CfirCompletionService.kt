package org.cangnova.cangjie.ide.completion.impl.cfir

import com.intellij.openapi.progress.ProgressManager
import org.cangnova.cangjie.ide.completion.api.CompletionFilter
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.CangJieCompletionService
import org.cangnova.cangjie.ide.completion.impl.cfir.filter.CfirExpectedTypeCompletionFilter
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.CfirCDocCompletionSection
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.CfirEnumEntryCompletionSection
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.CfirIndexCompletionSection
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.CfirKeywordCompletionSection
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.CfirLocalScopeCompletionSection
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.CfirMemberCompletionSection
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.CfirNamedArgumentCompletionSection
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.CfirTopLevelCompletionSection
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.distinctByIdentity
import org.cangnova.cangjie.ide.completion.impl.shared.CompletionResultSink
import org.cangnova.cangjie.ide.completion.impl.shared.CompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.SerialCompletionRunner
import org.cangnova.cangjie.ide.completion.impl.shared.lookup.sortedByAnalysisWeight

/**
 * 基于 Analysis API 的仓颉补全服务。
 *
 * 服务只做三件事：选择区段、按优先级串行执行、按稳定顺序归并结果。
 * 它不解析语义，也不决定候选内容——语义全部来自 Analysis 组件。
 *
 * 并行执行尚未启用：必须先通过会话隔离、确定顺序、取消传播与串并行结果等价四项准入。
 */
class CfirCompletionService(
    /**
     * 期望类型过滤器。SMART 请求会在此按期望类型过滤并重写匹配结论；
     * BASIC 请求由过滤实现保证原样返回。
     */
    private val filter: CompletionFilter = CfirExpectedTypeCompletionFilter(),
) : CangJieCompletionService {
    /**
     * 当前生效的候选区段；按执行优先级排列。
     *
     * 内部贡献者采用显式列表，不注册公共扩展点：
     * 补全区段的拆分是实现细节，不应成为宿主可替换的契约面。
     */
    private val sections: List<CompletionSection> = listOf(
        CfirKeywordCompletionSection(),
        CfirCDocCompletionSection(),
        CfirLocalScopeCompletionSection(),
        CfirMemberCompletionSection(),
        CfirEnumEntryCompletionSection(),
        CfirTopLevelCompletionSection(),
        CfirNamedArgumentCompletionSection(),
        CfirIndexCompletionSection(),
    )

    /**
     * 串行执行器；并行准入通过前不更换。
     */
    private val runner = SerialCompletionRunner()

    /**
     * 执行补全并返回稳定快照。
     *
     * 归并顺序固定为「按身份去重 → 期望类型过滤 → 按分析期权重稳定排序」：
     * - 去重放在过滤前：同一声明可能被两个区段各产一次（如类型位置的类型建议
     *   同时来自关键字区段与顶层区段），先按身份键合并，过滤与排序只处理一份；
     * - 排序放在过滤后：过滤会按匹配结论重写权重（MATCH 加权、MISMATCH 出局），
     *   排序必须看到最终权重，否则 SMART 的排序与 BASIC 不一致。
     *
     * 取消与失败都向上传播：把异常吞成空列表会让真实缺陷看起来像「没有候选」。
     */
    override fun complete(request: CompletionRequest): List<CompletionSnapshot> {
        val collected = mutableListOf<CompletionSnapshot>()
        val sink = object : CompletionResultSink {
            /**
             * 按到达顺序收集快照；去重、过滤与排序在全部区段完成后统一进行。
             */
            override fun consume(batch: List<CompletionSnapshot>, isLast: Boolean) {
                collected += batch
            }
        }
        runner.runSerial(request, sections, sink, ProgressManager.getInstance().progressIndicator)
        return collected
            .distinctByIdentity()
            .let { filter.apply(request, it) }
            .sortedByAnalysisWeight()
    }
}
