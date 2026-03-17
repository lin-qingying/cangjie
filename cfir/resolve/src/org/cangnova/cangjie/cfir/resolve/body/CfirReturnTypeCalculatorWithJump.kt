package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * 璺宠浆寮忚繑鍥炵被鍨嬭绠楀櫒銆? *
 * 鐢ㄤ簬 IMPLICIT_TYPES 闃舵锛屽綋閬囧埌灏氭湭璁＄畻杩斿洖绫诲瀷鐨勫０鏄庢椂锛? * 浼?璺宠浆"鍒拌澹版槑杩涜 designated resolve锛岃绠楀叾杩斿洖绫诲瀷銆? *
 * 閫掑綊淇濇姢锛氶€氳繃 [implicitBodyResolveComputationSession] 鐨勭姸鎬佹満妫€娴嬮€掑綊锛? * Computing 鐘舵€佷笅杩斿洖 [ConeErrorType]銆? *
 * 鍙傝€?K2 ReturnTypeCalculatorWithJump銆? */
class CfirReturnTypeCalculatorWithJump(
    private val session: org.cangnova.cangjie.cfir.session.CfirSession,
    private val scopeSession: CfirScopeSession,
    private val implicitBodyResolveComputationSession: CfirImplicitBodyResolveComputationSession,
) : CfirReturnTypeCalculator {

    override fun tryCalculateReturnType(declaration: CfirCallableDeclaration): ConeCangjieType? {
        // 1. 宸叉湁鏄惧紡瑙ｆ瀽绫诲瀷 鈫?鐩存帴杩斿洖
        val typeRef = extractReturnTypeRef(declaration) ?: return null
        if (typeRef is CfirResolvedTypeRef) {
            return typeRef.coneType
        }

        // 2. 闈為殣寮忕被鍨?鈫?鏃犳硶鎺ㄦ柇
        if (typeRef !is CfirImplicitTypeRef) {
            return null
        }

        // 3. 闇€瑕佹帹鏂?鈥?妫€鏌ヨ绠楃姸鎬?
        val symbol = extractSymbol(declaration) ?: return null
        return when (val status = implicitBodyResolveComputationSession.getStatus(symbol)) {
            is CfirImplicitBodyResolveComputationStatus.Computed -> {
                status.resolvedType
            }
            is CfirImplicitBodyResolveComputationStatus.Computing -> {
                // 閫掑綊渚濊禆 鈫?杩斿洖閿欒绫诲瀷
                ConeErrorType("recursive implicit type")
            }
            is CfirImplicitBodyResolveComputationStatus.NotComputed -> {
                // 瑙﹀彂 designated resolve
                resolveDesignated(declaration)
            }
        }
    }

    /** 瑙﹀彂 designated resolve 浠ヨ绠楀０鏄庣殑杩斿洖绫诲瀷 */
    private fun resolveDesignated(declaration: CfirCallableDeclaration): ConeCangjieType {
        val symbol = extractSymbol(declaration) ?: return ConeErrorType("no symbol for declaration")

        val result = implicitBodyResolveComputationSession.compute(symbol) {
            // 鍒涘缓 designated transformer 瑙ｆ瀽姝ゅ０鏄?
            val designatedTransformer = CfirDesignatedBodyResolveTransformer(
                designation = declaration,
                session = session,
                scopeSession = scopeSession,
                implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
                returnTypeCalculator = this,
            )
            // 鎵惧埌澹版槑鎵€鍦ㄧ殑鏂囦欢骞跺彉鎹?
            val file = findContainingFile(declaration)
            if (file != null) {
                designatedTransformer.transformFile(
                    file,
                    org.cangnova.cangjie.cfir.resolve.CfirResolutionMode.ContextIndependent,
                )
            }
            // 杩斿洖鍙樻崲鍚庣殑澹版槑
            declaration
        }
        return extractResolvedType(result) ?: ConeErrorType("failed to resolve implicit type")
    }

    private fun extractReturnTypeRef(declaration: CfirCallableDeclaration): CfirTypeRef? = when (declaration) {
        is CfirFunction -> declaration.returnTypeRef
        is CfirProperty -> declaration.returnTypeRef
        is CfirVariable -> declaration.returnTypeRef
        else -> null
    }

    private fun extractSymbol(declaration: CfirCallableDeclaration) =
        declaration.symbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>

    private fun extractResolvedType(declaration: CfirCallableDeclaration): ConeCangjieType? {
        val typeRef = extractReturnTypeRef(declaration)
        return if (typeRef is CfirResolvedTypeRef) typeRef.coneType else null
    }

    /** 鏌ユ壘澹版槑鎵€鍦ㄧ殑鏂囦欢锛堝悜涓婇亶鍘嗙鍙疯〃锛?*/
    private fun findContainingFile(declaration: CfirCallableDeclaration): org.cangnova.cangjie.cfir.declarations.CfirFile? {
        // Phase 3 绠€鍖栵細designated resolve 鐩存帴鍦?declaration 涓婃搷浣?        // 瀹屾暣瀹炵幇闇€瑕?file 鈫?(class)? 鈫?
        // declaration 璺緞
        return null
    }
}

