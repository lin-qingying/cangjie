package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.isItAllowedToCallLazyResolveTo
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.util.PrivateForInline

/**
 * 表示 `CfirCompilerLazyDeclarationResolverWithPhaseChecking`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirCompilerLazyDeclarationResolverWithPhaseChecking : CfirLazyDeclarationResolver() {
    /**
     * 维护 `currentTransformerPhase`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private var currentTransformerPhase: CfirResolvePhase? = null

    /**
     * 保存 `exceptions`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val exceptions = mutableListOf<CfirLazyResolveContractViolationException>()

    /**
     * 执行 `getContractViolationExceptions` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    fun getContractViolationExceptions(): List<CfirLazyResolveContractViolationException> = exceptions

    /**
     * 执行 `lazyResolveToPhase` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun lazyResolveToPhase(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase, element.resolvePhase)
    }

    /**
     * 执行 `lazyResolveToPhaseWithCallableMembers` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun lazyResolveToPhaseWithCallableMembers(clazz: CfirClass, toPhase: CfirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase, clazz.resolvePhase)
    }

    /**
     * 执行 `lazyResolveToPhaseRecursively` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun lazyResolveToPhaseRecursively(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase, element.resolvePhase)
    }

    /**
     * 执行 `startResolvingPhase` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun startResolvingPhase(phase: CfirResolvePhase) {
        check(currentTransformerPhase == null)
        currentTransformerPhase = phase
    }

    /**
     * 执行 `finishResolvingPhase` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun finishResolvingPhase(phase: CfirResolvePhase) {
        check(currentTransformerPhase == phase)
        currentTransformerPhase = null
    }

    /**
     * 提供 `checkIfCanLazyResolveToPhase` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    @OptIn(PrivateForInline::class)
    private fun checkIfCanLazyResolveToPhase(requestedPhase: CfirResolvePhase, elementPhase: CfirResolvePhase) {
        if (!_lazyResolveContractChecksEnabled.get() || elementPhase >= requestedPhase) return

        val currentPhase = currentTransformerPhase
            ?: error("Current phase is not set, please call ${this::startResolvingPhase.name} before starting transforming the file")

        if (!currentPhase.isItAllowedToCallLazyResolveTo(requestedPhase)) {
            exceptions += CfirLazyResolveContractViolationException(
                currentPhase = currentPhase,
                requestedPhase = requestedPhase,
            )
        }
    }
}

/**
 * 表示 `CfirLazyResolveContractViolationException`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirLazyResolveContractViolationException(
    /**
     * 保存 `currentPhase`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val currentPhase: CfirResolvePhase,
    /**
     * 保存 `requestedPhase`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val requestedPhase: CfirResolvePhase,
) : IllegalStateException("Lazy resolve contract violated: current=$currentPhase requested=$requestedPhase")
