package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 已解析拥有图复制后的分析能力。类型、binding 和 resolve phase 是输入事实；它们不能证明
 * 新节点已经拥有自己的 CFG。resolve 必须完整重建分析产品后，才能发布 COMPLETE。
 */
enum class CfirOwnedGraphAnalysisState {
    PENDING,
    BUILDING,
    COMPLETE,
    ABORTED,
    NO_BODY_ANALYSIS,
}

internal object OwnedGraphAnalysisStateKey : CfirDeclarationDataKey()
internal object OwnedGraphAnalysisReadinessKey : CfirDeclarationDataKey()
internal object OwnedGraphAnnotationsUnselectedKey : CfirDeclarationDataKey()

/** 未选Library注解被复制为空视图时仍缺原内容，不能被后续Owned流程解释为完成空语义。 */
var CfirDeclaration.ownedGraphAnnotationsUnselected: Boolean? by CfirDeclarationDataRegistry.data(OwnedGraphAnnotationsUnselectedKey)

/** 完成产品的根/同代核验抽象；具体 DFA/CFG 产品仍由 resolve 拥有。 */
interface CfirOwnedGraphAnalysisReadiness {
    fun requireReady(root: CfirElement, session: CfirSession)
}

var CfirDeclaration.ownedGraphAnalysisState: CfirOwnedGraphAnalysisState? by
    CfirDeclarationDataRegistry.data(OwnedGraphAnalysisStateKey)

var CfirDeclaration.ownedGraphAnalysisReadiness: CfirOwnedGraphAnalysisReadiness? by
    CfirDeclarationDataRegistry.data(OwnedGraphAnalysisReadinessKey)

/** 正式 provider、BODY、LL 和冻结导出入口共用此门，不把 BODY 再请求当作重建。 */
fun CfirDeclaration.requireOwnedGraphAnalysisReady() {
    check(ownedGraphAnnotationsUnselected != true) { "Owned analysis requires explicitly materialized annotation content" }
    when (ownedGraphAnalysisState) {
        null, CfirOwnedGraphAnalysisState.NO_BODY_ANALYSIS -> return
        CfirOwnedGraphAnalysisState.COMPLETE -> checkNotNull(ownedGraphAnalysisReadiness) {
            "Completed owned analysis requires its root/domain product: $symbol"
        }.requireReady(this, moduleData.session)
        else -> error("Resolved owned graph requires complete analysis rebuilding before consumption: $symbol")
    }
}
