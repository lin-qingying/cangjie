/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirGlobalResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTargetVisitor
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.session
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.tryCollectDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.withCfirDesignationEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirLockProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirPhaseUpdater
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.CfirElementFinder
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLFlightRecorder
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkPhase
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.canHaveDeferredReturnTypeCalculation
import org.cangnova.cangjie.cfir.correspondingProperty
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.originalIfFakeOverrideOrDelegated
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjPatternVariable

/**
 * This class represents the resolver for each [CfirResolvePhase].
 *
 * Usually such the resolver extends the corresponding compiler phase transformer or delegates to it.
 *
 * The main difference with original compiler transformers is that we can transform declarations
 * only under the lock of the declaration (see [LLCfirLockProvider] for locks implementation).
 * E.g., to avoid [contract violations][org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirLazyResolveContractChecker]
 * we cannot transform class member declaration under the class lock – we have to take the corresponding declaration lock
 * to avoid concurrent issues.
 *
 * So, at least we have a different implementation for transformations of such declarations as [CfirFile] and [CfirClass].
 *
 * Due to lazy resolution, we have to maintain the resolution order explicitly in some cases as we are not guaranteed by default that all
 * dependencies or outer declarations are resolved before the target one.
 * We have [resolveDependencies] method which describes common dependencies between declarations.
 * Also, each [LLCfirResolveTarget] can define phase-specific rules.
 *
 * Implementations:
 * - [MACRO_EXPAND][CfirResolvePhase.MACRO_EXPAND] – [LLCfirMacroExpandLazyResolver]
 * - [SUPER_TYPES][CfirResolvePhase.SUPER_TYPES] – [LLCfirSuperTypeTargetResolver]
 * - [TYPES][CfirResolvePhase.TYPES] – [LLCfirTypeTargetResolver]
 * - [STATUS][CfirResolvePhase.STATUS] – [LLCfirStatusTargetResolver]
 * - [EXTENSIONS][CfirResolvePhase.EXTENSIONS] – [LLCfirExtensionsTargetResolver]
 * - [IMPLICIT_TYPES][CfirResolvePhase.IMPLICIT_TYPES] – [LLCfirImplicitBodyTargetResolver]
 * - [BODY_RESOLVE][CfirResolvePhase.BODY_RESOLVE] – [LLCfirBodyTargetResolver]
 *
 * @see LLCfirLockProvider
 * @see CfirResolvePhase
 */
