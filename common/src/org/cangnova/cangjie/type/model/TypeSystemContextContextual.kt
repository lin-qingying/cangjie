/*
 * 仓颉编译器类型系统上下文工具函数
 *
 * 本文件为 TypeSystemContext 接口中定义的成员函数
 * 提供顶层包装，使其可在无需显式 with(context) 的场景下直接调用。
 *
 * 改造说明（相对于 Kotlin 原版）：
 * - 所有 KotlinTypeMarker 替换为 CangJieTypeMarker
 * - 删除弹性类型相关函数：asFlexibleType、upperBound、lowerBound、
 *   lowerBoundIfFlexible、upperBoundIfFlexible、isFlexible、
 *   isFlexibleWithDifferentTypeConstructors、isFlexibleNothing
 *   （仓颉所有类型均为刚性类型，C 互操作通过 CType 约束完全确定）
 * - 删除可空类型相关函数：isMarkedNullable、withNullability 等
 * - 删除 DefinitelyNotNullType 相关函数
 * - 删除 DynamicType、Raw 类型相关函数
 * - 删除星号投影、型变相关函数
 * - 所有注释改为中文
 */

package org.cangnova.cangjie.type.model

// =====================================================================
// 无意义调用的错误提示常量
// =====================================================================

/** 对已知类型调用转换函数时的编译期错误提示 */
private const val USELESS_CALL_MESSAGE = "此调用无实际效果，请直接使用该值"

// =====================================================================
// 类型转换（安全 downcast）
// =====================================================================

/** 尝试将任意仓颉类型转为刚性类型，失败返回 null */
context(c: TypeSystemContext)
fun CangJieTypeMarker.asRigidType(): RigidTypeMarker? = with(c) { asRigidType() }

/**
 * @deprecated 对 RigidTypeMarker 调用此方法是无意义的，请直接使用该值
 */
@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun RigidTypeMarker.asRigidType(): RigidTypeMarker = this

// =====================================================================
// 错误与未推断类型判断
// =====================================================================

/** 判断类型是否是错误类型（类型检查失败的占位） */
context(c: TypeSystemContext)
fun CangJieTypeMarker.isError(): Boolean = with(c) { isError() }

/** 判断类型构造器是否来自错误类型 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isError(): Boolean = with(c) { isError() }

/** 判断类型是否是尚未推断的参数类型 */
context(c: TypeSystemContext)
fun CangJieTypeMarker.isUninferredParameter(): Boolean = with(c) { isUninferredParameter() }




// =====================================================================
// 泛型实参访问
// 仓颉泛型实参无型变注解（in/out），实参就是一个具体类型
// =====================================================================

/** 获取类型的泛型实参数量 */
context(c: TypeSystemContext)
fun CangJieTypeMarker.argumentsCount(): Int = with(c) { argumentsCount() }

/** 获取指定位置的泛型实参 */
context(c: TypeSystemContext)
fun CangJieTypeMarker.getArgument(index: Int): TypeArgumentMarker = with(c) { getArgument(index) }

/** 获取所有泛型实参列表 */
context(c: TypeSystemContext)
fun CangJieTypeMarker.getArguments(): List<TypeArgumentMarker> = with(c) { getArguments() }

/** 安全地获取指定位置的泛型实参（越界返回 null） */
context(c: TypeSystemContext)
fun RigidTypeMarker.getArgumentOrNull(index: Int): TypeArgumentMarker? =
    with(c) { getArgumentOrNull(index) }

/**
 * 获取泛型实参的类型
 * 仓颉无星号投影，所以此方法始终返回非 null 的具体类型
 */
context(c: TypeSystemContext)
fun TypeArgumentMarker.getType(): CangJieTypeMarker? = with(c) { getType() }

/** 替换泛型实参的类型（保持其他属性不变） */
context(c: TypeSystemContext)
fun TypeArgumentMarker.replaceType(newType: CangJieTypeMarker): TypeArgumentMarker =
    with(c) { replaceType(newType) }

// =====================================================================
// 存根类型判断
// =====================================================================

/** 判断是否是存根类型（推断中间占位） */
context(c: TypeSystemContext)
fun RigidTypeMarker.isStubType(): Boolean = with(c) { isStubType() }

/** 判断是否是用于子类型检查的存根类型 */
context(c: TypeSystemContext)
fun RigidTypeMarker.isStubTypeForVariableInSubtyping(): Boolean =
    with(c) { isStubTypeForVariableInSubtyping() }



/** 判断是否是用于 Builder 推断的存根类型 */
context(c: TypeSystemContext)
fun RigidTypeMarker.isStubTypeForBuilderInference(): Boolean =
    with(c) { isStubTypeForBuilderInference() }

/** 解包存根类型构造器，还原出底层的类型变量构造器 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker =
    with(c) { unwrapStubTypeVariableConstructor() }

// =====================================================================
// 类型与类型实参互转
// =====================================================================

/** 将类型转为泛型实参（用于嵌套泛型场景） */
context(c: TypeSystemContext)
fun CangJieTypeMarker.asTypeArgument(): TypeArgumentMarker = with(c) { asTypeArgument() }

