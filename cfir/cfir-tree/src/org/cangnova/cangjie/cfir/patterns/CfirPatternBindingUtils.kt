package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 模式绑定出现位置。
 *
 * 统一把“名字 + 源位置 + 绑定变量实体”收敛成一个稳定视图，
 * 供 resolve、诊断、导航和索引层共享，避免每一层再次手写递归。
 *
 * @property name 绑定名。
 * @property source 绑定名或绑定节点的源码位置。
 * @property variable 绑定变量声明实体；无法形成变量实体时为 `null`。
 */
data class CfirPatternBindingOccurrence(
    val name: Name,
    val source: CjSourceElement?,
    val variable: CfirPatternBindingVariable?,
)

/**
 * 收集模式树中所有具名绑定。
 *
 * 注意：
 * 1. 外层 `CfirPatternVariable` 容器不是绑定本身，不会出现在结果里。
 * 2. `or-pattern` 仍然按各自分支分别保留，调用方可以自行决定是否只看首分支。
 */
fun CfirPattern.bindingOccurrences(): List<CfirPatternBindingOccurrence> = when (this) {
    is CfirBindingPattern -> buildList {
        add(CfirPatternBindingOccurrence(name, source, bindingVariable))
        nestedPattern?.let { addAll(it.bindingOccurrences()) }
    }

    is CfirVarOrEnumPattern -> bindingVariable?.let { variable ->
        listOf(CfirPatternBindingOccurrence(name, source, variable))
    }.orEmpty()
    is CfirTuplePattern -> elements.flatMap(CfirPattern::bindingOccurrences)
    is CfirEnumPattern -> arguments.flatMap(CfirPattern::bindingOccurrences)
    is CfirTypePattern -> bindingName?.let {
        listOf(CfirPatternBindingOccurrence(it, bindingVariable?.source ?: source, bindingVariable))
    }.orEmpty()

    is CfirOrPattern -> alternatives.flatMap(CfirPattern::bindingOccurrences)
    is CfirWildcardPattern, is CfirConstPattern, is CfirExpressionPattern -> emptyList()
}

/**
 * 收集模式树中所有绑定变量实体。
 */
fun CfirPattern.bindingVariables(): List<CfirPatternBindingVariable> =
    bindingOccurrences().mapNotNull(CfirPatternBindingOccurrence::variable)

/**
 * 返回可进入局部作用域/局部重定义检查的模式绑定。
 *
 * 官方语义中，`|` 连接的模式禁止引入变量。普通 enum/var 绑定在这种非法
 * or-pattern 中不会成为分支体可解析的局部变量；但 `x: T` type-pattern
 * 仍会形成可见绑定，并继续参与同一 case 内的重定义诊断。
 */
fun CfirPattern.visibleBindingVariables(): List<CfirPatternBindingVariable> = when (this) {
    is CfirOrPattern -> alternatives.flatMap(CfirPattern::typePatternBindingVariables)
    else -> bindingVariables()
}

/**
 * 收集 type-pattern 产生的绑定变量。
 *
 * 该入口只供 [visibleBindingVariables] 处理 or-pattern 特例使用。
 */
private fun CfirPattern.typePatternBindingVariables(): List<CfirPatternBindingVariable> = when (this) {
    is CfirTypePattern -> bindingVariable?.let(::listOf).orEmpty()
    is CfirTuplePattern -> elements.flatMap(CfirPattern::typePatternBindingVariables)
    is CfirEnumPattern -> arguments.flatMap(CfirPattern::typePatternBindingVariables)
    is CfirBindingPattern -> nestedPattern?.typePatternBindingVariables().orEmpty()
    is CfirOrPattern -> alternatives.flatMap(CfirPattern::typePatternBindingVariables)
    is CfirVarOrEnumPattern,
    is CfirWildcardPattern,
    is CfirConstPattern,
    is CfirExpressionPattern,
    -> emptyList()
}

/**
 * 收集模式树中所有绑定名称。
 */
fun CfirPattern.bindingNames(): List<Name> =
    bindingOccurrences().map(CfirPatternBindingOccurrence::name)

/**
 * 返回模式树中的主绑定名。
 *
 * 当前策略取绑定出现顺序中的第一个名称；没有绑定时返回 `null`。
 */
fun CfirPattern.primaryBindingNameOrNull(): Name? =
    bindingOccurrences().firstOrNull()?.name
