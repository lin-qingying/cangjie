package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType

/**
 * 泛型约束系统接口。
 *
 * 管理类型变量的注册、约束收集、变量固定和最终替换器构建。
 * 使用 2 种状态（BUILDING → COMPLETED），简化 K2 的 4 种状态模型。
 *
 * 对齐 K2 ConstraintStorage + ConstraintSystemBuilder（合并为单一接口）。
 */
interface CfirConstraintSystem {

    /** 注册类型变量 */
    fun registerTypeVariable(variable: CfirTypeVariable)

    /** 添加子类型约束：sub <: super */
    fun addSubtypeConstraint(subType: ConeCangjieType, superType: ConeCangjieType, position: CfirConstraintPosition)

    /** 添加等价约束：left == right */
    fun addEqualityConstraint(left: ConeCangjieType, right: ConeCangjieType, position: CfirConstraintPosition)

    /** 固定指定类型变量 */
    fun fixVariable(variable: CfirTypeVariable)

    /** 按依赖顺序固定所有未固定的类型变量 */
    fun fixAllVariables()

    /** 从固定结果构建类型替换器 */
    fun buildResultingSubstitutor(): CfirTypeSubstitutor

    /** 是否存在推断错误 */
    val hasErrors: Boolean

    /** 错误信息列表 */
    val errors: List<String>

    /** 所有已注册的类型变量 */
    val typeVariables: List<CfirTypeVariable>

    /** 所有已收集的约束 */
    val constraints: List<CfirConstraint>

    companion object {
        /** 创建空约束系统实例 */
        fun create(): CfirConstraintSystem = CfirConstraintSystemImpl()
    }
}
