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
    /** 类型别名声明的 ClassId。 */
    val classId: ClassId,
    /** 展开后的实际类型（解析后设置）。 */
    val expandedType: ConeCangJieType? = null,
    /** 类型别名实参。 */
    override val typeArguments: List<ConeTypeProjection> = emptyList(),
    /** 类型别名类型附带的属性。 */
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {

    /**
     * typealias 类型按别名身份、实参与属性判等，不把 [expandedType] 纳入等价关系。
     *
     * - 与 `ConeLookupTagBasedType` 一致：属性（注解投影等）参与等值，否则带注解与不带注解的
     *   同一别名会被 `typeIndex`/`Set<ConeCangJieType>` 合并；
     * - 与官方 `TypeAliasTy` 一致：展开类型只是缓存，不参与等值。
     *
     * `attributes` 按 `ConeAttributes` 自身等值语义比较；当前 `AttributeArrayOwner` 没有值等值
     * override，即**实例等值**。因此同一逻辑属性只有在复用同一实例（或 `withAttributes` 的快路径）
     * 时才保证相等，不可假定按属性内容值等值。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeTypeAliasType) return false
        return classId == other.classId &&
                typeArguments == other.typeArguments &&
                attributes == other.attributes
    }

    /**
     * typealias 类型的结构哈希。
     */
    override fun hashCode(): Int {
        var result = classId.hashCode()
        result = 31 * result + typeArguments.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }


}
