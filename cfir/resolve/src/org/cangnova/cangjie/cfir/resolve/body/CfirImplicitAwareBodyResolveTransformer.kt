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
 * 闅愬紡绫诲瀷鎰熺煡 body resolve transformer銆? *
 * 鍖呰 [CfirBodyResolveTransformer]锛屽湪鍙樻崲鍙皟鐢ㄥ０鏄庢椂閫氳繃
 * [CfirImplicitBodyResolveComputationSession] 鐨勭姸鎬佹満杩涜缂撳瓨鍜岄€掑綊淇濇姢銆? *
 * - IMPLICIT_TYPES 闃舵锛歚implicitTypeOnly=true`锛屽彧鎺ㄦ柇澹版槑杈圭晫绫诲瀷
 * - BODY_RESOLVE 闃舵锛歚implicitTypeOnly=false`锛屽畬鏁磋В鏋愬嚱鏁颁綋
 *
 * 鍙傝€?K2 FirImplicitAwareBodyResolveTransformer銆? */
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
     * 閫氳繃鐘舵€佹満缂撳瓨鍙樻崲缁撴灉銆?     *
     * 瀵瑰彲璋冪敤澹版槑锛氭鏌ユ槸鍚﹀凡璁＄畻锛岃嫢宸茬紦瀛樺垯鐩存帴杩斿洖锛屽惁鍒欐墽琛屽彉鎹㈠苟缂撳瓨銆?     * 瀵归潪鍙皟鐢ㄥ０鏄庯細鐩存帴鎵ц鍙樻崲銆?     */
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
                // 宸茬紦瀛?鈫?鐩存帴杩斿洖缂撳瓨鐨勫０鏄?
                status.transformedDeclaration
            }
            is CfirImplicitBodyResolveComputationStatus.Computing -> {
                // 閫掑綊 鈫?璺宠繃锛堣繑鍥炲師澹版槑锛?
                declaration
            }
            is CfirImplicitBodyResolveComputationStatus.NotComputed -> {
                // 鏈绠?鈫?閫氳繃鐘舵€佹満鎵ц
                implicitBodyResolveComputationSession.compute(symbol) {
                    @Suppress("UNCHECKED_CAST")
                    transformation() as CfirCallableDeclaration
                }
            }
        }
    }
}

