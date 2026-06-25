package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.TypeParameterMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker
import kotlin.hashCode
import kotlin.toString

/**
 * 类型推断过程中创建的 Cone 类型变量。
 *
 * @param name 调试名，通常来自原始类型参数。
 * @param originalTypeParameter 该变量对应的源码类型参数。
 */
open class ConeTypeVariable(name: String, originalTypeParameter: TypeParameterMarker? = null) : TypeVariableMarker {
    /**
     * 当前类型变量的构造器身份。
     */
    val typeConstructor:  ConeTypeVariableTypeConstructor = ConeTypeVariableTypeConstructor(name, originalTypeParameter)

    /**
     * 引用当前类型变量的默认 Cone 类型壳。
     */
    val defaultType:ConeTypeVariableType = ConeTypeVariableType(  typeConstructor)

    /**
     * 使用默认类型壳输出调试文本。
     */
    override fun toString(): String {
        return defaultType.toString()
    }
}

/**
 * 类型变量在类型图中的兼容引用壳。
 *
 * 语义上它仍然是“一个待求解的类型变量”，
 * 但为了兼容现有调用点和测试，这一层继续保留。
 *
 * @property typeConstructor 类型变量构造器身份。
 * @property attributes 类型变量引用携带的属性。
 */
class ConeTypeVariableType(
    /** 类型变量构造器身份。 */
    val typeConstructor: ConeTypeVariableTypeConstructor,
    /** 类型变量引用携带的属性。 */
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeSimpleCangJieType() {
    /**
     * 类型变量引用不携带类型实参。
     */
    override val typeArguments: List<  ConeTypeProjection> get() = emptyList()

    /**
     * 类型变量引用按构造器身份判等。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeTypeVariableType) return false

        if (typeConstructor != other.typeConstructor) return false

        return true
    }

    /**
     * 类型变量引用的结构哈希。
     */
    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + typeConstructor.hashCode()
        return result
    }
}
