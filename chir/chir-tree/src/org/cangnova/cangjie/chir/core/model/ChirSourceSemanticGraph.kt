package org.cangnova.cangjie.chir.core.model

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/** CHIR 源语义图中四个彼此独立的顶层身份域。 */
enum class ChirSourceSemanticDomain {
    TYPE,
    VALUE,
    EXPRESSION,
    DEFINITION,
}

/** 一个源语义表在仓内的完整身份。 */
data class ChirSourceSemanticNodeId(
    val domain: ChirSourceSemanticDomain,
    val semanticId: ChirSemanticId,
)

/** 按 schema 保存的源语义表；字段值中的引用已经指向同一源图的规范身份。 */
data class ChirSourceSemanticTable(
    val schemaType: String,
    val fields: Map<String, ChirSourceSemanticValue>,
)

/**
 * 无损源语义字段值。
 *
 * 该模型保留 schema 表、union 判别值、字节向量和引用域，供格式转换、链接与审计使用；它不以展示文本
 * 替代源图中的语义字段。
 */
sealed interface ChirSourceSemanticValue {
    data object NullValue : ChirSourceSemanticValue

    data class Scalar(val value: Any) : ChirSourceSemanticValue

    data class Bytes(val value: List<Byte>) : ChirSourceSemanticValue

    data class Reference(val target: ChirSourceSemanticNodeId) : ChirSourceSemanticValue

    data class Sequence(val values: List<ChirSourceSemanticValue>) : ChirSourceSemanticValue

    data class Table(val value: ChirSourceSemanticTable) : ChirSourceSemanticValue

    data class Union(
        val discriminant: Int,
        val variant: String,
        val value: ChirSourceSemanticTable,
    ) : ChirSourceSemanticValue
}

/** 一个官方/前端源包的完整语义图及有序根向量。 */
data class ChirSourceSemanticGraph(
    val packageSemanticId: ChirSemanticId,
    val roots: Map<ChirSourceSemanticDomain, List<ChirSourceSemanticNodeId>>,
    val nodes: Map<ChirSourceSemanticNodeId, ChirSourceSemanticNode>,
    val metadata: Map<String, ChirSourceSemanticValue>,
)

/** 一个带顶层 union 判别值的源语义节点。 */
data class ChirSourceSemanticNode(
    val id: ChirSourceSemanticNodeId,
    val unionTag: UByte,
    val table: ChirSourceSemanticTable,
)
