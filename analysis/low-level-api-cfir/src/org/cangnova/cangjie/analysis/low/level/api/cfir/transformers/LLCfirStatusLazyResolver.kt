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
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkAnalysisReadiness
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkDeclarationStatusIsResolved
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
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

    private inline fun withClassSession(classLikeDeclaration: CfirClassLikeDeclaration, action: () -> Unit) {
        val newSession = (classLikeDeclaration.moduleData.session as? LLCfirSession)
            ?.takeUnless { it == useSiteSessions.lastOrNull() }
        try {
            newSession?.let(useSiteSessions::add)
            action()
        } finally {
            newSession?.let { useSiteSessions.removeLast() }
        }
    }

    override fun forceResolveStatusesOfSupertypes(declaration: CfirDeclaration) {
        if (declaration !is CfirClassLikeDeclaration) return
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

    override fun resolveClassForSuperType(classLikeDeclaration: CfirClassLikeDeclaration): Boolean {
        val target = classLikeDeclaration.tryCollectDesignation()?.asResolveTarget() ?: return false
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
    private val transformer = Transformer(statusComputationSession)

    @Deprecated("Should never be called directly, only for override purposes, please use withClassLike", level = DeprecationLevel.ERROR)
    override fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        if (cfirClassLike is CfirClass || cfirClassLike is CfirInterface) {
            doResolveWithoutLock(cfirClassLike)
            transformer.storeClass(cfirClassLike) {
                action()
            }

            transformer.statusComputationSession.endComputing(cfirClassLike)
        } else {
            action()
        }
    }

    private fun resolveClassLikeTypeParameters(classLike: CfirClassLikeDeclaration) {
        classLike.transformTypeParameters(transformer, data = null)
    }

    private fun resolveCallableMembers(classLike: CfirClassLikeDeclaration) {
        for (member in classLike.declarations) {
            if (member !is CfirCallableDeclaration || !resolveMode.shouldBeResolved(member)) continue

            member.lazyResolveToPhase(resolverPhase.previous)
            performResolve(member)
        }
    }

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean = when (target) {
        is CfirClass -> {
            if (transformer.statusComputationSession[target].requiresComputation) {
                target.lazyResolveToPhase(resolverPhase.previous)
                resolveClassLike(target)
            }

            true
        }

        is CfirInterface -> {
            if (transformer.statusComputationSession[target].requiresComputation) {
                target.lazyResolveToPhase(resolverPhase.previous)
                resolveClassLike(target)
            }

            true
        }

        is CfirNamedFunction -> {
            performResolveWithOverriddenCallables(
                target,
                { transformer.statusResolver.getOverriddenFunctions(it, transformer.containingClass) },
                { element, overridden -> transformer.transformNamedFunction(element, overridden) },
            )

            true
        }

        is CfirFunction -> {
            if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase)) {
                true
            } else {
                performCustomResolveUnderLock(target) {
                    transformer.transformFunctionStatusWithoutPhaseGuard(target)
                }

                true
            }
        }

        is CfirExtend -> {
            if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase)) {
                true
            } else {
                performCustomResolveUnderLock(target) {
                    transformer.transformExtendStatusWithoutPhaseGuard(target)
                }

                true
            }
        }

        is CfirProperty -> {
            performResolveWithOverriddenCallables(
                target,
                { transformer.statusResolver.getOverriddenProperties(it, transformer.containingClass) },
                { element, overridden -> transformer.transformProperty(element, overridden) },
            )

            true
        }

        is CfirPatternVariable -> {
            performCustomResolveUnderLock(target) {
                transformer.transformVariableStatusWithoutPhaseGuard(target)
            }

            true
        }

        is CfirPatternBindingVariable -> {
            performCustomResolveUnderLock(target) {
                transformer.transformVariableStatusWithoutPhaseGuard(target)
            }

            true
        }

        else -> false
    }

    private fun resolveClassLike(classLike: CfirClassLikeDeclaration) {
        transformer.statusComputationSession.startComputing(classLike)

        if (resolveMode.resolveSupertypes) {
            transformer.statusComputationSession.forceResolveStatusesOfSupertypes(classLike)
        }

        performCustomResolveUnderLock(classLike) {
            when (classLike) {
                is CfirClass -> transformer.transformClassStatus(classLike)
                is CfirInterface -> transformer.transformInterfaceStatus(classLike)
                else -> error("Unexpected class-like declaration ${classLike::class.simpleName} for low-level STATUS resolver")
            }
            transformer.storeClass(classLike) {
                resolveClassLikeTypeParameters(classLike)
            }
        }

        if (resolveMode.resolveSupertypes) {
            transformer.storeClass(classLike) {
                withContainingDeclaration(classLike) {
                    resolveCallableMembers(classLike)
                }
            }

            transformer.statusComputationSession.endComputing(classLike)
        } else {
            transformer.statusComputationSession.computeOnlyDeclarationStatus(classLike)
        }
    }

    private inline fun <T : CfirCallableDeclaration> performResolveWithOverriddenCallables(
        target: T,
        getOverridden: (T) -> List<T>,
        crossinline transform: (T, List<T>) -> Unit,
    ) {
        if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase)) return

        val overriddenDeclarations = getOverridden(target)
        performCustomResolveUnderLock(target) {
            transform(target, overriddenDeclarations)
        }
    }

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        when (target) {
            is CfirClass -> error("should be resolved in doResolveWithoutLock")
            is CfirInterface -> error("should be resolved in doResolveWithoutLock")
            is CfirFile -> Unit
            else -> target.transformSingle(transformer, data = null)
        }
    }

    private class Transformer(statusComputationSession: CfirStatusComputationSession) :
        CfirStatusResolveTransformer(statusComputationSession) {
        override fun transformClass(klass: CfirClass, data: Nothing?): CfirClass {
            return klass
        }

        override fun transformInterface(interfaceDeclaration: CfirInterface, data: Nothing?): CfirInterface {
            return interfaceDeclaration
        }
    }
}
