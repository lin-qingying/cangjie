/*
 * 仓颉编译器后端公共类型系统上下文
 *
 * 本文件定义后端代码生成阶段所需的类型操作接口。
 *
 * 改造说明（相对于 Kotlin 原版）：
 * - 所有 KotlinTypeMarker 替换为 CangJieTypeMarker
 * - 删除 nullableAnyType()：仓颉无可空类型
 * - 删除 isArrayOrNullableArray()：改为 isArray()
 * - 删除 makeNullable()：仓颉无可空类型
 * - 删除 getPrimitiveType() / getPrimitiveArrayType()：JVM 字节码映射产物
 * - 删除 isUnderKotlinPackage()：仓颉无 kotlin.* 包
 * - 删除 isInnerClass()：仓颉无嵌套类
 * - 删除 isInlineClass()：仓颉无 inline class，值类型通过 isValueTypeConstructor() 判断
 * - 删除 getValueClassProperties()：同上，inline class / value class 是 Kotlin 概念
 * - 删除 isFinalClassOrEnumEntryOrAnnotationClassConstructor() 中的 EnumEntry 部分：
 *   仓颉 enum 是独立的顶层类型，无"枚举条目作为子类型"的概念
 *   → 改名为 isFinalClassOrAnnotationClassConstructor()
 * - 删除 isMultiFieldValueClass()：Kotlin 专属
 * - 删除 isSuspendFunction()：仓颉无挂起函数类型
 * - 删除 continuationTypeConstructor()：仓颉不涉及
 * - 删除 functionNTypeConstructor(n)：仓颉函数类型是内置类型
 * - 删除 isKClass()：KClass 是 Kotlin 反射类型，仓颉使用 TypeInfo
 * - 所有注释改为中文
 */

package org.cangnova.cangjie.types

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.model.*

/**
 * 后端公共类型系统上下文
 *
 * 供代码生成后端使用，在类型系统抽象之上提供后端所需的额外类型操作。
 * 仓颉后端（如 LLVM IR 生成）通过此接口查询类型的后端相关属性。
 */
interface TypeSystemCommonBackendContext : TypeSystemContext {

    /**
     * 根据元素类型创建数组类型
     * 仓颉数组类型为 Array<T>，T 必须是具体类型
     */
    fun arrayType(componentType: CangJieTypeMarker): SimpleTypeMarker

    /**
     * 判断类型是否是数组类型（Array<T>）
     * 仓颉数组本身是引用类型（class），无可空数组的概念
     */
    fun CangJieTypeMarker.isArray(): Boolean

    /**
     * 判断类型构造器是否对应 final class 或注解类
     *
     * - final class：不可被继承的引用类型，虚表布局固定
     * - 注解类：@Annotation 修饰的 class，后端需特殊处理元数据
     *
     * 注意：仓颉 enum 是独立的顶层类型，enum 构造器（枚举值）不单独作为子类型，
     * 因此不包含原 Kotlin 版本中的 EnumEntry 判断
     */
    fun TypeConstructorMarker.isFinalClassOrAnnotationClassConstructor(): Boolean

    /**
     * 判断类型是否带有指定全限定名的注解
     * 用于后端根据注解触发特殊代码生成逻辑（如 @Intrinsic、@Volatile 等）
     */
    fun CangJieTypeMarker.hasAnnotation(fqName: FqName): Boolean

    /**
     * 获取指定注解的第一个参数值
     *
     * @return 若注解存在且第一个参数是原始类型或 String，则返回其值；否则返回 null
     *
     * 注意：即使注解参数有默认值，若调用处未显式传参，也返回 null
     * TODO: 提供更细粒度的 API 以减少歧义
     */
    fun CangJieTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any?

    /**
     * 获取类型参数具有代表性的上界
     * 当类型参数有多个上界时，选取后端代码生成最有意义的那个
     * （通常是第一个非 Any 的上界，或 Any 本身）
     */
    fun TypeParameterMarker.getRepresentativeUpperBound(): CangJieTypeMarker

