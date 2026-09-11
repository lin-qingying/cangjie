package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.resolve.calls.CommonSuperTypeCalculator
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 对齐官方 JoinAsVisibleTy；无可表示的唯一最小公共父类型时返回 null，诊断由语法 owner 生成。
 * 内部约束求解继续使用 commonSuperTypeOrNull，不能把这里的可见性限制提前施加到推断状态。
 */
fun ConeInferenceContext.commonVisibleSuperTypeOrNull(types: List<ConeCangJieType>): ConeCangJieType? {
    if (types.isEmpty()) return null
    val joined = with(CommonSuperTypeCalculator) { commonSuperTypeAsVisible(types).asCone() }
    if (joined is ConeErrorType) return null
    return materializeVisibleType(joined)
}

/** ToUserVisibleTy 仅消除交集/并集及函数、元组分量中的中间类型，保留其它名义类型的结构。 */
private fun ConeInferenceContext.materializeVisibleType(type: ConeCangJieType): ConeCangJieType? {
    return when (type) {
        is ConeErrorType -> null
        is ConeIntersectionType -> {
            val components = type.intersectedTypes
            if (components.isEmpty()) {
                ConeAnyType
            } else {
                val smallest = components.firstOrNull { candidate ->
                    components.all { AbstractTypeChecker.isSubtypeOf(this, candidate, it) }
                }
                val visible = smallest?.let { materializeVisibleType(it) }
                if (visible == ConePrimitiveType.NOTHING) ConeAnyType else visible
            }
        }
        is ConeUnionType -> {
            val components = type.unionTypes.map { materializeVisibleType(it) ?: return null }
            val joined = commonVisibleSuperTypeOrNull(components)
            if (joined == ConeAnyType || joined is ConeClassLikeType && joined.classId == StdlibClassIds.Any) {
                ConePrimitiveType.NOTHING
            } else {
                joined
            }
        }
        is ConeFunctionType -> {
            val parameters = type.parameterTypes.map { materializeVisibleType(it) ?: return null }
            val result = materializeVisibleType(type.returnType) ?: return null
            if (parameters == type.parameterTypes && result == type.returnType) type else {
                ConeFunctionType(parameters, result, type.isCFunc, type.isClosureType, type.hasVariableLenArg, type.attributes)
            }
        }
        is ConeTupleType -> {
            val elements = type.elementTypes.map { materializeVisibleType(it) ?: return null }
            if (elements == type.elementTypes) type else ConeTupleType(elements, type.attributes)
        }
        else -> type
    }
}
