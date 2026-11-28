package org.cangnova.cangjie.chir.core.checker

import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticDomain
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticGraph
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticNodeId
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticTable
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticValue
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/** 校验完整源语义图及其跨域引用闭包的契约。 */
interface ChirSourceSemanticGraphValidator {
    /** 返回所有结构、身份或引用错误；只有无错误报告的图可发布给下游阶段。 */
    fun validate(graph: ChirSourceSemanticGraph): ChirValidationReport
}

/** 对四域完整语义图执行根索引、全局身份和递归引用闭包校验。 */
class DefaultChirSourceSemanticGraphValidator : ChirSourceSemanticGraphValidator {
    override fun validate(graph: ChirSourceSemanticGraph): ChirValidationReport {
        val issues = mutableListOf<ChirValidationIssue>()
        val expectedDomains = ChirSourceSemanticDomain.entries.toSet()
        if (graph.roots.keys != expectedDomains) {
            issue(
                issues,
                graph.packageSemanticId,
                "source graph must contain exactly the TYPE, VALUE, EXPRESSION and DEFINITION root domains",
            )
        }

        val identities = mutableSetOf<ChirSemanticId>()
        graph.nodes.forEach { (key, node) ->
            if (key != node.id) {
                issue(issues, node.id.semanticId, "source graph node key disagrees with its node identity")
            }
            if (!identities.add(node.id.semanticId)) {
                issue(issues, node.id.semanticId, "source graph reuses one semantic identity across ID domains")
            }
            if (node.unionTag == 0.toUByte() || node.table.schemaType.isBlank()) {
                issue(issues, node.id.semanticId, "source graph node has no registered union variant")
            }
        }

        val rootedIds = mutableSetOf<ChirSourceSemanticNodeId>()
        graph.roots.forEach { (domain, rootIds) ->
            val rootsInDomain = mutableSetOf<ChirSourceSemanticNodeId>()
            rootIds.forEach { rootId ->
                if (rootId.domain != domain) {
                    issue(issues, rootId.semanticId, "source graph root is stored in the wrong ID domain")
                }
                if (!rootsInDomain.add(rootId)) {
                    issue(issues, rootId.semanticId, "source graph root vector contains a duplicate identity")
                }
                rootedIds += rootId
                if (graph.nodes[rootId] == null) {
                    issue(issues, rootId.semanticId, "source graph root references a missing node")
                }
            }
        }
        if (rootedIds != graph.nodes.keys) {
            val unrooted = graph.nodes.keys - rootedIds
            unrooted.forEach { id -> issue(issues, id.semanticId, "source graph node is absent from its root vector") }
        }

        graph.nodes.values.forEach { node -> validateTable(node.table, node.id.semanticId, graph, issues) }
        graph.metadata.forEach { (name, value) ->
            if (name.isBlank()) issue(issues, graph.packageSemanticId, "source graph metadata has an empty field name")
            validateValue(value, graph.packageSemanticId, graph, issues)
        }

        return ChirValidationReport(issues)
    }

    private fun validateTable(
        table: ChirSourceSemanticTable,
        nodeId: ChirSemanticId,
        graph: ChirSourceSemanticGraph,
        issues: MutableList<ChirValidationIssue>,
    ) {
        if (table.schemaType.isBlank()) {
            issue(issues, nodeId, "source graph contains a nested table without a schema type")
        }
        table.fields.forEach { (name, value) ->
            if (name.isBlank()) issue(issues, nodeId, "source graph table contains an empty field name")
            validateValue(value, nodeId, graph, issues)
        }
    }

    private fun validateValue(
        value: ChirSourceSemanticValue,
        nodeId: ChirSemanticId,
        graph: ChirSourceSemanticGraph,
        issues: MutableList<ChirValidationIssue>,
    ) {
        when (value) {
            ChirSourceSemanticValue.NullValue,
            is ChirSourceSemanticValue.Scalar,
            is ChirSourceSemanticValue.Bytes,
            -> Unit

            is ChirSourceSemanticValue.Reference -> {
                if (graph.nodes[value.target] == null) {
                    issue(
                        issues,
                        nodeId,
                        "source graph reference targets missing " + value.target.domain + " node " + value.target.semanticId.value,
                    )
                }
            }

            is ChirSourceSemanticValue.Sequence -> {
                value.values.forEach { validateValue(it, nodeId, graph, issues) }
            }

            is ChirSourceSemanticValue.Table -> validateTable(value.value, nodeId, graph, issues)

            is ChirSourceSemanticValue.Union -> {
                if (value.discriminant == 0 || value.variant.isBlank() || value.variant == "NONE") {
                    issue(issues, nodeId, "source graph contains an unregistered nested union variant")
                }
                validateTable(value.value, nodeId, graph, issues)
            }
        }
    }

    private fun issue(
        issues: MutableList<ChirValidationIssue>,
        nodeId: ChirSemanticId,
        message: String,
    ) {
        issues += ChirValidationIssue(
            code = "INVALID_SOURCE_SEMANTIC_GRAPH",
            severity = ChirValidationSeverity.ERROR,
            message = message,
            nodeId = nodeId,
        )
    }
}
