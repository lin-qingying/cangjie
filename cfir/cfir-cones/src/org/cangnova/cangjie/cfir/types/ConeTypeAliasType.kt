package org.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId

/**
 * 类型别名类型，对应仓颉编译器中的 TypeAliasTy。
 * 在解析期间使用，最终会展开为实际类型。
 */
class ConeTypeAliasType(
    val classId: ClassId,
    /** 展开后的实际类型（解析后设置） */
    val expandedType: ConeCangjieType? = null,
    override val typeArguments: List<ConeCangjieType> = emptyList(),
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeTypeAliasType) return false
        return classId == other.classId && typeArguments == other.typeArguments
    }

    override fun hashCode(): Int {
        var result = classId.hashCode()
        result = 31 * result + typeArguments.hashCode()
        return result
    }

    override fun toString(): String = buildString {
        append("typealias ")
        append(classId.shortClassName)
        if (typeArguments.isNotEmpty()) {
            typeArguments.joinTo(this, prefix = "<", postfix = ">")
        }
        expandedType?.let { append(" = $it") }
    }
}
