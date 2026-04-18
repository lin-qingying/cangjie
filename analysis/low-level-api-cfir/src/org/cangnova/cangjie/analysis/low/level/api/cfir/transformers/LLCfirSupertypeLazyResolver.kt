/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirSingleResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.asResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.tryCollectDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.withCfirDesignationEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkAnalysisReadiness
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkTypeRefIsResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.CfirSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.defaultType
import org.cangnova.cangjie.cfir.resolve.providers.firProvider
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSupertypeResolverVisitor
import org.cangnova.cangjie.cfir.resolve.transformers.SupertypeComputationSession
import org.cangnova.cangjie.cfir.resolve.transformers.SupertypeComputationStatus
import org.cangnova.cangjie.cfir.resolve.transformers.platformSupertypeUpdater
import org.cangnova.cangjie.cfir.symbols.impl.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirEntry

internal object LLCfirSupertypeLazyResolver : LLCfirLazyResolver(CfirResolvePhase.SUPER_TYPES) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirSuperTypeTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        when (target) {
            is CfirClass -> {
                for (superTypeRef in target.superTypeRefs) {
                    checkTypeRefIsResolved(superTypeRef, "class super type", target)
                }
            }

            is CfirTypeAlias -> {
                checkTypeRefIsResolved(target.expandedTypeRef, typeRefName = "type alias expanded type", target)
            }
        }
    }
}

/**
 * This resolver is responsible for [SUPER_TYPES][CfirResolvePhase.SUPER_TYPES] phase.
 *
 * This resolver:
 * - Transforms all supertypes of classes.
 * - Performs type aliases expansion.
 * - Breaks loops in the type hierarchy if needed.
 *
 * Special rules:
 * - Cfirst resolves outer classes to this phase.
 * - Resolves all super types recursively.
 * - [Searches][org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.LLCfirSuperTypeTargetResolver.crawlSupertype]
 *   super types not only in the declaration site session, but also in the call site session to resolve `expect` declaration first.
 *
 * @see CfirSupertypeResolverVisitor
 * @see CfirResolvePhase.SUPER_TYPES
 */
