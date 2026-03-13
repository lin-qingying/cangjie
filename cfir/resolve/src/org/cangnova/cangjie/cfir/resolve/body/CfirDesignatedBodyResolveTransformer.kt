package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 指定路径 body resolve transformer。
 *
 * 仅解析指定的声明，跳过路径外的其他声明。
 * 支持 file → (class)? → declaration 三级路径。
 *
 * 用于 [CfirReturnTypeCalculatorWithJump] 触发的按需解析：
 * 当 IMPLICIT_TYPES 阶段遇到一个尚未解析的声明引用时，
 * 通过 designated transformer 仅解析目标声明，避免全文件重解析。
 *
 * 参考 K2 FirDesignatedBodyResolveTransformerForReturnTypeCalculator。
 */
class CfirDesignatedBodyResolveTransformer(
    private val designation: CfirCallableDeclaration,
    session: CfirSession,
    scopeSession: CfirScopeSession,
    implicitBodyResolveComputationSession: CfirImplicitBodyResolveComputationSession,
    returnTypeCalculator: CfirReturnTypeCalculator,
) : CfirImplicitAwareBodyResolveTransformer(
    session = session,
    scopeSession = scopeSession,
    implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
    phase = CfirResolvePhase.IMPLICIT_TYPES,
    implicitTypeOnly = true,
    returnTypeCalculator = returnTypeCalculator,
) {

    /** 最后一次变换的结果 */
    var lastResult: CfirElement? = null
        private set

    override fun transformDeclarationContent(
        declaration: CfirDeclaration,
        data: CfirResolutionMode,
    ): CfirDeclaration {
        // 仅变换指定的声明
        if (declaration === designation) {
            val result = declaration.transform<CfirDeclaration, CfirResolutionMode>(this, data)
            lastResult = result
            return result
        }
        // 路径上的容器（file、class）需要继续遍历以建立 scope 上下文
        return when (declaration) {
            is CfirFile -> super.transformDeclarationContent(declaration, data)
            is CfirClass -> {
                if (containsDesignation(declaration)) {
                    super.transformDeclarationContent(declaration, data)
                } else {
                    declaration
                }
            }
            else -> declaration // 跳过无关声明
        }
    }

    /** 检查类是否包含指定的声明 */
    private fun containsDesignation(klass: CfirClass): Boolean {
        return klass.declarations.any { it === designation }
    }
}
