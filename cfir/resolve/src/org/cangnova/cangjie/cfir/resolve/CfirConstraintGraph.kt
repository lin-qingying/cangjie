package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.types.*

/**
 * 类型变量拓扑依赖图，对标官方 TyVarConstraintGraph。
 *
 * 分析类型变量间的依赖关系（一个变量的 bound 中引用了另一个变量），
 * 按拓扑序批量求解无依赖的变量。
 */
class CfirConstraintGraph(
    variables: List<CfirTypeVariable>,
    private val store: CfirConstraintStore,
) {
    /** 变量名 → 变量 */
    private val variableMap = variables.associateBy { it.lookupTag.name }.toMutableMap()

    /** 变量名 → 依赖它的变量名集合（前驱 → 后继） */
    private val dependents = mutableMapOf<String, MutableSet<String>>()

    /** 变量名 → 它依赖的变量名数量（入度） */
    private val inDegree = mutableMapOf<String, Int>()

    /** 已求解的变量名 */
    private val solved = mutableSetOf<String>()

    init {
        preProcess(variables)
    }

    /** 返回当前无依赖的变量批次（入度为 0 且未求解） */
    fun topoOnce(): List<CfirTypeVariable> {
        val batch = variableMap.keys
            .filter { it !in solved && (inDegree[it] ?: 0) == 0 }
            .mapNotNull { variableMap[it] }

        // 标记为已求解并更新后继入度
        for (variable in batch) {
            val name = variable.lookupTag.name
            solved += name
            dependents[name]?.forEach { dependent ->
                inDegree[dependent] = (inDegree[dependent] ?: 1) - 1
            }
        }

        return batch
    }

    /** 用已解变量的解替换图中引用 */
    fun applySubstitution(subst: Map<String, ConeCangJieType>) {
        for ((_, variable) in variableMap) {
            if (variable.lookupTag.name in solved) continue
            substituteInBounds(variable, subst)
        }
    }

    /** 是否还有未求解的变量 */
    fun hasNext(): Boolean {
        return variableMap.keys.any { it !in solved }
    }

    /** 预处理：扫描 bounds 建立依赖边 */
    private fun preProcess(variables: List<CfirTypeVariable>) {
        val varNames = variables.map { it.lookupTag.name }.toSet()

        for (variable in variables) {
            val name = variable.lookupTag.name
            inDegree[name] = 0
            dependents.getOrPut(name) { mutableSetOf() }
        }

        for (variable in variables) {
            val name = variable.lookupTag.name
            val referencedVars = mutableSetOf<String>()

            for (bound in variable.upperBounds + variable.lowerBounds + variable.sumBounds) {
                referencedVars += bound.collectVariableNames(varNames)
            }
            variable.equalitySolution?.let {
                referencedVars += it.collectVariableNames(varNames)
            }

            // 当前变量依赖 referencedVars 中的变量
            referencedVars.remove(name) // 不依赖自己
            inDegree[name] = referencedVars.size

            for (dep in referencedVars) {
                dependents.getOrPut(dep) { mutableSetOf() } += name
            }
        }
    }

    /** 在变量的 bounds 中用替换映射替换已解的变量引用 */
    private fun substituteInBounds(variable: CfirTypeVariable, subst: Map<String, ConeCangJieType>) {
        substituteList(variable.upperBounds, subst)
        substituteList(variable.lowerBounds, subst)
        substituteList(variable.sumBounds, subst)
        variable.equalitySolution?.let {
            variable.equalitySolution = it.substituteVariables(subst)
        }
    }

    private fun substituteList(bounds: MutableList<ConeCangJieType>, subst: Map<String, ConeCangJieType>) {
        for (i in bounds.indices) {
            bounds[i] = bounds[i].substituteVariables(subst)
        }
    }
}

/** 检查类型中是否引用了某个变量 */
fun ConeCangJieType.containsVariable(varName: String): Boolean {
    return varName in collectVariableNames(setOf(varName))
}

