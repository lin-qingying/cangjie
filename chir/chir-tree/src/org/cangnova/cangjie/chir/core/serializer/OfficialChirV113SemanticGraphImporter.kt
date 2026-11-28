package org.cangnova.cangjie.chir.core.serializer

import org.cangnova.cangjie.chir.core.checker.ChirValidationReportFormatter
import org.cangnova.cangjie.chir.core.checker.DefaultChirSourceSemanticGraphValidator
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticDomain
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticGraph
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticNode
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticNodeId
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticTable
import org.cangnova.cangjie.chir.core.model.ChirSourceSemanticValue
import java.util.Collections

/** 将版本化官方语义包导入仓内完整源语义图的契约。 */
interface OfficialChirV113SemanticGraphImporter {
    /** 保留完整四域根向量和所有嵌套表，引用先登记身份后解析，转换失败时不发布部分图。 */
    fun import(
        packageSemanticId: ChirSemanticId,
        semanticPackage: OfficialChirV113SemanticPackage,
    ): ChirSourceSemanticGraph
}

/**
 * 以两阶段身份登记与引用解析构造官方 v1.1.3 的仓内源语义图。
 *
 * 第一阶段为全部顶层 ID 注册仓内身份；第二阶段递归转换所有字段并解析跨域前向引用。只有每个根表和
 * 每条嵌套引用都成功转换后才返回不可变图。
 */
class DefaultOfficialChirV113SemanticGraphImporter : OfficialChirV113SemanticGraphImporter {
    override fun import(
        packageSemanticId: ChirSemanticId,
        semanticPackage: OfficialChirV113SemanticPackage,
    ): ChirSourceSemanticGraph {
        val ids = OfficialChirV113SemanticIdMap(packageSemanticId, semanticPackage)
        val nodes = LinkedHashMap<ChirSourceSemanticNodeId, ChirSourceSemanticNode>(semanticPackage.tableCount)
        val roots = linkedMapOf<ChirSourceSemanticDomain, List<ChirSourceSemanticNodeId>>()

        fun importDomain(
            officialDomain: OfficialChirV113TableDomain,
            indexedTables: List<OfficialChirV113IndexedTable>,
        ) {
            val domain = officialDomain.toChirDomain()
            val domainRoots = ArrayList<ChirSourceSemanticNodeId>(indexedTables.size)
            indexedTables.forEach { indexed ->
                if (indexed.unionTag == 0.toUByte() || indexed.table.schemaType == "NONE") {
                    throw OfficialChirV113FormatException(
                        "Official CHIR v1.1.3 $officialDomain#${indexed.id} has no registered union variant",
                    )
                }
                val id = ChirSourceSemanticNodeId(domain, ids.idOf(officialDomain, indexed.id))
                val node = ChirSourceSemanticNode(
                    id = id,
                    unionTag = indexed.unionTag,
                    table = importTable(indexed.table, ids),
                )
                if (nodes.put(id, node) != null) {
                    throw OfficialChirV113FormatException(
                        "Official CHIR v1.1.3 $officialDomain#${indexed.id} was registered more than once",
                    )
                }
                domainRoots += id
            }
            roots[domain] = Collections.unmodifiableList(domainRoots)
        }

        importDomain(OfficialChirV113TableDomain.TYPE, semanticPackage.types)
        importDomain(OfficialChirV113TableDomain.VALUE, semanticPackage.values)
        importDomain(OfficialChirV113TableDomain.EXPRESSION, semanticPackage.expressions)
        importDomain(OfficialChirV113TableDomain.DEFINITION, semanticPackage.definitions)

        val graph = ChirSourceSemanticGraph(
            packageSemanticId = packageSemanticId,
            roots = Collections.unmodifiableMap(roots),
            nodes = Collections.unmodifiableMap(nodes),
            metadata = importMetadata(semanticPackage.metadata, ids),
        )
        val validation = DefaultChirSourceSemanticGraphValidator().validate(graph)
        if (validation.hasErrors) {
            throw OfficialChirV113FormatException(ChirValidationReportFormatter.render(validation))
        }
        return graph
    }

    private fun importTable(
        source: OfficialChirV113SemanticTable,
        ids: OfficialChirV113SemanticIdMap,
    ): ChirSourceSemanticTable {
        if (source.schemaType.isBlank()) {
            throw OfficialChirV113FormatException("Official CHIR v1.1.3 contains a table with no schema type")
        }
        val fields = LinkedHashMap<String, ChirSourceSemanticValue>(source.fields.size)
        source.fields.forEach { (name, value) ->
            if (name.isBlank()) {
                throw OfficialChirV113FormatException(
                    "Official CHIR v1.1.3 ${source.schemaType} contains a field with no schema name",
                )
            }
            fields[name] = importValue(value, ids)
        }
        return ChirSourceSemanticTable(source.schemaType, Collections.unmodifiableMap(fields))
    }

