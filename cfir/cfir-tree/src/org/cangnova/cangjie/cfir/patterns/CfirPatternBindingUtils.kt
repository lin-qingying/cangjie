package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 模式绑定出现位置。
 *
 * 统一把“名字 + 源位置 + 绑定变量实体”收敛成一个稳定视图，
 * 供 resolve、诊断、导航和索引层共享，避免每一层再次手写递归。
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
        listOf(CfirPatternBindingOccurrence(it, source, bindingVariable))
    }.orEmpty()

    is CfirOrPattern -> alternatives.flatMap(CfirPattern::bindingOccurrences)
    is CfirWildcardPattern, is CfirConstPattern, is CfirExpressionPattern -> emptyList()
}

fun CfirPattern.bindingVariables(): List<CfirPatternBindingVariable> =
    bindingOccurrences().mapNotNull(CfirPatternBindingOccurrence::variable)

fun CfirPattern.bindingNames(): List<Name> =
    bindingOccurrences().map(CfirPatternBindingOccurrence::name)

fun CfirPattern.primaryBindingNameOrNull(): Name? =
    bindingOccurrences().firstOrNull()?.name
