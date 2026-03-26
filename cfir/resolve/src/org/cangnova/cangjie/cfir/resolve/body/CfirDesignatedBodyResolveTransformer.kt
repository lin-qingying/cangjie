package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 指定路径的 body resolve transformer。
 * 它只解析目标声明以及必要的路径容器，用于按需触发 designated resolve，
 * 避免整份文件被重复解析。
 */
class CfirDesignatedBodyResolveTransformer(
    private val designation: CfirCallableDeclaration,
    session: CfirSession,
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: CfirImplicitBodyResolveComputationSession,
    returnTypeCalculator: ReturnTypeCalculator,
) : CfirImplicitAwareBodyResolveTransformer(
    session = session,
    scopeSession = scopeSession,
    implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
    phase = CfirResolvePhase.IMPLICIT_TYPES,
    implicitTypeOnly = true,
    returnTypeCalculator = returnTypeCalculator,
) {

    /** 最近一次变换的结果。 */
    var lastResult: CfirElement? = null
        private set

    override fun transformDeclarationContent(
        declaration: CfirDeclaration,
        data: ResolutionMode,
    ): CfirDeclaration {
        // 只变换指定目标声明
        if (declaration === designation) {
            val result = declaration.transform<CfirDeclaration, ResolutionMode>(this, data)
            lastResult = result
            return result
        }
        // 路径上的容器仍需继续遍历，以建立必要的 scope 上下文
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

    /** 检查类是否包含指定声明。 */
    private fun containsDesignation(klass: CfirClass): Boolean {
        return klass.declarations.any { it === designation }
    }
}

