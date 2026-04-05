package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.asCaDiagnostic
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacade
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * CFIR session 组件公共协议。
 *
 * 所有 `CaCfir*` 组件都通过这里共享最基础的 low-level -> public 适配能力：
 * - 访问当前 [CaCfirSession]
 * - 访问 low-level [CaCfirResolutionFacade]
 * - 把底层 Cone 类型和诊断映射为公开 Analysis API 对象
 *
 * 这些能力必须集中在这一层，避免各组件重复书写相同的桥接逻辑。
 */
internal interface CaCfirSessionComponent : CaSessionComponent {
    val analysisSession: CaCfirSession

    val project: Project get() = analysisSession.project
    val resolutionFacade: CaCfirResolutionFacade get() = analysisSession.resolutionFacade

    /**
     * 将 low-level Cone 类型绑定到当前组件的生命周期 token，
     * 并转换成公开 [CaType]。
     */
    fun ConeCangJieType.asPublicType(): CaType = asCaType(token)

    /**
     * 将 low-level 诊断转换成当前组件生命周期下的公开诊断对象。
     */
    fun CjPsiDiagnostic.asPublicDiagnostic() = asCaDiagnostic(token)

    /**
     * 断言当前公开类型确实由 CFIR 类型驱动，并返回底层 Cone 类型。
     *
     * 所有需要读取底层 Cone 类型的组件都应通过这一入口进入，
     * 不允许各组件自行分散编写类型转换逻辑。
     */
    fun CaType.requireCfirConeType(owner: String): ConeCangJieType {
        val cfirType = this as? CaCfirTypeImpl
            ?: error("仅支持对 CFIR Analysis API 类型执行 $owner：${this::class.simpleName}")
        return cfirType.coneType
    }
}
