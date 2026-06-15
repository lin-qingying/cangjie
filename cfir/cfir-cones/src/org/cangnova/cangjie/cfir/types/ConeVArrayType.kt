package org.cangnova.cangjie.cfir.types

/**
 * 仓颉中的 `Array<T>` 是标准库名义类型，在 Cone 层直接用 [ConeClassLikeType] 表示即可。
 * 这里只有需要编译器显式建模的 `VArray<T, N>`，因为它携带编译期固定长度语义。
 */
class ConeVArrayType(
    val elementType: ConeCangJieType,
    /** 数组大小，必须是编译期常量。 */
    val size: Long,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {
    /**
     * VArray 在官方类型系统中以元素类型作为唯一 typeArg，size 属于类型头部。
     * 这里暴露给通用约束系统，保证 `VArray<α, N>` 能拆解出元素类型约束。
     */
    override val typeArguments: List<ConeTypeProjection> = listOf(elementType)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeVArrayType) return false
        return elementType == other.elementType && size == other.size
    }

    override fun hashCode(): Int {
        var result = elementType.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }

}
