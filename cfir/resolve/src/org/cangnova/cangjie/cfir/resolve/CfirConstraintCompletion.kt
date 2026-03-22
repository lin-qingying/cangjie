package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirConstraintPosition
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/**
 * 约束求解完成器，对标官方 SolveConstraints + FindSolution。
 *
 * 使用拓扑排序 + Join/Meet 格运算，按依赖顺序逐批求解类型变量。
 */
class CfirConstraintCompletion(
    private val typeRelations: CfirTypeRelations,
) {
    private val joinMeet = CfirJoinMeet(typeRelations)

    fun complete(store: CfirConstraintStore) {
        if (store.typeVariables.isEmpty()) return

        val graph = CfirConstraintGraph(store.typeVariables, store)
        val solved = mutableMapOf<String, ConeCangJieType>()

        while (graph.hasNext()) {
            val batch = graph.topoOnce()
            if (batch.isEmpty()) {
                // 有环形依赖或所有变量都已处理，强制求解剩余变量
                reportUnsolved(store, solved)
                break
            }

            val batchSolution = solveBatch(batch, solved, store)
            solved.putAll(batchSolution)

            if (batchSolution.isNotEmpty()) {
                graph.applySubstitution(batchSolution)
            }
        }
    }

    /**
     * 求解一批无依赖的变量（对标 FindSolution 行 765-828）。
     */
    private fun solveBatch(
        batch: List<CfirTypeVariable>,
        alreadySolved: Map<String, ConeCangJieType>,
        store: CfirConstraintStore,
    ): Map<String, ConeCangJieType> {
        val result = mutableMapOf<String, ConeCangJieType>()

        for (variable in batch) {
            if (variable.fixedType != null) {
                result[variable.lookupTag.name] = variable.fixedType!!
                continue
            }

            // 优先使用等式解
            if (variable.equalitySolution != null) {
                val eqSolution = finalizeIdealType(variable.equalitySolution!!)
                variable.fixedType = eqSolution
                result[variable.lookupTag.name] = eqSolution
                continue
            }

            val solution = findSolution(variable, store)
            if (solution != null) {
                variable.fixedType = solution
                result[variable.lookupTag.name] = solution
            } else {
                store.reportIssue(
                    CfirConstraintIssue.UnresolvedVariable(
                        variable = variable,
                        position = CfirConstraintPosition.Unknown,
                        message = "Unresolved type variable ${variable.lookupTag.name}",
                    ),
                )
            }
        }

        return result
    }

    /**
     * 对单个变量求解（对标 FindSolution）。
     *
     * 优先级：
     * 1. tyJ 有效且非 ideal → 选 tyJ
     * 2. tyM 有效 → 选 tyM
     * 3. tyJ 含 ideal → 终结化后选 tyJ
     * 4. tyM 含 ideal → 终结化后选 tyM
     */
    private fun findSolution(variable: CfirTypeVariable, store: CfirConstraintStore): ConeCangJieType? {
        val lowerBounds = variable.lowerBounds.distinct()
        val upperBounds = variable.upperBounds.distinct()
        val ignoredVars = setOf(variable.lookupTag.name)

        // Join 下界
        val tyJ = if (lowerBounds.isNotEmpty()) joinMeet.join(lowerBounds) else null
        // Meet 上界
        val tyM = if (upperBounds.isNotEmpty()) {
            joinMeet.meetUpperBounds(variable, upperBounds, ignoredVars)
        } else null

        // 检查冲突
        if (tyJ != null && tyM != null && !typeRelations.isCompatible(tyJ, tyM)) {
            store.reportIssue(
                CfirConstraintIssue.ConflictingBounds(
                    variable = variable,
                    position = CfirConstraintPosition.Unknown,
                    message = "Conflicting bounds for ${variable.lookupTag.name}: join=$tyJ, meet=$tyM",
                ),
            )
            return null
        }

        // 选择优先级
        if (tyJ != null && !containsIdeal(tyJ)) {
            return tyJ
        }
        if (tyM != null && !containsIdeal(tyM)) {
            return tyM
        }
        if (tyJ != null) {
            return finalizeIdealType(tyJ)
        }
        if (tyM != null) {
            return finalizeIdealType(tyM)
        }

        // sumBounds 处理：选第一个兼容所有上界的
        for (candidate in variable.sumBounds) {
            val allUppersOk = upperBounds.all { typeRelations.isCompatible(candidate, it) }
            if (allUppersOk) return candidate
        }

        return null
    }

    /** 报告未求解的变量（环形依赖等情况） */
    private fun reportUnsolved(store: CfirConstraintStore, solved: Map<String, ConeCangJieType>) {
        for (variable in store.typeVariables) {
            if (variable.fixedType != null) continue
            if (variable.lookupTag.name in solved) continue

            // 尝试强制求解
            val solution = findSolution(variable, store)
            if (solution != null) {
                variable.fixedType = solution
            } else {
                store.reportIssue(
                    CfirConstraintIssue.UnresolvedVariable(
                        variable = variable,
                        position = CfirConstraintPosition.Unknown,
                        message = "Unresolved type variable ${variable.lookupTag.name} (possibly circular dependency)",
                    ),
                )
            }
        }
    }

    /** 检查类型是否包含 ideal 类型 */
    private fun containsIdeal(type: ConeCangJieType): Boolean {
        return type is ConePrimitiveType && type.kind.isIdeal
    }

    /** 将 ideal 类型终结化为具体类型 */
    private fun finalizeIdealType(type: ConeCangJieType): ConeCangJieType {
        if (type is ConePrimitiveType) {
            return when (type.kind) {
                PrimitiveTypeKind.IDEAL_INT -> ConePrimitiveType.INT64
                PrimitiveTypeKind.IDEAL_FLOAT -> ConePrimitiveType.FLOAT64
                else -> type
            }
        }
        return type
    }
}
