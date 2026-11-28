package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import java.util.Collections
import java.util.IdentityHashMap

/** 原常量对象只作不透明身份；不同但字段相同的对象不能合并。 */
class CfirExportConstantIdentity internal constructor(private val source: CfirConstantValue) {
    override fun equals(other: Any?): Boolean = other is CfirExportConstantIdentity && source === other.source
    override fun hashCode(): Int = System.identityHashCode(source)
    internal fun validate(graph: CfirExportConstantGraph) {
        val current = CfirExportConstantGraph.capture(source)
        check(current.nodes == graph.nodes && current.nodes.indices.all { index ->
            val before = graph.nodes[index]
            val after = current.nodes[index]
            before.type === after.type && (before !is CfirExportConstantNode.Object ||
                after is CfirExportConstantNode.Object && before.fields.indices.all { before.fields[it].type === after.fields[it].type })
        }) { "Evaluated constant content or storage-type identity changed after capture" }
    }
    companion object {
        /** 已完成值与格式存储事实共享此身份规则；token 不暴露值或允许改写节点。 */
        fun of(value: CfirConstantValue): CfirExportConstantIdentity = CfirExportConstantIdentity(value)
    }
}

/** 原值图的独立不可变记录；引用使用身份 token，支持共享及循环而不嵌套递归复制。 */
sealed interface CfirExportConstantNode {
    val identity: CfirExportConstantIdentity
    val type: ConeCangJieType

    data class Primitive(
        override val identity: CfirExportConstantIdentity,
        override val type: ConeCangJieType,
        val kind: CfirLiteralKind,
        val value: Any,
    ) : CfirExportConstantNode

    data class Tuple(override val identity: CfirExportConstantIdentity, override val type: ConeCangJieType, val elements: List<CfirExportConstantIdentity>) : CfirExportConstantNode
    data class Array(override val identity: CfirExportConstantIdentity, override val type: ConeCangJieType, val elements: List<CfirExportConstantIdentity>) : CfirExportConstantNode
    data class VArray(override val identity: CfirExportConstantIdentity, override val type: ConeCangJieType, val elements: List<CfirExportConstantIdentity>) : CfirExportConstantNode
    data class Enum(
        override val identity: CfirExportConstantIdentity,
        override val type: ConeCangJieType,
        val constructor: CfirEnumConstructorSymbol,
        val arguments: List<CfirExportConstantIdentity>,
    ) : CfirExportConstantNode
    data class Object(
        override val identity: CfirExportConstantIdentity,
        override val type: ConeCangJieType,
        val objectIdentity: Long,
        val fields: List<CfirExportConstantField>,
    ) : CfirExportConstantNode
}

data class CfirExportConstantField(
    val symbol: CfirCallableSymbol<*>,
    val value: CfirExportConstantIdentity,
    val type: ConeCangJieType,
)

/**
 * 冻结常量值内容而不重新求值。先登记 identity 再排入工作队列；
 * 原对象图有回边时只记录 token，所有记录完成后才交付不可变结果。
 */
data class CfirExportConstantGraph(
    val root: CfirExportConstantIdentity,
    val nodes: List<CfirExportConstantNode>,
) {
    /** 校验原对象内容，不重新求值，也不将原可变值交给编码器。 */
    internal fun validateUnchanged() = root.validate(this)

    companion object {
        internal fun capture(value: CfirConstantValue): CfirExportConstantGraph {
            val identities = IdentityHashMap<CfirConstantValue, CfirExportConstantIdentity>()
            val pending = ArrayDeque<CfirConstantValue>()
            fun reference(value: CfirConstantValue): CfirExportConstantIdentity = identities[value] ?: run {
                val identity = CfirExportConstantIdentity(value)
                identities[value] = identity
                pending.addLast(value)
                identity
            }
            val root = reference(value)
            val nodes = mutableListOf<CfirExportConstantNode>()
            fun references(values: List<CfirConstantValue>): List<CfirExportConstantIdentity> =
                Collections.unmodifiableList(values.map(::reference))
            while (pending.isNotEmpty()) {
                val source = pending.removeFirst()
                val identity = identities.getValue(source)
                nodes += when (source) {
                    is CfirConstantValue.Primitive -> CfirExportConstantNode.Primitive(identity, source.type, source.kind, source.value)
                    is CfirConstantValue.Tuple -> CfirExportConstantNode.Tuple(identity, source.type, references(source.elements))
                    is CfirConstantValue.ArrayValue -> CfirExportConstantNode.Array(identity, source.type, references(source.elements))
                    is CfirConstantValue.VArray -> CfirExportConstantNode.VArray(identity, source.type, references(source.elements))
                    is CfirConstantValue.EnumValue -> CfirExportConstantNode.Enum(identity, source.type, source.constructor, references(source.arguments))
                    is CfirConstantValue.ObjectValue -> CfirExportConstantNode.Object(
                        identity, source.type, source.identity,
                        Collections.unmodifiableList(source.fields.entries.map { (symbol, value) ->
                            CfirExportConstantField(symbol, reference(value), source.storageTypes.getValue(symbol))
                        }),
                    )
                }
            }
            return CfirExportConstantGraph(root, Collections.unmodifiableList(nodes.toList()))
        }
    }
}