private class LLCfirSuperTypeTargetResolver(
    target: LLCfirResolveTarget,
    private val supertypeComputationSession: LLCfirSupertypeComputationSession = LLCfirSupertypeComputationSession(),
    private val visitedElements: MutableSet<CfirElementWithResolveState> = hashSetOf(),
) : LLCfirTargetResolver(target, CfirResolvePhase.SUPER_TYPES) {
    private val supertypeResolver = object : CfirSupertypeResolverVisitor(
        session = resolveTargetSession,
        supertypeComputationSession = supertypeComputationSession,
        scopeSession = resolveTargetScopeSession,
    ) {
        /**
         * We can do nothing here because at a call moment we've already resolved [outerClass]
         * because we resolve classes from top to down
         */
        override fun resolveAllSupertypesForOuterClass(outerClass: CfirClass) {
            // We can get into this function during a loop calculation, so it is possible that the result for [outerClass]
            // is not yet published, so we expect that this class was already visited or resolved
            if (outerClass !in visitedElements) {
                outerClass.asResolveTarget()?.let { resolveTarget ->
                    // It is possible in case of declaration collision,
                    // so we need this logic only to be sure that [outerClass] is resolved
                    resolveToSupertypePhase(resolveTarget)
                }

                LLCfirSupertypeLazyResolver.checkIsResolved(outerClass)
            }
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withContainingRegularClass(firClass: CfirRegularClass, action: () -> Unit) {
        doResolveWithoutLock(firClass)
        supertypeResolver.withClass(firClass) {
            action()
        }
    }

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        when (target) {
            is CfirRegularClass -> performResolve(
                declaration = target,
                superTypeRefsForTransformation = {
                    // We should create a copy of the original collection
                    // to avoid [ConcurrentModificationException] during another thread publication
                    ArrayList(target.superTypeRefs)
                },
                resolver = {
                    supertypeResolver.withClass(target) {
                        supertypeResolver.resolveSpecificClassLikeSupertypes(target, it, resolveRecursively = false)
                    }
                },
                superTypeUpdater = { superTypeRefs ->
                    val expandedTypeRefs = superTypeRefs.map { supertypeComputationSession.expandTypealiasInPlace(it, target.llCfirSession) }
                    target.replaceSuperTypeRefs(expandedTypeRefs)
                    resolveTargetSession.platformSupertypeUpdater?.updateSupertypesIfNeeded(target, resolveTargetScopeSession)
                },
            )
            is CfirTypeAlias -> performResolve(
                declaration = target,
                superTypeRefsForTransformation = { target.expandedTypeRef },
                resolver = { supertypeResolver.resolveTypeAliasSupertype(target, it, resolveRecursively = false) },
                superTypeUpdater = { superTypeRefs ->
                    val expandedTypeRef = supertypeComputationSession.expandTypealiasInPlace(superTypeRefs.single(), target.llCfirSession)
                    target.replaceExpandedTypeRef(expandedTypeRef)
                },
            )
            else -> {
                performCustomResolveUnderLock(target) {
                    // just update the phase
                }
            }
        }

        return true
    }

    /**
     * [superTypeRefsForTransformation] will be executed under [declaration] lock
     */
    private inline fun <T : CfirClassLikeDeclaration, S> performResolve(
        declaration: T,
        superTypeRefsForTransformation: () -> S,
        resolver: (S) -> List<CfirResolvedTypeRef>,
        crossinline superTypeUpdater: (List<CfirTypeRef>) -> Unit,
    ) {
        // To avoid redundant work, because a publication won't be executed
        if (checkAnalysisReadiness(declaration, containingDeclarations, resolverPhase)) return

        // After the readiness check to properly log information,
        // but the check itself is still required since performResolve might lead to SOE due to the outer class resolution
        if (declaration in visitedElements) return

        declaration.lazyResolveToPhase(resolverPhase.previous)

        var superTypeRefs: S? = null
        withReadLock(declaration) {
            superTypeRefs = superTypeRefsForTransformation()
        }

        // "null" means that some other thread is already resolved [declaration] to [resolverPhase]
        if (superTypeRefs == null) return

        // The declaration is marked as visited as soon as the real resolution has started.
        // Not early to not mark already resolved declarations as visited since they have to be processed separately
        visitedElements += declaration

        // 1. Resolve declaration super type refs
        @Suppress("UNCHECKED_CAST")
        val resolvedSuperTypeRefs = resolver(superTypeRefs as S)

        // 2. Resolve super declarations
        val status = supertypeComputationSession.getSupertypesComputationStatus(declaration)
        if (status is SupertypeComputationStatus.Computed) {
            crawlAllSupertypes(declaration, status.supertypeRefs)
        }

        // 3. Find loops
        val loopedSuperTypeRefs = supertypeComputationSession.findLoopFor(declaration)

        // 4. Get error type refs or already resolved
        val resultedTypeRefs = loopedSuperTypeRefs ?: resolvedSuperTypeRefs

        // 5. Publish the result
        performCustomResolveUnderLock(declaration) {
            superTypeUpdater(resultedTypeRefs)
        }
    }

    fun crawlAllSupertypes(declaration: CfirClassLikeDeclaration, superTypeRefs: List<CfirResolvedTypeRef>) {
        supertypeComputationSession.withDeclarationSession(declaration) {
            for (computedType in superTypeRefs) {
                crawlSupertype(computedType.coneType)
            }
        }
    }

    private fun resolveToSupertypePhase(target: LLCfirSingleResolveTarget) {
        LLCfirSuperTypeTargetResolver(
            target = target,
            supertypeComputationSession = supertypeComputationSession,
            visitedElements = visitedElements,
        ).resolveDesignation()

        LLCfirSupertypeLazyResolver.checkIsResolved(target.target)
    }

    /**
     * We want to apply resolved supertypes to as many designations as possible.
     * So we crawl the resolved supertypes of visited designations to find more designations to collect.
     */
    private fun crawlSupertype(type: ConeKotlinType) {
        // Resolution order: from declaration site to use site
        for (session in supertypeComputationSession.useSiteSessions.asReversed()) {
            /**
             * We can avoid deduplication here as the symbol will be checked with [visitedElements]
             */
            type.toSymbol(session)?.let(::crawlSupertype)
        }

        if (type is ConeClassLikeType) {
            // The `classLikeDeclaration` is not associated with a file, and thus there is no need to resolve it, but it may still point
            // to declarations via its type arguments which need to be collected and have a containing file.
            // For example, a `Function1` could point to a type alias.
            type.typeArguments.forEach { it.type?.let(::crawlSupertype) }
        }
    }

    private fun crawlSupertype(symbol: CfirClassifierSymbol<*>) {
        val classLikeDeclaration = symbol.fir
        if (classLikeDeclaration !is CfirClassLikeDeclaration) return
        if (classLikeDeclaration in visitedElements) return

        val resolveTarget = classLikeDeclaration.asResolveTarget()
        if (resolveTarget != null) {
            resolveToSupertypePhase(resolveTarget)
        }

        /**
         * [resolveToSupertypePhase] doesn't guarantee that the declaration is checked since at that moment it might
         * be already resolved, so the explicit traversal is required.
         * [visitedElements] guarantees that the declaration is present in the set only if it wasn't resolved yet
         */
        val crawlIsRequired = visitedElements.add(classLikeDeclaration)
        if (crawlIsRequired && classLikeDeclaration.resolvePhase >= resolverPhase) {
            crawlSupertypeFromResolvedDeclaration(classLikeDeclaration)
        }
    }

    /**
     * We should process [classLikeDeclaration] with an assumption that there are some unresolved supertypes from
     * the declaration site point of view since the phase provide guarantees only for Kotlin classes that are entry points to the hierarchy.
     * In other words, classes for which [lazyResolveToPhase] with [CfirResolvePhase.SUPER_TYPES] was called
     */
    private fun crawlSupertypeFromResolvedDeclaration(classLikeDeclaration: CfirClassLikeDeclaration) {
        supertypeComputationSession.withDeclarationSession(classLikeDeclaration) {
            val parentClass = classLikeDeclaration.outerClass()
            if (parentClass != null) {
                crawlSupertype(parentClass.defaultType())
            }

            val superTypeRefs = when (classLikeDeclaration) {
                is CfirTypeAlias -> listOf(classLikeDeclaration.expandedTypeRef)
                is CfirClass -> classLikeDeclaration.superTypeRefs
            }

            for (typeRef in superTypeRefs) {
                val coneType = typeRef.coneTypeOrNull ?: errorWithCfirSpecificEntries(
                    "The declaration super type must be resolved, but the actual type reference is not resolved",
                    fir = classLikeDeclaration,
                ) {
                    withCfirEntry("typeRef", typeRef)
                }

                crawlSupertype(coneType)
            }
        }
    }

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        error("Should be resolved without lock in ${::doResolveWithoutLock.name}")
    }
}

