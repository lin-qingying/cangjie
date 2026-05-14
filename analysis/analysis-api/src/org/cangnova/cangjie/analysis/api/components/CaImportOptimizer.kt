package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjFile

/**
 * 导入优化协议。
 *
 * 设计要点/职责:
 * - 针对 [CjFile] 计算可独立执行的导入优化方案 [CaImportOptimizationPlan],
 *   包含未使用导入的清理、重复导入合并等元数据,真正的写入由调用方负责。
 * - 协议本身不修改 PSI,保持只读分析层语义。
 *
 * 对齐 Kotlin Analysis API 中负责导入优化分析的 component(参考 KaImportOptimizer)。
 */
interface CaImportOptimizer : CaLifetimeOwner {
    /**
     * 收集该文件的导入优化方案,可应用于自动 import 整理流程。
     */
    fun CjFile.collectImportOptimizationPlan(): CaImportOptimizationPlan
}
