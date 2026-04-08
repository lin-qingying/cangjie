package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * Analysis API 的公开类型抽象。
 *
 * 类型是 Analysis API 中与 symbol 并列的核心语义载体。
 * 公开 API 不应该只暴露一个字符串化的“黑盒类型”，
 * 因此这里将常见类型族拆分为稳定的公开子接口：
 * - [CaClassLikeType]
 * - [CaFunctionType]
 * - [CaTupleType]
 * - [CaIntersectionType]
 * - [CaUnionType]
 *
 * 每个具体后端都必须把底层类型系统映射到这些公开类型族，
 * 以保证 IDE、LSP、测试框架和重构工具基于同一套语义模型工作。
 */
interface CaType : CaLifetimeOwner {
    /**
     * 当前类型的稳定文本表示。
     *
     * 该属性用于渲染、日志和测试输出；
     * 结构化消费应优先使用下方的公开子接口，而不是反向解析文本。
     */
    val presentation: String
}

/**
 * 具名 class-like 类型。
 *
 * 对应 class / interface / struct / enum / typealias 这类基于 `ClassId` 标识的类型族。
 */
interface CaClassLikeType : CaType {
    /**
     * 当前类型对应的 class-like 声明 ID。
     */
    val classId: ClassId

    /**
     * 当前类型的类型实参。
     *
     * 当前阶段统一按不变语义建模公开类型实参；
     * 如果后续仓颉类型系统公开 variance，再在这层扩展投影模型。
     */
    val typeArguments: List<CaType>

    /**
     * 当前类型可稳定恢复到的公开 class-like 符号。
     *
     * 对于当前 session 中不可见或不可恢复的类型，允许返回 `null`。
     */
    val symbol: CaClassLikeSymbol?
}

/**
 * 函数类型 `(P1, P2, ...) -> R`。
 */
interface CaFunctionType : CaType {
    val parameterTypes: List<CaType>

    val returnType: CaType

    val isCFunction: Boolean

    val isClosureType: Boolean

    val hasVariableLengthArgument: Boolean
}

/**
 * 元组类型 `(T1, T2, ...)`。
 */
interface CaTupleType : CaType {
    val elementTypes: List<CaType>
}

/**
 * 交叉类型 `A & B & ...`。
 */
interface CaIntersectionType : CaType {
    val conjuncts: List<CaType>
}

/**
 * 联合类型 `A | B | ...`。
 */
interface CaUnionType : CaType {
    val alternatives: List<CaType>
}
