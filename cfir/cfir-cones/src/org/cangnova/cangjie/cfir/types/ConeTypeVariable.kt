package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.TypeParameterMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker
import kotlin.hashCode
import kotlin.toString

open class ConeTypeVariable(name: String, originalTypeParameter: TypeParameterMarker? = null) : TypeVariableMarker {
    val typeConstructor:  ConeTypeVariableTypeConstructor = ConeTypeVariableTypeConstructor(name, originalTypeParameter)
    val defaultType:ConeTypeVariableType = ConeTypeVariableType(  typeConstructor)

    override fun toString(): String {
        return defaultType.toString()
    }
}
/**
 * 类型变量在类型图中的兼容引用壳。
 *
 * 语义上它仍然是“一个待求解的类型变量”，
 * 但为了兼容现有调用点和测试，这一层继续保留。
 */
class ConeTypeVariableType(
    val typeConstructor: ConeTypeVariableTypeConstructor,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeSimpleCangJieType() {
    override val typeArguments: List<  ConeTypeProjection> get() = emptyList()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeTypeVariableType) return false

        if (typeConstructor != other.typeConstructor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + typeConstructor.hashCode()
        return result
    }
}