package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjFile

/**
 * 「位置 → 宏展开源映射」的只读查询。
 *
 * 只读是硬约束：本查询**不**把展开产物带进 CFIR，也不向调用方暴露 `MacroSurface` 本身，
 * 返回的只是「身份 + 区间 + 可靠性」。展开侧的状态机不因本查询而改变。
 *
 * 两个入口对应两类调用方：
 * - 按 PSI 元素（补全在分析期拿到的是元素）；
 * - 按文件 + 偏移（诊断等只有位置信息的调用方）。
 *
 * 可靠性取**载体通道**的既有状态，不引入启发式：子展开面由父边通道判定
 * （未解析 ⇒ 不可靠），无法唯一确定最内层时同样判为不可靠。
 */
interface CaMacroExpansionMappingProvider : CaLifetimeOwner {
    /**
     * 该 PSI 元素起始位置所在的宏展开面。
     *
     * 元素不在仓颉源文件内（或位置不可用）时返回 [CaMacroExpansionMapping.None]。
     */
    val PsiElement.macroExpansionMapping: CaMacroExpansionMapping

    /**
     * [file] 内 [offset] 所在的宏展开面。
     *
     * 偏移按**闭区间**与展开面区间比较：调用点首尾字符本身也属于该展开面。
     */
    fun macroExpansionMappingAt(file: CjFile, offset: Int): CaMacroExpansionMapping
}