private fun CfirClassLikeDeclaration.asResolveTarget(): LLCfirSingleResolveTarget? = tryCollectDesignation()?.asResolveTarget()

private fun CfirClassLikeDeclaration.outerClass(): CfirRegularClass? =
    llCfirSession.firProvider.getContainingClass(symbol)?.fir as? CfirRegularClass

private open class LLCfirSupertypeComputationSession(
    useSiteSessions: PersistentList<LLCfirSession> = persistentListOf(),
) : SupertypeComputationSession() {
    var useSiteSessions: PersistentList<LLCfirSession> = useSiteSessions
        private set

    inline fun withDeclarationSession(declaration: CfirClassLikeDeclaration, action: () -> Unit) {
        val newSession = declaration.llCfirSession.takeUnless { it == useSiteSessions.lastOrNull() }
        try {
            newSession?.let { useSiteSessions = useSiteSessions.add(it) }
            action()
        } finally {
            newSession?.let { useSiteSessions = useSiteSessions.removeAt(useSiteSessions.lastIndex) }
        }
    }

    /**
     * These collections exist to reuse a collection for each search to avoid repeated memory allocation.
     * Can be replaced with a new collection on each invocation of [findLoopFor]
     */
    private val visited: MutableSet<CfirClassLikeDeclaration> = hashSetOf()
    private val looped: MutableSet<CfirClassLikeDeclaration> = hashSetOf()
    private val pathOrderedSet: LinkedHashSet<CfirClassLikeDeclaration> = LinkedHashSet()
    // ---------------

    /**
     * Map from [CfirClassLikeDeclaration] to [List<CfirResolvedTypeRef>>],
     * where the list contains at least one [CfirErrorTypeRef] for looped references
     */
    private val updatedTypesForDeclarationsWithLoop: MutableMap<CfirClassLikeDeclaration, List<CfirResolvedTypeRef>> = hashMapOf()

    /**
     * @param declaration declaration to be checked for loops
     * @return list of resolved super type refs if at least one of them is [CfirErrorTypeRef] due to cycle hierarchy
     */
    fun findLoopFor(declaration: CfirClassLikeDeclaration): List<CfirResolvedTypeRef>? {
        breakLoopFor(
            declaration = declaration,
            // Only loops from the declaration site point of view should be processed
            session = declaration.llCfirSession,
            visited = visited,
            looped = looped,
            pathOrderedSet = pathOrderedSet,
            // LL resolver only works for non-local declarations
            localClassesNavigationInfo = null,
        )

        visited.clear()
        looped.clear()
        return updatedTypesForDeclarationsWithLoop[declaration]
    }

    /**
     * We shouldn't try to iterate over unresolved class. Otherwise, it can lead to [ConcurrentModificationException]
     */
    override fun getResolvedSuperTypeRefsForOutOfSessionDeclaration(
        classLikeDeclaration: CfirClassLikeDeclaration,
        useSiteSession: CfirSession,
    ): List<CfirResolvedTypeRef> {
        if (classLikeDeclaration.resolvePhase < CfirResolvePhase.SUPER_TYPES) return emptyList()

        return super.getResolvedSuperTypeRefsForOutOfSessionDeclaration(classLikeDeclaration, useSiteSession)
    }

    /**
     * It is possible that one of super type refs were already reported as an error, but the second – not.
     * So in this case, we want to save already reported errors and add a new one.
     * Example:
     * ```
     * interface B : A, ResolveMe {}
     * interface C : B {}
     * interface D : B {}
     * interface ResolveMe<caret> : F {}
     * // D will be marked as error during ResolveMe->F->D->B->ResolveMe round.
     * // And we will back to super type refs of class F during the resolution of class C
     * interface F : D, C {}
     * interface NonLoopedInterface : C
     * ```
     */
    override fun reportLoopErrorRefs(classLikeDeclaration: CfirClassLikeDeclaration, supertypeRefs: List<CfirResolvedTypeRef>) {
        updatedTypesForDeclarationsWithLoop.merge(classLikeDeclaration, supertypeRefs) { oldRefs, newRefs ->
            buildList(oldRefs.size) {
                for ((old, new) in oldRefs.zip(newRefs)) {
                    if (old is CfirErrorTypeRef) {
                        add(old)
                    } else {
                        add(new)
                    }
                }
            }
        }
    }
}

