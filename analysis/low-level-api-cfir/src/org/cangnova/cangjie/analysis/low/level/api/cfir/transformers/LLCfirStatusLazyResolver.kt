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
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkDeclarationStatusIsResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkAnalysisReadiness
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.CfirSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.classId
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.resolve.ScopeSession
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.resolve.transformers.CfirStatusResolveTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.StatusComputationSession
import org.cangnova.cangjie.cfir.symbols.impl.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhaseWithCallableMembers
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.utils.SmartSet

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
            )
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

/**
 * This session is designed to be used during local classes resolution to
 * properly handle non-local classes in the hierarchy to not modify them by the CLI transformer.
 *
 * It is not enough to just resolve non-local classes to [CfirResolvePhase.STATUS] due to classpath substitution.
 * Such non-local classes have to be processed via [LLStatusComputationSession.forceResolveStatusesOfSupertypes]
 * in the context of local use site.
 *
 * ### Example:
 * ```kotlin
 * // MODULE: dependency
 * // FILE: dependency.kt
 * package org.example
 *
 * interface Base
 *
 * abstract class Foo : Base
 *
 * // MODULE: usage(dependency)
 * // FILE: usage.kt
 * package org.example
 *
 * interface Base {
 *     fun bar()
 * }
 *
 * fun test() {
 *     abstract class FooImpl : Foo() {
 *         override fun bar() {}
 *     }
 * }
 * ```
 *
 * In this case, the entire hierarchy of `Foo` has to be resolved with the local use site session.
 *
 * @see LLStatusComputationSession
 * @see org.cangnova.cangjie.cfir.resolve.transformers.runStatusResolveForLocalClass
 */
internal class LLStatusComputationSessionLocalClassesAware(
    useSiteSession: CfirSession,
    useSiteScopeSession: ScopeSession,
) : StatusComputationSession(useSiteSession, useSiteScopeSession) {
    override fun resolveClassForSuperType(regularClass: CfirRegularClass): Boolean = if (regularClass.isLocal) {
        super.resolveClassForSuperType(regularClass)
    } else {
        // 1. Resolve the entire hierarchy for the non-local class (in the declaration-site context)
        regularClass.lazyResolveToPhaseWithCallableMembers(CfirResolvePhase.STATUS)

        val statusComputationSession = LLStatusComputationSession(
            useSiteSession as LLCfirSession,
            useSiteScopeSession,
            StatusResolveMode.AllCallables,
        )

        // 2. Resolve the entire hierarchy for the non-local class (in the use-site context)
        statusComputationSession.forceResolveStatusesOfSupertypes(regularClass)
        true
    }
}

private class LLStatusComputationSession(
    useSiteSession: LLCfirSession,
    useSiteScopeSession: ScopeSession,
    val resolveMode: StatusResolveMode,
) : StatusComputationSession(useSiteSession, useSiteScopeSession) {
    private val useSiteSessions: MutableList<LLCfirSession> = mutableListOf(useSiteSession)

    private inline fun withClassSession(regularClass: CfirClass, action: () -> Unit) {
        val newSession = regularClass.llCfirSession.takeUnless { it == useSiteSessions.lastOrNull() }
        try {
            newSession?.let(useSiteSessions::add)
            action()
        } finally {
            newSession?.let { useSiteSessions.removeLast() }
        }
    }

    override fun forceResolveStatusesOfSupertypes(regularClass: CfirClass) {
        withClassSession(regularClass) {
            super.forceResolveStatusesOfSupertypes(regularClass)
        }
    }

    override fun superTypeToSymbols(typeRef: CfirTypeRef): Collection<CfirClassifierSymbol<*>> {
        val type = typeRef.coneType
        return SmartSet.create<CfirClassifierSymbol<*>>().apply {
            // Resolution order: from declaration site to use site
            for (useSiteSession in useSiteSessions.asReversed()) {
                type.toSymbol(useSiteSession)?.let(::add)
            }
        }
    }

    override fun resolveClassForSuperType(regularClass: CfirRegularClass): Boolean {
        val target = regularClass.tryCollectDesignation()?.asResolveTarget() ?: return false
        val resolver = LLCfirStatusTargetResolver(
            target,
            resolveMode = resolveMode,
            this,
        )

        resolver.resolveDesignation()
        return true
    }

    override fun additionalSuperTypes(regularClass: CfirClass): List<CfirTypeRef> = emptyList()
}

