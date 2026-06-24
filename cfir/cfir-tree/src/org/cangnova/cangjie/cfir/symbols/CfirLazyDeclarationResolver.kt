package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.util.PrivateForInline

/**
 * 将 [CfirBasedSymbol] 懒解析到指定 [CfirResolvePhase]。
 *
 * 在 Analysis API / IDE lazy resolve 场景中，该入口保证声明阶段至少达到 [toPhase]；
 * 若当前阶段不足，则调用 session 中注册的 [CfirLazyDeclarationResolver] 推进声明。
 *
 * 若在 CFIR transformer 内调用该入口，目标阶段必须严格低于当前 transformer 处理阶段，
 * 否则可能形成递归解析、死锁或栈溢出。
 *
 * 在普通编译器批处理模式中，默认 resolver 是 no-op，因为编译器按阶段整体推进。
 *
 * @receiver 需要推进声明阶段的符号。
 * @param toPhase 调用完成后声明至少应达到的阶段。
 */
fun CfirBasedSymbol<*>.lazyResolveToPhase(toPhase: CfirResolvePhase) {
    cfir.lazyResolveToPhase(toPhase)
}

/**
 * 将带 resolve state 的 CFIR 元素懒解析到指定阶段。
 *
 * @see lazyResolveToPhase
 */
fun CfirElementWithResolveState.lazyResolveToPhase(toPhase: CfirResolvePhase) {
    invokeLazyResolveToPhase(toPhase, CfirLazyDeclarationResolver::lazyResolveToPhase)
}

/**
 * 将类声明及其 callable 成员一起懒解析到指定阶段。
 */
fun CfirClass.lazyResolveToPhaseWithCallableMembers(toPhase: CfirResolvePhase) {
    invokeLazyResolveToPhase(toPhase, CfirLazyDeclarationResolver::lazyResolveToPhaseWithCallableMembers)
}

/**
 * 递归推进元素及其可递归子声明的懒解析阶段。
 */
fun CfirElementWithResolveState.lazyResolveToPhaseRecursively(toPhase: CfirResolvePhase) {
    invokeLazyResolveToPhase(toPhase, CfirLazyDeclarationResolver::lazyResolveToPhaseRecursively)
}

/**
 * 通过当前元素所属 session 的 resolver 执行懒解析。
 */
private fun CfirElementWithResolveState.invokeLazyResolveToPhase(
    toPhase: CfirResolvePhase,
    resolver: CfirLazyDeclarationResolver.(CfirElementWithResolveState, CfirResolvePhase) -> Unit,
) {
    when (this) {


        else -> lazyDeclarationResolver.resolver(this, toPhase)
    }
}

/**
 * 通过类声明所属 session 的 resolver 执行类专用懒解析。
 */
private fun CfirClass.invokeLazyResolveToPhase(
    toPhase: CfirResolvePhase,
    resolver: CfirLazyDeclarationResolver.(CfirClass, CfirResolvePhase) -> Unit,
) {
    lazyDeclarationResolver.resolver(this, toPhase)
}

/**
 * 当前元素所属 session 中注册的 lazy declaration resolver。
 */
private val CfirElementWithResolveState.lazyDeclarationResolver get() = moduleData.session.lazyDeclarationResolver

/**
 * 当前 session 的 lazy declaration resolver 组件。
 */
val CfirSession.lazyDeclarationResolver: CfirLazyDeclarationResolver by CfirSession.sessionComponentAccessor()

/**
 * 单个 [CfirSession] 内的 lazy declaration resolver 抽象。
 *
 * 该组件负责推进声明阶段、记录当前活动阶段、检查 lazy resolve 是否允许发生，
 * 并提供编译器批处理模式与 IDE lazy 模式之间的统一入口。
 */
abstract class CfirLazyDeclarationResolver : CfirSessionComponent {
    /**
     * lazy resolve 契约检查开关。
     */
    @PrivateForInline
    @Suppress("PropertyName")
    val _lazyResolveContractChecksEnabled: ThreadLocal<Boolean> = ThreadLocal.withInitial { true }

    /**
     * 标记某个 resolve phase 开始执行。
     */
    abstract fun startResolvingPhase(phase: CfirResolvePhase)

    /**
     * 标记某个 resolve phase 执行结束。
     */
    abstract fun finishResolvingPhase(phase: CfirResolvePhase)


    /**
     * 当前线程是否允许触发 lazy resolve。
     */
    @PrivateForInline
    @Suppress("PropertyName")
    val _lazyResolveIsAllowed: ThreadLocal<Boolean> = ThreadLocal.withInitial { true }

    /**
     * 将指定元素懒解析到 [toPhase]。
     */
    abstract fun lazyResolveToPhase(element: CfirElementWithResolveState, toPhase: CfirResolvePhase)

    /**
     * 将类声明及其 callable 成员懒解析到 [toPhase]。
     */
    open fun lazyResolveToPhaseWithCallableMembers(clazz: CfirClass, toPhase: CfirResolvePhase) {
        lazyResolveToPhase(clazz, toPhase)
    }

    /**
     * 递归推进元素及其子声明到 [toPhase]。
     */
    open fun lazyResolveToPhaseRecursively(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        lazyResolveToPhase(element, toPhase)
    }

    /**
     * 在 [action] 执行期间禁止触发 lazy resolve。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> forbidLazyResolveInside(action: () -> T): T {
        val current = _lazyResolveIsAllowed.get()
        _lazyResolveIsAllowed.set(false)
        try {
            return action()
        } finally {
            _lazyResolveIsAllowed.set(current)
        }
    }

    /**
     * 若当前线程禁止 lazy resolve，则抛出 [CfirLazyResolveForbiddenException]。
     */
    @OptIn(PrivateForInline::class)
    protected fun assertLazyResolveAllowed() {
        if (!_lazyResolveIsAllowed.get()) {
            throw CfirLazyResolveForbiddenException()
        }
    }

    /**
     * 在 [action] 执行期间关闭 lazy resolve 契约检查。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> disableLazyResolveContractChecksInside(action: () -> T): T {
        val current = _lazyResolveContractChecksEnabled.get()
        _lazyResolveContractChecksEnabled.set(false)
        try {
            return action()
        } finally {
            _lazyResolveContractChecksEnabled.set(current)
        }
    }


}

/**
 * 在禁止 lazy resolve 的区域触发 lazy resolve 时抛出的异常。
 */
class CfirLazyResolveForbiddenException : IllegalStateException("Lazy resolve is forbidden")

/**
 * 编译器批处理模式使用的 no-op lazy declaration resolver。
 */
object CfirDummyCompilerLazyDeclarationResolver : CfirLazyDeclarationResolver() {
    /**
     * 批处理模式不记录单独阶段开始。
     */
    override fun startResolvingPhase(phase: CfirResolvePhase) {}

    /**
     * 批处理模式不记录单独阶段结束。
     */
    override fun finishResolvingPhase(phase: CfirResolvePhase) {}

    /**
     * 批处理模式中声明已由外层 phase runner 推进，这里不执行额外动作。
     */
    override fun lazyResolveToPhase(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {}

    /**
     * 批处理模式中 callable 成员已由外层 phase runner 处理。
     */
    override fun lazyResolveToPhaseWithCallableMembers(clazz: CfirClass, toPhase: CfirResolvePhase) {}

    /**
     * 批处理模式中递归解析由外层 phase runner 控制。
     */
    override fun lazyResolveToPhaseRecursively(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {}
}

