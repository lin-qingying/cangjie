/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirSingleResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.session
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkDeclarationStatusIsResolved
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.resolve.transformers.CfirStatusComputationSession
import org.cangnova.cangjie.cfir.resolve.transformers.CfirStatusResolveTransformer
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.visitors.transformSingle

internal object LLCfirStatusLazyResolver : LLCfirLazyResolver(CfirResolvePhase.STATUS) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver {
        val session = target.session
        val resolveMode = target.resolveMode()
        return LLCfirStatusTargetResolver(
            target = target,
            resolveMode = resolveMode,
            statusComputationSession = CfirStatusComputationSession(session, session.getScopeSession()),
        )
    }

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        if (target !is CfirMemberDeclaration) return
        checkDeclarationStatusIsResolved(target)
    }
}

private sealed class StatusResolveMode(val resolveSupertypes: Boolean) {
    abstract fun shouldBeResolved(callableDeclaration: CfirCallableDeclaration): Boolean

    object OnlyTarget : StatusResolveMode(resolveSupertypes = false) {
        override fun shouldBeResolved(callableDeclaration: CfirCallableDeclaration): Boolean = false
    }

    object AllCallables : StatusResolveMode(resolveSupertypes = true) {
        override fun shouldBeResolved(callableDeclaration: CfirCallableDeclaration): Boolean = true
    }
}

private fun LLCfirResolveTarget.resolveMode(): StatusResolveMode = when (this) {
    is LLCfirSingleResolveTarget -> when (target) {
        is CfirClassLikeDeclaration -> StatusResolveMode.OnlyTarget
        else -> StatusResolveMode.AllCallables
    }

    else -> StatusResolveMode.AllCallables
}
private class LLStatusComputationSession(
    useSiteSession: LLCfirSession,
    useSiteScopeSession: ScopeSession,
    val resolveMode:  StatusResolveMode,
) : CfirStatusComputationSession(useSiteSession, useSiteScopeSession){

}
/**
 * STATUS 阶段当前对齐仓颉主干 `CfirStatusResolveTransformer`。
 * 这里不再引用 Kotlin FIR 专有的 statusResolver/storeClass/internal hooks。
 */
private class LLCfirStatusTargetResolver(
    target: LLCfirResolveTarget,
    private val resolveMode: StatusResolveMode,
    statusComputationSession: LLStatusComputationSession,
) : LLCfirTargetResolver(target, CfirResolvePhase.STATUS) {
    private val transformer = Transformer(statusComputationSession)

    private class Transformer(statusComputationSession: LLStatusComputationSession) :
        CfirStatusResolveTransformer(statusComputationSession) {
        override fun CfirDeclaration.needResolveMembers(): Boolean = false
        override fun CfirDeclaration.needResolveNestedClassifiers(): Boolean = false

        override fun transformClass(klass: CfirClass, data: CfirResolvedDeclarationStatus?): CfirStatement {
            return klass
        }
    }
    @Deprecated("Should never be called directly, only for override purposes, please use withClass", level = DeprecationLevel.ERROR)
    override fun withContainingClass(firClass: CfirClass, action: () -> Unit) {
        doResolveWithoutLock(firClass)
        transformer.storeClass(firClass) {
            action()
            firClass
        }

        transformer.statusComputationSession.endComputing(firClass)

    }

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean = when (target) {
        is CfirClass -> {
            if (resolveMode.resolveSupertypes) {
                target.lazyResolveToPhase(resolverPhase.previous)
            }
            false
        }

        is CfirNamedFunction,
        is CfirProperty,
            -> false

        else -> false
    }

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        when (target) {
            is CfirFile -> {
                // file 自身没有 status 变换
            }

            else -> target.transformSingle(transformer, data = null)
        }
    }
}
