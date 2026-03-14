package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType

/**
 * 约束系统实现。
 *
 * 保守策略：直接收集上下界，不做 K2 完整的 ConstraintIncorporator 传递性规则。
 * 仓颉无 nullable、无 SAM，约束关系简单，保守策略覆盖绝大多数场景。
 *
 * 算法：
 * 1. registerTypeVariable(tv) — 注册变量，初始化空 bounds
 * 2. addSubtypeConstraint(sub, super) — 若涉及类型变量，收集为上界/下界
 * 3. fixVariable(tv) — 取 lowerBounds 中最具体类型；若无则取 upperBounds 中最宽泛类型
 * 4. fixAllVariables() — 按依赖顺序逐个固定
 * 5. buildResultingSubstitutor() — 从固定结果构建 CfirTypeSubstitutorByMap
 *
 * 对齐 K2 NewConstraintSystemImpl（大幅简化）。
 */
class CfirConstraintSystemImpl : CfirConstraintSystem {

    private val _typeVariables: MutableList<CfirTypeVariable> = mutableListOf()
    private val _constraints: MutableList<CfirConstraint> = mutableListOf()
    private val _errors: MutableList<String> = mutableListOf()

    /** 类型变量名称 → 类型变量实例的索引 */
    private val variableByName: MutableMap<String, CfirTypeVariable> = mutableMapOf()

    private var nextFreshId: Int = 0

    override val typeVariables: List<CfirTypeVariable> get() = _typeVariables
    override val constraints: List<CfirConstraint> get() = _constraints
    override val hasErrors: Boolean get() = _errors.isNotEmpty()
    override val errors: List<String> get() = _errors

    override fun registerTypeVariable(variable: CfirTypeVariable) {
        _typeVariables.add(variable)
        variableByName[variable.name] = variable
    }

    override fun addSubtypeConstraint(
        subType: ConeCangjieType,
        superType: ConeCangjieType,
        position: CfirConstraintPosition,
    ) {
        val constraint = CfirSubtypeConstraint(subType, superType, position)
        _constraints.add(constraint)

        // 提取约束到类型变量的 bounds
        processSubtypeConstraint(subType, superType)
    }

    override fun addEqualityConstraint(
        left: ConeCangjieType,
        right: ConeCangjieType,
        position: CfirConstraintPosition,
    ) {
        val constraint = CfirEqualityConstraint(left, right, position)
        _constraints.add(constraint)

        // 等价约束 = 双向子类型约束
        processSubtypeConstraint(left, right)
        processSubtypeConstraint(right, left)
    }

    /**
     * 处理子类型约束，将涉及类型变量的约束转换为 bounds。
     *
     * - T <: ConcreteType → T 的 upperBound += ConcreteType
     * - ConcreteType <: T → T 的 lowerBound += ConcreteType
     * - T <: U → U 的 lowerBound += T（间接）
     */
    private fun processSubtypeConstraint(subType: ConeCangjieType, superType: ConeCangjieType) {
        val subVariable = findTypeVariable(subType)
        val superVariable = findTypeVariable(superType)

        when {
            subVariable != null && superVariable != null -> {
                // T <: U — 两个类型变量之间的约束
                // 暂时不处理传递性，等固定时根据已收集 bounds 推断
            }
            subVariable != null -> {
                // T <: ConcreteType → T 的上界
                subVariable.upperBounds.add(superType)
            }
            superVariable != null -> {
                // ConcreteType <: T → T 的下界
                superVariable.lowerBounds.add(subType)
            }
            // 两边都不是类型变量 — 忽略（由子类型检查器处理）
        }
    }

    /** 尝试从类型中查找对应的类型变量 */
    private fun findTypeVariable(type: ConeCangjieType): CfirTypeVariable? {
        if (type !is ConeTypeParameterType) return null
        return variableByName[type.lookupTag.name]
    }

    override fun fixVariable(variable: CfirTypeVariable) {
        if (variable.isFixed) return

        val fixedType = computeFixedType(variable)
        if (fixedType != null) {
            variable.fixedType = fixedType
        } else {
            _errors.add("无法推断类型变量 ${variable.name} 的类型：无足够的约束")
        }
    }

    /**
     * 变量固定策略（简化版）：
     * - 有 lower bounds → 取 lower bounds 中第一个类型（最具体的，简化策略）
     * - 仅有 upper bounds → 取 upper bounds 中第一个类型（最宽泛的）
     * - 无 bounds → 返回 null（报错）
     */
    private fun computeFixedType(variable: CfirTypeVariable): ConeCangjieType? {
        // 优先使用下界（最具体类型）
        if (variable.lowerBounds.isNotEmpty()) {
            return variable.lowerBounds.first()
        }

        // 其次使用上界（最宽泛类型）
        if (variable.upperBounds.isNotEmpty()) {
            return variable.upperBounds.first()
        }

        return null
    }

    override fun fixAllVariables() {
        // 简单策略：按注册顺序固定（无复杂依赖图分析）
        for (variable in _typeVariables) {
            if (!variable.isFixed) {
                fixVariable(variable)
            }
        }
    }

    override fun buildResultingSubstitutor(): CfirTypeSubstitutor {
        val substitution = mutableMapOf<String, ConeCangjieType>()
        for (variable in _typeVariables) {
            val fixed = variable.fixedType
            if (fixed != null) {
                substitution[variable.name] = fixed
            }
        }
        return if (substitution.isEmpty()) {
            CfirTypeSubstitutor.Empty
        } else {
            CfirTypeSubstitutorByMap(substitution)
        }
    }

    /** 生成下一个唯一 ID */
    fun nextFreshTypeId(): Int = nextFreshId++

    override fun toString(): String = buildString {
        append("CfirConstraintSystem(")
        append("variables=${_typeVariables.size}, ")
        append("constraints=${_constraints.size}, ")
        append("errors=${_errors.size}")
        append(")")
    }
}
