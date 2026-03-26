/*
 * 仓颉编译器类型推断扩展上下文工具函数
 *
 * 本文件为 TypeSystemInferenceExtensionContext 接口中定义的成员函数
 * 提供顶层包装，使其可在无需显式 with(context) 的场景下直接调用。
 *
 * 改造说明（相对于 Kotlin 原版）：
 * - 所有 KotlinTypeMarker 替换为 CangJieTypeMarker
 * - 删除可空类型相关函数（仓颉无可空类型，Option<T> 是标准库类型）
 * - 删除型变相关函数（isContainedInInvariantOrContravariantPositions，
 *   仓颉无显式型变注解，形变由编译器内部控制）
 * - 删除星号投影相关函数（仓颉无投影概念）
 * - 删除 Raw 类型相关函数（convertToNonRaw、hasRawSuperTypeRecursive，
 *   Raw 类型是 Java 互操作产物，仓颉不涉及）
 * - 删除 @K2Only 注解（仓颉编译器无 K1/K2 之分）
 * - 删除挂起函数类型相关函数（isFunctionOrKFunctionWithAnySuspendability，
 *   仓颉协程模型与 Kotlin 不同，挂起函数类型不以此方式区分）
 * - 所有注释改为中文
 */

package org.cangnova.cangjie.type.model


// =====================================================================
// 类型内容检测
// =====================================================================

/**
 * 判断类型（递归地）是否包含满足谓词的子类型
 * 用于检测类型变量是否出现在某个位置
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.contains(predicate: (CangJieTypeMarker) -> Boolean): Boolean =
    with(c) { contains(predicate) }

/**
 * 将整型字面量类型构造器近似为具体整型类型
 * @param expectedType 期望类型，用于引导近似方向（如期望 Int 则选 Int）
 */
context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.getApproximatedIntegerLiteralType(expectedType: CangJieTypeMarker?): CangJieTypeMarker =
    with(c) { getApproximatedIntegerLiteralType(expectedType) }


/**
 * 擦除类型中出现的所有含类型参数的部分
 * 用于将泛型类型退化为其擦除形式，以便进行与泛型实参无关的子类型判断
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.eraseContainingTypeParameters(): CangJieTypeMarker =
    with(c) { eraseContainingTypeParameters() }

/**
 * 从候选类型集合中选出最具代表性的单一类型
 * 用于约束求解时从多个等价候选中取最优解
 */
context(c: TypeSystemInferenceExtensionContext)
fun Collection<CangJieTypeMarker>.singleBestRepresentative(): CangJieTypeMarker? =
    with(c) { singleBestRepresentative() }

// =====================================================================
// 常用类型谓词
// =====================================================================

/**
 * 判断该类型是否为 Unit（无返回值类型）
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.isUnit(): Boolean =
    with(c) { isUnit() }

/**
 * 判断该类型是否是内置函数类型或其子类型
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.isBuiltinFunctionTypeOrSubtype(): Boolean =
    with(c) { isBuiltinFunctionTypeOrSubtype() }

// =====================================================================
// 注解操作
// =====================================================================

/**
 * 移除类型上携带的所有注解
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.removeAnnotations(): CangJieTypeMarker =
    with(c) { removeAnnotations() }

/**
 * 移除类型上的 @Exact 注解（精确匹配注解，禁止子类型替换）
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.removeExactAnnotation(): CangJieTypeMarker =
    with(c) { removeExactAnnotation() }

/**
 * 判断类型是否带有 @Exact 注解
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.hasExactAnnotation(): Boolean =
    with(c) { hasExactAnnotation() }

/**
 * 判断类型是否带有 @NoInfer 注解（禁止参与类型推断）
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.hasNoInferAnnotation(): Boolean =
    with(c) { hasNoInferAnnotation() }

// =====================================================================
// 泛型实参替换
// 仓颉泛型实参无型变注解（in/out），实参就是一个具体类型
// =====================================================================

/**
 * 用新实参列表替换刚性类型的泛型实参
 */
context(c: TypeSystemInferenceExtensionContext)
fun RigidTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): RigidTypeMarker =
    with(c) { replaceArguments(newArguments) }

/**
 * 用变换函数逐一替换刚性类型的泛型实参
 */
context(c: TypeSystemInferenceExtensionContext)
fun RigidTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): RigidTypeMarker =
    with(c) { replaceArguments(replacement) }

/**
 * 对任意仓颉类型（刚性或弹性）替换泛型实参
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): CangJieTypeMarker =
    with(c) { replaceArguments(replacement) }

/**
 * 深度替换刚性类型中的泛型实参（递归进入嵌套泛型）
 * 例如：List<Map<T, U>> 中的 T、U 都会被替换
 */
