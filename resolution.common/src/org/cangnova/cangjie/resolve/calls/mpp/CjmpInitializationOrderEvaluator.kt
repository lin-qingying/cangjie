package org.cangnova.cangjie.resolve.calls.mpp

import java.util.Collections

/**
 * 官方 AST::GetVarsInitializationOrderWithPositions 的平台中立求值。
 *
 * 输入是容器合并的同一个成员图，读取已替换的依赖边；不重新匹配字段、不求依赖闭包。
 * 源码 begin 的 line/column 排序已由前端冻结为 sourceOrder，求值期间不读取源码、provider 或声明阶段。
 * 容器外依赖的 SPECIFIC 位由前端一并冻结，不能把本图未包含的声明当作普通声明。
 * 输出的 position 只计真实实例变量，供初始化和布局消费者共同保留原槽位。
 */
object CjmpInitializationOrderEvaluator {
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker> getVarsInitializationOrderWithPositions(
        container: D,
        graph: CjmpContainerMergeResult<D, T>,
        externalDependencyRoles: Map<D, Boolean>,
    ): List<CjmpVariableWithPosition<D>> {
        require(externalDependencyRoles.keys.none(graph.memberFacts::containsKey)) {
            "External dependency roles must not replace member facts from the merged graph"
        }
        val dependencyRoles = graph.memberFacts.mapValues { (_, member) -> member.isSpecific } + externalDependencyRoles
        val commonVariables = mutableListOf<CjmpVariableWithPosition<D>>()
        val specificVariables = mutableListOf<CjmpVariableWithPosition<D>>()
        var position = 0
        for (symbol in graph.members.getValue(container)) {
            val member = graph.memberFacts.getValue(symbol)
            if (!member.isInstanceVariable) continue
            val variable = CjmpVariableWithPosition(symbol, position++)
            if (member.isFromCommonPart) {
                commonVariables += variable
            } else {
                specificVariables += variable
            }
        }
        specificVariables.sortBy { graph.memberFacts.getValue(it.symbol).sourceOrder }

        val result = mutableListOf<CjmpVariableWithPosition<D>>()
        val seenSpecificDependencies = hashSetOf<D>()
        var specificIndex = 0
        for (common in commonVariables) {
            val pendingSpecificDependencies = hashSetOf<D>()
            val dependencies = graph.dependencies.getValue(common.symbol)
            check(dependencies is CjmpInitializationDependencies.Collected) {
                "Common-part instance initialization requires collected declaration dependencies"
            }
            for (dependency in dependencies.declarations) {
                if (!dependencyRoles.getValue(dependency)) continue
                if (seenSpecificDependencies.add(dependency)) pendingSpecificDependencies += dependency
            }
            // 官方按 specific 源码序列推进到所需依赖，保留途中普通字段，绝不只抽出依赖节点。
            while (pendingSpecificDependencies.isNotEmpty()) {
                check(specificIndex < specificVariables.size) {
                    "A common initialization dependency must occur in the remaining specific variable sequence"
                }
                val specific = specificVariables[specificIndex++]
                pendingSpecificDependencies.remove(specific.symbol)
                result += specific
            }
            result += common
        }
        while (specificIndex < specificVariables.size) result += specificVariables[specificIndex++]
        return Collections.unmodifiableList(result)
    }
}
