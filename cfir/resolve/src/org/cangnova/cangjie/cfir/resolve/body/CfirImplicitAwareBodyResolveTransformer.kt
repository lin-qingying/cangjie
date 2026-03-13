package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol

/**
 * 隐式类型感知 body resolve transformer。
 *
 * 包装 [CfirBodyResolveTransformer]，在变换可调用声明时通过
 * [CfirImplicitBodyResolveComputationSession] 的状态机进行缓存和递归保护。
 *
 * - IMPLICIT_TYPES 阶段：`implicitTypeOnly=true`，只推断声明边界类型
 * - BODY_RESOLVE 阶段：`implicitTypeOnly=false`，完整解析函数体
 *
 * 参考 K2 FirImplicitAwareBodyResolveTransformer。
 */
open class CfirImplicitAwareBodyResolveTransformer(
    session: CfirSession,
    scopeSession: CfirScopeSession,
    private val implicitBodyResolveComputationSession: CfirImplicitBodyResolveComputationSession,
    phase: CfirResolvePhase,
    implicitTypeOnly: Boolean,
    returnTypeCalculator: CfirReturnTypeCalculator,
    outerBodyResolveContext: CfirBodyResolveContext? = null,
) : CfirBodyResolveTransformer(
    session = session,
    scopeSession = scopeSession,
    returnTypeCalculator = returnTypeCalculator,
    outerBodyResolveContext = outerBodyResolveContext,
    phase = phase,
    implicitTypeOnly = implicitTypeOnly,
) {

    override fun transformFunction(function: CfirFunction, data: CfirResolutionMode): CfirDeclaration {
        return computeCachedTransformationResult(function) {
            super.transformFunction(function, data)
        }
    }

    override fun transformProperty(property: CfirProperty, data: CfirResolutionMode): CfirDeclaration {
        return computeCachedTransformationResult(property) {
            super.transformProperty(property, data)
        }
    }

    override fun transformVariable(variable: CfirVariable, data: CfirResolutionMode): CfirDeclaration {
        return computeCachedTransformationResult(variable) {
            super.transformVariable(variable, data)
        }
    }

    /**
     * 通过状态机缓存变换结果。
     *
     * 对可调用声明：检查是否已计算，若已缓存则直接返回，否则执行变换并缓存。
     * 对非可调用声明：直接执行变换。
     */
    private fun <D : CfirDeclaration> computeCachedTransformationResult(
        declaration: D,
        transformation: () -> CfirDeclaration,
    ): CfirDeclaration {
        if (declaration !is CfirCallableDeclaration) {
            return transformation()
        }
        val symbol = declaration.symbol as? CfirCallableSymbol<*> ?: return transformation()

        return when (val status = implicitBodyResolveComputationSession.getStatus(symbol)) {
            is CfirImplicitBodyResolveComputationStatus.Computed -> {
                // 已缓存 → 直接返回缓存的声明
                status.transformedDeclaration
            }
            is CfirImplicitBodyResolveComputationStatus.Computing -> {
                // 递归 → 跳过（返回原声明）
                declaration
            }
            is CfirImplicitBodyResolveComputationStatus.NotComputed -> {
                // 未计算 → 通过状态机执行
                implicitBodyResolveComputationSession.compute(symbol) {
                    @Suppress("UNCHECKED_CAST")
                    transformation() as CfirCallableDeclaration
                }
            }
        }
    }
}
