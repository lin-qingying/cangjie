package org.cangjie.cfir.types

/**
 * 仓颉类型体系的根类。
 *
 * 基于仓颉编译器 Types.h 中的 Ty 类层次和 TypeKind.inc 中的类型种类，
 * 参考 Kotlin FIR 的 Cone 类型体系结构设计。
 *
 * 类型层次：
 * ConeCangjieType
 *   ├── ConeRigidType（确定类型）
 *   │   ├── ConePrimitiveType（Unit/Bool/Int/UInt/Float/Rune/Nothing/Ideal*）
 *   │   ├── ConeClassLikeType（class/interface 实例类型）
 *   │   ├── ConeStructType（struct 值类型）
 *   │   ├── ConeEnumType（enum 代数数据类型）
 *   │   ├── ConeFuncType（函数类型）
 *   │   ├── ConeTupleType（元组类型）
 *   │   ├── ConeArrayType（RawArray 数组类型）
 *   │   ├── ConeVArrayType（VArray 定长数组类型）
 *   │   ├── ConePointerType（CPointer 指针类型）
 *   │   ├── ConeCStringType（CString 类型）
 *   │   ├── ConeTypeParameterType（泛型参数类型）
 *   │   ├── ConeIntersectionType（交叉类型，内部使用）
 *   │   ├── ConeUnionType（联合类型，内部使用）
 *   │   ├── ConeAnyType（Any 顶类型，内部使用）
 *   │   └── ConeErrorType（错误类型）
 *   └── ConeFlexibleType（柔性类型，用于互操作）
 */
sealed class ConeCangjieType {
    /** 泛型类型实参。仓颉泛型是不变的，类型实参只能是具体类型。 */
    abstract val typeArguments: List<ConeCangjieType>
    abstract val attributes: ConeAttributes

    open val isUnit: Boolean get() = false
    open val isNothing: Boolean get() = false
    open val isError: Boolean get() = false
}

/**
 * 确定的（rigid）类型，代表一个具体已知的类型。
 */
sealed class ConeRigidType : ConeCangjieType() {
    override val typeArguments: List<ConeCangjieType> get() = emptyList()
}

/**
 * 柔性类型，用于 C 互操作等场景，表示一个类型范围。
 */
class ConeFlexibleType(
    val lowerBound: ConeRigidType,
    val upperBound: ConeRigidType,
) : ConeCangjieType() {
    override val typeArguments: List<ConeCangjieType> get() = lowerBound.typeArguments
    override val attributes: ConeAttributes get() = lowerBound.attributes

    override fun toString(): String = "flexible($lowerBound..$upperBound)"
}
