package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 返回类型计算专用的 designated body resolve transformer。
 *
 * 对齐 Kotlin `FirDesignatedBodyResolveTransformerForReturnTypeCalculator`：
 * 沿指定 declaration iterator 逐层进入目标声明，并记录最后一次目标变换结果。
 */
open class CfirDesignatedBodyResolveTransformerForReturnTypeCalculator(
    /** 从外层到目标声明的指定路径迭代器。 */
    private val designation: Iterator<CfirElement>,
    session: CfirSession,
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: CfirImplicitBodyResolveComputationSession,
    returnTypeCalculator: ReturnTypeCalculatorWithJump,
) : CfirImplicitAwareBodyResolveTransformer(
    session = session,
    scopeSession = scopeSession,
    implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
    phase = CfirResolvePhase.IMPLICIT_TYPES,
    implicitTypeOnly = true,
    returnTypeCalculator = returnTypeCalculator,
) {
    /** 指定路径上最后一个目标声明的变换结果。 */
    var lastResult: CfirElement? = null

    /**
     * 按 designation 路径进入下一层声明内容。
     *
     * 到达路径末端后才执行普通 declaration content 转换，并记录目标声明结果。
     */
    override fun transformDeclarationContent(
        declaration: CfirDeclaration,
        data: ResolutionMode,
    ): CfirDeclaration {
        if (designation.hasNext()) {
            val result = designation.next().transform<CfirDeclaration, ResolutionMode>(this, data)
            if (!designation.hasNext() && lastResult == null) {
                lastResult = result
            }
            return declaration
        }

        return super.transformDeclarationContent(declaration, data)
    }
}
