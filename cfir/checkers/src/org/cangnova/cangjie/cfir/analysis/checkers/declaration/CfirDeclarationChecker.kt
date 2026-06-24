package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter

/**
 * CFIR 声明级检查器的基础抽象。
 *
 * 每个具体 checker 通过类型参数限定自己接收的声明种类，并在 checker registry
 * 分发到对应 CFIR 声明时执行诊断逻辑。
 */
abstract class CfirDeclarationChecker<D : CfirDeclaration> {
    /**
     * 对单个声明执行诊断检查。
     *
     * context 提供当前文件、session、作用域和所有者链信息；reporter 负责把检查结果
     * 统一提交到 CFIR 诊断管线。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(declaration: D)
}

