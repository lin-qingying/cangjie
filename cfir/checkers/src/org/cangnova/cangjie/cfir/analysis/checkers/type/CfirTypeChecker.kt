package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/** CFIR 类型引用 checker 基类，按具体 `CfirTypeRef` 子类型执行类型层诊断。 */
abstract class CfirTypeChecker<T : CfirTypeRef> {
    /** 在当前 checker 上下文和诊断 reporter 中检查一个类型引用节点。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(typeRef: T)
}