/**
 * This session is designed to be used during local classes resolution to
 * properly handle non-local classes in the hierarchy to not modify them by the CLI transformer.
 *
 * It is not enough to just resolve non-local classes to [CfirResolvePhase.SUPER_TYPES] due to classpath substitution.
 * Such non-local classes have to be processed via [LLCfirSuperTypeTargetResolver.crawlAllSupertypes] in the context of local use site.
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
 *     fun baz()
 * }
 *
 * fun test() {
 *     abstract class FooImpl : Foo() {
 *     }
 * }
 * ```
 *
 * In this case, the entire hierarchy of `Foo` has to be resolved with the local use site session.
 *
 * @see LLCfirSupertypeComputationSession
 * @see org.cangnova.cangjie.cfir.resolve.transformers.runSupertypeResolvePhaseForLocalClass
 */
internal class LLSupertypeComputationSessionLocalClassesAware : SupertypeComputationSession() {
    override fun getResolvedSuperTypeRefsForOutOfSessionDeclaration(
        classLikeDeclaration: CfirClassLikeDeclaration,
        useSiteSession: CfirSession,
    ): List<CfirResolvedTypeRef> {
        if (!classLikeDeclaration.isLocal) {
            resolveToSupertypePhase(classLikeDeclaration)
        }

        return super.getResolvedSuperTypeRefsForOutOfSessionDeclaration(classLikeDeclaration, useSiteSession)
    }

