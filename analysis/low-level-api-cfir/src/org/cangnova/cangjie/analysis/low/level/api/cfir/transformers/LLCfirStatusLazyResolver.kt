/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirSingleResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.asResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.session
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.tryCollectDesignation
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
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.resolve.transformers.CfirStatusComputationSession
import org.cangnova.cangjie.cfir.resolve.transformers.CfirStatusResolveTransformer
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.visitors.transformSingle

internal object LLCfirStatusLazyResolver : LLCfirLazyResolver(CfirResolvePhase.STATUS) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver {
        val session = target.session
        val resolveMode = target.resolveMode()
        return LLCfirStatusTargetResolver(
            target = target,
            resolveMode = resolveMode,
            statusComputationSession = LLStatusComputationSession(
                session,
                session.getScopeSession(),
                resolveMode,
            ),
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
    val resolveMode: StatusResolveMode,
) : CfirStatusComputationSession(useSiteSession, useSiteScopeSession) {
    private val useSiteSessions: MutableList<LLCfirSession> = mutableListOf(useSiteSession)

    private inline fun withClassSession(regularClass: CfirClass, action: () -> Unit) {
        val newSession = (regularClass.moduleData.session as? LLCfirSession)
            ?.takeUnless { it == useSiteSessions.lastOrNull() }
        try {
            newSession?.let(useSiteSessions::add)
            action()
        } finally {
            newSession?.let { useSiteSessions.removeLast() }
        }
    }

    override fun forceResolveStatusesOfSupertypes(declaration: CfirDeclaration) {
        if (declaration !is CfirClass) return
        withClassSession(declaration) {
            super.forceResolveStatusesOfSupertypes(declaration)
        }
    }

    override fun superTypeToSymbols(typeRef: CfirTypeRef) = buildSet {
        val classId = typeRef.coneType.classId ?: return@buildSet

        for (useSiteSession in useSiteSessions.asReversed()) {
            useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.let(::add)
        }
    }

    override fun resolveClassForSuperType(regularClass: CfirClass): Boolean {
        val target = regularClass.tryCollectDesignation()?.asResolveTarget() ?: return false
        val resolver = LLCfirStatusTargetResolver(
            target,
            resolveMode = resolveMode,
            statusComputationSession = this,
        )

        resolver.resolveDesignation()
        return true
    }
}

/**
 * STATUS 阶段当前保持 low-level 独立 `LLStatusComputationSession` 分层，
 * 同时仍然以仓颉主干 `CfirStatusResolveTransformer` 为真实变换器。
 */
private class LLCfirStatusTargetResolver(
    target: LLCfirResolveTarget,
    private val resolveMode: StatusResolveMode,
    statusComputationSession: CfirStatusComputationSession,
) : LLCfirTargetResolver(target, CfirResolvePhase.STATUS) {
    private val statusComputationSession: CfirStatusComputationSession = statusComputationSession
    private val transformer = CfirStatusResolveTransformer(statusComputationSession)

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean = when (target) {
        is CfirClass -> {
            if (resolveMode.resolveSupertypes) {
                target.lazyResolveToPhase(resolverPhase.previous)
                statusComputationSession.forceResolveStatusesOfSupertypes(target)
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
            is CfirFile -> Unit
            else -> target.transformSingle(transformer, data = null)
        }
    }
}
