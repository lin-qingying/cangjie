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
 * 感知隐式类型的 body resolve transformer。
 * 它包装 [CfirBodyResolveTransformer]，并借助
 * [CfirImplicitBodyResolveComputationSession] 做缓存和递归保护。
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

    override fun transformFunction(function: CfirFunction, data: CfirResolutionMode): CfirFunction {
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(function) {
            super.transformFunction(function, data)
        } as CfirFunction
    }

    override fun transformProperty(property: CfirProperty, data: CfirResolutionMode): CfirProperty {
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(property) {
            super.transformProperty(property, data)
        } as CfirProperty
    }

    override fun transformVariable(variable: CfirVariable, data: CfirResolutionMode): CfirVariable {
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(variable) {
            super.transformVariable(variable, data)
        } as CfirVariable
    }

    /**
     * 通过状态机缓存变换结果。
     * 对可调用声明先查询缓存；对其他声明则直接执行变换。
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
                // 已缓存，直接返回
                status.transformedDeclaration
            }
            is CfirImplicitBodyResolveComputationStatus.Computing -> {
                // 递归访问时直接返回原声明
                declaration
            }
            is CfirImplicitBodyResolveComputationStatus.NotComputed -> {
                // 未计算时，通过状态机执行
                implicitBodyResolveComputationSession.compute(symbol) {
                    @Suppress("UNCHECKED_CAST")
                    transformation() as CfirCallableDeclaration
                }
            }
        }
    }
}

