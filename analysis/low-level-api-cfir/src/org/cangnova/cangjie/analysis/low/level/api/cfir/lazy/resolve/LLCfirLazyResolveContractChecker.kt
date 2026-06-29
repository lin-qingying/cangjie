

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.isItAllowedToCallLazyResolveTo

/**
 * 检查 lazy resolve 调用是否满足 [CfirResolvePhase] 的阶段契约。
 *
 * 对齐 Kotlin `LLFirLazyResolveContractChecker`：
 * 当前线程进入某个 lazy resolve 阶段后，后续嵌套请求只能落在该阶段允许访问的范围内。
 */
internal class LLCfirLazyResolveContractChecker {
    /**
     * 当前线程正在执行的 lazy resolve transformer 阶段。
     *
     * 使用 [ThreadLocal] 是因为 lazy resolve 可以在不同读动作线程中并发发生，而阶段契约只约束当前调用栈。
     */
    private val currentTransformerPhase = ThreadLocal.withInitial<CfirResolvePhase?> { null }

    /**
     * 在 [phase] 阶段上下文中执行 [resolve]，并检查嵌套 lazy resolve 是否满足阶段访问规则。
     *
     * 方法会在退出时恢复进入前的阶段，因此允许合法的嵌套解析调用。
     */
    inline fun lazyResolveToPhaseInside(phase: CfirResolvePhase, resolve: () -> Unit) {
        checkIfCanLazyResolveToPhase(phase)

        val previousPhase = currentTransformerPhase.get()
        currentTransformerPhase.set(phase)
        try {
            resolve()
        } finally {
            currentTransformerPhase.set(previousPhase)
        }
    }

    /**
     * 检查当前阶段是否允许请求 [requestedPhase]。
     *
     * 如果系统属性 `cangjie.suppress.lazy.resolve.contract.violation` 存在，违规只记录警告；
     * 否则直接抛出 [CfirLazyResolveContractViolationException]。
     */
    private fun checkIfCanLazyResolveToPhase(requestedPhase: CfirResolvePhase) {
        val currentPhase = currentTransformerPhase.get() ?: return

        if (!currentPhase.isItAllowedToCallLazyResolveTo(requestedPhase)) {
            val exception = CfirLazyResolveContractViolationException(
                currentPhase = currentPhase,
                requestedPhase = requestedPhase,
            )
            if (System.getProperty("cangjie.suppress.lazy.resolve.contract.violation") != null) {
                LoggerHolder.LOG.warn(exception)
            } else {
                throw exception
            }
        }
    }

    /**
     * 延迟初始化 logger 的持有者，避免在无违规路径上提前创建日志实例。
     */
    private object LoggerHolder {
        /**
         * lazy resolve 契约违规日志实例。
         */
        val LOG = Logger.getInstance(LLCfirLazyResolveContractChecker::class.java)
    }
}

/**
 * 表示 lazy resolve 阶段契约被破坏的异常。
 *
 * @param currentPhase 当前线程正在执行的 lazy resolve 阶段。
 * @param requestedPhase 嵌套请求试图进入的目标阶段。
 */
internal class CfirLazyResolveContractViolationException(
    /**
     * 当前线程正在执行的 lazy resolve 阶段。
     */
    val currentPhase: CfirResolvePhase,

    /**
     * 嵌套请求试图进入的目标阶段。
     */
    val requestedPhase: CfirResolvePhase,
) : IllegalStateException("Lazy resolve contract violated: current=$currentPhase requested=$requestedPhase")
