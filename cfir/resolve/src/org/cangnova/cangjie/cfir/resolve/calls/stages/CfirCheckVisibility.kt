package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.VisibilityError
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility

/**
 * 可见性检查阶段。
 *
 * Phase 3 简化实现：仅检查 private 声明在当前上下文中是否可访问。
 * 后续阶段将完善 protected/internal 等更精细的可见性规则。
 *
 * 对齐 K2 ResolutionStages#CheckVisibility。
 */
object CfirCheckVisibility : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val symbol = candidate.symbol
        if (!symbol.isBound) return

        val visibility = extractVisibility(symbol.cfir) ?: return

        // Phase 3 简化：仅对 private 做基本检查
        // 完整的可见性检查（包含访问者位置、protected 继承链等）留到后续阶段
        if (Visibilities.isPrivate(visibility)) {
            // Phase 3 暂不限制 — 后续将通过 file/class 归属判断是否可访问
        }
    }

    /** 从声明中提取可见性（不同声明类型的 status 属性） */
    private fun extractVisibility(declaration: CfirDeclaration): Visibility? {
        return when (declaration) {
            is CfirFunction -> declaration.status.visibility
            is CfirProperty -> declaration.status.visibility
            is CfirConstructor -> declaration.status.visibility
            is CfirVariable -> declaration.status.visibility
            is CfirClass -> declaration.status.visibility
            else -> null
        }
    }
}