// =====================================================================
// 类型构造器属性访问
// =====================================================================

/** 获取类型构造器的泛型参数数量 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.parametersCount(): Int = with(c) { parametersCount() }

/** 获取指定位置的类型参数 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker = with(c) { getParameter(index) }

/** 获取所有类型参数列表 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker> = with(c) { getParameters() }

/** 获取类型构造器的直接父类型列表 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.supertypes(): Collection<CangJieTypeMarker> = with(c) { supertypes() }

/** 判断是否是交叉类型构造器（A & B） */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntersection(): Boolean = with(c) { isIntersection() }

/**
 * 判断是否是 class 或 struct 的类型构造器
 * 仓颉区分值类型（struct）和引用类型（class），两者都可实现接口
 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isClassTypeConstructor(): Boolean = with(c) { isClassTypeConstructor() }

/** 判断是否是 interface 的类型构造器 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isInterface(): Boolean = with(c) { isInterface() }

/** 判断是否是整型字面量类型构造器（用于字面量推断） */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean =
    with(c) { isIntegerLiteralTypeConstructor() }

/** 判断是否是整型字面量常量类型构造器 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean =
    with(c) { isIntegerLiteralConstantTypeConstructor() }

/** 判断是否是整型常量运算类型构造器 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean =
    with(c) { isIntegerConstantOperatorTypeConstructor() }

/** 判断是否是匿名类型 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isAnonymous(): Boolean = with(c) { isAnonymous() }

/** 若该类型构造器来自类型参数，返回对应的类型参数标记 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? =
    with(c) { getTypeParameterClassifier() }

/** 判断是否是类型参数的类型构造器（对应泛型形参 T） */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean =
    with(c) { isTypeParameterTypeConstructor() }

/** 获取类型变量构造器关联的类型参数（如有） */
context(c: TypeSystemContext)
val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
    get() = with(c) { typeParameter }

// =====================================================================
// 类型参数属性访问
// 仓颉无显式型变注解，此处仅保留上界约束相关操作
// =====================================================================

/** 获取类型参数的上界数量（如 T <: A & B 则为 2） */
context(c: TypeSystemContext)
fun TypeParameterMarker.upperBoundCount(): Int = with(c) { upperBoundCount() }

/** 获取指定位置的上界类型 */
context(c: TypeSystemContext)
fun TypeParameterMarker.getUpperBound(index: Int): CangJieTypeMarker = with(c) { getUpperBound(index) }

/** 获取所有上界类型列表 */
context(c: TypeSystemContext)
fun TypeParameterMarker.getUpperBounds(): List<CangJieTypeMarker> = with(c) { getUpperBounds() }

/** 获取类型参数对应的类型构造器 */
context(c: TypeSystemContext)
fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker = with(c) { getTypeConstructor() }

/** 判断类型参数是否有递归上界（如 T <: Comparable<T>） */
context(c: TypeSystemContext)
fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker? = null): Boolean =
    with(c) { hasRecursiveBounds(selfConstructor) }

// =====================================================================
// 类型构造器相等性与可表示性
// =====================================================================

/** 判断类型构造器是否是可表示类型（非内部合成类型） */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isDenotable(): Boolean = with(c) { isDenotable() }

// =====================================================================
// 常用类型谓词
// =====================================================================

/** 获取类型的类型构造器（仓颉所有类型均为刚性类型，直接转型） */
context(c: TypeSystemContext)
fun CangJieTypeMarker.typeConstructor(): TypeConstructorMarker = with(c) { typeConstructor() }

/** 获取刚性类型的类型构造器 */
context(c: TypeSystemContext)
fun RigidTypeMarker.typeConstructor(): TypeConstructorMarker = with(c) { typeConstructor() }

/** 判断是否是 Nothing（不可达底层类型，是所有类型的子类型） */
context(c: TypeSystemContext)
fun CangJieTypeMarker.isNothing() = with(c) { isNothing() }

/**
 * 判断是否是 class 或 struct 实例化的类型
 * 仓颉区分值类型（struct）和引用类型（class），两者都满足此判断
 */
context(c: TypeSystemContext)
fun RigidTypeMarker.isClassType(): Boolean = with(c) { isClassType() }

/** 快速查找当前类型中满足给定构造器的父类型列表（优化路径，可选实现） */
context(c: TypeSystemContext)
fun RigidTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? =
    with(c) { fastCorrespondingSupertypes(constructor) }

/** 判断是否是整型字面量类型 */
context(c: TypeSystemContext)
fun RigidTypeMarker.isIntegerLiteralType(): Boolean = with(c) { isIntegerLiteralType() }

