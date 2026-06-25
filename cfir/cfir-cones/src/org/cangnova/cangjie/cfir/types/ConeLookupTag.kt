package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.model.TypeParameterMarker
import org.cangnova.cangjie.type.model.TypeVariableTypeConstructorMarker

// ================================
// Lookup Tag 基础层
// ================================

/**
 * 分类器查找 tag 的抽象基类。
 *
 * lookup tag 是编译器在符号表中定位具名类型声明的句柄，
 * 本身不携带类型实参，与类型实参组合后才构成完整的 [ConeLookupTagBasedType]。
 *
 * 子类按是否携带 [ClassId] 分为两支：
 * - [ConeClassLikeLookupTag]：class/interface/struct/enum/typealias 等有 ClassId 的声明
 * - 其他：如类型参数对应的 tag，只有名字没有 ClassId
 */
abstract class ConeClassifierLookupTag : ConeTypeConstructorMarker {
    /**
     * 声明的简单名称，用于调试输出和错误信息。
     */
    abstract val name: Name

    /**
     * lookup tag 的默认调试输出使用简单名称。
     */
    override fun toString(): String = name.asString()
}

/**
 * 携带 [ClassId] 的 lookup tag 抽象基类。
 *
 * [ClassId] = 包名 + 相对类名，能在整个编译单元中唯一标识一个顶层或嵌套声明。
 * [name] 直接从 [ClassId.shortClassName] 派生，无需单独存储。
 */
abstract class ConeClassLikeLookupTag : ConeClassifierLookupTag() {
    /**
     * 该声明的全局唯一标识符，包含包路径和类名层级。
     */
    abstract val classId: ClassId

    /**
     * 短类名，直接委托给 classId，避免冗余字段。
     */
    override val name: Name
        get() = classId.shortClassName
}



// ================================
// 类型参数 Lookup Tag
// ================================



// ================================
// 类型变量构造器（推断期临时占位）
// ================================

/**
 * 类型推断期间使用的类型变量构造器。
 *
 * 类型变量是推断算法引入的临时占位符，代表一个尚未确定的具体类型，
 * 推断结束后会被替换为真实类型。
 *
 * 与真实类型参数（[TypeParameterMarker]）的区别：
 * - 类型参数来自源码声明，有稳定的符号身份。
 * - 类型变量由推断引擎动态创建，生命周期仅限于一次推断会话。
 *
 * 因此本类使用引用相等作为身份判定，不需要基于内容的 equals/hashCode。
 *
 * @param debugName 调试用名称，通常来自对应类型参数的名字（如 `"T"`）。
 * @param originalTypeParameter 该类型变量所对应的原始类型参数，可为 null。
 */
class ConeTypeVariableTypeConstructor(
    /** 调试用名称，通常来自对应类型参数的名字。 */
    val debugName: String,
    /** 该类型变量所对应的原始类型参数，可为 null。 */
    val originalTypeParameter: TypeParameterMarker?,
) : TypeVariableTypeConstructorMarker, ConeTypeConstructorMarker {

    /**
     * 将 debugName 包装为 [Name]，供需要统一 Name 类型的接口使用。
     */
    val name: Name get() = Name.identifier(debugName)

    /**
     * 该类型变量是否曾出现在不变（invariant）或逆变（contravariant）位置。
     *
     * 推断引擎在收集约束时会记录此信息，用于决定：
     * - 出现在协变位置 → 可安全地取上界（upper bound）作为近似结果。
     * - 出现在不变/逆变位置 → 近似时需更保守，避免引入不健全的子类型关系。
     *
     * 初始为 false，一旦检测到相关用法即置为 true，且不可逆（单调写入）。
     */
    var isContainedInInvariantOrContravariantPositions: Boolean = false
        private set

    /**
     * 标记该类型变量曾在不变或逆变位置使用。
     * 调用后 [isContainedInInvariantOrContravariantPositions] 永久置为 true。
     */
    fun recordInfoAboutTypeVariableUsagesAsInvariantOrContravariantParameter() {
        isContainedInInvariantOrContravariantPositions = true
    }

    /**
     * 调试输出，格式为 `ConeTypeVariableTypeConstructor(T)`。
     */
    override fun toString(): String = "${this::class.simpleName}($debugName)"
}
