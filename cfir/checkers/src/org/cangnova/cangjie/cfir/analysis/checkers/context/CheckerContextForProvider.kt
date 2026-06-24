package org.cangnova.cangjie.cfir.analysis.checkers.context

import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator

/**
 * checker 不应直接使用的可变 context Provider。
 *
 * 该类型只供运行 checker 的 `DiagnosticCollectorVisitor` 使用；和只读 [CheckerContext]
 * 不同，它暴露所有会修改遍历上下文栈的方法。
 */
abstract class CheckerContextForProvider(
    /** 提供 session 与 scopeSession 的持有者。 */
    override val sessionHolder: SessionAndScopeSessionHolder,
    /** 当前诊断流程使用的函数返回类型计算器。 */
    override val returnTypeCalculator: ReturnTypeCalculator,
    /** 当前作用域是否 suppress 所有 info 级别诊断。 */
    override val allInfosSuppressed: Boolean,
    /** 当前作用域是否 suppress 所有 warning 级别诊断。 */
    override val allWarningsSuppressed: Boolean,
    /** 当前作用域是否 suppress 所有 error 级别诊断。 */
    override val allErrorsSuppressed: Boolean
) : CheckerContext() {
    /** 返回带有新增 suppress 信息的新 context 或当前 context。 */
    abstract fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean
    ): CheckerContextForProvider

    /** 将声明压入声明栈。 */
    abstract fun addDeclaration(declaration: CfirDeclaration): CheckerContextForProvider

    /** 弹出最近压入的声明。 */
    abstract fun dropDeclaration()

    /** 将语句压入语句栈。 */
    abstract fun addStatement(statement: CfirStatement): CheckerContextForProvider

    /** 弹出最近压入的语句。 */
    abstract fun dropStatement()

    /** 将调用或赋值节点压入对应上下文栈。 */
    abstract fun addCallOrAssignment(qualifiedAccessOrAnnotationCall: CfirStatement): CheckerContextForProvider

    /** 弹出最近压入的调用或赋值节点。 */
    abstract fun dropCallOrAssignment()

    /** 将注解容器压入注解容器栈。 */
    abstract fun addAnnotationContainer(annotationContainer: CfirAnnotationContainer): CheckerContextForProvider

    /** 弹出最近压入的注解容器。 */
    abstract fun dropAnnotationContainer()

    /** 进入 contract body。 */
    abstract fun enterContractBody(): CheckerContextForProvider

    /** 退出 contract body。 */
    abstract fun exitContractBody(): CheckerContextForProvider



    /** 进入文件上下文。 */
    abstract fun enterFile(file: CfirFile): CheckerContextForProvider

    /** 退出文件上下文。 */
    abstract fun exitFile(file: CfirFile): CheckerContextForProvider

    /** 将元素压入元素栈。 */
    abstract fun addElement(element: CfirElement): CheckerContextForProvider

    /** 弹出最近压入的元素。 */
    abstract fun dropElement()
}
