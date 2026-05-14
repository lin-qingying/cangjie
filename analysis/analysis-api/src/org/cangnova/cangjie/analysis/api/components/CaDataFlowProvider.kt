package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjExpression

/**
 * 数据流信息协议。
 *
 * 设计要点/职责:
 * - 暴露表达式在当前 session 中的数据流视图(智能转换、可空性追踪等),由 [CaDataFlowInfo] 统一承载。
 * - 仅返回稳定的语义结果,不暴露底层控制流图或后端分析细节。
 *
 * 对齐 Kotlin Analysis API 的 `KaDataFlowProvider`。
 */
interface CaDataFlowProvider : CaLifetimeOwner {
    /**
     * 获取该表达式当前的数据流信息;若无可用信息则返回值由实现负责给出空数据。
     */
    fun CjExpression.getDataFlowInfo(): CaDataFlowInfo
}
