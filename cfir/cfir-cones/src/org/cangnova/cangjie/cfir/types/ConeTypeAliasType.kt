package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId

/**
 * 类型别名类型，对应仓颉编译器中的 TypeAliasTy。
 * 在解析期间使用，最终会展开为实际类型。
 *
 * @property classId 类型别名声明的 ClassId。
 * @property expandedType 展开后的实际类型（解析后设置）。
 * @property typeArguments 类型别名实参。
 * @property attributes 类型别名类型附带的属性。
 */
class ConeTypeAliasType(
    val classId: ClassId,
    val expandedType: ConeCangJieType? = null,
    override val typeArguments: List<ConeTypeProjection> = emptyList(),
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {

    /**
     * typealias 类型按别名身份与实参判等，不把 [expandedType] 纳入等价关系。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeTypeAliasType) return false
        return classId == other.classId && typeArguments == other.typeArguments
    }

    /**
     * typealias 类型的结构哈希。
     */
    override fun hashCode(): Int {
        var result = classId.hashCode()
        result = 31 * result + typeArguments.hashCode()
        return result
    }


}
