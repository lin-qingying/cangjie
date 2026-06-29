

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
 * 单个 [CfirResolvePhase] 的低阶目标解析器基类。
 *
 * 各阶段实现通常继承或委托对应的编译器阶段 transformer，但 low-level 解析必须按声明级锁粒度执行变更。
 * 与普通编译器 transformer 最大的差异是：不能在 class 锁下直接转换成员声明，而必须拿到成员自己的声明锁，避免并发修改和
 * lazy resolve 契约冲突。
 *
 * 懒解析还需要显式维护解析顺序，因为依赖声明或外层声明并不一定已经先于当前目标完成解析。[resolveDependencies] 描述通用声明依赖，
 * 每个 [LLCfirResolveTarget] 也可以通过 visitor 路径定义阶段特定规则。
 *
 * 支持的阶段实现包括 SUPER_TYPES、TYPES、STATUS、EXTENSIONS、IMPLICIT_TYPES 和 BODY_RESOLVE。
 *
 * @see LLCfirLockProvider
 * @see CfirResolvePhase
 */
internal sealed class LLCfirTargetResolver(
    /**
     * 当前 resolver 需要解析的 low-level 目标。
     */
    protected val resolveTarget: LLCfirResolveTarget,
    /**
     * 当前 resolver 负责推进到的 CFIR 解析阶段。
     */
    val resolverPhase: CfirResolvePhase,
) : LLCfirResolveTargetVisitor {
    /**
     * 当前解析目标所属的 low-level CFIR 会话。
     */
    val resolveTargetSession: LLCfirSession get() = resolveTarget.session
    /**
     * 当前会话的作用域会话。
     */
    val resolveTargetScopeSession: ScopeSession get() = resolveTargetSession.getScopeSession()
    /**
     * 当前会话的声明锁提供器。
     */
    private val lockProvider: LLCfirLockProvider get() = LLCfirGlobalResolveComponents.getInstance(resolveTargetSession).lockProvider
    /**
     * 当前阶段是否允许跳转式锁解析同一阶段依赖。
     */
    private val requiresJumpingLock: Boolean get() = resolverPhase.isItAllowedToCallLazyResolveToTheSamePhase

    /**
     * 解析 visitor 当前经过的外围声明栈。
     */
    val containingDeclarations: List<CfirDeclaration>
        field = mutableListOf<CfirDeclaration>()

    /**
     * 返回 [containingDeclarations] 中最近的 class-like 声明。
     *
     * @param context 异常附件中使用的上下文声明。
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

    /**
     * 在 [declaration] 作为当前外围声明的上下文中执行 [action]。
     */
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
     * 当前阶段是否可以跳过依赖目标解析步骤。
     *
     * BODY_RESOLVE 等阶段不会被其他声明依赖，可以跳过该步骤以减少额外 lazy resolve。
     *
     * @return 如果应跳过 [resolveDependencies] 则返回 `true`。
     *
     * @see resolveDependencies
     */
    open val skipDependencyTargetResolutionStep: Boolean get() = false

    /**
     * 解析 [target] 共享 CFIR 实例所依赖的声明，避免并发解析同一份类型或注解状态。
     *
     * 该步骤在无锁解析之前执行；阶段不需要依赖解析时可通过 [skipDependencyTargetResolutionStep] 跳过。
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

    /**
     * 以 [cfirFile] 为当前文件容器访问解析目标。
     */
    final override fun withFile(cfirFile: CfirFile, action: () -> Unit) {
        withContainingDeclaration(cfirFile) {
            @Suppress("DEPRECATION_ERROR")
            withContainingFile(cfirFile, action)
        }
    }

    /**
     * 在文件容器上下文中执行 [action]，供阶段子类覆写。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    protected open fun withContainingFile(cfirFile: CfirFile, action: () -> Unit) {
        action()
    }

    /**
     * 在 class-like 容器上下文中执行 [action]，供阶段子类覆写。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withClassLike", level = DeprecationLevel.ERROR)
    protected open fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        action()
    }

    /**
     * 在 extend 容器上下文中执行 [action]，供阶段子类覆写。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withExtend", level = DeprecationLevel.ERROR)
    protected open fun withContainingExtend(cfirExtend: CfirExtend, action: () -> Unit) {
        action()
    }

    /**
     * 以 [cfirClass] 作为 class-like 容器访问解析目标。
     */
    final override fun withClass(cfirClass: CfirClass, action: () -> Unit) {
        withClassLike(cfirClass, action)
    }

    /**
     * 以 [cfirClassLike] 作为当前 class-like 容器访问解析目标。
     */
    final override fun withClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        withContainingDeclaration(cfirClassLike) {
            @Suppress("DEPRECATION_ERROR")
            withContainingClassLike(cfirClassLike, action)
        }
    }

    /**
     * 兼容旧调用点的 class 容器入口。
     */
    @Deprecated("Use withClassLike instead", level = DeprecationLevel.HIDDEN)
    protected fun withContainingClass(cfirClass: CfirClass, action: () -> Unit) {
        withContainingDeclaration(cfirClass) {
            @Suppress("DEPRECATION_ERROR")
            withContainingClassLike(cfirClass, action)
        }
    }

    /**
     * 以 [cfirExtend] 作为当前 extend 容器访问解析目标。
     */
    final override fun withExtend(cfirExtend: CfirExtend, action: () -> Unit) {
        withContainingDeclaration(cfirExtend) {
            @Suppress("DEPRECATION_ERROR")
            withContainingExtend(cfirExtend, action)
        }
    }

    /**
     * 校验阶段子类内部使用的 transformer 与 [resolverPhase] 是否一致。
     */
    protected open fun checkResolveConsistency() {}

    /**
     * 在 [target] 的目标锁之外执行阶段特定的预解析。
     *
     * 如果需要不加目标锁地解析依赖或提前解析外围结构，可以覆写该方法。任何不安全读取必须通过 [withReadLock] 完成，
     * 对 [target] 的修改必须通过 [performCustomResolveUnderLock] 完成。
     *
     * @return 如果方法内部已经调用 [performCustomResolveUnderLock] 完成目标解析，则返回 `true`。
     *
     * @see withReadLock
     * @see performCustomResolveUnderLock
     */
    protected open fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean = false

    /**
     * 在 [target] 的目标锁内执行真实懒解析。
     */
    protected abstract fun doLazyResolveUnderLock(target: CfirElementWithResolveState)

    /**
     * 执行当前 designation 的解析。
     */
    fun resolveDesignation() {
        checkResolveConsistency()
        resolveTarget.visit(this)
    }

    /**
     * 对 visitor 当前命中的 [element] 执行解析。
     */
    final override fun performAction(element: CfirElementWithResolveState) {
        performResolve(element)
    }

    /**
     * 执行 [target] 的阶段解析。
     *
     * [target] 必须至少已经达到 [resolverPhase] 的前一阶段。
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
     * 在 jumping resolve 检测到元素间循环时调用。
     *
     * 不能假定 [target] 已由当前线程持有锁保护。
     *
     * @param target 检测到循环的目标元素。
     *
     * @see LLCfirLockProvider.withJumpingLock
     */
    protected open fun handleCycleInResolution(target: CfirElementWithResolveState) {
        errorWithCfirSpecificEntries("Resolution cycle is detected", cfir = target)
    }

    /**
     * 在 [target] 的写锁下执行 [action]。
     *
     * 该方法只允许非 jumping 阶段使用。
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

    /**
     * 将 [target] 内部声明内容推进到 [resolverPhase]。
     */
    private fun updatePhaseForDeclarationInternals(target: CfirElementWithResolveState) {
        LLCfirPhaseUpdater.updateDeclarationContent(
            target = target,
            newPhase = resolverPhase,
        )
    }

    /**
     * 在 [target] 的声明读锁下执行 [action]。
     *
     * 如果其他线程已经把 [target] 解析到 [resolverPhase]，则 [action] 不会再次执行。
     */
    protected inline fun withReadLock(target: CfirElementWithResolveState, action: () -> Unit) {
        checkThatResolvedAtLeastToPreviousPhase(target)
        lockProvider.withReadLock(target, resolverPhase, action)
    }

    /**
     * 校验 [target] 至少已经达到当前阶段的前一阶段。
     */
    private fun checkThatResolvedAtLeastToPreviousPhase(target: CfirElementWithResolveState) {
        when (val previousPhase = resolverPhase.previous) {
            CfirResolvePhase.IMPORTS -> {}
            else -> {
                target.checkPhase(previousPhase)
            }
        }
    }
}

