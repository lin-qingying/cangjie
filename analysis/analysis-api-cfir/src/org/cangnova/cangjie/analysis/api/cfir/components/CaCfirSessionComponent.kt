package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacade
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * CFIR session component 接口（对齐 K2 的 KaFirSessionComponent）。
 *
 * 为所有 CaCfir* component 提供到底层 [CaCfirSession] 和 [CfirSession] 的便捷访问。
 */
internal interface CaCfirSessionComponent : CaSessionComponent {
    val analysisSession: CaCfirSession

    val project: Project get() = analysisSession.project
    val rootModuleSession: CfirSession get() = analysisSession.resolutionFacade.useSiteFirSession
    val resolutionFacade: CaCfirResolutionFacade get() = analysisSession.resolutionFacade

    // TODO: 对齐 K2 的 asKaType / asKaDiagnostic / coneType 等辅助方法
}
