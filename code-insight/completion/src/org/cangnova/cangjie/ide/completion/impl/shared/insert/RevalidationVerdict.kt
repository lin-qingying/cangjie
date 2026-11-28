package org.cangnova.cangjie.ide.completion.impl.shared.insert

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionRecoveryService
import org.cangnova.cangjie.ide.completion.api.lookup.RecoveredTarget
import org.cangnova.cangjie.psi.CjElement

/**
 * 候选语义目标的复查结论。
 *
 * 复查必须在**选择时**（读动作）完成，写入时（写事务）只读结论：
 * 分析不能在写事务里跑，而「目标是否还存在」又必须用分析回答。
 */
sealed interface RevalidationVerdict {
    /**
     * 尚未复查。
     *
     * 平台监听器未安装时（例如宿主只用了插入处理器）就是这个值；
     * 此时仍受 [InsertionValidator] 的文档版本与区间校验约束，
     * 只是不再做语义层复查——**不假装复查过**。
     */
    data object NotPerformed : RevalidationVerdict

    /**
     * 目标已重新定位，可以写入；[target] 携带复算后的导入需求。
     */
    data class Recovered(val target: RecoveredTarget) : RevalidationVerdict

    /**
     * 目标已不存在或已不可见：**一个字符都不许写**。
     *
     * 只报告不复查结论还不够——写入过期目标会得到一个指向别处的名字，
     * 而用户不会察觉弹窗里的候选已经过期。
     */
    data object Invalid : RevalidationVerdict

    /**
     * 复查通过时复算出的导入计划。
     *
     * 失效候选返回 [CompletionImportPlan.NoImport]：它根本不会被写入，
     * 调用方不该因为「拿不到计划」而退化成「不写 import 但照写候选」。
     */
    val resolvedImportPlan: CompletionImportPlan
        get() = when (this) {
            is Recovered -> target.importPlan
            Invalid, NotPerformed -> CompletionImportPlan.NoImport
        }
}

/**
 * 复查结论回挂在 lookup 元素上的键。
 *
 * 用平台用户数据而不是扩展元素类型：宿主可能把仓颉候选与其它插件的候选
 * 混在同一次补全里，回挂数据不会改变元素的类型契约。
 */
val COMPLETION_REVALIDATION: Key<RevalidationVerdict> =
    Key.create("cangjie.completion.revalidation")

/**
 * 复查该元素承载的候选，并把结论回挂到元素上。
 *
 * 幂等：已经复查过的元素不再重查——用户在弹窗里来回移动选中项是常态，
 * 每次都重新进入分析会把这个动作变成分析风暴。
 *
 * 只处理本工厂创建的候选（元素上没有仓颉快照时直接返回）：
 * 宿主可能把其它插件的候选混在同一次补全里，那些候选没有身份可复查。
 */
@OptIn(CaExperimentalApi::class)
fun LookupElement.revalidateSelection(useSite: CjElement, recovery: CompletionRecoveryService) {
    if (getUserData(COMPLETION_REVALIDATION) != null) return
    val snapshot = CompletionInsertHandlers.snapshotOf(this) ?: return
    val verdict = try {
        recovery.recover(useSite, snapshot.identity)
            ?.let { if (it.visible) RevalidationVerdict.Recovered(it) else RevalidationVerdict.Invalid }
            ?: RevalidationVerdict.Invalid
    } catch (canceled: ProcessCanceledException) {
        throw canceled
    } catch (failure: RuntimeException) {
        // 复查是尽力而为：环境不允许分析时退回「未复查」，由文档版本与区间校验兜底。
        // 把环境失败判成「候选失效」会让用户在正常环境里选什么都插不进去。
        RevalidationVerdict.NotPerformed
    }
    putUserData(COMPLETION_REVALIDATION, verdict)
}