/**
 * This resolver is responsible for [STATUS][CfirResolvePhase.STATUS] phase.
 *
 * This resolver:
 * - Transforms modality, visibility, and modifiers for [member declarations][CfirMemberDeclaration].
 *
 * Special rules:
 * - Cfirst resolves outer classes to this phase.
 * - Cfirst resolves all members of super types for non-[CfirClassLikeDeclaration] declarations.
 * - [Searches][org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.LLStatusComputationSession.superTypeToSymbols]
 *   super types not only in the declaration site session, but also in the call site session to resolve `expect` declaration first.
 *
 * @see CfirStatusResolveTransformer
 * @see CfirResolvePhase.STATUS
 */
private class LLCfirStatusTargetResolver(
    target: LLCfirResolveTarget,
    private val resolveMode: StatusResolveMode,
    statusComputationSession: LLStatusComputationSession,
) : LLCfirTargetResolver(target, CfirResolvePhase.STATUS) {
    private val transformer = Transformer(statusComputationSession)

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withContainingRegularClass(firClass: CfirRegularClass, action: () -> Unit) {
        doResolveWithoutLock(firClass)
        transformer.storeClass(firClass) {
            action()
            firClass
        }

        transformer.statusComputationSession.endComputing(firClass)
    }

    private fun resolveClassTypeParameters(klass: CfirClass) {
        klass.typeParameters.forEach { it.transformSingle(transformer, data = null) }
    }

    private fun resolveCallableMembers(klass: CfirClass) {
        for (member in klass.declarations) {
            if (member !is CfirCallableDeclaration || !resolveMode.shouldBeResolved(member)) continue

            // we need the types to be resolved here to compute visibility
            // implicit types will not be handled (see bug in the compiler KT-55446)
            member.lazyResolveToPhase(resolverPhase.previous)
            performResolve(member)
        }
    }

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean = when (target) {
        is CfirRegularClass -> {
            if (transformer.statusComputationSession[target].requiresComputation) {
                target.lazyResolveToPhase(resolverPhase.previous)
                resolveClass(target)
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

        is CfirProperty -> {
            performResolveWithOverriddenCallables(
                target,
                { transformer.statusResolver.getOverriddenProperties(it, transformer.containingClass) },
                { element, overridden -> transformer.transformProperty(element, overridden) },
            )

            true
        }

        else -> false
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
            is CfirRegularClass -> error("should be resolved in doResolveWithoutLock")
            is CfirFile -> {}
            else -> target.transformSingle(transformer, data = null)
        }
    }

    private fun resolveClass(firClass: CfirRegularClass) {
        transformer.statusComputationSession.startComputing(firClass)

        if (resolveMode.resolveSupertypes) {
            transformer.statusComputationSession.forceResolveStatusesOfSupertypes(firClass)
        }

        performCustomResolveUnderLock(firClass) {
            transformer.transformClassStatus(firClass)
            transformer.storeClass(firClass) {
                resolveClassTypeParameters(firClass)
                firClass
            }
        }

        if (resolveMode.resolveSupertypes) {
            transformer.storeClass(firClass) {
                withContainingDeclaration(firClass) {
                    resolveCallableMembers(firClass)
                }

                firClass
            }

            transformer.statusComputationSession.endComputing(firClass)
        } else {
            transformer.statusComputationSession.computeOnlyClassStatus(firClass)
        }
    }

    private class Transformer(statusComputationSession: LLStatusComputationSession) :
        CfirStatusResolveTransformer(statusComputationSession) {
        override fun CfirDeclaration.needResolveMembers(): Boolean = false
        override fun CfirDeclaration.needResolveNestedClassifiers(): Boolean = false

        override fun transformClass(klass: CfirClass, data: CfirResolvedDeclarationStatus?): CfirStatement {
            return klass
        }
    }
}