/** 收集类型中引用的所有变量名（仅返回在 knownVars 中的） */
fun ConeCangJieType.collectVariableNames(knownVars: Set<String>): Set<String> {
    val result = mutableSetOf<String>()
    collectVariableNamesImpl(this, knownVars, result)
    return result
}

private fun collectVariableNamesImpl(type: ConeCangJieType, knownVars: Set<String>, result: MutableSet<String>) {
    when (type) {
        is ConeTypeParameterType -> {
            if (type.lookupTag.name in knownVars) {
                result += type.lookupTag.name
            }
        }
        is ConeClassLikeType -> type.typeArguments.forEach { collectVariableNamesImpl(it, knownVars, result) }
        is ConeStructType -> type.typeArguments.forEach { collectVariableNamesImpl(it, knownVars, result) }
        is ConeEnumType -> type.typeArguments.forEach { collectVariableNamesImpl(it, knownVars, result) }
        is ConeFuncType -> {
            type.parameterTypes.forEach { collectVariableNamesImpl(it, knownVars, result) }
            collectVariableNamesImpl(type.returnType, knownVars, result)
        }
        is ConeTupleType -> type.elementTypes.forEach { collectVariableNamesImpl(it, knownVars, result) }
        is ConeArrayType -> collectVariableNamesImpl(type.elementType, knownVars, result)
        is ConePointerType -> collectVariableNamesImpl(type.pointeeType, knownVars, result)
        is ConeIntersectionType -> type.intersectedTypes.forEach { collectVariableNamesImpl(it, knownVars, result) }
        is ConeUnionType -> type.unionTypes.forEach { collectVariableNamesImpl(it, knownVars, result) }
        else -> {}
    }
}

/** 用替换映射替换类型中的变量引用 */
fun ConeCangJieType.substituteVariables(subst: Map<String, ConeCangJieType>): ConeCangJieType {
    if (subst.isEmpty()) return this
    return substituteImpl(this, subst)
}

private fun substituteImpl(type: ConeCangJieType, subst: Map<String, ConeCangJieType>): ConeCangJieType {
    return when (type) {
        is ConeTypeParameterType -> subst[type.lookupTag.name] ?: type
        is ConeClassLikeType -> {
            val newArgs = type.typeArguments.map { substituteImpl(it, subst) }
            if (newArgs == type.typeArguments) type
            else ConeClassLikeType(type.lookupTag, newArgs, type.attributes, type.isInterface, type.isThisType)
        }
        is ConeFuncType -> {
            val newParams = type.parameterTypes.map { substituteImpl(it, subst) }
            val newReturn = substituteImpl(type.returnType, subst)
            if (newParams == type.parameterTypes && newReturn === type.returnType) type
            else ConeFuncType(newParams, newReturn, type.isCFunc, type.isClosureType, type.hasVariableLenArg, type.attributes)
        }
        is ConeTupleType -> {
            val newElems = type.elementTypes.map { substituteImpl(it, subst) }
            if (newElems == type.elementTypes) type else ConeTupleType(newElems, type.attributes)
        }
        is ConeArrayType -> {
            val newElem = substituteImpl(type.elementType, subst)
            if (newElem === type.elementType) type else ConeArrayType(newElem, type.dims, type.attributes)
        }
        is ConePointerType -> {
            val newPointee = substituteImpl(type.pointeeType, subst)
            if (newPointee === type.pointeeType) type else ConePointerType(newPointee, type.attributes)
        }
        is ConeIntersectionType -> {
            val newTypes = type.intersectedTypes.map { substituteImpl(it, subst) }
            if (newTypes == type.intersectedTypes) type else ConeIntersectionType(newTypes, type.attributes)
        }
        is ConeUnionType -> {
            val newTypes = type.unionTypes.map { substituteImpl(it, subst) }.toSet()
            if (newTypes == type.unionTypes) type else ConeUnionType(newTypes, type.attributes)
        }
        else -> type
    }
}