internal sealed class LLCfirTargetResolver(
    protected val resolveTarget: LLCfirResolveTarget,
    val resolverPhase: CfirResolvePhase,
) : LLCfirResolveTargetVisitor {
    val resolveTargetSession: LLCfirSession get() = resolveTarget.session
    val resolveTargetScopeSession: ScopeSession get() = resolveTargetSession.getScopeSession()
    private val lockProvider: LLCfirLockProvider get() = LLCfirGlobalResolveComponents.getInstance(resolveTargetSession).lockProvider
    private val requiresJumpingLock: Boolean get() = resolverPhase.isItAllowedToCallLazyResolveToTheSamePhase

    val containingDeclarations: List<CfirDeclaration>
        field = mutableListOf<CfirDeclaration>()

    /**
     * @param context used as a context in the case of exception
     * @return the last class-like declaration from [containingDeclarations]
     */
    fun containingClassLike(context: CfirDeclaration): CfirClassLikeDeclaration {
        val containingDeclaration = containingDeclarations.lastOrNull() ?: errorWithAttachment("Containing declaration is not found") {
            withCfirEntry("context", context)
            withCfirDesignationEntry("designation", resolveTarget.designation)
            context.tryCollectDesignation()?.let { withCfirDesignationEntry("calculatedDesignation", it) }
            withEntry("origin", context.origin.toString())
        }

        requireWithAttachment(
            containingDeclaration is CfirClassLikeDeclaration,
            { "${CfirClassLikeDeclaration::class.simpleName} expected, but ${containingDeclaration::class.simpleName} found" },
        ) {
            withCfirEntry("context", context)
            withCfirDesignationEntry("designation", resolveTarget.designation)
            context.tryCollectDesignation()?.let { withCfirDesignationEntry("calculatedDesignation", it) }
            withEntry("origin", context.origin.toString())
        }

        return containingDeclaration
    }

    /**
     * 基于当前解析栈查找最近的外围 class-like 声明。
     *
     * LL 解析路径总会先压入 file，再按需压入外层 class-like。
     * 对于非法源码或文件级错误恢复产物，constructor 可能并不真正位于 class-like 内。
     * 此时不能把 file 误当成 class-like 并强行报框架错误，而应仅在确有外围容器时建立该依赖。
     */
    fun containingClassLikeOrNull(): CfirClassLikeDeclaration? {
        return containingDeclarations.lastOrNull { it is CfirClassLikeDeclaration } as? CfirClassLikeDeclaration
    }

    protected inline fun withContainingDeclaration(declaration: CfirDeclaration, action: () -> Unit) {
        containingDeclarations += declaration
        try {
            action()
        } finally {
            val removed = containingDeclarations.removeLast()
            checkWithAttachment(removed === declaration, { "Unexpected state" }) {
                withCfirEntry("expected", declaration)
                withCfirEntry("actual", removed)
            }
        }
    }

    /**
     * Dependency target resolution can be skipped to optimize the resolution if this phase does not require any dependencies.
     *
     * For instance, [LLCfirBodyTargetResolver] skips it as no one should depend on body resolution of another declaration.
     *
     * @return **true** if [resolveDependencies] step should be skipped
     *
     * @see resolveDependencies
     */
    open val skipDependencyTargetResolutionStep: Boolean get() = false

    /**
     * Requests the resolution for dependencies to avoid race in the case of CFIR instance sharing.
     * Will be executed before resolution without a lock.
     *
     * @see skipDependencyTargetResolutionStep
     */
    private fun resolveDependencies(target: CfirElementWithResolveState) {
        if (skipDependencyTargetResolutionStep) return

        when {
            // Fake or delegate declaration shared types and annotations from the original one
            target is CfirCallableDeclaration && target.canHaveDeferredReturnTypeCalculation -> {
                val originalDeclaration = target.originalIfFakeOverrideOrDelegated()
                originalDeclaration?.lazyResolveToPhase(resolverPhase)
            }

            target is CfirProperty -> {
                // We share type references and annotations with the original parameter
                target.correspondingValueParameterFromPrimaryConstructor?.lazyResolveToPhase(resolverPhase)
            }

            target is CfirPatternBindingVariable -> {
                // Pattern binding variables share implicit type resolution with the owning pattern variable.
                target.owningPatternVariable?.lazyResolveToPhase(resolverPhase)
            }

            // constructor shares types inside delegation call with the containing class
            target is CfirConstructor -> {
                containingClassLikeOrNull()?.lazyResolveToPhase(resolverPhase)
            }

        }
    }

    final override fun withFile(cfirFile: CfirFile, action: () -> Unit) {
        withContainingDeclaration(cfirFile) {
            @Suppress("DEPRECATION_ERROR")
            withContainingFile(cfirFile, action)
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    protected open fun withContainingFile(cfirFile: CfirFile, action: () -> Unit) {
        action()
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withClassLike", level = DeprecationLevel.ERROR)
    protected open fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        action()
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withExtend", level = DeprecationLevel.ERROR)
    protected open fun withContainingExtend(cfirExtend: CfirExtend, action: () -> Unit) {
        action()
    }

    final override fun withClass(cfirClass: CfirClass, action: () -> Unit) {
        withClassLike(cfirClass, action)
    }

    final override fun withClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        withContainingDeclaration(cfirClassLike) {
            @Suppress("DEPRECATION_ERROR")
            withContainingClassLike(cfirClassLike, action)
        }
    }

    @Deprecated("Use withClassLike instead", level = DeprecationLevel.HIDDEN)
    protected fun withContainingClass(cfirClass: CfirClass, action: () -> Unit) {
        withContainingDeclaration(cfirClass) {
            @Suppress("DEPRECATION_ERROR")
            withContainingClassLike(cfirClass, action)
        }
    }

    final override fun withExtend(cfirExtend: CfirExtend, action: () -> Unit) {
        withContainingDeclaration(cfirExtend) {
            @Suppress("DEPRECATION_ERROR")
            withContainingExtend(cfirExtend, action)
        }
    }

    protected open fun checkResolveConsistency() {}

    /**
     * This method executes **not under the lock** of [target].
     * Any unsafe reads from [target] declaration have to be done under [withReadLock].
     * [performCustomResolveUnderLock] have to be used for modifications.
     *
     * This method can be useful to resolve some dependencies (like [resolveDependencies] in general),
     * but with some phase-specific rules.
     *
     * For instance, to pre-resolve [CfirClass] members before the class itself as it is required
     * to build the [CFG][org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph].
     *
     * @return **true** if [performCustomResolveUnderLock] has been called
     *
     * @see withReadLock
     * @see performCustomResolveUnderLock
     */
    protected open fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean = false

    /**
     * This method executes **under the lock** of [target].
     */
    protected abstract fun doLazyResolveUnderLock(target: CfirElementWithResolveState)

    /**
     * Executes the resolution.
     */
    fun resolveDesignation() {
        checkResolveConsistency()
        resolveTarget.visit(this)
    }

    final override fun performAction(element: CfirElementWithResolveState) {
        performResolve(element)
    }

    /**
     * Performs the resolution of [target].
     * The [target] element have to be at least in [resolverPhase].[previous][CfirResolvePhase.previous] phase.
     *
     * @see resolveDependencies
     * @see doResolveWithoutLock
     * @see doLazyResolveUnderLock
     */
    protected fun performResolve(target: CfirElementWithResolveState) {
        val event = LLFlightRecorder.phase(target, containingDeclarations, resolverPhase)

        try {
            resolveDependencies(target)

            if (doResolveWithoutLock(target)) {
                event?.notifyCompleted()
                return
            }

            if (requiresJumpingLock) {
                checkThatResolvedAtLeastToPreviousPhase(target)
                lockProvider.withJumpingLock(
                    target,
                    resolverPhase,
                    actionUnderLock = {
                        doLazyResolveUnderLock(target)
                        updatePhaseForDeclarationInternals(target)
                    },
                    actionOnCycle = {
                        handleCycleInResolution(target)
                    }
                )
            } else {
                performCustomResolveUnderLock(target) {
                    doLazyResolveUnderLock(target)
                }
            }

            event?.notifyCompleted()
        } catch (throwable: Throwable) {
            event?.notifyCompletedWithFailure(throwable)
            throw throwable
        }
    }

    /**
     * Will be executed in the case of detected cycle between elements during jumping resolve.
     *
     * **There is no guaranties that [target] is guarded by the lock of the current thread**
     *
     * @param target an element with detected cycle
     *
     * @see LLCfirLockProvider.withJumpingLock
     */
    protected open fun handleCycleInResolution(target: CfirElementWithResolveState) {
        errorWithCfirSpecificEntries("Resolution cycle is detected", cfir = target)
    }

    /**
     * Execute [action] under the write lock in the context of [target].
     *
     * Allowed only for non-jumping phases.
     *
     * @see requiresJumpingLock
     */
    protected inline fun performCustomResolveUnderLock(target: CfirElementWithResolveState, crossinline action: () -> Unit) {
        checkThatResolvedAtLeastToPreviousPhase(target)
        requireWithAttachment(!requiresJumpingLock, { "This function cannot be called with enabled jumping lock" }) {
            withCfirEntry("target", target)
        }

        lockProvider.withWriteLock(target, resolverPhase) {
            action()
            updatePhaseForDeclarationInternals(target)
        }
    }

    private fun updatePhaseForDeclarationInternals(target: CfirElementWithResolveState) {
        LLCfirPhaseUpdater.updateDeclarationContent(
            target = target,
            newPhase = resolverPhase,
        )
    }

    /**
     * Execute action under a declaration lock.
     * [action] will be executed only once in case of successful lock.
     * If some another thread is already resolved [target] declaration to [resolverPhase] then [action] won't be executed.
     */
    protected inline fun withReadLock(target: CfirElementWithResolveState, action: () -> Unit) {
        checkThatResolvedAtLeastToPreviousPhase(target)
        lockProvider.withReadLock(target, resolverPhase, action)
    }

    private fun checkThatResolvedAtLeastToPreviousPhase(target: CfirElementWithResolveState) {
        when (val previousPhase = resolverPhase.previous) {
            CfirResolvePhase.IMPORTS -> {}
            else -> {
                target.checkPhase(previousPhase)
            }
        }
    }
}

private val CfirProperty.correspondingValueParameterFromPrimaryConstructor: CfirValueParameter?
    get() {
        val ownerClassLike = symbol.callableId.classId?.let(moduleData.session.symbolProvider::getClassLikeSymbolByClassId)?.cfir as? CfirClassLikeDeclaration
            ?: return null
        val primaryConstructor = ownerClassLike.declarations.firstOrNull { it is CfirConstructor && it.isPrimary } as? CfirConstructor
            ?: return null
        return primaryConstructor.valueParameters.firstOrNull { it.correspondingProperty === this }
    }

private val CfirPatternBindingVariable.owningPatternVariable: CfirPatternVariable?
    get() {
        val containingFile = moduleData.session.cfirProvider.getContainingFile(symbol) ?: return null
        val bindingPsi = psi as? CjBindingPattern ?: return null
        val ownerPsi = PsiTreeUtil.getParentOfType(bindingPsi, CjPatternVariable::class.java, false) ?: return null
        return CfirElementFinder.findDeclaration(containingFile, ownerPsi) as? CfirPatternVariable
    }