/** 获取整型字面量类型所有可能的具体整型类型 */
context(c: TypeSystemContext)
fun RigidTypeMarker.possibleIntegerTypes(): Collection<CangJieTypeMarker> =
    with(c) { possibleIntegerTypes() }

/** 判断类型构造器是否对应不可被继承的 final class 或不可继承的 struct */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean =
    with(c) { isCommonFinalClassConstructor() }

// =====================================================================
// 内置类型构造器判断
// =====================================================================

/** 判断是否是 Any（顶层类型）的类型构造器 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isAnyConstructor(): Boolean = with(c) { isAnyConstructor() }

/** 判断是否是 Nothing（底层类型）的类型构造器 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isNothingConstructor(): Boolean = with(c) { isNothingConstructor() }

/** 判断是否是 Array 的类型构造器 */
context(c: TypeSystemContext)
fun TypeConstructorMarker.isArrayConstructor(): Boolean = with(c) { isArrayConstructor() }

// =====================================================================
// 值类型/引用类型相关
// 仓颉区分 class（引用类型）和 struct（值类型），两者都可实现接口
// =====================================================================

/**
 * 判断是否是单一类型构造器类型（非交叉类型）
 * 仓颉的 class 和 struct 实例化后均满足此条件
 */
context(c: TypeSystemContext)
fun RigidTypeMarker.isSingleClassifierType(): Boolean = with(c) { isSingleClassifierType() }

/** 判断是否是刚性类型 */
context(c: TypeSystemContext)
fun CangJieTypeMarker.isRigidType(): Boolean = with(c) { isRigidType() }

/** 判断是否是原始值类型（Int、Float、Bool 等内置值类型） */
context(c: TypeSystemContext)
fun RigidTypeMarker.isPrimitiveType(): Boolean = with(c) { isPrimitiveType() }

/** 判断简单类型是否是原始值类型 */
context(c: TypeSystemContext)
fun SimpleTypeMarker.isPrimitiveType(): Boolean = with(c) { isPrimitiveType() }

// =====================================================================
// 泛型实参列表操作
// =====================================================================

/** 将刚性类型转为泛型实参列表视图（用于遍历泛型实参） */
context(c: TypeSystemContext)
fun RigidTypeMarker.asArgumentList(): TypeArgumentListMarker = with(c) { asArgumentList() }

/** 按下标访问泛型实参列表中的元素 */
context(c: TypeSystemContext)
operator fun TypeArgumentListMarker.get(index: Int) = with(c) { get(index) }

/** 获取泛型实参列表的长度 */
context(c: TypeSystemContext)
fun TypeArgumentListMarker.size(): Int = with(c) { size() }

/** 为泛型实参列表提供迭代器 */
context(c: TypeSystemContext)
operator fun TypeArgumentListMarker.iterator() = with(c) { iterator() }

// =====================================================================
// 注解与类型变量相关
// =====================================================================

/** 获取类型携带的注解列表（用于反射，不干预推断） */
context(c: TypeSystemContext)
fun CangJieTypeMarker.getAttributes(): List<AnnotationMarker> = with(c) { getAttributes() }

/** 判断是否是类型变量类型（推断过程中的未知类型） */
context(c: TypeSystemContext)
fun CangJieTypeMarker.isTypeVariableType(): Boolean = with(c) { isTypeVariableType() }

// =====================================================================
// 类型替换
// =====================================================================

/**
 * 安全地对给定类型执行替换
 * @return 替换后的类型，若无需替换则返回原类型
 */
context(c: TypeSystemContext)
fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker =
    with(c) { safeSubstitute(type) }

// =====================================================================
// 函数类型与元组类型工具（TypeSystemCommonSuperTypesContext）
// CST 计算特化路径所需
// =====================================================================

/**
 * 判断类型构造器对应的类型声明是否可访问。
 * 对齐 C++ ImportManager::IsTyAccessible。
 */
context(c: TypeSystemCommonSuperTypesContext)
fun TypeConstructorMarker.isTypeAccessible(): Boolean = with(c) { isTypeAccessible() }

/** 判断该类型是否是函数类型 */
context(c: TypeSystemCommonSuperTypesContext)
fun CangJieTypeMarker.isFunctionType(): Boolean = with(c) { isFunctionType() }

/** 提取函数类型的参数类型列表（最后一个元素为返回值类型） */
context(c: TypeSystemCommonSuperTypesContext)
fun CangJieTypeMarker.extractArgumentsForFunctionType(): List<CangJieTypeMarker> =
    with(c) { extractArgumentsForFunctionType() }

/** 判断该类型是否是元组类型 */
context(c: TypeSystemCommonSuperTypesContext)
fun CangJieTypeMarker.isTupleType(): Boolean = with(c) { isTupleType() }

/** 提取元组类型的元素类型列表 */
context(c: TypeSystemCommonSuperTypesContext)
fun CangJieTypeMarker.extractElementsForTupleType(): List<CangJieTypeMarker> =
    with(c) { extractElementsForTupleType() }
