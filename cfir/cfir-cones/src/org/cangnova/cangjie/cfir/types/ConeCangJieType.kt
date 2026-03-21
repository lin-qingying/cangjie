package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.RigidTypeMarker
import org.cangnova.cangjie.type.model.SimpleTypeMarker
import org.cangnova.cangjie.type.model.TypeArgumentListMarker
import org.cangnova.cangjie.type.model.TypeArgumentMarker

/**
 * 仓颉类型体系的根类。
 *
 * 基于仓颉编译器 Types.h 中的 Ty 类层次和 TypeKind.inc 中的类型种类，
 * 参考 Kotlin FIR 的 Cone 类型体系结构设计。
 *
 * 同时实现 [TypeArgumentMarker]：仓颉泛型实参就是类型本身（无型变注解），
 * 因此 ConeCangJieType 既是类型也是泛型实参。
 *
 * 类型层次：
 * ConeCangJieType
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
 *   │   ├── ConeErrorType（错误类型）
 *   │   ├── ConeTypeVariableType（类型变量类型，推断使用）
 *   │   ├── ConeStubType（存根类型，推断使用）
 *   │   └── ConeCapturedType（捕获类型，推断使用）
 */
sealed class ConeCangJieType : CangJieTypeMarker, TypeArgumentMarker, TypeArgumentListMarker {
    /** 泛型类型实参。仓颉泛型是不变的，类型实参只能是具体类型。 */
    abstract val typeArguments: List<ConeCangJieType>
    abstract val attributes: ConeAttributes

    open val isUnit: Boolean get() = false
    open val isNothing: Boolean get() = false
    open val isError: Boolean get() = false
}

/**
 * 确定的（rigid）类型，代表一个具体已知的类型。
 *
 * 同时实现 [SimpleTypeMarker]：仓颉不区分简单类型和刚性类型，
 * 所有刚性类型都是简单类型。
 */
sealed class ConeRigidType : ConeCangJieType(), RigidTypeMarker, SimpleTypeMarker {
    /** 默认无泛型实参，子类按需覆盖 */
    override val typeArguments: List<ConeCangJieType> get() = emptyList()
}