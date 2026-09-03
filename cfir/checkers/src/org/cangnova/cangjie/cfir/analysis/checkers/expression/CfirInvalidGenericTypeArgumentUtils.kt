package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.checkUpperBoundViolated
import org.cangnova.cangjie.cfir.analysis.checkers.hasInvalidGenericTypeArgument
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 判断表达式自身及其 qualified-access 接收者是否包含非法泛型实例化。
 *
 * qualified access 的最终语义可能由多层 `receiver<...>.member` 组成；只检查最外层
 * 表达式类型会漏掉嵌套类型实参。因此这里沿表达式的类型实参、显式接收者和 dispatch
 * receiver 递归查询，让所有依赖该实例化的 expression checker 共享同一个失效边界。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun CfirExpression.hasInvalidGenericTypeArgument(): Boolean =
    hasInvalidGenericTypeArgument(
        visited = Collections.newSetFromMap(IdentityHashMap()),
    )

/**
 * 在 CFIR 表达式图上查询非法泛型实例化。
 *
 * qualified access 的 `explicitReceiver` 和 `dispatchReceiver` 在语义上经常
 * 指向同一个 CFIR 节点。这里必须按节点身份去重，而不能使用普通的结构
 * 相等性或无状态递归；否则链式调用会沿同一条 receiver 边指数重复遍历，
 * 甚至在构造阶段形成环时递归不终止。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun CfirExpression.hasInvalidGenericTypeArgument(
    visited: MutableSet<CfirExpression>,
): Boolean {
    if (!visited.add(this)) return false

    val access = this as? CfirQualifiedAccessExpression ?: return false
    val hasInvalidTypeArgument = access.typeArguments.any { typeArgument ->
        typeArgument.hasInvalidGenericTypeArgument()
    }
    if (hasInvalidTypeArgument) return true
    // 限定访问自身的完整类型也可能是非法泛型实例化；仅沿 typeArguments
    // 继续向下看不到 `MyInterface<UInt16>` 这种由类型引用解析出的约束违例。
    val accessType = access.coneTypeOrNull
    if (accessType != null && checkUpperBoundViolated(
            type = accessType,
            sourceTypeRef = null,
            fallbackSource = access.source,
            reportDiagnostics = false,
        )) return true
    val explicitReceiver = access.explicitReceiver
    if (explicitReceiver?.hasInvalidGenericTypeArgument(visited) == true) return true

    val dispatchReceiver = access.dispatchReceiver
    return dispatchReceiver != null &&
        dispatchReceiver !== explicitReceiver &&
        dispatchReceiver.hasInvalidGenericTypeArgument(visited)
}