    private fun importValue(
        source: OfficialChirV113SemanticValue,
        ids: OfficialChirV113SemanticIdMap,
    ): ChirSourceSemanticValue = when (source) {
        OfficialChirV113SemanticValue.NullValue -> ChirSourceSemanticValue.NullValue
        is OfficialChirV113SemanticValue.Scalar -> ChirSourceSemanticValue.Scalar(source.value)
        is OfficialChirV113SemanticValue.Bytes -> ChirSourceSemanticValue.Bytes(
            Collections.unmodifiableList(source.toByteArray().toList()),
        )
        is OfficialChirV113SemanticValue.Reference -> if (source.id == 0L) {
            ChirSourceSemanticValue.NullValue
        } else {
            val target = ChirSourceSemanticNodeId(source.domain.toChirDomain(), ids.idOf(source))
            ChirSourceSemanticValue.Reference(target)
        }
        is OfficialChirV113SemanticValue.Sequence -> ChirSourceSemanticValue.Sequence(
            Collections.unmodifiableList(source.values.map { importValue(it, ids) }),
        )
        is OfficialChirV113SemanticValue.Table -> ChirSourceSemanticValue.Table(importTable(source.value, ids))
        is OfficialChirV113SemanticValue.Union -> {
            if (source.discriminant == 0 || source.variant.isBlank() || source.variant == "NONE") {
                throw OfficialChirV113FormatException("Official CHIR v1.1.3 contains an unregistered nested union variant")
            }
            ChirSourceSemanticValue.Union(
                discriminant = source.discriminant,
                variant = source.variant,
                value = importTable(source.value, ids),
            )
        }
    }

    private fun importMetadata(
        metadata: OfficialChirV113PackageMetadata,
        ids: OfficialChirV113SemanticIdMap,
    ): Map<String, ChirSourceSemanticValue> = Collections.unmodifiableMap(
        linkedMapOf(
            "sourceFormat" to ChirSourceSemanticValue.Scalar("official-v1.1.3"),
            "name" to metadata.name.toSourceValue(),
            "path" to metadata.path.toSourceValue(),
            "accessLevel" to ChirSourceSemanticValue.Scalar(metadata.accessLevel.name),
            "phase" to ChirSourceSemanticValue.Scalar(metadata.phase.name),
            "packageInitFunction" to metadata.packageInitFunctionId.toValueReference(ids),
            "packageLiteralInitFunction" to metadata.packageLiteralInitFunctionId.toValueReference(ids),
            "maxImportedValueId" to ChirSourceSemanticValue.Scalar(metadata.maxImportedValueId),
            "maxImportedStructId" to ChirSourceSemanticValue.Scalar(metadata.maxImportedStructId),
            "maxImportedClassId" to ChirSourceSemanticValue.Scalar(metadata.maxImportedClassId),
            "maxImportedEnumId" to ChirSourceSemanticValue.Scalar(metadata.maxImportedEnumId),
            "maxImportedExtendId" to ChirSourceSemanticValue.Scalar(metadata.maxImportedExtendId),
        ),
    )

    private fun String?.toSourceValue(): ChirSourceSemanticValue =
        this?.let(ChirSourceSemanticValue::Scalar) ?: ChirSourceSemanticValue.NullValue

    private fun UInt.toValueReference(ids: OfficialChirV113SemanticIdMap): ChirSourceSemanticValue =
        if (this == 0u) {
            ChirSourceSemanticValue.NullValue
        } else {
            ChirSourceSemanticValue.Reference(
                ChirSourceSemanticNodeId(
                    ChirSourceSemanticDomain.VALUE,
                    ids.idOf(OfficialChirV113TableDomain.VALUE, this),
                ),
            )
        }

    private fun OfficialChirV113TableDomain.toChirDomain(): ChirSourceSemanticDomain = when (this) {
        OfficialChirV113TableDomain.TYPE -> ChirSourceSemanticDomain.TYPE
        OfficialChirV113TableDomain.VALUE -> ChirSourceSemanticDomain.VALUE
        OfficialChirV113TableDomain.EXPRESSION -> ChirSourceSemanticDomain.EXPRESSION
        OfficialChirV113TableDomain.DEFINITION -> ChirSourceSemanticDomain.DEFINITION
    }
}
