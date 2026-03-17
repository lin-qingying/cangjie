package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.VisibilityError
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility

/**
 * 鍙鎬ф鏌ラ樁娈点€? *
 * Phase 3 绠€鍖栧疄鐜帮細浠呮鏌?private 澹版槑鍦ㄥ綋鍓嶄笂涓嬫枃涓槸鍚﹀彲璁块棶銆? * 鍚庣画闃舵灏嗗畬鍠?protected/internal 绛夋洿绮剧粏鐨勫彲瑙佹€ц鍒欍€? *
 * 瀵归綈 K2 ResolutionStages#CheckVisibility銆? */
object CfirCheckVisibility : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val symbol = candidate.symbol
        if (!symbol.isBound) return

        val visibility = extractVisibility(symbol.cfir) ?: return

        // Phase 3 绠€鍖栵細浠呭 private 鍋氬熀鏈鏌?        // 瀹屾暣鐨勫彲瑙佹€ф鏌ワ紙鍖呭惈璁块棶鑰呬綅缃€乸rotected 缁ф壙閾剧瓑锛夌暀鍒板悗缁樁娈?
        if (Visibilities.isPrivate(visibility)) {
            // Phase 3 鏆備笉闄愬埗 鈥?鍚庣画灏嗛€氳繃 file/class 褰掑睘鍒ゆ柇鏄惁鍙闂?
            }
    }

    /** 浠庡０鏄庝腑鎻愬彇鍙鎬э紙涓嶅悓澹版槑绫诲瀷鐨?status 灞炴€э級 */
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

