package org.cangnova.cangjie.cfir.types


/**
 * 仓颉类型体系的根类。
 *
 * 它是 CFIR 中唯一的统一类型表示根。
 *
 * 这一层要求区分四类角色：
 * 1. 稳定语义类型：语言本身真实存在并可长期保留的类型表示。
 * 2. 类型变量引用：推断过程中指向某个类型变量状态的引用视图。
 * 3. 占位/延迟类型：用于中层推断与求解阶段的过渡类型。
 * 4. 过渡推断类型：仍保留但后续会继续收敛职责边界的中间类型。
 *
 * 类型层次：
 * ConeCangJieType
 *   ├── ConeRigidType（确定类型）
 *   │   ├── 稳定语义类型
 *   │   │   ├── ConePrimitiveType（Unit/Bool/Int/UInt/Float/Rune/Nothing/Ideal*）
 *   │   │   ├── ConeClassLikeType（class/interface 实例类型）
 *   │   │   ├── ConeStructType（struct 值类型）
 *   │   │   ├── ConeEnumType（enum 代数数据类型）
 *   │   │   ├── ConeFuncType（函数类型）
 *   │   │   ├── ConeTupleType（元组类型）
 *   │   │   ├── ConeArrayType（RawArray 数组类型）
 *   │   │   ├── ConeVArrayType（VArray 定长数组类型）
 *   │   │   ├── ConePointerType（CPointer 指针类型）
 *   │   │   ├── ConeCStringType（CString 类型）
 *   │   │   ├── ConeTypeParameterType（泛型参数类型）
 *   │   │   ├── ConeIntersectionType（交叉类型，内部使用）
 *   │   │   ├── ConeUnionType（联合类型，内部使用）
 *   │   │   ├── ConeAnyType（Any 顶类型，内部使用）
 *   │   │   └── ConeErrorType（错误类型）
 *   │   ├── 类型变量引用
 *   │   │   ├── ConeTypeVariableRef（新的变量引用视图）
 *   │   │   └── ConeTypeVariableType（过渡兼容类型）
 *   │   ├── 占位/延迟类型
 *   │   │   ├── ConePlaceholderType
 *   │   │   └── ConeDeferredType
 *   │   └── 过渡推断类型
 *   │       ├── ConeStubType
 *   │       └── ConeCapturedType
 *
 * 当前稳定保留的具体类型集合包括：
 * - ConePrimitiveType
 * - ConeClassLikeType
 * - ConeStructType
 * - ConeEnumType
 * - ConeFuncType
 * - ConeTupleType
 * - ConeArrayType
 * - ConeVArrayType
 * - ConePointerType
 * - ConeCStringType
 * - ConeTypeParameterType
 * - ConeIntersectionType
 * - ConeUnionType
 * - ConeAnyType
 * - ConeQuestType
 * - ConeErrorType
 */
sealed class ConeCangJieType    {
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
sealed class ConeRigidType : ConeCangJieType() {
    /** 默认无泛型实参，子类按需覆盖 */
    override val typeArguments: List<ConeCangJieType> get() = emptyList()
}
