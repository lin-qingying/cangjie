package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage

/**
 * 从显式类型实参的等式约束创建只读替换器。
 *
 * 参数检查、完成写回和诊断映射必须看到同一组显式绑定。约束失败时变量可能尚未固定，
 * 但源码实参仍已确定；不能让原始上界和代入实参后的上界形成两个不同的用户诊断。
 */
fun ConstraintStorage.explicitTypeArgumentsSubstitutor(): ConeSubstitutor {
    val replacements = notFixedTypeVariables.mapNotNull { (typeConstructor, variable) ->
        val explicitType = variable.constraints.firstOrNull { constraint ->
            constraint.kind == ConstraintKind.EQUALITY &&
                    constraint.position.from is ConeExplicitTypeParameterConstraintPosition
        }?.type as? ConeCangJieType ?: return@mapNotNull null
        typeConstructor to explicitType
    }.toMap()
    return if (replacements.isEmpty()) ConeSubstitutor.Empty else CfirTypeSubstitutorByMap(replacements)
}