    /**
     * The non-empty result means the compiler will iterate through the supertypes
     * and resolve them in the context of [useSiteSession].
     * In LL it would work even for non-local declarations since this session catches all supertypes
     * usages and resolves them via [supertypeRefsWithLazyResolve], so the compiler performs no real resolution,
     * it only triggers it
     *
     * @see resolveToSupertypePhase
     * */
    override fun supertypeRefs(
        declaration: CfirClassLikeDeclaration,
        useSiteSession: CfirSession,
    ): List<CfirTypeRef> = if (declaration.isLocal) {
        super.supertypeRefs(declaration, useSiteSession)
    } else {
        val resolvedTypeRefs = supertypeRefsWithLazyResolve(declaration, useSiteSession)
        // If the session is the same, we could optimize the traversal a bit by cutting the supertypes graph.
        // This is valid since the compiler won't find new cases if the declaration was already processed in this context
        resolvedTypeRefs.takeIf { declaration.llCfirSession != useSiteSession }.orEmpty()
    }

    /**
     * No need to care about the local use-site session view since the compiler will
     * trigger resolution of the hierarchy from this perspective once [supertypeRefs] returns a non-empty list.
     *
     * @see supertypeRefs
     */
    private fun resolveToSupertypePhase(declaration: CfirClassLikeDeclaration) {
        declaration.lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
    }

    private fun supertypeRefsWithLazyResolve(
        declaration: CfirClassLikeDeclaration,
        useSiteSession: CfirSession,
    ): List<CfirResolvedTypeRef> = when (val status = getSupertypesComputationStatus(declaration)) {
        is SupertypeComputationStatus.Computed -> status.supertypeRefs

        SupertypeComputationStatus.Computing -> {
            val designation = declaration.asResolveTarget()?.designation
            errorWithCfirSpecificEntries(
                "Unexpected uncomputed declaration" + if (designation == null) " (no designation)" else "",
                fir = declaration,
            ) {
                designation?.let { withCfirDesignationEntry("designation.txt", it) }
            }
        }

        SupertypeComputationStatus.NotComputed -> {
            startComputingSupertypes(declaration)

            resolveToSupertypePhase(declaration)

            val resolvedTypesRefs = super.getResolvedSuperTypeRefsForOutOfSessionDeclaration(declaration, useSiteSession)

            // Resolved references have to be stored in the session so the compiler logic would be able to use them.
            // Otherwise, the compiler will see the empty list from `supertypeRefs` and will treat it as a supertypes list.
            // Also, the compiler resolver might try to modify the already resolved non-local declaration without locks
            // if no resolved status is present
            storeSupertypes(declaration, resolvedTypesRefs)
            resolvedTypesRefs
        }
    }
}
