package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.analysis.collectors.components.ReportCommitterDiagnosticComponent

/**
 * 诊断收集组件容器。
 * 对齐 K2 `DiagnosticCollectorComponents`。
 * @param regularComponents 常规检查组件，如声明检查器、表达式检查器等
 * @param reportCommitter 诊断提交组件，在每个元素检查完成后提交 pending 诊断
 */
class DiagnosticCollectorComponents(
    /** 常规检查组件，如声明检查器、表达式检查器、类型检查器等。 */
    val regularComponents: Array<AbstractDiagnosticCollectorComponent>,
    /**
     * 只在当前文件 Sema 阶段无错误时运行的后续组件。
     *
     * 官方编译器不会在 Sema 已失败的源文件上继续执行 CHIR 常量检查；把这类组件与
     * 常规诊断遍历分开，避免遍历顺序导致前面表达式的 CHIR 诊断抢先泄露。
     */
    val postSemaComponents: Array<AbstractDiagnosticCollectorComponent>,
    /** 诊断提交组件，在元素或文件遍历结束时提交 pending 诊断。 */
    val reportCommitter: ReportCommitterDiagnosticComponent,
    /**
     * 与 [regularComponents] 一一对应的组件种类，供逐组件耗时计量归因。
     *
     * 由装配方（[org.cangnova.cangjie.cfir.analysis.collectors.components.DiagnosticComponentsFactory]
     * 与 IDE 侧组件工厂）声明，而不是从组件类型反推：组件类里没有种类字段，反推要么写成一处
     * 脆弱的 `when`，要么给基类加抽象属性——后者要动代码生成器，因为三个最大的组件类是生成产物。
     */
    val checkerComponentKinds: Array<CfirCheckerComponentKind>,
    /** 当前组件集合所属的阶段，供 IDE 宿主区分各阶段的遍历与诊断。 */
    val phase: DiagnosticCollectionPhase = DiagnosticCollectionPhase.SEMA,
) {
    init {
        // 平行数组一旦错位，耗时就会归到错误的组件上，而且不会立刻暴露。
        require(checkerComponentKinds.size == regularComponents.size) {
            "checkerComponentKinds must have the same size as regularComponents: " +
                "${checkerComponentKinds.size} vs ${regularComponents.size}"
        }
    }

    /** 创建只执行后续阶段检查器的一次诊断遍历配置。 */
    fun postSemaPass(forPartialDeclaration: Boolean = false): DiagnosticCollectorComponents {
        val retained = postSemaComponents.filter { !forPartialDeclaration || it.supportsDeclarationPostSemaPass }
        // filter 只做保留，因此按保留个数取 [POST_SEMA_COMPONENT_KINDS] 的同一前缀即可保持对齐。
        val retainedKinds = POST_SEMA_COMPONENT_KINDS.filterIndexed { index, _ -> index < retained.size }
        return DiagnosticCollectorComponents(
            regularComponents = retained.toTypedArray(),
            checkerComponentKinds = retainedKinds.toTypedArray(),
            postSemaComponents = emptyArray(),
            reportCommitter = reportCommitter,
            phase = DiagnosticCollectionPhase.POST_SEMA,
        )
    }

    private companion object {
        /**
         * POST_SEMA 组件的种类登记；顺序必须与所有装配方构造 `postSemaComponents` 的顺序一致。
         */
        private val POST_SEMA_COMPONENT_KINDS: List<CfirCheckerComponentKind> = listOf(
            CfirCheckerComponentKind.CONTROL_FLOW_ANALYSIS,
            CfirCheckerComponentKind.CHIR_ARITHMETIC,
        )
    }
}
