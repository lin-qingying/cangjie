package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.lazyDeclarationResolver

/**
 * CFIR 诊断收集器基类，负责创建 visitor 并驱动声明树遍历。
 *
 * 结构对齐 Kotlin FIR `AbstractDiagnosticCollector`，但 reporter 会沿当前 CFIR
 * checker context 传递，保证 pending 诊断在同一流水线中提交。
 *
 * @property session 当前诊断收集所属的 CFIR session。
 * @property scopeSession 本次诊断收集使用的作用域缓存 session。
 * @property createComponents 根据 pending reporter 创建诊断组件集合的工厂。
 */
abstract class AbstractDiagnosticCollector(
    /** 当前诊断收集所属的 CFIR session。 */
    override val session: CfirSession,
    /** 本次诊断收集使用的作用域缓存 session。 */
    override val scopeSession: ScopeSession = ScopeSession(),
    /** 根据 pending reporter 创建诊断组件集合的工厂。 */
    protected val createComponents: (PendingDiagnosticReporter) -> DiagnosticCollectorComponents,
) : SessionAndScopeSessionHolder {
    /** 对一个 CFIR 声明子树执行完整诊断收集流程。 */
    fun collectDiagnostics(cfirDeclaration: CfirDeclaration, reporter: PendingDiagnosticReporter) {
        val components = createComponents(reporter)
        runDiagnosticPass(cfirDeclaration, reporter, components)

        if (components.postSemaComponents.isEmpty()) return
        val file = cfirDeclaration as? CfirFile ?: return
        val sourceFile = file.sourceFile ?: return
        val filePath = sourceFile.path ?: return
        if (reporter.hasErrorsInFile(filePath)) return

        runDiagnosticPass(cfirDeclaration, reporter, components.postSemaPass())
    }

    /**
     * 执行一个完整的诊断遍历阶段。
     *
     * 常规 Sema 组件与 CHIR 后续组件复用相同的 traversal、suppression 和提交规则，仅由
     * [collectDiagnostics] 决定两阶段间是否跨越错误边界。
     */
    private fun runDiagnosticPass(
        cfirDeclaration: CfirDeclaration,
        reporter: PendingDiagnosticReporter,
        components: DiagnosticCollectorComponents,
    ) {
        val visitor = createVisitor(components, reporter)
        visitor.checkSettings()
        session.lazyDeclarationResolver.disableLazyResolveContractChecksInside {
            cfirDeclaration.accept(visitor, null)
        }
    }

    /** 仅执行不依赖具体声明节点的 session/语言设置诊断检查。 */
    fun collectDiagnosticsInSettings(reporter: PendingDiagnosticReporter) {
        val visitor = createVisitor(createComponents(reporter), reporter)
        visitor.checkSettings()
    }

    /** 为当前诊断收集创建实际遍历 CFIR 树的 visitor。 */
    protected abstract fun createVisitor(
        components: DiagnosticCollectorComponents,
        reporter: PendingDiagnosticReporter,
    ): CheckerRunningDiagnosticCollectorVisitor
}