    /**
     * 获取值类型（struct）未经替换的底层存储类型
     * 用于后端在内联优化时将 struct 展开为其原始存储形式
     * 若不是值类型，返回 null
     */
    fun CangJieTypeMarker.getUnsubstitutedUnderlyingType(): CangJieTypeMarker?

    /**
     * 为值类型底层类型创建类型替换器
     * 将类型参数替换为具体类型后，得到底层类型的实际形式
     * 默认使用基于类型构造器映射的替换器
     */
    fun typeSubstitutorForUnderlyingType(map: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker =
        typeSubstitutorByTypeConstructor(map)

    /**
     * 获取类型构造器所在包的全限定名（不安全形式，可能包含特殊字符）
     * 后端用于生成 mangled 名称或调试符号信息
     */
    fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe?

    /**
     * 获取类型参数的名称（如泛型声明 class A<T> 中的 T）
     * 后端生成调试信息和错误消息时使用
     */
    fun TypeParameterMarker.getName(): Name

    /**
     * 判断类型参数是否是具体化（reified）类型参数
     * 仓颉中具体化类型参数在运行时保留类型信息，
     * 后端需要为其生成不同的代码（传递类型令牌）
     */
    fun TypeParameterMarker.isReified(): Boolean

    /**
     * 判断类型是否是 interface 或注解类
     * 后端据此决定派发策略：interface 使用接口表（itable）派发
     */
    fun CangJieTypeMarker.isInterfaceOrAnnotationClass(): Boolean
}

/**
 * 后端类型映射专用上下文
 *
 * 在 TypeSystemCommonBackendContext 基础上，额外提供类型映射（type mapping）
 * 所需的操作。类型映射是将仓颉类型系统中的类型转换为目标平台（如 LLVM IR）
 * 所需类型表示的过程。
 */
interface TypeSystemCommonBackendContextForTypeMapping : TypeSystemCommonBackendContext {

    /**
     * 返回顶层类型 Any
     * 类型映射阶段在无法确定具体类型时使用 Any 作为 fallback
     */
    fun anyType(): SimpleTypeMarker

    /**
     * 判断类型构造器是否是类型参数（泛型形参）
     * 类型映射阶段需要特殊处理类型参数（通常映射为其上界类型）
     */
    fun TypeConstructorMarker.isTypeParameter(): Boolean

    /**
     * 将类型构造器转为类型参数标记
     * 调用前应先确认 isTypeParameter() 返回 true
     */
    fun TypeConstructorMarker.asTypeParameter(): TypeParameterMarker

    /**
     * 获取类型构造器的默认实例化类型（不含泛型实参）
     * 如 class List<T> 的默认类型为 List（未指定 T）
     */
    fun TypeConstructorMarker.defaultType(): CangJieTypeMarker

    /**
     * 判断类型构造器是否是脚本类型
     * 仓颉支持脚本模式，脚本顶层代码被包装为特殊类型
     */
    fun TypeConstructorMarker.isScript(): Boolean

    /**
     * 用给定实参列表实例化类型构造器，得到具体类型
     * 如：将 List 和 [Int] 组合为 List<Int>
     */
    fun TypeConstructorMarker.typeWithArguments(arguments: List<CangJieTypeMarker>): SimpleTypeMarker

    /** typeWithArguments 的变长参数重载 */
    fun TypeConstructorMarker.typeWithArguments(vararg arguments: CangJieTypeMarker): SimpleTypeMarker {
        return typeWithArguments(arguments.toList())
    }

    /**
     * 获取泛型实参的调整后类型
     * 仓颉无星号投影，泛型实参始终有具体类型
     * 若实参类型为 null（理论上不应发生），返回 anyType() 作为安全 fallback
     */
    fun TypeArgumentMarker.adjustedType(): CangJieTypeMarker {
        return getType() ?: anyType()
    }

    /**
     * 获取类型参数用于代码生成的代表性上界
     * 专门用于类型映射阶段，会进行必要的展开和规范化
     */
    fun TypeParameterMarker.representativeUpperBound(): CangJieTypeMarker

    /**
     * 获取错误类型的名称字符串（用于错误报告和调试信息生成）
     * 若类型不是错误类型，返回 null
     */
    fun CangJieTypeMarker.getNameForErrorType(): String?
}