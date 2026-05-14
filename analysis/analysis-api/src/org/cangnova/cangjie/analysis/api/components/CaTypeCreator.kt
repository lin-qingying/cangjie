package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.name.ClassId

/**
 * 类型构造协议。
 *
 * 设计要点/职责:
 * - 提供从 ClassId / Symbol / 元素类型出发,按声明式 builder 模式构造各类 [CaType] 的入口。
 * - 函数、元组、Intersection、Union 等复合类型直接以参数列表构造,避免暴露内部构造细节。
 * - 协议层只负责"构造"而不参与"求解",因此结果在语义上不进行类型推断或归一化。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeCreator`。
 */
interface CaTypeCreator : CaLifetimeOwner {
    /**
     * 基于 [ClassId] 构造一个类类型。
     *
     * 对泛型类，通过 [init] 块按声明顺序追加类型实参，调用方需要自行保证实参数量正确。
     *
     * 对内置类型推荐改用 [CaClassLikeSymbol] 重载，例如：
     * `buildClassType(builtinTypes.string)`。
     *
     * #### 示例
     *
     * ```kotlin
     * buildClassType(ClassId.fromString("std/collections/List")) {
     *     argument(buildClassType(ClassId.fromString("std/core/String")))
     * }
     * ```
     */
    fun buildClassType(classId: ClassId, init: CaClassTypeBuilder.() -> Unit = {}): CaType

    /**
     * 基于类符号 [symbol] 构造一个类类型。
     *
     * 对泛型类，通过 [init] 块按声明顺序追加类型实参，调用方需要自行保证实参数量正确。
     *
     * #### 示例
     *
     * ```kotlin
     * buildClassType(builtinTypes.string)
     * ```
     */
    fun buildClassType(symbol: CaClassLikeSymbol, init: CaClassTypeBuilder.() -> Unit = {}): CaType

    /**
     * 根据类型参数符号 [symbol] 构造一个 [CaTypeParameterType]。
     */
    fun buildTypeParameterType(symbol: CaTypeParameterSymbol, init: CaTypeParameterTypeBuilder.() -> Unit = {}): CaTypeParameterType

    /**
     * 构造函数类型;通过标志位区分常规函数、C 互操作函数、闭包以及变长参数函数。
     */
    fun buildFunctionType(
        parameterTypes: List<CaType>,
        returnType: CaType,
        isCFunction: Boolean = false,
        isClosureType: Boolean = false,
        hasVariableLengthArgument: Boolean = false,
    ): CaFunctionType

    /**
     * 构造元组类型,按 [elementTypes] 给定的顺序与元数。
     */
    fun buildTupleType(
        elementTypes: List<CaType>,
    ): CaTupleType

    /**
     * 构造由若干 [conjuncts] 组成的交叉类型(intersection type)。
     */
    fun buildIntersectionType(
        conjuncts: List<CaType>,
    ): CaIntersectionType

    /**
     * 构造由若干 [alternatives] 组成的联合类型(union type)。
     */
    fun buildUnionType(
        alternatives: Collection<CaType>,
    ): CaUnionType
}

/**
 * 类型 builder 的公共基接口;所有 builder 共享 lifetime 校验与公共能力插槽。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaTypeBuilder : CaLifetimeOwner


/**
 * 类类型 builder。
 *
 * @see CaTypeCreator.buildClassType
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaClassTypeBuilder : CaTypeBuilder {



    /**
     * 当前累计的类型实参列表(按追加顺序)。
     */
    val arguments: List<CaTypeProjection>

    /**
     * 追加一个类型投影 [argument] 作为类型实参。
     */
    fun argument(argument: CaTypeProjection)

    /**
     * 以给定类型 [type] 追加一个类型实参；具体型变信息按构造规则取默认值。
     */
    fun argument(type: CaType )
}

/**
 * 类型参数类型 builder。
 *
 * @see CaTypeCreator.buildTypeParameterType
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaTypeParameterTypeBuilder : CaTypeBuilder

