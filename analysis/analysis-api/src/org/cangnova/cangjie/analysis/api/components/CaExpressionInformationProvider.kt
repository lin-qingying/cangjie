package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjExpression

/**
 * 表达式元信息协议。
 *
 * 设计要点/职责:
 * - 暴露与表达式形态、用途相关的轻量级语义判定,
 *   不承担完整的控制流或求值职责(那些分别由 `CaDataFlowProvider`、`CaEvaluator` 等承担)。
 * - 判定结果统一以稳定的布尔属性给出,便于上层在 IDE 与诊断流程中复用。
 *
 * 对齐 Kotlin Analysis API 的 `KaExpressionInformationProvider`。
 */
interface CaExpressionInformationProvider : CaLifetimeOwner {
    /**
     * 该表达式是否表现为语句(其值不参与外部使用)。
     */
    val CjExpression.isStatementLike: Boolean

    /**
     * 该表达式是否可被视为编译期常量(此处仅做形态/语义级判定,实际求值由 `CaEvaluator` 完成)。
     */
    val CjExpression.isCompileTimeConstant: Boolean
}
