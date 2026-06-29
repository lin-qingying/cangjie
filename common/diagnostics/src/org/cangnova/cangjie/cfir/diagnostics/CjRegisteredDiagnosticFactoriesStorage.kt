

package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory

/**
 * Contains all diagnostic factories that could be used in the current compilation
 */
class CjRegisteredDiagnosticFactoriesStorage {
    /**
     * 已注册的诊断渲染器工厂集合。
     */
    private val factories = mutableSetOf<BaseDiagnosticRendererFactory>()

    /**
     * 注册一组诊断容器。
     */
    fun registerDiagnosticContainers(vararg containers: CjDiagnosticsContainer) {
        registerDiagnosticContainers(containers.toList())
    }

    /**
     * 注册诊断容器列表中的渲染器工厂。
     */
    fun registerDiagnosticContainers(containers: List<CjDiagnosticsContainer>) {
        this.factories += containers.map { it.getRendererFactory() }
    }

    /**
     * 当前已注册的全部诊断工厂。
     */
    val allDiagnosticFactories: List<AbstractCjDiagnosticFactory>
        get() = factories.flatMap { it.MAP.factories }
}
