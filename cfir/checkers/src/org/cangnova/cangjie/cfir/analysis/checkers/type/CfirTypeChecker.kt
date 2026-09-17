package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/** CFIR 类型引用 checker 基类，按具体 `CfirTypeRef` 子类型执行类型层诊断。 */
abstract class CfirTypeChecker<T : CfirTypeRef> {
    /**
     * 类型检查是否依赖声明的实现体或初始化器。
     *
     * 类型 checker 默认与声明文件无关；该属性与声明/表达式 checker 的能力契约
     * 对齐，使生成的统一分派器不需要为每一种 checker 维护另一套接口。
     */
    open val requiresImplementation: Boolean get() = false

    /** 在当前 checker 上下文和诊断 reporter 中检查一个类型引用节点。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(typeRef: T)
}
