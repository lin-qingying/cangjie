package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitAwareBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationSession
import org.cangnova.cangjie.cfir.resolve.body.CfirReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.body.CfirReturnTypeCalculatorWithJump
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * IMPLICIT_TYPES 闃舵 processor銆? *
 * 浣跨敤 [CfirImplicitAwareBodyResolveTransformer] 杩涜闅愬紡杩斿洖绫诲瀷鎺ㄦ柇锛? * - 鏃犳樉寮忚繑鍥炵被鍨嬬殑鍑芥暟 鈫?浠庡嚱鏁颁綋鏈€鍚庝竴涓〃杈惧紡鎺ㄦ柇
 * - 鏃犳樉寮忕被鍨嬬殑灞炴€?鍙橀噺 鈫?浠?initializer 鎺ㄦ柇
 * - 閫掑綊渚濊禆淇濇姢锛堢姸鎬佹満锛歂otComputed 鈫?Computing 鈫?Computed锛? *
 * 鍙傝€?K2 FirImplicitTypeBodyResolveProcessor銆? */
internal class CfirImplicitTypesResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.IMPLICIT_TYPES,
) {
    private val computationSession = CfirImplicitBodyResolveComputationSession()
    private val returnTypeCalculator = CfirReturnTypeCalculatorWithJump(session, scopeSession, computationSession)

    private val implicitTypesTransformer = CfirImplicitAwareBodyResolveTransformer(
        session = session,
        scopeSession = scopeSession,
        implicitBodyResolveComputationSession = computationSession,
        phase = CfirResolvePhase.IMPLICIT_TYPES,
        implicitTypeOnly = true,
        returnTypeCalculator = returnTypeCalculator,
    )

    @Suppress("UNCHECKED_CAST")
    override val transformer get() = implicitTypesTransformer as org.cangnova.cangjie.cfir.visitors.CfirTransformer<Nothing?>

    override fun processFile(file: CfirFile) {
        implicitTypesTransformer.transformFile(file, CfirResolutionMode.ContextIndependent)
    }
}

/**
 * BODY_RESOLVE 闃舵 processor銆? *
 * 浣跨敤 [CfirBodyResolveTransformer] 杩涜琛ㄨ揪寮忕骇鍒殑绫诲瀷鍚堟垚銆? * 瑕嗗啓 [processFile] 浠ヤ紶閫掓纭殑 [CfirResolutionMode] 鏁版嵁銆? */
internal class CfirBodyResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.BODY_RESOLVE,
) {
    private val bodyResolveTransformer = CfirBodyResolveTransformer(
        session = session,
        scopeSession = scopeSession,
        returnTypeCalculator = CfirReturnTypeCalculatorForFullBodyResolve.Default,
    )

    @Suppress("UNCHECKED_CAST")
    override val transformer get() = bodyResolveTransformer as org.cangnova.cangjie.cfir.visitors.CfirTransformer<Nothing?>

    override fun processFile(file: CfirFile) {
        bodyResolveTransformer.transformFile(file, CfirResolutionMode.ContextIndependent)
    }
}