/**
 * 返回属性对应的主构造参数。
 *
 * 主构造参数和属性会共享部分类型状态，懒解析属性前需要先推进该参数。
 */
private val CfirProperty.correspondingValueParameterFromPrimaryConstructor: CfirValueParameter?
    get() {
        val ownerClassLike = symbol.callableId.classId?.let(moduleData.session.symbolProvider::getClassLikeSymbolByClassId)?.cfir as? CfirClassLikeDeclaration
            ?: return null
        val primaryConstructor = ownerClassLike.declarations.firstOrNull { it is CfirConstructor && it.isPrimary } as? CfirConstructor
            ?: return null
        return primaryConstructor.valueParameters.firstOrNull { it.correspondingProperty === this }
    }

/**
 * 返回 pattern binding 变量所属的外层 pattern 变量。
 *
 * pattern binding 变量的隐式类型状态由所属 pattern 变量承载，解析前需要先推进 owner。
 */
private val CfirPatternBindingVariable.owningPatternVariable: CfirPatternVariable?
    get() {
        val containingFile = moduleData.session.cfirProvider.getContainingFile(symbol) ?: return null
        val bindingPsi = psi as? CjBindingPattern ?: return null
        val ownerPsi = PsiTreeUtil.getParentOfType(bindingPsi, CjPatternVariable::class.java, false) ?: return null
        return CfirElementFinder.findDeclaration(containingFile, ownerPsi) as? CfirPatternVariable
    }