context(c: TypeSystemInferenceExtensionContext)
fun RigidTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): RigidTypeMarker =
    with(c) { replaceArgumentsDeeply(replacement) }

/**
 * 对任意仓颉类型深度替换泛型实参
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): CangJieTypeMarker =
    with(c) { replaceArgumentsDeeply(replacement) }

// =====================================================================
// 类型构造器属性
// =====================================================================

/**
 * 判断该类型构造器是否对应一个不可继承的 final class 或 struct
 * 仓颉中 struct 默认不可继承（值类型语义），final class 同样不可继承
 */
context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.isFinalClassConstructor(): Boolean =
    with(c) { isFinalClassConstructor() }

/**
 * 判断该类型构造器是否是类型变量
 */
context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.isTypeVariable(): Boolean =
    with(c) { isTypeVariable() }

// =====================================================================
// 类型变量操作
// =====================================================================

/**
 * 为类型变量创建新鲜的类型构造器
 * 每次推断开始时调用，确保不同推断轮次的类型变量互不冲突
 */
context(c: TypeSystemInferenceExtensionContext)
fun TypeVariableMarker.freshTypeConstructor(): TypeVariableTypeConstructorMarker =
    with(c) { freshTypeConstructor() }

/**
 * 获取类型变量的默认类型（推断失败时的 fallback 类型）
 */
context(c: TypeSystemInferenceExtensionContext)
fun TypeVariableMarker.defaultType(): SimpleTypeMarker =
    with(c) { defaultType() }

// =====================================================================
// 捕获类型操作
// 仓颉无通配符/星号投影，捕获类型仅用于编译器内部推断约束的表示
// =====================================================================


// =====================================================================
// 交叉类型近似
// =====================================================================

/**
 * 获取交叉类型近似时使用的上界
 * 当交叉类型可能为空集时，用上界类型进行安全近似
 */
context(c: TypeSystemInferenceExtensionContext)
fun RigidTypeMarker.getUpperBoundForApproximationOfIntersectionType(): CangJieTypeMarker? =
    with(c) { getUpperBoundForApproximationOfIntersectionType() }

// =====================================================================
// 特殊类型与数值类型判断
// =====================================================================

/**
 * 判断该类型是否是编译器内部的特殊类型（如 FlexibleNothingType）
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.isSpecial(): Boolean =
    with(c) { isSpecial() }

/**
 * 判断该类型是否是有符号或无符号数值类型（Int、UInt、Long 等）
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.isSignedOrUnsignedNumberType(): Boolean =
    with(c) { isSignedOrUnsignedNumberType() }

// =====================================================================
// 函数类型工具
// 仓颉支持普通函数类型、扩展函数类型、上下文参数函数类型
// =====================================================================


/**
 * 判断是否是扩展函数类型（接收者.() -> R 形式）
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.isExtensionFunctionType(): Boolean =
    with(c) { isExtensionFunctionType() }

/**
 * 获取函数类型的上下文参数数量（context(A, B) 语法）
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.contextParameterCount(): Int =
    with(c) { contextParameterCount() }

/**
 * 提取函数类型或其子类型的参数类型列表（含返回值类型）
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<CangJieTypeMarker> =
    with(c) { extractArgumentsForFunctionTypeOrSubtype() }

/**
 * 从类型的父类型中提取出函数类型
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.getFunctionTypeFromSupertypes(): CangJieTypeMarker =
    with(c) { getFunctionTypeFromSupertypes() }

// =====================================================================
// 存根类型操作
// =====================================================================

/**
 * 从存根类型中还原出原始类型变量构造器
 * 推断结束后将存根类型还原为实际类型变量
 */
context(c: TypeSystemInferenceExtensionContext)
fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker =
    with(c) { getOriginalTypeVariable() }

// =====================================================================
// 类型变量与类型参数提取
// =====================================================================

/**
 * 提取类型中出现的所有类型变量构造器（递归）
 * 用于确定一个约束涉及哪些类型变量
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.extractTypeVariables(): Set<TypeVariableTypeConstructorMarker> =
    with(c) { extractTypeVariables() }

/**
 * 提取类型中出现的所有类型参数（递归）
 * 用于判断类型是否依赖某个泛型形参
 */
context(c: TypeSystemInferenceExtensionContext)
fun CangJieTypeMarker.extractTypeParameters(): Set<TypeParameterMarker> =
    with(c) { extractTypeParameters() }